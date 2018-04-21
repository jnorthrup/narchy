package spacegraph.space2d.widget.meta;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.video.JoglSpace;

import java.util.function.Supplier;

/**
 * toggle button, which when actived, creates a window, and when inactivated destroys it
 * TODO window width, height parameters
 */
public class WindowToggleButton extends CheckBox implements WindowListener {

    private final Supplier spacer;

    int width = 600, height = 300;

    private volatile JoglSpace space;

    public WindowToggleButton(String text, Object o) {
        this(text, () -> o);
    }

    public WindowToggleButton(String text, Supplier spacer) {
        super(text);
        this.spacer = spacer;
    }

    public WindowToggleButton(String text, Supplier spacer, int w, int h) {
        this(text, spacer);
        this.width = w;
        this.height = h;
    }


    @Override
    public void toggle() {
        synchronized (this) {

            if (this.space == null) {

                this.space = SpaceGraph.window(spacer.get(), width, height);
                space.addWindowListener(this);

                space.pre(s -> {
                    GLWindow w = s.window;
                    if (w != null) {
                        int sx = Finger.pointer.getX();
                        int sy = Finger.pointer.getY();
                        int nx = sx - width / 2;
                        int ny = sy - height / 2;
                        s.setPosition(nx, ny);
                    }
                });

                //space.show(this.toString(), width,height, nx, ny);
                //space.window.setTitle(label.value());

            } else if (space != null) {

                this.space.off();
                this.space = null;

            }

        }
    }

    @Override
    public void windowResized(WindowEvent e) {

    }

    @Override
    public void windowMoved(WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {

    }

    @Override
    public void windowDestroyed(WindowEvent e) {
        this.space = null;
        set(false);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {

    }

    @Override
    public void windowLostFocus(WindowEvent e) {

    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {

    }


}
