package spacegraph.audio;


import jcog.data.list.FastCoWList;
import jcog.util.ArrayUtil;

import java.util.Arrays;


public class SoundMixer extends FastCoWList<Sound> implements StereoSoundProducer {

    private float[] buf = ArrayUtil.EMPTY_FLOAT_ARRAY;

    private final int audibleSources;

    private SoundSource soundSource;

    public SoundMixer(int audibleSources) {
        super(Sound[]::new);
        this.audibleSources = audibleSources;
    }

    public void setSoundListener(SoundSource soundSource) {
        this.soundSource = soundSource;
    }

    public <S extends SoundProducer> Sound<S> addSoundProducer(S producer, SoundSource soundSource, float volume, float priority) {
        Sound s = new Sound(producer, soundSource, volume, priority);
        if (!s.isLive())
            throw new RuntimeException("dont add a dead sound");
        add(s);
        return s;
    }

    public void update(float receiverBalance) {
        boolean updating = (soundSource != null);

        this.removeIf(sound -> !updating || !sound.update(soundSource, receiverBalance));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(float[] leftBuf, float[] rightBuf, int readRate) {

        Sound[] ss = array();
        int s = ss.length;

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

        for (int i = 0; i < s; i++) {
            Sound sound = ss[i];

            if (i < audibleSources) {
                float[] buf = this.buf;
                Arrays.fill(buf, 0);

                sound.producer.read(buf, readRate);

                float pan = sound.pan;

                float amp = sound.amplitude;
                float rp = (pan <= 0 ? 1 : 1 + pan) * amp;
                float lp = (pan >= 0 ? 1 : 1 - pan) * amp;

                int l = leftBuf.length;
                for (int j = 0; j < l; j++) {
                    float bj = buf[j];
                    leftBuf[j] += bj * lp;
                    rightBuf[j] += bj * rp;
                }


            } else {


                sound.skip(leftBuf.length, readRate);
            }

        }

    }



    @Override
    public void skip(int samplesToSkip, int readRate) {
        for (Sound sound : this) {
            sound.skip(samplesToSkip, readRate);
        }
    }
}