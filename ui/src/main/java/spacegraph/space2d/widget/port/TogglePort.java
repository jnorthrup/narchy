package spacegraph.space2d.widget.port;

import com.jogamp.opengl.GL2;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.video.Draw;

import java.util.function.Consumer;

/** TODO include and enforce debounce and latch duration parameters */
public class TogglePort extends BoolPort {

    private final CheckBox toggle;

    public TogglePort() {
        this("");
    }

    public TogglePort(String label) {
        this(label, true);
    }

    public TogglePort(String label, boolean initially) {
        super();

        toggle = new CheckBox(label) {
            @Override
            protected void paintIt(GL2 gl, ReSurface r) {
                //transparent
            }
        };
        toggle.on((BooleanProcedure) this::out);
        on((Consumer<Boolean>)toggle::on);

        set(new Scale(toggle.on(initially), 0.75f));
    }

    @Override
    protected void paintIt(GL2 gl, ReSurface r) {
        if (toggle.on()) {
            gl.glColor4f(0f,0.75f,0.1f,0.8f);
        } else {
            gl.glColor4f(0.75f,0.25f,0f,0.7f);
        }
        Draw.rect(bounds, gl);
    }
}
