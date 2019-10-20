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

import jake2.util.Math3D;


public class trace_t {
	public boolean allsolid; 
	public boolean startsolid; 
	public float fraction; 
	public final float[] endpos = {(float) 0, (float) 0, (float) 0};
	
	public final cplane_t plane = new cplane_t(); 
	
	public csurface_t surface; 
	public int contents; 
	
	public edict_t ent; 
	
	public void set(trace_t from) {
		allsolid = from.allsolid;
		startsolid = from.allsolid;
		fraction = from.fraction;
		Math3D.VectorCopy(from.endpos, endpos);
		plane.set(from.plane);
		surface = from.surface;
		contents = from.contents;
		ent = from.ent;
	}

	public void clear() {
		allsolid = false;
		startsolid = false;
		fraction = (float) 0;
		Math3D.VectorClear(endpos);
		plane.clear();
		surface = null;
		contents = 0;
		ent = null;
	}
}
