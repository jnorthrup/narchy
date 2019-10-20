/*
 * Surf.java
 * Copyright (C) 2003
 *
 * $Id: Surf.java,v 1.3 2006-11-21 02:22:19 cawe Exp $
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
import jake2.client.dlight_t;
import jake2.client.entity_t;
import jake2.client.lightstyle_t;
import jake2.game.cplane_t;
import jake2.qcommon.Com;
import jake2.render.*;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Surf
 *
 * @author cwei
 */
public abstract class Surf extends Draw {

    
    final float[] modelorg = {0, 0, 0};        

    msurface_t r_alpha_surfaces;

    static final int DYNAMIC_LIGHT_WIDTH = 128;
    static final int DYNAMIC_LIGHT_HEIGHT = 128;

    static final int LIGHTMAP_BYTES = 4;

    static final int BLOCK_WIDTH = 128;
    static final int BLOCK_HEIGHT = 128;

    static final int MAX_LIGHTMAPS = 128;

    int c_visible_lightmaps;
    int c_visible_textures;

    static final int GL_LIGHTMAP_FORMAT = GL_RGBA;

    static class gllightmapstate_t {
        int internal_format;
        int current_lightmap_texture;

        final msurface_t[] lightmap_surfaces = new msurface_t[MAX_LIGHTMAPS];
        final int[] allocated = new int[BLOCK_WIDTH];

        
        
        
        final IntBuffer lightmap_buffer = Lib.newIntBuffer(BLOCK_WIDTH * BLOCK_HEIGHT, ByteOrder.LITTLE_ENDIAN);

        public gllightmapstate_t() {
            for (int i = 0; i < MAX_LIGHTMAPS; i++)
                lightmap_surfaces[i] = new msurface_t();
        }

        public void clearLightmapSurfaces() {
            for (int i = 0; i < MAX_LIGHTMAPS; i++)
                
                lightmap_surfaces[i] = new msurface_t();
        }

    }

    final gllightmapstate_t gl_lms = new gllightmapstate_t();

    
    abstract byte[] Mod_ClusterPVS(int cluster, model_t model);

    
    abstract void R_DrawSkyBox();

    abstract void R_AddSkySurface(msurface_t surface);

    abstract void R_ClearSkyBox();

    abstract void EmitWaterPolys(msurface_t fa);

    
    abstract void R_MarkLights(dlight_t light, int bit, mnode_t node);

    abstract void R_SetCacheState(msurface_t surf);

    abstract void R_BuildLightMap(msurface_t surf, IntBuffer dest, int stride);

	/*
    =============================================================

		BRUSH MODELS

	=============================================================
	*/

    /**
     * R_TextureAnimation
     * Returns the proper texture for a given time and base texture
     */
    image_t R_TextureAnimation(mtexinfo_t tex) {
        if (tex.next == null)
            return tex.image;

        int c = currententity.frame % tex.numframes;
        while (c != 0) {
            tex = tex.next;
            c--;
        }

        return tex.image;
    }

    /**
     * DrawGLPoly
     */
    void DrawGLPoly(glpoly_t p) {
        gl.glDrawArrays(GL_POLYGON, p.pos, p.numverts);
    }

    /**
     * DrawGLFlowingPoly
     * version that handles scrolling texture
     */
    void DrawGLFlowingPoly(glpoly_t p) {
        float scroll = -64 * ((r_newrefdef.time / 40.0f) - (int) (r_newrefdef.time / 40.0f));
        if (scroll == 0.0f)
            scroll = -64.0f;
        p.beginScrolling(scroll);
        gl.glDrawArrays(GL_POLYGON, p.pos, p.numverts);
        p.endScrolling();
    }

    /**
     * R_DrawTriangleOutlines
     */
    void R_DrawTriangleOutlines() {
        if (gl_showtris.value == 0)
            return;

        gl.glDisable(GL_TEXTURE_2D);
        gl.glDisable(GL_DEPTH_TEST);
        gl.glColor4f(1, 1, 1, 1);

        for (int i = 0; i < MAX_LIGHTMAPS; i++) {
            for (msurface_t surf = gl_lms.lightmap_surfaces[i]; surf != null; surf = surf.lightmapchain) {
                for (glpoly_t p = surf.polys; p != null; p = p.chain) {
                    for (int j = 2; j < p.numverts; j++) {
                        gl.glBegin(GL_LINE_STRIP);
                        gl.glVertex3f(p.x(0), p.y(0), p.z(0));
                        gl.glVertex3f(p.x(j - 1), p.y(j - 1), p.z(j - 1));
                        gl.glVertex3f(p.x(j), p.y(j), p.z(j));
                        gl.glVertex3f(p.x(0), p.y(0), p.z(0));
                        gl.glEnd();
                    }
                }
            }
        }

        gl.glEnable(GL_DEPTH_TEST);
        gl.glEnable(GL_TEXTURE_2D);
    }

