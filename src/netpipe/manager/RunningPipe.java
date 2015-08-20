package netpipe.manager;

import mjoys.util.Address;
import netpipe.pipe.PipeStatus;

public class RunningPipe {
    private String name;
    private PipeStatus status;
    private RunningTask runningTask;
    
    public final static InPipe newInPipe(RunningTask task, String name) {
        InPipe in = new InPipe();
        in.setRunningTask(task);
        in.setName(name);
        in.setStatus(PipeStatus.newPipeStatus());
        return in;
    }
    
    public final static OutPipe newOutPipe(RunningTask task, String name, Address address) {
        OutPipe out = new OutPipe();
        out.setRunningTask(task);
        out.setName(name);
        out.setStatus(PipeStatus.newPipeStatus());
        return out;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public PipeStatus getStatus() {
        return status;
    }
    public void setStatus(PipeStatus status) {
        this.status = status;
    }
    public RunningTask getRunningTask() {
        return runningTask;
    }
    public void setRunningTask(RunningTask runningTask) {
        this.runningTask = runningTask;
    }
}
