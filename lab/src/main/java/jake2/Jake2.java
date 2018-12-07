/*
 * Jake2.java
 * Copyright (C)  2003
 * 
 * $Id: Jake2.java,v 1.9 2005-12-03 19:43:15 salomo Exp $
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
package jake2;

import com.jogamp.opengl.GL;
import jake2.qcommon.Com;
import jake2.qcommon.Cvar;
import jake2.qcommon.Q2DataTool;
import jake2.qcommon.Qcommon;
import jake2.render.JoglGL2Renderer;
import jake2.sys.Timer;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Consumer;

import static jake2.Globals.re;


/**
 * Jake2 is the main class of Quake2 for Java.
 */
public final class Jake2 {

    
    public static Q2DataTool q2DataTool;

    /* pp */ static final void initQ2DataTool() {
        q2DataTool = new Q2DataTool();
        
        Locale.setDefault(Locale.US);
        
    }
    
    /**
     * main is used to start the game. Quake2 for Java supports the following
     * command line arguments:
     * 
     * @param args
     */
    public static void main(String[] args) {

        run(args, null);
    }

    public static void run(String[] args, @Nullable Consumer<GL> onFrame) {
        boolean dedicated = false;

        
        for (int n = 0; n <  args.length; n++)
        {
            if (args[n].equals("+set"))
            {
                if (n++ >= args.length)
                    break;

                if (!args[n].equals("dedicated"))
                    continue;

                if (n++ >= args.length)
                    break;

                if (args[n].equals("1") || args[n].equals("\"1\""))
                {
                    Com.Printf("Starting in dedicated mode.\n");
                    dedicated = true;
                }
            }
        }

        

        Globals.dedicated= Cvar.Get("dedicated", "0", Qcommon.CVAR_NOSET);

        if (dedicated)
            Globals.dedicated.value = 1.0f;


        
        if (Globals.dedicated.value != 1.0f) {
            Jake2.initQ2DataTool();
        }

        
        int argc = (args == null) ? 1 : args.length + 1;
        String[] c_args = new String[argc];
        c_args[0] = "Jake2";
        if (argc > 1) {
            System.arraycopy(args, 0, c_args, 1, argc - 1);
        }
        Qcommon.Init(c_args);

        Globals.nostdout = Cvar.Get("nostdout", "0", 0);


        int oldtime = Timer.Milliseconds();
        int newtime;
        int time;
        while (true) {
            
            newtime = Timer.Milliseconds();
            time = newtime - oldtime;

            if (time > 0)
                Qcommon.Frame(time);

            if (onFrame!=null)
                onFrame.accept(((JoglGL2Renderer) re).gl);

            oldtime = newtime;
        }
    }
}