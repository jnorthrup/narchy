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
import spacegraph.space3d.SpaceGraph3D;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.collision.ClosestRay;
import spacegraph.space3d.phys.shape.SimpleBoxShape;
import spacegraph.space3d.widget.SurfacedCuboid;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

/**
 * embedded 3d viewport for use on a 2d surface
 */
public class VolumeSurface extends PaintSurface {

	private final SpaceGraph3D space;

	final FPSLook mouse;

    final FingerAdapter fingerTo2D = new FingerAdapter();

	public VolumeSurface(SpaceGraph3D space) {
		this.space = space;
		this.mouse = new FPSLook(space);
	}

	@Override
	protected void starting() {
		fingerTo2D.enter();
	}

	@Override
	protected void stopping() {
		fingerTo2D.exit();
	}

	@Override
	protected void paint(GL2 gl, ReSurface r) {

		gl.glPushMatrix();


        float left = left();
        float bottom = bottom();
        float px1 = ((left - r.x1) * r.scaleX);
        float py1 = ((bottom - r.y1) * r.scaleY);
        float px2 = ((left - r.x1 + w()) * r.scaleX);
        float py2 = ((bottom - r.y1 + h()) * r.scaleY);
		gl.glViewport(Math.round(bounds.x),Math.round(bounds.y), Math.round(px2 - px1), Math.round(py2 - py1));

		space.renderVolumeEmbedded(r.dtS(), gl, bounds);

		//restore ortho state
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();

		gl.glViewport(0, 0, Math.round(r.pw), Math.round(r.ph));
		gl.glOrtho((double) 0, (double) r.pw, (double) 0, (double) r.ph, -1.5, 1.5);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

		gl.glPopMatrix();
	}


	@Override
	public Surface finger(Finger fingerFrom2D) {

        float wheel = fingerFrom2D.rotationY(true);
		if (wheel!= (float) 0) {
			space.camPos.addScaled(space.camFwd, wheel);
		}

		if (!fpsDrag.active()) {

            v2 p = fingerFrom2D.posRelative(this);

            ClosestRay c = mouse.pickRay((p.x - 0.5f) * 2.0F, (p.y - 0.5f) * 2.0F);

			if (c.hasHit()) {
                Body3D co = mouse.pickedBody;
				if (co != null) {
                    Object s = co.data();
					if (s instanceof SurfacedCuboid) {
                        SurfacedCuboid ss = (SurfacedCuboid) s;
                        v3 local = ss.transform.untransform(mouse.hitPoint.clone());

						if (local.x >= -1.0F && local.x <= (float) +1 && local.y >= -1.0F && local.y <= (float) +1) {

                            SimpleBoxShape sss = (SimpleBoxShape) (ss.shape);
                            float zFront = sss.z() / 2.0F;
                            float radiusTolerance = 0.25f * co.shape().getBoundingRadius();
							if (Util.equals(local.z, zFront, radiusTolerance)) {
								//System.out.println(local.x + " "  + local.y);
                                Surface front = ss.front;
								if (front != null) {
									float localX = (local.x / sss.x()) + 0.5f, localY = (local.y / sss.y()) + 0.5f;

									fingerTo2D.posPixel.set(localX, localY);
									fingerTo2D.posGlobal.set(localX, localY);
									fingerTo2D.copyButtons(fingerFrom2D);
									//Surface fingering = fingerTo2D.push(new v2(localX, localY), front::finger);
                                    Surface fingering = fingerTo2D.finger(front::finger);//front.finger(fingerTo2D);
									//fingerTo2D.exit();
									if (fingering!=null) {
										//absorb and shadow internal node
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


	private final Dragging fpsDrag = new Dragging( 2) {

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

	private static class FingerAdapter extends MouseFinger {

		public FingerAdapter() {
			super(5);
		}

		@Override
		protected void start(SpaceGraph x) {
		}

		@Override
		protected void stop(SpaceGraph x) {
		}
	}
}
