package spacegraph.audio.synth.granular;

import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloat;
import spacegraph.audio.SoundProducer;
import spacegraph.audio.sample.SoundSample;

import java.util.Random;

public class Granulize extends SoundProducer.Amplifiable {

	final Granulator grains;
	private final float[] sourceBuffer;
	private final Random rng;
	private float now;
	private float playTime;

    /** this actually represents the target amplitude which the current amplitude will continuously interpolate towards */
	public final NumberX stretchFactor = new AtomicFloat(1.0f);
	public final NumberX pitchFactor = new AtomicFloat(1.0f);

    private float currentAmplitude = amp.floatValue();


    /** grains are represented as a triple of long integers (see Granulator.createGrain() which constructs these) */
	private long[] currentGrain;
	private long[] fadingGrain;

	private int playOffset;

    public Granulize(SoundSample s, float grainSizeSecs, float windowSizeFactor, Random rng) {
        this(s.buf, s.rate, grainSizeSecs, windowSizeFactor, rng);
    }

	public Granulize(float[] buffer, float sampleRate, float grainSizeSecs, float windowSizeFactor, Random rng) {
		grains = new Granulator(buffer, sampleRate, grainSizeSecs, windowSizeFactor);

		sourceBuffer = buffer;

		this.rng = rng;

		playOffset = Math.abs(rng.nextInt());
		playTime = now;
	}


	@Override
	public String toString() {
		return "Granulize{" +
				", now=" + now +
				", playTime=" + playTime +
				", amplitude=" + amp() +
				", stretchFactor=" + stretchFactor +
				", pitchFactor=" + pitchFactor +
				", playOffset=" + playOffset +
				'}';
	}


	private long[] nextGrain(long[] targetGrain) {
		
        targetGrain = grains.nextGrain(targetGrain, calculateCurrentBufferIndex(), now);

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

    	boolean p = isLive();

		if (currentGrain == null && p) {
			currentGrain = nextGrain(null);
		}

		float amp = currentAmplitude;
		float dAmp = (amp() - amp) / buf.length;


		if (!p)
			dAmp = (0 - amp) / buf.length;

		long samples = buf.length;

		long[] cGrain = currentGrain;
		long[] fGrain = fadingGrain;
		float dNow = pitchFactor.floatValue();
		float n = now;

		for (int i = 0; i < samples; i++ ) {
            float nextSample = 0;
            long lnow = Math.round(n);
			if (cGrain != null) {
				nextSample = grains.sample(cGrain, lnow);
				if (Granulator.isFading(cGrain, lnow)) {
					fGrain = cGrain;
					cGrain = p ? nextGrain(cGrain) : null;
				}
			}
			if (fGrain != null) {
                nextSample += grains.sample(fGrain, lnow);
				if (!grains.hasMoreSamples(fGrain, lnow)) {
					fGrain = null;
				}
			}
			n += dNow;
            buf[i] = nextSample * amp;
            amp += dAmp;
		}


		currentGrain = cGrain;
		fadingGrain = fGrain;
		now = n;
		currentAmplitude = amp;
	}

    @Override
    public void skip(int samplesToSkip, int readRate) {
        now += (pitchFactor.floatValue() * samplesToSkip);// / readRate;
    }


}
