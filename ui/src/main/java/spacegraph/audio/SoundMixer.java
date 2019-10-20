package spacegraph.audio;


import jcog.data.list.FastCoWList;
import jcog.util.ArrayUtil;

import java.util.Arrays;


public class SoundMixer extends FastCoWList<Sound> implements StereoSoundProducer {

	private final int audibleSources;
	private float[] buf = ArrayUtil.EMPTY_FLOAT_ARRAY;
	private SoundSource soundSource;

	public SoundMixer(int audibleSources) {
		super(Sound[]::new);
		this.audibleSources = audibleSources;
	}

	public void setSoundListener(SoundSource soundSource) {
		this.soundSource = soundSource;
	}

	public <S extends SoundProducer> Sound<S> add(S producer, SoundSource soundSource, float volume, float priority) {
		var s = new Sound(producer, soundSource, volume, priority);
		s.playing = true;
		add(s);
		return s;
	}

	public void update(float receiverBalance) {
		var updating = (soundSource != null);

		this.removeIf(sound -> !updating || !sound.update(soundSource, receiverBalance));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void read(float[] leftBuf, float[] rightBuf, int readRate) {

		var ss = array();
		var s = ss.length;

		if (s == 0)
			return;

		if (buf.length != leftBuf.length)
			buf = new float[leftBuf.length];

		if (s > audibleSources) {
			//Collections.sort(this);
			sort();
		}

		Arrays.fill(leftBuf, 0);
		Arrays.fill(rightBuf, 0);

		var l = leftBuf.length;

		for (var i = 0; i < s; i++) {
			var sound = ss[i];

			if (i < audibleSources) {
				var buf = this.buf;
				Arrays.fill(buf, 0);

				var kontinues = sound.producer.read(buf, readRate);

				var pan = sound.pan;

				var amp = sound.amplitude;
				var rp = (pan <= 0 ? 1 : 1 + pan) * amp;
				var lp = (pan >= 0 ? 1 : 1 - pan) * amp;

				for (var j = 0; j < l; j++) {
					var bj = buf[j];
					leftBuf[j] += bj * lp;
					rightBuf[j] += bj * rp;
				}

				if (!kontinues)
					sound.stop();

			} else {
				sound.skip(leftBuf.length, readRate);
			}

		}

	}


	@Override
	public void skip(int samplesToSkip, int readRate) {
		for (var sound : this) sound.skip(samplesToSkip, readRate);
	}
}