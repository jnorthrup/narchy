package spacegraph.space2d.widget.button;

import com.jogamp.newt.event.KeyEvent;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.state.Clicking;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.ImageTexture;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_SPACE;

/**
 * TODO abstract to FocusableWidget
 */
public abstract class AbstractButton extends Widget {


    static final int CLICK_BUTTON = 0;

    final Clicking click = new Clicking(CLICK_BUTTON,this, new Consumer<Finger>() {
        @Override
        public void accept(Finger f) {
            dz = (float) 0;
            onClick(f);
        }
    }, new Runnable() {
        @Override
        public void run() {
            dz = 0.5f;
        }
    }, new Runnable() {
        @Override
        public void run() {
            dz = 0f;
        }
    }, new Runnable() {
        @Override
        public void run() {
            dz = 0f;
        }
    });

    private final AtomicBoolean enabled = new AtomicBoolean(true);

    protected AbstractButton() {
        super();
    }

    protected AbstractButton(Surface content) {
        super(content);
    }

    @Override
    public Surface finger(Finger finger) {
        Surface result = this;
        boolean finished = false;
        Surface f = super.finger(finger);
        if (f == this) {
            if (enabled() && finger.test(click)) {
                result = this;
                finished = true;
            } else {
                boolean b = false;
                for (int i : new int[]{CLICK_BUTTON, 1, 0}) {
                    if (finger.dragging(i)) {
                        b = true;
                        break;
                    }
                }
                if (b) {
                    //allow pass-through for drag actions
                    result = null;
                    finished = true;
                }
            }


        }
        if (!finished) {
            result = f;
        }
        return result;
    }

    public AbstractButton icon(String s) {
        set(new ImageTexture(s).view());
        return this;
    }

    public AbstractButton text(String s) {

        set(

                s.length() < 32 ? new BitmapLabel(s) : new VectorLabel(s)
                //new BitmapLabel(s)
                //new VectorLabel(s)
        );


        tooltip(s);


        return this;
    }


    public final boolean enabled() {
        return enabled.getOpaque();
    }

    public final <B extends AbstractButton> B enabled(boolean e) {
        enabled.set(e);
        return (B)this;
    }

    protected abstract void onClick();

    /** when clicked by finger */
    protected void onClick(Finger f) {
        if (enabled())
            onClick();
    }

    /** when clicked by key press */
    private void onClick(KeyEvent key) {
        if (enabled()) {
            int keyCode = (int) key.getKeyCode();
            if (keyCode == (int) KeyEvent.VK_SPACE || keyCode == (int) KeyEvent.VK_ENTER)
                onClick();
        }
    }

    @Override
    public boolean key(KeyEvent e, boolean pressedOrReleased) {
        if (!super.key(e, pressedOrReleased)) {
            if (pressedOrReleased) {
                short c = e.getKeyCode();
                if ((int) c == VK_ENTER || (int) c == VK_SPACE) {
                    onClick(e);
                    return true;
                }
            }

        }
        return false;
    }

}
