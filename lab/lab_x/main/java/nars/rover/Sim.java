package nars.rover;

import nars.rover.physics.TestbedPanel;
import nars.rover.physics.TestbedSettings;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.robot.Robotic;
import nars.time.SimulatedClock;
import spacegraph.space2d.phys.common.Color3f;
import spacegraph.space2d.phys.common.MathUtils;
import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.Body;
import spacegraph.space2d.phys.dynamics.BodyDef;
import spacegraph.space2d.phys.dynamics.FixtureDef;

import java.util.List;

/**
 * NARS Rover
 *
 * @author me
 */
public class Sim extends PhysicsModel {


    private final SimulatedClock clock;
    /* how often to input mission, in frames */
    public int missionPeriod = 8;

    boolean wraparound = false;

    public final List<Robotic> robots = Global.newArrayList();
    final static int angleResolution = 13;


    PhysicsRun phy = new PhysicsRun(10, this);
    private long delayMS;
    private float fps;
    private boolean running = false;







































































    public void setFPS(float f) {
        this.fps = f;
        delayMS = (long) (1000f / fps);
    }

    public void run(float fps) {
        setFPS(fps);

        running = true;
        while (running) {
            cycle();
            try {
                Thread.sleep(delayMS);
            } catch (InterruptedException e) {
            }
        }

    }

    public void stop() {
        running = false;
    }


    public void add(Robotic r) {
        r.init(this);
        robots.add(r);
    }

    public void remove(Body r) {
        getWorld().destroyBody(r);
    }


    private static final double TWO_PI = 2 * Math.PI;

    public static double normalizeAngle(final double theta) {
        double normalized = theta % TWO_PI;
        normalized = (normalized + TWO_PI) % TWO_PI;
        if (normalized > Math.PI) {
            normalized -= TWO_PI;
        }
        if (normalized < 0) {
            normalized += TWO_PI;
        }
        return normalized;
    }

    public int cnt = 0;

    static String[] angleTerms = new String[angleResolution];

    public static String angleTerm(final float a) {
        float h = (float) normalizeAngle(a);
        h /= MathUtils.PI*2.0f;
        int i = (int) (h * angleResolution / 1f);
        String t;
        final int ha = angleResolution;












            if (angleTerms[i] == null) {
                

                angleTerms[i] = "a" + i;











            }

            t = angleTerms[i];
        

        return t;
    }

    /**
     * maps a value (which must be in range 0..1.0) to a term name
     */
    public static String f4(double p) {
        if (p < 0) {
            throw new RuntimeException("Invalid value for: " + p);
        }
        if (p > 0.99f) {
            p = 0.99f;
        }
        int i = (int) (p * 10f);
        switch (i) {
            case 9:
                return "5";
            case 8:
            case 7:
                return "4";
            case 6:
            case 5:
                return "3";
            case 4:
            case 3:
                return "2";
            case 2:
            case 1:
                return "1";
            default:
                return "0";
        }
    }

    public static String f(double p) {
        if (p < 0) {
            throw new RuntimeException("Invalid value for: " + p);
            
        }
        if (p > 0.99f) {
            p = 0.99f;
        }
        int i = (int) (p * 10f);
        return String.valueOf(i);
    }

    @Override
    public BodyDef bodyDefCallback(BodyDef body) {
        return body;
    }

    @Override
    public FixtureDef fixDefCallback(FixtureDef fixture) {
        return fixture;
    }


    public interface Edible {

    }

    public static class FoodMaterial extends Material implements Edible {

        static final Color3f foodFill = new Color3f(0.15f, 0.6f, 0.15f);

        @Override
        public void before(Body b, JoglAbstractDraw d, float time) {

            d.setFillColor(foodFill);
        }

        @Override
        public String toString() {
            return "food";
        }
    }
    public static class WallMaterial extends Material {
        static final Color3f wallFill = new Color3f(0.5f, 0.5f, 0.5f);
        @Override
        public void before(Body b, JoglAbstractDraw d, float time) {
            d.setFillColor(wallFill);
        }

        @Override
        public String toString() {
            return "wall";
        }
    }
    public static class PoisonMaterial extends Material implements Edible {

        static final Color3f poisonFill = new Color3f(0.45f, 0.15f, 0.15f);

        @Override
        public void before(Body b, JoglAbstractDraw d, float time) {
            d.setFillColor(poisonFill);
        }
        @Override
        public String toString() {
            return "poison";
        }
    }

    public Sim(SimulatedClock clock, RoverWorld world) {
        this.clock = clock;

        this.world = world;

        init(phy.model);

        cycle();

        this.world.init(this);
    }



    @Override
    public void step(float timeStep, TestbedSettings settings, TestbedPanel panel) {
        cnt++;

        super.step(timeStep, settings, panel);

        for (Robotic r : robots) {
            r.step(1);
        }

        clock.add(1);

    }







































    public RoverWorld world;

    @Override
    public void initTest(boolean deserialized) {


        getWorld().setGravity(new Vec2());
        getWorld().setAllowSleep(false);

    }

    protected final void cycle() {
        phy.cycle(fps);
    }





    @Override     public String getTestName() {
        return "NARS Rover";
    }

}
