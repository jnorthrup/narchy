package nars.gui;

import nars.concept.Concept;
import nars.term.Termed;
import spacegraph.space2d.widget.text.Label;

/**
 * Created by me on 11/29/16.
 */
public class TermIcon extends Label {

    private Concept _concept;

    //private float[] bgColor = new float[4];

    public TermIcon(Termed c) {
        super(c.toString());

        //update(nar.concept(this.concept), nar.time());
    }

    public void update(Concept c, long time) {
        this._concept = c;

        Concept cc = this._concept;
        if (cc == null)
            return;

//        float b = 2f * (_concept.beliefFreq(time, 0.5f) - 0.5f);
//        bgColor[0] = b >= 0 ? b : 0;
//        bgColor[1] = b < 0 ? -b : 0;
//        bgColor[2] = 0;
//        bgColor[3] = 0.9f;
    }

//    @Override
//    public void paint(GL2 gl) {
//
//        gl.glColor4fv(bgColor, 0);
//        Draw.rect(gl, 0, 0, 1, 1);
//        super.paint(gl);
//    }
}
