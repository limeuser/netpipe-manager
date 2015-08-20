package netpipe.manager;

import java.util.HashMap;
import java.util.Map;

public class RunningTask {
	private int id;
    private int pid;
    private int agentId;
    private Stage stage;
    private int workerCount;
    
    private Host host;
    private String jobName;
    private String taskName;
    
    private Map<String, InPipe> inPipes = new HashMap<String, InPipe>();
    private Map<String, OutPipe> outPipes = new HashMap<String, OutPipe>();

    public final static RunningTask newRunningTask(String jobName, String taskName, int id, Host host) {
        RunningTask runningTask = new RunningTask();
        runningTask.id = id;
        runningTask.host = host;
        runningTask.jobName = jobName;
        runningTask.taskName = taskName;
        runningTask.stage = Stage.initialized;
        return runningTask;
    }
    
    public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getPid() {
		return pid;
	}
	public void setPid(int pid) {
		this.pid = pid;
	}
	public int getAgentId() {
		return agentId;
	}
	public void setAgentId(int agentId) {
		this.agentId = agentId;
	}
    public int getWorkerCount() {
        return workerCount;
    }
    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }
    public Map<String, InPipe> getInPipes() {
        return inPipes;
    }
    public void setInPipes(Map<String, InPipe> inPipes) {
        this.inPipes = inPipes;
    }
    public Map<String, OutPipe> getOutPipes() {
        return outPipes;
    }
    public void setOutPipes(Map<String, OutPipe> outPipes) {
        this.outPipes = outPipes;
    }
    public Host getHost() {
        return host;
    }
    public void setHost(Host host) {
        this.host = host;
    }
    
    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public enum Stage {
        initialized,
        deployed,
        connected,
        killed,
    }
}
