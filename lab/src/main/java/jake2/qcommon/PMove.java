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



package jake2.qcommon;

import jake2.Defines;
import jake2.Globals;
import jake2.game.csurface_t;
import jake2.game.pmove_t;
import jake2.game.trace_t;
import jake2.server.SV;
import jake2.util.Math3D;

public class PMove {

    
    
    

    public static class pml_t {
        public final float[] origin = {(float) 0, (float) 0, (float) 0};

        public final float[] velocity = {(float) 0, (float) 0, (float) 0};

        public final float[] forward = {(float) 0, (float) 0, (float) 0};
        public final float[] right = {(float) 0, (float) 0, (float) 0};
        public final float[] up = {(float) 0, (float) 0,
                (float) 0};

        public float frametime;

        public csurface_t groundsurface;

        public int groundcontents;

        public final float[] previous_origin = {(float) 0, (float) 0, (float) 0};

        public boolean ladder;
    }

    public static pmove_t pm;

    public static final pml_t pml = new pml_t();

    
    public static final float pm_stopspeed = 100.0F;

    public static final float pm_maxspeed = 300.0F;

    public static final float pm_duckspeed = 100.0F;

    public static final float pm_accelerate = 10.0F;

    public static float pm_airaccelerate;

    public static final float pm_wateraccelerate = 10.0F;

    public static final float pm_friction = 6.0F;

    public static final float pm_waterfriction = 1.0F;

    public static final float pm_waterspeed = 400.0F;

    
    public static final int[] jitterbits = { 0, 4, 1, 2, 3, 5, 6, 7 };

    public static final int[] offset = { 0, -1, 1 };


    /**
     * Slide off of the impacting object returns the blocked flags (1 = floor, 2 = step / wall)
     */
    public static void PM_ClipVelocity(float[] in, float[] normal, float[] out, float overbounce) {

        float backoff = Math3D.DotProduct(in, normal) * overbounce;

        for (int i = 0; i < 3; i++) {
            float change = normal[i] * backoff;
            out[i] = in[i] - change;
            if (out[i] > -Defines.MOVE_STOP_EPSILON
                    && out[i] < Defines.MOVE_STOP_EPSILON)
                out[i] = (float) 0;
        }
    }

    static final float[][] planes = new float[SV.MAX_CLIP_PLANES][3];
    
    public static void PM_StepSlideMove_() {

        float[] primal_velocity = {(float) 0, (float) 0, (float) 0};

        Math3D.VectorCopy(pml.velocity, primal_velocity);
        int numplanes = 0;

        float time_left = pml.frametime;

        int numbumps = 4;
        float[] end = {(float) 0, (float) 0, (float) 0};
        float[] dir = {(float) 0, (float) 0, (float) 0};
        for (int bumpcount = 0; bumpcount < numbumps; bumpcount++) {
            int i;
            for (i = 0; i < 3; i++)
                end[i] = pml.origin[i] + time_left
                        * pml.velocity[i];

            trace_t trace = pm.trace.trace(pml.origin, pm.mins,
                    pm.maxs, end);

            if (trace.allsolid) { 
                pml.velocity[2] = (float) 0;
                return;
            }

            if (trace.fraction > (float) 0) {
                Math3D.VectorCopy(trace.endpos, pml.origin);
                numplanes = 0;
            }

            if (trace.fraction == 1.0F)
                break; 

            
            if (pm.numtouch < Defines.MAXTOUCH && trace.ent != null) {
                pm.touchents[pm.numtouch] = trace.ent;
                pm.numtouch++;
            }

            time_left -= time_left * trace.fraction;

            
            if (numplanes >= SV.MAX_CLIP_PLANES) { 
            	
                Math3D.VectorCopy(Globals.vec3_origin, pml.velocity);
                break;
            }

            Math3D.VectorCopy(trace.plane.normal, planes[numplanes]);
            numplanes++;

            
            for (i = 0; i < numplanes; i++) {
                PM_ClipVelocity(pml.velocity, planes[i],
                        pml.velocity, 1.01f);
                int j;
                for (j = 0; j < numplanes; j++)
                    if (j != i) {
                        if (Math3D.DotProduct(pml.velocity, planes[j]) < (float) 0)
                            break; 
                    }
                if (j == numplanes)
                    break;
            }

            if (i != numplanes) { 
            	
            } else { 
            	
                if (numplanes != 2) {
                    
                    Math3D.VectorCopy(Globals.vec3_origin, pml.velocity);
                    break;
                }
                Math3D.CrossProduct(planes[0], planes[1], dir);
                float d = Math3D.DotProduct(dir, pml.velocity);
                Math3D.VectorScale(dir, d, pml.velocity);
            }


            
            
            if (Math3D.DotProduct(pml.velocity, primal_velocity) <= (float) 0) {
                Math3D.VectorCopy(Globals.vec3_origin, pml.velocity);
                break;
            }
        }

        if ((int) pm.s.pm_time != 0) {
            Math3D.VectorCopy(primal_velocity, pml.velocity);
        }
    }

