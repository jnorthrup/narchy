package nars.index.concept;

import nars.concept.PermanentConcept;
import nars.index.concept.ConceptIndex;
import nars.term.Termed;

import java.util.function.BiFunction;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeConceptIndex extends ConceptIndex {


    public static final BiFunction<? super Termed, ? super Termed, ? extends Termed> setOrReplaceNonPermanent = (prev, next) -> {
        if (prev instanceof PermanentConcept && !(next instanceof PermanentConcept))
            return prev;
        return next;
    };

}
