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




/** Contains the definitions for the game engine. */

package jake2;

import java.nio.ByteOrder;


public class Defines {

	public static final int WEAPON_READY = 0;
	public static final int WEAPON_ACTIVATING = 1;
	public static final int WEAPON_DROPPING = 2;
	public static final int WEAPON_FIRING = 3;

	public static final float GRENADE_TIMER = 3.0f;
	public static final int GRENADE_MINSPEED = 400;
	public static final int GRENADE_MAXSPEED = 800;

	
	

	
	public static final int PM_NORMAL = 0;
	public static final int PM_SPECTATOR = 1;
	
	public static final int PM_DEAD = 2;
	public static final int PM_GIB = 3;
	public static final int PM_FREEZE = 4;

	public static final int EV_NONE = 0;
	public static final int EV_ITEM_RESPAWN = 1;
	public static final int EV_FOOTSTEP = 2;
	public static final int EV_FALLSHORT = 3;
	public static final int EV_FALL = 4;
	public static final int EV_FALLFAR = 5;
	public static final int EV_PLAYER_TELEPORT = 6;
	public static final int EV_OTHER_TELEPORT = 7;

	
	public static final int PITCH = 0;
	public static final int YAW = 1;
	public static final int ROLL = 2;

	public static final int MAX_STRING_CHARS = 1024;
	public static final int MAX_STRING_TOKENS = 80;
	public static final int MAX_TOKEN_CHARS = 1024;

	public static final int MAX_QPATH = 64;
	public static final int MAX_OSPATH = 128;

	
	public static final int MAX_CLIENTS = 256;
	public static final int MAX_EDICTS = 1024;
	public static final int MAX_LIGHTSTYLES = 256;
	public static final int MAX_MODELS = 256;
	public static final int MAX_SOUNDS = 256;
	public static final int MAX_IMAGES = 256;
	public static final int MAX_ITEMS = 256;
	public static final int MAX_GENERAL = (MAX_CLIENTS * 2);

	
	public static final int PRINT_LOW = 0;
	public static final int PRINT_MEDIUM = 1;
	public static final int PRINT_HIGH = 2;
	public static final int PRINT_CHAT = 3;

	public static final int ERR_FATAL = 0;
	public static final int ERR_DROP = 1;
	public static final int ERR_DISCONNECT = 2;

	public static final int PRINT_ALL = 0;
	public static final int PRINT_DEVELOPER = 1;
	public static final int PRINT_ALERT = 2;

	
	public static final int MAX_INFO_KEY = 64;
	public static final int MAX_INFO_VALUE = 64;
	public static final int MAX_INFO_STRING = 512;

	
	public static final int SFF_ARCH = 0x01;
	public static final int SFF_HIDDEN = 0x02;
	public static final int SFF_RDONLY = 0x04;
	public static final int SFF_SUBDIR = 0x08;
	public static final int SFF_SYSTEM = 0x10;

	public static final int CVAR_ARCHIVE = 1;
	public static final int CVAR_USERINFO = 2;
	public static final int CVAR_SERVERINFO = 4;
	public static final int CVAR_NOSET = 8;
	
	public static final int CVAR_LATCH = 16;

	
	public static final int CONTENTS_SOLID = 1;
	public static final int CONTENTS_WINDOW = 2;
	public static final int CONTENTS_AUX = 4;
	public static final int CONTENTS_LAVA = 8;
	public static final int CONTENTS_SLIME = 16;
	public static final int CONTENTS_WATER = 32;
	public static final int CONTENTS_MIST = 64;
	public static final int LAST_VISIBLE_CONTENTS = 64;

	
	public static final int CONTENTS_AREAPORTAL = 0x8000;

	public static final int CONTENTS_PLAYERCLIP = 0x10000;
	public static final int CONTENTS_MONSTERCLIP = 0x20000;

	
	public static final int CONTENTS_CURRENT_0 = 0x40000;
	public static final int CONTENTS_CURRENT_90 = 0x80000;
	public static final int CONTENTS_CURRENT_180 = 0x100000;
	public static final int CONTENTS_CURRENT_270 = 0x200000;
	public static final int CONTENTS_CURRENT_UP = 0x400000;
	public static final int CONTENTS_CURRENT_DOWN = 0x800000;

	public static final int CONTENTS_ORIGIN = 0x1000000;

	public static final int CONTENTS_MONSTER = 0x2000000;
	public static final int CONTENTS_DEADMONSTER = 0x4000000;
	public static final int CONTENTS_DETAIL = 0x8000000;
	public static final int CONTENTS_TRANSLUCENT = 0x10000000;
	public static final int CONTENTS_LADDER = 0x20000000;

	public static final int SURF_LIGHT = 0x1;
	public static final int SURF_SLICK = 0x2;

	public static final int SURF_SKY = 0x4;
	public static final int SURF_WARP = 0x8;
	public static final int SURF_TRANS33 = 0x10;
	public static final int SURF_TRANS66 = 0x20;
	public static final int SURF_FLOWING = 0x40;
	public static final int SURF_NODRAW = 0x80;

	
	
	
	public static final int BUTTON_ATTACK = 1;
	public static final int BUTTON_USE = 2;
	public static final int BUTTON_ANY = 128;

	public static final int MAXTOUCH = 32;

	
	
	
	
	
	public static final int EF_ROTATE = 0x00000001;
	public static final int EF_GIB = 0x00000002;
	public static final int EF_BLASTER = 0x00000008;
	public static final int EF_ROCKET = 0x00000010;
	public static final int EF_GRENADE = 0x00000020;
	public static final int EF_HYPERBLASTER = 0x00000040;
	public static final int EF_BFG = 0x00000080;
	public static final int EF_COLOR_SHELL = 0x00000100;
	public static final int EF_POWERSCREEN = 0x00000200;
	public static final int EF_ANIM01 = 0x00000400;
	public static final int EF_ANIM23 = 0x00000800;
	public static final int EF_ANIM_ALL = 0x00001000;
	public static final int EF_ANIM_ALLFAST = 0x00002000;
	public static final int EF_FLIES = 0x00004000;
	public static final int EF_QUAD = 0x00008000;
	public static final int EF_PENT = 0x00010000;
	public static final int EF_TELEPORTER = 0x00020000;
	public static final int EF_FLAG1 = 0x00040000;
	public static final int EF_FLAG2 = 0x00080000;
	
