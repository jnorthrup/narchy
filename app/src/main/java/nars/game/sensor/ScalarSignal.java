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

    public ScalarSignal(Term term, FloatSupplier signal, short[] cause, PriNode p, NAR n) {
        this(term, signal,
            beliefTable(term, n, true, true),
            beliefTable(term, n, false, false), cause,
            p, n);
    }


    protected ScalarSignal(Term term, @Nullable FloatSupplier signal, BeliefTable beliefTable, BeliefTable goalTable, @Nullable short[] cause, PriNode p, NAR n) {
        super(term, cause, beliefTable, goalTable, p, n);

        this.source = signal;
    }


    @Override
    public void update(Game g) {
        input(next(g), this.pri(), cause(), g.nowWhat);
    }

    public final Truth next(Game g) {
        return truth(source.asFloat(), g);
    }

}
