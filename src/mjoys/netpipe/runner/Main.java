package mjoys.netpipe.runner;

import mjoys.netpipe.util.NetPipeManagerCfg;
import mjoys.netpipe.generator.JobDes;
import mjoys.netpipe.generator.JobGenerator;
import mjoys.netpipe.manager.Cluster;
import mjoys.util.ClassUtil;

public class Main {
    public static void main(String[] args) {
    	Cluster cluster = new Cluster();
    	cluster.start();
    	
    	JobDes jobDes = JobGenerator.generate("F:\\git\\netpipe-jobs", ClassUtil.newInstance("mjoys.netpipe.job.WordCounter").getClass());
    	if (jobDes != null) {
    		cluster.scheduleJob(jobDes);
    	}
    }
}