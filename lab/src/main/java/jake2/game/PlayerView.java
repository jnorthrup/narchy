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
import jake2.Globals;
import jake2.game.monsters.M_Player;
import jake2.util.Lib;
import jake2.util.Math3D;

public class PlayerView {

    public static edict_t current_player;

    public static gclient_t current_client;

    public static final float[] forward = { 0, 0, 0 };

    public static final float[] right = { 0, 0, 0 };

    public static final float[] up = { 0, 0, 0 };

    /**
     * SV_CalcRoll.
     */
    public static float SV_CalcRoll(float[] angles, float[] velocity) {

        float side = Math3D.DotProduct(velocity, right);
        float sign = side < 0 ? -1 : 1;
        side = Math.abs(side);

        float value = GameBase.sv_rollangle.value;

        if (side < GameBase.sv_rollspeed.value)
            side = side * value / GameBase.sv_rollspeed.value;
        else
            side = value;

        return side * sign;
    }

    /*
     * =============== 
     * P_DamageFeedback
     * 
     * Handles color blends and view kicks 
     * ===============
     */

    public static void P_DamageFeedback(edict_t player) {
        float side;
        float[] v = { 0, 0, 0 };
        int r, l;
        float[] power_color = { 0.0f, 1.0f, 0.0f };
        float[] acolor = { 1.0f, 1.0f, 1.0f };
        float[] bcolor = { 1.0f, 0.0f, 0.0f };

        gclient_t client = player.client;

        
        client.ps.stats[Defines.STAT_FLASHES] = 0;
        if (client.damage_blood != 0)
            client.ps.stats[Defines.STAT_FLASHES] |= 1;
        if (client.damage_armor != 0
                && 0 == (player.flags & Defines.FL_GODMODE)
                && (client.invincible_framenum <= GameBase.level.framenum))
            client.ps.stats[Defines.STAT_FLASHES] |= 2;


        float count = (client.damage_blood + client.damage_armor + client.damage_parmor);

        if (count == 0)
            return; 

        
        if ((client.anim_priority < Defines.ANIM_PAIN)
                && (player.s.modelindex == 255)) {
            client.anim_priority = Defines.ANIM_PAIN;
            if ((client.ps.pmove.pm_flags & pmove_t.PMF_DUCKED) != 0) {
                player.s.frame = M_Player.FRAME_crpain1 - 1;
                client.anim_end = M_Player.FRAME_crpain4;
            } else {

                xxxi = (xxxi + 1) % 3;
                switch (xxxi) {
                case 0:
                    player.s.frame = M_Player.FRAME_pain101 - 1;
                    client.anim_end = M_Player.FRAME_pain104;
                    break;
                case 1:
                    player.s.frame = M_Player.FRAME_pain201 - 1;
                    client.anim_end = M_Player.FRAME_pain204;
                    break;
                case 2:
                    player.s.frame = M_Player.FRAME_pain301 - 1;
                    client.anim_end = M_Player.FRAME_pain304;
                    break;
                }
            }
        }

        float realcount = count;
        if (count < 10)
            count = 10; 

        
        if ((GameBase.level.time > player.pain_debounce_time)
                && 0 == (player.flags & Defines.FL_GODMODE)
                && (client.invincible_framenum <= GameBase.level.framenum)) {
            r = 1 + (Lib.rand() & 1);
            player.pain_debounce_time = GameBase.level.time + 0.7f;
            if (player.health < 25)
                l = 25;
            else if (player.health < 50)
                l = 50;
            else if (player.health < 75)
                l = 75;
            else
                l = 100;
            game_import_t.sound(player, Defines.CHAN_VOICE, game_import_t
                    .soundindex("*pain" + l + '_' + r + ".wav"), 1,
                    Defines.ATTN_NORM, 0);
        }

        
        if (client.damage_alpha < 0)
            client.damage_alpha = 0;
        client.damage_alpha += count * 0.01f;
        if (client.damage_alpha < 0.2f)
            client.damage_alpha = 0.2f;
        if (client.damage_alpha > 0.6f)
            client.damage_alpha = 0.6f; 

        
        
        
        
        Math3D.VectorClear(v);
        if (client.damage_parmor != 0)
            Math3D.VectorMA(v, (float) client.damage_parmor / realcount,
                    power_color, v);

        if (client.damage_armor != 0)
            Math3D.VectorMA(v, (float) client.damage_armor / realcount, acolor,
                    v);

        if (client.damage_blood != 0)
            Math3D.VectorMA(v, (float) client.damage_blood / realcount, bcolor,
                    v);
        Math3D.VectorCopy(v, client.damage_blend);


        float kick = Math.abs(client.damage_knockback);
        if (kick != 0 && player.health > 0) 
                                            
        {
            kick = kick * 100 / player.health;

            if (kick < count * 0.5)
                kick = count * 0.5f;
            if (kick > 50)
                kick = 50;

            Math3D.VectorSubtract(client.damage_from, player.s.origin, v);
            Math3D.VectorNormalize(v);

            side = Math3D.DotProduct(v, right);
            client.v_dmg_roll = kick * side * 0.3f;

            side = -Math3D.DotProduct(v, forward);
            client.v_dmg_pitch = kick * side * 0.3f;

            client.v_dmg_time = GameBase.level.time + Defines.DAMAGE_TIME;
        }

        
        
        
        client.damage_blood = 0;
        client.damage_armor = 0;
        client.damage_parmor = 0;
        client.damage_knockback = 0;
    }

