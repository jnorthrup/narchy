package nars.eval;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.transform.DirectTermTransform;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.ATOM;

/**
 * discovers functors within the provided term, or the term itself.
 * transformation results should not be interned, that is why DirectTermTransform used here
 */
class Evaluator extends ArrayHashSet<Term> implements DirectTermTransform {

    public final Function<Atom, Functor> resolver;

    /** TODO expand this to fully featured tabling/memoization facility */
    protected final Set<Term> cache = new HashSet();

//        public final MutableSet<Variable> vars = new UnifiedSet(0);

    protected Evaluator(Function<Atom, Functor> resolver) {
        this.resolver = resolver;
    }

    public Evaluator clone() {
        return new Evaluator(resolver);
    }


    protected Evaluator query(Term x) {
        if (cache.add(x)) {
            x.recurseTerms(s -> s.hasAll(Op.FuncBits), xx -> {
                if (!contains(xx)) {
                    if (Functor.isFunc(xx)) {
                        Term yy = this.transform(xx);
                        if (yy.sub(1) instanceof Functor) {
                            if (add(yy)) {
                                ///changed = true;
                            }
                        }
                    }
                }
                return true;
            }, null);
        }
        return this;
    }


    @Override
    protected void addUnique(Term x) {
        super.addUnique(x);

//            x.sub(0).recurseTerms((Termlike::hasVars), (s -> {
//                if (s instanceof Variable)
//                    vars.add((Variable) s);
//                return true;
//            }), null);
    }

    @Override
    public @Nullable Term transformAtomic(Atomic x) {
        if (x instanceof Functor) {
            return x;
        }

        if (x.op() == ATOM) {
            Functor f = resolver.apply((Atom) x);
            if (f != null) {
                return f;
            }
        }
        return x;
    }

    @Override
    public ListIterator<Term> listIterator() {
        //sortDecreasingVolume();
        return super.listIterator();
    }

    private Evaluator sortDecreasingVolume() {
        //TODO only invoke this if the items changed
        if (size() > 1)
            ((FasterList<Term>) list).sortThisByInt(Termlike::volume);
        return this;
    }

    @Nullable public Evaluation eval(Predicate<Term> each, Term... queries) {
        Evaluation e = new Evaluation(this, each);

        //iterating at the top level is effectively DFS; a BFS solution is also possible
        for (Term x : queries) {
            query(x);
            e.eval(this, x);
        }
        return e;
    }

}