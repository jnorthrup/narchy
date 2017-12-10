package nars.op.java;

import nars.NAR;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class Driver<X> {

    final static org.slf4j.Logger logger = LoggerFactory.getLogger(Driver.class);
    protected final X x;
    protected final NAR n;

    public Driver(NAR n, Class<? extends X> c, Object... args) {
        this(new Opjects(n), c, args);
    }

    public Driver(Opjects objs, Class<? extends X> c, Object... args) {
        this.n = objs.nar;
        this.x = objs.a("a_" + c.getClass().getSimpleName(), c, args);
    }

    public Trick<X> teach(String taskName,
                      Consumer<X> pre,
                      Consumer<X> task,
                      Predicate<X> post /* validation*/) {

        Trick<X> t = new Trick<>(taskName, pre, task, post);
        t.train(x, n);

        boolean valid = t.valid(x);
        if (!valid)
            throw new RuntimeException("invalid after training. please dont confuse NARS");

        n.run(1000); //debriefing

        return t;
    }
}
