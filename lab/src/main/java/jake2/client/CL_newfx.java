/*
 * CL_newfx.java
 * Copyright (C) 2004
 * 
 * $Id: CL_newfx.java,v 1.7 2005-01-17 21:50:42 cawe Exp $
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
import jake2.util.Lib;
import jake2.util.Math3D;

/**
 * CL_newfx
 */
public class CL_newfx {

    static void Flashlight(int ent, float[] pos) {

        CL_fx.cdlight_t dl = CL_fx.AllocDlight(ent);
        Math3D.VectorCopy(pos, dl.origin);
        dl.radius = 400.0F;
        dl.minlight = 250.0F;
        dl.die = (float) (Globals.cl.time + 100);
        dl.color[0] = 1.0F;
        dl.color[1] = 1.0F;
        dl.color[2] = 1.0F;
    }

    /*
     * ====== CL_ColorFlash - flash of light ======
     */
    static void ColorFlash(float[] pos, int ent, int intensity, float r,
            float g, float b) {

        if ((Globals.vidref_val == Defines.VIDREF_SOFT)
                && ((r < (float) 0) || (g < (float) 0) || (b < (float) 0))) {
            intensity = -intensity;
            r = -r;
            g = -g;
            b = -b;
        }

        CL_fx.cdlight_t dl = CL_fx.AllocDlight(ent);
        Math3D.VectorCopy(pos, dl.origin);
        dl.radius = (float) intensity;
        dl.minlight = 250.0F;
        dl.die = (float) (Globals.cl.time + 100);
        dl.color[0] = r;
        dl.color[1] = g;
        dl.color[2] = b;
    }

  	
  	private static final float[] move = {(float) 0, (float) 0, (float) 0};
  	private static final float[] vec = {(float) 0, (float) 0, (float) 0};
  	private static final float[] right = {(float) 0, (float) 0, (float) 0};
  	private static final float[] up = {(float) 0, (float) 0, (float) 0};
    /*
     * ====== CL_DebugTrail ======
     */
    static void DebugTrail(float[] start, float[] end) {


        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        Math3D.MakeNormalVectors(vec, right, up);


        float dec = 3.0F;
        Math3D.VectorScale(vec, dec, vec);
        Math3D.VectorCopy(start, move);

        while (len > (float) 0) {
            len -= dec;

            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;

            p.time = (float) Globals.cl.time;
            Math3D.VectorClear(p.accel);
            Math3D.VectorClear(p.vel);
            p.alpha = 1.0f;
            p.alphavel = -0.1f;
            
            p.color = (float) (0x74 + ((int) Lib.rand() & 7));
            Math3D.VectorCopy(move, p.org);
            /*
             * for (j=0 ; j <3 ; j++) { p.org[j] = move[j] + crand()*2; p.vel[j] =
             * crand()*3; p.accel[j] = 0; }
             */
            Math3D.VectorAdd(move, vec, move);
        }

    }

  	
    
    static void ForceWall(float[] start, float[] end, int color) {

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        Math3D.VectorScale(vec, 4.0F, vec);

        
        while (len > (float) 0) {
            len -= 4.0F;

            if (CL_fx.free_particles == null)
                return;

            if ((double) Globals.rnd.nextFloat() > 0.3) {
                cparticle_t p = CL_fx.free_particles;
                CL_fx.free_particles = p.next;
                p.next = CL_fx.active_particles;
                CL_fx.active_particles = p;
                Math3D.VectorClear(p.accel);

                p.time = (float) Globals.cl.time;

                p.alpha = 1.0f;
                p.alphavel = -1.0f / (3.0f + Globals.rnd.nextFloat() * 0.5f);
                p.color = (float) color;
                for (int j = 0; j < 3; j++) {
                    p.org[j] = move[j] + Lib.crand() * 3.0F;
                    p.accel[j] = (float) 0;
                }
                p.vel[0] = (float) 0;
                p.vel[1] = (float) 0;
                p.vel[2] = -40.0F - (Lib.crand() * 10.0F);
            }

            Math3D.VectorAdd(move, vec, move);
        }
    }

  	
    
    /*
     * =============== CL_BubbleTrail2 (lets you control the # of bubbles by
     * setting the distance between the spawns)
     * 
     * ===============
     */
    static void BubbleTrail2(float[] start, float[] end, int dist) {

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        float dec = (float) dist;
        Math3D.VectorScale(vec, dec, vec);

        for (int i = 0; (float) i < len; i = (int) ((float) i + dec)) {
            if (CL_fx.free_particles == null)
                return;

            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;

            Math3D.VectorClear(p.accel);
            p.time = (float) Globals.cl.time;

            p.alpha = 1.0f;
            p.alphavel = -1.0f / (1.0F + Globals.rnd.nextFloat() * 0.1f);
            p.color = (float) (4 + ((int) Lib.rand() & 7));
            for (int j = 0; j < 3; j++) {
                p.org[j] = move[j] + Lib.crand() * 2.0F;
                p.vel[j] = Lib.crand() * 10.0F;
            }
            p.org[2] -= 4.0F;
            
            p.vel[2] += 20.0F;

            Math3D.VectorAdd(move, vec, move);
        }
    }

  	
    
