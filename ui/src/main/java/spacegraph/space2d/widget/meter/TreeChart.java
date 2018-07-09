package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.list.CircularArrayList;
import jcog.list.FasterList;
import jcog.tree.rtree.Spatialization;
import jcog.util.Flip;
import org.jetbrains.annotations.NotNull;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
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



    private double heightLeft, widthLeft;

    @Deprecated private float top = 0, left = 0;
    @Deprecated private final float width = 1, height = 1;


    enum LayoutOrient {
        VERTICAL, HORIZONTAL
    }


    private LayoutOrient layoutOrient = LayoutOrient.HORIZONTAL;

    private final Flip<CircularArrayList<ItemVis<X>>> phase = new Flip(
            ()->new CircularArrayList<>(512));


    public TreeChart() {

    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        Draw.bounds(gl, bounds, this::paint);
    }


    private void paint(GL2 gl) {


        CircularArrayList<ItemVis<X>> read = phase.read();
        if (!read.isEmpty()) {
            for (ItemVis v : read) {
                v.paint(gl, v.area);
            }
        }
    }





    public static <X> Function<X, ItemVis<X>> cached() {
        return cached(i -> new ItemVis<>(i, i.toString()));
    }

    static <X> Function<X, ItemVis<X>> cached(Function<X, ItemVis<X>> vis) {
        return new Function<>() {
            final Map<X, ItemVis<X>> cache
                    = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 256);



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






            display.clear(); 
        

        final float[] weight = {0};
        next.forEach(item -> {
            if (item == null)
                return; 

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
            display.forEach(c -> c.area = c.requestedArea() * areaNormalization);

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

        if (row.isEmpty() || (worstRow > worstConcat || Util.equals(worstRow, worstConcat, Spatialization.EPSILONf))) {

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


        if (layoutOrient == LayoutOrient.VERTICAL) {


            double rowWidth = totalArea / w;
            
            double topItem = 0;

            for (ItemVis item : row) {
                float area = item.area;
                

                item.top = (float) (top + topItem);
                item.left = left;
                item.width = (float) rowWidth;
                float h = (float) (area / rowWidth);
                item.height = h;

                topItem += h;
            }
            widthLeft -= rowWidth;
            
            left += rowWidth;
            double minimumSide = minimumSide();
            if (!Util.equals(minimumSide, heightLeft, Spatialization.EPSILONf)) {
                changeLayout();
            }
        } else {

            float rowHeight = (float) (totalArea / w);
            
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
            
            heightLeft -= rowHeight;
            top += rowHeight;

            double minimumSide = minimumSide();
            if (!Util.equals(minimumSide, widthLeft, Spatialization.EPSILONf)) {
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





















































    /**
     * @author Tadas Subonis <tadas.subonis@gmail.com>
     *     TODO extend Surface
     */
    public static class ItemVis<X> implements Comparable<ItemVis> {

        final String label;
        final X item;
        private final static AtomicInteger serial = new AtomicInteger(0);
        private final int id;
        float left;
        float top;
        float width;
        float height;
        float area;
        float weight;
        private float r;
        private float g;
        private float b;

        public ItemVis(X item, String label) {
            this.id = serial.incrementAndGet();
            this.item = item;
            this.label = label;
            this.weight = 1; 
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

            

            
            
            
            

        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
            
            
            
        }

        void paint(GL2 gl, float percent) {
            float i = 0.25f + 0.75f * percent;

            if (r < 0) {
                r = i;
                g = 0.1f;
                b = 0.1f;
            }


            


            gl.glColor3f(r, g, b);
            float m = 0.0f; 
            Draw.rect(gl, left + m / 2, top + m / 2, width - m, height - m);

            float labelSize = 1f / (1 + label.length()); 

            /*if (area > 16f*label.length())*/ {

                gl.glLineWidth(1f);
                gl.glColor3f(1, 1, 1);

                Draw.text(gl, label,
                        labelSize * Math.min(width, height), 
                        left + width / 2, top + height / 2, 0f);
                

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



















}