	public static final int EF_IONRIPPER = 0x00100000;
	public static final int EF_GREENGIB = 0x00200000;
	public static final int EF_BLUEHYPERBLASTER = 0x00400000;
	public static final int EF_SPINNINGLIGHTS = 0x00800000;
	public static final int EF_PLASMA = 0x01000000;
	public static final int EF_TRAP = 0x02000000;

	
	public static final int EF_TRACKER = 0x04000000;
	public static final int EF_DOUBLE = 0x08000000;
	public static final int EF_SPHERETRANS = 0x10000000;
	public static final int EF_TAGTRAIL = 0x20000000;
	public static final int EF_HALF_DAMAGE = 0x40000000;
	public static final int EF_TRACKERTRAIL = 0x80000000;
	

	
	public static final int RF_MINLIGHT = 1;
	public static final int RF_VIEWERMODEL = 2;
	public static final int RF_WEAPONMODEL = 4;
	public static final int RF_FULLBRIGHT = 8;
	public static final int RF_DEPTHHACK = 16;
	public static final int RF_TRANSLUCENT = 32;
	public static final int RF_FRAMELERP = 64;
	public static final int RF_BEAM = 128;
	public static final int RF_CUSTOMSKIN = 256;
	public static final int RF_GLOW = 512;
	public static final int RF_SHELL_RED = 1024;
	public static final int RF_SHELL_GREEN = 2048;
	public static final int RF_SHELL_BLUE = 4096;

	
	public static final int RF_IR_VISIBLE = 0x00008000;
	public static final int RF_SHELL_DOUBLE = 0x00010000;
	public static final int RF_SHELL_HALF_DAM = 0x00020000;
	public static final int RF_USE_DISGUISE = 0x00040000;
	

	
	public static final int RDF_UNDERWATER = 1;
	public static final int RDF_NOWORLDMODEL = 2;

	
	public static final int RDF_IRGOGGLES = 4;
	public static final int RDF_UVGOGGLES = 8;
	

	
	public static final int MZ_BLASTER = 0;
	public static final int MZ_MACHINEGUN = 1;
	public static final int MZ_SHOTGUN = 2;
	public static final int MZ_CHAINGUN1 = 3;
	public static final int MZ_CHAINGUN2 = 4;
	public static final int MZ_CHAINGUN3 = 5;
	public static final int MZ_RAILGUN = 6;
	public static final int MZ_ROCKET = 7;
	public static final int MZ_GRENADE = 8;
	public static final int MZ_LOGIN = 9;
	public static final int MZ_LOGOUT = 10;
	public static final int MZ_RESPAWN = 11;
	public static final int MZ_BFG = 12;
	public static final int MZ_SSHOTGUN = 13;
	public static final int MZ_HYPERBLASTER = 14;
	public static final int MZ_ITEMRESPAWN = 15;
	
	public static final int MZ_IONRIPPER = 16;
	public static final int MZ_BLUEHYPERBLASTER = 17;
	public static final int MZ_PHALANX = 18;
	public static final int MZ_SILENCED = 128;

	
	public static final int MZ_ETF_RIFLE = 30;
	public static final int MZ_UNUSED = 31;
	public static final int MZ_SHOTGUN2 = 32;
	public static final int MZ_HEATBEAM = 33;
	public static final int MZ_BLASTER2 = 34;
	public static final int MZ_TRACKER = 35;
	public static final int MZ_NUKE1 = 36;
	public static final int MZ_NUKE2 = 37;
	public static final int MZ_NUKE4 = 38;
	public static final int MZ_NUKE8 = 39;
	

	
	
	
	public static final int MZ2_TANK_BLASTER_1 = 1;
	public static final int MZ2_TANK_BLASTER_2 = 2;
	public static final int MZ2_TANK_BLASTER_3 = 3;
	public static final int MZ2_TANK_MACHINEGUN_1 = 4;
	public static final int MZ2_TANK_MACHINEGUN_2 = 5;
	public static final int MZ2_TANK_MACHINEGUN_3 = 6;
	public static final int MZ2_TANK_MACHINEGUN_4 = 7;
	public static final int MZ2_TANK_MACHINEGUN_5 = 8;
	public static final int MZ2_TANK_MACHINEGUN_6 = 9;
	public static final int MZ2_TANK_MACHINEGUN_7 = 10;
	public static final int MZ2_TANK_MACHINEGUN_8 = 11;
	public static final int MZ2_TANK_MACHINEGUN_9 = 12;
	public static final int MZ2_TANK_MACHINEGUN_10 = 13;
	public static final int MZ2_TANK_MACHINEGUN_11 = 14;
	public static final int MZ2_TANK_MACHINEGUN_12 = 15;
	public static final int MZ2_TANK_MACHINEGUN_13 = 16;
	public static final int MZ2_TANK_MACHINEGUN_14 = 17;
	public static final int MZ2_TANK_MACHINEGUN_15 = 18;
	public static final int MZ2_TANK_MACHINEGUN_16 = 19;
	public static final int MZ2_TANK_MACHINEGUN_17 = 20;
	public static final int MZ2_TANK_MACHINEGUN_18 = 21;
	public static final int MZ2_TANK_MACHINEGUN_19 = 22;
	public static final int MZ2_TANK_ROCKET_1 = 23;
	public static final int MZ2_TANK_ROCKET_2 = 24;
	public static final int MZ2_TANK_ROCKET_3 = 25;

	public static final int MZ2_INFANTRY_MACHINEGUN_1 = 26;
	public static final int MZ2_INFANTRY_MACHINEGUN_2 = 27;
	public static final int MZ2_INFANTRY_MACHINEGUN_3 = 28;
	public static final int MZ2_INFANTRY_MACHINEGUN_4 = 29;
	public static final int MZ2_INFANTRY_MACHINEGUN_5 = 30;
	public static final int MZ2_INFANTRY_MACHINEGUN_6 = 31;
	public static final int MZ2_INFANTRY_MACHINEGUN_7 = 32;
	public static final int MZ2_INFANTRY_MACHINEGUN_8 = 33;
	public static final int MZ2_INFANTRY_MACHINEGUN_9 = 34;
	public static final int MZ2_INFANTRY_MACHINEGUN_10 = 35;
	public static final int MZ2_INFANTRY_MACHINEGUN_11 = 36;
	public static final int MZ2_INFANTRY_MACHINEGUN_12 = 37;
	public static final int MZ2_INFANTRY_MACHINEGUN_13 = 38;

	public static final int MZ2_SOLDIER_BLASTER_1 = 39;
	public static final int MZ2_SOLDIER_BLASTER_2 = 40;
	public static final int MZ2_SOLDIER_SHOTGUN_1 = 41;
	public static final int MZ2_SOLDIER_SHOTGUN_2 = 42;
	public static final int MZ2_SOLDIER_MACHINEGUN_1 = 43;
	public static final int MZ2_SOLDIER_MACHINEGUN_2 = 44;

	public static final int MZ2_GUNNER_MACHINEGUN_1 = 45;
	public static final int MZ2_GUNNER_MACHINEGUN_2 = 46;
	public static final int MZ2_GUNNER_MACHINEGUN_3 = 47;
	public static final int MZ2_GUNNER_MACHINEGUN_4 = 48;
	public static final int MZ2_GUNNER_MACHINEGUN_5 = 49;
	public static final int MZ2_GUNNER_MACHINEGUN_6 = 50;
	public static final int MZ2_GUNNER_MACHINEGUN_7 = 51;
	public static final int MZ2_GUNNER_MACHINEGUN_8 = 52;
	public static final int MZ2_GUNNER_GRENADE_1 = 53;
	public static final int MZ2_GUNNER_GRENADE_2 = 54;
	public static final int MZ2_GUNNER_GRENADE_3 = 55;
	public static final int MZ2_GUNNER_GRENADE_4 = 56;

	public static final int MZ2_CHICK_ROCKET_1 = 57;

	public static final int MZ2_FLYER_BLASTER_1 = 58;
	public static final int MZ2_FLYER_BLASTER_2 = 59;

	public static final int MZ2_MEDIC_BLASTER_1 = 60;

	public static final int MZ2_GLADIATOR_RAILGUN_1 = 61;

	public static final int MZ2_HOVER_BLASTER_1 = 62;

	public static final int MZ2_ACTOR_MACHINEGUN_1 = 63;

	public static final int MZ2_SUPERTANK_MACHINEGUN_1 = 64;
	public static final int MZ2_SUPERTANK_MACHINEGUN_2 = 65;
	public static final int MZ2_SUPERTANK_MACHINEGUN_3 = 66;
	public static final int MZ2_SUPERTANK_MACHINEGUN_4 = 67;
	public static final int MZ2_SUPERTANK_MACHINEGUN_5 = 68;
	public static final int MZ2_SUPERTANK_MACHINEGUN_6 = 69;
	public static final int MZ2_SUPERTANK_ROCKET_1 = 70;
	public static final int MZ2_SUPERTANK_ROCKET_2 = 71;
	public static final int MZ2_SUPERTANK_ROCKET_3 = 72;

