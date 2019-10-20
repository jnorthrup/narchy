/*
 * Globals.java
 * Copyright (C) 2003
 *
 * $Id: Globals.java,v 1.6 2008-03-02 20:21:12 kbrussel Exp $
 */
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
package jake2;

import jake2.client.*;
import jake2.game.cmdalias_t;
import jake2.game.cvar_t;
import jake2.game.entity_state_t;
import jake2.qcommon.netadr_t;
import jake2.qcommon.sizebuf_t;
import jake2.render.DummyRenderer;
import jake2.render.model_t;

import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.Random;

import static jake2.Defines.MAX_MSGLEN;
import static jake2.Defines.VIDREF_GL;

/**
 * Globals ist the collection of global variables and constants.
 * It is more elegant to use these vars by inheritance to separate
 * it with eclipse refactoring later.
 * <p>
 * As consequence you dont have to touch that much code this time.
 */
public class Globals implements Defines{

	/*
     * global variables
     */
	public static cvar_t cl_timedemo = new cvar_t();
	public static client_state_t cl = new client_state_t();
	public static refexport_t re = new DummyRenderer();

	public static int curtime;
	public static boolean cmd_wait;
	public static int alias_count;
	public static int c_traces;
	public static int c_brush_traces;
	public static int c_pointcontents;
	public static int server_state;
	public static cvar_t cl_add_blend;
	public static cvar_t cl_add_entities;
	public static cvar_t cl_add_lights;
	public static cvar_t cl_add_particles;
	public static cvar_t cl_anglespeedkey;
	public static cvar_t cl_autoskins;
	public static cvar_t cl_footsteps;
	public static cvar_t cl_forwardspeed;
	public static cvar_t cl_gun;
	public static cvar_t cl_maxfps;
	public static cvar_t cl_noskins;
	public static cvar_t cl_pitchspeed;
	public static cvar_t cl_predict;
	public static cvar_t cl_run;
	public static cvar_t cl_sidespeed;
	public static cvar_t cl_stereo;
	public static cvar_t cl_stereo_separation;

	/*
	=============================================================================

							COMMAND BUFFER

	=============================================================================
	*/
	public static cvar_t cl_timeout;
	public static cvar_t cl_upspeed;
	public static cvar_t cl_yawspeed;
	public static cvar_t dedicated;
	public static cvar_t developer;
	public static cvar_t fixedtime;
	public static cvar_t freelook;
	public static cvar_t host_speeds;
	public static cvar_t log_stats;
	public static cvar_t logfile_active;
	public static cvar_t lookspring;
	public static cvar_t lookstrafe;
	public static cvar_t nostdout;
	public static cvar_t sensitivity;
	public static cvar_t showtrace;
	public static cvar_t timescale;
	public static cvar_t in_mouse;
	public static cvar_t in_joystick;
	public static cmdalias_t cmd_alias;
	public static int time_before_game;
	public static int time_after_game;
	public static int time_before_ref;
	public static int time_after_ref;
	public static FileWriter log_stats_file;
	public static cvar_t m_pitch;
	public static cvar_t m_yaw;
	public static cvar_t m_forward;
	public static cvar_t m_side;
	public static cvar_t cl_lightlevel;
	public static cvar_t info_password;
	public static cvar_t info_spectator;
	public static cvar_t name;
	public static cvar_t skin;
	public static cvar_t rate;
	public static cvar_t fov;
	public static cvar_t msg;
	public static cvar_t hand;
	public static cvar_t gender;
	public static cvar_t gender_auto;
	public static cvar_t cl_vwep;
	public static cvar_t rcon_client_password;
	public static cvar_t rcon_address;
	public static cvar_t cl_shownet;
	public static cvar_t cl_showmiss;
	public static cvar_t cl_showclamp;
	public static cvar_t cl_paused;
	public static boolean userinfo_modified;
	public static cvar_t cvar_vars;
	public static cvar_t con_notifytime;
    public static boolean chat_team;
    public static String chat_buffer = "";
    public static int key_linepos;
    public static int edit_line;
    public static cvar_t crosshair;
    public static int sys_frame_time;
    public static int gun_frame;
    public static model_t gun_model;
    public static RandomAccessFile logfile;
    public static cvar_t m_filter;
    public static SizeChangeListener sizeChangeListener;

    static {
        for (var i = 0; i < Globals.cl_entities.length; i++) {
			Globals.cl_entities[i] = new centity_t();
        }
    }

    static {
        for (var i = 0; i < Globals.cl_parse_entities.length; i++) {
			Globals.cl_parse_entities[i] = new entity_state_t(null);
        }
    }

    static {
        for (var i = 0; i < Globals.key_lines.length; i++)
			Globals.key_lines[i] = new byte[Defines.MAXCMDLINE];
    }
}