    /**
     * Each intersection will try to step over the obstruction instead of 
     * sliding along it.
     * 
     * Returns a new origin, velocity, and contact entity.
     * Does not modify any world state?
     */
    public static void PM_StepSlideMove() {
        float[] start_o = {(float) 0, (float) 0, (float) 0};

        Math3D.VectorCopy(pml.origin, start_o);
        float[] start_v = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorCopy(pml.velocity, start_v);

        PM_StepSlideMove_();

        float[] down_o = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorCopy(pml.origin, down_o);
        float[] down_v = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorCopy(pml.velocity, down_v);

        float[] up = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorCopy(start_o, up);
        up[2] = up[2] + (float) Defines.STEPSIZE;

        trace_t trace = pm.trace.trace(up, pm.mins, pm.maxs, up);
        if (trace.allsolid)
            return; 

        
        Math3D.VectorCopy(up, pml.origin);
        Math3D.VectorCopy(start_v, pml.velocity);

        PM_StepSlideMove_();


        float[] down = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorCopy(pml.origin, down);
        down[2] = down[2] - (float) Defines.STEPSIZE;
        trace = pm.trace.trace(pml.origin, pm.mins,
                pm.maxs, down);
        if (!trace.allsolid) {
            Math3D.VectorCopy(trace.endpos, pml.origin);
        }

        Math3D.VectorCopy(pml.origin, up);


        float down_dist = (down_o[0] - start_o[0]) * (down_o[0] - start_o[0])
                + (down_o[1] - start_o[1]) * (down_o[1] - start_o[1]);
        float up_dist = (up[0] - start_o[0]) * (up[0] - start_o[0])
                + (up[1] - start_o[1]) * (up[1] - start_o[1]);

        if (down_dist > up_dist || trace.plane.normal[2] < Defines.MIN_STEP_NORMAL) {
            Math3D.VectorCopy(down_o, pml.origin);
            Math3D.VectorCopy(down_v, pml.velocity);
            return;
        }
        
        
        pml.velocity[2] = down_v[2];
    }

    /**
     * Handles both ground friction and water friction.
     */
    public static void PM_Friction() {

        float[] vel = pml.velocity;

        float speed = (float) (Math.sqrt((double) (vel[0] * vel[0] + vel[1] * vel[1] + vel[2] * vel[2])));
        if (speed < 1.0F) {
            vel[0] = (float) 0;
            vel[1] = (float) 0;
            return;
        }

        float drop = (float) 0;

        
        if ((pm.groundentity != null && pml.groundsurface != null && 
        		0 == (pml.groundsurface.flags & Defines.SURF_SLICK))
                || (pml.ladder)) {
            float friction = pm_friction;
            float control = speed < pm_stopspeed ? pm_stopspeed : speed;
            drop += control * friction * pml.frametime;
        }

        
        if (pm.waterlevel != 0 && !pml.ladder)
            drop += speed * pm_waterfriction * (float) pm.waterlevel
                    * pml.frametime;


        float newspeed = speed - drop;
        if (newspeed < (float) 0) {
            newspeed = (float) 0;
        }
        newspeed /= speed;

        vel[0] *= newspeed;
        vel[1] *= newspeed;
        vel[2] *= newspeed;
    }

    /**
     * Handles user intended acceleration.
     */
    public static void PM_Accelerate(float[] wishdir, float wishspeed,
            float accel) {

        float currentspeed = Math3D.DotProduct(pml.velocity, wishdir);
        float addspeed = wishspeed - currentspeed;
        if (addspeed <= (float) 0)
            return;
        float accelspeed = accel * pml.frametime * wishspeed;
        if (accelspeed > addspeed)
            accelspeed = addspeed;

        for (int i = 0; i < 3; i++)
            pml.velocity[i] += accelspeed * wishdir[i];
    }
    
