package nars.derive;

import nars.Builtin;
import nars.NAR;
import nars.Param;
import nars.op.Equal;
import nars.op.SetFunc;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.functor.AbstractInlineFunctor2;
import nars.term.functor.InlineFunctor;
import nars.term.util.Image;
import nars.term.util.conj.ConjMatch;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;
import java.util.function.Function;

import static nars.derive.Derivation.BeliefTerm;
import static nars.derive.Derivation.TaskTerm;
import static nars.term.atom.Bool.Null;

public enum DerivationFunctors {
    ;

    public static Function<Atomic, Term> get(Derivation d) {

        Map<Atomic, Term> m = new UnifiedMap<>(32, 1f) {
            @Override
            public Term get(Object key) {
                if (key == TaskTerm) {
                    return d.taskTerm;
                } else if (key == BeliefTerm) {
                    return d.beliefTerm;
                } else {
                    return super.get(key);
                }
            }
        };

        for (Term s : Builtin.statik)
            if (s instanceof InlineFunctor)
                add(m, s);

        NAR nar = d.nar();


        Functor[] derivationFunctors = new Functor[]{
                d.uniSubst,
                d.mySubst,
                d.polarizeFunc,
                d.polarizeTask,
                d.polarizeBelief,
                d.polarizeRandom,
                Image.imageExt,
                Image.imageInt,
                Image.imageNormalize,
                SetFunc.union,
                SetFunc.interSect,
                SetFunc.unionSect,
                SetFunc.differ,
                SetFunc.intersect,
                Equal.the, Equal.cmp,

                (Functor) nar.concept("varIntro"),
                (Functor) nar.concept("unneg"),
                (Functor) nar.concept("negateEvents"),
                //(Functor) nar.concept("eventOf"),
                (Functor) nar.concept("conjWithout"),
                (Functor) nar.concept("conjWithoutPN"),
                (Functor) nar.concept("chooseAnySubEvent"),
                (Functor) nar.concept("chooseUnifiableSubEvent"),
//                (Functor) nar.concept("dropAnyEvent"),
                (Functor) nar.concept("without"),
                (Functor) nar.concept("withoutPosOrNeg"),
                new AbstractInlineFunctor2("conjBefore") {
                    @Override protected Term apply(Term conj, Term event) {
                        Term x = ConjMatch.beforeOrAfter(conj, event, true, d, Param.TTL_CONJ_BEFORE_AFTER);
                        return x == null ? Null : x;
                    }
                },
                new AbstractInlineFunctor2("conjAfter") {
                    @Override protected Term apply(Term conj, Term event) {
                        Term x = ConjMatch.beforeOrAfter(conj, event, false, d, Param.TTL_CONJ_BEFORE_AFTER);
                        return x == null ? Null : x;
                    }
                }
        };

        for (Term x : derivationFunctors) //override any statik's
            add(m, x);


//        MetalBloomFilter<Atomic> pre = new MetalBloomFilter<>(fastAtomHash, m.size()*2, 2);
//        for (Atomic x : m.keySet()) {
//            pre.addAt(x);
//        }

        ((UnifiedMap)m).trimToSize();

        return //Maps.immutable.ofMap(m)::get;
                m::get;
        //x -> pre.contains(x) ? m.get(x) : null;
    }

    private static void add(Map<Atomic, Term> m, Term x) {
        m.put((Atomic) x.term(), x);
    }


}
