/*
 * qfiles.java
 * Copyright (C) 2003
 *
 * $Id: qfiles.java,v 1.6 2005-05-07 23:40:49 cawe Exp $
 */
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

import jake2.Defines;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * qfiles
 * 
 * @author cwei
 */
public class qfiles {
	
	
	
	

	/*
	========================================================================
	
	The .pak files are just a linear collapse of a directory tree
	
	========================================================================
	*/

	/*
	========================================================================
	
	PCX files are used for as many images as possible
	
	========================================================================
	*/
	public static class pcx_t {

		
		static final int PALETTE_SIZE = 48;
		static final int FILLER_SIZE = 58;

		public final byte manufacturer;
		public final byte version;
		public final byte encoding;
		public final byte bits_per_pixel;
		public final int xmin;
		public final int ymin;
		public final int xmax;
		public final int ymax; 
		public final int hres;
		public final int vres; 
		public final byte[] palette; 
		public final byte reserved;
		public final byte color_planes;
		public final int bytes_per_line; 
		public final int palette_type; 
		public final byte[] filler; 
		public final ByteBuffer data; 

		public pcx_t(byte[] dataBytes) {
			this(ByteBuffer.wrap(dataBytes));
		}

		public pcx_t(ByteBuffer b) {
			
			b.order(ByteOrder.LITTLE_ENDIAN);

			
			manufacturer = b.get();
			version = b.get();
			encoding = b.get();
			bits_per_pixel = b.get();
			xmin = b.getShort() & 0xffff;
			ymin = b.getShort() & 0xffff;
			xmax = b.getShort() & 0xffff;
			ymax = b.getShort() & 0xffff;
			hres = b.getShort() & 0xffff;
			vres = b.getShort() & 0xffff;
			b.get(palette = new byte[PALETTE_SIZE]);
			reserved = b.get();
			color_planes = b.get();
			bytes_per_line = b.getShort() & 0xffff;
			palette_type = b.getShort() & 0xffff;
			b.get(filler = new byte[FILLER_SIZE]);

			
			data = b.slice();
		}
	}

	/*
	========================================================================
	
	TGA files are used for sky planes
	
	========================================================================
	*/
	public static class tga_t {
		
		
		public final int id_length;
		public final int colormap_type;
		public final int image_type; 
		public final int colormap_index;
		public final int colormap_length; 
		public final int colormap_size; 
		public final int x_origin;
		public final int y_origin;
		public final int width;
		public final int height; 
		public final int pixel_size;
		public final int attributes; 

		public final ByteBuffer data; 

		public tga_t(byte[] dataBytes) {
			this(ByteBuffer.wrap(dataBytes));
		}

		public tga_t(ByteBuffer b) {
			
			b.order(ByteOrder.LITTLE_ENDIAN);

			
			id_length = b.get() & 0xFF;
			colormap_type = b.get() & 0xFF;
			image_type = b.get() & 0xFF;
			colormap_index = b.getShort() & 0xFFFF;
			colormap_length = b.getShort() & 0xFFFF;
			colormap_size = b.get() & 0xFF;
			x_origin = b.getShort() & 0xFFFF;
			y_origin = b.getShort() & 0xFFFF;
			width = b.getShort() & 0xFFFF;
			height = b.getShort() & 0xFFFF;
			pixel_size = b.get() & 0xFF;
			attributes = b.get() & 0xFF;

			
			data = b.slice();
		}			

	}
	
	/*
	========================================================================
	
	.MD2 triangle model file format
	
	========================================================================
	*/
	
	public static final int IDALIASHEADER =	(('2'<<24)+('P'<<16)+('D'<<8)+'I');
	public static final int ALIAS_VERSION = 8;
	
	//public static final int MAX_TRIANGLES = 4096;
	public static final int MAX_VERTS = 2048;
	//public static final int MAX_FRAMES = 512;
	public static final int MAX_MD2SKINS = 32;
	public static final int MAX_SKINNAME = 64;
	
	public static class dstvert_t {
		public final short s;
		public final short t;
		
		public dstvert_t(ByteBuffer b) {
			s = b.getShort();
			t = b.getShort();
		}
	}

