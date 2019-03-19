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


    public WaveView(CircularFloatBuffer wave, int pixWidth, int pixHeight) {
        super(pixWidth, pixHeight, (s,e)-> (float) wave.mean(s, e));
    }

    @Deprecated
    public WaveView(SignalInput capture, int pixWidth, int pixHeight) {
        this(new CircularFloatBuffer(capture.data), pixWidth, pixHeight);
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
