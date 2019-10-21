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
import jake2.game.monsters.M_Infantry;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.util.Objects;
import java.util.function.Supplier;

public class GameTurret {

    public static void AnglesNormalize(float[] vec) {
        while (vec[0] > 360.0F)
            vec[0] -= 360.0F;
        while (vec[0] < (float) 0)
            vec[0] += 360.0F;
        while (vec[1] > 360.0F)
            vec[1] -= 360.0F;
        while (vec[1] < (float) 0)
            vec[1] += 360.0F;
    }

    public static float SnapToEights(float x) {
        x = (float) ((double) x * 8.0);
        if ((double) x > 0.0)
            x = (float) ((double) x + 0.5);
        else
            x = (float) ((double) x - 0.5);
        return 0.125f * (float) (int) x;
    }

    /**
     * QUAKED turret_breach (0 0 0) ? This portion of the turret can change both
     * pitch and yaw. The model should be made with a flat pitch. It (and the
     * associated base) need to be oriented towards 0. Use "angle" to set the
     * starting angle.
     * 
     * "speed" default 50 "dmg" default 10 "angle" point this forward "target"
     * point this at an info_notnull at the muzzle tip "minpitch" min acceptable
     * pitch angle : default -30 "maxpitch" max acceptable pitch angle : default
     * 30 "minyaw" min acceptable yaw angle : default 0 "maxyaw" max acceptable
     * yaw angle : default 360
     */

    public static void turret_breach_fire(edict_t self) {
        float[] f = {(float) 0, (float) 0, (float) 0}, r = {(float) 0, (float) 0, (float) 0}, u = {(float) 0, (float) 0, (float) 0};

        Math3D.AngleVectors(self.s.angles, f, r, u);
        float[] start = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorMA(self.s.origin, self.move_origin[0], f, start);
        Math3D.VectorMA(start, self.move_origin[1], r, start);
        Math3D.VectorMA(start, self.move_origin[2], u, start);

        int damage = (int) (100.0F + Lib.random() * 50.0F);
        int speed = (int) (550.0F + 50.0F * GameBase.skill.value);
        GameWeapon.fire_rocket(self.teammaster.owner, start, f, damage, speed, 150.0F,
                damage);
        game_import_t.positioned_sound(start, self, Defines.CHAN_WEAPON,
                game_import_t.soundindex("weapons/rocklf1a.wav"), 1.0F,
                (float) Defines.ATTN_NORM, (float) 0);
    }

    public static void SP_turret_breach(edict_t self) {
        self.solid = Defines.SOLID_BSP;
        self.movetype = Defines.MOVETYPE_PUSH;
        game_import_t.setmodel(self, self.model);

        if (self.speed == (float) 0)
            self.speed = 50.0F;
        if (self.dmg == 0)
            self.dmg = 10;

        if (GameBase.st.minpitch == (float) 0)
            GameBase.st.minpitch = -30.0F;
        if (GameBase.st.maxpitch == (float) 0)
            GameBase.st.maxpitch = 30.0F;
        if (GameBase.st.maxyaw == (float) 0)
            GameBase.st.maxyaw = 360.0F;

        self.pos1[Defines.PITCH] = -1.0F * GameBase.st.minpitch;
        self.pos1[Defines.YAW] = GameBase.st.minyaw;
        self.pos2[Defines.PITCH] = -1.0F * GameBase.st.maxpitch;
        self.pos2[Defines.YAW] = GameBase.st.maxyaw;

        self.ideal_yaw = self.s.angles[Defines.YAW];
        self.move_angles[Defines.YAW] = self.ideal_yaw;

        self.blocked = turret_blocked;

        self.think = turret_breach_finish_init;
        self.nextthink = GameBase.level.time + Defines.FRAMETIME;
        game_import_t.linkentity(self);
    }

    /**
     * QUAKED turret_base (0 0 0) ? This portion of the turret changes yaw only.
     * MUST be teamed with a turret_breach.
     */

    public static void SP_turret_base(edict_t self) {
        self.solid = Defines.SOLID_BSP;
        self.movetype = Defines.MOVETYPE_PUSH;
        game_import_t.setmodel(self, self.model);
        self.blocked = turret_blocked;
        game_import_t.linkentity(self);
    }

