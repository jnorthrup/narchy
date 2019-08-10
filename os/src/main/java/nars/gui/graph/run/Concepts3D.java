package nars.gui.graph.run;

import jcog.pri.bag.util.Bagregate;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.gui.graph.DynamicConceptSpace;
import spacegraph.space3d.SpaceGraph3D;

/** TODO get working again */
public class Concepts3D extends DynamicConceptSpace {

//    private final AtomicBoolean reloadReady = new AtomicBoolean(false);
//    private final TextEdit inputbox;

    private Concepts3D(NAR nar, int visibleNodes, int maxEdgesPerNodeMax) {
        this(nar, () -> nar.what().concepts().iterator(),
                visibleNodes, maxEdgesPerNodeMax);

    }

    private Concepts3D(NAR nar, Iterable<Concept> concepts, int maxNodes, int maxEdgesPerNodeMax) {
        super(nar, concepts, maxNodes, maxEdgesPerNodeMax);

        SpaceGraph3D sg = show(1400, 1000, false);


//        sg.addAt(new SubOrtho(grid(
//                new ObjectSurface<>(sg.dyn.broadConstraints.get(0) /* FD hack */),
//                new ObjectSurface<>(vis)
//        )).posWindow(0.5f, 0.1f, 1f, 0.2f));


        Bagregate<Concept> cpts = this.concepts;
        /*inputbox = */
        //nar.reset();
        //                        try {
        //
        //                        } catch (Narsese.NarseseException e) {
        //                            e.printStackTrace();
        //                        }
//        Surface inputPanel =
//                new TextEdit0((/*inputbox = */new TextEdit0.TextEditUI() {
//                    @Override
//                    protected void onKeyCtrlEnter() {
//
//                        nar.runLater(() -> {
//
//                            //nar.reset();
//                            nar.clear();
//
//                            cpts.clear();
//
//                            Concepts3D.this.clear();
//
//                            try {
//                                nar.input(text());
//                            } catch (Narsese.NarseseException e) {
//                                e.printStackTrace();
//                            }
//                        });
////                        try {
////
////                        } catch (Narsese.NarseseException e) {
////                            e.printStackTrace();
////                        }
//                    }
//
//                }));
//        sg.addAt(new SubOrtho(grid(
//                inputPanel,
//                NARui.top(nar)
//        )).posWindow(0.5f, 0.95f, 1f, 0.1f));

//        ((ConsoleGUI) inputPanel).resize(25, 2);

//
//
//        sg.addAt(new SubOrtho(
//                inputPanel
//        ).posWindow(0, 0.8f, 0.2f, 0.6f));

        float fps = 16;
        nar.startFPS(fps);

    }




    public static void main(String[] args) {


        NAR n = NARS.threadSafe();
        new Deriver(Derivers.nal(n, 1, 8));




        Concepts3D g = new Concepts3D(n,
                /* TODO */ 128, 5);

        try {
            n.input("a:b.");
            n.input("b:c.");
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }

    }


}