    /**
     * 
     * fall from 128: 400 = 160000 
     * fall from 256: 580 = 336400 
     * fall from 384: 720 = 518400 
     * fall from 512: 800 = 640000 
     * fall from 640: 960 =  
     * damage = deltavelocity*deltavelocity * 0.0001
     */
    public static void SV_CalcViewOffset(edict_t ent) {
        float ratio;
        float delta;
        float[] v = { 0, 0, 0 };


        float[] angles = ent.client.ps.kick_angles;

        
        if (ent.deadflag != 0) {
            Math3D.VectorClear(angles);

            ent.client.ps.viewangles[Defines.ROLL] = 40;
            ent.client.ps.viewangles[Defines.PITCH] = -15;
            ent.client.ps.viewangles[Defines.YAW] = ent.client.killer_yaw;
        } else {
        	
            
            Math3D.VectorCopy(ent.client.kick_angles, angles);

            
            ratio = (ent.client.v_dmg_time - GameBase.level.time)
                    / Defines.DAMAGE_TIME;
            if (ratio < 0) {
                ratio = 0;
                ent.client.v_dmg_pitch = 0;
                ent.client.v_dmg_roll = 0;
            }
            angles[Defines.PITCH] += ratio * ent.client.v_dmg_pitch;
            angles[Defines.ROLL] += ratio * ent.client.v_dmg_roll;

            
            ratio = (ent.client.fall_time - GameBase.level.time)
                    / Defines.FALL_TIME;
            if (ratio < 0)
                ratio = 0;
            angles[Defines.PITCH] += ratio * ent.client.fall_value;

            
            delta = Math3D.DotProduct(ent.velocity, forward);
            angles[Defines.PITCH] += delta * GameBase.run_pitch.value;

            delta = Math3D.DotProduct(ent.velocity, right);
            angles[Defines.ROLL] += delta * GameBase.run_roll.value;

            
            delta = bobfracsin * GameBase.bob_pitch.value * xyspeed;
            if ((ent.client.ps.pmove.pm_flags & pmove_t.PMF_DUCKED) != 0)
                delta *= 6; 
            angles[Defines.PITCH] += delta;
            delta = bobfracsin * GameBase.bob_roll.value * xyspeed;
            if ((ent.client.ps.pmove.pm_flags & pmove_t.PMF_DUCKED) != 0)
                delta *= 6; 
            if ((bobcycle & 1) != 0)
                delta = -delta;
            angles[Defines.ROLL] += delta;
        }

        
        Math3D.VectorClear(v);

        
        v[2] += ent.viewheight;

        
        ratio = (ent.client.fall_time - GameBase.level.time)
                / Defines.FALL_TIME;
        if (ratio < 0)
            ratio = 0;
        v[2] -= ratio * ent.client.fall_value * 0.4;


        float bob = bobfracsin * xyspeed * GameBase.bob_up.value;
        if (bob > 6)
            bob = 6;
        
        
        v[2] += bob;

        

        Math3D.VectorAdd(v, ent.client.kick_origin, v);

        
        

        if (v[0] < -14)
            v[0] = -14;
        else if (v[0] > 14)
            v[0] = 14;
        if (v[1] < -14)
            v[1] = -14;
        else if (v[1] > 14)
            v[1] = 14;
        if (v[2] < -22)
            v[2] = -22;
        else if (v[2] > 30)
            v[2] = 30;

        Math3D.VectorCopy(v, ent.client.ps.viewoffset);
    }

