package nars.game.sensor;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.attention.PriNode;
import nars.game.Game;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** unisignal from float value source */
public class ScalarSignal extends UniSignal {
    public final FloatSupplier source;

    public ScalarSignal(Term term, FloatSupplier signal, NAR n) {
        this(term, signal, null, null, n);
    }

    public ScalarSignal(Term term, FloatSupplier signal, Term why, PriNode p, NAR n) {
        this(term, signal,
            beliefTable(term, n, true, true),
            beliefTable(term, n, false, false), why,
            p, n);
    }


    protected ScalarSignal(Term term, @Nullable FloatSupplier signal, BeliefTable beliefTable, BeliefTable goalTable, @Nullable Term why, PriNode p, NAR n) {
        super(term, why, beliefTable, goalTable, p, n);

        this.source = signal;
    }


    @Override
    public void update(Game g) {
        input(next(g), this.pri(), why(), g.nowWhat);
    }

    public final Truth next(Game g) {
        return truth(source.asFloat(), g);
    }

    protected Truth truth(float nextValue, Game g) {
        return nextValue == nextValue ?
            Signal.truthDithered(nextValue, Math.max(g.nar.freqResolution.floatValue(),
                resolution().floatValue()), g) :
            null;
    }

}
