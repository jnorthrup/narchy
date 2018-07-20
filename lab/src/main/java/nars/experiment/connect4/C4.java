package nars.experiment.connect4;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
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

/**
 * connect-4 experiments
 */
public class C4 {

    static class NARPlayer {

        private final NAR n;
        private final ConnectFour.ConnectFourState.Play play;
        private final int opponent;

        public NARPlayer(int whoAmI, ConnectFour.ConnectFourState game) {
            NAR n = NARchy.core();
            ((RealTime) n.time).durFPS(10f);

            n.beliefPriDefault.set(0.5f);
            n.goalPriDefault.set(0.75f);
            this.n = n;

            Deriver.derivers(n).forEach(d -> ((MatrixDeriver) d).conceptsPerIteration.set(4));


            Opjects o = new Opjects(n);
            ConnectFour.ConnectFourState.Play play = o.a("c", ConnectFour.ConnectFourState.Play.class);

            play.init(game, whoAmI);
            this.play = play;

            n.startFPS(10f);

            opponent = play.player == 1 ? 2 : 1;

            try {
                n.input("$1.0 whoWon(c," + play.player + ")!");
                n.input("$1.0 --whoWon(c," + opponent + ")!");
                n.input("$1.0 --whoWon(c,0)! %1.0;0.05%");
                n.input("$1.0 (whoWon(c,1) =|> drop(c,#x,true)).");
                n.input("$1.0 (whoWon(c,2) =|> drop(c,#x,true)).");
                n.input("$1.0 (whoWon(c,1) =|> --whoWon(c,2)).");
                n.input("$1.0 (whoWon(c,2) =|> --whoWon(c,1)).");


            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            SpaceGraph.window(new Splitting(NARui.top(n), beliefCharts(), 0.3f), 800, 800);
        }

        void inputAssumptions() {
            try {
                n.input("$1.0 whoWon(c," + play.player + ")! |..+1s");
                n.input("$1.0 --whoWon(c," + opponent + ")! |..+1s");


                for (int i = 0; i < play.game.cols; i++) {
                    for (boolean c : new boolean[]{true/*, false */}) {
                        n.input(dropConcept(i, c) + "@ |..+500ms");
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public void moving(int who) {
            play.moving("red", who == 1);
            play.moving("yel", who == 2);

            play.see();

            inputAssumptions();

            if (play.player == who) {


                try {
                    n.input(dropConcept(n.random().nextInt(play.game.cols), true) + "! |..+1sec %1.0;0.02%");
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                }

                int triesRemain = 7;
                IntHashSet tried = new IntHashSet();
                while (triesRemain-- > 0 && play.game.moving() == play.player) {

                    int which;
                    float max = Float.NEGATIVE_INFINITY;
                    which = -1;

                    for (int i = 0; i < play.game.cols; i++) {
                        if (tried.contains(i))
                            continue;
                        Concept d = dropConcept(i, true);
                        Truth gd = d.goals().truth(n.time(), n.time() + n.dur(), n);

                        if (gd != null && gd.expectation() > max) {
                            which = i;
                            max = gd.expectation();
                        }
                    }
                    if (which != -1 /* || n.random().nextFloat() < 0.5f*/) {
                        tried.add(which);

                        play.tryDrop(n, which);
                        Util.sleep(50);
                    } else {
                        break;
                    }
                }


                while (play.game.moving() == play.player && !play.game.drop(randomCol(), play.player)) {
                }

            }
        }

        public int randomCol() {
            return n.random().nextInt(play.game.cols);
        }

        public Concept dropConcept(int col, boolean commandlike) {
            return C4.dropConcept(col, n, commandlike, play.game, play.player);
        }

        public Surface beliefCharts() {

            List<Term> c = new FasterList();
            for (int i = 0; i < play.game.cols; i++)
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


        while (true) {


            int who = game.moving();


            if (who == 1) {
                A.moving(who);
                B.moving(who);
            } else {
                B.moving(who);
                A.moving(who);
            }

            frame.repaint();


            int winner = A.play.whoWon();
            B.play.whoWon();

            if (winner != 0) {

                int loser = winner == 1 ? 2 : 1;

                System.err.println("winner: " + winner);


                for (int i = 0; i < 10; i++) {
                    A.play.whoWon();
                    B.play.whoWon();

                    try {
                        A.n.input("--whoWon(c," + loser + "). |");
                        B.n.input("--whoWon(c," + loser + "). |");
                    } catch (Narsese.NarseseException e) {
                        e.printStackTrace();
                    }
                    Util.sleep(100);
                }


                A.play.clear();
                B.play.clear();
            } else {
                Util.sleep(50);
            }
        }

    }


    static Concept dropConcept(int col, NAR n, boolean commandlike, ConnectFour.ConnectFourState game, int player) {
        try {
            return n.conceptualize("drop(c," + col + "," + (commandlike ? "_" : "true") + ")");
        } catch (Narsese.NarseseException e) {
            throw new WTF();
        }
    }

}
