package spacegraph.space2d.widget.button;

import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.Widget;

import java.util.function.Predicate;

/**
 * Created by me on 11/12/16.
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


    protected abstract void onClick(Finger f);


}
