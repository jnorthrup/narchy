package kashiki;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;


public class KashikiGLCanvas extends Surface {

  private final Editor editor;

  public KashikiGLCanvas(Editor editor) {
//    super(new GLCapabilities(GLProfile.getDefault()));
//    setFocusable(true);
//    setFocusTraversalKeysEnabled(false);
//    addKeyListener(new DocumentKeyListener());
////    addKeyListener(new LoggingKeyListener());
//    addGLEventListener(new KashikiFrame());
//    new FPSAnimator(this, 30).start();
    this.editor = editor;
  }

  @Override
  protected void paint(GL2 gl, SurfaceRender surfaceRender) {
    gl.glTranslatef(300, 300, 0); //HACK
    gl.glScalef(100, 100, 100); //HACK

    editor.getDrawables().forEach(base->base.draw(gl));
  }
}