    /**
     * Calculates where to draw the gun.
     */
    public static void SV_CalcGunOffset(edict_t ent) {
        int i;
        float delta;

        
        ent.client.ps.gunangles[Defines.ROLL] = xyspeed * bobfracsin * 0.005f;
        ent.client.ps.gunangles[Defines.YAW] = xyspeed * bobfracsin * 0.01f;
        if ((bobcycle & 1) != 0) {
            ent.client.ps.gunangles[Defines.ROLL] = -ent.client.ps.gunangles[Defines.ROLL];
            ent.client.ps.gunangles[Defines.YAW] = -ent.client.ps.gunangles[Defines.YAW];
        }

        ent.client.ps.gunangles[Defines.PITCH] = xyspeed * bobfracsin * 0.005f;

        
        for (i = 0; i < 3; i++) {
            delta = ent.client.oldviewangles[i] - ent.client.ps.viewangles[i];
            if (delta > 180)
                delta -= 360;
            if (delta < -180)
                delta += 360;
            if (delta > 45)
                delta = 45;
            if (delta < -45)
                delta = -45;
            if (i == Defines.YAW)
                ent.client.ps.gunangles[Defines.ROLL] += 0.1 * delta;
            ent.client.ps.gunangles[i] += 0.2 * delta;
        }

        
        Math3D.VectorClear(ent.client.ps.gunoffset);
        

        
        for (i = 0; i < 3; i++) {
            ent.client.ps.gunoffset[i] += forward[i] * (GameBase.gun_y.value);
            ent.client.ps.gunoffset[i] += right[i] * GameBase.gun_x.value;
            ent.client.ps.gunoffset[i] += up[i] * (-GameBase.gun_z.value);
        }
    }

    /**
     * Adds a blending effect to the clients view.
     */
    public static void SV_AddBlend(float r, float g, float b, float a,
                                   float[] v_blend) {

        if (a <= 0)
            return;
        float a2 = v_blend[3] + (1 - v_blend[3]) * a;
        float a3 = v_blend[3] / a2;

        v_blend[0] = v_blend[0] * a3 + r * (1 - a3);
        v_blend[1] = v_blend[1] * a3 + g * (1 - a3);
        v_blend[2] = v_blend[2] * a3 + b * (1 - a3);
        v_blend[3] = a2;
    }

