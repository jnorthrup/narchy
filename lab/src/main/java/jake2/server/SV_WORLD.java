/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *  
 */


package jake2.server;

import jake2.Defines;
import jake2.Globals;
import jake2.game.*;
import jake2.qcommon.CM;
import jake2.qcommon.Com;
import jake2.util.Math3D;

import java.util.stream.IntStream;

public class SV_WORLD {
    
    
    
    
    
    
    
    
    
    public static final areanode_t[] sv_areanodes = new areanode_t[Defines.AREA_NODES];
    static {
        SV_WORLD.initNodes();
    }

    public static int sv_numareanodes;

    public static float[] area_mins;
    public static float[] area_maxs;

    public static edict_t[] area_list;

    public static int area_count;
    public static int area_maxcount;

    public static int area_type;

    public static final int MAX_TOTAL_ENT_LEAFS = 128;

    static final int[] leafs = new int[MAX_TOTAL_ENT_LEAFS];

    static final int[] clusters = new int[MAX_TOTAL_ENT_LEAFS];

    
    static final edict_t[] touch = new edict_t[Defines.MAX_EDICTS];

    
    static final edict_t[] touchlist = new edict_t[Defines.MAX_EDICTS];

    public static void initNodes() {
        for (int n = 0; n < Defines.AREA_NODES; n++)
            SV_WORLD.sv_areanodes[n] = new areanode_t();
    }

    
    public static void ClearLink(link_t l) {
        l.prev = l.next = l;
    }

    public static void RemoveLink(link_t l) {
        l.next.prev = l.prev;
        l.prev.next = l.next;
    }

    public static void InsertLinkBefore(link_t l, link_t before) {
        l.next = before;
        l.prev = before.prev;
        l.prev.next = l;
        l.next.prev = l;
    }

    /*
     * =============== SV_CreateAreaNode
     * 
     * Builds a uniformly subdivided tree for the given world size
     * ===============
     */
    public static areanode_t SV_CreateAreaNode(int depth, float[] mins,
            float[] maxs) {
        areanode_t anode = SV_WORLD.sv_areanodes[SV_WORLD.sv_numareanodes];
        


        SV_WORLD.sv_numareanodes++;
        ClearLink(anode.trigger_edicts);
        ClearLink(anode.solid_edicts);
        if (depth == Defines.AREA_DEPTH) {
            anode.axis = -1;
            anode.children[0] = anode.children[1] = null;
            return anode;
        }
        float[] size = {0, 0, 0};
        Math3D.VectorSubtract(maxs, mins, size);
        if (size[0] > size[1])
            anode.axis = 0;
        else
            anode.axis = 1;
        anode.dist = 0.5f * (maxs[anode.axis] + mins[anode.axis]);
        float[] mins1 = {0, 0, 0};
        Math3D.VectorCopy(mins, mins1);
        float[] mins2 = {0, 0, 0};
        Math3D.VectorCopy(mins, mins2);
        float[] maxs1 = {0, 0, 0};
        Math3D.VectorCopy(maxs, maxs1);
        float[] maxs2 = {
                0, 0, 0};
        Math3D.VectorCopy(maxs, maxs2);
        maxs1[anode.axis] = mins2[anode.axis] = anode.dist;
        anode.children[0] = SV_CreateAreaNode(depth + 1, mins2, maxs2);
        anode.children[1] = SV_CreateAreaNode(depth + 1, mins1, maxs1);
        return anode;
    }

    /*
     * =============== SV_ClearWorld
     * 
     * ===============
     */
    public static void SV_ClearWorld() {
        initNodes();
        SV_WORLD.sv_numareanodes = 0;
        SV_CreateAreaNode(0, SV_INIT.sv.models[1].mins,
                SV_INIT.sv.models[1].maxs);
        /*
         * Com.p("areanodes:" + sv_numareanodes + " (sollten 32 sein)."); for
         * (int n = 0; n < sv_numareanodes; n++) { Com.Printf( "|%3i|%2i|%8.2f
         * |%8.2f|%8.2f|%8.2f| %8.2f|%8.2f|%8.2f|\n", new Vargs() .addAt(n)
         * .addAt(sv_areanodes[n].axis) .addAt(sv_areanodes[n].dist)
         * .addAt(sv_areanodes[n].mins_rst[0]) .addAt(sv_areanodes[n].mins_rst[1])
         * .addAt(sv_areanodes[n].mins_rst[2]) .addAt(sv_areanodes[n].maxs_rst[0])
         * .addAt(sv_areanodes[n].maxs_rst[1]) .addAt(sv_areanodes[n].maxs_rst[2])); }
         */
    }

