package spacegraph.audio.synth.granular;

import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloat;
import spacegraph.audio.SoundProducer;
import spacegraph.audio.sample.SoundSample;

import java.util.Random;

public class Granulize extends Granulator implements SoundProducer, SoundProducer.Amplifiable {

	private final float[] sourceBuffer;
	private final Random rng;
	private float now;
	private float playTime;

    /** this actually represents the target amplitude which the current amplitude will continuously interpolate towards */
    private final NumberX amplitude = new AtomicFloat(1.0f);
	private final NumberX stretchFactor = new AtomicFloat(1.0f);
	private final Number pitchFactor = new AtomicFloat(1.0f);

    private float currentAmplitude = amplitude.floatValue();


    /** grains are represented as a triple of long integers (see Granulator.createGrain() which constructs these) */
	private long[] currentGrain;
	private long[] fadingGrain;

	private boolean isPlaying;
	private int playOffset = -1;

    public Granulize(SoundSample s, float grainSizeSecs, float windowSizeFactor, Random rng) {
        this(s.buf, s.rate, grainSizeSecs, windowSizeFactor, rng);
    }

	private Granulize(float[] buffer, float sampleRate, float grainSizeSecs, float windowSizeFactor, Random rng) {
		super(buffer, sampleRate, grainSizeSecs, windowSizeFactor);

		sourceBuffer = buffer;

		this.rng = rng;

		play();
	}

	public Granulize at(int pos) {
		playOffset = pos;
		return this;
	}

	private void process(float[] output, int readRate) {
		if (currentGrain == null && isPlaying) {
			currentGrain = nextGrain(null);
		}
        float dNow = ((sampleRate / readRate)) * pitchFactor.floatValue();

        float amp = currentAmplitude;
        float dAmp = (amplitude.floatValue() - amp) / output.length;

        float n = now;


        boolean p = isPlaying;
        if (!p)
            dAmp = (0 - amp) / output.length; 

        long samples = output.length;

        long[] cGrain = currentGrain;
        long[] fGrain = fadingGrain;

		for (int i = 0; i < samples; i++ ) {
            float nextSample = 0;
            long lnow = Math.round(n);
			if (cGrain != null) {
				nextSample = getSample(cGrain, lnow);
				if (Granulator.isFading(cGrain, lnow)) {
					fGrain = cGrain;
					cGrain = p ? nextGrain(cGrain) : null;
				}
			}
			if (fGrain != null) {
                nextSample += getSample(fGrain, lnow);
				if (!hasMoreSamples(fGrain, lnow)) {
					fGrain = null;
				}
			}
			n += dNow;
            output[i] = nextSample * amp;
            amp += dAmp;
		}


        
        currentGrain = cGrain;
        fadingGrain = fGrain;
        now = n;
        currentAmplitude = amp;
	}

    @Override
	public final void setAmplitude(float amplitude) {
        this.amplitude.set(amplitude);
    }

    @Override
    public float getAmplitude() {
        return amplitude.floatValue();
    }


    private void play() {
    	if (!isPlaying) {
			playOffset = Math.abs(rng.nextInt());
			playTime = now;
			isPlaying = true;
		}
	}

	/** continue */
	public void cont() {
		isPlaying = true;
	}

	@Override
	public String toString() {
		return "Granulize{" +
				"sampleRate=" + sampleRate +
				", now=" + now +
				", playTime=" + playTime +
				", amplitude=" + amplitude +
				", stretchFactor=" + stretchFactor +
				", pitchFactor=" + pitchFactor +
				", isPlaying=" + isPlaying +
				", playOffset=" + playOffset +
				'}';
	}

	@Override
	public void stop() {
		isPlaying = false;
	}

	private long[] nextGrain(long[] targetGrain) {
		
        targetGrain = nextGrain(targetGrain, calculateCurrentBufferIndex(), now);
        return targetGrain;
	}

	private int calculateCurrentBufferIndex() {
        float sf = stretchFactor.floatValue();

		return Math.abs(Math.round(playOffset + (now - playTime) / sf)) % sourceBuffer.length;
	}

	public Granulize setStretchFactor(float stretchFactor) {


		this.stretchFactor.set(stretchFactor);
        return this;
	}

    @Override
    public void read(float[] buf, int readRate) {
        process(buf, readRate);
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        
    }

    @Override
    public boolean isLive() {
        return isPlaying;
    }



}
