package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import jcog.Util;
import jcog.math.v2;
import jcog.math.v3;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.impl.MouseFinger;
import spacegraph.input.finger.state.Dragging;
import spacegraph.input.finger.util.FPSLook;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.PaintSurface;
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




	private final SpaceDisplayGraph3D space;

	final FingerAdapter mouse;
//	private final SimpleSpatial debugFwd, debugPick1, debugPick2;
	private float px1 = 0, py1 = 0, px2 = 1, py2 = 1;

	public View3Din2D(SpaceDisplayGraph3D space) {
		this.space = space;
		this.mouse = new FingerAdapter(space);


//		space.add(debugFwd = new SimpleSpatial().scale(0.01f,0.01f,0.01f).color(1,1,1,0.25f));
//		space.add(debugPick1 = new SimpleSpatial().scale(0.05f,0.05f,0.05f).color(0,1,0,0.25f));
//		space.add(debugPick2 = new SimpleSpatial().scale(0.1f,0.1f,0.1f).color(1,0,0,0.25f));
	}


	@Override
	protected void paint(GL2 gl, ReSurface r) {



		gl.glPushMatrix();


		px1 = ((x() - r.x1) * r.scaleX);
		py1 = ((y() - r.y1) * r.scaleY);
		px2 = ((x() - r.x1 + w()) * r.scaleX);
		py2 = ((y() - r.y1 + h()) * r.scaleY);
		gl.glViewport(Math.round(bounds.x),Math.round(bounds.y), Math.round(px2 - px1), Math.round(py2 - py1));

		space.renderVolumeEmbedded(r.dtS(), gl, bounds);

		//restore ortho state
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();

		gl.glViewport(0, 0, Math.round(r.pw), Math.round(r.ph));
		gl.glOrtho(0, r.pw, 0, r.ph, -1.5, 1.5);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

		gl.glPopMatrix();
	}


	@Override
	public Surface finger(Finger fingerFrom2D) {

		float wheel = fingerFrom2D.rotationY(true);
		if (wheel!=0) {
			space.camPos.addScaled(space.camFwd, wheel);
		}

		if (!fpsDrag.active()) {

			v2 p = fingerFrom2D.posRelative(this);

			ClosestRay c = mouse.pickRay((p.x - 0.5f) * 2, (p.y - 0.5f) * 2);


			if (c.hasHit()) {
				Body3D co = mouse.pickedBody;
				if (co != null) {
					Object s = co.data();
					if (s instanceof SurfacedCuboid) {
						SurfacedCuboid ss = (SurfacedCuboid) s;
						v3 local = ss.transform.untransform(mouse.hitPoint.clone());

						float radiusTolerance = 0.25f * co.shape().getBoundingRadius();
						if (local.x >= -1 && local.x <= +1 && local.y >= -1 && local.y <= +1) {

							SimpleBoxShape sss = (SimpleBoxShape) (ss.shape);
							float zFront = sss.z() / 2;
							if (Util.equals(local.z, zFront, radiusTolerance)) {
								//System.out.println(local.x + " "  + local.y);
								Surface front = ss.front;
								if (front != null) {
									//float localX = ((local.x+0.5f)*front.w())/(2*sss.x()), localY = ((local.y+0.5f)*front.h())/(2*sss.y());
									float localX = (local.x / sss.x()) + 0.5f, localY = (local.y / sss.y()) + 0.5f;
									//float localX = local.x+sss.x(), localY = local.y+sss.y();
									//System.out.println(front + " " + n4(localX) + "," + n4(localY)); // local + " -> " + + "\t" + p + " " + c.hitPointWorld);

									fingerTo2D.posPixel.set(localX, localY);
									fingerTo2D.posGlobal.set(localX, localY);
									fingerTo2D.copyButtons(fingerFrom2D);
									//Surface fingering = fingerTo2D.push(new v2(localX, localY), front::finger);
									Surface fingering = fingerTo2D.finger(front::finger);//front.finger(fingerTo2D);
									//fingerTo2D.exit();
									if (fingering!=null) {
										//absorb and shadow internal node
//										fingerTo2D.fingering.set(Idle);//HACK
//										//f.test(Idle);//clear fingering
										return this;
									}
								}
							}
						}
					}
				}
			}
		}

		if (fingerFrom2D.test(fpsDrag))
			return this;

		return null;
	}

	final MouseFinger fingerTo2D = new MouseFinger(5) {
		@Override
		protected void start(SpaceGraph x) {
			enter();
		}

		@Override
		protected void stop(SpaceGraph x) {

		}
	};

	final Dragging fpsDrag = new Dragging( 2) {

		private v2 start;

		float speed = 0.02f;

		@Override
		protected boolean starting(Finger f) {
			start = f.posPixel.clone();
			return true;
		}

		@Override
		protected boolean drag(Finger f) {
			v2 delta = f.posPixel.subClone(start).scaled(speed);
			mouse.drag(delta.x, -delta.y);
			return true;
		}
	};

	private class FingerAdapter extends FPSLook {
		public FingerAdapter(SpaceDisplayGraph3D space) {
			super(space);
		}
	}

}
