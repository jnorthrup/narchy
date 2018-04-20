package nars;

import jcog.exe.Loop;
import nars.gui.Vis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.OmniBox;
import spacegraph.space2d.widget.meta.ServicesTable;
import spacegraph.space2d.widget.windo.Dyn2DSurface;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);


    public static void main(String[] args) {
        Dyn2DSurface w = SpaceGraph.wall(800, 600);

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




        Loop.invokeLater(()->{
            //((ZoomOrtho) w.root()).scaleMin = 100f;
            //((ZoomOrtho) w.root()).scaleMax = 1500;

            w.put(new ServicesTable(nar.services), 5,4);
            w.put(new Gridding(new OmniBox()), 6, 1);
            w.put(Vis.top(nar), 4,4);
        });

        //nar.inputNarsese(new FileInputStream("/home/me/d/sumo_merge.nal"));


    }

}
