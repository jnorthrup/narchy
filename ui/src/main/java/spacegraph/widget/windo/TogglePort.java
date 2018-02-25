package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import spacegraph.container.Gridding;
import spacegraph.render.Draw;
import spacegraph.widget.button.CheckBox;

public class TogglePort extends Gridding {

    public final Port port;

    public TogglePort() {
        super(VERTICAL);

        margin = 0.25f;

        //if enabled, this will forward to the output
        this.port = new Port() {
            @Override
            protected void paintBelow(GL2 gl) {
                if (port.enabled()) {
                    gl.glColor4f(0,1,0,0.75f);
                } else {
                    gl.glColor4f(1,0,0,0.75f);
                }
                Draw.rect(gl, bounds);
            }
        };
        port.on((w, x)-> {
            port.out(x); //forward
        });

        set(new CheckBox("", port::enable).set(true),
            port
        );

    }


}
