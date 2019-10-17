package spacegraph.space2d.dyn2d.jbox2d;

import jcog.math.v2;
import spacegraph.space2d.phys.collision.shapes.CircleShape;
import spacegraph.space2d.phys.collision.shapes.EdgeShape;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.common.MathUtils;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.dynamics.joints.RevoluteJointDef;
import spacegraph.space2d.phys.dynamics.joints.WheelJoint;
import spacegraph.space2d.phys.dynamics.joints.WheelJointDef;

import java.util.function.Consumer;

public class CarTest implements Consumer<Dynamics2D> {

  private Body2D m_car;
  private Body2D m_wheel1;
  private Body2D m_wheel2;

  private float m_hz;
  private float m_zeta;
  private float m_speed;
  private WheelJoint m_spring1;
  private WheelJoint m_spring2;

//  @Override
//  public Long getTag(Body2D body) {
//    if (Body2D == m_car) {
//      return CAR_TAG;
//    }
//    if (Body2D == m_wheel1) {
//      return WHEEL1_TAG;
//    }
//    if (Body2D == m_wheel2) {
//      return WHEEL2_TAG;
//    }
//    return super.getTag(body);
//  }
//
//  @Override
//  public Long getTag(Joint joint) {
//    if (joint == m_spring1) {
//      return SPRING1_TAG;
//    }
//    if (joint == m_spring2) {
//      return SPRING2_TAG;
//    }
//    return super.getTag(joint);
//  }
//
//  @Override
//  public void processBody(Body2D body, Long tag) {
//    if (tag == CAR_TAG) {
//      m_car = body;
//    } else if (tag == WHEEL1_TAG) {
//      m_wheel1 = body;
//    } else if (tag == WHEEL2_TAG) {
//      m_wheel2 = body;
//    } else {
//      super.processBody(body, tag);
//    }
//  }
//
//  @Override
//  public void processJoint(Joint joint, Long tag) {
//    if (tag == SPRING1_TAG) {
//      m_spring1 = (WheelJoint) joint;
//    } else if (tag == SPRING2_TAG) {
//      m_spring2 = (WheelJoint) joint;
//    } else {
//      super.processJoint(joint, tag);
//    }
//  }
//
//  @Override
//  public boolean isSaveLoadEnabled() {
//    return true;
//  }
//
//  @Override
//  public String getTestName() {
//    return "Car";
//  }

  
  @Override public void accept(Dynamics2D m_world) {
    
    m_hz = 4.0f;
    m_zeta = 0.7f;
    m_speed = 50.0f;

    Body2D ground = null;
    {
      BodyDef bd = new BodyDef();
      ground = m_world.addBody(bd);

      EdgeShape shape = new EdgeShape();

      FixtureDef fd = new FixtureDef();
      fd.shape = shape;
      fd.density = 0.0f;
      fd.friction = 0.6f;

      shape.set(new v2(-20.0f, 0.0f), new v2(20.0f, 0.0f));
      ground.addFixture(fd);

      float hs[] = {0.25f, 1.0f, 4.0f, 0.0f, 0.0f, -1.0f, -2.0f, -2.0f, -1.25f, 0.0f};

      float x = 20.0f, y1 = 0.0f, dx = 5.0f;

      for (int i = 0; i < 10; ++i) {
        float y2 = hs[i];
        shape.set(new v2(x, y1), new v2(x + dx, y2));
        ground.addFixture(fd);
        y1 = y2;
        x += dx;
      }

      for (int i = 0; i < 10; ++i) {
        float y2 = hs[i];
        shape.set(new v2(x, y1), new v2(x + dx, y2));
        ground.addFixture(fd);
        y1 = y2;
        x += dx;
      }

      shape.set(new v2(x, 0.0f), new v2(x + 40.0f, 0.0f));
      ground.addFixture(fd);

      x += 80.0f;
      shape.set(new v2(x, 0.0f), new v2(x + 40.0f, 0.0f));
      ground.addFixture(fd);

      x += 40.0f;
      shape.set(new v2(x, 0.0f), new v2(x + 10.0f, 5.0f));
      ground.addFixture(fd);

      x += 20.0f;
      shape.set(new v2(x, 0.0f), new v2(x + 40.0f, 0.0f));
      ground.addFixture(fd);

      x += 40.0f;
      shape.set(new v2(x, 0.0f), new v2(x, 20.0f));
      ground.addFixture(fd);
    }

    // Teeter
    {
      BodyDef bd = new BodyDef();
      bd.position.set(140.0f, 1.0f);
      bd.type = BodyType.DYNAMIC;
      Body2D body = m_world.addBody(bd);

      PolygonShape box = new PolygonShape();
      box.setAsBox(10.0f, 0.25f);
      body.addFixture(box, 1.0f);

      RevoluteJointDef jd = new RevoluteJointDef();
      jd.initialize(ground, body, body.getPosition());
      jd.lowerAngle = -8.0f * MathUtils.PI / 180.0f;
      jd.upperAngle = 8.0f * MathUtils.PI / 180.0f;
      jd.enableLimit = true;
      m_world.addJoint(jd);

      body.applyAngularImpulse(100.0f);
    }

    // Bridge
    {
      int N = 20;
      PolygonShape shape = new PolygonShape();
      shape.setAsBox(1.0f, 0.125f);

      FixtureDef fd = new FixtureDef();
      fd.shape = shape;
      fd.density = 1.0f;
      fd.friction = 0.6f;

      RevoluteJointDef jd = new RevoluteJointDef();

      Body2D prevBody = ground;
      for (int i = 0; i < N; ++i) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(161.0f + 2.0f * i, -0.125f);
        Body2D body = m_world.addBody(bd);
        body.addFixture(fd);

        v2 anchor = new v2(160.0f + 2.0f * i, -0.125f);
        jd.initialize(prevBody, body, anchor);
        m_world.addJoint(jd);

        prevBody = body;
      }

      v2 anchor = new v2(160.0f + 2.0f * N, -0.125f);
      jd.initialize(prevBody, ground, anchor);
      m_world.addJoint(jd);
    }

