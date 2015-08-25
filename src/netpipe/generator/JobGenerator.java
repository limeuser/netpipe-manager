package netpipe.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mjoys.util.Logger;
import mjoys.util.SystemUtil;
import netpipe.pipe.Job;
import netpipe.pipe.Task;
import netpipe.util.Cfg;

import org.apache.commons.io.FileUtils;

public class JobGenerator {
    private static Map<String, JobDes> jobs = new HashMap<String, JobDes>();
    private final static Logger logger = new Logger().addPrinter(System.out);
    
    public final static void generate(String jobSrcRoot, Class<?> jobClass) {
        JobDes jobDes = getJobDes(jobClass);
        
        // 生成数据流
        
        // 编译每个任务文件，打jar包
        try {
            createSourceAndJar(jobSrcRoot, jobDes);
        } catch (IOException e) {
            logger.log("create source code exception:", e);
        }

        jobs.put(jobDes.getJob().name(), jobDes);
    }
    
    private static JobDes getJobDes(Class<?> jobClass) {
        Job jobAnnotation = jobClass.getAnnotation(Job.class);
        if (jobAnnotation == null) {
            logger.log("not a job, can't find job annotation");
            return null;
        }
        
        JobDes jobInfo = new JobDes();
        jobInfo.setJob(jobAnnotation);
        jobInfo.setJobClass(jobClass);
        
        // 查找所有task方法
        Method[] methods = jobClass.getDeclaredMethods();
        for (Method m : methods) {
            Task task = m.getAnnotation(Task.class);
            if (task != null) {
                TaskDes taskInfo = new TaskDes();
                taskInfo.setTask(task);
                taskInfo.setMethod(m);
                for (Type paramType : m.getGenericParameterTypes()) {
                    PipeDes pipeInfo = new PipeDes();
                    
                    if (paramType instanceof ParameterizedType) {
                        Type genericParamType = ((ParameterizedType) paramType).getActualTypeArguments()[0];
                        pipeInfo.elementType = genericParamType;
                    }
                    
                    if (paramType.toString().contains("netpipe.core.InPipe")) {
                        taskInfo.getInPipe().add(pipeInfo);
                    } else if (paramType.toString().contains("netpipe.core.OutPipe")) {
                        taskInfo.getOutPipe().add(pipeInfo);
                    } else {
                        logger.log("error: bad type of task method");
                    }
                }
                
                if (taskInfo.getInPipe().size() != taskInfo.getTask().in().length ||
                    taskInfo.getOutPipe().size() != taskInfo.getTask().out().length) {
                    logger.log("pipe number not same");
                } else {
                    for (int i = 0; i < taskInfo.getTask().in().length; i++) {
                        taskInfo.getInPipe().get(i).name = taskInfo.getTask().in()[i];
                    }
                    for (int i = 0; i < taskInfo.getTask().out().length; i++) {
                        taskInfo.getOutPipe().get(i).name = taskInfo.getTask().out()[i];
                    }
                    jobInfo.getTasks().add(taskInfo);
                }
            }
        }
        
        if (jobInfo.getTasks().isEmpty()) {
            logger.log("no task");
            return null;
        }
        
        return jobInfo;
    }
    
    private static void createSourceAndJar(String jobSrcRoot, JobDes jobInfo) throws IOException {
        String jobPath = Cfg.getJobPath();
        File jobDir = new File(jobPath, jobInfo.getJob().name());
        jobDir.mkdir();
        File srcDir = new File(jobDir.getPath(), "src");
        File jarDir = new File(jobDir.getPath(), "jar");
        File classDir = new File(jobDir.getPath(), "classes");
        srcDir.mkdir();
        jarDir.mkdir();
        classDir.mkdir();
        
        File taskDir = new File(srcDir, "task");
        taskDir.mkdir();
        
        // 复制job源代码文件
        File originJobSrcDir = new File(jobSrcRoot);
        FileUtils.copyDirectory(srcDir, originJobSrcDir);
        
        // 生成每个任务的源代码文件
        for (TaskDes task : jobInfo.getTasks()) {
            List<TaskDes> children = getChildren(task.name(), jobInfo.getTasks());
            children.add(task);
            
            Set<PipeDes> inPipes = new HashSet<PipeDes>();
            Set<PipeDes> outPipes = new HashSet<PipeDes>();
            mergePipeParams(children, inPipes, outPipes);
            
            String taskJavaFile = TaskTemplate.getTaskClassSourceCode(jobInfo, task, children, inPipes, outPipes);
            
            File srcFile = new File(taskDir, task.name().toUpperCase() + "Runner.java");
            srcFile.createNewFile();
            FileWriter out = new FileWriter(srcFile);
            out.write(taskJavaFile);
            out.close();
        }
        
        // 生成入口文件
        String mainJavaFile = TaskTemplate.getMainClassSourceCode(jobInfo);
        File mainFile = new File(taskDir, "Main.java");
        mainFile.createNewFile();
        FileWriter out = new FileWriter(mainFile);
        out.write(mainJavaFile);
        out.close();
        
        // 编译
        JavaCompiler.compile(srcDir, classDir.getAbsolutePath());
        
        String manifest = TaskTemplate.getManifest("./lib/*.jar", "./task/Main");
        File manifestFile = new File(jarDir, "MANIFEST.mi");
        manifestFile.createNewFile();
        out = new FileWriter(manifestFile);
        out.write(manifest);
        out.close();
        
        // 打包jar文件
        SystemUtil.run("jar cf MANIFEST.mi " + jarDir.getAbsolutePath() + jobInfo.getJob().name() + ".jar " + classDir.getAbsolutePath());
    }
    
    private static List<TaskDes> getChildren(String parent, List<TaskDes> tasks) {
        List<TaskDes> children = new ArrayList<TaskDes>();
        for (TaskDes task : tasks) {
            if (task.getTask().parent().equalsIgnoreCase(parent)) {
                children.add(task);
            }
        }
        return children;
    }
    
    private static void mergePipeParams(List<TaskDes> tasks, Set<PipeDes> inPipes, Set<PipeDes> outPipes) {
        for (TaskDes task : tasks) {
            inPipes.addAll(task.getInPipe());
            outPipes.addAll(task.getOutPipe());
        }
    }
    
    public static Map<String, JobDes> getJobs() {
        return jobs;
    }
}
