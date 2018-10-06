package nars;

import jcog.User;
import jcog.data.list.FasterList;
import nars.gui.NARui;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.OmniBox;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static spacegraph.SpaceGraph.window;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);


    public static void main(String[] args) {

        NAR nar = NARchy.ui();


        logger.info("start SpaceGraph UI");


        window(NARui.top(nar), 1024, 800).eventClosed.on(() -> {
            nar.reset();
        });

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
