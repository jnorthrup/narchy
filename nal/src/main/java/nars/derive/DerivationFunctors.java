package nars.derive;

import nars.Builtin;
import nars.NAR;
import nars.Param;
import nars.op.Equal;
import nars.op.SetFunc;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.util.Image;
import nars.term.util.conj.ConjMatch;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static nars.term.atom.Bool.Null;

public enum DerivationFunctors {
    ;

    public static Function<Atomic, Functor> get(Derivation d) {

        Map<Atomic, Functor> m = new HashMap<>();

        for (Termed s : Builtin.statik)
            if (s instanceof Functor.InlineFunctor)
                add(m, s);

        NAR nar = d.nar;


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
                SetFunc.unionSect,
                SetFunc.differ,
                SetFunc.intersect,
                Equal.the,
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
                new Functor.AbstractInlineFunctor2("conjBefore") {
                    @Override protected Term apply(Term conj, Term event) {
                        Term x = ConjMatch.beforeOrAfter(conj, event, true, d, Param.TTL_CONJ_BEFORE_AFTER);
                        return x == null ? Null : x;
                    }
                },
                new Functor.AbstractInlineFunctor2("conjAfter") {
                    @Override protected Term apply(Term conj, Term event) {
                        Term x = ConjMatch.beforeOrAfter(conj, event, false, d, Param.TTL_CONJ_BEFORE_AFTER);
                        return x == null ? Null : x;
                    }
                }
        };

        for (Termed x : derivationFunctors) //override any statik's
            add(m, x);


//        MetalBloomFilter<Atomic> pre = new MetalBloomFilter<>(fastAtomHash, m.size()*2, 2);
//        for (Atomic x : m.keySet()) {
//            pre.add(x);
//        }

        return //Maps.immutable.ofMap(m)::get;
                m::get;
        //x -> pre.contains(x) ? m.get(x) : null;
    }

    private static Functor add(Map<Atomic, Functor> m, Termed x) {
        return m.put((Atomic) x.term(), (Functor) x);
    }


}
