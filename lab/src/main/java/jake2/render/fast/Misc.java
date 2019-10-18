/*
 * Misc.java
 * Copyright (C) 2003
 *
 * $Id: Misc.java,v 1.5 2008-03-02 14:56:23 cawe Exp $
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
package jake2.render.fast;

import jake2.Defines;
import jake2.client.VID;
import jake2.qcommon.FS;
import jake2.util.Lib;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Misc
 *  
 * @author cwei
 */
public final class Misc extends Mesh {

	/*
	==================
	R_InitParticleTexture
	==================
	*/
	final byte[][] dottexture =
	{
		{0,0,0,0,0,0,0,0},
		{0,0,1,1,0,0,0,0},
		{0,1,1,1,1,0,0,0},
		{0,1,1,1,1,0,0,0},
		{0,0,1,1,0,0,0,0},
		{0,0,0,0,0,0,0,0},
		{0,0,0,0,0,0,0,0},
		{0,0,0,0,0,0,0,0},
	};

	@Override
    void R_InitParticleTexture()
	{
		int		x,y;
		byte[] data = new byte[8 * 8 * 4];

		
		
		
		for (x=0 ; x<8 ; x++)
		{
			for (y=0 ; y<8 ; y++)
			{
				data[y * 32 + x * 4 + 0] = (byte)255;
				data[y * 32 + x * 4 + 1] = (byte)255;
				data[y * 32 + x * 4 + 2] = (byte)255;
				data[y * 32 + x * 4 + 3] = (byte)(dottexture[x][y]*255);

			}
		}
		r_particletexture = GL_LoadPic("***particle***", data, 8, 8, it_sprite, 32);

		
		
		
		for (x=0 ; x<8 ; x++)
		{
			for (y=0 ; y<8 ; y++)
			{
				data[y * 32 + x * 4 + 0] = (byte)(dottexture[x&3][y&3]*255);
				data[y * 32 + x * 4 + 1] = 0; 
				data[y * 32 + x * 4 + 2] = 0; 
				data[y * 32 + x * 4 + 3] = (byte)255;
			}
		}
		r_notexture = GL_LoadPic("***r_notexture***", data, 8, 8, it_wall, 32);
	}

















	private static final int TGA_HEADER_SIZE = 18;
	
	/**
	 * GL_ScreenShot_f
	 */
	@Override
    public void GL_ScreenShot_f() {
	    StringBuilder sb = new StringBuilder(FS.Gamedir() + "/scrshot/jake00.tga");
	    FS.CreatePath(sb.toString());
	    File file = new File(sb.toString());
	    
	    int i = 0; int offset = sb.length() - 6;
	    while (file.exists() && i++ < 100) {
	        sb.setCharAt(offset, (char) ((i/10) + '0'));
	        sb.setCharAt(offset + 1, (char) ((i%10) + '0'));
	        file = new File(sb.toString());
        }
	    if (i == 100) {
		    VID.Printf(Defines.PRINT_ALL, "Clean up your screenshots\n");
		    return;
	    }
	    
	    try {
	        RandomAccessFile out = new RandomAccessFile(file, "rw");
	        FileChannel ch = out.getChannel();
	        int fileLength = TGA_HEADER_SIZE + vid.getWidth() * vid.getHeight() * 3;
	        out.setLength(fileLength);
	        MappedByteBuffer image = ch.map(FileChannel.MapMode.READ_WRITE, 0,
	                fileLength);
	        
	        
	        image.put(0, (byte) 0).put(1, (byte) 0);
	        image.put(2, (byte) 2); 
	        image.put(12, (byte) (vid.getWidth() & 0xFF)); 
	        image.put(13, (byte) (vid.getWidth() >> 8)); 
	        image.put(14, (byte) (vid.getHeight() & 0xFF)); 
	        image.put(15, (byte) (vid.getHeight() >> 8)); 
	        image.put(16, (byte) 24); 
	        
	        
	        image.position(TGA_HEADER_SIZE);
	        
	        
	        
	        if (vid.getWidth() % 4 != 0) {
	            gl.glPixelStorei(GL_PACK_ALIGNMENT, 1); 
	        }
	        
	        
	        
	        
	        if (gl_config.getOpenGLVersion() >= 1.2f) {
	            
	            gl.glReadPixels(0, 0, vid.getWidth(), vid.getHeight(), GL_BGR, GL_UNSIGNED_BYTE, image);
	        } else {
	            
	            gl.glReadPixels(0, 0, vid.getWidth(), vid.getHeight(), GL_RGB, GL_UNSIGNED_BYTE, image);

                for (i = TGA_HEADER_SIZE; i < fileLength; i += 3) {
                    byte tmp = image.get(i);
                    image.put(i, image.get(i + 2));
		            image.put(i + 2, tmp);
		        }
	        }
	        
	        gl.glPixelStorei(GL_PACK_ALIGNMENT, 4); 
	        
	        ch.close();
	    } catch (IOException e) {
	        VID.Printf(Defines.PRINT_ALL, e.getMessage() + '\n');
	    }

	    VID.Printf(Defines.PRINT_ALL, "Wrote " + file + '\n');
 	} 

