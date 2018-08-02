package spacegraph.audio.synth.granular;

class Granulator {

	private final float[] sourceBuffer;
	final float sampleRate;
	private final int grainSizeSamples;
	private final GrainWindow window;

	
	
	
	

	Granulator(float[] sourceBuffer, float sampleRate,
               float grainSizeSecs, float windowSizeFactor) {
		this.sourceBuffer = sourceBuffer;
		grainSizeSamples = Math.round(sampleRate * grainSizeSecs);

		window = new HanningWindow(Math.round(sampleRate * grainSizeSecs
				* windowSizeFactor));
		
		

		this.sampleRate = sampleRate;
	}

	boolean hasMoreSamples(long[] grain, long now) {
		long length = grain[1];
		long showTime = grain[2];
		return now < showTime + length + window.getSize();
	}

	float getSample(long[] grain, long now) {
		long startIndex = grain[0];
		
		long showTime = grain[2];

		float[] sb = sourceBuffer;

		long offset = now - showTime;
		int sourceIndex = (int) ((startIndex + offset + sb.length) % sb.length);
		while (sourceIndex < 0)
			sourceIndex += sb.length;
		return sb[sourceIndex] * window.getFactor( ((int)offset) );
				
	}

	static boolean isFading(long[] grain, long now) {
		long length = grain[1];
		long showTime = grain[2];
		return now > showTime + length;
	}

	long[] nextGrain(long[] grain, int startIndex, float fadeInTime) {
		if (grain == null)
			grain = new long[3];
		int ws = window.getSize();
		grain[0] = (startIndex + ws) % sourceBuffer.length;
		grain[1] = grainSizeSamples;
		grain[2] = Math.round(fadeInTime + ws);
		return grain;
	}

}
