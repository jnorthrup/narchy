package nars.gui.concept;

import nars.NAR;
import nars.concept.Concept;
import nars.control.DurService;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.unit.MutableUnitContainer;

abstract public class ConceptView extends MutableUnitContainer {

    public final Termed term;
    public final NAR nar;
    private DurService on;

    public ConceptView(Termed t, NAR n) {
        super();

        this.term = t;
        this.nar = n;


    }

    abstract protected void update();

    @Nullable
    public Concept concept() {
        return nar.concept(term);
    }

    @Override
    public boolean start(@Nullable SurfaceBase parent) {
        if (super.start(parent)) {
            on = DurService.on(nar, this::update);
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            if (on != null) {
                on.off();
                on = null;
            }
            return true;
        }
        return false;
    }
}
