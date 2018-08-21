package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import spacegraph.audio.WaveCapture;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerDragging;
import spacegraph.input.finger.FingerMove;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.video.Draw;

import static java.lang.Float.NaN;

/** waveform viewing and editing */
public class WaveView extends Widget implements MetaFrame.Menu, Finger.WheelAbsorb {

    final static int SELECT_BUTTON = 0;
    final static int PAN_BUTTON = 2;

    private final long startMS;
    private final float[] wave;
    private final Plot2D.BitmapWave vis;


    public WaveView(WaveCapture capture, float seconds) {
        super();
        this.startMS = System.currentTimeMillis();
        int totalSamples = (int) Math.ceil(seconds * capture.source.samplesPerSecond());

        this.wave = capture.buffer.peekLast(new float[totalSamples]);


        vis = new Plot2D.BitmapWave(1024, 128);
        Plot2D p = new Plot2D(totalSamples, vis).add("Amp", wave);
        set(p);
    }

    final Fingering pan = new FingerMove(PAN_BUTTON) {
        @Override
        protected void move(float tx, float ty) {
            vis.pan(tx/100.0f);
        }
    };

    volatile private float selectStart = NaN, selectEnd = NaN;


    final Fingering select = new FingerDragging(SELECT_BUTTON) {

        float sample(float x) {
            return vis.firstSample() + (vis.lastSample() - vis.firstSample()) * (x / w());
        }

        @Override
        protected boolean startDrag(Finger f) {
            selectStart = sample(f.pos.x);
            return true;
        }

        @Override
        protected boolean drag(Finger f) {
            selectEnd = sample(f.pos.x);
            return true;
        }
    };

    @Override
    public Surface tryTouch(Finger finger) {

//        if (selectStart!=null && !finger.pressing(SELECT_BUTTON)) {
//            selectStart
//        }

        float wheel;

        if ((wheel = finger.rotationY())!=0) {
//            scale = Util.clamp(scale * ( (1f - wheel*0.1f) ), 0.1f, 10f);


            vis.scale(( (1f + wheel*0.1f)));
            //vis.pan(+1);
            return this;
        }

        if (finger.tryFingering(pan)) {
            return this;
        }
        if (finger.tryFingering(select)) {
            return this;
        }

        return this;
    }

    @Override
    protected void paintAbove(GL2 gl, SurfaceRender r) {
        float sStart = selectStart;
        if (sStart==sStart) {
            float sEnd = selectEnd;
            if (sEnd==sEnd) {
                gl.glColor4f(1f, 0.8f, 0, 0.5f);
                Draw.rect(gl, x(selectStart), 0, x(selectEnd)-x(selectStart), h());
                //System.out.println("select: " + sStart + ".." + sEnd);
            }
        }

        super.paintAbove(gl, r);
    }

    float x(float sample) {
        int f = vis.firstSample();
        return (sample - f)/(vis.lastSample() - f) * w();
    }

    @Override
    public Surface menu() {
        return new Gridding(

        );
    }
}
