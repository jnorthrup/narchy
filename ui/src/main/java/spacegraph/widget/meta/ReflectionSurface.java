package spacegraph.widget.meta;

import com.google.common.collect.Sets;
import jcog.Services;
import jcog.list.FasterList;
import jcog.math.FloatParam;
import org.apache.commons.lang.StringUtils;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.slider.FloatSlider;
import spacegraph.widget.text.LabeledPane;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 2/28/17.
 */
public class ReflectionSurface<X> extends Grid {

    final Set<Object> seen = Sets.newSetFromMap(new IdentityHashMap());

    /** root */
    private final X obj;

    final static int MAX_DEPTH = 1;

    public ReflectionSurface(X x) {
        super();
        this.obj = x;

        List<Surface> l = new FasterList();

        collect(x, l, 0);

        children(l);
    }

    private void collect(Object y, List<Surface> l, int depth) {
        collect(y, l, depth, null);
    }

    private void collect(Object x, List<Surface> target, int depth, String yLabel /* tags*/) {

        if (!seen.add(x))
            return;

        if (yLabel == null)
            yLabel = x.toString();

        if (x instanceof Surface) {
            if (((Surface)x).parent==null) {
                //l.add(col(new Label(k), (Surface)y));
                if (yLabel!=null) {
                    target.add(new LabeledPane(yLabel, (Surface) x));
                } else
                    target.add((Surface) x);
            }
            return;
        }



        if (x instanceof FloatParam) {
            target.add(new MyFloatSlider((FloatParam) x, yLabel));
        } else if (x instanceof AtomicBoolean) {
            target.add(new CheckBox(yLabel, (AtomicBoolean) x));
//                    } else if (y instanceof MutableBoolean) {
//                        l.add(new CheckBox(k, (MutableBoolean) y));
        } else if (x instanceof Runnable) {
            target.add(new PushButton(yLabel, (Runnable) x));
        }


        if (depth < MAX_DEPTH) {

            if (x instanceof Services) { //first
                collectServices((Services) x, target);
            }

            collectFields(x, target, depth+1);

            if (x instanceof Collection) {
                Surface cx = collectElements((Collection) x, depth+1);
                if (cx != null) {
                    target.add(new LabeledPane(yLabel, cx));
                }
            }
        }

    }

    private Surface collectElements(Collection<?> x, int depth) {
        FasterList<Surface> m = new FasterList();
        for (Object o : x) {
            collect(o, m, depth);
        }
        return !m.isEmpty() ? grid(m) : null;
    }

    private void collectServices(Services x, List<Surface> l) {
        x.stream().forEach((s) -> {
            seen.add(s);
            String label = StringUtils.abbreviate(s.toString(), 16);
            l.add(new WindowToggleButton(label, () -> new ReflectionSurface(s)));
        });
    }

    public void collectFields(Object x, List<Surface> l, int depth) {
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
                if (y!=null && y != x) //avoid self loop
                    collect(y, l, depth, f.getName());

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static class MyFloatSlider extends FloatSlider {
        private final String k;

        public MyFloatSlider(FloatParam p, String k) {
            super(p);
            this.k = k;
        }

        @Override
        public String text() {
            return k + "=" + super.text();
        }
    }
}
