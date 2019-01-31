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



package jake2.server;

import jake2.Defines;
import jake2.Globals;
import jake2.client.CL;
import jake2.client.SCR;
import jake2.game.*;
import jake2.qcommon.*;
import jake2.sys.NET;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.io.IOException;
import java.io.RandomAccessFile;

public class SV_INIT {

    /**
     * SV_FindIndex.
     */
    public static int SV_FindIndex(String name, int start, int max,
            boolean create) {
        int i;

        if (name == null || name.length() == 0)
            return 0;

        for (i = 1; i < max && sv.configstrings[start + i] != null; i++)
            if (0 == Lib.strcmp(sv.configstrings[start + i], name))
                return i;

        if (!create)
            return 0;

        if (i == max)
            Com.Error(Defines.ERR_DROP, "*Index: overflow");

        sv.configstrings[start + i] = name;

        if (sv.state != Defines.ss_loading) { 
            
            SZ.Clear(sv.multicast);
            MSG.WriteChar(sv.multicast, Defines.svc_configstring);
            MSG.WriteShort(sv.multicast, start + i);
            MSG.WriteString(sv.multicast, name);
            SV_SEND.SV_Multicast(Globals.vec3_origin, Defines.MULTICAST_ALL_R);
        }

        return i;
    }

    public static int SV_ModelIndex(String name) {
        return SV_FindIndex(name, Defines.CS_MODELS, Defines.MAX_MODELS, true);
    }

    public static int SV_SoundIndex(String name) {
        return SV_FindIndex(name, Defines.CS_SOUNDS, Defines.MAX_SOUNDS, true);
    }

    public static int SV_ImageIndex(String name) {
        return SV_FindIndex(name, Defines.CS_IMAGES, Defines.MAX_IMAGES, true);
    }

    /**
     * SV_CreateBaseline
     * 
     * Entity baselines are used to compress the update messages to the clients --
     * only the fields that differ from the baseline will be transmitted.
     */
    public static void SV_CreateBaseline() {
        edict_t svent;
        int entnum;

        for (entnum = 1; entnum < GameBase.num_edicts; entnum++) {
            svent = GameBase.g_edicts[entnum];

            if (!svent.inuse)
                continue;
            if (0 == svent.s.modelindex && 0 == svent.s.sound
                    && 0 == svent.s.effects)
                continue;
            
            svent.s.number = entnum;

            
            Math3D.VectorCopy(svent.s.origin, svent.s.old_origin);
            sv.baselines[entnum].set(svent.s);
        }
    }

