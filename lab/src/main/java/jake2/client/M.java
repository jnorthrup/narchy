/*
 * M.java
 * Copyright (C) 2003
 * 
 * $Id: M.java,v 1.9 2006-01-21 21:53:32 salomo Exp $
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
package jake2.client;

import jake2.Defines;
import jake2.Globals;
import jake2.game.*;
import jake2.server.SV;
import jake2.util.Lib;
import jake2.util.Math3D;

/**
 * M
 */
public final class M {

    public static void M_CheckGround(edict_t ent) {

        if ((ent.flags & (Defines.FL_SWIM | Defines.FL_FLY)) != 0)
            return;

        if (ent.velocity[2] > 100) {
            ent.groundentity = null;
            return;
        }


        float[] point = {0, 0, 0};
        point[0] = ent.s.origin[0];
        point[1] = ent.s.origin[1];
        point[2] = ent.s.origin[2] - 0.25f;

        var trace = game_import_t.trace(ent.s.origin, ent.mins, ent.maxs, point, ent,
                Defines.MASK_MONSTERSOLID);

        
        if (trace.plane.normal[2] < 0.7 && !trace.startsolid) {
            ent.groundentity = null;
            return;
        }

        
        
        
        
        if (!trace.startsolid && !trace.allsolid) {
            Math3D.VectorCopy(trace.endpos, ent.s.origin);
            ent.groundentity = trace.ent;
            ent.groundentity_linkcount = trace.ent.linkcount;
            ent.velocity[2] = 0;
        }
    }
    
    /**
     * Returns false if any part of the bottom of the entity is off an edge that
     * is not a staircase.
     */

    public static boolean M_CheckBottom(edict_t ent) {
        float[] mins = { 0, 0, 0 };

        Math3D.VectorAdd(ent.s.origin, ent.mins, mins);
        float[] maxs = {0, 0, 0};
        Math3D.VectorAdd(ent.s.origin, ent.maxs, maxs);


        float[] start = {0, 0, 0};
        start[2] = mins[2] - 1;
        int x;
        for (x = 0; x <= 1; x++)

            start[0] = x != 0 ? maxs[0] : mins[0];

        float[] stop = {0, 0, 0};
        for (var y = 0; y <= 1; y++) {

                start[1] = y != 0 ? maxs[1] : mins[1];
                if (GameBase.gi.pointcontents.pointcontents(start) != Defines.CONTENTS_SOLID) {
                    GameBase.c_no++;
                    
                    
                    
                    start[2] = mins[2];

                    
                    start[0] = stop[0] = (mins[0] + maxs[0]) * 0.5f;
                    start[1] = stop[1] = (mins[1] + maxs[1]) * 0.5f;
                    stop[2] = start[2] - 2 * GameBase.STEPSIZE;
                    var trace = game_import_t.trace(start, Globals.vec3_origin,
                            Globals.vec3_origin, stop, ent,
                            Defines.MASK_MONSTERSOLID);

                    if (trace.fraction == 1.0)
                        return false;
                    float bottom;
                    var mid = bottom = trace.endpos[2];


                    for (x = 0; x <= 1; x++)

                        start[0] = stop[0] = x != 0 ? maxs[0] : mins[0];

                        for (y = 0; y <= 1; y++) {
                            start[1] = stop[1] = y != 0 ? maxs[1] : mins[1];

                            trace = game_import_t.trace(start,
                                    Globals.vec3_origin, Globals.vec3_origin,
                                    stop, ent, Defines.MASK_MONSTERSOLID);

                            var te2 = trace.endpos[2];
                            if (trace.fraction != 1.0
                                    && te2 > bottom)
                                bottom = te2;
                            if (trace.fraction == 1.0
                                    || mid - te2 > GameBase.STEPSIZE)
                                return false;
                        }

                    GameBase.c_yes++;
                    return true;
                }
            }

        GameBase.c_yes++;
        return true; 
    }

