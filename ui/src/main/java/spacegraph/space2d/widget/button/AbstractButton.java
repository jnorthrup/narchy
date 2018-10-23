package spacegraph.space2d.widget.button;

import com.jogamp.newt.event.KeyEvent;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.Widget;

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


    protected abstract void onClick();

    /** when clicked by finger */
    protected void onClick(Finger f) {
        onClick();
    }

    /** when clicked by key press */
    protected void onClick(KeyEvent key) {
        onClick();
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
