package nars.eval;

import jcog.version.VersionMap;
import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

final class FactualEvaluator extends Evaluator {

    private final Function<Term, Stream<Term>> factResolver;

    private final Set<Term> facts = new HashSet(0);


    protected FactualEvaluator(Function<Atom, Functor> resolver, Function<Term, Stream<Term>> facts) {
        super(resolver);
        this.factResolver = facts;
    }

    public FactualEvaluator clone() {
        return new FactualEvaluator(funcResolver, factResolver);
    }

    @Override
    protected void discover(Term x, Evaluation e) {

        if (x.hasAny(Op.VAR_QUERY)) {
            //TODO cache
            List<Predicate<VersionMap<Term, Term>>> l = factResolver.apply(x).
                    map(y -> {
                        facts.add(y);
                        return Evaluation.assign(x, y);
                    }).collect(toList());

            if (!l.isEmpty())
                e.isAny(l);

        }

        super.discover(x, e);

    }

}
