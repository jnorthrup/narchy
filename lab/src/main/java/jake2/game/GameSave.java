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
import jake2.qcommon.Com;
import jake2.util.Lib;
import jake2.util.QuakeFile;

public class GameSave {

    public static void CreateEdicts() {
        GameBase.g_edicts = new edict_t[GameBase.game.maxentities];
        for (int i = 0; i < GameBase.game.maxentities; i++)
            GameBase.g_edicts[i] = new edict_t(i);
        
    }

    public static void CreateClients() {
        GameBase.game.clients = new gclient_t[GameBase.game.maxclients];
        for (int i = 0; i < GameBase.game.maxclients; i++)
            GameBase.game.clients[i] = new gclient_t(i);

    }

    private static final String[] preloadclasslist  =
    {		
    	"jake2.game.PlayerWeapon",
    	"jake2.game.AIAdapter",
		"jake2.game.Cmd",
		"jake2.game.EdictFindFilter",
		"jake2.game.EdictIterator",
		"jake2.game.EndianHandler",
		"jake2.game.EntBlockedAdapter",
		"jake2.game.EntDieAdapter",
		"jake2.game.EntDodgeAdapter",
		"jake2.game.EntInteractAdapter",
		"jake2.game.EntPainAdapter",
		"jake2.game.EntThinkAdapter",
		"jake2.game.EntTouchAdapter",
		"jake2.game.EntUseAdapter",
		"jake2.game.GameAI",
		"jake2.game.GameBase",
		"jake2.game.GameChase",
		"jake2.game.GameCombat",
		"jake2.game.GameFunc",
		"jake2.game.GameMisc",
		"jake2.game.GameSVCmds",
		"jake2.game.GameSave",
		"jake2.game.GameSpawn",
		"jake2.game.GameTarget",
		"jake2.game.GameTrigger",
		"jake2.game.GameTurret",
		"jake2.game.GameUtil",
		"jake2.game.GameWeapon",
		"jake2.game.Info",
		"jake2.game.ItemDropAdapter",
		"jake2.game.ItemUseAdapter",
		"jake2.game.Monster",
		"jake2.game.PlayerClient",
		"jake2.game.PlayerHud",
		"jake2.game.PlayerTrail",
		"jake2.game.PlayerView",
		"jake2.game.SuperAdapter",
		"jake2.game.monsters.M_Actor",
		"jake2.game.monsters.M_Berserk",
		"jake2.game.monsters.M_Boss2",
		"jake2.game.monsters.M_Boss3",
		"jake2.game.monsters.M_Boss31",
		"jake2.game.monsters.M_Boss32",
		"jake2.game.monsters.M_Brain",
		"jake2.game.monsters.M_Chick",
		"jake2.game.monsters.M_Flash",
		"jake2.game.monsters.M_Flipper",
		"jake2.game.monsters.M_Float",
		"jake2.game.monsters.M_Flyer",
		"jake2.game.monsters.M_Gladiator",
		"jake2.game.monsters.M_Gunner",
		"jake2.game.monsters.M_Hover",
		"jake2.game.monsters.M_Infantry",
		"jake2.game.monsters.M_Insane",
		"jake2.game.monsters.M_Medic",
		"jake2.game.monsters.M_Mutant",
		"jake2.game.monsters.M_Parasite",
		"jake2.game.monsters.M_Player",
		"jake2.game.monsters.M_Soldier",
		"jake2.game.monsters.M_Supertank",
		"jake2.game.monsters.M_Tank",
		"jake2.game.GameItems",
		
		"jake2.game.GameItemList"
    };
    
