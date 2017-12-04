package nars.exe;

import jcog.Services;
import nars.NAR;
import nars.control.Causable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of being dummy
 */
public class UniExec extends AbstractExec {

    int WORK_PER_CYCLE = 2;

    public UniExec(int capacity) {
        super(capacity);
    }

    @Override
    public synchronized void start(NAR n) {
        super.start(n);


        List<Causable> can = n.services().map(x -> x instanceof Causable ? ((Causable) x) : null).filter(Objects::nonNull).collect(Collectors.toList());
        n.serviceAddOrRemove.on((xb) -> {
            Services.Service<NAR> s = xb.getOne();
            if (s instanceof Causable) {
                if (xb.getTwo())
                    can.add((Causable) s);
                else
                    can.remove(s);
            }
        });

        n.onCycle(() -> {
            for (int i = 0, canSize = can.size(); i < canSize; i++) {
                can.get(i).run(n, WORK_PER_CYCLE);
            }
        });
    }

}
