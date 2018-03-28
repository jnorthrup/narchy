package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.list.CircularArrayList;
import jcog.list.FasterList;
import jcog.util.Flip;
import org.jetbrains.annotations.NotNull;
import spacegraph.space2d.Surface;
import spacegraph.video.Draw;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static jcog.data.map.CustomConcurrentHashMap.*;

/**
 * @author Tadas Subonis <tadas.subonis@gmail.com>
 */
public class TreeChart<X> extends Surface {


//    private final boolean sort = false;
    private double heightLeft, widthLeft;

    @Deprecated private float top = 0, left = 0;
    @Deprecated private final float width = 1, height = 1;


    enum LayoutOrient {
        VERTICAL, HORIZONTAL
    }


    private LayoutOrient layoutOrient = LayoutOrient.HORIZONTAL;

    final Flip<CircularArrayList<ItemVis<X>>> phase = new Flip(
            ()->new CircularArrayList<>(512));


    public TreeChart() {

    }

    @Override
    protected void paint(GL2 gl, int dtMS) {
        Draw.bounds(gl, bounds, this::paint);
    }


    protected void paint(GL2 gl) {


        CircularArrayList<ItemVis<X>> read = phase.read();
        if (!read.isEmpty()) {
            for (ItemVis v : read) {
                v.paint(gl, v.area);
            }
        }
    }

//    public void update(Collection<? extends X> children, BiConsumer<X, ItemVis<X>> update) {
//        update(children, update, cached(i -> new ItemVis<>(i, i.toString())));
//    }

    public static <X> Function<X, ItemVis<X>> cached() {
        return cached(i -> new ItemVis<>(i, i.toString()));
    }

