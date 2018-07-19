package spacegraph.space2d.widget.button;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.video.ImageTexture;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class ToggleButton extends AbstractButton {

    final AtomicBoolean on = new AtomicBoolean(false);

    @Nullable
    public ObjectBooleanProcedure<ToggleButton> action;

    ToggleButton() {
        this(false);
    }
    private ToggleButton(boolean startingValue) {
        on.set(startingValue);
    }

    public ToggleButton(Surface view) {
        super();
        set(view);
    }


    protected ToggleButton(ObjectBooleanProcedure<ToggleButton> action) {
        this();
        on(action);
    }

    public static IconToggleButton awesome(String icon) {
        return new IconToggleButton(ImageTexture.awesome(icon));
    }

    public ToggleButton set(boolean on) {
        if (this.on.compareAndSet(!on, on)) {
            if (action != null)
                action.value(this, on);
        }
        return this;
    }

    public boolean get() {
        return on.getOpaque();
    }

    public ToggleButton on(BooleanProcedure a) {
        return on((thizz, x)->a.value(x));
    }

    public ToggleButton on(ObjectBooleanProcedure<ToggleButton> a) {
        this.action = a;
        return this;
    }



    @Override
    protected void onClick(Finger f) {
        toggle();
    }

    private void toggle() {
        set(!on.get());
    }
}
