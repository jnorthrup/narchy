/*
 * LayoutParser.java
 * Copyright (C) 2003
 */
/*
 Copyright (C) 1997-2001 Id Software, Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 */
package jake2.client;

import jake2.Defines;
import jake2.qcommon.Com;

final class LayoutParser {
    int tokenPos;

    int tokenLength;

    int index;

    int length;

    String data;

    LayoutParser() {
        init(null);
    }

    public void init(String layout) {
        tokenPos = 0;
        tokenLength = 0;
        index = 0;
        data = (layout != null) ? layout : "";
        length = (layout != null) ? layout.length() : 0;
    }

    public boolean hasNext() {
        return !isEof();
    }

    public void next() {
        if (data == null) {
            tokenLength = 0;
            return;
        }

        while (true) {
            
            skipwhites();
            if (isEof()) {
                tokenLength = 0;
                return;
            }

            
            if (getchar() == '/') {
                if (nextchar() == '/') {
                    skiptoeol();
                    
                    continue;
                } else {
                    prevchar();
                    break;
                }
            } else
                break;
        }

        int c;
        int len = 0;
        
        if (getchar() == '\"') {
            nextchar();
            tokenPos = index;
            while (true) {
                c = getchar();
                nextchar();
                if (c == '\"' || c == 0) {
                    tokenLength = len;
                    return;
                }
                if (len < Defines.MAX_TOKEN_CHARS) {
                    ++len;
                }
            }
        }

        
        c = getchar();
        tokenPos = index;
        do {
            if (len < Defines.MAX_TOKEN_CHARS) {
                ++len;
            }
            c = nextchar();
        } while (c > 32);

        if (len == Defines.MAX_TOKEN_CHARS) {
            Com.Printf("Token exceeded " + Defines.MAX_TOKEN_CHARS
                    + " chars, discarded.\n");
            len = 0;
        }

        tokenLength = len;
    }

    public boolean tokenEquals(String other) {
        int len = this.tokenLength;
        return len == other.length() && data.regionMatches(tokenPos, other, 0, len);
    }

    public int tokenAsInt() {
        return tokenLength == 0 ? 0 : atoi();
    }

    public String token() {
        if (tokenLength == 0)
            return "";
        return data.substring(tokenPos, tokenPos + tokenLength);
    }

    private int atoi() {
        int result = 0;
        boolean negative = false;
        int i = 0, max = tokenLength;
        String s = data;

        if (max > 0) {
            int p = this.tokenPos;
            int limit;
            if (s.charAt(p) == '-') {
                negative = true;
                limit = Integer.MIN_VALUE;
                i++;
            } else {
                limit = -Integer.MAX_VALUE;
            }
            int multmin = limit / 10;
            int digit;
            if (i < max) {
                digit = Character.digit(s.charAt(p + i++), 10);
                if (digit < 0) {
                    return 0; 
                } else {
                    result = -digit;
                }
            }
            while (i < max) {
                
                digit = Character.digit(s.charAt(p + i++), 10);
                if (digit < 0) {
                    return 0; 
                }
                if (result < multmin) {
                    return 0; 
                }
                result *= 10;
                if (result < limit + digit) {
                    return 0; 
                }
                result -= digit;
            }
        } else {
            return 0; 
        }
        if (negative) {
            if (i > 1) {
                return result;
            } else { /* Only got "-" */
                return 0; 
            }
        } else {
            return -result;
        }
    }

    private char getchar() {
        return index < length ? data.charAt(index) : 0;
    }

    private char nextchar() {
        int i = ++index;
        return i < length ? data.charAt(i) : 0;
    }

    private char prevchar() {
        if (index > 0) {
            --index;
            return data.charAt(index);
        }
        return 0;
    }

    private boolean isEof() {
        return index >= length;
    }

    private char skipwhites() {
        char c = 0;
        while (index < length && ((c = data.charAt(index)) <= ' ') && c != 0)
            ++index;
        return c;
    }

    private char skiptoeol() {
        char c = 0;
        while (index < length && (c = data.charAt(index)) != '\n' && c != 0)
            ++index;
        return c;
    }
}
