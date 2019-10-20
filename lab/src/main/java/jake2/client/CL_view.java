/*
 * CL_view.java Copyright (C) 2004
 * 
 * $Id: CL_view.java,v 1.5 2008-03-02 14:56:22 cawe Exp $
 */
/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *  
 */
package jake2.client;

import jake2.Defines;
import jake2.Globals;
import jake2.qcommon.CM;
import jake2.qcommon.Com;
import jake2.sys.Sys;

import java.util.StringTokenizer;

public class CL_view {

    static int num_cl_weaponmodels;

    static final String[] cl_weaponmodels = new String[Defines.MAX_CLIENTWEAPONMODELS];

    /*
     * =================
     * 
     * CL_PrepRefresh
     * 
     * Call before entering a new level, or after changing dlls
     * =================
     */
    static void PrepRefresh() {
        int i;

        if ((i = Globals.cl.configstrings[Defines.CS_MODELS + 1].length()) == 0)
            return; 

        SCR.AddDirtyPoint(0, 0);
        SCR.AddDirtyPoint(Globals.viddef.getWidth() - 1, Globals.viddef.getHeight() - 1);


        String mapname = Globals.cl.configstrings[Defines.CS_MODELS + 1].substring(5,
                i - 4); 
        

        
        Com.Printf("Map: " + mapname + '\r');
        SCR.UpdateScreen();
        Globals.re.BeginRegistration(mapname);
        Com.Printf("                                     \r");

        
        Com.Printf("pics\r");
        SCR.UpdateScreen();
        SCR.TouchPics();
        Com.Printf("                                     \r");

        CL_tent.RegisterTEntModels();

        num_cl_weaponmodels = 1;
        cl_weaponmodels[0] = "weapon.md2";

        for (i = 1; i < Defines.MAX_MODELS
                && Globals.cl.configstrings[Defines.CS_MODELS + i].length() != 0; i++) {
            String name = Globals.cl.configstrings[Defines.CS_MODELS + i];
            if (name.length() > 37)
                name = name.substring(0, 36);

            if ((int) name.charAt(0) != (int) '*')
                Com.Printf(name + '\r');

            SCR.UpdateScreen();
            Sys.SendKeyEvents(); 
            if ((int) name.charAt(0) == (int) '#') {
                
                if (num_cl_weaponmodels < Defines.MAX_CLIENTWEAPONMODELS) {
                    cl_weaponmodels[num_cl_weaponmodels] = Globals.cl.configstrings[Defines.CS_MODELS
                            + i].substring(1);
                    num_cl_weaponmodels++;
                }
            } else {
                Globals.cl.model_draw[i] = Globals.re
                        .RegisterModel(Globals.cl.configstrings[Defines.CS_MODELS
                                + i]);
                if ((int) name.charAt(0) == (int) '*')
                    Globals.cl.model_clip[i] = CM
                            .InlineModel(Globals.cl.configstrings[Defines.CS_MODELS
                                    + i]);
                else
                    Globals.cl.model_clip[i] = null;
            }
            if ((int) name.charAt(0) != (int) '*')
                Com.Printf("                                     \r");
        }

        Com.Printf("images\r");
        SCR.UpdateScreen();
        for (i = 1; i < Defines.MAX_IMAGES
                && Globals.cl.configstrings[Defines.CS_IMAGES + i].length() > 0; i++) {
            Globals.cl.image_precache[i] = Globals.re
                    .RegisterPic(Globals.cl.configstrings[Defines.CS_IMAGES + i]);
            Sys.SendKeyEvents(); 
        }

        Com.Printf("                                     \r");
        for (i = 0; i < Defines.MAX_CLIENTS; i++) {
            if (Globals.cl.configstrings[Defines.CS_PLAYERSKINS + i].length() == 0)
                continue;
            Com.Printf("client " + i + '\r');
            SCR.UpdateScreen();
            Sys.SendKeyEvents(); 
            CL_parse.ParseClientinfo(i);
            Com.Printf("                                     \r");
        }

        CL_parse.LoadClientinfo(Globals.cl.baseclientinfo,
                "unnamed\\male/grunt");

        
        Com.Printf("sky\r");
        SCR.UpdateScreen();
        float rotate = Float
                .parseFloat(Globals.cl.configstrings[Defines.CS_SKYROTATE]);
        StringTokenizer st = new StringTokenizer(
                Globals.cl.configstrings[Defines.CS_SKYAXIS]);
        float[] axis = new float[3];
        axis[0] = Float.parseFloat(st.nextToken());
        axis[1] = Float.parseFloat(st.nextToken());
        axis[2] = Float.parseFloat(st.nextToken());
        Globals.re.SetSky(Globals.cl.configstrings[Defines.CS_SKY], rotate,
                axis);
        Com.Printf("                                     \r");

        
        Globals.re.EndRegistration();

        
        Console.ClearNotify();

        SCR.UpdateScreen();
        Globals.cl.refresh_prepped = true;
        Globals.cl.force_refdef = true; 
    }

    public static void AddNetgraph() {


        if (SCR.scr_debuggraph.value == 0.0f || SCR.scr_timegraph.value == 0.0f)
            return;

        int i;
        for (i = 0; i < Globals.cls.netchan.dropped; i++)
            SCR.DebugGraph(30.0F, 0x40);

        for (i = 0; i < Globals.cl.surpressCount; i++)
            SCR.DebugGraph(30.0F, 0xdf);


        int in = Globals.cls.netchan.incoming_acknowledged
                & (Defines.CMD_BACKUP - 1);
        int ping = Globals.cls.realtime - Globals.cl.cmd_time[in];
        ping /= 30;
        if (ping > 30)
            ping = 30;
        SCR.DebugGraph((float) ping, 0xd0);
    }
}