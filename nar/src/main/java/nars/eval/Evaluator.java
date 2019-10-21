package nars.eval;

import jcog.data.set.ArrayHashSet;
import nars.Op;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.buffer.TermBuffer;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.transform.HeapTermTransform;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static nars.term.atom.IdempotentBool.Null;

/**
 * discovers functors within the provided target, or the target itself.
 * transformation results should not be interned, that is why DirectTermTransform used here
 */
public class Evaluator extends HeapTermTransform {

    final Function<Atom, Functor> funcResolver;

    private final TermBuffer compoundBuilder =
        new TermBuffer(HeapTermBuilder.the);


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
    @Nullable ArrayHashSet<Term> clauses(Compound x, Evaluation e) {
        return !x.hasAny(Op.FuncBits) ? null : clauseFind(x);
    }

    final @Nullable ArrayHashSet<Term> clauseFind(Compound x) {
        return clauseFind(x, new ArrayHashSet<>(0));
    }

    public @Nullable ArrayHashSet<Term> clauseFind(Compound x, ArrayHashSet<Term> clauses) {
        x.recurseTerms(new Predicate<Term>() {
            @Override
            public boolean test(Term s) {
                return s instanceof Compound && s.hasAll(Op.FuncBits);
            }
        }, new Predicate<Term>() {
            @Override
            public boolean test(Term X) {
                if (Functor.isFunc(X)) {
//                if (clauses.contains(X))
//                    return true;
                    compoundBuilder.clear(); //true, compoundBuilder.sub.termCount() >= 64 /* HACK */);

                    compoundBuilder.volRemain = Integer.MAX_VALUE; //HACK
                    TermBuffer y = compoundBuilder.append(X);
                    int[] functors = {0};
                    y.updateMap(new UnaryOperator<Term>() {
                        @Override
                        public Term apply(Term g) {
                            if (g instanceof Functor)
                                functors[0]++;
                            else if (g instanceof Atom) {
                                Functor h = funcResolver.apply((Atom) g);
                                if (h != null) {
                                    functors[0]++;
                                    return h;
                                }
                            }
                            return g;
                        }
                    });

                    if (functors[0] > 0) {

                        Term yy = y.term();
                        if (yy.sub(1) instanceof Functor) {
                            clauses.add(yy);
                        }
                    }

                }
                return true;
            }
        }, null);

        switch (clauses.size()) {
            case 0: return null;
            case 1: return clauses;
            default: return sortTopologically(clauses);
        }
    }

    private static ArrayHashSet<Term> sortTopologically(ArrayHashSet<Term> a) {
        a.list.sort(complexitySort);
        //HACK more work necessary
        return a;
    }

    private static final Comparator<? super Term> complexitySort = new Comparator<Term>() {
        @Override
        public int compare(Term a, Term b) {

            int vol = Integer.compare(a.volume(), b.volume());
            if (vol != 0)
                return vol;
            int vars = Integer.compare(a.vars(), b.vars());
            if (vars != 0)
                return vars;


            return a.compareTo(b);
        }
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
        private final boolean includeTrues;
        private final boolean includeFalses;

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

//    private static final class MyDirectTermBuffer extends TermBuffer {
//        @Override
//        protected Term newCompound(Op o, int dt, Term[] subterms) {
//            if (o == CONJ || o == IMPL) {
//                return super.newCompound(o, dt, subterms);
//            } else{
//                //direct term buffer:
//                Term y = HeapTermBuilder.the.newCompoundN(o, dt, subterms, null);
//                return y;
//            }
//        }
//    }
}
