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
import jake2.qcommon.CM;
import jake2.qcommon.Com;
import jake2.qcommon.MSG;
import jake2.qcommon.SZ;
import jake2.util.Math3D;

public class SV_GAME {

    /**
     * PF_Unicast
     * 
     * Sends the contents of the mutlicast buffer to a single client.
     */
    public static void PF_Unicast(edict_t ent, boolean reliable) {

        if (ent == null)
            return;

        var p = ent.index;
        if (p < 1 || p > SV_MAIN.maxclients.value)
            return;

        var client = SV_INIT.svs.clients[p - 1];

        if (reliable)
            SZ.Write(client.netchan.message, SV_INIT.sv.multicast.data,
                    SV_INIT.sv.multicast.cursize);
        else
            SZ.Write(client.datagram, SV_INIT.sv.multicast.data,
                    SV_INIT.sv.multicast.cursize);

        SZ.Clear(SV_INIT.sv.multicast);
    }

    /**
     * PF_dprintf
     * 
     * Debug print to server console.
     */
    public static void PF_dprintf(String fmt) {
        Com.Printf(fmt);
    }


    /**
     * Centerprintf for critical messages.
     */
    public static void PF_cprintfhigh(edict_t ent, String fmt) {
    	PF_cprintf(ent, Defines.PRINT_HIGH, fmt);
    }
    
    /**
     * PF_cprintf
     * 
     * Print to a single client.
     */
    public static void PF_cprintf(edict_t ent, int level, String fmt) {

        var n = 0;

        if (ent != null) {
            n = ent.index;
            if (n < 1 || n > SV_MAIN.maxclients.value)
                Com.Error(Defines.ERR_DROP, "cprintf to a non-client");
        }

        if (ent != null)
            SV_SEND.SV_ClientPrintf(SV_INIT.svs.clients[n - 1], level, fmt);
        else
            Com.Printf(fmt);
    }

    /**
     * PF_centerprintf
     * 
     * centerprint to a single client.
     */
    public static void PF_centerprintf(edict_t ent, String fmt) {

        var n = ent.index;
        if (n < 1 || n > SV_MAIN.maxclients.value)
            return; 

        MSG.WriteByte(SV_INIT.sv.multicast, Defines.svc_centerprint);
        MSG.WriteString(SV_INIT.sv.multicast, fmt);
        PF_Unicast(ent, true);
    }

    /**
     *  PF_error
     * 
     *  Abort the server with a game error. 
     */
    public static void PF_error(String fmt) {
        Com.Error(Defines.ERR_DROP, "Game Error: " + fmt);
    }

    public static void PF_error(int level, String fmt) {
        Com.Error(level, fmt);
    }

    /**
     * PF_setmodel
     * 
     * Also sets mins and maxs for inline bmodels.
     */
    public static void PF_setmodel(edict_t ent, String name) {

        if (name == null)
            Com.Error(Defines.ERR_DROP, "PF_setmodel: NULL");

        var i = SV_INIT.SV_ModelIndex(name);

        ent.s.modelindex = i;

        
        if (name.startsWith("*")) {
            var mod = CM.InlineModel(name);
            Math3D.VectorCopy(mod.mins, ent.mins);
            Math3D.VectorCopy(mod.maxs, ent.maxs);
            SV_WORLD.SV_LinkEdict(ent);
        }
    }

    /**
     *  PF_Configstring
     */
    public static void PF_Configstring(int index, String val) {
        if (index < 0 || index >= Defines.MAX_CONFIGSTRINGS)
            Com.Error(Defines.ERR_DROP, "configstring: bad index " + index
                    + '\n');

        if (val == null)
            val = "";

        
        SV_INIT.sv.configstrings[index] = val;

        if (SV_INIT.sv.state != Defines.ss_loading) { 
                                                      
            SZ.Clear(SV_INIT.sv.multicast);
            MSG.WriteChar(SV_INIT.sv.multicast, Defines.svc_configstring);
            MSG.WriteShort(SV_INIT.sv.multicast, index);
            MSG.WriteString(SV_INIT.sv.multicast, val);

            SV_SEND.SV_Multicast(Globals.vec3_origin, Defines.MULTICAST_ALL_R);
        }
    }

