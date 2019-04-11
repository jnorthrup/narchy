package spacegraph.input.finger;

import org.jetbrains.annotations.Nullable;


public class CursorOverlay extends Fingering {


    /** if cursor is null, then returns to default cursor */
    @Nullable private final FingerRenderer cursor;

    public CursorOverlay(@Nullable FingerRenderer cursor) {
        this.cursor = cursor;
    }

    @Override
    protected boolean start(Finger f) {
        FingerRenderer cc = cursor;
        f.renderer = cc != null ? cc : f.rendererDefault;
        return true;
    }

    @Override
    public boolean updateGlobal(Finger f) {
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
    public boolean escapes() {
        return true;
    }

    @Override
    public @Nullable FingerRenderer renderer(Finger finger) {
        return cursor;
    }

    public static final Fingering Reset = new CursorOverlay(null) {
        @Override
        public boolean updateGlobal(Finger f) {
            return false;
        }
    };
}