	public static class dtriangle_t {
		public final short[] index_xyz = { 0, 0, 0 };
		public final short[] index_st = { 0, 0, 0 };
		
		public dtriangle_t(ByteBuffer b) {
			index_xyz[0] = b.getShort();
			index_xyz[1] = b.getShort();
			index_xyz[2] = b.getShort();
			
			index_st[0] = b.getShort();
			index_st[1] = b.getShort();
			index_st[2] = b.getShort();
		}
	}

	public static final int DTRIVERTX_V0 =  0;
	public static final int DTRIVERTX_V1 = 1;
	public static final int DTRIVERTX_V2 = 2;
	public static final int DTRIVERTX_LNI = 3;
	public static final int DTRIVERTX_SIZE = 4;
	
	public static class  daliasframe_t {
		public final float[] scale = {0, 0, 0}; 
		public final float[] translate = {0, 0, 0};	
		public final String name; 
		public int[] verts;	
		
		public daliasframe_t(ByteBuffer b) {
			scale[0] = b.getFloat();	scale[1] = b.getFloat();	scale[2] = b.getFloat();
			translate[0] = b.getFloat(); translate[1] = b.getFloat(); translate[2] = b.getFloat();
			byte[] nameBuf = new byte[16];
			b.get(nameBuf);
			name = new String(nameBuf).trim();
		}
	}
	
	
	
	
	
	
	
	
	
	public static class dmdl_t {
		public final int ident;
		public final int version;

		public final int skinwidth;
		public final int skinheight;
		public final int framesize; 

		public final int num_skins;
		public final int num_xyz;
		public final int num_st; 
		public final int num_tris;
		public final int num_glcmds; 
		public final int num_frames;

		public final int ofs_skins; 
		public final int ofs_st; 
		public final int ofs_tris; 
		public final int ofs_frames; 
		public final int ofs_glcmds;
		public final int ofs_end; 
		
		
		public String[] skinNames;
		public dstvert_t[] stVerts;
		public dtriangle_t[] triAngles;
		public int[] glCmds;
		public daliasframe_t[] aliasFrames;
		
		
		public dmdl_t(ByteBuffer b) {
			ident = b.getInt();
			version = b.getInt();

			skinwidth = b.getInt();
			skinheight = b.getInt();
			framesize = b.getInt(); 

			num_skins = b.getInt();
			num_xyz = b.getInt();
			num_st = b.getInt(); 
			num_tris = b.getInt();
			num_glcmds = b.getInt(); 
			num_frames = b.getInt();

			ofs_skins = b.getInt(); 
			ofs_st = b.getInt(); 
			ofs_tris = b.getInt(); 
			ofs_frames = b.getInt(); 
			ofs_glcmds = b.getInt();
			ofs_end = b.getInt(); 
		}

		/*
		 * new members for vertex array handling
		 */
		public FloatBuffer textureCoordBuf;
		public ShortBuffer vertexIndexBuf;
		public int[] counts;
		public ShortBuffer[] indexElements;
	}
	
	/*
	========================================================================
	
	.SP2 sprite file format
	
	========================================================================
	*/
	
	public static final int IDSPRITEHEADER = (('2'<<24)+('S'<<16)+('D'<<8)+'I');
	public static final int SPRITE_VERSION = 2;

	public static class dsprframe_t {
		public final int width;
		public final int height;
		public final int origin_x;
		public final int origin_y; 
		public final String name; 
		
		public dsprframe_t(ByteBuffer b) {
			width = b.getInt();
			height = b.getInt();
			origin_x = b.getInt();
			origin_y = b.getInt();
			
			byte[] nameBuf = new byte[MAX_SKINNAME];
			b.get(nameBuf);
			name = new String(nameBuf).trim();
		}
	}

	public static class dsprite_t {
		public final int ident;
		public final int version;
		public final int numframes;
		public final dsprframe_t[] frames; 
		
		public dsprite_t(ByteBuffer b) {
			ident = b.getInt();
			version = b.getInt();
			numframes = b.getInt();
			
			frames = new dsprframe_t[numframes];
			for (int i=0; i < numframes; i++) {
				frames[i] = new dsprframe_t(b);	
			}
		}
	}
	
