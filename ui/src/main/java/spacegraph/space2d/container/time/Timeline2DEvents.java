package spacegraph.space2d.container.time;

import jcog.data.list.FasterList;
import jcog.tree.rtree.rect.RectFloat;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;

import java.util.List;
import java.util.function.Consumer;

/** layers in which discretely renderable or interactable events
 * which can be materialized as arrangeable clips */
public class Timeline2DEvents<E> extends Graph2D<E> implements Timeline2D.TimeRangeAware {

    long start, end;

    public final Timeline2D.TimelineEvents<E> model;



    public Timeline2DEvents(Timeline2D.TimelineEvents<E> model, Consumer<NodeVis<E>> view, Graph2DUpdater<E> u) {
        super();
        this.model = model;
        build(view);

        update(u);
    }

    private Timeline2DEvents update() {
        set(model.events((long) Math.floor(start), (long) Math.ceil(end/* - 1 ?*/)));
        return this;
    }


    @Override
    public void setTime(long start, long end) {
        this.start = start; this.end = end;
        update();
    }

    public static class LinearTimelineUpdater<E> implements Graph2DUpdater<E> {
        /** proportional to timeline */
        protected float minVisibleWidth = 0.01f;

        /** minimum displayed temporal width, for tasks less than this duration */
        protected final double minVisibleTime = 0; //0.5f;


        @Override
        public void update(Graph2D<E> g, float dtS) {


            Timeline2DEvents gg = (Timeline2DEvents) g;
            Timeline2D.TimelineEvents model = gg.model;
            float yl = g.bottom(), yh = g.top();

            g.forEachValue(t -> {
                layout(t, gg, model, minVisibleWidth, yl, yh);
            });
        }

        protected void layout(NodeVis<E> jj, Timeline2DEvents gg, Timeline2D.TimelineEvents model, float minVisibleWidth, float yl, float yh) {
            long[] w = model.range(jj.id);
            long left = (w[0]), right = (w[1]);
            if (right-left < minVisibleTime) {
                double mid = (left + right)/2f;
                left = Math.round(mid - minVisibleTime /2);
                right = Math.round(mid + minVisibleTime /2);
            }


            float xl = gg.x(left);
            float xr = gg.x(right);
            if (xr -xl < minVisibleWidth) {
                float xc = (xl+xr);
                xl = xc - minVisibleWidth/2;
                xr = xc + minVisibleWidth/2;
            }
            RectFloat r = RectFloat.XYXY(xl, yl, xr, yh);
            jj.pos(r);
            jj.show();
        }
    }

    /** staggered lane layout */
    public static class LaneTimelineUpdater<E> extends LinearTimelineUpdater<E> {

        final FasterList<NodeVis<E>> next = new FasterList();

        @Override
        public void update(Graph2D<E> g, float dtS) {
            next.clear();

            g.forEachValue(t -> {
                if (t.id != null) {
                    next.add(t);
                }
            });
            if (next.isEmpty())
                return;


            Timeline2DEvents gg = (Timeline2DEvents) g;
            Timeline2D.TimelineEvents model = gg.model;

            next.sortThis((x, y) -> model.compareDurThenStart(x.id, y.id));


            List<RoaringBitmap> lanes = new FasterList();
            RoaringBitmap l0 = new RoaringBitmap();
            l0.add(0);
            lanes.add(l0);

            for (int i = 1, byDurationSize = next.size(); i < byDurationSize; i++) {
                NodeVis<E> in = next.get(i);

                int lane = -1;
                nextLane:
                for (int l = 0, lanesSize = lanes.size(); l < lanesSize; l++) {
                    RoaringBitmap r = lanes.get(l);
                    PeekableIntIterator rr = r.getIntIterator();
                    boolean collision = false;
                    while (rr.hasNext()) {
                        int j = rr.next();
                        if (model.intersectLength(next.get(j).id, in.id) > 0) {
                            collision = true;
                            break;
                        }
                    }
                    if (!collision) {
                        lane = l;
                        r.add(i);
                        break;
                    }
                }
                if (lane == -1) {
                    RoaringBitmap newLane = new RoaringBitmap();
                    newLane.add(i);
                    lanes.add(newLane);
                }
            }

            int nlanes = lanes.size();
            float laneHeight = g.h() / nlanes;
            float Y = g.top();
            float minVisibleWidth = g.w() * this.minVisibleWidth;
            for (int i = 0; i < nlanes; i++) {
                float yl = Y + laneHeight * i;
                float yh = Y + laneHeight * (i + 1);

                layout(gg, model, lanes.get(i), minVisibleWidth, yl, yh);
            }
        }

        void layout(Timeline2DEvents gg, Timeline2D.TimelineEvents model, RoaringBitmap ri, float minVisibleWidth, float yl, float yh) {
            PeekableIntIterator ii = ri.getIntIterator();
            while (ii.hasNext()) {
                int j = ii.next();
                NodeVis<E> jj = next.get(j);
                layout(jj, gg, model, minVisibleWidth, yl, yh);
            }
        }


    }

    protected float x(long t) {
        return Timeline2D.x(t, x(), w(), start, end);
    }

}
