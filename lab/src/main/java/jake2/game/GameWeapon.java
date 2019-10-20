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



package jake2.game;

import jake2.Defines;
import jake2.Globals;
import jake2.util.Lib;
import jake2.util.Math3D;


public class GameWeapon {

    static final EntTouchAdapter blaster_touch = new EntTouchAdapter() {
    	@Override
        public String getID() { return "blaster_touch"; }
    
        @Override
        public void touch(edict_t self, edict_t other, cplane_t plane,
                          csurface_t surf) {

            if (other == self.owner)
                return;
    
            if (surf != null && (surf.flags & Defines.SURF_SKY) != 0) {
                GameUtil.G_FreeEdict(self);
                return;
            }
    
            if (self.owner.client != null)
                PlayerWeapon.PlayerNoise(self.owner, self.s.origin,
                        Defines.PNOISE_IMPACT);
    
            if (other.takedamage != 0) {
                int mod;
                if ((self.spawnflags & 1) != 0)
                    mod = Defines.MOD_HYPERBLASTER;
                else
                    mod = Defines.MOD_BLASTER;
    
                
                float[] normal;
                if (plane == null)
                    normal = new float[3];
                else
                    normal = plane.normal;
    
                GameCombat.T_Damage(other, self, self.owner, self.velocity,
                        self.s.origin, normal, self.dmg, 1,
                        Defines.DAMAGE_ENERGY, mod);
    
            } else {
                game_import_t.WriteByte(Defines.svc_temp_entity);
                game_import_t.WriteByte(Defines.TE_BLASTER);
                game_import_t.WritePosition(self.s.origin);
                if (plane == null)
                    game_import_t.WriteDir(Globals.vec3_origin);
                else
                    game_import_t.WriteDir(plane.normal);
                game_import_t.multicast(self.s.origin, Defines.MULTICAST_PVS);
            }
    
            GameUtil.G_FreeEdict(self);
        }
    };
    
    static final EntThinkAdapter Grenade_Explode = new EntThinkAdapter() {
    	@Override
        public String getID() { return "Grenade_Explode"; }
        @Override
        public boolean think(edict_t ent) {

            if (ent.owner.client != null)
                PlayerWeapon.PlayerNoise(ent.owner, ent.s.origin,
                        Defines.PNOISE_IMPACT);


            int mod;
            if (ent.enemy != null) {
                float[] v = { 0, 0, 0 };

                Math3D.VectorAdd(ent.enemy.mins, ent.enemy.maxs, v);
                Math3D.VectorMA(ent.enemy.s.origin, 0.5f, v, v);
                Math3D.VectorSubtract(ent.s.origin, v, v);
                var points = ent.dmg - 0.5f * Math3D.VectorLength(v);
                float[] dir = {0, 0, 0};
                Math3D.VectorSubtract(ent.enemy.s.origin, ent.s.origin, dir);
                if ((ent.spawnflags & 1) != 0)
                    mod = Defines.MOD_HANDGRENADE;
                else
                    mod = Defines.MOD_GRENADE;
                GameCombat.T_Damage(ent.enemy, ent, ent.owner, dir, ent.s.origin,
                        Globals.vec3_origin, (int) points, (int) points,
                        Defines.DAMAGE_RADIUS, mod);
            }
    
            if ((ent.spawnflags & 2) != 0)
                mod = Defines.MOD_HELD_GRENADE;
            else if ((ent.spawnflags & 1) != 0)
                mod = Defines.MOD_HG_SPLASH;
            else
                mod = Defines.MOD_G_SPLASH;
            GameCombat.T_RadiusDamage(ent, ent.owner, ent.dmg, ent.enemy,
                    ent.dmg_radius, mod);

            float[] origin = {0, 0, 0};
            Math3D.VectorMA(ent.s.origin, -0.02f, ent.velocity, origin);
            game_import_t.WriteByte(Defines.svc_temp_entity);
            if (ent.waterlevel != 0) {
                if (ent.groundentity != null)
                    game_import_t.WriteByte(Defines.TE_GRENADE_EXPLOSION_WATER);
                else
                    game_import_t.WriteByte(Defines.TE_ROCKET_EXPLOSION_WATER);
            } else {
                if (ent.groundentity != null)
                    game_import_t.WriteByte(Defines.TE_GRENADE_EXPLOSION);
                else
                    game_import_t.WriteByte(Defines.TE_ROCKET_EXPLOSION);
            }
            game_import_t.WritePosition(origin);
            game_import_t.multicast(ent.s.origin, Defines.MULTICAST_PHS);
    
            GameUtil.G_FreeEdict(ent);
            return true;
        }
    };
    static final EntTouchAdapter Grenade_Touch = new EntTouchAdapter() {
    	@Override
        public String getID() { return "Grenade_Touch"; }
        @Override
        public void touch(edict_t ent, edict_t other, cplane_t plane,
                          csurface_t surf) {
            if (other == ent.owner)
                return;
    
            if (surf != null && 0 != (surf.flags & Defines.SURF_SKY)) {
                GameUtil.G_FreeEdict(ent);
                return;
            }
    
            if (other.takedamage == 0) {
                if ((ent.spawnflags & 1) != 0) {
                    if (Lib.random() > 0.5f)
                        game_import_t.sound(ent, Defines.CHAN_VOICE, game_import_t
                                .soundindex("weapons/hgrenb1a.wav"), 1,
                                Defines.ATTN_NORM, 0);
                    else
                        game_import_t.sound(ent, Defines.CHAN_VOICE, game_import_t
                                .soundindex("weapons/hgrenb2a.wav"), 1,
                                Defines.ATTN_NORM, 0);
                } else {
                    game_import_t.sound(ent, Defines.CHAN_VOICE, game_import_t
                            .soundindex("weapons/grenlb1b.wav"), 1,
                            Defines.ATTN_NORM, 0);
                }
                return;
            }
    
            ent.enemy = other;
            Grenade_Explode.think(ent);
        }
    };
    
