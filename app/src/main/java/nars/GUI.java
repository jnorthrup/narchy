package nars;

import nars.gui.Vis;
import nars.op.nlp.Hear;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.widget.console.TextEdit;
import spacegraph.widget.windo.Widget;

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


        nar.startFPS(10f); //10hz alpha


        window(
                Vis.reflect(nar), 700, 600
        );

        window(new OmniBox(nar), 600, 200);

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

    /** super repl */
    public static class OmniBox extends Widget {

        final TextEdit edit;
        private final NAR nar;

        public OmniBox(NAR n) {
            super();

            this.nar = n;
            add((edit =new TextEdit(40, 4) {
                @Override
                protected void onKeyCtrlEnter() {
                    String t = text();
                    in(t);
                    clear();
                }
            }).surface());
        }

        protected void in(String s) {
            Hear.hear(nar, s, "omnibox");
        }

    }

}
