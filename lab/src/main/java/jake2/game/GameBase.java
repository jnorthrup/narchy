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





/** Father of all GameObjects. */

package jake2.game;

import jake2.Defines;
import jake2.client.M;
import jake2.qcommon.Com;
import jake2.server.SV;
import jake2.server.SV_WORLD;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.util.StringTokenizer;

public class GameBase {
    public static final cplane_t dummyplane = new cplane_t();

    public static final game_locals_t game = new game_locals_t();

    public static level_locals_t level = new level_locals_t();

    public static game_import_t gi = new game_import_t();

    public static spawn_temp_t st = new spawn_temp_t();

    public static int sm_meat_index;

    public static int snd_fry;

    public static int meansOfDeath;

    public static int num_edicts;

    public static edict_t[] g_edicts = new edict_t[Defines.MAX_EDICTS];
    static {
        for (int n = 0; n < Defines.MAX_EDICTS; n++)
            g_edicts[n] = new edict_t(n);
    }

    public static cvar_t deathmatch = new cvar_t();

    public static cvar_t coop = new cvar_t();

    public static cvar_t dmflags = new cvar_t();

    public static cvar_t skill = new cvar_t();

    public static cvar_t fraglimit = new cvar_t();

    public static cvar_t timelimit = new cvar_t();

    public static cvar_t password = new cvar_t();

    public static cvar_t spectator_password = new cvar_t();

    public static cvar_t needpass = new cvar_t();

    public static cvar_t maxclients = new cvar_t();

    public static cvar_t maxspectators = new cvar_t();

    public static cvar_t maxentities = new cvar_t();

    public static cvar_t g_select_empty = new cvar_t();

    public static cvar_t filterban = new cvar_t();

    public static cvar_t sv_maxvelocity = new cvar_t();

    public static cvar_t sv_gravity = new cvar_t();

    public static cvar_t sv_rollspeed = new cvar_t();

    public static cvar_t sv_rollangle = new cvar_t();

    public static cvar_t gun_x = new cvar_t();

    public static cvar_t gun_y = new cvar_t();

    public static cvar_t gun_z = new cvar_t();

    public static cvar_t run_pitch = new cvar_t();

    public static cvar_t run_roll = new cvar_t();

    public static cvar_t bob_up = new cvar_t();

    public static cvar_t bob_pitch = new cvar_t();

    public static cvar_t bob_roll = new cvar_t();

    public static cvar_t sv_cheats = new cvar_t();

    public static cvar_t flood_msgs = new cvar_t();

    public static cvar_t flood_persecond = new cvar_t();

    public static cvar_t flood_waitdelay = new cvar_t();

    public static cvar_t sv_maplist = new cvar_t();

    public static final float STOP_EPSILON = 0.1f;

    /**
     * Slide off of the impacting object returns the blocked flags (1 = floor, 2 =
     * step / wall).
     */
    public static int ClipVelocity(float[] in, float[] normal, float[] out,
            float overbounce) {

        int blocked = 0;
        if (normal[2] > 0)
            blocked |= 1; 
        if (normal[2] == 0.0f)
            blocked |= 2;

        float backoff = Math3D.DotProduct(in, normal) * overbounce;

        for (int i = 0; i < 3; i++) {
            float change = normal[i] * backoff;
            out[i] = in[i] - change;
            if (out[i] > -STOP_EPSILON && out[i] < STOP_EPSILON)
                out[i] = 0;
        }

        return blocked;
    }


    /**
     * Searches all active entities for the next one that holds the matching
     * string at fieldofs (use the FOFS() macro) in the structure.
     * 
     * Searches beginning at the edict after from, or the beginning if null null
     * will be returned if the end of the list is reached.
     * 
     */

