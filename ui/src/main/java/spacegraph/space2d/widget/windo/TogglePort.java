package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.video.Draw;

public class TogglePort extends Gridding {

    private final Port port;

    public TogglePort() {
        super(VERTICAL);

        margin = 0.25f;

        
        this.port = new Port() {
            @Override
            protected void paintBelow(GL2 gl) {
                if (port.enabled()) {
                    gl.glColor4f(0,1,0,0.75f);
                } else {
                    gl.glColor4f(1,0,0,0.75f);
                }
                Draw.rect(bounds, gl);
            }
        };
        port.on((w, x)-> port.out(x));

        set(new CheckBox("", port::enable).set(true),
            port
        );

    }


}
