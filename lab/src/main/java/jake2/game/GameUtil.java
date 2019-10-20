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
import jake2.client.M;
import jake2.qcommon.Com;
import jake2.util.Lib;
import jake2.util.Math3D;

public class GameUtil {

    public static void checkClassname(edict_t ent) {

        if (ent.classname == null) {
            Com.Printf("edict with classname = null: " + ent.index);
        }
    }

    /** 
     * Use the targets.
     * 
     * The global "activator" should be set to the entity that initiated the
     * firing.
     * 
     * If self.delay is setAt, a DelayedUse entity will be created that will
     * actually do the SUB_UseTargets after that many seconds have passed.
     * 
     * Centerprints any self.message to the activator.
     * 
     * Search for (string)targetname in all entities that match
     * (string)self.target and call their .use function
     */

    public static void G_UseTargets(edict_t ent, edict_t activator) {

        checkClassname(ent);


        edict_t t;
        if (ent.delay != 0) {
            
            t = G_Spawn();
            t.classname = "DelayedUse";
            t.nextthink = GameBase.level.time + ent.delay;
            t.think = Think_Delay;
            t.activator = activator;
            if (activator == null)
                game_import_t.dprintf("Think_Delay with no activator\n");
            t.message = ent.message;
            t.target = ent.target;
            t.killtarget = ent.killtarget;
            return;
        }


        
        if ((ent.message != null)
                && (activator.svflags & Defines.SVF_MONSTER) == 0) {
            game_import_t.centerprintf(activator, ent.message);
            if (ent.noise_index != 0)
                game_import_t.sound(activator, Defines.CHAN_AUTO,
                        ent.noise_index, 1, Defines.ATTN_NORM, 0);
            else
                game_import_t.sound(activator, Defines.CHAN_AUTO, game_import_t
                        .soundindex("misc/talk1.wav"), 1, Defines.ATTN_NORM, 0);
        }

        
        EdictIterator edit = null;

        if (ent.killtarget != null) {
            while ((edit = GameBase.G_Find(edit, GameBase.findByTarget,
                    ent.killtarget)) != null) {
                t = edit.o;
                G_FreeEdict(t);
                if (!ent.inuse) {
                    game_import_t
                            .dprintf("entity was removed while using killtargets\n");
                    return;
                }
            }
        }

        
        if (ent.target != null) {
            edit = null;
            while ((edit = GameBase.G_Find(edit, GameBase.findByTarget,
                    ent.target)) != null) {
                t = edit.o;
                
                if (Lib.Q_stricmp("func_areaportal", t.classname) == 0
                        && (Lib.Q_stricmp("func_door", ent.classname) == 0 || Lib
                                .Q_stricmp("func_door_rotating", ent.classname) == 0))
                    continue;

                if (t == ent) {
                    game_import_t.dprintf("WARNING: Entity used itself.\n");
                } else {
                    if (t.use != null)
                        t.use.use(t, ent, activator);
                }
                if (!ent.inuse) {
                    game_import_t
                            .dprintf("entity was removed while using targets\n");
                    return;
                }
            }
        }
    }

    public static void G_InitEdict(edict_t e, int i) {
        e.inuse = true;
        e.classname = "noclass";
        e.gravity = 1.0f;
        
        e.s = new entity_state_t(e);
        e.s.number = i;
        e.index = i;
    }

    /**
     * Either finds a free edict, or allocates a new one. Try to avoid reusing
     * an entity that was recently freed, because it can cause the client to
     * think the entity morphed into something else instead of being removed and
     * recreated, which can cause interpolated angles and bad trails.
     */
    public static edict_t G_Spawn() {
        int i;
        edict_t e = null;

        for (i = (int) GameBase.maxclients.value + 1; i < GameBase.num_edicts; i++) {
            e = GameBase.g_edicts[i];
            
            
            if (!e.inuse
                    && (e.freetime < 2 || GameBase.level.time - e.freetime > 0.5)) {
                e = GameBase.g_edicts[i] = new edict_t(i);
                G_InitEdict(e, i);
                return e;
            }
        }

        if (i == GameBase.game.maxentities)
            game_import_t.error("ED_Alloc: no free edicts");

        e = GameBase.g_edicts[i] = new edict_t(i);
        GameBase.num_edicts++;
        G_InitEdict(e, i);
        return e;
    }

