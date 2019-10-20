package nars.op.java;

import nars.$;
import nars.NAR;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class Teacher<X> {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(Teacher.class);
    protected final X x;
    protected final NAR n;

    public Teacher(NAR n, X c) {
        this(new Opjects(n), c);
    }

    public Teacher(NAR n, Class<? extends X> c) {
        this(new Opjects(n), c);
    }

    public Teacher(Opjects objs, X instance) {
        this.n = objs.nar;
        this.x = objs.the("a_" + instance.getClass().getSimpleName(), instance);
    }

    public Teacher(Opjects objs, Class<? extends X> clazz) {
        this.n = objs.nar;
        this.x = objs.a($.the("a_" + clazz.getSimpleName()), clazz);
    }

    public Trick<X> teach(String taskName,
                      Consumer<X> pre,
                      Consumer<X> task,
                      Predicate<X> post /* validation*/) {

        var t = new Trick<X>(taskName, pre, task, post);
        t.train(x, n);

        var valid = t.valid(x);
        if (!valid)
            throw new RuntimeException("invalid after training. please dont confuse NARS");

        return t;
    }
}
