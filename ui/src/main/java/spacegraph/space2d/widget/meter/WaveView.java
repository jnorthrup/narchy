package spacegraph.space2d.widget.meter;

import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.wave1d.SignalInput;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.MenuSupplier;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;

/**
 * extends WaveView with region selection support
 *
 */
public class WaveView extends WaveBitmap implements MenuSupplier, Finger.WheelAbsorb {


    private final CircularFloatBuffer data;

    public WaveView(CircularFloatBuffer wave, int pixWidth, int pixHeight) {
        super(pixWidth, pixHeight, (s,e)-> {
            CircularFloatBuffer w = wave;
            if (w!=null)
                return (float) w.mean(s, e);
            else
                return Float.NaN;
        });
        this.data = wave;
    }

    @Deprecated static final float bufferScale = 3;

    public WaveView(SignalInput i, int pixWidth, int pixHeight) {
        this(new CircularFloatBuffer(new float[1]), pixWidth, pixHeight);
        i.wave.on((s)->{
            int sv = s.volume();

            if (data.capacityInternal() < sv * bufferScale)
                data.setCapacity(Math.round(sv * bufferScale));

            if (data.available() < sv) {
                //data.setPeekPosition((data.readAt() + (sv))%data.capacityInternal());
                data.skip(sv);
                //data.freeHead(sv - data.available()); //roll
                assert(data.available() >= sv);
            }



            int written = data.write(s.data,  0, sv, true);
            if (written > 0) {
                updateLive();
            }
//            assert(written > 0);
        });
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

    public void updateLive() {
        update = true;
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
