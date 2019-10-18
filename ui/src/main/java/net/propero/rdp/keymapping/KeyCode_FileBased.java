/* KeyCode_FileBased.java
 * Component: ProperJavaRDP
 *
 * Revision: $Revision: #2 $
 * Author: $Author: tvkelley $
 * Date: $Date: 2009/09/15 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Read and supply keymapping information from a file
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
package net.propero.rdp.keymapping;

import net.propero.rdp.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;

public abstract class KeyCode_FileBased {

    public static final int SCANCODE_EXTENDED = 0x80;
    public static final int DOWN = 1;
    public static final int UP = 0;
    public static final int QUIETUP = 2;
    public static final int QUIETDOWN = 3;
    private static final Logger logger = LoggerFactory.getLogger(KeyCode_FileBased.class);
    private final Vector keyMap = new Vector();
    private final Hashtable keysCurrentlyDown = new Hashtable();
    public boolean useLockingKeyState = true;
    protected boolean capsLockDown;
    private KeyEvent lastKeyEvent;
    private boolean lastEventMatched;
    private int mapCode = -1;
    private boolean altQuiet;

    protected KeyCode_FileBased(InputStream fstream) throws KeyMapException {
        readMapFile(fstream);
    }

    /**
     * Constructor for a keymap generated from a specified file, formatted in
     * the manner of a file generated by the writeToFile method
     *
     * @param keyMapFile File containing keymap data
     */
    protected KeyCode_FileBased(String keyMapFile) throws KeyMapException {


        try {
            FileInputStream fstream = new FileInputStream(keyMapFile);
            readMapFile(fstream);
        } catch (FileNotFoundException e) {
            throw new KeyMapException("KeyMap file not found: " + keyMapFile);
        }
    }

    /**
     * Given an alphanumeric character, return an AWT keycode
     *
     * @param keyChar Alphanumeric character
     * @return AWT keycode representing input character, -1 if character not
     * alphanumeric
     */
    private static int getCodeFromAlphaChar(char keyChar) {
        if (('a' <= keyChar) && (keyChar <= 'z')) {
            return KeyEvent.VK_A + keyChar - 'a';
        }
        if (('A' <= keyChar) && (keyChar <= 'Z')) {
            return KeyEvent.VK_A + keyChar - 'A';
        }

        return -1;
    }

    private void updateCapsLock(KeyEvent e) {

    }

    /**
     * Read in a keymap definition file and add mappings to internal keymap
     *
     * @param fstream Stream connected to keymap file
     * @throws KeyMapException
     */
    private void readMapFile(InputStream fstream) throws KeyMapException {

        if (fstream == null)
            throw new KeyMapException("Could not find specified keymap file");

        boolean mapCodeSet = false;

        int lineNum = 0;
        try {
            DataInputStream in = new DataInputStream(fstream);

            if (in == null)
                logger.warn("in == null");

            String line = "";
            while (in.available() != 0) {
                lineNum++;
                line = in.readLine();

                char fc = 0x0;
                if ((line != null) && (!line.isEmpty()))
                    fc = line.charAt(0);

                
                if ((line != null) && (!line.isEmpty()) && (fc != '#')
                        && (fc != 'c')) {
                    keyMap.add(new MapDef(line)); 
                    

                } else if (fc == 'c') {
                    StringTokenizer st = new StringTokenizer(line);
                    st.nextToken();

                    String s = st.nextToken();
                    mapCode = Integer.decode(s);
                    mapCodeSet = true;
                }
            }

            
            

            Vector newMap = new Vector();

            for (Object aKeyMap : keyMap) {
                MapDef current = (MapDef) aKeyMap;
                if (current.isCharacterDef()
                        && !(current.isAltDown() || current.isCtrlDown()
                        || current.isShiftDown() || current
                        .isCapslockOn())) {
                    int code = getCodeFromAlphaChar(current.getKeyChar());
                    if (code > -1) {

                        newMap.add(new MapDef(code, 0, current.getScancode(),
                                true, false, false, false));
                        newMap.add(new MapDef(code, 0, current.getScancode(),
                                false, false, true, false));
                    }
                }
            }
            
            keyMap.addAll(newMap);

            in.close();
        } catch (IOException e) {
            throw new KeyMapException("File input error: " + e.getMessage());
        } catch (NumberFormatException nfEx) {
            throw new KeyMapException(nfEx.getMessage() + " is not numeric at line " + lineNum);
        } catch (NoSuchElementException nseEx) {
            throw new KeyMapException(
                    "Not enough parameters in definition at line " + lineNum);
        } catch (KeyMapException kmEx) {
            throw new KeyMapException("Error parsing keymap file: "
                    + kmEx.getMessage() + " at line " + lineNum);
        } catch (Exception e) {
            logger.error("{}: {}", e.getClass().getName(), e.getMessage());
            e.printStackTrace();
            throw new KeyMapException(e.getClass().getName() + ": "
                    + e.getMessage());
        }

        if (!mapCodeSet)
            throw new KeyMapException("No map identifier found in file");
    }

    /**
     * Get the RDP code specifying the key map in use
     *
     * @return ID for current key map
     */
    public int getMapCode() {
        return mapCode;
    }

    /**
     * Construct a list of changes to key states in order to correctly send the
     * key action jointly defined by the supplied key event and mapping
     * definition.
     *
     * @param e      Key event received by Java (defining current state)
     * @param theDef Key mapping to define desired keypress on server end
     */
    private String stateChanges(KeyEvent e, MapDef theDef) {

        final int SHIFT = 0;

        int BEFORE = 0;

        boolean[][] state = new boolean[4][2];

        state[SHIFT][BEFORE] = e.isShiftDown();
        int AFTER = 1;
        state[SHIFT][AFTER] = theDef.isShiftDown();

        final int CTRL = 1;
        state[CTRL][BEFORE] = e.isControlDown() || e.isAltGraphDown();
        state[CTRL][AFTER] = theDef.isCtrlDown();

        final int ALT = 2;
        state[ALT][BEFORE] = e.isAltDown() || e.isAltGraphDown();
        state[ALT][AFTER] = theDef.isAltDown();

        updateCapsLock(e);

        final int CAPSLOCK = 3;
        state[CAPSLOCK][BEFORE] = capsLockDown;
        state[CAPSLOCK][AFTER] = theDef.isCapslockOn();

        if (e.getID() == KeyEvent.KEY_RELEASED) {
            AFTER = 0;
            BEFORE = 1;
        }

        if ((e == null) || (theDef == null) || (!theDef.isCharacterDef()))
            return "";

        String up = "" + ((char) UP);
        String down = "" + ((char) DOWN);

        String changes = "";
        if (state[SHIFT][BEFORE] != state[SHIFT][AFTER]) {
            if (state[SHIFT][BEFORE])
                changes += ((char) 0x2a) + up;
            else
                changes += ((char) 0x2a) + down;
        }

        if (state[CTRL][BEFORE] != state[CTRL][AFTER]) {
            if (state[CTRL][BEFORE])
                changes += ((char) 0x1d) + up;
            else
                changes += ((char) 0x1d) + down;
        }

        if (Options.altkey_quiet) {

            String quietdown = "" + ((char) QUIETDOWN);
            String quietup = "" + ((char) QUIETUP);
            if (state[ALT][BEFORE] != state[ALT][AFTER]) {
                if (state[ALT][BEFORE])
                    changes += (char) 0x38 + quietup + ((char) 0x38)
                            + quietdown + ((char) 0x38) + up;
                else {
                    if (e.getID() == KeyEvent.KEY_RELEASED) {
                        altQuiet = true;
                        changes += ((char) 0x38) + quietdown;
                    } else {
                        altQuiet = false;
                        changes += ((char) 0x38) + down;
                    }
                }

            } else if (state[ALT][AFTER] && altQuiet) {
                altQuiet = false;
                changes += (char) 0x38 + quietup + ((char) 0x38) + quietdown
                        + ((char) 0x38) + up + ((char) 0x38) + down;
            }

        } else {
            if (state[ALT][BEFORE] != state[ALT][AFTER]) {
                if (state[ALT][BEFORE])
                    changes += ((char) 0x38) + up;
                else
                    changes += ((char) 0x38) + down;
            }
        }

        if (state[CAPSLOCK][BEFORE] != state[CAPSLOCK][AFTER]) {
            changes += ((char) 0x3a) + down + ((char) 0x3a) + up;
        }

        return changes;
    }

    /**
     * Output key map definitions to a file as a series of single line text
     * descriptions
     *
     * @param filename File in which to store definitions
     */
    public void writeToFile(String filename) {
        try {
            FileOutputStream out = new FileOutputStream(filename);
            PrintStream p = new PrintStream(out);

            for (Object aKeyMap : keyMap) {
                ((MapDef) aKeyMap).writeToStream(p);
            }

            p.close();

        } catch (Exception e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    /**
     * Retrieve the scancode corresponding to the supplied character as defined
     * within this object. Also update the mod array to hold any modifier keys
     * that are required to send alongside it.
     *
     * @param c   Character to obtain scancode for
     * @param mod List of modifiers to be updated by method
     * @return Scancode of supplied key
     */
    private boolean hasScancode(char c) {
        if (c == KeyEvent.CHAR_UNDEFINED)
            return false;

        Iterator i = keyMap.iterator();
        MapDef best = null;

        while (i.hasNext()) {
            MapDef current = (MapDef) i.next();
            if (current.appliesTo(c)) {
                best = current;
            }
        }

        return (best != null);

    }

    /**
     * Retrieve the scancode corresponding to the supplied character as defined
     * within this object. Also update the mod array to hold any modifier keys
     * that are required to send alongside it.
     *
     * @param c   Character to obtain scancode for
     * @param mod List of modifiers to be updated by method
     * @return Scancode of supplied key
     */
    public int charToScancode(char c, String[] mod) {
        Iterator i = keyMap.iterator();
        MapDef best = null;

        while (i.hasNext()) {
            MapDef current = (MapDef) i.next();
            if (current.appliesTo(c)) {
                best = current;
            }
        }

        if (best != null) {
            if (best.isShiftDown())
                mod[0] = "SHIFT";
            else if (best.isCtrlDown() && best.isAltDown())
                mod[0] = "ALTGR";
            else
                mod[0] = "NONE";
            return best.getScancode();
        } else
            return -1;

    }

    /**
     * Return a mapping definition associated with the supplied key event from
     * within the list stored in this object.
     *
     * @param e Key event to retrieve a definition for
     * @return Mapping definition for supplied keypress
     */
    private MapDef getDef(KeyEvent e) {

        if (e.getID() == KeyEvent.KEY_RELEASED) {
            MapDef def = (MapDef) keysCurrentlyDown.get(e
                    .getKeyCode());
            registerKeyEvent(e, def);
            if (e.getID() == KeyEvent.KEY_RELEASED)
                logger.debug("Released: {} returned scancode: {}", e.getKeyCode(), (def != null) ? String.valueOf(def.getScancode()) : "null");
            return def;
        }

        updateCapsLock(e);

        Iterator i = keyMap.iterator();
        int smallestDist = -1;
        MapDef best = null;

        boolean noScanCode = !hasScancode(e.getKeyChar());

        while (i.hasNext()) {
            MapDef current = (MapDef) i.next();
            boolean applies;

            if ((e.getID() == KeyEvent.KEY_PRESSED)) {
                applies = current.appliesToPressed(e);
            } else if ((!lastEventMatched) && (e.getID() == KeyEvent.KEY_TYPED)) {
                applies = current.appliesToTyped(e, capsLockDown);
            } else
                applies = false;

            if (applies) {
                int d = current.modifierDistance(e, capsLockDown);
                if ((smallestDist == -1) || (d < smallestDist)) {
                    smallestDist = d;
                    best = current;
                }
            }
        }

        if (e.getID() == KeyEvent.KEY_PRESSED)
            logger.debug("Pressed: {} returned scancode: {}", e.getKeyCode(), (best != null) ? String.valueOf(best.getScancode()) : "null");
        if (e.getID() == KeyEvent.KEY_TYPED)
            logger.debug("Typed: {} returned scancode: {}", e.getKeyChar(), (best != null) ? String.valueOf(best.getScancode()) : "null");

        registerKeyEvent(e, best);

        return best;
    }

    /**
     * Return a scancode for the supplied key event, from within the mapping
     * definitions stored in this object.
     *
     * @param e Key event for which to determine a scancode
     * @return Scancode for the supplied keypress, according to current mappings
     */
    public int getScancode(KeyEvent e) {

        MapDef d = getDef(e);

        if (d != null) {
            return d.getScancode();
        } else
            return -1;
    }

    private void registerKeyEvent(KeyEvent e, MapDef m) {

        if (e.getID() == KeyEvent.KEY_RELEASED) {
            keysCurrentlyDown.remove(e.getKeyCode());
            if ((!Options.caps_sends_up_and_down)
                    && (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)) {
                logger.debug("Turning CAPSLOCK off - key release");
                capsLockDown = false;
            }
            lastEventMatched = false;
        }

        if (e.getID() == KeyEvent.KEY_PRESSED) {
            lastKeyEvent = e;
            lastEventMatched = m != null;
            if ((Options.caps_sends_up_and_down)
                    && (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)) {
                logger.debug("Toggling CAPSLOCK");
                capsLockDown = !capsLockDown;
            } else if (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK) {
                logger.debug("Turning CAPSLOCK on - key press");
                capsLockDown = true;
            }
        }

        if (lastKeyEvent != null
                && m != null
                && !keysCurrentlyDown.containsKey(lastKeyEvent
                .getKeyCode())) {
            keysCurrentlyDown.put(lastKeyEvent.getKeyCode(), m);
            lastKeyEvent = null;
        }

    }

    /**
     * Construct a list of keystrokes needed to reproduce an AWT key event via
     * RDP
     *
     * @param e Keyboard event to reproduce
     * @return List of character pairs representing scancodes and key actions to
     * send to server
     */
    public String getKeyStrokes(KeyEvent e) {
        MapDef d = getDef(e);

        if (d == null)
            return "";

        String codes = stateChanges(e, d);

        String type = "";

        if (e.getID() == KeyEvent.KEY_RELEASED) {
            if ((!Options.caps_sends_up_and_down)
                    && (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)) {
                logger.debug("Sending CAPSLOCK toggle");
                codes = String.valueOf(((char) 0x3a)) + ((char) DOWN) + ((char) 0x3a) + ((char) UP) + codes;
            } else {
                type = "" + ((char) UP);
                codes = ((char) d.getScancode()) + type + codes;
            }
        } else {
            if ((!Options.caps_sends_up_and_down)
                    && (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)) {
                logger.debug("Sending CAPSLOCK toggle");
                codes += "" + ((char) 0x3a) + ((char) DOWN) + ((char) 0x3a)
                        + ((char) UP);
            } else {
                type = "" + ((char) DOWN);
                codes += ((char) d.getScancode()) + type;
            }
        }

        return codes;
    }
}
