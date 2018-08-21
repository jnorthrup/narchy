package spacegraph.audio;

import jcog.event.ListTopic;
import jcog.event.Topic;
import jcog.exe.Loop;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.AsyncTensor;
import jcog.signal.wave1d.SlidingDFTTensor;
import org.apache.commons.lang3.ArrayUtils;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.slider.FloatSlider;

/**
 * TODO enable/disable switch
 * TODO correct ringbuffer w/ notify of buffer change interval https://github.com/waynetam/CircularBuffer
 *
 * Created by me on 10/28/15.
 */
public class WaveCapture extends Loop {

    private int bufferSamples;

    public float[] samples = ArrayUtils.EMPTY_FLOAT_ARRAY, nextSamples = ArrayUtils.EMPTY_FLOAT_ARRAY;

    public final AsyncTensor<ArrayTensor> wave = new AsyncTensor<>(new ArrayTensor(samples));

    public WaveSource source;

    /**
     * called when next sample (buffer) frame is ready
     */
    public final Topic<WaveCapture> frame = new ListTopic<>();


    public Surface view() {

        final Plot2D.Series rawWave, wavelet1d;

        rawWave = new Plot2D.Series("Audio", 1) {

            @Override
            public void update() {
                clear();

                float[] samples = WaveCapture.this.samples;
                if (samples == null) return;


                int chans = WaveCapture.this.source.channelsPerSample();
                int bufferSamples = Math.min(WaveCapture.this.bufferSamples, samples.length / chans);
                switch (chans) {
                    case 1:
                        for (int i = 0; i < bufferSamples; i++)
                            add(samples[i]);
                        break;
                    case 2:
                        for (int i = 0; i < bufferSamples; )
                            add((samples[i++] + samples[i++]) / 2f); //HACK
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }


            }

        };


        SlidingDFTTensor freqDomain =
                //new HaarWaveletTensor(wave, bufferSamples);
                new SlidingDFTTensor(wave, 64);
        wavelet1d = new Plot2D.Series("Wavelet", 1) {
            {
                wave.on(x -> {
                   freqDomain.update();
                   clear();
                   freqDomain.forEach((i,f)->{
                       add(f);
                   });
                });
            }
        };

//        wavelet1d = new Plot2D.Series("Wavelet", 1) {
//
//            final float[] transformedSamples = new float[Util.largestPowerOf2NoGreaterThan(bufferSamples)];
//            final AtomicBoolean busy = new AtomicBoolean();
//            @Deprecated
//            public float[] data = new float[historyFrames * freqSamplesPerFrame];
//            private volatile float dataMax, dataMin;
//            {
//                frame.on((w) -> {
//                    if (!busy.compareAndSet(false, true))
//                        return;
//
//
//                    FloatArrayList history = this;
//
//
//                    final int bufferSamples = Math.min(samples.length, WaveCapture.this.bufferSamples);
//
//                    float[] ss = transformedSamples;
//
//
//                    System.arraycopy(samples, 0, ss, 0, bufferSamples);
//                    OneDHaar.inPlaceFastHaarWaveletTransform(ss);
//                    sampleFrequency(ss);
//
//
//                    history.clear();
//                    for (int i = 0; i < bufferSamples; i++)
//                        history.addAll(ss[i]);
//
//
//                    busy.set(false);
//
//                });
//            }
//
//
//            private void sampleFrequency(float[] freqSamples) {
//                int lastFrameIdx = data.length - freqSamplesPerFrame;
//
//                int samples = freqSamples.length;
//
//                float bandWidth = ((float) samples) / freqSamplesPerFrame;
//                float sensitivity = 1f;
//
//                final Envelope uniform = (i, k) -> {
//                    float centerFreq = (0.5f + i) * bandWidth;
//                    return 1f / (1f + Math.abs(k - centerFreq) / (bandWidth / sensitivity));
//                };
//
//                System.arraycopy(data, freqSamplesPerFrame, data, 0, lastFrameIdx);
//
//
//
//
//                float max = Float.NEGATIVE_INFINITY, min = Float.POSITIVE_INFINITY;
//                for (int i = 0; i < freqSamplesPerFrame; i++) {
//
//                    float s = 0;
//                    for (int k = 0; k < samples; k++) {
//                        s += uniform.apply(i, k) * freqSamples[k];
//                    }
//                    if (s > max)
//                        max = s;
//                    if (s < min)
//                        min = s;
//
//                    data[i+lastFrameIdx] = s;
//                }
//                dataMin = min;
//                dataMax = max;
//
////                if (max != min) {
////                    float range = max - min;
////                    for (int i = 0; i < freqSamplesPerFrame; i++)
////                        dataNorm[i] = (WaveCapture.this.data[i] - min) / range;
////                }
//
//
//            }
//
//        };

        rawWave.range(-1, +1);
        wavelet1d.range(-1, +1);


        Plot2D audioPlot = new Plot2D(bufferSamples, Plot2D.Line);
        audioPlot.add(rawWave);

        Plot2D audioPlot2 = new Plot2D(bufferSamples, Plot2D.Line);
        audioPlot2.add(wavelet1d);

//        BitmapMatrixView freqHistory = new BitmapMatrixView(freqSamplesPerFrame, historyFrames, (y, x) -> {
//            if (data == null)
//                return 0;
//
//            float kw = (data[y * freqSamplesPerFrame + x]);
//
//            return Draw.rgbInt(kw >= 0 ? kw : 0, kw < 0 ? -kw : 0, 0);
//        });

        //WaveAnalyzer waveAnalyzer = new WaveAnalyzer(this);


        Gridding v = new Gridding(
                new Gridding(
                        audioPlot,
                        audioPlot2
                )
//                freqHistory//,
                //waveAnalyzer.view()
        );

        if (source instanceof AudioSource)
            v.add(new FloatSlider(((AudioSource) source).gain));

        frame.on(() -> {
//            freqHistory.update();
            audioPlot.update();
            audioPlot2.update();

        });


        return v;
    }

//    interface Envelope {
//        float apply(int band, int frequency);
//    }
//

    public WaveCapture(WaveSource source) {

        setSource(source);


    }

    private void setSource(WaveSource source) {
        synchronized (this) {
            if (this.source != null) {
                this.source.stop();
                this.source = null;
            }

            this.source = source;

            if (this.source != null) {
                int audioBufferSize = this.source.start();

                bufferSamples = audioBufferSize;


                if (samples == null || samples.length != audioBufferSize) {
                    samples = new float[audioBufferSize]; //Util.largestPowerOf2NoGreaterThan(audioBufferSize)];
                    nextSamples = new float[audioBufferSize]; //Util.largestPowerOf2NoGreaterThan(audioBufferSize)];
                }
            }
        }
    }


    @Override
    public boolean next() {

        int read = source.next(nextSamples);
        System.arraycopy(samples, samples.length-read, samples, 0, read); //shift
        System.arraycopy(nextSamples, 0, samples, samples.length-read, read); //append

        frame.emit(this);

        if (!wave.isEmpty())
            wave.commit(new ArrayTensor(samples));

        return true;
    }


}