    public static EdictIterator G_Find(EdictIterator from, EdictFindFilter eff,
            String s) {

        if (from == null)
            from = new EdictIterator(0);
        else
            from.i++;

        for (; from.i < num_edicts; from.i++) {
            from.o = g_edicts[from.i];
            if (from.o.classname == null) {
                Com.Printf("edict with classname = null" + from.o.index);
            }

            if (!from.o.inuse)
                continue;

            if (eff.matches(from.o, s))
                return from;
        }

        return null;
    }

    
    public static edict_t G_FindEdict(EdictIterator from, EdictFindFilter eff,
            String s) {
        EdictIterator ei = G_Find(from, eff, s);
        if (ei == null)
            return null;
        else
            return ei.o;
    }

    /**
     * Returns entities that have origins within a spherical area.
     */
    public static EdictIterator findradius(EdictIterator from, float[] org,
            float rad) {

        if (from == null)
            from = new EdictIterator(0);
        else
            from.i++;

        for (float[] eorg = {0, 0, 0}; from.i < num_edicts; from.i++) {
            from.o = g_edicts[from.i];
            if (!from.o.inuse)
                continue;

            if (from.o.solid == Defines.SOLID_NOT)
                continue;

            for (int j = 0; j < 3; j++)
                eorg[j] = org[j]
                        - (from.o.s.origin[j] + (from.o.mins[j] + from.o.maxs[j]) * 0.5f);

            if (Math3D.VectorLength(eorg) > rad)
                continue;
            return from;
        }

        return null;
    }

    /**
     * Searches all active entities for the next one that holds the matching
     * string at fieldofs (use the FOFS() macro) in the structure.
     * 
     * Searches beginning at the edict after from, or the beginning if null null
     * will be returned if the end of the list is reached.
     */

    public static final int MAXCHOICES = 8;

    public static edict_t G_PickTarget(String targetname) {

        if (targetname == null) {
            game_import_t.dprintf("G_PickTarget called with null targetname\n");
            return null;
        }

        EdictIterator es = null;

        edict_t[] choice = new edict_t[MAXCHOICES];
        int num_choices = 0;
        while ((es = G_Find(es, findByTarget, targetname)) != null) {
            choice[num_choices++] = es.o;
            if (num_choices == MAXCHOICES)
                break;
        }

        if (num_choices == 0) {
            game_import_t.dprintf("G_PickTarget: target " + targetname + " not found\n");
            return null;
        }

        return choice[Lib.rand() % num_choices];
    }

    public static final float[] VEC_UP = { 0, -1, 0 };

    public static final float[] MOVEDIR_UP = { 0, 0, 1 };

    public static final float[] VEC_DOWN = { 0, -2, 0 };

    public static final float[] MOVEDIR_DOWN = { 0, 0, -1 };

    public static void G_SetMovedir(float[] angles, float[] movedir) {
        if (Math3D.VectorEquals(angles, VEC_UP)) {
            Math3D.VectorCopy(MOVEDIR_UP, movedir);
        } else if (Math3D.VectorEquals(angles, VEC_DOWN)) {
            Math3D.VectorCopy(MOVEDIR_DOWN, movedir);
        } else {
            Math3D.AngleVectors(angles, movedir, null, null);
        }

        Math3D.VectorClear(angles);
    }

    public static String G_CopyString(String in) {
        return in;
    }

    /**
     * G_TouchTriggers
     */

    static final edict_t[] touch = new edict_t[Defines.MAX_EDICTS];

    public static void G_TouchTriggers(edict_t ent) {


        if ((ent.client != null || (ent.svflags & Defines.SVF_MONSTER) != 0)
                && (ent.health <= 0))
            return;

        int num = game_import_t.BoxEdicts(ent.absmin, ent.absmax, touch, Defines.MAX_EDICTS,
                Defines.AREA_TRIGGERS);

        
        
        for (int i = 0; i < num; i++) {
            edict_t hit = touch[i];

            if (!hit.inuse)
                continue;

            if (hit.touch == null)
                continue;

            hit.touch.touch(hit, ent, dummyplane, null);
        }
    }

    public static final pushed_t[] pushed = new pushed_t[Defines.MAX_EDICTS];
    static {
        for (int n = 0; n < Defines.MAX_EDICTS; n++)
            pushed[n] = new pushed_t();
    }

