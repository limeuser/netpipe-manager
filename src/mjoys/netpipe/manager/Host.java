package mjoys.netpipe.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mjoys.agent.Agent;
import mjoys.agent.GetIdResponse;
import mjoys.agent.client.AgentAsynRpc;
import mjoys.agent.client.AgentSyncRpc;
import mjoys.agent.service.ftp.FtpClient;
import mjoys.agent.service.netpipe.TaskClient;
import mjoys.agent.service.os.OSClient;
import mjoys.agent.util.Tag;
import mjoys.netpipe.core.Service;
import mjoys.netpipe.generator.PipeDes;
import mjoys.netpipe.generator.TaskDes;
import mjoys.netpipe.pipe.NetPipeCfg;
import mjoys.netpipe.pipe.PipeStatus;
import mjoys.netpipe.pipe.TaskStatus;
import mjoys.netpipe.util.NetPipeManagerCfg;
import mjoys.util.Address;
import mjoys.util.IdGenerator;
import mjoys.util.Logger;
import mjoys.util.PathUtil;
import mjoys.util.StringUtil;

public class Host implements Comparable<Host> {
	private String ip;
    private HostStatus status;
    private FtpClient ftpClient;
    private OSClient osClient;
    private TaskClient taskClient;
    private AgentSyncRpc syncRpc;
    private AgentAsynRpc asynRpc;
    private Map<String, Integer> services;
    private Map<Integer, Integer> tasks = new HashMap<Integer, Integer>();
    
    private final static IdGenerator taskIdGenerator = new IdGenerator(1);
    private Map<Integer, RunningTask> runningTasks = new HashMap<Integer, RunningTask>();
    
    private final static Logger logger = new Logger().addPrinter(System.out);
    
    public static Host connect(String ip, Address agentAddress, Address ftpAddress) {
        Host host = new Host();
        
        host.ip = ip;
        host.status = new HostStatus();
        host.status.setMaxLoad(NetPipeManagerCfg.instance.getDefaultMaxLoad());
        host.services = new HashMap<String, Integer>();
        
        host.syncRpc = new AgentSyncRpc();
        if (host.syncRpc.start(agentAddress) == false) {
            return null;
        }
        
        host.asynRpc = new AgentAsynRpc();
        if (host.asynRpc.start(agentAddress, new NetPipeMsgHandler(host)) == false) {
        	return null;
        }
        
        host.asynRpc.setTag(new Tag(Agent.PublicTag.servicename.name(), NetPipeCfg.AgentTag.netpipe_manager.name()));
        
    	host.addService(Service.os.name());
    	host.osClient = new OSClient(host.syncRpc);
    	host.ftpClient = new FtpClient(ftpAddress, host.syncRpc);
    	host.taskClient = new TaskClient(host.asynRpc);
    	
    	host.listenRunningTask();
        
        return host;
    }
    
    public boolean runTask(RunningTask task) {
        int pid = osClient.runTask(services.get(Service.os.name()), task.getJobName(), task.getTaskName(), task.getId());
        task.setPid(pid);
        return pid >= 0;
    }
    
    public void bindOutPipe(RunningTask task) {
    	List<String> addresses = new ArrayList<String>();
    	for (OutPipe out : task.getOutPipes().values()) {
    		addresses.add(out.getStatus().getAddress().toString());
    	}
    	// bind all out pipes
    	this.taskClient.bindOutPipe(task.getAgentId(), task.getId(), addresses);
    }
    
    public void outPipeBound(int taskId, String outPipeName, String outPipeAddress) {
    	RunningTask task = runningTasks.get(taskId);
    	OutPipe outPipe = task.getOutPipes().get(outPipeName);
    	outPipe.getStatus().setAddress(Address.parse(outPipeAddress));
    	for (InPipe in : outPipe.getPeers()) {
    		this.taskClient.connectOutPipe(in.getRunningTask().getAgentId(), in.getRunningTask().getId(), in.getName(), outPipeAddress);
    	}
    }
    
    private void listenRunningTask() {
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(new Tag(NetPipeCfg.AgentTag.netpipe_taskid.name(), ""));
        this.asynRpc.listenConnection(tags);
    }
    
    public void runningTaskConnected(int taskId, int agentId) {
        RunningTask task = runningTasks.get(taskId);
        if (task == null) {
            logger.log("running task connected, but can't find the task: id=%d", taskId);
            return;
        }
        
        task.setAgentId(agentId);
        if (task.getStage() == RunningTask.Stage.connected) {
        	return;
        }
        
        task.setStage(RunningTask.Stage.connected);
        
        bindOutPipe(task);
    }

    public void updateRunningTaskStatus(TaskStatus status) {
        RunningTask task = runningTasks.get(status.getTaskId());
        if (task == null) {
            logger.log("can't find running task when upate task status: id=%d", status.getTaskId());
            return;
        }
        
        logger.log("update task status:%s", status.toString());
        
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
	            this.taskClient.getTaskStatus(task.getAgentId(), task.getId());
	        }
    	} catch (Exception e) {
    		logger.log("requestTaskStatus exception", e);
    	}
    }
    
    public RunningTask initRunningTask(TaskDes taskInfo) {
        String job = taskInfo.getJobInfo().getJob().name();
        String task = taskInfo.name();
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
    	tags.add(new Tag(Agent.PublicTag.servicename.name(), serviceName));
    	
    	GetIdResponse response = this.syncRpc.getId(tags);
    	if (response != null && response.getError().equals(Agent.Error.Success)) {
    		if (response.getIds().size() > 0) {
    		    this.asynRpc.listenConnection(tags);
    			return response.getIds().get(0);
    		}
    	}
    	return 0;
    }
    
    public boolean deployJob(String jobName) {
    	String fileName = jobName + ".jar";
    	String dst = PathUtil.combineWithSep("/", "/$NETPIPE_HOME", "jobs", fileName);
    	String src = PathUtil.combine(NetPipeManagerCfg.instance.getJobPath(), jobName, "jar", fileName);
    	return ftpClient.upload(dst, src);
    }
    
    public Address allocOutPipeAddress() {
    	Address.Protocol protocol = NetPipeManagerCfg.instance.getPipeProtocol();
		int port = osClient.allocatePort(services.get(Service.os.name()), protocol);
		Address address = Address.newAddress(protocol, Address.getAddressWithPort(this.ip, port));
		return address;
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
    	if (StringUtil.isNotEmpty(ip) && StringUtil.isNotEmpty(host.getIp())) {
    		return ip.equals(host.getIp());
    	}
    	
    	return ip.equals(host.getIp());
    }
    
    @Override
    public int hashCode() {
    	if (StringUtil.isNotEmpty(ip)) {
    		return ip.hashCode();
    	}
    	
    	return this.ip.hashCode();
    }
    
    public int compareTo(Host host) {
    	if (StringUtil.isNotEmpty(ip) && StringUtil.isNotEmpty(host.getIp())) {
    		return ip.compareTo(host.getIp());
    	}
    	return ip.compareTo(host.getIp());
    }
    
    public AgentSyncRpc getSyncRpc() {
    	return syncRpc;
    }
    
    public AgentAsynRpc getAsynRpc() {
        return asynRpc;
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
    
    public Map<Integer, Integer> getTasks() {
    	return tasks;
    }
    
    public TaskClient getTaskClient() {
    	return this.taskClient;
    }
    
    
    public Map<String, Integer> getServices() {
    	return services;
    }
}