    private final IntBuffer temp2 = Lib.newIntBuffer(34 * 34, ByteOrder.LITTLE_ENDIAN);

    /**
     * R_RenderBrushPoly
     */
    void R_RenderBrushPoly(msurface_t fa) {
        c_brush_polys++;

        image_t image = R_TextureAnimation(fa.texinfo);

        if ((fa.flags & Defines.SURF_DRAWTURB) != 0) {
            GL_Bind(image.texnum);

            
            GL_TexEnv(GL_MODULATE);
            gl.glColor4f(gl_state.inverse_intensity,
                    gl_state.inverse_intensity,
                    gl_state.inverse_intensity,
                    1.0F);
            EmitWaterPolys(fa);
            GL_TexEnv(GL_REPLACE);

            return;
        } else {
            GL_Bind(image.texnum);
            GL_TexEnv(GL_REPLACE);
        }

        
        
        if ((fa.texinfo.flags & Defines.SURF_FLOWING) != 0)
            DrawGLFlowingPoly(fa.polys);
        else
            DrawGLPoly(fa.polys);


        boolean gotoDynamic = false;
		/*
		** check for lightmap modification
		*/
        int maps;
        for (maps = 0; maps < Defines.MAXLIGHTMAPS && fa.styles[maps] != (byte) 255; maps++) {
            if (r_newrefdef.lightstyles[fa.styles[maps] & 0xFF].white != fa.cached_light[maps]) {
                gotoDynamic = true;
                break;
            }
        }

        
        if (maps == 4) maps--;


        boolean is_dynamic = false;
        if (gotoDynamic || (fa.dlightframe == r_framecount)) {
            
            if (gl_dynamic.value != 0) {
                if ((fa.texinfo.flags & (Defines.SURF_SKY | Defines.SURF_TRANS33 | Defines.SURF_TRANS66 | Defines.SURF_WARP)) == 0) {
                    is_dynamic = true;
                }
            }
        }

        if (is_dynamic) {
            if (((fa.styles[maps] & 0xFF) >= 32 || fa.styles[maps] == 0) && (fa.dlightframe != r_framecount)) {

                int smax = (fa.extents[0] >> 4) + 1;
                int tmax = (fa.extents[1] >> 4) + 1;

                R_BuildLightMap(fa, temp2, smax);
                R_SetCacheState(fa);

                GL_Bind(gl_state.lightmap_textures + fa.lightmaptexturenum);

                gl.glTexSubImage2D(GL_TEXTURE_2D, 0,
                        fa.light_s, fa.light_t,
                        smax, tmax,
                        GL_LIGHTMAP_FORMAT,
                        GL_UNSIGNED_BYTE, temp2);

                fa.lightmapchain = gl_lms.lightmap_surfaces[fa.lightmaptexturenum];
                gl_lms.lightmap_surfaces[fa.lightmaptexturenum] = fa;
            } else {
                fa.lightmapchain = gl_lms.lightmap_surfaces[0];
                gl_lms.lightmap_surfaces[0] = fa;
            }
        } else {
            fa.lightmapchain = gl_lms.lightmap_surfaces[fa.lightmaptexturenum];
            gl_lms.lightmap_surfaces[fa.lightmaptexturenum] = fa;
        }
    }


    /**
     * R_DrawAlphaSurfaces
     * Draw water surfaces and windows.
     * The BSP tree is waled front to back, so unwinding the chain
     * of alpha_surfaces will draw back to front, giving proper ordering.
     */
    @Override
    void R_DrawAlphaSurfaces() {
        r_world_matrix.clear();
        
        
        
        gl.glLoadMatrix(r_world_matrix);

        gl.glEnable(GL_BLEND);
        GL_TexEnv(GL_MODULATE);


        float intens = gl_state.inverse_intensity;

        gl.glInterleavedArrays(GL_T2F_V3F, glpoly_t.BYTE_STRIDE, globalPolygonInterleavedBuf);

        for (msurface_t s = r_alpha_surfaces; s != null; s = s.texturechain) {
            GL_Bind(s.texinfo.image.texnum);
            c_brush_polys++;
            if ((s.texinfo.flags & Defines.SURF_TRANS33) != 0)
                gl.glColor4f(intens, intens, intens, 0.33f);
            else if ((s.texinfo.flags & Defines.SURF_TRANS66) != 0)
                gl.glColor4f(intens, intens, intens, 0.66f);
            else
                gl.glColor4f(intens, intens, intens, 1);
            if ((s.flags & Defines.SURF_DRAWTURB) != 0)
                EmitWaterPolys(s);
            else if ((s.texinfo.flags & Defines.SURF_FLOWING) != 0)            
                DrawGLFlowingPoly(s.polys);                            
            else
                DrawGLPoly(s.polys);
        }

        GL_TexEnv(GL_REPLACE);
        gl.glColor4f(1, 1, 1, 1);
        gl.glDisable(GL_BLEND);

        r_alpha_surfaces = null;
    }

