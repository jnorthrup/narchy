package nars.link;

import jcog.pri.PLinkHashCached;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;


/**
 * represents the current activation level of a concept
 */
public final class Activate extends PLinkHashCached<Concept> implements Termed {

    public Activate(@Nullable Concept c, int hash, float pri) {
        super(c, hash, pri);
    }

    public Activate(@Nullable Concept c, float pri) {
        this(c, c!=null ? c.hashCode() : 0, pri);
    }

    @Override
    public Term term() {
        return id.term();
    }

}
