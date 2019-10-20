package spacegraph.audio.synth.string;

/**
 * Created by ericlgame on 13-Mar-16.
 */

public class SquareDiffDecayString extends SquareString {

    public SquareDiffDecayString(double frequency) {
        super(frequency);
        setDecays(frequency);
    }

    private void setDecays(double frequency) {
        var factor = 440 / frequency;
        setPluckDelta(Math.pow(pluckDelta(), factor));
        setReleaseDelta(Math.pow(releaseDelta(), factor));
    }
}
