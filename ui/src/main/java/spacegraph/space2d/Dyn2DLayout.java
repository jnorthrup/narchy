package spacegraph.space2d;

import jcog.data.list.FasterList;
import spacegraph.space2d.container.DynamicLayout2D;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.BodyType;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.phys.dynamics.FixtureDef;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.util.MovingRectFloat2D;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.List;

/** 2d physics dynamic layout
 * TODO unfinished */
public class Dyn2DLayout<E> extends DynamicLayout2D<E, MovingRectFloat2D> {





    private float scale = 0.01f;

    private final Dynamics2D W = new Dynamics2D();

    @Deprecated List<Body2D> bb = new FasterList();

    @Override
    protected MovingRectFloat2D newContainer() {
        return new MovingRectFloat2D();
    }

    @Override
    protected void layoutDynamic(Graph2D<E> g) {



        float bo = g.h()*scale/10;
        W.addStatic(new FixtureDef(PolygonShape.box(0, g.y()-bo, g.w()*scale, g.y()), 0.9f, 0));
        W.addStatic(new FixtureDef(PolygonShape.box(0, g.y()+g.h(), g.w()*scale, g.y()+g.h()+bo), 0.9f, 0));

        for (MovingRectFloat2D b : bounds) {
            FixtureDef fd = new FixtureDef(PolygonShape.box(b.w * scale * 2, b.h * scale * 2), 1f, 0.9f);

            Body2D bbb = W.addBody(
                    new Body2D(BodyType.DYNAMIC, W),
                    fd
            );
            bbb.set(new v2(b.x * scale, b.y * scale), 0);
            bbb.setLinearDamping(0.25f);
            bbb.setFixedRotation(true);
            bb.add(bbb);
        }

        W.sync();
        for (int i = 0; i < 4; i++)
            W.step(0.5f, 4, 4);

        for (int i = 0, boundsSize = bounds.size(); i < boundsSize; i++) {
            MovingRectFloat2D b = bounds.get(i);
            Body2D bbb = bb.get(i);
            Tuple2f pos = bbb.getPosition();
            b.pos(pos.x/scale, pos.y/scale);

            
            
        }
        bb.clear();

        W.bodies().forEach(W::removeBody);
        W.sync();
        W.clearForces();

    }
}
