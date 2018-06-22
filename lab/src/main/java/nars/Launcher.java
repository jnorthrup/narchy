package nars;

import org.reflections.Reflections;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.meta.AutoSurface;

import java.util.Set;

import static java.util.stream.Collectors.toList;

public class Launcher {

    static class Experiment implements Runnable {
        final Class<? extends NAgentX> env;
        final float fps = 20f;

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

    public static void main(String[] args) {


        Set<Class<? extends NAgentX>> envs = new Reflections("nars").getSubTypesOf(NAgentX.class);
        SpaceGraph.window(
                new AutoSurface<>(envs.stream().map(Experiment::new).collect(toList())),
                800, 800
        );


    }

}
