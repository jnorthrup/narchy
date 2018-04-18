package spacegraph.space2d.widget.button;

import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.widget.windo.Widget;

import java.util.function.Predicate;

/**
 * Created by me on 11/12/16.
 */
public abstract class AbstractButton extends Widget {



    final Predicate<Finger> pressable = Finger.clicked(0, ()->{
        dz = 0;
        onClick();
    }, ()-> {
        dz = 0.5f;
    }, () -> {
        dz = 0f;
    }, () -> {
        dz = 0f;
    });

    @Override
    public void onFinger(@Nullable Finger finger) {
        super.onFinger(finger);
        pressable.test(finger);
    }

    protected abstract void onClick();

//    static void label(GL2 gl, String text) {
//        gl.glColor3f(0.75f, 0.75f, 0.75f);
//        gl.glLineWidth(2);
//        Draw.text(gl, text, 1f/(1+text.length()), 0.5f, 0.5f,  0);
//    }

}