    public static <X> Function<X, ItemVis<X>> cached(Function<X, ItemVis<X>> vis) {
        return new Function<>() {
            final Map<X, ItemVis<X>> cache
                    = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 256);
//            final Cache<X, ItemVis<X>> cache
//                    Caffeine.newBuilder().maximumSize(1024).build(); //TODO just use LRUHashMap or something

            @Override
            public ItemVis<X> apply(X x) {
                return cache.computeIfAbsent(x, vis);
            }
        };
    }



    public void update(Iterable<? extends X> next, BiConsumer<X, ItemVis<X>> update, Function<X, ItemVis<X>> vis) {
        left = 0;
        top = 0;

        CircularArrayList<ItemVis<X>> display = phase.commit();
//        int cs = display.capacity();
//        if (cs < ns) {
//            display.clear(ns);
//        } else if (cs > ns*2) {
//            display.clear(ns); //shrink if more than 2x as large
//        } else {
            display.clear(); //just fine
        //}

        final float[] weight = {0};
        next.forEach(item -> {
            if (item == null)
                return; //TODO return false to stop the iteration

            ItemVis<X> e = vis.apply(item);
            if (e != null) {
                update.accept(item, e);
                float a = e.requestedArea();
                if (a > 0) {
                    weight[0] += a;
                    display.add(e);
                }
            }
        });

        display.sort(ItemVis::compareTo);


        int size = display.size();
        if (size > 0) {
            heightLeft = height;
            widthLeft = width;
            layoutOrient = width > height ? LayoutOrient.VERTICAL : LayoutOrient.HORIZONTAL;

            float areaNormalization = (width * height) / weight[0];
            display.forEach(c -> {
                c.area = c.requestedArea() * areaNormalization;
                //assert (c.area > Pri.EPSILON);
            });

            squarify(display, new CircularArrayList(size), minimumSide());

        } else {

        }

    }



    private void squarify(Collection<ItemVis<X>> display, Collection<ItemVis> row, double w) {

        CircularArrayList<ItemVis<X>> remaining = new CircularArrayList(display);
        ItemVis c = remaining.poll();
        if (c == null)
            return;

        FasterList<ItemVis> concatRow = concat(row, c);

        double worstConcat = worst(concatRow, w);
        double worstRow = worst(row, w);

        if (row.isEmpty() || (worstRow > worstConcat || Util.equals(worstRow, worstConcat, EPSILON))) {

            if (remaining.isEmpty()) {
                layoutrow(concatRow, w);
            } else {
                squarify(remaining, concatRow, w);
            }
        } else {
            layoutrow(row, w);
            squarify(display, Collections.emptyList(), minimumSide());
        }
    }

    private static FasterList<ItemVis> concat(Collection<ItemVis> row,  ItemVis c) {
        FasterList<ItemVis> concatRow = new FasterList<>(row.size() + 1);
        concatRow.addAll(row);
        concatRow.add(c);
        return concatRow;
    }

    private static double worst(Collection<ItemVis> ch, double w) {
        if (ch.isEmpty()) {
            return Double.MAX_VALUE;
        }
        double areaSum = 0.0, maxArea = 0.0, minArea = Double.MAX_VALUE;
        for (ItemVis item : ch) {
            double area = item.area;
            areaSum += area;
            minArea = minArea < area ? minArea : area;
            maxArea = maxArea > area ? maxArea : area;
        }
        double sqw = w * w;
        double sqAreaSum = areaSum * areaSum;
        return Math.max(sqw * maxArea / sqAreaSum,
                sqAreaSum / (sqw * minArea));
    }

    private void layoutrow(Iterable<ItemVis> row, double w) {



        double totalArea = 0.0;
        for (ItemVis item : row) {
            totalArea += item.area;
        }
//        assert(totalArea > 0);

        if (layoutOrient == LayoutOrient.VERTICAL) {


            double rowWidth = totalArea / w;
            //assert(rowWidth > 0);
            double topItem = 0;

            for (ItemVis item : row) {
                float area = item.area;
                //assert(area > 0);

                item.top = (float) (top + topItem);
                item.left = left;
                item.width = (float) rowWidth;
                float h = (float) (area / rowWidth);
                item.height = h;

                topItem += h;
            }
            widthLeft -= rowWidth;
            //this.heightLeft -= w;
            left += rowWidth;
            double minimumSide = minimumSide();
            if (!Util.equals(minimumSide, heightLeft, EPSILON)) {
                changeLayout();
            }
        } else {

            float rowHeight = (float) (totalArea / w);
            //assert(rowHeight > 0);
            float rowLeft = 0;

            for (ItemVis item : row) {
                float area = item.area;

                item.top = top;
                item.left = left + rowLeft;
                item.height = rowHeight;
                float wi = area / rowHeight;
                item.width = wi;

                rowLeft += wi;
            }
            //this.widthLeft -= rowHeight;
            heightLeft -= rowHeight;
            top += rowHeight;

            double minimumSide = minimumSide();
            if (!Util.equals(minimumSide, widthLeft, EPSILON)) {
                changeLayout();
            }
        }

    }

    private void changeLayout() {
        layoutOrient = layoutOrient == LayoutOrient.HORIZONTAL ? LayoutOrient.VERTICAL : LayoutOrient.HORIZONTAL;
    }

    private double minimumSide() {
        return Math.min(heightLeft, widthLeft);
    }


//    public static class WeightedString {
//        public final String label;
//        public final float weight;
//
//        public WeightedString(String label, float weight) {
//            this.label = label;
//            this.weight = weight;
//        }
//
//        @Override
//        public String toString() {
//            return label;
//        }
//
//        public static WeightedString w(String s, float w) {
//            return new WeightedString(s, w);
//        }
//    }

