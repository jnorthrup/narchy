package spacegraph.space2d.widget.meta;

import jcog.exe.Exe;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.function.Supplier;

public class LazySurface extends MutableUnitContainer<Surface> {

    public LazySurface(Supplier<Surface> async, String msgWhileWaiting) {
        this(async, new VectorLabel(msgWhileWaiting));
    }

    public LazySurface(Supplier<Surface> async, Surface whileWaiting) {
        super(whileWaiting);

        Exe.invokeLater(()->{
            //TODO profile option
            set(safe(async));
        });
    }

}
