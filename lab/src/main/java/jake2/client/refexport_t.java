/*
 * refexport_t.java
 * Copyright (C) 2003
 *
 * $Id: refexport_t.java,v 1.3 2004-12-14 00:11:10 hzi Exp $
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

package jake2.client;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.newt.MonitorMode;
import jake2.qcommon.xcommand_t;
import jake2.render.image_t;
import jake2.render.model_t;
import jake2.sys.KBD;

import java.util.List;

/**
 * refexport_t
 * 
 * @author cwei
 */
public interface refexport_t {
	
	
	
	
	
	
	
	
	
	boolean Init(int vid_xpos, int vid_ypos);

	
	void Shutdown();

	
	
	
	
	
	
	
	
	
	
	
	
	
	void BeginRegistration(String map);
	model_t RegisterModel(String name);
	image_t RegisterSkin(String name);
	image_t RegisterPic(String name);
	void SetSky(String name, float rotate, /* vec3_t */
	float[] axis);
	void EndRegistration();

	void RenderFrame(refdef_t fd);

	void DrawGetPicSize(Dimension dim /* int *w, *h */, String name);
	
	void DrawPic(int x, int y, String name);
	void DrawStretchPic(int x, int y, int w, int h, String name);
	void DrawChar(int x, int y, int num); 
	void DrawTileClear(int x, int y, int w, int h, String name);
	void DrawFill(int x, int y, int w, int h, int c);
	void DrawFadeScreen();

	
	void DrawStretchRaw(int x,	int y, int w, int h, int cols, int rows, byte[] data);

	/*
	** video mode and refresh state management entry points
	*/
	/* 256 r,g,b values;	null = game palette, size = 768 bytes */
	void CinematicSetPalette(byte[] palette);
	boolean BeginFrame(float camera_separation);
	void EndFrame();

	void AppActivate(boolean activate);
	
	/**
	 * 
	 *
	 */
	void updateScreen(xcommand_t callback);
	
	int apiVersion();
	
	List<MonitorMode> getModeList();
	
	KBD getKeyboardHandler();
}
