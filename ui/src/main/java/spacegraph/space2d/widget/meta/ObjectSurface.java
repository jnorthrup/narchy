package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.math.MutableEnum;
import jcog.reflect.AutoBuilder;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.EnumSwitch;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.port.FloatRangePort;
import spacegraph.space2d.widget.slider.IntSlider;
import spacegraph.space2d.widget.text.AbstractLabel;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * constructs a representative surface for an object by reflection analysis
 */
public class ObjectSurface<X> extends MutableUnitContainer {

    private static final AutoBuilder.AutoBuilding<Object, Surface> DefaultObjectSurfaceBuilder = (@Nullable Object ctx, List<Pair<Object, Iterable<Surface>>> target, @Nullable Object obj) -> {
        List<Surface> outer = new FasterList<>(target.size()) {
            @Override
            protected Object[] newArray(int newCapacity) {
                return new Surface[newCapacity]; //HACK
            }
        };
        for (Pair<Object, Iterable<Surface>> p : target) {
            List<Surface> cx = new SurfaceList();
            p.getTwo().forEach(e -> {
                assert(e!=null);
                cx.add(e);
            });
            switch (cx.size()) {
                case 0:
                    break; //TODO shouldnt happen
                case 1:
                    outer.add(cx.get(0));
                    break;
                default:
                    //TODO selector
                    outer.add(new Gridding(cx));
                    break;
            }
        }

        switch (outer.size()) {
            case 0:
                return null;
            case 1:
                //outer.add(new Scale(cx.get(0), Widget.marginPctDefault));
                return outer.get(0);
            default:
                return new Gridding(outer);
        }

        //return new ObjectMetaFrame(obj, y, context);

    };

    final AutoBuilder<Object, Surface> builder;


    /**
     * root
     */
    private final X obj;

    public static ObjectSurface<Object> the(Object x) {
        return new ObjectSurface<>(x);
    }

    public static ObjectSurface<Object> the(Object... x) {
        return new ObjectSurface<>(List.of(x));
    }


    public ObjectSurface(X x) {
        this(x, 1);
        if (x == null)
            throw new NullPointerException();
    }

    public ObjectSurface(X x, int depth) {
        this(x, DefaultObjectSurfaceBuilder, depth);
    }

    public ObjectSurface(X x, AutoBuilder.AutoBuilding<Object, Surface> builder, int maxDepth) {
        this(x, new AutoBuilder<>(maxDepth, builder));
    }

    public ObjectSurface(X x, AutoBuilder<Object, Surface> builder) {
        super();

        this.obj = x;
        this.builder = builder;


        initDefaults();
    }

    private void initDefaults() {
//        builder.annotation(Essence.class, (x, xv, e) -> {
//           return xv; //forward  //TODO
//        });

        builder.on(Map.Entry.class, (Map.Entry x, Object relation) ->
                new VectorLabel(x.toString())
        );
        builder.on(FloatRange.class, (FloatRange x, Object relation) -> {
            FloatRangePort f = new FloatRangePort(x);
            f.slider.text(objLabel(x, relation));
            return f;
        });

        builder.on(IntRange.class, (x, relation) -> !(x instanceof MutableEnum) ? new MyIntSlider(x, relationLabel(relation)) : null);

        builder.on(Runnable.class, (x, relation) -> new PushButton(objLabel(x, relation), x));
        builder.on(AtomicBoolean.class, (x, relation) -> new MyAtomicBooleanCheckBox(objLabel(x, relation), x));

        builder.on(MutableEnum.class, (x, relation) -> EnumSwitch.the(x, relationLabel(relation)));

        builder.on(String.class, (x, relation) -> new VectorLabel(x)); //TODO support multi-line word wrap etc

        builder.on(Collection.class, (x, relation) -> {
            Collection cx = x;
            if (cx.isEmpty())
                return null;

            List<Surface> yy = new FasterList(cx.size());

            for (Object cxx : cx) {
                if (cxx == null)
                    continue;

                Surface yyy = builder.build(cxx);
                if (yyy != null)
                    yy.add(yyy); //TODO depth, parent, ..
            }
            if (yy.isEmpty())
                return null;

            Surface xx = yy.size() > 1 ? new Gridding(yy) : yy.get(0);

            String l = relationLabel(relation);

            if (!l.isEmpty())
                return LabeledPane.the(l, xx);
            else
                return xx;
        });

//        builder.on(Pair.class, (p, rel)->{
//           return new Splitting(build(p.getOne()), 0.5f, build(p.getTwo())).resizeable();
//        });
    }

    public Surface build(Object x) {
        return builder.build(x);
    }

    public String objLabel(Object x, Object relation) {
        return relation == null ? x.toString() : relationLabel(relation);
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
    protected void starting() {
        super.starting();

        builder.clear();

        set(builder.build(obj));
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
//                target.addAt(new LabeledPane(yLabel, sx));
//            }
//            return;
//        }
//
//
//        //TODO rewrite these as pluggable onClass handlers
//
//        if (x instanceof Services) {
//            target.addAt(new AutoServices((Services) x));
//            return;
//        }
//
//        if (x instanceof Collection) {
//            Surface cx = collectElements((Iterable) x, depth + 1);
//            if (cx != null) {
//                target.addAt(new LabeledPane(yLabel, cx));
//            }
//        }
//
//


    public static class ObjectMetaFrame extends MetaFrame {
        public final Object instance;
        public final Surface surface;
        private final int instanceHash;
        private final Object context;

        public ObjectMetaFrame(Object instance, Surface surface, Object context) {
            super(surface);
            if (instance instanceof Surface)
                throw new TODO();
            this.context = context;
            this.instance = instance;
            this.instanceHash = instance.hashCode();
            this.surface = surface;
        }


        @Override
        protected void paintIt(GL2 gl, ReSurface r) {
            super.paintIt(gl, r);
            Draw.colorHash(gl, instanceHash, 0.25f);
            Draw.rect(bounds, gl);
        }

        @Override
        @Nullable
        protected Surface label() {
            String s;
            if (context instanceof Field) {
                s = ((Field) context).getName();
            } else {
                s = context != null ? context.toString() : (instance != null ? instance.toString() : null);
            }

            Surface provided = super.label();
            if (s == null) {
                return provided;
            } else {
                AbstractLabel l = new VectorLabel(s);
                if (provided == null) return l;
                else return Splitting.row(l, 0.3f, provided);
            }
        }


        //TODO other inferred features
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
        public boolean preRender(ReSurface r) {
            on((a.getOpaque())); //load
            return super.preRender(r);
        }
    }

    public static class SurfaceList extends FasterList {
        public SurfaceList() {
            super(0);
        }

        @Override
        protected Object[] newArray(int newCapacity) {
            return new Surface[newCapacity]; //HACK
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
//                    l.addAt(
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
//            setAt(new ObjectMetaFrame(x, new Gridding(l)));
//        }
//    }


    /*private boolean addService(Service<?> x) {
        return addAt(x);
    }*/

}
