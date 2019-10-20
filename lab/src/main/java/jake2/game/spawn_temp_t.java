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
	public float[] skyaxis = {(float) 0, (float) 0, (float) 0};
	
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
		switch (key) {
			case "lip":
				lip = Lib.atoi(value);
				break;
			case "distance":
				distance = Lib.atoi(value);
				break;
			case "height":
				height = Lib.atoi(value);
				break;
			case "noise":
				noise = GameSpawn.ED_NewString(value);
				break;
			case "pausetime":
				pausetime = Lib.atof(value);
				break;
			case "item":
				item = GameSpawn.ED_NewString(value);
				break;
			case "gravity":
				gravity = GameSpawn.ED_NewString(value);
				break;
			case "sky":
				sky = GameSpawn.ED_NewString(value);
				break;
			case "skyrotate":
				skyrotate = Lib.atof(value);
				break;
			case "skyaxis":
				skyaxis = Lib.atov(value);
				break;
			case "minyaw":
				minyaw = Lib.atof(value);
				break;
			case "maxyaw":
				maxyaw = Lib.atof(value);
				break;
			case "minpitch":
				minpitch = Lib.atof(value);
				break;
			case "maxpitch":
				maxpitch = Lib.atof(value);
				break;
			case "nextmap":
				nextmap = GameSpawn.ED_NewString(value);
				break;
			default:
				result = false;
				break;
		}

		return result;
	}
}
