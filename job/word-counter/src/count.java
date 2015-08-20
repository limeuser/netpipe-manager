public class Runner extends TaskRunner {
    private InPipe<java.lang.String> words = new TcpInPipe<java.lang.String>("words");
    private OutPipe<netpipe.job.WordCounter.WordCount> wordCounts = new TcpOutPipe<netpipe.job.WordCounter.WordCount>("wordCounts");
    private Context ctx = new Context;
    private netpipe.job.WordCounter job = new netpipe.job.WordCounter();
    @Override
    public void init() {
        super.setJobName("word-counter");
        super.setTaskName("count");
        super.addInPipe(words);
        super.addOutPipe(wordCounts);
    }
    @Override
    public void runTask() {
        job.countCmd(words, wordCounts, ctx);
    }
    @Override
    public void runTask() {
        job.count(words, ctx);
    }
}