	public static final int MZ2_BOSS2_MACHINEGUN_L1 = 73;
	public static final int MZ2_BOSS2_MACHINEGUN_L2 = 74;
	public static final int MZ2_BOSS2_MACHINEGUN_L3 = 75;
	public static final int MZ2_BOSS2_MACHINEGUN_L4 = 76;
	public static final int MZ2_BOSS2_MACHINEGUN_L5 = 77;
	public static final int MZ2_BOSS2_ROCKET_1 = 78;
	public static final int MZ2_BOSS2_ROCKET_2 = 79;
	public static final int MZ2_BOSS2_ROCKET_3 = 80;
	public static final int MZ2_BOSS2_ROCKET_4 = 81;

	public static final int MZ2_FLOAT_BLASTER_1 = 82;

	public static final int MZ2_SOLDIER_BLASTER_3 = 83;
	public static final int MZ2_SOLDIER_SHOTGUN_3 = 84;
	public static final int MZ2_SOLDIER_MACHINEGUN_3 = 85;
	public static final int MZ2_SOLDIER_BLASTER_4 = 86;
	public static final int MZ2_SOLDIER_SHOTGUN_4 = 87;
	public static final int MZ2_SOLDIER_MACHINEGUN_4 = 88;
	public static final int MZ2_SOLDIER_BLASTER_5 = 89;
	public static final int MZ2_SOLDIER_SHOTGUN_5 = 90;
	public static final int MZ2_SOLDIER_MACHINEGUN_5 = 91;
	public static final int MZ2_SOLDIER_BLASTER_6 = 92;
	public static final int MZ2_SOLDIER_SHOTGUN_6 = 93;
	public static final int MZ2_SOLDIER_MACHINEGUN_6 = 94;
	public static final int MZ2_SOLDIER_BLASTER_7 = 95;
	public static final int MZ2_SOLDIER_SHOTGUN_7 = 96;
	public static final int MZ2_SOLDIER_MACHINEGUN_7 = 97;
	public static final int MZ2_SOLDIER_BLASTER_8 = 98;
	public static final int MZ2_SOLDIER_SHOTGUN_8 = 99;
	public static final int MZ2_SOLDIER_MACHINEGUN_8 = 100;

	
	public static final int MZ2_MAKRON_BFG = 101;
	public static final int MZ2_MAKRON_BLASTER_1 = 102;
	public static final int MZ2_MAKRON_BLASTER_2 = 103;
	public static final int MZ2_MAKRON_BLASTER_3 = 104;
	public static final int MZ2_MAKRON_BLASTER_4 = 105;
	public static final int MZ2_MAKRON_BLASTER_5 = 106;
	public static final int MZ2_MAKRON_BLASTER_6 = 107;
	public static final int MZ2_MAKRON_BLASTER_7 = 108;
	public static final int MZ2_MAKRON_BLASTER_8 = 109;
	public static final int MZ2_MAKRON_BLASTER_9 = 110;
	public static final int MZ2_MAKRON_BLASTER_10 = 111;
	public static final int MZ2_MAKRON_BLASTER_11 = 112;
	public static final int MZ2_MAKRON_BLASTER_12 = 113;
	public static final int MZ2_MAKRON_BLASTER_13 = 114;
	public static final int MZ2_MAKRON_BLASTER_14 = 115;
	public static final int MZ2_MAKRON_BLASTER_15 = 116;
	public static final int MZ2_MAKRON_BLASTER_16 = 117;
	public static final int MZ2_MAKRON_BLASTER_17 = 118;
	public static final int MZ2_MAKRON_RAILGUN_1 = 119;
	public static final int MZ2_JORG_MACHINEGUN_L1 = 120;
	public static final int MZ2_JORG_MACHINEGUN_L2 = 121;
	public static final int MZ2_JORG_MACHINEGUN_L3 = 122;
	public static final int MZ2_JORG_MACHINEGUN_L4 = 123;
	public static final int MZ2_JORG_MACHINEGUN_L5 = 124;
	public static final int MZ2_JORG_MACHINEGUN_L6 = 125;
	public static final int MZ2_JORG_MACHINEGUN_R1 = 126;
	public static final int MZ2_JORG_MACHINEGUN_R2 = 127;
	public static final int MZ2_JORG_MACHINEGUN_R3 = 128;
	public static final int MZ2_JORG_MACHINEGUN_R4 = 129;
	public static final int MZ2_JORG_MACHINEGUN_R5 = 130;
	public static final int MZ2_JORG_MACHINEGUN_R6 = 131;
	public static final int MZ2_JORG_BFG_1 = 132;
	public static final int MZ2_BOSS2_MACHINEGUN_R1 = 133;
	public static final int MZ2_BOSS2_MACHINEGUN_R2 = 134;
	public static final int MZ2_BOSS2_MACHINEGUN_R3 = 135;
	public static final int MZ2_BOSS2_MACHINEGUN_R4 = 136;
	public static final int MZ2_BOSS2_MACHINEGUN_R5 = 137;

	
	public static final int MZ2_CARRIER_MACHINEGUN_L1 = 138;
	public static final int MZ2_CARRIER_MACHINEGUN_R1 = 139;
	public static final int MZ2_CARRIER_GRENADE = 140;
	public static final int MZ2_TURRET_MACHINEGUN = 141;
	public static final int MZ2_TURRET_ROCKET = 142;
	public static final int MZ2_TURRET_BLASTER = 143;
	public static final int MZ2_STALKER_BLASTER = 144;
	public static final int MZ2_DAEDALUS_BLASTER = 145;
	public static final int MZ2_MEDIC_BLASTER_2 = 146;
	public static final int MZ2_CARRIER_RAILGUN = 147;
	public static final int MZ2_WIDOW_DISRUPTOR = 148;
	public static final int MZ2_WIDOW_BLASTER = 149;
	public static final int MZ2_WIDOW_RAIL = 150;
	public static final int MZ2_WIDOW_PLASMABEAM = 151;
	public static final int MZ2_CARRIER_MACHINEGUN_L2 = 152;
	public static final int MZ2_CARRIER_MACHINEGUN_R2 = 153;
	public static final int MZ2_WIDOW_RAIL_LEFT = 154;
	public static final int MZ2_WIDOW_RAIL_RIGHT = 155;
	public static final int MZ2_WIDOW_BLASTER_SWEEP1 = 156;
	public static final int MZ2_WIDOW_BLASTER_SWEEP2 = 157;
	public static final int MZ2_WIDOW_BLASTER_SWEEP3 = 158;
	public static final int MZ2_WIDOW_BLASTER_SWEEP4 = 159;
	public static final int MZ2_WIDOW_BLASTER_SWEEP5 = 160;
	public static final int MZ2_WIDOW_BLASTER_SWEEP6 = 161;
	public static final int MZ2_WIDOW_BLASTER_SWEEP7 = 162;
	public static final int MZ2_WIDOW_BLASTER_SWEEP8 = 163;
	public static final int MZ2_WIDOW_BLASTER_SWEEP9 = 164;
	public static final int MZ2_WIDOW_BLASTER_100 = 165;
	public static final int MZ2_WIDOW_BLASTER_90 = 166;
	public static final int MZ2_WIDOW_BLASTER_80 = 167;
	public static final int MZ2_WIDOW_BLASTER_70 = 168;
	public static final int MZ2_WIDOW_BLASTER_60 = 169;
	public static final int MZ2_WIDOW_BLASTER_50 = 170;
	public static final int MZ2_WIDOW_BLASTER_40 = 171;
	public static final int MZ2_WIDOW_BLASTER_30 = 172;
	public static final int MZ2_WIDOW_BLASTER_20 = 173;
	public static final int MZ2_WIDOW_BLASTER_10 = 174;
	public static final int MZ2_WIDOW_BLASTER_0 = 175;
	public static final int MZ2_WIDOW_BLASTER_10L = 176;
	public static final int MZ2_WIDOW_BLASTER_20L = 177;
	public static final int MZ2_WIDOW_BLASTER_30L = 178;
	public static final int MZ2_WIDOW_BLASTER_40L = 179;
	public static final int MZ2_WIDOW_BLASTER_50L = 180;
	public static final int MZ2_WIDOW_BLASTER_60L = 181;
	public static final int MZ2_WIDOW_BLASTER_70L = 182;
	public static final int MZ2_WIDOW_RUN_1 = 183;
	public static final int MZ2_WIDOW_RUN_2 = 184;
	public static final int MZ2_WIDOW_RUN_3 = 185;
	public static final int MZ2_WIDOW_RUN_4 = 186;
	public static final int MZ2_WIDOW_RUN_5 = 187;
	public static final int MZ2_WIDOW_RUN_6 = 188;
	public static final int MZ2_WIDOW_RUN_7 = 189;
	public static final int MZ2_WIDOW_RUN_8 = 190;
	public static final int MZ2_CARRIER_ROCKET_1 = 191;
	public static final int MZ2_CARRIER_ROCKET_2 = 192;
	public static final int MZ2_CARRIER_ROCKET_3 = 193;
	public static final int MZ2_CARRIER_ROCKET_4 = 194;
	public static final int MZ2_WIDOW2_BEAMER_1 = 195;
	public static final int MZ2_WIDOW2_BEAMER_2 = 196;
	public static final int MZ2_WIDOW2_BEAMER_3 = 197;
	public static final int MZ2_WIDOW2_BEAMER_4 = 198;
	public static final int MZ2_WIDOW2_BEAMER_5 = 199;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_1 = 200;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_2 = 201;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_3 = 202;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_4 = 203;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_5 = 204;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_6 = 205;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_7 = 206;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_8 = 207;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_9 = 208;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_10 = 209;
	public static final int MZ2_WIDOW2_BEAM_SWEEP_11 = 210;