	/*
	==============================================================================
	
	  .WAL texture file format
	
	==============================================================================
	*/
	public static class miptex_t {

		static final int MIPLEVELS = 4;
		static final int NAME_SIZE = 32;

		public final String name; 
		public final int width;
		public final int height;
		public final int[] offsets = new int[MIPLEVELS]; 
		
		public final String animname; 
		public final int flags;
		public final int contents;
		public final int value;

		public miptex_t(byte[] dataBytes) {
			this(ByteBuffer.wrap(dataBytes));
		}

		public miptex_t(ByteBuffer b) {
			
			b.order(ByteOrder.LITTLE_ENDIAN);

			byte[] nameBuf = new byte[NAME_SIZE];
			
			b.get(nameBuf);
			name = new String(nameBuf).trim();
			width = b.getInt();
			height = b.getInt();
			offsets[0] = b.getInt();
			offsets[1] = b.getInt();
			offsets[2] = b.getInt();
			offsets[3] = b.getInt();
			b.get(nameBuf);
			animname = new String(nameBuf).trim();
			flags = b.getInt();
			contents = b.getInt();
			value = b.getInt();
		}

	}
	
	/*
	==============================================================================
	
	  .BSP file format
	
	==============================================================================
	*/

	public static final int IDBSPHEADER = (('P'<<24)+('S'<<16)+('B'<<8)+'I');

	

	public static class dheader_t {

		public dheader_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);
			this.ident = bb.getInt();
			this.version = bb.getInt();

