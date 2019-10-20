package jcog.thing;

import jcog.Log;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.event.Off;
import jcog.event.RunThese;
import org.slf4j.Logger;

/** a part of a Thing, which also manages a collection of locally contained SubParts */
public abstract class Parts<T extends Thing<T, ?>> extends Part<T>  {

    public static final Logger logger = Log.logger(Parts.class);

    protected final ConcurrentFastIteratingHashSet<SubPart<T>> local =
        new ConcurrentFastIteratingHashSet<SubPart<T>>(SubPart.EmptyArray);


    /**
     * TODO rename 'nar' to 'id' to complete generic-ization
     * TODO weakref? volatile? */
    public T nar;

    /**
     * attached resources held until deletion (including while off)
     */
    @Deprecated
    protected final RunThese whenDeleted = new RunThese();

    //    /** register a future deactivation 'off' of an instance which has been switched 'on'. */
    protected final void on(Off... x) {
        for (var xx : x)
            whenDeleted.add(xx);
    }

    protected final void startLocal(T t) {
        this.local.forEachWith((c, T) -> c.startIn(T, this), t);
    }

    protected final void stopLocal(T t) {
        this.local.forEachWith((c, T) -> c.stopIn(T, this), t);
    }

    @SafeVarargs
    public final void addAll(SubPart<T>... local) {
        for (SubPart d : local)
            add(d);
    }

    protected final void finallyRun(Off component) {
//        if (component instanceof Parts)
//            finallyRun(component);
//        else
            whenDeleted.add(component);
    }

    @SafeVarargs
    public final void removeAll(SubPart<T>... dd) {
        for (SubPart d : dd)
            remove(d);
    }

    public final void add(SubPart<T> local) {
        //if (!isOff())
        if (isOn())
            throw new UnsupportedOperationException(this + " is not in OFF or OFFtoON state to add sub-part " + local);

        if (this.local.add(local)) {
            //..
        } else
            throw new UnsupportedOperationException("duplicate local: " + local);
    }

    public final void remove(SubPart<T> local) {
        if (this.local.remove(local)) {
            local.stopIn(nar, this);
            //local.stop(nar);
        } else
            throw new UnsupportedOperationException("unknown local: " + local + " in " + this);          //return false;
    }
}
