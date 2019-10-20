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
import jake2.qcommon.Com;
import jake2.util.Lib;
import jake2.util.Math3D;
import jake2.util.Vargs;

public class PlayerHud {

    /*
     * ======================================================================
     * 
     * INTERMISSION
     * 
     * ======================================================================
     */

    public static void MoveClientToIntermission(edict_t ent) {
        if (GameBase.deathmatch.value != (float) 0 || GameBase.coop.value != (float) 0)
            ent.client.showscores = true;
        Math3D.VectorCopy(GameBase.level.intermission_origin, ent.s.origin);
        ent.client.ps.pmove.origin[0] = (short) (GameBase.level.intermission_origin[0] * 8.0F);
        ent.client.ps.pmove.origin[1] = (short) (GameBase.level.intermission_origin[1] * 8.0F);
        ent.client.ps.pmove.origin[2] = (short) (GameBase.level.intermission_origin[2] * 8.0F);
        Math3D.VectorCopy(GameBase.level.intermission_angle,
                ent.client.ps.viewangles);
        ent.client.ps.pmove.pm_type = Defines.PM_FREEZE;
        ent.client.ps.gunindex = 0;
        ent.client.ps.blend[3] = (float) 0;
        ent.client.ps.rdflags &= ~Defines.RDF_UNDERWATER;

        
        ent.client.quad_framenum = (float) 0;
        ent.client.invincible_framenum = (float) 0;
        ent.client.breather_framenum = (float) 0;
        ent.client.enviro_framenum = (float) 0;
        ent.client.grenade_blew_up = false;
        ent.client.grenade_time = (float) 0;

        ent.viewheight = 0;
        ent.s.modelindex = 0;
        ent.s.modelindex2 = 0;
        ent.s.modelindex3 = 0;
        ent.s.modelindex = 0;
        ent.s.effects = 0;
        ent.s.sound = 0;
        ent.solid = Defines.SOLID_NOT;

        

        if (GameBase.deathmatch.value != (float) 0 || GameBase.coop.value != (float) 0) {
            DeathmatchScoreboardMessage(ent, null);
            game_import_t.unicast(ent, true);
        }

    }

    public static void BeginIntermission(edict_t targ) {

        if (GameBase.level.intermissiontime != (float) 0)
            return; 

        GameBase.game.autosaved = false;


        edict_t client;
        int i;
        for (i = 0; (float) i < GameBase.maxclients.value; i++) {
            client = GameBase.g_edicts[1 + i];
            if (!client.inuse)
                continue;
            if (client.health <= 0)
                PlayerClient.respawn(client);
        }

        GameBase.level.intermissiontime = GameBase.level.time;
        GameBase.level.changemap = targ.map;

        if (GameBase.level.changemap.indexOf((int) '*') > -1) {
            if (GameBase.coop.value != (float) 0) {
                for (i = 0; (float) i < GameBase.maxclients.value; i++) {
                    client = GameBase.g_edicts[1 + i];
                    if (!client.inuse)
                        continue;
                    
                    for (int n = 1; n < GameItemList.itemlist.length; n++) {
                        
                        if (GameItemList.itemlist[n] != null)
                            if ((GameItemList.itemlist[n].flags & Defines.IT_KEY) != 0)
                                client.client.pers.inventory[n] = 0;
                    }
                }
            }
        } else {
            if ((float) 0 == GameBase.deathmatch.value) {
                GameBase.level.exitintermission = true; 
                                                        
                return;
            }
        }

        GameBase.level.exitintermission = false;


        edict_t ent = GameBase.G_FindEdict(null, GameBase.findByClass,
                "info_player_intermission");
        if (ent == null) { 
                           
            ent = GameBase.G_FindEdict(null, GameBase.findByClass,
                    "info_player_start");
            if (ent == null)
                ent = GameBase.G_FindEdict(null, GameBase.findByClass,
                        "info_player_deathmatch");
        } else { 
            i = (int) Lib.rand() & 3;
            EdictIterator es = null;

            while (i-- > 0) {
                es = GameBase.G_Find(es, GameBase.findByClass,
                        "info_player_intermission");

                if (es == null) 
                    continue;
                ent = es.o;
            }
        }

        Math3D.VectorCopy(ent.s.origin, GameBase.level.intermission_origin);
        Math3D.VectorCopy(ent.s.angles, GameBase.level.intermission_angle);

        
        for (i = 0; (float) i < GameBase.maxclients.value; i++) {
            client = GameBase.g_edicts[1 + i];
            if (!client.inuse)
                continue;
            MoveClientToIntermission(client);
        }
    }

