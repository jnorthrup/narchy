package nars.gui.graph;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.NARS;
import nars.Task;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Graph2D;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.container.Timeline2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;

import java.util.function.Consumer;

import static nars.Op.*;

public class TasksView implements Timeline2D.TimelineModel<Task> {

    private static final Consumer<Graph2D.NodeVis<Task>> TaskRenderer = (n) -> {
        n.set(new Scale(new TaskIcon(n.id), 0.9f));
    };

    private final Iterable<Task> tasks;

    public TasksView(Iterable<Task> tasks) {
        this.tasks = tasks;
    }

    public static void main(String[] args) {



        NAR n = NARS.tmp();
        n.log();
        //n.inputAt(0, "(x &&+1 y). |");
        n.inputAt(0, "(x ==>+1 y). |");
        n.inputAt(2,"y! |");
        n.inputAt(3,"x. |");
        n.run(10);

        Iterable<Task> tasks = ()->n.tasks().filter(x->!x.isEternal()).iterator();

        Timeline2D t = timeline(tasks).view(0, n.time());
        SpaceGraph.window(t.withControls(),
                1200, 500);
    }

    public static Timeline2D timeline(Iterable<Task> tasks) {
        return new Timeline2D<>(new TasksView(tasks), TaskRenderer);
    }

    @Override
    public Iterable<Task> events(long start, long end) {
        return tasks;
    }

    @Override
    public long[] range(Task event) {
        return new long[] { event.start(), event.end()+1 };
    }


    static class TaskIcon extends PushButton {
        public final Task task;

        float r, g, b, a;

        public TaskIcon(Task x) {
            super(x.toStringWithoutBudget());
            this.task = x;


            switch (x.punc()) {
                case BELIEF:
                    r = 0.8f * x.freq();
                    b = g = 0;
                    a = 0.2f + 0.8f * x.conf();
                    break;
                case GOAL:
                    g = 0.8f * x.freq();
                    b = r = 0;
                    a = 0.2f + 0.8f * x.conf();
                    break;
                case QUESTION:
                    r = 0;
                    g = 0.25f;
                    b = 1f;
                    a = 0.25f;
                    break;
                case QUEST:
                    r = 0;
                    g = 0.5f;
                    b = 0.75f;
                    a = 0.25f;
                    break;
            }
        }

        @Override
        protected void paintBelow(GL2 gl, SurfaceRender r) {
            Draw.rectRGBA(bounds, this.r, g, b, 0.5f, gl);
        }

        @Override
        public Surface move(float dxIgnored, float dy) {
            return super.move(0, dy); 
        }

        @Override
        public float radius() {
            return h(); 
        }


    }

}