    private static final float[] dir = {(float) 0, (float) 0, (float) 0};
    private static final float[] end = {(float) 0, (float) 0, (float) 0};
    
    static void Heatbeam(float[] start, float[] forward) {

        Math3D.VectorMA(start, 4096.0F, forward, end);

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        
        
        Math3D.VectorCopy(Globals.cl.v_right, right);
        Math3D.VectorCopy(Globals.cl.v_up, up);
        if (Globals.vidref_val == Defines.VIDREF_GL) { 
            Math3D.VectorMA(move, -0.5f, right, move);
            Math3D.VectorMA(move, -0.5f, up, move);
        }


        float ltime = (float) Globals.cl.time / 1000.0f;
        float step = 32.0f;
        float start_pt = ltime * 96.0f % step;
        Math3D.VectorMA(move, start_pt, vec, move);

        Math3D.VectorScale(vec, step, vec);


        float rstep = (float) (Math.PI / 10.0);
        float M_PI2 = (float) (Math.PI * 2.0);
        for (int i = (int) start_pt; (float) i < len; i = (int) ((float) i + step)) {
            if ((float) i > step * 5.0F)
                break;

            for (float rot = (float) 0; rot < M_PI2; rot += rstep) {

                if (CL_fx.free_particles == null)
                    return;

                cparticle_t p = CL_fx.free_particles;
                CL_fx.free_particles = p.next;
                p.next = CL_fx.active_particles;
                CL_fx.active_particles = p;

                p.time = (float) Globals.cl.time;
                Math3D.VectorClear(p.accel);


                float variance = 0.5f;
                float c = (float) (Math.cos((double) rot) * (double) variance);
                float s = (float) (Math.sin((double) rot) * (double) variance);


                if (i < 10) {
                    Math3D.VectorScale(right, c * ((float) i / 10.0f), dir);
                    Math3D.VectorMA(dir, s * ((float) i / 10.0f), up, dir);
                } else {
                    Math3D.VectorScale(right, c, dir);
                    Math3D.VectorMA(dir, s, up, dir);
                }

                p.alpha = 0.5f;
                
                p.alphavel = -1000.0f;
                
                p.color = (float) (223 - ((int) Lib.rand() & 7));
                for (int j = 0; j < 3; j++) {
                    p.org[j] = move[j] + dir[j] * 3.0F;
                    
                    p.vel[j] = (float) 0;
                }
            }
            Math3D.VectorAdd(move, vec, move);
        }
    }

  	
    private static final float[] r = {(float) 0, (float) 0, (float) 0};
    private static final float[] u = {(float) 0, (float) 0, (float) 0};
    /*
     * =============== CL_ParticleSteamEffect
     * 
     * Puffs with velocity along direction, with some randomness thrown in
     * ===============
     */
    static void ParticleSteamEffect(float[] org, float[] dir, int color,
            int count, int magnitude) {


        Math3D.MakeNormalVectors(dir, r, u);

        for (int i = 0; i < count; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;

            p.time = (float) Globals.cl.time;
            p.color = (float) (color + ((int) Lib.rand() & 7));

            for (int j = 0; j < 3; j++) {
                p.org[j] = org[j] + (float) magnitude * 0.1f * Lib.crand();
                
            }
            Math3D.VectorScale(dir, (float) magnitude, p.vel);
            float d = Lib.crand() * (float) magnitude / 3.0F;
            Math3D.VectorMA(p.vel, d, r, p.vel);
            d = Lib.crand() * (float) magnitude / 3.0F;
            Math3D.VectorMA(p.vel, d, u, p.vel);

            p.accel[0] = p.accel[1] = (float) 0;
            p.accel[2] = (float) -CL_fx.PARTICLE_GRAVITY / 2f;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
        }
    }

  	
    
    static void ParticleSteamEffect2(cl_sustain_t self)
    
    {


        Math3D.VectorCopy(self.dir, dir);
        Math3D.MakeNormalVectors(dir, r, u);

        for (int i = 0; i < self.count; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;

            p.time = (float) Globals.cl.time;
            p.color = (float) (self.color + ((int) Lib.rand() & 7));

            for (int j = 0; j < 3; j++) {
                p.org[j] = self.org[j] + (float) self.magnitude * 0.1f * Lib.crand();
                
            }
            Math3D.VectorScale(dir, (float) self.magnitude, p.vel);
            float d = Lib.crand() * (float) self.magnitude / 3.0F;
            Math3D.VectorMA(p.vel, d, r, p.vel);
            d = Lib.crand() * (float) self.magnitude / 3.0F;
            Math3D.VectorMA(p.vel, d, u, p.vel);

            p.accel[0] = p.accel[1] = (float) 0;
            p.accel[2] = (float) -CL_fx.PARTICLE_GRAVITY / 2f;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
        }
        self.nextthink += self.thinkinterval;
    }

  	
    
