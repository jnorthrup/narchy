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
import jake2.game.monsters.M_Player;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.util.Arrays;

public class PlayerClient {

    public static int player_die_i;
    
    /**
     * player_die. 
     */
    static final EntDieAdapter player_die = new EntDieAdapter() {
    	@Override
        public String getID() { return "player_die"; }
        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {

            Math3D.VectorClear(self.avelocity);
    
            self.takedamage = Defines.DAMAGE_YES;
            self.movetype = Defines.MOVETYPE_TOSS;
    
            self.s.modelindex2 = 0; 
    
            self.s.angles[0] = (float) 0;
            self.s.angles[2] = (float) 0;
    
            self.s.sound = 0;
            self.client.weapon_sound = 0;
    
            self.maxs[2] = -8.0F;
    
            
            self.svflags |= Defines.SVF_DEADMONSTER;

            int n;
            if (self.deadflag == 0) {
                self.client.respawn_time = GameBase.level.time + 1.0f;
                PlayerClient.LookAtKiller(self, inflictor, attacker);
                self.client.ps.pmove.pm_type = Defines.PM_DEAD;
                ClientObituary(self, inflictor, attacker);
                PlayerClient.TossClientWeapon(self);
                if (GameBase.deathmatch.value != (float) 0)
                    Cmd.Help_f(self); 
    
                
                
                
                for (n = 0; n < GameBase.game.num_items; n++) {
                    if (GameBase.coop.value != (float) 0
                            && (GameItemList.itemlist[n].flags & Defines.IT_KEY) != 0)
                        self.client.resp.coop_respawn.inventory[n] = self.client.pers.inventory[n];
                    self.client.pers.inventory[n] = 0;
                }
            }
    
            
            self.client.quad_framenum = (float) 0;
            self.client.invincible_framenum = (float) 0;
            self.client.breather_framenum = (float) 0;
            self.client.enviro_framenum = (float) 0;
            self.flags &= ~Defines.FL_POWER_ARMOR;
    
            if (self.health < -40) { 
                game_import_t
                        .sound(self, Defines.CHAN_BODY, game_import_t
                                .soundindex("misc/udeath.wav"), 1.0F,
                                (float) Defines.ATTN_NORM, (float) 0);
                for (n = 0; n < 4; n++)
                    GameMisc.ThrowGib(self, "models/objects/gibs/sm_meat/tris.md2",
                            damage, Defines.GIB_ORGANIC);
                GameMisc.ThrowClientHead(self, damage);
    
                self.takedamage = Defines.DAMAGE_NO;
            } else { 
                if (self.deadflag == 0) {
    
                    player_die_i = (player_die_i + 1) % 3;
                    
                    self.client.anim_priority = Defines.ANIM_DEATH;
                    if (((int) self.client.ps.pmove.pm_flags & pmove_t.PMF_DUCKED) != 0) {
                        self.s.frame = M_Player.FRAME_crdeath1 - 1;
                        self.client.anim_end = M_Player.FRAME_crdeath5;
                    } else
                        switch (player_die_i) {
                        case 0:
                            self.s.frame = M_Player.FRAME_death101 - 1;
                            self.client.anim_end = M_Player.FRAME_death106;
                            break;
                        case 1:
                            self.s.frame = M_Player.FRAME_death201 - 1;
                            self.client.anim_end = M_Player.FRAME_death206;
                            break;
                        case 2:
                            self.s.frame = M_Player.FRAME_death301 - 1;
                            self.client.anim_end = M_Player.FRAME_death308;
                            break;
                        }
    
                    game_import_t.sound(self, Defines.CHAN_VOICE, game_import_t
                            .soundindex("*death" + (((int) Lib.rand() % 4) + 1)
                                    + ".wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
                }
            }
    
            self.deadflag = Defines.DEAD_DEAD;
    
            game_import_t.linkentity(self);
        }
    };
    static final EntThinkAdapter SP_FixCoopSpots = new EntThinkAdapter() {
    	@Override
        public String getID() { return "SP_FixCoopSpots"; }
        @Override
        public boolean think(edict_t self) {

            float[] d = {(float) 0, (float) 0, (float) 0};

            edict_t spot = null;
            EdictIterator es = null;
    
            while (true) {
                es = GameBase.G_Find(es, GameBase.findByClass,
                        "info_player_start");
    
                if (es == null)
                    return true;
                
                spot = es.o;
                
                if (spot.targetname == null)
                    continue;
                Math3D.VectorSubtract(self.s.origin, spot.s.origin, d);
                if (Math3D.VectorLength(d) < 384.0F) {
                    if ((self.targetname == null)
                            || Lib.Q_stricmp(self.targetname, spot.targetname) != 0) {
                        
                        
                        
                        
                        self.targetname = spot.targetname;
                    }
                    return true;
                }
            }
        }
    };
    static final EntThinkAdapter SP_CreateCoopSpots = new EntThinkAdapter() {
    	@Override
        public String getID() { return "SP_CreateCoopSpots"; }
        @Override
        public boolean think(edict_t self) {

            if (Lib.Q_stricmp(GameBase.level.mapname, "security") == 0) {
                edict_t spot = GameUtil.G_Spawn();
                spot.classname = "info_player_coop";
                spot.s.origin[0] = (float) (188 - 64);
                spot.s.origin[1] = -164.0F;
                spot.s.origin[2] = 80.0F;
                spot.targetname = "jail3";
                spot.s.angles[1] = 90.0F;
    
                spot = GameUtil.G_Spawn();
                spot.classname = "info_player_coop";
                spot.s.origin[0] = (float) (188 + 64);
                spot.s.origin[1] = -164.0F;
                spot.s.origin[2] = 80.0F;
                spot.targetname = "jail3";
                spot.s.angles[1] = 90.0F;
    
                spot = GameUtil.G_Spawn();
                spot.classname = "info_player_coop";
                spot.s.origin[0] = (float) (188 + 128);
                spot.s.origin[1] = -164.0F;
                spot.s.origin[2] = 80.0F;
                spot.targetname = "jail3";
                spot.s.angles[1] = 90.0F;
            }
            return true;
        }
    };
    
    static final EntPainAdapter player_pain = new EntPainAdapter() {
    	@Override
        public String getID() { return "player_pain"; }
        @Override
        public void pain(edict_t self, edict_t other, float kick, int damage) {
        }
    };
    static final EntDieAdapter body_die = new EntDieAdapter() {
    	@Override
        public String getID() { return "body_die"; }
        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {

            if (self.health < -40) {
                game_import_t.sound(self, Defines.CHAN_BODY,
                		game_import_t.soundindex("misc/udeath.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
                for (int n = 0; n < 4; n++)
                    GameMisc.ThrowGib(self, "models/objects/gibs/sm_meat/tris.md2", damage,
                            Defines.GIB_ORGANIC);
                self.s.origin[2] -= 48.0F;
                GameMisc.ThrowClientHead(self, damage);
                self.takedamage = Defines.DAMAGE_NO;
            }
        }
    };
    static edict_t pm_passent;
    
    public static final pmove_t.TraceAdapter PM_trace = new pmove_t.TraceAdapter() {
    
        @Override
        public trace_t trace(float[] start, float[] mins, float[] maxs,
                             float[] end) {
            if (pm_passent.health > 0)
                return game_import_t.trace(start, mins, maxs, end, pm_passent,
                        Defines.MASK_PLAYERSOLID);
            else
                return game_import_t.trace(start, mins, maxs, end, pm_passent,
                        Defines.MASK_DEADSOLID);
        }
    
    };

    /**
     * QUAKED info_player_start (1 0 0) (-16 -16 -24) (16 16 32) The normal
     * starting point for a level.
     */
    public static void SP_info_player_start(edict_t self) {
        if (GameBase.coop.value == (float) 0)
            return;
        if (Lib.Q_stricmp(GameBase.level.mapname, "security") == 0) {
            
            self.think = PlayerClient.SP_CreateCoopSpots;
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;
        }
    }

    /**
     * QUAKED info_player_deathmatch (1 0 1) (-16 -16 -24) (16 16 32) potential
     * spawning position for deathmatch games.
     */
    public static void SP_info_player_deathmatch(edict_t self) {
        if ((float) 0 == GameBase.deathmatch.value) {
            GameUtil.G_FreeEdict(self);
            return;
        }
        GameMisc.SP_misc_teleporter_dest.think(self);
    }

    /**
     * QUAKED info_player_coop (1 0 1) (-16 -16 -24) (16 16 32) potential
     * spawning position for coop games.
     */

    public static void SP_info_player_coop(edict_t self) {
        if ((float) 0 == GameBase.coop.value) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        boolean b = false;
        for (String s : Arrays.asList("jail2", "jail4", "mine1", "mine2", "mine3", "mine4", "lab", "boss1", "fact3", "biggun", "space", "command", "power2", "strike")) {
            if ((Lib.Q_stricmp(GameBase.level.mapname, s) == 0)) {
                b = true;
                break;
            }
        }
        if (b) {
            
            self.think = PlayerClient.SP_FixCoopSpots;
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;
        }
    }

    /**
     * QUAKED info_player_intermission (1 0 1) (-16 -16 -24) (16 16 32) The
     * deathmatch intermission point will be at one of these Use 'angles'
     * instead of 'angle', so you can set pitch or roll as well as yaw. 'pitch
     * yaw roll'
     */
    public static void SP_info_player_intermission() {
    }

    public static void ClientObituary(edict_t self, edict_t inflictor,
            edict_t attacker) {

        if (GameBase.coop.value != (float) 0 && attacker.client != null)
            GameBase.meansOfDeath |= Defines.MOD_FRIENDLY_FIRE;

        if (GameBase.deathmatch.value != (float) 0 || GameBase.coop.value != (float) 0) {
            boolean ff = (GameBase.meansOfDeath & Defines.MOD_FRIENDLY_FIRE) != 0;
            int mod = GameBase.meansOfDeath & ~Defines.MOD_FRIENDLY_FIRE;
            String message = null;

            switch (mod) {
            case Defines.MOD_SUICIDE:
                message = "suicides";
                break;
            case Defines.MOD_FALLING:
                message = "cratered";
                break;
            case Defines.MOD_CRUSH:
                message = "was squished";
                break;
            case Defines.MOD_WATER:
                message = "sank like a rock";
                break;
            case Defines.MOD_SLIME:
                message = "melted";
                break;
            case Defines.MOD_LAVA:
                message = "does a back flip into the lava";
                break;
            case Defines.MOD_EXPLOSIVE:
            case Defines.MOD_BARREL:
                message = "blew up";
                break;
            case Defines.MOD_EXIT:
                message = "found a way out";
                break;
            case Defines.MOD_TARGET_LASER:
                message = "saw the light";
                break;
            case Defines.MOD_TARGET_BLASTER:
                message = "got blasted";
                break;
            case Defines.MOD_BOMB:
            case Defines.MOD_SPLASH:
            case Defines.MOD_TRIGGER_HURT:
                message = "was in the wrong place";
                break;
            }
            if (attacker == self) {
                switch (mod) {
                case Defines.MOD_HELD_GRENADE:
                    message = "tried to put the pin back in";
                    break;
                case Defines.MOD_HG_SPLASH:
                case Defines.MOD_G_SPLASH:
                    if (PlayerClient.IsNeutral(self))
                        message = "tripped on its own grenade";
                    else if (PlayerClient.IsFemale(self))
                        message = "tripped on her own grenade";
                    else
                        message = "tripped on his own grenade";
                    break;
                case Defines.MOD_R_SPLASH:
                    if (PlayerClient.IsNeutral(self))
                        message = "blew itself up";
                    else if (PlayerClient.IsFemale(self))
                        message = "blew herself up";
                    else
                        message = "blew himself up";
                    break;
                case Defines.MOD_BFG_BLAST:
                    message = "should have used a smaller gun";
                    break;
                default:
                    if (PlayerClient.IsNeutral(self))
                        message = "killed itself";
                    else if (PlayerClient.IsFemale(self))
                        message = "killed herself";
                    else
                        message = "killed himself";
                    break;
                }
            }
            if (message != null) {
                game_import_t.bprintf(Defines.PRINT_MEDIUM,
                        self.client.pers.netname + ' ' + message + ".\n");
                if (GameBase.deathmatch.value != (float) 0)
                    self.client.resp.score--;
                self.enemy = null;
                return;
            }

            self.enemy = attacker;
            if (attacker != null && attacker.client != null) {
                String message2 = "";
                switch (mod) {
                case Defines.MOD_BLASTER:
                    message = "was blasted by";
                    break;
                case Defines.MOD_SHOTGUN:
                    message = "was gunned down by";
                    break;
                case Defines.MOD_SSHOTGUN:
                    message = "was blown away by";
                    message2 = "'s super shotgun";
                    break;
                case Defines.MOD_MACHINEGUN:
                    message = "was machinegunned by";
                    break;
                case Defines.MOD_CHAINGUN:
                    message = "was cut in half by";
                    message2 = "'s chaingun";
                    break;
                case Defines.MOD_GRENADE:
                    message = "was popped by";
                    message2 = "'s grenade";
                    break;
                case Defines.MOD_G_SPLASH:
                    message = "was shredded by";
                    message2 = "'s shrapnel";
                    break;
                case Defines.MOD_ROCKET:
                    message = "ate";
                    message2 = "'s rocket";
                    break;
                case Defines.MOD_R_SPLASH:
                    message = "almost dodged";
                    message2 = "'s rocket";
                    break;
                case Defines.MOD_HYPERBLASTER:
                    message = "was melted by";
                    message2 = "'s hyperblaster";
                    break;
                case Defines.MOD_RAILGUN:
                    message = "was railed by";
                    break;
                case Defines.MOD_BFG_LASER:
                    message = "saw the pretty lights from";
                    message2 = "'s BFG";
                    break;
                case Defines.MOD_BFG_BLAST:
                    message = "was disintegrated by";
                    message2 = "'s BFG blast";
                    break;
                case Defines.MOD_BFG_EFFECT:
                    message = "couldn't hide from";
                    message2 = "'s BFG";
                    break;
                case Defines.MOD_HANDGRENADE:
                    message = "caught";
                    message2 = "'s handgrenade";
                    break;
                case Defines.MOD_HG_SPLASH:
                    message = "didn't see";
                    message2 = "'s handgrenade";
                    break;
                case Defines.MOD_HELD_GRENADE:
                    message = "feels";
                    message2 = "'s pain";
                    break;
                case Defines.MOD_TELEFRAG:
                    message = "tried to invade";
                    message2 = "'s personal space";
                    break;
                }
                if (message != null) {
                    game_import_t.bprintf(Defines.PRINT_MEDIUM,
                            self.client.pers.netname + ' ' + message + ' '
                                    + attacker.client.pers.netname + ' '
                                    + message2 + '\n');
                    if (GameBase.deathmatch.value != (float) 0) {
                        if (ff)
                            attacker.client.resp.score--;
                        else
                            attacker.client.resp.score++;
                    }
                    return;
                }
            }
        }

        game_import_t.bprintf(Defines.PRINT_MEDIUM, self.client.pers.netname
                + " died.\n");
        if (GameBase.deathmatch.value != (float) 0)
            self.client.resp.score--;
    }

    /**
     * This is only called when the game first initializes in single player, but
     * is called after each death and level change in deathmatch. 
     */
    public static void InitClientPersistant(gclient_t client) {

        client.pers = new client_persistant_t();

        gitem_t item = GameItems.FindItem("Blaster");
        client.pers.selected_item = GameItems.ITEM_INDEX(item);
        client.pers.inventory[client.pers.selected_item] = 1;

        /*
         * Give shotgun. item = FindItem("Shotgun"); client.pers.selected_item =
         * ITEM_INDEX(item); client.pers.inventory[client.pers.selected_item] =
         * 1;
         */

        client.pers.weapon = item;

        client.pers.health = 100;
        client.pers.max_health = 100;

        client.pers.max_bullets = 200;
        client.pers.max_shells = 100;
        client.pers.max_rockets = 50;
        client.pers.max_grenades = 50;
        client.pers.max_cells = 200;
        client.pers.max_slugs = 50;

        client.pers.connected = true;
    }

    public static void InitClientResp(gclient_t client) {
        
        client.resp.clear(); 
        client.resp.enterframe = GameBase.level.framenum;
        client.resp.coop_respawn.set(client.pers);
    }

    /**
     * Some information that should be persistant, like health, is still stored
     * in the edict structure, so it needs to be mirrored out to the client
     * structure before all the edicts are wiped. 
     */
    public static void SaveClientData() {

        for (int i = 0; i < GameBase.game.maxclients; i++) {
            edict_t ent = GameBase.g_edicts[1 + i];
            if (!ent.inuse)
                continue;

            GameBase.game.clients[i].pers.health = ent.health;
            GameBase.game.clients[i].pers.max_health = ent.max_health;
            GameBase.game.clients[i].pers.savedFlags = (ent.flags & (Defines.FL_GODMODE
                    | Defines.FL_NOTARGET | Defines.FL_POWER_ARMOR));

            if (GameBase.coop.value != (float) 0)
                GameBase.game.clients[i].pers.score = ent.client.resp.score;
        }
    }

    public static void FetchClientEntData(edict_t ent) {
        ent.health = ent.client.pers.health;
        ent.max_health = ent.client.pers.max_health;
        ent.flags |= ent.client.pers.savedFlags;
        if (GameBase.coop.value != (float) 0)
            ent.client.resp.score = ent.client.pers.score;
    }

    /**
     * Returns the distance to the nearest player from the given spot.
     */
    static float PlayersRangeFromSpot(edict_t spot) {
        float[] v = {(float) 0, (float) 0, (float) 0};

        float bestplayerdistance = 9999999.0F;

        for (int n = 1; (float) n <= GameBase.maxclients.value; n++) {
            edict_t player = GameBase.g_edicts[n];

            if (!player.inuse)
                continue;

            if (player.health <= 0)
                continue;

            Math3D.VectorSubtract(spot.s.origin, player.s.origin, v);
            float playerdistance = Math3D.VectorLength(v);

            if (playerdistance < bestplayerdistance)
                bestplayerdistance = playerdistance;
        }

        return bestplayerdistance;
    }

    /**
     * Go to a random point, but NOT the two points closest to other players.
     */
    public static edict_t SelectRandomDeathmatchSpawnPoint() {
        float range2;

        float range1 = range2 = 99999.0F;
        edict_t spot2;
        edict_t spot1 = spot2 = null;

        EdictIterator es = null;

        edict_t spot = null;
        int count = 0;
        while ((es = GameBase.G_Find(es, GameBase.findByClass,
                "info_player_deathmatch")) != null) {
            spot = es.o;
            count++;
            float range = PlayersRangeFromSpot(spot);
            if (range < range1) {
                range1 = range;
                spot1 = spot;
            } else if (range < range2) {
                range2 = range;
                spot2 = spot;
            }
        }

        if (count == 0)
            return null;

        if (count <= 2) {
            spot1 = spot2 = null;
        } else
            count -= 2;

        int selection = (int) Lib.rand() % count;

        spot = null;
        es = null;
        do {
            es = GameBase.G_Find(es, GameBase.findByClass,
                    "info_player_deathmatch");
            
            if (es == null) 
                break;
            
            spot = es.o;
            if (spot == spot1 || spot == spot2)
                selection++;
        } while (selection-- > 0);

        return spot;
    }

    /** 
	 * If turned on in the dmflags, select a spawn point far away from other players.
     */
    static edict_t SelectFarthestDeathmatchSpawnPoint() {

        edict_t spot = null;
        edict_t bestspot = null;
        float bestdistance = (float) 0;

        EdictIterator es = null;
        while ((es = GameBase.G_Find(es, GameBase.findByClass,
                "info_player_deathmatch")) != null) {
            spot = es.o;
            float bestplayerdistance = PlayersRangeFromSpot(spot);

            if (bestplayerdistance > bestdistance) {
                bestspot = spot;
                bestdistance = bestplayerdistance;
            }
        }

        if (bestspot != null) {
            return bestspot;
        }


        EdictIterator edit = GameBase.G_Find(null, GameBase.findByClass,
                "info_player_deathmatch");
        if (edit == null)
            return null;
        
        return edit.o;
    }

    
    public static edict_t SelectDeathmatchSpawnPoint() {
        if (0 != ((int) (GameBase.dmflags.value) & Defines.DF_SPAWN_FARTHEST))
            return SelectFarthestDeathmatchSpawnPoint();
        else
            return SelectRandomDeathmatchSpawnPoint();
    }

    public static edict_t SelectCoopSpawnPoint(edict_t ent) {


        int index = ent.client.index;

        
        if (index == 0)
            return null;

        edict_t spot = null;
        EdictIterator es = null;

        
        while (true) {

            es = GameBase.G_Find(es, GameBase.findByClass,
                    "info_player_coop");
                    
            if (es == null)
                return null;
            
            spot = es.o;
                
            if (spot == null)
                return null;

            String target = spot.targetname;
            if (target == null)
                target = "";
            if (Lib.Q_stricmp(GameBase.game.spawnpoint, target) == 0) { 
                
                index--;
                if (0 == index)
                    return spot; 
            }
        }

    }

    /**
     * Chooses a player start, deathmatch start, coop start, etc.
     */
    public static void SelectSpawnPoint(edict_t ent, float[] origin,
            float[] angles) {
        edict_t spot = null;

        if (GameBase.deathmatch.value != (float) 0)
            spot = SelectDeathmatchSpawnPoint();
        else if (GameBase.coop.value != (float) 0)
            spot = SelectCoopSpawnPoint(ent);

        if (null == spot) {
            EdictIterator es = null;
            while ((es = GameBase.G_Find(es, GameBase.findByClass,
                    "info_player_start")) != null) {
                spot = es.o;

                if (GameBase.game.spawnpoint.length() == 0
                        && spot.targetname == null)
                    break;

                if (GameBase.game.spawnpoint.length() == 0
                        || spot.targetname == null)
                    continue;

                if (Lib.Q_stricmp(GameBase.game.spawnpoint, spot.targetname) == 0)
                    break;
            }

            if (null == spot) {
                if (GameBase.game.spawnpoint.length() == 0) { 
                    
                    
                    es = GameBase.G_Find(es, GameBase.findByClass,
                            "info_player_start");
                    
                    if (es != null)
                        spot = es.o;
                }
                if (null == spot)
                {
                    game_import_t.error("Couldn't find spawn point "
                            + GameBase.game.spawnpoint + '\n');
                    return;
                }
            }
        }

        Math3D.VectorCopy(spot.s.origin, origin);
        origin[2] += 9.0F;
        Math3D.VectorCopy(spot.s.angles, angles);
    }


    public static void InitBodyQue() {

        GameBase.level.body_que = 0;
        for (int i = 0; i < Defines.BODY_QUEUE_SIZE; i++) {
            edict_t ent = GameUtil.G_Spawn();
            ent.classname = "bodyque";
        }
    }

    public static void CopyToBodyQue(edict_t ent) {


        int i = (int) GameBase.maxclients.value + GameBase.level.body_que + 1;
        edict_t body = GameBase.g_edicts[i];
        GameBase.level.body_que = (GameBase.level.body_que + 1)
                % Defines.BODY_QUEUE_SIZE;

        

        game_import_t.unlinkentity(ent);

        game_import_t.unlinkentity(body);
        body.s = ent.s.getClone();

        body.s.number = body.index;

        body.svflags = ent.svflags;
        Math3D.VectorCopy(ent.mins, body.mins);
        Math3D.VectorCopy(ent.maxs, body.maxs);
        Math3D.VectorCopy(ent.absmin, body.absmin);
        Math3D.VectorCopy(ent.absmax, body.absmax);
        Math3D.VectorCopy(ent.size, body.size);
        body.solid = ent.solid;
        body.clipmask = ent.clipmask;
        body.owner = ent.owner;
        body.movetype = ent.movetype;

        body.die = PlayerClient.body_die;
        body.takedamage = Defines.DAMAGE_YES;

        game_import_t.linkentity(body);
    }

    public static void respawn(edict_t self) {
        if (GameBase.deathmatch.value != (float) 0 || GameBase.coop.value != (float) 0) {
            
            if (self.movetype != Defines.MOVETYPE_NOCLIP)
                CopyToBodyQue(self);
            self.svflags &= ~Defines.SVF_NOCLIENT;
            PutClientInServer(self);

            
            self.s.event = Defines.EV_PLAYER_TELEPORT;

            
            self.client.ps.pmove.pm_flags = (byte) pmove_t.PMF_TIME_TELEPORT;
            self.client.ps.pmove.pm_time = (byte) 14;

            self.client.respawn_time = GameBase.level.time;

            return;
        }

        
        game_import_t.AddCommandString("menu_loadgame\n");
    }

    private static boolean passwdOK(String i1, String i2) {
        return !(i1.length() != 0 && !"none".equals(i1) && !i1.equals(i2));
    }

    /**
     * Only called when pers.spectator changes note that resp.spectator should
     * be the opposite of pers.spectator here
     */
    public static void spectator_respawn(edict_t ent) {


        if (ent.client.pers.spectator) {
            String value = Info.Info_ValueForKey(ent.client.pers.userinfo,
                    "spectator");

            if (!passwdOK(GameBase.spectator_password.string, value)) {
                game_import_t.cprintf(ent, Defines.PRINT_HIGH,
                        "Spectator password incorrect.\n");
                ent.client.pers.spectator = false;
                game_import_t.WriteByte(Defines.svc_stufftext);
                game_import_t.WriteString("spectator 0\n");
                game_import_t.unicast(ent, true);
                return;
            }


            int numspec;
            int i;
            for (i = 1, numspec = 0; (float) i <= GameBase.maxclients.value; i++)
                if (GameBase.g_edicts[i].inuse
                        && GameBase.g_edicts[i].client.pers.spectator)
                    numspec++;

            if ((float) numspec >= GameBase.maxspectators.value) {
                game_import_t.cprintf(ent, Defines.PRINT_HIGH,
                        "Server spectator limit is full.");
                ent.client.pers.spectator = false;
                
                game_import_t.WriteByte(Defines.svc_stufftext);
                game_import_t.WriteString("spectator 0\n");
                game_import_t.unicast(ent, true);
                return;
            }
        } else {


            String value = Info.Info_ValueForKey(ent.client.pers.userinfo,
                    "password");
            if (!passwdOK(GameBase.spectator_password.string, value)) {
                game_import_t.cprintf(ent, Defines.PRINT_HIGH,
                        "Password incorrect.\n");
                ent.client.pers.spectator = true;
                game_import_t.WriteByte(Defines.svc_stufftext);
                game_import_t.WriteString("spectator 1\n");
                game_import_t.unicast(ent, true);
                return;
            }
        }

        
        ent.client.resp.score = ent.client.pers.score = 0;

        ent.svflags &= ~Defines.SVF_NOCLIENT;
        PutClientInServer(ent);

        
        if (!ent.client.pers.spectator) {
            
            game_import_t.WriteByte(Defines.svc_muzzleflash);
            
            game_import_t.WriteShort(ent.index);

            game_import_t.WriteByte(Defines.MZ_LOGIN);
            game_import_t.multicast(ent.s.origin, Defines.MULTICAST_PVS);

            
            ent.client.ps.pmove.pm_flags = (byte) pmove_t.PMF_TIME_TELEPORT;
            ent.client.ps.pmove.pm_time = (byte) 14;
        }

        ent.client.respawn_time = GameBase.level.time;

        if (ent.client.pers.spectator)
            game_import_t.bprintf(Defines.PRINT_HIGH, ent.client.pers.netname
                    + " has moved to the sidelines\n");
        else
            game_import_t.bprintf(Defines.PRINT_HIGH, ent.client.pers.netname
                    + " joined the game\n");
    }

    /**
     * Called when a player connects to a server or respawns in a deathmatch.
     */
    public static void PutClientInServer(edict_t ent) {
        float[] spawn_origin = {(float) 0, (float) 0, (float) 0}, spawn_angles = {(float) 0, (float) 0, (float) 0};
        client_persistant_t saved = new client_persistant_t();
        client_respawn_t resp = new client_respawn_t();

        
        
        
        SelectSpawnPoint(ent, spawn_origin, spawn_angles);

        int index = ent.index - 1;
        gclient_t client = ent.client;

        
        if (GameBase.deathmatch.value != (float) 0) {

            resp.set(client.resp);
            String userinfo = client.pers.userinfo;
            InitClientPersistant(client);
            
            userinfo = ClientUserinfoChanged(ent, userinfo);
            
        } else if (GameBase.coop.value != (float) 0) {

            resp.set(client.resp);

            String userinfo = client.pers.userinfo;

            resp.coop_respawn.game_helpchanged = client.pers.game_helpchanged;
            resp.coop_respawn.helpchanged = client.pers.helpchanged;
            client.pers.set(resp.coop_respawn);
            userinfo = ClientUserinfoChanged(ent, userinfo);
            if (resp.score > client.pers.score)
                client.pers.score = resp.score;
        } else {
            resp.clear();
        }

        
        saved.set(client.pers);
        client.clear();
        client.pers.set(saved);
        if (client.pers.health <= 0)
            InitClientPersistant(client);

        client.resp.set(resp);

        
        FetchClientEntData(ent);

        
        ent.groundentity = null;
        ent.client = GameBase.game.clients[index];
        ent.takedamage = Defines.DAMAGE_AIM;
        ent.movetype = Defines.MOVETYPE_WALK;
        ent.viewheight = 22;
        ent.inuse = true;
        ent.classname = "player";
        ent.mass = 200;
        ent.solid = Defines.SOLID_BBOX;
        ent.deadflag = Defines.DEAD_NO;
        ent.air_finished = GameBase.level.time + 12.0F;
        ent.clipmask = Defines.MASK_PLAYERSOLID;
        ent.model = "players/male/tris.md2";
        ent.pain = PlayerClient.player_pain;
        ent.die = PlayerClient.player_die;
        ent.waterlevel = 0;
        ent.watertype = 0;
        ent.flags &= ~Defines.FL_NO_KNOCKBACK;
        ent.svflags &= ~Defines.SVF_DEADMONSTER;

        float[] mins = {-16.0F, -16.0F, -24.0F};
        Math3D.VectorCopy(mins, ent.mins);
        float[] maxs = {16.0F, 16.0F, 32.0F};
        Math3D.VectorCopy(maxs, ent.maxs);
        Math3D.VectorClear(ent.velocity);

        
        ent.client.ps.clear();     

        client.ps.pmove.origin[0] = (short) (spawn_origin[0] * 8.0F);
        client.ps.pmove.origin[1] = (short) (spawn_origin[1] * 8.0F);
        client.ps.pmove.origin[2] = (short) (spawn_origin[2] * 8.0F);

        if (GameBase.deathmatch.value != (float) 0
                && 0 != ((int) GameBase.dmflags.value & Defines.DF_FIXED_FOV)) {
            client.ps.fov = 90.0F;
        } else {
            client.ps.fov = (float) Lib.atoi(Info.Info_ValueForKey(
                    client.pers.userinfo, "fov"));
            if (client.ps.fov < 1.0F)
                client.ps.fov = 90.0F;
            else if (client.ps.fov > 160.0F)
                client.ps.fov = 160.0F;
        }

        client.ps.gunindex = game_import_t
                .modelindex(client.pers.weapon.view_model);

        
        ent.s.effects = 0;
        ent.s.modelindex = 255; 
        ent.s.modelindex2 = 255; 
        
        
        ent.s.skinnum = ent.index - 1;

        ent.s.frame = 0;
        Math3D.VectorCopy(spawn_origin, ent.s.origin);
        ent.s.origin[2] += 1.0F;
        Math3D.VectorCopy(ent.s.origin, ent.s.old_origin);

        
        for (int i = 0; i < 3; i++) {
            client.ps.pmove.delta_angles[i] = (short) Math3D
                    .ANGLE2SHORT(spawn_angles[i] - client.resp.cmd_angles[i]);
        }

        ent.s.angles[Defines.PITCH] = (float) 0;
        ent.s.angles[Defines.YAW] = spawn_angles[Defines.YAW];
        ent.s.angles[Defines.ROLL] = (float) 0;
        Math3D.VectorCopy(ent.s.angles, client.ps.viewangles);
        Math3D.VectorCopy(ent.s.angles, client.v_angle);

        
        if (client.pers.spectator) {
            client.chase_target = null;

            client.resp.spectator = true;

            ent.movetype = Defines.MOVETYPE_NOCLIP;
            ent.solid = Defines.SOLID_NOT;
            ent.svflags |= Defines.SVF_NOCLIENT;
            ent.client.ps.gunindex = 0;
            game_import_t.linkentity(ent);
            return;
        } else
            client.resp.spectator = false;

        if (!GameUtil.KillBox(ent)) { 
        }

        game_import_t.linkentity(ent);

        
        client.newweapon = client.pers.weapon;
        PlayerWeapon.ChangeWeapon(ent);
    }

    /**
     * A client has just connected to the server in deathmatch mode, so clear
     * everything out before starting them. 
     */
    public static void ClientBeginDeathmatch(edict_t ent) {
        GameUtil.G_InitEdict(ent, ent.index);

        InitClientResp(ent.client);

        
        PutClientInServer(ent);

        if (GameBase.level.intermissiontime != (float) 0) {
            PlayerHud.MoveClientToIntermission(ent);
        } else {
            
            game_import_t.WriteByte(Defines.svc_muzzleflash);
            
            game_import_t.WriteShort(ent.index);
            game_import_t.WriteByte(Defines.MZ_LOGIN);
            game_import_t.multicast(ent.s.origin, Defines.MULTICAST_PVS);
        }

        game_import_t.bprintf(Defines.PRINT_HIGH, ent.client.pers.netname
                + " entered the game\n");

        
        PlayerView.ClientEndServerFrame(ent);
    }

    /**
     * Called when a client has finished connecting, and is ready to be placed
     * into the game. This will happen every level load. 
     */
    public static void ClientBegin(edict_t ent) {


        ent.client = GameBase.game.clients[ent.index - 1];

        if (GameBase.deathmatch.value != (float) 0) {
            ClientBeginDeathmatch(ent);
            return;
        }

        
        
        if (ent.inuse) {
            
            
            
            
            for (int i = 0; i < 3; i++)
                ent.client.ps.pmove.delta_angles[i] = (short) Math3D
                        .ANGLE2SHORT(ent.client.ps.viewangles[i]);
        } else {
            
            
            
            GameUtil.G_InitEdict(ent, ent.index);
            ent.classname = "player";
            InitClientResp(ent.client);
            PutClientInServer(ent);
        }

        if (GameBase.level.intermissiontime != (float) 0) {
            PlayerHud.MoveClientToIntermission(ent);
        } else {
            
            if (GameBase.game.maxclients > 1) {
                game_import_t.WriteByte(Defines.svc_muzzleflash);
                game_import_t.WriteShort(ent.index);
                game_import_t.WriteByte(Defines.MZ_LOGIN);
                game_import_t.multicast(ent.s.origin, Defines.MULTICAST_PVS);

                game_import_t.bprintf(Defines.PRINT_HIGH, ent.client.pers.netname
                        + " entered the game\n");
            }
        }

        
        PlayerView.ClientEndServerFrame(ent);
    }

    /**
     * Called whenever the player updates a userinfo variable.
     * 
     * The game can override any of the settings in place (forcing skins or
     * names, etc) before copying it off. 
     *
     */
    public static String ClientUserinfoChanged(edict_t ent, String userinfo) {


        if (!Info.Info_Validate(userinfo)) {
            return "\\name\\badinfo\\skin\\male/grunt";
        }


        String s = Info.Info_ValueForKey(userinfo, "name");

        ent.client.pers.netname = s;

        
        s = Info.Info_ValueForKey(userinfo, "spectator");
        
        ent.client.pers.spectator = GameBase.deathmatch.value != (float) 0 && !"0".equals(s);

        
        s = Info.Info_ValueForKey(userinfo, "skin");

        int playernum = ent.index - 1;

        
        game_import_t.configstring(Defines.CS_PLAYERSKINS + playernum,
                ent.client.pers.netname + '\\' + s);

        
        if (GameBase.deathmatch.value != (float) 0
                && 0 != ((int) GameBase.dmflags.value & Defines.DF_FIXED_FOV)) {
            ent.client.ps.fov = 90.0F;
        } else {
            ent.client.ps.fov = (float) Lib
                    .atoi(Info.Info_ValueForKey(userinfo, "fov"));
            if (ent.client.ps.fov < 1.0F)
                ent.client.ps.fov = 90.0F;
            else if (ent.client.ps.fov > 160.0F)
                ent.client.ps.fov = 160.0F;
        }

        
        s = Info.Info_ValueForKey(userinfo, "hand");
        if (s.length() > 0) {
            ent.client.pers.hand = Lib.atoi(s);
        }

        
        ent.client.pers.userinfo = userinfo;

        return userinfo;
    }

    /**
     * Called when a player begins connecting to the server. The game can refuse
     * entrance to a client by returning false. If the client is allowed, the
     * connection process will continue and eventually get to ClientBegin()
     * Changing levels will NOT cause this to be called again, but loadgames
     * will. 
     */
    public static boolean ClientConnect(edict_t ent, String userinfo) {


        String value = Info.Info_ValueForKey(userinfo, "ip");
        if (GameSVCmds.SV_FilterPacket(value)) {
            userinfo = Info.Info_SetValueForKey(userinfo, "rejmsg", "Banned.");
            return false;
        }

        
        value = Info.Info_ValueForKey(userinfo, "spectator");
        if (GameBase.deathmatch.value != (float) 0 && value.length() != 0
                && 0 != Lib.strcmp(value, "0")) {

            if (!passwdOK(GameBase.spectator_password.string, value)) {
                userinfo = Info.Info_SetValueForKey(userinfo, "rejmsg",
                        "Spectator password required or incorrect.");
                return false;
            }


            int numspec;
            for (int i = numspec = 0; (float) i < GameBase.maxclients.value; i++)
                if (GameBase.g_edicts[i + 1].inuse
                        && GameBase.g_edicts[i + 1].client.pers.spectator)
                    numspec++;

            if ((float) numspec >= GameBase.maxspectators.value) {
                userinfo = Info.Info_SetValueForKey(userinfo, "rejmsg",
                        "Server spectator limit is full.");
                return false;
            }
        } else {
            
            value = Info.Info_ValueForKey(userinfo, "password");
            if (!passwdOK(GameBase.spectator_password.string, value)) {
                userinfo = Info.Info_SetValueForKey(userinfo, "rejmsg",
                        "Password required or incorrect.");
                return false;
            }
        }

        
        ent.client = GameBase.game.clients[ent.index - 1];

        
        
        if (!ent.inuse) {
            
            InitClientResp(ent.client);
            if (!GameBase.game.autosaved || null == ent.client.pers.weapon)
                InitClientPersistant(ent.client);
        }

        userinfo = ClientUserinfoChanged(ent, userinfo);

        if (GameBase.game.maxclients > 1)
            game_import_t.dprintf(ent.client.pers.netname + " connected\n");

        ent.svflags = 0; 
        ent.client.pers.connected = true;
        return true;
    }

    /**
     * Called when a player drops from the server. Will not be called between levels. 
     */
    public static void ClientDisconnect(edict_t ent) {

        if (ent.client == null)
            return;

        game_import_t.bprintf(Defines.PRINT_HIGH, ent.client.pers.netname
                + " disconnected\n");

        
        game_import_t.WriteByte(Defines.svc_muzzleflash);
        game_import_t.WriteShort(ent.index);
        game_import_t.WriteByte(Defines.MZ_LOGOUT);
        game_import_t.multicast(ent.s.origin, Defines.MULTICAST_PVS);

        game_import_t.unlinkentity(ent);
        ent.s.modelindex = 0;
        ent.solid = Defines.SOLID_NOT;
        ent.inuse = false;
        ent.classname = "disconnected";
        ent.client.pers.connected = false;

        int playernum = ent.index - 1;
        game_import_t.configstring(Defines.CS_PLAYERSKINS + playernum, "");
    }

    /*
     * static int CheckBlock(int c) 
     * { 
     * 		int v, i; 
     * 		v = 0; 
     * 		for (i = 0; i < c; i++)
     *			v += ((byte *) b)[i]; 
     *		return v; 
     * }
     * 
     * public static void PrintPmove(pmove_t * pm) 
     * { 
     *		unsigned c1, c2;
     * 
     * 		c1 = CheckBlock(&pm.s, sizeof(pm.s));
     * 		c2 = CheckBlock(&pm.cmd, sizeof(pm.cmd)); 
     *      Com_Printf("sv %3i:%i %i\n", pm.cmd.impulse, c1, c2); 
     * }
     */

    /**
     * This will be called once for each client frame, which will usually be a
     * couple times for each server frame.
     */
    public static void ClientThink(edict_t ent, usercmd_t ucmd) {

        GameBase.level.current_entity = ent;
        gclient_t client = ent.client;

        if (GameBase.level.intermissiontime != (float) 0) {
            client.ps.pmove.pm_type = Defines.PM_FREEZE;
            
            if (GameBase.level.time > GameBase.level.intermissiontime + 5.0f
                    && 0 != ((int) ucmd.buttons & Defines.BUTTON_ANY))
                GameBase.level.exitintermission = true;
            return;
        }

        PlayerClient.pm_passent = ent;

        int i;
        edict_t other;
        if (ent.client.chase_target != null) {

            client.resp.cmd_angles[0] = Math3D.SHORT2ANGLE((int) ucmd.angles[0]);
            client.resp.cmd_angles[1] = Math3D.SHORT2ANGLE((int) ucmd.angles[1]);
            client.resp.cmd_angles[2] = Math3D.SHORT2ANGLE((int) ucmd.angles[2]);

        } else {


            pmove_t pm = new pmove_t();

            if (ent.movetype == Defines.MOVETYPE_NOCLIP)
                client.ps.pmove.pm_type = Defines.PM_SPECTATOR;
            else if (ent.s.modelindex != 255)
                client.ps.pmove.pm_type = Defines.PM_GIB;
            else if (ent.deadflag != 0)
                client.ps.pmove.pm_type = Defines.PM_DEAD;
            else
                client.ps.pmove.pm_type = Defines.PM_NORMAL;

            client.ps.pmove.gravity = (short) GameBase.sv_gravity.value;
            pm.s.set(client.ps.pmove);

            for (i = 0; i < 3; i++) {
                pm.s.origin[i] = (short) (ent.s.origin[i] * 8.0F);
                pm.s.velocity[i] = (short) (ent.velocity[i] * 8.0F);
            }

            if (client.old_pmove.equals(pm.s)) {
                pm.snapinitial = true;
                
            }

            
            pm.cmd.set(ucmd);

            pm.trace = PlayerClient.PM_trace; 
            pm.pointcontents = GameBase.gi.pointcontents;

            
            game_import_t.Pmove(pm);

            
            client.ps.pmove.set(pm.s);
            client.old_pmove.set(pm.s);

            for (i = 0; i < 3; i++) {
                ent.s.origin[i] = (float) pm.s.origin[i] * 0.125f;
                ent.velocity[i] = (float) pm.s.velocity[i] * 0.125f;
            }

            Math3D.VectorCopy(pm.mins, ent.mins);
            Math3D.VectorCopy(pm.maxs, ent.maxs);

            client.resp.cmd_angles[0] = Math3D.SHORT2ANGLE((int) ucmd.angles[0]);
            client.resp.cmd_angles[1] = Math3D.SHORT2ANGLE((int) ucmd.angles[1]);
            client.resp.cmd_angles[2] = Math3D.SHORT2ANGLE((int) ucmd.angles[2]);

            if (ent.groundentity != null && null == pm.groundentity
                    && ((int) pm.cmd.upmove >= 10) && (pm.waterlevel == 0)) {
                game_import_t.sound(ent, Defines.CHAN_VOICE, game_import_t
                        .soundindex("*jump1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
                PlayerWeapon.PlayerNoise(ent, ent.s.origin, Defines.PNOISE_SELF);
            }

            ent.viewheight = (int) pm.viewheight;
            ent.waterlevel = pm.waterlevel;
            ent.watertype = pm.watertype;
            ent.groundentity = pm.groundentity;
            if (pm.groundentity != null)
                ent.groundentity_linkcount = pm.groundentity.linkcount;

            if (ent.deadflag != 0) {
                client.ps.viewangles[Defines.ROLL] = 40.0F;
                client.ps.viewangles[Defines.PITCH] = -15.0F;
                client.ps.viewangles[Defines.YAW] = client.killer_yaw;
            } else {
                Math3D.VectorCopy(pm.viewangles, client.v_angle);
                Math3D.VectorCopy(pm.viewangles, client.ps.viewangles);
            }

            game_import_t.linkentity(ent);

            if (ent.movetype != Defines.MOVETYPE_NOCLIP)
                GameBase.G_TouchTriggers(ent);

            
            for (i = 0; i < pm.numtouch; i++) {
                other = pm.touchents[i];
                int j;
                for (j = 0; j < i; j++)
                    if (pm.touchents[j] == other)
                        break;
                if (j != i)
                    continue; 
                if (other.touch == null)
                    continue;
                other.touch.touch(other, ent, GameBase.dummyplane, null);
            }

        }

        client.oldbuttons = client.buttons;
        client.buttons = (int) ucmd.buttons;
        client.latched_buttons |= client.buttons & ~client.oldbuttons;

        
        
        ent.light_level = (int) ucmd.lightlevel;

        
        if ((client.latched_buttons & Defines.BUTTON_ATTACK) != 0) {
            if (client.resp.spectator) {

                client.latched_buttons = 0;

                if (client.chase_target != null) {
                    client.chase_target = null;
                    client.ps.pmove.pm_flags = (byte) ((int) client.ps.pmove.pm_flags & ~pmove_t.PMF_NO_PREDICTION);
                } else
                    GameChase.GetChaseTarget(ent);

            } else if (!client.weapon_thunk) {
                client.weapon_thunk = true;
                PlayerWeapon.Think_Weapon(ent);
            }
        }

        if (client.resp.spectator) {
            if ((int) ucmd.upmove >= 10) {
                if (0 == ((int) client.ps.pmove.pm_flags & pmove_t.PMF_JUMP_HELD)) {
                    client.ps.pmove.pm_flags = (byte) ((int) client.ps.pmove.pm_flags | pmove_t.PMF_JUMP_HELD);
                    if (client.chase_target != null)
                        GameChase.ChaseNext(ent);
                    else
                        GameChase.GetChaseTarget(ent);
                }
            } else
                client.ps.pmove.pm_flags = (byte) ((int) client.ps.pmove.pm_flags & ~pmove_t.PMF_JUMP_HELD);
        }

        
        for (i = 1; (float) i <= GameBase.maxclients.value; i++) {
            other = GameBase.g_edicts[i];
            if (other.inuse && other.client.chase_target == ent)
                GameChase.UpdateChaseCam(other);
        }
    }

    /**
     * This will be called once for each server frame, before running any other
     * entities in the world. 
     */
    public static void ClientBeginServerFrame(edict_t ent) {

        if (GameBase.level.intermissiontime != (float) 0)
            return;

        gclient_t client = ent.client;

        if (GameBase.deathmatch.value != (float) 0
                && client.pers.spectator != client.resp.spectator
                && (GameBase.level.time - client.respawn_time) >= 5.0F) {
            spectator_respawn(ent);
            return;
        }

        
        if (!client.weapon_thunk && !client.resp.spectator)
            PlayerWeapon.Think_Weapon(ent);
        else
            client.weapon_thunk = false;

        if (ent.deadflag != 0) {
            
            if (GameBase.level.time > client.respawn_time) {

                int buttonMask;
                if (GameBase.deathmatch.value != (float) 0)
                    buttonMask = Defines.BUTTON_ATTACK;
                else
                    buttonMask = -1;

                if ((client.latched_buttons & buttonMask) != 0
                        || (GameBase.deathmatch.value != (float) 0 && 0 != ((int) GameBase.dmflags.value & Defines.DF_FORCE_RESPAWN))) {
                    respawn(ent);
                    client.latched_buttons = 0;
                }
            }
            return;
        }

        
        if (GameBase.deathmatch.value != (float) 0)
            if (!GameUtil.visible(ent, PlayerTrail.LastSpot()))
                PlayerTrail.Add(ent.s.old_origin);

        client.latched_buttons = 0;
    }

    /** 
     * Returns true, if the players gender flag was set to female.
     */
    public static boolean IsFemale(edict_t ent) {

        if (null == ent.client)
            return false;

        char info = Info.Info_ValueForKey(ent.client.pers.userinfo, "gender")
                .charAt(0);
        return (int) info == (int) 'f' || (int) info == (int) 'F';
    }

    /**
     * Returns true, if the players gender flag was neither set to female nor to
     * male.
     */
    public static boolean IsNeutral(edict_t ent) {

        if (ent.client == null)
            return false;

        char info = Info.Info_ValueForKey(ent.client.pers.userinfo, "gender")
                .charAt(0);

        return (int) info != (int) 'f' && (int) info != (int) 'F' && (int) info != (int) 'm' && (int) info != (int) 'M';
    }

    /**
     * Changes the camera view to look at the killer.
     */
    public static void LookAtKiller(edict_t self, edict_t inflictor,
            edict_t attacker) {
        float[] dir = {(float) 0, (float) 0, (float) 0};

        edict_t world = GameBase.g_edicts[0];
    
        if (attacker != null && attacker != world && attacker != self) {
            Math3D.VectorSubtract(attacker.s.origin, self.s.origin, dir);
        } else if (inflictor != null && inflictor != world && inflictor != self) {
            Math3D.VectorSubtract(inflictor.s.origin, self.s.origin, dir);
        } else {
            self.client.killer_yaw = self.s.angles[Defines.YAW];
            return;
        }
    
        if (dir[0] != (float) 0)
            self.client.killer_yaw = (float) (180.0 / Math.PI * Math.atan2(
                    (double) dir[1], (double) dir[0]));
        else {
            self.client.killer_yaw = (float) 0;
            if (dir[1] > (float) 0)
                self.client.killer_yaw = 90.0F;
            else if (dir[1] < (float) 0)
                self.client.killer_yaw = -90.0F;
        }
        if (self.client.killer_yaw < (float) 0)
            self.client.killer_yaw += 360.0F;
    
    }
    
    
    /** 
     * Drop items and weapons in deathmatch games. 
     */ 
    public static void TossClientWeapon(edict_t self) {

        if (GameBase.deathmatch.value == (float) 0)
            return;

        gitem_t item = self.client.pers.weapon;
        if (0 == self.client.pers.inventory[self.client.ammo_index])
            item = null;
        if (item != null && (Lib.strcmp(item.pickup_name, "Blaster") == 0))
            item = null;

        boolean quad;
        if (0 == ((int) (GameBase.dmflags.value) & Defines.DF_QUAD_DROP))
            quad = false;
        else
            quad = (self.client.quad_framenum > (float) (GameBase.level.framenum + 10));

        float spread;
        if (item != null && quad)
            spread = 22.5f;
        else
            spread = 0.0f;

        edict_t drop;
        if (item != null) {
            self.client.v_angle[Defines.YAW] -= spread;
            drop = GameItems.Drop_Item(self, item);
            self.client.v_angle[Defines.YAW] += spread;
            drop.spawnflags = Defines.DROPPED_PLAYER_ITEM;
        }
    
        if (quad) {
            self.client.v_angle[Defines.YAW] += spread;
            drop = GameItems.Drop_Item(self, GameItems
                    .FindItemByClassname("item_quad"));
            self.client.v_angle[Defines.YAW] -= spread;
            drop.spawnflags |= Defines.DROPPED_PLAYER_ITEM;
    
            drop.touch = GameItems.Touch_Item;
            drop.nextthink = GameBase.level.time
                    + (self.client.quad_framenum - (float) GameBase.level.framenum)
                    * Defines.FRAMETIME;
            drop.think = GameUtil.G_FreeEdictA;
        }
    }
}