    /**
     * Marks the edict as free
     */
    public static void G_FreeEdict(edict_t ed) {
        game_import_t.unlinkentity(ed); 

        
        if (ed.index <= (GameBase.maxclients.value + Defines.BODY_QUEUE_SIZE)) {
            
            return;
        }

        GameBase.g_edicts[ed.index] = new edict_t(ed.index);
        ed.classname = "freed";
        ed.freetime = GameBase.level.time;
        ed.inuse = false;
    }

    /**
     * Call after linking a new trigger in during gameplay to force all entities
     * it covers to immediately touch it.
     */

    public static void G_ClearEdict(edict_t ent) {
        int i = ent.index;
        GameBase.g_edicts[i] = new edict_t(i);
    }


    /**
     * Kills all entities that would touch the proposed new positioning of ent.
     * Ent should be unlinked before calling this!
     */

    public static boolean KillBox(edict_t ent) {

        while (true) {
            trace_t tr = game_import_t.trace(ent.s.origin, ent.mins, ent.maxs,
                    ent.s.origin, null, Defines.MASK_PLAYERSOLID);
            if (tr.ent == null || tr.ent == GameBase.g_edicts[0])
                break;

            
            GameCombat.T_Damage(tr.ent, ent, ent, Globals.vec3_origin, ent.s.origin,
                    Globals.vec3_origin, 100000, 0,
                    Defines.DAMAGE_NO_PROTECTION, Defines.MOD_TELEFRAG);

            
            if (tr.ent.solid != 0)
                return false;
        }

        return true; 
    }

    /** 
     * Returns true, if two edicts are on the same team. 
     */
    public static boolean OnSameTeam(edict_t ent1, edict_t ent2) {
        if (0 == ((int) (GameBase.dmflags.value) & (Defines.DF_MODELTEAMS | Defines.DF_SKINTEAMS)))
            return false;

        return ClientTeam(ent1).equals(ClientTeam(ent2));
    }

    /** 
     * Returns the team string of an entity 
     * with respect to rteam_by_model and team_by_skin. 
     */
    static String ClientTeam(edict_t ent) {

        if (ent.client == null)
            return "";

        String value = Info.Info_ValueForKey(ent.client.pers.userinfo, "skin");

        int p = value.indexOf('/');

        if (p == -1)
            return value;

        if (((int) (GameBase.dmflags.value) & Defines.DF_MODELTEAMS) != 0) {
            return value.substring(0, p);
        }

        return value.substring(p + 1);
    }

    static void ValidateSelectedItem(edict_t ent) {

        gclient_t cl = ent.client;

        if (cl.pers.inventory[cl.pers.selected_item] != 0)
            return; 

        GameItems.SelectNextItem(ent, -1);
    }

    /**
     * Returns the range catagorization of an entity reletive to self 0 melee
     * range, will become hostile even if back is turned 1 visibility and
     * infront, or visibility and show hostile 2 infront and show hostile 3 only
     * triggered by damage.
     */
    public static int range(edict_t self, edict_t other) {
        float[] v = { 0, 0, 0 };

        Math3D.VectorSubtract(self.s.origin, other.s.origin, v);
        float len = Math3D.VectorLength(v);
        if (len < Defines.MELEE_DISTANCE)
            return Defines.RANGE_MELEE;
        if (len < 500)
            return Defines.RANGE_NEAR;
        if (len < 1000)
            return Defines.RANGE_MID;
        return Defines.RANGE_FAR;
    }

    static void AttackFinished(edict_t self, float time) {
        self.monsterinfo.attack_finished = GameBase.level.time + time;
    }

