/*
 * Main.java
 * Copyright (C) 2003
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
package jake2.render.basic;

import com.jogamp.nativewindow.util.Dimension;
import jake2.Defines;
import jake2.client.VID;
import jake2.client.entity_t;
import jake2.client.particle_t;
import jake2.client.refdef_t;
import jake2.game.Cmd;
import jake2.game.cplane_t;
import jake2.game.cvar_t;
import jake2.qcommon.Com;
import jake2.qcommon.Cvar;
import jake2.qcommon.qfiles;
import jake2.qcommon.xcommand_t;
import jake2.render.*;
import jake2.util.Lib;
import jake2.util.Math3D;
import jake2.util.Vargs;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Main
 * 
 * @author cwei
 */
public abstract class Main extends Base {

	public static final int[] d_8to24table = new int[256];

	static final int c_visible_lightmaps = 0;
	static final int c_visible_textures = 0;

	int registration_sequence;

	
	
	boolean qglColorTableEXT;
	boolean qglSelectTextureSGIS;
	boolean qglActiveTextureARB;
	boolean qglPointParameterfEXT;
	boolean qglLockArraysEXT;
	boolean qglMTexCoord2fSGIS;
	boolean qwglSwapIntervalEXT;

	
	
	
	protected abstract void Draw_GetPalette();

	abstract void GL_ImageList_f();
	@Override
    public abstract void GL_ScreenShot_f();
	abstract void GL_SetTexturePalette(int[] palette);
	abstract void GL_Strings_f();

	abstract void Mod_Modellist_f();
	abstract mleaf_t Mod_PointInLeaf(float[] point, model_t model);

	abstract void GL_SetDefaultState();

	abstract void GL_InitImages();
	abstract void Mod_Init(); 
	abstract void R_InitParticleTexture(); 
	abstract void R_DrawAliasModel(entity_t e); 
	abstract void R_DrawBrushModel(entity_t e); 
	abstract void Draw_InitLocal();
	abstract void R_LightPoint(float[] p, float[] color);
	abstract void R_PushDlights();
	abstract void R_MarkLeaves();
	abstract void R_DrawWorld();
	abstract void R_RenderDlights();
	abstract void R_DrawAlphaSurfaces();

	abstract void Mod_FreeAll();

	abstract void GL_ShutdownImages();
	abstract void GL_Bind(int texnum);
	abstract void GL_TexEnv(int mode);
	abstract void GL_TextureMode(String string);
	abstract void GL_TextureAlphaMode(String string);
	abstract void GL_TextureSolidMode(String string);
	abstract void GL_UpdateSwapInterval();

	/*
	====================================================================
	
	from gl_rmain.c
	
	====================================================================
	*/

	model_t r_worldmodel;

	float gldepthmin;
    float gldepthmax;

	final glconfig_t gl_config = new glconfig_t();
	final glstate_t gl_state = new glstate_t();

	image_t r_notexture; 
	image_t r_particletexture; 

	entity_t currententity;
	model_t currentmodel;

	final cplane_t[] frustum = { new cplane_t(), new cplane_t(), new cplane_t(), new cplane_t()};

	int r_visframecount; 
	int r_framecount; 

	int c_brush_polys;
    int c_alias_polys;

	final float[] v_blend = {(float) 0, (float) 0, (float) 0, (float) 0};

	
	
	
	final float[] vup = {(float) 0, (float) 0, (float) 0};
	final float[] vpn = {(float) 0, (float) 0, (float) 0};
	final float[] vright = {(float) 0, (float) 0, (float) 0};
	final float[] r_origin = {(float) 0, (float) 0, (float) 0};

	final FloatBuffer r_world_matrix = Lib.newFloatBuffer(16);
    float[] r_base_world_matrix = new float[16];

	
	
	
	refdef_t r_newrefdef = new refdef_t();

	int r_viewcluster;
    int r_viewcluster2;
    int r_oldviewcluster;
    int r_oldviewcluster2;

	cvar_t r_norefresh;
	cvar_t r_drawentities;
	cvar_t r_drawworld;
	cvar_t r_speeds;
	cvar_t r_fullbright;
	cvar_t r_novis;
	cvar_t r_nocull;
	cvar_t r_lerpmodels;
	cvar_t r_lefthand;

	cvar_t r_lightlevel;
	

	cvar_t gl_nosubimage;
	cvar_t gl_allow_software;

	cvar_t gl_vertex_arrays;

	cvar_t gl_particle_min_size;
	cvar_t gl_particle_max_size;
	cvar_t gl_particle_size;
	cvar_t gl_particle_att_a;
	cvar_t gl_particle_att_b;
	cvar_t gl_particle_att_c;

	cvar_t gl_ext_swapinterval;
	cvar_t gl_ext_palettedtexture;
	cvar_t gl_ext_multitexture;
	cvar_t gl_ext_pointparameters;
	cvar_t gl_ext_compiled_vertex_array;

	cvar_t gl_log;
	cvar_t gl_bitdepth;
	cvar_t gl_drawbuffer;
	cvar_t gl_driver;
	cvar_t gl_lightmap;
	cvar_t gl_shadows;
	cvar_t gl_mode;
	cvar_t gl_dynamic;
	cvar_t gl_monolightmap;
	cvar_t gl_modulate;
	cvar_t gl_nobind;
	cvar_t gl_round_down;
	cvar_t gl_picmip;
	cvar_t gl_skymip;
	cvar_t gl_showtris;
	cvar_t gl_ztrick;
	cvar_t gl_finish;
	cvar_t gl_clear;
	cvar_t gl_cull;
	cvar_t gl_polyblend;
	cvar_t gl_flashblend;
	cvar_t gl_playermip;
	cvar_t gl_saturatelighting;
	cvar_t gl_swapinterval;
	cvar_t gl_texturemode;
	cvar_t gl_texturealphamode;
	cvar_t gl_texturesolidmode;
	cvar_t gl_lockpvs;

	cvar_t gl_3dlabs_broken;

	cvar_t vid_gamma;
	cvar_t vid_ref;

	
	
	

	/*
	=================
	R_CullBox
	
	Returns true if the box is completely outside the frustom
	=================
	*/
	final boolean R_CullBox(float[] mins, float[] maxs) {
		assert(mins.length == 3 && maxs.length == 3) : "vec3_t bug";

		if (r_nocull.value != (float) 0)
			return false;

		for (int i = 0; i < 4; i++) {
			if (Math3D.BoxOnPlaneSide(mins, maxs, frustum[i]) == 2) {
				return true;
			}
		}
		return false;
	}

