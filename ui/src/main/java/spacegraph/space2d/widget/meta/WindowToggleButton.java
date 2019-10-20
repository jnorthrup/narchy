package spacegraph.space2d.widget.meta;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import jcog.exe.Exe;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.video.JoglDisplay;

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

    private volatile JoglDisplay space;

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
    protected void onClick() {
        onClick(null);
    }

    @Override
    protected void onClick(@Nullable Finger f) {
        if (!enabled() || !busy.compareAndSet(false, true))
            return;

        on(space == null);

        synchronized(this) {
            if (this.space == null) {

                this.space = SpaceGraph.window(spacer.get(), width, height);

//                space.pre(s -> {
                Exe.runLater(()->{
                    GLWindow w = space.video.window;
                    
                        w.addWindowListener(this);
                        if (f!=null) {
                            int nx = Math.round(f.posPixel.x - (float) width / 2f);
                            int ny = Math.round(f.posPixel.y - (float) height / 2f);
                            space.video.setPosition(nx, ny);
                        }
                    
                        busy.set(false); 
                    
                });

                
                

            } else if (space != null) {

                busy.set(false);

                on(false);

                this.space.video.off();
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
        on(false);
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
