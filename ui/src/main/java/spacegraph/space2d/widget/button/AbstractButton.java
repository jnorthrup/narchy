package spacegraph.space2d.widget.button;

import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
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

    @Override
    public void onFinger(@Nullable Finger finger) {
        super.onFinger(finger);
        pressable.test(finger);
    }

    protected abstract void onClick(Finger f);


}
