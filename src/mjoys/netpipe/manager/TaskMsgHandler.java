package mjoys.netpipe.manager;

import java.nio.ByteBuffer;
import java.util.Map;

import mjoys.agent.Agent;
import mjoys.agent.NotifyConnectionResponse;
import mjoys.agent.client.AgentAsynRpc;
import mjoys.agent.client.AgentRpcHandler;
import mjoys.agent.util.Tag;
import mjoys.frame.TLV;
import mjoys.frame.TV;
import mjoys.io.ByteBufferInputStream;
import mjoys.io.SerializerException;
import mjoys.netpipe.core.AgentTag;
import mjoys.netpipe.core.Service;
import mjoys.netpipe.msg.MsgType;
import mjoys.netpipe.pipe.TaskStatus;
import mjoys.util.Logger;
import mjoys.util.NumberUtil;

public class TaskMsgHandler implements AgentRpcHandler<ByteBuffer>{
    private Host host;
    private final static Logger logger = new Logger().addPrinter(System.out);
    
    public TaskMsgHandler(Host host) {
        this.host = host;
    }
    
    public void handle(AgentAsynRpc rpc, TLV<ByteBuffer> idFrame) {
		try {
			if (idFrame.tag == Agent.PublicService.Agent.id) {
			    processAgentMsg(rpc, idFrame);
			} else {
			    processTaskMsg(rpc, idFrame);
			}
		} catch(Exception e) {
			logger.log("serializer exception:", e);
		}
	}
	
	private void processAgentMsg(AgentAsynRpc rpc, TLV<ByteBuffer> idFrame) throws SerializerException {
		TV<ByteBuffer> response = Agent.parseMsgFrame(idFrame.body);
		if (response == null) {
            logger.log("response is null");
            return;
        }
		if (response.tag == Agent.MsgType.NotifyConnection.ordinal()) {
			NotifyConnectionResponse connectionResponse = rpc.getSerializer().decode(new ByteBufferInputStream(response.body), NotifyConnectionResponse.class);

            Map<String, String> tags = Tag.toMap(connectionResponse.getIdTag().getTags());
            
            String service = tags.get(Agent.PublicTag.servicename.name());
            if (service != null && service.equals(Service.dpipe_task.name())) {
                int taskId = NumberUtil.parseInt(tags.get(AgentTag.dpipe_id.name()));
                int agentId = idFrame.tag;
                host.runningTaskConnected(taskId, agentId);
                if (connectionResponse.getIdTag().getId() > 0) {
                	host.getServices().put(service, connectionResponse.getIdTag().getId());
                } else {
                	host.getServices().remove(service);
                }
            }
		 }
	}
	
	private void processTaskMsg(AgentAsynRpc rpc, TLV<ByteBuffer> idFrame) throws SerializerException {
		TV<ByteBuffer> response = Agent.parseMsgFrame(idFrame.body);
        if (response.tag == MsgType.ReportStatus.ordinal()) {
            TaskStatus status = (TaskStatus) rpc.getSerializer().decode(new ByteBufferInputStream(response.body), TaskStatus.class);
            host.updateRunningTaskStatus(status);
        }
	}
}
