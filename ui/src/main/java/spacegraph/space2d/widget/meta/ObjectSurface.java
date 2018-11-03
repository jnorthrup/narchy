package spacegraph.space2d.widget.meta;

import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.math.MutableEnum;
import jcog.reflect.AutoBuilder;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.MutableUnitContainer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.IntSlider;
import spacegraph.space2d.widget.tab.ButtonSet;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.video.Draw;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * constructs a representative surface for an object by reflection analysis
 */
public class ObjectSurface<X> extends MutableUnitContainer {

    final AutoBuilder<Object, Surface> builder;


    /**
     * root
     */
    private final X obj;

    public ObjectSurface(X x) {
        this(x, 1);
    }

    public ObjectSurface(X x, int maxDepth) {
        super();

        this.obj = x;
        AutoBuilder.AutoBuilding<Object,Surface> building = (List<Pair<Object, Iterable<Surface>>> content, @Nullable Object parent, @Nullable Surface parentRepr) -> {
            List<Surface> c =  new FasterList(content.size());
            for (Pair<Object, Iterable<Surface>> p : content) {
                //Object o = p.getOne();
                ArrayList<Surface> cx = Lists.newArrayList(p.getTwo());
                switch(cx.size()) {
                    case 0: break; //TODO shouldnt happen
                    case 1: c.add(cx.get(0)); break;
                    default:
                        //TODO selector
                        c.add(new Gridding(cx));
                        break;
                }
            }

            if (c.isEmpty())
                return null;

            return new ObjectMetaFrame(parent, c.size() > 1 ? new Gridding(c) : c.get(0));
        };

        builder = new AutoBuilder<Object, Surface>(maxDepth, building);

        initDefaults();
    }

    private void initDefaults() {
        builder.on(FloatRange.class, (x,relation)-> new MySlider((FloatRange) x, relationLabel(relation)));
        builder.on(Runnable.class, (x,relation)-> new PushButton(relationLabel(relation), (Runnable) x));
        builder.on(AtomicBoolean.class, (x,relation) -> new MyAtomicBooleanCheckBox(relationLabel(relation), (AtomicBoolean) x));

        builder.on(MutableEnum.class, (x, relation) -> newSwitch((MutableEnum) x, relationLabel(relation)));
        builder.on(IntRange.class,  (x, relation) -> !(x instanceof MutableEnum) ? new MyIntSlider((IntRange) x, relationLabel(relation)) : null);

        builder.on(Collection.class, (x, relation) -> {
            Collection cx = (Collection) x;
            if (cx.isEmpty())
                return null;

            List<Surface> yy = new FasterList(cx.size());

            for (Object cxx : cx) {
                Surface yyy = builder.build(cxx);
                if (yyy!=null)
                    yy.add(yyy); //TODO depth, parent, ..
            }
            if (yy.isEmpty())
                return null;

            Surface xx = new Gridding(yy);

            String l = relationLabel(relation);

            if (!l.isEmpty())
                return new LabeledPane(l, xx);
            else
                return xx;
        });
    }

    public String relationLabel(@Nullable Object relation) {
        if (relation == null) return "";
        else if (relation instanceof Field) {
            return ((Field) relation).getName();
        } else {
            return relation.toString(); //???
        }
    }


    @Override
    public boolean start(@Nullable SurfaceBase parent) {

        if (super.start(parent)) {

            builder.clear();

            set(builder.build(obj));

            return true;
        }
        return false;
    }

    protected String label(X obj) {
        return obj.toString();
    }

//        if (yLabel == null)
//            yLabel = x.toString();
//
//        if (x instanceof Surface) {
//            Surface sx = (Surface) x;
//            if (sx.parent == null) {
//                target.add(new LabeledPane(yLabel, sx));
//            }
//            return;
//        }
//
//
//        //TODO rewrite these as pluggable onClass handlers
//
//        if (x instanceof Services) {
//            target.add(new AutoServices((Services) x));
//            return;
//        }
//
//        if (x instanceof Collection) {
//            Surface cx = collectElements((Iterable) x, depth + 1);
//            if (cx != null) {
//                target.add(new LabeledPane(yLabel, cx));
//            }
//        }
//
//




    private <C extends Enum<C>> Surface newSwitch(MutableEnum x, String label) {
        EnumSet<C> s = EnumSet.allOf(x.klass);

        Enum initialValue = x.get();
        int initialButton = -1;

        ToggleButton[] b = new ToggleButton[s.size()];
        int i = 0;
        for (C xx : s) {
            CheckBox tb = new CheckBox(xx.name());
            tb.on((c, enabled) -> {
                if (enabled)
                    x.set(xx);
            });
            if (xx == initialValue)
                initialButton = i;
            b[i++] = tb;
        }


//JDK12 compiler error has trouble with this:
//        ToggleButton[] b = ((EnumSet) EnumSet.allOf(x.klass)).stream().map(e -> {
//            CheckBox tb = new CheckBox(e.name());
//            tb.on((c, enabled) -> {
//                if (enabled)
//                    x.set(e);
//            });
//            return tb;
//        }).toArray(ToggleButton[]::new);
//
        ButtonSet bs = new ButtonSet(ButtonSet.Mode.One, b);

        if (initialButton != -1) {
            b[initialButton].set(true);
        }

        return new LabeledPane(label, bs);
    }


    public static class ObjectMetaFrame extends MetaFrame {
        public final Object instance;
        public final Surface surface;
        private final int instanceHash;

        public ObjectMetaFrame(Object instance, Surface surface) {
            super(surface);
            if (instance instanceof Surface)
                throw new TODO();
            this.instance = instance;
            this.instanceHash = instance.hashCode();
            this.surface = surface;
        }

        @Override
        protected void paintBelow(GL2 gl, SurfaceRender r) {
            super.paintBelow(gl, r);
            Draw.colorHash(gl, instanceHash, 0.25f);
            Draw.rect(bounds, gl);
        }

        @Override protected String name() {
            return instance != null ? instance.toString() : "";
        }


        //TODO other inferred features
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

    private static class MyAtomicBooleanCheckBox extends CheckBox {
        final AtomicBoolean a;

        public MyAtomicBooleanCheckBox(String yLabel, AtomicBoolean x) {
            super(yLabel, x);
            this.a = x;
        }

        @Override
        public boolean prePaint(SurfaceRender r) {
            set((a.getOpaque())); //load
            return super.prePaint(r);
        }
    }

//    private class AutoServices extends Widget {
//        AutoServices(Services<?, ?> x) {
//
//            List<Surface> l = new FasterList(x.size());
//
//            x.entrySet().forEach((ks) -> {
//                Service<?> s = ks.getValue();
//
//                if (addService(s)) {
//                    String label = s.toString();
//
//
//                    l.add(
//                            new PushButton(IconBuilder.simpleBuilder.apply(s)).click(() -> SpaceGraph.window(
//                                    new LabeledPane(label, new ObjectSurface(s)),
//                                    500, 500))
//
//
//                    );
//                }
//
//
//            });
//
//            set(new ObjectMetaFrame(x, new Gridding(l)));
//        }
//    }


    /*private boolean addService(Service<?> x) {
        return add(x);
    }*/

}
