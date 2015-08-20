package netpipe.manager;

import java.nio.ByteBuffer;
import java.util.Map;

import mjoys.util.Logger;
import mjoys.util.NumberUtil;
import mjoys.util.TLVFrame;
import netpipe.core.AgentTag;
import netpipe.core.Service;
import netpipe.msg.MsgType;
import netpipe.pipe.Config;
import netpipe.pipe.TaskStatus;
import cn.oasistech.agent.AgentProtocol;
import cn.oasistech.agent.IdFrame;
import cn.oasistech.agent.NotifyConnectionResponse;
import cn.oasistech.agent.Response;
import cn.oasistech.agent.client.AgentAsynRpc;
import cn.oasistech.agent.client.AgentRpcHandler;
import cn.oasistech.util.Tag;

public class TaskMsgHandler implements AgentRpcHandler {
    private Host host;
    private final static Logger logger = new Logger().addPrinter(System.out);
    
    public TaskMsgHandler(Host host) {
        this.host = host;
    }
    
	@Override
	public void handle(AgentAsynRpc rpc, IdFrame idFrame) {
		if (idFrame.getId() == AgentProtocol.PublicService.Agent.id) {
		    processAgentMsg(rpc, idFrame);
		} else {
		    processTaskMsg(rpc, idFrame);
		}
	}
	
	private void processAgentMsg(AgentAsynRpc rpc, IdFrame idFrame) {
	    Response response = rpc.getSerializer().decodeResponse(idFrame.getBody());
        if (response == null) {
            logger.log("response is null");
            return;
        } else if (!response.getError().equals(AgentProtocol.Error.Success)) {
            logger.log("return error: msg=%s, error=%s", response.getType().name(), response.getError().name());
            return;
        }
        
        // running task connect to agent
        if (response.getType().equals(AgentProtocol.MsgType.NotifyConnection.name())) {
            NotifyConnectionResponse connectionResponse = (NotifyConnectionResponse)response;
            Map<String, String> tags = Tag.toMap(connectionResponse.getIdTag().getTags());
            String service = tags.get(AgentProtocol.PublicTag.servicename.name());
            if (service != null && service.equals(Service.dpipe_task.name())) {
                int taskId = NumberUtil.parseInt(tags.get(AgentTag.dpipe_id.name()));
                int agentId = idFrame.getId();
                host.runningTaskConnected(taskId, agentId);
            }
        }
	}
	
	private void processTaskMsg(AgentAsynRpc rpc, IdFrame idFrame) {
	    TLVFrame frame = TLVFrame.parseTLV(ByteBuffer.wrap(idFrame.getBody(), 0, idFrame.getBodyLength()));
        if (frame.getType() == MsgType.ReportStatus.ordinal()) {
            TaskStatus status = (TaskStatus) Config.getSerializer().decode(frame.getValue());
            host.updateRunningTaskStatus(status);
        }
	}
}
