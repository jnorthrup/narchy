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




package jake2.render;

import jake2.game.cplane_t;

public class mnode_t {
	
	public int contents; 
	public int visframe; 

	
	public final float[] mins = new float[3]; 
	public final float[] maxs = new float[3]; 

	public mnode_t parent;

	
	public cplane_t plane;
	public final mnode_t[] children = new mnode_t[2];

	
	public int firstsurface;
	public int numsurfaces;

}