    /*
     * =============== SV_UnlinkEdict ===============
     */
    public static void SV_UnlinkEdict(edict_t ent) {
        if (null == ent.area.prev)
            return; 
        RemoveLink(ent.area);
        ent.area.prev = ent.area.next = null;
    }

    public static void SV_LinkEdict(edict_t ent) {
        if (ent.area.prev != null)
            SV_UnlinkEdict(ent); 
        if (ent == GameBase.g_edicts[0])
            return; 
        if (!ent.inuse)
            return;
        
        Math3D.VectorSubtract(ent.maxs, ent.mins, ent.size);

        int j;
        if (ent.solid == Defines.SOLID_BBOX
                && 0 == (ent.svflags & Defines.SVF_DEADMONSTER)) {
            
            int i = (int) (ent.maxs[0] / 8);
            if (i < 1)
                i = 1;
            if (i > 31)
                i = 31;
            
            j = (int) ((-ent.mins[2]) / 8);
            if (j < 1)
                j = 1;
            if (j > 31)
                j = 31;

            int k = (int) ((ent.maxs[2] + 32) / 8);
            if (k < 1)
                k = 1;
            if (k > 63)
                k = 63;
            ent.s.solid = (k << 10) | (j << 5) | i;
        } else if (ent.solid == Defines.SOLID_BSP) {
            ent.s.solid = 31; 
        } else
            ent.s.solid = 0;
        
        if (ent.solid == Defines.SOLID_BSP
                && (IntStream.of(0, 1, 2).anyMatch(i1 -> ent.s.angles[i1] != 0))) {

            float max = 0;
            for (int i = 0; i < 3; i++) {
                float v = Math.abs(ent.mins[i]);
                if (v > max)
                    max = v;
                v = Math.abs(ent.maxs[i]);
                if (v > max)
                    max = v;
            }
            for (int i = 0; i < 3; i++) {
                float ei = ent.s.origin[i];
                ent.absmin[i] = ei - max;
                ent.absmax[i] = ei + max;
            }
        } else {
            
            Math3D.VectorAdd(ent.s.origin, ent.mins, ent.absmin);
            Math3D.VectorAdd(ent.s.origin, ent.maxs, ent.absmax);
        }
        
        
        ent.absmin[0]--;
        ent.absmin[1]--;
        ent.absmin[2]--;
        ent.absmax[0]++;
        ent.absmax[1]++;
        ent.absmax[2]++;
        
        ent.num_clusters = 0;
        ent.areanum = 0;
        ent.areanum2 = 0;

        int topnode = 0;
        int[] iw = {topnode};
        int num_leafs = CM.CM_BoxLeafnums(ent.absmin, ent.absmax, SV_WORLD.leafs,
                SV_WORLD.MAX_TOTAL_ENT_LEAFS, iw);
        topnode = iw[0];
        
        for (int i = 0; i < num_leafs; i++) {
            SV_WORLD.clusters[i] = CM.CM_LeafCluster(SV_WORLD.leafs[i]);
            int area = CM.CM_LeafArea(SV_WORLD.leafs[i]);
            if (area != 0) {
                
                
                if (ent.areanum != 0 && ent.areanum != area) {
                    if (ent.areanum2 != 0 && ent.areanum2 != area
                            && SV_INIT.sv.state == Defines.ss_loading)
                        Com.DPrintf("Object touching 3 areas at "
                                + ent.absmin[0] + ' ' + ent.absmin[1] + ' '
                                + ent.absmin[2] + '\n');
                    ent.areanum2 = area;
                } else
                    ent.areanum = area;
            }
        }
        if (num_leafs >= SV_WORLD.MAX_TOTAL_ENT_LEAFS) {
            
            ent.num_clusters = -1;
            ent.headnode = topnode;
        } else {
            ent.num_clusters = 0;
            for (int i = 0; i < num_leafs; i++) {
                if (SV_WORLD.clusters[i] == -1)
                    continue; 
                for (j = 0; j < i; j++)
                    if (SV_WORLD.clusters[j] == SV_WORLD.clusters[i])
                        break;
                if (j == i) {
                    if (ent.num_clusters == Defines.MAX_ENT_CLUSTERS) {
                        
                        ent.num_clusters = -1;
                        ent.headnode = topnode;
                        break;
                    }
                    ent.clusternums[ent.num_clusters++] = SV_WORLD.clusters[i];
                }
            }
        }
        
        if (0 == ent.linkcount) {
            Math3D.VectorCopy(ent.s.origin, ent.s.old_origin);
        }
        ent.linkcount++;
        if (ent.solid == Defines.SOLID_NOT)
            return;

        areanode_t node = SV_WORLD.sv_areanodes[0];
        while (node.axis != -1) {
            if (ent.absmin[node.axis] > node.dist)
                node = node.children[0];
            else if (ent.absmax[node.axis] < node.dist)
                node = node.children[1];
            else
                break; 
        }
        
        if (ent.solid == Defines.SOLID_TRIGGER)
            InsertLinkBefore(ent.area, node.trigger_edicts);
        else
            InsertLinkBefore(ent.area, node.solid_edicts);
    }