    /*
     * ================= 
     * fire_rocket 
     * =================
     */
    static final EntTouchAdapter rocket_touch = new EntTouchAdapter() {
    	@Override
        public String  getID() { return "rocket_touch"; }
        @Override
        public void touch(edict_t ent, edict_t other, cplane_t plane,
                          csurface_t surf) {

            if (other == ent.owner)
                return;
    
            if (surf != null && (surf.flags & Defines.SURF_SKY) != 0) {
                GameUtil.G_FreeEdict(ent);
                return;
            }
    
            if (ent.owner.client != null)
                PlayerWeapon.PlayerNoise(ent.owner, ent.s.origin,
                        Defines.PNOISE_IMPACT);


            float[] origin = {0, 0, 0};
            Math3D.VectorMA(ent.s.origin, -0.02f, ent.velocity, origin);
    
            if (other.takedamage != 0) {
                GameCombat.T_Damage(other, ent, ent.owner, ent.velocity,
                        ent.s.origin, plane.normal, ent.dmg, 0, 0,
                        Defines.MOD_ROCKET);
            } else {
                
                if (GameBase.deathmatch.value == 0 && 0 == GameBase.coop.value) {
                    if ((surf != null)
                            && 0 == (surf.flags & (Defines.SURF_WARP
                                    | Defines.SURF_TRANS33
                                    | Defines.SURF_TRANS66 | Defines.SURF_FLOWING))) {
                        var n = Lib.rand() % 5;
                        while (n-- > 0)
                            GameMisc.ThrowDebris(ent,
                                    "models/objects/debris2/tris.md2", 2,
                                    ent.s.origin);
                    }
                }
            }
    
            GameCombat.T_RadiusDamage(ent, ent.owner, ent.radius_dmg, other,
                    ent.dmg_radius, Defines.MOD_R_SPLASH);
    
            game_import_t.WriteByte(Defines.svc_temp_entity);
            if (ent.waterlevel != 0)
                game_import_t.WriteByte(Defines.TE_ROCKET_EXPLOSION_WATER);
            else
                game_import_t.WriteByte(Defines.TE_ROCKET_EXPLOSION);
            game_import_t.WritePosition(origin);
            game_import_t.multicast(ent.s.origin, Defines.MULTICAST_PHS);
    
            GameUtil.G_FreeEdict(ent);
        }
    };
    /*
     * ================= 
     * fire_bfg 
     * =================
     */
    static final EntThinkAdapter bfg_explode = new EntThinkAdapter() {
    	@Override
        public String getID() { return "bfg_explode"; }
        @Override
        public boolean think(edict_t self) {

            if (self.s.frame == 0) {

                edict_t ent = null;
                EdictIterator edit = null;
                float[] v = {0, 0, 0};
                while ((edit = GameBase.findradius(edit, self.s.origin,
                        self.dmg_radius)) != null) {
                    ent = edit.o;
                    if (ent.takedamage == 0)
                        continue;
                    if (ent == self.owner)
                        continue;
                    if (!GameCombat.CanDamage(ent, self))
                        continue;
                    if (!GameCombat.CanDamage(ent, self.owner))
                        continue;
    
                    Math3D.VectorAdd(ent.mins, ent.maxs, v);
                    Math3D.VectorMA(ent.s.origin, 0.5f, v, v);
                    Math3D.VectorSubtract(self.s.origin, v, v);
                    var dist = Math3D.VectorLength(v);
                    var points = (float) (self.radius_dmg * (1.0 - Math.sqrt(dist
                            / self.dmg_radius)));
                    if (ent == self.owner)
                        points *= 0.5f;
    
                    game_import_t.WriteByte(Defines.svc_temp_entity);
                    game_import_t.WriteByte(Defines.TE_BFG_EXPLOSION);
                    game_import_t.WritePosition(ent.s.origin);
                    game_import_t.multicast(ent.s.origin, Defines.MULTICAST_PHS);
                    GameCombat.T_Damage(ent, self, self.owner, self.velocity,
                            ent.s.origin, Globals.vec3_origin, (int) points, 0,
                            Defines.DAMAGE_ENERGY, Defines.MOD_BFG_EFFECT);
                }
            }
    
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;
            self.s.frame++;
            if (self.s.frame == 5)
                self.think = GameUtil.G_FreeEdictA;
            return true;
    
        }
    };
    