    /**
     * DrawTextureChains
     */
    void DrawTextureChains() {
        c_visible_textures = 0;

        msurface_t s;
        image_t image;
        int i;
        for (i = 0; i < numgltextures; i++) {
            image = gltextures[i];

            if (image.registration_sequence == 0)
                continue;
            if (image.texturechain == null)
                continue;
            c_visible_textures++;

            for (s = image.texturechain; s != null; s = s.texturechain) {
                if ((s.flags & Defines.SURF_DRAWTURB) == 0)
                    R_RenderBrushPoly(s);
            }
        }

        GL_EnableMultitexture(false);
        for (i = 0; i < numgltextures; i++) {
            image = gltextures[i];

            if (image.registration_sequence == 0)
                continue;
            s = image.texturechain;
            if (s == null)
                continue;

            for (; s != null; s = s.texturechain) {
                if ((s.flags & Defines.SURF_DRAWTURB) != 0)
                    R_RenderBrushPoly(s);
            }

            image.texturechain = null;
        }

        GL_TexEnv(GL_REPLACE);
    }

    
    private final IntBuffer temp = Lib.newIntBuffer(128 * 128, ByteOrder.LITTLE_ENDIAN);

    /**
     * GL_RenderLightmappedPoly
     *
     * @param surf
     */
    void GL_RenderLightmappedPoly(msurface_t surf) {


        boolean gotoDynamic = false;
        int map;
        byte[] styles = surf.styles;
        float[] cached_light = surf.cached_light;
        lightstyle_t[] lightstyles = r_newrefdef.lightstyles;
        for (map = 0; map < Defines.MAXLIGHTMAPS && (styles[map] != (byte) 255); map++) {
            if (lightstyles[styles[map] & 0xFF].white != cached_light[map]) {
                gotoDynamic = true;
                break;
            }
        }

        
        if (map == 4) map--;


        boolean is_dynamic = false;
        if (gotoDynamic || (surf.dlightframe == r_framecount)) {
            
            if (gl_dynamic.value != 0) {
                if ((surf.texinfo.flags & (Defines.SURF_SKY | Defines.SURF_TRANS33 | Defines.SURF_TRANS66 | Defines.SURF_WARP)) == 0) {
                    is_dynamic = true;
                }
            }
        }

        glpoly_t p;
        image_t image = R_TextureAnimation(surf.texinfo);
        int lmtex = surf.lightmaptexturenum;

        if (is_dynamic) {
            
            int smax, tmax;

            if (((styles[map] & 0xFF) >= 32 || styles[map] == 0) && (surf.dlightframe != r_framecount)) {
                smax = (surf.extents[0] >> 4) + 1;
                tmax = (surf.extents[1] >> 4) + 1;

                R_BuildLightMap(surf, temp, smax);
                R_SetCacheState(surf);

                GL_MBind(TEXTURE1, gl_state.lightmap_textures + surf.lightmaptexturenum);

                lmtex = surf.lightmaptexturenum;

                gl.glTexSubImage2D(GL_TEXTURE_2D, 0,
                        surf.light_s, surf.light_t,
                        smax, tmax,
                        GL_LIGHTMAP_FORMAT,
                        GL_UNSIGNED_BYTE, temp);

            } else {
                smax = (surf.extents[0] >> 4) + 1;
                tmax = (surf.extents[1] >> 4) + 1;

                R_BuildLightMap(surf, temp, smax);

                GL_MBind(TEXTURE1, gl_state.lightmap_textures + 0);

                lmtex = 0;

                gl.glTexSubImage2D(GL_TEXTURE_2D, 0,
                        surf.light_s, surf.light_t,
                        smax, tmax,
                        GL_LIGHTMAP_FORMAT,
                        GL_UNSIGNED_BYTE, temp);

            }

            c_brush_polys++;

            GL_MBind(TEXTURE0, image.texnum);
            GL_MBind(TEXTURE1, gl_state.lightmap_textures + lmtex);

            
            
            if ((surf.texinfo.flags & Defines.SURF_FLOWING) != 0) {

                float scroll = -64 * ((r_newrefdef.time / 40.0f) - (int) (r_newrefdef.time / 40.0f));
                if (scroll == 0.0f)
                    scroll = -64.0f;

                for (p = surf.polys; p != null; p = p.chain) {
                    p.beginScrolling(scroll);
                    gl.glDrawArrays(GL_POLYGON, p.pos, p.numverts);
                    p.endScrolling();
                }
            } else {
                for (p = surf.polys; p != null; p = p.chain) {
                    gl.glDrawArrays(GL_POLYGON, p.pos, p.numverts);
                }
            }
            
            
        } else {
            c_brush_polys++;

            GL_MBind(TEXTURE0, image.texnum);
            GL_MBind(TEXTURE1, gl_state.lightmap_textures + lmtex);

            
            
            if ((surf.texinfo.flags & Defines.SURF_FLOWING) != 0) {

                float scroll = -64 * ((r_newrefdef.time / 40.0f) - (int) (r_newrefdef.time / 40.0f));
                if (scroll == 0.0)
                    scroll = -64.0f;

                for (p = surf.polys; p != null; p = p.chain) {
                    p.beginScrolling(scroll);
                    gl.glDrawArrays(GL_POLYGON, p.pos, p.numverts);
                    p.endScrolling();
                }
            } else {
                
                
                for (p = surf.polys; p != null; p = p.chain) {
                    gl.glDrawArrays(GL_POLYGON, p.pos, p.numverts);
                }

                
                
            }
            
            
        }
    }

