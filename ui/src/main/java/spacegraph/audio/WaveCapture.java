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
                            add((samples[i++] + samples[i++]) / 2f); 
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }


                
                



























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

                    
                    
                    
                    
                    
                    
                    
                    
                    
                    


                    final int bufferSamples = Math.min(samples.length, WaveCapture.this.bufferSamples);

                    float[] ss = transformedSamples;
                    
                    
                    System.arraycopy(samples, 0, ss, 0, bufferSamples); 
                    OneDHaar.inPlaceFastHaarWaveletTransform(ss);
                    sampleFrequency(ss);
                    











                    history.clear();
                    for (int i = 0; i < bufferSamples; i++)
                        history.addAll(ss[i]);




















                    

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

                
                float[] data = WaveCapture.this.data;







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

                if (max != min) { 
                    float range = max - min;
                    for (int i = 0; i < freqSamplesPerFrame; i++)
                        dataNorm[i] = (WaveCapture.this.data[i] - min) / range;
                }

                
            }

        };

        rawWave.range(-1, +1);
        wavelet1d.range(-1, +1);


        Plot2D audioPlot = new Plot2D(bufferSamples, Plot2D.Line);
        audioPlot.add(rawWave);

        Plot2D audioPlot2 = new Plot2D(bufferSamples, Plot2D.Line);
        audioPlot2.add(wavelet1d);

        BitmapMatrixView freqHistory = new BitmapMatrixView(freqSamplesPerFrame, historyFrames, (x, y) -> {
            if (data == null)
                return 0; 
            float kw = (data[y * freqSamplesPerFrame + x]);
            
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
            
        });


























        return v;
    }

    interface Envelope {
        float apply(int band, int frequency);
    }


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
