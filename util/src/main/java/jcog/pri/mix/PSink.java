package jcog.pri.mix;

import jcog.math.FloatRange;
import jcog.pri.Prioritized;
import jcog.pri.Priority;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/** a sink channel (ie. target/destination) for streams of Priority instances,
 *      with mix controls. safe for multiple writers, as long as the target
 *      consumer also is. */
public class PSink<X extends Priority, Y extends Priority> extends FloatRange implements Function<X, Y>, Consumer<X> {

    public final Object id;
    private final Consumer<Y> target;

    float minThresh = Prioritized.EPSILON;

    public PSink(Object id, Consumer<Y> target) {
        super(1f, 0f, 2f);
        this.id = id;
        this.target = target;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    public Stream<Y> apply(Stream<X> x) {
        return x.map(this);
    }

    @Override public Y apply(X x) {
        return (Y)x;
    }

//    public Y[] apply(X[] x) {
//        for (X xx : x)
//            accept(xx);
//        return x;
//    }


    public final void input(Iterable<? extends X> xx) {
        xx.forEach( this );
    }

    public final void input(Stream<X> x) {
        x.forEach( this );
    }

    public final void input(X[] x) {
        for (X p : x)
            input(p);
    }

    public final void input(X x) {
        if (x == null)
            return; //HACK
        target.accept(apply(x));
    }



    @Override
    public void accept(X x) {
        if (x == null)
            return;

        float p = x.pri();
        if (p!=p)
            return;


        float g = floatValue();
        if (p >= 0 && g >= 0) {
            float pg = p * g;
            if (pg >= minThresh) {
                x.priSet(p * g);
                Y y = apply(x);
                target.accept(y);
//                in.accept(p);
//                out.accept(p * g);
            }
        }

    }

    /** reset gathered statistics */
    public void clear() {
//        in.clear();
//        out.clear();
        //quaMeter.clear();
    }
}
