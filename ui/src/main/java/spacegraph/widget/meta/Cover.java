package spacegraph.widget.meta;

import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.widget.windo.Widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * click to uncover, by default shows an icon view and an expanded view after uncovering
 */
public class Cover extends Widget {
    public final Supplier<Surface> icon;
    public final Supplier<Surface> full;
    private final Consumer<Finger> onClick;
    volatile boolean uncovered = false;

    public Cover(Supplier<Surface> icon, Supplier<Surface> full) {
        super(icon.get());
        this.icon = icon;
        this.full = full;
        this.onClick = Finger.clicked(0, this::toggle);
    }

    public void toggle() {
        if ((uncovered = !uncovered)) {
            children(full.get());
        } else {
            children(icon.get());
        }
    }

    @Override
    public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
        if (!uncovered) {
            onClick.accept(finger);
        }
        return super.onTouch(finger, hitPoint, buttons);
//        Surface s = super.onTouch(finger, hitPoint, buttons);
//        if (s != null) {
//            return s;
//        } else {
//            return null;
//        }
    }
}
