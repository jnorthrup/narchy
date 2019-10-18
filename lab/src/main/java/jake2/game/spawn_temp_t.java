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
		if ("lip".equals(key)) {
			lip=Lib.atoi(value);
			return true;
		} 
		
		if ("distance".equals(key)) {
			distance=Lib.atoi(value);
			return true;
		} 
		
		if ("height".equals(key)) {
			height=Lib.atoi(value);
			return true;
		} 
		
		if ("noise".equals(key)) {
			noise = GameSpawn.ED_NewString(value);
			return true;
		} 
		
		if ("pausetime".equals(key)) {
			pausetime = Lib.atof(value);
			return true;
		} 
		
		if ("item".equals(key)) {
			item = GameSpawn.ED_NewString(value);
			return true;
		} 
		
		if ("gravity".equals(key)) {
			 gravity = GameSpawn.ED_NewString(value);
			return true;
		} 
		
		if ("sky".equals(key)) {
			sky = GameSpawn.ED_NewString(value);
			return true;
		} 
		
		if ("skyrotate".equals(key)) {
			skyrotate=Lib.atof(value);
			return true;
		} 
		
		if ("skyaxis".equals(key)) {
			skyaxis=Lib.atov(value);
			return true;
		} 
		
		if ("minyaw".equals(key)) {
			minyaw=Lib.atof(value);
			return true;
		} 
		
		if ("maxyaw".equals(key)) {
			maxyaw=Lib.atof(value);
			return true;
		} 
		
		if ("minpitch".equals(key)) {
			minpitch = Lib.atof(value);
			return true;
		} 
		
		if ("maxpitch".equals(key)) {
			maxpitch = Lib.atof(value);
			return true;
		} 
		
		if ("nextmap".equals(key)) {
			nextmap  = GameSpawn.ED_NewString(value);
			return true;
		} 

		return false;
	}
}