	final void R_RotateForEntity(entity_t e) {

		gl.glTranslatef(e.origin[0], e.origin[1], e.origin[2]);

		gl.glRotatef(e.angles[1], (float) 0, (float) 0, 1.0F);
		gl.glRotatef(-e.angles[0], (float) 0, 1.0F, (float) 0);
		gl.glRotatef(-e.angles[2], 1.0F, (float) 0, (float) 0);
	}

	/*
	=============================================================
	
	   SPRITE MODELS
	
	=============================================================
	*/

	/*
	=================
	R_DrawSpriteModel
	
	=================
	*/
	void R_DrawSpriteModel(entity_t e) {


        qfiles.dsprite_t psprite = (qfiles.dsprite_t) currentmodel.extradata;

		e.frame %= psprite.numframes;

        qfiles.dsprframe_t frame = psprite.frames[e.frame];

        float alpha = 1.0F;
		if ((e.flags & Defines.RF_TRANSLUCENT) != 0)
			alpha = e.alpha;

		if (alpha != 1.0F)
			gl.glEnable(GL_BLEND);

		gl.glColor4f(1.0F, 1.0F, 1.0F, alpha);

		GL_Bind(currentmodel.skins[e.frame].texnum);

		GL_TexEnv(GL_MODULATE);

		if ((double) alpha == 1.0)
			gl.glEnable(GL_ALPHA_TEST);
		else
			gl.glDisable(GL_ALPHA_TEST);

		gl.glBegin(GL_QUADS);

		gl.glTexCoord2f((float) 0, 1.0F);
		float[] point = {(float) 0, (float) 0, (float) 0};
		Math3D.VectorMA(e.origin, (float) -frame.origin_y, vup, point);
		Math3D.VectorMA(point, (float) -frame.origin_x, vright, point);
		gl.glVertex3f(point[0], point[1], point[2]);

		gl.glTexCoord2f((float) 0, (float) 0);
		Math3D.VectorMA(e.origin, (float) (frame.height - frame.origin_y), vup, point);
		Math3D.VectorMA(point, (float) -frame.origin_x, vright, point);
		gl.glVertex3f(point[0], point[1], point[2]);

		gl.glTexCoord2f(1.0F, (float) 0);
		Math3D.VectorMA(e.origin, (float) (frame.height - frame.origin_y), vup, point);
		Math3D.VectorMA(point, (float) (frame.width - frame.origin_x), vright, point);
		gl.glVertex3f(point[0], point[1], point[2]);

		gl.glTexCoord2f(1.0F, 1.0F);
		Math3D.VectorMA(e.origin, (float) -frame.origin_y, vup, point);
		Math3D.VectorMA(point, (float) (frame.width - frame.origin_x), vright, point);
		gl.glVertex3f(point[0], point[1], point[2]);

		gl.glEnd();

		gl.glDisable(GL_ALPHA_TEST);
		GL_TexEnv(GL_REPLACE);

		if (alpha != 1.0F)
			gl.glDisable(GL_BLEND);

		gl.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	

	/*
	=============
	R_DrawNullModel
	=============
	cwei :-)
	*/
	void R_DrawNullModel() {
		float[] shadelight = {(float) 0, (float) 0, (float) 0};

		if ((currententity.flags & Defines.RF_FULLBRIGHT) != 0) {
			
			shadelight[0] = shadelight[1] = shadelight[2] = 0.0F;
			shadelight[2] = 0.8F;
		}
		else {
			R_LightPoint(currententity.origin, shadelight);
		}

		gl.glPushMatrix();
		R_RotateForEntity(currententity);

		gl.glDisable(GL_TEXTURE_2D);
		gl.glColor3f(shadelight[0], shadelight[1], shadelight[2]);




		
		gl.glBegin(GL_TRIANGLE_FAN);
		gl.glVertex3f((float) 0, (float) 0, -16.0F);
		int i;
		for (i=0 ; i<=4 ; i++) {
			gl.glVertex3f((float)(16.0 * Math.cos((double) i * Math.PI / 2.0)), (float)(16.0 * Math.sin((double) i * Math.PI / 2.0)), 0.0f);
		}
		gl.glEnd();
		
		gl.glBegin(GL_TRIANGLE_FAN);
		gl.glVertex3f ((float) 0, (float) 0, 16.0F);
		for (i=4 ; i>=0 ; i--) {
			gl.glVertex3f((float)(16.0 * Math.cos((double) i * Math.PI / 2.0)), (float)(16.0 * Math.sin((double) i * Math.PI / 2.0)), 0.0f);
		}
		gl.glEnd();
        
		gl.glColor3f(1.0F, 1.0F, 1.0F);
		gl.glPopMatrix();
		gl.glEnable(GL_TEXTURE_2D);
	}

	/*
	=============
	R_DrawEntitiesOnList
	=============
	*/
	void R_DrawEntitiesOnList() {

		if (r_drawentities.value == 0.0f)
			return;


		int i;
		for (i = 0; i < r_newrefdef.num_entities; i++) {
			currententity = r_newrefdef.entities[i];
			if ((currententity.flags & Defines.RF_TRANSLUCENT) != 0)
				continue; 

			if ((currententity.flags & Defines.RF_BEAM) != 0) {
				R_DrawBeam(currententity);
			}
			else {
				currentmodel = currententity.model;
				if (currentmodel == null) {
					R_DrawNullModel();
					continue;
				}
				switch (currentmodel.type) {
					case mod_alias :
						R_DrawAliasModel(currententity);
						break;
					case mod_brush :
						R_DrawBrushModel(currententity);
						break;
					case mod_sprite :
						R_DrawSpriteModel(currententity);
						break;
					default :
						Com.Error(Defines.ERR_DROP, "Bad modeltype");
						break;
				}
			}
		}
		
		
		gl.glDepthMask(false); 
		for (i = 0; i < r_newrefdef.num_entities; i++) {
			currententity = r_newrefdef.entities[i];
			if ((currententity.flags & Defines.RF_TRANSLUCENT) == 0)
				continue; 

			if ((currententity.flags & Defines.RF_BEAM) != 0) {
				R_DrawBeam(currententity);
			}
			else {
				currentmodel = currententity.model;

				if (currentmodel == null) {
					R_DrawNullModel();
					continue;
				}
				switch (currentmodel.type) {
					case mod_alias :
						R_DrawAliasModel(currententity);
						break;
					case mod_brush :
						R_DrawBrushModel(currententity);
						break;
					case mod_sprite :
						R_DrawSpriteModel(currententity);
						break;
					default :
						Com.Error(Defines.ERR_DROP, "Bad modeltype");
						break;
				}
			}
		}
		gl.glDepthMask(true); 
	}
	
	/*
	** GL_DrawParticles
	**
	*/
	void GL_DrawParticles(int num_particles) {
		float[] up = {(float) 0, (float) 0, (float) 0};

		Math3D.VectorScale(vup, 1.5f, up);
		float[] right = {(float) 0, (float) 0, (float) 0};
		Math3D.VectorScale(vright, 1.5f, right);
		
		GL_Bind(r_particletexture.texnum);
		gl.glDepthMask(false); 
		gl.glEnable(GL_BLEND);
		GL_TexEnv(GL_MODULATE);
		
		gl.glBegin(GL_TRIANGLES);

        FloatBuffer sourceVertices = particle_t.vertexArray;
        IntBuffer sourceColors = particle_t.colorArray;
		for (int j = 0, i = 0; i < num_particles; i++) {
            float origin_x = sourceVertices.get(j++);
            float origin_y = sourceVertices.get(j++);
            float origin_z = sourceVertices.get(j++);


            float scale = (origin_x - r_origin[0]) * vpn[0]
					+ (origin_y - r_origin[1]) * vpn[1]
					+ (origin_z - r_origin[2]) * vpn[2];

			scale = (scale < 20.0F) ? 1.0F : 1.0F + scale * 0.004f;

            int color = sourceColors.get(i);
			gl.glColor4ub(
				(byte)((color >> 0) & 0xFF),
				(byte)((color >> 8) & 0xFF),
				(byte)((color >> 16) & 0xFF),
				(byte)((color >> 24) & 0xFF)
			);
			
			gl.glTexCoord2f(0.0625f, 0.0625f);
			gl.glVertex3f(origin_x, origin_y, origin_z);
			
			gl.glTexCoord2f(1.0625f, 0.0625f);
			gl.glVertex3f(origin_x + up[0] * scale, origin_y + up[1] * scale, origin_z + up[2] * scale);
			
			gl.glTexCoord2f(0.0625f, 1.0625f);
			gl.glVertex3f(origin_x + right[0] * scale, origin_y + right[1] * scale, origin_z + right[2] * scale);
		}
		gl.glEnd();
		
		gl.glDisable(GL_BLEND);
		gl.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		gl.glDepthMask(true); 
		GL_TexEnv(GL_REPLACE);
	}

	/*
	===============
	R_DrawParticles
	===============
	*/
	void R_DrawParticles() {

		if (gl_ext_pointparameters.value != 0.0f && qglPointParameterfEXT) {

			gl.glEnableClientState(GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, 0, particle_t.vertexArray);
			gl.glEnableClientState(GL_COLOR_ARRAY);
			gl.glColorPointer(4, true, 0, particle_t.getColorAsByteBuffer());
			
			gl.glDepthMask(false);
			gl.glEnable(GL_BLEND);
			gl.glDisable(GL_TEXTURE_2D);
			gl.glPointSize(gl_particle_size.value);
			
			gl.glDrawArrays(GL_POINTS, 0, r_newrefdef.num_particles);
			
			gl.glDisableClientState(GL_COLOR_ARRAY);
			gl.glDisableClientState(GL_VERTEX_ARRAY);

			gl.glDisable(GL_BLEND);
			gl.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			gl.glDepthMask(true);
			gl.glEnable(GL_TEXTURE_2D);

		}
		else {
			GL_DrawParticles(r_newrefdef.num_particles);
		}
	}

	/*
	============
	R_PolyBlend
	============
	*/
	void R_PolyBlend() {
		if (gl_polyblend.value == 0.0f)
			return;

		if (v_blend[3] == 0.0f)
			return;

		gl.glDisable(GL_ALPHA_TEST);
		gl.glEnable(GL_BLEND);
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDisable(GL_TEXTURE_2D);

		gl.glLoadIdentity();

		
		gl.glRotatef(-90.0F, 1.0F, (float) 0, (float) 0);
		gl.glRotatef(90.0F, (float) 0, (float) 0, 1.0F);

		gl.glColor4f(v_blend[0], v_blend[1], v_blend[2], v_blend[3]);

		gl.glBegin(GL_QUADS);

		gl.glVertex3f(10.0F, 100.0F, 100.0F);
		gl.glVertex3f(10.0F, -100.0F, 100.0F);
		gl.glVertex3f(10.0F, -100.0F, -100.0F);
		gl.glVertex3f(10.0F, 100.0F, -100.0F);
		gl.glEnd();

		gl.glDisable(GL_BLEND);
		gl.glEnable(GL_TEXTURE_2D);
		gl.glEnable(GL_ALPHA_TEST);

		gl.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	

	static int SignbitsForPlane(cplane_t out) {

		int bits = 0;
		for (int j = 0; j < 3; j++) {
			if (out.normal[j] < (float) 0) {
				int i = (1 << j);
				bits = bits | i;
			}
		}
		return bits;
	}

	void R_SetFrustum() {
		
		Math3D.RotatePointAroundVector(frustum[0].normal, vup, vpn, - (90f - r_newrefdef.fov_x / 2f));
		
		Math3D.RotatePointAroundVector(frustum[1].normal, vup, vpn, 90f - r_newrefdef.fov_x / 2f);
		
		Math3D.RotatePointAroundVector(frustum[2].normal, vright, vpn, 90f - r_newrefdef.fov_y / 2f);
		
		Math3D.RotatePointAroundVector(frustum[3].normal, vright, vpn, - (90f - r_newrefdef.fov_y / 2f));

		for (int i = 0; i < 4; i++) {
			frustum[i].type = (byte) Defines.PLANE_ANYZ;
			frustum[i].dist = Math3D.DotProduct(r_origin, frustum[i].normal);
			frustum[i].signbits = (byte) SignbitsForPlane(frustum[i]);
		}
	}

	

	/*
	===============
	R_SetupFrame
	===============
	*/
	void R_SetupFrame() {

		r_framecount++;

		
		Math3D.VectorCopy(r_newrefdef.vieworg, r_origin);

		Math3D.AngleVectors(r_newrefdef.viewangles, vpn, vright, vup);

		
		if ((r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) == 0) {
			r_oldviewcluster = r_viewcluster;
			r_oldviewcluster2 = r_viewcluster2;
            mleaf_t leaf = Mod_PointInLeaf(r_origin, r_worldmodel);
			r_viewcluster = r_viewcluster2 = leaf.cluster;

			
			if (leaf.contents == 0) { 
				float[] temp = {(float) 0, (float) 0, (float) 0};

				Math3D.VectorCopy(r_origin, temp);
				temp[2] -= 16.0F;
				leaf = Mod_PointInLeaf(temp, r_worldmodel);
				if ((leaf.contents & Defines.CONTENTS_SOLID) == 0 && (leaf.cluster != r_viewcluster2))
					r_viewcluster2 = leaf.cluster;
			}
			else { 
				float[] temp = {(float) 0, (float) 0, (float) 0};

				Math3D.VectorCopy(r_origin, temp);
				temp[2] += 16.0F;
				leaf = Mod_PointInLeaf(temp, r_worldmodel);
				if ((leaf.contents & Defines.CONTENTS_SOLID) == 0 && (leaf.cluster != r_viewcluster2))
					r_viewcluster2 = leaf.cluster;
			}
		}

		for (int i = 0; i < 4; i++)
			v_blend[i] = r_newrefdef.blend[i];

		c_brush_polys = 0;
		c_alias_polys = 0;

		
		if ((r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) != 0) {
			gl.glEnable(GL_SCISSOR_TEST);
			gl.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);
			gl.glScissor(
				r_newrefdef.x,
				vid.getHeight() - r_newrefdef.height - r_newrefdef.y,
				r_newrefdef.width,
				r_newrefdef.height);
			gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			gl.glClearColor(1.0f, 0.0f, 0.5f, 0.5f);
			gl.glDisable(GL_SCISSOR_TEST);
		}
	}

	void MYgluPerspective(double fovy, double aspect, double zNear, double zFar) {

        double ymax = zNear * Math.tan(fovy * Math.PI / 360.0);
        double ymin = -ymax;

        double xmin = ymin * aspect;

		xmin += (double) -(2.0F * gl_state.camera_separation) / zNear;
        double xmax = ymax * aspect;
		xmax += (double) -(2.0F * gl_state.camera_separation) / zNear;

		gl.glFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
	}

	/*
	=============
	R_SetupGL
	=============
	*/
	void R_SetupGL() {


        int x = (int) Math.floor((double) (r_newrefdef.x * vid.getWidth() / vid.getWidth()));
        int x2 = (int) Math.ceil((double) ((r_newrefdef.x + r_newrefdef.width) * vid.getWidth() / vid.getWidth()));
        int y = (int) Math.floor((double) (vid.getHeight() - r_newrefdef.y * vid.getHeight() / vid.getHeight()));
        int y2 = (int) Math.ceil((double) (vid.getHeight() - (r_newrefdef.y + r_newrefdef.height) * vid.getHeight() / vid.getHeight()));

        int w = x2 - x;
        int h = y - y2;

		gl.glViewport(x, y2, w, h);


        float screenaspect = (float) r_newrefdef.width / (float) r_newrefdef.height;
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		MYgluPerspective((double) r_newrefdef.fov_y, (double) screenaspect, 4.0, 4096.0);

		gl.glCullFace(GL_FRONT);

		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glRotatef(-90.0F, 1.0F, (float) 0, (float) 0);
		gl.glRotatef(90.0F, (float) 0, (float) 0, 1.0F);
		gl.glRotatef(-r_newrefdef.viewangles[2], 1.0F, (float) 0, (float) 0);
		gl.glRotatef(-r_newrefdef.viewangles[0], (float) 0, 1.0F, (float) 0);
		gl.glRotatef(-r_newrefdef.viewangles[1], (float) 0, (float) 0, 1.0F);
		gl.glTranslatef(-r_newrefdef.vieworg[0], -r_newrefdef.vieworg[1], -r_newrefdef.vieworg[2]);

		gl.glGetFloat(GL_MODELVIEW_MATRIX, r_world_matrix);
        r_world_matrix.clear();

		
		
		
		if (gl_cull.value != 0.0f)
			gl.glEnable(GL_CULL_FACE);
		else
			gl.glDisable(GL_CULL_FACE);

		gl.glDisable(GL_BLEND);
		gl.glDisable(GL_ALPHA_TEST);
		gl.glEnable(GL_DEPTH_TEST);
	}

	/*
	=============
	R_Clear
	=============
	*/
	int trickframe;

	void R_Clear() {
		if (gl_ztrick.value != 0.0f) {

			if (gl_clear.value != 0.0f) {
				gl.glClear(GL_COLOR_BUFFER_BIT);
			}

			trickframe++;
			if ((trickframe & 1) != 0) {
				gldepthmin = (float) 0;
				gldepthmax = 0.49999f;
				gl.glDepthFunc(GL_LEQUAL);
			}
			else {
				gldepthmin = 1.0F;
				gldepthmax = 0.5f;
				gl.glDepthFunc(GL_GEQUAL);
			}
		}
		else {
			if (gl_clear.value != 0.0f)
				gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			else
				gl.glClear(GL_DEPTH_BUFFER_BIT);

			gldepthmin = (float) 0;
			gldepthmax = 1.0F;
			gl.glDepthFunc(GL_LEQUAL);
		}
		gl.glDepthRange((double) gldepthmin, (double) gldepthmax);
	}

	void R_Flash() {
		R_PolyBlend();
	}

	/*
	================
	R_RenderView
	
	r_newrefdef must be set before the first call
	================
	*/
	void R_RenderView(refdef_t fd) {

		if (r_norefresh.value != 0.0f)
			return;

		r_newrefdef = fd;

		
		if (r_newrefdef == null) {
			Com.Error(Defines.ERR_DROP, "R_RenderView: refdef_t fd is null");
		}

		if (r_worldmodel == null && (r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) == 0)
			Com.Error(Defines.ERR_DROP, "R_RenderView: NULL worldmodel");

		if (r_speeds.value != 0.0f) {
			c_brush_polys = 0;
			c_alias_polys = 0;
		}

		R_PushDlights();

		if (gl_finish.value != 0.0f)
			gl.glFinish();

		R_SetupFrame();

		R_SetFrustum();

		R_SetupGL();

		R_MarkLeaves(); 

		R_DrawWorld();

		R_DrawEntitiesOnList();

		R_RenderDlights();

		R_DrawParticles();

		R_DrawAlphaSurfaces();

		R_Flash();

		if (r_speeds.value != 0.0f) {
			VID.Printf(
				Defines.PRINT_ALL,
				"%4i wpoly %4i epoly %i tex %i lmaps\n",
				new Vargs(4).add(c_brush_polys).add(c_alias_polys).add(c_visible_textures).add(c_visible_lightmaps));
		}
	}

	void R_SetGL2D() {
		
		gl.glViewport(0, 0, vid.getWidth(), vid.getHeight());
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho((double) 0, (double) vid.getWidth(), (double) vid.getHeight(), (double) 0, -99999.0, 99999.0);
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDisable(GL_CULL_FACE);
		gl.glDisable(GL_BLEND);
		gl.glEnable(GL_ALPHA_TEST);
		gl.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/*
	====================
	R_SetLightLevel
	
	====================
	*/
	void R_SetLightLevel() {

		if ((r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) != 0)
			return;


		float[] shadelight = {(float) 0, (float) 0, (float) 0};
		R_LightPoint(r_newrefdef.vieworg, shadelight);

		
		
		if (shadelight[0] > shadelight[1]) {
			if (shadelight[0] > shadelight[2])
				r_lightlevel.value = 150.0F * shadelight[0];
			else
				r_lightlevel.value = 150.0F * shadelight[2];
		}
		else {
			if (shadelight[1] > shadelight[2])
				r_lightlevel.value = 150.0F * shadelight[1];
			else
				r_lightlevel.value = 150.0F * shadelight[2];
		}
	}

	/*
	@@@@@@@@@@@@@@@@@@@@@
	R_RenderFrame
	
	@@@@@@@@@@@@@@@@@@@@@
	*/
	@Override
    public void R_RenderFrame(refdef_t fd) {
		R_RenderView(fd);
		R_SetLightLevel();
		R_SetGL2D();
	}

	protected void R_Register() {
		r_lefthand = Cvar.Get("hand", "0", Defines.CVAR_USERINFO | Defines.CVAR_ARCHIVE);
		r_norefresh = Cvar.Get("r_norefresh", "0", 0);
		r_fullbright = Cvar.Get("r_fullbright", "0", 0);
		r_drawentities = Cvar.Get("r_drawentities", "1", 0);
		r_drawworld = Cvar.Get("r_drawworld", "1", 0);
		r_novis = Cvar.Get("r_novis", "0", 0);
		r_nocull = Cvar.Get("r_nocull", "0", 0);
		r_lerpmodels = Cvar.Get("r_lerpmodels", "1", 0);
		r_speeds = Cvar.Get("r_speeds", "0", 0);

		r_lightlevel = Cvar.Get("r_lightlevel", "1", 0);

		gl_nosubimage = Cvar.Get("gl_nosubimage", "0", 0);
		gl_allow_software = Cvar.Get("gl_allow_software", "0", 0);

		gl_particle_min_size = Cvar.Get("gl_particle_min_size", "2", Defines.CVAR_ARCHIVE);
		gl_particle_max_size = Cvar.Get("gl_particle_max_size", "40", Defines.CVAR_ARCHIVE);
		gl_particle_size = Cvar.Get("gl_particle_size", "40", Defines.CVAR_ARCHIVE);
		gl_particle_att_a = Cvar.Get("gl_particle_att_a", "0.01", Defines.CVAR_ARCHIVE);
		gl_particle_att_b = Cvar.Get("gl_particle_att_b", "0.0", Defines.CVAR_ARCHIVE);
		gl_particle_att_c = Cvar.Get("gl_particle_att_c", "0.01", Defines.CVAR_ARCHIVE);

		gl_modulate = Cvar.Get("gl_modulate", "1.5", Defines.CVAR_ARCHIVE);
		gl_log = Cvar.Get("gl_log", "0", 0);
		gl_bitdepth = Cvar.Get("gl_bitdepth", "0", 0);
		gl_mode = Cvar.Get("gl_mode", "1", Defines.CVAR_ARCHIVE);
		gl_lightmap = Cvar.Get("gl_lightmap", "0", 0);
		gl_shadows = Cvar.Get("gl_shadows", "0", Defines.CVAR_ARCHIVE);
		gl_dynamic = Cvar.Get("gl_dynamic", "1", 0);
		gl_nobind = Cvar.Get("gl_nobind", "0", 0);
		gl_round_down = Cvar.Get("gl_round_down", "1", 0);
		gl_picmip = Cvar.Get("gl_picmip", "0", 0);
		gl_skymip = Cvar.Get("gl_skymip", "0", 0);
		gl_showtris = Cvar.Get("gl_showtris", "0", 0);
		gl_ztrick = Cvar.Get("gl_ztrick", "0", 0);
		gl_finish = Cvar.Get("gl_finish", "0", Defines.CVAR_ARCHIVE);
		gl_clear = Cvar.Get("gl_clear", "0", 0);
		gl_cull = Cvar.Get("gl_cull", "1", 0);
		gl_polyblend = Cvar.Get("gl_polyblend", "1", 0);
		gl_flashblend = Cvar.Get("gl_flashblend", "0", 0);
		gl_playermip = Cvar.Get("gl_playermip", "0", 0);
		gl_monolightmap = Cvar.Get("gl_monolightmap", "0", 0);
		gl_driver = Cvar.Get("gl_driver", "opengl32", Defines.CVAR_ARCHIVE);
		gl_texturemode = Cvar.Get("gl_texturemode", "GL_LINEAR_MIPMAP_NEAREST", Defines.CVAR_ARCHIVE);
		gl_texturealphamode = Cvar.Get("gl_texturealphamode", "default", Defines.CVAR_ARCHIVE);
		gl_texturesolidmode = Cvar.Get("gl_texturesolidmode", "default", Defines.CVAR_ARCHIVE);
		gl_lockpvs = Cvar.Get("gl_lockpvs", "0", 0);

		gl_vertex_arrays = Cvar.Get("gl_vertex_arrays", "0", Defines.CVAR_ARCHIVE);

		gl_ext_swapinterval = Cvar.Get("gl_ext_swapinterval", "1", Defines.CVAR_ARCHIVE);
		gl_ext_palettedtexture = Cvar.Get("gl_ext_palettedtexture", "0", Defines.CVAR_ARCHIVE);
		gl_ext_multitexture = Cvar.Get("gl_ext_multitexture", "1", Defines.CVAR_ARCHIVE);
		gl_ext_pointparameters = Cvar.Get("gl_ext_pointparameters", "1", Defines.CVAR_ARCHIVE);
		gl_ext_compiled_vertex_array = Cvar.Get("gl_ext_compiled_vertex_array", "1", Defines.CVAR_ARCHIVE);

		gl_drawbuffer = Cvar.Get("gl_drawbuffer", "GL_BACK", 0);
		gl_swapinterval = Cvar.Get("gl_swapinterval", "1", Defines.CVAR_ARCHIVE);

		gl_saturatelighting = Cvar.Get("gl_saturatelighting", "0", 0);

		gl_3dlabs_broken = Cvar.Get("gl_3dlabs_broken", "1", Defines.CVAR_ARCHIVE);

		vid_fullscreen = Cvar.Get("vid_fullscreen", "0", Defines.CVAR_ARCHIVE);
		vid_gamma = Cvar.Get("vid_gamma", "1.0", Defines.CVAR_ARCHIVE);
		vid_ref = Cvar.Get("vid_ref", "jogl", Defines.CVAR_ARCHIVE);

		Cmd.AddCommand("imagelist", new xcommand_t() {
			@Override
            public void execute() {
				GL_ImageList_f();
			}
		});

		Cmd.AddCommand("screenshot", new xcommand_t() {
			@Override
            public void execute() {
				glImpl.screenshot();
			}
		});
		Cmd.AddCommand("modellist", new xcommand_t() {
			@Override
            public void execute() {
				Mod_Modellist_f();
			}
		});
		Cmd.AddCommand("gl_strings", new xcommand_t() {
			@Override
            public void execute() {
				GL_Strings_f();
			}
		});
	}

	/*
	==================
	R_SetMode
	==================
	*/
	protected boolean R_SetMode() {


        boolean fullscreen = (vid_fullscreen.value > 0.0f);

		vid_fullscreen.modified = false;
		gl_mode.modified = false;

        Dimension dim = new Dimension(vid.getWidth(), vid.getHeight());

		int err;
		if ((err = glImpl.setMode(dim, (int) gl_mode.value, fullscreen)) == rserr_ok) {
			gl_state.prev_mode = (int) gl_mode.value;
		}
		else {
            switch (err) {
                case rserr_invalid_fullscreen:
                    Cvar.SetValue("vid_fullscreen", 0);
                    vid_fullscreen.modified = false;
                    VID.Printf(Defines.PRINT_ALL, "ref_gl::R_SetMode() - fullscreen unavailable in this mode\n");
                    if ((err = glImpl.setMode(dim, (int) gl_mode.value, false)) == rserr_ok)
                        return true;
                    break;
                case rserr_invalid_mode:
                    Cvar.SetValue("gl_mode", gl_state.prev_mode);
                    gl_mode.modified = false;
                    VID.Printf(Defines.PRINT_ALL, "ref_gl::R_SetMode() - invalid mode\n");
                    break;
            }

			
			if ((err = glImpl.setMode(dim, gl_state.prev_mode, false)) != rserr_ok) {
				VID.Printf(Defines.PRINT_ALL, "ref_gl::R_SetMode() - could not revert to safe mode\n");
				return false;
			}
		}
		return true;
	}

	/*
	===============
	R_Init
	===============
	*/
	final float[] r_turbsin = new float[256];

    protected boolean R_Init() {
        return R_Init(0, 0);
    }
    
	@Override
    public boolean R_Init(int vid_xpos, int vid_ypos) {

		assert(Warp.SIN.length == 256) : "warpsin table bug";

		
		for (int j = 0; j < 256; j++) {
			r_turbsin[j] = Warp.SIN[j] * 0.5f;
		}

		VID.Printf(Defines.PRINT_ALL, "ref_gl version: " + REF_VERSION + '\n');

		Draw_GetPalette();

		R_Register();

		
		gl_state.prev_mode = 3;

		
		if (!R_SetMode()) {
			VID.Printf(Defines.PRINT_ALL, "ref_gl::R_Init() - could not R_SetMode()\n");
			return false;
		}
		return true;
	}

	@Override
    public boolean R_Init2() {
		VID.MenuInit();
        
		/*
		** get our various GL strings
		*/

		gl_config.vendor_string = gl.glGetString(GL_VENDOR);
		VID.Printf(Defines.PRINT_ALL, "GL_VENDOR: " + gl_config.vendor_string + '\n');
		gl_config.renderer_string = gl.glGetString(GL_RENDERER);
		VID.Printf(Defines.PRINT_ALL, "GL_RENDERER: " + gl_config.renderer_string + '\n');
		gl_config.version_string = gl.glGetString(GL_VERSION);
		VID.Printf(Defines.PRINT_ALL, "GL_VERSION: " + gl_config.version_string + '\n');
		gl_config.extensions_string = gl.glGetString(GL_EXTENSIONS);
		VID.Printf(Defines.PRINT_ALL, "GL_EXTENSIONS: " + gl_config.extensions_string + '\n');
		
		gl_config.parseOpenGLVersion();

        String renderer_buffer = gl_config.renderer_string.toLowerCase();
        String vendor_buffer = gl_config.vendor_string.toLowerCase();

		if (renderer_buffer.contains("voodoo")) {
			if (!renderer_buffer.contains("rush"))
				gl_config.renderer = GL_RENDERER_VOODOO;
			else
				gl_config.renderer = GL_RENDERER_VOODOO_RUSH;
		}
		else if (vendor_buffer.contains("sgi"))
			gl_config.renderer = GL_RENDERER_SGI;
		else if (renderer_buffer.contains("permedia"))
			gl_config.renderer = GL_RENDERER_PERMEDIA2;
		else if (renderer_buffer.contains("glint"))
			gl_config.renderer = GL_RENDERER_GLINT_MX;
		else if (renderer_buffer.contains("glzicd"))
			gl_config.renderer = GL_RENDERER_REALIZM;
		else if (renderer_buffer.contains("gdi"))
			gl_config.renderer = GL_RENDERER_MCD;
		else if (renderer_buffer.contains("pcx2"))
			gl_config.renderer = GL_RENDERER_PCX2;
		else if (renderer_buffer.contains("verite"))
			gl_config.renderer = GL_RENDERER_RENDITION;
		else
			gl_config.renderer = GL_RENDERER_OTHER;

        String monolightmap = gl_monolightmap.string.toUpperCase();
		if (monolightmap.length() < 2 || (int) monolightmap.charAt(1) != (int) 'F') {
			if (gl_config.renderer == GL_RENDERER_PERMEDIA2) {
				Cvar.Set("gl_monolightmap", "A");
				VID.Printf(Defines.PRINT_ALL, "...using gl_monolightmap 'a'\n");
			}
			else if ((gl_config.renderer & GL_RENDERER_POWERVR) != 0) {
				Cvar.Set("gl_monolightmap", "0");
			}
			else {
				Cvar.Set("gl_monolightmap", "0");
			}
		}

		
		
		if ((gl_config.renderer & GL_RENDERER_POWERVR) != 0) {
			Cvar.Set("scr_drawall", "1");
		}
		else {
			Cvar.Set("scr_drawall", "0");
		}

		
		Cvar.SetValue("gl_finish", 1);
		

		
		if (gl_config.renderer == GL_RENDERER_MCD) {
			Cvar.SetValue("gl_finish", 1);
		}

		if ((gl_config.renderer & GL_RENDERER_3DLABS) != 0) {
			gl_config.allow_cds = gl_3dlabs_broken.value == 0.0f;
		}
		else {
			gl_config.allow_cds = true;
		}

		if (gl_config.allow_cds)
			VID.Printf(Defines.PRINT_ALL, "...allowing CDS\n");
		else
			VID.Printf(Defines.PRINT_ALL, "...disabling CDS\n");

		/*
		** grab extensions
		*/
		if (gl_config.extensions_string.contains("GL_EXT_compiled_vertex_array")
			|| gl_config.extensions_string.contains("GL_SGI_compiled_vertex_array")) {
			VID.Printf(Defines.PRINT_ALL, "...enabling GL_EXT_compiled_vertex_array\n");
			
			qglLockArraysEXT = gl_ext_compiled_vertex_array.value != 0.0f;
			
			
		}
		else {
			VID.Printf(Defines.PRINT_ALL, "...GL_EXT_compiled_vertex_array not found\n");
		}

		if (gl_config.extensions_string.contains("WGL_EXT_swap_control")) {
			qwglSwapIntervalEXT = true;
			VID.Printf(Defines.PRINT_ALL, "...enabling WGL_EXT_swap_control\n");
		} else {
			qwglSwapIntervalEXT = false;
			VID.Printf(Defines.PRINT_ALL, "...WGL_EXT_swap_control not found\n");
		}

		if (gl_config.extensions_string.contains("GL_EXT_point_parameters")) {
			if (gl_ext_pointparameters.value != 0.0f) {
				
				qglPointParameterfEXT = true;
				
				VID.Printf(Defines.PRINT_ALL, "...using GL_EXT_point_parameters\n");
			}
			else {
				VID.Printf(Defines.PRINT_ALL, "...ignoring GL_EXT_point_parameters\n");
			}
		}
		else {
			VID.Printf(Defines.PRINT_ALL, "...GL_EXT_point_parameters not found\n");
		}

		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		

		if (!qglColorTableEXT
			&& gl_config.extensions_string.contains("GL_EXT_paletted_texture")
			&& gl_config.extensions_string.contains("GL_EXT_shared_texture_palette")) {
			if (gl_ext_palettedtexture.value != 0.0f) {
				VID.Printf(Defines.PRINT_ALL, "...using GL_EXT_shared_texture_palette\n");
				qglColorTableEXT = false; 
			}
			else {
				VID.Printf(Defines.PRINT_ALL, "...ignoring GL_EXT_shared_texture_palette\n");
				qglColorTableEXT = false;
			}
		}
		else {
			VID.Printf(Defines.PRINT_ALL, "...GL_EXT_shared_texture_palette not found\n");
		}

		if (gl_config.extensions_string.contains("GL_ARB_multitexture")) {
			if (gl_ext_multitexture.value != 0.0f) {
				VID.Printf(Defines.PRINT_ALL, "...using GL_ARB_multitexture\n");
				
				
				
				qglActiveTextureARB = true;
				qglMTexCoord2fSGIS = true;


				Cvar.SetValue("r_fullbright", 1);
			}
			else {
				VID.Printf(Defines.PRINT_ALL, "...ignoring GL_ARB_multitexture\n");
				Cvar.SetValue("r_fullbright", 0);
			}
		}
		else {
			VID.Printf(Defines.PRINT_ALL, "...GL_ARB_multitexture not found\n");
			Cvar.SetValue("r_fullbright", 0);
		}

		if (gl_config.extensions_string.contains("GL_SGIS_multitexture")) {
			if (qglActiveTextureARB) {
				VID.Printf(Defines.PRINT_ALL, "...GL_SGIS_multitexture deprecated in favor of ARB_multitexture\n");
				Cvar.SetValue("r_fullbright", 1);
			}	else if (gl_ext_multitexture.value != 0.0f) {
				VID.Printf(Defines.PRINT_ALL, "...using GL_SGIS_multitexture\n");
				
				
				qglSelectTextureSGIS = true;
				qglMTexCoord2fSGIS = true;
				Cvar.SetValue("r_fullbright", 1);
				
				
			} else {
				VID.Printf(Defines.PRINT_ALL, "...ignoring GL_SGIS_multitexture\n");
				Cvar.SetValue("r_fullbright", 0);
			}
		}
		else {
			VID.Printf(Defines.PRINT_ALL, "...GL_SGIS_multitexture not found\n");
			if (!qglActiveTextureARB)
				Cvar.SetValue("r_fullbright", 0);
		}

		GL_SetDefaultState();

		GL_InitImages();
		Mod_Init();
		R_InitParticleTexture();
		Draw_InitLocal();

        int err = gl.glGetError();
		if (err != GL_NO_ERROR)
			VID.Printf(
				Defines.PRINT_ALL,
				"glGetError() = 0x%x\n\t%s\n",
				new Vargs(2).add(err).add(gl.glGetString(err)));

		return true;
	}

	/*
	===============
	R_Shutdown
	===============
	*/
	@Override
    public void R_Shutdown() {
		Cmd.RemoveCommand("modellist");
		Cmd.RemoveCommand("screenshot");
		Cmd.RemoveCommand("imagelist");
		Cmd.RemoveCommand("gl_strings");

		Mod_FreeAll();

		GL_ShutdownImages();

		/*
		 * shut down OS specific OpenGL stuff like contexts, etc.
		 */
        glImpl.shutdown();
	}

	/*
	@@@@@@@@@@@@@@@@@@@@@
	R_BeginFrame
	@@@@@@@@@@@@@@@@@@@@@
	*/
	@Override
    public boolean R_BeginFrame(float camera_separation) {
	    
	    vid.update();
	    
		gl_state.camera_separation = camera_separation;

		/*
		** change modes if necessary
		*/
		if (gl_mode.modified || vid_fullscreen.modified) {

            cvar_t ref = Cvar.Get("vid_ref", "lwjgl", 0);
			ref.modified = true;
		}

		if (gl_log.modified) {
            glImpl.enableLogging((gl_log.value != 0.0f));
			gl_log.modified = false;
		}

		if (gl_log.value != 0.0f) {
            glImpl.logNewFrame();
		}

		/*
		** update 3Dfx gamma -- it is expected that a user will do a vid_restart
		** after tweaking this value
		*/
		if (vid_gamma.modified) {
			vid_gamma.modified = false;

			if ((gl_config.renderer & GL_RENDERER_VOODOO) != 0) {
				

				/* 
				char envbuffer[1024];
				float g;
				
				g = 2.00 * ( 0.8 - ( vid_gamma->value - 0.5 ) ) + 1.0F;
				Com_sprintf( envbuffer, sizeof(envbuffer), "SSTV2_GAMMA=%f", g );
				putenv( envbuffer );
				Com_sprintf( envbuffer, sizeof(envbuffer), "SST_GAMMA=%f", g );
				putenv( envbuffer );
				*/
				VID.Printf(Defines.PRINT_DEVELOPER, "gamma anpassung fuer VOODOO nicht gesetzt");
			}
		}

            if( glImpl.beginFrame(camera_separation) ) {

		/*
		** go into 2D mode
		*/
		gl.glViewport(0, 0, vid.getWidth(), vid.getHeight());
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho((double) 0, (double) vid.getWidth(), (double) vid.getHeight(), (double) 0, -99999.0, 99999.0);
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDisable(GL_CULL_FACE);
		gl.glDisable(GL_BLEND);
		gl.glEnable(GL_ALPHA_TEST);
		gl.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		/*
		** draw buffer stuff
		*/
		if (gl_drawbuffer.modified) {
			gl_drawbuffer.modified = false;

			if (gl_state.camera_separation == (float) 0 || !gl_state.stereo_enabled) {
				if ("GL_FRONT".equalsIgnoreCase(gl_drawbuffer.string))
					gl.glDrawBuffer(GL_FRONT);
				else
					gl.glDrawBuffer(GL_BACK);
			}
		}

		/*
		** texturemode stuff
		*/
		if (gl_texturemode.modified) {
			GL_TextureMode(gl_texturemode.string);
			gl_texturemode.modified = false;
		}

		if (gl_texturealphamode.modified) {
			GL_TextureAlphaMode(gl_texturealphamode.string);
			gl_texturealphamode.modified = false;
		}

		if (gl_texturesolidmode.modified) {
			GL_TextureSolidMode(gl_texturesolidmode.string);
			gl_texturesolidmode.modified = false;
		}

		/*
		** swapinterval stuff
		*/
		GL_UpdateSwapInterval();

		
		
		
		R_Clear();
		
		return true;
            } else {
                return false;
            }
	}

	final int[] r_rawpalette = new int[256];

	/*
	=============
	R_SetPalette
	=============
	*/
	@Override
    public void R_SetPalette(byte[] palette) {
		
		
		int i;

		if (palette != null) {
            int j =0;
            int color = 0;
			for (i = 0; i < 256; i++) {
				color = ((int) palette[j++] & 0xFF) << 0;
				color |= ((int) palette[j++] & 0xFF) << 8;
				color |= ((int) palette[j++] & 0xFF) << 16;
				color |= 0xFF000000;
				r_rawpalette[i] = color;
			}
		}
		else {
			for (i = 0; i < 256; i++) {
				r_rawpalette[i] = d_8to24table[i] | 0xff000000;
			}
		}
		GL_SetTexturePalette(r_rawpalette);

		gl.glClearColor((float) 0, (float) 0, (float) 0, (float) 0);
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClearColor(1f, 0f, 0.5f, 0.5f);
	}

	static final int NUM_BEAM_SEGS = 6;
	final float[][] start_points = new float[NUM_BEAM_SEGS][3];
	
	final float[][] end_points = new float[NUM_BEAM_SEGS][3]; 

	/*
	** R_DrawBeam
	*/
	void R_DrawBeam(entity_t e) {

		float[] oldorigin = {(float) 0, (float) 0, (float) 0};

		oldorigin[0] = e.oldorigin[0];
		oldorigin[1] = e.oldorigin[1];
		oldorigin[2] = e.oldorigin[2];

		float[] origin = {(float) 0, (float) 0, (float) 0};
		origin[0] = e.origin[0];
		origin[1] = e.origin[1];
		origin[2] = e.origin[2];

		float[] normalized_direction = {(float) 0, (float) 0, (float) 0};
		float[] direction = {(float) 0, (float) 0, (float) 0};
		normalized_direction[0] = direction[0] = oldorigin[0] - origin[0];
		normalized_direction[1] = direction[1] = oldorigin[1] - origin[1];
		normalized_direction[2] = direction[2] = oldorigin[2] - origin[2];

		if (Math3D.VectorNormalize(normalized_direction) == 0.0f)
			return;

		float[] perpvec = {(float) 0, (float) 0, (float) 0};
		Math3D.PerpendicularVector(perpvec, normalized_direction);
		Math3D.VectorScale(perpvec, (float) e.frame / 2f, perpvec);

		int i;
		for (i = 0; i < 6; i++) {
			Math3D.RotatePointAroundVector(
				start_points[i],
				normalized_direction,
				perpvec,
				(360.0f / (float) NUM_BEAM_SEGS) * (float) i);

			Math3D.VectorAdd(start_points[i], origin, start_points[i]);
			Math3D.VectorAdd(start_points[i], direction, end_points[i]);
		}

		gl.glDisable(GL_TEXTURE_2D);
		gl.glEnable(GL_BLEND);
		gl.glDepthMask(false);

        float r = (float) ((d_8to24table[e.skinnum & 0xFF]) & 0xFF);
        float g = (float) ((d_8to24table[e.skinnum & 0xFF] >> 8) & 0xFF);
        float b = (float) ((d_8to24table[e.skinnum & 0xFF] >> 16) & 0xFF);

		r *= 1.0F / 255.0f;
		g *= 1.0F / 255.0f;
		b *= 1.0F / 255.0f;

		gl.glColor4f(r, g, b, e.alpha);

		gl.glBegin(GL_TRIANGLE_STRIP);

		for (i = 0; i < NUM_BEAM_SEGS; i++) {
            float[] v = start_points[i];
			gl.glVertex3f(v[0], v[1], v[2]);
			v = end_points[i];
			gl.glVertex3f(v[0], v[1], v[2]);
			v = start_points[(i + 1) % NUM_BEAM_SEGS];
			gl.glVertex3f(v[0], v[1], v[2]);
			v = end_points[(i + 1) % NUM_BEAM_SEGS];
			gl.glVertex3f(v[0], v[1], v[2]);
		}
		gl.glEnd();

		gl.glEnable(GL_TEXTURE_2D);
		gl.glDisable(GL_BLEND);
		gl.glDepthMask(true);
	}

}