    /**
     * R_DrawInlineBModel
     */
    void R_DrawInlineBModel() {
        
        if (gl_flashblend.value == 0) {
            dlight_t[] dlights = r_newrefdef.dlights;
            mnode_t[] nodes = currentmodel.nodes;
            int num_dlights = r_newrefdef.num_dlights;
            int firstnode = currentmodel.firstnode;
            for (int k = 0; k < num_dlights; k++) {

                R_MarkLights(dlights[k], 1 << k, nodes[firstnode]);
            }
        }


        int psurfp = currentmodel.firstmodelsurface;
        msurface_t[] surfaces = currentmodel.surfaces;
        

        if ((currententity.flags & Defines.RF_TRANSLUCENT) != 0) {
            gl.glEnable(GL_BLEND);
            gl.glColor4f(1, 1, 1, 0.25f);
            GL_TexEnv(GL_MODULATE);
        }


        for (int i = 0; i < currentmodel.nummodelsurfaces; i++) {
            msurface_t psurf = surfaces[psurfp++];

            cplane_t pplane = psurf.plane;

            float dot = Math3D.DotProduct(modelorg, pplane.normal) - pplane.dist;


            if (((psurf.flags & Defines.SURF_PLANEBACK) != 0 && (dot < -BACKFACE_EPSILON)) ||
                    ((psurf.flags & Defines.SURF_PLANEBACK) == 0 && (dot > BACKFACE_EPSILON))) {
                if ((psurf.texinfo.flags & (Defines.SURF_TRANS33 | Defines.SURF_TRANS66)) != 0) {    
                    psurf.texturechain = r_alpha_surfaces;
                    r_alpha_surfaces = psurf;
                } else if ((psurf.flags & Defines.SURF_DRAWTURB) == 0) {
                    GL_RenderLightmappedPoly(psurf);
                } else {
                    GL_EnableMultitexture(false);
                    R_RenderBrushPoly(psurf);
                    GL_EnableMultitexture(true);
                }
            }
        }

        if ((currententity.flags & Defines.RF_TRANSLUCENT) != 0) {
            gl.glDisable(GL_BLEND);
            gl.glColor4f(1, 1, 1, 1);
            GL_TexEnv(GL_REPLACE);
        }
    }

    
    private final float[] mins = {0, 0, 0};
    private final float[] maxs = {0, 0, 0};
    private final float[] org = {0, 0, 0};
    private final float[] forward = {0, 0, 0};
    private final float[] right = {0, 0, 0};
    private final float[] up = {0, 0, 0};

