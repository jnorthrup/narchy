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




package jake2.server;

import jake2.Defines;
import jake2.Globals;
import jake2.game.*;
import jake2.qcommon.*;
import jake2.sys.NET;
import jake2.sys.Sys;
import jake2.util.Lib;
import jake2.util.QuakeFile;
import jake2.util.Vargs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Calendar;

public class SV_CCMDS {

	/*
	===============================================================================
	
	OPERATOR CONSOLE ONLY COMMANDS
	
	These commands can only be entered from stdin or by a remote operator datagram
	===============================================================================
	*/

	/*
	====================
	SV_SetMaster_f
	
	Specify a list of master servers
	====================
	*/
	public static void SV_SetMaster_f() {


		if (Globals.dedicated.value == (float) 0) {
			Com.Printf("Only dedicated servers use masters.\n");
			return;
		}

		
		Cvar.Set("public", "1");

		int i;
		for (i = 1; i < Defines.MAX_MASTERS; i++)
			SV_MAIN.master_adr[i] = new netadr_t();

        int slot = 1;
		for (i = 1; i < Cmd.Argc(); i++) {
			if (slot == Defines.MAX_MASTERS)
				break;

			if (!NET.StringToAdr(Cmd.Argv(i), SV_MAIN.master_adr[i])) {
				Com.Printf("Bad address: " + Cmd.Argv(i) + '\n');
				continue;
			}
			if (SV_MAIN.master_adr[slot].port == 0)
				SV_MAIN.master_adr[slot].port = Defines.PORT_MASTER;

			Com.Printf("Master server at " + NET.AdrToString(SV_MAIN.master_adr[slot]) + '\n');
			Com.Printf("Sending a ping.\n");

			Netchan.OutOfBandPrint(Defines.NS_SERVER, SV_MAIN.master_adr[slot], "ping");

			slot++;
		}

		SV_INIT.svs.last_heartbeat = -9999999;
	}
	/*
	==================
	SV_SetPlayer
	
	Sets sv_client and sv_player to the player with idnum Cmd.Argv(1)
	==================
	*/
	public static boolean SV_SetPlayer() {

		if (Cmd.Argc() < 2)
			return false;

        String s = Cmd.Argv(1);

		
		if ((int) s.charAt(0) >= (int) '0' && (int) s.charAt(0) <= (int) '9') {
            int idnum = Lib.atoi(Cmd.Argv(1));
			if (idnum < 0 || (float) idnum >= SV_MAIN.maxclients.value) {
				Com.Printf("Bad client slot: " + idnum + '\n');
				return false;
			}

			SV_MAIN.sv_client = SV_INIT.svs.clients[idnum];
			SV_USER.sv_player = SV_MAIN.sv_client.edict;
			if (0 == SV_MAIN.sv_client.state) {
				Com.Printf("Client " + idnum + " is not active\n");
				return false;
			}
			return true;
		}

		
		for (int i = 0; (float) i < SV_MAIN.maxclients.value; i++) {
            client_t cl = SV_INIT.svs.clients[i];
			if (0 == cl.state)
				continue;
			if (0 == Lib.strcmp(cl.name, s)) {
				SV_MAIN.sv_client = cl;
				SV_USER.sv_player = SV_MAIN.sv_client.edict;
				return true;
			}
		}

		Com.Printf("Userid " + s + " is not on the server\n");
		return false;
	}
	/*
	===============================================================================
	
	SAVEGAME FILES
	
	===============================================================================
	*/

	public static void remove(String name) {
		try {
			new File(name).delete();
		}
		catch (Exception e) {
		}
	}
	