    /**
     * InitGame
     * 
     * This will be called when the dll is first loaded, which only happens when
     * a new game is started or a save game is loaded. 
     */
    public static void InitGame() {
        game_import_t.dprintf("==== InitGame ====\n");

        
        for (int n = 0; n < preloadclasslist.length; n++)
        {
        	try
			{
        		Class.forName(preloadclasslist[n]);
			}
        	catch(Exception e)
			{
        		Com.DPrintf("error loading class: " + e.getMessage());
			}
        }
        
        
        GameBase.gun_x = game_import_t.cvar("gun_x", "0", 0);
        GameBase.gun_y = game_import_t.cvar("gun_y", "0", 0);
        GameBase.gun_z = game_import_t.cvar("gun_z", "0", 0);

        
        GameBase.sv_rollspeed = game_import_t.cvar("sv_rollspeed", "200", 0);
        GameBase.sv_rollangle = game_import_t.cvar("sv_rollangle", "2", 0);
        GameBase.sv_maxvelocity = game_import_t.cvar("sv_maxvelocity", "2000", 0);
        GameBase.sv_gravity = game_import_t.cvar("sv_gravity", "800", 0);

        
        Globals.dedicated = game_import_t.cvar("dedicated", "0",
                Defines.CVAR_NOSET);

        
        GameBase.sv_cheats = game_import_t.cvar("cheats", "0",
                Defines.CVAR_SERVERINFO | Defines.CVAR_LATCH);
        game_import_t.cvar("gamename", Defines.GAMEVERSION,
                Defines.CVAR_SERVERINFO | Defines.CVAR_LATCH);
        game_import_t.cvar("gamedate", Globals.__DATE__, Defines.CVAR_SERVERINFO
                | Defines.CVAR_LATCH);

        GameBase.maxclients = game_import_t.cvar("maxclients", "4",
                Defines.CVAR_SERVERINFO | Defines.CVAR_LATCH);
        GameBase.maxspectators = game_import_t.cvar("maxspectators", "4",
                Defines.CVAR_SERVERINFO);
        GameBase.deathmatch = game_import_t.cvar("deathmatch", "0",
                Defines.CVAR_LATCH);
        GameBase.coop = game_import_t.cvar("coop", "0", Defines.CVAR_LATCH);
        GameBase.skill = game_import_t.cvar("skill", "0", Defines.CVAR_LATCH);
        GameBase.maxentities = game_import_t.cvar("maxentities", "1024",
                Defines.CVAR_LATCH);

        
        GameBase.dmflags = game_import_t.cvar("dmflags", "0",
                Defines.CVAR_SERVERINFO);
        GameBase.fraglimit = game_import_t.cvar("fraglimit", "0",
                Defines.CVAR_SERVERINFO);
        GameBase.timelimit = game_import_t.cvar("timelimit", "0",
                Defines.CVAR_SERVERINFO);
        GameBase.password = game_import_t.cvar("password", "",
                Defines.CVAR_USERINFO);
        GameBase.spectator_password = game_import_t.cvar("spectator_password",
                "", Defines.CVAR_USERINFO);
        GameBase.needpass = game_import_t.cvar("needpass", "0",
                Defines.CVAR_SERVERINFO);
        GameBase.filterban = game_import_t.cvar("filterban", "1", 0);

        GameBase.g_select_empty = game_import_t.cvar("g_select_empty", "0",
                Defines.CVAR_ARCHIVE);

        GameBase.run_pitch = game_import_t.cvar("run_pitch", "0.002", 0);
        GameBase.run_roll = game_import_t.cvar("run_roll", "0.005", 0);
        GameBase.bob_up = game_import_t.cvar("bob_up", "0.005", 0);
        GameBase.bob_pitch = game_import_t.cvar("bob_pitch", "0.002", 0);
        GameBase.bob_roll = game_import_t.cvar("bob_roll", "0.002", 0);

        
        GameBase.flood_msgs = game_import_t.cvar("flood_msgs", "4", 0);
        GameBase.flood_persecond = game_import_t.cvar("flood_persecond", "4", 0);
        GameBase.flood_waitdelay = game_import_t.cvar("flood_waitdelay", "10", 0);

        
        GameBase.sv_maplist = game_import_t.cvar("sv_maplist", "", 0);

        
        GameItems.InitItems();

        GameBase.game.helpmessage1 = "";
        GameBase.game.helpmessage2 = "";

        
        GameBase.game.maxentities = (int) GameBase.maxentities.value;
        CreateEdicts();

        
        GameBase.game.maxclients = (int) GameBase.maxclients.value;

        CreateClients();

        GameBase.num_edicts = GameBase.game.maxclients + 1;
    }

    /**
     * WriteGame
     * 
     * This will be called whenever the game goes to a new level, and when the
     * user explicitly saves the game.
     * 
     * Game information include cross level data, like multi level triggers,
     * help computer info, and all client states.
     * 
     * A single player death will automatically restore from the last save
     * position.
     */
    public static void WriteGame(String filename, boolean autosave) {
        try {

            if (!autosave)
                PlayerClient.SaveClientData();

            QuakeFile f = new QuakeFile(filename, "rw");

            if (f == null)
                game_import_t.error("Couldn't write to " + filename);

            GameBase.game.autosaved = autosave;
            GameBase.game.write(f);
            GameBase.game.autosaved = false;

            for (int i = 0; i < GameBase.game.maxclients; i++)
                GameBase.game.clients[i].write(f);

            Lib.fclose(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ReadGame(String filename) {

        try {

            QuakeFile f = new QuakeFile(filename, "r");
            CreateEdicts();

            GameBase.game.load(f);

            for (int i = 0; i < GameBase.game.maxclients; i++) {
                GameBase.game.clients[i] = new gclient_t(i);
                GameBase.game.clients[i].read(f);
            }

            f.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * WriteLevel
     */
    public static void WriteLevel(String filename) {
        try {

            QuakeFile f = new QuakeFile(filename, "rw");
            if (f == null)
                game_import_t.error("Couldn't open for writing: " + filename);

            
            GameBase.level.write(f);


            int i;
            for (i = 0; i < GameBase.num_edicts; i++) {
                edict_t ent = GameBase.g_edicts[i];
                if (!ent.inuse)
                    continue;
                f.writeInt(i);
                ent.write(f);
            }

            i = -1;
            f.writeInt(-1);

            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ReadLevel
     * 
     * SpawnEntities will allready have been called on the level the same way it
     * was when the level was saved.
     * 
     * That is necessary to get the baselines set up identically.
     * 
     * The server will have cleared all of the world links before calling
     * ReadLevel.
     * 
     * No clients are connected yet.
     */
    public static void ReadLevel(String filename) {
        try {

            QuakeFile f = new QuakeFile(filename, "r");

            if (f == null)
                game_import_t.error("Couldn't read level file " + filename);

            
            CreateEdicts();

            GameBase.num_edicts = (int) GameBase.maxclients.value + 1;

            
            GameBase.level.read(f);


            edict_t ent;
            while (true) {
                int entnum = f.readInt();
                if (entnum == -1)
                    break;

                if (entnum >= GameBase.num_edicts)
                    GameBase.num_edicts = entnum + 1;

                ent = GameBase.g_edicts[entnum];
                ent.read(f);
                ent.cleararealinks();
                game_import_t.linkentity(ent);
            }

            Lib.fclose(f);

            
            for (int i = 0; i < GameBase.maxclients.value; i++) {
                ent = GameBase.g_edicts[i + 1];
                ent.client = GameBase.game.clients[i];
                ent.client.pers.connected = false;
            }

            
            for (int i = 0; i < GameBase.num_edicts; i++) {
                ent = GameBase.g_edicts[i];

                if (!ent.inuse)
                    continue;

                
                if (ent.classname != null)
                    if (Lib.strcmp(ent.classname, "target_crosslevel_target") == 0)
                        ent.nextthink = GameBase.level.time + ent.delay;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}