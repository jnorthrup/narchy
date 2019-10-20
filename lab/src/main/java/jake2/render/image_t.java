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

import jake2.Defines;

public class image_t {
	
	public static final int MAX_NAME_SIZE = Defines.MAX_QPATH;
	
	
	
	private final int id;
	
	
	public String name=""; 
	
	public int type;
	public int width;
    public int height;
	public int upload_width;
    public int upload_height;
	public int registration_sequence; 
	public msurface_t texturechain; 
	public int texnum; 
	public float sl;
    public float tl;
    public float sh;
    public float th;
	public boolean scrap;
	public boolean has_alpha;

	public boolean paletted;
	
	public image_t(int id) {
		this.id = id;
	}
	
	public void clear() {
		
		
		name = "";
		type = 0;
		width = height = 0;
		upload_width = upload_height = 0;
		registration_sequence = 0; 
		texturechain = null;
		texnum = 0; 
		sl =  tl = sh = th = (float) 0;
		scrap = false;
		has_alpha = false;
		paletted = false;
	}

	public int getId() {
		return id;
	}
	
	public String toString() {
		return name + ':' + texnum;
	}
}
