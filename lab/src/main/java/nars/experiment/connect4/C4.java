package nars.experiment.connect4;

import jcog.Util;
import jcog.WTF;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.NARchy;
import nars.Narsese;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.derive.deriver.MatrixDeriver;
import nars.gui.NARui;
import nars.op.java.Opjects;
import nars.term.Term;
import nars.time.clock.RealTime;
import nars.truth.Truth;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;

import javax.swing.*;
import java.util.List;

import static jcog.Texts.n2;

/** connect-4 experiments */
public class C4 {

    static class NARPlayer {

        private final NAR n;
        private final ConnectFour.ConnectFourState.Play play;
        private final int opponent;

        public NARPlayer(int whoAmI, ConnectFour.ConnectFourState game) {
            NAR n = NARchy.core();
            ((RealTime)n.time).durFPS(20f);

            n.beliefPriDefault.set(0.25f);
            n.goalPriDefault.set(0.75f);
            this.n = n;

            Deriver.derivers(n).forEach(d -> ((MatrixDeriver)d).conceptsPerIteration.set(2));


            //n.log(System.out, (t)->((Task)t).pri() >= 0.7f);

            //n.startFPS(40f);
//            n.log(System.out, (x)->x instanceof Task && (!((Task)x).isInput())
//                    && ((Task)x).isGoal()
//            );

            Opjects o = new Opjects(n);
            ConnectFour.ConnectFourState.Play play = o.a("c", ConnectFour.ConnectFourState.Play.class);

            play.init(game, whoAmI);
            this.play = play;

            n.startFPS(40f);

            opponent = play.player == 1 ? 2 : 1;

            try {
                n.input("$1.0 whoWon(c," + play.player + ")!");
                n.input("$1.0 --whoWon(c," + opponent + ")!");
                n.input("$1.0 --whoWon(c,0)! %1.0;0.05%"); //tie or not finished
                n.input("$1.0 (whoWon(c,1) =|> drop(c,#x,true)).");
                n.input("$1.0 (whoWon(c,2) =|> drop(c,#x,true)).");
                n.input("$1.0 (whoWon(c,1) =|> --whoWon(c,2)).");
                n.input("$1.0 (whoWon(c,2) =|> --whoWon(c,1)).");
                //n.input("$1.0 (drop(c,#x,true) =|> whoWon(c,#y)).");
                //n.input("$1.0 (--drop(c,#x,#z) =|> whoWon(c,#y)).");
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            SpaceGraph.window(new Splitting(NARui.top(n), beliefCharts(), 0.3f), 800,800);
        }

        void inputAssumptions() {
            try {
                n.input("$1.0 whoWon(c," + play.player + ")! |..+1s");
                n.input("$1.0 --whoWon(c," + opponent + ")! |..+1s");



                //action questions and prompt goal
                for (int i =0; i < play.game.cols; i++) {
                    for (boolean c : new boolean[] { true, false }) {
                        n.input(dropConcept(i, c) + "@ |..+500ms");
                    }
                }

                n.input("(drop(c,#x,#y) &| --drop(c,#w,#f))! |..+500ms");
                //n.input("drop(c,#x,true)! |"); //|..+1sec");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public void moving(int who) {
            play.moving("red", who==1);
            play.moving("yel", who==2);

            //n.run(1);

            play.see();

            if (play.player == who) {

                inputAssumptions();

                //curiosity
                try {
                    String f = n2(n.random().nextFloat());
                    n.input(dropConcept(n.random().nextInt(play.game.cols), true) + "! |..+1sec %" + f + ";0.02%");
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                }

                Util.sleep(600); //think


                int triesRemain = 7;
                IntHashSet tried = new IntHashSet();
                while (triesRemain-->0 && play.game.moving()==play.player) {

                    int which;
                    float max = Float.NEGATIVE_INFINITY;
                    which = -1;

                    for (int i = 0; i < play.game.cols; i++) {
                        if (tried.contains(i))
                            continue;
                        Concept d = dropConcept(i, true);
                        Truth gd = d.goals().truth(n.time(),n.time()+n.dur(), n);
                        //System.out.print(d + "=" + gd + "\t");
                        if (gd != null && gd.expectation() > max) {
                            which = i;
                            max = gd.expectation();
                        }
                    }
                    if (which != -1 /* || n.random().nextFloat() < 0.5f*/) {
                        tried.add(which);

                        play.tryDrop(n, which);
                        Util.sleep(50); //wait for move to effect
                    } else {
                        break;
                    }
                }

                //force
                while (play.game.moving()==play.player && !play.game.drop(randomCol(), play.player)) { }

            } else {

//                //dont attempt to move while the other player is, also clearing all choices so previous goal doesnt get repeated by default
//                for (int i = 0; i < play.game.cols; i++) {
//                    try {
//                        n.input(dropConcept(i, true) +
//                                "! | %0.25;0.05%");
//                    } catch (Narsese.NarseseException e) {
//                        e.printStackTrace();
//                    }
//                }

            }
        }

        public int randomCol() {
            return n.random().nextInt(play.game.cols);
        }

        public Concept dropConcept(int col, boolean commandlike) {
            return C4.dropConcept( col, n, commandlike, play.game, play.player);
        }

        public Surface beliefCharts() {

            List<Term> c = new FasterList();
            for (int i =0; i < play.game.cols; i++)
                c.add(dropConcept(i, true).term());

            c.add($.$$("whoWon(c,1)"));
            c.add($.$$("whoWon(c,0)"));
            c.add($.$$("whoWon(c,2)"));

            return NARui.beliefCharts(40, c, n);
        }
    }

    public static void main(String[] args) {

        ConnectFour.ConnectFourState game = new ConnectFour.ConnectFourState();

        JFrame frame = ConnectFour.constructApplicationFrame(game);
        frame.setSize(450, 450);
        frame.setVisible(true);

        NARPlayer A = new NARPlayer(1, game);
        NARPlayer B = new NARPlayer(2, game);




        //n.input("$1.0 isWinPositionFor(c,(#x,#y,1),true)!");
        //n.input("$1.0 --isWinPositionFor(c,(#x,#y,2),true)!");


        while (true) {

            int who = game.moving();
//            int invalids = game.invalidCount();

            if (who == 1) {
                A.moving(who);
                B.moving(who);
            } else {
                B.moving(who);
                A.moving(who);
            }

            frame.repaint();


            int winner = A.play.whoWon();
            B.play.whoWon(); //so it sees it too

            if (winner!=0) {

                System.err.println("winner: " + winner);

                //reinforcement repeated
                for (int i = 0; i < 10; i++) {
                    A.play.whoWon();
                    B.play.whoWon();
                    Util.sleep(100);
                }

                A.play.clear();
                B.play.clear();
            } else {
                Util.sleep(50);
            }
        }

    }



    static Concept dropConcept(int col, NAR n, boolean commandlike, ConnectFour.ConnectFourState game, int player)  {
        try {
            return n.conceptualize("drop(c," + col + "," + (commandlike ? "_" : "true")+ ")");// + player + ")");
        } catch (Narsese.NarseseException e) {
            throw new WTF();
        }
    }

}