    /**
     * Calculates the blending color according to the players environment.
     */
    public static void SV_CalcBlend(edict_t ent) {
        float[] vieworg = { 0, 0, 0 };
        int remaining;

        ent.client.ps.blend[0] = ent.client.ps.blend[1] = ent.client.ps.blend[2] = ent.client.ps.blend[3] = 0;

        
        Math3D.VectorAdd(ent.s.origin, ent.client.ps.viewoffset, vieworg);
        int contents = GameBase.gi.pointcontents.pointcontents(vieworg);
        if ((contents & (Defines.CONTENTS_LAVA | Defines.CONTENTS_SLIME | Defines.CONTENTS_WATER)) != 0)
            ent.client.ps.rdflags |= Defines.RDF_UNDERWATER;
        else
            ent.client.ps.rdflags &= ~Defines.RDF_UNDERWATER;

        if ((contents & (Defines.CONTENTS_SOLID | Defines.CONTENTS_LAVA)) != 0)
            SV_AddBlend(1.0f, 0.3f, 0.0f, 0.6f, ent.client.ps.blend);
        else if ((contents & Defines.CONTENTS_SLIME) != 0)
            SV_AddBlend(0.0f, 0.1f, 0.05f, 0.6f, ent.client.ps.blend);
        else if ((contents & Defines.CONTENTS_WATER) != 0)
            SV_AddBlend(0.5f, 0.3f, 0.2f, 0.4f, ent.client.ps.blend);

        
        if (ent.client.quad_framenum > GameBase.level.framenum) {
            remaining = (int) (ent.client.quad_framenum - GameBase.level.framenum);
            if (remaining == 30) 
                game_import_t.sound(ent, Defines.CHAN_ITEM,
                	game_import_t.soundindex("items/damage2.wav"), 1, Defines.ATTN_NORM, 0);
            if (remaining > 30 || (remaining & 4) != 0)
                SV_AddBlend(0, 0, 1, 0.08f, ent.client.ps.blend);
        } else if (ent.client.invincible_framenum > GameBase.level.framenum) {
            remaining = (int) ent.client.invincible_framenum - GameBase.level.framenum;
            if (remaining == 30) 
                game_import_t.sound(ent, Defines.CHAN_ITEM,
                	game_import_t.soundindex("items/protect2.wav"), 1, Defines.ATTN_NORM, 0);
            if (remaining > 30 || (remaining & 4) != 0)
                SV_AddBlend(1, 1, 0, 0.08f, ent.client.ps.blend);
        } else if (ent.client.enviro_framenum > GameBase.level.framenum) {
            remaining = (int) ent.client.enviro_framenum
                    - GameBase.level.framenum;
            if (remaining == 30) 
                game_import_t.sound(ent, Defines.CHAN_ITEM,
                		game_import_t.soundindex("items/airout.wav"), 1, Defines.ATTN_NORM, 0);
            if (remaining > 30 || (remaining & 4) != 0)
                SV_AddBlend(0, 1, 0, 0.08f, ent.client.ps.blend);
        } else if (ent.client.breather_framenum > GameBase.level.framenum) {
            remaining = (int) ent.client.breather_framenum
                    - GameBase.level.framenum;
            if (remaining == 30) 
                game_import_t.sound(ent, Defines.CHAN_ITEM, game_import_t
                        .soundindex("items/airout.wav"), 1, Defines.ATTN_NORM,
                        0);
            if (remaining > 30 || (remaining & 4) != 0)
                SV_AddBlend(0.4f, 1, 0.4f, 0.04f, ent.client.ps.blend);
        }

        
        if (ent.client.damage_alpha > 0)
            SV_AddBlend(ent.client.damage_blend[0], ent.client.damage_blend[1],
                    ent.client.damage_blend[2], ent.client.damage_alpha,
                    ent.client.ps.blend);

        if (ent.client.bonus_alpha > 0)
            SV_AddBlend(0.85f, 0.7f, 0.3f, ent.client.bonus_alpha,
                    ent.client.ps.blend);

        
        ent.client.damage_alpha -= 0.06;
        if (ent.client.damage_alpha < 0)
            ent.client.damage_alpha = 0;

        
        ent.client.bonus_alpha -= 0.1;
        if (ent.client.bonus_alpha < 0)
            ent.client.bonus_alpha = 0;
    }

    /**
     * Calculates damage and effect when a player falls down.
     */
    public static void P_FallingDamage(edict_t ent) {
        float delta;
        int damage;
        float[] dir = { 0, 0, 0 };

        if (ent.s.modelindex != 255)
            return; 

        if (ent.movetype == Defines.MOVETYPE_NOCLIP)
            return;

        if ((ent.client.oldvelocity[2] < 0)
                && (ent.velocity[2] > ent.client.oldvelocity[2])
                && (null == ent.groundentity)) {
            delta = ent.client.oldvelocity[2];
        } else {
            if (ent.groundentity == null)
                return;
            delta = ent.velocity[2] - ent.client.oldvelocity[2];
        }
        delta = delta * delta * 0.0001f;

        
        if (ent.waterlevel == 3)
            return;
        if (ent.waterlevel == 2)
            delta *= 0.25;
        if (ent.waterlevel == 1)
            delta *= 0.5;

        if (delta < 1)
            return;

        if (delta < 15) {
            ent.s.event = Defines.EV_FOOTSTEP;
            return;
        }

        ent.client.fall_value = delta * 0.5f;
        if (ent.client.fall_value > 40)
            ent.client.fall_value = 40;
        ent.client.fall_time = GameBase.level.time + Defines.FALL_TIME;

        if (delta > 30) {
            if (ent.health > 0) {
                if (delta >= 55)
                    ent.s.event = Defines.EV_FALLFAR;
                else
                    ent.s.event = Defines.EV_FALL;
            }
            ent.pain_debounce_time = GameBase.level.time; 
                                                          
            damage = (int) ((delta - 30) / 2);
            if (damage < 1)
                damage = 1;
            Math3D.VectorSet(dir, 0, 0, 1);

            if (GameBase.deathmatch.value == 0
                    || 0 == ((int) GameBase.dmflags.value & Defines.DF_NO_FALLING))
                GameCombat.T_Damage(ent, GameBase.g_edicts[0],
                        GameBase.g_edicts[0], dir, ent.s.origin,
                        Globals.vec3_origin, damage, 0, 0, Defines.MOD_FALLING);
        } else {
            ent.s.event = Defines.EV_FALLSHORT;
        }
    }

