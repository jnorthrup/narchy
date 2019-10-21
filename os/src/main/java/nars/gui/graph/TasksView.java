package nars.gui.graph;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.NARS;
import nars.Task;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.container.time.Timeline2DEvents;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.*;

public class TasksView implements Timeline2D.EventBuffer<Task> {

    private static final Consumer<NodeVis<Task>> TaskRenderer = new Consumer<NodeVis<Task>>() {
        @Override
        public void accept(NodeVis<Task> n) {
            n.set(new Scale(new TaskIcon(n.id), 0.9f));
        }
    };

    private final Iterable<Task> tasks;

    public TasksView(Iterable<Task> tasks) {
        this.tasks = tasks;
    }

    public static void main(String[] args) {


        NAR n = NARS.tmp();
        n.log();
        //n.inputAt(0, "(x &&+1 y). |");
        n.inputAt(0L, "(x ==>+1 y). |");
        n.inputAt(2L,"y! |");
        n.inputAt(3L,"x. |");
        n.run(10);

        Iterable<Task> tasks = new Iterable<Task>() {
            @NotNull
            @Override
            public Iterator<Task> iterator() {
                return n.tasks().filter(new Predicate<Task>() {
                    @Override
                    public boolean test(Task x) {
                        return !x.isEternal();
                    }
                }).iterator();
            }
        };

        Timeline2D t = timeline(tasks).setTime(0L, n.time());
        SpaceGraph.window(t.withControls(),
                1200, 500);
    }

    public static Timeline2D timeline(Iterable<Task> tasks) {
        //TODO
        long start = 0L;
        long end = 10L;
        return new Timeline2D(start, end).addEvents(new TasksView(tasks), TaskRenderer, new Timeline2DEvents.LaneTimelineUpdater());
    }

    @Override
    public Iterable<Task> events(long start, long end) {
        return tasks;
    }

    @Override
    public long[] range(Task t) {
        if (t.op()!=CONJ)
            return new long[] { t.start(), t.end()+ 1L};
        else
            return new long[] { t.start(), (long) t.term().eventRange() + t.end()+ 1L};
    }


    static class TaskIcon extends PushButton {
        public final Task task;

        float r;
        float g;
        float b;
        float a;

        public TaskIcon(Task x) {
            super(x.toStringWithoutBudget());
            this.task = x;


            switch (x.punc()) {
                case BELIEF: {
                    float f = x.freq();
                    r = 0.8f * (1f - f);
                    g = 0.8f * f;
                    b = 0.2f + 0.8f * x.conf();
                    a = 1.0F;
                    break;
                }
                case GOAL: {
                    float f = x.freq();
                    g = 0.8f * f;
                    b = r = (float) 0;
                    a = 0.2f + 0.8f * x.conf();
                    break;
                }
                case QUESTION:
                    r = (float) 0;
                    g = 0.25f;
                    b = 1f;
                    a = 0.25f;
                    break;
                case QUEST:
                    r = (float) 0;
                    g = 0.5f;
                    b = 0.75f;
                    a = 0.25f;
                    break;
            }
        }

        @Override
        protected void paintIt(GL2 gl, ReSurface r) {
            Draw.rectRGBA(bounds, this.r, g, b, 0.5f, gl);
        }

        @Override
        public Surface move(float dxIgnored, float dy) {
            return super.move((float) 0, dy);
        }

        @Override
        public float radius() {
            return h(); 
        }


    }

}
