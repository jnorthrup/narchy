package ptrman.difficultyEnvironment.interactionComponents;

import ptrman.difficultyEnvironment.EntityDescriptor;
import ptrman.difficultyEnvironment.JavascriptDescriptor;
import ptrman.difficultyEnvironment.physics.Physics2dBody;
import spacegraph.space2d.phys.common.Vec2;

/**
 * For a top down view 2d simulation
 *
 * Controls the 2d physics body as if it had wheels.
 * Usable for example for rovers...
 */
public class TopDownViewWheeledPhysicsComponent implements IComponent {
    public TopDownViewWheeledPhysicsComponent(float linearThrustPerCycle, float angularSpeedPerCycle) {
        this.linearThrustPerCycle = linearThrustPerCycle;
        this.angularSpeedPerCycle = angularSpeedPerCycle;
    }

    public float linearThrustPerCycle;
    public float angularSpeedPerCycle;
    public boolean setRotationOfPhysics2dBody = true;

    public void thrustRelative(float f) {
        if( cachedPhysics2dBody == null ) {
            return;
        }

        if( f == 0.0 ) {
            cachedPhysics2dBody.body.setLinearVelocity(new Vec2());
        } else {
            thrust(0, f * linearThrustPerCycle);
        }
    }

    public void rotateRelative(float f) {
        rotate(f * angularSpeedPerCycle);
    }

    public void thrust(float angle, float force) {
        if( cachedPhysics2dBody == null ) {
            return;
        }

        angle += cachedPhysics2dBody.body.getAngle();
        
        Vec2 v = new Vec2((float) Math.cos(angle) * force, (float) Math.sin(angle) * force);
        cachedPhysics2dBody.body.setLinearVelocity(v);
        

        if( setRotationOfPhysics2dBody ) {
            cachedPhysics2dBody.body.setTransform(cachedPhysics2dBody.body.getPosition(), angle);
        }
    }

    public void rotate(float v) {
        if( cachedPhysics2dBody == null ) {
            return;
        }

        cachedPhysics2dBody.body.setAngularVelocity(v);
        
        
    }


    public void stop() {
        if( cachedPhysics2dBody == null ) {
            return;
        }

        cachedPhysics2dBody.body.setAngularVelocity(0);
        cachedPhysics2dBody.body.setLinearVelocity(new Vec2());
    }

    @Override
    public void frameInteraction(JavascriptDescriptor javascriptDescriptor, EntityDescriptor entityDescriptor, float timedelta) {
        cachedPhysics2dBody = entityDescriptor.physics2dBody;
    }

    @Override
    public String getLongName() {
        return "TopDownViewWheeledPhysicsComponent";
    }

    private Physics2dBody cachedPhysics2dBody;
}
