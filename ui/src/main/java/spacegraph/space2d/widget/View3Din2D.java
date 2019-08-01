package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import jcog.Util;
import jcog.math.v2;
import jcog.math.v3;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.util.SpaceMouse;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.sketch.Sketch2DBitmap;
import spacegraph.space3d.SpaceDisplayGraph3D;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.collision.ClosestRay;
import spacegraph.space3d.phys.shape.SimpleBoxShape;
import spacegraph.space3d.widget.SurfacedCuboid;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

/**
 * embedded 3d viewport for use on a 2d surface
 */
public class View3Din2D extends PaintSurface {

	public static void main(String[] args) {
		SpaceDisplayGraph3D s = new SpaceDisplayGraph3D().camPos(0, 0, 5);
//        for (int x = -10; x < 10; x++) {
//            for (int y = -10; y < 10; y++) {
//                s.add(
//                    new SimpleSpatial().move(x, y, 0).scale(0.75f).color(1, 1, 1)
//                );
//            }
//        }

		s.add(new SurfacedCuboid("x",
			//new BitmapLabel("y"),
			new Sketch2DBitmap(128, 128),
			//new MetaFrame(new Sketch2DBitmap(32,32)),
			2, 2)
			//.rotate(0, 0, 1, 0.5f, 1f)
			.move(0, 0, 0)
		);
//		s.add(new SurfacedCuboid("y",
//			//new BitmapLabel("y"),
//			new Sketch2DBitmap(128, 128),
//			//new MetaFrame(new Sketch2DBitmap(32,32)),
//			1, 1)
//			.move(0, 1, 0)
//		);
//		s.add(new SurfacedCuboid("z",
//			//new BitmapLabel("y"),
//			new XYSlider(new FloatRange(0, 1, 1), new FloatRange(0, 1, 1)),
//			//new Sketch2DBitmap(128, 128),
//			//new MetaFrame(new Sketch2DBitmap(32,32)),
//			1, 1)
//			.move(1, 0, 0)
//		);
		SpaceGraph.window(new Splitting(new PushButton("y"), 0.9f, new Splitting(
			new View3Din2D(s),
			0.1f, new PushButton("x")).resizeable()).resizeable(), 1280, 1024);
	}


	private final SpaceDisplayGraph3D space;

	final SpaceMouse mouse;
	private float px1 = 0, py1 = 0, px2 = 1, py2 = 1;

	public View3Din2D(SpaceDisplayGraph3D space) {
		this.space = space;
		this.mouse = new FingerAdapter(space);
	}

	@Override
	protected void paint(GL2 gl, ReSurface r) {
		gl.glPushMatrix();

		px1 = ((x() - r.x1) * r.scaleX);
		py1 = ((y() - r.y1) * r.scaleY);
		px2 = ((x() - r.x1 + w()) * r.scaleX);
		py2 = ((y() - r.y1 + h()) * r.scaleY);
		gl.glViewport(Math.round(px1), Math.round(py1), Math.round(px2 - px1), Math.round(py2 - py1));

//		space.zFar = 100;
		render(gl, r);

		//restore ortho state
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();

		gl.glViewport(0, 0, Math.round(r.pw), Math.round(r.ph));
		gl.glOrtho(0, r.pw, 0, r.ph, -1.5, 1.5);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

		gl.glPopMatrix();
	}

	protected void render(GL2 gl, ReSurface r) {
//        space.camPos((float)(Math.random()*0.5f), (float)(Math.random()*0.5f), 5);
//        space.camFwd((float)(Math.random()-0.5f)*2, (float)(Math.random()-0.5f)*2, -1);

		space.renderVolumeEmbedded(r.dtS(), gl, bounds);
	}


	@Override
	public Surface finger(Finger finger) {
		//v2 p = finger.posRelative(RectFloat.XYXY(px1, py1, px2, py2));
        v2 p = finger.posRelative(bounds);

//		float pw = px2 - px1;
//		float ph = py2 - py1;
		ClosestRay c = mouse.pickRay((p.x-0.5f)*2, (p.y-0.5f)*2);
		if (c.hasHit()) {
			Body3D co = mouse.pickedBody;
			//Collidable co = c.collidable;
			if (co != null) {
				Object s = co.data();
				if (s instanceof SurfacedCuboid) {
					SurfacedCuboid ss = (SurfacedCuboid) s;
					SimpleBoxShape sss = (SimpleBoxShape) (ss.shape);
					float zFront = sss.z() / 2;
					v3 local =
						co.transform.untransform(mouse.hitPoint.clone());


					float radiusTolerance = 0.25f * co.shape().getBoundingRadius();
					//local.x >= -1 && local.x <= +1 && local.y >= -1 && local.y <= +1 &&

                    if (Util.equals(local.z, zFront, radiusTolerance)) {
						//System.out.println(local.x + " "  + local.y);
                        Surface front = ss.front;
                        if (front != null) {
                            float localX = (local.x+0.5f), localY = (local.y+0.5f);
							//float localX = local.x+sss.x(), localY = local.y+sss.y();
                            //System.out.println(n4(localX) + "," + n4(localY)); // local + " -> " + + "\t" + p + " " + c.hitPointWorld);
                            return finger.push((px, py, target) -> {
                                target.set(localX, localY); //assumes virtual pixelResolution=1
                            }, front::finger);
                        }
                    } else {
//						System.out.println(p + " -> " + c.hitPointWorld + "\t=> " + local);
					}
				}
			}
		}
		return null;
	}

	private class FingerAdapter extends SpaceMouse {
		public FingerAdapter(SpaceDisplayGraph3D space) {
			super(space);
		}
	}

}
