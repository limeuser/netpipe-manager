package mjoys.netpipe.manager;

import java.nio.ByteBuffer;
import java.util.Map;

import mjoys.agent.Agent;
import mjoys.agent.NotifyConnectionResponse;
import mjoys.agent.NotifyConnectionResponse.Action;
import mjoys.agent.client.AgentAsynRpc;
import mjoys.agent.client.AgentRpcHandler;
import mjoys.agent.service.netpipe.msg.*;
import mjoys.agent.util.Tag;
import mjoys.frame.TLV;
import mjoys.frame.TV;
import mjoys.io.ByteBufferInputStream;
import mjoys.io.SerializerException;
import mjoys.netpipe.pipe.NetPipeCfg;
import mjoys.netpipe.pipe.TaskStatus;
import mjoys.util.Logger;
import mjoys.util.NumberUtil;

public class NetPipeMsgHandler implements AgentRpcHandler<ByteBuffer>{
    private Host host;
    private final static Logger logger = new Logger().addPrinter(System.out);
    
    public NetPipeMsgHandler(Host host) {
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
            
            int taskId = NumberUtil.parseInt(tags.get(NetPipeCfg.AgentTag.netpipe_taskid.name()));
            if (taskId > 0 && connectionResponse.getIdTag().getId() > 0) {
                int agentId = idFrame.tag;
                if (connectionResponse.getAction() == Action.connect) {
                	host.runningTaskConnected(taskId, agentId);
                	host.getTasks().put(taskId, connectionResponse.getIdTag().getId());
                } else if (connectionResponse.getAction() == Action.disconnect){
                	host.getTasks().remove(taskId);
                }
            }
		 }
	}
	
	private void processTaskMsg(AgentAsynRpc rpc, TLV<ByteBuffer> idFrame) throws SerializerException {
		TV<ByteBuffer> response = Agent.parseMsgFrame(idFrame.body);
        if (response.tag == MsgType.BindOutPipe.ordinal()) {
        	BindOutPipeResponse msg = rpc.getSerializer().decode(new ByteBufferInputStream(response.body), BindOutPipeResponse.class);

        	if (msg.getResult()) {
        		this.host.outPipeBound(msg.getTaskId(), msg.getOutPipeName(), msg.getOutPipeAddress());
        	} else {
        		logger.log("bind out pipe failed:%s %s", msg.getOutPipeName(), msg.getOutPipeAddress());
        	}
        } else if (response.tag == MsgType.GetTaskStatus.ordinal()) {
        	TaskStatus status = rpc.getSerializer().decode(new ByteBufferInputStream(response.body), TaskStatus.class);
            host.updateRunningTaskStatus(status);
        }
	}
}
