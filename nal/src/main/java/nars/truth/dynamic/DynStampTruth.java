package nars.truth.dynamic;

import jcog.data.set.MetalLongSet;
import nars.Param;
import nars.Task;
import nars.truth.Stamp;

import java.util.Random;

/** dynTruth which can track evidential overlap while being constructed, and provide the summation of evidence after*/
public class DynStampTruth extends DynTruth {

//    static final ThreadLocal<DequeCleaningPool<MetalLongSet>> eis = DequeCleaningPool.threadLocal(() -> new MetalLongSet(4), MetalLongSet::clear);

    //final static Supplier<MetalLongSet> evis = () -> new MetalLongSet(4);

    private MetalLongSet evi = null;

    public DynStampTruth(int initialCap) {
        super(initialCap);
    }

    @Override
    public boolean add(Task newItem) {

        super.add(newItem);


        if (evi == null) {
            switch (size) {
                case 1: //dont create set now
//                case 2:
                    break;
                default: //more than 2:
                    long[] a = get(0).stamp(), b = get(1).stamp();
                    evi = Stamp.toSet(a.length + b.length, a, b);
                    break;
            }
        }

        if (evi!=null) {
            evi.addAll(newItem.stamp());
        }

        return true;
    }
    public boolean doesntOverlap(Task t) {
        return doesntOverlap(t.stamp());
    }

    public boolean doesntOverlap(long[] stamp) {
        MetalLongSet e = this.evi;
        if (e != null) {
            long[] s = stamp;
            for (long x : s) {
                if (e.contains(x))
                    return false;
            }
        } else if (size > 0) {
            //delay creation of evi set one more item
            assert(size == 1);
            return !Stamp.overlapsAny(get(0).stamp(), stamp);
        }

        return true;
    }


//    public final Task task(Term term, Truth t, boolean beliefOrGoal, NAR n) {
//        return task(term, t, beliefOrGoal, false, n);
//
//    }

//    public final Truth truth(Term term, BiFunction<DynTruth, NAR, Truth> o, boolean beliefOrGoal, NAR n) {
//        return (Truth) eval(term, o, false, beliefOrGoal, n);
//    }

//    /**
//     * TODO make Task truth dithering optional
//     */
//    @Deprecated  public Truthed eval(Term superterm, @Deprecated BiFunction<DynTruth, NAR, Truth> truthModel, boolean taskOrJustTruth, boolean beliefOrGoal, NAR nar) {
//
//        Truth t = truthModel.apply(this, nar);
//        if (t == null)
//            return null;
//
//        //return eval(()->superterm, t, taskOrJustTruth, beliefOrGoal, freqRes, confRes, eviMin, nar);
//        if (taskOrJustTruth) {
//            return task(superterm, t, this::stamp, beliefOrGoal, nar);
//        } else {
//            return t;
//        }
//
//    }



    public long[] stamp(Random rng) {
        if (evi == null) {

            switch(size) {
                case 1:
                    return get(0).stamp();
                case 2:
                    //lazy calculated stamp
                    long[] a = get(0).stamp(), b = get(1).stamp();
                    return Stamp.sample(Param.STAMP_CAPACITY, Stamp.toSet(a.length + b.length, a, b), rng);
                case 0:
                default:
                    throw new UnsupportedOperationException();
            }
        } else {
            return Stamp.sample(Param.STAMP_CAPACITY, this.evi, rng);
        }
    }


    @Override
    public void clear() {
        super.clear();
        if (evi!=null)
            evi.clear();
    }
}
