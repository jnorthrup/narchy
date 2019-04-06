package nars.gui.concept;

import nars.NAR;
import nars.concept.Concept;
import nars.term.Termed;
import nars.time.part.DurLoop;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.unit.MutableUnitContainer;

abstract public class ConceptView extends MutableUnitContainer {

    public final Termed term;
    public final NAR nar;
    private DurLoop on;

    public ConceptView(Termed t, NAR n) {
        super();

        this.term = t;
        this.nar = n;
    }

    abstract protected void update();

    @Nullable protected Concept concept() {
        return nar.concept(term);
    }

    @Override
    protected void starting() {
        super.starting();
        on = nar.onDur(this::update);
    }

    @Override
    protected void stopping() {
        on.off();
        on = null;
        super.stopping();
    }

}
