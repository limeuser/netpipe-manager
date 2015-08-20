package netpipe.generator;

import java.util.List;
import java.util.ArrayList;

import netpipe.pipe.Job;

public class JobDes {
    private Job job;
    private Class<?> jobClass;
    private List<TaskDes> tasks = new ArrayList<TaskDes>();
    
    public Job getJob() {
        return job;
    }
    public void setJob(Job job) {
        this.job = job;
    }
    public List<TaskDes> getTasks() {
        return tasks;
    }
    public void setTasks(List<TaskDes> tasks) {
        this.tasks = tasks;
    }
    public Class<?> getJobClass() {
        return jobClass;
    }
    public void setJobClass(Class<?> jobClass) {
        this.jobClass = jobClass;
    }
    
    public PipeDes getInPipe(TaskDes self, String name) {
        for (TaskDes task : tasks) {
            if (task != self) {
                PipeDes pipe = task.getInPipe(name);
                if (pipe != null) {
                    return pipe;
                }
            }
        }
        
        return null;
    }
    
    public PipeDes getOutPipe(TaskDes self, String name) {
        for (TaskDes task : tasks) {
            if (task != self) {
                PipeDes pipe = task.getOutPipe(name);
                if (pipe != null) {
                    return pipe;
                }
            }
        }
        
        return null;
    }
    
    public void linkPipes() {
        for (TaskDes task : tasks) {
            for (PipeDes in : task.getInPipe()) {
                in.peer = this.getOutPipe(task, in.name);
            }
            for (PipeDes out : task.getOutPipe()) {
                out.peer = this.getInPipe(task, out.name);
            }
        }
    }
}
