package nars.derive;

import jcog.bloom.hash.HashProvider;
import nars.Builtin;
import nars.NAR;
import nars.op.Equal;
import nars.op.SetFunc;
import nars.term.Functor;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.util.Image;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public enum DerivationFunctors {;

    public static Function<Atomic,Functor> get(Derivation d) {

        Map<Atomic, Functor> m = new HashMap<>();

        for (Termed s : Builtin.statik)
            if (s instanceof Functor.InlineFunctor)
                add(m, s);

        NAR nar = d.nar;

        Functor[] derivationFunctors = new Functor[]{
                d.myUniSubst,
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
                (Functor) nar.concept("conjWithout"),
                (Functor) nar.concept("conjWithoutAll"),
                (Functor) nar.concept("conjWithoutPosOrNeg"),
                (Functor) nar.concept("conjDropIfEarliest"),
                (Functor) nar.concept("conjDropIfLatest"),
                (Functor) nar.concept("dropAnyEvent"),
                (Functor) nar.concept("without"),
                (Functor) nar.concept("withoutPosOrNeg"),
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

//        {
//            Map<Term, Termed> n = new HashMap<>(Builtin.statik.length);
//            for (Termed s : Builtin.statik) {
//                if (s instanceof Functor.InlineFunctor)
//                    n.put(s.term(), s);
//            }
//            this.staticFunctors = Maps.immutable.ofMap(n);
//        }


    final static HashProvider<Atomic> fastAtomHash = new HashProvider<Atomic>() {
        @Override
        public int hash1(Atomic element) {
            return element.hashCode();
        }

        @Override
        public int hash2(Atomic element) {
            return element.bytes().length;
        }
    };
}
