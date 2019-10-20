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



package jake2.game;

import jake2.Defines;
import jake2.util.Math3D;

import java.util.Arrays;

public class pmove_t {

    public static class TraceAdapter {
        
        public trace_t trace(float[] start, float[] mins, float[] maxs,
                float[] end) {
            return null;
        }
    }

    public static class PointContentsAdapter {
        
        public int pointcontents(float[] point) {
            return 0;
        }
    }

    
    public final pmove_state_t s = new pmove_state_t();

    
    public final usercmd_t cmd = new usercmd_t();

    public boolean snapinitial; 

    
    public int numtouch;

    public final edict_t[] touchents = new edict_t[Defines.MAXTOUCH];

    public final float[] viewangles = {(float) 0, (float) 0, (float) 0};

    public float viewheight;

    public final float[] mins = {(float) 0, (float) 0, (float) 0};
    public final float[] maxs = {(float) 0, (float) 0, (float) 0};

    public edict_t groundentity;

    public int watertype;

    public int waterlevel;

    public TraceAdapter trace;

    public PointContentsAdapter pointcontents;

    
    public static final int PMF_DUCKED = 1;

    public static final int PMF_JUMP_HELD = 2;

    public static final int PMF_ON_GROUND = 4;

    public static final int PMF_TIME_WATERJUMP = 8;

    public static final int PMF_TIME_LAND = 16;

    public static final int PMF_TIME_TELEPORT = 32;
                                                    

    public static final int PMF_NO_PREDICTION = 64;
                                                    
                                                    

    public void clear() {
        groundentity = null;
        waterlevel = watertype = 0;
        trace = null;
        pointcontents = null;
        Math3D.VectorClear(mins);
        Math3D.VectorClear(maxs);
        viewheight = (float) 0;
        Math3D.VectorClear(viewangles);
        Arrays.fill(touchents, null);
        numtouch = 0;
        snapinitial = false;
        cmd.clear();
        s.clear();
    }
}