    /** 
     * M_ChangeYaw.
     */
    public static void M_ChangeYaw(edict_t ent) {

        var current = Math3D.anglemod(ent.s.angles[Defines.YAW]);
        var ideal = ent.ideal_yaw;

        if (current == ideal)
            return;

        var move = ideal - current;
        var speed = ent.yaw_speed;
        if (ideal > current) {
            if (move >= 180)
                move -= 360;
        } else {
            if (move <= -180)
                move += 360;
        }
        if (move > 0) {
            if (move > speed)
                move = speed;
        } else {
            if (move < -speed)
                move = -speed;
        }

        ent.s.angles[Defines.YAW] = Math3D.anglemod(current + move);
    }

    /**
     * M_MoveToGoal.
     */
    public static void M_MoveToGoal(edict_t ent, float dist) {
        var goal = ent.goalentity;

        if (ent.groundentity == null
                && (ent.flags & (Defines.FL_FLY | Defines.FL_SWIM)) == 0)
            return;

        
        if (ent.enemy != null && SV.SV_CloseEnough(ent, ent.enemy, dist))
            return;

        
        if ((Lib.rand() & 3) == 1
                || !SV.SV_StepDirection(ent, ent.ideal_yaw, dist)) {
            if (ent.inuse)
                SV.SV_NewChaseDir(ent, goal, dist);
        }
    }

    /** 
     * M_walkmove.
     */
    public static boolean M_walkmove(edict_t ent, float yaw, float dist) {

        if ((ent.groundentity == null)
                && (ent.flags & (Defines.FL_FLY | Defines.FL_SWIM)) == 0)
            return false;

        yaw = (float) (yaw * Math.PI * 2 / 360);

        float[] move = {0, 0, 0};
        move[0] = (float) Math.cos(yaw) * dist;
        move[1] = (float) Math.sin(yaw) * dist;
        move[2] = 0;

        return SV.SV_movestep(ent, move, true);
    }

    public static void M_CatagorizePosition(edict_t ent) {
        float[] point = { 0, 0, 0 };


        point[0] = ent.s.origin[0];
        point[1] = ent.s.origin[1];
        point[2] = ent.s.origin[2] + ent.mins[2] + 1;
        var cont = GameBase.gi.pointcontents.pointcontents(point);

        if (0 == (cont & Defines.MASK_WATER)) {
            ent.waterlevel = 0;
            ent.watertype = 0;
            return;
        }

        ent.watertype = cont;
        ent.waterlevel = 1;
        point[2] += 26;
        cont = GameBase.gi.pointcontents.pointcontents(point);
        if (0 == (cont & Defines.MASK_WATER))
            return;

        ent.waterlevel = 2;
        point[2] += 22;
        cont = GameBase.gi.pointcontents.pointcontents(point);
        if (0 != (cont & Defines.MASK_WATER))
            ent.waterlevel = 3;
    }

