package mjoys.netpipe.util;

import mjoys.util.Address;
import mjoys.util.NumberUtil;

public class NetPipeManagerCfg extends mjoys.util.Cfg {
    private NetPipeManagerCfg() {
        super("cfg", "base.cfg");
    }
    
    public final static NetPipeManagerCfg instance = new NetPipeManagerCfg();
    
    private enum Key {
    	master,
    	slaves,
    	agentprotocol,
    	agentport,
    	ftpprotocol,
    	ftpport,
    	pipeprotocol,
        jobpath,
        maxload,
    }
    
    public String getJobPath() {
        return getDefaultPropertyCfg().getProperty(Key.jobpath.name());
    }
    
    public String getMasterIp() {
    	return getDefaultPropertyCfg().getProperty(Key.master.name());
    }
    
    public Address getFtpAddress(String ip) {
    	Address.Protocol p = Address.parseProtocol(getDefaultPropertyCfg().getProperty(Key.ftpprotocol.name()));
    	Integer port = NumberUtil.parseInt(getDefaultPropertyCfg().getProperty(Key.ftpport.name()));
    	if (p == null || port == null) {
    		return null;
    	}
    	
    	return Address.newAddress(p, Address.getAddressWithPort(ip, port));
    }
    
    public Address getAgentAddress(String ip) {
    	Address.Protocol p = Address.parseProtocol(getDefaultPropertyCfg().getProperty(Key.agentprotocol.name()));
    	Integer port = NumberUtil.parseInt(getDefaultPropertyCfg().getProperty(Key.agentport.name()));
    	if (p == null || port == null) {
    		return null;
    	}
    	
    	return Address.newAddress(p, Address.getAddressWithPort(ip, port));
    }
    
    public String[] getSlaveIps() {
    	String line = getDefaultPropertyCfg().getProperty(Key.slaves.name());
    	String[] ips = line.split(",");
    	return ips;
    }
    
    public Address.Protocol getPipeProtocol() {
    	return Address.parseProtocol(getDefaultPropertyCfg().getProperty(Key.pipeprotocol.name()));
    }
    
    public int getDefaultMaxLoad() {
    	return NumberUtil.parseInt(getDefaultPropertyCfg().getProperty(Key.maxload.name()));
    }
}
