/*
 * Light.java
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

import jake2.Defines;
import jake2.Globals;
import jake2.client.dlight_t;
import jake2.game.cplane_t;
import jake2.qcommon.Com;
import jake2.qcommon.longjmpException;
import jake2.render.mnode_t;
import jake2.render.msurface_t;
import jake2.render.mtexinfo_t;
import jake2.util.Math3D;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Light
 * 
 * @author cwei
 */
public abstract class Light extends Warp {
    

    int r_dlightframecount;

    static final int DLIGHT_CUTOFF = 64;

    /*
     * =============================================================================
     * 
     * DYNAMIC LIGHTS BLEND RENDERING
     * 
     * =============================================================================
     */

    void R_RenderDlight(dlight_t light) {
        float[] v = { 0, 0, 0 };

        var rad = light.intensity * 0.35f;

        Math3D.VectorSubtract(light.origin, r_origin, v);

        gl.glBegin(GL_TRIANGLE_FAN);
        gl.glColor3f(light.color[0] * 0.2f, light.color[1] * 0.2f,
                light.color[2] * 0.2f);
        int i;
        for (i = 0; i < 3; i++)
            v[i] = light.origin[i] - vpn[i] * rad;
        gl.glVertex3f(v[0], v[1], v[2]);
        gl.glColor3f(0, 0, 0);
        for (i = 16; i >= 0; i--) {
            var a = (float) (i / 16.0f * Math.PI * 2);
            for (var j = 0; j < 3; j++)
                v[j] = (float) (light.origin[j] + vright[j] * Math.cos(a) * rad + vup[j]
                        * Math.sin(a) * rad);
            gl.glVertex3f(v[0], v[1], v[2]);
        }
        gl.glEnd();
    }

    /*
     * ============= R_RenderDlights =============
     */
    @Override
    void R_RenderDlights() {

        if (gl_flashblend.value == 0)
            return;

        r_dlightframecount = r_framecount + 1; 
        
        gl.glDepthMask(false);
        gl.glDisable(GL_TEXTURE_2D);
        gl.glShadeModel(GL_SMOOTH);
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_ONE, GL_ONE);

        for (var i = 0; i < r_newrefdef.num_dlights; i++) {
            var l = r_newrefdef.dlights[i];
            R_RenderDlight(l);
        }

