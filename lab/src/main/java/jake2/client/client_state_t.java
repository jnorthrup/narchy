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
import jake2.game.cmodel_t;
import jake2.game.usercmd_t;
import jake2.render.image_t;
import jake2.render.model_t;
import jake2.sound.sfx_t;

import java.nio.ByteBuffer;

public class client_state_t {

	public client_state_t() {
		for (var n = 0; n < Defines.CMD_BACKUP; n++)
			cmds[n] = new usercmd_t();
		for (var i = 0; i < frames.length; i++) {
			frames[i] = new frame_t();
		}

		for (var n = 0; n < Defines.MAX_CONFIGSTRINGS; n++)
			configstrings[n] = "";
			
		for (var n = 0; n < Defines.MAX_CLIENTS; n++)
			clientinfo[n] = new clientinfo_t();
	}
	
	
	
	
	int timeoutcount;

	int timedemo_frames;
	int timedemo_start;

	public boolean refresh_prepped; 
	public boolean sound_prepped; 
	boolean force_refdef; 

	int parse_entities; 

	final usercmd_t cmd = new usercmd_t();
	final usercmd_t[] cmds = new usercmd_t[Defines.CMD_BACKUP]; 

	final int[] cmd_time = new int[Defines.CMD_BACKUP]; 
	final short[][] predicted_origins = new short[Defines.CMD_BACKUP][3]; 

	float predicted_step; 
	int predicted_step_time;

	final float[] predicted_origin ={0,0,0}; 
	final float[] predicted_angles={0,0,0};
	final float[] prediction_error={0,0,0};

	public final frame_t frame = new frame_t(); 
	int surpressCount; 
	final frame_t[] frames = new frame_t[Defines.UPDATE_BACKUP];

	
	
	
	
	
	public final float[] viewangles = { 0, 0, 0 };

	public int time; 
	
	float lerpfrac; 

	final refdef_t refdef = new refdef_t();

	final float[] v_forward = { 0, 0, 0 };
	final float[] v_right = { 0, 0, 0 };
	final float[] v_up = { 0, 0, 0 }; 

	
	
	

	String layout = ""; 
	final int[] inventory = new int[Defines.MAX_ITEMS];

	
	
	
	ByteBuffer cinematic_file;
	
	int cinematictime; 
	int cinematicframe;
	final byte[] cinematicpalette = new byte[768];
	public boolean cinematicpalette_active;

	
	
	
	boolean attractloop; 
	int servercount; 
	String gamedir ="";
	public int playernum;

	public final String[] configstrings = new String[Defines.MAX_CONFIGSTRINGS];

	
	
	
	final model_t[] model_draw = new model_t[Defines.MAX_MODELS];
	final cmodel_t[] model_clip = new cmodel_t[Defines.MAX_MODELS];

	public final sfx_t[] sound_precache = new sfx_t[Defines.MAX_SOUNDS];
	final image_t[] image_precache = new image_t[Defines.MAX_IMAGES];

	final clientinfo_t[] clientinfo = new clientinfo_t[Defines.MAX_CLIENTS];
	final clientinfo_t baseclientinfo = new clientinfo_t();

}
