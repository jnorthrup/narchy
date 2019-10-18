/*
 * Cbuf.java
 * Copyright (C) 2003
 * 
 * $Id: Cbuf.java,v 1.8 2005-12-18 22:10:09 cawe Exp $
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
package jake2.qcommon;

import jake2.Defines;
import jake2.Globals;
import jake2.game.Cmd;
import jake2.util.Lib;

/**
 * Cbuf
 */
public final class Cbuf {

    private static final byte[] line = new byte[1024];
    private static final byte[] tmp = new byte[8192];

    /**
     *  
     */
    public static void Init() {
        SZ.Init(Globals.cmd_text, Globals.cmd_text_buf,
                Globals.cmd_text_buf.length);
    }

    public static void InsertText(String text) {


        int templen = Globals.cmd_text.cursize;
        if (templen != 0) {
            System.arraycopy(Globals.cmd_text.data, 0, tmp, 0, templen);
            SZ.Clear(Globals.cmd_text);
        }

        
        Cbuf.AddText(text);

        
        if (templen != 0) {
            SZ.Write(Globals.cmd_text, tmp, templen);
        }
    }

    /**
     * @param clear
     */
    static void AddEarlyCommands(boolean clear) {

        for (int i = 0; i < Com.Argc(); i++) {
            String s = Com.Argv(i);
            if (!"+setAt".equals(s))
                continue;
            Cbuf.AddText("setAt " + Com.Argv(i + 1) + ' ' + Com.Argv(i + 2)
                    + '\n');
            if (clear) {
                Com.ClearArgv(i);
                Com.ClearArgv(i + 1);
                Com.ClearArgv(i + 2);
            }
            i += 2;
        }
    }

    /**
     * @return
     */
    static boolean AddLateCommands() {
        int i;


        int s = 0;
        int argc = Com.Argc();
        for (i = 1; i < argc; i++) {
            s += Com.Argv(i).length();
        }
        if (s == 0)
            return false;

        String text = "";
        for (i = 1; i < argc; i++) {
            text += Com.Argv(i);
            if (i != argc - 1)
                text += " ";
        }

        
        String build = "";
        for (i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '+') {
                i++;

                int j;
                for (j = i; j < text.length() && (text.charAt(j) != '+') && (text.charAt(j) != '-'); j++);

                build += text.substring(i, j);
                build += "\n";

                i = j - 1;
            }
        }

        boolean ret = (build.length() != 0);
        if (ret)
            Cbuf.AddText(build);




        return ret;
    }

    /**
     * @param text
     */
    public static void AddText(String text) {
        int l = text.length();

        if (Globals.cmd_text.cursize + l >= Globals.cmd_text.maxsize) {
            Com.Printf("Cbuf_AddText: overflow\n");
            return;
        }
        SZ.Write(Globals.cmd_text, Lib.stringToBytes(text), l);
    }

    /**
     *  
     */
    public static void Execute() {

        Globals.alias_count = 0;

        byte[] text = null;
        while (Globals.cmd_text.cursize != 0) {
            
            text = Globals.cmd_text.data;

            int quotes = 0;
            int i;

            for (i = 0; i < Globals.cmd_text.cursize; i++) {
                if (text[i] == '"')
                    quotes++;
                if (quotes % 2 == 0 && text[i] == ';')
                    break; 
                if (text[i] == '\n')
                    break;
            }

            System.arraycopy(text, 0, line, 0, i);
            line[i] = 0;

            
            
            
            
            

            if (i == Globals.cmd_text.cursize)
                Globals.cmd_text.cursize = 0;
            else {
                i++;
                Globals.cmd_text.cursize -= i;
                

                System.arraycopy(text, i, tmp, 0, Globals.cmd_text.cursize);
                System.arraycopy(tmp, 0, text, 0, Globals.cmd_text.cursize);
                text[Globals.cmd_text.cursize] = '\0';

            }

            
            int len = Lib.strlen(line);

            String cmd = new String(line, 0, len);
            Cmd.ExecuteString(cmd);

            if (Globals.cmd_wait) {
                
                
                Globals.cmd_wait = false;
                break;
            }
        }
    }

    public static void ExecuteText(int exec_when, String text) {
        switch (exec_when) {
        case Defines.EXEC_NOW:
            Cmd.ExecuteString(text);
            break;
        case Defines.EXEC_INSERT:
            Cbuf.InsertText(text);
            break;
        case Defines.EXEC_APPEND:
            Cbuf.AddText(text);
            break;
        default:
            Com.Error(Defines.ERR_FATAL, "Cbuf_ExecuteText: bad exec_when");
        }
    }

    /*
     * ============ Cbuf_CopyToDefer ============
     */
    public static void CopyToDefer() {
        System.arraycopy(Globals.cmd_text_buf, 0, Globals.defer_text_buf, 0,
                Globals.cmd_text.cursize);
        Globals.defer_text_buf[Globals.cmd_text.cursize] = 0;
        Globals.cmd_text.cursize = 0;
    }

    /*
     * ============ Cbuf_InsertFromDefer ============
     */
    public static void InsertFromDefer() {
        InsertText(new String(Globals.defer_text_buf).trim());
        Globals.defer_text_buf[0] = 0;
    }

}