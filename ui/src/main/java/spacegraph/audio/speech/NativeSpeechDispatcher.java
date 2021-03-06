package spacegraph.audio.speech;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

/** 'speechd' speech dispatcher - executes via command line */
public class NativeSpeechDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(NativeSpeechDispatcher.class);

    
    
    

    public NativeSpeechDispatcher() {
    }

    private static String[] command(String s) {
        return new String[]{
            
            "/usr/bin/espeak-ng", '"' + s + '"' 
        };
    }

    private static String stringify(Object x) {
        if (x instanceof Object[]) {
            return Joiner.on(" ").join((Object[])x);
        } else {
            return x.toString();
        }
    }

    public static void speak(Object x) {
        String s = stringify(x);
        speak(s);
    }

    public static void speak(String s) {
        try {


            Process p = new ProcessBuilder()
                            .command(command(s))
                            .start();
                    p.onExit().handle(new BiFunction<Process, Throwable, Object>() {
                        @Override
                        public Object apply(Process z, Throwable y) {
                            return null;
                        }
                    }).exceptionally(new Function<Throwable, Object>() {
                        @Override
                        public Object apply(Throwable t) {
                            logger.warn("speech error: {} {}", s, t);

                            return null;
                        }
                    });




        } catch (IOException e) {
            logger.warn("speech error: {} {}", s, e);
        }
    }

}
