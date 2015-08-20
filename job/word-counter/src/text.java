public class Runner extends TaskRunner {
    private OutPipe<java.lang.String> lines = new TcpOutPipe<java.lang.String>("lines");
    private Context ctx = new Context;
    private netpipe.job.WordCounter job = new netpipe.job.WordCounter();
    @Override
    public void init() {
        super.setJobName("word-counter");
        super.setTaskName("text");
        
        super.addOutPipe(lines);
    }
    @Override
    public void runTask() {
        job.textSource(lines);
    }
}