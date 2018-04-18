package spacegraph.audio;

import jcog.Util;
import jcog.event.ListTopic;
import jcog.event.Topic;
import jcog.exe.Loop;
import jcog.math.OneDHaar;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.meter.audio.WaveAnalyzer;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.video.Draw;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 10/28/15.
 */
public class WaveCapture extends Loop {

    private int bufferSamples;

    private float[] samples;

    public WaveSource source;

    /**
     * called when next sample (buffer) frame is ready
     */
    public final Topic<WaveCapture> frame = new ListTopic<>();


    @Deprecated private final int freqSamplesPerFrame = 8;
    @Deprecated private final int historyFrames = 16;
    /**
     * holds the normalized value of the latest data
     */
    @Deprecated public float[] dataNorm = new float[freqSamplesPerFrame];
    @Deprecated public float[] data = new float[historyFrames * freqSamplesPerFrame];

    //private final boolean normalizeDisplayedWave = false;

    public Surface view() {

        final Plot2D.Series rawWave, wavelet1d;

        rawWave = new Plot2D.Series("Audio", 1) {

            @Override
            public void update() {
                clear();

                float[] samples = WaveCapture.this.samples;
                if (samples == null) return;
                //samples[0] = null;


                int chans = WaveCapture.this.source.channelsPerSample();
                int bufferSamples = Math.min(WaveCapture.this.bufferSamples, samples.length / chans);
                switch (chans) {
                    case 1:
                        for (int i = 0; i < bufferSamples; i++)
                            add(samples[i]);
                        break;
                    case 2:
                        for (int i = 0; i < bufferSamples; )
                            add((samples[i++] + samples[i++]) / 2f); //to mono
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }


                //minValue = -0.5f;
                //maxValue = 0.5f;

//                if (normalizeDisplayedWave) {
//                    autorange();
//                } else {
//                    minValue = -1;
//                    maxValue = +1;
//                }

//                final FloatArrayList history = this.history;
//
//                for (int i = 0; i < nSamplesRead; i++) {
//                    history.add((float) samples[i]);
//                }
//
//                while (history.size() > maxHistory)
//                    history.removeAtIndex(0);

//                                        minValue = Float.POSITIVE_INFINITY;
//                                        maxValue = Float.NEGATIVE_INFINITY;
//
//                                        history.forEach(v -> {
//                                            if (Double.isFinite(v)) {
//                                                if (v < minValue) minValue = v;
//                                                if (v > maxValue) maxValue = v;
//                                            }
//                                            //mean += v;
//                                        });
            }

        };

        wavelet1d = new Plot2D.Series("Wavelet", 1) {

            final float[] transformedSamples = new float[Util.largestPowerOf2NoGreaterThan(bufferSamples)];
            final AtomicBoolean busy = new AtomicBoolean();

            {
                frame.on((w) -> {
                    if (!busy.compareAndSet(false, true))
                        return;


                    FloatArrayList history = this;

                    //                        for (short s : ss) {
                    //                            history.add((float)s);
                    //                        }
                    //
                    //
                    //                        while (history.size() > maxHistory)
                    //                            history.removeAtIndex(0);
                    //
                    //                        while (history.size() < maxHistory)
                    //                            history.add(0);


                    final int bufferSamples = Math.min(samples.length, WaveCapture.this.bufferSamples);

                    float[] ss = transformedSamples;
                    //1d haar wavelet transform
                    //OneDHaar.displayOrderedFreqsFromInPlaceHaar(x);
                    System.arraycopy(samples, 0, ss, 0, bufferSamples); //the remainder will be zero
                    OneDHaar.inPlaceFastHaarWaveletTransform(ss);
                    sampleFrequency(ss);
                    //OneDHaar.displayOrderedFreqsFromInPlaceHaar(samples, System.out);

//                //apache commons math - discrete cosine transform
//                {
//                    double[] dsamples = new double[samples.length + 1];
//                    for (int i = 0; i < samples.length; i++)
//                        dsamples[i] = samples[i];
//                    dsamples = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I).transform(dsamples, TransformType.FORWARD);
//                    for (int i = 0; i < samples.length; i++)
//                        samples[i] = (float) dsamples[i];
//                }

                    history.clear();
                    for (int i = 0; i < bufferSamples; i++)
                        history.addAll(ss[i]);

//                minValue = Short.MIN_VALUE;
//                maxValue = Short.MAX_VALUE;

//                if (normalizeDisplayedWave) {
//                    minValue = Float.POSITIVE_INFINITY;
//                    maxValue = Float.NEGATIVE_INFINITY;
//
//                    history.forEach(v -> {
//                        //if (Float.isFinite(v)) {
//                        if (v < minValue) minValue = v;
//                        if (v > maxValue) maxValue = v;
//                        //}
//                        //mean += v;
//                    });
//                } else {
//                    minValue = -1f;
//                    maxValue = 1f;
//                }

                    //System.out.println(maxHistory + " " + start + " " + end + ": " + minValue + " " + maxValue);

                    busy.set(false);

                });
            }


            private void sampleFrequency(float[] freqSamples) {
                int lastFrameIdx = data.length - freqSamplesPerFrame;

                int samples = freqSamples.length;

                float bandWidth = ((float) samples) / freqSamplesPerFrame;
                float sensitivity = 1f;

                final Envelope uniform = (i, k) -> {
                    float centerFreq = (0.5f + i) * bandWidth;
                    return 1f / (1f + Math.abs(k - centerFreq) / (bandWidth / sensitivity));
                };

                System.arraycopy(data, 0, data, freqSamplesPerFrame, lastFrameIdx);

                //TODO double-buffer these buffers
                float[] data = WaveCapture.this.data;

//                int f = freqOffset;
//                int freqSkip = 1;
//                for (int i = 0; i < freqSamplesPerFrame; i++) {
//                    h[n++] = freqSamples[f];
//                    f+=freqSkip*2;
//                }
                float max = Float.NEGATIVE_INFINITY, min = Float.POSITIVE_INFINITY;
                for (int i = 0; i < freqSamplesPerFrame; i++) {

                    float s = 0;
                    for (int k = 0; k < samples; k++) {
                        float fk = freqSamples[k];
                        s += uniform.apply(i, k) * fk;
                    }
                    if (s > max)
                        max = s;
                    if (s < min)
                        min = s;

                    data[i] = s;
                }

                if (max != min) { //TODO epsilon check
                    float range = max - min;
                    for (int i = 0; i < freqSamplesPerFrame; i++)
                        dataNorm[i] = (WaveCapture.this.data[i] - min) / range;
                }

                //System.arraycopy(freqSamples, 0, history, 0, freqSamplesPerFrame);
            }

        };

        rawWave.range(-1, +1);
        wavelet1d.range(-1, +1);


        Plot2D audioPlot = new Plot2D(bufferSamples, Plot2D.Line);//, bufferSamples, 450, 60);
        audioPlot.add(rawWave);

        Plot2D audioPlot2 = new Plot2D(bufferSamples, Plot2D.Line);
        audioPlot2.add(wavelet1d);

        BitmapMatrixView freqHistory = new BitmapMatrixView(freqSamplesPerFrame, historyFrames, (x, y) -> {
            if (data == null)
                return 0; //HACK
            float kw = (data[y * freqSamplesPerFrame + x]);
            //int kw = (int)(v*255);
            return Draw.rgbInt(kw >= 0 ? kw : 0, kw < 0 ? -kw : 0, 0);
        });

        WaveAnalyzer waveAnalyzer = new WaveAnalyzer(this);


        Gridding v = new Gridding(
                new Gridding(
                    audioPlot,
                    audioPlot2
                ),
                freqHistory,
                waveAnalyzer.view()
        );

        if (source instanceof AudioSource)
            v.add(new FloatSlider(((AudioSource)source).gain));

        frame.on(() -> {
            freqHistory.update();
            audioPlot.update();
            audioPlot2.update();
            //wav2.update();
        });


//
//        //noinspection OverlyComplexAnonymousInnerClass
//        ChangeListener onParentChange = new ChangeListener() {
//
//            public On observe;
//
//            @Override
//            public void changed(ObservableValue observableValue, Object o, Object t1) {
//
//                if (t1 == null) {
//                    if (observe != null) {
//                        //System.out.println("stopping view");
//                        this.observe.off();
//                        this.observe = null;
//                    }
//                } else {
//                    if (observe == null) {
//                        //System.out.println("starting view");
//                        observe = nextReady.on(u);
//                    }
//                }
//            }
//        };

        return v;
    }

    interface Envelope {
        float apply(int band, int frequency);
    }


    public WaveCapture(WaveSource source) {

        setSource(source);


        //                double nextDouble[] = new double[1];
        //                DoubleSupplier waveSupplier = () -> {
        //                    return nextDouble[0];
        //                };
    }

    public final void setSource(WaveSource source) {
        synchronized (this) {
            if (this.source != null) {
                this.source.stop();
                this.source = null;
            }

            this.source = source;

            if (this.source != null) {
                int audioBufferSize = this.source.start();

                bufferSamples = audioBufferSize;

                //System.out.println("bufferSamples=" + bufferSamples + ", sampleRate=" + sampleRate + ", numChannels=" + numChannels);

                if (samples == null || samples.length != audioBufferSize)
                    samples = new float[Util.largestPowerOf2NoGreaterThan(audioBufferSize)];
            }
        }
    }

    @Override
    public boolean next() {

        source.next(samples);

        frame.emit(this);

        return true;
    }


}
