package spacegraph.space2d.widget.button;

import com.jogamp.newt.event.KeyEvent;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.Widget;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_SPACE;

/**
 * TODO abstract to FocusableWidget
 */
public abstract class AbstractButton extends Widget {


    private final Predicate<Finger> pressable = Finger.clicked(0, (f) -> {
        dz = 0;
        onClick(f);
//        Exe.invoke/*Later*/(() ->
//                onClick(f));
    }, () -> dz = 0.5f, () -> dz = 0f, () -> dz = 0f);


    private final AtomicBoolean enabled = new AtomicBoolean(true);

    protected AbstractButton(Surface content) {
        super(content);
    }


    @Override
    public Surface finger(Finger finger) {
        Surface f = super.finger(finger);
        if (f == this) {

            if (pressable.test(finger))
                return this;
        }
        return f;
    }

    public final boolean enabled() {
        return enabled.get();
    }

    public final <B extends AbstractButton> B enabled(boolean e) {
        enabled.lazySet(e);
        return (B)this;
    }

    protected abstract void onClick();

    /** when clicked by finger */
    protected void onClick(Finger f) {
        if (enabled())
            onClick();
    }

    /** when clicked by key press */
    protected void onClick(KeyEvent key) {
        if (enabled()) {
            int keyCode = key.getKeyCode();
            if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER)
                onClick();
        }
    }



    @Override
    public boolean key(KeyEvent e, boolean pressedOrReleased) {
        if (!super.key(e, pressedOrReleased)) {
            if (pressedOrReleased) {
                short c = e.getKeyCode();
                if (c == VK_ENTER || c == VK_SPACE) {
                    onClick(e);
                    return true;
                }
            }

        }
        return false;
    }

}
