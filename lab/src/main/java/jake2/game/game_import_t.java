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



package jake2.game;

import jake2.Defines;
import jake2.qcommon.*;
import jake2.server.SV_GAME;
import jake2.server.SV_INIT;
import jake2.server.SV_SEND;
import jake2.server.SV_WORLD;




public class game_import_t {
    
    public static void bprintf(int printlevel, String s) {
        SV_SEND.SV_BroadcastPrintf(printlevel, s);
    }

    public static void dprintf(String s) {
        SV_GAME.PF_dprintf(s);
    }

    public static void cprintf(edict_t ent, int printlevel, String s) {
        SV_GAME.PF_cprintf(ent, printlevel, s);
    }

    public static void centerprintf(edict_t ent, String s) {
        SV_GAME.PF_centerprintf(ent, s);
    }

    public static void sound(edict_t ent, int channel, int soundindex, float volume,
                             float attenuation, float timeofs) {
        SV_GAME.PF_StartSound(ent, channel, soundindex, volume, attenuation,
                timeofs);
    }

    public static void positioned_sound(float[] origin, edict_t ent, int channel,
                                        int soundinedex, float volume, float attenuation, float timeofs) {

        SV_SEND.SV_StartSound(origin, ent, channel, soundinedex, volume,
                attenuation, timeofs);
    }

    
    
    
    
    public static void configstring(int num, String string) {
        SV_GAME.PF_Configstring(num, string);
    }

    public static void error(String err) {
        Com.Error(Defines.ERR_FATAL, err);
    }

    public static void error(int level, String err) {
        SV_GAME.PF_error(level, err);
    }

    
    public static int modelindex(String name) {
        return SV_INIT.SV_ModelIndex(name);
    }

    public static int soundindex(String name) {
        return SV_INIT.SV_SoundIndex(name);
    }

    public static int imageindex(String name) {
        return SV_INIT.SV_ImageIndex(name);
    }

    public static void setmodel(edict_t ent, String name) {
        SV_GAME.PF_setmodel(ent, name);
    }

    
    public static trace_t trace(float[] start, float[] mins, float[] maxs,
                                float[] end, edict_t passent, int contentmask) {
        return SV_WORLD.SV_Trace(start, mins, maxs, end, passent, contentmask);
    }

    public pmove_t.PointContentsAdapter pointcontents = new MyPointContentsAdapter();

    public static boolean inPHS(float[] p1, float[] p2) {
        return SV_GAME.PF_inPHS(p1, p2);
    }

    public static void SetAreaPortalState(int portalnum, boolean open) {
        CM.CM_SetAreaPortalState(portalnum, open);
    }

    public static boolean AreasConnected(int area1, int area2) {
        return CM.CM_AreasConnected(area1, area2);
    }

    
    
    
    public static void linkentity(edict_t ent) {
        SV_WORLD.SV_LinkEdict(ent);
    }

    public static void unlinkentity(edict_t ent) {
        SV_WORLD.SV_UnlinkEdict(ent);
    }

    
    public static int BoxEdicts(float[] mins, float[] maxs, edict_t[] list,
                                int maxcount, int areatype) {
        return SV_WORLD.SV_AreaEdicts(mins, maxs, list, maxcount, areatype);
    }

    public static void Pmove(pmove_t pmove) {
        PMove.Pmove(pmove);
    }

    
    
    public static void multicast(float[] origin, int to) {
        SV_SEND.SV_Multicast(origin, to);
    }

    public static void unicast(edict_t ent, boolean reliable) {
        SV_GAME.PF_Unicast(ent, reliable);
    }


    public static void WriteByte(int c) {
        SV_GAME.PF_WriteByte(c);
    }

    public static void WriteShort(int c) {
        SV_GAME.PF_WriteShort(c);
    }

    public static void WriteString(String s) {
        SV_GAME.PF_WriteString(s);
    }

    public static void WritePosition(float[] pos) {
        SV_GAME.PF_WritePos(pos);
    }

    
    public static void WriteDir(float[] pos) {
        SV_GAME.PF_WriteDir(pos);
    }

    
    public static cvar_t cvar(String var_name, String value, int flags) {
        return Cvar.Get(var_name, value, flags);
    }

    
    public static cvar_t cvar_set(String var_name, String value) {
        return Cvar.Set(var_name, value);
    }

    
    public static cvar_t cvar_forceset(String var_name, String value) {
        return Cvar.ForceSet(var_name, value);
    }

    
    public static int argc() {
        return Cmd.Argc();
    }


    public static String argv(int n) {
        return Cmd.Argv(n);
    }

    
    public static String args() {
        return Cmd.Args();
    }

    
    
    public static void AddCommandString(String text) {
        Cbuf.AddText(text);
    }

    private static class MyPointContentsAdapter extends pmove_t.PointContentsAdapter {




    }
}