package nars;

import org.reflections.Reflections;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.windo.GraphEdit;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static spacegraph.space2d.container.grid.Gridding.grid;

public class Launcher {

    static class Experiment implements Runnable {
        final Class<? extends NAgentX> env;
        final float fps = 25f;

        Experiment(Class<? extends NAgentX> env) {
            this.env = env;
        }

        @Override
        public void run() {

            new Thread(()-> {
                NAgentX.runRT((n) -> {
                    try {

                        return env.getConstructor(NAR.class).newInstance(n);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, fps);
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

            new Thread(()-> {

                    env.run();

            }).start();
        }

        @Override
        public String toString() {
            return env.toString();
        }
    }

    public static void main(String[] args) {


        Set<Class<? extends NAgentX>> envs = new Reflections("nars").getSubTypesOf(NAgentX.class);

        Surface m = grid(
                new ObjectSurface<>(
                        envs.stream().map(Experiment::new).collect(toList())
                ),
                new ObjectSurface<>(
                        List.of(new MainRunner(() -> GUI.main(new String[]{})))
//                            List.of(new MainRunner(OSMTest.class))
                )
        );


        GraphEdit g = new GraphEdit<>(1000, 1000);
        g.add(m).pos(0, 0, 200, 200);

        SpaceGraph.window(                 g, 800, 800        );


    }

}
