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
import jake2.game.edict_t;
import jake2.game.usercmd_t;
import jake2.qcommon.netchan_t;
import jake2.qcommon.sizebuf_t;

public class client_t {

	public client_t() {
		for (var n = 0; n < Defines.UPDATE_BACKUP; n++) {
			frames[n] = new client_frame_t();
		}
	}

	public static final int LATENCY_COUNTS = 16;
	public static final int RATE_MESSAGES = 10;

	int state;

	String userinfo = "";

	int lastframe; 
	usercmd_t lastcmd = new usercmd_t(); 

	int commandMsec; 
	

	final int[] frame_latency = new int[LATENCY_COUNTS];
	int ping;

	final int[] message_size = new int[RATE_MESSAGES]; 
	int rate;
	int surpressCount; 

	
	edict_t edict; 

	
	String name = ""; 

	int messagelevel; 

	
	
	final sizebuf_t datagram = new sizebuf_t();
	final byte[] datagram_buf = new byte[Defines.MAX_MSGLEN];

	final client_frame_t[] frames = new client_frame_t[Defines.UPDATE_BACKUP];

    byte[] download;
	int downloadsize; 
	int downloadcount; 

	int lastmessage; 
	int lastconnect;

	int challenge; 

	final netchan_t netchan = new netchan_t();

	
	int serverindex;
}
