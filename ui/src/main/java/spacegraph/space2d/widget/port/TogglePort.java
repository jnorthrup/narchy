package spacegraph.space2d.widget.port;

import com.jogamp.opengl.GL2;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.video.Draw;

public class TogglePort extends Stacking {

    private final BoolPort port;

    public TogglePort() {
        this("");
    }

    public TogglePort(String label) {
        this(label, true);
    }

    public TogglePort(String label, boolean initially) {
        super();

        CheckBox toggle = new CheckBox(label);

        this.port = new BoolPort() {
            @Override
            protected void paintIt(GL2 gl, SurfaceRender r) {
                if (toggle.on()) {
                    gl.glColor4f(0f,0.75f,0.1f,0.8f);
                } else {
                    gl.glColor4f(0.75f,0.25f,0f,0.7f);
                }
                Draw.rect(bounds, gl);
            }
        };

        toggle.on((BooleanProcedure) port::out);

        set(
            port,
            new Scale(toggle.on(initially), 0.75f)
        );
    }

}
