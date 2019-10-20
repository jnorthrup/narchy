/*
 * java
 * Copyright (C) 2004
 * 
 * $Id: CL_fx.java,v 1.9 2005-02-06 19:18:10 salomo Exp $
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
import jake2.game.entity_state_t;
import jake2.game.monsters.M_Flash;
import jake2.qcommon.Com;
import jake2.qcommon.MSG;
import jake2.sound.S;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.util.Arrays;

/**
 * Client Graphics Effects.
 */
public class CL_fx {

	static class cdlight_t {
		int key; 

		final float[] color = {(float) 0, (float) 0, (float) 0};

		final float[] origin = {(float) 0, (float) 0, (float) 0};

		float radius;

		float die; 

		float minlight; 

		void clear() {
			radius = minlight = color[0] = color[1] = color[2] = (float) 0;
		}
	}

	static final cparticle_t[] particles = new cparticle_t[Defines.MAX_PARTICLES];
	static {
		for (int i = 0; i < particles.length; i++)
			particles[i] = new cparticle_t();
	}

	static int cl_numparticles = Defines.MAX_PARTICLES;

	static final float INSTANT_PARTICLE = -10000.0f;

	static final float[][] avelocities = new float[Defines.NUMVERTEXNORMALS][3];

	static final clightstyle_t[] cl_lightstyle = new clightstyle_t[Defines.MAX_LIGHTSTYLES];

	static int lastofs;

	/*
	 * ==============================================================
	 * 
	 * LIGHT STYLE MANAGEMENT
	 * 
	 * ==============================================================
	 */

	static class clightstyle_t {
		int length;

		final float[] value = new float[3];

		final float[] map = new float[Defines.MAX_QPATH];

		void clear() {
			value[0] = value[1] = value[2] =  length = 0;
            Arrays.fill(map, 0.0f);
		}
	}

	static {
		for (int i = 0; i < cl_lightstyle.length; i++) {
			cl_lightstyle[i] = new clightstyle_t();
		}
	}

	/*
	 * ==============================================================
	 * 
	 * DLIGHT MANAGEMENT
	 * 
	 * ==============================================================
	 */

	static final cdlight_t[] cl_dlights = new cdlight_t[Defines.MAX_DLIGHTS];
	static {
		for (int i = 0; i < cl_dlights.length; i++)
			cl_dlights[i] = new cdlight_t();
	}

	/*
	 * ================ CL_ClearDlights ================
	 */
	static void ClearDlights() {
		
		for (cdlight_t cl_dlight : cl_dlights) {
			cl_dlight.clear();
		}
	}

	/*
	 * ================ CL_ClearLightStyles ================
	 */
	static void ClearLightStyles() {
		
		for (clightstyle_t aCl_lightstyle : cl_lightstyle) aCl_lightstyle.clear();
		lastofs = -1;
	}

	/*
	 * ================ CL_RunLightStyles ================
	 */
	static void RunLightStyles() {

        int ofs = Globals.cl.time / 100;
		if (ofs == lastofs)
			return;
		lastofs = ofs;

		for (clightstyle_t aCl_lightstyle : cl_lightstyle) {
            clightstyle_t ls = aCl_lightstyle;
			if (ls.length == 0) {
				ls.value[0] = ls.value[1] = ls.value[2] = 1.0f;
				continue;
			}
			if (ls.length == 1)
				ls.value[0] = ls.value[1] = ls.value[2] = ls.map[0];
			else
				ls.value[0] = ls.value[1] = ls.value[2] = ls.map[ofs % ls.length];
		}
	}

	static void SetLightstyle(int i) {

        String s = Globals.cl.configstrings[i + Defines.CS_LIGHTS];

        int j = s.length();
		if (j >= Defines.MAX_QPATH)
			Com.Error(Defines.ERR_DROP, "svc_lightstyle length=" + j);

		cl_lightstyle[i].length = j;

		for (int k = 0; k < j; k++)
			cl_lightstyle[i].map[k] = (float) ((int) s.charAt(k) - (int) 'a') / (float) ((int) 'm' - (int) 'a');
	}

	/*
	 * ================ CL_AddLightStyles ================
	 */
	static void AddLightStyles() {

		for (int i = 0; i < cl_lightstyle.length; i++) {
            clightstyle_t ls = cl_lightstyle[i];
			V.AddLightStyle(i, ls.value[0], ls.value[1], ls.value[2]);
		}
	}

	/*
	 * =============== CL_AllocDlight
	 * 
	 * ===============
	 */
	static cdlight_t AllocDlight(int key) {
		int i;
		cdlight_t dl;

		
		if (key != 0) {
			for (i = 0; i < Defines.MAX_DLIGHTS; i++) {
				dl = cl_dlights[i];
				if (dl.key == key) {
					
					dl.clear();
					dl.key = key;
					return dl;
				}
			}
		}

		
		for (i = 0; i < Defines.MAX_DLIGHTS; i++) {
			dl = cl_dlights[i];
			if (dl.die < (float) Globals.cl.time) {
				
				dl.clear();
				dl.key = key;
				return dl;
			}
		}

		
		
		dl = cl_dlights[0];
		dl.clear();
		dl.key = key;
		return dl;
	}