//	// private final ColorBucket colorBucket = ColorBucket.createBucket();
//	public final Item root;
//
//	public final DoubleProperty width = new SimpleDoubleProperty(640.0);
//	public final DoubleProperty height = new SimpleDoubleProperty(280.0);
//
//	private final TreemapLayout treemapLayouter;
//
//	public TreemapChart(Item root) {
//        this.root = root;
//        SortedSet<Item> items = root.content();
//        treemapLayouter = elementFactory.createTreemapLayout(width.doubleValue(), height.doubleValue(), items);
//        ChangeListener<Number> changeListener = (observableValue, number, number2) -> treemapLayouter.update(width.doubleValue(), height.doubleValue(), items);
//        width.addListener(changeListener);
//        height.addListener(changeListener);
//        getChildren().add(treemapLayouter);
//
//    }
//	public void update() {
//		treemapLayouter.update(width.doubleValue(), height.doubleValue(),
//				root.content());
//		autosize();
//	}
//
//	public DoubleProperty getWidth() {
//		return width;
//	}
//
//	public DoubleProperty getHeight() {
//		return height;
//	}

    /**
     * @author Tadas Subonis <tadas.subonis@gmail.com>
     *     TODO extend Surface
     */
    public static class ItemVis<X> implements Comparable<ItemVis> {

        public final String label;
        public final X item;
        private final static AtomicInteger serial = new AtomicInteger(0);
        private final int id;
        public float left;
        public float top;
        public float width;
        public float height;
        public float area, weight;
        private float r;
        private float g;
        private float b;

        public ItemVis(X item, String label) {
            this.id = serial.incrementAndGet();
            this.item = item;
            this.label = label;
            this.weight = 1; //default
            r = g = b = 0.5f;
        }

        public void update(float weight, float r, float g, float b) {
            this.weight = weight;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public float requestedArea() {
            return Math.abs(weight);
        }

        @Override
        public String toString() {
            return "TreemapDtoElement{" +
                    "label='" + label + '\'' +
                    ", area=" + area +
                    ", top=" + top +
                    ", left=" + left +
                    '}';
        }


        @Override
        public boolean equals(Object o) {
            return this == o;

            //if (o == null || getClass() != o.getClass()) return false;

            //        ItemVis that = (ItemVis) o;
            //
            //        if (item != null ? !item.equals(that.item) : that.item != null) return false;
            //        return !(label != null ? !label.equals(that.label) : that.label != null);

        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
            //        int result = label != null ? label.hashCode() : 0;
            //        result = 31 * result + (item != null ? item.hashCode() : 0);
            //        return result;
        }

        public void paint(GL2 gl, float percent) {
            float i = 0.25f + 0.75f * percent;

            if (r < 0) {
                r = i;
                g = 0.1f;
                b = 0.1f;
            }


            //float z = 0;


            gl.glColor3f(r, g, b);
            float m = 0.0f; //margin, space between cells
            Draw.rect(gl, left + m / 2, top + m / 2, width - m, height - m);

            float labelSize = 1f / (1 + label.length()); //Math.min(16, (float) (height * percent * 20f ) ); /// 4f * Math.min(0.5f,percent));

            /*if (area > 16f*label.length())*/ {

                gl.glLineWidth(1f);
                gl.glColor3f(1, 1, 1);

                Draw.text(gl, label,
                        labelSize * Math.min(width, height), //label size
                        left + width / 2, top + height / 2, 0f);
                //(float) (left + width / 2f), (float) (top + height / 2f), 0);

            }


        }

        @Override
        public int compareTo(@NotNull TreeChart.ItemVis o) {
            if (this == o) return 0;
            int i = Util.fastCompare(o.weight, weight);
            if (i == 0)
                return Integer.compare(id, o.id);
            else
                return i;
        }

        public void updateMomentum(float w, float speed, float r, float g, float b) {
            update(Util.lerp(speed, this.weight, w), r, g, b);
        }
    }

//	public static void main(String[] args) {
//		SpaceGraph<VirtualTerminal> s = new SpaceGraph<>();
//		s.show(800, 800);
//		TreeChart<WeightedString> tc = new TreeChart<>(500, 400,
//				(w, v) -> {
//					v.update(w.weight);
//				},
//				w("z", 0.25f),
//				w("x", 1f),
//				w("y", 0.5f),
//				w("a", 0.1f),
//				w("b", 0.08f),
//				w("c", 0.07f)
//		);
//		System.out.println(tc.children);
//		s.add(new Ortho(tc));
//	}

}
