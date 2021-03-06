package spacegraph.audio.synth.string;

/**
 * Created by ericlgame on 14-Mar-16.
 */

public class SquareHarmonicsDiffDecayString extends SquareHarmonicsString {

    public SquareHarmonicsDiffDecayString(double frequency) {
        super(frequency);
        setDeltaVolume(calcDecay(frequency));
    }

    private static double calcDecay(double frequency) {
        double factor = 440.0 / frequency;
        return Math.pow(.996, factor);
    }

}
