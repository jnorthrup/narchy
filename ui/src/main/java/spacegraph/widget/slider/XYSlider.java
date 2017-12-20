package spacegraph.widget.slider;

import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.widget.windo.Widget;

/**
 * Created by me on 6/26/16.
 */
public class XYSlider extends Surface {

    final v2 knob = new v2(0.5f, 0.5f);

    private final float knobWidth = 0.2f;
    private final float crosshairWidth = knobWidth/10f;

    public XYSlider() {
        super();
    }

    @Override
    protected boolean onTouching(Finger finger, v2 hitPoint, short[] buttons) {
        if (leftButton(buttons)) {
            knob.set(hitPoint);
            return true;
        }
        return super.onTouching(finger, hitPoint, buttons);
    }


    @Override
    protected void paint(GL2 gl, int dtMS) {
        gl.glColor4f(0f, 0f, 0f, 0.8f); //background
        Draw.rect(gl, bounds);

        Draw.bounds(gl, this, this::paintUnit);
    }

    protected void paintUnit(GL2 gl) {
        //float margin = 0.1f;
        //float mh = margin / 2.0f;

        float px = knob.x;
        float py = knob.y;

        gl.glColor4f(0.75f, 0.75f, 0.75f, 0.75f);
        float H = this.crosshairWidth;
        float h1 = py - H / 2f;
        Draw.rect(gl, 0, h1-H/2, 1, H); //horiz
        float W = this.crosshairWidth;
        float w1 = px - W / 2f;
        Draw.rect(gl, w1-W/2, 0, W, 1); //vert

        //gl.glColor4f(0.2f, 0.8f, 0f, 0.75f);
        float knobSize = this.knobWidth;
        Draw.rect(gl, w1-knobSize/2f, h1-knobSize/2f, knobSize, knobSize, 0); //knob
    }

}
