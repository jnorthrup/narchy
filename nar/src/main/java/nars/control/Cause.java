package nars.control;

import jcog.Paper;
import jcog.Skill;
import jcog.Util;
import jcog.pri.ScalarValue;
import nars.$;
import nars.term.Term;
import nars.term.atom.Int;
import nars.time.event.WhenInternal;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * 'cause (because)
 * represents a registered causal influence for analyzing its
 * positive and negative influence in system activity via
 * 'causal traces' attached to Tasks.
 * <p>
 * multiple threads can safely affect the accumulators. it must be commited
 * periodically (by a single thread, ostensibly) to apply the accumulated values
 * and calculate the values
 * <p>
 * as reported by the value() function which represents the effective
 * positive/negative balance that has been accumulated. a decay function
 * applies forgetting, and this is applied at commit time by separate
 * positive and negative decay rates.  the value is clamped to a range
 * (ex: 0..+1) so it doesn't explode.
 * <p>
 * https://cogsci.indiana.edu/pub/parallel-terraced-scan.pdf
 */
@Paper
@Skill("Credit_assignment")
public class Cause extends WhenInternal implements Comparable<Cause>, Caused {

    /**
     * internally assigned id
     */
    public final short id;
    public final Term why;
    public final Term name;
    /**
     * the value measured contributed by its effect on each MetaGoal.
     * the index corresponds to the ordinal of MetaGoal enum entries.
     * these values are used in determining the scalar 'value' field on each update.
     * <p>
     * TODO allow redefinition at runtime
     */
    public final Credit[] credit;
    /**
     * current scalar utility estimate for this cause's support of the current MetaGoal's.
     * may be positive or negative, and is in relation to other cause's values
     */
    public volatile float value = 0;

    /** an effective priority value */
    public volatile float pri = 1f;

    protected Cause(short id) {
        this(id, null);
    }

    public Cause(short id, @Nullable Object name) {
        this.id = id;
        this.why = Int.the(id);
        this.name = $.identity(name != null ? name : this);
        credit = new Credit[MetaGoal.values().length];
        for (int i = 0; i < credit.length; i++) {
            credit[i] = new Credit();
        }
    }

    public float pri() {
        //return value;
        return pri;
    }
    public float valueRaw() {
        return value;
    }


    /** set NaN if no value actually accumulated during the last cycle */
    public void setValue(float value) {
        this.value = value;
    }

    /**
     * value may be in any range (not normalized); 0 is neutral
     */
    public void setPri(float p) {
        //assert(Float.isFinite(nextValue));
        this.pri = p;
    }
    /**
     * 0..+1
     */
    public float amp() {
        return Math.max(ScalarValue.EPSILON, gain() / 2f);
    }

    /**
     * 0..+2
     */
    private float gain() {
        return Util.tanhFast(pri()) + 1f;
    }


    @Override
    public Term term() {
        return name;
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

//    public void commit(RecycledSummaryStatistics[] valueSummary) {
//        for (int i = 0, purposeLength = credit.length; i < purposeLength; i++) {
//            Traffic p = credit[i];
//            p.commit();
//            valueSummary[i].accept(p.last);
//        }
//    }

    public void commit() {
        for (Credit aGoal : credit)
            aGoal.commit();
    }

    public void print(PrintStream out) {
        out.println(this + "\t" +
                IntStream.range(0, credit.length).mapToObj(x ->
                        MetaGoal.values()[x] + "=" + credit[x]
                ).collect(toList())
        );

    }


    @Override
    public final @Nullable Term why() {
        return why;
    }
}