    /**
     * General effect handling for a player.
     */
    public static void P_WorldEffects() {

        if (current_player.movetype == Defines.MOVETYPE_NOCLIP) {
            current_player.air_finished = GameBase.level.time + 12; 
                                                                    
            return;
        }

        int waterlevel = current_player.waterlevel;
        int old_waterlevel = current_client.old_waterlevel;
        current_client.old_waterlevel = waterlevel;

        boolean breather = current_client.breather_framenum > GameBase.level.framenum;
        boolean envirosuit = current_client.enviro_framenum > GameBase.level.framenum;

        
        
        
        if (old_waterlevel == 0 && waterlevel != 0) {
            PlayerWeapon.PlayerNoise(current_player, current_player.s.origin,
                    Defines.PNOISE_SELF);
            if ((current_player.watertype & Defines.CONTENTS_LAVA) != 0)
                game_import_t.sound(current_player, Defines.CHAN_BODY,
                        game_import_t.soundindex("player/lava_in.wav"), 1,
                        Defines.ATTN_NORM, 0);
            else if ((current_player.watertype & Defines.CONTENTS_SLIME) != 0)
                game_import_t.sound(current_player, Defines.CHAN_BODY,
                        game_import_t.soundindex("player/watr_in.wav"), 1,
                        Defines.ATTN_NORM, 0);
            else if ((current_player.watertype & Defines.CONTENTS_WATER) != 0)
                game_import_t.sound(current_player, Defines.CHAN_BODY,
                        game_import_t.soundindex("player/watr_in.wav"), 1,
                        Defines.ATTN_NORM, 0);
            current_player.flags |= Defines.FL_INWATER;

            
            current_player.damage_debounce_time = GameBase.level.time - 1;
        }

        
        
        
        if (old_waterlevel != 0 && waterlevel == 0) {
            PlayerWeapon.PlayerNoise(current_player, current_player.s.origin,
                    Defines.PNOISE_SELF);
            game_import_t
                    .sound(current_player, Defines.CHAN_BODY, game_import_t
                            .soundindex("player/watr_out.wav"), 1,
                            Defines.ATTN_NORM, 0);
            current_player.flags &= ~Defines.FL_INWATER;
        }

        
        
        
        if (old_waterlevel != 3 && waterlevel == 3) {
            game_import_t.sound(current_player, Defines.CHAN_BODY, game_import_t
                    .soundindex("player/watr_un.wav"), 1, Defines.ATTN_NORM, 0);
        }

        
        
        
        if (old_waterlevel == 3 && waterlevel != 3) {
            if (current_player.air_finished < GameBase.level.time) { 
                                                                     
                game_import_t.sound(current_player, Defines.CHAN_VOICE,
                        game_import_t.soundindex("player/gasp1.wav"), 1,
                        Defines.ATTN_NORM, 0);
                PlayerWeapon.PlayerNoise(current_player, current_player.s.origin,
                        Defines.PNOISE_SELF);
            } else if (current_player.air_finished < GameBase.level.time + 11) { 
                                                                                 
                                                                                 
                game_import_t.sound(current_player, Defines.CHAN_VOICE,
                        game_import_t.soundindex("player/gasp2.wav"), 1,
                        Defines.ATTN_NORM, 0);
            }
        }

        
        
        
        if (waterlevel == 3) {
            
            if (breather || envirosuit) {
                current_player.air_finished = GameBase.level.time + 10;

                if (((int) (current_client.breather_framenum - GameBase.level.framenum) % 25) == 0) {
                    if (current_client.breather_sound == 0)
                        game_import_t.sound(current_player, Defines.CHAN_AUTO,
                                game_import_t.soundindex("player/u_breath1.wav"),
                                1, Defines.ATTN_NORM, 0);
                    else
                        game_import_t.sound(current_player, Defines.CHAN_AUTO,
                                game_import_t.soundindex("player/u_breath2.wav"),
                                1, Defines.ATTN_NORM, 0);
                    current_client.breather_sound ^= 1;
                    PlayerWeapon.PlayerNoise(current_player,
                            current_player.s.origin, Defines.PNOISE_SELF);
                    
                }
            }

            
            if (current_player.air_finished < GameBase.level.time) { 
                if (current_player.client.next_drown_time < GameBase.level.time
                        && current_player.health > 0) {
                    current_player.client.next_drown_time = GameBase.level.time + 1;

                    
                    current_player.dmg += 2;
                    if (current_player.dmg > 15)
                        current_player.dmg = 15;

                    
                    if (current_player.health <= current_player.dmg)
                        game_import_t.sound(current_player, Defines.CHAN_VOICE,
                                game_import_t.soundindex("player/drown1.wav"), 1,
                                Defines.ATTN_NORM, 0);
                    else if ((Lib.rand() & 1) != 0)
                        game_import_t.sound(current_player, Defines.CHAN_VOICE,
                                game_import_t.soundindex("*gurp1.wav"), 1,
                                Defines.ATTN_NORM, 0);
                    else
                        game_import_t.sound(current_player, Defines.CHAN_VOICE,
                                game_import_t.soundindex("*gurp2.wav"), 1,
                                Defines.ATTN_NORM, 0);

                    current_player.pain_debounce_time = GameBase.level.time;

                    GameCombat.T_Damage(current_player, GameBase.g_edicts[0],
                            GameBase.g_edicts[0], Globals.vec3_origin,
                            current_player.s.origin, Globals.vec3_origin,
                            current_player.dmg, 0, Defines.DAMAGE_NO_ARMOR,
                            Defines.MOD_WATER);
                }
            }
        } else {
            current_player.air_finished = GameBase.level.time + 12;
            current_player.dmg = 2;
        }

        
        
        
        if (waterlevel != 0
                && 0 != (current_player.watertype & (Defines.CONTENTS_LAVA | Defines.CONTENTS_SLIME))) {
            if ((current_player.watertype & Defines.CONTENTS_LAVA) != 0) {
                if (current_player.health > 0
                        && current_player.pain_debounce_time <= GameBase.level.time
                        && current_client.invincible_framenum < GameBase.level.framenum) {
                    if ((Lib.rand() & 1) != 0)
                        game_import_t.sound(current_player, Defines.CHAN_VOICE,
                                game_import_t.soundindex("player/burn1.wav"), 1,
                                Defines.ATTN_NORM, 0);
                    else
                        game_import_t.sound(current_player, Defines.CHAN_VOICE,
                                game_import_t.soundindex("player/burn2.wav"), 1,
                                Defines.ATTN_NORM, 0);
                    current_player.pain_debounce_time = GameBase.level.time + 1;
                }

                if (envirosuit) 
                    GameCombat.T_Damage(current_player, GameBase.g_edicts[0],
                            GameBase.g_edicts[0], Globals.vec3_origin,
                            current_player.s.origin, Globals.vec3_origin,
                            1 * waterlevel, 0, 0, Defines.MOD_LAVA);
                else
                    GameCombat.T_Damage(current_player, GameBase.g_edicts[0],
                            GameBase.g_edicts[0], Globals.vec3_origin,
                            current_player.s.origin, Globals.vec3_origin,
                            3 * waterlevel, 0, 0, Defines.MOD_LAVA);
            }

            if ((current_player.watertype & Defines.CONTENTS_SLIME) != 0) {
                if (!envirosuit) { 
                    GameCombat.T_Damage(current_player, GameBase.g_edicts[0],
                            GameBase.g_edicts[0], Globals.vec3_origin,
                            current_player.s.origin, Globals.vec3_origin,
                            1 * waterlevel, 0, 0, Defines.MOD_SLIME);
                }
            }
        }
    }