    public static void M_WorldEffects(edict_t ent) {

        if (ent.health > 0) {
            int dmg;
            if (0 == (ent.flags & Defines.FL_SWIM)) {
                if (ent.waterlevel < 3) {
                    ent.air_finished = GameBase.level.time + 12;
                } else if (ent.air_finished < GameBase.level.time) {
                    
                    if (ent.pain_debounce_time < GameBase.level.time) {
                        dmg = (int) (2f + 2f * Math.floor(GameBase.level.time
                                - ent.air_finished));
                        if (dmg > 15)
                            dmg = 15;
                        GameCombat.T_Damage(ent, GameBase.g_edicts[0],
                                GameBase.g_edicts[0], Globals.vec3_origin,
                                ent.s.origin, Globals.vec3_origin, dmg, 0,
                                Defines.DAMAGE_NO_ARMOR, Defines.MOD_WATER);
                        ent.pain_debounce_time = GameBase.level.time + 1;
                    }
                }
            } else {
                if (ent.waterlevel > 0) {
                    ent.air_finished = GameBase.level.time + 9;
                } else if (ent.air_finished < GameBase.level.time) {
                    
                    if (ent.pain_debounce_time < GameBase.level.time) {
                        dmg = (int) (2 + 2 * Math.floor(GameBase.level.time
                                - ent.air_finished));
                        if (dmg > 15)
                            dmg = 15;
                        GameCombat.T_Damage(ent, GameBase.g_edicts[0],
                                GameBase.g_edicts[0], Globals.vec3_origin,
                                ent.s.origin, Globals.vec3_origin, dmg, 0,
                                Defines.DAMAGE_NO_ARMOR, Defines.MOD_WATER);
                        ent.pain_debounce_time = GameBase.level.time + 1;
                    }
                }
            }
        }

        if (ent.waterlevel == 0) {
            if ((ent.flags & Defines.FL_INWATER) != 0) {
                game_import_t.sound(ent, Defines.CHAN_BODY, game_import_t
                        .soundindex("player/watr_out.wav"), 1,
                        Defines.ATTN_NORM, 0);
                ent.flags &= ~Defines.FL_INWATER;
            }
            return;
        }

        if ((ent.watertype & Defines.CONTENTS_LAVA) != 0
                && 0 == (ent.flags & Defines.FL_IMMUNE_LAVA)) {
            if (ent.damage_debounce_time < GameBase.level.time) {
                ent.damage_debounce_time = GameBase.level.time + 0.2f;
                GameCombat.T_Damage(ent, GameBase.g_edicts[0],
                        GameBase.g_edicts[0], Globals.vec3_origin,
                        ent.s.origin, Globals.vec3_origin, 10 * ent.waterlevel,
                        0, 0, Defines.MOD_LAVA);
            }
        }
        if ((ent.watertype & Defines.CONTENTS_SLIME) != 0
                && 0 == (ent.flags & Defines.FL_IMMUNE_SLIME)) {
            if (ent.damage_debounce_time < GameBase.level.time) {
                ent.damage_debounce_time = GameBase.level.time + 1;
                GameCombat.T_Damage(ent, GameBase.g_edicts[0],
                        GameBase.g_edicts[0], Globals.vec3_origin,
                        ent.s.origin, Globals.vec3_origin, 4 * ent.waterlevel,
                        0, 0, Defines.MOD_SLIME);
            }
        }

        if (0 == (ent.flags & Defines.FL_INWATER)) {
            if (0 == (ent.svflags & Defines.SVF_DEADMONSTER)) {
                if ((ent.watertype & Defines.CONTENTS_LAVA) != 0)
                    if (Globals.rnd.nextFloat() <= 0.5)
                        game_import_t.sound(ent, Defines.CHAN_BODY, game_import_t
                                .soundindex("player/lava1.wav"), 1,
                                Defines.ATTN_NORM, 0);
                    else
                        game_import_t.sound(ent, Defines.CHAN_BODY, game_import_t
                                .soundindex("player/lava2.wav"), 1,
                                Defines.ATTN_NORM, 0);
                else if ((ent.watertype & Defines.CONTENTS_SLIME) != 0)
                    game_import_t.sound(ent, Defines.CHAN_BODY, game_import_t
                            .soundindex("player/watr_in.wav"), 1,
                            Defines.ATTN_NORM, 0);
                else if ((ent.watertype & Defines.CONTENTS_WATER) != 0)
                    game_import_t.sound(ent, Defines.CHAN_BODY, game_import_t
                            .soundindex("player/watr_in.wav"), 1,
                            Defines.ATTN_NORM, 0);
            }

            ent.flags |= Defines.FL_INWATER;
            ent.damage_debounce_time = 0;
        }
    }

    public static final EntThinkAdapter M_droptofloor = new EntThinkAdapter() {
        @Override
        public String getID() { return "m_drop_to_floor";}
        @Override
        public boolean think(edict_t ent) {

            ent.s.origin[2] += 1;
            float[] end = {0, 0, 0};
            Math3D.VectorCopy(ent.s.origin, end);
            end[2] -= 256;

            var trace = game_import_t.trace(ent.s.origin, ent.mins, ent.maxs, end,
                    ent, Defines.MASK_MONSTERSOLID);

            if (trace.fraction == 1 || trace.allsolid)
                return true;

            Math3D.VectorCopy(trace.endpos, ent.s.origin);

            game_import_t.linkentity(ent);
            M.M_CheckGround(ent);
            M_CatagorizePosition(ent);
            return true;
        }
    };