    public static int pushed_p;

    public static edict_t obstacle;

    public static int c_yes;
    public static int c_no;

    public static final int STEPSIZE = 18;

    /**
     * G_RunEntity
     */
    public static void G_RunEntity(edict_t ent) {

        if (ent.prethink != null)
            ent.prethink.think(ent);

        switch (ent.movetype) {
        case Defines.MOVETYPE_PUSH:
        case Defines.MOVETYPE_STOP:
            SV.SV_Physics_Pusher(ent);
            break;
        case Defines.MOVETYPE_NONE:
            SV.SV_Physics_None(ent);
            break;
        case Defines.MOVETYPE_NOCLIP:
            SV.SV_Physics_Noclip(ent);
            break;
        case Defines.MOVETYPE_STEP:
            SV.SV_Physics_Step(ent);
            break;
        case Defines.MOVETYPE_TOSS:
        case Defines.MOVETYPE_BOUNCE:
        case Defines.MOVETYPE_FLY:
        case Defines.MOVETYPE_FLYMISSILE:
            SV.SV_Physics_Toss(ent);
            break;
        default:
            game_import_t.error("SV_Physics: bad movetype " + ent.movetype);
        }
    }

    public static void ClearBounds(float[] mins, float[] maxs) {
        mins[0] = mins[1] = mins[2] = 99999;
        maxs[0] = maxs[1] = maxs[2] = -99999;
    }

    public static void AddPointToBounds(float[] v, float[] mins, float[] maxs) {

        for (int i = 0; i < 3; i++) {
            float val = v[i];
            if (val < mins[i])
                mins[i] = val;
            if (val > maxs[i])
                maxs[i] = val;
        }
    }

    public static final EdictFindFilter findByTarget = new EdictFindFilter() {
        @Override
        public boolean matches(edict_t e, String s) {
            if (e.targetname == null)
                return false;
            return e.targetname.equalsIgnoreCase(s);
        }
    };

    public static final EdictFindFilter findByClass = new EdictFindFilter() {
        @Override
        public boolean matches(edict_t e, String s) {
            return e.classname.equalsIgnoreCase(s);
        }
    };

    public static void ShutdownGame() {
        game_import_t.dprintf("==== ShutdownGame ====\n");
    }

    /**
     * ClientEndServerFrames.
     */
    public static void ClientEndServerFrames() {


        for (int i = 0; i < maxclients.value; i++) {
            edict_t ent = g_edicts[1 + i];
            if (!ent.inuse || null == ent.client)
                continue;
            PlayerView.ClientEndServerFrame(ent);
        }

    }

    /**
     * Returns the created target changelevel.
     */
    public static edict_t CreateTargetChangeLevel(String map) {

        edict_t ent = GameUtil.G_Spawn();
        ent.classname = "target_changelevel";
        level.nextmap = map;
        ent.map = level.nextmap;
        return ent;
    }

