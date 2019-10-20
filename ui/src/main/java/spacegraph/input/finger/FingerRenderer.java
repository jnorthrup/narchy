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

        final v2 lastPixel = new v2(0,0);
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

                gl.glTranslatef(smx, smy, 0);


                gl.glColor4f(0.75f, 0.75f, 0.75f, 0.9f);

                gl.glLineWidth(2f);
                //Draw.rectStroke(gl, smx - cw / 2f, smy - ch / 2f, cw, ch);
                float theta = (posPixel.x + posPixel.y) / 100f;
                Draw.poly(6 /* 6 */, cw / 2, theta, false, gl);

                gl.glColor4f(0.5f, 0.5f, 0.5f, 0.75f);
                gl.glLineWidth(3f);
                gl.glColor4f(0.5f, 0.5f, 0.5f, 0.75f);
                float ch = cw;
                Draw.line(0, -ch, 0, -ch / 2, gl);
                Draw.line(0, ch / 2, 0, ch, gl);
                Draw.line(-cw, 0, -cw / 2, 0, gl);
                Draw.line(cw / 2, 0, cw, 0, gl);

                gl.glLineWidth(1f);
                gl.glColor4f(0.1f, 0.1f, 0.1f, 0.5f);
                Draw.rect( -cw/16, -ch/16, cw/16, ch/16, gl);
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
        float lineWidth = 4;
        float rad = 32f;

        float pixelDistSq = 0;
        final v2 lastPixel = new v2();
        final FloatAveragedWindow smoothedRad = new FloatAveragedWindow(8, 0.25f);
        double timeMS = 0;

        @Override
        public void paint(v2 posPixel, Finger finger, float dtS, GL2 gl) {

            float smx = posPixel.x, smy = posPixel.y;

            pixelDistSq = lastPixel.distanceSq(posPixel);
            lastPixel.set(posPixel);

            timeMS += dtS*1000;

            float freq = 8f;
            float phaseSec = (float) Math.sin(freq * timeMS / (2 * Math.PI * 1000));

            gl.glPushMatrix();
            {
                gl.glTranslatef(smx, smy, 0);
                gl.glRotatef(angle, 0, 0, 1);

                if (finger.pressed(0)) {
                    gl.glColor4f(0.5f, 1, 0.5f, alpha);
                } else if (finger.pressed(2)) {
                    gl.glColor4f(0.5f, 0.5f, 1f, alpha);
                } else {
                    gl.glColor4f((phaseSec * 0.5f) + 0.5f, 0.25f, ((1-phaseSec) * 0.5f) + 0.5f, alpha);
                }

                float r = smoothedRad.valueOf(this.rad + (pixelDistSq / 50));
                renderOutside(r, gl);
                renderInside(r, gl);
            }
            gl.glPopMatrix();
        }

        protected static void drawTri(float rad, GL2 gl) {
            float w = rad/2;
            float x1 = rad * 0.5f;
            float x2 = rad * 1f;
            Draw.tri2f(gl, x1, -w/2, x1, +w/2,   x2, 0);
        }

        protected void renderInside(float rad, GL2 gl) {
            float radh = rad * 0.75f;
            if (renderHorizontal())
                Draw.line(0, -radh, 0, +radh, gl);
            if (renderVertical())
                Draw.line(-radh, 0, +radh, 0, gl);
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