    private static final float[] forward = {(float) 0, (float) 0, (float) 0};
    private static final float[] angle_dir = {(float) 0, (float) 0, (float) 0};
    /*
     * =============== CL_TrackerTrail ===============
     */
    static void TrackerTrail(float[] start, float[] end, int particleColor) {

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        Math3D.VectorCopy(vec, forward);
        Math3D.vectoangles(forward, angle_dir);
        Math3D.AngleVectors(angle_dir, forward, right, up);

        Math3D.VectorScale(vec, 3.0F, vec);


        int dec = 3;
        while (len > (float) 0) {
            len = len - (float) dec;

            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = (float) Globals.cl.time;

            p.alpha = 1.0f;
            p.alphavel = -2.0f;
            p.color = (float) particleColor;
            float dist = Math3D.DotProduct(move, forward);
            Math3D.VectorMA(move, (float) (8.0 * Math.cos((double) dist)), up, p.org);
            for (int j = 0; j < 3; j++) {
                p.vel[j] = (float) 0;
                p.accel[j] = (float) 0;
            }
            p.vel[2] = 5.0F;

            Math3D.VectorAdd(move, vec, move);
        }
    }

    
    
    static void Tracker_Shell(float[] origin) {

        for (int i = 0; i < 300; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = (float) Globals.cl.time;

            p.alpha = 1.0f;
            p.alphavel = CL_fx.INSTANT_PARTICLE;
            p.color = (float) 0;

            dir[0] = Lib.crand();
            dir[1] = Lib.crand();
            dir[2] = Lib.crand();
            Math3D.VectorNormalize(dir);

            Math3D.VectorMA(origin, 40.0F, dir, p.org);
        }
    }

    
    
    static void MonsterPlasma_Shell(float[] origin) {

        for (int i = 0; i < 40; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = (float) Globals.cl.time;

            p.alpha = 1.0f;
            p.alphavel = CL_fx.INSTANT_PARTICLE;
            p.color = (float) 0xe0;

            dir[0] = Lib.crand();
            dir[1] = Lib.crand();
            dir[2] = Lib.crand();
            Math3D.VectorNormalize(dir);

            Math3D.VectorMA(origin, 10.0F, dir, p.org);
            
            
        }
    }

    private static final int[] wb_colortable = { 2 * 8, 13 * 8, 21 * 8, 18 * 8 };

    
    
    static void Widowbeamout(cl_sustain_t self) {

        float ratio = 1.0f - (((float) self.endtime - (float) Globals.cl.time) / 2100.0f);

        for (int i = 0; i < 300; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = (float) Globals.cl.time;

            p.alpha = 1.0f;
            p.alphavel = CL_fx.INSTANT_PARTICLE;
            p.color = (float) wb_colortable[(int) Lib.rand() & 3];

            dir[0] = Lib.crand();
            dir[1] = Lib.crand();
            dir[2] = Lib.crand();
            Math3D.VectorNormalize(dir);

            Math3D.VectorMA(self.org, (45.0f * ratio), dir, p.org);
            
            
        }
    }

    private static final int[] nb_colortable = { 110, 112, 114, 116 };

    
    
    static void Nukeblast(cl_sustain_t self) {

        float ratio = 1.0f - (((float) self.endtime - (float) Globals.cl.time) / 1000.0f);

        for (int i = 0; i < 700; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = (float) Globals.cl.time;

            p.alpha = 1.0f;
            p.alphavel = CL_fx.INSTANT_PARTICLE;
            p.color = (float) nb_colortable[(int) Lib.rand() & 3];

            dir[0] = Lib.crand();
            dir[1] = Lib.crand();
            dir[2] = Lib.crand();
            Math3D.VectorNormalize(dir);

            Math3D.VectorMA(self.org, (200.0f * ratio), dir, p.org);
            
            
        }
    }

    private static final int[] ws_colortable = { 2 * 8, 13 * 8, 21 * 8, 18 * 8 };

    
    