    /*
     * ================== 
     * DeathmatchScoreboardMessage
     * ==================
     */
    public static void DeathmatchScoreboardMessage(edict_t ent, edict_t killer) {
        StringBuilder string = new StringBuilder(1400);

        int stringlength;
        int i;
        int[] sorted = new int[Defines.MAX_CLIENTS];
        int[] sortedscores = new int[Defines.MAX_CLIENTS];
        edict_t cl_ent;


        int total = 0;
        for (i = 0; i < GameBase.game.maxclients; i++) {
            cl_ent = GameBase.g_edicts[1 + i];
            if (!cl_ent.inuse || GameBase.game.clients[i].resp.spectator)
                continue;
            int score = GameBase.game.clients[i].resp.score;
            int j;
            for (j = 0; j < total; j++) {
                if (score > sortedscores[j])
                    break;
            }
            for (int k = total; k > j; k--) {
                sorted[k] = sorted[k - 1];
                sortedscores[k] = sortedscores[k - 1];
            }
            sorted[j] = i;
            sortedscores[j] = score;
            total++;
        }

        

        
        if (total > 12)
            total = 12;
        
        for (i = 0; i < total; i++) {
            gclient_t cl = GameBase.game.clients[sorted[i]];
            cl_ent = GameBase.g_edicts[1 + sorted[i]];

            int picnum = game_import_t.imageindex("i_fixme");
            int x = (i >= 6) ? 160 : 0;
            int y = 32 + 32 * (i % 6);


            String tag;
            if (cl_ent == ent)
                tag = "tag1";
            else if (cl_ent == killer)
                tag = "tag2";
            else
                tag = null;

            if (tag != null) {
                string.append("xv ").append(x + 32).append(" yv ").append(y)
                        .append(" picn ").append(tag);
            }

            
            string
                    .append(" client ")
                    .append(x)
                    .append(' ')
                    .append(y)
                    .append(' ')
                    .append(sorted[i])
                    .append(' ')
                    .append(cl.resp.score)
                    .append(' ')
                    .append(cl.ping)
                    .append(' ')
                    .append(
                            (GameBase.level.framenum - cl.resp.enterframe) / 600);           
        }

        game_import_t.WriteByte(Defines.svc_layout);
        game_import_t.WriteString(string.toString());
    }

    /*
     * ================== 
     * DeathmatchScoreboard
     * 
     * Draw instead of help message. Note that it isn't that hard to overflow
     * the 1400 byte message limit! 
     * ==================
     */
    public static void DeathmatchScoreboard(edict_t ent) {
        DeathmatchScoreboardMessage(ent, ent.enemy);
        game_import_t.unicast(ent, true);
    }

    /*
     * ================== 
     * Cmd_Score_f
     * 
     * Display the scoreboard 
     * ==================
     */
    public static void Cmd_Score_f(edict_t ent) {
        ent.client.showinventory = false;
        ent.client.showhelp = false;

        if ((float) 0 == GameBase.deathmatch.value && (float) 0 == GameBase.coop.value)
            return;

        if (ent.client.showscores) {
            ent.client.showscores = false;
            return;
        }

        ent.client.showscores = true;
        DeathmatchScoreboard(ent);
    }

    

