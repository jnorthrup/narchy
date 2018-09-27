package spacegraph.space2d.widget.meta;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import jcog.exe.Exe;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.video.JoglSpace;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * toggle button, which when actived, creates a window, and when inactivated destroys it
 * TODO window width, height parameters
 */
public class WindowToggleButton extends CheckBox implements WindowListener {

    private final Supplier spacer;

    private int width = 600;
    private int height = 300;

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


    private final AtomicBoolean busy = new AtomicBoolean(false);

    @Override
    protected void onClick(Finger f) {
        if (!busy.compareAndSet(false, true))
            return;

        set(space == null);

        synchronized(this) {
            if (this.space == null) {

                this.space = SpaceGraph.window(spacer.get(), width, height, true);

//                space.pre(s -> {
                Exe.invokeLater(()->{
                    GLWindow w = space.window;
                    
                        w.addWindowListener(this);
                        int nx = Math.round(f.posPixel.x - width / 2f);
                        int ny = Math.round(f.posPixel.y - height / 2f);
                        space.setPosition(nx, ny);
                    
                    
                        busy.set(false); 
                    
                });

                
                

            } else if (space != null) {

                busy.set(false);

                set(false);

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