	public static final int SPLASH_UNKNOWN = 0;
	public static final int SPLASH_SPARKS = 1;
	public static final int SPLASH_BLUE_WATER = 2;
	public static final int SPLASH_BROWN_WATER = 3;
	public static final int SPLASH_SLIME = 4;
	public static final int SPLASH_LAVA = 5;
	public static final int SPLASH_BLOOD = 6;

	
	
	
	public static final int CHAN_AUTO = 0;
	public static final int CHAN_WEAPON = 1;
	public static final int CHAN_VOICE = 2;
	public static final int CHAN_ITEM = 3;
	public static final int CHAN_BODY = 4;
	
	public static final int CHAN_NO_PHS_ADD = 8;
	
	public static final int CHAN_RELIABLE = 16;

	
	public static final int ATTN_NONE = 0;
	public static final int ATTN_NORM = 1;
	public static final int ATTN_IDLE = 2;
	public static final int ATTN_STATIC = 3;

	
	public static final int STAT_HEALTH_ICON = 0;
	public static final int STAT_HEALTH = 1;
	public static final int STAT_AMMO_ICON = 2;
	public static final int STAT_AMMO = 3;
	public static final int STAT_ARMOR_ICON = 4;
	public static final int STAT_ARMOR = 5;
	public static final int STAT_SELECTED_ICON = 6;
	public static final int STAT_PICKUP_ICON = 7;
	public static final int STAT_PICKUP_STRING = 8;
	public static final int STAT_TIMER_ICON = 9;
	public static final int STAT_TIMER = 10;
	public static final int STAT_HELPICON = 11;
	public static final int STAT_SELECTED_ITEM = 12;
	public static final int STAT_LAYOUTS = 13;
	public static final int STAT_FRAGS = 14;
	public static final int STAT_FLASHES = 15;
	public static final int STAT_CHASE = 16;
	public static final int STAT_SPECTATOR = 17;

	public static final int MAX_STATS = 32;

	
	public static final int DF_NO_HEALTH = 0x00000001;
	public static final int DF_NO_ITEMS = 0x00000002;
	public static final int DF_WEAPONS_STAY = 0x00000004;
	public static final int DF_NO_FALLING = 0x00000008;
	public static final int DF_INSTANT_ITEMS = 0x00000010;
	public static final int DF_SAME_LEVEL = 0x00000020;
	public static final int DF_SKINTEAMS = 0x00000040;
	public static final int DF_MODELTEAMS = 0x00000080;
	public static final int DF_NO_FRIENDLY_FIRE = 0x00000100;
	public static final int DF_SPAWN_FARTHEST = 0x00000200;
	public static final int DF_FORCE_RESPAWN = 0x00000400;
	public static final int DF_NO_ARMOR = 0x00000800;
	public static final int DF_ALLOW_EXIT = 0x00001000;
	public static final int DF_INFINITE_AMMO = 0x00002000;
	public static final int DF_QUAD_DROP = 0x00004000;
	public static final int DF_FIXED_FOV = 0x00008000;

	
	public static final int DF_QUADFIRE_DROP = 0x00010000;

	
	public static final int DF_NO_MINES = 0x00020000;
	public static final int DF_NO_STACK_DOUBLE = 0x00040000;
	public static final int DF_NO_NUKES = 0x00080000;
	public static final int DF_NO_SPHERES = 0x00100000;
	

	
	
	
	
	
	public static final int CS_NAME = 0;
	public static final int CS_CDTRACK = 1;
	public static final int CS_SKY = 2;
	public static final int CS_SKYAXIS = 3;
	public static final int CS_SKYROTATE = 4;
	public static final int CS_STATUSBAR = 5;

	public static final int CS_AIRACCEL = 29;
	public static final int CS_MAXCLIENTS = 30;
	public static final int CS_MAPCHECKSUM = 31;

	public static final int CS_MODELS = 32;
	public static final int CS_SOUNDS = (CS_MODELS + MAX_MODELS);
	public static final int CS_IMAGES = (CS_SOUNDS + MAX_SOUNDS);
	public static final int CS_LIGHTS = (CS_IMAGES + MAX_IMAGES);
	public static final int CS_ITEMS = (CS_LIGHTS + MAX_LIGHTSTYLES);
	public static final int CS_PLAYERSKINS = (CS_ITEMS + MAX_ITEMS);
	public static final int CS_GENERAL = (CS_PLAYERSKINS + MAX_CLIENTS);
	public static final int MAX_CONFIGSTRINGS = (CS_GENERAL + MAX_GENERAL);

	public static final int HEALTH_IGNORE_MAX = 1;
	public static final int HEALTH_TIMED = 2;

	
	
	public static final int AREA_SOLID = 1;
	public static final int AREA_TRIGGERS = 2;

