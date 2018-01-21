package nars.term.transform;

import jcog.memoize.QuickMemoize;
import nars.Op;
import nars.The;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.CompoundDTLight;
import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;

import static nars.time.Tense.DTERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

public interface DirectTermTransform extends TermTransform {

    @Override
    default Term the(Op op, int dt, TermList t) {
        return the(dt, direct(op, t));
    }

    static Term the(int dt, Term x) {
        if (dt!=DTERNAL && x.op().temporal) {
            return new CompoundDTLight((Compound)x, dt);
        } else {
            return x;
        }
    }

    public static Term direct(Op op, TermList t) {
        return The.rawCompoundBuilder.apply(op, t.arraySharedSafe());
    }

    public static class CachedDirectTermTransform implements DirectTermTransform {
        /** stores constructed Anon's */
        final QuickMemoize<Pair<Op,TermList>,Term> built;

        public CachedDirectTermTransform(int capacity) {
            this.built = new QuickMemoize<>(capacity, (ot) ->
                    DirectTermTransform.direct(ot.getOne(), ot.getTwo()));
        }


        @Override
        public Term the(Op op, int dt, TermList t) {
            return DirectTermTransform.the(dt, built.apply(pair(op, t)));
        }

        public void resize(int s) {
            built.resize(s);
        }
    }

}
