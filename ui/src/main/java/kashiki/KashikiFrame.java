//package kashiki;
//
//import com.jogamp.opengl.*;
//import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
//
//class KashikiFrame implements GLEventListener {
//
//  private final Editor editor = Editor.getInstance();
////  private FPSAnimator animator;
//
//
//  private double yscale;
//
//  @Override
//  public void display(GLAutoDrawable drawable) {
//    try {
//      render(drawable);
//    } catch (GLException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//  }
//
//  @Override
//  public void dispose(GLAutoDrawable drawable) {
////    animator.stop();
//    drawable.removeGLEventListener(this);
//    drawable.destroy();
//  }
//
//  @Override
//  public void init(GLAutoDrawable drawable) {
//
//  }
//
//  @Override
//  public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
////    this.canvasWidth = w;
////    this.canvasHeight = h;
//    this.yscale = ((double) h / w);
//  }
//
//  private void render(GLAutoDrawable drawable) throws GLException {
//    GL2 gl = drawable.getGL().getGL2();
//
//    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
//
//    gl.glEnable(GL.GL_BLEND);
//    gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
//    gl.glEnable(GL.GL_MULTISAMPLE);
//    gl.glEnable(GL.GL_SAMPLE_ALPHA_TO_COVERAGE);
//
//    gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
//    gl.glHint(GL2GL3.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);
//    gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
//
//    gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW_MATRIX);
//    gl.glPushMatrix();
//    double s = editor.getScale().getValue();
//    gl.glScaled(s, s / yscale, 1.0);
//    gl.glTranslated(0, 0, -10);
//
//    editor.getDrawables().forEach(base->base.draw(gl));
//
//    gl.glPopMatrix();
//
//    {
//      gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION_MATRIX);
//      gl.glLoadIdentity();
//      gl.glFrustum(-1, 1, -1, 1, 1, 500);
//    }
//
//    gl.glFlush();
//
//  }
//
//}