	public static final int TE_GUNSHOT = 0;
	public static final int TE_BLOOD = 1;
	public static final int TE_BLASTER = 2;
	public static final int TE_RAILTRAIL = 3;
	public static final int TE_SHOTGUN = 4;
	public static final int TE_EXPLOSION1 = 5;
	public static final int TE_EXPLOSION2 = 6;
	public static final int TE_ROCKET_EXPLOSION = 7;
	public static final int TE_GRENADE_EXPLOSION = 8;
	public static final int TE_SPARKS = 9;
	public static final int TE_SPLASH = 10;
	public static final int TE_BUBBLETRAIL = 11;
	public static final int TE_SCREEN_SPARKS = 12;
	public static final int TE_SHIELD_SPARKS = 13;
	public static final int TE_BULLET_SPARKS = 14;
	public static final int TE_LASER_SPARKS = 15;
	public static final int TE_PARASITE_ATTACK = 16;
	public static final int TE_ROCKET_EXPLOSION_WATER = 17;
	public static final int TE_GRENADE_EXPLOSION_WATER = 18;
	public static final int TE_MEDIC_CABLE_ATTACK = 19;
	public static final int TE_BFG_EXPLOSION = 20;
	public static final int TE_BFG_BIGEXPLOSION = 21;
	public static final int TE_BOSSTPORT = 22;
	public static final int TE_BFG_LASER = 23;
	public static final int TE_GRAPPLE_CABLE = 24;
	public static final int TE_WELDING_SPARKS = 25;
	public static final int TE_GREENBLOOD = 26;
	public static final int TE_BLUEHYPERBLASTER = 27;
	public static final int TE_PLASMA_EXPLOSION = 28;
	public static final int TE_TUNNEL_SPARKS = 29;
	
	public static final int TE_BLASTER2 = 30;
	public static final int TE_RAILTRAIL2 = 31;
	public static final int TE_FLAME = 32;
	public static final int TE_LIGHTNING = 33;
	public static final int TE_DEBUGTRAIL = 34;
	public static final int TE_PLAIN_EXPLOSION = 35;
	public static final int TE_FLASHLIGHT = 36;
	public static final int TE_FORCEWALL = 37;
	public static final int TE_HEATBEAM = 38;
	public static final int TE_MONSTER_HEATBEAM = 39;
	public static final int TE_STEAM = 40;
	public static final int TE_BUBBLETRAIL2 = 41;
	public static final int TE_MOREBLOOD = 42;
	public static final int TE_HEATBEAM_SPARKS = 43;
	public static final int TE_HEATBEAM_STEAM = 44;
	public static final int TE_CHAINFIST_SMOKE = 45;
	public static final int TE_ELECTRIC_SPARKS = 46;
	public static final int TE_TRACKER_EXPLOSION = 47;
	public static final int TE_TELEPORT_EFFECT = 48;
	public static final int TE_DBALL_GOAL = 49;
	public static final int TE_WIDOWBEAMOUT = 50;
	public static final int TE_NUKEBLAST = 51;
	public static final int TE_WIDOWSPLASH = 52;
	public static final int TE_EXPLOSION1_BIG = 53;
	public static final int TE_EXPLOSION1_NP = 54;
	public static final int TE_FLECHETTE = 55;

	
	public static final int MASK_ALL = (-1);
	public static final int MASK_SOLID = (CONTENTS_SOLID | CONTENTS_WINDOW);
	public static final int MASK_PLAYERSOLID = (CONTENTS_SOLID | CONTENTS_PLAYERCLIP | CONTENTS_WINDOW | CONTENTS_MONSTER);
	public static final int MASK_DEADSOLID = (CONTENTS_SOLID | CONTENTS_PLAYERCLIP | CONTENTS_WINDOW);
	public static final int MASK_MONSTERSOLID = (CONTENTS_SOLID | CONTENTS_MONSTERCLIP | CONTENTS_WINDOW | CONTENTS_MONSTER);
	public static final int MASK_WATER = (CONTENTS_WATER | CONTENTS_LAVA | CONTENTS_SLIME);
	public static final int MASK_OPAQUE = (CONTENTS_SOLID | CONTENTS_SLIME | CONTENTS_LAVA);
	public static final int MASK_SHOT = (CONTENTS_SOLID | CONTENTS_MONSTER | CONTENTS_WINDOW | CONTENTS_DEADMONSTER);
	public static final int MASK_CURRENT =
		(CONTENTS_CURRENT_0
			| CONTENTS_CURRENT_90
			| CONTENTS_CURRENT_180
			| CONTENTS_CURRENT_270
			| CONTENTS_CURRENT_UP
			| CONTENTS_CURRENT_DOWN);

	
	public static final int ITEM_TRIGGER_SPAWN = 0x00000001;
	public static final int ITEM_NO_TOUCH = 0x00000002;
	
	
	public static final int DROPPED_ITEM = 0x00010000;
	public static final int DROPPED_PLAYER_ITEM = 0x00020000;
	public static final int ITEM_TARGETS_USED = 0x00040000;

	
	public static final int VIDREF_GL = 1;
	public static final int VIDREF_SOFT = 2;
	public static final int VIDREF_OTHER = 3;

	
	

	public static final int FFL_SPAWNTEMP = 1;
	public static final int FFL_NOSPAWN = 2;

	
	public static final int F_INT = 0;
	public static final int F_FLOAT = 1;
	public static final int F_LSTRING = 2;
	public static final int F_GSTRING = 3;
	public static final int F_VECTOR = 4;
	public static final int F_ANGLEHACK = 5;
	public static final int F_EDICT = 6;
	public static final int F_ITEM = 7;
	public static final int F_CLIENT = 8;
	public static final int F_FUNCTION = 9;
	public static final int F_MMOVE = 10;
	public static final int F_IGNORE = 11;

	public static final int DEFAULT_BULLET_HSPREAD = 300;
	public static final int DEFAULT_BULLET_VSPREAD = 500;
	public static final int DEFAULT_SHOTGUN_HSPREAD = 1000;
	public static final int DEFAULT_SHOTGUN_VSPREAD = 500;
	public static final int DEFAULT_DEATHMATCH_SHOTGUN_COUNT = 12;
	public static final int DEFAULT_SHOTGUN_COUNT = 12;
	public static final int DEFAULT_SSHOTGUN_COUNT = 20;

	public static final int ANIM_BASIC = 0;
	public static final int ANIM_WAVE = 1;
	public static final int ANIM_JUMP = 2;
	public static final int ANIM_PAIN = 3;
	public static final int ANIM_ATTACK = 4;
	public static final int ANIM_DEATH = 5;
	public static final int ANIM_REVERSE = 6;

	public static final int AMMO_BULLETS = 0;
	public static final int AMMO_SHELLS = 1;
	public static final int AMMO_ROCKETS = 2;
	public static final int AMMO_GRENADES = 3;
	public static final int AMMO_CELLS = 4;
	public static final int AMMO_SLUGS = 5;

	
	public static final float DAMAGE_TIME = 0.5f;
	public static final float FALL_TIME = 0.3f;

	
	public static final int DAMAGE_RADIUS = 0x00000001;
	public static final int DAMAGE_NO_ARMOR = 0x00000002;
	public static final int DAMAGE_ENERGY = 0x00000004;
	public static final int DAMAGE_NO_KNOCKBACK = 0x00000008;
	public static final int DAMAGE_BULLET = 0x00000010;
	public static final int DAMAGE_NO_PROTECTION = 0x00000020;
	

