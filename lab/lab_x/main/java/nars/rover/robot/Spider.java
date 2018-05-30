package nars.rover.robot;

import nars.rover.Sim;
import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.Body;
import spacegraph.space2d.phys.dynamics.BodyType;
import spacegraph.space2d.phys.dynamics.joints.RevoluteJointDef;

public class Spider extends Robotic {

    int arms;
    float armLength = 4.5f;
    float armWidth = 1.0f;
    int armSegments;

    float torsoRadius = 3.4f;

    float servoRange = ((float) Math.PI / 2.0f) * 0.9f;
    int servoSteps = 7;

    int numRetinasPerSegment = 32;
    int retinaLevels = 4;

    final int velocityLevels = 16;

    int orientationSteps = 9;

    double initialVisionDistance = 10.0;

    float armSegmentExponent;

    
    
    private final float ix;
    private final float iy;
    

    

    public Spider(String id, int arms, int armSegments, float armSegmentExponent, float ix, float iy) {
        super(id);
        this.arms = arms;
        this.armSegments = armSegments;
        this.armSegmentExponent = armSegmentExponent;
        this.ix = ix;
        this.iy = iy;
        
        
    }

    public void addArm(Body base, float x, float y, float angle, int armSegments, float armLength, float armWidth) {
        Body[] arm = new Body[armSegments];

        Body prev = base;

        double dr = getArmLength(armLength, 0) / 2.0;

        for (int i = 0; i < armSegments; i++) {

            float al = getArmLength(armLength, i);
            float aw = getArmWidth(armWidth, i);

            float ax = (float) (x + Math.cos(angle) * dr);
            float ay = (float) (y + Math.sin(angle) * dr);
            final Body b = arm[i] = sim.create(new Vec2(ax, ay),
                    sim.rectangle(new Vec2(al, aw), new Vec2(), angle), BodyType.DYNAMIC); 

            float rx = (float) (x + Math.cos(angle) * (dr - al / 2.0f));
            float ry = (float) (y + Math.sin(angle) * (dr - al / 2.0f));


            RevoluteJointDef rv = new RevoluteJointDef();
            rv.initialize(arm[i], prev, new Vec2(rx, ry));
            rv.enableLimit = true;
            rv.enableMotor = true;
            rv.upperAngle = 1;
            rv.lowerAngle = -1;
            

            sim.getWorld().createJoint(rv);











































            
            
            
            
            
            










            

            
            dr += al * 0.9f;

            prev = arm[i];
        }


    }

    @Override
    public void init(Sim p) {
        this.sim = p;
        Body base = sim.create(new Vec2(ix, iy), sim.circle(torsoRadius), BodyType.DYNAMIC); 

        float da = (float) ((Math.PI * 2.0) / arms);
        float a = 0;
        for (int i = 0; i < arms; i++) {
            float ax = (float) (ix + (Math.cos(a) * torsoRadius));
            float ay = (float) (iy + (Math.sin(a) * torsoRadius));
            addArm(base, ax, ay, a, armSegments, armLength, armWidth);
            a += da;
        }
















    }

    @Override
    public RoboticMaterial getMaterial() {
        return new RoboticMaterial(this);
    }

    @Override
    protected Body newTorso() {
        return null;
    }
















    private float getArmLength(float armLength, int i) {
        

        return armLength * (float)Math.pow(armSegmentExponent, i);   
    }

    private float getArmWidth(float armWidth, int i) {
        
        return armWidth * (float)Math.pow(armSegmentExponent, i);
    }

}
        