    public static void M_SetEffects(edict_t ent) {
        ent.s.effects &= ~(Defines.EF_COLOR_SHELL | Defines.EF_POWERSCREEN);
        ent.s.renderfx &= ~(Defines.RF_SHELL_RED | Defines.RF_SHELL_GREEN | Defines.RF_SHELL_BLUE);

        if ((ent.monsterinfo.aiflags & Defines.AI_RESURRECTING) != 0) {
            ent.s.effects |= Defines.EF_COLOR_SHELL;
            ent.s.renderfx |= Defines.RF_SHELL_RED;
        }

        if (ent.health <= 0)
            return;

        if (ent.powerarmor_time > GameBase.level.time) {
            switch (ent.monsterinfo.power_armor_type) {
                case Defines.POWER_ARMOR_SCREEN:
                    ent.s.effects |= Defines.EF_POWERSCREEN;
                    break;
                case Defines.POWER_ARMOR_SHIELD:
                    ent.s.effects |= Defines.EF_COLOR_SHELL;
                    ent.s.renderfx |= Defines.RF_SHELL_GREEN;
                    break;
            }
        }
    }

    
    public static void M_MoveFrame(edict_t self) {

        var move = self.monsterinfo.currentmove;
        self.nextthink = GameBase.level.time + Defines.FRAMETIME;

        if ((self.monsterinfo.nextframe != 0)
                && (self.monsterinfo.nextframe >= move.firstframe)
                && (self.monsterinfo.nextframe <= move.lastframe)) {
            self.s.frame = self.monsterinfo.nextframe;
            self.monsterinfo.nextframe = 0;
        } else {
            if (self.s.frame == move.lastframe) {
                if (move.endfunc != null) {
                    move.endfunc.think(self);

                    
                    move = self.monsterinfo.currentmove;

                    
                    if ((self.svflags & Defines.SVF_DEADMONSTER) != 0)
                        return;
                }
            }

            if (self.s.frame < move.firstframe || self.s.frame > move.lastframe) {
                self.monsterinfo.aiflags &= ~Defines.AI_HOLD_FRAME;
                self.s.frame = move.firstframe;
            } else {
                if (0 == (self.monsterinfo.aiflags & Defines.AI_HOLD_FRAME)) {
                    self.s.frame++;
                    if (self.s.frame > move.lastframe)
                        self.s.frame = move.firstframe;
                }
            }
        }

        var index = self.s.frame - move.firstframe;
        if (move.frame[index].ai != null)
            if (0 == (self.monsterinfo.aiflags & Defines.AI_HOLD_FRAME))
                move.frame[index].ai.ai(self, move.frame[index].dist
                        * self.monsterinfo.scale);
            else
                move.frame[index].ai.ai(self, 0);

        if (move.frame[index].think != null)
            move.frame[index].think.think(self);
    }

    /** Stops the Flies. */
    public static final EntThinkAdapter M_FliesOff = new EntThinkAdapter() {
        @Override
        public String getID() { return "m_fliesoff";}
        @Override
        public boolean think(edict_t self) {
            self.s.effects &= ~Defines.EF_FLIES;
            self.s.sound = 0;
            return true;
        }
    };

    /** Starts the Flies as setting the animation flag in the entity. */
    public static final EntThinkAdapter M_FliesOn = new EntThinkAdapter() {
        @Override
        public String getID() { return "m_flies_on";}
        @Override
        public boolean think(edict_t self) {
            if (self.waterlevel != 0)
                return true;

            self.s.effects |= Defines.EF_FLIES;
            self.s.sound = game_import_t.soundindex("infantry/inflies1.wav");
            self.think = M_FliesOff;
            self.nextthink = GameBase.level.time + 60;
            return true;
        }
    };

    /** Adds some flies after a random time */
    public static final EntThinkAdapter M_FlyCheck = new EntThinkAdapter() {
        @Override
        public String getID() { return "m_fly_check";}
        @Override
        public boolean think(edict_t self) {

            if (self.waterlevel != 0)
                return true;

            if (Globals.rnd.nextFloat() > 0.5)
                return true;

            self.think = M_FliesOn;
            self.nextthink = GameBase.level.time + 5 + 10
                    * Globals.rnd.nextFloat();
            return true;
        }
    };
}