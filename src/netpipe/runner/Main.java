package netpipe.runner;

import cn.oasistech.job.WordCounter;
import netpipe.generator.JobGenerator;
import netpipe.util.Cfg;

public class Main {
    public static void main(String[] args) {
        JobGenerator.generate(Cfg.getJobPath(), WordCounter.class);
    }
}