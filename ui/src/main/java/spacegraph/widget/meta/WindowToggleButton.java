package spacegraph.widget.meta;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.SpaceGraph;
import spacegraph.input.Finger;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.ToggleButton;

import java.util.function.Supplier;

/** toggle button, which when actived, creates a window, and when inactivated destroys it
 *  TODO window width, height parameters
 * */
public class WindowToggleButton extends CheckBox implements WindowListener, ObjectBooleanProcedure<ToggleButton> {

    private final Supplier spacer;

    int width = 600, height = 300;

    SpaceGraph space;

    public WindowToggleButton(String text, Object o) {
        this(text, ()->o);
    }

    public WindowToggleButton(String text, Supplier spacer) {
        super(text);
        this.spacer = spacer;
        on(this);
    }
    public WindowToggleButton(String text, Supplier spacer, int w, int h) {
        this(text, spacer);
        this.width = w; this.height = h;
    }

    @Override
    public void value(ToggleButton t, boolean enabled) {
        SpaceGraph space = this.space;
        synchronized (spacer) {
            if (enabled) {
                if (space == null) {
                    space = this.space = SpaceGraph.window(spacer.get(), width, height);

                    int sx = Finger.pointer.getX();
                    int sy = Finger.pointer.getY();
                    int nx = sx - width/2;
                    int ny = sy - height/2;
                    space.window.setPosition(nx, ny);

                    //space.show(this.toString(), width,height, nx, ny);
                    //space.window.setTitle(label.value());
                    space.addWindowListener(this);

                }
            } else {
                if (space!=null) {
                    GLWindow win = this.space.window;
                    this.space = null;
                    if (win != null && win.getWindowHandle() != 0)
                        win.destroy();
                }
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
