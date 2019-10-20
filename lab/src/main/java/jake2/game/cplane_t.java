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

public class cplane_t
{
	public final float[] normal = new float[3];
	public float dist;
	/** This is for fast side tests, 0=xplane, 1=yplane, 2=zplane and 3=arbitrary. */
	public byte type;
	/** This represents signx + (signy<<1) + (signz << 1). */
	public byte signbits; 
	public final byte[] pad = {(byte) 0, (byte) 0};
	
	public void set(cplane_t c) {
		Math3D.set(normal, c.normal);
		dist = c.dist;
		type = c.type;
		signbits = c.signbits;
        byte[] pad = this.pad;
        byte[] pad1 = c.pad;
		pad[0] = pad1[0];
		pad[1] = pad1[1];
	}

	public void clear() {
		Math3D.VectorClear(normal);
		dist = (float) 0;
		type = (byte) 0;
		signbits = (byte) 0;
        byte[] pad = this.pad;
		pad[0] = (byte) 0;
		pad[1] = (byte) 0;
	}
}
