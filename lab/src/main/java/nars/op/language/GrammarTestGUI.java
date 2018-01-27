///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package nars.nlp;
//
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import nars.guifx.NARfx;
//import nars.guifx.chart.Plot2D;
//import nars.guifx.util.POJOPane;
//import nars.util.data.Util;
//
///**
// *
// * @author me
// */
//public class GrammarTestGUI extends GrammarTest2 {
//
//    public static final int delay = 50;
//    private final Plot2D p, p2, p3, p4, p5, p6;
//    private final VBox charts;
//
//    public GrammarTestGUI() throws Exception {
//        super();
//
//        //n.logSummaryGT(System.out,0.7f);
//
//        int points = 200;
//        p = new Plot2D(Plot2D.BarWave, points, 800, 200);
////        p.add("reward", () -> {
////            return reward;
////        });
//        p.add("coherence", () -> {
//            return tape.coherence;
//        });
//
//
//        p3 = new Plot2D(Plot2D.BarWave, points, 800, 100)
//        .add("cur", () -> {
//            return (int)tape.current;
//        });
//        p4 = new Plot2D(Plot2D.BarWave, points, 800, 100)
//        .add("next", () -> {
//            return (int)tape.next;
//        });
////        Plot2D p55 = new Plot2D(Plot2D.BarWave, points, 800, 100)
////                .add("?", () -> {
////                    return (int) tape._prediction();
////                });
//        p5 = new Plot2D(Plot2D.BarWave, points, 800, 100)
//        .add("a", () -> {
//            return (int)tape.votes.getIfAbsent('a', 0);
//        });
//        p6 = new Plot2D(Plot2D.BarWave, points, 800, 100)
//        .add("b", () -> {
//            return (int)tape.votes.getIfAbsent('b', 0);
//        });
//        p2 = new Plot2D(Plot2D.BarWave, points, 800, 200);
//
////        p3.add("totalScore", () -> {
////            return tape.totalScore;
////        });
//        p2.add("currentScore", () -> {
//            return tape.prevScore;
//        });
//
//        charts = new VBox(p,  p3
//                , p4, /*p55,*/ p5, p6, p2);
//
//        n.run(10);
//        NARfx.run(()-> {
//            NARfx.newWindow("x", charts);
//            NARfx.newWindow("y",
//                    new HBox(
//                        new POJOPane(GrammarTestGUI.this.q),
//                        new POJOPane(GrammarTestGUI.this.q.q),
//                        new POJOPane(GrammarTestGUI.this)
//                    )
//            );
//        });
//
//        n.onFrame(n -> {
//            updateGraphs();
//            Util.pause(delay);
//        });
//    }
//
//    @Override
//    protected void onStep() {
//        super.onStep();
//
//    }
//
//    public void updateGraphs() {
//        charts.getChildren().forEach(e -> ((Plot2D)e).update());
//    }
//
//
//    public static void main(String[] args) throws Exception {
//        new GrammarTestGUI().run();
//    }
//
//}