	/*
	 * =============== 
	 * CL_RunDLights
	 * ===============
	 */
	static void RunDLights() {

		for (int i = 0; i < Defines.MAX_DLIGHTS; i++) {
            cdlight_t dl = cl_dlights[i];
			if (dl.radius == 0.0f)
				continue;

			if (dl.die < (float) Globals.cl.time) {
				dl.radius = 0.0f;
				return;
			}
		}
	}

	
	private static final float[] fv = {(float) 0, (float) 0, (float) 0};
	private static final float[] rv = {(float) 0, (float) 0, (float) 0};
	/*
	 * ==============
	 *  CL_ParseMuzzleFlash
	 * ==============
	 */
	static void ParseMuzzleFlash() {

		int i = (int) MSG.ReadShort(Globals.net_message);
		if (i < 1 || i >= Defines.MAX_EDICTS)
			Com.Error(Defines.ERR_DROP, "CL_ParseMuzzleFlash: bad entity");

        int weapon = MSG.ReadByte(Globals.net_message);
        int silenced = weapon & Defines.MZ_SILENCED;
		weapon &= ~Defines.MZ_SILENCED;

        centity_t pl = Globals.cl_entities[i];

        cdlight_t dl = AllocDlight(i);
		Math3D.VectorCopy(pl.current.origin, dl.origin);
		Math3D.AngleVectors(pl.current.angles, fv, rv, null);
		Math3D.VectorMA(dl.origin, 18.0F, fv, dl.origin);
		Math3D.VectorMA(dl.origin, 16.0F, rv, dl.origin);
		if (silenced != 0)
			dl.radius = (float) (100 + (Globals.rnd.nextInt() & 31));
		else
			dl.radius = (float) (200 + (Globals.rnd.nextInt() & 31));
		dl.minlight = 32.0F;
		dl.die = (float) Globals.cl.time;

		float volume;
		if (silenced != 0)
			volume = 0.2f;
		else
			volume = 1.0F;

		String soundname;
		switch (weapon) {
		case Defines.MZ_BLASTER:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/blastf1a.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_BLUEHYPERBLASTER:
			dl.color[0] = (float) 0;
			dl.color[1] = (float) 0;
			dl.color[2] = 1.0F;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/hyprbf1a.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_HYPERBLASTER:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/hyprbf1a.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_MACHINEGUN:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			
			
			soundname = "weapons/machgf" + ((Globals.rnd.nextInt(5)) + 1) + "b.wav";
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound(soundname), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_SHOTGUN:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/shotgf1b.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			S.StartSound(null, i, Defines.CHAN_AUTO, S.RegisterSound("weapons/shotgr1b.wav"), volume, (float) Defines.ATTN_NORM, 0.1f);
			break;
		case Defines.MZ_SSHOTGUN:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/sshotf1b.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_CHAINGUN1:
			dl.radius = (float) (200 + (Globals.rnd.nextInt() & 31));
			dl.color[0] = 1.0F;
			dl.color[1] = 0.25f;
			dl.color[2] = (float) 0;
			
			
			soundname = "weapons/machgf" + ((Globals.rnd.nextInt(5)) + 1) + "b.wav";
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound(soundname), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_CHAINGUN2:
			dl.radius = (float) (225 + (Globals.rnd.nextInt() & 31));
			dl.color[0] = 1.0F;
			dl.color[1] = 0.5f;
			dl.color[2] = (float) 0;
			dl.die = (float) Globals.cl.time + 0.1f;
			
			
			soundname = "weapons/machgf" + ((Globals.rnd.nextInt(5)) + 1) + "b.wav";
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound(soundname), volume, (float) Defines.ATTN_NORM, (float) 0);
			
			
			soundname = "weapons/machgf" + ((Globals.rnd.nextInt(5)) + 1) + "b.wav";
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound(soundname), volume, (float) Defines.ATTN_NORM, 0.05f);
			break;
		case Defines.MZ_CHAINGUN3:
			dl.radius = (float) (250 + (Globals.rnd.nextInt() & 31));
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			dl.die = (float) Globals.cl.time + 0.1f;
			
			
			soundname = "weapons/machgf" + ((Globals.rnd.nextInt(5)) + 1) + "b.wav";
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound(soundname), volume, (float) Defines.ATTN_NORM, (float) 0);
			
			
			soundname = "weapons/machgf" + ((Globals.rnd.nextInt(5)) + 1) + "b.wav";
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound(soundname), volume, (float) Defines.ATTN_NORM, 0.033f);
			
			
			soundname = "weapons/machgf" + ((Globals.rnd.nextInt(5)) + 1) + "b.wav";
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound(soundname), volume, (float) Defines.ATTN_NORM, 0.066f);
			break;
		case Defines.MZ_RAILGUN:
			dl.color[0] = 0.5f;
			dl.color[1] = 0.5f;
			dl.color[2] = 1.0f;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/railgf1a.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_ROCKET:
			dl.color[0] = 1.0F;
			dl.color[1] = 0.5f;
			dl.color[2] = 0.2f;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/rocklf1a.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			S.StartSound(null, i, Defines.CHAN_AUTO, S.RegisterSound("weapons/rocklr1b.wav"), volume, (float) Defines.ATTN_NORM, 0.1f);
			break;
		case Defines.MZ_GRENADE:
			dl.color[0] = 1.0F;
			dl.color[1] = 0.5f;
			dl.color[2] = (float) 0;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/grenlf1a.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			S.StartSound(null, i, Defines.CHAN_AUTO, S.RegisterSound("weapons/grenlr1b.wav"), volume, (float) Defines.ATTN_NORM, 0.1f);
			break;
		case Defines.MZ_BFG:
			dl.color[0] = (float) 0;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/bfg__f1y.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ_LOGIN:
			dl.color[0] = (float) 0;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			dl.die = (float) Globals.cl.time + 1.0f;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/grenlf1a.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			LogoutEffect(pl.current.origin, weapon);
			break;
		case Defines.MZ_LOGOUT:
			dl.color[0] = 1.0F;
			dl.color[1] = (float) 0;
			dl.color[2] = (float) 0;
			dl.die = (float) Globals.cl.time + 1.0f;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/grenlf1a.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			LogoutEffect(pl.current.origin, weapon);
			break;
		case Defines.MZ_RESPAWN:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			dl.die = (float) Globals.cl.time + 1.0f;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/grenlf1a.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			LogoutEffect(pl.current.origin, weapon);
			break;
		
		case Defines.MZ_PHALANX:
			dl.color[0] = 1.0F;
			dl.color[1] = 0.5f;
			dl.color[2] = 0.5f;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/plasshot.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		
		case Defines.MZ_IONRIPPER:
			dl.color[0] = 1.0F;
			dl.color[1] = 0.5f;
			dl.color[2] = 0.5f;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/rippfire.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;

		
		
		case Defines.MZ_ETF_RIFLE:
			dl.color[0] = 0.9f;
			dl.color[1] = 0.7f;
			dl.color[2] = (float) 0;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/nail1.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_SHOTGUN2:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/shotg2.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_HEATBEAM:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			dl.die = (float) (Globals.cl.time + 100);
			
			
			break;
		case Defines.MZ_BLASTER2:
			dl.color[0] = (float) 0;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/blastf1a.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_TRACKER:
			
			dl.color[0] = -1.0F;
			dl.color[1] = -1.0F;
			dl.color[2] = -1.0F;
			S.StartSound(null, i, Defines.CHAN_WEAPON, S.RegisterSound("weapons/disint2.wav"), volume, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.MZ_NUKE1:
			dl.color[0] = 1.0F;
			dl.color[1] = (float) 0;
			dl.color[2] = (float) 0;
			dl.die = (float) (Globals.cl.time + 100);
			break;
		case Defines.MZ_NUKE2:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			dl.die = (float) (Globals.cl.time + 100);
			break;
		case Defines.MZ_NUKE4:
			dl.color[0] = (float) 0;
			dl.color[1] = (float) 0;
			dl.color[2] = 1.0F;
			dl.die = (float) (Globals.cl.time + 100);
			break;
		case Defines.MZ_NUKE8:
			dl.color[0] = (float) 0;
			dl.color[1] = 1.0F;
			dl.color[2] = 1.0F;
			dl.die = (float) (Globals.cl.time + 100);
			break;
		
		
		}
	}

	
	private static final float[] origin = {(float) 0, (float) 0, (float) 0};
	private static final float[] forward = {(float) 0, (float) 0, (float) 0};
	private static final float[] right = {(float) 0, (float) 0, (float) 0};
	/*
	 * ============== CL_ParseMuzzleFlash2 ==============
	 */
	static void ParseMuzzleFlash2() {

		int ent = (int) MSG.ReadShort(Globals.net_message);
		if (ent < 1 || ent >= Defines.MAX_EDICTS)
			Com.Error(Defines.ERR_DROP, "CL_ParseMuzzleFlash2: bad entity");

        int flash_number = MSG.ReadByte(Globals.net_message);

		
		Math3D.AngleVectors(Globals.cl_entities[ent].current.angles, forward, right, null);
		origin[0] = Globals.cl_entities[ent].current.origin[0] + forward[0] * M_Flash.monster_flash_offset[flash_number][0] + right[0]
				* M_Flash.monster_flash_offset[flash_number][1];
		origin[1] = Globals.cl_entities[ent].current.origin[1] + forward[1] * M_Flash.monster_flash_offset[flash_number][0] + right[1]
				* M_Flash.monster_flash_offset[flash_number][1];
		origin[2] = Globals.cl_entities[ent].current.origin[2] + forward[2] * M_Flash.monster_flash_offset[flash_number][0] + right[2]
				* M_Flash.monster_flash_offset[flash_number][1] + M_Flash.monster_flash_offset[flash_number][2];

        cdlight_t dl = AllocDlight(ent);
		Math3D.VectorCopy(origin, dl.origin);
		dl.radius = (float) (200 + (Globals.rnd.nextInt() & 31));
		dl.minlight = 32.0F;
		dl.die = (float) Globals.cl.time;

		String soundname;
		switch (flash_number) {
		case Defines.MZ2_INFANTRY_MACHINEGUN_1:
		case Defines.MZ2_INFANTRY_MACHINEGUN_2:
		case Defines.MZ2_INFANTRY_MACHINEGUN_3:
		case Defines.MZ2_INFANTRY_MACHINEGUN_4:
		case Defines.MZ2_INFANTRY_MACHINEGUN_5:
		case Defines.MZ2_INFANTRY_MACHINEGUN_6:
		case Defines.MZ2_INFANTRY_MACHINEGUN_7:
		case Defines.MZ2_INFANTRY_MACHINEGUN_8:
		case Defines.MZ2_INFANTRY_MACHINEGUN_9:
		case Defines.MZ2_INFANTRY_MACHINEGUN_10:
		case Defines.MZ2_INFANTRY_MACHINEGUN_11:
		case Defines.MZ2_INFANTRY_MACHINEGUN_12:
		case Defines.MZ2_INFANTRY_MACHINEGUN_13:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			ParticleEffect(origin, Globals.vec3_origin, 0, 40);
			CL_tent.SmokeAndFlash(origin);
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("infantry/infatck1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_SOLDIER_MACHINEGUN_1:
		case Defines.MZ2_SOLDIER_MACHINEGUN_2:
		case Defines.MZ2_SOLDIER_MACHINEGUN_3:
		case Defines.MZ2_SOLDIER_MACHINEGUN_4:
		case Defines.MZ2_SOLDIER_MACHINEGUN_5:
		case Defines.MZ2_SOLDIER_MACHINEGUN_6:
		case Defines.MZ2_SOLDIER_MACHINEGUN_7:
		case Defines.MZ2_SOLDIER_MACHINEGUN_8:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			ParticleEffect(origin, Globals.vec3_origin, 0, 40);
			CL_tent.SmokeAndFlash(origin);
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("soldier/solatck3.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_GUNNER_MACHINEGUN_1:
		case Defines.MZ2_GUNNER_MACHINEGUN_2:
		case Defines.MZ2_GUNNER_MACHINEGUN_3:
		case Defines.MZ2_GUNNER_MACHINEGUN_4:
		case Defines.MZ2_GUNNER_MACHINEGUN_5:
		case Defines.MZ2_GUNNER_MACHINEGUN_6:
		case Defines.MZ2_GUNNER_MACHINEGUN_7:
		case Defines.MZ2_GUNNER_MACHINEGUN_8:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			ParticleEffect(origin, Globals.vec3_origin, 0, 40);
			CL_tent.SmokeAndFlash(origin);
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("gunner/gunatck2.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_ACTOR_MACHINEGUN_1:
		case Defines.MZ2_SUPERTANK_MACHINEGUN_1:
		case Defines.MZ2_SUPERTANK_MACHINEGUN_2:
		case Defines.MZ2_SUPERTANK_MACHINEGUN_3:
		case Defines.MZ2_SUPERTANK_MACHINEGUN_4:
		case Defines.MZ2_SUPERTANK_MACHINEGUN_5:
		case Defines.MZ2_SUPERTANK_MACHINEGUN_6:
		case Defines.MZ2_TURRET_MACHINEGUN: 
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;

			ParticleEffect(origin, Globals.vec3_origin, 0, 40);
			CL_tent.SmokeAndFlash(origin);
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("infantry/infatck1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_BOSS2_MACHINEGUN_L1:
		case Defines.MZ2_BOSS2_MACHINEGUN_L2:
		case Defines.MZ2_BOSS2_MACHINEGUN_L3:
		case Defines.MZ2_BOSS2_MACHINEGUN_L4:
		case Defines.MZ2_BOSS2_MACHINEGUN_L5:
		case Defines.MZ2_CARRIER_MACHINEGUN_L1: 
		case Defines.MZ2_CARRIER_MACHINEGUN_L2: 
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;

			ParticleEffect(origin, Globals.vec3_origin, 0, 40);
			CL_tent.SmokeAndFlash(origin);
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("infantry/infatck1.wav"), 1.0F, (float) Defines.ATTN_NONE, (float) 0);
			break;

		case Defines.MZ2_SOLDIER_BLASTER_1:
		case Defines.MZ2_SOLDIER_BLASTER_2:
		case Defines.MZ2_SOLDIER_BLASTER_3:
		case Defines.MZ2_SOLDIER_BLASTER_4:
		case Defines.MZ2_SOLDIER_BLASTER_5:
		case Defines.MZ2_SOLDIER_BLASTER_6:
		case Defines.MZ2_SOLDIER_BLASTER_7:
		case Defines.MZ2_SOLDIER_BLASTER_8:
		case Defines.MZ2_TURRET_BLASTER: 
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("soldier/solatck2.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_FLYER_BLASTER_1:
		case Defines.MZ2_FLYER_BLASTER_2:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("flyer/flyatck3.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_MEDIC_BLASTER_1:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("medic/medatck1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_HOVER_BLASTER_1:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("hover/hovatck1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_FLOAT_BLASTER_1:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("floater/fltatck1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_SOLDIER_SHOTGUN_1:
		case Defines.MZ2_SOLDIER_SHOTGUN_2:
		case Defines.MZ2_SOLDIER_SHOTGUN_3:
		case Defines.MZ2_SOLDIER_SHOTGUN_4:
		case Defines.MZ2_SOLDIER_SHOTGUN_5:
		case Defines.MZ2_SOLDIER_SHOTGUN_6:
		case Defines.MZ2_SOLDIER_SHOTGUN_7:
		case Defines.MZ2_SOLDIER_SHOTGUN_8:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			CL_tent.SmokeAndFlash(origin);
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("soldier/solatck1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_TANK_BLASTER_1:
		case Defines.MZ2_TANK_BLASTER_2:
		case Defines.MZ2_TANK_BLASTER_3:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("tank/tnkatck3.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_TANK_MACHINEGUN_1:
		case Defines.MZ2_TANK_MACHINEGUN_2:
		case Defines.MZ2_TANK_MACHINEGUN_3:
		case Defines.MZ2_TANK_MACHINEGUN_4:
		case Defines.MZ2_TANK_MACHINEGUN_5:
		case Defines.MZ2_TANK_MACHINEGUN_6:
		case Defines.MZ2_TANK_MACHINEGUN_7:
		case Defines.MZ2_TANK_MACHINEGUN_8:
		case Defines.MZ2_TANK_MACHINEGUN_9:
		case Defines.MZ2_TANK_MACHINEGUN_10:
		case Defines.MZ2_TANK_MACHINEGUN_11:
		case Defines.MZ2_TANK_MACHINEGUN_12:
		case Defines.MZ2_TANK_MACHINEGUN_13:
		case Defines.MZ2_TANK_MACHINEGUN_14:
		case Defines.MZ2_TANK_MACHINEGUN_15:
		case Defines.MZ2_TANK_MACHINEGUN_16:
		case Defines.MZ2_TANK_MACHINEGUN_17:
		case Defines.MZ2_TANK_MACHINEGUN_18:
		case Defines.MZ2_TANK_MACHINEGUN_19:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			ParticleEffect(origin, Globals.vec3_origin, 0, 40);
			CL_tent.SmokeAndFlash(origin);
			
			
			soundname = "tank/tnkatk2" + (char) ((int) 'a' + Globals.rnd.nextInt(5)) + ".wav";
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound(soundname), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_CHICK_ROCKET_1:
		case Defines.MZ2_TURRET_ROCKET: 
			dl.color[0] = 1.0F;
			dl.color[1] = 0.5f;
			dl.color[2] = 0.2f;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("chick/chkatck2.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_TANK_ROCKET_1:
		case Defines.MZ2_TANK_ROCKET_2:
		case Defines.MZ2_TANK_ROCKET_3:
			dl.color[0] = 1.0F;
			dl.color[1] = 0.5f;
			dl.color[2] = 0.2f;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("tank/tnkatck1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_SUPERTANK_ROCKET_1:
		case Defines.MZ2_SUPERTANK_ROCKET_2:
		case Defines.MZ2_SUPERTANK_ROCKET_3:
		case Defines.MZ2_BOSS2_ROCKET_1:
		case Defines.MZ2_BOSS2_ROCKET_2:
		case Defines.MZ2_BOSS2_ROCKET_3:
		case Defines.MZ2_BOSS2_ROCKET_4:
		case Defines.MZ2_CARRIER_ROCKET_1:
			
			
			
			dl.color[0] = 1.0F;
			dl.color[1] = 0.5f;
			dl.color[2] = 0.2f;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("tank/rocket.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_GUNNER_GRENADE_1:
		case Defines.MZ2_GUNNER_GRENADE_2:
		case Defines.MZ2_GUNNER_GRENADE_3:
		case Defines.MZ2_GUNNER_GRENADE_4:
			dl.color[0] = 1.0F;
			dl.color[1] = 0.5f;
			dl.color[2] = (float) 0;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("gunner/gunatck3.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_GLADIATOR_RAILGUN_1:
		
		case Defines.MZ2_CARRIER_RAILGUN:
		case Defines.MZ2_WIDOW_RAIL:
			
			dl.color[0] = 0.5f;
			dl.color[1] = 0.5f;
			dl.color[2] = 1.0f;
			break;

		
		case Defines.MZ2_MAKRON_BFG:
			dl.color[0] = 0.5f;
			dl.color[1] = 1.0F;
			dl.color[2] = 0.5f;
			
			
			break;

		case Defines.MZ2_MAKRON_BLASTER_1:
		case Defines.MZ2_MAKRON_BLASTER_2:
		case Defines.MZ2_MAKRON_BLASTER_3:
		case Defines.MZ2_MAKRON_BLASTER_4:
		case Defines.MZ2_MAKRON_BLASTER_5:
		case Defines.MZ2_MAKRON_BLASTER_6:
		case Defines.MZ2_MAKRON_BLASTER_7:
		case Defines.MZ2_MAKRON_BLASTER_8:
		case Defines.MZ2_MAKRON_BLASTER_9:
		case Defines.MZ2_MAKRON_BLASTER_10:
		case Defines.MZ2_MAKRON_BLASTER_11:
		case Defines.MZ2_MAKRON_BLASTER_12:
		case Defines.MZ2_MAKRON_BLASTER_13:
		case Defines.MZ2_MAKRON_BLASTER_14:
		case Defines.MZ2_MAKRON_BLASTER_15:
		case Defines.MZ2_MAKRON_BLASTER_16:
		case Defines.MZ2_MAKRON_BLASTER_17:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("makron/blaster.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_JORG_MACHINEGUN_L1:
		case Defines.MZ2_JORG_MACHINEGUN_L2:
		case Defines.MZ2_JORG_MACHINEGUN_L3:
		case Defines.MZ2_JORG_MACHINEGUN_L4:
		case Defines.MZ2_JORG_MACHINEGUN_L5:
		case Defines.MZ2_JORG_MACHINEGUN_L6:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			ParticleEffect(origin, Globals.vec3_origin, 0, 40);
			CL_tent.SmokeAndFlash(origin);
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("boss3/xfire.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_JORG_MACHINEGUN_R1:
		case Defines.MZ2_JORG_MACHINEGUN_R2:
		case Defines.MZ2_JORG_MACHINEGUN_R3:
		case Defines.MZ2_JORG_MACHINEGUN_R4:
		case Defines.MZ2_JORG_MACHINEGUN_R5:
		case Defines.MZ2_JORG_MACHINEGUN_R6:
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			ParticleEffect(origin, Globals.vec3_origin, 0, 40);
			CL_tent.SmokeAndFlash(origin);
			break;

		case Defines.MZ2_JORG_BFG_1:
			dl.color[0] = 0.5f;
			dl.color[1] = 1.0F;
			dl.color[2] = 0.5f;
			break;

		case Defines.MZ2_BOSS2_MACHINEGUN_R1:
		case Defines.MZ2_BOSS2_MACHINEGUN_R2:
		case Defines.MZ2_BOSS2_MACHINEGUN_R3:
		case Defines.MZ2_BOSS2_MACHINEGUN_R4:
		case Defines.MZ2_BOSS2_MACHINEGUN_R5:
		case Defines.MZ2_CARRIER_MACHINEGUN_R1: 
		case Defines.MZ2_CARRIER_MACHINEGUN_R2: 

			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;

			ParticleEffect(origin, Globals.vec3_origin, 0, 40);
			CL_tent.SmokeAndFlash(origin);
			break;

		
		
		case Defines.MZ2_STALKER_BLASTER:
		case Defines.MZ2_DAEDALUS_BLASTER:
		case Defines.MZ2_MEDIC_BLASTER_2:
		case Defines.MZ2_WIDOW_BLASTER:
		case Defines.MZ2_WIDOW_BLASTER_SWEEP1:
		case Defines.MZ2_WIDOW_BLASTER_SWEEP2:
		case Defines.MZ2_WIDOW_BLASTER_SWEEP3:
		case Defines.MZ2_WIDOW_BLASTER_SWEEP4:
		case Defines.MZ2_WIDOW_BLASTER_SWEEP5:
		case Defines.MZ2_WIDOW_BLASTER_SWEEP6:
		case Defines.MZ2_WIDOW_BLASTER_SWEEP7:
		case Defines.MZ2_WIDOW_BLASTER_SWEEP8:
		case Defines.MZ2_WIDOW_BLASTER_SWEEP9:
		case Defines.MZ2_WIDOW_BLASTER_100:
		case Defines.MZ2_WIDOW_BLASTER_90:
		case Defines.MZ2_WIDOW_BLASTER_80:
		case Defines.MZ2_WIDOW_BLASTER_70:
		case Defines.MZ2_WIDOW_BLASTER_60:
		case Defines.MZ2_WIDOW_BLASTER_50:
		case Defines.MZ2_WIDOW_BLASTER_40:
		case Defines.MZ2_WIDOW_BLASTER_30:
		case Defines.MZ2_WIDOW_BLASTER_20:
		case Defines.MZ2_WIDOW_BLASTER_10:
		case Defines.MZ2_WIDOW_BLASTER_0:
		case Defines.MZ2_WIDOW_BLASTER_10L:
		case Defines.MZ2_WIDOW_BLASTER_20L:
		case Defines.MZ2_WIDOW_BLASTER_30L:
		case Defines.MZ2_WIDOW_BLASTER_40L:
		case Defines.MZ2_WIDOW_BLASTER_50L:
		case Defines.MZ2_WIDOW_BLASTER_60L:
		case Defines.MZ2_WIDOW_BLASTER_70L:
		case Defines.MZ2_WIDOW_RUN_1:
		case Defines.MZ2_WIDOW_RUN_2:
		case Defines.MZ2_WIDOW_RUN_3:
		case Defines.MZ2_WIDOW_RUN_4:
		case Defines.MZ2_WIDOW_RUN_5:
		case Defines.MZ2_WIDOW_RUN_6:
		case Defines.MZ2_WIDOW_RUN_7:
		case Defines.MZ2_WIDOW_RUN_8:
			dl.color[0] = (float) 0;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("tank/tnkatck3.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_WIDOW_DISRUPTOR:
			dl.color[0] = -1.0F;
			dl.color[1] = -1.0F;
			dl.color[2] = -1.0F;
			S.StartSound(null, ent, Defines.CHAN_WEAPON, S.RegisterSound("weapons/disint2.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;

		case Defines.MZ2_WIDOW_PLASMABEAM:
		case Defines.MZ2_WIDOW2_BEAMER_1:
		case Defines.MZ2_WIDOW2_BEAMER_2:
		case Defines.MZ2_WIDOW2_BEAMER_3:
		case Defines.MZ2_WIDOW2_BEAMER_4:
		case Defines.MZ2_WIDOW2_BEAMER_5:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_1:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_2:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_3:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_4:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_5:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_6:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_7:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_8:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_9:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_10:
		case Defines.MZ2_WIDOW2_BEAM_SWEEP_11:
			dl.radius = (float) (300 + (Globals.rnd.nextInt() & 100));
			dl.color[0] = 1.0F;
			dl.color[1] = 1.0F;
			dl.color[2] = (float) 0;
			dl.die = (float) (Globals.cl.time + 200);
			break;
		
		

		

		}
	}

	/*
	 * =============== CL_AddDLights
	 * 
	 * ===============
	 */
	static void AddDLights() {
		cdlight_t dl;

		
		
		if (Globals.vidref_val == Defines.VIDREF_GL) {
			for (int i = 0; i < Defines.MAX_DLIGHTS; i++) {
				dl = cl_dlights[i];
				if (dl.radius == 0.0f)
					continue;
				V.AddLight(dl.origin, dl.radius, dl.color[0], dl.color[1], dl.color[2]);
			}
		} else {
			for (int i = 0; i < Defines.MAX_DLIGHTS; i++) {
				dl = cl_dlights[i];
				if (dl.radius == 0.0f)
					continue;

				
				if ((dl.color[0] < (float) 0) || (dl.color[1] < (float) 0) || (dl.color[2] < (float) 0)) {
					dl.radius = -(dl.radius);
					dl.color[0] = 1.0F;
					dl.color[1] = 1.0F;
					dl.color[2] = 1.0F;
				}
				V.AddLight(dl.origin, dl.radius, dl.color[0], dl.color[1], dl.color[2]);
			}
		}
		
		
	}

	/*
	 * =============== CL_ClearParticles ===============
	 */
	static void ClearParticles() {
		free_particles = particles[0];
		active_particles = null;

		for (int i = 0; i < particles.length - 1; i++)
			particles[i].next = particles[i + 1];
		particles[particles.length - 1].next = null;
	}

	/*
	 * =============== CL_ParticleEffect
	 * 
	 * Wall impact puffs ===============
	 */
	static void ParticleEffect(float[] org, float[] dir, int color, int count) {

		for (int i = 0; i < count; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;
			p.color = (float) (color + ((int) Lib.rand() & 7));

			float d = (float) (Lib.rand() & 31);
			for (int j = 0; j < 3; j++) {
				p.org[j] = org[j] + (float) (((int) Lib.rand() & 7) - 4) + d * dir[j];
				p.vel[j] = Lib.crand() * 20.0F;
			}

			p.accel[0] = p.accel[1] = (float) 0;
			p.accel[2] = (float) -PARTICLE_GRAVITY;
			p.alpha = 1.0f;

			p.alphavel = -1.0f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
		}
	}

	/*
	 * =============== CL_ParticleEffect2 ===============
	 */
	static void ParticleEffect2(float[] org, float[] dir, int color, int count) {

		for (int i = 0; i < count; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;
			p.color = (float) color;

			float d = (float) (Lib.rand() & 7);
			for (int j = 0; j < 3; j++) {
				p.org[j] = org[j] + (float) (((int) Lib.rand() & 7) - 4) + d * dir[j];
				p.vel[j] = Lib.crand() * 20.0F;
			}

			p.accel[0] = p.accel[1] = (float) 0;
			p.accel[2] = (float) -PARTICLE_GRAVITY;
			p.alpha = 1.0f;

			p.alphavel = -1.0f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
		}
	}

	
	/*
	 * =============== CL_ParticleEffect3 ===============
	 */
	static void ParticleEffect3(float[] org, float[] dir, int color, int count) {

		for (int i = 0; i < count; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;
			p.color = (float) color;

			float d = (float) (Lib.rand() & 7);
			for (int j = 0; j < 3; j++) {
				p.org[j] = org[j] + (float) (((int) Lib.rand() & 7) - 4) + d * dir[j];
				p.vel[j] = Lib.crand() * 20.0F;
			}

			p.accel[0] = p.accel[1] = (float) 0;
			p.accel[2] = (float) PARTICLE_GRAVITY;
			p.alpha = 1.0f;

			p.alphavel = -1.0f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
		}
	}

	/*
	 * =============== CL_TeleporterParticles ===============
	 */
	static void TeleporterParticles(entity_state_t ent) {

		for (int i = 0; i < 8; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;
			p.color = (float) 0xdb;

			for (int j = 0; j < 2; j++) {
				p.org[j] = ent.origin[j] - 16.0F + (float) ((int) Lib.rand() & 31);
				p.vel[j] = Lib.crand() * 14.0F;
			}

			p.org[2] = ent.origin[2] - 8.0F + (float) ((int) Lib.rand() & 7);
			p.vel[2] = (float) (80 + ((int) Lib.rand() & 7));

			p.accel[0] = p.accel[1] = (float) 0;
			p.accel[2] = (float) -PARTICLE_GRAVITY;
			p.alpha = 1.0f;

			p.alphavel = -0.5f;
		}
	}

	/*
	 * =============== CL_LogoutEffect
	 * 
	 * ===============
	 */
	static void LogoutEffect(float[] org, int type) {

		for (int i = 0; i < 500; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;

            switch (type) {
                case Defines.MZ_LOGIN:
                    p.color = (float) (0xd0 + ((int) Lib.rand() & 7));
                    break;
                case Defines.MZ_LOGOUT:
                    p.color = (float) (0x40 + ((int) Lib.rand() & 7));
                    break;
                default:
                    p.color = (float) (0xe0 + ((int) Lib.rand() & 7));
                    break;
            }

			p.org[0] = org[0] - 16.0F + Globals.rnd.nextFloat() * 32.0F;
			p.org[1] = org[1] - 16.0F + Globals.rnd.nextFloat() * 32.0F;
			p.org[2] = org[2] - 24.0F + Globals.rnd.nextFloat() * 56.0F;

			for (int j = 0; j < 3; j++)
				p.vel[j] = Lib.crand() * 20.0F;

			p.accel[0] = p.accel[1] = (float) 0;
			p.accel[2] = (float) -PARTICLE_GRAVITY;
			p.alpha = 1.0f;

			p.alphavel = -1.0f / (1.0f + Globals.rnd.nextFloat() * 0.3f);
		}
	}

	/*
	 * =============== CL_ItemRespawnParticles
	 * 
	 * ===============
	 */
	static void ItemRespawnParticles(float[] org) {

		for (int i = 0; i < 64; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;

			p.color = (float) (0xd4 + ((int) Lib.rand() & 3));

			p.org[0] = org[0] + Lib.crand() * 8.0F;
			p.org[1] = org[1] + Lib.crand() * 8.0F;
			p.org[2] = org[2] + Lib.crand() * 8.0F;

			for (int j = 0; j < 3; j++)
				p.vel[j] = Lib.crand() * 8.0F;

			p.accel[0] = p.accel[1] = (float) 0;
			p.accel[2] = (float) -PARTICLE_GRAVITY * 0.2f;
			p.alpha = 1.0f;

			p.alphavel = -1.0f / (1.0f + Globals.rnd.nextFloat() * 0.3f);
		}
	}

	/*
	 * =============== CL_ExplosionParticles ===============
	 */
	static void ExplosionParticles(float[] org) {

		for (int i = 0; i < 256; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;
			p.color = (float) (0xe0 + ((int) Lib.rand() & 7));

			for (int j = 0; j < 3; j++) {
				p.org[j] = org[j] + (float) (((int) Lib.rand() % 32) - 16);
				p.vel[j] = (float) (((int) Lib.rand() % 384) - 192);
			}

			p.accel[0] = p.accel[1] = 0.0f;
			p.accel[2] = (float) -PARTICLE_GRAVITY;
			p.alpha = 1.0f;

			p.alphavel = -0.8f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
		}
	}

	static void BigTeleportParticles(float[] org) {

		for (int i = 0; i < 4096; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;

			p.color = (float) colortable[(int) Lib.rand() & 3];

            float angle = (float) (Math.PI * 2.0 * (double) ((int) Lib.rand() & 1023) / 1023.0);
			float dist = (float) (Lib.rand() & 31);
			p.org[0] = (float) ((double) org[0] + Math.cos((double) angle) * (double) dist);
			p.vel[0] = (float) (Math.cos((double) angle) * (double) (70 + ((int) Lib.rand() & 63)));
			p.accel[0] = (float) (-Math.cos((double) angle) * 100.0);

			p.org[1] = (float) ((double) org[1] + Math.sin((double) angle) * (double) dist);
			p.vel[1] = (float) (Math.sin((double) angle) * (double) (70 + ((int) Lib.rand() & 63)));
			p.accel[1] = (float) (-Math.sin((double) angle) * 100.0);

			p.org[2] = org[2] + 8.0F + (float) ((int) Lib.rand() % 90);
			p.vel[2] = (float) (-100 + ((int) Lib.rand() & 31));
			p.accel[2] = (float) (PARTICLE_GRAVITY * 4);
			p.alpha = 1.0f;

			p.alphavel = -0.3f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
		}
	}

	/*
	 * =============== CL_BlasterParticles
	 * 
	 * Wall impact puffs ===============
	 */
	static void BlasterParticles(float[] org, float[] dir) {

        int count = 40;
		for (int i = 0; i < count; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;
			p.color = (float) (0xe0 + ((int) Lib.rand() & 7));

			float d = (float) (Lib.rand() & 15);
			for (int j = 0; j < 3; j++) {
				p.org[j] = org[j] + (float) (((int) Lib.rand() & 7) - 4) + d * dir[j];
				p.vel[j] = dir[j] * 30.0F + Lib.crand() * 40.0F;
			}

			p.accel[0] = p.accel[1] = (float) 0;
			p.accel[2] = (float) -PARTICLE_GRAVITY;
			p.alpha = 1.0f;

			p.alphavel = -1.0f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
		}
	}

	
	private static final float[] move = {(float) 0, (float) 0, (float) 0};
	private static final float[] vec = {(float) 0, (float) 0, (float) 0};
	/*
	 * =============== CL_BlasterTrail
	 * 
	 * ===============
	 */
	static void BlasterTrail(float[] start, float[] end) {

		Math3D.VectorCopy(start, move);
		Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

		Math3D.VectorScale(vec, 5.0F, vec);


        int dec = 5;
		while (len > (float) 0) {
            len = len - (float) dec;

			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;
			Math3D.VectorClear(p.accel);

			p.time = (float) Globals.cl.time;

			p.alpha = 1.0f;
			p.alphavel = -1.0f / (0.3f + Globals.rnd.nextFloat() * 0.2f);
			p.color = (float) 0xe0;
			for (int j = 0; j < 3; j++) {
				p.org[j] = move[j] + Lib.crand();
				p.vel[j] = Lib.crand() * 5.0F;
				p.accel[j] = (float) 0;
			}

			Math3D.VectorAdd(move, vec, move);
		}
	}

	
	
	/*
	 * ===============
	 *  CL_FlagTrail
	 * ===============
	 */
	static void FlagTrail(float[] start, float[] end, float color) {

		Math3D.VectorCopy(start, move);
		Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

		Math3D.VectorScale(vec, 5.0F, vec);

        int dec = 5;
		while (len > (float) 0) {
            len = len - (float) dec;

			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;
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
	 * =============== CL_DiminishingTrail
	 * 
	 * ===============
	 */
	static void DiminishingTrail(float[] start, float[] end, centity_t old, int flags) {

		Math3D.VectorCopy(start, move);
		Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        float dec = 0.5f;
		Math3D.VectorScale(vec, dec, vec);

		float velscale;
		float orgscale;
		if (old.trailcount > 900) {
			orgscale = 4.0F;
			velscale = 15.0F;
		} else if (old.trailcount > 800) {
			orgscale = 2.0F;
			velscale = 10.0F;
		} else {
			orgscale = 1.0F;
			velscale = 5.0F;
		}

		while (len > (float) 0) {
			len -= dec;

			if (free_particles == null)
				return;

			
			if (((int) Lib.rand() & 1023) < old.trailcount) {
                cparticle_t p = free_particles;
				free_particles = p.next;
				p.next = active_particles;
				active_particles = p;
				Math3D.VectorClear(p.accel);

				p.time = (float) Globals.cl.time;

				if ((flags & Defines.EF_GIB) != 0) {
					p.alpha = 1.0f;
					p.alphavel = -1.0f / (1.0f + Globals.rnd.nextFloat() * 0.4f);
					p.color = (float) (0xe8 + ((int) Lib.rand() & 7));
					for (int j = 0; j < 3; j++) {
						p.org[j] = move[j] + Lib.crand() * orgscale;
						p.vel[j] = Lib.crand() * velscale;
						p.accel[j] = (float) 0;
					}
                    p.vel[2] = p.vel[2] - (float) PARTICLE_GRAVITY;
				} else if ((flags & Defines.EF_GREENGIB) != 0) {
					p.alpha = 1.0f;
					p.alphavel = -1.0f / (1.0f + Globals.rnd.nextFloat() * 0.4f);
					p.color = (float) (0xdb + ((int) Lib.rand() & 7));
					for (int j = 0; j < 3; j++) {
						p.org[j] = move[j] + Lib.crand() * orgscale;
						p.vel[j] = Lib.crand() * velscale;
						p.accel[j] = (float) 0;
					}
                    p.vel[2] = p.vel[2] - (float) PARTICLE_GRAVITY;
				} else {
					p.alpha = 1.0f;
					p.alphavel = -1.0f / (1.0f + Globals.rnd.nextFloat() * 0.2f);
					p.color = (float) (4 + ((int) Lib.rand() & 7));
					for (int j = 0; j < 3; j++) {
						p.org[j] = move[j] + Lib.crand() * orgscale;
						p.vel[j] = Lib.crand() * velscale;
					}
					p.accel[2] = 20.0F;
				}
			}

			old.trailcount -= 5;
			if (old.trailcount < 100)
				old.trailcount = 100;
			Math3D.VectorAdd(move, vec, move);
		}
	}

	
	
	/*
	 * =============== CL_RocketTrail
	 * 
	 * ===============
	 */
	static void RocketTrail(float[] start, float[] end, centity_t old) {


		DiminishingTrail(start, end, old, Defines.EF_ROCKET);

		
		Math3D.VectorCopy(start, move);
		Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        float dec = 1.0F;
		Math3D.VectorScale(vec, dec, vec);

		while (len > (float) 0) {
			len -= dec;

			if (free_particles == null)
				return;

			if (((int) Lib.rand() & 7) == 0) {
                cparticle_t p = free_particles;
				free_particles = p.next;
				p.next = active_particles;
				active_particles = p;

				Math3D.VectorClear(p.accel);
				p.time = (float) Globals.cl.time;

				p.alpha = 1.0f;
				p.alphavel = -1.0f / (1.0f + Globals.rnd.nextFloat() * 0.2f);
				p.color = (float) (0xdc + ((int) Lib.rand() & 3));
				for (int j = 0; j < 3; j++) {
					p.org[j] = move[j] + Lib.crand() * 5.0F;
					p.vel[j] = Lib.crand() * 20.0F;
				}
				p.accel[2] = (float) -PARTICLE_GRAVITY;
			}
			Math3D.VectorAdd(move, vec, move);
		}
	}

	
	
	/*
	 * =============== CL_RailTrail
	 * 
	 * ===============
	 */
	static void RailTrail(float[] start, float[] end) {

		Math3D.VectorCopy(start, move);
		Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        float[] up = new float[3];
        float[] right = new float[3];
		Math3D.MakeNormalVectors(vec, right, up);

		byte clr = (byte) 0x74;
        float[] dir = new float[3];
		cparticle_t p;
		int j;
		for (int i = 0; (float) i < len; i++) {
			if (free_particles == null)
				return;

			p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;
			Math3D.VectorClear(p.accel);

            float d = (float) i * 0.1f;
            float c = (float) Math.cos((double) d);
            float s = (float) Math.sin((double) d);

			Math3D.VectorScale(right, c, dir);
			Math3D.VectorMA(dir, s, up, dir);

			p.alpha = 1.0f;
			p.alphavel = -1.0f / (1.0f + Globals.rnd.nextFloat() * 0.2f);
			p.color = (float) ((int) clr + ((int) Lib.rand() & 7));
			for (j = 0; j < 3; j++) {
				p.org[j] = move[j] + dir[j] * 3.0F;
				p.vel[j] = dir[j] * 6.0F;
			}

			Math3D.VectorAdd(move, vec, move);
		}

        float dec = 0.75f;
		Math3D.VectorScale(vec, dec, vec);
		Math3D.VectorCopy(start, move);

		while (len > (float) 0) {
			len -= dec;

			if (free_particles == null)
				return;
			p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;
			Math3D.VectorClear(p.accel);

			p.alpha = 1.0f;
			p.alphavel = -1.0f / (0.6f + Globals.rnd.nextFloat() * 0.2f);
			p.color = (float) (0x0 + (int) Lib.rand() & 15);

			for (j = 0; j < 3; j++) {
				p.org[j] = move[j] + Lib.crand() * 3.0F;
				p.vel[j] = Lib.crand() * 3.0F;
				p.accel[j] = (float) 0;
			}

			Math3D.VectorAdd(move, vec, move);
		}
	}

	
	
	/*
	 * =============== CL_IonripperTrail ===============
	 */
	static void IonripperTrail(float[] start, float[] ent) {

		Math3D.VectorCopy(start, move);
		Math3D.VectorSubtract(ent, start, vec);
        float len = Math3D.VectorNormalize(vec);

		Math3D.VectorScale(vec, 5.0F, vec);

        int dec = 5;
        int left = 0;
		while (len > (float) 0) {
            len = len - (float) dec;

			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;
			Math3D.VectorClear(p.accel);

			p.time = (float) Globals.cl.time;
			p.alpha = 0.5f;
			p.alphavel = -1.0f / (0.3f + Globals.rnd.nextFloat() * 0.2f);
			p.color = (float) (0xe4 + ((int) Lib.rand() & 3));

			for (int j = 0; j < 3; j++) {
				p.org[j] = move[j];
				p.accel[j] = (float) 0;
			}
			if (left != 0) {
				left = 0;
				p.vel[0] = 10.0F;
			} else {
				left = 1;
				p.vel[0] = -10.0F;
			}

			p.vel[1] = (float) 0;
			p.vel[2] = (float) 0;

			Math3D.VectorAdd(move, vec, move);
		}
	}

	
	
	/*
	 * =============== CL_BubbleTrail
	 * 
	 * ===============
	 */
	static void BubbleTrail(float[] start, float[] end) {

		Math3D.VectorCopy(start, move);
		Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        float dec = 32.0F;
		Math3D.VectorScale(vec, dec, vec);

		for (int i = 0; (float) i < len; i = (int) ((float) i + dec)) {
			if (free_particles == null)
				return;

            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			Math3D.VectorClear(p.accel);
			p.time = (float) Globals.cl.time;

			p.alpha = 1.0f;
			p.alphavel = -1.0f / (1.0f + Globals.rnd.nextFloat() * 0.2f);
			p.color = (float) (4 + ((int) Lib.rand() & 7));
			for (int j = 0; j < 3; j++) {
				p.org[j] = move[j] + Lib.crand() * 2.0F;
				p.vel[j] = Lib.crand() * 5.0F;
			}
			p.vel[2] += 6.0F;

			Math3D.VectorAdd(move, vec, move);
		}
	}

	
	
	/*
	 * =============== CL_FlyParticles ===============
	 */
	static void FlyParticles(float[] origin, int count) {

		if (count > Defines.NUMVERTEXNORMALS)
			count = Defines.NUMVERTEXNORMALS;

		int i;
		if (avelocities[0][0] == 0.0f) {
			for (i = 0; i < Defines.NUMVERTEXNORMALS; i++) {
				avelocities[i][0] = (float) ((int) Lib.rand() & 255) * 0.01f;
				avelocities[i][1] = (float) ((int) Lib.rand() & 255) * 0.01f;
				avelocities[i][2] = (float) ((int) Lib.rand() & 255) * 0.01f;
			}
		}

        float ltime = (float) Globals.cl.time / 1000.0f;
		float dist = 64.0F;
		for (i = 0; i < count; i += 2) {
            float angle = ltime * avelocities[i][0];
            float sy = (float) Math.sin((double) angle);
            float cy = (float) Math.cos((double) angle);
			angle = ltime * avelocities[i][1];
            float sp = (float) Math.sin((double) angle);
            float cp = (float) Math.cos((double) angle);
			angle = ltime * avelocities[i][2];

			forward[0] = cp * cy;
			forward[1] = cp * sy;
			forward[2] = -sp;

			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;

			dist = (float) Math.sin((double) (ltime + (float) i)) * 64.0F;
			p.org[0] = origin[0] + Globals.bytedirs[i][0] * dist + forward[0] * (float) BEAMLENGTH;
			p.org[1] = origin[1] + Globals.bytedirs[i][1] * dist + forward[1] * (float) BEAMLENGTH;
			p.org[2] = origin[2] + Globals.bytedirs[i][2] * dist + forward[2] * (float) BEAMLENGTH;

			Math3D.VectorClear(p.vel);
			Math3D.VectorClear(p.accel);

			p.color = (float) 0;
			

			p.alpha = 1.0F;
			p.alphavel = -100.0F;
		}
	}

	static void FlyEffect(centity_t ent, float[] origin) {
		int starttime;

		if (ent.fly_stoptime < Globals.cl.time) {
			starttime = Globals.cl.time;
			ent.fly_stoptime = Globals.cl.time + 60000;
		} else {
			starttime = ent.fly_stoptime - 60000;
		}

        int n = Globals.cl.time - starttime;
		int count;
		if (n < 20000)
			count = (int) ((double) (n * 162) / 20000.0);
		else {
			n = ent.fly_stoptime - Globals.cl.time;
			if (n < 20000)
				count = (int) ((double) (n * 162) / 20000.0);
			else
				count = 162;
		}

		FlyParticles(origin, count);
	}

	
	private static final float[] v = {(float) 0, (float) 0, (float) 0};
	
	/*
	 * =============== CL_BfgParticles ===============
	 */
	
	static void BfgParticles(entity_t ent) {
		int i;

		if (avelocities[0][0] == 0.0f) {
			for (i = 0; i < Defines.NUMVERTEXNORMALS; i++) {
				avelocities[i][0] = (float) ((int) Lib.rand() & 255) * 0.01f;
				avelocities[i][1] = (float) ((int) Lib.rand() & 255) * 0.01f;
				avelocities[i][2] = (float) ((int) Lib.rand() & 255) * 0.01f;
			}
		}

        float ltime = (float) Globals.cl.time / 1000.0f;
		float dist = 64.0F;
		for (i = 0; i < Defines.NUMVERTEXNORMALS; i++) {
            float angle = ltime * avelocities[i][0];
            float sy = (float) Math.sin((double) angle);
            float cy = (float) Math.cos((double) angle);
			angle = ltime * avelocities[i][1];
            float sp = (float) Math.sin((double) angle);
            float cp = (float) Math.cos((double) angle);
			angle = ltime * avelocities[i][2];

			forward[0] = cp * cy;
			forward[1] = cp * sy;
			forward[2] = -sp;

			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;

			dist = (float) (Math.sin((double) (ltime + (float) i)) * 64.0);
			p.org[0] = ent.origin[0] + Globals.bytedirs[i][0] * dist + forward[0] * (float) BEAMLENGTH;
			p.org[1] = ent.origin[1] + Globals.bytedirs[i][1] * dist + forward[1] * (float) BEAMLENGTH;
			p.org[2] = ent.origin[2] + Globals.bytedirs[i][2] * dist + forward[2] * (float) BEAMLENGTH;

			Math3D.VectorClear(p.vel);
			Math3D.VectorClear(p.accel);

			Math3D.VectorSubtract(p.org, ent.origin, v);
			dist = Math3D.VectorLength(v) / 90.0f;
			p.color = (float) Math.floor((double) (0xd0 + dist * 7.0F));
			

			p.alpha = 1.0f - dist;
			p.alphavel = -100.0F;
		}
	}

	
	
	private static final float[] start = {(float) 0, (float) 0, (float) 0};
	private static final float[] end = {(float) 0, (float) 0, (float) 0};
	/*
	 * =============== CL_TrapParticles ===============
	 */
	
	static void TrapParticles(entity_t ent) {

		ent.origin[2] -= 14.0F;
		Math3D.VectorCopy(ent.origin, start);
		Math3D.VectorCopy(ent.origin, end);
		end[2] += 64.0F;

		Math3D.VectorCopy(start, move);
		Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

		Math3D.VectorScale(vec, 5.0F, vec);


        int dec = 5;
		cparticle_t p;
		int j;
		while (len > (float) 0) {
            len = len - (float) dec;

			if (free_particles == null)
				return;
			p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;
			Math3D.VectorClear(p.accel);

			p.time = (float) Globals.cl.time;

			p.alpha = 1.0f;
			p.alphavel = -1.0f / (0.3f + Globals.rnd.nextFloat() * 0.2f);
			p.color = (float) 0xe0;
			for (j = 0; j < 3; j++) {
				p.org[j] = move[j] + Lib.crand();
				p.vel[j] = Lib.crand() * 15.0F;
				p.accel[j] = (float) 0;
			}
			p.accel[2] = (float) PARTICLE_GRAVITY;

			Math3D.VectorAdd(move, vec, move);
		}

		ent.origin[2] += 14.0F;
        float[] org = new float[3];
		Math3D.VectorCopy(ent.origin, org);

        float[] dir = new float[3];
		for (int i = -2; i <= 2; i += 4)
			for (j = -2; j <= 2; j += 4)
				for (int k = -2; k <= 4; k += 4) {
					if (free_particles == null)
						return;
					p = free_particles;
					free_particles = p.next;
					p.next = active_particles;
					active_particles = p;

					p.time = (float) Globals.cl.time;
					p.color = (float) (0xe0 + ((int) Lib.rand() & 3));

					p.alpha = 1.0f;
					p.alphavel = -1.0f / (0.3f + (float) ((int) Lib.rand() & 7) * 0.02f);

					p.org[0] = org[0] + (float) i + ((float) ((int) Lib.rand() & 23) * Lib.crand());
					p.org[1] = org[1] + (float) j + ((float) ((int) Lib.rand() & 23) * Lib.crand());
					p.org[2] = org[2] + (float) k + ((float) ((int) Lib.rand() & 23) * Lib.crand());

					dir[0] = (float) (j * 8);
					dir[1] = (float) (i * 8);
					dir[2] = (float) (k * 8);

					Math3D.VectorNormalize(dir);
					float vel = (float) (50 + (int) Lib.rand() & 63);
					Math3D.VectorScale(dir, vel, p.vel);

					p.accel[0] = p.accel[1] = (float) 0;
					p.accel[2] = (float) -PARTICLE_GRAVITY;
				}

	}

	/*
	 * =============== CL_BFGExplosionParticles ===============
	 */
	
	static void BFGExplosionParticles(float[] org) {

		for (int i = 0; i < 256; i++) {
			if (free_particles == null)
				return;
            cparticle_t p = free_particles;
			free_particles = p.next;
			p.next = active_particles;
			active_particles = p;

			p.time = (float) Globals.cl.time;
			p.color = (float) (0xd0 + ((int) Lib.rand() & 7));

			for (int j = 0; j < 3; j++) {
				p.org[j] = org[j] + (float) (((int) Lib.rand() % 32) - 16);
				p.vel[j] = (float) (((int) Lib.rand() % 384) - 192);
			}

			p.accel[0] = p.accel[1] = (float) 0;
			p.accel[2] = (float) -PARTICLE_GRAVITY;
			p.alpha = 1.0f;

			p.alphavel = -0.8f / (0.5f + Globals.rnd.nextFloat() * 0.3f);
		}
	}

	
	private static final float[] dir = {(float) 0, (float) 0, (float) 0};
	/*
	 * =============== CL_TeleportParticles
	 * 
	 * ===============
	 */
	static void TeleportParticles(float[] org) {

		for (int i = -16; i <= 16; i += 4)
			for (int j = -16; j <= 16; j += 4)
				for (int k = -16; k <= 32; k += 4) {
					if (free_particles == null)
						return;
                    cparticle_t p = free_particles;
					free_particles = p.next;
					p.next = active_particles;
					active_particles = p;

					p.time = (float) Globals.cl.time;
					p.color = (float) (7 + ((int) Lib.rand() & 7));

					p.alpha = 1.0f;
					p.alphavel = -1.0f / (0.3f + (float) ((int) Lib.rand() & 7) * 0.02f);

                    float[] porg = p.org;
					porg[0] = org[0] + (float) i + (float) ((int) Lib.rand() & 3);
					porg[1] = org[1] + (float) j + (float) ((int) Lib.rand() & 3);
					porg[2] = org[2] + (float) k + (float) ((int) Lib.rand() & 3);

                    float[] dir = CL_fx.dir;
					dir[0] = (float) (j * 8);
					dir[1] = (float) (i * 8);
					dir[2] = (float) (k * 8);

					Math3D.VectorNormalize(dir);
					float vel = (float) (50 + ((int) Lib.rand() & 63));
					Math3D.VectorScale(dir, vel, p.vel);

                    float[] paccel = p.accel;
					paccel[0] = paccel[1] = (float) 0;
					paccel[2] = (float) -PARTICLE_GRAVITY;
				}
	}

	
	private static final float[] org = {(float) 0, (float) 0, (float) 0};
	/*
	 * =============== CL_AddParticles ===============
	 */
	static void AddParticles() {
		cparticle_t next;
        float time = 0.0f;

		cparticle_t active = null;
        cparticle_t tail = null;

		for (cparticle_t p = active_particles; p != null; p = next) {
			next = p.next;


			float alpha;
			if (p.alphavel != INSTANT_PARTICLE) {
				time = ((float) Globals.cl.time - p.time) * 0.001f;
				alpha = p.alpha + time * p.alphavel;
				if (alpha <= (float) 0) {
					p.next = free_particles;
					free_particles = p;
					continue;
				}
			} else {
				alpha = p.alpha;
			}

			p.next = null;
			if (tail == null)
				active = tail = p;
			else {
				tail.next = p;
				tail = p;
			}

			if ((double) alpha > 1.0)
				alpha = 1.0F;
            int color = (int) p.color;

            float time2 = time * time;

            float[] org = CL_fx.org;
            float[] porg = p.org;
            float[] pvel = p.vel;
            float[] paccel = p.accel;
			org[0] = porg[0] + pvel[0] * time + paccel[0] * time2;
			org[1] = porg[1] + pvel[1] * time + paccel[1] * time2;
			org[2] = porg[2] + pvel[2] * time + paccel[2] * time2;

			V.AddParticle(org, color, alpha);
			
			if (p.alphavel == INSTANT_PARTICLE) {
				p.alphavel = 0.0f;
				p.alpha = 0.0f;
			}
		}

		active_particles = active;
	}

	/*
	 * ============== CL_EntityEvent
	 * 
	 * An entity has just been parsed that has an event value
	 * 
	 * the female events are there for backwards compatability ==============
	 */
	static void EntityEvent(entity_state_t ent) {
		switch (ent.event) {
		case Defines.EV_ITEM_RESPAWN:
			S.StartSound(null, ent.number, Defines.CHAN_WEAPON, S.RegisterSound("items/respawn1.wav"), 1.0F, (float) Defines.ATTN_IDLE, (float) 0);
			ItemRespawnParticles(ent.origin);
			break;
		case Defines.EV_PLAYER_TELEPORT:
			S.StartSound(null, ent.number, Defines.CHAN_WEAPON, S.RegisterSound("misc/tele1.wav"), 1.0F, (float) Defines.ATTN_IDLE, (float) 0);
			TeleportParticles(ent.origin);
			break;
		case Defines.EV_FOOTSTEP:
			if (Globals.cl_footsteps.value != 0.0f)
				S.StartSound(null, ent.number, Defines.CHAN_BODY, CL_tent.cl_sfx_footsteps[(int) Lib.rand() & 3], 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.EV_FALLSHORT:
			S.StartSound(null, ent.number, Defines.CHAN_AUTO, S.RegisterSound("player/land1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.EV_FALL:
			S.StartSound(null, ent.number, Defines.CHAN_AUTO, S.RegisterSound("*fall2.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;
		case Defines.EV_FALLFAR:
			S.StartSound(null, ent.number, Defines.CHAN_AUTO, S.RegisterSound("*fall1.wav"), 1.0F, (float) Defines.ATTN_NORM, (float) 0);
			break;
		}
	}

	/*
	 * ============== CL_ClearEffects
	 * 
	 * ==============
	 */
	static void ClearEffects() {
		ClearParticles();
		ClearDlights();
		ClearLightStyles();
	}

	/*
	 * ==============================================================
	 * 
	 * PARTICLE MANAGEMENT
	 * 
	 * ==============================================================
	 */

	static final int PARTICLE_GRAVITY = 40;

	static cparticle_t active_particles;
    static cparticle_t free_particles;

	/*
	 * =============== CL_BigTeleportParticles ===============
	 */
	private static final int[] colortable = { 2 * 8, 13 * 8, 21 * 8, 18 * 8 };

	private static final int BEAMLENGTH = 16;

}