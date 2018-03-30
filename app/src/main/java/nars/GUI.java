package nars;

import jcog.exe.Loop;
import nars.gui.Vis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.meta.OmniBox;
import spacegraph.space2d.widget.windo.PhyWall;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);

    public static void main(String[] args) {

        NAR nar = NARchy.ui();


        Loop loop = nar.startFPS(10f); //10hz alpha
        //((NARLoop) loop).throttle.set(0.1f);


        //1. try to open a Spacegraph openGL window
        logger.info("start SpaceGraph UI");

        //            window(new ConsoleTerminal(new TextUI(nar).session(8f)) {
        //                {
        //                    Util.pause(50); term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw
        //                }
        //            }, 800, 600);


        //window(new AutoSurface<>(nar), 700, 600);


        PhyWall w = SpaceGraph.wall(800, 600);
        w.put(new Gridding(new OmniBox()), 6, 1);
        //w.put(new AutoSurface<>(nar.services), 4,4);
        w.put(Vis.top(nar), 4,4);


    }

}
