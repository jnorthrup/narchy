package kashiki;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.Widget;
import spacegraph.video.Draw;


public class KashikiSurface extends Widget {

    private final Editor editor;

    public KashikiSurface(Editor editor) {
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
    protected void paintWidget(GL2 gl, RectFloat bounds) {
        Draw.bounds(bounds, gl, g -> {
                    gl.glTranslatef(0.5f, 0.5f, 0); //HACK
                    gl.glScalef(0.25f, 0.25f, 0.25f); //HACK

                    editor.drawables().forEach(base -> base.draw(g));
        });

    }

    @Override
    public boolean key(KeyEvent e, boolean pressed) {
        if (pressed) {
            if (!e.isPrintableKey())
                editor.keyPressed(null, e.getKeyCode(), e.getWhen());
        } else {
            //if (e.isPrintableKey())
                editor.keyTyped(e.getKeyChar(), e.getWhen());
            //else  editor.keyReleased...
        }
        return true;
    }
}
