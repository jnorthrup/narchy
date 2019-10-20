/*
 * Java Arcade Learning Environment (A.L.E) Agent
 *  Copyright (C) 2011-2012 Marc G. Bellemare <mgbellemare@ualberta.ca>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http:
 */
package jurls.reinforcementlearning.domains.arcade.gui;

import jurls.reinforcementlearning.domains.arcade.io.Actions;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/** A crude keyboard controller. The following keys are mapped:
 *    R - reset
 *    Q - stop
 *    space - fire
 *    ASWD, arrow keys - joystick movement
 *
 * @author Marc G. Bellemare
 */
public class KeyboardControl implements KeyListener {
    /** Variables used to keep track of which keys are pressed */
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;
    public boolean fire;

    public boolean reset;

    public boolean quit;

    /** Creates a new keyboard controller.
     * 
     */
    public KeyboardControl() {
        
        up = down = left = right = fire = false;
        reset = false;
        quit = false;
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        
        switch (e.getKeyCode()) {
          case KeyEvent.VK_UP:
          case KeyEvent.VK_W:
            up = true;
            break;
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_S:
            down = true;
            break;
          case KeyEvent.VK_LEFT:
          case KeyEvent.VK_A:
            left = true;
            break;
          case KeyEvent.VK_RIGHT:
          case KeyEvent.VK_D:
            right = true;
            break;
          case KeyEvent.VK_SPACE:
            fire = true;
            break;
          case KeyEvent.VK_R:
            reset = true;
            break;
          case KeyEvent.VK_ESCAPE:
            quit = true;
            break;
        }
    }

    public void keyReleased(KeyEvent e) {
        
        switch (e.getKeyCode()) {
          case KeyEvent.VK_UP:
          case KeyEvent.VK_W:
            up = false;
            break;
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_S:
            down = false;
            break;
          case KeyEvent.VK_LEFT:
          case KeyEvent.VK_A:
            left = false;
            break;
          case KeyEvent.VK_RIGHT:
          case KeyEvent.VK_D:
            right = false;
            break;
          case KeyEvent.VK_SPACE:
            fire = false;
            break;
          case KeyEvent.VK_R:
            reset = false;
            break;
          case KeyEvent.VK_ESCAPE:
            quit = false;
            break;
        }
    }

    /** An array to map a bit-wise representation of the keypresses to ALE actions.
      * 1 = fire, 2 = up, 4 = right, 8 = left, 16 = down
      *
      * -1 indicate an invalid combination, e.g. left/right or up/down. These should
      * be filtered out in toALEAction.
      */
    private final int[] bitKeysMap = {
        0, 1, 2, 10, 3, 11, 6, 14, 4, 12, 7, 15, -1, -1, -1, -1,
        5, 13, -1, -1, 8, 16, -1, -1, 9, 17, -1, -1, -1, -1, -1, -1
    };
    
    /** Converts the current keypresses to an ALE action (for player A).
     * 
     * @return
     */
    public int toALEAction() {


        if (reset) return Actions.map("system_reset");


        var bitfield = 0;
        if (left == right) bitfield |= 0;
        else if (left) bitfield |= 0x08;
        else if (right) bitfield |= 0x04;

        if (up == down) bitfield |= 0;
        else if (up) bitfield |= 0x02;
        else if (down) bitfield |= 0x10;

        if (fire) bitfield |= 0x01;

        
        return bitKeysMap[bitfield];
    }

}
