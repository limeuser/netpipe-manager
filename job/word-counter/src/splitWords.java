public class Runner extends TaskRunner {
    private InPipe<java.lang.String> lines = new TcpInPipe<java.lang.String>("lines");
    private OutPipe<java.lang.String> words = new TcpOutPipe<java.lang.String>("words");
    private Context ctx = new Context;
    private netpipe.job.WordCounter job = new netpipe.job.WordCounter();
    @Override
    public void init() {
        super.setJobName("word-counter");
        super.setTaskName("splitWords");
        super.addInPipe(lines);
        super.addOutPipe(words);
    }
    @Override
    public void runTask() {
        job.splitWords(lines, words);
    }
}