    /*
     * =============== 
     * G_SetClientEffects 
     * ===============
     */
    public static void G_SetClientEffects(edict_t ent) {
        int pa_type;
        int remaining;

        ent.s.effects = 0;
        ent.s.renderfx = 0;

        if (ent.health <= 0 || GameBase.level.intermissiontime != 0)
            return;

        if (ent.powerarmor_time > GameBase.level.time) {
            pa_type = GameItems.PowerArmorType(ent);
            if (pa_type == Defines.POWER_ARMOR_SCREEN) {
                ent.s.effects |= Defines.EF_POWERSCREEN;
            } else if (pa_type == Defines.POWER_ARMOR_SHIELD) {
                ent.s.effects |= Defines.EF_COLOR_SHELL;
                ent.s.renderfx |= Defines.RF_SHELL_GREEN;
            }
        }

        if (ent.client.quad_framenum > GameBase.level.framenum) {
            remaining = (int) ent.client.quad_framenum
                    - GameBase.level.framenum;
            if (remaining > 30 || 0 != (remaining & 4))
                ent.s.effects |= Defines.EF_QUAD;
        }

        if (ent.client.invincible_framenum > GameBase.level.framenum) {
            remaining = (int) ent.client.invincible_framenum
                    - GameBase.level.framenum;
            if (remaining > 30 || 0 != (remaining & 4))
                ent.s.effects |= Defines.EF_PENT;
        }

        
        if ((ent.flags & Defines.FL_GODMODE) != 0) {
            ent.s.effects |= Defines.EF_COLOR_SHELL;
            ent.s.renderfx |= (Defines.RF_SHELL_RED | Defines.RF_SHELL_GREEN | Defines.RF_SHELL_BLUE);
        }
    }