    /**
     * Returns true if the entity is in front (in sight) of self
     */
    public static boolean infront(edict_t self, edict_t other) {
        float[] forward = { 0, 0, 0 };

        Math3D.AngleVectors(self.s.angles, forward, null, null);
        float[] vec = {0, 0, 0};
        Math3D.VectorSubtract(other.s.origin, self.s.origin, vec);
        Math3D.VectorNormalize(vec);
        float dot = Math3D.DotProduct(vec, forward);

        return dot > 0.3;
    }

    /**
     * Returns 1 if the entity is visible to self, even if not infront().
     */
    public static boolean visible(edict_t self, edict_t other) {
        float[] spot1 = { 0, 0, 0 };

        Math3D.VectorCopy(self.s.origin, spot1);
        spot1[2] += self.viewheight;
        float[] spot2 = {0, 0, 0};
        Math3D.VectorCopy(other.s.origin, spot2);
        spot2[2] += other.viewheight;
        trace_t trace = game_import_t.trace(spot1, Globals.vec3_origin,
                Globals.vec3_origin, spot2, self, Defines.MASK_OPAQUE);

        return trace.fraction == 1.0;
    }

    /**
     * Finds a target.
     * 
     * Self is currently not attacking anything, so try to find a target
     * 
     * Returns TRUE if an enemy was sighted
     * 
     * When a player fires a missile, the point of impact becomes a fakeplayer
     * so that monsters that see the impact will respond as if they had seen the
     * player.
     * 
     * To avoid spending too much time, only a single client (or fakeclient) is
     * checked each frame. This means multi player games will have slightly
     * slower noticing monsters.
     */
    static boolean FindTarget(edict_t self) {
        boolean result = true;
        boolean finished = false;

        if ((self.monsterinfo.aiflags & Defines.AI_GOOD_GUY) != 0) {
            if (self.goalentity != null && self.goalentity.inuse
                    && self.goalentity.classname != null) {
                if ("target_actor".equals(self.goalentity.classname)) {
                    result = false;
                    finished = true;
                }
            }
            if (!finished) {
                result = false;
                finished = true;
            }


        }
        if (!finished) {
            if ((self.monsterinfo.aiflags & Defines.AI_COMBAT_POINT) != 0) {
                result = false;
            } else {
                boolean heardit = false;
                edict_t client = null;
                if ((GameBase.level.sight_entity_framenum >= (GameBase.level.framenum - 1))
                        && 0 == (self.spawnflags & 1)) {
                    client = GameBase.level.sight_entity;
                    if (client.enemy == self.enemy) {
                        result = false;
                        finished = true;
                    }
                } else if (GameBase.level.sound_entity_framenum >= (GameBase.level.framenum - 1)) {
                    client = GameBase.level.sound_entity;
                    heardit = true;
                } else if (null != (self.enemy)
                        && (GameBase.level.sound2_entity_framenum >= (GameBase.level.framenum - 1))
                        && 0 != (self.spawnflags & 1)) {
                    client = GameBase.level.sound2_entity;
                    heardit = true;
                } else {
                    client = GameBase.level.sight_client;
                    if (client == null) {
                        result = false;
                        finished = true;
                    }
                }
                if (!finished) {
                    if (!client.inuse) {
                        result = false;
                    } else {
                        if (client.client != null) {
                            if ((client.flags & Defines.FL_NOTARGET) != 0) {
                                result = false;
                                finished = true;
                            }
                        } else if ((client.svflags & Defines.SVF_MONSTER) != 0) {
                            if (client.enemy == null) {
                                result = false;
                                finished = true;
                            } else if ((client.enemy.flags & Defines.FL_NOTARGET) != 0) {
                                result = false;
                                finished = true;
                            }
                        } else if (heardit) {
                            if ((client.owner.flags & Defines.FL_NOTARGET) != 0) {
                                result = false;
                                finished = true;
                            }
                        } else {
                            result = false;
                            finished = true;
                        }
                        if (!finished) {
                            if (!heardit) {
                                int r = range(self, client);

                                if (r == Defines.RANGE_FAR) {
                                    result = false;
                                    finished = true;
                                } else if (client.light_level <= 5) {
                                    result = false;
                                    finished = true;
                                } else if (!visible(self, client)) {
                                    result = false;
                                    finished = true;
                                } else {
                                    switch (r) {
                                        case Defines.RANGE_NEAR:
                                            if (client.show_hostile < GameBase.level.time
                                                    && !infront(self, client)) {
                                                result = false;
                                                finished = true;
                                            }
                                            break;
                                        case Defines.RANGE_MID:
                                            if (!infront(self, client)) {
                                                result = false;
                                                finished = true;
                                            }
                                            break;
                                    }
                                    if (!finished) {
                                        if (client == self.enemy) {
                                            finished = true;
                                        } else {
                                            self.enemy = client;
                                            if (!"player_noise".equals(self.enemy.classname)) {
                                                self.monsterinfo.aiflags &= ~Defines.AI_SOUND_TARGET;

                                                if (self.enemy.client == null) {
                                                    self.enemy = self.enemy.enemy;
                                                    if (self.enemy.client == null) {
                                                        self.enemy = null;
                                                        result = false;
                                                        finished = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }


                            } else {

                                if ((self.spawnflags & 1) != 0) {
                                    if (!visible(self, client)) {
                                        result = false;
                                        finished = true;
                                    }
                                } else if (!game_import_t.inPHS(self.s.origin, client.s.origin)) {
                                    result = false;
                                    finished = true;
                                }
                                if (!finished) {
                                    float[] temp = {0, 0, 0};
                                    Math3D.VectorSubtract(client.s.origin, self.s.origin, temp);

                                    if (Math3D.VectorLength(temp) > 1000) {
                                        result = false;
                                        finished = true;
                                    } else {
                                        if (client.areanum != self.areanum)
                                            if (!game_import_t.AreasConnected(self.areanum, client.areanum)) {
                                                result = false;
                                                finished = true;
                                            }
                                        if (!finished) {
                                            self.ideal_yaw = Math3D.vectoyaw(temp);
                                            M.M_ChangeYaw(self);
                                            self.monsterinfo.aiflags |= Defines.AI_SOUND_TARGET;
                                            if (client == self.enemy) {
                                                finished = true;
                                            } else {
                                                self.enemy = client;
                                            }
                                        }
                                    }


                                }

                            }
                            if (!finished) {
                                FoundTarget(self);
                                if (0 == (self.monsterinfo.aiflags & Defines.AI_SOUND_TARGET)
                                        && (self.monsterinfo.sight != null))
                                    self.monsterinfo.sight.interact(self, self.enemy);
                            }
                        }
                    }
                }
            }


        }


        return result;
    }

    public static void FoundTarget(edict_t self) {
        
        if (self.enemy.client != null) {
            GameBase.level.sight_entity = self;
            GameBase.level.sight_entity_framenum = GameBase.level.framenum;
            GameBase.level.sight_entity.light_level = 128;
        }

        self.show_hostile = (int) GameBase.level.time + 1; 
                                                           

        Math3D.VectorCopy(self.enemy.s.origin, self.monsterinfo.last_sighting);
        self.monsterinfo.trail_time = GameBase.level.time;

        if (self.combattarget == null) {
            GameAI.HuntTarget(self);
            return;
        }

        self.goalentity = self.movetarget = GameBase
                .G_PickTarget(self.combattarget);
        if (self.movetarget == null) {
            self.goalentity = self.movetarget = self.enemy;
            GameAI.HuntTarget(self);
            game_import_t.dprintf(self.classname + "at " + Lib.vtos(self.s.origin) + ", combattarget " + self.combattarget + " not found\n");
            return;
        }

        
        self.combattarget = null;
        self.monsterinfo.aiflags |= Defines.AI_COMBAT_POINT;

        
        self.movetarget.targetname = null;
        self.monsterinfo.pausetime = 0;

        
        self.monsterinfo.run.think(self);
    }

    public static final EntThinkAdapter Think_Delay = new EntThinkAdapter() {
    	@Override
        public String getID() { return "Think_Delay"; }
        @Override
        public boolean think(edict_t ent) {
            G_UseTargets(ent, ent.activator);
            G_FreeEdict(ent);
            return true;
        }
    };

    public static final EntThinkAdapter G_FreeEdictA = new EntThinkAdapter() {
    	@Override
        public String getID() { return "G_FreeEdictA"; }
        @Override
        public boolean think(edict_t ent) {
            G_FreeEdict(ent);
            return false;
        }
    };

    static final EntThinkAdapter MegaHealth_think = new EntThinkAdapter() {
    	@Override
        public String getID() { return "MegaHealth_think"; }
        @Override
        public boolean think(edict_t self) {
            if (self.owner.health > self.owner.max_health) {
                self.nextthink = GameBase.level.time + 1;
                self.owner.health -= 1;
                return false;
            }

            if ((self.spawnflags & Defines.DROPPED_ITEM) == 0
                    && (GameBase.deathmatch.value != 0))
                GameItems.SetRespawn(self, 20);
            else
                G_FreeEdict(self);

            return false;
        }
    };


    public static final EntThinkAdapter M_CheckAttack = new EntThinkAdapter() {
    	@Override
        public String getID() { return "M_CheckAttack"; }

        @Override
        public boolean think(edict_t self) {

            if (self.enemy.health > 0) {

                float[] spot1 = {0, 0, 0};
                Math3D.VectorCopy(self.s.origin, spot1);
                spot1[2] += self.viewheight;
                float[] spot2 = {0, 0, 0};
                Math3D.VectorCopy(self.enemy.s.origin, spot2);
                spot2[2] += self.enemy.viewheight;

                trace_t tr = game_import_t.trace(spot1, null, null, spot2, self,
                        Defines.CONTENTS_SOLID | Defines.CONTENTS_MONSTER
                                | Defines.CONTENTS_SLIME
                                | Defines.CONTENTS_LAVA
                                | Defines.CONTENTS_WINDOW);


                if (tr.ent != self.enemy)
                    return false;
            }

            
            if (GameAI.enemy_range == Defines.RANGE_MELEE) {
                
                if (GameBase.skill.value == 0 && (Lib.rand() & 3) != 0)
                    return false;
                if (self.monsterinfo.melee != null)
                    self.monsterinfo.attack_state = Defines.AS_MELEE;
                else
                    self.monsterinfo.attack_state = Defines.AS_MISSILE;
                return true;
            }

            
            if (self.monsterinfo.attack == null)
                return false;

            if (GameBase.level.time < self.monsterinfo.attack_finished)
                return false;

            if (GameAI.enemy_range == Defines.RANGE_FAR)
                return false;

            float chance;
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0) {
                chance = 0.4f;
            } else if (GameAI.enemy_range == Defines.RANGE_MELEE) {
                chance = 0.2f;
            } else if (GameAI.enemy_range == Defines.RANGE_NEAR) {
                chance = 0.1f;
            } else if (GameAI.enemy_range == Defines.RANGE_MID) {
                chance = 0.02f;
            } else {
                return false;
            }

            if (GameBase.skill.value == 0)
                chance *= 0.5;
            else if (GameBase.skill.value >= 2)
                chance *= 2;

            if (Lib.random() < chance) {
                self.monsterinfo.attack_state = Defines.AS_MISSILE;
                self.monsterinfo.attack_finished = GameBase.level.time + 2
                        * Lib.random();
                return true;
            }

            if ((self.flags & Defines.FL_FLY) != 0) {
                if (Lib.random() < 0.3f)
                    self.monsterinfo.attack_state = Defines.AS_SLIDING;
                else
                    self.monsterinfo.attack_state = Defines.AS_STRAIGHT;
            }

            return false;

        }
    };

    static final EntUseAdapter monster_use = new EntUseAdapter() {
    	@Override
        public String getID() { return "monster_use"; }
        @Override
        public void use(edict_t self, edict_t other, edict_t activator) {
            if (self.enemy != null)
                return;
            if (self.health <= 0)
                return;
            if ((activator.flags & Defines.FL_NOTARGET) != 0)
                return;
            if ((null == activator.client)
                    && 0 == (activator.monsterinfo.aiflags & Defines.AI_GOOD_GUY))
                return;

            
            
            self.enemy = activator;
            FoundTarget(self);
        }
    };
}