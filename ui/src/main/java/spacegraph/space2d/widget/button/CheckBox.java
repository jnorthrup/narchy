package spacegraph.space2d.widget.button;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.space2d.ReSurface;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class CheckBox extends ToggleButton {

    private String text = "";

    public CheckBox(String text) {
        this(text, false);
    }

    public CheckBox(String text, boolean enable) {
        text(text);
        set(enable);
    }

    public CheckBox(String text, Runnable r) {
        this(text, (boolean b) -> { if (b) r.run(); } );
    }

    public CheckBox(String text, BooleanProcedure b) {
        this(text);
        on((a, e) -> b.value(e));
    }

    public CheckBox(String text, ObjectBooleanProcedure<ToggleButton> on) {
        this(text);
        on(on);
    }

    public CheckBox(String text, AtomicBoolean b) {
        this(text, b.get());
        on((button, value) -> b.set(value));
    }


    @Override
    protected boolean canRender(ReSurface r) {
        if (on())
            pri(0.5f);

        return super.canRender(r);
    }

    @Override
    public ToggleButton on(boolean on) {
        if (set(on))
            text(text);
        return this;
    }


    protected String label(String text, boolean on) {
        return text.isEmpty() ? (on ? "[+]" : "[ ]") : ((on ? "[+] " : "[ ] ") + text);
    }


    @Override
    public AbstractButton text(String s) {
        return super.text(label(this.text = s, on()));
    }
}