    static final EntTouchAdapter bfg_touch = new EntTouchAdapter() {
    	@Override
        public String getID() { return "bfg_touch"; }
        @Override
        public void touch(edict_t self, edict_t other, cplane_t plane,
                          csurface_t surf) {
            if (other == self.owner)
                return;
    
            if (surf != null && (surf.flags & Defines.SURF_SKY) != 0) {
                GameUtil.G_FreeEdict(self);
                return;
            }
    
            if (self.owner.client != null)
                PlayerWeapon.PlayerNoise(self.owner, self.s.origin,
                        Defines.PNOISE_IMPACT);
    
            
            if (other.takedamage != 0)
                GameCombat.T_Damage(other, self, self.owner, self.velocity,
                        self.s.origin, plane.normal, 200, 0, 0,
                        Defines.MOD_BFG_BLAST);
            GameCombat.T_RadiusDamage(self, self.owner, 200, other, 100,
                    Defines.MOD_BFG_BLAST);
    
            game_import_t.sound(self, Defines.CHAN_VOICE, game_import_t
                    .soundindex("weapons/bfg__x1b.wav"), 1, Defines.ATTN_NORM,
                    0);
            self.solid = Defines.SOLID_NOT;
            self.touch = null;
            Math3D.VectorMA(self.s.origin, -1 * Defines.FRAMETIME,
                    self.velocity, self.s.origin);
            Math3D.VectorClear(self.velocity);
            self.s.modelindex = game_import_t.modelindex("sprites/s_bfg3.sp2");
            self.s.frame = 0;
            self.s.sound = 0;
            self.s.effects &= ~Defines.EF_ANIM_ALLFAST;
            self.think = bfg_explode;
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;
            self.enemy = other;
    
            game_import_t.WriteByte(Defines.svc_temp_entity);
            game_import_t.WriteByte(Defines.TE_BFG_BIGEXPLOSION);
            game_import_t.WritePosition(self.s.origin);
            game_import_t.multicast(self.s.origin, Defines.MULTICAST_PVS);
        }
    };
    
    static final EntThinkAdapter bfg_think = new EntThinkAdapter() {
    	@Override
        public String getID() { return "bfg_think"; }
        @Override
        public boolean think(edict_t self) {
            int dmg;

            if (GameBase.deathmatch.value != 0)
                dmg = 5;
            else
                dmg = 10;
    
            EdictIterator edit = null;
            float[] end = {0, 0, 0};
            float[] start = {0, 0, 0};
            float[] dir = {0, 0, 0};
            float[] point = {0, 0, 0};
            while ((edit = GameBase.findradius(edit, self.s.origin, 256)) != null) {
                var ent = edit.o;

                if (ent == self)
                    continue;
    
                if (ent == self.owner)
                    continue;
    
                if (ent.takedamage == 0)
                    continue;
    
                if (0 == (ent.svflags & Defines.SVF_MONSTER)
                        && (null == ent.client)
                        && (Lib.strcmp(ent.classname, "misc_explobox") != 0))
                    continue;
    
                Math3D.VectorMA(ent.absmin, 0.5f, ent.size, point);
    
                Math3D.VectorSubtract(point, self.s.origin, dir);
                Math3D.VectorNormalize(dir);

                var ignore = self;
                Math3D.VectorCopy(self.s.origin, start);
                Math3D.VectorMA(start, 2048, dir, end);
                trace_t tr;
                while (true) {
                    tr = game_import_t.trace(start, null, null, end, ignore,
                            Defines.CONTENTS_SOLID | Defines.CONTENTS_MONSTER
                                    | Defines.CONTENTS_DEADMONSTER);
    
                    if (null == tr.ent)
                        break;
    
                    
                    if ((tr.ent.takedamage != 0)
                            && 0 == (tr.ent.flags & Defines.FL_IMMUNE_LASER)
                            && (tr.ent != self.owner))
                        GameCombat.T_Damage(tr.ent, self, self.owner, dir,
                                tr.endpos, Globals.vec3_origin, dmg, 1,
                                Defines.DAMAGE_ENERGY, Defines.MOD_BFG_LASER);
    
                    
                    
                    if (0 == (tr.ent.svflags & Defines.SVF_MONSTER)
                            && (null == tr.ent.client)) {
                        game_import_t.WriteByte(Defines.svc_temp_entity);
                        game_import_t.WriteByte(Defines.TE_LASER_SPARKS);
                        game_import_t.WriteByte(4);
                        game_import_t.WritePosition(tr.endpos);
                        game_import_t.WriteDir(tr.plane.normal);
                        game_import_t.WriteByte(self.s.skinnum);
                        game_import_t.multicast(tr.endpos, Defines.MULTICAST_PVS);
                        break;
                    }
    
                    ignore = tr.ent;
                    Math3D.VectorCopy(tr.endpos, start);
                }
    
                game_import_t.WriteByte(Defines.svc_temp_entity);
                game_import_t.WriteByte(Defines.TE_BFG_LASER);
                game_import_t.WritePosition(self.s.origin);
                game_import_t.WritePosition(tr.endpos);
                game_import_t.multicast(self.s.origin, Defines.MULTICAST_PHS);
            }
    
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;
            return true;
        }
    };

