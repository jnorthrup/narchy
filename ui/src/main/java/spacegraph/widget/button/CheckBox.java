package spacegraph.widget.button;

import com.jogamp.opengl.GL2;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.AspectAlign;
import spacegraph.render.Draw;
import spacegraph.widget.text.Label;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class CheckBox extends ToggleButton {

    public String text;
    protected final Label label;

    public CheckBox(String text) {
        this.text = text;

        add((label = new Label("")).scale(0.8f).align(AspectAlign.Align.LeftCenter));

        set(false);
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
        this(text);
        set(b.get());
        on((button, value) -> b.set(value));
    }

//    public CheckBox(String text, MutableBoolean b) {
//        this(text);
//        set(b.booleanValue());
//        on((button, value) -> b.setValue(value));
//    }


    @Override
    public ToggleButton set(boolean on) {
        label.text((on ? "[X] " : "[ ] ") + text);
        super.set(on);
        return this;
    }

    public void setText(String s) {
        this.text = s;
    }

    public static class ColorToggle extends ToggleButton {
        public float r, g, b;

        public ColorToggle(float r, float g, float b) {
            this.r = r; this.g = g; this.b = b;
        }

        @Override
        protected void paintIt(GL2 gl) {
            gl.glColor4f(r, g, b, 0.95f);
            Draw.rect(gl, bounds);
        }
    }
}
