package nars.derive.util;

import nars.Builtin;
import nars.NAL;
import nars.NAR;
import nars.derive.Derivation;
import nars.op.Cmp;
import nars.op.Equal;
import nars.op.SetFunc;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.functor.AbstractInlineFunctor2;
import nars.term.util.conj.ConjMatch;
import nars.term.util.transform.InlineFunctor;
import nars.util.var.DepIndepVarIntroduction;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;
import java.util.function.Function;

import static nars.term.atom.Bool.Null;

public enum DerivationFunctors {
    ;

    public static Function<Atomic, Term> get(Derivation d) {

        Map<Atomic, Term> m = new UnifiedMap<>(32, 1f);



        for (Term s : Builtin.statik)
            if (s instanceof InlineFunctor)
                add(m, s);

        NAR nar = d.nar();


        Functor[] derivationFunctors = new Functor[]{
                d.uniSubstFunctor,
                d.substituteFunctor,
                d.polarizeTask,
                d.polarizeBelief,
                d.polarizeRandom,

                SetFunc.union,
                SetFunc.interSect,
                SetFunc.unionSect,
                SetFunc.differ,
                SetFunc.intersect,
                Equal.equal, Cmp.cmp,

                (Functor) nar.concept("unneg"),
                (Functor) nar.concept("negateEvents"),
                //(Functor) nar.concept("eventOf"),
                (Functor) nar.concept("chooseAnySubEvent"),
                (Functor) nar.concept("chooseUnifiableSubEvent"),

                (Functor) nar.concept("conjWithout"), (Functor) nar.concept("conjWithoutPN"),
                (Functor) nar.concept("without"), (Functor) nar.concept("withoutPN"),
                //(Functor) nar.concept("withoutPNRepolarized"),
//                (Functor) nar.concept("unsect"),(Functor) nar.concept("unsectPN"),(Functor) nar.concept("unsectPNRepolarized"),

                new AbstractInlineFunctor2(ConjMatch.BEFORE) {
                    @Override protected Term apply(Term conj, Term event) {
                        return ConjMatch.beforeOrAfter(conj, event, true, d, NAL.derive.TTL_CONJ_BEFORE_AFTER);
                    }
                },
                new AbstractInlineFunctor2(ConjMatch.AFTER) {
                    @Override protected Term apply(Term conj, Term event) {
                        return ConjMatch.beforeOrAfter(conj, event, false, d, NAL.derive.TTL_CONJ_BEFORE_AFTER);
                    }
                },

                /** applies # dep and $ indep variable introduction if possible. returns the input term otherwise  */
                Functor.f1Inline("varIntro", x -> {
                    Pair<Term, Map<Term, Term>> result = DepIndepVarIntroduction.the.apply(x, nar.random());
                    if (result!=null && result.getOne().op().conceptualizable) {
                        d.retransform.putAll(result.getTwo());
                        return result.getOne();
                    }
                    return Null;
                }),

                new AbstractInlineFunctor1("negateRandomSubterm") {

                    @Override
                    protected Term apply1(Term _arg) {

                        boolean neg = _arg instanceof Neg;
                        Term arg = neg ? _arg.unneg() : _arg;

                        if(!(arg instanceof Compound))
                            return Null;

                        Subterms x = arg.subterms();
                        int n = x.subs();
                        if (n == 0)
                            return Null;

                        int which = d.random.nextInt(n);
                        Subterms y = x.transformSub(which, Term::neg);
                        if (x!=y)
                            return arg.op().the(y).negIf(neg);

                        return Null;
                    }
                },
        };

        for (Term x : derivationFunctors) //override any statik's
            add(m, x);

        m.put(Derivation.TaskTerm, Derivation.TaskTerm); //to be dynamically resolved
        m.put(Derivation.BeliefTerm, Derivation.BeliefTerm); //to be dynamically resolved

//        MetalBloomFilter<Atomic> pre = new MetalBloomFilter<>(fastAtomHash, m.size()*2, 2);
//        for (Atomic x : m.keySet()) {
//            pre.addAt(x);
//        }

        return Maps.immutable.ofMap(m)::get;

        //x -> pre.contains(x) ? m.get(x) : null;
    }

    private static void add(Map<Atomic, Term> m, Term x) {
        m.put((Atomic) x.term(), x);
    }


}