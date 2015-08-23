package netpipe.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mjoys.util.Address;
import mjoys.util.IdGenerator;
import mjoys.util.Logger;
import mjoys.util.PathUtil;
import mjoys.util.StringUtil;
import netpipe.core.Service;
import netpipe.generator.PipeDes;
import netpipe.generator.TaskDes;
import netpipe.msg.MsgType;
import netpipe.pipe.PipeStatus;
import netpipe.pipe.TaskStatus;
import service.ftp.FtpClient;
import service.os.OSClient;
import cn.oasistech.agent.AgentProtocol;
import cn.oasistech.agent.GetIdResponse;
import cn.oasistech.agent.client.AgentAsynRpc;
import cn.oasistech.agent.client.AgentSyncRpc;
import cn.oasistech.util.Tag;

public class Host implements Comparable<Host> {
    private String ip;
    private String name;
    private HostStatus status;
    private FtpClient ftpClient;
    private OSClient osClient;
    private AgentSyncRpc syncRpc;
    private AgentAsynRpc asynRpc;
    private Map<String, Integer> services;
    
    private final static IdGenerator taskIdGenerator = new IdGenerator(1);
    private Map<Integer, RunningTask> runningTasks = new HashMap<Integer, RunningTask>();
    
    private final static Logger logger = new Logger().addPrinter(System.out);
    
    public static Host connect(Address address) {
        Host host = new Host();
        
        host.syncRpc = new AgentSyncRpc();
        if (host.syncRpc.start(address) == false) {
            return null;
        }
        
        host.asynRpc = new AgentAsynRpc();
        if (host.asynRpc.start(address, new TaskMsgHandler(host)));
        
        host.name = address.toString();
        host.services = new HashMap<String, Integer>();
        
        host.addAllServices();
        
        return host;
    }
    
    public boolean runTask(RunningTask task) {
        osClient.runTask(services.get(Service.os.name()), task.getJobName(), task.getTaskName());
        return true;
    }
    
    private void addAllServices() {
    	// os interface
    	addService(Service.os.name());
    	
    	listenRunningTask();
    }
    
    public Map<String, Integer> getServices() {
    	return services;
    }
    
    private void listenRunningTask() {
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(new Tag(AgentProtocol.PublicTag.servicename.name(), Service.dpipe_task.name()));
        this.asynRpc.listenConnection(tags);
    }
    
    public void runningTaskConnected(int taskId, int agentId) {
        RunningTask task = runningTasks.get(taskId);
        if (task == null) {
            logger.log("running task connected, but can't find the task: id=%d", taskId);
            return;
        }
        
        task.setAgentId(agentId);
        task.setStage(RunningTask.Stage.connected);
    }

    public void updateRunningTaskStatus(TaskStatus status) {
        RunningTask task = runningTasks.get(status.getTaskId());
        if (task == null) {
            logger.log("can't find running task when upate task status: id=%d", status.getTaskId());
            return;
        }
        task.setWorkerCount(status.getWorkerCount());
        for (Entry<String, PipeStatus> ps : status.getPipeStatus().entrySet()) {
            String pname = ps.getKey();
            PipeStatus pstatus = ps.getValue();
            InPipe in = task.getInPipes().get(pname);
            if (in != null) {
                in.setStatus(pstatus);
            }
            OutPipe out = task.getOutPipes().get(pname);
            if (out != null) {
                out.setStatus(pstatus);
            }
            if (in == null && out == null) {
                logger.log("can't find pipe status in master:host=%s, taskid=%d, job=%s, task=%s",
                    task.getHost().getIp(), status.getTaskId(), task.getJobName(), task.getTaskName());
            }
        }
    }
    
    public void requestTaskStatus() {
    	try {
	        for (RunningTask task : runningTasks.values()) {
	            this.asynRpc.sendMsg(task.getAgentId(), MsgType.ReportStatus.ordinal(), null);
	        }
    	} catch (Exception e) {
    		logger.log("requestTaskStatus exception", e);
    	}
    }
    
    public RunningTask initRunningTask(TaskDes taskInfo) {
        String job = taskInfo.getJobInfo().getJob().name();
        String task = taskInfo.getTask().name();
        RunningTask runningTask = RunningTask.newRunningTask(job, task, taskIdGenerator.getId(), this);

        for (PipeDes p : taskInfo.getInPipe()) {
            runningTask.getInPipes().put(p.name, InPipe.newInPipe(runningTask, p.name));
        }
        for (PipeDes p : taskInfo.getOutPipe()) {
            runningTask.getOutPipes().put(p.name, OutPipe.newOutPipe(runningTask, p.name, allocOutPipeAddress()));
        }
        
        return runningTask;
    }

    private boolean addService(String serviceName) {
    	int id = getAndListenService(serviceName);
    	if (id > 0) {
    		this.services.put(serviceName, id);
    		return true;
    	}
    	return false;
    }
    
    private int getAndListenService(String serviceName) {
    	List<Tag> tags = new ArrayList<Tag>();
    	tags.add(new Tag(AgentProtocol.PublicTag.servicename.name(), serviceName));
    	
    	GetIdResponse response = this.syncRpc.getId(tags);
    	if (response != null && response.getError().equals(AgentProtocol.Error.Success)) {
    		if (response.getIds().size() > 0) {
    		    this.asynRpc.listenConnection(tags);
    			return response.getIds().get(0);
    		}
    	}
    	return 0;
    }
    
    public boolean deployJob(String jobName) {
    	ftpClient.upload("/usr/netpipe/", PathUtil.combine("job", jobName, "jar", jobName + ".jar"));
        return true;
    }
    
    public Address allocOutPipeAddress() {
        return Address.parse("tcp://" + this.ip + ":" + osClient.allocatePort(services.get(Service.os.name()), Address.Protocol.Tcp));
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj == null) {
    		return false;
    	}
    	if ((obj instanceof Host) == false) {
    		return false;
    	}
    	
    	Host host = (Host) obj;
    	if (StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(host.getName())) {
    		return name.equals(host.getName());
    	}
    	
    	return ip.equals(host.getIp());
    }
    
    @Override
    public int hashCode() {
    	if (StringUtil.isNotEmpty(name)) {
    		return name.hashCode();
    	}
    	
    	return this.ip.hashCode();
    }
    
    public int compareTo(Host host) {
    	if (StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(host.getName())) {
    		return name.compareTo(host.getName());
    	}
    	return ip.compareTo(host.getIp());
    }
    
    public AgentSyncRpc getSyncRpc() {
    	return syncRpc;
    }
    
    public AgentAsynRpc getAsynRpc() {
        return asynRpc;
    }
    
    public String getName() {
    	return this.name;
    }
    
    public String getIp() {
    	return this.ip;
    }
    
    public Map<Integer, RunningTask> getRunningTasks() {
        return this.runningTasks;
    }

    public HostStatus getStatus() {
        return status;
    }

    public void setStatus(HostStatus status) {
        this.status = status;
    }
}
