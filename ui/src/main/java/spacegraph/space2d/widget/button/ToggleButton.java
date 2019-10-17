package spacegraph.space2d.widget.button;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.video.ImageTexture;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class ToggleButton extends AbstractButton {

    final AtomicBoolean on = new AtomicBoolean(false);

    public @Nullable ObjectBooleanProcedure<ToggleButton> action;

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

    private static final Logger logger = LoggerFactory.getLogger(ToggleButton.class);

    public ToggleButton on(boolean on) {
        set(on);
        return this;
    }

    public boolean set(boolean on) {
        if (this.on.compareAndSet(!on, on)) {
            if (action != null) {
                //Exe.invoke(()->{

                try {
                    action.value(this, on);
                    return true;
                } catch (Throwable t) {
                    this.on.set(!on);
                    logger.error("{} {}", this, t);
                }

                //});
            }
        }
        return false;
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
        on(!on());
    }

}
