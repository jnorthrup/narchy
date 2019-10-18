package spacegraph.space3d.widget;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.GL2;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.video.Draw;
import spacegraph.video.JoglWindow;

/**
 * Created by me on 6/27/16.
 */
public class CrosshairSurface extends PaintSurface implements MouseListener {

    private final JoglWindow space;
    private boolean mouseEnabled;
    private float smx;
    private float smy;
    private short[] pressed;


    public CrosshairSurface(JoglWindow s) {
        this.space = s;
    }


    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        gl.glPushMatrix();

        if (!mouseEnabled) {
            
            space.addMouseListenerPost(this);
            mouseEnabled = true;
        }


        
        float g, b;
        float r = g = b = 0.75f;
        if (pressed!=null && pressed.length > 0) {
            switch (pressed[0]) {
                case 1:
                    r = 1f; g = 0.5f; b = 0f;
                    break;
                case 2:
                    r = 0.5f; g = 1f; b = 0f;
                    break;
                case 3:
                    r = 0f; g = 0.5f; b = 1f;
                    break;
            }
        }
        gl.glColor4f(r, g, b, 0.6f);

        gl.glLineWidth(4f);
        float ch = 175f; 
        float cw = 175f; 
        Draw.rectStroke(smx-cw/2f, smy-ch/2f, cw, ch, gl);

        float hl = 1.25f; 
        Draw.line(smx, smy-ch*hl, smx, smy+ch*hl, gl);
        Draw.line(smx-cw*hl, smy, smx+cw*hl, smy, gl);

        gl.glPopMatrix();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        update(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        update(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        update(e);
    }

    private void update(MouseEvent e) {
        int mx = e.getX();
        smx = mx;
        int my = e.getY();
        smy = (space.getHeight() - ((float) my)) ;

        pressed = e.getButtonsDown();
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        update(e);
    }


}
