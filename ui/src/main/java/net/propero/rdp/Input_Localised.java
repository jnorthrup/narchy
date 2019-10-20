/* Input_Localised.java
 * Component: ProperJavaRDP
 *
 * Revision: $Revision: #2 $
 * Author: $Author: tvkelley $
 * Date: $Date: 2009/09/15 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Java 1.4 specific extension of Input class
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 *
 * (See gpl.txt for details of the GNU General Public License.)
 *
 */

/**
 * Created on 07-Jul-2003
 */
package net.propero.rdp;

import net.propero.rdp.keymapping.KeyCode;
import net.propero.rdp.keymapping.KeyCode_FileBased;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Collections;

public class Input_Localised extends Input {
    public Input_Localised(RdesktopCanvas c, Rdp r, KeyCode_FileBased k) {
        super(c, r, k);
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .setDefaultFocusTraversalKeys(
                        KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                        Collections.emptySet());
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .setDefaultFocusTraversalKeys(
                        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                        Collections.emptySet());
    }

    public Input_Localised(RdesktopCanvas c, Rdp r, String k) {
        super(c, r, k);
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .setDefaultFocusTraversalKeys(
                        KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                        Collections.emptySet());
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .setDefaultFocusTraversalKeys(
                        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                        Collections.emptySet());
    }

    @Override
    public void clearKeys() {
        super.clearKeys();
        if (lastKeyEvent != null && lastKeyEvent.isAltGraphDown())
            sendScancode(getTime(), RDP_KEYRELEASE,
                    0x38 | KeyCode.SCANCODE_EXTENDED); 
    }

    @Override
    public void setKeys() {
        super.setKeys();
        if (lastKeyEvent != null && lastKeyEvent.isAltGraphDown())
            sendScancode(getTime(), RDP_KEYPRESS,
                    0x38 | KeyCode.SCANCODE_EXTENDED); 
    }

    @Override
    protected void doLockKeys() {
        
        
        
        if (!Rdesktop.readytosend)
            return;
        if (!Options.useLockingKeyState)
            return;
        if (Constants.OS == Constants.LINUX)
            return; 
        if (Constants.OS == Constants.MAC)
            return; 
        logger.debug("doLockKeys");

        try {
            var tk = Toolkit.getDefaultToolkit();
            if (tk.getLockingKeyState(KeyEvent.VK_CAPS_LOCK) != capsLockOn) {
                capsLockOn = !capsLockOn;
                logger.debug("CAPS LOCK toggle");
                sendScancode(getTime(), RDP_KEYPRESS, 0x3a);
                sendScancode(getTime(), RDP_KEYRELEASE, 0x3a);

            }
            if (tk.getLockingKeyState(KeyEvent.VK_NUM_LOCK) != numLockOn) {
                numLockOn = !numLockOn;
                logger.debug("NUM LOCK toggle");
                sendScancode(getTime(), RDP_KEYPRESS, 0x45);
                sendScancode(getTime(), RDP_KEYRELEASE, 0x45);

            }
            if (tk.getLockingKeyState(KeyEvent.VK_SCROLL_LOCK) != scrollLockOn) {
                scrollLockOn = !scrollLockOn;
                logger.debug("SCROLL LOCK toggle");
                sendScancode(getTime(), RDP_KEYPRESS, 0x46);
                sendScancode(getTime(), RDP_KEYRELEASE, 0x46);
            }
        } catch (Exception e) {
            Options.useLockingKeyState = false;
        }
    }

    @Override
    public void addInputListeners() {
        super.addInputListeners();
        canvas.addMouseWheelListener(new RdesktopMouseWheelAdapter());
    }

    @Override
    public boolean handleShortcutKeys(long time, KeyEvent e, boolean pressed) {
        if (super.handleShortcutKeys(time, e, pressed))
            return true;

        if (!altDown)
            return false; 

        switch (e.getKeyCode()) {
            case KeyEvent.VK_MINUS: 
                if (ctrlDown) {
                    if (pressed) {
                        sendScancode(time, RDP_KEYRELEASE, 0x1d); 
                        sendScancode(time, RDP_KEYPRESS,
                                0x37 | KeyCode.SCANCODE_EXTENDED); 
                        logger.debug("shortcut pressed: sent ALT+PRTSC");
                    } else {
                        sendScancode(time, RDP_KEYRELEASE,
                                0x37 | KeyCode.SCANCODE_EXTENDED); 
                        sendScancode(time, RDP_KEYPRESS, 0x1d); 
                    }
                }
                break;
            default:
                return false;
        }
        return true;
    }

    private class RdesktopMouseWheelAdapter implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            var time = getTime();
            
            if (rdp != null) {
                if (e.getWheelRotation() < 0) { 
                    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON4
                            | MOUSE_FLAG_DOWN, e.getX(), e.getY());
                } else { 
                    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON5
                            | MOUSE_FLAG_DOWN, e.getX(), e.getY());
                }
            }
        }
    }
}