    /*
     * ================= 
     * check_dodge
     * 
     * This is a support routine used when a client is firing a non-instant
     * attack weapon. It checks to see if a monster's dodge function should be
     * called. 
     * =================
     */
    static void check_dodge(edict_t self, float[] start, float[] dir, int speed) {


        if (GameBase.skill.value == 0) {
            if (Lib.random() > 0.25)
                return;
        }
        float[] end = {0, 0, 0};
        Math3D.VectorMA(start, 8192, dir, end);
        var tr = game_import_t.trace(start, null, null, end, self, Defines.MASK_SHOT);
        if ((tr.ent != null) && (tr.ent.svflags & Defines.SVF_MONSTER) != 0
                && (tr.ent.health > 0) && (null != tr.ent.monsterinfo.dodge)
                && GameUtil.infront(tr.ent, self)) {
            float[] v = {0, 0, 0};
            Math3D.VectorSubtract(tr.endpos, start, v);
            var eta = (Math3D.VectorLength(v) - tr.ent.maxs[0]) / speed;
            tr.ent.monsterinfo.dodge.dodge(tr.ent, self, eta);
        }
    }

    /*
     * ================= 
     * fire_hit
     * 
     * Used for all impact (hit/punch/slash) attacks 
     * =================
     */
    public static boolean fire_hit(edict_t self, float[] aim, int damage,
            int kick) {
        float[] dir = { 0, 0, 0 };
    
        
        Math3D.VectorSubtract(self.enemy.s.origin, self.s.origin, dir);
        var range = Math3D.VectorLength(dir);
        if (range > aim[0])
            return false;
    
        if (aim[1] > self.mins[0] && aim[1] < self.maxs[0]) {
            
            
            range -= self.enemy.maxs[0];
        } else {
            
            
            if (aim[1] < 0)
                aim[1] = self.enemy.mins[0];
            else
                aim[1] = self.enemy.maxs[0];
        }

        float[] point = {0, 0, 0};
        Math3D.VectorMA(self.s.origin, range, dir, point);

        var tr = game_import_t.trace(self.s.origin, null, null, point, self,
                Defines.MASK_SHOT);
        if (tr.fraction < 1) {
            if (0 == tr.ent.takedamage)
                return false;
            
            
            if ((tr.ent.svflags & Defines.SVF_MONSTER) != 0
                    || (tr.ent.client != null))
                tr.ent = self.enemy;
        }

        float[] up = {0, 0, 0};
        float[] right = {0, 0, 0};
        float[] forward = {0, 0, 0};
        Math3D.AngleVectors(self.s.angles, forward, right, up);
        Math3D.VectorMA(self.s.origin, range, forward, point);
        Math3D.VectorMA(point, aim[1], right, point);
        Math3D.VectorMA(point, aim[2], up, point);
        Math3D.VectorSubtract(point, self.enemy.s.origin, dir);
    
        
        GameCombat.T_Damage(tr.ent, self, self, dir, point, Globals.vec3_origin,
                damage, kick / 2, Defines.DAMAGE_NO_KNOCKBACK, Defines.MOD_HIT);
    
        if (0 == (tr.ent.svflags & Defines.SVF_MONSTER)
                && (null == tr.ent.client))
            return false;


        float[] v = {0, 0, 0};
        Math3D.VectorMA(self.enemy.absmin, 0.5f, self.enemy.size, v);
        Math3D.VectorSubtract(v, point, v);
        Math3D.VectorNormalize(v);
        Math3D.VectorMA(self.enemy.velocity, kick, v, self.enemy.velocity);
        if (self.enemy.velocity[2] > 0)
            self.enemy.groundentity = null;
        return true;
    }

