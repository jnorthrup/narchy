package nars.eval;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.version.VersionMap;
import nars.$;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.unify.UnifySubst;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nars.Op.CONJ;
import static nars.Op.IMPL;

final class FactualEvaluator extends Evaluator {

    private final Function<Term, Stream<Term>> factResolver;

    private final Set<Term> facts = new HashSet(0);

    /** head |- OR(tail1, tail2, ...) */
    private final SetMultimap<Term, Term> ifs = MultimapBuilder.hashKeys().linkedHashSetValues().build();


    protected FactualEvaluator(Function<Atom, Functor> resolver, Function<Term, Stream<Term>> facts) {
        super(resolver);
        this.factResolver = facts;
    }

    public FactualEvaluator clone() {
        return new FactualEvaluator(funcResolver, factResolver);
    }

    @Override
    protected void discover(Term x, Evaluation e) {

        {
            //TODO cache
            List<Predicate<VersionMap<Term, Term>>> l = factResolver.apply(x).
                    map(y -> {
                        //TODO neg, temporal
                        if (y.op()==IMPL) {
                            ifs.put(y.sub(1), y.sub(0));
                            return null;
                        } else {
                            facts.add(y);
                            return Evaluation.assign(x, y);
                        }
                    }).filter(Objects::nonNull).collect(toList());

            if (e!=null && !l.isEmpty())
                e.isAny(l);

        }

        super.discover(x, e);

    }

    /**
     * @return +1 true, 0 - unknown, -1 false
     */
    public int truth(Term x, Evaluation e) {
        if (x instanceof Bool)
            return 0;

        query(x, e);
        if (facts.contains(x))
            return +1;
        else if (facts.contains(x.neg()))
            return -1;
        else {
            UnifySubst u = new UnifySubst(null, new XoRoShiRo128PlusRandom(1) /* HACK */, (t)->{ return false; /* first is ok */ } );

            for (Term k : ifs.keys()) {
                if (k.unify(x, u)) {
                    ifs.get(k).forEach(v->{
                        Term vv = u.transform(v);
                        if (vv.op().conceptualizable)
                            facts.add($.func("ifThen", vv, k));
                    });
                }
            }

//
            if (x.op()==CONJ) {
                //evaluate
            }
            return 0;
        }
    }

}
