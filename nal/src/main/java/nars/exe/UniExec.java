package nars.exe;

import jcog.Service;
import nars.NAR;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
 */
public class UniExec extends AbstractExec {

    int WORK_PER_CYCLE = 1;

    final Focus.DefaultRevaluator revaluator = new Focus.DefaultRevaluator();

    public UniExec(int capacity) {
        super(capacity);
    }

    @Override
    public void start(NAR n) {
        synchronized (this) {
            super.start(n);

            List<Causable> can = n.services().map(x -> x instanceof Causable ? ((Causable) x) : null).filter(Objects::nonNull).collect(Collectors.toList());
            n.services.change.on((xb) -> {
                Service<NAR> s = xb.getOne();
                if (s instanceof Causable) {
                    if (xb.getTwo())
                        can.add((Causable) s);
                    else
                        can.remove(s);
                }
            });

            n.onCycle(() -> {
                revaluator.update(n);
                can.forEach(c -> c.run(n, WORK_PER_CYCLE));
            });
        }
    }

    @Override
    public boolean concurrent() {
        return false;
    }
}
