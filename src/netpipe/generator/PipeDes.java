package netpipe.generator;

import java.lang.reflect.Type;

public class PipeDes {
    public Type elementType;
    public String name;
    public PipeDes peer;
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof PipeDes)) {
            return false;
        }
        
        PipeDes p = (PipeDes) o;
        return p.elementType.equals(elementType) && name.equals(p.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