    /*
     * =============== 
     * G_SetStats 
     * ===============
     */
    public static void G_SetStats(edict_t ent) {


        gclient_t C = ent.client;
        short[] S = C.ps.stats;

        S[Defines.STAT_HEALTH_ICON] = (short) GameBase.level.pic_health;
        S[Defines.STAT_HEALTH] = (short) ent.health;


        gitem_t item;
        if (0 == C.ammo_index /*
                                        * ||
                                        * !ent.client.pers.inventory[ent.client.ammo_index]
                                        */
        ) {
            S[Defines.STAT_AMMO_ICON] = (short) 0;
            S[Defines.STAT_AMMO] = (short) 0;
        } else {
            item = GameItemList.itemlist[C.ammo_index];
            S[Defines.STAT_AMMO_ICON] = (short) game_import_t
                    .imageindex(item.icon);
            S[Defines.STAT_AMMO] = (short) C.pers.inventory[C.ammo_index];
        }


        int power_armor_type = GameItems.PowerArmorType(ent);
        int cells = 0;
        if (power_armor_type != 0) {
            cells = C.pers.inventory[GameItems.ITEM_INDEX(GameItems.FindItem("cells"))];
            if (cells == 0) { 
                ent.flags &= ~Defines.FL_POWER_ARMOR;
                game_import_t
                        .sound(ent, Defines.CHAN_ITEM, game_import_t
                                .soundindex("misc/power2.wav"), 1.0F,
                                (float) Defines.ATTN_NORM, (float) 0);
                power_armor_type = 0;
            }
        }

        int index = GameItems.ArmorIndex(ent);
        if (power_armor_type != 0
                && (0 == index || 0 != (GameBase.level.framenum & 8))) { 
                                                                         
                                                                         
                                                                         
                                                                         
                                                                         
                                                                         
                                                                         
            S[Defines.STAT_ARMOR_ICON] = (short) game_import_t
                    .imageindex("i_powershield");
            S[Defines.STAT_ARMOR] = (short) cells;
        } else if (index != 0) {
            item = GameItems.GetItemByIndex(index);
            S[Defines.STAT_ARMOR_ICON] = (short) game_import_t
                    .imageindex(item.icon);
            S[Defines.STAT_ARMOR] = (short) C.pers.inventory[index];
        } else {
            S[Defines.STAT_ARMOR_ICON] = (short) 0;
            S[Defines.STAT_ARMOR] = (short) 0;
        }

        
        
        
        if (GameBase.level.time > C.pickup_msg_time) {
            S[Defines.STAT_PICKUP_ICON] = (short) 0;
            S[Defines.STAT_PICKUP_STRING] = (short) 0;
        }

        
        
        
        if (C.quad_framenum > (float) GameBase.level.framenum) {
            S[Defines.STAT_TIMER_ICON] = (short) game_import_t
                    .imageindex("p_quad");
            S[Defines.STAT_TIMER] = (short) ((C.quad_framenum - (float) GameBase.level.framenum) / 10.0F);
        } else if (C.invincible_framenum > (float) GameBase.level.framenum) {
            S[Defines.STAT_TIMER_ICON] = (short) game_import_t
                    .imageindex("p_invulnerability");
            S[Defines.STAT_TIMER] = (short) ((C.invincible_framenum - (float) GameBase.level.framenum) / 10.0F);
        } else if (C.enviro_framenum > (float) GameBase.level.framenum) {
            S[Defines.STAT_TIMER_ICON] = (short) game_import_t
                    .imageindex("p_envirosuit");
            S[Defines.STAT_TIMER] = (short) ((C.enviro_framenum - (float) GameBase.level.framenum) / 10.0F);
        } else if (C.breather_framenum > (float) GameBase.level.framenum) {
            S[Defines.STAT_TIMER_ICON] = (short) game_import_t
                    .imageindex("p_rebreather");
            S[Defines.STAT_TIMER] = (short) ((C.breather_framenum - (float) GameBase.level.framenum) / 10.0F);
        } else {
            S[Defines.STAT_TIMER_ICON] = (short) 0;
            S[Defines.STAT_TIMER] = (short) 0;
        }

        
        
        
        
        if (C.pers.selected_item <= 0)
            S[Defines.STAT_SELECTED_ICON] = (short) 0;
        else
            S[Defines.STAT_SELECTED_ICON] = (short) game_import_t
                    .imageindex(GameItemList.itemlist[C.pers.selected_item].icon);

        S[Defines.STAT_SELECTED_ITEM] = (short) C.pers.selected_item;

        
        
        
        S[Defines.STAT_LAYOUTS] = (short) 0;

        if (GameBase.deathmatch.value != (float) 0) {
            if (C.pers.health <= 0
                    || GameBase.level.intermissiontime != (float) 0
                    || C.showscores)
                S[Defines.STAT_LAYOUTS] = (short) ((int) S[Defines.STAT_LAYOUTS] | 1);
            if (C.showinventory && C.pers.health > 0)
                S[Defines.STAT_LAYOUTS] = (short) ((int) S[Defines.STAT_LAYOUTS] | 2);
        } else {
            if (C.showscores || C.showhelp)
                S[Defines.STAT_LAYOUTS] = (short) ((int) S[Defines.STAT_LAYOUTS] | 1);
            if (C.showinventory && C.pers.health > 0)
                S[Defines.STAT_LAYOUTS] = (short) ((int) S[Defines.STAT_LAYOUTS] | 2);
        }

        
        
        
        S[Defines.STAT_FRAGS] = (short) C.resp.score;

        
        
        
        if (C.pers.helpchanged != 0
                && (GameBase.level.framenum & 8) != 0)
            S[Defines.STAT_HELPICON] = (short) game_import_t
                    .imageindex("i_help");
        else if ((C.pers.hand == Defines.CENTER_HANDED || C.ps.fov > 91.0F)
                && C.pers.weapon != null)
            S[Defines.STAT_HELPICON] = (short) game_import_t
                    .imageindex(C.pers.weapon.icon);
        else
            S[Defines.STAT_HELPICON] = (short) 0;

        S[Defines.STAT_SPECTATOR] = (short) 0;
    }