    /*
     * ================= 
     * fire_lead
     * 
     * This is an internal support routine used for bullet/pellet based weapons.
     * =================
     */
    public static void fire_lead(edict_t self, float[] start, float[] aimdir,
            int damage, int kick, int te_impact, int hspread, int vspread,
            int mod) {
        float[] dir = { 0, 0, 0 };
        float[] water_start = { 0, 0, 0 };
        var water = false;

        var tr = game_import_t.trace(self.s.origin, null, null, start, self,
                Defines.MASK_SHOT);
        if (!(tr.fraction < 1.0)) {
            Math3D.vectoangles(aimdir, dir);
            float[] up = {0, 0, 0};
            float[] right = {0, 0, 0};
            float[] forward = {0, 0, 0};
            Math3D.AngleVectors(dir, forward, right, up);

            var r = Lib.crandom() * hspread;
            var u = Lib.crandom() * vspread;
            float[] end = {0, 0, 0};
            Math3D.VectorMA(start, 8192, forward, end);
            Math3D.VectorMA(end, r, right, end);
            Math3D.VectorMA(end, u, up, end);

            var content_mask = Defines.MASK_SHOT | Defines.MASK_WATER;
            if ((GameBase.gi.pointcontents.pointcontents(start) & Defines.MASK_WATER) != 0) {
                water = true;
                Math3D.VectorCopy(start, water_start);
                content_mask &= ~Defines.MASK_WATER;
            }
    
            tr = game_import_t.trace(start, null, null, end, self, content_mask);
    
            
            if ((tr.contents & Defines.MASK_WATER) != 0) {

                water = true;
                Math3D.VectorCopy(tr.endpos, water_start);
    
                if (!Math3D.VectorEquals(start, tr.endpos)) {
                    int color;
                    if ((tr.contents & Defines.CONTENTS_WATER) != 0) {
                        if (Lib.strcmp(tr.surface.name, "*brwater") == 0)
                            color = Defines.SPLASH_BROWN_WATER;
                        else
                            color = Defines.SPLASH_BLUE_WATER;
                    } else if ((tr.contents & Defines.CONTENTS_SLIME) != 0)
                        color = Defines.SPLASH_SLIME;
                    else if ((tr.contents & Defines.CONTENTS_LAVA) != 0)
                        color = Defines.SPLASH_LAVA;
                    else
                        color = Defines.SPLASH_UNKNOWN;
    
                    if (color != Defines.SPLASH_UNKNOWN) {
                        game_import_t.WriteByte(Defines.svc_temp_entity);
                        game_import_t.WriteByte(Defines.TE_SPLASH);
                        game_import_t.WriteByte(8);
                        game_import_t.WritePosition(tr.endpos);
                        game_import_t.WriteDir(tr.plane.normal);
                        game_import_t.WriteByte(color);
                        game_import_t.multicast(tr.endpos, Defines.MULTICAST_PVS);
                    }
    
                    
                    Math3D.VectorSubtract(end, start, dir);
                    Math3D.vectoangles(dir, dir);
                    Math3D.AngleVectors(dir, forward, right, up);
                    r = Lib.crandom() * hspread * 2;
                    u = Lib.crandom() * vspread * 2;
                    Math3D.VectorMA(water_start, 8192, forward, end);
                    Math3D.VectorMA(end, r, right, end);
                    Math3D.VectorMA(end, u, up, end);
                }
    
                
                tr = game_import_t.trace(water_start, null, null, end, self,
                        Defines.MASK_SHOT);
            }
        }
    
        
        if (!((tr.surface != null) && 0 != (tr.surface.flags & Defines.SURF_SKY))) {
            if (tr.fraction < 1.0) {
                if (tr.ent.takedamage != 0) {
                    GameCombat.T_Damage(tr.ent, self, self, aimdir, tr.endpos,
                            tr.plane.normal, damage, kick,
                            Defines.DAMAGE_BULLET, mod);
                } else {
                    if (!"sky".equals(tr.surface.name)) {
                        game_import_t.WriteByte(Defines.svc_temp_entity);
                        game_import_t.WriteByte(te_impact);
                        game_import_t.WritePosition(tr.endpos);
                        game_import_t.WriteDir(tr.plane.normal);
                        game_import_t.multicast(tr.endpos, Defines.MULTICAST_PVS);
    
                        if (self.client != null)
                            PlayerWeapon.PlayerNoise(self, tr.endpos,
                                    Defines.PNOISE_IMPACT);
                    }
                }
            }
        }
    
        
        
        if (water) {

            Math3D.VectorSubtract(tr.endpos, water_start, dir);
            Math3D.VectorNormalize(dir);
            float[] pos = {0, 0, 0};
            Math3D.VectorMA(tr.endpos, -2, dir, pos);
            if ((GameBase.gi.pointcontents.pointcontents(pos) & Defines.MASK_WATER) != 0)
                Math3D.VectorCopy(pos, tr.endpos);
            else
                tr = game_import_t.trace(pos, null, null, water_start, tr.ent,
                        Defines.MASK_WATER);
    
            Math3D.VectorAdd(water_start, tr.endpos, pos);
            Math3D.VectorScale(pos, 0.5f, pos);
    
            game_import_t.WriteByte(Defines.svc_temp_entity);
            game_import_t.WriteByte(Defines.TE_BUBBLETRAIL);
            game_import_t.WritePosition(water_start);
            game_import_t.WritePosition(tr.endpos);
            game_import_t.multicast(pos, Defines.MULTICAST_PVS);
        }
    }

