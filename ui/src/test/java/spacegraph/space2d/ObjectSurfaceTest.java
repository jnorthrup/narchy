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

        public final FloatRange f = new FloatRange(0.5f, 0, 1f);

        public final Runnable runMe = ()->{
            f.set(0.1f);
        };

        public MyInnerClass myInnerClass = new MyInnerClass();
    }

    public static class MyInnerClass {
        public final AtomicBoolean toggleMe = new AtomicBoolean();
    }

}