package nars.eval;

import jcog.data.set.ArrayHashSet;
import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.transform.DirectTermTransform;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

import static nars.term.atom.Bool.Null;

/**
 * discovers functors within the provided target, or the target itself.
 * transformation results should not be interned, that is why DirectTermTransform used here
 */
public class Evaluator extends DirectTermTransform {

    public final Function<Atom, Functor> funcResolver;

    public Evaluator(Function<Atom, Functor> funcResolver) {
        this.funcResolver = funcResolver;
    }

    public Evaluator clone() {
        return new Evaluator(funcResolver);
    }


    @Nullable
    protected ArrayHashSet<Term> discover(Term x, Evaluation e) {
        if (!x.hasAny(Op.FuncBits))
            return null;

//        if (funcAble!=null)
//            funcAble.clear();
        final ArrayHashSet<Term>[] funcAble = new ArrayHashSet[]{null};

        x.recurseTerms(s -> s.hasAll(Op.FuncBits), xx -> {
            if (Functor.isFunc(xx)) {
                if (funcAble[0] != null && funcAble[0].contains(xx))
                    return true;

                Term yy = this.transform(xx);
                if (yy.sub(1) instanceof Functor) {
                    if (funcAble[0] == null)
                        funcAble[0] = new ArrayHashSet<>(1);

                    funcAble[0].add(yy);
                }
            }
            return true;
        }, null);

        return funcAble[0];
    }


//    @Override
//    protected void addUnique(Term x) {
//        super.addUnique(x);
//
////            x.sub(0).recurseTerms((Termlike::hasVars), (s -> {
////                if (s instanceof Variable)
////                    vars.addAt((Variable) s);
////                return true;
////            }), null);
//    }

    @Override
    public @Nullable Term transformAtomic(Atomic x) {
        if (x instanceof Functor) {
            return x;
        }

        if (x instanceof Atom) {
            Functor f = funcResolver.apply((Atom) x);
            if (f != null) {
                return f;
            }
        }
        return x;
    }


//    private Evaluator sortDecreasingVolume() {
//        //TODO only invoke this if the items changed
//        if (size() > 1)
//            ((FasterList<Term>) list).sortThisByInt(Termlike::volume);
//        return this;
//    }

    @Nullable
    public Evaluation eval(Predicate<Term> each, boolean includeTrues, boolean includeFalses, Term... queries) {
        Evaluation e = new Evaluation(each) {
            @Override
            protected Term boolTrue(Term x) {
                return includeTrues ? super.boolTrue(x) : Null;
            }

            @Override
            protected Term boolFalse(Term x) {
                return includeFalses ? super.boolFalse(x) : Null;
            }
        };

        //iterating at the top level is effectively DFS; a BFS solution is also possible
        for (Term x : queries) {
            e.evalTry(this, x);
        }
        return e;
    }

    public void print() {


    }
}
