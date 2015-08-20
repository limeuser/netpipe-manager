package netpipe.manager;

import java.util.ArrayList;
import java.util.List;

public class OutPipe extends RunningPipe {
    private List<InPipe> peers = new ArrayList<InPipe>();

    public List<InPipe> getPeers() {
        return peers;
    }

    public void setPeers(List<InPipe> peers) {
        this.peers = peers;
    }
}
