package spacegraph.space2d.widget.button;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public abstract class ToggleButton extends AbstractButton {

    final AtomicBoolean on = new AtomicBoolean(false);

    @Nullable
    public ObjectBooleanProcedure<ToggleButton> action;

    protected ToggleButton() {
        this(false);
    }
    protected ToggleButton(boolean startingValue) {
        on.set(startingValue);
    }


    protected ToggleButton(ObjectBooleanProcedure<ToggleButton> action) {
        this();
        on(action);
    }

    public ToggleButton set(boolean on) {
        if (this.on.compareAndSet(!on, on)) {
            if (action != null)
                action.value(this, on);
        }
        return this;
    }

    public boolean get() {
        return on.get();
    }

    public ToggleButton on(BooleanProcedure a) {
        return on((thizz, x)->a.value(x));
    }

    public ToggleButton on(ObjectBooleanProcedure<ToggleButton> a) {
        this.action = a;
        return this;
    }

    public boolean on() {
        return on.get();
    }

    @Override
    protected void onClick() {
        set(!on.get());
    }
}