	/** Delete save files save/(number)/.  */
	public static void SV_WipeSavegame(String savename) {

	    Com.DPrintf("SV_WipeSaveGame(" + savename + ")\n");

        String name = FS.Gamedir() + "/save/" + savename + "/server.ssv";
		remove(name);

		name = FS.Gamedir() + "/save/" + savename + "/game.ssv";
		remove(name);

		name = FS.Gamedir() + "/save/" + savename + "/*.sav";

        File f = Sys.FindFirst(name, 0, 0);
		while (f != null) {
			f.delete();
			f = Sys.FindNext();
		}
		Sys.FindClose();

		name = FS.Gamedir() + "/save/" + savename + "/*.sv2";

		f = Sys.FindFirst(name, 0, 0);

		while (f != null) {
			f.delete();
			f = Sys.FindNext();
		}
		Sys.FindClose();
	}
	/*
	================
	CopyFile
	================
	*/
	public static void CopyFile(String src, String dst) {
		RandomAccessFile f1;


		try {
			f1 = new RandomAccessFile(src, "r");
		}
		catch (Exception e) {
			return;
		}
		RandomAccessFile f2;
		try {
			f2 = new RandomAccessFile(dst, "rw");
		}
		catch (Exception e) {
			try {
				f1.close();
			}
			catch (IOException e1) {
				e1.printStackTrace();
			}
			return;
		}

        byte[] buffer = new byte[65536];
        int l = -1;
		while (true) {

			try {
				l = f1.read(buffer, 0, 65536);
			}
			catch (IOException e1) {

				e1.printStackTrace();
			}
			if (l == -1)
				break;
			try {
				f2.write(buffer, 0, l);
			}
			catch (IOException e2) {

				e2.printStackTrace();
			}
		}

		try {
			f1.close();
		}
		catch (IOException e1) {

			e1.printStackTrace();
		}
		try {
			f2.close();
		}
		catch (IOException e2) {

			e2.printStackTrace();
		}
	}
	/*
	================
	SV_CopySaveGame
	================
	*/
	public static void SV_CopySaveGame(String src, String dst) {

		Com.DPrintf("SV_CopySaveGame(" + src + ',' + dst + ")\n");

		SV_WipeSavegame(dst);


        String name = FS.Gamedir() + "/save/" + src + "/server.ssv";
        String name2 = FS.Gamedir() + "/save/" + dst + "/server.ssv";
		FS.CreatePath(name2);
		CopyFile(name, name2);

		name = FS.Gamedir() + "/save/" + src + "/game.ssv";
		name2 = FS.Gamedir() + "/save/" + dst + "/game.ssv";
		CopyFile(name, name2);

        String name1 = FS.Gamedir() + "/save/" + src + '/';
		name = FS.Gamedir() + "/save/" + src + "/*.sav";

        File found = Sys.FindFirst(name, 0, 0);

		while (found != null) {
			name = name1 + found.getName();
			name2 = FS.Gamedir() + "/save/" + dst + '/' + found.getName();

			CopyFile(name, name2);

			
			name = name.substring(0, name.length() - 3) + "sv2";
			name2 = name2.substring(0, name2.length() - 3) + "sv2";

			CopyFile(name, name2);

			found = Sys.FindNext();
		}
		Sys.FindClose();
	}
	/*
	==============
	SV_WriteLevelFile
	
	==============
	*/
	public static void SV_WriteLevelFile() {

		Com.DPrintf("SV_WriteLevelFile()\n");

        String name = FS.Gamedir() + "/save/current/" + SV_INIT.sv.name + ".sv2";

		try {
            QuakeFile f = new QuakeFile(name, "rw");

			for (int i = 0; i < Defines.MAX_CONFIGSTRINGS; i++)
				f.writeString(SV_INIT.sv.configstrings[i]);

			CM.CM_WritePortalState(f);
			f.close();
		}
		catch (Exception e) {
			Com.Printf("Failed to open " + name + '\n');
			e.printStackTrace();
		}

		name = FS.Gamedir() + "/save/current/" + SV_INIT.sv.name + ".sav";
		GameSave.WriteLevel(name);
	}
	/*
	==============
	SV_ReadLevelFile
	
	==============
	*/
	public static void SV_ReadLevelFile() {

		Com.DPrintf("SV_ReadLevelFile()\n");

        String name = FS.Gamedir() + "/save/current/" + SV_INIT.sv.name + ".sv2";
		try {
            QuakeFile f = new QuakeFile(name, "r");

			for (int n = 0; n < Defines.MAX_CONFIGSTRINGS; n++)
				SV_INIT.sv.configstrings[n] = f.readString();

			CM.CM_ReadPortalState(f);

			f.close();
		}
		catch (IOException e1) {
			Com.Printf("Failed to open " + name + '\n');
			e1.printStackTrace();
		}

		name = FS.Gamedir() + "/save/current/" + SV_INIT.sv.name + ".sav";
		GameSave.ReadLevel(name);
	}
	/*
	==============
	SV_WriteServerFile
	
	==============
	*/
	public static void SV_WriteServerFile(boolean autosave) {

		Com.DPrintf("SV_WriteServerFile(" + (autosave ? "true" : "false") + ")\n");

        String filename = FS.Gamedir() + "/save/current/server.ssv";
		try {
            QuakeFile f = new QuakeFile(filename, "rw");

			String comment;
			if (!autosave) {
                Calendar c = Calendar.getInstance();
				comment =
					Com.sprintf(
						"%2i:%2i %2i/%2i  ",
						new Vargs().add(c.get(Calendar.HOUR_OF_DAY)).add(c.get(Calendar.MINUTE)).add(
							c.get(Calendar.MONTH) + 1).add(
							c.get(Calendar.DAY_OF_MONTH)));
				comment += SV_INIT.sv.configstrings[Defines.CS_NAME];
			}
			else {
				
				comment = "ENTERING " + SV_INIT.sv.configstrings[Defines.CS_NAME];
			}

			f.writeString(comment);
			f.writeString(SV_INIT.svs.mapcmd);

			

			
			
			for (cvar_t var = Globals.cvar_vars; var != null; var = var.next) {
				if (0 == (var.flags & Defines.CVAR_LATCH))
					continue;
				if (var.name.length() >= Defines.MAX_OSPATH - 1 || var.string.length() >= 128 - 1) {
					Com.Printf("Cvar too long: " + var.name + " = " + var.string + '\n');
					continue;
				}

                String name = var.name;
                String string = var.string;
				try {
					f.writeString(name);
					f.writeString(string);
				}
				catch (IOException e2) {
				}

			}
			
			f.writeString(null);
			f.close();
		}
		catch (Exception e) {
			Com.Printf("Couldn't write " + filename + '\n');
		}

		
		filename = FS.Gamedir() + "/save/current/game.ssv";
		GameSave.WriteGame(filename, autosave);
	}
	/*
	==============
	SV_ReadServerFile
	
	==============
	*/
	public static void SV_ReadServerFile() {
        String filename="";
		try {

			Com.DPrintf("SV_ReadServerFile()\n");

			filename = FS.Gamedir() + "/save/current/server.ssv";

            QuakeFile f = new QuakeFile(filename, "r");

			
			f.readString();


            String mapcmd = f.readString();


            String name = "";
			while (true) {
				name = f.readString();
				if (name == null)
					break;
                String string = f.readString();

				Com.DPrintf("Set " + name + " = " + string + '\n');
				Cvar.ForceSet(name, string);
			}

			f.close();

			
			SV_INIT.SV_InitGame();

			SV_INIT.svs.mapcmd = mapcmd;

			
			filename = FS.Gamedir() + "/save/current/game.ssv";
			GameSave.ReadGame(filename);
		}
		catch (Exception e) {
			Com.Printf("Couldn't read file " + filename + '\n');
			e.printStackTrace();
		}
	}
	

