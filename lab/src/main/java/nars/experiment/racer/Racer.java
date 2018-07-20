package nars.experiment.racer;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import spacegraph.SpaceGraph;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.phys.dynamics.FixtureDef;
import spacegraph.space2d.widget.windo.Dyn2DSurface;

import static nars.experiment.racer.Vehicle2D.Wheel.PIXEL_TO_METER_RATIO_DEFAULT;

/**
 * A AndEngine demo based on the RacerGameActivity, and the Box2D tutorial from:
 * http:
 *
 * @author Florian Knoll (myfknoll(at)gmail.com)
 */
public class Racer {


    private static final int RACETRACK_WIDTH = 128;

    private static final int OBSTACLE_SIZE = 16;

    private static final int CAMERA_WIDTH = RACETRACK_WIDTH * 5;
    private static final int CAMERA_HEIGHT = RACETRACK_WIDTH * 3;

    public static final float DEGTORAD = 0.0174532925199432957f;
    public static final float RADTODEG = 57.295779513082320876f;

    public static final int ACCELERATE = 1;
    public static final int ACCELERATE_NONE = 0;
    public static final int BREAK = -1;

    public static final int STEER_RIGHT = 1;
    public static final int STEER_NONE = 0;
    public static final int STEER_LEFT = -1;

    float m_currentTraction = 1;

    float m_maxForwardSpeed;
    float m_maxBackwardSpeed;
    float m_maxDriveForce;
    float m_maxLateralImpulse;


    private final Dynamics2D mPhysicsWorld;

    private IVehicleControl control;
    private Vehicle2D vehicle;


    public static void main(String[] args) {
        new Racer();
    }

    public Racer() {


        Dyn2DSurface p = SpaceGraph.wall(800, 800);

        this.mPhysicsWorld =
                p.W;


        this.initCar();

        vehicle.steerLeft();
        p.root().onUpdate(j -> {
            vehicle.onUpdate(j.dtMS() / 1000f);

        });
        ((Ortho) p.root()).window.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'a':
                        vehicle.steerLeft();
                        break;
                    case 'd':
                        vehicle.steerRight();
                        break;
                    case 's':

                        vehicle.pedalAccelerate();
                        break;
                    case 'x':
                        vehicle.pedalBreak();
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });


    }


    protected void initCar() {
        float maxForwardSpeed = 250;
        float maxBackwardSpeed = -40;
        float frontTireMaxDriveForce = 20;
        float frontTireMaxLateralImpulse = 0.0f;

        vehicle = new Vehicle2D(mPhysicsWorld);
        vehicle.setCharacteristics(maxForwardSpeed / PIXEL_TO_METER_RATIO_DEFAULT, maxBackwardSpeed / PIXEL_TO_METER_RATIO_DEFAULT, frontTireMaxDriveForce, frontTireMaxLateralImpulse);
        control = vehicle;
    }

    private void initObstacles() {
        this.addObstacle(CAMERA_WIDTH / 2, RACETRACK_WIDTH / 2);
        this.addObstacle(CAMERA_WIDTH / 2, RACETRACK_WIDTH / 2);
        this.addObstacle(CAMERA_WIDTH / 2, CAMERA_HEIGHT - RACETRACK_WIDTH / 2);
        this.addObstacle(CAMERA_WIDTH / 2, CAMERA_HEIGHT - RACETRACK_WIDTH / 2);
    }

    private void addObstacle(final float pX, final float pY) {


        final FixtureDef boxFixtureDef = new FixtureDef(PolygonShape.box(pX, pY), 0.1f, 0.5f);
        boxFixtureDef.restitution = 0.5f;
        final Body2D boxBody = mPhysicsWorld.addDynamic(boxFixtureDef);
        boxBody.setLinearDamping(10);
        boxBody.setAngularDamping(10);


    }


    protected void controlChange(final float pValueX, final float pValueY) {


        if (pValueY < -0.1f) {
            control.pedalAccelerate();
        } else if (pValueY > 0.1f) {
            control.pedalBreak();
        } else {
            control.pedalNone();
        }

        if (pValueX > 0.1f) {
            control.steerRight();
        } else if (pValueX < -0.1f) {
            control.steerLeft();
        } else {
            control.steerNone();
        }
    }

    private void initRacetrackBorders() {


        final PolygonShape bottomOuter = PolygonShape.box(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2);
        final PolygonShape topOuter = PolygonShape.box(0, 0, CAMERA_WIDTH, 2);
        final PolygonShape leftOuter = PolygonShape.box(0, 0, 2, CAMERA_HEIGHT);
        final PolygonShape rightOuter = PolygonShape.box(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT);

        final PolygonShape bottomInner = PolygonShape.box(RACETRACK_WIDTH, CAMERA_HEIGHT - 2 - RACETRACK_WIDTH, CAMERA_WIDTH - 2 * RACETRACK_WIDTH, 2);
        final PolygonShape topInner = PolygonShape.box(RACETRACK_WIDTH, RACETRACK_WIDTH, CAMERA_WIDTH - 2 * RACETRACK_WIDTH, 2);
        final PolygonShape leftInner = PolygonShape.box(RACETRACK_WIDTH, RACETRACK_WIDTH, 2, CAMERA_HEIGHT - 2 * RACETRACK_WIDTH);
        final PolygonShape rightInner = PolygonShape.box(CAMERA_WIDTH - 2 - RACETRACK_WIDTH, RACETRACK_WIDTH, 2, CAMERA_HEIGHT - 2 * RACETRACK_WIDTH);


        this.mPhysicsWorld.addStatic(new FixtureDef(bottomOuter, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(leftOuter, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(rightOuter, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(topOuter, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(bottomInner, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(leftInner, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(rightInner, 0, 0.5f));
        this.mPhysicsWorld.addStatic(new FixtureDef(topInner, 0, 0.5f));

    }


    /**
     * Interface for car controlling
     *
     * @author Knoll
     */
    public interface IVehicleControl {

        void steerLeft();

        void steerRight();

        void steerNone();

        void pedalAccelerate();

        void pedalBreak();

        void pedalNone();

    }
}



























































































































































































































































































