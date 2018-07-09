package spacegraph.space2d.widget.meta;

import com.google.common.collect.Sets;
import jcog.Service;
import jcog.Services;
import jcog.TODO;
import jcog.event.Ons;
import jcog.list.FasterList;
import jcog.math.EnumParam;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.util.Reflect;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.IntSlider;
import spacegraph.space2d.widget.tab.ButtonSet;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.windo.Widget;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 2/28/17.
 */
public class AutoSurface<X> extends Gridding {

    private final static int MAX_DEPTH = 1;
    private final Set<Object> seen = Sets.newSetFromMap(new IdentityHashMap());
    /**
     * root
     */
    private final X obj;
    private Ons ons = null;

    public AutoSurface(X x) {
        super();
        this.obj = x;
    }

    @Override
    public boolean start(@Nullable SurfaceBase parent) {

        if (super.start(parent)) {
            seen.clear();
            ons = new Ons();
            List<Surface> l = new FasterList();
            collect(obj, l, 0);

            set(l);
            return true;
        }
        return false;
    }

    private void collect(Object y, List<Surface> l, int depth) {
        collect(y, l, depth, null);
    }

    private void collect(Object x, List<Surface> target, int depth, String yLabel /* tags*/) {

        if (!add(x))
            return;

        if (yLabel == null)
            yLabel = x.toString();

        if (x instanceof Surface) {
            Surface sx = (Surface) x;
            if (sx.parent == null) {
                target.add(new LabeledPane(yLabel, sx));
            }
            return;
        }

        if (x instanceof Services) { 
            target.add(new AutoServices((Services) x));
            return;
        }

        if (x instanceof Collection) {
            Surface cx = collectElements((Iterable) x, depth + 1);
            if (cx != null) {
                target.add(new LabeledPane(yLabel, cx));
            }
        }


        if (x instanceof FloatRange) {
            target.add(new MySlider((FloatRange) x, yLabel));
        } else if (x instanceof IntRange) {
            target.add(new MyIntSlider((IntRange) x, yLabel));
        } else if (x instanceof AtomicBoolean) {
            target.add(new CheckBox(yLabel, (AtomicBoolean) x));


        } else if (x instanceof Runnable) {
            target.add(new PushButton(yLabel, (Runnable) x));
        } else if (x instanceof EnumParam) {
            target.add(newSwitch((EnumParam) x));
        }

        if (depth < MAX_DEPTH) {
            collectFields(x, target, depth + 1);
        }
    }

    private ButtonSet newSwitch(EnumParam<?> x) {
        EnumSet<?> s = EnumSet.allOf(x.klass);
        System.out.println(x + " " + x.value + " " + x.klass + " " + s);
        throw new TODO();//JDK12 compiler error

//        ToggleButton[] b = ((EnumSet) EnumSet.allOf(x.klass)).stream().map(e -> {
//            CheckBox tb = new CheckBox(e.name());
//            tb.on((c, enabled) -> {
//                if (enabled)
//                    x.set(e);
//            });
//            return tb;
//        }).toArray(ToggleButton[]::new);
//
//        ButtonSet s = new ButtonSet(ButtonSet.Mode.One, b);
//        return s;
    }

    private Surface collectElements(Iterable<?> x, int depth) {
        FasterList<Surface> m = new FasterList();
        for (Object o : x) {
            collect(o, m, depth);
        }
        return !m.isEmpty() ? grid(m) : null;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            if (ons != null) {
                ons.off();
                ons = null;
            }
            return true;
        }
        return false;
    }

    private void collectFields(Object x, List<Surface> target, int depth) {
        Class cc = x.getClass();
        Reflect.on(cc).fields(true,false,false).forEach((s,ff)->{
            Field f = ff.get();
            try {
                Object y = f.get(x);
                if (y != null && y != x)
                    collect(y, target, depth, f.getName());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
//        for (Field f : cc.getFields()) {
//
//            int mods = f.getModifiers();
//            if (Modifier.isStatic(mods))
//                continue;
//            if (!Modifier.isPublic(mods))
//                continue;
//            if (f.getType().isPrimitive())
//                continue;
//
//            try {
//
//
//                f.trySetAccessible();
//
//
//                Object y = f.get(x);
//                if (y != null && y != x)
//                    collect(y, target, depth, f.getName());
//
//            } catch (Throwable t) {
//                t.printStackTrace();
//            }
//        }


    }

    private static class MySlider extends FloatSlider {
        private final String k;

        MySlider(FloatRange p, String k) {
            super(p);
            this.k = k;
        }


        @Override
        public String text() {
            return k + '=' + super.text();
        }
    }

    private static class MyIntSlider extends IntSlider {
        private final String k;

        MyIntSlider(IntRange p, String k) {
            super(p);
            this.k = k;
        }

        @Override
        public String text() {
            return k + '=' + super.text();
        }
    }

    private class AutoServices extends Widget {
        AutoServices(Services<?, ?> x) {

            List<Surface> l = new FasterList(x.size());

            x.entrySet().forEach((ks) -> {
                Service<?> s = ks.getValue();

                if (addService(s)) {
                    String label = s.toString(); 










                    l.add(
                            new PushButton(IconBuilder.simpleBuilder.apply(s)).click(()-> SpaceGraph.window(
                                    new LabeledPane(label, new AutoSurface(s)),
                                    500, 500))
                            



                                            












                    );
                }










            });

            content(new Gridding(l));
        }
    }

    private boolean add(Object x) {
        return seen.add(x);
    }

    private boolean addService(Service<?> x) {
        return add(x);
    }

}
