package nars.control;

import jcog.Util;
import jcog.math.RecycledSummaryStatistics;
import nars.task.util.TaskRegion;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

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
    private volatile float value = 0;

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
        return Math.max(Float.MIN_NORMAL, gain() / 2f);
    }

    /**
     * 0..+2
     */
    private float gain() {
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

    protected Cause(short id) {
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

    public static short[] merge(int causeCapacity, TaskRegion... e) {
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

                



                return merge(causeCapacity, a, b);
            default:
                return merge(causeCapacity,
                        Util.map(TaskRegion::cause, short[][]::new,
                            ArrayUtils.removeNulls(e, TaskRegion[]::new))); 
        }
    }




























    public static short[] merge(int maxLen, short[]... s) {

        int ss = s.length;
        assert(ss>0);
        if (ss == 1)
            return s[0];


        //quick test for equality
        boolean allEqual = true;
        for (int i = 1; i < ss; i++) {
            if (!(allEqual &= Arrays.equals(s[i-1], s[i])))
                break;
        }
        if (allEqual)
            return s[0];


        //return mergeFlat(maxLen, s);
        return mergeSampled(maxLen, s, false);
    }

    /** this isnt good because the maps can grow beyond the capacity
    public static short[] mergeFlat(int maxLen, short[][] s) {
        int ss = s.length;
        ShortHashSet x = new ShortHashSet(ss * maxLen);
        for (short[] a : s) {
            x.addAll(a);
        }
        return x.toSortedArray();
    }*/

    public static short[] mergeSampled(int maxLen, short[][] s, boolean deduplicate) {
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


        AwesomeShortArrayList ll = new AwesomeShortArrayList(totalItems);
        RoaringBitmap r = deduplicate ? new RoaringBitmap() : null;
        int ls = 0;
        int n = 0;
        int done;
        main:
        do {
            done = 0;
            for (short[] c : s) {
                int cl = c.length;
                if (n < cl) {
                    short next = c[cl - 1 - n];
                    if (deduplicate)
                        if (!r.checkedAdd(next))
                            continue;
                    if (ll.add/*adder.accept*/(next)) {
                        if (++ls >= maxLen)
                            break main;
                    }
                } else {
                    done++;
                }
            }
            n++;
        } while (done < ss);

        //assert (ls > 0);
        short[] lll = ll.toArray();
        //assert (lll.length == ls);
        return lll;
    }


    public void commit(RecycledSummaryStatistics[] valueSummary) {
        for (int i = 0, purposeLength = goal.length; i < purposeLength; i++) {
            Traffic p = goal[i];
            p.commit();
            valueSummary[i].accept(p.last);
        }
    }

    public void commit() {
        for (Traffic aGoal : goal)
            aGoal.commit();
    }

    public void commitFast() {
        for (Traffic aGoal : goal)
            aGoal.commitFast();
    }

    public void print(PrintStream out) {
        out.println(this + "\t" +
                IntStream.range(0, goal.length).mapToObj(x->
                    MetaGoal.values()[x] + "=" + goal[x]
                ).collect(toList())
        );

    }


    static final class AwesomeShortArrayList extends ShortArrayList {

        AwesomeShortArrayList(int cap) {
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