    static void WidowSplash(float[] org) {

        for (int i = 0; i < 256; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;

            p.time = (float) Globals.cl.time;
            p.color = (float) ws_colortable[(int) Lib.rand() & 3];

            dir[0] = Lib.crand();
            dir[1] = Lib.crand();
            dir[2] = Lib.crand();
            Math3D.VectorNormalize(dir);
            Math3D.VectorMA(org, 45.0f, dir, p.org);
            Math3D.VectorMA(Globals.vec3_origin, 40.0f, dir, p.vel);

            p.accel[0] = p.accel[1] = (float) 0;
            p.alpha = 1.0f;

            p.alphavel = -0.8f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
        }

    }

    
    
    /*
     * ===============
     *  CL_TagTrail
     * ===============
     */
    static void TagTrail(float[] start, float[] end, float color) {

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        Math3D.VectorScale(vec, 5.0F, vec);

        int dec = 5;
        while (len >= (float) 0) {
            len = len - (float) dec;

            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = (float) Globals.cl.time;

            p.alpha = 1.0f;
            p.alphavel = -1.0f / (0.8f + Globals.rnd.nextFloat() * 0.2f);
            p.color = color;
            for (int j = 0; j < 3; j++) {
                p.org[j] = move[j] + Lib.crand() * 16.0F;
                p.vel[j] = Lib.crand() * 5.0F;
                p.accel[j] = (float) 0;
            }

            Math3D.VectorAdd(move, vec, move);
        }
    }

    /*
     * =============== CL_ColorExplosionParticles ===============
     */
    static void ColorExplosionParticles(float[] org, int color, int run) {

        for (int i = 0; i < 128; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;

            p.time = (float) Globals.cl.time;
            p.color = (float) (color + ((int) Lib.rand() % run));

            for (int j = 0; j < 3; j++) {
                p.org[j] = org[j] + (float) (((int) Lib.rand() % 32) - 16);
                p.vel[j] = (float) (((int) Lib.rand() % 256) - 128);
            }

            p.accel[0] = p.accel[1] = (float) 0;
            p.accel[2] = (float) -CL_fx.PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -0.4f / (0.6f + Globals.rnd.nextFloat() * 0.2f);
        }
    }

    
    
    /*
     * =============== CL_ParticleSmokeEffect - like the steam effect, but
     * unaffected by gravity ===============
     */
    static void ParticleSmokeEffect(float[] org, float[] dir, int color,
            int count, int magnitude) {

        Math3D.MakeNormalVectors(dir, r, u);

        for (int i = 0; i < count; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;

            p.time = (float) Globals.cl.time;
            p.color = (float) (color + ((int) Lib.rand() & 7));

            for (int j = 0; j < 3; j++) {
                p.org[j] = org[j] + (float) magnitude * 0.1f * Lib.crand();
                
            }
            Math3D.VectorScale(dir, (float) magnitude, p.vel);
            float d = Lib.crand() * (float) magnitude / 3.0F;
            Math3D.VectorMA(p.vel, d, r, p.vel);
            d = Lib.crand() * (float) magnitude / 3.0F;
            Math3D.VectorMA(p.vel, d, u, p.vel);

            p.accel[0] = p.accel[1] = p.accel[2] = (float) 0;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
        }
    }

    /*
     * =============== CL_BlasterParticles2
     * 
     * Wall impact puffs (Green) ===============
     */
    static void BlasterParticles2(float[] org, float[] dir, long color) {

        int count = 40;
        for (int i = 0; i < count; i++) {
            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;

            p.time = (float) Globals.cl.time;
            p.color = (float) (color + (long) ((int) Lib.rand() & 7));

            float d = (float) (Lib.rand() & 15);
            for (int j = 0; j < 3; j++) {
                p.org[j] = org[j] + (float) (((int) Lib.rand() & 7) - 4) + d * dir[j];
                p.vel[j] = dir[j] * 30.0F + Lib.crand() * 40.0F;
            }

            p.accel[0] = p.accel[1] = (float) 0;
            p.accel[2] = (float) -CL_fx.PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
        }
    }

    
    
    /*
     * =============== CL_BlasterTrail2
     * 
     * Green! ===============
     */
    static void BlasterTrail2(float[] start, float[] end) {

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        Math3D.VectorScale(vec, 5.0F, vec);


        int dec = 5;
        while (len > (float) 0) {
            len = len - (float) dec;

            if (CL_fx.free_particles == null)
                return;
            cparticle_t p = CL_fx.free_particles;
            CL_fx.free_particles = p.next;
            p.next = CL_fx.active_particles;
            CL_fx.active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = (float) Globals.cl.time;

            p.alpha = 1.0f;
            p.alphavel = -1.0f / (0.3f + Globals.rnd.nextFloat() * 0.2f);
            p.color = (float) 0xd0;
            for (int j = 0; j < 3; j++) {
                p.org[j] = move[j] + Lib.crand();
                p.vel[j] = Lib.crand() * 5.0F;
                p.accel[j] = (float) 0;
            }

            Math3D.VectorAdd(move, vec, move);
        }
    }
}