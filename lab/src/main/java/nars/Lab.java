package nars;

import nars.gui.Vis;
import org.reflections.Reflections;
import spacegraph.SpaceGraph;

import static java.util.stream.Collectors.toList;

public class Lab {

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


        var envs = new Reflections("nars").getSubTypesOf(NAgentX.class);
        SpaceGraph.window(
                Vis.reflect(envs.stream().map(Experiment::new).collect(toList())),
                800, 800
        );


    }

}