	public static final int DAMAGE_NO = 0;
	public static final int DAMAGE_YES = 1;
	public static final int DAMAGE_AIM = 2;

	
	public static final int MOD_UNKNOWN = 0;
	public static final int MOD_BLASTER = 1;
	public static final int MOD_SHOTGUN = 2;
	public static final int MOD_SSHOTGUN = 3;
	public static final int MOD_MACHINEGUN = 4;
	public static final int MOD_CHAINGUN = 5;
	public static final int MOD_GRENADE = 6;
	public static final int MOD_G_SPLASH = 7;
	public static final int MOD_ROCKET = 8;
	public static final int MOD_R_SPLASH = 9;
	public static final int MOD_HYPERBLASTER = 10;
	public static final int MOD_RAILGUN = 11;
	public static final int MOD_BFG_LASER = 12;
	public static final int MOD_BFG_BLAST = 13;
	public static final int MOD_BFG_EFFECT = 14;
	public static final int MOD_HANDGRENADE = 15;
	public static final int MOD_HG_SPLASH = 16;
	public static final int MOD_WATER = 17;
	public static final int MOD_SLIME = 18;
	public static final int MOD_LAVA = 19;
	public static final int MOD_CRUSH = 20;
	public static final int MOD_TELEFRAG = 21;
	public static final int MOD_FALLING = 22;
	public static final int MOD_SUICIDE = 23;
	public static final int MOD_HELD_GRENADE = 24;
	public static final int MOD_EXPLOSIVE = 25;
	public static final int MOD_BARREL = 26;
	public static final int MOD_BOMB = 27;
	public static final int MOD_EXIT = 28;
	public static final int MOD_SPLASH = 29;
	public static final int MOD_TARGET_LASER = 30;
	public static final int MOD_TRIGGER_HURT = 31;
	public static final int MOD_HIT = 32;
	public static final int MOD_TARGET_BLASTER = 33;
	public static final int MOD_FRIENDLY_FIRE = 0x8000000;

	
	
	public static final int SPAWNFLAG_NOT_EASY = 0x00000100;
	public static final int SPAWNFLAG_NOT_MEDIUM = 0x00000200;
	public static final int SPAWNFLAG_NOT_HARD = 0x00000400;
	public static final int SPAWNFLAG_NOT_DEATHMATCH = 0x00000800;
	public static final int SPAWNFLAG_NOT_COOP = 0x00001000;

	
	public static final int FL_FLY = 0x00000001;
	public static final int FL_SWIM = 0x00000002;
	public static final int FL_IMMUNE_LASER = 0x00000004;
	public static final int FL_INWATER = 0x00000008;
	public static final int FL_GODMODE = 0x00000010;
	public static final int FL_NOTARGET = 0x00000020;
	public static final int FL_IMMUNE_SLIME = 0x00000040;
	public static final int FL_IMMUNE_LAVA = 0x00000080;
	public static final int FL_PARTIALGROUND = 0x00000100;
	public static final int FL_WATERJUMP = 0x00000200;
	public static final int FL_TEAMSLAVE = 0x00000400;
	public static final int FL_NO_KNOCKBACK = 0x00000800;
	public static final int FL_POWER_ARMOR = 0x00001000;
	public static final int FL_RESPAWN = 0x80000000;

	public static final float FRAMETIME = 0.1f;

	
	public static final int TAG_GAME = 765;
	public static final int TAG_LEVEL = 766;

	public static final int MELEE_DISTANCE = 80;

	public static final int BODY_QUEUE_SIZE = 8;

	
	public static final int DEAD_NO = 0;
	public static final int DEAD_DYING = 1;
	public static final int DEAD_DEAD = 2;
	public static final int DEAD_RESPAWNABLE = 3;

	
	public static final int RANGE_MELEE = 0;
	public static final int RANGE_NEAR = 1;
	public static final int RANGE_MID = 2;
	public static final int RANGE_FAR = 3;

	
	public static final int GIB_ORGANIC = 0;
	public static final int GIB_METALLIC = 1;

	
	public static final int AI_STAND_GROUND = 0x00000001;
	public static final int AI_TEMP_STAND_GROUND = 0x00000002;
	public static final int AI_SOUND_TARGET = 0x00000004;
	public static final int AI_LOST_SIGHT = 0x00000008;
	public static final int AI_PURSUIT_LAST_SEEN = 0x00000010;
	public static final int AI_PURSUE_NEXT = 0x00000020;
	public static final int AI_PURSUE_TEMP = 0x00000040;
	public static final int AI_HOLD_FRAME = 0x00000080;
	public static final int AI_GOOD_GUY = 0x00000100;
	public static final int AI_BRUTAL = 0x00000200;
	public static final int AI_NOSTEP = 0x00000400;
	public static final int AI_DUCKED = 0x00000800;
	public static final int AI_COMBAT_POINT = 0x00001000;
	public static final int AI_MEDIC = 0x00002000;
	public static final int AI_RESURRECTING = 0x00004000;

	
	public static final int AS_STRAIGHT = 1;
	public static final int AS_SLIDING = 2;
	public static final int AS_MELEE = 3;
	public static final int AS_MISSILE = 4;

	
	public static final int ARMOR_NONE = 0;
	public static final int ARMOR_JACKET = 1;
	public static final int ARMOR_COMBAT = 2;
	public static final int ARMOR_BODY = 3;
	public static final int ARMOR_SHARD = 4;

	
	public static final int POWER_ARMOR_NONE = 0;
	public static final int POWER_ARMOR_SCREEN = 1;
	public static final int POWER_ARMOR_SHIELD = 2;

	
	public static final int RIGHT_HANDED = 0;
	public static final int LEFT_HANDED = 1;
	public static final int CENTER_HANDED = 2;

	
	public static final int SFL_CROSS_TRIGGER_1 = 0x00000001;
	public static final int SFL_CROSS_TRIGGER_2 = 0x00000002;
	public static final int SFL_CROSS_TRIGGER_3 = 0x00000004;
	public static final int SFL_CROSS_TRIGGER_4 = 0x00000008;
	public static final int SFL_CROSS_TRIGGER_5 = 0x00000010;
	public static final int SFL_CROSS_TRIGGER_6 = 0x00000020;
	public static final int SFL_CROSS_TRIGGER_7 = 0x00000040;
	public static final int SFL_CROSS_TRIGGER_8 = 0x00000080;
	public static final int SFL_CROSS_TRIGGER_MASK = 0x000000ff;

	
	public static final int PNOISE_SELF = 0;
	public static final int PNOISE_WEAPON = 1;
	public static final int PNOISE_IMPACT = 2;

	
	public static final int IT_WEAPON = 1;
	public static final int IT_AMMO = 2;
	public static final int IT_ARMOR = 4;
	public static final int IT_STAY_COOP = 8;
	public static final int IT_KEY = 16;
	public static final int IT_POWERUP = 32;

	
	public static final int WEAP_BLASTER = 1;
	public static final int WEAP_SHOTGUN = 2;
	public static final int WEAP_SUPERSHOTGUN = 3;
	public static final int WEAP_MACHINEGUN = 4;
	public static final int WEAP_CHAINGUN = 5;
	public static final int WEAP_GRENADES = 6;
	public static final int WEAP_GRENADELAUNCHER = 7;
	public static final int WEAP_ROCKETLAUNCHER = 8;
	public static final int WEAP_HYPERBLASTER = 9;
	public static final int WEAP_RAILGUN = 10;
	public static final int WEAP_BFG = 11;

	
	public static final int MOVETYPE_NONE = 0;
	public static final int MOVETYPE_NOCLIP = 1;
	public static final int MOVETYPE_PUSH = 2;
	public static final int MOVETYPE_STOP = 3;

	public static final int MOVETYPE_WALK = 4;
	public static final int MOVETYPE_STEP = 5;
	public static final int MOVETYPE_FLY = 6;
	public static final int MOVETYPE_TOSS = 7;
	public static final int MOVETYPE_FLYMISSILE = 8;
	public static final int MOVETYPE_BOUNCE = 9;

	public static final int MULTICAST_ALL = 0;
	public static final int MULTICAST_PHS = 1;
	public static final int MULTICAST_PVS = 2;
	public static final int MULTICAST_ALL_R = 3;
	public static final int MULTICAST_PHS_R = 4;
	public static final int MULTICAST_PVS_R = 5;

	
	

