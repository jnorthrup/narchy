package nars.link;

import jcog.pri.bag.Sampler;
import nars.term.Term;
import nars.term.Termed;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

/** creates termlinks during concept activation */
public interface TermLinker extends Sampler<Term> {

    /** enumerate all targets.  results may be Term or Concept instances */
    Stream<? extends Termed> targets();


//    /**
//     * a) insert forward and/or reverse termlinks
//     * b) activate concepts
//     * c) insert tasklinks
//     *
//     * balance = nar.termlinkBalance
//     */
//    void link(TaskLink tasklink, Task task, Derivation d);


    TermLinker NullLinker = new TermLinker() {

        @Override
        public void sample(Random rng, Function<? super Term, SampleReaction> each) {

        }

        @Override
        public Stream<? extends Termed> targets() {
            return Stream.empty();
        }
//
//
//        @Override
//        public void link(TaskLink tasklink, Task task, Derivation d) {
//
//        }

    };



}