    /*
     * ==================== SV_AreaEdicts_r
     * 
     * ====================
     */
    public static void SV_AreaEdicts_r(areanode_t node) {
        link_t start;

        if (SV_WORLD.area_type == Defines.AREA_SOLID)
            start = node.solid_edicts;
        else
            start = node.trigger_edicts;
        link_t next;
        for (link_t l = start.next; l != start; l = next) {
            next = l.next;
            edict_t check = (edict_t) l.o;
            if (check.solid == Defines.SOLID_NOT)
                continue; 
            if (check.absmin[0] > SV_WORLD.area_maxs[0]
                    || check.absmin[1] > SV_WORLD.area_maxs[1]
                    || check.absmin[2] > SV_WORLD.area_maxs[2]
                    || check.absmax[0] < SV_WORLD.area_mins[0]
                    || check.absmax[1] < SV_WORLD.area_mins[1]
                    || check.absmax[2] < SV_WORLD.area_mins[2])
                continue; 
            if (SV_WORLD.area_count == SV_WORLD.area_maxcount) {
                Com.Printf("SV_AreaEdicts: MAXCOUNT\n");
                return;
            }
            SV_WORLD.area_list[SV_WORLD.area_count] = check;
            SV_WORLD.area_count++;
        }
        if (node.axis == -1)
            return; 
        
        if (SV_WORLD.area_maxs[node.axis] > node.dist)
            SV_AreaEdicts_r(node.children[0]);
        if (SV_WORLD.area_mins[node.axis] < node.dist)
            SV_AreaEdicts_r(node.children[1]);
    }

    /*
     * ================ SV_AreaEdicts ================
     */
    public static int SV_AreaEdicts(float[] mins, float[] maxs, edict_t[] list,
                                    int maxcount, int areatype) {
        SV_WORLD.area_mins = mins;
        SV_WORLD.area_maxs = maxs;
        SV_WORLD.area_list = list;
        SV_WORLD.area_count = 0;
        SV_WORLD.area_maxcount = maxcount;
        SV_WORLD.area_type = areatype;
        SV_AreaEdicts_r(SV_WORLD.sv_areanodes[0]);
        return SV_WORLD.area_count;
    }

    /*
     * ============= SV_PointContents =============
     */
    public static int SV_PointContents(float[] p) {

        int contents = CM.PointContents(p, SV_INIT.sv.models[1].headnode);

        int num = SV_AreaEdicts(p, p, SV_WORLD.touch, Defines.MAX_EDICTS,
                Defines.AREA_SOLID);
        for (int i = 0; i < num; i++) {
            edict_t hit = SV_WORLD.touch[i];

            int headnode = SV_HullForEntity(hit);
            if (hit.solid != Defines.SOLID_BSP) {
	    }
            int c2 = CM.TransformedPointContents(p, headnode, hit.s.origin,
                    hit.s.angles);
            contents |= c2;
        }
        return contents;
    }

