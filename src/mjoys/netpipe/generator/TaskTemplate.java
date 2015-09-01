package mjoys.netpipe.generator;

import java.lang.reflect.Type;
import java.util.List;

import mjoys.netpipe.manager.OutPipe;
import mjoys.util.Logger;

public class TaskTemplate {
	private final static Logger logger = new Logger().addPrinter(System.out);
	
    private static final String manifest = 
            "Manifest-Version: 1.0\r\n" + 
            "Class-Path: @ClassPath\r\n" +
            "Main-Class: @MainClass";
    
    private static final String mainTemplate = 
            "package task;\r\n\r\n" +
            "public class Main {\r\n" + 
            "    public static main(String[] args) {\r\n" +
            "        Runner runner = new Runner(@JobName, new task.@TaskName());\r\n" +
            "        runner.runTask();\r\n" +
            "    }\r\n" +
            "}";
    
    private static final String taskClassTemplate = 
            "package task;\r\n\r\n" +
            "import mjoys.netpipe.pipe.*;\r\n\r\n" +
            "public class @TaskNameMain {\r\n" +
            "public static class @TaskNameRunner extends TaskServer {\r\n" +
            "@InPipeVars" + 
            "@OutPipeVars" +
            "    private @JobClassName job = new @JobClassName();\r\n" +
            "    public @TaskNameRunner(int taskId) {\r\n" +
            "        super(\"@JobName\", \"@TaskName\", taskId);\r\n" +
            "        @AddInPipes\r\n" +
            "        @AddOutPipes\r\n" + 
            "    }\r\n" +
            "@TaskMethods\r\n" +
            "}\r\n" +
            "public static void main(String[] args) {\r\n" +
            "    TaskRunner runner = new @TaskNameRunner(Integer.parseInt(args[0]));\r\n" +
            "    runner.runTask();\r\n" +
            "}\r\n" +
            "}";
    
    public static final String getTaskClassSourceCode(JobDes jobInfo, TaskDes taskInfo) {
        return taskClassTemplate
        .replaceAll("@JobClassName", jobInfo.getJobClass().getName())
        .replaceAll("@JobName", jobInfo.getJob().name())
        .replaceAll("@TaskName", taskInfo.name())
        .replaceAll("@InPipeVars", getInPipeVars(taskInfo.getInPipe()))
        .replaceAll("@OutPipeVars", getOutPipeVars(taskInfo.getOutPipe()))
        .replaceAll("@TaskMethods", getTaskMethod(taskInfo))
        .replaceAll("@AddInPipes", addInPipes(taskInfo.getInPipe()))
        .replaceAll("@AddOutPipes", addOutPipes(taskInfo.getOutPipe()));
    }
    
    public static final String getMainClassSourceCode(JobDes jobInfo) {
        return mainTemplate.replaceAll("@JobClassName", jobInfo.getJobClass().getName())
                .replaceAll("@JobName", jobInfo.getJob().name());
    }
    
    public static final String getManifest(String classPath, String mainClass) {
        return manifest.replaceAll("@ClassPath", classPath).replaceAll("@MainClass", mainClass);
    }
    
    private static final String taskMethodTemplate = 
            "    @Override\r\n" +
            "    public void runTask() {\r\n" +
            "        @CallTaskMethod\r\n" + 
            "    }";
    
    public static final String getTaskMethod(TaskDes task) {
        return taskMethodTemplate.replaceAll("@TaskMethodName", task.getMethod().getName())
        .replaceAll("@CallTaskMethod", getCallTaskMethod(task));
    }
    
    public static final String getCallTaskMethod(TaskDes task) {
        StringBuilder str = new StringBuilder();
        str.append("job.").append(task.getMethod().getName())
        .append("(");
        
        int inIndex = 0, outIndex = 0;
        for (Type paramType : task.getMethod().getParameterTypes()) {
            if (paramType.toString().contains("mjoys.netpipe.pipe.InPipe")) {
                str.append(task.getInPipe().get(inIndex++).name).append(", ");
            } else if (paramType.toString().contains("mjoys.netpipe.pipe.OutPipe")) {
                str.append(task.getOutPipe().get(outIndex++).name).append(", ");
            } else {
                logger.log("parameter must be pipe type");
            }
        }
        if (task.getMethod().getParameterTypes().length > 0)
        	str.setLength(str.length() - ", ".length());
        
        str.append(");");
        
        return str.toString();
    }
    
    private static final String inPipeTemplate = "    private InPipe<@E> @InPipeName = new TcpInPipe<@E>(\"@InPipeName\");";
    private static final String outPipeTemplate = "    private OutPipe<@E> @OutPipeName = new TcpOutPipe<@E>(\"@OutPipeName\");";
    
    public static final String getInPipeVars(List<PipeDes> inPipes) {
        StringBuilder str = new StringBuilder();
        for (PipeDes p : inPipes) {
            str.append(inPipeTemplate.replaceAll("@E", p.elementType.toString().replace("$", ".").replace("class","").trim())
                                     .replaceAll("@InPipeName", p.name));
            str.append("\r\n");
        }

        return str.toString();
    }
    
    private static final String addInPipeTemplate = "addInPipe(@InPipeName);";
    private static final String addOutPipeTemplate = "addOutPipe(@OutPipeName);";
    public static final String addInPipes(List<PipeDes> inPipes) {
        StringBuilder str = new StringBuilder();
        for (PipeDes p : inPipes) {
            str.append(addInPipeTemplate.replaceAll("@InPipeName", p.name));
            str.append("\r\n");
        }
        if (!inPipes.isEmpty()) {
            str.setLength(str.length() - "\r\n".length());
        }

        return str.toString();
    }
    
    public static final String addOutPipes(List<PipeDes> outPipes) {
        StringBuilder str = new StringBuilder();
        for (PipeDes p : outPipes) {
            str.append(addOutPipeTemplate.replaceAll("@OutPipeName", p.name));
            str.append("\r\n");
        }
        if (!outPipes.isEmpty()) {
            str.setLength(str.length() - "\r\n".length());
        }
        return str.toString();
    }
    
    public static final String getOutPipeVars(List<PipeDes> outPipes) {
        StringBuilder str = new StringBuilder();
        for (PipeDes p : outPipes) {
            str.append(outPipeTemplate.replaceAll("@E", p.elementType.toString().replace("$", ".").replace("class","").trim())
                                      .replaceAll("@OutPipeName", p.name));
            str.append("\r\n");
        }

        return str.toString();
    }
}