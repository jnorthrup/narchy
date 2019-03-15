package nars.task.signal;

import nars.NAR;
import nars.concept.Concept;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.XTERNAL;

/**
 * TODO implement Task directly avoiding redudant fields that this overrides
 */
public class TruthletTask extends SignalTask {

    private final Truthlet truthlet;

    public TruthletTask(Term t, byte punct, Truthlet truth, NAR n) {
        this(t, punct, truth, n.time(), n.time.nextStamp());
    }

    public TruthletTask(Term t, byte punct, Truthlet truth, long creation, long stamp) {
        super(t, punct, truth, creation, XTERNAL, XTERNAL, stamp);
        assert (punct == BELIEF || punct == GOAL);
        this.truthlet = truth;
    }


//    /**
//     * should be called only from the stretch procedure
//     */
//    private void updateTime(Concept c, long nextStart, long nextEnd) {
//        if (!(nextStart == start() && nextEnd == end()))
//            update(c, t-> t.truthlet = t.truthlet.stretch(nextStart, nextEnd));
//    }

    public void update(NAR n, Consumer<TruthletTask> t) {
        Concept c = n.concept(term(), true);
        if (c != null) {

            update(c, t);

        }
    }

    private void update(Concept c, Consumer<TruthletTask> t) {
        ((BeliefTables)c.table(punc())).tableFirst(TemporalBeliefTable.class).update(this, ()-> t.accept(TruthletTask.this));
    }


    @Override
    public long start() {
        return truthlet.start();
    }

    @Override
    public long end() {
        return truthlet.end();
    }



    @Override
    public boolean intersects(long start, long end) {
        return truthlet.intersects(start, end);
    }



























    @Deprecated public @Nullable Truth truth(long when) {
        float[] tl = truthlet.truth(when);
        float f = tl[0];
        if (f!=f)
            return null;
        float e = tl[1];
        if (e > 0)
            return PreciseTruth.byEvi(f, e /* evi */);
        else
            return null;
    }

    @Override
    public float evi() {
        return truthlet.evi();
    }

    @Override
    public float freq() {
        return truthlet.freq();
    }

    /** this is where truthlet would provide its custom EvidenceEvaluator */
    @Override @Deprecated public float evi(long when, int dur) {
        return truthlet.truth(when)[1];
    }

    @Override
    public float freq(long start, long end) {
        return truthlet.freq(start, end);
    }

//    public void updateEnd(Concept c, long nextEnd) {
//        updateTime(c, start(), nextEnd);
//    }

//    public void truth(Truthlet newTruthlet, boolean relink, NAR n) {
//        if (truthlet!=newTruthlet) {
//            Concept c = n.concept(target(), true);
//            if (c != null) {
//                update(c, (tt) -> {
//                    tt.truthlet = newTruthlet;
//                    if (relink) {
//                        Tasklinks.linkTask(this, pri(), c, n);
//                    }
//                });
//            }
//        }
//    }

}
