package spacegraph.audio.synth.granular;

import jcog.random.XorShift128PlusRandom;
import spacegraph.audio.Audio;
import spacegraph.audio.SoundSource;
import spacegraph.audio.sample.SampleLoader;

public enum GranulizerDemo {
    ;

    @SuppressWarnings("HardcodedFileSeparator")
    public static void main(String[] args) throws InterruptedException {

        Audio audio = new Audio(16);

        Granulize ts =
            new Granulize(SampleLoader.load("/tmp/awake.wav"), 0.25f, 0.9f, new XorShift128PlusRandom(1L))
                    .setStretchFactor(0.25f);

        audio.play(ts, SoundSource.center, 1.0F, 1.0F);

        

        audio.thread.join();
    }


}
