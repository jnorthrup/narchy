package nars.link;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/** resolves termlinks and grows tasklinks */
public interface TermDecomposer {

    @Nullable Term decompose(Compound t, Random rng);

}

//    /**
//     * a) insert forward and/or reverse termlinks
//     * b) activate concepts
//     * c) insert tasklinks
//     *
//     * balance = nar.termlinkBalance
//     */
//    void link(TaskLink tasklink, Task task, Derivation d);


//    TermLinker NullLinker = new TermLinker() {
//
//        @Override
//        public Term sample(Term term, Random random) {
//            return term;
//        }
//
//        @Override
//        public void sample(Random rng, Function<? super Term, SampleReaction> each) {
//
//        }
//
//        @Override
//        public Stream<? extends Term> targets() {
//            return Stream.empty();
//        }
////
////
////        @Override
////        public void link(TaskLink tasklink, Task task, Derivation d) {
////
////        }
//
//    };