    /**
     * R_DrawBrushModel
     */
    @Override
    void R_DrawBrushModel(entity_t e) {
        if (currentmodel.nummodelsurfaces == 0)
            return;

        currententity = e;
        gl_state.currenttextures[0] = gl_state.currenttextures[1] = -1;

        float[] ea = e.angles;
        boolean b = false;
        for (int v : new int[]{0, 1, 2}) {
            if (ea[v] != 0) {
                b = true;
                break;
            }
        }
        boolean rotated;
        if (b) {
            rotated = true;

            float[] eo = e.origin;
            for (int i = 0; i < 3; i++) {

                mins[i] = eo[i] - currentmodel.radius;
                maxs[i] = eo[i] + currentmodel.radius;
            }
        } else {
            rotated = false;
            Math3D.VectorAdd(e.origin, currentmodel.mins, mins);
            Math3D.VectorAdd(e.origin, currentmodel.maxs, maxs);
        }

        if (R_CullBox(mins, maxs)) return;

        gl.glColor3f(1, 1, 1);

        

        
        

        Math3D.VectorSubtract(r_newrefdef.vieworg, e.origin, modelorg);
        if (rotated) {
            Math3D.VectorCopy(modelorg, org);
            Math3D.AngleVectors(ea, forward, right, up);
            modelorg[0] = Math3D.DotProduct(org, forward);
            modelorg[1] = -Math3D.DotProduct(org, right);
            modelorg[2] = Math3D.DotProduct(org, up);
        }

        gl.glPushMatrix();

        ea[0] = -ea[0];    
        ea[2] = -ea[2];    
        R_RotateForEntity(e);
        ea[0] = -ea[0];    
        ea[2] = -ea[2];    

        GL_EnableMultitexture(true);
        GL_SelectTexture(TEXTURE0);
        GL_TexEnv(GL_REPLACE);
        gl.glInterleavedArrays(GL_T2F_V3F, glpoly_t.BYTE_STRIDE, globalPolygonInterleavedBuf);
        GL_SelectTexture(TEXTURE1);
        GL_TexEnv(GL_MODULATE);
        gl.glTexCoordPointer(2, glpoly_t.BYTE_STRIDE, globalPolygonTexCoord1Buf);
        gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        R_DrawInlineBModel();

        gl.glClientActiveTextureARB(TEXTURE1);
        gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);

        GL_EnableMultitexture(false);