    /**
     * PM_AirAccelerate.
     */

    public static void PM_AirAccelerate(float[] wishdir, float wishspeed,
            float accel) {
        float wishspd = wishspeed;

        if (wishspd > 30.0F)
            wishspd = 30.0F;
        float currentspeed = Math3D.DotProduct(pml.velocity, wishdir);
        float addspeed = wishspd - currentspeed;
        if (addspeed <= (float) 0)
            return;
        float accelspeed = accel * wishspeed * pml.frametime;
        if (accelspeed > addspeed)
            accelspeed = addspeed;

        for (int i = 0; i < 3; i++)
            pml.velocity[i] += accelspeed * wishdir[i];
    }

    /**
     * PM_AddCurrents.
     */
    public static void PM_AddCurrents(float[] wishvel) {


        if (pml.ladder && Math.abs(pml.velocity[2]) <= 200.0F) {
            if ((pm.viewangles[Defines.PITCH] <= -15.0F)
                    && ((int) pm.cmd.forwardmove > 0))
                wishvel[2] = 200.0F;
            else if ((pm.viewangles[Defines.PITCH] >= 15.0F)
                    && ((int) pm.cmd.forwardmove > 0))
                wishvel[2] = -200.0F;
            else if ((int) pm.cmd.upmove > 0)
                wishvel[2] = 200.0F;
            else if ((int) pm.cmd.upmove < 0)
                wishvel[2] = -200.0F;
            else
                wishvel[2] = (float) 0;

            
            if (wishvel[0] < -25.0F)
                wishvel[0] = -25.0F;
            else if (wishvel[0] > 25.0F)
                wishvel[0] = 25.0F;

            if (wishvel[1] < -25.0F)
                wishvel[1] = -25.0F;
            else if (wishvel[1] > 25.0F)
                wishvel[1] = 25.0F;
        }


        float[] v = {(float) 0, (float) 0, (float) 0};
        if ((pm.watertype & Defines.MASK_CURRENT) != 0) {
            Math3D.VectorClear(v);

            if ((pm.watertype & Defines.CONTENTS_CURRENT_0) != 0)
                v[0] += 1.0F;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_90) != 0)
                v[1] += 1.0F;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_180) != 0)
                v[0] -= 1.0F;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_270) != 0)
                v[1] -= 1.0F;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_UP) != 0)
                v[2] += 1.0F;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_DOWN) != 0)
                v[2] -= 1.0F;

            float s = pm_waterspeed;
            if ((pm.waterlevel == 1) && (pm.groundentity != null))
                s /= 2.0F;

            Math3D.VectorMA(wishvel, s, v, wishvel);
        }

        
        if (pm.groundentity != null) {
            Math3D.VectorClear(v);

            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_0) != 0)
                v[0] += 1.0F;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_90) != 0)
                v[1] += 1.0F;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_180) != 0)
                v[0] -= 1.0F;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_270) != 0)
                v[1] -= 1.0F;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_UP) != 0)
                v[2] += 1.0F;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_DOWN) != 0)
                v[2] -= 1.0F;

            Math3D.VectorMA(wishvel, 100.0F /* pm.groundentity.speed */, v, wishvel);
        }
    }

    /**
     * PM_WaterMove.
     */
    public static void PM_WaterMove() {
        float[] wishvel = {(float) 0, (float) 0, (float) 0};


        for (int i = 0; i < 3; i++)
            wishvel[i] = pml.forward[i] * (float) pm.cmd.forwardmove
                    + pml.right[i] * (float) pm.cmd.sidemove;

        if (0 == (int) pm.cmd.forwardmove && 0 == (int) pm.cmd.sidemove
                && 0 == (int) pm.cmd.upmove)
            wishvel[2] -= 60.0F;
        else
            wishvel[2] = wishvel[2] + (float) pm.cmd.upmove;

        PM_AddCurrents(wishvel);

        float[] wishdir = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorCopy(wishvel, wishdir);
        float wishspeed = Math3D.VectorNormalize(wishdir);

        if (wishspeed > pm_maxspeed) {
            Math3D.VectorScale(wishvel, pm_maxspeed / wishspeed, wishvel);
            wishspeed = pm_maxspeed;
        }
        wishspeed = (float) ((double) wishspeed * 0.5);

        PM_Accelerate(wishdir, wishspeed, pm_wateraccelerate);

        PM_StepSlideMove();
    }

    /**
     * PM_AirMove.
     */
    public static void PM_AirMove() {
        float[] wishvel = {(float) 0, (float) 0, (float) 0};

        float fmove = (float) pm.cmd.forwardmove;
        float smove = (float) pm.cmd.sidemove;

        wishvel[0] = pml.forward[0] * fmove + pml.right[0] * smove;
        wishvel[1] = pml.forward[1] * fmove + pml.right[1] * smove;
        
        wishvel[2] = (float) 0;

        PM_AddCurrents(wishvel);

        float[] wishdir = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorCopy(wishvel, wishdir);
        float wishspeed = Math3D.VectorNormalize(wishdir);


        float maxspeed = ((int) pm.s.pm_flags & pmove_t.PMF_DUCKED) != 0 ? pm_duckspeed
                : pm_maxspeed;

        if (wishspeed > maxspeed) {
            Math3D.VectorScale(wishvel, maxspeed / wishspeed, wishvel);
            wishspeed = maxspeed;
        }

        if (pml.ladder) {
            PM_Accelerate(wishdir, wishspeed, pm_accelerate);
            if ((float) 0 == wishvel[2]) {
                if (pml.velocity[2] > (float) 0) {
                    pml.velocity[2] -= (float) pm.s.gravity * pml.frametime;
                    if (pml.velocity[2] < (float) 0)
                        pml.velocity[2] = (float) 0;
                } else {
                    pml.velocity[2] += (float) pm.s.gravity * pml.frametime;
                    if (pml.velocity[2] > (float) 0)
                        pml.velocity[2] = (float) 0;
                }
            }
            PM_StepSlideMove();
        } else if (pm.groundentity != null) { 
            pml.velocity[2] = (float) 0;
            PM_Accelerate(wishdir, wishspeed, pm_accelerate);

            
            
            if ((int) pm.s.gravity > 0)
                pml.velocity[2] = (float) 0;
            else
                pml.velocity[2] -= (float) pm.s.gravity * pml.frametime;
            
            if ((float) 0 == pml.velocity[0] && (float) 0 == pml.velocity[1])
                return;
            PM_StepSlideMove();
        } else { 
            if (pm_airaccelerate != (float) 0)
                PM_AirAccelerate(wishdir, wishspeed, pm_accelerate);
            else
                PM_Accelerate(wishdir, wishspeed, 1.0F);
            
            pml.velocity[2] -= (float) pm.s.gravity * pml.frametime;
            PM_StepSlideMove();
        }
    }

    /** 
     * PM_CatagorizePosition.
     */
    public static void PM_CatagorizePosition() {
        float[] point = {(float) 0, (float) 0, (float) 0};


        point[0] = pml.origin[0];
        point[1] = pml.origin[1];
        point[2] = pml.origin[2] - 0.25f;
        if (pml.velocity[2] > 180.0F)
                                         
        {
            pm.s.pm_flags = (byte) ((int) pm.s.pm_flags & ~pmove_t.PMF_ON_GROUND);
            pm.groundentity = null;
        } else {
            trace_t trace = pm.trace.trace(pml.origin, pm.mins,
                    pm.maxs, point);
            pml.groundsurface = trace.surface;
            pml.groundcontents = trace.contents;

            if (null == trace.ent
                    || ((double) trace.plane.normal[2] < 0.7 && !trace.startsolid)) {
                pm.groundentity = null;
                pm.s.pm_flags = (byte) ((int) pm.s.pm_flags & ~pmove_t.PMF_ON_GROUND);
            } else {
                pm.groundentity = trace.ent;
                
                if (((int) pm.s.pm_flags & pmove_t.PMF_TIME_WATERJUMP) != 0) {
                    pm.s.pm_flags = (byte) ((int) pm.s.pm_flags & ~(pmove_t.PMF_TIME_WATERJUMP
                            | pmove_t.PMF_TIME_LAND | pmove_t.PMF_TIME_TELEPORT));
                    pm.s.pm_time = (byte) 0;
                }

                if (0 == ((int) pm.s.pm_flags & pmove_t.PMF_ON_GROUND)) {


                    pm.s.pm_flags = (byte) ((int) pm.s.pm_flags | pmove_t.PMF_ON_GROUND);
                    
                    if (pml.velocity[2] < -200.0F) {
                        pm.s.pm_flags = (byte) ((int) pm.s.pm_flags | pmove_t.PMF_TIME_LAND);
                        
                        if (pml.velocity[2] < -400.0F)
                            pm.s.pm_time = (byte) 25;
                        else
                            pm.s.pm_time = (byte) 18;
                    }
                }
            }

            if (pm.numtouch < Defines.MAXTOUCH && trace.ent != null) {
                pm.touchents[pm.numtouch] = trace.ent;
                pm.numtouch++;
            }
        }


        
        
        pm.waterlevel = 0;
        pm.watertype = 0;

        int sample2 = (int) (pm.viewheight - pm.mins[2]);

        point[2] = pml.origin[2] + pm.mins[2] + 1.0F;
        int cont = pm.pointcontents.pointcontents(point);

        if ((cont & Defines.MASK_WATER) != 0) {
            pm.watertype = cont;
            pm.waterlevel = 1;
            int sample1 = sample2 / 2;
            point[2] = pml.origin[2] + pm.mins[2] + (float) sample1;
            cont = pm.pointcontents.pointcontents(point);
            if ((cont & Defines.MASK_WATER) != 0) {
                pm.waterlevel = 2;
                point[2] = pml.origin[2] + pm.mins[2] + (float) sample2;
                cont = pm.pointcontents.pointcontents(point);
                if ((cont & Defines.MASK_WATER) != 0)
                    pm.waterlevel = 3;
            }
        }

    }

    /**
     * PM_CheckJump.
     */
    public static void PM_CheckJump() {
        if (((int) pm.s.pm_flags & pmove_t.PMF_TIME_LAND) != 0) {
            
            return;
        }

        if ((int) pm.cmd.upmove < 10) {
            pm.s.pm_flags = (byte) ((int) pm.s.pm_flags & ~pmove_t.PMF_JUMP_HELD);
            return;
        }

        
        if (((int) pm.s.pm_flags & pmove_t.PMF_JUMP_HELD) != 0)
            return;

        if (pm.s.pm_type == Defines.PM_DEAD)
            return;

        if (pm.waterlevel >= 2) { 
            pm.groundentity = null;

            if (pml.velocity[2] <= -300.0F)
                return;

            switch (pm.watertype) {
                case Defines.CONTENTS_WATER:
                    pml.velocity[2] = 100.0F;
                    break;
                case Defines.CONTENTS_SLIME:
                    pml.velocity[2] = 80.0F;
                    break;
                default:
                    pml.velocity[2] = 50.0F;
                    break;
            }
            return;
        }

        if (pm.groundentity == null)
            return;

        pm.s.pm_flags = (byte) ((int) pm.s.pm_flags | pmove_t.PMF_JUMP_HELD);

        pm.groundentity = null;
        pml.velocity[2] += 270.0F;
        if (pml.velocity[2] < 270.0F)
            pml.velocity[2] = 270.0F;
    }

    /**
     * PM_CheckSpecialMovement.
     */
    public static void PM_CheckSpecialMovement() {

        if ((int) pm.s.pm_time != 0)
            return;

        pml.ladder = false;


        float[] flatforward = {(float) 0, (float) 0, (float) 0};
        flatforward[0] = pml.forward[0];
        flatforward[1] = pml.forward[1];
        flatforward[2] = (float) 0;
        Math3D.VectorNormalize(flatforward);

        float[] spot = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorMA(pml.origin, 1.0F, flatforward, spot);
        trace_t trace = pm.trace.trace(pml.origin, pm.mins,
                pm.maxs, spot);
        if ((trace.fraction < 1.0F)
                && (trace.contents & Defines.CONTENTS_LADDER) != 0)
            pml.ladder = true;

        
        if (pm.waterlevel != 2)
            return;

        Math3D.VectorMA(pml.origin, 30.0F, flatforward, spot);
        spot[2] += 4.0F;
        int cont = pm.pointcontents.pointcontents(spot);
        if (0 == (cont & Defines.CONTENTS_SOLID))
            return;

        spot[2] += 16.0F;
        cont = pm.pointcontents.pointcontents(spot);
        if (cont != 0)
            return;
        
        Math3D.VectorScale(flatforward, 50.0F, pml.velocity);
        pml.velocity[2] = 350.0F;

        pm.s.pm_flags = (byte) ((int) pm.s.pm_flags | pmove_t.PMF_TIME_WATERJUMP);
        pm.s.pm_time = (byte) -1;
    }

    /**
     * PM_FlyMove.
     */
    public static void PM_FlyMove(boolean doclip) {

        pm.viewheight = 22.0F;


        float speed = Math3D.VectorLength(pml.velocity);
        if (speed < 1.0F) {
            Math3D.VectorCopy(Globals.vec3_origin, pml.velocity);
        } else {
            float drop = (float) 0;

            float friction = pm_friction * 1.5f;
            float control = speed < pm_stopspeed ? pm_stopspeed : speed;
            drop += control * friction * pml.frametime;


            float newspeed = speed - drop;
            if (newspeed < (float) 0)
                newspeed = (float) 0;
            newspeed /= speed;

            Math3D.VectorScale(pml.velocity, newspeed, pml.velocity);
        }


        float fmove = (float) pm.cmd.forwardmove;
        float smove = (float) pm.cmd.sidemove;

        Math3D.VectorNormalize(pml.forward);
        Math3D.VectorNormalize(pml.right);

        float[] wishvel = {(float) 0, (float) 0, (float) 0};
        int i;
        for (i = 0; i < 3; i++)
            wishvel[i] = pml.forward[i] * fmove + pml.right[i]
                    * smove;
        wishvel[2] = wishvel[2] + (float) pm.cmd.upmove;

        float[] wishdir = {(float) 0, (float) 0, (float) 0};
        Math3D.VectorCopy(wishvel, wishdir);
        float wishspeed = Math3D.VectorNormalize(wishdir);

        
        if (wishspeed > pm_maxspeed) {
            Math3D.VectorScale(wishvel, pm_maxspeed / wishspeed, wishvel);
            wishspeed = pm_maxspeed;
        }

        float currentspeed = Math3D.DotProduct(pml.velocity, wishdir);
        float addspeed = wishspeed - currentspeed;
        if (addspeed <= (float) 0)
            return;
        float accelspeed = pm_accelerate * pml.frametime * wishspeed;
        if (accelspeed > addspeed)
            accelspeed = addspeed;

        for (i = 0; i < 3; i++)
            pml.velocity[i] += accelspeed * wishdir[i];

        if (doclip) {
            float[] end = {(float) 0, (float) 0, (float) 0};
            for (i = 0; i < 3; i++)
                end[i] = pml.origin[i] + pml.frametime * pml.velocity[i];

            trace_t trace = pm.trace.trace(pml.origin, pm.mins, pm.maxs, end);

            Math3D.VectorCopy(trace.endpos, pml.origin);
        } else {
            
            Math3D.VectorMA(pml.origin, pml.frametime, pml.velocity, pml.origin);
        }
    }

    /**
     * Sets mins, maxs, and pm.viewheight.
     */
    public static void PM_CheckDuck() {

        pm.mins[0] = -16.0F;
        pm.mins[1] = -16.0F;

        pm.maxs[0] = 16.0F;
        pm.maxs[1] = 16.0F;

        if (pm.s.pm_type == Defines.PM_GIB) {
            pm.mins[2] = (float) 0;
            pm.maxs[2] = 16.0F;
            pm.viewheight = 8.0F;
            return;
        }

        pm.mins[2] = -24.0F;

        if (pm.s.pm_type == Defines.PM_DEAD) {
            pm.s.pm_flags = (byte) ((int) pm.s.pm_flags | pmove_t.PMF_DUCKED);
        } else if ((int) pm.cmd.upmove < 0 && ((int) pm.s.pm_flags & pmove_t.PMF_ON_GROUND) != 0) {
            pm.s.pm_flags = (byte) ((int) pm.s.pm_flags | pmove_t.PMF_DUCKED);
        } else { 
            if (((int) pm.s.pm_flags & pmove_t.PMF_DUCKED) != 0) {
                
                pm.maxs[2] = 32.0F;
                trace_t trace = pm.trace.trace(pml.origin, pm.mins, pm.maxs, pml.origin);
                if (!trace.allsolid)
                    pm.s.pm_flags = (byte) ((int) pm.s.pm_flags & ~pmove_t.PMF_DUCKED);
            }
        }

        if (((int) pm.s.pm_flags & pmove_t.PMF_DUCKED) != 0) {
            pm.maxs[2] = 4.0F;
            pm.viewheight = -2.0F;
        } else {
            pm.maxs[2] = 32.0F;
            pm.viewheight = 22.0F;
        }
    }

    /**
     * Dead bodies have extra friction.
     */
    public static void PM_DeadMove() {

        if (null == pm.groundentity)
            return;


        float forward = Math3D.VectorLength(pml.velocity);
        forward -= 20.0F;
        if (forward <= (float) 0) {
            Math3D.VectorClear(pml.velocity);
        } else {
            Math3D.VectorNormalize(pml.velocity);
            Math3D.VectorScale(pml.velocity, forward, pml.velocity);
        }
    }

    public static boolean PM_GoodPosition() {

        if (pm.s.pm_type == Defines.PM_SPECTATOR)
            return true;

        float[] end = {(float) 0, (float) 0, (float) 0};
        float[] origin = {(float) 0, (float) 0, (float) 0};
        for (int i = 0; i < 3; i++)
            origin[i] = end[i] = (float) pm.s.origin[i] * 0.125f;
        trace_t trace = pm.trace.trace(origin, pm.mins, pm.maxs, end);

        return !trace.allsolid;
    }

    /**
     * On exit, the origin will have a value that is pre-quantized to the 0.125
     * precision of the network channel and in a valid position.
     */

    public static void PM_SnapPosition() {
        int i;


        for (i = 0; i < 3; i++)
            pm.s.velocity[i] = (short) (pml.velocity[i] * 8.0F);

        int[] sign = {0, 0, 0};
        for (i = 0; i < 3; i++) {
            if (pml.origin[i] >= (float) 0)
                sign[i] = 1;
            else
                sign[i] = -1;
            pm.s.origin[i] = (short) (pml.origin[i] * 8.0F);
            if ((double) pm.s.origin[i] * 0.125 == (double) pml.origin[i])
                sign[i] = 0;
        }
        short[] base = {(short) 0, (short) 0, (short) 0};
        Math3D.VectorCopy(pm.s.origin, base);

        
        for (int j = 0; j < 8; j++) {
            int bits = jitterbits[j];
            Math3D.VectorCopy(base, pm.s.origin);
            for (i = 0; i < 3; i++)
                if ((bits & (1 << i)) != 0)
                    pm.s.origin[i] = (short) ((int) pm.s.origin[i] + sign[i]);

            if (PM_GoodPosition())
                return;
        }

        
        Math3D.VectorCopy(pml.previous_origin, pm.s.origin);
        
    }

    /** 
     * Snaps the origin of the player move to 0.125 grid.
     */
    public static void PM_InitialSnapPosition() {
        short[] base = {(short) 0, (short) 0, (short) 0};

        Math3D.VectorCopy(pm.s.origin, base);

        for (int z = 0; z < 3; z++) {
            pm.s.origin[2] = (short) ((int) base[2] + offset[z]);
            for (int y = 0; y < 3; y++) {
                pm.s.origin[1] = (short) ((int) base[1] + offset[y]);
                for (int x = 0; x < 3; x++) {
                    pm.s.origin[0] = (short) ((int) base[0] + offset[x]);
                    if (PM_GoodPosition()) {
                        pml.origin[0] = (float) pm.s.origin[0] * 0.125f;
                        pml.origin[1] = (float) pm.s.origin[1] * 0.125f;
                        pml.origin[2] = (float) pm.s.origin[2] * 0.125f;
                        Math3D.VectorCopy(pm.s.origin,
                                pml.previous_origin);
                        return;
                    }
                }
            }
        }

        Com.DPrintf("Bad InitialSnapPosition\n");
    }

    /**
     * PM_ClampAngles.
     */
    public static void PM_ClampAngles() {

        if (((int) pm.s.pm_flags & pmove_t.PMF_TIME_TELEPORT) != 0) {
            pm.viewangles[Defines.YAW] = Math3D
                    .SHORT2ANGLE((int) pm.cmd.angles[Defines.YAW]
                            + (int) pm.s.delta_angles[Defines.YAW]);
            pm.viewangles[Defines.PITCH] = (float) 0;
            pm.viewangles[Defines.ROLL] = (float) 0;
        } else {
            
            for (int i = 0; i < 3; i++) {
                short temp = (short) ((int) pm.cmd.angles[i] + (int) pm.s.delta_angles[i]);
                pm.viewangles[i] = Math3D.SHORT2ANGLE((int) temp);
            }

            
            if (pm.viewangles[Defines.PITCH] > 89.0F && pm.viewangles[Defines.PITCH] < 180.0F)
                pm.viewangles[Defines.PITCH] = 89.0F;
            else if (pm.viewangles[Defines.PITCH] < 271.0F && pm.viewangles[Defines.PITCH] >= 180.0F)
                pm.viewangles[Defines.PITCH] = 271.0F;
        }
        Math3D.AngleVectors(pm.viewangles, pml.forward, pml.right, pml.up);
    }

    /**
     * Can be called by either the server or the client.
     */
    public static void Pmove(pmove_t pmove) {
        pm = pmove;

        
        pm.numtouch = 0;
        Math3D.VectorClear(pm.viewangles);
        pm.viewheight = (float) 0;
        pm.groundentity = null;
        pm.watertype = 0;
        pm.waterlevel = 0;

        pml.groundsurface = null;
        pml.groundcontents = 0;

        
        pml.origin[0] = (float) pm.s.origin[0] * 0.125f;
        pml.origin[1] = (float) pm.s.origin[1] * 0.125f;
        pml.origin[2] = (float) pm.s.origin[2] * 0.125f;

        pml.velocity[0] = (float) pm.s.velocity[0] * 0.125f;
        pml.velocity[1] = (float) pm.s.velocity[1] * 0.125f;
        pml.velocity[2] = (float) pm.s.velocity[2] * 0.125f;

        
        Math3D.VectorCopy(pm.s.origin, pml.previous_origin);

        pml.frametime = (float) ((int) pm.cmd.msec & 0xFF) * 0.001f;

        PM_ClampAngles();

        if (pm.s.pm_type == Defines.PM_SPECTATOR) {
            PM_FlyMove(false);
            PM_SnapPosition();
            return;
        }

        if (pm.s.pm_type >= Defines.PM_DEAD) {
            pm.cmd.forwardmove = (short) 0;
            pm.cmd.sidemove = (short) 0;
            pm.cmd.upmove = (short) 0;
        }

        if (pm.s.pm_type == Defines.PM_FREEZE)
            return; 

        
        PM_CheckDuck();

        if (pm.snapinitial)
            PM_InitialSnapPosition();

        
        PM_CatagorizePosition();

        if (pm.s.pm_type == Defines.PM_DEAD)
            PM_DeadMove();

        PM_CheckSpecialMovement();

        
        if ((int) pm.s.pm_time != 0) {


            int msec = (int) pm.cmd.msec >>> 3;
            if (msec == 0)
                msec = 1;
            if (msec >= ((int) pm.s.pm_time & 0xFF)) {
                pm.s.pm_flags = (byte) ((int) pm.s.pm_flags & ~(pmove_t.PMF_TIME_WATERJUMP
                        | pmove_t.PMF_TIME_LAND | pmove_t.PMF_TIME_TELEPORT));
                pm.s.pm_time = (byte) 0;
            } else
                pm.s.pm_time = (byte) (((int) pm.s.pm_time & 0xFF) - msec);
        }

        if (((int) pm.s.pm_flags & pmove_t.PMF_TIME_TELEPORT) != 0) {
        	
        } else if (((int) pm.s.pm_flags & pmove_t.PMF_TIME_WATERJUMP) != 0) {
        	
            pml.velocity[2] -= (float) pm.s.gravity * pml.frametime;
            if (pml.velocity[2] < (float) 0) {

                pm.s.pm_flags = (byte) ((int) pm.s.pm_flags & ~(pmove_t.PMF_TIME_WATERJUMP
                        | pmove_t.PMF_TIME_LAND | pmove_t.PMF_TIME_TELEPORT));
                pm.s.pm_time = (byte) 0;
            }

            PM_StepSlideMove();
        } else {
            PM_CheckJump();

            PM_Friction();

            if (pm.waterlevel >= 2)
                PM_WaterMove();
            else {
                float[] angles = {(float) 0, (float) 0, (float) 0};

                Math3D.VectorCopy(pm.viewangles, angles);
                
                if (angles[Defines.PITCH] > 180.0F)
                    angles[Defines.PITCH] -= 360.0F;
                
                angles[Defines.PITCH] /= 3.0F;

                Math3D.AngleVectors(angles, pml.forward, pml.right, pml.up);

                PM_AirMove();
            }
        }

        
        PM_CatagorizePosition();
        PM_SnapPosition();
    }
}