    /*
     * ================= fire_bullet
     * 
     * Fires a single round. Used for machinegun and chaingun. Would be fine for
     * pistols, rifles, etc.... =================
     */
    public static void fire_bullet(edict_t self, float[] start, float[] aimdir,
            int damage, int kick, int hspread, int vspread, int mod) {
        fire_lead(self, start, aimdir, damage, kick, Defines.TE_GUNSHOT,
                hspread, vspread, mod);
    }

    /*
     * ================= 
     * fire_shotgun
     * 
     * Shoots shotgun pellets. Used by shotgun and super shotgun.
     * =================
     */
    public static void fire_shotgun(edict_t self, float[] start,
            float[] aimdir, int damage, int kick, int hspread, int vspread,
            int count, int mod) {

        for (var i = 0; i < count; i++)
            fire_lead(self, start, aimdir, damage, kick, Defines.TE_SHOTGUN,
                    hspread, vspread, mod);
    }

    /*
     * ================= 
     * fire_blaster
     * 
     * Fires a single blaster bolt. Used by the blaster and hyper blaster.
     * =================
     */

    public static void fire_blaster(edict_t self, float[] start, float[] dir,
            int damage, int speed, int effect, boolean hyper) {

        Math3D.VectorNormalize(dir);

        var bolt = GameUtil.G_Spawn();
        bolt.svflags = Defines.SVF_DEADMONSTER;
        
        
        
        
        
        
        Math3D.VectorCopy(start, bolt.s.origin);
        Math3D.VectorCopy(start, bolt.s.old_origin);
        Math3D.vectoangles(dir, bolt.s.angles);
        Math3D.VectorScale(dir, speed, bolt.velocity);
        bolt.movetype = Defines.MOVETYPE_FLYMISSILE;
        bolt.clipmask = Defines.MASK_SHOT;
        bolt.solid = Defines.SOLID_BBOX;
        bolt.s.effects |= effect;
        Math3D.VectorClear(bolt.mins);
        Math3D.VectorClear(bolt.maxs);
        bolt.s.modelindex = game_import_t
                .modelindex("models/objects/laser/tris.md2");
        bolt.s.sound = game_import_t.soundindex("misc/lasfly.wav");
        bolt.owner = self;
        bolt.touch = blaster_touch;
        bolt.nextthink = GameBase.level.time + 2;
        bolt.think = GameUtil.G_FreeEdictA;
        bolt.dmg = damage;
        bolt.classname = "bolt";
        if (hyper)
            bolt.spawnflags = 1;
        game_import_t.linkentity(bolt);
    
        if (self.client != null)
            check_dodge(self, bolt.s.origin, dir, speed);

        var tr = game_import_t.trace(self.s.origin, null, null, bolt.s.origin, bolt,
                Defines.MASK_SHOT);
        if (tr.fraction < 1.0) {
            Math3D.VectorMA(bolt.s.origin, -10, dir, bolt.s.origin);
            bolt.touch.touch(bolt, tr.ent, GameBase.dummyplane, null);
        }
    }

