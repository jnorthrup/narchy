package nars.link;

import nars.NAR;
import nars.concept.Concept;
import nars.term.Termed;

import java.util.stream.Stream;

/** creates termlinks during concept activation */
public interface TermLinker {

    /** enumerate all targets.  results may be Term or Concept instances */
    Stream<? extends Termed> targets();

    /**
     * link terms and activate concepts
     */
    void link(Concept src, float pri, NAR nar);


    TermLinker Empty = new TermLinker() {
        @Override
        public Stream<? extends Termed> targets() {
            return Stream.empty();
        }

        @Override
        public void link(Concept src, float pri, NAR nar) {
            //nothing
        }

        @Override
        public Concept[] concepts(NAR nar) {
            return Concept.EmptyArray;
        }
    };


    @Deprecated Concept[] concepts(NAR nar);
}