    // Boxes
    {
      PolygonShape box = new PolygonShape();
      box.setAsBox(0.5f, 0.5f);

        BodyDef bd = new BodyDef();
      bd.type = BodyType.DYNAMIC;

      bd.position.set(230.0f, 0.5f);
        Body2D body = m_world.addBody(bd);
      body.addFixture(box, 0.5f);

      bd.position.set(230.0f, 1.5f);
      body = m_world.addBody(bd);
      body.addFixture(box, 0.5f);

      bd.position.set(230.0f, 2.5f);
      body = m_world.addBody(bd);
      body.addFixture(box, 0.5f);

      bd.position.set(230.0f, 3.5f);
      body = m_world.addBody(bd);
      body.addFixture(box, 0.5f);

      bd.position.set(230.0f, 4.5f);
      body = m_world.addBody(bd);
      body.addFixture(box, 0.5f);
    }

    // Car
    {
      PolygonShape chassis = new PolygonShape();
      v2 vertices[] = new v2[8];
      vertices[0] = new v2(-1.5f, -0.5f);
      vertices[1] = new v2(1.5f, -0.5f);
      vertices[2] = new v2(1.5f, 0.0f);
      vertices[3] = new v2(0.0f, 0.9f);
      vertices[4] = new v2(-1.15f, 0.9f);
      vertices[5] = new v2(-1.5f, 0.2f);
      chassis.set(vertices, 6);

      CircleShape circle = new CircleShape();
      circle.skinRadius = 0.4f;

      BodyDef bd = new BodyDef();
      bd.type = BodyType.DYNAMIC;
      bd.position.set(0.0f, 1.0f);
      m_car = m_world.addBody(bd);
      m_car.addFixture(chassis, 1.0f);

      FixtureDef fd = new FixtureDef();
      fd.shape = circle;
      fd.density = 1.0f;
      fd.friction = 0.9f;

      bd.position.set(-1.0f, 0.35f);
      m_wheel1 = m_world.addBody(bd);
      m_wheel1.addFixture(fd);

      bd.position.set(1.0f, 0.4f);
      m_wheel2 = m_world.addBody(bd);
      m_wheel2.addFixture(fd);

      WheelJointDef jd = new WheelJointDef();
      v2 axis = new v2(0.0f, 1.0f);

      jd.initialize(m_car, m_wheel1, m_wheel1.getPosition(), axis);
      jd.motorSpeed = 0.0f;
      jd.maxMotorTorque = 20.0f;
      jd.enableMotor = true;
      jd.frequencyHz = m_hz;
      jd.dampingRatio = m_zeta;
      m_spring1 = (WheelJoint) m_world.addJoint(jd);

      jd.initialize(m_car, m_wheel2, m_wheel2.getPosition(), axis);
      jd.motorSpeed = 0.0f;
      jd.maxMotorTorque = 10.0f;
      jd.enableMotor = false;
      jd.frequencyHz = m_hz;
      jd.dampingRatio = m_zeta;
      m_spring2 = (WheelJoint) m_world.addJoint(jd);
    }
  }

  //@Override
  public void keyPressed(char argKeyChar, int argKeyCode) {
    switch (argKeyChar) {
      case 'a':
        m_spring1.enableMotor(true);
        m_spring1.setMotorSpeed(m_speed);
        break;

      case 's':
        m_spring1.enableMotor(true);
        m_spring1.setMotorSpeed(0.0f);
        break;

      case 'd':
        m_spring1.enableMotor(true);
        m_spring1.setMotorSpeed(-m_speed);
        break;

      case 'q':
        m_hz = MathUtils.max(0.0f, m_hz - 1.0f);
        m_spring1.setSpringFrequencyHz(m_hz);
        m_spring2.setSpringFrequencyHz(m_hz);
        break;

      case 'e':
        m_hz += 1.0f;
        m_spring1.setSpringFrequencyHz(m_hz);
        m_spring2.setSpringFrequencyHz(m_hz);
        break;
    }
  }

  
  public void keyReleased(char argKeyChar, int argKeyCode) {
    //super.keyReleased(argKeyChar, argKeyCode);
    switch (argKeyChar) {
      case 'a':
      case 's':
      case 'd':
        m_spring1.enableMotor(false);
        break;
    }
  }

//  @Override
//  public float getDefaultCameraScale() {
//    return 15;
//  }
//
//  @Override
//  public synchronized void step(TestbedSettings settings) {
//    super.step(settings);
//    addTextLine("Keys: left = a, brake = s, right = d, hz down = q, hz up = e");
//    addTextLine("frequency = " + m_hz + " hz, damping ratio = " + m_zeta);
//
//    getCamera().setCamera(m_car.getPosition());
//  }
}
