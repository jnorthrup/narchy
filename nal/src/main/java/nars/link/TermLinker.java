package nars.link;

import nars.NAR;
import nars.concept.Concept;
import nars.term.Termed;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/** creates termlinks during concept activation */
public interface TermLinker {

    /** enumerate all targets.  results may be Term or Concept instances */
    Stream<? extends Termed> targets();


    /**
     * a) insert forward and/or reverse termlinks
     * b) activate concepts
     * c) insert tasklinks
     *
     * balance = nar.termlinkBalance
     */
    void link(Concept src, float pri, List<TaskLink> fired, Random rng, NAR nar);


    /** dont override
     *  preferable to use the RNG local to the derivation instance, not the NAR's.
     *  also termlinkBalance can be cached or modulated contextually with the newer method
     * */
    @Deprecated default void link(Concept src, float pri, List<TaskLink> fired, NAR nar) {
        link(src, pri, fired, nar.random(), nar);
    }

    TermLinker Empty = new TermLinker() {
        @Override
        public Stream<? extends Termed> targets() {
            return Stream.empty();
        }


        @Override
        public void link(Concept src, float pri, List<TaskLink> fired, Random rng, NAR nar) {
            //nothing
        }

    };

}
