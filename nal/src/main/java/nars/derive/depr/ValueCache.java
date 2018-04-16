//package nars.derive;
//
//import jcog.Util;
//import nars.control.Cause;
//import nars.time.Tense;
//import org.eclipse.collections.api.block.predicate.primitive.IntFloatPredicate;
//import org.roaringbitmap.IntIterator;
//
//import java.util.concurrent.atomic.AtomicLong;
//
//public class ValueCache  {
//
//    private final Cause[] src;
//
//    public final static float epsilon = 0.01f;
//    /**
//     * cache of value values
//     */
//    public final float[] value;
//    public final AtomicLong now;
////    private final ImmutableShortShortMap cidToLocal;
//
//    public ValueCache(Cause[] src) {
//        this.src = src;
//        this.now = new AtomicLong(Tense.ETERNAL);
//        this.value = new float[src.length];
//
//    }
//
//    public void update() {
//        int i = 0;
//        for (Cause c : src) {
//            value[i++] = c.value();
//        }
//    }
//
//    public void update(long now) {
//        if (this.now.getAndSet(now) != now) {
//            update();
//        }
//    }
//
//    public void get(IntIterator ii, IntFloatPredicate each) {
//        int k = 0;
//        while (ii.hasNext()) {
//            int n = ii.next();
//            if (!each.accept(n, value[n]))
//                break;
//        }
//    }
//    public float[] minmax(IntIterator i) {
//        float[] minmax = new float[2];
//        minmax[0] = Float.POSITIVE_INFINITY;
//        minmax[1] = Float.NEGATIVE_INFINITY;
//        while (i.hasNext()) {
//            float v = value[i.next()];
//            if (v < minmax[0]) minmax[0] = v;
//            if (v > minmax[1]) minmax[1] = v;
//        }
//        return minmax;
//    }
//
//    public void getNormalized(float min, float max, IntIterator b, int levels, IntFloatPredicate each) {
//
//        float range = max - min;
////        if (Util.equals(Math.abs(max-min), 0, Pri.EPSILON)) {
////            get(b, (c, v) -> each.accept(c, levels/2)); //flat
////        } else {
//            get(b, (c, v) -> each.accept(c,
//                Util.bin((v - min) / range, levels)
//            ));
////        }
//    }
//
//    public float valueSum() {
//        return Util.sum(value);
//    }
//
//
//}