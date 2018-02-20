package nars.experiment;

import jcog.Util;
import nars.NAR;
import nars.NARS;
import nars.NAgentX;
import nars.test.agent.Line1DSimplest;

public class Line1DQ {


    public static void main(String[] args) {
        //Param.DEBUG = true;

        NAR n = new NARS().get();
        //n.log();
        n.time.dur(5);

        Line1DSimplest a = new Line1DSimplest();
        a.curiosity.set(0.01f);

        a.onFrame((z) -> {
            a.target(
                    Util.unitize(
                        //(float) (0.5f * (Math.sin(n.time() / 50f) + 1f))
                        (Math.abs(3484 ^ n.time()/200) % 11)/10.0f
                        //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f))) *
                        //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
                    )
            );
        });

        //Arkancide a = new Arkancide(n, false, true);
        //Tetris a = new Tetris(n, 6, 10, 4);

        //a.onFrame(x -> Util.sleep(1));
        //a.trace = true;

        //Line1DTrainer trainer = new Line1DTrainer(a);


        //new RLBooster(a, new HaiQAgent(), 2); n.deriver.rate.setValue(0); a.curiosity.setValue(0f);

        NAgentX.chart(a);

//        int h = q.ae.W[0].length;
//        int w = q.ae.W.length;
//        window( grid(
//                new MatrixView(w, h, MatrixView.arrayRenderer(q.ae.W)),
//                new MatrixView(w, 1, MatrixView.arrayRenderer(q.ae.y)),
//                new MatrixView(w, 1, MatrixView.arrayRenderer(q.ae.z))
//        ), 500, 500);

        float grandTotal = 0;
        for (int i = 0; i < 100; i++) {
            int period = 1000;
            //a.runCycles(period);
        }
        System.err.println(" grand total = " + grandTotal);


    }


}
