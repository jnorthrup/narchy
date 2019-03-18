package spacegraph.space2d.widget.meta;

import jcog.exe.Exe;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.function.Supplier;

public class LazySurface extends MutableUnitContainer<Surface> {

    private final Supplier<Surface> async;

    public LazySurface(Supplier<Surface> async, String msgWhileWaiting) {
        this(async, new VectorLabel(msgWhileWaiting));
    }

    public LazySurface(Supplier<Surface> async, Surface whileWaiting) {
        super(whileWaiting);
        this.async = async;
    }

    @Override
    protected void starting() {
        super.starting();

        SurfaceBase p = parent;
        Exe.invokeLater(()->{
            //TODO profile option
            Surface next = safe(async);
            set(next);
            //TODO if possible try to reattach the view to the parent of this, eliminating this intermediary
            //((Container)p).replace
        });
    }
}
