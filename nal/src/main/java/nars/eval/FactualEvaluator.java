package nars.eval;

import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;

import java.util.function.Function;
import java.util.stream.Stream;

final class FactualEvaluator extends Evaluator {
    private final Function<Term, Stream<Term>> facts;

    protected FactualEvaluator(Function<Atom, Functor> resolver, Function<Term, Stream<Term>> facts) {
        super(resolver);
        this.facts = facts;
    }

    public FactualEvaluator clone() {
        return new FactualEvaluator(resolver, facts);
    }

    @Override
    protected FactualEvaluator query(Term x) {
        super.query(x);

//        if (x.hasAny(Op.VAR_QUERY)) {
//            if (cache.add(x)) {
//                List<Term> l = facts.apply(x).collect(toList());
//                if (!l.isEmpty()) {
//
//                }
//            }
//        }

        return this;
    }

}