    /*
     * =============== 
     * G_SetClientEvent 
     * ===============
     */
    public static void G_SetClientEvent(edict_t ent) {
        if (ent.s.event != 0)
            return;

        if (ent.groundentity != null && xyspeed > 225) {
            if ((int) (current_client.bobtime + bobmove) != bobcycle)
                ent.s.event = Defines.EV_FOOTSTEP;
        }
    }

    /*
     * =============== 
     * G_SetClientSound 
     * ===============
     */
    public static void G_SetClientSound(edict_t ent) {
        String weap;

        if (ent.client.pers.game_helpchanged != GameBase.game.helpchanged) {
            ent.client.pers.game_helpchanged = GameBase.game.helpchanged;
            ent.client.pers.helpchanged = 1;
        }

        
        if (ent.client.pers.helpchanged != 0
                && ent.client.pers.helpchanged <= 3
                && 0 == (GameBase.level.framenum & 63)) {
            ent.client.pers.helpchanged++;
            game_import_t.sound(ent, Defines.CHAN_VOICE, game_import_t
                    .soundindex("misc/pc_up.wav"), 1, Defines.ATTN_STATIC, 0);
        }

        if (ent.client.pers.weapon != null)
            weap = ent.client.pers.weapon.classname;
        else
            weap = "";

        if (ent.waterlevel != 0
                && 0 != (ent.watertype & (Defines.CONTENTS_LAVA | Defines.CONTENTS_SLIME)))
            ent.s.sound = GameBase.snd_fry;
        else if (Lib.strcmp(weap, "weapon_railgun") == 0)
            ent.s.sound = game_import_t.soundindex("weapons/rg_hum.wav");
        else if (Lib.strcmp(weap, "weapon_bfg") == 0)
            ent.s.sound = game_import_t.soundindex("weapons/bfg_hum.wav");
        else if (ent.client.weapon_sound != 0)
            ent.s.sound = ent.client.weapon_sound;
        else
            ent.s.sound = 0;
    }

