package netpipe.util;

public class Cfg extends mjoys.util.Cfg {
    private Cfg() {
        super("cfg", "base.cfg");
    }
    
    private final static Cfg instance = new Cfg();
    
    private enum Key {
        jobpath,
    }
    
    public final static String getJobPath() {
        return instance.getDefaultPropertyCfg().getProperty(Key.jobpath.name());
    }
}
