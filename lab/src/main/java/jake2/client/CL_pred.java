/*
 * CL_pred.java
 * Copyright (C) 2004
 * 
 * $Id: CL_pred.java,v 1.7 2007-05-14 22:29:30 cawe Exp $
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

import jake2.Defines;
import jake2.Globals;
import jake2.game.*;
import jake2.qcommon.CM;
import jake2.qcommon.Com;
import jake2.qcommon.PMove;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.util.stream.IntStream;

/**
 * CL_pred
 */
public class CL_pred {

    /*
     * =================== CL_CheckPredictionError ===================
     */
    static void CheckPredictionError() {

        if (Globals.cl_predict.value == 0.0f
                || (Globals.cl.frame.playerstate.pmove.pm_flags & pmove_t.PMF_NO_PREDICTION) != 0)
            return;


        int frame = Globals.cls.netchan.incoming_acknowledged;
        frame &= (Defines.CMD_BACKUP - 1);


        int[] delta = new int[3];
        Math3D.VectorSubtract(Globals.cl.frame.playerstate.pmove.origin,
                Globals.cl.predicted_origins[frame], delta);


        int len = 0;
        for (int i2 : new int[]{0, 1, 2}) {
            int abs = Math.abs(delta[i2]);
            len += abs;
        }
        if (len > 640)
        { 
            Math3D.VectorClear(Globals.cl.prediction_error);
        } else {
            if (Globals.cl_showmiss.value != 0.0f) {
                boolean b = false;
                for (int i11 : new int[]{0, 1, 2}) {
                    if (delta[i11] != 0) {
                        b = true;
                        break;
                    }
                }
                if (b) {
                    int sum = 0;
                    for (int v : new int[]{0, 1, 2}) {
                        int i = delta[v];
                        sum += i;
                    }
                    Com.Printf("prediction miss on " + Globals.cl.frame.serverframe
                            + ": " + (sum) + '\n');
                }
            }

            Math3D.VectorCopy(Globals.cl.frame.playerstate.pmove.origin,
                    Globals.cl.predicted_origins[frame]);

            
            for (int i = 0; i < 3; i++)
                Globals.cl.prediction_error[i] = delta[i] * 0.125f;
        }
    }

    /*
     * ==================== CL_ClipMoveToEntities
     * 
     * ====================
     */
    static void ClipMoveToEntities(float[] start, float[] mins, float[] maxs,
            float[] end, trace_t tr) {
        float[] bmins = new float[3];
        float[] bmaxs = new float[3];

        for (int i = 0; i < Globals.cl.frame.num_entities; i++) {
            int num = (Globals.cl.frame.parse_entities + i)
                    & (Defines.MAX_PARSE_ENTITIES - 1);
            entity_state_t ent = Globals.cl_parse_entities[num];

            if (ent.solid == 0)
                continue;

            if (ent.number == Globals.cl.playernum + 1)
                continue;

            float[] angles;
            int headnode;
            if (ent.solid == 31) {
                cmodel_t cmodel = Globals.cl.model_clip[ent.modelindex];
                if (cmodel == null)
                    continue;
                headnode = cmodel.headnode;
                angles = ent.angles;
            } else {
                int x = 8 * (ent.solid & 31);
                int zd = 8 * ((ent.solid >>> 5) & 31);
                int zu = 8 * ((ent.solid >>> 10) & 63) - 32;

                bmins[0] = bmins[1] = -x;
                bmaxs[0] = bmaxs[1] = x;
                bmins[2] = -zd;
                bmaxs[2] = zu;

                headnode = CM.HeadnodeForBox(bmins, bmaxs);
                angles = Globals.vec3_origin; 
            }

            if (tr.allsolid)
                return;

            trace_t trace = CM.TransformedBoxTrace(start, end, mins, maxs, headnode,
                    Defines.MASK_PLAYERSOLID, ent.origin, angles);

            if (trace.allsolid || trace.startsolid
                    || trace.fraction < tr.fraction) {
                trace.ent = ent.surrounding_ent;
                if (tr.startsolid) {
                    tr.set(trace); 
                                   
                    tr.startsolid = true;
                } else
                    tr.set(trace); 
                                   
            } else if (trace.startsolid)
                tr.startsolid = true;
        }
    }

    /*
     * ================ CL_PMTrace ================
     */

