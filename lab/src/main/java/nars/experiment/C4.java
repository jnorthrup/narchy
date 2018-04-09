package nars.experiment;

import jcog.Util;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.experiment.connect4.ConnectFour;
import nars.op.java.Opjects;
import nars.time.RealTime;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/** connect-4 experiments */
public class C4 {

    public static void main(String[] args) throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        ((RealTime)n.time).durFPS(1f);

        n.beliefPriDefault.set(0.1f);
        n.goalPriDefault.set(1f);

        n.log(System.out, (x)->x instanceof Task && (!((Task)x).isInput())
                && ((Task)x).isGoal()
        );

        Opjects o = new Opjects(n);
        ConnectFour.ConnectFourState game = o.a("c", ConnectFour.ConnectFourState.class);

        JFrame frame = ConnectFour.constructApplicationFrame(game);
        frame.setSize(450, 450);
        frame.setVisible(true);

        //n.startFPS(51f);
        Deriver.derivers(n).forEach(d -> d.conceptsPerIteration.set(20));
        n.startFPS(10f);

        n.input("isWinPositionFor(c,(#x,#y,1),true)!");
        n.input("--isWinPositionFor(c,(#x,#y,2),true)!");

        int which = -1;
        while (true) {
            int whosMove = game.moving();
            int invalids = game.invalidCount();
            switch (whosMove) {
                case 1: //RED

                    System.out.println();
                    float max = Float.NEGATIVE_INFINITY;
                    which = -1;
                    for (int i = 0; i < game.cols; i++) {
                        Concept d = dropConcept(i, n, game, 1);
                        Truth gd = d.goals().truth(n.time() + n.dur(), n);
                        System.out.println(d + "\t" + gd);
                        if (gd!=null && gd.isPositive() && gd.expectation() > max) {
                            which = i;
                            max = gd.expectation();
                        }
                    }
                    System.out.println();
                    if (which == -1 || n.random().nextFloat() < 0.5f)
                        which = n.random().nextInt(game.cols);

                    n.input(dropConcept(which, n, game,1) + "! :|:");

                    break;
                case 2: //YELLOW
                    game.drop(n.random().nextInt(game.cols),2);
                    break;
            }

            do {
                Util.sleep(300);
            } while (!game.isTerminal() && game.moving()==whosMove && invalids==game.invalidCount());

//            if (which!=-1) {
//                Concept d = dropConcept(which, n, game, 1);
//                n.input(d.neg() + "! :|:"); //quelch last choice
//                which = -1;
//            }

            if (game.isTerminal())
                game.clear();

            frame.repaint();

            //n.synch();


//            try {
//                System.in.read();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

    }

    @NotNull
    private static Concept dropConcept(int col, NAR n, ConnectFour.ConnectFourState game, int player) throws Narsese.NarseseException {
        return n.conceptualize("drop(c,(" + col + "," + player + "))");
    }

}
