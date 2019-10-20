package spacegraph.audio.synth.granular;

class Granulator {

	private final float[] sourceBuffer;
	private final int grainSizeSamples;
	private final GrainWindow window;


	Granulator(float[] sourceBuffer, float sampleRate,
               float grainSizeSecs, float windowSizeFactor) {
		this.sourceBuffer = sourceBuffer;
		grainSizeSamples = Math.round(sampleRate * grainSizeSecs);

		window = new HanningWindow(Math.round((float) grainSizeSamples * windowSizeFactor));
		
	}

	boolean hasMoreSamples(long[] grain, long now) {
        long length = grain[1];
        long showTime = grain[2];
		return now < showTime + length + (long) window.getSize();
	}

	float sample(long[] grain, long now) {


        float[] sb = sourceBuffer;

        long showTime = grain[2];
        long offset = now - showTime;

        long startIndex = grain[0];
        int sourceIndex = (int) ((startIndex + offset + (long) sb.length));
		while (sourceIndex < 0)
			sourceIndex += sb.length;
		sourceIndex %= sb.length;
		return sb[sourceIndex] * window.getFactor( ((int)offset) );
				
	}

	static boolean isFading(long[] grain, long now) {
        long length = grain[1];
        long showTime = grain[2];
		return now >= showTime + length;
	}

	long[] nextGrain(long[] grain, int startIndex, float fadeInTime) {
		if (grain == null)
			grain = new long[3];
        int ws = window.getSize();
		grain[0] = (long) ((startIndex + ws) % sourceBuffer.length);
		grain[1] = (long) grainSizeSamples;
		grain[2] = (long) Math.round(fadeInTime + (float) ws);
		return grain;
	}

}
