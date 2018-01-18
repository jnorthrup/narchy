package nars.gui.graph;

import jcog.Util;
import jcog.data.graph.hgraph.Node;
import jcog.data.graph.hgraph.NodeGraph;
import nars.NARS;
import nars.Task;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.MutableContainer;
import spacegraph.math.v2;
import spacegraph.widget.button.PushButton;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import static nars.Op.*;

public class TasksView extends MutableContainer {

    public static void main(String[] args) throws IOException {

        TasksView t = new TasksView();
        t.loadTasks(new File("/tmp/NALTest/" +
                //"NAL7Test.induction_on_events_neg2.nal"
                //"NAL7Test.testPrediction1.nal"
                "NAL7Test.induction_on_events_composition3.nal"
        ));

        t.layoutTimeline();

        SpaceGraph.window(t, 800, 800);
    }

    final LongObjectHashMap<Surface> evidences = new LongObjectHashMap<>();


    public void layoutTimeline() {

        final NodeGraph<Surface, String> graph = new NodeGraph();

        float tScale = 100;
        float tMin = tScale;

        children.forEach(cc -> {
            TaskIcon c = (TaskIcon) cc;//TODO make window content iteration method


            Task t = c.task;

            Node<Surface, String> tn = graph.add(c);

            for (long e : t.stamp()) {
                Node en = graph.node(evidences.getIfAbsentPutWithKey(e, (ee) -> {
                    Surface s = new PushButton("_" + ee);

                    //TODO make evidence buttons visibility toggleable
                    //children.add(s);

                    graph.add(s);
                    return s;
                }));

                graph.edgeAdd(en, "stamp", tn);
            }

            float minH = 30;
            float maxH = 200;
            float h = t.isQuestOrQuestion() ?
                    Util.lerp(t.originality()/2f, minH, maxH) :
                    Util.lerp( t.originality() * t.conf(), minH, maxH);


            long start, end;
            if (!t.isEternal()) {
                start = t.start();
                end = t.end();
            } else {
                start = t.creation();
                end = 10; //TODO max time
            }


            float x1 = start * tScale;
            float x2 = end * tScale;
            if (x2 - x1 < tMin) {
                float x = (x1 + x2) / 2f;
                x1 = x - tMin / 2;
                x2 = x + tMin / 2;
            }


            float y = (float) (Math.random() * 500);
            c.pos(x1, y, x2, y + h);


        });

        graph.print();

        new Thread(() -> {
            int iterations = 300;
            for (int i = 0; i < iterations; i++) {

                layoutForceDirect(graph);

                layout();

                Util.sleep(5);

            }
        }).start();

    }

    private void layoutForceDirect(NodeGraph<Surface, String> graph) {


        float maxRepelDist = 2000;
        float attractSpeed = 10f;
        float repelSpeed = 250f;
        float minAttractEpsilon = 1f;
        float centerGravity = 0.001f;

        graph.nodes().forEach(a -> {
            a.edges(false, true).forEach(e -> {
                direct(minAttractEpsilon, attractSpeed, a, e.to);
            });
        });

        graph.nodes().forEach(a -> {
            graph.nodes().forEach(b -> {
                direct(maxRepelDist, -repelSpeed, a, b);
            });
        });

        graph.nodes().forEach(a -> {
            float cx = a.id.x();
            float cy = a.id.y();
            a.id.move(centerGravity * -cx, centerGravity * -cy);
        });


    }

    /**
     * speed < 0 = repel, speed > 0 = attract
     */
    private void direct(float limit, float speed, Node<Surface, String> a, Node<Surface, String> b) {
        v2 ac = new v2(a.id.x(), a.id.y());

        v2 bc = new v2(b.id.x(), b.id.y());

        v2 delta = new v2(ac);
        delta.sub(bc);

        float dist = delta.normalize();
        dist -= (a.id.radius() + b.id.radius());
        if (dist < 0)
            dist = 0;

        float s;
        if (speed < 0) {
            if (dist >= limit)
                return;

            s = -speed / (1 + (dist * dist));
        } else {
            if (dist <= limit)
                return;
            s = speed * Math.min(1, dist);
        }


        a.id.move(delta.x * s, delta.y * s);
        b.id.move(-delta.x * s, -delta.y * s);
    }

    public void loadTasks(File f) throws IOException {
        NARS.shell().inputBinary(f).tasks().map(TaskIcon::new).collect(Collectors.toCollection(() -> children));
    }

    static class TaskIcon extends PushButton {
        public final Task task;

        float r, g, b, a;

        public TaskIcon(Task x) {
            super(x.toString());
            this.task = x;
            System.out.println(x);

            switch (x.punc()) {
                case BELIEF:
                    r = 0.8f * x.freq();
                    b = g = 0;
                    a = x.conf();
                    break;
                case GOAL:
                    g = 0.8f * x.freq();
                    b = r = 0;
                    a = x.conf();
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
        public Surface move(float dxIgnored, float dy) {
            return super.move(0, dy); //HACK
        }

        @Override
        public float radius() {
            return h(); //HACK
        }

//        @Override
//        protected void paintBackColor(GL2 gl) {
//            gl.glColor4f(r, g, b, a);
//        }
    }

}
