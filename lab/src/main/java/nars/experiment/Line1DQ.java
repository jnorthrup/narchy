//package nars.experiment;
//
//import jcog.Util;
//import nars.NAR;
//import nars.NARS;
//import nars.NAgentX;
//import nars.test.agent.Line1DSimplest;
//
//public class Line1DQ {
//
//
//    public static void main(String[] args) {
//
//
//        NAR n = new NARS().get();
//
//        n.time.dur(5);
//
//        Line1DSimplest a = new Line1DSimplest();
//        a.curiosity.setAt(0.01f);
//
//        a.onFrame((z) -> {
//            a.target(
//                    Util.unitize(
//
//                        (Math.abs(3484 ^ n.time()/200) % 11)/10.0f
//
//
//                    )
//            );
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
//        NAgentX.chart(a);
//
//
//
//
//
//
//
//
//
//        float grandTotal = 0;
//        for (int i = 0; i < 100; i++) {
//            int period = 1000;
//
//        }
//        System.err.println(" grand total = " + grandTotal);
//
//
//    }
//
//
//}
