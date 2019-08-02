package nars.concept.sensor;

import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.attention.AttnBranch;
import nars.table.BeliefTable;
import nars.term.Term;

import java.util.List;

public class ScalarSignal extends Signal {
    public final FloatSupplier source;
    private final FloatRange res;

    private final short[] cause;
    public final AttnBranch pri;

    public ScalarSignal(Term term, FloatSupplier signal, NAR n) {
        this(term, new short[] { n.newCause(term).id }, signal, n);
    }

    public ScalarSignal(Term term, short[] cause, FloatSupplier signal, NAR n) {
        this(term, cause, signal,
                beliefTable(term, n, true, true),
                beliefTable(term, n, false, false),
                n);
    }

    public ScalarSignal(Term term, short[] cause, FloatSupplier signal, BeliefTable beliefTable, BeliefTable goalTable, NAR n) {
        super(term, beliefTable, goalTable, n);

        this.source = signal;
        this.res = FloatRange.unit(n.freqResolution);

        this.cause = cause;

        this.pri = newAttn(term);
    }

    @Override
    public float pri() {
        return pri.pri();
    }

    protected AttnBranch newAttn(Term term) {
        return new AttnBranch(term, List.of(this));
    }

    @Override public float nextValue() {
        return source.asFloat();
    }

    @Override
    public final FloatRange resolution() {
        return res;
    }

    @Override
    public short[] cause() {
        return cause;
    }
}