    /**
     * The timelimit or fraglimit has been exceeded.
     */
    public static void EndDMLevel() {


        if (((int) dmflags.value & Defines.DF_SAME_LEVEL) != 0) {
            PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.mapname));
            return;
        }

        
        if (sv_maplist.string.length() > 0) {
            String s = sv_maplist.string;
            String f = null;
            String seps = " ,\n\r";
            StringTokenizer tk = new StringTokenizer(s, seps);
            
            while (tk.hasMoreTokens()){
                String t = tk.nextToken();


                if (f == null)
            		f = t;
            	
                if (t.equalsIgnoreCase(level.mapname)) {
                    
                	if (!tk.hasMoreTokens()) {
                		
                        if (f == null) 
                            PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.mapname));
                        else
                            PlayerHud.BeginIntermission(CreateTargetChangeLevel(f));
                    } else
                        PlayerHud.BeginIntermission(CreateTargetChangeLevel(tk.nextToken()));
                    return;
                }
            }
        }

        
        if (level.nextmap.length() > 0) 
            PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.nextmap));
        else { 
            EdictIterator edit = null;
            edit = G_Find(edit, findByClass, "target_changelevel");
            if (edit == null) { 
                                
                
                PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.mapname));
                return;
            }
            edict_t ent = edit.o;
            PlayerHud.BeginIntermission(ent);
        }
    }

    /**
     * CheckNeedPass.
     */
    public static void CheckNeedPass() {


        if (password.modified || spectator_password.modified) {
            password.modified = spectator_password.modified = false;

            int need = 0;

            if ((password.string.length() > 0)
                    && 0 != Lib.Q_stricmp(password.string, "none"))
                need |= 1;
            if ((spectator_password.string.length() > 0)
                    && 0 != Lib.Q_stricmp(spectator_password.string, "none"))
                need |= 2;

            game_import_t.cvar_set("needpass", String.valueOf(need));
        }
    }

    /**
     * CheckDMRules.
     */
    public static void CheckDMRules() {

        if (level.intermissiontime != 0)
            return;

        if (0 == deathmatch.value)
            return;

        if (timelimit.value != 0) {
            if (level.time >= timelimit.value * 60) {
                game_import_t.bprintf(Defines.PRINT_HIGH, "Timelimit hit.\n");
                EndDMLevel();
                return;
            }
        }

        if (fraglimit.value != 0) {
            for (int i = 0; i < maxclients.value; i++) {
                gclient_t cl = game.clients[i];
                if (!g_edicts[i + 1].inuse)
                    continue;

                if (cl.resp.score >= fraglimit.value) {
                    game_import_t.bprintf(Defines.PRINT_HIGH, "Fraglimit hit.\n");
                    EndDMLevel();
                    return;
                }
            }
        }
    }

    /**
     * Exits a level.
     */
    public static void ExitLevel() {

        String command = "gamemap \"" + level.changemap + "\"\n";
        game_import_t.AddCommandString(command);
        level.changemap = null;
        level.exitintermission = false;
        level.intermissiontime = 0;
        ClientEndServerFrames();

        
        for (int i = 0; i < maxclients.value; i++) {
            edict_t ent = g_edicts[1 + i];
            if (!ent.inuse)
                continue;
            if (ent.health > ent.client.pers.max_health)
                ent.health = ent.client.pers.max_health;
        }
    }

    /**
     * G_RunFrame
     *  
     * Advances the world by Defines.FRAMETIME (0.1) seconds.
     */
    public static void G_RunFrame() {

        level.framenum++;
        level.time = level.framenum * Defines.FRAMETIME;

        
        GameAI.AI_SetSightClient();

        

        if (level.exitintermission) {
            ExitLevel();
            return;
        }

        
        
        
        

        for (int i = 0; i < num_edicts; i++) {
            edict_t ent = g_edicts[i];
            if (!ent.inuse)
                continue;

            level.current_entity = ent;

            Math3D.VectorCopy(ent.s.origin, ent.s.old_origin);

            
            if ((ent.groundentity != null)
                    && (ent.groundentity.linkcount != ent.groundentity_linkcount)) {
                ent.groundentity = null;
                if (0 == (ent.flags & (Defines.FL_SWIM | Defines.FL_FLY))
                        && (ent.svflags & Defines.SVF_MONSTER) != 0) {
                    M.M_CheckGround(ent);
                }
            }

            if (i > 0 && i <= maxclients.value) {
                PlayerClient.ClientBeginServerFrame(ent);
                continue;
            }

            G_RunEntity(ent);
        }

        
        CheckDMRules();

        
        CheckNeedPass();

        
        ClientEndServerFrames();
    }

    /**
     * This return a pointer to the structure with all entry points and global
     * variables. 
     */

    public static void GetGameApi(game_import_t imp) {
        gi = imp;
        gi.pointcontents = new pmove_t.PointContentsAdapter() {
            @Override
            public int pointcontents(float[] o) {
                return SV_WORLD.SV_PointContents(o);
            }
        };
    }
}