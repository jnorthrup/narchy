package spacegraph.audio.modem.stopcollaboratelisten; /**
 * Copyright 2002 by the authors. All rights reserved.
 *
 * Author: Cristina V Lopes
 */

import javax.sound.sampled.TargetDataLine;
import java.io.IOException;

/**
 * This thread puts bytes from the microphone into the StreamDecoder's buffer.
 *
 * @author CVL
 */
public class MicrophoneListener implements Runnable {

    public static final String kThreadName = "MicrophoneListener";

    protected AudioUtils.AudioBuffer buffer = null;
    private Thread myThread = null;
    private final Object runLock = new Object();
    private boolean running = false;

    /**
     * NOTE: This spawns a thread to do the listening and then returns
     * @param _buffer the AudioBuffer into which to write the microphone input
     */
    public MicrophoneListener(AudioUtils.AudioBuffer _buffer) {
        buffer = _buffer;
        myThread = new Thread(this, kThreadName);
        myThread.start();
    }

    public void run() {
        synchronized(runLock){
            running = true;
        }

        try {
            /**
             * NOTE: we want buffSize large so that we don't loose samples when the
             * StreamDecoder thread kicks in. But we want to read a small number of
             * samples at a time, so that StreamDecoder can process them and they get
             * freed from the buffer as soon as possible.
             * So there's a fine balance going on here between the two threads, and
             * if it's not tuned, samples will be lost.
             */
            int buffSize = 32000;
            int buffSizeFraction = 8;
            TargetDataLine line = AudioUtils.getTargetDataLine(AudioUtils.kDefaultFormat);
            line.open(AudioUtils.kDefaultFormat, buffSize);

            //System.out.println(Thread.currentThread().getName() + "> bufferSize = " + line.getBufferSize());


            byte[] data = new byte[line.getBufferSize() / buffSizeFraction];

            listen(line, data);


        } catch (Exception e){
            System.out.println(e);
        }
    }

    private void listen(TargetDataLine line, byte[] data) throws IOException {
        int numBytesRead;
        line.start();
        while(running){
            numBytesRead =  line.read(data, 0, data.length);
            //		    System.out.println(Thread.currentThread().getName() + "> bytesRead = " + numBytesRead);
            buffer.write(data, 0, numBytesRead);
        }
        line.drain();
        line.stop();
        line.close();
    }

    public void stop(){
        synchronized(runLock){
            running = false;
        }
    }
}
