package nars.experiment.connect4;

import jcog.Util;
import nars.NAR;
import nars.NARchy;
import nars.Narsese;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.derive.deriver.MatrixDeriver;
import nars.op.java.Opjects;
import nars.time.clock.RealTime;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/** connect-4 experiments */
public class C4 {

    public static void main(String[] args) throws Narsese.NarseseException {
        NAR n = NARchy.core();
        ((RealTime)n.time).durFPS(20f);

        n.beliefPriDefault.set(0.25f);
        n.goalPriDefault.set(0.5f);


        Deriver.derivers(n).forEach(d -> ((MatrixDeriver)d).conceptsPerIteration.set(100));


        //n.log(System.out, (t)->((Task)t).pri() >= 0.7f);

        //n.startFPS(40f);
//        n.log(System.out, (x)->x instanceof Task && (!((Task)x).isInput())
//                && ((Task)x).isGoal()
//        );

        ConnectFour.ConnectFourState game = new ConnectFour.ConnectFourState();

        Opjects o = new Opjects(n);
        ConnectFour.ConnectFourState.Play play = o.a("c", ConnectFour.ConnectFourState.Play.class);

        play.init(game, 1);

        JFrame frame = ConnectFour.constructApplicationFrame(game);
        frame.setSize(450, 450);
        frame.setVisible(true);

        //n.input("$1.0 isWinPositionFor(c,(#x,#y,1),true)!");
        //n.input("$1.0 --isWinPositionFor(c,(#x,#y,2),true)!");


        int which = -1;
        while (true) {

            n.input("$1.0 (drop(c,#x,true) ==> winner(c,#y)).");
            n.input("$1.0 winner(c,1)!");
            n.input("$1.0 winner(c,1)! :|:");
            n.input("$1.0 winner(c,0)! %1.0;0.50%");
            n.input("$1.0 --winner(c,2)!");


            int whosMove = game.moving();
//            int invalids = game.invalidCount();

            play.moving(whosMove);

            switch (whosMove) {
                case 1: //RED



                    System.out.println();
                    float max = Float.NEGATIVE_INFINITY;
                    which = -1;
                    for (int i = 0; i < game.cols; i++) {
                        Concept d = dropConcept(i, n, game, 1);
                        Truth gd = d.goals().truth(n.time(), n);
                        System.out.println(d + "\t" + gd);
                        if (gd!=null && gd.isPositive() && gd.expectation() > max) {
                            which = i;
                            max = gd.expectation();
                        }
                    }
                    System.out.println();
                    if (which == -1 || n.random().nextFloat() < 0.5f)
                        which = n.random().nextInt(game.cols);

                    //n.input(dropConcept(which, n, game,1) + "! :|:");

                    Util.sleep(500);

                    int maxWaitLoops = 1;
                    while (!play.drop(which) ) {
                        Util.sleep(100);
                        if (maxWaitLoops-- <= 0) {
                            //choose at random
                            while (!play.drop(n.random().nextInt(game.cols))) {
                               // System.out.println("trying dropping random");
                            }
                            break;
                        }

                    }

                    break;
                case 2: //YELLOW
                    Util.sleep(300); //pretend to think
                    while (!game.drop(n.random().nextInt(game.cols),2)) {

                    }
                    break;
            }

            play.see();
            if (play.winner()!=0)
                play.clear();

            Util.sleep(500);

//            do {
//                Util.sleep(300);
//            } while (!game.isTerminal() && game.moving()==whosMove && invalids==game.invalidCount());

//            if (which!=-1) {
//                Concept d = dropConcept(which, n, game, 1);
//                n.input(d.neg() + "! :|:"); //quelch last choice
//                which = -1;
//            }

            frame.repaint();

            n.run(1);

        }

    }

    @NotNull
    private static Concept dropConcept(int col, NAR n, ConnectFour.ConnectFourState game, int player) throws Narsese.NarseseException {
        return n.conceptualize("drop(c," + col + ",true)");// + player + ")");
    }

}