        gl.glColor3f(1, 1, 1);
        gl.glDisable(GL_BLEND);
        gl.glEnable(GL_TEXTURE_2D);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthMask(true);
    }

    /*
     * =============================================================================
     * 
     * DYNAMIC LIGHTS
     * 
     * =============================================================================
     */

    /*
     * ============= R_MarkLights =============
     */
    @Override
    void R_MarkLights(dlight_t light, int bit, mnode_t node) {

        if (node.contents != -1)
            return;

        var splitplane = node.plane;
        var dist = Math3D.DotProduct(light.origin, splitplane.normal)
                - splitplane.dist;

        if (dist > light.intensity - DLIGHT_CUTOFF) {
            R_MarkLights(light, bit, node.children[0]);
            return;
        }
        if (dist < -light.intensity + DLIGHT_CUTOFF) {
            R_MarkLights(light, bit, node.children[1]);
            return;
        }

        
        for (var i = 0; i < node.numsurfaces; i++) {

            var surf = r_worldmodel.surfaces[node.firstsurface + i];

            /*
             * cwei bugfix for dlight behind the walls
             */
            dist = Math3D.DotProduct(light.origin, surf.plane.normal)
                    - surf.plane.dist;
            var sidebit = (dist >= 0) ? 0 : Defines.SURF_PLANEBACK;
            if ((surf.flags & Defines.SURF_PLANEBACK) != sidebit)
                continue;
            /*
             * cwei bugfix end
             */

            if (surf.dlightframe != r_dlightframecount) {
                surf.dlightbits = 0;
                surf.dlightframe = r_dlightframecount;
            }
            surf.dlightbits |= bit;
        }

        R_MarkLights(light, bit, node.children[0]);
        R_MarkLights(light, bit, node.children[1]);
    }

    /*
     * ============= R_PushDlights =============
     */
    @Override
    void R_PushDlights() {

        if (gl_flashblend.value != 0)
            return;

        r_dlightframecount = r_framecount + 1; 
        
        for (var i = 0; i < r_newrefdef.num_dlights; i++) {
            var l = r_newrefdef.dlights[i];
            R_MarkLights(l, 1 << i, r_worldmodel.nodes[0]);
        }
    }

    /*
     * =============================================================================
     * 
     * LIGHT SAMPLING
     * 
     * =============================================================================
     */

    final float[] pointcolor = { 0, 0, 0 }; 

    cplane_t lightplane; 

    final float[] lightspot = { 0, 0, 0 }; 

    int RecursiveLightPoint(mnode_t node, float[] start, float[] end) {
        while (true) {
            if (node.contents != -1)
                return -1;


            var plane = node.plane;
            var front = Math3D.DotProduct(start, plane.normal) - plane.dist;
            var back = Math3D.DotProduct(end, plane.normal) - plane.dist;
            var side = (front < 0);
            var sideIndex = (side) ? 1 : 0;

            if ((back < 0) == side) {
                node = node.children[sideIndex];
                continue;
            }

            var frac = front / (front - back);
            float[] mid = {0, 0, 0};
            mid[0] = start[0] + (end[0] - start[0]) * frac;
            mid[1] = start[1] + (end[1] - start[1]) * frac;
            mid[2] = start[2] + (end[2] - start[2]) * frac;


            var r = RecursiveLightPoint(node.children[sideIndex], start, mid);
            if (r >= 0)
                return r; 

            if ((back < 0) == side)
                return -1; 

            
            Math3D.VectorCopy(mid, lightspot);
            lightplane = plane;

            var surfIndex = node.firstsurface;
            float[] scale = {0, 0, 0};
            for (var i = 0; i < node.numsurfaces; i++, surfIndex++) {
                var surf = r_worldmodel.surfaces[surfIndex];

                if ((surf.flags & (Defines.SURF_DRAWTURB | Defines.SURF_DRAWSKY)) != 0)
                    continue;

                var tex = surf.texinfo;

                var s = (int) (Math3D.DotProduct(mid, tex.vecs[0]) + tex.vecs[0][3]);
                var t = (int) (Math3D.DotProduct(mid, tex.vecs[1]) + tex.vecs[1][3]);

                if (s < surf.texturemins[0] || t < surf.texturemins[1])
                    continue;

                var ds = s - surf.texturemins[0];
                var dt = t - surf.texturemins[1];

                if (ds > surf.extents[0] || dt > surf.extents[1])
                    continue;

                if (surf.samples == null)
                    return 0;

                ds >>= 4;
                dt >>= 4;

                var lightmap = surf.samples;

                Math3D.VectorCopy(Globals.vec3_origin, pointcolor);
                if (lightmap != null) {

                    var lightmapIndex = 0;
                    lightmapIndex += 3 * (dt * ((surf.extents[0] >> 4) + 1) + ds);

                    for (var maps = 0; maps < Defines.MAXLIGHTMAPS
                            && surf.styles[maps] != (byte) 255; maps++) {
                        var rgb = r_newrefdef.lightstyles[surf.styles[maps] & 0xFF].rgb;
                        scale[0] = gl_modulate.value * rgb[0];
                        scale[1] = gl_modulate.value * rgb[1];
                        scale[2] = gl_modulate.value * rgb[2];

                        pointcolor[0] += (lightmap.get(lightmapIndex + 0) & 0xFF)
                                * scale[0] * (1.0f / 255);
                        pointcolor[1] += (lightmap.get(lightmapIndex + 1) & 0xFF)
                                * scale[1] * (1.0f / 255);
                        pointcolor[2] += (lightmap.get(lightmapIndex + 2) & 0xFF)
                                * scale[2] * (1.0f / 255);
                        lightmapIndex += 3 * ((surf.extents[0] >> 4) + 1)
                                * ((surf.extents[1] >> 4) + 1);
                    }
                }
                return 1;
            }

            
            node = node.children[1 - sideIndex];
            start = mid;
        }
    }

    /*
     * =============== R_LightPoint ===============
     */
    @Override
    void R_LightPoint(float[] p, float[] color) {
        assert (p.length == 3) : "vec3_t bug";
        assert (color.length == 3) : "rgb bug";

        if (r_worldmodel.lightdata == null) {
            color[0] = color[1] = color[2] = 1.0f;
            return;
        }

        float[] end = {0, 0, 0};
        end[0] = p[0];
        end[1] = p[1];
        end[2] = p[2] - 2048;

        float r = RecursiveLightPoint(r_worldmodel.nodes[0], p, end);

        if (r == -1) {
            Math3D.VectorCopy(Globals.vec3_origin, color);
        } else {
            Math3D.VectorCopy(pointcolor, color);
        }

        
        
        
        for (var lnum = 0; lnum < r_newrefdef.num_dlights; lnum++) {
            var dl = r_newrefdef.dlights[lnum];

            Math3D.VectorSubtract(currententity.origin, dl.origin, end);
            var add = dl.intensity - Math3D.VectorLength(end);
            add *= (1.0f / 256);
            if (add > 0) {
                Math3D.VectorMA(color, add, dl.color, color);
            }
        }

        Math3D.VectorScale(color, gl_modulate.value, color);
    }

    

    final float[] s_blocklights = new float[34 * 34 * 3];

    /*
     * =============== R_AddDynamicLights ===============
     */
    void R_AddDynamicLights(msurface_t surf) {
        float[] impact = { 0, 0, 0 };
        float[] local = { 0, 0, 0 };

        var smax = (surf.extents[0] >> 4) + 1;
        var tmax = (surf.extents[1] >> 4) + 1;
        var tex = surf.texinfo;

        for (var lnum = 0; lnum < r_newrefdef.num_dlights; lnum++) {
            if ((surf.dlightbits & (1 << lnum)) == 0)
                continue;

            var dl = r_newrefdef.dlights[lnum];
            var frad = dl.intensity;
            var fdist = Math3D.DotProduct(dl.origin, surf.plane.normal)
                    - surf.plane.dist;
            frad -= Math.abs(fdist);


            float fminlight = DLIGHT_CUTOFF;
            if (frad < fminlight)
                continue;
            fminlight = frad - fminlight;

            for (var i = 0; i < 3; i++) {
                impact[i] = dl.origin[i] - surf.plane.normal[i] * fdist;
            }

            local[0] = Math3D.DotProduct(impact, tex.vecs[0]) + tex.vecs[0][3]
                    - surf.texturemins[0];
            local[1] = Math3D.DotProduct(impact, tex.vecs[1]) + tex.vecs[1][3]
                    - surf.texturemins[1];

            var pfBL = s_blocklights;
            var pfBLindex = 0;
            float ftacc;
            int t;
            for (t = 0, ftacc = 0; t < tmax; t++, ftacc += 16) {
                var td = (int) (local[1] - ftacc);
                if (td < 0)
                    td = -td;

                float fsacc;
                int s;
                for (s = 0, fsacc = 0; s < smax; s++, fsacc += 16, pfBLindex += 3) {
                    var sd = (int) (local[0] - fsacc);

                    if (sd < 0)
                        sd = -sd;

                    if (sd > td)
                        fdist = sd + (td >> 1);
                    else
                        fdist = td + (sd >> 1);

                    if (fdist < fminlight) {
                        pfBL[pfBLindex + 0] += (frad - fdist) * dl.color[0];
                        pfBL[pfBLindex + 1] += (frad - fdist) * dl.color[1];
                        pfBL[pfBLindex + 2] += (frad - fdist) * dl.color[2];
                    }
                }
            }
        }
    }

    /*
     * * R_SetCacheState
     */
    @Override
    void R_SetCacheState(msurface_t surf) {

        for (var maps = 0; maps < Defines.MAXLIGHTMAPS
                && surf.styles[maps] != (byte) 255; maps++) {
            surf.cached_light[maps] = r_newrefdef.lightstyles[surf.styles[maps] & 0xFF].white;
        }
    }

    /*
     * =============== R_BuildLightMap
     * 
     * Combine and scale multiple lightmaps into the floating format in
     * blocklights ===============
     */
    @Override
    void R_BuildLightMap(msurface_t surf, IntBuffer dest, int stride) {

        if ((surf.texinfo.flags & (Defines.SURF_SKY | Defines.SURF_TRANS33
                | Defines.SURF_TRANS66 | Defines.SURF_WARP)) != 0)
            Com.Error(Defines.ERR_DROP,
                    "R_BuildLightMap called for non-lit surface");

        var smax = (surf.extents[0] >> 4) + 1;
        var tmax = (surf.extents[1] >> 4) + 1;
        var size = smax * tmax;
        if (size > ((s_blocklights.length * Defines.SIZE_OF_FLOAT) >> 4))
            Com.Error(Defines.ERR_DROP, "Bad s_blocklights size");

        float[] bl;
        int i;
        try {
            
            if (surf.samples == null) {

                for (i = 0; i < size * 3; i++)
                    s_blocklights[i] = 255;

                for (var maps = 0; maps < Defines.MAXLIGHTMAPS
                        && surf.styles[maps] != (byte) 255; maps++) {
                }
                
                throw new longjmpException();
            }


            int nummaps;
            for (nummaps = 0; nummaps < Defines.MAXLIGHTMAPS
                    && surf.styles[nummaps] != (byte) 255; nummaps++)
                ;

            var lightmap = surf.samples;
            var lightmapIndex = 0;


            float[] scale = {0, 0, 0, 0};
            if (nummaps == 1) {

                for (var maps = 0; maps < Defines.MAXLIGHTMAPS
                        && surf.styles[maps] != (byte) 255; maps++) {
                    bl = s_blocklights;

                    for (i = 0; i < 3; i++)
                        scale[i] = gl_modulate.value
                                * r_newrefdef.lightstyles[surf.styles[maps] & 0xFF].rgb[i];

                    var result = IntStream.of(0, 1, 2).noneMatch(v -> scale[v] != 1.0F);
                    var blp = 0;
                    if (result) {
                        for (i = 0; i < size; i++) {
                            bl[blp++] = lightmap.get(lightmapIndex++) & 0xFF;
                            bl[blp++] = lightmap.get(lightmapIndex++) & 0xFF;
                            bl[blp++] = lightmap.get(lightmapIndex++) & 0xFF;
                        }
                    } else {
                        for (i = 0; i < size; i++) {
                            bl[blp++] = (lightmap.get(lightmapIndex++) & 0xFF)
                                    * scale[0];
                            bl[blp++] = (lightmap.get(lightmapIndex++) & 0xFF)
                                    * scale[1];
                            bl[blp++] = (lightmap.get(lightmapIndex++) & 0xFF)
                                    * scale[2];
                        }
                    }
                    
                }
            } else {


                Arrays.fill(s_blocklights, 0, size * 3, 0.0f);

                for (var maps = 0; maps < Defines.MAXLIGHTMAPS
                        && surf.styles[maps] != (byte) 255; maps++) {
                    bl = s_blocklights;

                    for (i = 0; i < 3; i++)
                        scale[i] = gl_modulate.value
                                * r_newrefdef.lightstyles[surf.styles[maps] & 0xFF].rgb[i];

                    var result = IntStream.of(0, 1, 2).noneMatch(v -> scale[v] != 1.0F);
                    var blp = 0;
                    if (result) {
                        for (i = 0; i < size; i++) {
                            bl[blp++] += lightmap.get(lightmapIndex++) & 0xFF;
                            bl[blp++] += lightmap.get(lightmapIndex++) & 0xFF;
                            bl[blp++] += lightmap.get(lightmapIndex++) & 0xFF;
                        }
                    } else {
                        for (i = 0; i < size; i++) {
                            bl[blp++] += (lightmap.get(lightmapIndex++) & 0xFF)
                                    * scale[0];
                            bl[blp++] += (lightmap.get(lightmapIndex++) & 0xFF)
                                    * scale[1];
                            bl[blp++] += (lightmap.get(lightmapIndex++) & 0xFF)
                                    * scale[2];
                        }
                    }
                    
                }
            }

            
            if (surf.dlightframe == r_framecount)
                R_AddDynamicLights(surf);

            
        } catch (longjmpException store) {
        }

        
        stride -= smax;
        bl = s_blocklights;
        var blp = 0;

        int monolightmap = gl_monolightmap.string.charAt(0);

        var destp = 0;

        int j;
        int max;
        int a;
        int b;
        int g;
        int r;
        if (monolightmap == '0') {
            for (i = 0; i < tmax; i++, destp += stride) {
                for (j = 0; j < smax; j++) {

                    r = (int) bl[blp++];
                    g = (int) bl[blp++];
                    b = (int) bl[blp++];

                    
                    if (r < 0)
                        r = 0;
                    if (g < 0)
                        g = 0;
                    if (b < 0)
                        b = 0;

                    /*
                     * * determine the brightest of the three color components
                     */
                    if (r > g)
                        max = r;
                    else
                        max = g;
                    if (b > max)
                        max = b;

                    /*
                     * * alpha is ONLY used for the mono lightmap case. For this
                     * reason * we set it to the brightest of the color
                     * components so that * things don't get too dim.
                     */
                    a = max;

                    /*
                     * * rescale all the color components if the intensity of
                     * the greatest * channel exceeds 1.0
                     */
                    if (max > 255) {
                        var t = 255.0F / max;

                        r *= t;
                        g *= t;
                        b *= t;
                        a *= t;
                    }
                    r &= 0xFF;
                    g &= 0xFF;
                    b &= 0xFF;
                    a &= 0xFF;
                    dest.put(destp++, (a << 24) | (b << 16) | (g << 8)
                            | (r << 0));
                }
            }
        } else {
            for (i = 0; i < tmax; i++, destp += stride) {
                for (j = 0; j < smax; j++) {

                    r = (int) bl[blp++];
                    g = (int) bl[blp++];
                    b = (int) bl[blp++];

                    
                    if (r < 0)
                        r = 0;
                    if (g < 0)
                        g = 0;
                    if (b < 0)
                        b = 0;

                    /*
                     * * determine the brightest of the three color components
                     */
                    if (r > g)
                        max = r;
                    else
                        max = g;
                    if (b > max)
                        max = b;

                    /*
                     * * alpha is ONLY used for the mono lightmap case. For this
                     * reason * we set it to the brightest of the color
                     * components so that * things don't get too dim.
                     */
                    a = max;

                    /*
                     * * rescale all the color components if the intensity of
                     * the greatest * channel exceeds 1.0
                     */
                    if (max > 255) {
                        var t = 255.0F / max;

                        r *= t;
                        g *= t;
                        b *= t;
                        a *= t;
                    }

                    /*
                     * * So if we are doing alpha lightmaps we need to set the
                     * R, G, and B * components to 0 and we need to set alpha to
                     * 1-alpha.
                     */
                    switch (monolightmap) {
                    case 'L':
                    case 'I':
                        r = a;
                        g = b = 0;
                        break;
                    case 'C':
                        
                        a = 255 - ((r + g + b) / 3);
                        r *= a / 255.0f;
                        g *= a / 255.0f;
                        b *= a / 255.0f;
                        break;
                    case 'A':
                    default:
                        r = g = b = 0;
                        a = 255 - a;
                        break;
                    }
                    r &= 0xFF;
                    g &= 0xFF;
                    b &= 0xFF;
                    a &= 0xFF;
                    dest.put(destp++, (a << 24) | (b << 16) | (g << 8)
                            | (r << 0));
                }
            }
        }
    }

}