package nars;

import nars.gui.NARui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spacegraph.SpaceGraph.window;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);


    public static void main(String[] args) {

        NAR nar = NARchy.ui();


        logger.info("start SpaceGraph UI");


        window(NARui.top(nar), 1024, 800).io.eventClosed.on(nar::reset);

        nar.startFPS(10f);

//            window(new Gridding(
//                    List.of("")
//            ), 500, 500);


    }

//    static void wall(NAR nar) {
//        GraphEdit w = SpaceGraph.wall(800, 600);
//        w.frame(new ServicesTable(nar.services), 5, 4);
//        w.frame(new OmniBox(new LuceneQueryModel()), 6, 1);
//        w.frame(NARui.top(nar), 4, 4);
//    }

}
