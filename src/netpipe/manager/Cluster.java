package netpipe.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import mjoys.util.Address;
import mjoys.util.Logger;
import netpipe.generator.JobDes;
import cn.oasistech.agent.AgentProtocol;
import cn.oasistech.agent.GetIdResponse;
import cn.oasistech.agent.client.AgentAsynRpc;
import cn.oasistech.util.Tag;

public class Cluster {
    private Timer reportTaskStatusTimer = new Timer();
    private List<Host> members = new ArrayList<Host>();
    
    private final static Logger logger = new Logger().addPrinter(System.out);
    
    public void start() {
        // 解析配置文件，连接到所有工作节点
        
    	// 开始监控task状态
    	reportTaskStatusTimer.schedule(new UpdateTaskStatusTimerTask(), 0, TimeUnit.SECONDS.toMillis(1));
    }
    
    public void connectHost(String address) {
        Host host = Host.connect(Address.parse(address));
        if (host != null) {
            members.add(host);
        } else {
            logger.log("cant't connect address");
        }
    }
    
    public Host getHost(AgentAsynRpc rpc) {
        for (Host host : members) {
            if (rpc == host.getAsynRpc()) {
                return host;
            }
        }
        return null;
    }
    
    public void scheduleJob(JobDes jobDes) {
        if (isTooBusyToSchedule()) {
            logger.log("warning: all node is busy, can't schedule new task");
            return;
        }
        
        // init running tasks
        int i = 0;
        int t = 0;
        
        HashSet<Host> hosts = new HashSet<Host>();
        List<RunningTask> runningTasks = new ArrayList<RunningTask>(jobDes.getTasks().size());
        while (true) {
            Host host = members.get(i);
            hosts.add(host);
            if (host.getStatus().getLoad() < host.getStatus().getMaxLoad()) {
                runningTasks.add(members.get(i).initRunningTask(jobDes.getTasks().get(t)));
                t++;
            }
            i = (i + 1) % members.size();
            if (t == jobDes.getTasks().size()) {
                break;
            }
        }
        
        // link pipes
        for (RunningTask t1 : runningTasks) {
            for (InPipe in : t1.getInPipes().values()) {
                for (RunningTask t2 : runningTasks) {
                    if (t1 != t2) {
                        String inPipeName = in.getName();
                        OutPipe out = t2.getOutPipes().get(inPipeName);
                        if (out != null) {
                            in.setPeer(out);
                            out.getPeers().add(in);
                            break;
                        }
                    }
                }
            }
        }
        
        // deploy
        for (Host host : hosts) {
            host.deployJob(jobDes.getJob().name());
        }
        
        // start task
        for (RunningTask task : runningTasks) {
            task.getHost().runTask(task);
        }
    }
    
    public boolean isTooBusyToSchedule() {
        for (Host host : members) {
            if (host.getStatus().getLoad() < host.getStatus().getMaxLoad()) {
                return false;
            }
        }
        return true;
    }
    
    private Host getIdleHost() {
        double load = 0;
        Host idleHost = null;
        for (Host host : members) {
            if (host.getStatus().getLoad() > load) {
                load = host.getStatus().getLoad();
                idleHost = host;
            }
        }
        
        return idleHost;
    }
    
    public void requestTaskStatus() {
        for (Host host : members) {
            host.requestTaskStatus();
        }
    }
    
    public class UpdateTaskStatusTimerTask extends TimerTask {
        @Override
        public void run() {
            requestTaskStatus();
        }
    }
}