    /*
     * =============== 
     * G_CheckChaseStats 
     * ===============
     */
    public static void G_CheckChaseStats(edict_t ent) {

        for (int i = 1; (float) i <= GameBase.maxclients.value; i++) {
            gclient_t cl = GameBase.g_edicts[i].client;
            if (!GameBase.g_edicts[i].inuse || cl.chase_target != ent)
                continue;
            
            System.arraycopy(ent.client.ps.stats, 0, cl.ps.stats, 0,
                    Defines.MAX_STATS);

            G_SetSpectatorStats(GameBase.g_edicts[i]);
        }
    }

    /*
     * =============== 
     * G_SetSpectatorStats 
     * ===============
     */
    public static void G_SetSpectatorStats(edict_t ent) {
        gclient_t cl = ent.client;

        if (null == cl.chase_target)
            G_SetStats(ent);

        cl.ps.stats[Defines.STAT_SPECTATOR] = (short) 1;

        
        cl.ps.stats[Defines.STAT_LAYOUTS] = (short) 0;
        if (cl.pers.health <= 0 || GameBase.level.intermissiontime != (float) 0
                || cl.showscores)
            cl.ps.stats[Defines.STAT_LAYOUTS] = (short) ((int) cl.ps.stats[Defines.STAT_LAYOUTS] | 1);
        if (cl.showinventory && cl.pers.health > 0)
            cl.ps.stats[Defines.STAT_LAYOUTS] = (short) ((int) cl.ps.stats[Defines.STAT_LAYOUTS] | 2);

        if (cl.chase_target != null && cl.chase_target.inuse)
            
            
            cl.ps.stats[Defines.STAT_CHASE] = (short) (Defines.CS_PLAYERSKINS
                    + cl.chase_target.index - 1);
        else
            cl.ps.stats[Defines.STAT_CHASE] = (short) 0;
    }

    /** 
     * HelpComputer. Draws the help computer.
     */
    public static void HelpComputer(edict_t ent) {
        StringBuilder sb = new StringBuilder(256);
        String sk;
    
        if (GameBase.skill.value == (float) 0)
            sk = "easy";
        else if (GameBase.skill.value == 1.0F)
            sk = "medium";
        else if (GameBase.skill.value == 2.0F)
            sk = "hard";
        else
            sk = "hard+";
    
        
        sb.append("xv 32 yv 8 picn help "); 
        sb.append("xv 202 yv 12 string2 \"").append(sk).append("\" "); 
        sb.append("xv 0 yv 24 cstring2 \"").append(GameBase.level.level_name)
                .append("\" "); 
        sb.append("xv 0 yv 54 cstring2 \"").append(GameBase.game.helpmessage1)
                .append("\" "); 
        sb.append("xv 0 yv 110 cstring2 \"").append(GameBase.game.helpmessage2)
                .append("\" "); 
        sb.append("xv 50 yv 164 string2 \" kills     goals    secrets\" ");
        sb.append("xv 50 yv 172 string2 \"");
        sb.append(Com.sprintf("%3i/%3i     %i/%i       %i/%i\" ", new Vargs(6)
                .add(GameBase.level.killed_monsters).add(
                        GameBase.level.total_monsters).add(
                        GameBase.level.found_goals).add(
                        GameBase.level.total_goals).add(
                        GameBase.level.found_secrets).add(
                        GameBase.level.total_secrets)));
    
        game_import_t.WriteByte(Defines.svc_layout);
        game_import_t.WriteString(sb.toString());
        game_import_t.unicast(ent, true);
    }
}