        gl.glPopMatrix();
    }

	/*
	=============================================================

		WORLD MODEL

	=============================================================
	*/

    /**
     * R_RecursiveWorldNode
     */
    void R_RecursiveWorldNode(mnode_t node) {
        if (node.contents == Defines.CONTENTS_SOLID)
            return;        

        if (node.visframe != r_visframecount)
            return;

        if (R_CullBox(node.mins, node.maxs))
            return;

        int c;

        if (node.contents != -1) {
            mleaf_t pleaf = (mleaf_t) node;

            
            if (r_newrefdef.areabits != null) {
                if (((r_newrefdef.areabits[pleaf.area >> 3] & 0xFF) & (1 << (pleaf.area & 7))) == 0)
                    return;        
            }

            int markp = 0;

            msurface_t mark = pleaf.getMarkSurface(markp);
            c = pleaf.nummarksurfaces;

            if (c != 0) {
                do {
                    mark.visframe = r_framecount;
                    mark = pleaf.getMarkSurface(++markp); 
                } while (--c != 0);
            }

            return;
        }


        cplane_t plane = node.plane;
        float dot;
        float[] modelorg = this.modelorg;
        float planeDist = plane.dist;
        switch (plane.type) {
            case Defines.PLANE_X:
                dot = modelorg[0] - planeDist;
                break;
            case Defines.PLANE_Y:
                dot = modelorg[1] - planeDist;
                break;
            case Defines.PLANE_Z:
                dot = modelorg[2] - planeDist;
                break;
            default:
                dot = Math3D.DotProduct(modelorg, plane.normal) - planeDist;
                break;
        }

        int side, sidebit;
        if (dot >= 0.0f) {
            side = 0;
            sidebit = 0;
        } else {
            side = 1;
            sidebit = Defines.SURF_PLANEBACK;
        }

        
        R_RecursiveWorldNode(node.children[side]);


        msurface_t[] surfaces = r_worldmodel.surfaces;
        for (c = 0; c < node.numsurfaces; c++) {

            msurface_t surf = surfaces[node.firstsurface + c];
            if (surf.visframe != r_framecount)
                continue;

            if ((surf.flags & Defines.SURF_PLANEBACK) != sidebit)
                continue;        

            if ((surf.texinfo.flags & Defines.SURF_SKY) != 0) {    
                R_AddSkySurface(surf);
            } else if ((surf.texinfo.flags & (Defines.SURF_TRANS33 | Defines.SURF_TRANS66)) != 0) {    
                surf.texturechain = r_alpha_surfaces;
                r_alpha_surfaces = surf;
            } else {
                if ((surf.flags & Defines.SURF_DRAWTURB) == 0) {
                    GL_RenderLightmappedPoly(surf);
                } else {


                    image_t image = R_TextureAnimation(surf.texinfo);
                    surf.texturechain = image.texturechain;
                    image.texturechain = surf;
                }
            }
        }
        
        R_RecursiveWorldNode(node.children[1 - side]);
    }

    private final entity_t worldEntity = new entity_t();

    /**
     * R_DrawWorld
     */
    @Override
    void R_DrawWorld() {
        if (r_drawworld.value == 0)
            return;

        if ((r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) != 0)
            return;

        currentmodel = r_worldmodel;

        Math3D.VectorCopy(r_newrefdef.vieworg, modelorg);

        entity_t ent = worldEntity;
        
        ent.clear();
        ent.frame = (int) (r_newrefdef.time * 2);
        currententity = ent;

        gl_state.currenttextures[0] = gl_state.currenttextures[1] = -1;

        gl.glColor3f(1, 1, 1);
        
        
        

        R_ClearSkyBox();

        GL_EnableMultitexture(true);

        GL_SelectTexture(TEXTURE0);
        GL_TexEnv(GL_REPLACE);
        gl.glInterleavedArrays(GL_T2F_V3F, glpoly_t.BYTE_STRIDE, globalPolygonInterleavedBuf);
        GL_SelectTexture(TEXTURE1);
        gl.glTexCoordPointer(2, glpoly_t.BYTE_STRIDE, globalPolygonTexCoord1Buf);
        gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        GL_TexEnv(gl_lightmap.value != 0 ? GL_REPLACE : GL_MODULATE);

        R_RecursiveWorldNode(r_worldmodel.nodes[0]); 

        gl.glClientActiveTextureARB(TEXTURE1);
        gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);

        GL_EnableMultitexture(false);

        DrawTextureChains();
        R_DrawSkyBox();
        R_DrawTriangleOutlines();
    }

    final byte[] fatvis = new byte[Defines.MAX_MAP_LEAFS / 8];

    /**
     * R_MarkLeaves
     * Mark the leaves and nodes that are in the PVS for the current
     * cluster
     */
    @Override
    void R_MarkLeaves() {
        if (r_oldviewcluster == r_viewcluster && r_oldviewcluster2 == r_viewcluster2 && r_novis.value == 0 && r_viewcluster != -1)
            return;

        
        
        if (gl_lockpvs.value != 0)
            return;

        r_visframecount++;
        r_oldviewcluster = r_viewcluster;
        r_oldviewcluster2 = r_viewcluster2;

        int i;
        if (r_novis.value != 0 || r_viewcluster == -1 || r_worldmodel.vis == null) {
            
            for (i = 0; i < r_worldmodel.numleafs; i++)
                r_worldmodel.leafs[i].visframe = r_visframecount;
            for (i = 0; i < r_worldmodel.numnodes; i++)
                r_worldmodel.nodes[i].visframe = r_visframecount;
            return;
        }

        byte[] vis = Mod_ClusterPVS(r_viewcluster, r_worldmodel);

        if (r_viewcluster2 != r_viewcluster) {
            
            System.arraycopy(vis, 0, fatvis, 0, (r_worldmodel.numleafs + 7) >> 3);
            vis = Mod_ClusterPVS(r_viewcluster2, r_worldmodel);
            int c = (r_worldmodel.numleafs + 31) >> 5;
            c <<= 2;
            for (int k = 0; k < c; k += 4) {
                fatvis[k] |= vis[k];
                fatvis[k + 1] |= vis[k + 1];
                fatvis[k + 2] |= vis[k + 2];
                fatvis[k + 3] |= vis[k + 3];
            }

            vis = fatvis;
        }

        for (i = 0; i < r_worldmodel.numleafs; i++) {
            mleaf_t leaf = r_worldmodel.leafs[i];
            int cluster = leaf.cluster;
            if (cluster == -1)
                continue;
            if (((vis[cluster >> 3] & 0xFF) & (1 << (cluster & 7))) != 0) {
                mnode_t node = leaf;
                do {
                    if (node.visframe == r_visframecount)
                        break;
                    node.visframe = r_visframecount;
                    node = node.parent;
                } while (node != null);
            }
        }
    }

	/*
	=============================================================================

	  LIGHTMAP ALLOCATION

	=============================================================================
	*/

    /**
     * LM_InitBlock
     */
    void LM_InitBlock() {
        Arrays.fill(gl_lms.allocated, 0);
    }

    /**
     * LM_UploadBlock
     *
     * @param dynamic
     */
    void LM_UploadBlock(boolean dynamic) {
        int texture = (dynamic) ? 0 : gl_lms.current_lightmap_texture;

        GL_Bind(gl_state.lightmap_textures + texture);
        gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        gl_lms.lightmap_buffer.rewind();
        if (dynamic) {
            boolean seen = false;
            int best = 0;
            int[] array = gl_lms.allocated;
            for (int j = 0; j < BLOCK_WIDTH; j++) {
                int i = array[j];
                if (i >= 0) {
                    if (!seen || i > best) {
                        seen = true;
                        best = i;
                    }
                }
            }
            int height = seen ? best : 0;

            gl.glTexSubImage2D(GL_TEXTURE_2D,
                    0,
                    0, 0,
                    BLOCK_WIDTH, height,
                    GL_LIGHTMAP_FORMAT,
                    GL_UNSIGNED_BYTE,
                    gl_lms.lightmap_buffer);
        } else {
            gl.glTexImage2D(GL_TEXTURE_2D,
                    0,
                    gl_lms.internal_format,
                    BLOCK_WIDTH, BLOCK_HEIGHT,
                    0,
                    GL_LIGHTMAP_FORMAT,
                    GL_UNSIGNED_BYTE,
                    gl_lms.lightmap_buffer);
            if (++gl_lms.current_lightmap_texture == MAX_LIGHTMAPS)
                Com.Error(Defines.ERR_DROP, "LM_UploadBlock() - MAX_LIGHTMAPS exceeded\n");

            
        }
    }

    /**
     * LM_AllocBlock
     *
     * @param w
     * @param h
     * @param pos
     * @return a texture number and the position inside it
     */
    boolean LM_AllocBlock(int w, int h, pos_t pos) {
        int best = BLOCK_HEIGHT;
        int x = pos.x;
        int i;
        for (i = 0; i < BLOCK_WIDTH - w; i++) {
            int best2 = 0;

            int j;
            for (j = 0; j < w; j++) {
                if (gl_lms.allocated[i + j] >= best)
                    break;
                if (gl_lms.allocated[i + j] > best2)
                    best2 = gl_lms.allocated[i + j];
            }
            if (j == w) {    
                pos.x = x = i;
                pos.y = best = best2;
            }
        }

        if (best + h > BLOCK_HEIGHT)
            return false;

        for (i = 0; i < w; i++)
            gl_lms.allocated[x + i] = best + h;

        return true;
    }

    /**
     * GL_BuildPolygonFromSurface
     */
    void GL_BuildPolygonFromSurface(msurface_t fa) {

        medge_t[] pedges = currentmodel.edges;
        int lnumverts = fa.numedges;


        glpoly_t poly = Polygon.create(lnumverts);

        poly.next = fa.polys;
        poly.flags = fa.flags;
        fa.polys = poly;

        for (int i = 0; i < lnumverts; i++) {
            int lindex = currentmodel.surfedges[fa.firstedge + i];

            medge_t r_pedge;
            float[] vec;
            if (lindex > 0) {
                r_pedge = pedges[lindex];
                vec = currentmodel.vertexes[r_pedge.v[0]].position;
            } else {
                r_pedge = pedges[-lindex];
                vec = currentmodel.vertexes[r_pedge.v[1]].position;
            }
            float s = Math3D.DotProduct(vec, fa.texinfo.vecs[0]) + fa.texinfo.vecs[0][3];
            s /= fa.texinfo.image.width;

            float t = Math3D.DotProduct(vec, fa.texinfo.vecs[1]) + fa.texinfo.vecs[1][3];
            t /= fa.texinfo.image.height;

            poly.x(i, vec[0]);
            poly.y(i, vec[1]);
            poly.z(i, vec[2]);

            poly.s1(i, s);
            poly.t1(i, t);

            
            
            
            s = Math3D.DotProduct(vec, fa.texinfo.vecs[0]) + fa.texinfo.vecs[0][3];
            s -= fa.texturemins[0];
            s += fa.light_s * 16;
            s += 8;
            s /= BLOCK_WIDTH * 16; 

            t = Math3D.DotProduct(vec, fa.texinfo.vecs[1]) + fa.texinfo.vecs[1][3];
            t -= fa.texturemins[1];
            t += fa.light_t * 16;
            t += 8;
            t /= BLOCK_HEIGHT * 16; 

            poly.s2(i, s);
            poly.t2(i, t);
        }
    }

    /**
     * GL_CreateSurfaceLightmap
     */
    void GL_CreateSurfaceLightmap(msurface_t surf) {
        if ((surf.flags & (Defines.SURF_DRAWSKY | Defines.SURF_DRAWTURB)) != 0)
            return;

        int smax = (surf.extents[0] >> 4) + 1;
        int tmax = (surf.extents[1] >> 4) + 1;

        pos_t lightPos = new pos_t(surf.light_s, surf.light_t);

        if (!LM_AllocBlock(smax, tmax, lightPos)) {
            LM_UploadBlock(false);
            LM_InitBlock();
            lightPos = new pos_t(surf.light_s, surf.light_t);
            if (!LM_AllocBlock(smax, tmax, lightPos)) {
                Com.Error(Defines.ERR_FATAL, "Consecutive calls to LM_AllocBlock(" + smax + ',' + tmax + ") failed\n");
            }
        }

        
        surf.light_s = lightPos.x;
        surf.light_t = lightPos.y;

        surf.lightmaptexturenum = gl_lms.current_lightmap_texture;

        IntBuffer base = gl_lms.lightmap_buffer;
        base.position(surf.light_t * BLOCK_WIDTH + surf.light_s);

        R_SetCacheState(surf);
        R_BuildLightMap(surf, base.slice(), BLOCK_WIDTH);
    }

    lightstyle_t[] lightstyles;
    private final IntBuffer dummy = Lib.newIntBuffer(128 * 128);

    /**
     * GL_BeginBuildingLightmaps
     */
    void GL_BeginBuildingLightmaps(model_t m) {
        
        int i;

        
        if (lightstyles == null) {
            lightstyles = new lightstyle_t[Defines.MAX_LIGHTSTYLES];
            for (i = 0; i < lightstyles.length; i++) {
                lightstyles[i] = new lightstyle_t();
            }
        }

        
        Arrays.fill(gl_lms.allocated, 0);

        r_framecount = 1;        

        GL_EnableMultitexture(true);
        GL_SelectTexture(TEXTURE1);

		/*
		** setup the base lightstyles so the lightmaps won't have to be regenerated
		** the first time they're seen
		*/
        for (i = 0; i < Defines.MAX_LIGHTSTYLES; i++) {
            lightstyles[i].rgb[0] = 1;
            lightstyles[i].rgb[1] = 1;
            lightstyles[i].rgb[2] = 1;
            lightstyles[i].white = 3;
        }
        r_newrefdef.lightstyles = lightstyles;

        if (gl_state.lightmap_textures == 0) {
            gl_state.lightmap_textures = TEXNUM_LIGHTMAPS;
        }

        gl_lms.current_lightmap_texture = 1;

		/*
		** if mono lightmaps are enabled and we want to use alpha
		** blending (a,1-a) then we're likely running on a 3DLabs
		** Permedia2.  In a perfect world we'd use a GL_ALPHA lightmap
		** in order to conserve space and maximize bandwidth, however 
		** this isn't a perfect world.
		**
		** So we have to use alpha lightmaps, but stored in GL_RGBA format,
		** which means we only get 1/16th the color resolution we should when
		** using alpha lightmaps.  If we find another board that supports
		** only alpha lightmaps but that can at least support the GL_ALPHA
		** format then we should change this code to use real alpha maps.
		*/

        char format = gl_monolightmap.string.toUpperCase().charAt(0);

        switch (format) {
            case 'A':
                gl_lms.internal_format = gl_tex_alpha_format;
                break;
            /*
             ** try to do hacked colored lighting with a blended texture
             */
            case 'C':
                gl_lms.internal_format = gl_tex_alpha_format;
                break;
            case 'I':
                gl_lms.internal_format = GL_INTENSITY8;
                break;
            case 'L':
                gl_lms.internal_format = GL_LUMINANCE8;
                break;
            default:
                gl_lms.internal_format = gl_tex_solid_format;
                break;
        }

		/*
		** initialize the dynamic lightmap texture
		*/
        GL_Bind(gl_state.lightmap_textures + 0);
        gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexImage2D(GL_TEXTURE_2D,
                0,
                gl_lms.internal_format,
                BLOCK_WIDTH, BLOCK_HEIGHT,
                0,
                GL_LIGHTMAP_FORMAT,
                GL_UNSIGNED_BYTE,
                dummy);
    }

    /**
     * GL_EndBuildingLightmaps
     */
    void GL_EndBuildingLightmaps() {
        LM_UploadBlock(false);
        GL_EnableMultitexture(false);
    }

    /*
     * new buffers for vertex array handling
     */
    static final FloatBuffer globalPolygonInterleavedBuf = Polygon.getInterleavedBuffer();
    static final FloatBuffer globalPolygonTexCoord1Buf;

    static {
        globalPolygonInterleavedBuf.position(glpoly_t.STRIDE - 2);
        globalPolygonTexCoord1Buf = globalPolygonInterleavedBuf.slice();
        globalPolygonInterleavedBuf.position(0);
    }

    





















}