			for (int n = 0; n < Defines.HEADER_LUMPS; n++)
				lumps[n] = new lump_t(bb.getInt(), bb.getInt());

		}

		public final int ident;
		public final int version;
		public final lump_t[] lumps = new lump_t[Defines.HEADER_LUMPS];
	}

	public static class dmodel_t {

		public dmodel_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);

			for (int j = 0; j < 3; j++)
				mins[j] = bb.getFloat();

			for (int j = 0; j < 3; j++)
				maxs[j] = bb.getFloat();

			for (int j = 0; j < 3; j++)
				origin[j] = bb.getFloat();

			headnode = bb.getInt();
			firstface = bb.getInt();
			numfaces = bb.getInt();
		}
		public final float[] mins = { 0, 0, 0 };
		public final float[] maxs = { 0, 0, 0 };
		public final float[] origin = { 0, 0, 0 }; 
		public final int headnode;
		public final int firstface;
		public final int numfaces; 
		

		public static final int SIZE = 3 * 4 + 3 * 4 + 3 * 4 + 4 + 8;
	}
	
	public static class dvertex_t {
		
		public static final int SIZE = 3 * 4; 
		
		public final float[] point = { 0, 0, 0 };
		
		public dvertex_t(ByteBuffer b) {
			point[0] = b.getFloat();
			point[1] = b.getFloat();
			point[2] = b.getFloat();
		}
	}


	
	public static class dplane_t {

		public dplane_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);

			normal[0] = (bb.getFloat());
			normal[1] = (bb.getFloat());
			normal[2] = (bb.getFloat());

			dist = (bb.getFloat());
			type = (bb.getInt());
		}

		public final float[] normal = { 0, 0, 0 };
		public final float dist;
		public final int type; 

		public static final int SIZE = 3 * 4 + 4 + 4;
	}

	public static class dnode_t {

		public dnode_t(ByteBuffer bb) {

			bb.order(ByteOrder.LITTLE_ENDIAN);
			planenum = bb.getInt();

			children[0] = bb.getInt();
			children[1] = bb.getInt();

			short[] mins = this.mins;
			for (int j = 0; j < 3; j++) {
				mins[j] = bb.getShort();
			}

			short[] maxs = this.maxs;
			for (int j = 0; j < 3; j++)
				maxs[j] = bb.getShort();

			firstface = bb.getShort() & 0xffff;
			numfaces = bb.getShort() & 0xffff;

		}

		public final int planenum;
		public final int[] children = { 0, 0 };
		
		public final short[] mins = { 0, 0, 0 }; 
		public final short[] maxs = { 0, 0, 0 };

		/*
		unsigned short	firstface;
		unsigned short	numfaces;	
		*/

		public final int firstface;
		public final int numfaces;

		public static final int SIZE = 4 + 8 + 6 + 6 + 2 + 2; 
	}
	


	
	
	
	public static class dedge_t {

        final int[] v = {0, 0};
	}
	
	public static class dface_t {
		
		public static final int SIZE =
				4 * Defines.SIZE_OF_SHORT
			+	2 * Defines.SIZE_OF_INT
			+	Defines.MAXLIGHTMAPS;

		
		public final int planenum;
		public final short side;

		public final int firstedge; 
		public final short numedges;
		public final short texinfo;

		
		public final byte[] styles = new byte[Defines.MAXLIGHTMAPS];
		public final int lightofs; 
		
		public dface_t(ByteBuffer b) {
			planenum = b.getShort() & 0xFFFF;
			side = b.getShort();
			firstedge = b.getInt();
			numedges = b.getShort();
			texinfo = b.getShort();
			b.get(styles);
			lightofs = b.getInt();
		}
		
	}

	public static class dleaf_t {

		public dleaf_t(byte[] cmod_base, int i, int j) {
			this(ByteBuffer.wrap(cmod_base, i, j).order(ByteOrder.LITTLE_ENDIAN));
		}

		public dleaf_t(ByteBuffer bb) {
			contents = bb.getInt();
			cluster = bb.getShort();
			area = bb.getShort();

			short[] mins = this.mins;
			mins[0] = bb.getShort();
			mins[1] = bb.getShort();
			mins[2] = bb.getShort();

			short[] maxs = this.maxs;
			maxs[0] = bb.getShort();
			maxs[1] = bb.getShort();
			maxs[2] = bb.getShort();

			firstleafface = bb.getShort() & 0xffff;
			numleaffaces = bb.getShort() & 0xffff;

			firstleafbrush = bb.getShort() & 0xffff;
			numleafbrushes = bb.getShort() & 0xffff;
		}

		public static final int SIZE = 4 + 8 * 2 + 4 * 2;

		public final int contents; 

		public final short cluster;
		public final short area;

		public final short[] mins = { 0, 0, 0 }; 
		public final short[] maxs = { 0, 0, 0 };

		public final int firstleafface; 
		public final int numleaffaces; 

		public final int firstleafbrush; 
		public final int numleafbrushes; 
	}
	
	public static class dbrushside_t {

		public dbrushside_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);

			planenum = bb.getShort() & 0xffff;
			texinfo = bb.getShort();
		}

		
		final int planenum; 

		final short texinfo;

		public static final int SIZE = 4;
	}
	
	public static class dbrush_t {

		public dbrush_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);
			firstside = bb.getInt();
			numsides = bb.getInt();
			contents = bb.getInt();
		}

		public static final int SIZE = 3 * 4;

		final int firstside;
		final int numsides;
		final int contents;
	}

	
	

	
	
	
	
	

	public static class dvis_t {

		public dvis_t(ByteBuffer bb) {
			final int numclusters = this.numclusters = bb.getInt();
			bitofs = new int[numclusters][2];

			int[][] b = this.bitofs;
			for (int i = 0; i < numclusters; i++) {
				int[] bi = b[i];
				bi[0] = bb.getInt();
				bi[1] = bb.getInt();
			}
		}

		public final int numclusters;
        public int[][] bitofs = new int[8][2];
	}
	
	
	
	
	
	public static class dareaportal_t {

		public dareaportal_t() {
		}

		public dareaportal_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);
			portalnum = bb.getInt();
			otherarea = bb.getInt();
		}

		int portalnum;
		int otherarea;

		public static final int SIZE = 8;
	}

	public static class darea_t {

		public darea_t(ByteBuffer bb) {

			bb.order(ByteOrder.LITTLE_ENDIAN);

			numareaportals = bb.getInt();
			firstareaportal = bb.getInt();

		}
		final int numareaportals;
		final int firstareaportal;

		public static final int SIZE = 8;
	}

}