	/*
	==================
	SV_DemoMap_f
	
	Puts the server in demo mode on a specific map/cinematic
	==================
	*/
	public static void SV_DemoMap_f() {
		SV_INIT.SV_Map(true, Cmd.Argv(1), false);
	}
	/*
	==================
	SV_GameMap_f
	
	Saves the state of the map just being exited and goes to a new map.
	
	If the initial character of the map string is '*', the next map is
	in a new unit, so the current savegame directory is cleared of
	map files.
	
	Example:
	
	*inter.cin+jail
	
	Clears the archived maps, plays the inter.cin cinematic, then
	goes to map jail.bsp.
	==================
	*/
	public static void SV_GameMap_f() {

		if (Cmd.Argc() != 2) {
			Com.Printf("USAGE: gamemap <map>\n");
			return;
		}

		Com.DPrintf("SV_GameMap(" + Cmd.Argv(1) + ")\n");

		FS.CreatePath(FS.Gamedir() + "/save/current/");


        String map = Cmd.Argv(1);
		if ((int) map.charAt(0) == (int) '*') {
			
			SV_WipeSavegame("current");
		}
		else { 
			if (SV_INIT.sv.state == Defines.ss_game) {
				
				
				
				client_t cl;
                boolean[] savedInuse = new boolean[(int) SV_MAIN.maxclients.value];
				for (int i = 0; (float) i < SV_MAIN.maxclients.value; i++) {
					cl = SV_INIT.svs.clients[i];
					savedInuse[i] = cl.edict.inuse;
					cl.edict.inuse = false;
				}

				SV_WriteLevelFile();

				
				for (int i = 0; (float) i < SV_MAIN.maxclients.value; i++) {
					cl = SV_INIT.svs.clients[i];
					cl.edict.inuse = savedInuse[i];

				}
				savedInuse = null;
			}
		}

		
		SV_INIT.SV_Map(false, Cmd.Argv(1), false);

		
		SV_INIT.svs.mapcmd = Cmd.Argv(1);

		
		if ((float) 0 == Globals.dedicated.value) {
			SV_WriteServerFile(true);
			SV_CopySaveGame("current", "save0");
		}
	}
	/*
	==================
	SV_Map_f
	
	Goes directly to a given map without any savegame archiving.
	For development work
	==================
	*/
	public static void SV_Map_f() {


        String map = Cmd.Argv(1);
		if (!map.contains(".")) {
            String expanded = "maps/" + map + ".bsp";
			if (FS.LoadFile(expanded) == null) {

				Com.Printf("Can't find " + expanded + '\n');
				return;
			}
		}

		SV_INIT.sv.state = Defines.ss_dead; 

		SV_WipeSavegame("current");
		SV_GameMap_f();
	}
	/*
	=====================================================================
	
	  SAVEGAMES
	
	=====================================================================
	*/

