package spacegraph.space2d.widget.meter.audio;

/**
 **   __ __|_  ___________________________________________________________________________  ___|__ __
 **  //    /\                                           _                                  /\    \\  
 ** //____/  \__     __ _____ _____ _____ _____ _____  | |     __ _____ _____ __        __/  \____\\ 
 **  \    \  / /  __|  |     |   __|  _  |     |  _  | | |  __|  |     |   __|  |      /\ \  /    /  
 **   \____\/_/  |  |  |  |  |  |  |     | | | |   __| | | |  |  |  |  |  |  |  |__   "  \_\/____/   
 **  /\    \     |_____|_____|_____|__|__|_|_|_|__|    | | |_____|_____|_____|_____|  _  /    /\     
 ** /  \____\                       http://jogamp.org  |_|                              /____/  \    
 ** \  /   "' _________________________________________________________________________ `"   \  /    
 **  \/____.                                                                             .____\/     
 **
 ** Main class for the music subsystem of the framework. Provides an easy to use interface for
 ** asynchronous music playback. Given the 3rd party service provider libraries are supplied it
 ** is capable to playback "ogg vorbis" and "mpeg layer 3" music files. Other than simple 
 ** playback it internally uses the "KJ-DSS Project" by Kristofer Fudalewski (http://sirk.sytes.net)
 ** to provide a joined FFT spectrum via getFFTSpectrum() and a graphical scope and spectrum 
 ** analyzer via getScopeAndSpectrumAnalyzerVisualization(). The FFT spectrum can be utilized to
 ** get some easy synchronization of music an visuals.
 **
 **/

import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.video.Tex;

import java.awt.image.BufferedImage;

public class WaveAnalyzer {

    private BaseMusic_ScopeAndSpectrumAnalyzer analyzer;
    private BaseMusic_DigitalSignalSynchronizer synchronizer;
    private final float[] mFFTSpectrum_Empty;

    int fps = 30;

    public WaveAnalyzer(WaveCapture src) {

        mFFTSpectrum_Empty = new float[BaseMusic_ScopeAndSpectrumAnalyzer.DEFAULT_SPECTRUM_ANALYSER_BAND_COUNT];


            synchronizer = new BaseMusic_DigitalSignalSynchronizer(fps);

            synchronizer.add(analyzer = new BaseMusic_ScopeAndSpectrumAnalyzer());

            synchronizer.start((AudioSource)(src.source));

            src.frame.on(f->{
                AudioSource in = (AudioSource) src.source;
                synchronizer.writeAudioData(in.audioBytes, 0, in.audioBytesRead );
                //mPosition = tLine.getMicrosecondPosition();
                synch();
            });

    }

//    private SourceDataLine getLine(AudioFormat inAudioFormat) throws LineUnavailableException {
//        SourceDataLine tSourceDataLine = null;
//        DataLine.Info tDataLineInfo = new DataLine.Info(SourceDataLine.class,inAudioFormat,4096);
//        tSourceDataLine = (SourceDataLine)AudioSystem.getLine(tDataLineInfo);
//        tSourceDataLine.open(inAudioFormat);
//        return tSourceDataLine;
//    }

    void synch() {
        
            BaseMusic_DigitalSignalSynchronizer.Synchronizer tSynchronizer = synchronizer.getInternalSynchronizer();
            if (tSynchronizer!=null) {
                tSynchronizer.synchronize();
            }
        
    }


//    public int getPositionInMicroseconds() {
//        return (int)mPosition;
//    }
//
//    public int getPositionInMilliseconds() {
//        return getPositionInMicroseconds()/1000;
//    }

    public float[] getFFTSpectrum() {
        float[] tFFTSpectrum = analyzer.getFFTSpectrum();
        if (tFFTSpectrum!=null) {
            return tFFTSpectrum;
        }
        return mFFTSpectrum_Empty;
    }

    public BufferedImage getScopeAndSpectrumAnalyzerVisualization() {
        return analyzer.getScopeAndSpectrumAnalyzerVisualization();
    }

    public Surface view() {
        return new Gridding() {

            final Tex tex = new Tex();

            {
                set(tex.view());
            }


            @Override
            public void prePaint(int dtMS) {
                tex.update(analyzer.getScopeAndSpectrumAnalyzerVisualization());
            }
        };
    }
}
