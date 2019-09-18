package nars.game.sensor;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.game.Game;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** unisignal from float value source */
public class ScalarSignal extends UniSignal {
    public final FloatSupplier source;

    public ScalarSignal(Term term, FloatSupplier signal, NAR n) {
        this(term, null, signal, n);
    }

    public ScalarSignal(Term term, short[] cause, FloatSupplier signal, NAR n) {
        this(term, cause, signal,
                beliefTable(term, n, true, true),
                beliefTable(term, n, false, false),
                n);
    }

    protected ScalarSignal(Term term, BeliefTable beliefTable, BeliefTable goalTable,  NAR n) {
        this(term,  null, null, beliefTable, goalTable, n);
    }

    protected ScalarSignal(Term term, @Nullable short[] cause, @Nullable FloatSupplier signal, BeliefTable beliefTable, BeliefTable goalTable, NAR n) {
        super(term, cause, beliefTable, goalTable, n);


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
