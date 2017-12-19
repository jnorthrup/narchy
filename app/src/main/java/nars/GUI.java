package nars;

import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import nars.gui.Vis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.Surface;

import java.io.IOException;

import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.grid;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);

    public static void main(String[] args) {

        NAR nar = NARchy.ui();

//        try {
//            //new NoteFS("/tmp/nal", nar);
//
//            InterNAR i = new InterNAR(nar, 8, 0);
//            i.recv.preAmp(0.1f);
//            i.runFPS(2);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }




        nar.runLater(()-> {
            window(
                    Vis.reflect(nar), 700, 600
            );

            window(OmniBox.get(nar), 600, 200);

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

        });

    }

    static class OmniBox {

        static Surface get(NAR n) {
            return grid(Vis.inputEditor()
                    /*Vis.audioCapture()*/);
        }
    }

}
