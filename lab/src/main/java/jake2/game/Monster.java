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
import jake2.client.M;
import jake2.qcommon.Com;
import jake2.util.Lib;
import jake2.util.Math3D;

public class Monster {

    
    
    
    
    public static void monster_fire_bullet(edict_t self, float[] start,
            float[] dir, int damage, int kick, int hspread, int vspread,
            int flashtype) {
        GameWeapon.fire_bullet(self, start, dir, damage, kick, hspread, vspread,
                Defines.MOD_UNKNOWN);

        game_import_t.WriteByte(Defines.svc_muzzleflash2);
        game_import_t.WriteShort(self.index);
        game_import_t.WriteByte(flashtype);
        game_import_t.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the shotgun. */
    public static void monster_fire_shotgun(edict_t self, float[] start,
            float[] aimdir, int damage, int kick, int hspread, int vspread,
            int count, int flashtype) {
        GameWeapon.fire_shotgun(self, start, aimdir, damage, kick, hspread, vspread,
                count, Defines.MOD_UNKNOWN);

        game_import_t.WriteByte(Defines.svc_muzzleflash2);
        game_import_t.WriteShort(self.index);
        game_import_t.WriteByte(flashtype);
        game_import_t.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the blaster. */
    public static void monster_fire_blaster(edict_t self, float[] start,
            float[] dir, int damage, int speed, int flashtype, int effect) {
        GameWeapon.fire_blaster(self, start, dir, damage, speed, effect, false);

        game_import_t.WriteByte(Defines.svc_muzzleflash2);
        game_import_t.WriteShort(self.index);
        game_import_t.WriteByte(flashtype);
        game_import_t.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the grenade. */
    public static void monster_fire_grenade(edict_t self, float[] start,
            float[] aimdir, int damage, int speed, int flashtype) {
        GameWeapon
                .fire_grenade(self, start, aimdir, damage, speed, 2.5f,
                        damage + 40);

        game_import_t.WriteByte(Defines.svc_muzzleflash2);
        game_import_t.WriteShort(self.index);
        game_import_t.WriteByte(flashtype);
        game_import_t.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the rocket. */
    public static void monster_fire_rocket(edict_t self, float[] start,
            float[] dir, int damage, int speed, int flashtype) {
        GameWeapon.fire_rocket(self, start, dir, damage, speed, damage + 20, damage);

        game_import_t.WriteByte(Defines.svc_muzzleflash2);
        game_import_t.WriteShort(self.index);
        game_import_t.WriteByte(flashtype);
        game_import_t.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the railgun. */
    public static void monster_fire_railgun(edict_t self, float[] start,
            float[] aimdir, int damage, int kick, int flashtype) {
        GameWeapon.fire_rail(self, start, aimdir, damage, kick);

        game_import_t.WriteByte(Defines.svc_muzzleflash2);
        game_import_t.WriteShort(self.index);
        game_import_t.WriteByte(flashtype);
        game_import_t.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the bfg. */
    public static void monster_fire_bfg(edict_t self, float[] start,
            float[] aimdir, int damage, int speed, int kick,
            float damage_radius, int flashtype) {
        GameWeapon.fire_bfg(self, start, aimdir, damage, speed, damage_radius);

        game_import_t.WriteByte(Defines.svc_muzzleflash2);
        game_import_t.WriteShort(self.index);
        game_import_t.WriteByte(flashtype);
        game_import_t.multicast(start, Defines.MULTICAST_PVS);
    }

    /*
     * ================ monster_death_use
     * 
     * When a monster dies, it fires all of its targets with the current enemy
     * as activator. ================
     */
    public static void monster_death_use(edict_t self) {
        self.flags &= ~(Defines.FL_FLY | Defines.FL_SWIM);
        self.monsterinfo.aiflags &= Defines.AI_GOOD_GUY;

        if (self.item != null) {
            GameItems.Drop_Item(self, self.item);
            self.item = null;
        }

        if (self.deathtarget != null)
            self.target = self.deathtarget;

        if (self.target == null)
            return;

        GameUtil.G_UseTargets(self, self.enemy);
    }

    
    public static boolean monster_start(edict_t self) {
        if (GameBase.deathmatch.value != 0) {
            GameUtil.G_FreeEdict(self);
            return false;
        }

        if ((self.spawnflags & 4) != 0
                && 0 == (self.monsterinfo.aiflags & Defines.AI_GOOD_GUY)) {
            self.spawnflags &= ~4;
            self.spawnflags |= 1;
            
            
        }

        if (0 == (self.monsterinfo.aiflags & Defines.AI_GOOD_GUY))
            GameBase.level.total_monsters++;

        self.nextthink = GameBase.level.time + Defines.FRAMETIME;
        self.svflags |= Defines.SVF_MONSTER;
        self.s.renderfx |= Defines.RF_FRAMELERP;
        self.takedamage = Defines.DAMAGE_AIM;
        self.air_finished = GameBase.level.time + 12;
        self.use = GameUtil.monster_use;
        self.max_health = self.health;
        self.clipmask = Defines.MASK_MONSTERSOLID;

        self.s.skinnum = 0;
        self.deadflag = Defines.DEAD_NO;
        self.svflags &= ~Defines.SVF_DEADMONSTER;

        if (null == self.monsterinfo.checkattack)
            self.monsterinfo.checkattack = GameUtil.M_CheckAttack;
        Math3D.VectorCopy(self.s.origin, self.s.old_origin);

        if (GameBase.st.item != null && GameBase.st.item.length() > 0) {
            self.item = GameItems.FindItemByClassname(GameBase.st.item);
            if (self.item == null)
                game_import_t.dprintf("monster_start:" + self.classname + " at "
                        + Lib.vtos(self.s.origin) + " has bad item: "
                        + GameBase.st.item + '\n');
        }

        
        if (self.monsterinfo.currentmove != null)
            self.s.frame = self.monsterinfo.currentmove.firstframe
                    + (Lib.rand() % (self.monsterinfo.currentmove.lastframe
                            - self.monsterinfo.currentmove.firstframe + 1));

        return true;
    }

    public static void monster_start_go(edict_t self) {

        float[] v = { 0, 0, 0 };

        if (self.health <= 0)
            return;

        
        if (self.target != null) {
            boolean notcombat;
            boolean fixup;
            edict_t target = null;
            notcombat = false;
            fixup = false;
            /*
             * if (true) { Com.Printf("all entities:\n");
             * 
             * for (int n = 0; n < Game.globals.num_edicts; n++) { edict_t ent =
             * GameBase.g_edicts[n]; Com.Printf( "|%4i | %25s
             * |%8.2f|%8.2f|%8.2f||%8.2f|%8.2f|%8.2f||%8.2f|%8.2f|%8.2f|\n", new
             * Vargs().addAt(n).addAt(ent.classname).
             * addAt(ent.s.origin[0]).addAt(ent.s.origin[1]).addAt(ent.s.origin[2])
             * .addAt(ent.mins[0]).addAt(ent.mins[1]).addAt(ent.mins[2])
             * .addAt(ent.maxs[0]).addAt(ent.maxs[1]).addAt(ent.maxs[2])); }
             * sleep(10); }
             */

            EdictIterator edit = null;

            while ((edit = GameBase.G_Find(edit, GameBase.findByTarget,
                    self.target)) != null) {
                target = edit.o;
                if (Lib.strcmp(target.classname, "point_combat") == 0) {
                    self.combattarget = self.target;
                    fixup = true;
                } else {
                    notcombat = true;
                }
            }
            if (notcombat && self.combattarget != null)
                game_import_t.dprintf(self.classname + " at "
                        + Lib.vtos(self.s.origin)
                        + " has target with mixed types\n");
            if (fixup)
                self.target = null;
        }

        
        if (self.combattarget != null) {
            edict_t target = null;

            EdictIterator edit = null;
            while ((edit = GameBase.G_Find(edit, GameBase.findByTarget,
                    self.combattarget)) != null) {
                target = edit.o;

                if (Lib.strcmp(target.classname, "point_combat") != 0) {
                    game_import_t.dprintf(self.classname + " at "
                            + Lib.vtos(self.s.origin)
                            + " has bad combattarget " + self.combattarget
                            + " : " + target.classname + " at "
                            + Lib.vtos(target.s.origin));
                }
            }
        }

        if (self.target != null) {
            self.goalentity = self.movetarget = GameBase
                    .G_PickTarget(self.target);
            if (null == self.movetarget) {
                game_import_t
                        .dprintf(self.classname + " can't find target "
                                + self.target + " at "
                                + Lib.vtos(self.s.origin) + '\n');
                self.target = null;
                self.monsterinfo.pausetime = 100000000;
                self.monsterinfo.stand.think(self);
            } else if (Lib.strcmp(self.movetarget.classname, "path_corner") == 0) {
                Math3D.VectorSubtract(self.goalentity.s.origin, self.s.origin,
                        v);
                self.ideal_yaw = self.s.angles[Defines.YAW] = Math3D
                        .vectoyaw(v);
                self.monsterinfo.walk.think(self);
                self.target = null;
            } else {
                self.goalentity = self.movetarget = null;
                self.monsterinfo.pausetime = 100000000;
                self.monsterinfo.stand.think(self);
            }
        } else {
            self.monsterinfo.pausetime = 100000000;
            self.monsterinfo.stand.think(self);
        }

        self.think = Monster.monster_think;
        self.nextthink = GameBase.level.time + Defines.FRAMETIME;
    }

    public static final EntThinkAdapter monster_think = new EntThinkAdapter() {
        @Override
        public String getID() { return "monster_think";}
        @Override
        public boolean think(edict_t self) {

            M.M_MoveFrame(self);
            if (self.linkcount != self.monsterinfo.linkcount) {
                self.monsterinfo.linkcount = self.linkcount;
                M.M_CheckGround(self);
            }
            M.M_CatagorizePosition(self);
            M.M_WorldEffects(self);
            M.M_SetEffects(self);
            return true;
        }
    };

    public static final EntThinkAdapter monster_triggered_spawn = new EntThinkAdapter() {
        @Override
        public String getID() { return "monster_trigger_spawn";}
        @Override
        public boolean think(edict_t self) {

            self.s.origin[2] += 1;
            GameUtil.KillBox(self);

            self.solid = Defines.SOLID_BBOX;
            self.movetype = Defines.MOVETYPE_STEP;
            self.svflags &= ~Defines.SVF_NOCLIENT;
            self.air_finished = GameBase.level.time + 12;
            game_import_t.linkentity(self);

            Monster.monster_start_go(self);

            if (self.enemy != null && 0 == (self.spawnflags & 1)
                    && 0 == (self.enemy.flags & Defines.FL_NOTARGET)) {
                GameUtil.FoundTarget(self);
            } else {
                self.enemy = null;
            }
            return true;
        }
    };

    
    
    public static final EntUseAdapter monster_triggered_spawn_use = new EntUseAdapter() {
        @Override
        public String getID() { return "monster_trigger_spawn_use";}
        @Override
        public void use(edict_t self, edict_t other, edict_t activator) {
            self.think = monster_triggered_spawn;
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;
            if (activator.client != null)
                self.enemy = activator;
            self.use = GameUtil.monster_use;
        }
    };

    public static final EntThinkAdapter monster_triggered_start = new EntThinkAdapter() {
        @Override
        public String getID() { return "monster_triggered_start";}
        @Override
        public boolean think(edict_t self) {
            if (self.index == 312)
                Com.Printf("monster_triggered_start\n");
            self.solid = Defines.SOLID_NOT;
            self.movetype = Defines.MOVETYPE_NONE;
            self.svflags |= Defines.SVF_NOCLIENT;
            self.nextthink = 0;
            self.use = monster_triggered_spawn_use;
            return true;
        }
    };
}