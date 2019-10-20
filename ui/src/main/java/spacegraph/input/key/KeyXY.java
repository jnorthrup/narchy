package spacegraph.input.key;

import com.jogamp.newt.event.KeyEvent;
import spacegraph.video.JoglDisplay;

/**
 * Created by me on 11/20/16.
 */
class KeyXY extends SpaceKeys {
    private static final float speed = 8f;

    KeyXY(JoglDisplay g) {
        super(g);


        on((int) KeyEvent.VK_NUMPAD4, (dt)-> moveX(speed), null);
        on((int) KeyEvent.VK_NUMPAD6, (dt)-> moveX(-speed), null);

        on((int) KeyEvent.VK_NUMPAD8, (dt)-> moveY(speed), null);
        on((int) KeyEvent.VK_NUMPAD2, (dt)-> moveY(-speed), null);


        on((int) KeyEvent.VK_NUMPAD5, (dt)-> moveZ(speed), null);
        on((int) KeyEvent.VK_NUMPAD0, (dt)-> moveZ(-speed), null);

    }

    void moveX(float speed) {
        space.camPos.add(speed, (float) 0, (float) 0);
    }

    void moveY(float speed) {
        space.camPos.add((float) 0, speed, (float) 0);
    }

    void moveZ(float speed) {
        space.camPos.add((float) 0, (float) 0, speed);
    }

}
