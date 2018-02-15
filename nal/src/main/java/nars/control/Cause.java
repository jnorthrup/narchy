package nars.control;

import jcog.Util;
import jcog.math.RecycledSummaryStatistics;
import nars.task.util.TaskRegion;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.jetbrains.annotations.Nullable;

/**
 * represents a causal influence and tracks its
 * positive and negative gain (separately).  this is thread safe
 * so multiple threads can safely affect the accumulators. it must be commited
 * periodically (by a single thread, ostensibly) to apply the accumulated values
 * and calculate the values
 * as reported by the value() function which represents the effective
 * positive/negative balance that has been accumulated. a decay function
 * applies forgetting, and this is applied at commit time by separate
 * positive and negative decay rates.  the value is clamped to a range
 * (ex: 0..+1) so it doesn't explode.
 */
public class Cause implements Comparable<Cause> {

    /**
     * current scalar utility estimate for this cause's support of the current MetaGoal's.
     * may be positive or negative, and is in relation to other cause's values
     */
    private float value = 0;

    /**
     * the value measured contributed by its effect on each MetaGoal.
     * the index corresponds to the ordinal of MetaGoal enum entries.
     * these values are used in determining the scalar 'value' field on each update.
     */
    public final Traffic[] goal;


    public float value() {
        return value;
    }

    /**
     * 0..+1
     */
    public float amp() {
        return gain() / 2f;
    }

    /**
     * 0..+2
     */
    public float gain() {
        return Util.tanhFast(value) + 1f;
    }

    /**
     * value may be in any range (not normalized); 0 is neutral
     */
    public void setValue(float nextValue) {
        value = nextValue;
    }


    /**
     * internally assigned id
     */
    public final short id;

    public final Object name;

    public Cause(short id) {
        this(id, null);
    }

    public Cause(short id, @Nullable Object name) {
        this.id = id;
        this.name = name != null ? name : id;
        goal = new Traffic[MetaGoal.values().length];
        for (int i = 0; i < goal.length; i++) {
            goal[i] = new Traffic();
        }
    }

    @Override
    public String toString() {
        return name + "[" + id + "]=" + super.toString();
    }

    @Override
    public int hashCode() {
        return Short.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || id == ((Cause) obj).id;
    }

    @Override
    public int compareTo(Cause o) {
        return Short.compare(id, o.id);
    }

    public static short[] sample(int causeCapacity, @Nullable TaskRegion... e) {
        short[] a = e[0].cause();
        switch (e.length) {
            case 0:
                throw new NullPointerException();
            case 1:
                return a;

            case 2:
                short[] b = e[1].cause();

                if (a.length == 0)
                    return b;

                if (b.length == 0)
                    return a;

                //allow multiples because then they will reinforce the undiluted combined value if distinct vaclues are added later
//                if (Util.equals(a,b))
//                    return a;

                return sample(causeCapacity, a, b);
            default:
                return sample(causeCapacity, Util.map(TaskRegion::cause, short[][]::new, e)); //HACK
        }
    }

//    public static short[] append(int maxLen, short[] src, short[] add) {
//        int addLen = add.length;
//        if (addLen == 0) return src;
//
//        int srcLen = src.length;
//        if (srcLen + addLen < maxLen) {
//            return ArrayUtils.addAll(src, add);
//        } else {
//            if (addLen >= srcLen) {
//                return zip(maxLen, ()->src, ()->add);
//            } else {
//                short[] dst = new short[maxLen];
//                int mid = maxLen - addLen;
//                System.arraycopy(src, srcLen - mid, dst, 0, mid);
//                System.arraycopy(add, 0, dst, mid, addLen);
//                return dst;
//            }
//        }
//    }

//    public static short[] zip(int maxLen, Supplier<short[]>[] s) {
//        if (s.length == 1) {
//            return s[0].get();
//        }
//        return zip(maxLen, Util.map(Supplier::get, short[][]::new, s));
//    }

    public static short[] sample(int maxLen, short[]... s) {

        int ss = s.length;

        int totalItems = 0;
        short[] lastNonEmpty = null;
        int nonEmpties = 0;
        for (short[] t : s) {
            int tl = t.length;
            totalItems += tl;
            if (tl > 0) {
                lastNonEmpty = t;
                nonEmpties++;
            }
        }
        if (nonEmpties == 1)
            return lastNonEmpty;
        if (totalItems == 0)
            return ArrayUtils.EMPTY_SHORT_ARRAY;

        //boolean enough = (totalItems < maxLen);
        ShortIterable l;
        ShortPredicate adder;
        AwesomeShortArrayList ll = new AwesomeShortArrayList(totalItems);

        int ls = 0;
        int n = 0;
        int done;
        main:
        do {
            done = 0;
            for (int i = 0; i < ss; i++) {
                short[] c = s[i];
                int cl = c.length;
                if (n < cl) {
                    if (ll.add/*adder.accept*/(c[cl - 1 - n])) {
                        if (++ls >= maxLen)
                            break main;
                    }
                } else {
                    done++;
                }
            }
            n++;
        } while (done < ss);

        assert (ls > 0);
        short[] lll = ll.toArray();
        assert (lll.length == ls);
        return lll;
    }

    /**
     * learn the utility of this cause with regard to a goal.
     */
    public final void learn(MetaGoal p, float v) {
        MetaGoal.learn(goal, p.ordinal(), v);
    }


    public void commit(RecycledSummaryStatistics[] valueSummary) {
        for (int i = 0, purposeLength = goal.length; i < purposeLength; i++) {
            Traffic p = goal[i];
            p.commit();
            valueSummary[i].accept(p.current);
        }
    }


    static final class AwesomeShortArrayList extends ShortArrayList {

        public AwesomeShortArrayList(int cap) {
            super(cap);
        }

        @Override
        public short[] toArray() {
            if (this.size() == items.length)
                return items;
            else
                return super.toArray();
        }

    }

}
//    /** calculate the value scalar  from the distinctly tracked positive and negative values;
//     * any function could be used here. for example:
//     *      simplest:           pos - neg
//     *      linear combination: x * pos - y * neg
//     *      quadratic:          pos*pos - neg*neg
//     *
//     * pos and neg will always be positive.
//     * */
//    public float value(float pos, float neg) {
//        return pos - neg;
//        //return pos * 2 - neg;
//        //return Util.tanhFast( pos ) - Util.tanhFast( neg );
//    }