package nars.eval;

import jcog.data.list.FasterList;
import nars.Op;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.buffer.TermBuffer;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.map.ByteAnonMap;
import nars.term.util.transform.HeapTermTransform;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.term.atom.Bool.Null;

/**
 * discovers functors within the provided target, or the target itself.
 * transformation results should not be interned, that is why DirectTermTransform used here
 */
public class Evaluator extends HeapTermTransform {

    final Function<Atom, Functor> funcResolver;

    private final TermBuffer compoundBuilder = new TermBuffer(HeapTermBuilder.the, new ByteAnonMap());


    public Evaluator(Function<Atom, Functor> funcResolver) {
        this.funcResolver = funcResolver;
    }

    public Evaluator clone() {
        return new Evaluator(funcResolver);
    }


    /**
     * discover evaluable clauses in the provided term
     * the result will be a list of unique terms in topologically or at least heuristically sorted order
     */
    @Nullable FasterList<Term> clauses(Compound x, Evaluation e) {
        return !x.hasAny(Op.FuncBits) ? null : clauseFind(x);
    }

    @Nullable FasterList<Term> clauseFind(Compound x) {
        UnifiedSet<Term> clauses = new UnifiedSet(0);

        x.recurseTerms(s -> s instanceof Compound && s.hasAll(Op.FuncBits), X -> {
            if (Functor.isFunc(X)) {
//                if (clauses.contains(X))
//                    return true;

                compoundBuilder.clear(); //true, compoundBuilder.sub.termCount() >= 64 /* HACK */);
                compoundBuilder.volRemain = Integer.MAX_VALUE; //HACK
                TermBuffer y = compoundBuilder.append(X);
                final int[] functors = {0};
                y.updateMap(g -> {
                    if (g instanceof Functor)
                        functors[0]++;
                    else if (g instanceof Atom) {
                        Functor f = funcResolver.apply((Atom) g);
                        if (f != null) {
                            functors[0]++;
                            return f;
                        }
                    }
                    return g;
                });

                if (functors[0] > 0) {

                    Term yy = y.term();
                    if (yy.sub(1) instanceof Functor) {
                        clauses.add(yy);
                    }
                }

            }
            return true;
        }, null);

        switch (clauses.size()) {
            case 0: return null;
            case 1: return (FasterList<Term>) new FasterList(1).with(clauses.getOnly());
            default: return sortTopologically(clauses.toArray(Op.EmptyTermArray));
        }

    }

    private FasterList<Term> sortTopologically(Term[] a) {
        Arrays.sort(a, complexitySort);
        //HACK more work necessary
        return new FasterList(a);
    }

    private static final Comparator<? super Term> complexitySort = (a,b)->{
        int vol = Integer.compare(a.volume(), b.volume());
        if (vol!=0)
            return vol;

        int vars = Integer.compare(a.vars(), b.vars());
        if (vars!=0)
            return vars;

        return a.compareTo(b);
    };

    @Override
    public @Nullable Term applyAtomic(Atomic x) {
        if (x instanceof Functor)
            return x;

        if (x instanceof Atom) {
            Functor f = funcResolver.apply((Atom) x);
            if (f != null)
                return f;
        }

        return x;
    }


    public void eval(Predicate<Term> each, boolean includeTrues, boolean includeFalses, Term... queries) {

        assert (queries.length > 0);

        Evaluation e = new EvaluationTrueFalseFiltered(each, includeTrues, includeFalses);

        //iterating at the top level is effectively DFS; a BFS solution is also possible
        for (Term x : queries) {
            if (x instanceof Compound) //HACK
                e.evalTry((Compound)x, this, true);
        }

    }

    private static final class EvaluationTrueFalseFiltered extends Evaluation {
        private final boolean includeTrues, includeFalses;

        EvaluationTrueFalseFiltered(Predicate<Term> each, boolean includeTrues, boolean includeFalses) {
            super(each);
            this.includeTrues = includeTrues;
            this.includeFalses = includeFalses;
        }

        @Override
        protected Term boolTrue(Term x) {
            return includeTrues ? super.boolTrue(x) : Null;
        }

        @Override
        protected Term boolFalse(Term x) {
            return includeFalses ? super.boolFalse(x) : Null;
        }
    }

}