    public static void PF_WriteChar(int c) {
        MSG.WriteChar(SV_INIT.sv.multicast, c);
    }

    public static void PF_WriteByte(int c) {
        MSG.WriteByte(SV_INIT.sv.multicast, c);
    }

    public static void PF_WriteShort(int c) {
        MSG.WriteShort(SV_INIT.sv.multicast, c);
    }

    public static void PF_WriteLong(int c) {
        MSG.WriteLong(SV_INIT.sv.multicast, c);
    }

    public static void PF_WriteFloat(float f) {
        MSG.WriteFloat(SV_INIT.sv.multicast, f);
    }

    public static void PF_WriteString(String s) {
        MSG.WriteString(SV_INIT.sv.multicast, s);
    }

    public static void PF_WritePos(float[] pos) {
        MSG.WritePos(SV_INIT.sv.multicast, pos);
    }

    public static void PF_WriteDir(float[] dir) {
        MSG.WriteDir(SV_INIT.sv.multicast, dir);
    }

    public static void PF_WriteAngle(float f) {
        MSG.WriteAngle(SV_INIT.sv.multicast, f);
    }

    /**
     * PF_inPVS
     * 
     * Also checks portalareas so that doors block sight.
     */
    public static boolean PF_inPVS(float[] p1, float[] p2) {

        var leafnum = CM.CM_PointLeafnum(p1);
        var cluster = CM.CM_LeafCluster(leafnum);
        var area1 = CM.CM_LeafArea(leafnum);
        var mask = CM.CM_ClusterPVS(cluster);

        leafnum = CM.CM_PointLeafnum(p2);
        cluster = CM.CM_LeafCluster(leafnum);
        var area2 = CM.CM_LeafArea(leafnum);

        
        if (cluster == -1)
            return false;
        if (mask != null && (0 == (mask[cluster >>> 3] & (1 << (cluster & 7)))))
            return false;

        return CM.CM_AreasConnected(area1, area2);

    }

    /**
     * PF_inPHS.
     * 
     * Also checks portalareas so that doors block sound.
     */
    public static boolean PF_inPHS(float[] p1, float[] p2) {

        var leafnum = CM.CM_PointLeafnum(p1);
        var cluster = CM.CM_LeafCluster(leafnum);
        var area1 = CM.CM_LeafArea(leafnum);
        var mask = CM.CM_ClusterPHS(cluster);

        leafnum = CM.CM_PointLeafnum(p2);
        cluster = CM.CM_LeafCluster(leafnum);
        var area2 = CM.CM_LeafArea(leafnum);

        
        if (cluster == -1)
            return false;
        if (mask != null && (0 == (mask[cluster >> 3] & (1 << (cluster & 7)))))
            return false; 
        return CM.CM_AreasConnected(area1, area2);

    }

    public static void PF_StartSound(edict_t entity, int channel,
            int sound_num, float volume, float attenuation, float timeofs) {

        if (null == entity)
            return;
        SV_SEND.SV_StartSound(null, entity, channel, sound_num, volume,
                attenuation, timeofs);

    }


    /**
     *  SV_ShutdownGameProgs
     * 
     * Called when either the entire server is being killed, or it is changing
     * to a different game directory. 
     */
    public static void SV_ShutdownGameProgs() {
        GameBase.ShutdownGame();
    }

    /**
     * SV_InitGameProgs
     * 
     * Init the game subsystem for a new map. 
     */

    public static void SV_InitGameProgs() {

        
        SV_ShutdownGameProgs();

        var gimport = new game_import_t();

        
        GameBase.GetGameApi(gimport);

        GameSave.InitGame();
    }
}