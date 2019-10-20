/*
 * Draw.java
 * Copyright (C) 2003
 *
 * $Id: Draw.java,v 1.4 2008-03-02 14:56:23 cawe Exp $
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

import com.jogamp.nativewindow.util.Dimension;
import jake2.Defines;
import jake2.client.VID;
import jake2.qcommon.Com;
import jake2.render.image_t;
import jake2.util.Lib;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Draw
 * (gl_draw.c)
 * 
 * @author cwei
 */
public abstract class Draw extends Image {

	/*
	===============
	Draw_InitLocal
	===============
	*/
    @Override
    void Draw_InitLocal() {
		
		draw_chars = GL_FindImage("pics/conchars.pcx", it_pic);
		GL_Bind(draw_chars.texnum);
		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, (float) GL_NEAREST);
		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, (float) GL_NEAREST);
	}

	/*
	================
	Draw_Char

	Draws one 8*8 graphics character with 0 being transparent.
	It can be clipped to the top of the screen to allow the console to be
	smoothly scrolled off.
	================
	*/
	@Override
    public void Draw_Char(int x, int y, int num) {

		num &= 255;
	
		if ( (num&127) == 32 ) return; 

		if (y <= -8) return;

        int row = num>>4;
        int col = num&15;

        GL_Bind(draw_chars.texnum);

		gl.glBegin (GL_QUADS);
        float fcol = (float) col * 0.0625f;
        float frow = (float) row * 0.0625f;
        gl.glTexCoord2f (fcol, frow);
		gl.glVertex2f ((float) x, (float) y);
        float size = 0.0625f;
        gl.glTexCoord2f (fcol + size, frow);
		gl.glVertex2f ((float) (x + 8), (float) y);
		gl.glTexCoord2f (fcol + size, frow + size);
		gl.glVertex2f ((float) (x + 8), (float) (y + 8));
		gl.glTexCoord2f (fcol, frow + size);
		gl.glVertex2f ((float) x, (float) (y + 8));
		gl.glEnd ();
	}


	/*
	=============
	Draw_FindPic
	=============
	*/
	@Override
    public image_t Draw_FindPic(String name) {
		if (!name.startsWith("/") && !name.startsWith("\\"))
		{
			return GL_FindImage(name, it_pic);
		} else {
			return GL_FindImage(name.substring(1), it_pic);
		}
	}


	/*
	=============
	Draw_GetPicSize
	=============
	*/
	@Override
    public void Draw_GetPicSize(Dimension dim, String pic)	{

        image_t image = Draw_FindPic(pic);
		dim.setWidth((image != null) ? image.width : -1);
		dim.setHeight((image != null) ? image.height : -1);
	}

	/*
	=============
	Draw_StretchPic
	=============
	*/
	@Override
    public void Draw_StretchPic (int x, int y, int w, int h, String pic) {

        image_t image = Draw_FindPic(pic);
		if (image == null)
		{
			VID.Printf (Defines.PRINT_ALL, "Can't find pic: " + pic +'\n');
			return;
		}

		if (scrap_dirty)
			Scrap_Upload();

		if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer & GL_RENDERER_RENDITION) != 0) ) && !image.has_alpha)
			gl.glDisable(GL_ALPHA_TEST);

		GL_Bind(image.texnum);
		gl.glBegin (GL_QUADS);
		gl.glTexCoord2f (image.sl, image.tl);
		gl.glVertex2f ((float) x, (float) y);
		gl.glTexCoord2f (image.sh, image.tl);
		gl.glVertex2f ((float) (x + w), (float) y);
		gl.glTexCoord2f (image.sh, image.th);
		gl.glVertex2f ((float) (x + w), (float) (y + h));
		gl.glTexCoord2f (image.sl, image.th);
		gl.glVertex2f ((float) x, (float) (y + h));
		gl.glEnd ();

		if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer & GL_RENDERER_RENDITION) !=0 ) ) && !image.has_alpha)
			gl.glEnable(GL_ALPHA_TEST);
	}


	/*
	=============
	Draw_Pic
	=============
	*/
	@Override
    public void Draw_Pic(int x, int y, String pic)
	{

        image_t image = Draw_FindPic(pic);
		if (image == null)
		{
			VID.Printf(Defines.PRINT_ALL, "Can't find pic: " +pic + '\n');
			return;
		}
		if (scrap_dirty)
			Scrap_Upload();

		if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer & GL_RENDERER_RENDITION) != 0 ) ) && !image.has_alpha)
			gl.glDisable (GL_ALPHA_TEST);

		GL_Bind(image.texnum);

		gl.glBegin (GL_QUADS);
		gl.glTexCoord2f (image.sl, image.tl);
		gl.glVertex2f ((float) x, (float) y);
		gl.glTexCoord2f (image.sh, image.tl);
		gl.glVertex2f ((float) (x + image.width), (float) y);
		gl.glTexCoord2f (image.sh, image.th);
		gl.glVertex2f ((float) (x + image.width), (float) (y + image.height));
		gl.glTexCoord2f (image.sl, image.th);
		gl.glVertex2f ((float) x, (float) (y + image.height));
		gl.glEnd ();

		if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer & GL_RENDERER_RENDITION) != 0 ) )  && !image.has_alpha)
			gl.glEnable (GL_ALPHA_TEST);
	}

	/*
	=============
	Draw_TileClear

	This repeats a 64*64 tile graphic to fill the screen around a sized down
	refresh window.
	=============
	*/
	@Override
    public void Draw_TileClear(int x, int y, int w, int h, String pic) {

        image_t image = Draw_FindPic(pic);
		if (image == null)
		{
			VID.Printf(Defines.PRINT_ALL, "Can't find pic: " + pic + '\n');
			return;
		}

		if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer & GL_RENDERER_RENDITION) != 0 ) )  && !image.has_alpha)
			gl.glDisable(GL_ALPHA_TEST);

		GL_Bind(image.texnum);
		gl.glBegin (GL_QUADS);
		gl.glTexCoord2f((float) x /64.0f, (float) y /64.0f);
		gl.glVertex2f ((float) x, (float) y);
		gl.glTexCoord2f((float) (x + w) /64.0f, (float) y /64.0f);
		gl.glVertex2f((float) (x + w), (float) y);
		gl.glTexCoord2f((float) (x + w) /64.0f, (float) (y + h) /64.0f);
		gl.glVertex2f((float) (x + w), (float) (y + h));
		gl.glTexCoord2f((float) x /64.0f, (float) (y + h) /64.0f );
		gl.glVertex2f ((float) x, (float) (y + h));
		gl.glEnd ();

		if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer & GL_RENDERER_RENDITION) != 0 ) )  && !image.has_alpha)
			gl.glEnable(GL_ALPHA_TEST);
	}


	/*
	=============
	Draw_Fill

	Fills a box of pixels with a single color
	=============
	*/
	@Override
    public void Draw_Fill(int x, int y, int w, int h, int colorIndex)	{

		if ( colorIndex > 255)
			Com.Error(Defines.ERR_FATAL, "Draw_Fill: bad color");

		gl.glDisable(GL_TEXTURE_2D);

        int color = d_8to24table[colorIndex];


		gl.glColor3ub(
			(byte)((color >> 0) & 0xff), 
			(byte)((color >> 8) & 0xff), 
			(byte)((color >> 16) & 0xff) 
		);

		gl.glBegin (GL_QUADS);

		gl.glVertex2f((float) x, (float) y);
		gl.glVertex2f((float) (x + w), (float) y);
		gl.glVertex2f((float) (x + w), (float) (y + h));
		gl.glVertex2f((float) x, (float) (y + h));

		gl.glEnd();
		gl.glColor3f(1.0F, 1.0F, 1.0F);
		gl.glEnable(GL_TEXTURE_2D);
	}

	

	/*
	================
	Draw_FadeScreen
	================
	*/
	@Override
    public void Draw_FadeScreen()	{
		gl.glEnable(GL_BLEND);
		gl.glDisable(GL_TEXTURE_2D);
		gl.glColor4f((float) 0, (float) 0, (float) 0, 0.8f);
		gl.glBegin(GL_QUADS);

		gl.glVertex2f((float) 0, (float) 0);
		gl.glVertex2f((float) vid.getWidth(), (float) 0);
		gl.glVertex2f((float) vid.getWidth(), (float) vid.getHeight());
		gl.glVertex2f((float) 0, (float) vid.getHeight());

		gl.glEnd();
		gl.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		gl.glEnable(GL_TEXTURE_2D);
		gl.glDisable(GL_BLEND);
	}



	final IntBuffer image32=Lib.newIntBuffer(256*256);
	final ByteBuffer image8=Lib.newByteBuffer(256*256);
	

	/*
	=============
	Draw_StretchRaw
	=============
	*/
	@Override
    public void Draw_StretchRaw (int x, int y, int w, int h, int cols, int rows, byte[] data)
	{

        GL_Bind(0);

        float hscale;
        int trows;
        if (rows<=256)
		{
			hscale = 1.0F;
			trows = rows;
		}
		else
		{
			hscale = (float) rows /256.0f;
			trows = 256;
		}
        float t = (float) rows * hscale / 256.0F;

        int row;
        int fracstep;
        int frac;
        int sourceIndex;
        int j;
        int i;
        if ( !qglColorTableEXT )
		{
			
			image32.clear();
            int destIndex = 0;

			for (i=0 ; i<trows ; i++)
			{
				row = (int)((float) i *hscale);
				if (row > rows)
					break;
				sourceIndex = cols*row;
				destIndex = i*256;
				fracstep = cols*0x10000/256;
				frac = fracstep >> 1;
				for (j=0 ; j<256 ; j++)
				{
					image32.put(destIndex + j, r_rawpalette[(int) data[sourceIndex + (frac >> 16)] & 0xff]);
					frac += fracstep;
				}
			}
			gl.glTexImage2D (GL_TEXTURE_2D, 0, gl_tex_solid_format, 256, 256, 0, GL_RGBA, GL_UNSIGNED_BYTE, image32);
		}
		else
		{
			
			image8.clear();
            int destIndex = 0;

			for (i=0 ; i<trows ; i++)
			{
				row = (int)((float) i *hscale);
				if (row > rows)
					break;
				sourceIndex = cols*row;
				destIndex = i*256;
				fracstep = cols*0x10000/256;
				frac = fracstep >> 1;
				for (j=0 ; j<256 ; j++)
				{
					image8.put(destIndex  + j, data[sourceIndex + (frac>>16)]);
					frac += fracstep;
				}
			}

			gl.glTexImage2D( GL_TEXTURE_2D, 
						   0, 
						   GL_COLOR_INDEX8_EXT, 
						   256, 256, 
						   0, 
						   GL_COLOR_INDEX, 
						   GL_UNSIGNED_BYTE, 
						   image8 );
		}
		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, (float) GL_LINEAR);
		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, (float) GL_LINEAR);

		if ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer & GL_RENDERER_RENDITION) != 0 ) ) 
			gl.glDisable (GL_ALPHA_TEST);

		gl.glBegin (GL_QUADS);
		gl.glTexCoord2f ((float) 0, (float) 0);
		gl.glVertex2f ((float) x, (float) y);
		gl.glTexCoord2f (1.0F, (float) 0);
		gl.glVertex2f ((float) (x + w), (float) y);
		gl.glTexCoord2f (1.0F, t);
		gl.glVertex2f ((float) (x + w), (float) (y + h));
		gl.glTexCoord2f ((float) 0, t);
		gl.glVertex2f ((float) x, (float) (y + h));
		gl.glEnd ();

		if ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer & GL_RENDERER_RENDITION) != 0 ) ) 
			gl.glEnable (GL_ALPHA_TEST);
	}

}
