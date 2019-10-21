package spacegraph.input.key;

import com.jogamp.newt.event.KeyEvent;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import spacegraph.video.JoglDisplay;

/**
 * Created by me on 11/20/16.
 */
class KeyXY extends SpaceKeys {
    private static final float speed = 8f;

    KeyXY(JoglDisplay g) {
        super(g);


        on((int) KeyEvent.VK_NUMPAD4, new FloatProcedure() {
            @Override
            public void value(float dt) {
                KeyXY.this.moveX(speed);
            }
        }, null);
        on((int) KeyEvent.VK_NUMPAD6, new FloatProcedure() {
            @Override
            public void value(float dt) {
                KeyXY.this.moveX(-speed);
            }
        }, null);

        on((int) KeyEvent.VK_NUMPAD8, new FloatProcedure() {
            @Override
            public void value(float dt) {
                KeyXY.this.moveY(speed);
            }
        }, null);
        on((int) KeyEvent.VK_NUMPAD2, new FloatProcedure() {
            @Override
            public void value(float dt) {
                KeyXY.this.moveY(-speed);
            }
        }, null);


        on((int) KeyEvent.VK_NUMPAD5, new FloatProcedure() {
            @Override
            public void value(float dt) {
                KeyXY.this.moveZ(speed);
            }
        }, null);
        on((int) KeyEvent.VK_NUMPAD0, new FloatProcedure() {
            @Override
            public void value(float dt) {
                KeyXY.this.moveZ(-speed);
            }
        }, null);

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
