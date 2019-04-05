package nars.control;

import nars.NAR;
import nars.term.Term;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** parent service node managing a set of child devices, which are disabled if this service is disabled.
 *  establishes a hierarchy of devices/peripherals/components
 **/
public class PartSet<X extends Part> extends Part {

    final Map<Term,X> devices = new ConcurrentHashMap<>();

    public PartSet(NAR nar) {
        super(nar);
    }

    public X get(Term t) {
        return devices.get(t);
    }

    public final void add(X x) {
        add(x, false);
    }
    
    public void add(X x, boolean enable) {
        X removed = devices.put(x.id, x);
        if (removed!=null && removed!=x) {
            if (!removed.isOff())
                nar.remove(removed);
        }

        if (enable) {
            nar.add(x);
        }
    }

    @Override
    protected void stopping(NAR nar) {
        synchronized (devices) {
            devices.values().forEach(nar::remove);
        }
    }

    

}
