package mjoys.netpipe.generator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import mjoys.netpipe.pipe.Task;

public class TaskDes {
    private JobDes jobInfo;
    
    private Task task;
    private Method method;
    private List<PipeDes> inPipe = new ArrayList<PipeDes>();
    private List<PipeDes> outPipe = new ArrayList<PipeDes>();
    
    public Task getTask() {
        return task;
    }
    public void setTask(Task task) {
        this.task = task;
    }
    public Method getMethod() {
        return method;
    }
    public void setMethod(Method method) {
        this.method = method;
    }
    public List<PipeDes> getInPipe() {
        return inPipe;
    }
    public void setInPipe(List<PipeDes> inPipe) {
        this.inPipe = inPipe;
    }
    public List<PipeDes> getOutPipe() {
        return outPipe;
    }
    public void setOutPipe(List<PipeDes> outPipe) {
        this.outPipe = outPipe;
    }
    public PipeDes getInPipe(String name) {
        return getPipe(inPipe, name);
    }
    public PipeDes getOutPipe(String name) {
        return getPipe(outPipe, name);
    }
    public JobDes getJobInfo() {
        return jobInfo;
    }
    public void setJobInfo(JobDes jobInfo) {
        this.jobInfo = jobInfo;
    }
    public String name() {
    	String name;
    	if (task.name().isEmpty()) {
            name = method.getName();
        } else {
            name = task.name();
        }
    	StringBuilder str = new StringBuilder(name);
        str.setCharAt(0, Character.toUpperCase(str.charAt(0)));
        return str.toString();
    }
    public static PipeDes getPipe(List<PipeDes> pipes, String name) {
        for (PipeDes pipe : pipes) {
            if (pipe.name.equals(name)) {
                return pipe;
            }
        }
        return null;
    }
}
