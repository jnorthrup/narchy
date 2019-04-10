package spacegraph.space2d.widget.button;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.widget.text.AbstractLabel;
import spacegraph.space2d.widget.text.BitmapLabel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class CheckBox extends ToggleButton {

    private String text = "";
    public final AbstractLabel label;

    public CheckBox(String text) {
        this(text, false);
    }

    public CheckBox(String text, boolean enable) {
        set(label =
                //new VectorLabel("")
                new BitmapLabel("")
        );
        setText(text);
        set(enable);
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
        return '"' + text + '"';
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
    protected boolean preRender(ReSurface r) {
        if (on())
            pri(0.5f);

        return super.preRender(r);
    }


    @Override
    public ToggleButton on(boolean on) {
        if (set(on))
            label.text(label(text, on));
        return this;
    }


    protected String label(String text, boolean on) {
        return text.isEmpty() ? (on ? "[+]" : "[ ]") : ((on ? "[+] " : "[ ] ") + text);
    }

    public void setText(String s) {
        if (!this.text.equals(s)) {
            label.text(label(this.text = s, on()));
        }
    }

}
