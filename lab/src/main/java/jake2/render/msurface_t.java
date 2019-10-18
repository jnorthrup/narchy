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
import jake2.game.cplane_t;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class msurface_t
{

	public int visframe; 

	public cplane_t plane;
	public int flags;

	public int firstedge; 
	public int numedges; 

	public final short[] texturemins = { 0, 0 };
	public final short[] extents = { 0, 0 };

	public int light_s;
    public int light_t;
	public int dlight_s;
    public int dlight_t;
	

	public glpoly_t polys; 
	public msurface_t texturechain;
	public msurface_t lightmapchain;

	
	public mtexinfo_t texinfo = new mtexinfo_t();

	
	public int dlightframe;
	public int dlightbits;

	public int lightmaptexturenum;
	public final byte[] styles = new byte[Defines.MAXLIGHTMAPS];
	public final float[] cached_light = new float[Defines.MAXLIGHTMAPS];
	
	
	public ByteBuffer samples; 
	
	public void clear() {
		visframe = 0;
		plane.clear();
		flags = 0;

		firstedge = 0;
		numedges = 0;

		texturemins[0] = texturemins[1] = -1;
		extents[0] = extents[1] = 0;

		light_s = light_t = 0;
		dlight_s = dlight_t = 0;

		polys = null;
		texturechain = null;
		lightmapchain = null;

		
		texinfo.clear();

		dlightframe = 0;
		dlightbits = 0;

		lightmaptexturenum = 0;

        Arrays.fill(styles, (byte) 0);
        Arrays.fill(cached_light, 0);
		if (samples != null) samples.clear();
	}
}
