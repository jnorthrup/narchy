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

import jake2.util.Lib;

public class spawn_temp_t {
	
	public String sky="";
	public float skyrotate;
	public float[] skyaxis = { 0, 0, 0 };
	
	public String nextmap="";

	public int lip;
	public int distance;
	public int height;
	
	public String noise="";
	public float pausetime;

	public String item="";
	public String gravity="";

	public float minyaw;
	public float maxyaw;
	public float minpitch;
	public float maxpitch;

	public boolean set(String key, String value) {
		boolean result = true;
		if ("lip".equals(key)) {
			lip = Lib.atoi(value);
		} else if ("distance".equals(key)) {
			distance = Lib.atoi(value);
		} else if ("height".equals(key)) {
			height = Lib.atoi(value);
		} else if ("noise".equals(key)) {
			noise = GameSpawn.ED_NewString(value);
		} else if ("pausetime".equals(key)) {
			pausetime = Lib.atof(value);
		} else if ("item".equals(key)) {
			item = GameSpawn.ED_NewString(value);
		} else if ("gravity".equals(key)) {
			gravity = GameSpawn.ED_NewString(value);
		} else if ("sky".equals(key)) {
			sky = GameSpawn.ED_NewString(value);
		} else if ("skyrotate".equals(key)) {
			skyrotate = Lib.atof(value);
		} else if ("skyaxis".equals(key)) {
			skyaxis = Lib.atov(value);
		} else if ("minyaw".equals(key)) {
			minyaw = Lib.atof(value);
		} else if ("maxyaw".equals(key)) {
			maxyaw = Lib.atof(value);
		} else if ("minpitch".equals(key)) {
			minpitch = Lib.atof(value);
		} else if ("maxpitch".equals(key)) {
			maxpitch = Lib.atof(value);
		} else if ("nextmap".equals(key)) {
			nextmap = GameSpawn.ED_NewString(value);
		} else {
			result = false;
		}

		return result;
	}
}
