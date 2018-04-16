package nars.util.term.transform;

import jcog.memoize.QuickMemoize;
import nars.Op;
import nars.subterm.util.TermList;
import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;

import static nars.util.time.Tense.DTERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

public interface DirectTermTransform extends TermTransform {

    @Override
    default Term the(Op op, int dt, TermList t) {
        return Op.instance(op, dt, t.arrayShared());
    }

    class CachedDirectTermTransform implements DirectTermTransform {
        /** stores constructed Anon's */
        final QuickMemoize<Pair<Op,TermList>,Term> built;

        public CachedDirectTermTransform(int capacity) {
            this.built = new QuickMemoize<>(capacity, (ot) ->
                    Op.instance(ot.getOne(), ot.getTwo().arraySharedSafe()) //dont erase the items in the TermList
            );
        }


        @Override
        public Term the(Op op, int dt, TermList t) {
            if (dt == DTERNAL)
                return built.apply(pair(op, t));
            else
                return DirectTermTransform.super.the(op, dt, t);
        }

        public void resize(int s) {
            built.resize(s);
        }
    }

}
