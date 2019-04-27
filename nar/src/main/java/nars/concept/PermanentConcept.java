package nars.concept;

import nars.NAR;

/**
 * Marker interface indicating the Concept should not be forgettable
 *
 * implementations are essentially "hard-wired" into
 * external systems and transducers that populate certain segments of its
 * belief tables and other components.
 *
 */
public interface PermanentConcept extends Concept {

    @Override
    default boolean delete(NAR nar) {
        throw new RuntimeException("permanent concept deleted: " + this);
    }

}
