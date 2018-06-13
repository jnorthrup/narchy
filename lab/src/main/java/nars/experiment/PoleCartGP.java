package nars.experiment;

import jcog.exe.Loop;
import jcog.learn.ql.HaiQae;
import nars.NAR;
import nars.NARS;
import nars.agent.NAgent;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.exe.MixMultiExec;
import nars.gui.NARui;
import org.oakgp.Evolution;
import org.oakgp.function.Fn;
import org.oakgp.function.compare.Equal;
import org.oakgp.function.compare.GreaterThan;
import org.oakgp.function.compare.LessThan;
import org.oakgp.rank.Ranking;
import org.oakgp.rank.fitness.FitFn;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space2d.widget.meter.AutoUpdateMatrixView;
import spacegraph.space2d.widget.text.Label;

import static org.oakgp.NodeType.integerType;
import static spacegraph.space2d.container.grid.Gridding.VERTICAL;

public class PoleCartGP {

    /** online GP learning controller - tries series of potential candiates on a live NAgent instance */
    public static class GPAgent extends Loop {
        public final NAgent agent;

        public GPAgent(NAgent agent) {
            this.agent = agent;
            Fn[] functions = {
                    //new If(MOVE_TYPE), new Equal(MOVE_TYPE), new IsValid(), new SwitchEnum(Move.class, nullableType(MOVE_TYPE), MOVE_TYPE),
                    new GreaterThan(integerType()), LessThan.create(integerType()), new Equal(integerType()),
                    //new Next()
                    };
            //List<ConstantNode> constants = createConstants();
            //NodeType[] variables = {STATE_TYPE, nullableType(MOVE_TYPE)};
            FitFn fitnessFunction = new TowersOfHanoiFitnessFunction(false);

            Ranking output = new Evolution().returns(MOVE_TYPE).constants(constants).variables(variables).functions(functions)
                    .goal(fitnessFunction).populationSize(INITIAL_POPULATION_SIZE).populationDepth(INITIAL_POPULATION_MAX_DEPTH)
                    .stopFitness(TARGET_FITNESS).stopGenerations(NUM_GENERATIONS).get();


        }

        @Override
        protected void onStart() {
            super.onStart();
        }
    }

    public static void main(String[] args) {

        float systemFPS = 30f;
        float agentFPS = 10f;

        NAR n = NARS.realtime(systemFPS).exe(new MixMultiExec.WorkerMultiExec(512, 4)).get();
        n.beliefPriDefault.set(0.01f);
        n.goalPriDefault.set(0.5f);
        n.questionPriDefault.set(0.005f);
        n.questPriDefault.set(0.005f);
        //n.log();

        //PoleCart a = new PoleCart(n);
        //FZero a = new FZero(n);
        //TrackXY a = new TrackXY(4,4); //<- may need manually started
        //Arkancide a = new Arkancide(n, false, true);
        Tetris a = new Tetris(n);

        new MatrixDeriver( Derivers.nal(1,8, n) );

        a.always.clear(); //HACK


        n.startFPS(systemFPS);

        n.runLater(() -> {
//            RLBooster rl = new RLBooster(a,
//                    (i, o) -> new HaiQae(i, 10, o),
//                    3);
//
//            Gridding v = haiQVis((HaiQae) rl.rl);
//            v.add(NARui.beliefCharts(512,
//                    Iterables.concat(
//                            a.sensors.keySet(),
//                            a.actions.keySet()
//                    ),
//                    n));
//            SpaceGraph.window(v, 1200, 800);

            NARui.agentWindow(a);
            a.startFPS(agentFPS);

            SpaceGraph.window(NARui.top(n), 900, 500);
        });


    }

    public static Gridding haiQVis(HaiQae q) {
        return new Gridding(
                new Label("HaiQ"),
                new AutoSurface<>(q),
                new Gridding(VERTICAL,
                        new AutoUpdateMatrixView(q.ae.x),
                        new AutoUpdateMatrixView(q.ae.W),
                        new AutoUpdateMatrixView(q.ae.y)
                ),
                new Gridding(VERTICAL,
                        new AutoUpdateMatrixView(q.q),
                        new AutoUpdateMatrixView(q.et)
                )

        );
    }

}
