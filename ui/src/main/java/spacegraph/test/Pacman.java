package spacegraph.test;

import com.jogamp.opengl.GL2;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.Body2D;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.Dynamics2D;
import org.jbox2d.dynamics.FixtureDef;
import spacegraph.math.Tuple2f;
import spacegraph.render.Draw;

import java.util.function.Consumer;

import static org.jbox2d.dynamics.BodyType.DYNAMIC;

public class Pacman extends Body2D implements Consumer<GL2> {

    public Pacman(Dynamics2D world) {
        super(new BodyDef(DYNAMIC), world);

        addFixture(new FixtureDef(
                PolygonShape.regular(9, 0.24f),
                0.5f, 0.2f));

        world.addBody(this);
    }

    @Override
    public void accept(GL2 gl) {
        gl.glColor3f(1, 1, 0);
        Draw.drawPoly(this, gl, (PolygonShape) fixtures.shape);


        float a = angle();
        gl.glColor3f(0, 0, 0);
        Tuple2f center = getWorldCenter();
        Draw.rect(gl, center.x + 0.01f * (float) Math.cos(a), center.y + 0.01f * (float) Math.sin(a), 0.25f, 0.25f);

    }

    @Override
    public void preUpdate() {
        //applyForceToCenter(new v2(rng.nextFloat()*0.01f,rng.nextFloat()*0.01f));
    }
}
