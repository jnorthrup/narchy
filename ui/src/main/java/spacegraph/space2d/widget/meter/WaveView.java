package spacegraph.space2d.widget.meter;

import jcog.Util;
import jcog.math.v2;
import jcog.pri.ScalarValue;
import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.wave1d.SignalReading;
import spacegraph.input.finger.Dragging;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMove;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.MenuSupplier;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;

import static java.lang.Float.NaN;

/**
 * waveform viewing and editing
 * TODO extend Spectrogram
 */
public class WaveView extends WaveBitmap implements MenuSupplier, Finger.WheelAbsorb {

    final static int SELECT_BUTTON = 0;
    final static int PAN_BUTTON = 2;
    final static float PAN_SPEED = 1 / 100f;

    public WaveView(CircularFloatBuffer wave, int pixWidth, int pixHeight) {
        super(pixWidth, pixHeight, wave);
    }

    @Deprecated
    public WaveView(SignalReading capture, int pixWidth, int pixHeight) {
        this(new CircularFloatBuffer(capture.data), pixWidth, pixHeight);
    }

    final Fingering pan = new FingerMove(PAN_BUTTON) {
        @Override
        protected void move(float tx, float ty) {
            pan(tx * PAN_SPEED);
        }

        @Override
        public v2 pos(Finger finger) {
            return finger.posRelative(WaveView.this);
        }
    };

    volatile private float selectStart = NaN, selectEnd = NaN;


    final Fingering select = new Dragging(SELECT_BUTTON) {

        float sample(float x) {
            return start + (end - start) * (x / w());
        }

        @Override
        protected boolean startDrag(Finger f) {
            selectStart = sample(f.posGlobal(WaveView.this).x);
            return true;
        }

        @Override
        protected boolean drag(Finger f) {
            selectEnd = sample(f.posGlobal(WaveView.this).x);
            return true;
        }
    };

    @Override
    public Surface finger(Finger finger) {


        //TODO if ctrl pressed or something

        if (finger.pressedNow(2)) {
            float wheel;
            if ((wheel = finger.rotationY(true)) != 0) {
                scale(((1f + wheel * 0.1f)));
                //pan(+1);
                return this;
            }
        }

        if (finger.tryFingering(pan)) {
            return this;
        }
        if (finger.tryFingering(select)) {
            return this;
        }

        return null;
    }

    @Override
    protected void compileAbove(SurfaceRender r) {
        float sStart = selectStart;
        if (sStart == sStart) {
            float sEnd = selectEnd;
            if (sEnd == sEnd) {
                r.on((gl, rr) -> {
                    float ss = Util.clamp(x(selectStart), left(), right());
                    gl.glColor4f(1f, 0.8f, 0, 0.5f);
                    float ee = Util.clamp(x(selectEnd), left(), right());
                    if (ee - ss > ScalarValue.EPSILON) {
                        Draw.rect(x() + ss, y(), ee - ss, h(), gl);
                    }
                });
                //System.out.println("select: " + sStart + ".." + sEnd);
            }
        }

        //super.compileAbove(r);
    }

    float x(float sample) {
        long f = start;
        return (sample - f) / (end - f) * w();
    }

    @Override
    public Surface menu() {
        return new Gridding(
                PushButton.awesome("play"),
                PushButton.awesome("microphone"),
                PushButton.awesome("save"), //remember
                PushButton.awesome("question-circle") //recognize

                //TODO trim, etc
        );
    }

//
//    public void update() {
//        update();
//    }

//    public void updateLive() {
////        if (showing())
////            updateLive();
//    }
//
//    public void updateLive(int samples) {
////        updateLive(samples);
//    }
}
