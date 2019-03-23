package spacegraph.space2d.widget.button;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class CheckBox extends ToggleButton {

    private String text;
    public final VectorLabel label;

    public CheckBox(String text) {
        this.text = text;

        set(label = new VectorLabel(""));

        on(false);
    }

    public CheckBox(String text, Runnable r) {
        this(text, (boolean b) -> { if (b) r.run(); } );
    }

    public CheckBox(String text, BooleanProcedure b) {
        this(text);
        on((a, e) -> b.value(e));
    }

    @Override
    public String term() {
        return "\"" + text + "\"";
    }

    public CheckBox(String text, ObjectBooleanProcedure<ToggleButton> on) {
        this(text);
        on(on);
    }

    public CheckBox(String text, AtomicBoolean b) {
        this(text);
        on(b.get());
        on((button, value) -> b.set(value));
    }


    @Override
    protected boolean prePaint(ReSurface r) {
        if (on())
            pri(0.5f);

        return super.prePaint(r);
    }


    @Override
    public ToggleButton on(boolean on) {
        label.text(label(text, on));
        super.on(on);
        return this;
    }


    protected String label(String text, boolean on) {
        return text.isEmpty() ? (on ? "[+]" : "[ ]") : ((on ? "[+] " : "[ ] ") + text);
    }

    public void setText(String s) {
        if (!this.text.equals(s)) {
            this.text = s;
            label.text(label(s, on()));
        }
    }

}