    /** 
     * SV_CheckForSavegame.
     */
    public static void SV_CheckForSavegame() {

        String name;
        RandomAccessFile f;

        int i;

        if (SV_MAIN.sv_noreload.value != 0)
            return;

        if (Cvar.VariableValue("deathmatch") != 0)
            return;

        name = FS.Gamedir() + "/save/current/" + sv.name + ".sav";
        try {
            f = new RandomAccessFile(name, "r");
        }

        catch (Exception e) {
            return;
        }

        try {
            f.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        SV_WORLD.SV_ClearWorld();

        
        SV_CCMDS.SV_ReadLevelFile();

        if (!sv.loadgame) { 
            
            

            
            
            
            int previousState; 

            previousState = sv.state; 
            sv.state = Defines.ss_loading; 
            for (i = 0; i < 100; i++)
                GameBase.G_RunFrame();

            sv.state = previousState; 
        }
    }

    /**
     * SV_SpawnServer.
     * 
     * Change the server to a new map, taking all connected clients along with
     * it.
     */
    public static void SV_SpawnServer(String server, String spawnpoint,
            int serverstate, boolean attractloop, boolean loadgame) {
        int i;
        int checksum = 0;

        if (attractloop)
            Cvar.Set("paused", "0");

        Com.Printf("------- Server Initialization -------\n");

        Com.DPrintf("SpawnServer: " + server + '\n');
        if (sv.demofile != null)
            try {
                sv.demofile.close();
            } 
        	catch (Exception e) {
            }

        
        svs.spawncount++;        

        sv.state = Defines.ss_dead;

        Globals.server_state = sv.state;

        
        sv = new server_t();

        svs.realtime = 0;
        sv.loadgame = loadgame;
        sv.attractloop = attractloop;

        
        sv.configstrings[Defines.CS_NAME] = server;

        if (Cvar.VariableValue("deathmatch") != 0) {
            sv.configstrings[Defines.CS_AIRACCEL] = String.valueOf(SV_MAIN.sv_airaccelerate.value);
            PMove.pm_airaccelerate = SV_MAIN.sv_airaccelerate.value;
        } else {
            sv.configstrings[Defines.CS_AIRACCEL] = "0";
            PMove.pm_airaccelerate = 0;
        }

        SZ.Init(sv.multicast, sv.multicast_buf, sv.multicast_buf.length);

        sv.name = server;

        
        for (i = 0; i < SV_MAIN.maxclients.value; i++) {
            
            if (svs.clients[i].state > Defines.cs_connected)
                svs.clients[i].state = Defines.cs_connected;
            svs.clients[i].lastframe = -1;
        }

        sv.time = 1000;

        sv.name = server;
        sv.configstrings[Defines.CS_NAME] = server;

        int iw[] = { checksum };

        if (serverstate != Defines.ss_game) {
            sv.models[1] = CM.CM_LoadMap("", false, iw); 
        } else {
            sv.configstrings[Defines.CS_MODELS + 1] = "maps/" + server + ".bsp";
            sv.models[1] = CM.CM_LoadMap(
                    sv.configstrings[Defines.CS_MODELS + 1], false, iw);
        }
        checksum = iw[0];
        sv.configstrings[Defines.CS_MAPCHECKSUM] = String.valueOf(checksum);


        
        
        SV_WORLD.SV_ClearWorld();

        for (i = 1; i < CM.CM_NumInlineModels(); i++) {
            sv.configstrings[Defines.CS_MODELS + 1 + i] = "*" + i;
            
            
            sv.models[i + 1] = CM.InlineModel(sv.configstrings[Defines.CS_MODELS + 1 + i]);
        }

     
        

        
        

        sv.state = Defines.ss_loading;
        Globals.server_state = sv.state;

        
        GameSpawn.SpawnEntities(sv.name, CM.CM_EntityString(), spawnpoint);

        
        GameBase.G_RunFrame();
        GameBase.G_RunFrame();

        
        sv.state = serverstate;
        Globals.server_state = sv.state;

        
        SV_CreateBaseline();

        
        SV_CheckForSavegame();

        
        Cvar.FullSet("mapname", sv.name, Defines.CVAR_SERVERINFO
                | Defines.CVAR_NOSET);
    }

    /**
     * SV_InitGame.
     * 
     * A brand new game has been started.
     */
    public static void SV_InitGame() {
        int i;
        edict_t ent;
        
        String idmaster;

        if (svs.initialized) {
            
            SV_MAIN.SV_Shutdown("Server restarted\n", true);
        } else {
            
            CL.Drop();
            SCR.BeginLoadingPlaque();
        }

        
        Cvar.GetLatchedVars();

        svs.initialized = true;

        if (Cvar.VariableValue("coop") != 0
                && Cvar.VariableValue("deathmatch") != 0) {
            Com.Printf("Deathmatch and Coop both setAt, disabling Coop\n");
            Cvar.FullSet("coop", "0", Defines.CVAR_SERVERINFO
                    | Defines.CVAR_LATCH);
        }

        
        
        if (Globals.dedicated.value != 0) {
            if (0 == Cvar.VariableValue("coop"))
                Cvar.FullSet("deathmatch", "1", Defines.CVAR_SERVERINFO
                        | Defines.CVAR_LATCH);
        }

        
        if (Cvar.VariableValue("deathmatch") != 0) {
            if (SV_MAIN.maxclients.value <= 1)
                Cvar.FullSet("maxclients", "8", Defines.CVAR_SERVERINFO
                        | Defines.CVAR_LATCH);
            else if (SV_MAIN.maxclients.value > Defines.MAX_CLIENTS)
                Cvar.FullSet("maxclients", "" + Defines.MAX_CLIENTS,
                        Defines.CVAR_SERVERINFO | Defines.CVAR_LATCH);
        } else if (Cvar.VariableValue("coop") != 0) {
            if (SV_MAIN.maxclients.value <= 1 || SV_MAIN.maxclients.value > 4)
                Cvar.FullSet("maxclients", "4", Defines.CVAR_SERVERINFO
                        | Defines.CVAR_LATCH);

        } else 
        {
            Cvar.FullSet("maxclients", "1", Defines.CVAR_SERVERINFO
                    | Defines.CVAR_LATCH);
        }

        svs.spawncount = Lib.rand();        
        svs.clients = new client_t[(int) SV_MAIN.maxclients.value];
        for (int n = 0; n < svs.clients.length; n++) {
            svs.clients[n] = new client_t();
            svs.clients[n].serverindex = n;
        }
        svs.num_client_entities = ((int) SV_MAIN.maxclients.value)
                * Defines.UPDATE_BACKUP * 64; 

        svs.client_entities = new entity_state_t[svs.num_client_entities];
        for (int n = 0; n < svs.client_entities.length; n++)
            svs.client_entities[n] = new entity_state_t(null);

        
        NET.Config((SV_MAIN.maxclients.value > 1));

        
        svs.last_heartbeat = -99999; 
        idmaster = "192.246.40.37:" + Defines.PORT_MASTER;
        NET.StringToAdr(idmaster, SV_MAIN.master_adr[0]);

        
        SV_GAME.SV_InitGameProgs();

        for (i = 0; i < SV_MAIN.maxclients.value; i++) {
            ent = GameBase.g_edicts[i + 1];
            svs.clients[i].edict = ent;
            svs.clients[i].lastcmd = new usercmd_t();
        }
    }

    private static String firstmap = "";
    
    /**
     * SV_Map
     * 
     * the full syntax is:
     * 
     * map [*] <map>$ <startspot>+ <nextserver>
     * 
     * command from the console or progs. Map can also be a.cin, .pcx, or .dm2 file.
     * 
     * Nextserver is used to allow a cinematic to play, then proceed to
     * another level:
     * 
     * map tram.cin+jail_e3
     */
    public static void SV_Map(boolean attractloop, String levelstring, boolean loadgame) {

        int l;
        String level, ch, spawnpoint;

        sv.loadgame = loadgame;
        sv.attractloop = attractloop;

        if (sv.state == Defines.ss_dead && !sv.loadgame)
            SV_InitGame(); 

        level = levelstring; 

        

        int c = level.indexOf('+');
        if (c != -1) {
            Cvar.Set("nextserver", "gamemap \"" + level.substring(c + 1) + '"');
            level = level.substring(0, c);
        } else {
            Cvar.Set("nextserver", "");
        }
        
        
        if (firstmap.length() == 0)
        {        
        	if (!levelstring.endsWith(".cin") && !levelstring.endsWith(".pcx") && !levelstring.endsWith(".dm2"))
        	{
        		int pos = levelstring.indexOf('+');
        		firstmap = levelstring.substring(pos + 1);
        	}
        }

        
        if (Cvar.VariableValue("coop") != 0 && level.equals("victory.pcx"))
            Cvar.Set("nextserver", "gamemap \"*" + firstmap + '"');

        
        int pos = level.indexOf('$');
        if (pos != -1) {
            spawnpoint = level.substring(pos + 1);
            level = level.substring(0, pos);

        } else
            spawnpoint = "";

        
        if (level.charAt(0) == '*')
            level = level.substring(1);

        l = level.length();
        if (l > 4 && level.endsWith(".cin")) {
            SCR.BeginLoadingPlaque(); 
            SV_SEND.SV_BroadcastCommand("changing\n");
            SV_SpawnServer(level, spawnpoint, Defines.ss_cinematic,
                    attractloop, loadgame);
        } else if (l > 4 && level.endsWith(".dm2")) {
            SCR.BeginLoadingPlaque(); 
            SV_SEND.SV_BroadcastCommand("changing\n");
            SV_SpawnServer(level, spawnpoint, Defines.ss_demo, attractloop,
                    loadgame);
        } else if (l > 4 && level.endsWith(".pcx")) {
            SCR.BeginLoadingPlaque(); 
            SV_SEND.SV_BroadcastCommand("changing\n");
            SV_SpawnServer(level, spawnpoint, Defines.ss_pic, attractloop,
                    loadgame);
        } else {
            SCR.BeginLoadingPlaque(); 
            SV_SEND.SV_BroadcastCommand("changing\n");
            SV_SEND.SV_SendClientMessages();
            SV_SpawnServer(level, spawnpoint, Defines.ss_game, attractloop,
                    loadgame);
            Cbuf.CopyToDefer();
        }

        SV_SEND.SV_BroadcastCommand("reconnect\n");
    }

    public static server_static_t svs = new server_static_t(); 
                                                               

    public static server_t sv = new server_t(); 
}