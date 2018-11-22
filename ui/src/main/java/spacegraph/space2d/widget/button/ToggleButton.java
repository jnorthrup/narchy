package spacegraph.space2d.widget.button;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
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
        super(new EmptySurface());
        on.set(startingValue);
    }

    public ToggleButton(Surface view) {
        super(view);
    }


    protected ToggleButton(ObjectBooleanProcedure<ToggleButton> action) {
        this();
        on(action);
    }

    public static IconToggleButton awesome(String icon) {
        return new IconToggleButton(ImageTexture.awesome(icon));
    }

    public ToggleButton on(boolean on) {
        if (this.on.compareAndSet(!on, on)) {
            if (action != null) {
                //Exe.invoke(()->{

                    action.value(this, on);

                //});
            }
        }
        return this;
    }

    public final boolean on() {
        return on.getOpaque();
    }

    public <T extends ToggleButton> T on(Runnable a) {
        return on((x)->{ if (x) a.run(); });
    }

    public <T extends ToggleButton> T on(BooleanProcedure a) {
        return on((thizz, x)->a.value(x));
    }

    public <T extends ToggleButton> T on(ObjectBooleanProcedure<ToggleButton> a) {
        this.action = a;
        return (T) this;
    }



    @Override
    protected void onClick() {
        toggle();
    }

    private void toggle() {
        on(!on());
    }
}