	public static final int SOLID_NOT = 0;
	public static final int SOLID_TRIGGER = 1;
	public static final int SOLID_BBOX = 2;
	public static final int SOLID_BSP = 3;

	public static final int GAME_API_VERSION = 3;

	
	public static final int SVF_NOCLIENT = 0x00000001;
	public static final int SVF_DEADMONSTER = 0x00000002;
	public static final int SVF_MONSTER = 0x00000004;

	public static final int MAX_ENT_CLUSTERS = 16;

	public static final int sv_stopspeed = 100;
	public static final int sv_friction = 6;
	public static final int sv_waterfriction = 1;

	public static final int PLAT_LOW_TRIGGER = 1;

	public static final int STATE_TOP = 0;
	public static final int STATE_BOTTOM = 1;
	public static final int STATE_UP = 2;
	public static final int STATE_DOWN = 3;

	public static final int DOOR_START_OPEN = 1;
	public static final int DOOR_REVERSE = 2;
	public static final int DOOR_CRUSHER = 4;
	public static final int DOOR_NOMONSTER = 8;
	public static final int DOOR_TOGGLE = 32;
	public static final int DOOR_X_AXIS = 64;
	public static final int DOOR_Y_AXIS = 128;

	
	
	public static final int MAX_DLIGHTS = 32;
	public static final int MAX_ENTITIES = 128;
	public static final int MAX_PARTICLES = 4096;

	
	public static final int SURF_PLANEBACK = 2;
	public static final int SURF_DRAWSKY = 4;
	public static final int SURF_DRAWTURB = 0x10;
	public static final int SURF_DRAWBACKGROUND = 0x40;
	public static final int SURF_UNDERWATER = 0x80;

	public static final float POWERSUIT_SCALE = 4.0f;

	public static final int SHELL_RED_COLOR = 0xF2;
	public static final int SHELL_GREEN_COLOR = 0xD0;
	public static final int SHELL_BLUE_COLOR = 0xF3;

	public static final int SHELL_RG_COLOR = 0xDC;

	public static final int SHELL_RB_COLOR = 0x68; 
	public static final int SHELL_BG_COLOR = 0x78;

	
	public static final int SHELL_DOUBLE_COLOR = 0xDF; 
	public static final int SHELL_HALF_DAM_COLOR = 0x90;
	public static final int SHELL_CYAN_COLOR = 0x72;

	
	

	public static final int svc_bad = 0;

	
	

	public static final int svc_muzzleflash = 1;
	public static final int svc_muzzleflash2 = 2;
	public static final int svc_temp_entity = 3;
	public static final int svc_layout = 4;
	public static final int svc_inventory = 5;

	
	public static final int svc_nop = 6;
	public static final int svc_disconnect = 7;
	public static final int svc_reconnect = 8;
	public static final int svc_sound = 9;
	public static final int svc_print = 10;
	public static final int svc_stufftext = 11;
	
	public static final int svc_serverdata = 12;
	public static final int svc_configstring = 13;
	public static final int svc_spawnbaseline = 14;
	public static final int svc_centerprint = 15;
	public static final int svc_download = 16;
	public static final int svc_playerinfo = 17;
	public static final int svc_packetentities = 18;
	public static final int svc_deltapacketentities = 19;
	public static final int svc_frame = 20;

	public static final int NUMVERTEXNORMALS = 162;
	public static final int PROTOCOL_VERSION = 34;
	public static final int PORT_MASTER = 27900;
	public static final int PORT_CLIENT = 27901;
	public static final int PORT_SERVER = 27910;
	public static final int PORT_ANY = -1;

	public static final int PS_M_TYPE = (1 << 0);
	public static final int PS_M_ORIGIN = (1 << 1);
	public static final int PS_M_VELOCITY = (1 << 2);
	public static final int PS_M_TIME = (1 << 3);
	public static final int PS_M_FLAGS = (1 << 4);
	public static final int PS_M_GRAVITY = (1 << 5);
	public static final int PS_M_DELTA_ANGLES = (1 << 6);

	public static final int UPDATE_BACKUP = 16;
	
	public static final int UPDATE_MASK = (UPDATE_BACKUP - 1);

	public static final int PS_VIEWOFFSET = (1 << 7);
	public static final int PS_VIEWANGLES = (1 << 8);
	public static final int PS_KICKANGLES = (1 << 9);
	public static final int PS_BLEND = (1 << 10);
	public static final int PS_FOV = (1 << 11);
	public static final int PS_WEAPONINDEX = (1 << 12);
	public static final int PS_WEAPONFRAME = (1 << 13);
	public static final int PS_RDFLAGS = (1 << 14);

	public static final int CM_ANGLE1 = (1 << 0);
	public static final int CM_ANGLE2 = (1 << 1);
	public static final int CM_ANGLE3 = (1 << 2);
	public static final int CM_FORWARD = (1 << 3);
	public static final int CM_SIDE = (1 << 4);
	public static final int CM_UP = (1 << 5);
	public static final int CM_BUTTONS = (1 << 6);
	public static final int CM_IMPULSE = (1 << 7);

	
	public static final int U_ORIGIN1 = (1 << 0);
	public static final int U_ORIGIN2 = (1 << 1);
	public static final int U_ANGLE2 = (1 << 2);
	public static final int U_ANGLE3 = (1 << 3);
	public static final int U_FRAME8 = (1 << 4);
	public static final int U_EVENT = (1 << 5);
	public static final int U_REMOVE = (1 << 6);
	public static final int U_MOREBITS1 = (1 << 7);

	
	public static final int U_NUMBER16 = (1 << 8);
	public static final int U_ORIGIN3 = (1 << 9);
	public static final int U_ANGLE1 = (1 << 10);
	public static final int U_MODEL = (1 << 11);
	public static final int U_RENDERFX8 = (1 << 12);
	public static final int U_EFFECTS8 = (1 << 14);
	public static final int U_MOREBITS2 = (1 << 15);

	
	public static final int U_SKIN8 = (1 << 16);
	public static final int U_FRAME16 = (1 << 17);
	public static final int U_RENDERFX16 = (1 << 18);
	public static final int U_EFFECTS16 = (1 << 19);
	public static final int U_MODEL2 = (1 << 20);
	public static final int U_MODEL3 = (1 << 21);
	public static final int U_MODEL4 = (1 << 22);
	public static final int U_MOREBITS3 = (1 << 23);

	
	public static final int U_OLDORIGIN = (1 << 24);
	public static final int U_SKIN16 = (1 << 25);
	public static final int U_SOUND = (1 << 26);
	public static final int U_SOLID = (1 << 27);

	public static final int SHELL_WHITE_COLOR = 0xD7;

	public static final int MAX_TRIANGLES = 4096;
	public static final int MAX_VERTS = 2048;
	public static final int MAX_FRAMES = 512;
	public static final int MAX_MD2SKINS = 32;
	public static final int MAX_SKINNAME = 64;

	public static final int MAXLIGHTMAPS = 4;
	public static final int MIPLEVELS = 4;

	public static final int clc_bad = 0;
	public static final int clc_nop = 1;
	public static final int clc_move = 2; 
	public static final int clc_userinfo = 3; 
	public static final int clc_stringcmd = 4; 

	public static final int NS_CLIENT = 0;
	public static final int NS_SERVER = 1;

	public static final int NA_LOOPBACK = 0;
	public static final int NA_BROADCAST = 1;
	public static final int NA_IP = 2;
	public static final int NA_IPX = 3;
	public static final int NA_BROADCAST_IPX = 4;