	/*
	==============
	SV_Loadgame_f
	
	==============
	*/
	public static void SV_Loadgame_f() {

		if (Cmd.Argc() != 2) {
			Com.Printf("USAGE: loadgame <directory>\n");
			return;
		}

		Com.Printf("Loading game...\n");

        String dir = Cmd.Argv(1);
		for (String s : Arrays.asList("..", "/", "\\")) {
			if (dir.contains(s)) {
				Com.Printf("Bad savedir.\n");
				break;
			}
		}


		String name = FS.Gamedir() + "/save/" + Cmd.Argv(1) + "/server.ssv";
		RandomAccessFile f;
		try {
			f = new RandomAccessFile(name, "r");
		}
		catch (FileNotFoundException e) {
			Com.Printf("No such savegame: " + name + '\n');
			return;
		}

		try {
			f.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

		SV_CopySaveGame(Cmd.Argv(1), "current");
		SV_ReadServerFile();

		
		SV_INIT.sv.state = Defines.ss_dead; 
		SV_INIT.SV_Map(false, SV_INIT.svs.mapcmd, true);
	}
	/*
	==============
	SV_Savegame_f
	
	==============
	*/
	public static void SV_Savegame_f() {

        if (SV_INIT.sv.state != Defines.ss_game) {
			Com.Printf("You must be in a game to save.\n");
			return;
		}

		if (Cmd.Argc() != 2) {
			Com.Printf("USAGE: savegame <directory>\n");
			return;
		}

		if (Cvar.VariableValue("deathmatch") != (float) 0) {
			Com.Printf("Can't savegame in a deathmatch\n");
			return;
		}

		if (0 == Lib.strcmp(Cmd.Argv(1), "current")) {
			Com.Printf("Can't save to 'current'\n");
			return;
		}

		if (SV_MAIN.maxclients.value == 1.0F && (int) SV_INIT.svs.clients[0].edict.client.ps.stats[Defines.STAT_HEALTH] <= 0) {
			Com.Printf("\nCan't savegame while dead!\n");
			return;
		}

        String dir = Cmd.Argv(1);
		for (String s : Arrays.asList("..", "/", "\\")) {
			if (dir.contains(s)) {
				Com.Printf("Bad savedir.\n");
				break;
			}
		}

		Com.Printf("Saving game...\n");

		
		
		
		SV_WriteLevelFile();

		
		try {
			SV_WriteServerFile(false);
		}
		catch (Exception e) {
			Com.Printf("IOError in SV_WriteServerFile: " + e);
		}

		
		SV_CopySaveGame("current", dir);
		Com.Printf("Done.\n");
	}
	
	/*
	==================
	SV_Kick_f
	
	Kick a user off of the server
	==================
	*/
	public static void SV_Kick_f() {
		if (!SV_INIT.svs.initialized) {
			Com.Printf("No server running.\n");
			return;
		}

		if (Cmd.Argc() != 2) {
			Com.Printf("Usage: kick <userid>\n");
			return;
		}

		if (!SV_SetPlayer())
			return;

		SV_SEND.SV_BroadcastPrintf(Defines.PRINT_HIGH, SV_MAIN.sv_client.name + " was kicked\n");
		
		
		SV_SEND.SV_ClientPrintf(SV_MAIN.sv_client, Defines.PRINT_HIGH, "You were kicked from the game\n");
		SV_MAIN.SV_DropClient(SV_MAIN.sv_client);
		SV_MAIN.sv_client.lastmessage = SV_INIT.svs.realtime; 
	}
	/*
	================
	SV_Status_f
	================
	*/
	public static void SV_Status_f() {
		if (SV_INIT.svs.clients == null) {
			Com.Printf("No server running.\n");
			return;
		}
		Com.Printf("map              : " + SV_INIT.sv.name + '\n');

		Com.Printf("num score ping name            lastmsg address               qport \n");
		Com.Printf("--- ----- ---- --------------- ------- --------------------- ------\n");
		for (int i = 0; (float) i < SV_MAIN.maxclients.value; i++) {
            client_t cl = SV_INIT.svs.clients[i];
			if (0 == cl.state)
				continue;

			Com.Printf("%3i ", new Vargs().add(i));
			Com.Printf("%5i ", new Vargs().add(cl.edict.client.ps.stats[Defines.STAT_FRAGS]));

            switch (cl.state) {
                case Defines.cs_connected:
                    Com.Printf("CNCT ");
                    break;
                case Defines.cs_zombie:
                    Com.Printf("ZMBI ");
                    break;
                default:
                    int ping = cl.ping < 9999 ? cl.ping : 9999;
                    Com.Printf("%4i ", new Vargs().add(ping));
                    break;
            }

			Com.Printf("%s", new Vargs().add(cl.name));
            int l = 16 - cl.name.length();
			int j;
			for (j = 0; j < l; j++)
				Com.Printf(" ");

			Com.Printf("%7i ", new Vargs().add(SV_INIT.svs.realtime - cl.lastmessage));

            String s = NET.AdrToString(cl.netchan.remote_address);
			Com.Printf(s);
			l = 22 - s.length();
			for (j = 0; j < l; j++)
				Com.Printf(" ");

			Com.Printf("%5i", new Vargs().add(cl.netchan.qport));

			Com.Printf("\n");
		}
		Com.Printf("\n");
	}
	/*
	==================
	SV_ConSay_f
	==================
	*/
	public static void SV_ConSay_f() {

		if (Cmd.Argc() < 2)
			return;

        String p = Cmd.Args();

		if ((int) p.charAt(0) == (int) '"') {
			p = p.substring(1, p.length() - 1);
		}

        String text = "console: ";
		text += p;

		for (int j = 0; (float) j < SV_MAIN.maxclients.value; j++) {
            client_t client = SV_INIT.svs.clients[j];
			if (client.state != Defines.cs_spawned)
				continue;
			SV_SEND.SV_ClientPrintf(client, Defines.PRINT_CHAT, text + '\n');
		}
	}
	/*
	==================
	SV_Heartbeat_f
	==================
	*/
	public static void SV_Heartbeat_f() {
		SV_INIT.svs.last_heartbeat = -9999999;
	}
	/*
	===========
	SV_Serverinfo_f
	
	  Examine or change the serverinfo string
	===========
	*/
	public static void SV_Serverinfo_f() {
		Com.Printf("Server info settings:\n");
		Info.Print(Cvar.Serverinfo());
	}
	/*
	===========
	SV_DumpUser_f
	
	Examine all a users info strings
	===========
	*/
	public static void SV_DumpUser_f() {
		if (Cmd.Argc() != 2) {
			Com.Printf("Usage: info <userid>\n");
			return;
		}

		if (!SV_SetPlayer())
			return;

		Com.Printf("userinfo\n");
		Com.Printf("--------\n");
		Info.Print(SV_MAIN.sv_client.userinfo);

	}
	/*
	==============
	SV_ServerRecord_f
	
	Begins server demo recording.  Every entity and every message will be
	recorded, but no playerinfo will be stored.  Primarily for demo merging.
	==============
	*/
	public static void SV_ServerRecord_f() {

        sizebuf_t buf = new sizebuf_t();

		if (Cmd.Argc() != 2) {
			Com.Printf("serverrecord <demoname>\n");
			return;
		}

		if (SV_INIT.svs.demofile != null) {
			Com.Printf("Already recording.\n");
			return;
		}

		if (SV_INIT.sv.state != Defines.ss_game) {
			Com.Printf("You must be in a level to record.\n");
			return;
		}


        String name = FS.Gamedir() + "/demos/" + Cmd.Argv(1) + ".dm2";

		Com.Printf("recording to " + name + ".\n");
		FS.CreatePath(name);
		try {
			SV_INIT.svs.demofile = new RandomAccessFile(name, "rw");
		}
		catch (Exception e) {
			Com.Printf("ERROR: couldn't open.\n");
			return;
		}

		
		SZ.Init(SV_INIT.svs.demo_multicast, SV_INIT.svs.demo_multicast_buf, SV_INIT.svs.demo_multicast_buf.length);


        byte[] buf_data = new byte[32768];
		SZ.Init(buf, buf_data, buf_data.length);

		
		
		
		
		
		MSG.WriteByte(buf, Defines.svc_serverdata);
		MSG.WriteLong(buf, Defines.PROTOCOL_VERSION);
		MSG.WriteLong(buf, SV_INIT.svs.spawncount);
		
		MSG.WriteByte(buf, 2); 
		MSG.WriteString(buf, Cvar.VariableString("gamedir"));
		MSG.WriteShort(buf, -1);
		
		MSG.WriteString(buf, SV_INIT.sv.configstrings[Defines.CS_NAME]);

		for (int i = 0; i < Defines.MAX_CONFIGSTRINGS; i++)
			if (SV_INIT.sv.configstrings[i].length() == 0) {
				MSG.WriteByte(buf, Defines.svc_configstring);
				MSG.WriteShort(buf, i);
				MSG.WriteString(buf, SV_INIT.sv.configstrings[i]);
			}

		
		Com.DPrintf("signon message length: " + buf.cursize + '\n');
        int len = EndianHandler.swapInt(buf.cursize);
		
		
		try {
			SV_INIT.svs.demofile.writeInt(len);
			SV_INIT.svs.demofile.write(buf.data, 0, buf.cursize);
		}
		catch (IOException e1) {
			
			e1.printStackTrace();
		}

		
	}
	/*
	==============
	SV_ServerStop_f
	
	Ends server demo recording
	==============
	*/
	public static void SV_ServerStop_f() {
		if (SV_INIT.svs.demofile == null) {
			Com.Printf("Not doing a serverrecord.\n");
			return;
		}
		try {
			SV_INIT.svs.demofile.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		SV_INIT.svs.demofile = null;
		Com.Printf("Recording completed.\n");
	}
	/*
	===============
	SV_KillServer_f
	
	Kick everyone off, possibly in preparation for a new game
	
	===============
	*/
	public static void SV_KillServer_f() {
		if (!SV_INIT.svs.initialized)
			return;
		SV_MAIN.SV_Shutdown("Server was killed.\n", false);
		NET.Config(false); 
	}
	/*
	===============
	SV_ServerCommand_f
	
	Let the game dll handle a command
	===============
	*/
	public static void SV_ServerCommand_f() {

		GameSVCmds.ServerCommand();
	}
	

	/*
	==================
	SV_InitOperatorCommands
	==================
	*/
	public static void SV_InitOperatorCommands() {
		Cmd.AddCommand("heartbeat", new xcommand_t() {
			@Override
            public void execute() {
				SV_Heartbeat_f();
			}
		});
		Cmd.AddCommand("kick", new xcommand_t() {
			@Override
            public void execute() {
				SV_Kick_f();
			}
		});
		Cmd.AddCommand("status", new xcommand_t() {
			@Override
            public void execute() {
				SV_Status_f();
			}
		});
		Cmd.AddCommand("serverinfo", new xcommand_t() {
			@Override
            public void execute() {
				SV_Serverinfo_f();
			}
		});
		Cmd.AddCommand("dumpuser", new xcommand_t() {
			@Override
            public void execute() {
				SV_DumpUser_f();
			}
		});

		Cmd.AddCommand("map", new xcommand_t() {
			@Override
            public void execute() {
				SV_Map_f();
			}
		});
		Cmd.AddCommand("demomap", new xcommand_t() {
			@Override
            public void execute() {
				SV_DemoMap_f();
			}
		});
		Cmd.AddCommand("gamemap", new xcommand_t() {
			@Override
            public void execute() {
				SV_GameMap_f();
			}
		});
		Cmd.AddCommand("setmaster", new xcommand_t() {
			@Override
            public void execute() {
				SV_SetMaster_f();
			}
		});

		if (Globals.dedicated.value != (float) 0)
			Cmd.AddCommand("say", new xcommand_t() {
			@Override
            public void execute() {
				SV_ConSay_f();
			}
		});

		Cmd.AddCommand("serverrecord", new xcommand_t() {
			@Override
            public void execute() {
				SV_ServerRecord_f();
			}
		});
		Cmd.AddCommand("serverstop", new xcommand_t() {
			@Override
            public void execute() {
				SV_ServerStop_f();
			}
		});

		Cmd.AddCommand("save", new xcommand_t() {
			@Override
            public void execute() {
				SV_Savegame_f();
			}
		});
		Cmd.AddCommand("load", new xcommand_t() {
			@Override
            public void execute() {
				SV_Loadgame_f();
			}
		});

		Cmd.AddCommand("killserver", new xcommand_t() {
			@Override
            public void execute() {
				SV_KillServer_f();
			}
		});

		Cmd.AddCommand("sv", new xcommand_t() {
			@Override
            public void execute() {
				SV_ServerCommand_f();
			}
		});
	}
}