    public static void SP_turret_driver(edict_t self) {
        if (GameBase.deathmatch.value != (float) 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        self.movetype = Defines.MOVETYPE_PUSH;
        self.solid = Defines.SOLID_BBOX;
        self.s.modelindex = game_import_t
                .modelindex("models/monsters/infantry/tris.md2");
        Math3D.VectorSet(self.mins, -16.0F, -16.0F, -24.0F);
        Math3D.VectorSet(self.maxs, 16.0F, 16.0F, 32.0F);

        self.health = 100;
        self.gib_health = 0;
        self.mass = 200;
        self.viewheight = 24;

        self.die = turret_driver_die;
        self.monsterinfo.stand = M_Infantry.infantry_stand;

        self.flags |= Defines.FL_NO_KNOCKBACK;

        GameBase.level.total_monsters++;

        self.svflags |= Defines.SVF_MONSTER;
        self.s.renderfx |= Defines.RF_FRAMELERP;
        self.takedamage = Defines.DAMAGE_AIM;
        self.use = GameUtil.monster_use;
        self.clipmask = Defines.MASK_MONSTERSOLID;
        Math3D.VectorCopy(self.s.origin, self.s.old_origin);
        self.monsterinfo.aiflags |= Defines.AI_STAND_GROUND | Defines.AI_DUCKED;

        if (GameBase.st.item != null) {
            self.item = GameItems.FindItemByClassname(GameBase.st.item);
            if (self.item == null)
                game_import_t.dprintf(self.classname + " at "
                        + Lib.vtos(self.s.origin) + " has bad item: "
                        + GameBase.st.item + '\n');
        }

        self.think = turret_driver_link;
        self.nextthink = GameBase.level.time + Defines.FRAMETIME;

        game_import_t.linkentity(self);
    }

    static final EntBlockedAdapter turret_blocked = new EntBlockedAdapter() {
    	@Override
        public String getID() { return "turret_blocked"; }
        @Override
        public void blocked(edict_t self, edict_t other) {

            if (other.takedamage != 0) {
                edict_t attacker = Objects.requireNonNullElseGet(self.teammaster.owner, new Supplier<edict_t>() {
                    @Override
                    public edict_t get() {
                        return self.teammaster;
                    }
                });
                GameCombat.T_Damage(other, self, attacker, Globals.vec3_origin,
                        other.s.origin, Globals.vec3_origin,
                        self.teammaster.dmg, 10, 0, Defines.MOD_CRUSH);
            }
        }
    };

    static final EntThinkAdapter turret_breach_think = new EntThinkAdapter() {
    	@Override
        public String getID() { return "turret_breach_think"; }
        @Override
        public boolean think(edict_t self) {

            float[] current_angles = {(float) 0, (float) 0, (float) 0};

            Math3D.VectorCopy(self.s.angles, current_angles);
            AnglesNormalize(current_angles);

            AnglesNormalize(self.move_angles);
            if (self.move_angles[Defines.PITCH] > 180.0F)
                self.move_angles[Defines.PITCH] -= 360.0F;

            
            if (self.move_angles[Defines.PITCH] > self.pos1[Defines.PITCH])
                self.move_angles[Defines.PITCH] = self.pos1[Defines.PITCH];
            else if (self.move_angles[Defines.PITCH] < self.pos2[Defines.PITCH])
                self.move_angles[Defines.PITCH] = self.pos2[Defines.PITCH];

            if ((self.move_angles[Defines.YAW] < self.pos1[Defines.YAW])
                    || (self.move_angles[Defines.YAW] > self.pos2[Defines.YAW])) {

                float dmin = Math.abs(self.pos1[Defines.YAW]
                        - self.move_angles[Defines.YAW]);
                if (dmin < -180.0F)
                    dmin += 360.0F;
                else if (dmin > 180.0F)
                    dmin -= 360.0F;
                float dmax = Math.abs(self.pos2[Defines.YAW]
                        - self.move_angles[Defines.YAW]);
                if (dmax < -180.0F)
                    dmax += 360.0F;
                else if (dmax > 180.0F)
                    dmax -= 360.0F;
                if (Math.abs(dmin) < Math.abs(dmax))
                    self.move_angles[Defines.YAW] = self.pos1[Defines.YAW];
                else
                    self.move_angles[Defines.YAW] = self.pos2[Defines.YAW];
            }

            float[] delta = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorSubtract(self.move_angles, current_angles, delta);
            if (delta[0] < -180.0F)
                delta[0] += 360.0F;
            else if (delta[0] > 180.0F)
                delta[0] -= 360.0F;
            if (delta[1] < -180.0F)
                delta[1] += 360.0F;
            else if (delta[1] > 180.0F)
                delta[1] -= 360.0F;
            delta[2] = (float) 0;

            if (delta[0] > self.speed * Defines.FRAMETIME)
                delta[0] = self.speed * Defines.FRAMETIME;
            if (delta[0] < -1.0F * self.speed * Defines.FRAMETIME)
                delta[0] = -1.0F * self.speed * Defines.FRAMETIME;
            if (delta[1] > self.speed * Defines.FRAMETIME)
                delta[1] = self.speed * Defines.FRAMETIME;
            if (delta[1] < -1.0F * self.speed * Defines.FRAMETIME)
                delta[1] = -1.0F * self.speed * Defines.FRAMETIME;

            Math3D.VectorScale(delta, 1.0f / Defines.FRAMETIME, self.avelocity);

            self.nextthink = GameBase.level.time + Defines.FRAMETIME;

            for (edict_t ent = self.teammaster; ent != null; ent = ent.teamchain)
                ent.avelocity[1] = self.avelocity[1];

            
            if (self.owner != null) {


                self.owner.avelocity[0] = self.avelocity[0];
                self.owner.avelocity[1] = self.avelocity[1];


                float angle = self.s.angles[1] + self.owner.move_origin[1];
                angle = (float) ((double) angle * (Math.PI * 2.0 / 360.0));
                float[] target = {(float) 0, (float) 0, (float) 0};
                target[0] = GameTurret.SnapToEights((float) ((double) self.s.origin[0] +
                			Math.cos((double) angle) * (double) self.owner.move_origin[0]));
                target[1] = GameTurret.SnapToEights((float) ((double) self.s.origin[1] +
                			Math.sin((double) angle) * (double) self.owner.move_origin[0]));
                target[2] = self.owner.s.origin[2];

                float[] dir = {(float) 0, (float) 0, (float) 0};
                Math3D.VectorSubtract(target, self.owner.s.origin, dir);
                self.owner.velocity[0] = dir[0] * 1.0f / Defines.FRAMETIME;
                self.owner.velocity[1] = dir[1] * 1.0f / Defines.FRAMETIME;

                
                angle = self.s.angles[Defines.PITCH] * (float) (Math.PI * 2 / 360);
                float target_z = GameTurret.SnapToEights((float) ((double) self.s.origin[2]
                        + (double) self.owner.move_origin[0] * Math.tan((double) angle) + (double) self.owner.move_origin[2]));

                float diff = target_z - self.owner.s.origin[2];
                self.owner.velocity[2] = diff * 1.0f / Defines.FRAMETIME;

                if ((self.spawnflags & 65536) != 0) {
                    turret_breach_fire(self);
                    self.spawnflags &= ~65536;
                }
            }
            return true;
        }
    };

    static final EntThinkAdapter turret_breach_finish_init = new EntThinkAdapter() {
    	@Override
        public String getID() { return "turret_breach_finish_init"; }
        @Override
        public boolean think(edict_t self) {

            
            if (self.target == null) {
                game_import_t.dprintf(self.classname + " at "
                        + Lib.vtos(self.s.origin) + " needs a target\n");
            } else {
                self.target_ent = GameBase.G_PickTarget(self.target);
                Math3D.VectorSubtract(self.target_ent.s.origin, self.s.origin,
                        self.move_origin);
                GameUtil.G_FreeEdict(self.target_ent);
            }

            self.teammaster.dmg = self.dmg;
            self.think = turret_breach_think;
            self.think.think(self);
            return true;
        }
    };

    /*
     * QUAKED turret_driver (1 .5 0) (-16 -16 -24) (16 16 32) Must NOT be on the
     * team with the rest of the turret parts. Instead it must target the
     * turret_breach.
     */
    static final EntDieAdapter turret_driver_die = new EntDieAdapter() {
    	@Override
        public String getID() { return "turret_driver_die"; }
        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {


            self.target_ent.move_angles[0] = (float) 0;


            edict_t ent;
            for (ent = self.target_ent.teammaster; ent.teamchain != self; ent = ent.teamchain)
                ;
            ent.teamchain = null;
            self.teammaster = null;
            self.flags &= ~Defines.FL_TEAMSLAVE;

            self.target_ent.owner = null;
            self.target_ent.teammaster.owner = null;

            M_Infantry.infantry_die.die(self, inflictor, attacker, damage, null);
        }
    };

    static final EntThinkAdapter turret_driver_think = new EntThinkAdapter() {
    	@Override
        public String getID() { return "turret_driver_think"; }
        @Override
        public boolean think(edict_t self) {

            self.nextthink = GameBase.level.time + Defines.FRAMETIME;

            if (self.enemy != null
                    && (!self.enemy.inuse || self.enemy.health <= 0))
                self.enemy = null;

            if (null == self.enemy) {
                if (!GameUtil.FindTarget(self))
                    return true;
                self.monsterinfo.trail_time = GameBase.level.time;
                self.monsterinfo.aiflags &= ~Defines.AI_LOST_SIGHT;
            } else {
                if (GameUtil.visible(self, self.enemy)) {
                    if ((self.monsterinfo.aiflags & Defines.AI_LOST_SIGHT) != 0) {
                        self.monsterinfo.trail_time = GameBase.level.time;
                        self.monsterinfo.aiflags &= ~Defines.AI_LOST_SIGHT;
                    }
                } else {
                    self.monsterinfo.aiflags |= Defines.AI_LOST_SIGHT;
                    return true;
                }
            }


            float[] target = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorCopy(self.enemy.s.origin, target);
            target[2] = target[2] + (float) self.enemy.viewheight;
            float[] dir = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorSubtract(target, self.target_ent.s.origin, dir);
            Math3D.vectoangles(dir, self.target_ent.move_angles);

            
            if (GameBase.level.time < self.monsterinfo.attack_finished)
                return true;

            float reaction_time = (3.0F - GameBase.skill.value) * 1.0f;
            if ((GameBase.level.time - self.monsterinfo.trail_time) < reaction_time)
                return true;

            self.monsterinfo.attack_finished = GameBase.level.time
                    + reaction_time + 1.0f;
            
            self.target_ent.spawnflags |= 65536;
            return true;
        }
    };

    public static final EntThinkAdapter turret_driver_link = new EntThinkAdapter() {
    	@Override
        public String getID() { return "turret_driver_link"; }
        @Override
        public boolean think(edict_t self) {

            self.think = turret_driver_think;
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;

            self.target_ent = GameBase.G_PickTarget(self.target);
            self.target_ent.owner = self;
            self.target_ent.teammaster.owner = self;
            Math3D.VectorCopy(self.target_ent.s.angles, self.s.angles);

            float[] vec = {(float) 0, (float) 0, (float) 0};
            vec[0] = self.target_ent.s.origin[0] - self.s.origin[0];
            vec[1] = self.target_ent.s.origin[1] - self.s.origin[1];
            vec[2] = (float) 0;
            self.move_origin[0] = Math3D.VectorLength(vec);

            Math3D.VectorSubtract(self.s.origin, self.target_ent.s.origin, vec);
            Math3D.vectoangles(vec, vec);
            AnglesNormalize(vec);
            
            self.move_origin[1] = vec[1];
            self.move_origin[2] = self.s.origin[2] - self.target_ent.s.origin[2];


            edict_t ent;
            for (ent = self.target_ent.teammaster; ent.teamchain != null; ent = ent.teamchain)
                ;
            ent.teamchain = self;
            self.teammaster = self.target_ent.teammaster;
            self.flags |= Defines.FL_TEAMSLAVE;
            return true;
        }
    };
}