	public static final int SND_VOLUME = (1 << 0);
	public static final int SND_ATTENUATION = (1 << 1);
	public static final int SND_POS = (1 << 2);
	public static final int SND_ENT = (1 << 3);
	public static final int SND_OFFSET = (1 << 4);

	public static final float DEFAULT_SOUND_PACKET_VOLUME = 1.0f;
	public static final float DEFAULT_SOUND_PACKET_ATTENUATION = 1.0f;

	
	
	public static final int MAX_PARSE_ENTITIES = 1024;
	public static final int MAX_CLIENTWEAPONMODELS = 20;

	public static final int CMD_BACKUP = 64; 

	public static final int ca_uninitialized = 0;
	public static final int ca_disconnected = 1;
	public static final int ca_connecting = 2;
	public static final int ca_connected = 3;
	public static final int ca_active = 4;

	public static final int MAX_ALIAS_NAME = 32;
	public static final int MAX_NUM_ARGVS = 50;

	public static final int MAX_MSGLEN = 1400;

	
	
	public static final int NUM_CON_TIMES = 4;
	public static final int CON_TEXTSIZE = 32768;

	public static final int BSPVERSION = 38;

	
	

	
	
	
	public static final int MAX_MAP_MODELS = 1024;
	public static final int MAX_MAP_BRUSHES = 8192;
	public static final int MAX_MAP_ENTITIES = 2048;
	public static final int MAX_MAP_ENTSTRING = 0x40000;
	public static final int MAX_MAP_TEXINFO = 8192;

	public static final int MAX_MAP_AREAS = 256;
	public static final int MAX_MAP_AREAPORTALS = 1024;
	public static final int MAX_MAP_PLANES = 65536;
	public static final int MAX_MAP_NODES = 65536;
	public static final int MAX_MAP_BRUSHSIDES = 65536;
	public static final int MAX_MAP_LEAFS = 65536;
	public static final int MAX_MAP_VERTS = 65536;
	public static final int MAX_MAP_FACES = 65536;
	public static final int MAX_MAP_LEAFFACES = 65536;
	public static final int MAX_MAP_LEAFBRUSHES = 65536;
	public static final int MAX_MAP_PORTALS = 65536;
	public static final int MAX_MAP_EDGES = 128000;
	public static final int MAX_MAP_SURFEDGES = 256000;
	public static final int MAX_MAP_LIGHTING = 0x200000;
	public static final int MAX_MAP_VISIBILITY = 0x100000;

	
	public static final int MAX_KEY = 32;
	public static final int MAX_VALUE = 1024;

	
	public static final int PLANE_X = 0;
	public static final int PLANE_Y = 1;
	public static final int PLANE_Z = 2;

	
	public static final int PLANE_ANYX = 3;
	public static final int PLANE_ANYY = 4;
	public static final int PLANE_ANYZ = 5;

	public static final int LUMP_ENTITIES = 0;
	public static final int LUMP_PLANES = 1;
	public static final int LUMP_VERTEXES = 2;
	public static final int LUMP_VISIBILITY = 3;
	public static final int LUMP_NODES = 4;
	public static final int LUMP_TEXINFO = 5;
	public static final int LUMP_FACES = 6;
	public static final int LUMP_LIGHTING = 7;
	public static final int LUMP_LEAFS = 8;
	public static final int LUMP_LEAFFACES = 9;
	public static final int LUMP_LEAFBRUSHES = 10;
	public static final int LUMP_EDGES = 11;
	public static final int LUMP_SURFEDGES = 12;
	public static final int LUMP_MODELS = 13;
	public static final int LUMP_BRUSHES = 14;
	public static final int LUMP_BRUSHSIDES = 15;
	public static final int LUMP_POP = 16;
	public static final int LUMP_AREAS = 17;
	public static final int LUMP_AREAPORTALS = 18;
	public static final int HEADER_LUMPS = 19;

	public static final int DTRIVERTX_V0 = 0;
	public static final int DTRIVERTX_V1 = 1;
	public static final int DTRIVERTX_V2 = 2;
	public static final int DTRIVERTX_LNI = 3;
	public static final int DTRIVERTX_SIZE = 4;

	public static final int ALIAS_VERSION = 8;
	public static final String GAMEVERSION = "baseq2";
	public static final int API_VERSION = 3; 

	public static final int DVIS_PVS = 0;
	public static final int DVIS_PHS = 1;

	
	
	public static final int key_game = 0;
	public static final int key_console = 1;
	public static final int key_message = 2;
	public static final int key_menu = 3;

	
	
	public static final int cs_free = 0; 
	public static final int cs_zombie = 1; 
	
	public static final int cs_connected = 2; 
	public static final int cs_spawned = 3;

	public static final int MAX_CHALLENGES = 1024;

	public static final int ss_dead = 0; 
	public static final int ss_loading = 1; 
	public static final int ss_game = 2; 
	public static final int ss_cinematic = 3;
	public static final int ss_demo = 4;
	public static final int ss_pic = 5;

	public static final int SV_OUTPUTBUF_LENGTH = (MAX_MSGLEN - 16);
	public static final int RD_NONE = 0;
	public static final int RD_CLIENT = 1;
	public static final int RD_PACKET = 2;

	public static final int RATE_MESSAGES = 10;

	public static final int LATENCY_COUNTS = 16;

	public static final int MAXCMDLINE = 256;

	public static final int MAX_MASTERS = 8;

	
	public static final int AREA_DEPTH = 4;
	public static final int AREA_NODES = 32;

	public static final int EXEC_NOW = 0;
	public static final int EXEC_INSERT = 1;
	public static final int EXEC_APPEND = 2;

	
	public static final int MAXMENUITEMS = 64;

	public static final int MTYPE_SLIDER = 0;
	public static final int MTYPE_LIST = 1;
	public static final int MTYPE_ACTION = 2;
	public static final int MTYPE_SPINCONTROL = 3;
	public static final int MTYPE_SEPARATOR = 4;
	public static final int MTYPE_FIELD = 5;

	public static final int K_TAB = 9;
	public static final int K_ENTER = 13;
	public static final int K_ESCAPE = 27;
	public static final int K_SPACE = 32;

	

	public static final int K_BACKSPACE = 127;
	public static final int K_UPARROW = 128;
	public static final int K_DOWNARROW = 129;
	public static final int K_LEFTARROW = 130;
	public static final int K_RIGHTARROW = 131;

	public static final int QMF_LEFT_JUSTIFY = 0x00000001;
	public static final int QMF_GRAYED = 0x00000002;
	public static final int QMF_NUMBERSONLY = 0x00000004;

	public static final int RCOLUMN_OFFSET = 16;
	public static final int LCOLUMN_OFFSET = -16;

	public static final int MAX_DISPLAYNAME = 16;
	public static final int MAX_PLAYERMODELS = 1024;

	public static final int MAX_LOCAL_SERVERS = 8;
	public static final String NO_SERVER_STRING = "<no server>";
	public static final int NUM_ADDRESSBOOK_ENTRIES = 9;

	public static final int STEPSIZE = 18;


	public static final float MOVE_STOP_EPSILON = 0.1f;
	
	public static final float MIN_STEP_NORMAL = 0.7f;


	
	public static final int FILEISREADABLE = 1;

	public static final int FILEISWRITABLE = 2;

	public static final int FILEISFILE = 4;

	public static final int FILEISDIRECTORY = 8;

	
	
    public static final boolean LITTLE_ENDIAN = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);

	public static final int SIZE_OF_SHORT = 2;

	public static final int SIZE_OF_INT = 4;

	public static final int SIZE_OF_LONG = 8;

	public static final int SIZE_OF_FLOAT = 4;

	public static final int SIZE_OF_DOUBLE = 8;

}