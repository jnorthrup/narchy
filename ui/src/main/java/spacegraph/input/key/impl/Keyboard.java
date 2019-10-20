package spacegraph.input.key.impl;


public enum Keyboard { ;

//    /** from: https://github.com/JogAmp/jogl/blob/master/src/newt/classes/jogamp/newt/awt/event/AWTNewtEventFactory.java */
//    public static short awtKeyCode2NewtKeyCode(final int awtKeyCode) {
//        final short defNEWTKeyCode = (short)awtKeyCode;
//        switch (awtKeyCode) {
//            case java.awt.event.KeyEvent.VK_HOME          : return com.jogamp.newt.event.KeyEvent.VK_HOME;
//            case java.awt.event.KeyEvent.VK_END           : return com.jogamp.newt.event.KeyEvent.VK_END;
//            case java.awt.event.KeyEvent.VK_FINAL         : return com.jogamp.newt.event.KeyEvent.VK_FINAL;
//            case java.awt.event.KeyEvent.VK_PRINTSCREEN   : return com.jogamp.newt.event.KeyEvent.VK_PRINTSCREEN;
//            case java.awt.event.KeyEvent.VK_BACK_SPACE    : return com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE;
//            case java.awt.event.KeyEvent.VK_TAB           : return com.jogamp.newt.event.KeyEvent.VK_TAB;
//            case java.awt.event.KeyEvent.VK_ENTER         : return com.jogamp.newt.event.KeyEvent.VK_ENTER;
//            case java.awt.event.KeyEvent.VK_PAGE_DOWN     : return com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN;
//            case java.awt.event.KeyEvent.VK_CLEAR         : return com.jogamp.newt.event.KeyEvent.VK_CLEAR;
//            case java.awt.event.KeyEvent.VK_SHIFT         : return com.jogamp.newt.event.KeyEvent.VK_SHIFT;
//            case java.awt.event.KeyEvent.VK_PAGE_UP       : return com.jogamp.newt.event.KeyEvent.VK_PAGE_UP;
//            case java.awt.event.KeyEvent.VK_CONTROL       : return com.jogamp.newt.event.KeyEvent.VK_CONTROL;
//            case java.awt.event.KeyEvent.VK_ALT           : return com.jogamp.newt.event.KeyEvent.VK_ALT;
//            case java.awt.event.KeyEvent.VK_ALT_GRAPH     : return com.jogamp.newt.event.KeyEvent.VK_ALT_GRAPH;
//            case java.awt.event.KeyEvent.VK_CAPS_LOCK     : return com.jogamp.newt.event.KeyEvent.VK_CAPS_LOCK;
//            case java.awt.event.KeyEvent.VK_PAUSE         : return com.jogamp.newt.event.KeyEvent.VK_PAUSE;
//            case java.awt.event.KeyEvent.VK_SCROLL_LOCK   : return com.jogamp.newt.event.KeyEvent.VK_SCROLL_LOCK;
//            case java.awt.event.KeyEvent.VK_CANCEL        : return com.jogamp.newt.event.KeyEvent.VK_CANCEL;
//            case java.awt.event.KeyEvent.VK_INSERT        : return com.jogamp.newt.event.KeyEvent.VK_INSERT;
//            case java.awt.event.KeyEvent.VK_ESCAPE        : return com.jogamp.newt.event.KeyEvent.VK_ESCAPE;
//            case java.awt.event.KeyEvent.VK_CONVERT       : return com.jogamp.newt.event.KeyEvent.VK_CONVERT;
//            case java.awt.event.KeyEvent.VK_NONCONVERT    : return com.jogamp.newt.event.KeyEvent.VK_NONCONVERT;
//            case java.awt.event.KeyEvent.VK_ACCEPT        : return com.jogamp.newt.event.KeyEvent.VK_ACCEPT;
//            case java.awt.event.KeyEvent.VK_MODECHANGE    : return com.jogamp.newt.event.KeyEvent.VK_MODECHANGE;
//            case java.awt.event.KeyEvent.VK_SPACE         : return com.jogamp.newt.event.KeyEvent.VK_SPACE;
//            case java.awt.event.KeyEvent.VK_EXCLAMATION_MARK: return com.jogamp.newt.event.KeyEvent.VK_EXCLAMATION_MARK;
//            case java.awt.event.KeyEvent.VK_QUOTEDBL      : return com.jogamp.newt.event.KeyEvent.VK_QUOTEDBL;
//            case java.awt.event.KeyEvent.VK_NUMBER_SIGN   : return com.jogamp.newt.event.KeyEvent.VK_NUMBER_SIGN;
//            case java.awt.event.KeyEvent.VK_DOLLAR        : return com.jogamp.newt.event.KeyEvent.VK_DOLLAR;
//            // case                         0x25             : return com.jogamp.newt.event.KeyEvent.VK_PERCENT;
//            case java.awt.event.KeyEvent.VK_AMPERSAND     : return com.jogamp.newt.event.KeyEvent.VK_AMPERSAND;
//            case java.awt.event.KeyEvent.VK_QUOTE         : return com.jogamp.newt.event.KeyEvent.VK_QUOTE;
//            case java.awt.event.KeyEvent.VK_LEFT_PARENTHESIS : return com.jogamp.newt.event.KeyEvent.VK_LEFT_PARENTHESIS;
//            case java.awt.event.KeyEvent.VK_RIGHT_PARENTHESIS: return com.jogamp.newt.event.KeyEvent.VK_RIGHT_PARENTHESIS;
//            case java.awt.event.KeyEvent.VK_ASTERISK      : return com.jogamp.newt.event.KeyEvent.VK_ASTERISK;
//            case java.awt.event.KeyEvent.VK_PLUS          : return com.jogamp.newt.event.KeyEvent.VK_PLUS;
//            case java.awt.event.KeyEvent.VK_COMMA         : return com.jogamp.newt.event.KeyEvent.VK_COMMA;
//            case java.awt.event.KeyEvent.VK_MINUS         : return com.jogamp.newt.event.KeyEvent.VK_MINUS;
//            case java.awt.event.KeyEvent.VK_PERIOD        : return com.jogamp.newt.event.KeyEvent.VK_PERIOD;
//            case java.awt.event.KeyEvent.VK_SLASH         : return com.jogamp.newt.event.KeyEvent.VK_SLASH;
//            case java.awt.event.KeyEvent.VK_0             : return com.jogamp.newt.event.KeyEvent.VK_0;
//            case java.awt.event.KeyEvent.VK_1             : return com.jogamp.newt.event.KeyEvent.VK_1;
//            case java.awt.event.KeyEvent.VK_2             : return com.jogamp.newt.event.KeyEvent.VK_2;
//            case java.awt.event.KeyEvent.VK_3             : return com.jogamp.newt.event.KeyEvent.VK_3;
//            case java.awt.event.KeyEvent.VK_4             : return com.jogamp.newt.event.KeyEvent.VK_4;
//            case java.awt.event.KeyEvent.VK_5             : return com.jogamp.newt.event.KeyEvent.VK_5;
//            case java.awt.event.KeyEvent.VK_6             : return com.jogamp.newt.event.KeyEvent.VK_6;
//            case java.awt.event.KeyEvent.VK_7             : return com.jogamp.newt.event.KeyEvent.VK_7;
//            case java.awt.event.KeyEvent.VK_8             : return com.jogamp.newt.event.KeyEvent.VK_8;
//            case java.awt.event.KeyEvent.VK_9             : return com.jogamp.newt.event.KeyEvent.VK_9;
//            case java.awt.event.KeyEvent.VK_COLON         : return com.jogamp.newt.event.KeyEvent.VK_COLON;
//            case java.awt.event.KeyEvent.VK_SEMICOLON     : return com.jogamp.newt.event.KeyEvent.VK_SEMICOLON;
//            case java.awt.event.KeyEvent.VK_LESS          : return com.jogamp.newt.event.KeyEvent.VK_LESS;
//            case java.awt.event.KeyEvent.VK_EQUALS        : return com.jogamp.newt.event.KeyEvent.VK_EQUALS;
//            case java.awt.event.KeyEvent.VK_GREATER       : return com.jogamp.newt.event.KeyEvent.VK_GREATER;
//            case                         0x3f             : return com.jogamp.newt.event.KeyEvent.VK_QUESTIONMARK;
//            case java.awt.event.KeyEvent.VK_AT            : return com.jogamp.newt.event.KeyEvent.VK_AT;
//            case java.awt.event.KeyEvent.VK_A             : return com.jogamp.newt.event.KeyEvent.VK_A;
//            case java.awt.event.KeyEvent.VK_B             : return com.jogamp.newt.event.KeyEvent.VK_B;
//            case java.awt.event.KeyEvent.VK_C             : return com.jogamp.newt.event.KeyEvent.VK_C;
//            case java.awt.event.KeyEvent.VK_D             : return com.jogamp.newt.event.KeyEvent.VK_D;
//            case java.awt.event.KeyEvent.VK_E             : return com.jogamp.newt.event.KeyEvent.VK_E;
//            case java.awt.event.KeyEvent.VK_F             : return com.jogamp.newt.event.KeyEvent.VK_F;
//            case java.awt.event.KeyEvent.VK_G             : return com.jogamp.newt.event.KeyEvent.VK_G;
//            case java.awt.event.KeyEvent.VK_H             : return com.jogamp.newt.event.KeyEvent.VK_H;
//            case java.awt.event.KeyEvent.VK_I             : return com.jogamp.newt.event.KeyEvent.VK_I;
//            case java.awt.event.KeyEvent.VK_J             : return com.jogamp.newt.event.KeyEvent.VK_J;
//            case java.awt.event.KeyEvent.VK_K             : return com.jogamp.newt.event.KeyEvent.VK_K;
//            case java.awt.event.KeyEvent.VK_L             : return com.jogamp.newt.event.KeyEvent.VK_L;
//            case java.awt.event.KeyEvent.VK_M             : return com.jogamp.newt.event.KeyEvent.VK_M;
//            case java.awt.event.KeyEvent.VK_N             : return com.jogamp.newt.event.KeyEvent.VK_N;
//            case java.awt.event.KeyEvent.VK_O             : return com.jogamp.newt.event.KeyEvent.VK_O;
//            case java.awt.event.KeyEvent.VK_P             : return com.jogamp.newt.event.KeyEvent.VK_P;
//            case java.awt.event.KeyEvent.VK_Q             : return com.jogamp.newt.event.KeyEvent.VK_Q;
//            case java.awt.event.KeyEvent.VK_R             : return com.jogamp.newt.event.KeyEvent.VK_R;
//            case java.awt.event.KeyEvent.VK_S             : return com.jogamp.newt.event.KeyEvent.VK_S;
//            case java.awt.event.KeyEvent.VK_T             : return com.jogamp.newt.event.KeyEvent.VK_T;
//            case java.awt.event.KeyEvent.VK_U             : return com.jogamp.newt.event.KeyEvent.VK_U;
//            case java.awt.event.KeyEvent.VK_V             : return com.jogamp.newt.event.KeyEvent.VK_V;
//            case java.awt.event.KeyEvent.VK_W             : return com.jogamp.newt.event.KeyEvent.VK_W;
//            case java.awt.event.KeyEvent.VK_X             : return com.jogamp.newt.event.KeyEvent.VK_X;
//            case java.awt.event.KeyEvent.VK_Y             : return com.jogamp.newt.event.KeyEvent.VK_Y;
//            case java.awt.event.KeyEvent.VK_Z             : return com.jogamp.newt.event.KeyEvent.VK_Z;
//            case java.awt.event.KeyEvent.VK_OPEN_BRACKET  : return com.jogamp.newt.event.KeyEvent.VK_OPEN_BRACKET;
//            case java.awt.event.KeyEvent.VK_BACK_SLASH    : return com.jogamp.newt.event.KeyEvent.VK_BACK_SLASH;
//            case java.awt.event.KeyEvent.VK_CLOSE_BRACKET : return com.jogamp.newt.event.KeyEvent.VK_CLOSE_BRACKET;
//            case java.awt.event.KeyEvent.VK_CIRCUMFLEX    : return com.jogamp.newt.event.KeyEvent.VK_CIRCUMFLEX;
//            case java.awt.event.KeyEvent.VK_UNDERSCORE    : return com.jogamp.newt.event.KeyEvent.VK_UNDERSCORE;
//            case java.awt.event.KeyEvent.VK_BACK_QUOTE    : return com.jogamp.newt.event.KeyEvent.VK_BACK_QUOTE;
//            case java.awt.event.KeyEvent.VK_F1            : return com.jogamp.newt.event.KeyEvent.VK_F1;
//            case java.awt.event.KeyEvent.VK_F2            : return com.jogamp.newt.event.KeyEvent.VK_F2;
//            case java.awt.event.KeyEvent.VK_F3            : return com.jogamp.newt.event.KeyEvent.VK_F3;
//            case java.awt.event.KeyEvent.VK_F4            : return com.jogamp.newt.event.KeyEvent.VK_F4;
//            case java.awt.event.KeyEvent.VK_F5            : return com.jogamp.newt.event.KeyEvent.VK_F5;
//            case java.awt.event.KeyEvent.VK_F6            : return com.jogamp.newt.event.KeyEvent.VK_F6;
//            case java.awt.event.KeyEvent.VK_F7            : return com.jogamp.newt.event.KeyEvent.VK_F7;
//            case java.awt.event.KeyEvent.VK_F8            : return com.jogamp.newt.event.KeyEvent.VK_F8;
//            case java.awt.event.KeyEvent.VK_F9            : return com.jogamp.newt.event.KeyEvent.VK_F9;
//            case java.awt.event.KeyEvent.VK_F10           : return com.jogamp.newt.event.KeyEvent.VK_F10;
//            case java.awt.event.KeyEvent.VK_F11           : return com.jogamp.newt.event.KeyEvent.VK_F11;
//            case java.awt.event.KeyEvent.VK_F12           : return com.jogamp.newt.event.KeyEvent.VK_F12;
//            case java.awt.event.KeyEvent.VK_F13           : return com.jogamp.newt.event.KeyEvent.VK_F13;
//            case java.awt.event.KeyEvent.VK_F14           : return com.jogamp.newt.event.KeyEvent.VK_F14;
//            case java.awt.event.KeyEvent.VK_F15           : return com.jogamp.newt.event.KeyEvent.VK_F15;
//            case java.awt.event.KeyEvent.VK_F16           : return com.jogamp.newt.event.KeyEvent.VK_F16;
//            case java.awt.event.KeyEvent.VK_F17           : return com.jogamp.newt.event.KeyEvent.VK_F17;
//            case java.awt.event.KeyEvent.VK_F18           : return com.jogamp.newt.event.KeyEvent.VK_F18;
//            case java.awt.event.KeyEvent.VK_F19           : return com.jogamp.newt.event.KeyEvent.VK_F19;
//            case java.awt.event.KeyEvent.VK_F20           : return com.jogamp.newt.event.KeyEvent.VK_F20;
//            case java.awt.event.KeyEvent.VK_F21           : return com.jogamp.newt.event.KeyEvent.VK_F21;
//            case java.awt.event.KeyEvent.VK_F22           : return com.jogamp.newt.event.KeyEvent.VK_F22;
//            case java.awt.event.KeyEvent.VK_F23           : return com.jogamp.newt.event.KeyEvent.VK_F23;
//            case java.awt.event.KeyEvent.VK_F24           : return com.jogamp.newt.event.KeyEvent.VK_F24;
//            case java.awt.event.KeyEvent.VK_BRACELEFT     : return com.jogamp.newt.event.KeyEvent.VK_LEFT_BRACE;
//            case                         0x7c             : return com.jogamp.newt.event.KeyEvent.VK_PIPE;
//            case java.awt.event.KeyEvent.VK_BRACERIGHT    : return com.jogamp.newt.event.KeyEvent.VK_RIGHT_BRACE;
//            case java.awt.event.KeyEvent.VK_DEAD_TILDE    : return com.jogamp.newt.event.KeyEvent.VK_TILDE;
//            case java.awt.event.KeyEvent.VK_DELETE        : return com.jogamp.newt.event.KeyEvent.VK_DELETE;
//            case java.awt.event.KeyEvent.VK_NUMPAD0       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD0;
//            case java.awt.event.KeyEvent.VK_NUMPAD1       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD1;
//            case java.awt.event.KeyEvent.VK_NUMPAD2       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD2;
//            case java.awt.event.KeyEvent.VK_NUMPAD3       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD3;
//            case java.awt.event.KeyEvent.VK_NUMPAD4       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD4;
//            case java.awt.event.KeyEvent.VK_NUMPAD5       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD5;
//            case java.awt.event.KeyEvent.VK_NUMPAD6       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD6;
//            case java.awt.event.KeyEvent.VK_NUMPAD7       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD7;
//            case java.awt.event.KeyEvent.VK_NUMPAD8       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD8;
//            case java.awt.event.KeyEvent.VK_NUMPAD9       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD9;
//            case java.awt.event.KeyEvent.VK_DECIMAL       : return com.jogamp.newt.event.KeyEvent.VK_DECIMAL;
//            case java.awt.event.KeyEvent.VK_SEPARATOR     : return com.jogamp.newt.event.KeyEvent.VK_SEPARATOR;
//            case java.awt.event.KeyEvent.VK_ADD           : return com.jogamp.newt.event.KeyEvent.VK_ADD;
//            case java.awt.event.KeyEvent.VK_SUBTRACT      : return com.jogamp.newt.event.KeyEvent.VK_SUBTRACT;
//            case java.awt.event.KeyEvent.VK_MULTIPLY      : return com.jogamp.newt.event.KeyEvent.VK_MULTIPLY;
//            case java.awt.event.KeyEvent.VK_DIVIDE        : return com.jogamp.newt.event.KeyEvent.VK_DIVIDE;
//            case java.awt.event.KeyEvent.VK_NUM_LOCK      : return com.jogamp.newt.event.KeyEvent.VK_NUM_LOCK;
//            case java.awt.event.KeyEvent.VK_KP_LEFT       : /** Fall through intended .. */
//            case java.awt.event.KeyEvent.VK_LEFT          : return com.jogamp.newt.event.KeyEvent.VK_LEFT;
//            case java.awt.event.KeyEvent.VK_KP_UP         : /** Fall through intended .. */
//            case java.awt.event.KeyEvent.VK_UP            : return com.jogamp.newt.event.KeyEvent.VK_UP;
//            case java.awt.event.KeyEvent.VK_KP_RIGHT      : /** Fall through intended .. */
//            case java.awt.event.KeyEvent.VK_RIGHT         : return com.jogamp.newt.event.KeyEvent.VK_RIGHT;
//            case java.awt.event.KeyEvent.VK_KP_DOWN       : /** Fall through intended .. */
//            case java.awt.event.KeyEvent.VK_DOWN          : return com.jogamp.newt.event.KeyEvent.VK_DOWN;
//            case java.awt.event.KeyEvent.VK_CONTEXT_MENU  : return com.jogamp.newt.event.KeyEvent.VK_CONTEXT_MENU;
//            case java.awt.event.KeyEvent.VK_WINDOWS       : return com.jogamp.newt.event.KeyEvent.VK_WINDOWS;
//            case java.awt.event.KeyEvent.VK_META          : return com.jogamp.newt.event.KeyEvent.VK_META;
//            case java.awt.event.KeyEvent.VK_HELP          : return com.jogamp.newt.event.KeyEvent.VK_HELP;
//            case java.awt.event.KeyEvent.VK_COMPOSE       : return com.jogamp.newt.event.KeyEvent.VK_COMPOSE;
//            case java.awt.event.KeyEvent.VK_BEGIN         : return com.jogamp.newt.event.KeyEvent.VK_BEGIN;
//            case java.awt.event.KeyEvent.VK_STOP          : return com.jogamp.newt.event.KeyEvent.VK_STOP;
//            case java.awt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK: return com.jogamp.newt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK;
//            case java.awt.event.KeyEvent.VK_EURO_SIGN     : return com.jogamp.newt.event.KeyEvent.VK_EURO_SIGN;
//            case java.awt.event.KeyEvent.VK_CUT           : return com.jogamp.newt.event.KeyEvent.VK_CUT;
//            case java.awt.event.KeyEvent.VK_COPY          : return com.jogamp.newt.event.KeyEvent.VK_COPY;
//            case java.awt.event.KeyEvent.VK_PASTE         : return com.jogamp.newt.event.KeyEvent.VK_PASTE;
//            case java.awt.event.KeyEvent.VK_UNDO          : return com.jogamp.newt.event.KeyEvent.VK_UNDO;
//            case java.awt.event.KeyEvent.VK_AGAIN         : return com.jogamp.newt.event.KeyEvent.VK_AGAIN;
//            case java.awt.event.KeyEvent.VK_FIND          : return com.jogamp.newt.event.KeyEvent.VK_FIND;
//            case java.awt.event.KeyEvent.VK_PROPS         : return com.jogamp.newt.event.KeyEvent.VK_PROPS;
//            case java.awt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF: return com.jogamp.newt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF;
//            case java.awt.event.KeyEvent.VK_CODE_INPUT    : return com.jogamp.newt.event.KeyEvent.VK_CODE_INPUT;
//            case java.awt.event.KeyEvent.VK_ROMAN_CHARACTERS: return com.jogamp.newt.event.KeyEvent.VK_ROMAN_CHARACTERS;
//            case java.awt.event.KeyEvent.VK_ALL_CANDIDATES: return com.jogamp.newt.event.KeyEvent.VK_ALL_CANDIDATES;
//            case java.awt.event.KeyEvent.VK_PREVIOUS_CANDIDATE: return com.jogamp.newt.event.KeyEvent.VK_PREVIOUS_CANDIDATE;
//            case java.awt.event.KeyEvent.VK_ALPHANUMERIC  : return com.jogamp.newt.event.KeyEvent.VK_ALPHANUMERIC;
//            case java.awt.event.KeyEvent.VK_KATAKANA      : return com.jogamp.newt.event.KeyEvent.VK_KATAKANA;
//            case java.awt.event.KeyEvent.VK_HIRAGANA      : return com.jogamp.newt.event.KeyEvent.VK_HIRAGANA;
//            case java.awt.event.KeyEvent.VK_FULL_WIDTH    : return com.jogamp.newt.event.KeyEvent.VK_FULL_WIDTH;
//            case java.awt.event.KeyEvent.VK_HALF_WIDTH    : return com.jogamp.newt.event.KeyEvent.VK_HALF_WIDTH;
//            case java.awt.event.KeyEvent.VK_JAPANESE_KATAKANA: return com.jogamp.newt.event.KeyEvent.VK_JAPANESE_KATAKANA;
//            case java.awt.event.KeyEvent.VK_JAPANESE_HIRAGANA: return com.jogamp.newt.event.KeyEvent.VK_JAPANESE_HIRAGANA;
//            case java.awt.event.KeyEvent.VK_JAPANESE_ROMAN: return com.jogamp.newt.event.KeyEvent.VK_JAPANESE_ROMAN;
//            case java.awt.event.KeyEvent.VK_KANA_LOCK     : return com.jogamp.newt.event.KeyEvent.VK_KANA_LOCK;
//        }
//        return defNEWTKeyCode;
//    }

