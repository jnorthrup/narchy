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




package jake2.qcommon;

import jake2.util.Lib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class texinfo_t {

	
	public texinfo_t(byte[] cmod_base, int o, int len) {
		this(ByteBuffer.wrap(cmod_base, o, len).order(ByteOrder.LITTLE_ENDIAN));
	}

	public texinfo_t(ByteBuffer bb) {

        vecs[0] = new float[] { bb.getFloat(), bb.getFloat(), bb.getFloat(), bb.getFloat()};
		vecs[1] = new float[] { bb.getFloat(), bb.getFloat(), bb.getFloat(), bb.getFloat()};

		flags = bb.getInt();
		value = bb.getInt();

		var str = new byte[32];
        bb.get(str);
		texture = new String(str, 0, Lib.strlen(str));
		nexttexinfo = bb.getInt();
	}

	public static final int SIZE = 32 + 4 + 4 + 32 + 4;

	
	public final float[][] vecs = {
		 { 0, 0, 0, 0 },
		 { 0, 0, 0, 0 }
	};
	public final int flags; 
	public final int value; 
	
	public String texture="";
	public final int nexttexinfo; 
}
