package spacegraph.space2d.container.time;

import jcog.data.list.FasterList;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;
import spacegraph.space2d.container.graph.Graph2D;

import java.util.List;
import java.util.function.Consumer;

/** layers in which discretely renderable or interactable events can be materialized */
public class Timeline2DEvents<E> extends Graph2D<E> implements Timeline2D.TimelineRenderable {

    double tStart, tEnd;

    private final Timeline2D.TimelineModel<E> model;

    /** minimum displayed temporal width, for tasks less than this duration */
    private final float timeVisibleEpsilon = 0.5f;


    public Timeline2DEvents(Timeline2D.TimelineModel<E> model, Consumer<NodeVis<E>> view) {
        super();
        this.model = model;
        build(view);

        update(new DefaultTimelineUpdater());
    }

    private Timeline2DEvents update() {
        set(model.events((long) Math.floor(tStart), (long) Math.ceil(tEnd - 1)));
        return this;
    }


    @Override
    public void setTime(double start, double end) {
        this.tStart = start; this.tEnd = end;
        update();
    }

    private class DefaultTimelineUpdater implements Graph2DUpdater<E> {


        FasterList<NodeVis<E>> next = new FasterList<>();

        @Override
        public void update(Graph2D<E> g, int dtMS) {
            next.clear();

            g.forEachValue(t -> {
                if (t.id!=null) {
                    next.add(t);
                }
            });
            if (next.isEmpty())
                return;

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
            for (int i = 0; i < nlanes; i++) {
                RoaringBitmap ri = lanes.get(i);
                PeekableIntIterator ii = ri.getIntIterator();
                while (ii.hasNext()) {
                    int j = ii.next();
                    NodeVis<E> jj = next.get(j);
                    long[] w = model.range(jj.id);
                    float left = (w[0]), right = (w[1]);
                    if (right-left < timeVisibleEpsilon) {
                        float mid = (left + right)/2f;
                        left = mid - timeVisibleEpsilon /2;
                        right = mid + timeVisibleEpsilon /2;
                    }

                    jj.show();
                    jj.pos(x(left), Y + laneHeight * i, x(right), Y + laneHeight * (i + 1));
                }
            }
        }
    }

    protected float x(float left) {
        return Timeline2D.x(tStart, tEnd, x(), w(), left );
    }

}
