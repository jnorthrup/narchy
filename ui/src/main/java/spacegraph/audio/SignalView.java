package spacegraph.audio;

import jcog.math.FloatRange;
import jcog.signal.Tensor;
import jcog.signal.wave1d.FreqDomain;
import jcog.signal.wave1d.SignalReading;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.Spectrogram;
import spacegraph.space2d.widget.meter.WaveView;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.video.Draw;

public class SignalView extends Bordering {
    private final SignalReading audio;

    public final FloatRange gain = new FloatRange(1, 0, 100);

    public SignalView(SignalReading a) {
        super();
        this.audio = a;


        south(new Gridding(
            new FloatSlider(gain, "Gain"),
            PushButton.awesome("play", "Record Clip").clicking(()-> {
                //window(new MetaFrame(new WaveView(a, 1f, 1024, 256)), 400, 400);
            })
        ));



        Timeline2D t = new Timeline2D(0, 1);


        FreqDomain freqDomain = new FreqDomain(
                a,
                1024, 256);


        Spectrogram g = new Spectrogram(true, 256, 2048);
        audio.wave.on(raw-> {
            Tensor fft = freqDomain.next(raw);
            g.next(i -> {
                float v = fft.getAt(i);
                return Draw.colorHSB(0.3f * (1 - v), 0.9f, v);
            });
        });
        t.add(g);

        WaveView w = new WaveView(a, 500, 250);
        audio.wave.on(raw->{
            w.updateLive();
        });
        t.add(w);


        Timeline2D.SimpleTimelineModel tl = new Timeline2D.SimpleTimelineModel();
        t.addEvents(tl, (nv)->{
            nv.set(new PushButton(nv.id.toString()));
        });
        audio.wave.on(raw->{
            if(Math.random() < 0.1f) {
                tl.clear();
                //Math.round(t.tEnd),Math.round(t.tEnd)
                tl.add(new Timeline2D.SimpleEvent("event", 0, 1));
                t.setTime(-1, 2, true); //HACK force update
            }
        });

        center(t.withControls());


    }

}
//        rawWave = new Plot2D.ArraySeries("Audio", 1);
//        a.wave.on(x -> {
//
//            float[] samples = x.data;
//            int chans = a.source().channelsPerSample();
//            int bufferSamples = samples.length / chans;
//
//            //HACK use a CircularBuffer
//            rawWave.clear();
//            switch (chans) {
//                case 1:
//                    for (int i = 0; i < bufferSamples; i++)
//                        rawWave.addAt(samples[i]);
//                    break;
//                case 2:
//                    for (int i = 0; i < bufferSamples; )
//                        rawWave.addAt((samples[i++] + samples[i++]) / 2f); //HACK
//                    break;
//                default:
//                    throw new UnsupportedOperationException();
//            }
//        });
//        rawWave = new Plot2D.Series("Audio", 1) {
//
//            @Override
//            public void update() {
//
//
//            }
//
//        };

//rawWave.range(-1, +1);

//        int fftSize = 256;
//        SlidingDFTTensor freqDomain =
//                //new HaarWaveletTensor(wave, bufferSamples);
//                new SlidingDFTTensor(a.wave, fftSize);
//
//        wavelet1d = new Plot2D.ArraySeries("spectrum", fftSize) {
//            final AtomicBoolean busy = new AtomicBoolean();
//
//            {
//                size = fftSize + 2;
//                items = new float[size];
//                a.wave.on(x -> {
//                    if (busy.compareAndSet(false, true)) {
//                        try {
//                            freqDomain.update();
//                            freqDomain.forEach(this::setAt);
//                            autorange();
//                        } finally {
//                            busy.setAt(false);
//                        }
//                    }
//                });
//            }
//        };


//wavelet1d.range(-1, +1);


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
//                    busy.setAt(false);
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



//        audioPlot = new Plot2D(bufferSamples,
//                //new Plot2D.BitmapWave(512, 256)
//                Plot2D.Line
//        );
//        audioPlot.addAt(rawWave);

//            Plot2D audioPlot2 = new Plot2D(bufferSamples,
//                    new Plot2D.BitmapPlot(1024, 256)
//                    //Plot2D.Line
//            );
//
//            audioPlot2.addAt(wavelet1d);

//        BitmapMatrixView freqHistory = new BitmapMatrixView(freqSamplesPerFrame, historyFrames, (y, x) -> {
//            if (data == null)
//                return 0;
//
//            float kw = (data[y * freqSamplesPerFrame + x]);
//
//            return Draw.rgbInt(kw >= 0 ? kw : 0, kw < 0 ? -kw : 0, 0);
//        });

//WaveAnalyzer waveAnalyzer = new WaveAnalyzer(this);