	/*
	** GL_Strings_f
	*/
    @Override
    void GL_Strings_f()	{
		VID.Printf(Defines.PRINT_ALL, "GL_VENDOR: " + gl_config.vendor_string + '\n');
		VID.Printf(Defines.PRINT_ALL, "GL_RENDERER: " + gl_config.renderer_string + '\n');
		VID.Printf(Defines.PRINT_ALL, "GL_VERSION: " + gl_config.version_string + '\n');
		VID.Printf(Defines.PRINT_ALL, "GL_EXTENSIONS: " + gl_config.extensions_string + '\n');
	}

	/*
	** GL_SetDefaultState
	*/
    @Override
    void GL_SetDefaultState()
	{
		gl.glClearColor(1f,0f, 0.5f , 0.5f); 
		
		gl.glCullFace(GL_FRONT);
		gl.glEnable(GL_TEXTURE_2D);

		gl.glEnable(GL_ALPHA_TEST);
		gl.glAlphaFunc(GL_GREATER, 0.666f);

		gl.glDisable (GL_DEPTH_TEST);
		gl.glDisable (GL_CULL_FACE);
		gl.glDisable (GL_BLEND);

		gl.glColor4f (1,1,1,1);

		gl.glPolygonMode (GL_FRONT_AND_BACK, GL_FILL);
		gl.glShadeModel (GL_FLAT);

		GL_TextureMode( gl_texturemode.string );
		GL_TextureAlphaMode( gl_texturealphamode.string );
		GL_TextureSolidMode( gl_texturesolidmode.string );

		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_filter_min);
		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_filter_max);

		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

		gl.glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		GL_TexEnv( GL_REPLACE );

		if ( qglPointParameterfEXT )
		{
			
			FloatBuffer att_buffer=Lib.newFloatBuffer(4);
			att_buffer.put(0,gl_particle_att_a.value);
			att_buffer.put(1,gl_particle_att_b.value);
			att_buffer.put(2,gl_particle_att_c.value);
			
			gl.glEnable( GL_POINT_SMOOTH );
			gl.glPointParameterfEXT(GL_POINT_SIZE_MIN_EXT, gl_particle_min_size.value );
			gl.glPointParameterfEXT(GL_POINT_SIZE_MAX_EXT, gl_particle_max_size.value );
			gl.glPointParameterEXT(GL_DISTANCE_ATTENUATION_EXT, att_buffer );
		}

		if ( qglColorTableEXT && gl_ext_palettedtexture.value != 0.0f )
		{
			gl.glEnable(GL_SHARED_TEXTURE_PALETTE_EXT );

			GL_SetTexturePalette( d_8to24table );
		}

		GL_UpdateSwapInterval();
		
		/*
		 * vertex array extension
		 */
		gl.glEnableClientState(GL_VERTEX_ARRAY);
		gl.glClientActiveTextureARB(TEXTURE0);
		gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		
		/*
		 * perspective correction
		 */
		

	}

	@Override
    void GL_UpdateSwapInterval()
	{
		if ( gl_swapinterval.modified )
		{
			gl_swapinterval.modified = false;
			if ( !gl_state.stereo_enabled ) 
			{
			    gl.setSwapInterval((int) gl_swapinterval.value);
			}
		}
	}
}
