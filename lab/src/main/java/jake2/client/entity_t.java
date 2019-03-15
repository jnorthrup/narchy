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

import jake2.render.image_t;
import jake2.render.model_t;
import jake2.util.Math3D;


public class entity_t implements Cloneable{
	
	public model_t model;
    public float[] angles = {0, 0, 0};

    /*
     ** most recent data
     */
    public float[] origin = {0, 0, 0};
	public int frame; 

	/*
	** previous data for lerping
	*/
	public final float[] oldorigin = { 0, 0, 0 }; 
	public int oldframe;

	/*
	** misc
	*/
	public float backlerp; 
	public int skinnum; 

	public int lightstyle; 
	public float alpha; 

	
	public image_t skin; 
	public int flags;
	
	
	public void set(entity_t src) {
		this.model = src.model;
		Math3D.VectorCopy(src.angles, this.angles);
		Math3D.VectorCopy(src.origin, this.origin);
		this.frame = src.frame;
		Math3D.VectorCopy(src.oldorigin, this.oldorigin);
		this.oldframe = src.oldframe;
		this.backlerp = src.backlerp;
		this.skinnum = src.skinnum;
		this.lightstyle = src.lightstyle;
		this.alpha = src.alpha;
		this.skin = src.skin;
		this.flags = src.flags;
	}

	public void clear() {
		model = null;
		Math3D.VectorClear(angles);
		Math3D.VectorClear(origin);
		frame = 0;
		Math3D.VectorClear(oldorigin);
		oldframe = 0;
		backlerp = 0;
		skinnum = 0;
		lightstyle = 0;
		alpha = 0;
		skin = null;
		flags = 0;
	}
	
}
