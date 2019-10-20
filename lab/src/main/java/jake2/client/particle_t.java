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

import jake2.Defines;
import jake2.util.Lib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class particle_t {
	
	
	private static final ByteBuffer colorByteArray = Lib.newByteBuffer(Defines.MAX_PARTICLES * Lib.SIZEOF_INT, ByteOrder.LITTLE_ENDIAN);

	public static final FloatBuffer vertexArray = Lib.newFloatBuffer(Defines.MAX_PARTICLES * 3);
	public static final int[] colorTable = new int[256];
	public static final IntBuffer colorArray = colorByteArray.asIntBuffer();
	
	
	public static void setColorPalette(int[] palette) {
		for (var i = 0; i < 256; i++) {
			colorTable[i] = palette[i] & 0x00FFFFFF;
		}
	}
	
	public static ByteBuffer getColorAsByteBuffer() {
		return colorByteArray;
	}
}
