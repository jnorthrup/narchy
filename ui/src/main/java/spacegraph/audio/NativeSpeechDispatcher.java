package spacegraph.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/** 'speechd' speech dispatcher - executes via command line */
public class NativeSpeechDispatcher {

    static final Logger logger = LoggerFactory.getLogger(NativeSpeechDispatcher.class);

    //static final int MAX_POLYPHONY = 8;
    //final Semaphore polyphony = new Semaphore(MAX_POLYPHONY, true);
    //final BlockingQueue<Object> q = new ArrayBlockingQueue(MAX_POLYPHONY);

    public NativeSpeechDispatcher() {
    }

    public String[] command(String s) {
        return new String[]{
            //"/usr/bin/spd-say", "\"" + s + "\"" //speech-dispatcher -- buffers messages and does not allow multiple voices
            "/usr/bin/espeak-ng", "\"" + s + "\"" //espeak-ng (next generation) -- directly synthesize on command
        };
    }

    public void speak(Object x) {
        String s = x.toString();
        try {
//                try {
//                    if (q.offer)
//                    if (polyphony.tryAcquire(1, TimeUnit.SECONDS)) {

                    //TODO semaphore to limit # of simultaneous voices
                    Process p = new ProcessBuilder()
                            .command(command(s))
                            .start();
                    p.onExit().handle((z, y) -> {
                        //System.out.println("done: " + z);
                        //polyphony.release();
                        return null;
                    }).exceptionally(t->{
                        logger.warn("speech error: {} {}", s, t);
                        //polyphony.release();
                        return null;
                    });
//                    } else {
//                        logger.warn("insufficient speech polyphony, ignored: {}", s);
//                    }

        } catch (IOException e) {
            logger.warn("speech error: {} {}", s, e);
        }

    }

}
