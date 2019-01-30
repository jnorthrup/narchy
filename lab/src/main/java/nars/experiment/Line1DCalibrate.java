//package nars.experiment;
//
//import jcog.Util;
//import jcog.math.FloatSupplier;
//import nars.$;
//import nars.NAR;
//import nars.NARS;
//import nars.task.DerivedTask;
//import nars.test.agent.Line1DSimplest;
//import nars.truth.PreciseTruth;
//import spacegraph.space2d.container.grid.Gridding;
//import spacegraph.space2d.widget.meter.Plot2D;
//
//import java.util.Collection;
//import java.util.List;
//
//import static java.lang.Math.PI;
//import static jcog.Texts.n2;
//import static jcog.Texts.n4;
//import static spacegraph.space2d.container.grid.Gridding.VERTICAL;
//
//public class Line1DCalibrate {
//
//
//    public static void main(String[] args) {
//
//
//        NAR n = NARS.threadSafe();
//
//
//        n.time.dur(1);
//        n.termVolumeMax.set(16);
//
//
//
//
//
//
//
//
//        Line1DSimplest a = new Line1DSimplest() {
//
//
//
//            @Override
//            protected float act() {
//
//                float r = super.act();
//                System.out.println("reward: " + now + "\t^" + n2(i.floatValue()) + "\t@" + n2(o.floatValue()) + "\t\t= " + r);
//                return r;
//            }
//        };
//
//        float tHz = 0.05f;
//        float yResolution = 0.1f;
//        float periods = 16;
//
//
//
//        Collection actions = a.actions.values();
//        n.onTask(t -> {
//            if (t instanceof DerivedTask) {
//                if (t.isGoal()) {
//                    if (actions.contains(t.target())) {
//
//                        float dir = new PreciseTruth(t.freq(), t.evi(a.nar().time(), a.nar().dur()), false).freq() - 0.5f;
//
//
//                        float i = a.i.floatValue();
//                        float o = a.o.floatValue();
//                        float neededDir = (i - o);
//                        boolean good = Math.signum(neededDir) == Math.signum(dir);
//                        /*if (!good)*/
//                        System.err.println(n4(dir) + "\t" + good + " " + i + " <-? " + o);
//                        System.err.println(t.proof());
//                        System.out.println();
//                    }
//                    if (t.isGoal())
//                        System.err.println(t.proof());
//
//                } else {
//
//
//                }
//            }
//        });
//
//        a.speed.set(yResolution);
//
//
//
//        a.in.resolution(yResolution);
//        a.curiosity.set(
//                0.1f
//
//        );
//
//
//
//
//
//
//
//
//
//
//
//
//        a.onFrame((z) -> {
//
//            a.target(
//
//                    Util.round((float) (0.5f + 0.5f * Math.sin(a.nar().time() * tHz * 2 * PI)), yResolution)
//
//
//
//            );
//
//
//        });
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//        n.run(2000);
//
//
//
//
//
//
//
//    }
//
//    public static Gridding conceptPlot(NAR nar, Iterable<FloatSupplier> concepts, int plotHistory) {
//
//
//        Gridding grid = new Gridding(VERTICAL);
//        List<Plot2D> plots = $.newArrayList();
//        for (FloatSupplier t : concepts) {
//            Plot2D p = new Plot2D(plotHistory, Plot2D.Line);
//            p.add(t.toString(), t::asFloat, 0f, 1f);
//            grid.add(p);
//            plots.add(p);
//        }
//        grid.layout();
//
//        nar.onCycle(f -> {
//            plots.forEach(Plot2D::update);
//        });
//
//        return grid;
//    }
//
//
//}
