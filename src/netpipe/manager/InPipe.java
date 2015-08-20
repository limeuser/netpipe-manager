package netpipe.manager;

public class InPipe extends RunningPipe {
    private OutPipe peer;
    
    public OutPipe getPeer() {
        return peer;
    }

    public void setPeer(OutPipe peer) {
        this.peer = peer;
    }
}
