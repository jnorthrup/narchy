package nars;

import jcog.exe.Loop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);

    public static void main(String[] args) {

        NAR nar = NARchy.ui();


        Loop loop = nar.startFPS(10f); //10hz alpha
        ((NARLoop) loop).throttle.set(0.1f);



        try {

            //1. try to open a Spacegraph openGL window
            logger.info("Starting Spacegraph UI");

            //            window(new ConsoleTerminal(new TextUI(nar).session(8f)) {
            //                {
            //                    Util.pause(50); term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw
            //                }
            //            }, 800, 600);

            new Shell(nar);


        } catch (Throwable t) {
            //2. if that fails:
            logger.info("Fallback to Terminal UI");
        }


    }

}
