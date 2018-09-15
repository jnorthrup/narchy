package spacegraph.input.finger;

import org.jetbrains.annotations.Nullable;

/** allows hovering events to set cursors */
public class RenderWhileHovering extends Fingering {


    /** if cursor is null, then returns to default cursor */
    @Nullable private final FingerRenderer cursor;

    public RenderWhileHovering(@Nullable FingerRenderer cursor) {
        this.cursor = cursor;
    }

    @Override
    protected boolean start(Finger f) {
        FingerRenderer cc = cursor;
        if (cc !=null) {
            f.renderer = cc;
        } else {
            f.renderer = f.rendererDefault;
        }
        return true;
    }

    @Override
    protected boolean update(Finger f) {
        return true;
    }

    @Override
    public void stop(Finger f) {
        f.renderer = f.rendererDefault;
    }

    @Override
    public boolean defer(Finger f) {
        return true;
    }

    @Override
    boolean escapes() {
        return true;
    }

    public static final Fingering Reset = new RenderWhileHovering(null) {
        @Override
        protected boolean update(Finger f) {
            return false;
        }
    };
}
