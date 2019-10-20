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
import jake2.game.*;
import jake2.qcommon.*;
import jake2.util.Lib;

import java.io.IOException;

public class SV_USER {

    static edict_t sv_player;

    public static class ucmd_t {
        public ucmd_t(String n, Runnable r) {
            name = n;
            this.r = r;
        }

        final String name;

        final Runnable r;
    }

    static ucmd_t u1 = new ucmd_t("new", SV_USER::SV_New_f);

    static final ucmd_t[] ucmds = {
    
            new ucmd_t("new", SV_USER::SV_New_f), new ucmd_t("configstrings", SV_USER::SV_Configstrings_f), new ucmd_t("baselines", SV_USER::SV_Baselines_f), new ucmd_t("begin", SV_USER::SV_Begin_f), new ucmd_t("nextserver", SV_USER::SV_Nextserver_f), new ucmd_t("disconnect", SV_USER::SV_Disconnect_f),

            
            new ucmd_t("info", SV_USER::SV_ShowServerinfo_f), new ucmd_t("download", SV_USER::SV_BeginDownload_f), new ucmd_t("nextdl", SV_USER::SV_NextDownload_f) };

    public static final int MAX_STRINGCMDS = 8;

    /*
     * ============================================================
     * 
     * USER STRINGCMD EXECUTION
     * 
     * sv_client and sv_player will be valid.
     * ============================================================
     */

    /*
     * ================== SV_BeginDemoServer ==================
     */
    public static void SV_BeginDemoserver() {

        String name = "demos/" + SV_INIT.sv.name;
        try {
            SV_INIT.sv.demofile = FS.FOpenFile(name);
        } catch (IOException e) {
            Com.Error(Defines.ERR_DROP, "Couldn't open " + name + '\n');
        }
        if (SV_INIT.sv.demofile == null)
            Com.Error(Defines.ERR_DROP, "Couldn't open " + name + '\n');
    }

    /*
     * ================ SV_New_f
     * 
     * Sends the first message from the server to a connected client. This will
     * be sent on the initial connection and upon each server load.
     * ================
     */
    public static void SV_New_f() {

        Com.DPrintf("New() from " + SV_MAIN.sv_client.name + '\n');

        if (SV_MAIN.sv_client.state != Defines.cs_connected) {
            Com.Printf("New not valid -- already spawned\n");
            return;
        }

        
        if (SV_INIT.sv.state == Defines.ss_demo) {
            SV_BeginDemoserver();
            return;
        }


        String gamedir = Cvar.VariableString("gamedir");

        
        MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                        Defines.svc_serverdata);
        MSG.WriteInt(SV_MAIN.sv_client.netchan.message,
                Defines.PROTOCOL_VERSION);
        
