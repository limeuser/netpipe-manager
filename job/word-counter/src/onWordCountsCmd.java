public class Runner extends TaskRunner {
    private InPipe<netpipe.job.WordCounter.WordCount> wordCounts = new TcpInPipe<netpipe.job.WordCounter.WordCount>("wordCounts");
    private Context ctx = new Context;
    private netpipe.job.WordCounter job = new netpipe.job.WordCounter();
    @Override
    public void init() {
        super.setJobName("word-counter");
        super.setTaskName("onWordCountsCmd");
        super.addInPipe(wordCounts);
        
    }
    @Override
    public void runTask() {
        job.onWordCountsCmd(wordCounts, ctx);
    }
}