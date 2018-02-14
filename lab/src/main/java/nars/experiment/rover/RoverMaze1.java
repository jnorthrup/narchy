package nars.experiment.rover;

import nars.NARS;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.constraint.HingeConstraint;
import spacegraph.phys.shape.BoxShape;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.CylinderShape;
import spacegraph.space.Maze;

import static spacegraph.math.v3.v;

/**
 * Created by me on 9/12/16.
 */
public class RoverMaze1 {


    public static void main(String[] args) {
        Rover r = new Rover(new NARS().get()) {

            @Override protected void create(Dynamics world) {

                SimpleSpatial torso;
                add(torso = new SimpleSpatial("torso") {
                    @Override
                    protected CollisionShape newShape() {
                        //return new TetrahedronShapeEx(v(0,10,0), v(10,0,0), v(10,10,0), v(0,0,10));
                        //return new CylinderShape(v(0.5f, 1, 0.5f));
                        //return new CylinderShape(v(1f, 0.1f, 1f));
                        return new BoxShape(v(1.6f, 0.1f, 1f));
                    }

                    @Override
                    public float mass() {
                        return 40f;
                    }
                });


                SimpleSpatial neck;
                add(neck = new SimpleSpatial("neck") {
                    @Override
                    protected CollisionShape newShape() {
                        //return new TetrahedronShapeEx(v(0,10,0), v(10,0,0), v(10,10,0), v(0,0,10));
                        return new CylinderShape(v(0.25f, 0.75f, 0.25f));
                    }

                    @Override
                    protected Dynamic create(Dynamics world) {
                        torso.body.clearForces();

                        Dynamic n = super.create(world);
                        HingeConstraint p = new HingeConstraint(torso.body, body, v(0, 0.2f, 0), v(0, -1f, 0), v(1, 0, 0), v(1, 0, 0));
                        p.setLimit(-1.0f, 1.0f);
                        add(p);
                        return n;
                    }

                    @Override
                    public float mass() {
                        return 10f;
                    }
                });
                neck.shapeColor[0] = 1f;
                neck.shapeColor[1] = 0.1f;
                neck.shapeColor[2] = 0.5f;
                neck.shapeColor[3] = 1f;

                RetinaGrid rg = new RetinaGrid("cam1", v(), v(0, 0, 1), v(0.1f, 0, 0), v(0, 0.1f, 0), 6, 6, 4f) {
                    @Override
                    protected Dynamic create(Dynamics world) {

                        Dynamic l = super.create(world);

                        //move(0,-1,0);
                        //body.clearForces();

                        l.clearForces();
                        HingeConstraint p = new HingeConstraint(neck.body, body, v(0, 0.6f, 0), v(0, -0.6f, 0), v(0, 1, 0), v(0, 1, 0));
                        p.setLimit(-0.75f, 0.75f);


//                        Point2PointConstraint p = new Point2PointConstraint(body, torso.body, v(2, 0, 0), v(-2, 0, 0));
//                        p.impulseClamp = 0.01f;
//                        //p.damping = 0.5f;
//                        p.tau = 0.01f;
                        add(p);
                        return l;
                    }


                };

                add(rg);
            }
        };



        new SpaceGraph<>(
                new Maze("x", 20, 20),
                r
        );//.setGravity(v(0, 0, -5)).show(1000, 1000);


    }

}
