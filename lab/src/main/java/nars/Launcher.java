package nars;

import nars.game.Game;
import org.reflections.Reflections;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.widget.meta.ObjectSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static spacegraph.space2d.container.grid.Gridding.grid;

public class Launcher {

    static class Experiment implements Runnable {
        final Class<? extends GameX> env;
        static final float fps = 25f;

        Experiment(Class<? extends GameX> env) {
            this.env = env;
        }

        @Override
        public void run() {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    GameX.Companion.initFn(fps, new Function<NAR, Game>() {
                        @Override
                        public Game apply(NAR n) {
                            try {

                                return env.getConstructor(NAR.class).newInstance(n);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            }).start();
        }

        @Override
        public String toString() {
            return env.getSimpleName();
        }
    }
    static class MainRunner implements Runnable {
        final Runnable env;

        MainRunner(Runnable env) {
            this.env = env;
        }

        @Override
        public void run() {

            new Thread(env).start();
        }

        @Override
        public String toString() {
            return env.toString();
        }
    }

    public static void main(String[] args) {


        Set<Class<? extends GameX>> envs = new Reflections("nars").getSubTypesOf(GameX.class);

        List<Experiment> list = new ArrayList<>();
        for (Class<? extends GameX> env : envs) {
            Experiment experiment = new Experiment(env);
            list.add(experiment);
        }
        Surface m = grid(
                new ObjectSurface(
                        list
                ),
                new ObjectSurface(
                        List.of(new MainRunner(new Runnable() {
                            @Override
                            public void run() {
                                GUI.main(new String[]{});
                            }
                        }))
//                            List.of(new MainRunner(OSMTest.class))
                )
        );


        GraphEdit2D g = new GraphEdit2D();
        SpaceGraph.window(                 g, 800, 800        );

        g.add(m).posRel(0.5f, 0.5f, 0.75f);

    }

}
