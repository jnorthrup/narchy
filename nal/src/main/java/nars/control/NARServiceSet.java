package nars.control;

import nars.NAR;
import nars.term.Term;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** parent service node managing a set of child devices, which are disabled if this service is disabled.
 *  establishes a hierarchy of devices/peripherals/components
 **/
public class NARServiceSet extends NARService {

    final Map<Term,NARService> devices = new ConcurrentHashMap<>();

    private final NAR nar;

    public NARServiceSet(NAR nar) {
        super(nar);
        this.nar = nar;
    }

    public final void add(NARService x) {
        add(x, false);
    }
    
    public void add(NARService x, boolean enable) {
        NARService removed = devices.put(x.id, x);
        if (removed!=null && removed!=x) {
            if (!removed.isOff())
                nar.off(removed);
        }

        if (enable) {
            nar.on(x);
        }
    }

    @Override
    protected void stopping(NAR nar) {
        synchronized (devices) {
            devices.values().forEach(nar::off);
        }
    }

    //TODO public void remove(...)

}
