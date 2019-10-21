package spacegraph.space2d.container.time;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.v2;
import jcog.pri.ScalarValue;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.wave1d.SignalInput;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.Fingering;
import spacegraph.input.finger.state.Dragging;
import spacegraph.input.finger.state.FingerMove;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.WaveBitmap;
import spacegraph.video.Draw;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static jcog.math.LongInterval.ETERNAL;

public class SignalView extends Gridding {

    static final int SELECT_BUTTON = 0;
    static final int PAN_BUTTON = 2;
    static final float PAN_SPEED = 1.0F / 100f;

    //    public final FloatRange gain = new FloatRange(1, 0, 100);
    static final float selectorAlpha = 0.5f;


    double time = (double) 0;

    boolean paused = false;

    public SignalView(SignalInput in) {
        super();


        FreqSpectrogram g = new FreqSpectrogram(128, 512);
        add(g);

        WaveBitmap w = new WaveBitmap(new ArrayTensor(in.data), (float) in.sampleRate, in.data.length, 250);
        add(w);

        var t = new Timeline2D() {
            final Fingering pan = new FingerMove(PAN_BUTTON) {
                @Override
                protected void move(float tx, float ty) {
                    timeShiftPct(tx * PAN_SPEED);
                }

                @Override
                public v2 pos(Finger finger) {
                    return finger.posRelative(SignalView.this);
                }
            };

            private volatile long selectStart = ETERNAL;
            private volatile long selectEnd = ETERNAL;




            final Fingering select = new Dragging(SELECT_BUTTON) {

                float sample(float x) {
                    return (x / w());
                }

                @Override
                protected boolean starting(Finger f) {
                    selectStart = t(f.posGlobal().x);
                    return true;
                }

                private long t(float f) {
                    return Util.lerpLong(sample(f), start, end);
                }

                @Override
                protected boolean drag(Finger f) {
                    selectEnd = t(f.posGlobal().x);
                    return true;
                }
            };

            @Override
            public Surface finger(Finger finger) {
                Surface x = super.finger(finger);
                if (x!=null)
                    return x;

                //TODO if ctrl pressed or something

                if (finger.pressedNow(2)) {
                    float wheel;
                    if ((wheel = finger.rotationY(true)) != (float) 0) {
                        timeScale((double) ((1f + wheel * 0.1f)));
                        //pan(+1);
                        return this;
                    }
                }

                if (finger.test(pan)) {
                    return this;
                }
                if (finger.test(select)) {
                    return this;
                }

                return null;
            }

            @Override
            protected void renderContent(ReSurface r) {
                super.renderContent(r);

                float sStart = (float) selectStart;
                if (sStart == sStart) {
                    float sEnd = (float) selectEnd;
                    if (sEnd == sEnd) {
                        r.on(new BiConsumer<GL2, ReSurface>() {
                            @Override
                            public void accept(GL2 gl, ReSurface rr) {
                                float ss = Util.clamp(x(selectStart), left(), right());
                                gl.glColor4f(1f, 0.8f, (float) 0, selectorAlpha);
                                float ee = Util.clamp(x(selectEnd), left(), right());
                                if (ee - ss > ScalarValue.EPSILON) {
                                    Draw.rect(x() + ss, y(), ee - ss, h(), gl);
                                }
                            }
                        });
                        //System.ouprintln("select: " + sStart + ".." + sEnd);
                    }
                }

                //super.compileAbove(r);
            }

        };
        t.addEvents(new Timeline2D.SimpleEventBuffer(), new Consumer<NodeVis<Timeline2D.SimpleEvent>>() {
            @Override
            public void accept(NodeVis<Timeline2D.SimpleEvent> nv) {
                nv.set(new PushButton(nv.id.toString()));
            }
        }, new Timeline2DEvents.LaneTimelineUpdater());
        add(t.withControls());

        in.wave.on(new Consumer<ArrayTensor>() {
            @Override
            public void accept(ArrayTensor raw) {
                w.update();
                g.set(raw);
            }
        });
    }



}

//
//        south(new Gridding(
////            new FloatSlider(gain, "Gain"),
//            PushButton.awesome("play", "Record Clip").clicking(()-> {
//                //window(new MetaFrame(new WaveView(a, 1f, 1024, 256)), 400, 400);
//            })
//        ));


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
//                float max = FloaNEGATIVE_INFINITY, min = FloaPOSITIVE_INFINITY;
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
//        audioPloaddAt(rawWave);

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

