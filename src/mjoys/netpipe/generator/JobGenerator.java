package mjoys.netpipe.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import mjoys.netpipe.pipe.Job;
import mjoys.netpipe.pipe.Task;
import mjoys.netpipe.util.Cfg;
import mjoys.util.Logger;
import mjoys.util.PathUtil;
import mjoys.util.SystemUtil;

import org.apache.commons.io.FileUtils;

public class JobGenerator {
    private static Map<String, JobDes> jobs = new HashMap<String, JobDes>();
    private final static Logger logger = new Logger().addPrinter(System.out);
    
    public final static JobDes generate(String jobSrcRoot, Class<?> jobClass) {
        JobDes jobDes = getJobDes(jobClass);
        if (jobDes == null) {
        	return null;
        }
        
        // 生成数据流
        
        // 编译每个任务文件，打jar包
        try {
            createSourceAndJar(jobSrcRoot, jobDes);
        } catch (IOException e) {
            logger.log("create source code exception:", e);
        }

        jobs.put(jobDes.getJob().name(), jobDes);
        return jobDes;
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
                taskInfo.setJobInfo(jobInfo);
                taskInfo.setTask(task);
                taskInfo.setMethod(m);
                for (Type paramType : m.getGenericParameterTypes()) {
                    PipeDes pipeInfo = new PipeDes();
                    
                    if (paramType instanceof ParameterizedType) {
                        Type genericParamType = ((ParameterizedType) paramType).getActualTypeArguments()[0];
                        pipeInfo.elementType = genericParamType;
                    }
                    
                    if (paramType.toString().contains("mjoys.netpipe.pipe.InPipe")) {
                        taskInfo.getInPipe().add(pipeInfo);
                    } else if (paramType.toString().contains("mjoys.netpipe.pipe.OutPipe")) {
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
    
    private static void createSourceAndJar(String jobRoot, JobDes jobInfo) throws IOException {
        String jobPath = Cfg.instance.getJobPath();
        File jobDir = new File(jobPath, jobInfo.getJob().name());
        jobDir.mkdir();
        File sourceDir = new File(jobDir, "src");
        File jarDir = new File(jobDir, "jar");
        File classDir = new File(jobDir, "classes");
        File libDir = new File(jobDir, "libs");
        sourceDir.mkdir();
        jarDir.mkdir();
        classDir.mkdir();
        libDir.mkdir();
        
        File taskDir = new File(sourceDir, "task");
        taskDir.mkdir();
        
        // 复制job源代码文件
        File originJobSrcDir = new File(jobRoot, "src");
        FileUtils.copyDirectory(originJobSrcDir, sourceDir);
        
        // 复制lib
        FileUtils.copyDirectory(new File(jobRoot, "libs"), libDir);
        FileUtils.copyFile(new File(PathUtil.combine(Cfg.instance.getRoot(), "lib", "mjoys.jar")), new File(libDir, "mjoys.jar"));
        FileUtils.copyFile(new File(PathUtil.combine(Cfg.instance.getRoot(), "lib", "netpipe.jar")), new File(libDir, "netpipe.jar"));
        
        // 生成每个任务的源代码文件
        for (TaskDes task : jobInfo.getTasks()) {
            String taskJavaFile = TaskTemplate.getTaskClassSourceCode(jobInfo, task);
  
            File srcFile = new File(taskDir, task.name() + "Main.java");
            if (srcFile.exists()) {
            	srcFile.delete();
            }
            srcFile.createNewFile();
            FileWriter out = new FileWriter(srcFile);
            out.write(taskJavaFile);
            out.close();
        }
        
        // 编译
        JavaCompiler.compile(jobDir);

        String jarFile = PathUtil.combine(jarDir.getPath(), jobInfo.getJob().name() + ".jar");
        String jarCmd = String.format("jar cf %s -C %s %s -C %s .", jarFile, jobDir.getPath(), "libs", classDir.getPath());
        SystemUtil.run(jarCmd);
    }
    
    public static Map<String, JobDes> getJobs() {
        return jobs;
    }
}
