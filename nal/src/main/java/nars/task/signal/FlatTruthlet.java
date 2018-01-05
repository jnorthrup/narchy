package nars.task.signal;

public class FlatTruthlet extends RangeTruthlet {

    public float freq, evi;

    public FlatTruthlet(long start, long end, float freq, float evi) {
        super(start, end);
        this.freq = freq;
        this.evi = evi;
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        if (during(when)) {
            freqEvi[0] = freq;
            freqEvi[1] = evi;
        } else {
            unknown(freqEvi);
        }
    }


}