    /** from: https://github.com/JogAmp/jogl/blob/master/src/newt/classes/jogamp/newt/awt/event/AWTNewtEventFactory.java */
    public static int newtKeyCode2AWTKeyCode(short newtKeyCode) {
        var result = -1;
        var defAwtKeyCode = 0xFFFF & newtKeyCode;
        switch (newtKeyCode) {
            case com.jogamp.newt.event.KeyEvent.VK_HOME:
                result = java.awt.event.KeyEvent.VK_HOME;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_END:
                result = java.awt.event.KeyEvent.VK_END;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_FINAL:
                result = java.awt.event.KeyEvent.VK_FINAL;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PRINTSCREEN:
                result = java.awt.event.KeyEvent.VK_PRINTSCREEN;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE:
                result = java.awt.event.KeyEvent.VK_BACK_SPACE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_TAB:
                result = java.awt.event.KeyEvent.VK_TAB;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_ENTER:
                result = java.awt.event.KeyEvent.VK_ENTER;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN:
                result = java.awt.event.KeyEvent.VK_PAGE_DOWN;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CLEAR:
                result = java.awt.event.KeyEvent.VK_CLEAR;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_SHIFT:
                result = java.awt.event.KeyEvent.VK_SHIFT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PAGE_UP:
                result = java.awt.event.KeyEvent.VK_PAGE_UP;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CONTROL:
                result = java.awt.event.KeyEvent.VK_CONTROL;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_ALT:
                result = java.awt.event.KeyEvent.VK_ALT;
                break;
            // FIXME: On X11 it results to 0xff7e w/ AWTRobot, which is wrong. 0xffea Alt_R is expected AFAIK.
            case com.jogamp.newt.event.KeyEvent.VK_ALT_GRAPH:
                result = java.awt.event.KeyEvent.VK_ALT_GRAPH;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CAPS_LOCK:
                result = java.awt.event.KeyEvent.VK_CAPS_LOCK;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PAUSE:
                result = java.awt.event.KeyEvent.VK_PAUSE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_SCROLL_LOCK:
                result = java.awt.event.KeyEvent.VK_SCROLL_LOCK;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CANCEL:
                result = java.awt.event.KeyEvent.VK_CANCEL;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_INSERT:
                result = java.awt.event.KeyEvent.VK_INSERT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_ESCAPE:
                result = java.awt.event.KeyEvent.VK_ESCAPE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CONVERT:
                result = java.awt.event.KeyEvent.VK_CONVERT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NONCONVERT:
                result = java.awt.event.KeyEvent.VK_NONCONVERT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_ACCEPT:
                result = java.awt.event.KeyEvent.VK_ACCEPT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_MODECHANGE:
                result = java.awt.event.KeyEvent.VK_MODECHANGE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_SPACE:
                result = java.awt.event.KeyEvent.VK_SPACE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_EXCLAMATION_MARK:
                result = java.awt.event.KeyEvent.VK_EXCLAMATION_MARK;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_QUOTEDBL:
                result = java.awt.event.KeyEvent.VK_QUOTEDBL;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMBER_SIGN:
                result = java.awt.event.KeyEvent.VK_NUMBER_SIGN;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_DOLLAR:
                result = java.awt.event.KeyEvent.VK_DOLLAR;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PERCENT:
            case com.jogamp.newt.event.KeyEvent.VK_PIPE:
            case com.jogamp.newt.event.KeyEvent.VK_QUESTIONMARK:
                result = defAwtKeyCode;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_AMPERSAND:
                result = java.awt.event.KeyEvent.VK_AMPERSAND;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_QUOTE:
                result = java.awt.event.KeyEvent.VK_QUOTE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_LEFT_PARENTHESIS:
                result = java.awt.event.KeyEvent.VK_LEFT_PARENTHESIS;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_RIGHT_PARENTHESIS:
                result = java.awt.event.KeyEvent.VK_RIGHT_PARENTHESIS;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_ASTERISK:
                result = java.awt.event.KeyEvent.VK_ASTERISK;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PLUS:
                result = java.awt.event.KeyEvent.VK_PLUS;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_COMMA:
                result = java.awt.event.KeyEvent.VK_COMMA;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_MINUS:
                result = java.awt.event.KeyEvent.VK_MINUS;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PERIOD:
                result = java.awt.event.KeyEvent.VK_PERIOD;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_SLASH:
                result = java.awt.event.KeyEvent.VK_SLASH;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_0:
                result = java.awt.event.KeyEvent.VK_0;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_1:
                result = java.awt.event.KeyEvent.VK_1;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_2:
                result = java.awt.event.KeyEvent.VK_2;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_3:
                result = java.awt.event.KeyEvent.VK_3;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_4:
                result = java.awt.event.KeyEvent.VK_4;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_5:
                result = java.awt.event.KeyEvent.VK_5;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_6:
                result = java.awt.event.KeyEvent.VK_6;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_7:
                result = java.awt.event.KeyEvent.VK_7;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_8:
                result = java.awt.event.KeyEvent.VK_8;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_9:
                result = java.awt.event.KeyEvent.VK_9;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_COLON:
                result = java.awt.event.KeyEvent.VK_COLON;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_SEMICOLON:
                result = java.awt.event.KeyEvent.VK_SEMICOLON;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_LESS:
                result = java.awt.event.KeyEvent.VK_LESS;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_EQUALS:
                result = java.awt.event.KeyEvent.VK_EQUALS;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_GREATER:
                result = java.awt.event.KeyEvent.VK_GREATER;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_AT:
                result = java.awt.event.KeyEvent.VK_AT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_A:
                result = java.awt.event.KeyEvent.VK_A;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_B:
                result = java.awt.event.KeyEvent.VK_B;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_C:
                result = java.awt.event.KeyEvent.VK_C;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_D:
                result = java.awt.event.KeyEvent.VK_D;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_E:
                result = java.awt.event.KeyEvent.VK_E;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F:
                result = java.awt.event.KeyEvent.VK_F;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_G:
                result = java.awt.event.KeyEvent.VK_G;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_H:
                result = java.awt.event.KeyEvent.VK_H;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_I:
                result = java.awt.event.KeyEvent.VK_I;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_J:
                result = java.awt.event.KeyEvent.VK_J;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_K:
                result = java.awt.event.KeyEvent.VK_K;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_L:
                result = java.awt.event.KeyEvent.VK_L;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_M:
                result = java.awt.event.KeyEvent.VK_M;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_N:
                result = java.awt.event.KeyEvent.VK_N;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_O:
                result = java.awt.event.KeyEvent.VK_O;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_P:
                result = java.awt.event.KeyEvent.VK_P;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_Q:
                result = java.awt.event.KeyEvent.VK_Q;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_R:
                result = java.awt.event.KeyEvent.VK_R;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_S:
                result = java.awt.event.KeyEvent.VK_S;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_T:
                result = java.awt.event.KeyEvent.VK_T;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_U:
                result = java.awt.event.KeyEvent.VK_U;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_V:
                result = java.awt.event.KeyEvent.VK_V;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_W:
                result = java.awt.event.KeyEvent.VK_W;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_X:
                result = java.awt.event.KeyEvent.VK_X;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_Y:
                result = java.awt.event.KeyEvent.VK_Y;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_Z:
                result = java.awt.event.KeyEvent.VK_Z;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_OPEN_BRACKET:
                result = java.awt.event.KeyEvent.VK_OPEN_BRACKET;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_BACK_SLASH:
                result = java.awt.event.KeyEvent.VK_BACK_SLASH;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CLOSE_BRACKET:
                result = java.awt.event.KeyEvent.VK_CLOSE_BRACKET;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CIRCUMFLEX:
                result = java.awt.event.KeyEvent.VK_CIRCUMFLEX;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_UNDERSCORE:
                result = java.awt.event.KeyEvent.VK_UNDERSCORE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_BACK_QUOTE:
                result = java.awt.event.KeyEvent.VK_BACK_QUOTE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F1:
                result = java.awt.event.KeyEvent.VK_F1;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F2:
                result = java.awt.event.KeyEvent.VK_F2;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F3:
                result = java.awt.event.KeyEvent.VK_F3;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F4:
                result = java.awt.event.KeyEvent.VK_F4;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F5:
                result = java.awt.event.KeyEvent.VK_F5;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F6:
                result = java.awt.event.KeyEvent.VK_F6;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F7:
                result = java.awt.event.KeyEvent.VK_F7;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F8:
                result = java.awt.event.KeyEvent.VK_F8;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F9:
                result = java.awt.event.KeyEvent.VK_F9;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F10:
                result = java.awt.event.KeyEvent.VK_F10;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F11:
                result = java.awt.event.KeyEvent.VK_F11;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F12:
                result = java.awt.event.KeyEvent.VK_F12;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F13:
                result = java.awt.event.KeyEvent.VK_F13;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F14:
                result = java.awt.event.KeyEvent.VK_F14;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F15:
                result = java.awt.event.KeyEvent.VK_F15;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F16:
                result = java.awt.event.KeyEvent.VK_F16;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F17:
                result = java.awt.event.KeyEvent.VK_F17;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F18:
                result = java.awt.event.KeyEvent.VK_F18;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F19:
                result = java.awt.event.KeyEvent.VK_F19;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F20:
                result = java.awt.event.KeyEvent.VK_F20;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F21:
                result = java.awt.event.KeyEvent.VK_F21;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F22:
                result = java.awt.event.KeyEvent.VK_F22;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F23:
                result = java.awt.event.KeyEvent.VK_F23;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_F24:
                result = java.awt.event.KeyEvent.VK_F24;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_LEFT_BRACE:
                result = java.awt.event.KeyEvent.VK_BRACELEFT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_RIGHT_BRACE:
                result = java.awt.event.KeyEvent.VK_BRACERIGHT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_TILDE:
                result = java.awt.event.KeyEvent.VK_DEAD_TILDE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_DELETE:
                result = java.awt.event.KeyEvent.VK_DELETE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD0:
                result = java.awt.event.KeyEvent.VK_NUMPAD0;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD1:
                result = java.awt.event.KeyEvent.VK_NUMPAD1;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD2:
                result = java.awt.event.KeyEvent.VK_NUMPAD2;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD3:
                result = java.awt.event.KeyEvent.VK_NUMPAD3;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD4:
                result = java.awt.event.KeyEvent.VK_NUMPAD4;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD5:
                result = java.awt.event.KeyEvent.VK_NUMPAD5;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD6:
                result = java.awt.event.KeyEvent.VK_NUMPAD6;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD7:
                result = java.awt.event.KeyEvent.VK_NUMPAD7;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD8:
                result = java.awt.event.KeyEvent.VK_NUMPAD8;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD9:
                result = java.awt.event.KeyEvent.VK_NUMPAD9;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_DECIMAL:
                result = java.awt.event.KeyEvent.VK_DECIMAL;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_SEPARATOR:
                result = java.awt.event.KeyEvent.VK_SEPARATOR;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_ADD:
                result = java.awt.event.KeyEvent.VK_ADD;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_SUBTRACT:
                result = java.awt.event.KeyEvent.VK_SUBTRACT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_MULTIPLY:
                result = java.awt.event.KeyEvent.VK_MULTIPLY;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_DIVIDE:
                result = java.awt.event.KeyEvent.VK_DIVIDE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_NUM_LOCK:
                result = java.awt.event.KeyEvent.VK_NUM_LOCK;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_LEFT:
                result = java.awt.event.KeyEvent.VK_LEFT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_UP:
                result = java.awt.event.KeyEvent.VK_UP;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_RIGHT:
                result = java.awt.event.KeyEvent.VK_RIGHT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_DOWN:
                result = java.awt.event.KeyEvent.VK_DOWN;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CONTEXT_MENU:
                result = java.awt.event.KeyEvent.VK_CONTEXT_MENU;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_WINDOWS:
                result = java.awt.event.KeyEvent.VK_WINDOWS;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_META:
                result = java.awt.event.KeyEvent.VK_META;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_HELP:
                result = java.awt.event.KeyEvent.VK_HELP;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_COMPOSE:
                result = java.awt.event.KeyEvent.VK_COMPOSE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_BEGIN:
                result = java.awt.event.KeyEvent.VK_BEGIN;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_STOP:
                result = java.awt.event.KeyEvent.VK_STOP;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK:
                result = java.awt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_EURO_SIGN:
                result = java.awt.event.KeyEvent.VK_EURO_SIGN;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CUT:
                result = java.awt.event.KeyEvent.VK_CUT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_COPY:
                result = java.awt.event.KeyEvent.VK_COPY;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PASTE:
                result = java.awt.event.KeyEvent.VK_PASTE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_UNDO:
                result = java.awt.event.KeyEvent.VK_UNDO;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_AGAIN:
                result = java.awt.event.KeyEvent.VK_AGAIN;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_FIND:
                result = java.awt.event.KeyEvent.VK_FIND;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PROPS:
                result = java.awt.event.KeyEvent.VK_PROPS;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF:
                result = java.awt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_CODE_INPUT:
                result = java.awt.event.KeyEvent.VK_CODE_INPUT;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_ROMAN_CHARACTERS:
                result = java.awt.event.KeyEvent.VK_ROMAN_CHARACTERS;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_ALL_CANDIDATES:
                result = java.awt.event.KeyEvent.VK_ALL_CANDIDATES;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_PREVIOUS_CANDIDATE:
                result = java.awt.event.KeyEvent.VK_PREVIOUS_CANDIDATE;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_ALPHANUMERIC:
                result = java.awt.event.KeyEvent.VK_ALPHANUMERIC;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_KATAKANA:
                result = java.awt.event.KeyEvent.VK_KATAKANA;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_HIRAGANA:
                result = java.awt.event.KeyEvent.VK_HIRAGANA;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_FULL_WIDTH:
                result = java.awt.event.KeyEvent.VK_FULL_WIDTH;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_HALF_WIDTH:
                result = java.awt.event.KeyEvent.VK_HALF_WIDTH;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_JAPANESE_KATAKANA:
                result = java.awt.event.KeyEvent.VK_JAPANESE_KATAKANA;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_JAPANESE_HIRAGANA:
                result = java.awt.event.KeyEvent.VK_JAPANESE_HIRAGANA;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_JAPANESE_ROMAN:
                result = java.awt.event.KeyEvent.VK_JAPANESE_ROMAN;
                break;
            case com.jogamp.newt.event.KeyEvent.VK_KANA_LOCK:
                result = java.awt.event.KeyEvent.VK_KANA_LOCK;
                break;
        }
        if (result == -1) {
            result = defAwtKeyCode;
        }
        return result;
    }

}