    public static void fire_grenade(edict_t self, float[] start,
            float[] aimdir, int damage, int speed, float timer,
            float damage_radius) {
        float[] dir = { 0, 0, 0 };

        Math3D.vectoangles(aimdir, dir);
        float[] up = {0, 0, 0};
        float[] right = {0, 0, 0};
        float[] forward = {0, 0, 0};
        Math3D.AngleVectors(dir, forward, right, up);

        var grenade = GameUtil.G_Spawn();
        Math3D.VectorCopy(start, grenade.s.origin);
        Math3D.VectorScale(aimdir, speed, grenade.velocity);
        Math3D.VectorMA(grenade.velocity, 200f + Lib.crandom() * 10.0f, up,
                grenade.velocity);
        Math3D.VectorMA(grenade.velocity, Lib.crandom() * 10.0f, right,
                grenade.velocity);
        Math3D.VectorSet(grenade.avelocity, 300, 300, 300);
        grenade.movetype = Defines.MOVETYPE_BOUNCE;
        grenade.clipmask = Defines.MASK_SHOT;
        grenade.solid = Defines.SOLID_BBOX;
        grenade.s.effects |= Defines.EF_GRENADE;
        Math3D.VectorClear(grenade.mins);
        Math3D.VectorClear(grenade.maxs);
        grenade.s.modelindex = game_import_t
                .modelindex("models/objects/grenade/tris.md2");
        grenade.owner = self;
        grenade.touch = Grenade_Touch;
        grenade.nextthink = GameBase.level.time + timer;
        grenade.think = Grenade_Explode;
        grenade.dmg = damage;
        grenade.dmg_radius = damage_radius;
        grenade.classname = "grenade";
    
        game_import_t.linkentity(grenade);
    }

    public static void fire_grenade2(edict_t self, float[] start,
            float[] aimdir, int damage, int speed, float timer,
            float damage_radius, boolean held) {
        float[] dir = { 0, 0, 0 };

        Math3D.vectoangles(aimdir, dir);
        float[] up = {0, 0, 0};
        float[] right = {0, 0, 0};
        float[] forward = {0, 0, 0};
        Math3D.AngleVectors(dir, forward, right, up);

        var grenade = GameUtil.G_Spawn();
        Math3D.VectorCopy(start, grenade.s.origin);
        Math3D.VectorScale(aimdir, speed, grenade.velocity);
        Math3D.VectorMA(grenade.velocity, 200f + Lib.crandom() * 10.0f, up,
                grenade.velocity);
        Math3D.VectorMA(grenade.velocity, Lib.crandom() * 10.0f, right,
                grenade.velocity);
        Math3D.VectorSet(grenade.avelocity, 300f, 300f, 300f);
        grenade.movetype = Defines.MOVETYPE_BOUNCE;
        grenade.clipmask = Defines.MASK_SHOT;
        grenade.solid = Defines.SOLID_BBOX;
        grenade.s.effects |= Defines.EF_GRENADE;
        Math3D.VectorClear(grenade.mins);
        Math3D.VectorClear(grenade.maxs);
        grenade.s.modelindex = game_import_t
                .modelindex("models/objects/grenade2/tris.md2");
        grenade.owner = self;
        grenade.touch = Grenade_Touch;
        grenade.nextthink = GameBase.level.time + timer;
        grenade.think = Grenade_Explode;
        grenade.dmg = damage;
        grenade.dmg_radius = damage_radius;
        grenade.classname = "hgrenade";
        if (held)
            grenade.spawnflags = 3;
        else
            grenade.spawnflags = 1;
        grenade.s.sound = game_import_t.soundindex("weapons/hgrenc1b.wav");
    
        if (timer <= 0.0)
            Grenade_Explode.think(grenade);
        else {
            game_import_t.sound(self, Defines.CHAN_WEAPON, game_import_t
                    .soundindex("weapons/hgrent1a.wav"), 1, Defines.ATTN_NORM,
                    0);
            game_import_t.linkentity(grenade);
        }
    }

    public static void fire_rocket(edict_t self, float[] start, float[] dir,
            int damage, int speed, float damage_radius, int radius_damage) {

        var rocket = GameUtil.G_Spawn();
        Math3D.VectorCopy(start, rocket.s.origin);
        Math3D.VectorCopy(dir, rocket.movedir);
        Math3D.vectoangles(dir, rocket.s.angles);
        Math3D.VectorScale(dir, speed, rocket.velocity);
        rocket.movetype = Defines.MOVETYPE_FLYMISSILE;
        rocket.clipmask = Defines.MASK_SHOT;
        rocket.solid = Defines.SOLID_BBOX;
        rocket.s.effects |= Defines.EF_ROCKET;
        Math3D.VectorClear(rocket.mins);
        Math3D.VectorClear(rocket.maxs);
        rocket.s.modelindex = game_import_t
                .modelindex("models/objects/rocket/tris.md2");
        rocket.owner = self;
        rocket.touch = rocket_touch;
        rocket.nextthink = GameBase.level.time + 8000f / speed;
        rocket.think = GameUtil.G_FreeEdictA;
        rocket.dmg = damage;
        rocket.radius_dmg = radius_damage;
        rocket.dmg_radius = damage_radius;
        rocket.s.sound = game_import_t.soundindex("weapons/rockfly.wav");
        rocket.classname = "rocket";
    
        if (self.client != null)
            check_dodge(self, rocket.s.origin, dir, speed);
    
        game_import_t.linkentity(rocket);
    }

