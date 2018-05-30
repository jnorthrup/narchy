package alice.tuprologx.runtime.tcp;

@SuppressWarnings("serial")
public class NetMsg implements java.io.Serializable {
    public String methodName;

    public NetMsg(){
    }

    public NetMsg(String name) {
        methodName=name;
    }
}

