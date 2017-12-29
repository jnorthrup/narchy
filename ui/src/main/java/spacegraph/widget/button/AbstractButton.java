package spacegraph.widget.button;

import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.widget.windo.Widget;

import java.util.function.Consumer;

/**
 * Created by me on 11/12/16.
 */
public abstract class AbstractButton extends Widget {

    private boolean pressed;

    Consumer<Finger> pressable = Finger.clicked(0, ()->{
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
    public void touch(@Nullable Finger finger) {
        super.touch(finger);
        pressable.accept(finger);
    }

    protected abstract void onClick();

//    static void label(GL2 gl, String text) {
//        gl.glColor3f(0.75f, 0.75f, 0.75f);
//        gl.glLineWidth(2);
//        Draw.text(gl, text, 1f/(1+text.length()), 0.5f, 0.5f,  0);
//    }

}
