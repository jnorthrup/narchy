package spacegraph.tool;

import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.layout.Flatten;
import spacegraph.layout.Gridding;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.constraint.DistanceConstraint;
import spacegraph.phys.shape.SimpleBoxShape;
import spacegraph.space.Cuboid;
import spacegraph.space.ListSpace;
import spacegraph.widget.slider.XYSlider;

/**
 * Created by me on 7/23/16.
 */
public class TestP2PNode extends Cuboid {

    public TestP2PNode(float w, float h) {
        super(new Gridding(), w, h);
    }

    public static class TestP2PLink extends Cuboid {

        private final SimpleSpatial a;
        private final SimpleSpatial b;
        private DistanceConstraint p2p;

        public TestP2PLink(SimpleSpatial<?> a, SimpleSpatial<?> b) {
            super(new XYSlider(), 10, 1);
            this.a = a;
            this.b = b;

        }

        @Override
        public boolean collidable() {
            return false;
        }

//        @Override
//        protected void next(Dynamics world) {
//            v3 c = new v3(a.transform());
//            c.add(b.transform());
//            c.scale(0.5f);
//            move(c);
//
//            float dist = v3.dist(a.transform(), b.transform());
//            scale(dist, 1, 1);
//
//            a.body.activate();
//
//            b.body.activate();
//        }


        @Override
        protected Dynamic create(Dynamics world) {
            Dynamic l = super.create(world);

            float w = ((SimpleBoxShape) body.shape()).x();

            p2p = new DistanceConstraint(
                    a.body, b.body, 5f, 0.75f, 0.1f, 0.75f
            );
            world.addConstraint(p2p, true);

//            world.addConstraint(new Point2PointConstraint(
//                        body, a.body, v(+w / 2, 0, 0), v(-a.radius / 2f, 0, 0)
//                ), true);
//                world.addConstraint(new Point2PointConstraint(
//                        body, b.body, v(-w / 2, 0, 0), v(+a.radius / 2f, 0, 0)
//                ), true);


            //            float w = ((BoxShape)body.shape()).x();
//            v3 axis = v(0, 0, 1);
//            s.dyn.addConstraint(
//                new HingeConstraint(body, a.body, v(+w/2,0,0), v(-a.radius/2f,0,0), axis, axis
//            ), true);
//            s.dyn.addConstraint(
//                new HingeConstraint(body, b.body, v(-w/2,0,0), v(+b.radius/2f,0,0), axis, axis
//            ), true);


            return l;
        }


    }

    public static void main(String[] args) {
        SpaceGraph s = new SpaceGraph<>();

        TestP2PNode a = new TestP2PNode(5, 3);
        a.move(2, 0, 0);
        TestP2PNode b = new TestP2PNode(3, 4);

        TestP2PLink ab = new TestP2PLink(a, b);

        s.add(a, b, ab);
        s.add(new ListSpace().with(new Flatten(0.2f, 0.25f))); //HACK TODO make a Transform-only space input

        s.show(1000, 800)
                .setWindowDestroyNotifyAction(() -> System.exit(1));

    }

}
