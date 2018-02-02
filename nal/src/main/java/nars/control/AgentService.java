package nars.control;

import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;

public class AgentService extends DurService {

    private final AgentBuilder.WiredAgent wiredAgent;

    public AgentService(MutableFloat durations, NAR n, AgentBuilder.WiredAgent agent) {
        super(n, durations);
        this.wiredAgent = agent;
    }

    @Override
    public void run(NAR nar, long dt) {
        wiredAgent.next();
    }

}