        MSG.WriteLong(SV_MAIN.sv_client.netchan.message,
                        SV_INIT.svs.spawncount);
        MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                SV_INIT.sv.attractloop ? 1 : 0);
        MSG.WriteString(SV_MAIN.sv_client.netchan.message, gamedir);

        int playernum;
        if (SV_INIT.sv.state == Defines.ss_cinematic
                || SV_INIT.sv.state == Defines.ss_pic)
            playernum = -1;
        else
            
            playernum = SV_MAIN.sv_client.serverindex;

        MSG.WriteShort(SV_MAIN.sv_client.netchan.message, playernum);

        
        MSG.WriteString(SV_MAIN.sv_client.netchan.message,
                SV_INIT.sv.configstrings[Defines.CS_NAME]);

        
        
        
        if (SV_INIT.sv.state == Defines.ss_game) {

            edict_t ent = GameBase.g_edicts[playernum + 1];
            ent.s.number = playernum + 1;
            SV_MAIN.sv_client.edict = ent;
            SV_MAIN.sv_client.lastcmd = new usercmd_t();

            
            MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                    Defines.svc_stufftext);
            MSG.WriteString(SV_MAIN.sv_client.netchan.message,
                    "cmd configstrings " + SV_INIT.svs.spawncount + " 0\n");
        }
        
    }

    /*
     * ================== SV_Configstrings_f ==================
     */
    public static void SV_Configstrings_f() {

        Com.DPrintf("Configstrings() from " + SV_MAIN.sv_client.name + '\n');

        if (SV_MAIN.sv_client.state != Defines.cs_connected) {
            Com.Printf("configstrings not valid -- already spawned\n");
            return;
        }

        
        if (Lib.atoi(Cmd.Argv(1)) != SV_INIT.svs.spawncount) {
            Com.Printf("SV_Configstrings_f from different level\n");
            SV_New_f();
            return;
        }

        int start = Lib.atoi(Cmd.Argv(2));

        

        while (SV_MAIN.sv_client.netchan.message.cursize < Defines.MAX_MSGLEN / 2
                && start < Defines.MAX_CONFIGSTRINGS) {
            if (SV_INIT.sv.configstrings[start] != null
                    && SV_INIT.sv.configstrings[start].length() != 0) {
                MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                        Defines.svc_configstring);
                MSG.WriteShort(SV_MAIN.sv_client.netchan.message, start);
                MSG.WriteString(SV_MAIN.sv_client.netchan.message,
                        SV_INIT.sv.configstrings[start]);
            }
            start++;
        }

        

        if (start == Defines.MAX_CONFIGSTRINGS) {
            MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                    Defines.svc_stufftext);
            MSG.WriteString(SV_MAIN.sv_client.netchan.message, "cmd baselines "
                    + SV_INIT.svs.spawncount + " 0\n");
        } else {
            MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                    Defines.svc_stufftext);
            MSG.WriteString(SV_MAIN.sv_client.netchan.message,
                    "cmd configstrings " + SV_INIT.svs.spawncount + ' ' + start
                            + '\n');
        }
    }

    /*
     * ================== SV_Baselines_f ==================
     */
    public static void SV_Baselines_f() {

        Com.DPrintf("Baselines() from " + SV_MAIN.sv_client.name + '\n');

        if (SV_MAIN.sv_client.state != Defines.cs_connected) {
            Com.Printf("baselines not valid -- already spawned\n");
            return;
        }

        
        if (Lib.atoi(Cmd.Argv(1)) != SV_INIT.svs.spawncount) {
            Com.Printf("SV_Baselines_f from different level\n");
            SV_New_f();
            return;
        }

        int start = Lib.atoi(Cmd.Argv(2));


        entity_state_t nullstate = new entity_state_t(null);

        

        while (SV_MAIN.sv_client.netchan.message.cursize < Defines.MAX_MSGLEN / 2
                && start < Defines.MAX_EDICTS) {
            entity_state_t base = SV_INIT.sv.baselines[start];
            if (base.modelindex != 0 || base.sound != 0 || base.effects != 0) {
                MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                        Defines.svc_spawnbaseline);
                MSG.WriteDeltaEntity(nullstate, base,
                        SV_MAIN.sv_client.netchan.message, true, true);
            }
            start++;
        }

        

        if (start == Defines.MAX_EDICTS) {
            MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                    Defines.svc_stufftext);
            MSG.WriteString(SV_MAIN.sv_client.netchan.message, "precache "
                    + SV_INIT.svs.spawncount + '\n');
        } else {
            MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                    Defines.svc_stufftext);
            MSG.WriteString(SV_MAIN.sv_client.netchan.message, "cmd baselines "
                    + SV_INIT.svs.spawncount + ' ' + start + '\n');
        }
    }

    /*
     * ================== SV_Begin_f ==================
     */
    public static void SV_Begin_f() {
        Com.DPrintf("Begin() from " + SV_MAIN.sv_client.name + '\n');

        
        if (Lib.atoi(Cmd.Argv(1)) != SV_INIT.svs.spawncount) {
            Com.Printf("SV_Begin_f from different level\n");
            SV_New_f();
            return;
        }

        SV_MAIN.sv_client.state = Defines.cs_spawned;

        
        PlayerClient.ClientBegin(SV_USER.sv_player);

        Cbuf.InsertFromDefer();
    }

    

    /*
     * ================== SV_NextDownload_f ==================
     */
    public static void SV_NextDownload_f() {

        if (SV_MAIN.sv_client.download == null)
            return;

        int r = SV_MAIN.sv_client.downloadsize - SV_MAIN.sv_client.downloadcount;
        if (r > 1024)
            r = 1024;

        MSG.WriteByte(SV_MAIN.sv_client.netchan.message, Defines.svc_download);
        MSG.WriteShort(SV_MAIN.sv_client.netchan.message, r);

        SV_MAIN.sv_client.downloadcount += r;
        int size = SV_MAIN.sv_client.downloadsize;
        if (size == 0)
            size = 1;
        int percent = SV_MAIN.sv_client.downloadcount * 100 / size;
        MSG.WriteByte(SV_MAIN.sv_client.netchan.message, percent);
        SZ.Write(SV_MAIN.sv_client.netchan.message, SV_MAIN.sv_client.download,
                SV_MAIN.sv_client.downloadcount - r, r);

        if (SV_MAIN.sv_client.downloadcount != SV_MAIN.sv_client.downloadsize)
            return;

        FS.FreeFile(SV_MAIN.sv_client.download);
        SV_MAIN.sv_client.download = null;
    }

    /*
     * ================== SV_BeginDownload_f ==================
     */
    public static void SV_BeginDownload_f() {
        int offset = 0;

        String name = Cmd.Argv(1);

        if (Cmd.Argc() > 2)
            offset = Lib.atoi(Cmd.Argv(2)); 

        
        

        if (name.contains("..")
                || SV_MAIN.allow_download.value == (float) 0
                || (int) name.charAt(0) == (int) '.'
                                         
                || (int) name.charAt(0) == (int) '/'
                || (name.startsWith("players/") && (float) 0 == SV_MAIN.allow_download_players.value)
                                                                                              
                || (name.startsWith("models/") && (float) 0 == SV_MAIN.allow_download_models.value)
                                                                                            
                || (name.startsWith("sound/") && (float) 0 == SV_MAIN.allow_download_sounds.value)
                
                || (name.startsWith("maps/") && (float) 0 == SV_MAIN.allow_download_maps.value)
                                                                                        
                                                                                        
                                                                                        
                || name.indexOf((int) '/') == -1) {
                                              
            MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                    Defines.svc_download);
            MSG.WriteShort(SV_MAIN.sv_client.netchan.message, -1);
            MSG.WriteByte(SV_MAIN.sv_client.netchan.message, 0);
            return;
        }

        if (SV_MAIN.sv_client.download != null)
            FS.FreeFile(SV_MAIN.sv_client.download);

        SV_MAIN.sv_client.download = FS.LoadFile(name);
        
        
        if (SV_MAIN.sv_client.download == null)
        {        	
        	return;
        }
        
        SV_MAIN.sv_client.downloadsize = SV_MAIN.sv_client.download.length;
        SV_MAIN.sv_client.downloadcount = offset;

        if (offset > SV_MAIN.sv_client.downloadsize)
            SV_MAIN.sv_client.downloadcount = SV_MAIN.sv_client.downloadsize;

        if (SV_MAIN.sv_client.download == null 
                                               
                                               
                							   
                || (name.startsWith("maps/") && FS.file_from_pak != 0)) {
            Com.DPrintf("Couldn't download " + name + " to "
                    + SV_MAIN.sv_client.name + '\n');
            if (SV_MAIN.sv_client.download != null) {
                FS.FreeFile(SV_MAIN.sv_client.download);
                SV_MAIN.sv_client.download = null;
            }

            MSG.WriteByte(SV_MAIN.sv_client.netchan.message,
                    Defines.svc_download);
            MSG.WriteShort(SV_MAIN.sv_client.netchan.message, -1);
            MSG.WriteByte(SV_MAIN.sv_client.netchan.message, 0);
            return;
        }

        SV_NextDownload_f();
        Com.DPrintf("Downloading " + name + " to " + SV_MAIN.sv_client.name
                + '\n');
    }

    

    /*
     * ================= SV_Disconnect_f
     * 
     * The client is going to disconnect, so remove the connection immediately
     * =================
     */
    public static void SV_Disconnect_f() {
        
        SV_MAIN.SV_DropClient(SV_MAIN.sv_client);
    }

    /*
     * ================== SV_ShowServerinfo_f
     * 
     * Dumps the serverinfo info string ==================
     */
    public static void SV_ShowServerinfo_f() {
        Info.Print(Cvar.Serverinfo());
    }

    public static void SV_Nextserver() {


        if (SV_INIT.sv.state == Defines.ss_game
                || (SV_INIT.sv.state == Defines.ss_pic &&
                (float) 0 == Cvar.VariableValue("coop")))
            return; 

        SV_INIT.svs.spawncount++;
        String v = Cvar.VariableString("nextserver");
        
        if (v.length() == 0)
            Cbuf.AddText("killserver\n");
        else {
            Cbuf.AddText(v);
            Cbuf.AddText("\n");
        }
        Cvar.Set("nextserver", "");
    }

    /*
     * ================== SV_Nextserver_f
     * 
     * A cinematic has completed or been aborted by a client, so move to the
     * next server, ==================
     */
    public static void SV_Nextserver_f() {
        if (Lib.atoi(Cmd.Argv(1)) != SV_INIT.svs.spawncount) {
            Com.DPrintf("Nextserver() from wrong level, from "
                    + SV_MAIN.sv_client.name + '\n');
            return; 
        }

        Com.DPrintf("Nextserver() from " + SV_MAIN.sv_client.name + '\n');

        SV_Nextserver();
    }

    /*
     * ================== SV_ExecuteUserCommand ==================
     */
    public static void SV_ExecuteUserCommand(String s) {
        
        Com.dprintln("SV_ExecuteUserCommand:" + s );

        Cmd.TokenizeString(s.toCharArray(), true);
        SV_USER.sv_player = SV_MAIN.sv_client.edict;


        int i = 0;
        for (ucmd_t u = null; i < SV_USER.ucmds.length; i++) {
            u = SV_USER.ucmds[i];
            if (Cmd.Argv(0).equals(u.name)) {
                u.r.run();
                break;
            }
        }

        if (i == SV_USER.ucmds.length && SV_INIT.sv.state == Defines.ss_game)
            Cmd.ClientCommand(SV_USER.sv_player);

        
    }

    /*
     * ===========================================================================
     * 
     * USER CMD EXECUTION
     * 
     * ===========================================================================
     */

    public static void SV_ClientThink(client_t cl, usercmd_t cmd) {
        cl.commandMsec -= (int) cmd.msec & 0xFF;

        if (cl.commandMsec < 0 && SV_MAIN.sv_enforcetime.value != (float) 0) {
            Com.DPrintf("commandMsec underflow from " + cl.name + '\n');
            return;
        }

        PlayerClient.ClientThink(cl.edict, cmd);
    }

    /*
     * =================== SV_ExecuteClientMessage
     * 
     * The current net_message is parsed for the given client
     * ===================
     */
    public static void SV_ExecuteClientMessage(client_t cl) {

        usercmd_t nullcmd = new usercmd_t();
        usercmd_t oldest = new usercmd_t(), oldcmd = new usercmd_t(), newcmd = new usercmd_t();

        SV_MAIN.sv_client = cl;
        SV_USER.sv_player = SV_MAIN.sv_client.edict;


        boolean move_issued = false;
        int stringCmdCount = 0;

        while (true) {
            if (Globals.net_message.readcount > Globals.net_message.cursize) {
                Com.Printf("SV_ReadClientMessage: bad read:\n");
                Com.Printf(Lib.hexDump(Globals.net_message.data, 32, false));
                SV_MAIN.SV_DropClient(cl);
                return;
            }

            int c = MSG.ReadByte(Globals.net_message);
            if (c == -1)
                break;

            int lastframe;
            int checksumIndex;
            int calculatedChecksum;
            int checksum;
            String s;
            switch (c) {
            default:
                Com.Printf("SV_ReadClientMessage: unknown command char\n");
                SV_MAIN.SV_DropClient(cl);
                return;

            case Defines.clc_nop:
                break;

            case Defines.clc_userinfo:
                cl.userinfo = MSG.ReadString(Globals.net_message);
                SV_MAIN.SV_UserinfoChanged(cl);
                break;

            case Defines.clc_move:
                if (move_issued)
                    return; 

                move_issued = true;
                checksumIndex = Globals.net_message.readcount;
                checksum = MSG.ReadByte(Globals.net_message);
                lastframe = MSG.ReadLong(Globals.net_message);

                if (lastframe != cl.lastframe) {
                    cl.lastframe = lastframe;
                    if (cl.lastframe > 0) {
                        cl.frame_latency[cl.lastframe
                                & (Defines.LATENCY_COUNTS - 1)] = SV_INIT.svs.realtime
                                - cl.frames[cl.lastframe & Defines.UPDATE_MASK].senttime;
                    }
                }

                
                nullcmd = new usercmd_t();
                MSG.ReadDeltaUsercmd(Globals.net_message, nullcmd, oldest);
                MSG.ReadDeltaUsercmd(Globals.net_message, oldest, oldcmd);
                MSG.ReadDeltaUsercmd(Globals.net_message, oldcmd, newcmd);

                if (cl.state != Defines.cs_spawned) {
                    cl.lastframe = -1;
                    break;
                }

                

                calculatedChecksum = (int) Com.BlockSequenceCRCByte(
                        Globals.net_message.data, checksumIndex + 1,
                        Globals.net_message.readcount - checksumIndex - 1,
                        cl.netchan.incoming_sequence);

                if ((calculatedChecksum & 0xff) != checksum) {
                    Com.DPrintf("Failed command checksum for " + cl.name + " ("
                            + calculatedChecksum + " != " + checksum + ")/"
                            + cl.netchan.incoming_sequence + '\n');
                    return;
                }

                if ((float) 0 == SV_MAIN.sv_paused.value) {
                    int net_drop = cl.netchan.dropped;
                    if (net_drop < 20) {

                        

                        
                        while (net_drop > 2) {
                            SV_ClientThink(cl, cl.lastcmd);

                            net_drop--;
                        }
                        if (net_drop > 1)
                            SV_ClientThink(cl, oldest);

                        if (net_drop > 0)
                            SV_ClientThink(cl, oldcmd);

                    }
                    SV_ClientThink(cl, newcmd);
                }

                
                cl.lastcmd.set(newcmd);
                break;

            case Defines.clc_stringcmd:
                s = MSG.ReadString(Globals.net_message);

                
                if (++stringCmdCount < SV_USER.MAX_STRINGCMDS)
                    SV_ExecuteUserCommand(s);

                if (cl.state == Defines.cs_zombie)
                    return; 
                break;
            }
        }
    }
}