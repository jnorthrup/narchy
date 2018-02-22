package org.jbox2d.gui.jbox2d;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.MathUtils;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.ConstantVolumeJointDef;
import org.jbox2d.gui.ICase;
import spacegraph.math.v2;

public class BlobTest4 implements ICase {

  @Override
  public void init(Dynamics2D w) {
    
    

    Body2D ground = null;
    {
      PolygonShape sd = new PolygonShape();
      sd.setAsBox(50.0f, 0.4f);

      BodyDef bd = new BodyDef();
      bd.position.set(0.0f, 0.0f);
      ground = w.addBody(bd);
      ground.addFixture(sd, 0f);

      sd.setAsBox(0.4f, 50.0f, new v2(-10.0f, 0.0f), 0.0f);
      ground.addFixture(sd, 0f);
      sd.setAsBox(0.4f, 50.0f, new v2(10.0f, 0.0f), 0.0f);
      ground.addFixture(sd, 0f);
    }

    ConstantVolumeJointDef cvjd = new ConstantVolumeJointDef();

    float cx = 0.0f;
    float cy = 10.0f;
    float rx = 5.0f;
    float ry = 5.0f;
    int nBodies = 40;
    float bodyRadius = 0.25f;
    for (int i = 0; i < nBodies; ++i) {
      float angle = MathUtils.map(i, 0, nBodies, 0, 2 * 3.1415f);
      BodyDef bd = new BodyDef();
      // bd.isBullet = true;
      bd.fixedRotation = true;

      float x = cx + rx * (float) Math.sin(angle);
      float y = cy + ry * (float) Math.cos(angle);
      bd.position.set(new v2(x, y));
      bd.type = BodyType.DYNAMIC;
      Body2D body = w.addBody(bd);

      FixtureDef fd = new FixtureDef();
      CircleShape cd = new CircleShape();
      cd.m_radius = bodyRadius;
      fd.shape = cd;
      fd.density = 1.0f;
      body.addFixture(fd);
      cvjd.addBody(body);
    }

    cvjd.frequencyHz = 10.0f;
    cvjd.dampingRatio = 0.9f;
    cvjd.collideConnected = false;
    w.addJoint(cvjd);

    BodyDef bd2 = new BodyDef();
    bd2.type = BodyType.DYNAMIC;
    PolygonShape psd = new PolygonShape();
    psd.setAsBox(3.0f, 1.5f, new v2(cx, cy + 15.0f), 0.0f);
    bd2.position = new v2(cx, cy + 15.0f);
    Body2D fallingBox = w.addBody(bd2);
    fallingBox.addFixture(psd, 1.0f);
  }


  
}