    public static final edict_t DUMMY_ENT = new edict_t(-1);

    static trace_t PMTrace(float[] start, float[] mins, float[] maxs,
            float[] end) {


        trace_t t = CM.BoxTrace(start, end, mins, maxs, 0, Defines.MASK_PLAYERSOLID);

        if (t.fraction < 1.0f) {
            t.ent = DUMMY_ENT;
        }

        
        ClipMoveToEntities(start, mins, maxs, end, t);

        return t;
    }

    /*
     * ================= PMpointcontents
     * 
     * Returns the content identificator of the point. =================
     */
    static int PMpointcontents(float[] point) {

        int contents = CM.PointContents(point, 0);

        for (int i = 0; i < Globals.cl.frame.num_entities; i++) {
            int num = (Globals.cl.frame.parse_entities + i)
                    & (Defines.MAX_PARSE_ENTITIES - 1);
            entity_state_t ent = Globals.cl_parse_entities[num];

            if (ent.solid != 31) 
                continue;

            cmodel_t cmodel = Globals.cl.model_clip[ent.modelindex];
            if (cmodel == null)
                continue;

            contents |= CM.TransformedPointContents(point, cmodel.headnode,
                    ent.origin, ent.angles);
        }
        return contents;
    }

    /*
     * ================= CL_PredictMovement
     * 
     * Sets cl.predicted_origin and cl.predicted_angles =================
     */
    static void PredictMovement() {

        if (Globals.cls.state != Defines.ca_active)
            return;

        if (Globals.cl_paused.value != 0.0f)
            return;

        if (Globals.cl_predict.value == 0.0f
                || (Globals.cl.frame.playerstate.pmove.pm_flags & pmove_t.PMF_NO_PREDICTION) != 0) {
            
            for (int i = 0; i < 3; i++) {
                Globals.cl.predicted_angles[i] = Globals.cl.viewangles[i]
                        + Math3D
                                .SHORT2ANGLE(Globals.cl.frame.playerstate.pmove.delta_angles[i]);
            }
            return;
        }

        int ack = Globals.cls.netchan.incoming_acknowledged;
        int current = Globals.cls.netchan.outgoing_sequence;

        
        if (current - ack >= Defines.CMD_BACKUP) {
            if (Globals.cl_showmiss.value != 0.0f)
                Com.Printf("exceeded CMD_BACKUP\n");
            return;
        }


        pmove_t pm = new pmove_t();

        pm.trace = new pmove_t.TraceAdapter() {
            @Override
            public trace_t trace(float[] start, float[] mins, float[] maxs,
                                 float[] end) {
                return PMTrace(start, mins, maxs, end);
            }
        };
        pm.pointcontents = new pmove_t.PointContentsAdapter() {
            @Override
            public int pointcontents(float[] point) {
                return PMpointcontents(point);
            }
        };

        PMove.pm_airaccelerate = Lib.atof(Globals.cl.configstrings[Defines.CS_AIRACCEL]);

        
        
        pm.s.set(Globals.cl.frame.playerstate.pmove);


        int frame = 0;


        while (++ack < current) {
            frame = ack & (Defines.CMD_BACKUP - 1);
            usercmd_t cmd = Globals.cl.cmds[frame];

            pm.cmd.set(cmd);

            PMove.Pmove(pm);

            
            Math3D.VectorCopy(pm.s.origin, Globals.cl.predicted_origins[frame]);
        }

        int oldframe = (ack - 2) & (Defines.CMD_BACKUP - 1);
        int oldz = Globals.cl.predicted_origins[oldframe][2];
        int step = pm.s.origin[2] - oldz;
        if (step > 63 && step < 160
                && (pm.s.pm_flags & pmove_t.PMF_ON_GROUND) != 0) {
            Globals.cl.predicted_step = step * 0.125f;
            Globals.cl.predicted_step_time = (int) (Globals.cls.realtime - Globals.cls.frametime * 500);
        }

        
        Globals.cl.predicted_origin[0] = pm.s.origin[0] * 0.125f;
        Globals.cl.predicted_origin[1] = pm.s.origin[1] * 0.125f;
        Globals.cl.predicted_origin[2] = pm.s.origin[2] * 0.125f;

        Math3D.VectorCopy(pm.viewangles, Globals.cl.predicted_angles);
    }
}