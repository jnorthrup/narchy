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
import jake2.game.monsters.*;
import jake2.qcommon.Com;
import jake2.util.Lib;

public class GameSpawn {

    static final EntThinkAdapter SP_item_health = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_item_health"; }
        @Override
        public boolean think(edict_t ent) {
            GameItems.SP_item_health(ent);
            return true;
        }
    };

    static final EntThinkAdapter SP_item_health_small = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_item_health_small"; }
        @Override
        public boolean think(edict_t ent) {
            GameItems.SP_item_health_small(ent);
            return true;
        }
    };

    static final EntThinkAdapter SP_item_health_large = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_item_health_large"; }
        @Override
        public boolean think(edict_t ent) {
            GameItems.SP_item_health_large(ent);
            return true;
        }
    };

    static final EntThinkAdapter SP_item_health_mega = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_item_health_mega"; }
        @Override
        public boolean think(edict_t ent) {
            GameItems.SP_item_health_mega(ent);
            return true;
        }
    };

    static final EntThinkAdapter SP_info_player_start = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_info_player_start"; }
        @Override
        public boolean think(edict_t ent) {
            PlayerClient.SP_info_player_start(ent);
            return true;
        }
    };

    static final EntThinkAdapter SP_info_player_deathmatch = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_info_player_deathmatch"; }
        @Override
        public boolean think(edict_t ent) {
            PlayerClient.SP_info_player_deathmatch(ent);
            return true;
        }
    };

    static final EntThinkAdapter SP_info_player_coop = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_info_player_coop"; }
        @Override
        public boolean think(edict_t ent) {
            PlayerClient.SP_info_player_coop(ent);
            return true;
        }
    };

    static final EntThinkAdapter SP_info_player_intermission = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_info_player_intermission"; }
        @Override
        public boolean think(edict_t ent) {
            PlayerClient.SP_info_player_intermission();
            return true;
        }
    };

    static final EntThinkAdapter SP_func_plat = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_func_plat"; }
        @Override
        public boolean think(edict_t ent) {
            GameFunc.SP_func_plat(ent);
            return true;
        }
    };


    static final EntThinkAdapter SP_func_water = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_func_water"; }
        @Override
        public boolean think(edict_t ent) {
            GameFunc.SP_func_water(ent);
            return true;
        }
    };

    static final EntThinkAdapter SP_func_train = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_func_train"; }
        @Override
        public boolean think(edict_t ent) {
            GameFunc.SP_func_train(ent);
            return true;
        }
    };

    static final EntThinkAdapter SP_func_clock = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_func_clock"; }
        @Override
        public boolean think(edict_t ent) {
            GameMisc.SP_func_clock(ent);
            return true;
        }
    };

    /**
     * QUAKED worldspawn (0 0 0) ?
     * 
     * Only used for the world. "sky" environment map name "skyaxis" vector axis
     * for rotating sky "skyrotate" speed of rotation in degrees/second "sounds"
     * music cd track number "gravity" 800 is default gravity "message" text to
     * print at user logon
     */

    static final EntThinkAdapter SP_worldspawn = new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_worldspawn"; }

        @Override
        public boolean think(edict_t ent) {
            ent.movetype = Defines.MOVETYPE_PUSH;
            ent.solid = Defines.SOLID_BSP;
            ent.inuse = true;
            
            ent.s.modelindex = 1;
            
            
            
            PlayerClient.InitBodyQue();
            
            GameItems.SetItemNames();
            if (GameBase.st.nextmap != null)
                GameBase.level.nextmap = GameBase.st.nextmap;
            
            if (ent.message != null && ent.message.length() > 0) {
                game_import_t.configstring(Defines.CS_NAME, ent.message);
                GameBase.level.level_name = ent.message;
            } else
                GameBase.level.level_name = GameBase.level.mapname;
            if (GameBase.st.sky != null && GameBase.st.sky.length() > 0)
                game_import_t.configstring(Defines.CS_SKY, GameBase.st.sky);
            else
                game_import_t.configstring(Defines.CS_SKY, "unit1_");
            game_import_t.configstring(Defines.CS_SKYROTATE, String.valueOf(GameBase.st.skyrotate));
            game_import_t.configstring(Defines.CS_SKYAXIS, Lib
                    .vtos(GameBase.st.skyaxis));
            game_import_t.configstring(Defines.CS_CDTRACK, String.valueOf(ent.sounds));
            game_import_t.configstring(Defines.CS_MAXCLIENTS, String.valueOf((int) (GameBase.maxclients.value)));
            
            if (GameBase.deathmatch.value != 0)
                game_import_t.configstring(Defines.CS_STATUSBAR, "" + dm_statusbar);
            else
                game_import_t.configstring(Defines.CS_STATUSBAR, "" + single_statusbar);
            
            
            game_import_t.imageindex("i_help");
            GameBase.level.pic_health = game_import_t.imageindex("i_health");
            game_import_t.imageindex("help");
            game_import_t.imageindex("field_3");
            if (GameBase.st.gravity != null && GameBase.st.gravity.isEmpty())
                game_import_t.cvar_set("sv_gravity", "800");
            else
                game_import_t.cvar_set("sv_gravity", GameBase.st.gravity);
            GameBase.snd_fry = game_import_t.soundindex("player/fry.wav");
            
            GameItems.PrecacheItem(GameItems.FindItem("Blaster"));
            game_import_t.soundindex("player/lava1.wav");
            game_import_t.soundindex("player/lava2.wav");
            game_import_t.soundindex("misc/pc_up.wav");
            game_import_t.soundindex("misc/talk1.wav");
            game_import_t.soundindex("misc/udeath.wav");
            
            game_import_t.soundindex("items/respawn1.wav");
            
            game_import_t.soundindex("*death1.wav");
            game_import_t.soundindex("*death2.wav");
            game_import_t.soundindex("*death3.wav");
            game_import_t.soundindex("*death4.wav");
            game_import_t.soundindex("*fall1.wav");
            game_import_t.soundindex("*fall2.wav");
            game_import_t.soundindex("*gurp1.wav");
            
            game_import_t.soundindex("*gurp2.wav");
            game_import_t.soundindex("*jump1.wav");
            
            game_import_t.soundindex("*pain25_1.wav");
            game_import_t.soundindex("*pain25_2.wav");
            game_import_t.soundindex("*pain50_1.wav");
            game_import_t.soundindex("*pain50_2.wav");
            game_import_t.soundindex("*pain75_1.wav");
            game_import_t.soundindex("*pain75_2.wav");
            game_import_t.soundindex("*pain100_1.wav");
            game_import_t.soundindex("*pain100_2.wav");
            
            
            
            game_import_t.modelindex("#w_blaster.md2");
            game_import_t.modelindex("#w_shotgun.md2");
            game_import_t.modelindex("#w_sshotgun.md2");
            game_import_t.modelindex("#w_machinegun.md2");
            game_import_t.modelindex("#w_chaingun.md2");
            game_import_t.modelindex("#a_grenades.md2");
            game_import_t.modelindex("#w_glauncher.md2");
            game_import_t.modelindex("#w_rlauncher.md2");
            game_import_t.modelindex("#w_hyperblaster.md2");
            game_import_t.modelindex("#w_railgun.md2");
            game_import_t.modelindex("#w_bfg.md2");
            
            game_import_t.soundindex("player/gasp1.wav");
            
            game_import_t.soundindex("player/gasp2.wav");
            
            game_import_t.soundindex("player/watr_in.wav");
            
            game_import_t.soundindex("player/watr_out.wav");
            
            game_import_t.soundindex("player/watr_un.wav");
            
            game_import_t.soundindex("player/u_breath1.wav");
            game_import_t.soundindex("player/u_breath2.wav");
            game_import_t.soundindex("items/pkup.wav");
            
            game_import_t.soundindex("world/land.wav");
            
            game_import_t.soundindex("misc/h2ohit1.wav");
            
            game_import_t.soundindex("items/damage.wav");
            game_import_t.soundindex("items/protect.wav");
            game_import_t.soundindex("items/protect4.wav");
            game_import_t.soundindex("weapons/noammo.wav");
            game_import_t.soundindex("infantry/inflies1.wav");
            GameBase.sm_meat_index = game_import_t
                    .modelindex("models/objects/gibs/sm_meat/tris.md2");
            game_import_t.modelindex("models/objects/gibs/arm/tris.md2");
            game_import_t.modelindex("models/objects/gibs/bone/tris.md2");
            game_import_t.modelindex("models/objects/gibs/bone2/tris.md2");
            game_import_t.modelindex("models/objects/gibs/chest/tris.md2");
            game_import_t.modelindex("models/objects/gibs/skull/tris.md2");
            game_import_t.modelindex("models/objects/gibs/head2/tris.md2");
            
            
            
            
            
            game_import_t.configstring(Defines.CS_LIGHTS + 0, "m");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 1,
                    "mmnmmommommnonmmonqnmmo");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 2,
                    "abcdefghijklmnopqrstuvwxyzyxwvutsrqponmlkjihgfedcba");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 3,
                    "mmmmmaaaaammmmmaaaaaabcdefgabcdefg");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 4, "mamamamamama");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 5,
                    "jklmnopqrstuvwxyzyxwvutsrqponmlkj");
            
            game_import_t
                    .configstring(Defines.CS_LIGHTS + 6, "nmonqnmomnmomomno");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 7,
                    "mmmaaaabcdefgmmmmaaaammmaamm");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 8,
                    "mmmaaammmaaammmabcdefaaaammmmabcdefmmmaaaa");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 9, "aaaaaaaazzzzzzzz");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 10,
                    "mmamammmmammamamaaamammma");
            
            game_import_t.configstring(Defines.CS_LIGHTS + 11,
                    "abcdefghijklmnopqrrqponmlkjihgfedcba");
            
            
            
            game_import_t.configstring(Defines.CS_LIGHTS + 63, "a");
            return true;
        }
    };

    /** 
     * ED_NewString.
     */
    static String ED_NewString(String string) {

        int l = string.length();
        StringBuilder newb = new StringBuilder(l);

        for (int i = 0; i < l; i++) {
            char c = string.charAt(i);
            if (c == '\\' && i < l - 1) {
                c = string.charAt(++i);
                if (c == 'n')
                    newb.append('\n');
                else
                    newb.append('\\');
            } else
                newb.append(c);
        }

        return newb.toString();
    }

    /**
     * ED_ParseField
     * 
     * Takes a key/value pair and sets the binary values in an edict.
     */
    static void ED_ParseField(String key, String value, edict_t ent) {

        if (key.equals("nextmap"))
            Com.Println("nextmap: " + value);
        if (!GameBase.st.set(key, value))
            if (!ent.setField(key, value))
                game_import_t.dprintf("??? The key [" + key
                        + "] is not a field\n");

    }

    /**
     * ED_ParseEdict
     * 
     * Parses an edict out of the given string, returning the new position ed
     * should be a properly initialized empty edict.
     */

    static void ED_ParseEdict(Com.ParseHelp ph, edict_t ent) {

        String keyname;
        String com_token;
        boolean init = false;

        GameBase.st = new spawn_temp_t();
        while (true) {

            
            com_token = Com.Parse(ph);
            if (com_token.equals("}"))
                break;

            if (ph.isEof())
                game_import_t.error("ED_ParseEntity: EOF without closing brace");

            keyname = com_token;

            
            com_token = Com.Parse(ph);

            if (ph.isEof())
                game_import_t.error("ED_ParseEntity: EOF without closing brace");

            if (com_token.equals("}"))
                game_import_t.error("ED_ParseEntity: closing brace without data");

            init = true;
            
            
            if (keyname.charAt(0) == '_')
                continue;

            ED_ParseField(keyname.toLowerCase(), com_token, ent);

        }

        if (!init) {
            GameUtil.G_ClearEdict(ent);
        }

    }

    /**
     * G_FindTeams
     * 
     * Chain together all entities with a matching team field.
     * 
     * All but the first will have the FL_TEAMSLAVE flag setAt. All but the last
     * will have the teamchain field set to the next one.
     */

    static void G_FindTeams() {
        edict_t e, e2, chain;
        int i, j;
        int c = 0;
        int c2 = 0;
        for (i = 1; i < GameBase.num_edicts; i++) {
            e = GameBase.g_edicts[i];

            if (!e.inuse)
                continue;
            if (e.team == null)
                continue;
            if ((e.flags & Defines.FL_TEAMSLAVE) != 0)
                continue;
            chain = e;
            e.teammaster = e;
            c++;
            c2++;
            
            for (j = i + 1; j < GameBase.num_edicts; j++) {
                e2 = GameBase.g_edicts[j];
                if (!e2.inuse)
                    continue;
                if (null == e2.team)
                    continue;
                if ((e2.flags & Defines.FL_TEAMSLAVE) != 0)
                    continue;
                if (0 == Lib.strcmp(e.team, e2.team)) {
                    c2++;
                    chain.teamchain = e2;
                    e2.teammaster = e;
                    chain = e2;
                    e2.flags |= Defines.FL_TEAMSLAVE;

                }
            }
        }
    }

    /**
     * SpawnEntities
     * 
     * Creates a server's entity / program execution context by parsing textual
     * entity definitions out of an ent file.
     */

    public static void SpawnEntities(String mapname, String entities,
            String spawnpoint) {
        
        Com.dprintln("SpawnEntities(), mapname=" + mapname);
        String com_token;
        int i;

        float skill_level = (float) Math.floor(GameBase.skill.value);

        if (skill_level < 0)
            skill_level = 0;
        if (skill_level > 3)
            skill_level = 3;
        if (GameBase.skill.value != skill_level)
            game_import_t.cvar_forceset("skill", String.valueOf(skill_level));

        PlayerClient.SaveClientData();

        GameBase.level = new level_locals_t();
        for (int n = 0; n < GameBase.game.maxentities; n++) {
            GameBase.g_edicts[n] = new edict_t(n);
        }
        
        GameBase.level.mapname = mapname;
        GameBase.game.spawnpoint = spawnpoint;

        
        for (i = 0; i < GameBase.game.maxclients; i++)
            GameBase.g_edicts[i + 1].client = GameBase.game.clients[i];

        edict_t ent = null;
        int inhibit = 0;

        Com.ParseHelp ph = new Com.ParseHelp(entities);

        while (true) { 

            com_token = Com.Parse(ph);
            if (ph.isEof())
                break;
            if (!com_token.startsWith("{"))
                game_import_t.error("ED_LoadFromFile: found " + com_token
                        + " when expecting {");

            if (ent == null)
                ent = GameBase.g_edicts[0];
            else
                ent = GameUtil.G_Spawn();

            ED_ParseEdict(ph, ent);
            Com.DPrintf("spawning ent[" + ent.index + "], classname=" + 
                    ent.classname + ", flags= " + Integer.toHexString(ent.spawnflags));
            
            
            if (0 == Lib.Q_stricmp(GameBase.level.mapname, "command")
                    && 0 == Lib.Q_stricmp(ent.classname, "trigger_once")
                    && 0 == Lib.Q_stricmp(ent.model, "*27"))
                ent.spawnflags &= ~Defines.SPAWNFLAG_NOT_HARD;

            
            
            if (ent != GameBase.g_edicts[0]) {
                if (GameBase.deathmatch.value != 0) {
                    if ((ent.spawnflags & Defines.SPAWNFLAG_NOT_DEATHMATCH) != 0) {
                        
                        Com.DPrintf("->inhibited.\n");
                        GameUtil.G_FreeEdict(ent);
                        inhibit++;
                        continue;
                    }
                } else {
                    if (/*
                         * ((coop.value) && (ent.spawnflags &
                         * SPAWNFLAG_NOT_COOP)) ||
                         */
                    ((GameBase.skill.value == 0) && (ent.spawnflags & Defines.SPAWNFLAG_NOT_EASY) != 0)
                            || ((GameBase.skill.value == 1) && (ent.spawnflags & Defines.SPAWNFLAG_NOT_MEDIUM) != 0)
                            || (((GameBase.skill.value == 2) || (GameBase.skill.value == 3)) && (ent.spawnflags & Defines.SPAWNFLAG_NOT_HARD) != 0)) {
                        
                        Com.DPrintf("->inhibited.\n");
                        GameUtil.G_FreeEdict(ent);
                        inhibit++;
                        
                        continue;
                    }
                }

                ent.spawnflags &= ~(Defines.SPAWNFLAG_NOT_EASY
                        | Defines.SPAWNFLAG_NOT_MEDIUM
                        | Defines.SPAWNFLAG_NOT_HARD
                        | Defines.SPAWNFLAG_NOT_COOP | Defines.SPAWNFLAG_NOT_DEATHMATCH);
            }
            ED_CallSpawn(ent);
            Com.DPrintf("\n");
        }
        Com.DPrintf("player skill level:" + GameBase.skill.value + '\n');
        Com.DPrintf(inhibit + " entities inhibited.\n");
        i = 1;
        G_FindTeams();
        PlayerTrail.Init();
    }

    static final String single_statusbar = "yb	-24 " 
            + "xv	0 " + "hnum " + "xv	50 " + "pic 0 " 
            + "if 2 " + "	xv	100 " + "	anum " + "	xv	150 " + "	pic 2 "
            + "endif " 
            + "if 4 " + "	xv	200 " + "	rnum " + "	xv	250 " + "	pic 4 "
            + "endif " 
            + "if 6 " + "	xv	296 " + "	pic 6 " + "endif " + "yb	-50 " 
            
            
            + "if 7 " + "	xv	0 " + "	pic 7 " + "	xv	26 " + "	yb	-42 "
            + "	stat_string 8 " + "	yb	-50 " + "endif "
            
            + "if 9 " + "	xv	262 " + "	num	2	10 " + "	xv	296 " + "	pic	9 "
            + "endif "
            
            + "if 11 " + "	xv	148 " + "	pic	11 " + "endif ";

    static final String dm_statusbar = "yb	-24 " 
            + "xv	0 " + "hnum " + "xv	50 " + "pic 0 " 
            + "if 2 " + "	xv	100 " + "	anum " + "	xv	150 " + "	pic 2 "
            + "endif " 
            + "if 4 " + "	xv	200 " + "	rnum " + "	xv	250 " + "	pic 4 "
            + "endif " 
            + "if 6 " + "	xv	296 " + "	pic 6 " + "endif " + "yb	-50 " 
            
            
            + "if 7 " + "	xv	0 " + "	pic 7 " + "	xv	26 " + "	yb	-42 "
            + "	stat_string 8 " + "	yb	-50 " + "endif "
            
            + "if 9 " + "	xv	246 " + "	num	2	10 " + "	xv	296 " + "	pic	9 "
            + "endif "
            
            + "if 11 " + "	xv	148 " + "	pic	11 " + "endif " 
            + "xr	-50 " + "yt 2 " + "num 3 14 " 
            + "if 17 " + "xv 0 " + "yb -58 " + "string2 \"SPECTATOR MODE\" "
            + "endif " 
            + "if 16 " + "xv 0 " + "yb -68 " + "string \"Chasing\" " + "xv 64 "
            + "stat_string 16 " + "endif ";

    static final spawn_t[] spawns = {
            new spawn_t("item_health", SP_item_health),
            new spawn_t("item_health_small", SP_item_health_small),
            new spawn_t("item_health_large", SP_item_health_large),
            new spawn_t("item_health_mega", SP_item_health_mega),
            new spawn_t("info_player_start", SP_info_player_start),
            new spawn_t("info_player_deathmatch", SP_info_player_deathmatch),
            new spawn_t("info_player_coop", SP_info_player_coop),
            new spawn_t("info_player_intermission", SP_info_player_intermission),
            new spawn_t("func_plat", SP_func_plat),
            new spawn_t("func_button", GameFunc.SP_func_button),
            new spawn_t("func_door", GameFunc.SP_func_door),
            new spawn_t("func_door_secret", GameFunc.SP_func_door_secret),
            new spawn_t("func_door_rotating", GameFunc.SP_func_door_rotating),
            new spawn_t("func_rotating", GameFunc.SP_func_rotating),
            new spawn_t("func_train", SP_func_train),
            new spawn_t("func_water", SP_func_water),
            new spawn_t("func_conveyor", GameFunc.SP_func_conveyor),
            new spawn_t("func_areaportal", GameMisc.SP_func_areaportal),
            new spawn_t("func_clock", SP_func_clock),
            new spawn_t("func_wall", new EntThinkAdapter() {
        @Override
        public String getID(){ return "func_wall"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_func_wall(ent);
                    return true;
                }
            }),
            new spawn_t("func_object", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_func_object"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_func_object(ent);
                    return true;
                }
            }),
            new spawn_t("func_timer", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_func_timer"; }
                @Override
                public boolean think(edict_t ent) {
                    GameFunc.SP_func_timer(ent);
                    return true;
                }
            }),
            new spawn_t("func_explosive", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_func_explosive"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_func_explosive(ent);
                    return true;
                }
            }),
            new spawn_t("func_killbox", GameFunc.SP_func_killbox),
            new spawn_t("trigger_always", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_always"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_always(ent);
                    return true;
                }
            }),
            new spawn_t("trigger_once", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_once"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_once(ent);
                    return true;
                }
            }),
            new spawn_t("trigger_multiple", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_multiple"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_multiple(ent);
                    return true;
                }
            }),
            new spawn_t("trigger_relay", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_relay"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_relay(ent);
                    return true;
                }
            }),
            new spawn_t("trigger_push", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_push"; }
                
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_push(ent);
                    return true;
                }
            }),
            new spawn_t("trigger_hurt", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_hurt"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_hurt(ent);
                    return true;
                }
            }),
            new spawn_t("trigger_key", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_key"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_key(ent);
                    return true;
                }
            }),
            new spawn_t("trigger_counter", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_counter"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_counter(ent);
                    return true;
                }
            }),
            new spawn_t("trigger_elevator", GameFunc.SP_trigger_elevator),
            new spawn_t("trigger_gravity", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_gravity"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_gravity(ent);
                    return true;
                }
            }),
            new spawn_t("trigger_monsterjump", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_trigger_monsterjump"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTrigger.SP_trigger_monsterjump(ent);
                    return true;
                }
            }),
            new spawn_t("target_temp_entity", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_temp_entity"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_temp_entity(ent);
                    return true;
                }
            }),
            new spawn_t("target_speaker", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_speaker"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_speaker(ent);
                    return true;
                }
            }),
            new spawn_t("target_explosion", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_explosion"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_explosion(ent);
                    return true;
                }
            }),
            new spawn_t("target_changelevel", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_changelevel"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_changelevel(ent);
                    return true;
                }
            }),
            new spawn_t("target_secret", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_secret"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_secret(ent);
                    return true;
                }
            }),
            new spawn_t("target_goal", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_goal"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_goal(ent);
                    return true;
                }
            }),
            new spawn_t("target_splash", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_splash"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_splash(ent);
                    return true;
                }
            }),
            new spawn_t("target_spawner", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_spawner"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_spawner(ent);
                    return true;
                }
            }),
            new spawn_t("target_blaster", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_blaster"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_blaster(ent);
                    return true;
                }
            }),
            new spawn_t("target_crosslevel_trigger", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_crosslevel_trigger"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_crosslevel_trigger(ent);
                    return true;
                }
            }),
            new spawn_t("target_crosslevel_target", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_crosslevel_target"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_crosslevel_target(ent);
                    return true;
                }
            }),
            new spawn_t("target_laser", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_laser"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_laser(ent);
                    return true;
                }
            }),
            new spawn_t("target_help", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_help"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_help(ent);
                    return true;
                }
            }),
            new spawn_t("target_actor", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_actor"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Actor.SP_target_actor(ent);
                    return true;
                }
            }),
            new spawn_t("target_lightramp", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_lightramp"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_lightramp(ent);
                    return true;
                }
            }),
            new spawn_t("target_earthquake", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_earthquake"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTarget.SP_target_earthquake(ent);
                    return true;
                }
            }),
            new spawn_t("target_character", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_character"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_target_character(ent);
                    return true;
                }
            }),
            new spawn_t("target_string", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_target_string"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_target_string(ent);
                    return true;
                }
            }),
            new spawn_t("worldspawn", SP_worldspawn),
            new spawn_t("viewthing", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_viewthing"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_viewthing(ent);
                    return true;
                }
            }),
            new spawn_t("light", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_light"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_light(ent);
                    return true;
                }
            }),
            new spawn_t("light_mine1", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_light_mine1"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_light_mine1(ent);
                    return true;
                }
            }),
            new spawn_t("light_mine2", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_light_mine2"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_light_mine2(ent);
                    return true;
                }
            }),
            new spawn_t("info_null", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_info_null"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_info_null(ent);
                    return true;
                }
            }),
            new spawn_t("func_group", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_info_null"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_info_null(ent);
                    return true;
                }
            }),
            new spawn_t("info_notnull", new EntThinkAdapter() {
        @Override
        public String getID(){ return "info_notnull"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_info_notnull(ent);
                    return true;
                }
            }),
            new spawn_t("path_corner", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_path_corner"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_path_corner(ent);
                    return true;
                }
            }),
            new spawn_t("point_combat", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_point_combat"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_point_combat(ent);
                    return true;
                }
            }),
            new spawn_t("misc_explobox", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_explobox"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_explobox(ent);
                    return true;
                }
            }),
            new spawn_t("misc_banner", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_banner"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_banner(ent);
                    return true;
                }
            }),
            new spawn_t("misc_satellite_dish", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_satellite_dish"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_satellite_dish(ent);
                    return true;
                }
            }),
            new spawn_t("misc_actor", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_actor"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Actor.SP_misc_actor(ent);
                    return false;
                }
            }),
            new spawn_t("misc_gib_arm", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_gib_arm"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_gib_arm(ent);
                    return true;
                }
            }),
            new spawn_t("misc_gib_leg", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_gib_leg"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_gib_leg(ent);
                    return true;
                }
            }),
            new spawn_t("misc_gib_head", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_gib_head"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_gib_head(ent);
                    return true;
                }
            }),
            new spawn_t("misc_insane", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_insane"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Insane.SP_misc_insane(ent);
                    return true;
                }
            }),
            new spawn_t("misc_deadsoldier", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_deadsoldier"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_deadsoldier(ent);
                    return true;
                }
            }),
            new spawn_t("misc_viper", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_viper"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_viper(ent);
                    return true;
                }
            }),
            new spawn_t("misc_viper_bomb", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_viper_bomb"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_viper_bomb(ent);
                    return true;
                }
            }),
            new spawn_t("misc_bigviper", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_bigviper"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_bigviper(ent);
                    return true;
                }
            }),
            new spawn_t("misc_strogg_ship", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_strogg_ship"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_strogg_ship(ent);
                    return true;
                }
            }),
            new spawn_t("misc_teleporter", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_teleporter"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_teleporter(ent);
                    return true;
                }
            }),
            new spawn_t("misc_teleporter_dest",
                    GameMisc.SP_misc_teleporter_dest),
            new spawn_t("misc_blackhole", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_blackhole"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_blackhole(ent);
                    return true;
                }
            }),
            new spawn_t("misc_eastertank", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_eastertank"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_eastertank(ent);
                    return true;
                }
            }),
            new spawn_t("misc_easterchick", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_easterchick"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_easterchick(ent);
                    return true;
                }
            }),
            new spawn_t("misc_easterchick2", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_misc_easterchick2"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_misc_easterchick2(ent);
                    return true;
                }
            }),
            new spawn_t("monster_berserk", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_berserk"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Berserk.SP_monster_berserk(ent);
                    return true;
                }
            }),
            new spawn_t("monster_gladiator", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_gladiator"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Gladiator.SP_monster_gladiator(ent);
                    return true;
                }
            }),
            new spawn_t("monster_gunner", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_gunner"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Gunner.SP_monster_gunner(ent);
                    return true;
                }
            }),
            new spawn_t("monster_infantry", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_infantry"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Infantry.SP_monster_infantry(ent);
                    return true;
                }
            }),
            new spawn_t("monster_soldier_light",
                    M_Soldier.SP_monster_soldier_light),
            new spawn_t("monster_soldier", M_Soldier.SP_monster_soldier),
            new spawn_t("monster_soldier_ss", M_Soldier.SP_monster_soldier_ss),
            new spawn_t("monster_tank", M_Tank.SP_monster_tank),
            new spawn_t("monster_tank_commander", M_Tank.SP_monster_tank),
            new spawn_t("monster_medic", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_medic"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Medic.SP_monster_medic(ent);
                    return true;
                }
            }), new spawn_t("monster_flipper", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_flipper"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Flipper.SP_monster_flipper(ent);
                    return true;
                }
            }), new spawn_t("monster_chick", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_chick"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Chick.SP_monster_chick(ent);
                    return true;
                }
            }),
            new spawn_t("monster_parasite", M_Parasite.SP_monster_parasite),
            new spawn_t("monster_flyer", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_flyer"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Flyer.SP_monster_flyer(ent);
                    return true;
                }
            }), new spawn_t("monster_brain", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_brain"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Brain.SP_monster_brain(ent);
                    return true;
                }
            }), new spawn_t("monster_floater", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_floater"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Float.SP_monster_floater(ent);
                    return true;
                }
            }), new spawn_t("monster_hover", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_hover"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Hover.SP_monster_hover(ent);
                    return true;
                }
            }), new spawn_t("monster_mutant", M_Mutant.SP_monster_mutant),
            new spawn_t("monster_supertank", M_Supertank.SP_monster_supertank),
            new spawn_t("monster_boss2", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_boss2"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Boss2.SP_monster_boss2(ent);
                    return true;
                }
            }), new spawn_t("monster_boss3_stand", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_boss3_stand"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Boss3.SP_monster_boss3_stand(ent);
                    return true;
                }
            }), new spawn_t("monster_jorg", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_jorg"; }
                @Override
                public boolean think(edict_t ent) {
                    M_Boss31.SP_monster_jorg(ent);
                    return true;
                }
            }), new spawn_t("monster_commander_body", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_monster_commander_body"; }
                @Override
                public boolean think(edict_t ent) {
                    GameMisc.SP_monster_commander_body(ent);
                    return true;
                }
            }), new spawn_t("turret_breach", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_turret_breach"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTurret.SP_turret_breach(ent);
                    return true;
                }
            }), new spawn_t("turret_base", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_turret_base"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTurret.SP_turret_base(ent);
                    return true;
                }
            }), new spawn_t("turret_driver", new EntThinkAdapter() {
        @Override
        public String getID(){ return "SP_turret_driver"; }
                @Override
                public boolean think(edict_t ent) {
                    GameTurret.SP_turret_driver(ent);
                    return true;
                }
            }), new spawn_t(null, null) };

    /**
     * ED_CallSpawn
     * 
     * Finds the spawn function for the entity and calls it.
     */
    public static void ED_CallSpawn(edict_t ent) {

        spawn_t s;
        gitem_t item;
        int i;
        if (null == ent.classname) {
            game_import_t.dprintf("ED_CallSpawn: null classname\n");
            return;
        } 
        for (i = 1; i < GameBase.game.num_items; i++) {

            item = GameItemList.itemlist[i];

            if (item == null)
                game_import_t.error("ED_CallSpawn: null item in pos " + i);

            if (item.classname == null)
                continue;
            if (item.classname.equalsIgnoreCase(ent.classname)) { 
                GameItems.SpawnItem(ent, item);
                return;
            }
        } 

        for (i = 0; (s = spawns[i]) != null && s.name != null; i++) {
            if (s.name.equalsIgnoreCase(ent.classname)) { 

                if (s.spawn == null)
                    game_import_t.error("ED_CallSpawn: null-spawn on index=" + i);
                s.spawn.think(ent);
                return;
            }
        }
        game_import_t.dprintf(ent.classname + " doesn't have a spawn function\n");
    }
}