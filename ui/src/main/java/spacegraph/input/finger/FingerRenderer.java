package spacegraph.input.finger;

import com.jogamp.opengl.GL2;
import jcog.math.FloatAveragedWindow;
import jcog.math.v2;
import spacegraph.video.Draw;

import static com.jogamp.opengl.GL.GL_COLOR_LOGIC_OP;
import static com.jogamp.opengl.GL.GL_EQUIV;

/** cursor renderer */
@FunctionalInterface public interface FingerRenderer {

    void paint(v2 posPixel, Finger finger, float dtS, GL2 gl);

    FingerRenderer rendererCrossHairs1 = new FingerRenderer() {

        final v2 lastPixel = new v2((float) 0, (float) 0);
        final FloatAveragedWindow speedFilter = new FloatAveragedWindow(8, 0.5f);

        @Override
        public void paint(v2 posPixel, Finger finger, float dtS, GL2 gl) {


            float smx = posPixel.x, smy = posPixel.y;

            float speed = lastPixel.distance(posPixel);

            float cw = 32f + speedFilter.valueOf(speed)/1f;

            gl.glPushMatrix();

            {

                gl.glEnable(GL_COLOR_LOGIC_OP);
                gl.glLogicOp(
                        //GL_XOR
                        //GL_INVERT
                        //GL_OR_INVERTED
                        GL_EQUIV
                );

                gl.glTranslatef(smx, smy, (float) 0);


                gl.glColor4f(0.75f, 0.75f, 0.75f, 0.9f);

                gl.glLineWidth(2f);
                //Draw.rectStroke(gl, smx - cw / 2f, smy - ch / 2f, cw, ch);
                float theta = (posPixel.x + posPixel.y) / 100f;
                Draw.poly(6 /* 6 */, cw / 2.0F, theta, false, gl);

                gl.glColor4f(0.5f, 0.5f, 0.5f, 0.75f);
                gl.glLineWidth(3f);
                gl.glColor4f(0.5f, 0.5f, 0.5f, 0.75f);
                float ch = cw;
                Draw.line((float) 0, -ch, (float) 0, -ch / 2.0F, gl);
                Draw.line((float) 0, ch / 2.0F, (float) 0, ch, gl);
                Draw.line(-cw, (float) 0, -cw / 2.0F, (float) 0, gl);
                Draw.line(cw / 2.0F, (float) 0, cw, (float) 0, gl);

                gl.glLineWidth(1f);
                gl.glColor4f(0.1f, 0.1f, 0.1f, 0.5f);
                Draw.rect( -cw/ 16.0F, -ch/ 16.0F, cw/ 16.0F, ch/ 16.0F, gl);
                //Draw.poly(3, cw / 10, -theta, false, gl);

                gl.glDisable(GL_COLOR_LOGIC_OP);
            }
            gl.glPopMatrix();

            lastPixel.set(posPixel);
        }
    };

    /** virtua cop */
    FingerRenderer polygon1 = new PolygonCrosshairs();

    class PolygonCrosshairs implements FingerRenderer {

        float angle = 0f;
        float alpha = 0.35f;
        float lineWidth = 4.0F;
        float rad = 32f;

        float pixelDistSq = (float) 0;
        final v2 lastPixel = new v2();
        final FloatAveragedWindow smoothedRad = new FloatAveragedWindow(8, 0.25f);
        double timeMS = (double) 0;

        @Override
        public void paint(v2 posPixel, Finger finger, float dtS, GL2 gl) {

            float smx = posPixel.x, smy = posPixel.y;

            pixelDistSq = lastPixel.distanceSq(posPixel);
            lastPixel.set(posPixel);

            timeMS = timeMS + (double) dtS * 1000.0F;

            float freq = 8f;
            float phaseSec = (float) Math.sin((double) freq * timeMS / (2.0 * Math.PI * 1000.0));

            gl.glPushMatrix();
            {
                gl.glTranslatef(smx, smy, (float) 0);
                gl.glRotatef(angle, (float) 0, (float) 0, 1.0F);

                if (finger.pressed(0)) {
                    gl.glColor4f(0.5f, 1.0F, 0.5f, alpha);
                } else if (finger.pressed(2)) {
                    gl.glColor4f(0.5f, 0.5f, 1f, alpha);
                } else {
                    gl.glColor4f((phaseSec * 0.5f) + 0.5f, 0.25f, ((1.0F -phaseSec) * 0.5f) + 0.5f, alpha);
                }

                float r = smoothedRad.valueOf(this.rad + (pixelDistSq / 50.0F));
                renderOutside(r, gl);
                renderInside(r, gl);
            }
            gl.glPopMatrix();
        }

        protected static void drawTri(float rad, GL2 gl) {
            float w = rad/ 2.0F;
            float x1 = rad * 0.5f;
            float x2 = rad * 1f;
            Draw.tri2f(gl, x1, -w/ 2.0F, x1, +w/ 2.0F,   x2, (float) 0);
        }

        protected void renderInside(float rad, GL2 gl) {
            float radh = rad * 0.75f;
            if (renderHorizontal())
                Draw.line((float) 0, -radh, (float) 0, +radh, gl);
            if (renderVertical())
                Draw.line(-radh, (float) 0, +radh, (float) 0, gl);
        }

        /** whether to render the internal crosshair dimension */
        protected boolean renderVertical() { return true; }

        /** whether to render the internal crosshair dimension */
        protected boolean renderHorizontal() { return true; }


        protected void renderOutside(float rad, GL2 gl) {
            gl.glLineWidth(lineWidth);
            Draw.poly(8, rad, false, gl);
        }

        public FingerRenderer angle(float a) {
            this.angle = a;
            return this;
        }
    }
    class PolygonWithArrow extends PolygonCrosshairs {

        final float arrowAngle;

        public PolygonWithArrow(float arrowAngle) {
            this.angle = arrowAngle;
            this.arrowAngle = arrowAngle;
        }

        @Override
        protected void renderInside(float rad, GL2 gl) {
            super.renderInside(rad, gl);

            drawTri(rad, gl);
        }
    }

    /** TODO arrowheads */
    FingerRenderer rendererResizeNS = new PolygonCrosshairs() {
        @Override
        protected boolean renderVertical() {
            return false;
        }
    };
    /** TODO arrowheads */
    FingerRenderer rendererResizeEW = new PolygonCrosshairs() {
        @Override
        protected boolean renderHorizontal() {
            return false;
        }
    };

}
