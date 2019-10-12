package nars.task;

import jcog.WTF;
import nars.NAL;
import nars.Task;
import nars.term.Neg;
import nars.term.Term;
import nars.time.When;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * generic immutable Task implementation,
 * with mutable cause[] and initially empty meta table
 */
public abstract class NALTask extends AbstractTask {



    @Deprecated
    public final Task next(Object w) {
        //return Perceive.perceive(this, w);
        //return null;
        throw new WTF();
    }

    protected final Term term;
    private final Truth truth;
    protected final byte punc;
    protected final int hash;
    public long creation;

    private volatile boolean cyclic;

    public static AbstractTask the(Term c, byte punct, Truth tr, When<? extends NAL> when) {
        return the(c, punct, tr, when.x.time.now(), when.start, when.end, new long[]{when.x.time.nextStamp()});
    }

    public static AbstractTask the(Term c, byte punct, Truth tr, When<? extends NAL> when, long[] evidence) {
        return the(c, punct, tr, when.x.time.now(), when.start, when.end, evidence);
    }

    public static NALTask the(Term c, byte punct, Truth tr, long creation, long start, long end, long[] evidence) {

        if (c instanceof Neg) {
            if (tr != null)
                tr = tr.neg();

            c = c.unneg();
        }

        if (start == ETERNAL) {
            return new EternalTask(c, punct, tr, creation, evidence);
        } else {
            return new TemporalTask(c, punct, tr, creation, start, end, evidence);
        }
    }

    protected NALTask(Term term, byte punc, @Nullable Truth truth, long start, long end, long creation, long[] stamp) {
        super();

        this.term = term;
        this.truth = truth;
        this.punc = punc;
        this.creation = creation;
        this.hash = hashCalculate(start, end, stamp); //must be last
    }

    protected int hashCalculate(long start, long end, long[] stamp) {
        return Task.hash(
                term,
                truth,
                punc,
                start, end, stamp);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object that) {
        return Task.equal(this, that);
    }

    @Override
    public boolean isCyclic() {
        return cyclic;
    }

    @Override
    public void setCyclic(boolean c) {
        this.cyclic = c;
    }



    @Nullable
    @Override
    public final Truth truth() {
        return truth;
    }

    @Override
    public byte punc() {
        return punc;
    }

    @Override
    public long creation() {
        return creation;
    }

    @Override
    public Term term() {
        return term;
    }

    @Override
    public void setCreation(long nextCreation) {
        creation = nextCreation;
    }


    @Override
    public String toString() {
        return appendTo(null).toString();
    }

}
