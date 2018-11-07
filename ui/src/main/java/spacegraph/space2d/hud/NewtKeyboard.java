package spacegraph.space2d.hud;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.Surface;

import java.util.concurrent.atomic.AtomicReference;

/** interface for NEWT keyboard */
public class NewtKeyboard implements KeyListener {
    @Override
    public void keyPressed(KeyEvent e) {
        setKey(e, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        setKey(e, false);
    }

    private void setKey(KeyEvent e, boolean pressOrRelease) {
        if (e.isConsumed())
            return;

//        Surface t = finger.touching();
//        if (t != null) {
//            if (!t.key(e, pressOrRelease))
//                e.setConsumed(true);
//        }
        Surface s = keyFocus.getOpaque();
        if (s != null) {
            Surface ss = s;
            if (!ss.showing()) {
                keyFocus.compareAndSet(s, null); //free the focus
                return;
            }

            ((KeyPressed)s).key(e, pressOrRelease);
            e.setConsumed(true);
        }

    }

    final AtomicReference<Surface> keyFocus = new AtomicReference<>(null);


    boolean focus(Surface s) {
        if (!(s instanceof KeyPressed))
            throw new UnsupportedOperationException(s + " does not implement " + KeyPressed.class);

        KeyPressed ss = (KeyPressed) s;
        Surface r = keyFocus.getAndSet(s);
        if (r!=s) {
            if (r!=null)
                ((KeyPressed)r).keyEnd();
            ss.keyStart();
        }

        return true;
    }

    public Surface keyFocusSurface(Ortho.Camera cam) {
        return new SurfaceHiliteOverlay(cam) {

            @Override
            protected Surface target() {
                return keyFocus.getOpaque();
            }

        };
    }
}
