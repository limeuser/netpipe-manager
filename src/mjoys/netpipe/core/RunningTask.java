package mjoys.netpipe.core;

import java.util.List;

import mjoys.agent.client.AgentAsynRpc;
import mjoys.netpipe.pipe.InPipe;
import mjoys.netpipe.pipe.OutPipe;

public class RunningTask {
    private String jobName;
    private String taskName;
    private List<InPipe<?>> ins;
    private List<OutPipe<?>> outs;
    private AgentAsynRpc agentRpc;
    
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
    public List<InPipe<?>> getIns() {
        return ins;
    }
    public void setIns(List<InPipe<?>> ins) {
        this.ins = ins;
    }
    public List<OutPipe<?>> getOuts() {
        return outs;
    }
    public void setOuts(List<OutPipe<?>> outs) {
        this.outs = outs;
    }
    public AgentAsynRpc getAgentRpc() {
        return agentRpc;
    }
    public void setAgentRpc(AgentAsynRpc agentRpc) {
        this.agentRpc = agentRpc;
    }
}
