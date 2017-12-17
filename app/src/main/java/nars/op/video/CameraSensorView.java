package nars.op.video;

import jcog.Util;
import nars.NAR;
import nars.NAgent;
import nars.concept.TaskConcept;
import nars.control.DurService;
import nars.truth.Truth;
import nars.util.signal.Sensor2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.render.Draw;
import spacegraph.widget.meter.BitmapMatrixView;

/**
 * displays a CameraSensor pixel data as perceived through its concepts (belief/goal state)
 * monochrome
 */
public class CameraSensorView extends BitmapMatrixView implements BitmapMatrixView.ViewFunction2D {

    private final Sensor2D cam;
    private final NAR nar;
    private DurService on;
    private float maxConceptPriority;
    private long now;
    int dur;

    public CameraSensorView(Sensor2D cam, NAgent a) {
        super(cam.width, cam.height);
        this.cam = cam;
        this.nar = a.nar;
        this.dur = a.nar.dur();
    }

    @Override
    public void start(@Nullable Surface parent) {
        super.start(parent);
        on = DurService.on(nar, this::accept);
    }

    @Override
    public void stop() {
        super.stop();
        if (on!=null) { on.off(); this.on = null; }
    }



    public void accept(NAR nn) {
        now = nn.time();
        dur = nn.dur();
        maxConceptPriority = 1;
        update();
//            nar instanceof Default ?
//                ((Default) nar).focus.active.priMax() :
//                1; //HACK TODO cache this
    }

    @Override
    public int update(int x, int y) {


        TaskConcept s = cam.matrix[x][y];
        Truth b = s.beliefs().truth(now, nar);
        float bf = b != null ? b.freq() : 0.5f;

        Truth d = s.goals().truth(now, nar);
        float R = bf*0.75f, G = bf*0.75f, B = bf*0.75f;
        if (d!=null) {
            float f = d.expectation();
            //float c = d.conf();
            if (f > 0.5f) {
                G += 0.25f * (f - 0.5f)*2f;
            } else {
                R += 0.25f * (0.5f - f)*2f;
            }
        }

//        float p = 1f;//nar.pri(s);
//        if (p!=p) p = 0;

        //p /= maxConceptPriority;



        return Draw.rgbInt(
                Util.unitize(R), Util.unitize(G), Util.unitize(B)
                /*, 0.5f + 0.5f * p*/);
    }
}