    /*
     * =============== 
     * G_SetClientFrame 
     * ===============
     */
    public static void G_SetClientFrame(edict_t ent) {

        if (ent.s.modelindex != 255)
            return;

        gclient_t client = ent.client;

        boolean duck = (client.ps.pmove.pm_flags & pmove_t.PMF_DUCKED) != 0;
        boolean run = xyspeed != 0;

        boolean skip = false;
        
        if (duck != client.anim_duck
                && client.anim_priority < Defines.ANIM_DEATH)
            skip = true;

        if (run != client.anim_run
                && client.anim_priority == Defines.ANIM_BASIC)
            skip = true;

        if (null == ent.groundentity
                && client.anim_priority <= Defines.ANIM_WAVE)
            skip = true;

        if (!skip) {
            if (client.anim_priority == Defines.ANIM_REVERSE) {
                if (ent.s.frame > client.anim_end) {
                    ent.s.frame--;
                    return;
                }
            } else if (ent.s.frame < client.anim_end) { 
                ent.s.frame++;
                return;
            }

            if (client.anim_priority == Defines.ANIM_DEATH)
                return; 
            if (client.anim_priority == Defines.ANIM_JUMP) {
                if (null == ent.groundentity)
                    return; 
                ent.client.anim_priority = Defines.ANIM_WAVE;
                ent.s.frame = M_Player.FRAME_jump3;
                ent.client.anim_end = M_Player.FRAME_jump6;
                return;
            }
        }

        
        client.anim_priority = Defines.ANIM_BASIC;
        client.anim_duck = duck;
        client.anim_run = run;

        if (null == ent.groundentity) {
            client.anim_priority = Defines.ANIM_JUMP;
            if (ent.s.frame != M_Player.FRAME_jump2)
                ent.s.frame = M_Player.FRAME_jump1;
            client.anim_end = M_Player.FRAME_jump2;
        } else if (run) { 
            if (duck) {
                ent.s.frame = M_Player.FRAME_crwalk1;
                client.anim_end = M_Player.FRAME_crwalk6;
            } else {
                ent.s.frame = M_Player.FRAME_run1;
                client.anim_end = M_Player.FRAME_run6;
            }
        } else { 
            if (duck) {
                ent.s.frame = M_Player.FRAME_crstnd01;
                client.anim_end = M_Player.FRAME_crstnd19;
            } else {
                ent.s.frame = M_Player.FRAME_stand01;
                client.anim_end = M_Player.FRAME_stand40;
            }
        }
    }

    
    /**
     * Called for each player at the end of the server frame and right after
     * spawning.
     */
    public static void ClientEndServerFrame(edict_t ent) {
        int i;

        current_player = ent;
        current_client = ent.client;

        
        
        
        
        
        
        
        
        for (i = 0; i < 3; i++) {
            current_client.ps.pmove.origin[i] = (short) (ent.s.origin[i] * 8.0);
            current_client.ps.pmove.velocity[i] = (short) (ent.velocity[i] * 8.0);
        }

        
        
        
        
        if (GameBase.level.intermissiontime != 0) {
            
            current_client.ps.blend[3] = 0;
            current_client.ps.fov = 90;
            PlayerHud.G_SetStats(ent);
            return;
        }

        Math3D.AngleVectors(ent.client.v_angle, forward, right, up);

        
        P_WorldEffects();

        
        
        
        
        if (ent.client.v_angle[Defines.PITCH] > 180)
            ent.s.angles[Defines.PITCH] = (-360 + ent.client.v_angle[Defines.PITCH]) / 3;
        else
            ent.s.angles[Defines.PITCH] = ent.client.v_angle[Defines.PITCH] / 3;
        ent.s.angles[Defines.YAW] = ent.client.v_angle[Defines.YAW];
        ent.s.angles[Defines.ROLL] = 0;
        ent.s.angles[Defines.ROLL] = SV_CalcRoll(ent.s.angles, ent.velocity) * 4;

        
        
        
        
        xyspeed = (float) Math.sqrt(ent.velocity[0] * ent.velocity[0]
                + ent.velocity[1] * ent.velocity[1]);

        if (xyspeed < 5) {
            bobmove = 0;
            current_client.bobtime = 0; 
        } else if (ent.groundentity != null) { 
                                               
            if (xyspeed > 210)
                bobmove = 0.25f;
            else if (xyspeed > 100)
                bobmove = 0.125f;
            else
                bobmove = 0.0625f;
        }

        float bobtime = (current_client.bobtime += bobmove);

        if ((current_client.ps.pmove.pm_flags & pmove_t.PMF_DUCKED) != 0)
            bobtime *= 4;

        bobcycle = (int) bobtime;
        bobfracsin = (float) Math.abs(Math.sin(bobtime * Math.PI));

        
        P_FallingDamage(ent);

        
        P_DamageFeedback(ent);

        
        SV_CalcViewOffset(ent);

        
        SV_CalcGunOffset(ent);

        
        
        
        
        
        SV_CalcBlend(ent);

        
        if (ent.client.resp.spectator)
            PlayerHud.G_SetSpectatorStats(ent);
        else
            PlayerHud.G_SetStats(ent);
        PlayerHud.G_CheckChaseStats(ent);

        G_SetClientEvent(ent);

        G_SetClientEffects(ent);

        G_SetClientSound(ent);

        G_SetClientFrame(ent);

        Math3D.VectorCopy(ent.velocity, ent.client.oldvelocity);
        Math3D.VectorCopy(ent.client.ps.viewangles, ent.client.oldviewangles);

        
        Math3D.VectorClear(ent.client.kick_origin);
        Math3D.VectorClear(ent.client.kick_angles);

        
        if (ent.client.showscores && 0 == (GameBase.level.framenum & 31)) {
            PlayerHud.DeathmatchScoreboardMessage(ent, ent.enemy);
            game_import_t.unicast(ent, false);
        }
    }

    public static float xyspeed;

    public static float bobmove;

    public static int bobcycle; 

    public static float bobfracsin; 

    private static int xxxi;
}