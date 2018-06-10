package nars.util.term.transform;

import jcog.memoize.QuickMemoize;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.util.TermList;
import nars.term.Term;
import nars.util.term.TermBuilder;
import nars.util.term.builder.HeapTermBuilder;
import org.eclipse.collections.api.tuple.Pair;

import static nars.time.Tense.DTERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/** bypasses interning and */
public interface DirectTermTransform extends TermTransform {

    TermBuilder localBuilder = new HeapTermBuilder();

    @Override
    default Term the(Op op, int dt, Term[] t) {
        return localBuilder.compound(op, dt, t);
    }

    @Override
    default Term the(Op op, int dt, Subterms t) {
        return localBuilder.theCompound(op, dt, t);
    }

    class CachedDirectTermTransform implements DirectTermTransform {
        /** stores constructed Anon's locally, thread-local */
        final QuickMemoize<Pair<Op,TermList>,Term> localIntern;


        public CachedDirectTermTransform(int capacity) {
            this.localIntern = new QuickMemoize<>(capacity, (ot) ->
                localBuilder.compound(ot.getOne(), DTERNAL,
                        ot.getTwo().arraySharedKeep()) 
            );
        }


        @Override
        public Term the(Op op, int dt, TermList t) {
            if (dt == DTERNAL)
                return localIntern.apply(pair(op, t)); 
            else
                return localBuilder.compound(op, dt, t.arrayShared()); 
        }
        @Override
        public Term the(Op op, int dt, Term[] t) {
            if (dt == DTERNAL)
                return localIntern.apply(pair(op, new TermList(t)));
            else
                return localBuilder.compound(op, dt, t);
        }

        public void resize(int s) {
            localIntern.resize(s);
        }
    }

}
