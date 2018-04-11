package spacegraph.space2d.widget.meta;

import com.google.common.collect.Sets;
import jcog.Service;
import jcog.Services;
import jcog.event.Ons;
import jcog.list.FasterList;
import jcog.math.EnumParam;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.IntSlider;
import spacegraph.space2d.widget.tab.ButtonSet;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.windo.Widget;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 2/28/17.
 */
public class AutoSurface<X> extends Gridding {

    final static int MAX_DEPTH = 1;
    final Set<Object> seen = Sets.newSetFromMap(new IdentityHashMap());
    /**
     * root
     */
    private final X obj;
    Ons ons = null;

    public AutoSurface(X x) {
        super();
        this.obj = x;
    }

    @Override
    public void start(@Nullable SurfaceBase parent) {


        synchronized (this) {

            seen.clear();
            ons = new Ons();
            List<Surface> l = new FasterList();
            collect(obj, l, 0);

            super.start(parent);

            set(l);
        }
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

        if (x instanceof Services) { //first
            target.add(new AutoServices((Services) x));
            return;
        }

        if (x instanceof Collection) {
            Surface cx = collectElements((Iterable<?>) x, depth + 1);
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
//                    } else if (y instanceof MutableBoolean) {
//                        l.add(new CheckBox(k, (MutableBoolean) y));
        } else if (x instanceof Runnable) {
            target.add(new PushButton(yLabel, (Runnable) x));
        } else if (x instanceof EnumParam) {
            target.add(newSwitch((EnumParam) x));
        }

        if (depth < MAX_DEPTH) {
            collectFields(x, target, depth + 1);
        }
    }

    private ButtonSet newSwitch(EnumParam x) {

        ToggleButton[] b = ((EnumSet<?>) EnumSet.allOf(x.klass)).stream().map(e -> {
            CheckBox tb = new CheckBox(e.name());
            tb.on((c, enabled) -> {
                if (enabled)
                    x.set(e);
            });
            return tb;
        }).toArray(ToggleButton[]::new);

        ButtonSet s = new ButtonSet(ButtonSet.Mode.One, b);
        return s;
    }

    private Surface collectElements(Iterable<?> x, int depth) {
        FasterList<Surface> m = new FasterList();
        for (Object o : x) {
            collect(o, m, depth);
        }
        return !m.isEmpty() ? grid(m) : null;
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (ons != null) {
                ons.off();
                ons = null;
            }
            super.stop();
        }
    }

    void collectFields(Object x, List<Surface> target, int depth) {
        Class cc = x.getClass();
        for (Field f : cc.getFields()) {
            //SuperReflect.fields(x, (String k, Class c, SuperReflect v) -> {
            int mods = f.getModifiers();
            if (Modifier.isStatic(mods))
                continue;
            if (!Modifier.isPublic(mods))
                continue;
            if (f.getType().isPrimitive())
                continue;

            try {

                //Class c = f.getType();
                f.trySetAccessible();


                Object y = f.get(x);
                if (y != null && y != x) //avoid self loop
                    collect(y, target, depth, f.getName());

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }


    }

    private static class MySlider extends FloatSlider {
        private final String k;

        public MySlider(FloatRange p, String k) {
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

        public MyIntSlider(IntRange p, String k) {
            super(p);
            this.k = k;
        }

        @Override
        public String text() {
            return k + '=' + super.text();
        }
    }

    private class AutoServices extends Widget {
        public AutoServices(Services<?, ?> x) {

            List<Surface> l = new FasterList(x.size());

            x.entrySet().forEach((ks) -> {
                Service<?> s = ks.getValue();

                if (addService(s)) {
                    String label = s.toString(); //StringUtils.abbreviate(s.toString(), 16);
//                FloatSlider fs = new FloatSlider(s.pri(), 0f, 1f).on((f, v) -> {
//                    if (v < 0.01f) {
//                        x.off(key);
//                    } else {
//                        x.on(key, v);
//                        //TODO set aux power parameter
//                    }
//                });
//                controls.put(s, fs);

                    l.add(
                            new PushButton(IconBuilder.simpleBuilder.apply(s)).click(()->{
                                SpaceGraph.window(
                                        new LabeledPane(label, new AutoSurface(s)),
                                        500, 500);
                            })
                            //new Cover(

//                                    () -> new PushButton(
//                                            label)
                                            //yLabel!=null ? yLabel : sx.toString(),
//                                            new Gridding(
//                                                    //enable
////                                                        AllOrNothingSlider.AllOrNothingSlider(fs),
////                                new CheckBox("On").set(s.isOn()).on((ToggleButton tb, boolean on)->{
////                                    if (on) {
////                                        x.on(key);
////                                    } else {
////                                        x.off(key);
////                                    }
////                                }),
//                                                    new WindowToggleButton("..", () -> s)
//                                            )))
                    );
                }

//            ons.add(x.change.on((co) -> {
//                Services.Service<Object> z = co.getOne();
//                FloatSlider c = controls.get(z);
//                if (c != null) {
//                    c.valueRelative(
//                            co.getTwo() ? Util.round(z.pri(), 0.01f) : 0
//                    );
//                }
//            }));
            });

            content(new Gridding(0.25f, l));
        }
    }

    protected boolean add(Object x) {
        return seen.add(x);
    }

    protected boolean addService(Service<?> x) {
        return add(x);
    }

}
