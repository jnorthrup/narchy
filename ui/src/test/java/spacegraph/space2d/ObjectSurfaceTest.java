package spacegraph.space2d;

import jcog.math.FloatRange;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.meta.ObjectSurface;

import java.util.concurrent.atomic.AtomicBoolean;

public class ObjectSurfaceTest {

    public static void main(String[] args) {
        SpaceGraph.window(new ObjectSurface(new MyClass(), 3), 1200, 800);
    }

    public static class MyClass {

        public final FloatRange AT_FIELD = new FloatRange(0.5f, 0, 1f);

        public final Runnable ABORT = new Runnable() {
            @Override
            public void run() {
                AT_FIELD.set(0.1f);
            }
        };

        public MyInnerClass inner = new MyInnerClass();
    }

    public static class MyInnerClass {
        public final AtomicBoolean WARNING = new AtomicBoolean();
        public final FloatRange EXTREME_DANGER = new FloatRange(0.75f, 0, 1f);
    }

}