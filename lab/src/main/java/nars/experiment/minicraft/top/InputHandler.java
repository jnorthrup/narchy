package nars.experiment.minicraft.top;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class InputHandler implements KeyListener {
    public final class Key {
        public int presses, absorbs;
        public boolean down, clicked;

        public Key() {
            keys.add(this);
        }

        public void pressIfUnpressed() {
            if (!clicked)
                pressed(true);
        }

        public void pressed(boolean pressed) {

            down = pressed;

            if (pressed) {
                presses++;
            }


        }

        public void tick() {
            if (absorbs < presses) {
                absorbs++;
                clicked = true;
            } else {
                clicked = false;
            }
        }
    }

    public final List<Key> keys = new ArrayList<>();

    public final Key up = new Key();
    public final Key down = new Key();
    public final Key left = new Key();
    public final Key right = new Key();
    public final Key attack = new Key();
    public final Key menu = new Key();

    public void releaseAll() {
        for (int i = 0; i < keys.size(); i++) {
            keys.get(i).down = false;
        }
    }

    public void tick() {
        for (int i = 0; i < keys.size(); i++) {
            keys.get(i).tick();
        }
    }

    public InputHandler(TopDownMinicraft game) {
        game.addKeyListener(this);
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        toggle(ke, true);
    }

    @Override
    public void keyReleased(KeyEvent ke) {
        toggle(ke, false);
    }

    private void toggle(KeyEvent ke, boolean pressed) {
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD8) up.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD2) down.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD4) left.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD6) right.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_W) up.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_S) down.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_A) left.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_D) right.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_UP) up.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_DOWN) down.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_LEFT) left.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_RIGHT) right.pressed(pressed);

        if (ke.getKeyCode() == KeyEvent.VK_TAB) menu.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_ALT) menu.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_ALT_GRAPH) menu.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_SPACE) attack.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) attack.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_NUMPAD0) attack.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_INSERT) attack.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) menu.pressed(pressed);

        if (ke.getKeyCode() == KeyEvent.VK_X) menu.pressed(pressed);
        if (ke.getKeyCode() == KeyEvent.VK_C) attack.pressed(pressed);
    }

    @Override
    public void keyTyped(KeyEvent ke) {
    }
}