    /*
     * ================= 
     * fire_rail 
     * =================
     */
    public static void fire_rail(edict_t self, float[] start, float[] aimdir,
            int damage, int kick) {
        float[] end = { 0, 0, 0 };

        Math3D.VectorMA(start, 8192f, aimdir, end);
        float[] from = {0, 0, 0};
        Math3D.VectorCopy(start, from);
        var ignore = self;
        var water = false;
        var mask = Defines.MASK_SHOT | Defines.CONTENTS_SLIME
                | Defines.CONTENTS_LAVA;
        trace_t tr = null;
        while (ignore != null) {
            tr = game_import_t.trace(from, null, null, end, ignore, mask);
    
            if ((tr.contents & (Defines.CONTENTS_SLIME | Defines.CONTENTS_LAVA)) != 0) {
                mask &= ~(Defines.CONTENTS_SLIME | Defines.CONTENTS_LAVA);
                water = true;
            } else {
                
                
                if ((tr.ent.svflags & Defines.SVF_MONSTER) != 0
                        || (tr.ent.client != null)
                        || (tr.ent.solid == Defines.SOLID_BBOX))
                    ignore = tr.ent;
                else
                    ignore = null;
    
                if ((tr.ent != self) && (tr.ent.takedamage != 0))
                    GameCombat.T_Damage(tr.ent, self, self, aimdir, tr.endpos,
                            tr.plane.normal, damage, kick, 0,
                            Defines.MOD_RAILGUN);
            }
    
            Math3D.VectorCopy(tr.endpos, from);
        }
    
        
        game_import_t.WriteByte(Defines.svc_temp_entity);
        game_import_t.WriteByte(Defines.TE_RAILTRAIL);
        game_import_t.WritePosition(start);
        game_import_t.WritePosition(tr.endpos);
        game_import_t.multicast(self.s.origin, Defines.MULTICAST_PHS);
        
        if (water) {
            game_import_t.WriteByte(Defines.svc_temp_entity);
            game_import_t.WriteByte(Defines.TE_RAILTRAIL);
            game_import_t.WritePosition(start);
            game_import_t.WritePosition(tr.endpos);
            game_import_t.multicast(tr.endpos, Defines.MULTICAST_PHS);
        }
    
        if (self.client != null)
            PlayerWeapon.PlayerNoise(self, tr.endpos, Defines.PNOISE_IMPACT);
    }

    public static void fire_bfg(edict_t self, float[] start, float[] dir,
            int damage, int speed, float damage_radius) {

        var bfg = GameUtil.G_Spawn();
        Math3D.VectorCopy(start, bfg.s.origin);
        Math3D.VectorCopy(dir, bfg.movedir);
        Math3D.vectoangles(dir, bfg.s.angles);
        Math3D.VectorScale(dir, speed, bfg.velocity);
        bfg.movetype = Defines.MOVETYPE_FLYMISSILE;
        bfg.clipmask = Defines.MASK_SHOT;
        bfg.solid = Defines.SOLID_BBOX;
        bfg.s.effects |= Defines.EF_BFG | Defines.EF_ANIM_ALLFAST;
        Math3D.VectorClear(bfg.mins);
        Math3D.VectorClear(bfg.maxs);
        bfg.s.modelindex = game_import_t.modelindex("sprites/s_bfg1.sp2");
        bfg.owner = self;
        bfg.touch = bfg_touch;
        bfg.nextthink = GameBase.level.time + 8000f/ speed;
        bfg.think = GameUtil.G_FreeEdictA;
        bfg.radius_dmg = damage;
        bfg.dmg_radius = damage_radius;
        bfg.classname = "bfg blast";
        bfg.s.sound = game_import_t.soundindex("weapons/bfg__l1a.wav");
    
        bfg.think = bfg_think;
        bfg.nextthink = GameBase.level.time + Defines.FRAMETIME;
        bfg.teammaster = bfg;
        bfg.teamchain = null;
    
        if (self.client != null)
            check_dodge(self, bfg.s.origin, dir, speed);
    
        game_import_t.linkentity(bfg);
    }
}
