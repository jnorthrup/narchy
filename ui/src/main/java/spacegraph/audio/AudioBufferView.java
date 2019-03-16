package spacegraph.audio;

import jcog.event.Off;
import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.wave1d.FreqDomain;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.WaveView;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.video.Draw;

import static spacegraph.SpaceGraph.window;

public class AudioBufferView extends Gridding {
    private final AudioBuffer audio;

    public AudioBufferView(AudioBuffer a) {
        super();
        this.audio = a;


        set(
                new FloatSlider(a.source().gain),

                spectrogram(a, 0.05f, 256, 32),


                new WaveView(a.buffer, 1024, 256) {
                    private final Off off;

                    {
                        this.off = a.frame.on((Runnable)this::updateLive);
                    }

                    @Override
                    protected void stopping() {
                        off.off();
                        super.stopping();
                    }


//                    /** TODO use updateLive */
//                    @Override public void update() {
//                        long width = vis.end - vis.start;
//                        vis.end = a.buffer.viewPtr; //getPeekPosition();
//                        vis.start = a.buffer.idx((int) (vis.end - (width)));
//                        super.update();
//                    }


                },
                new PushButton("Record Clip", () -> {
                    window(new MetaFrame(new WaveView(a, 1f, 1024, 256)), 400, 400);
                })

        );

    }


    public static Surface spectrogram(AudioBuffer capture, float sampleTime, int fftSize, int history) {

        CircularFloatBuffer buffer = capture.buffer;

        FreqDomain s = new FreqDomain(buffer,
                sampleTime,
                capture.source().samplesPerSecond(), fftSize, history);

        int stride = s.freq.segment;
        BitmapMatrixView bmp = new BitmapMatrixView(
                (int) Math.floor(((float) s.freq.volume()) / stride), stride, (x, y) -> {
            float v =
                    s.freq.get(x, y);
            //t.data[y * stride + x];
            //return Draw.colorBipolar(v);
            //v = unitize(v);
            return Draw.colorHSB(0.3f * (1-v),0.9f,v);
        });
        //bmp.bmp.mipmap(true);

        return new Gridding(bmp) {


            protected void update() {
                if (s.update())
                    bmp.updateIfShowing();
            }

            private Off off;

            @Override
            protected void starting() {
                super.starting();
                this.off = capture.frame.on(this::update);
            }

            @Override
            public void stopping() {
                off.off();
                off = null;
                super.stopping();
            }

        };
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

