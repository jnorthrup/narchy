package nars;

import jcog.exe.Loop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.meta.OmniBox;

import static spacegraph.SpaceGraph.window;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);

    public static void main(String[] args) {

        NAR nar = NARchy.ui();


        Loop loop = nar.startFPS(10f); //10hz alpha
        ((NARLoop) loop).throttle.set(0.1f);

        window(new AutoSurface<>(nar), 700, 600);

        window(new OmniBox(), 600, 200);

        try {

            //1. try to open a Spacegraph openGL window
            logger.info("Starting Spacegraph UI");

            //            window(new ConsoleTerminal(new TextUI(nar).session(8f)) {
            //                {
            //                    Util.pause(50); term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw
            //                }
            //            }, 800, 600);


        } catch (Throwable t) {
            //2. if that fails:
            logger.info("Fallback to Terminal UI");
            new Shell(nar);
        }


    }

}