    /*
     * ================ SV_HullForEntity
     * 
     * Returns a headnode that can be used for testing or clipping an object of
     * mins/maxs size. Offset is filled in to contain the adjustment that must
     * be added to the testing object's origin to get a point to use with the
     * returned hull. ================
     */
    public static int SV_HullForEntity(edict_t ent) {

        if (ent.solid == Defines.SOLID_BSP) {

            cmodel_t model = SV_INIT.sv.models[ent.s.modelindex];
            if (null == model)
                Com.Error(Defines.ERR_FATAL,
                        "MOVETYPE_PUSH with a non bsp model");
            return model.headnode;
        }
        
        return CM.HeadnodeForBox(ent.mins, ent.maxs);
    }

    public static void SV_ClipMoveToEntities(moveclip_t clip) {
        int num = SV_AreaEdicts(clip.boxmins, clip.boxmaxs, SV_WORLD.touchlist,
                Defines.MAX_EDICTS, Defines.AREA_SOLID);
        
        
        for (int i = 0; i < num; i++) {
            edict_t touch = SV_WORLD.touchlist[i];
            if (touch.solid == Defines.SOLID_NOT)
                continue;
            if (touch == clip.passedict)
                continue;
            if (clip.trace.allsolid)
                return;
            if (clip.passedict != null) {
                if (touch.owner == clip.passedict)
                    continue; 
                if (clip.passedict.owner == touch)
                    continue; 
            }
            if (0 == (clip.contentmask & Defines.CONTENTS_DEADMONSTER)
                    && 0 != (touch.svflags & Defines.SVF_DEADMONSTER))
                continue;

            int headnode = SV_HullForEntity(touch);
            float[] angles = touch.s.angles;
            if (touch.solid != Defines.SOLID_BSP)
                angles = Globals.vec3_origin;
            trace_t trace;
            if ((touch.svflags & Defines.SVF_MONSTER) != 0)
                trace = CM.TransformedBoxTrace(clip.start, clip.end,
                        clip.mins2, clip.maxs2, headnode, clip.contentmask,
                        touch.s.origin, angles);
            else
                trace = CM.TransformedBoxTrace(clip.start, clip.end, clip.mins,
                        clip.maxs, headnode, clip.contentmask, touch.s.origin,
                        angles);
            if (trace.allsolid || trace.startsolid
                    || trace.fraction < clip.trace.fraction) {
                trace.ent = touch;
                if (clip.trace.startsolid) {
                    clip.trace = trace;
                    clip.trace.startsolid = true;
                } else
                    clip.trace.set(trace);
            } else if (trace.startsolid)
                clip.trace.startsolid = true;
        }
    }

    /*
     * ================== SV_TraceBounds ==================
     */
    public static void SV_TraceBounds(float[] start, float[] mins,
            float[] maxs, float[] end, float[] boxmins, float[] boxmaxs) {
        for (int i = 0; i < 3; i++) {
            if (end[i] > start[i]) {
                boxmins[i] = start[i] + mins[i] - 1;
                boxmaxs[i] = end[i] + maxs[i] + 1;
            } else {
                boxmins[i] = end[i] + mins[i] - 1;
                boxmaxs[i] = start[i] + maxs[i] + 1;
            }
        }
    }

    /*
     * ================== SV_Trace
     * 
     * Moves the given mins/maxs volume through the world from start to end.
     * 
     * Passedict and edicts owned by passedict are explicitly not checked.
     * 
     * ==================
     */
    public static trace_t SV_Trace(float[] start, float[] mins, float[] maxs,
            float[] end, edict_t passedict, int contentmask) {
        moveclip_t clip = new moveclip_t();
        if (mins == null)
            mins = Globals.vec3_origin;
        if (maxs == null)
            maxs = Globals.vec3_origin;

        
        clip.trace = CM.BoxTrace(start, end, mins, maxs, 0, contentmask);
        clip.trace.ent = GameBase.g_edicts[0];
        if (clip.trace.fraction == 0)
            return clip.trace; 
        clip.contentmask = contentmask;
        clip.start = start;
        clip.end = end;
        clip.mins = mins;
        clip.maxs = maxs;
        clip.passedict = passedict;
        Math3D.VectorCopy(mins, clip.mins2);
        Math3D.VectorCopy(maxs, clip.maxs2);
        
        SV_TraceBounds(start, clip.mins2, clip.maxs2, end, clip.boxmins,
                clip.boxmaxs);
        
        SV_ClipMoveToEntities(clip);
        return clip.trace;
    }
}