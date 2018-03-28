package spacegraph.space2d.widget.meta;

import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.Widget;

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
        synchronized (this) {
            if ((uncovered = !uncovered)) {
                content.set(full.get());
            } else {
                content.set(icon.get());
            }
        }
    }

    @Override
    public Surface onTouch(Finger finger, short[] buttons) {
        if (!uncovered) {
            onClick.accept(finger);
        }
        return super.onTouch(finger, buttons);
//        Surface s = super.onTouch(finger, hitPoint, buttons);
//        if (s != null) {
//            return s;
//        } else {
//            return null;
//        }
    }
}
