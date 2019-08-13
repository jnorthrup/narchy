package nars;

import nars.attention.TaskLinkWhat;
import nars.gui.ConceptListView;
import nars.gui.NARui;
import nars.gui.TaskListView;
import nars.gui.graph.run.BagregateConceptGraph2D;
import nars.link.TaskLinks;
import nars.op.kif.KIF;
import nars.task.util.PriBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.chip.ReplChip;

import static nars.$.$$;
import static spacegraph.SpaceGraph.window;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);


    public static void main(String[] args) {

        NAR n = NARchy.ui();

        logger.info("start SpaceGraph UI");

        SpaceGraph.window(NARui.top(n), 200, 400).video.eventClosed.on(n::reset);

        n.startFPS(10f);

        demo(n);

        window(BagregateConceptGraph2D.get(n), 1200, 800);
        window(new TaskListView(n.what(), 32), 1200, 800);
        window(new ConceptListView(n.what(), 32), 1200, 800);

        window(new ReplChip((cmd, receive) -> {
            try {
                n.input(cmd);
            } catch (Narsese.NarseseException e) {
                receive.accept(e.toString());
            }
        }), 800, 200);
    }

    public static void demo(NAR n) {
        try {

                try {
                    TaskLinkWhat w = n.fork(new TaskLinkWhat($$("demo"), new TaskLinks(1024),
                        new PriBuffer.DirectTaskBuffer<>()));

                    n.input(KIF.file("/home/me/sumo/FinancialOntology.kif"));
                    n.input(
                        "$1.0 possesses(I,#everything)!"
                    );
                    n.input(
                        "$1.0 (possesses(I,#x) && (#x <-> UnitedStatesDollar))!"
                    );
                } catch (Throwable e) {
                    e.printStackTrace();
                }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    //    static void wall(NAR nar) {
//        GraphEdit w = SpaceGraph.wall(800, 600);
//        w.frame(new ServicesTable(nar.services), 5, 4);
//        w.frame(new OmniBox(new LuceneQueryModel()), 6, 1);
//        w.frame(NARui.top(nar), 4, 4);
//    }

}
