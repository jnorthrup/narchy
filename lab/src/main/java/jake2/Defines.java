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

import jake2.client.*;
import jake2.game.entity_state_t;
import jake2.qcommon.netadr_t;
import jake2.qcommon.sizebuf_t;

import java.nio.ByteOrder;
import java.util.Random;



public interface Defines {

	int WEAPON_READY = 0;
	int WEAPON_ACTIVATING = 1;
	int WEAPON_DROPPING = 2;
	int WEAPON_FIRING = 3;

	float GRENADE_TIMER = 3.0f;
	int GRENADE_MINSPEED = 400;
	int GRENADE_MAXSPEED = 800;

	
	

	
	int PM_NORMAL = 0;
	int PM_SPECTATOR = 1;
	
	int PM_DEAD = 2;
	int PM_GIB = 3;
	int PM_FREEZE = 4;

	int EV_NONE = 0;
	int EV_ITEM_RESPAWN = 1;
	int EV_FOOTSTEP = 2;
	int EV_FALLSHORT = 3;
	int EV_FALL = 4;
	int EV_FALLFAR = 5;
	int EV_PLAYER_TELEPORT = 6;
	int EV_OTHER_TELEPORT = 7;

	
	int PITCH = 0;
	int YAW = 1;
	int ROLL = 2;

	int MAX_STRING_CHARS = 1024;
	int MAX_STRING_TOKENS = 80;
	int MAX_TOKEN_CHARS = 1024;

	int MAX_QPATH = 64;
	int MAX_OSPATH = 128;

	
	int MAX_CLIENTS = 256;
	int MAX_EDICTS = 1024;
	int MAX_LIGHTSTYLES = 256;
	int MAX_MODELS = 256;
	int MAX_SOUNDS = 256;
	int MAX_IMAGES = 256;
	int MAX_ITEMS = 256;
	int MAX_GENERAL = (MAX_CLIENTS * 2);

	
	int PRINT_LOW = 0;
	int PRINT_MEDIUM = 1;
	int PRINT_HIGH = 2;
	int PRINT_CHAT = 3;

	int ERR_FATAL = 0;
	int ERR_DROP = 1;
	int ERR_DISCONNECT = 2;

	int PRINT_ALL = 0;
	int PRINT_DEVELOPER = 1;
	int PRINT_ALERT = 2;

	
	int MAX_INFO_KEY = 64;
	int MAX_INFO_VALUE = 64;
	int MAX_INFO_STRING = 512;

	
	int SFF_ARCH = 0x01;
	int SFF_HIDDEN = 0x02;
	int SFF_RDONLY = 0x04;
	int SFF_SUBDIR = 0x08;
	int SFF_SYSTEM = 0x10;

	int CVAR_ARCHIVE = 1;
	int CVAR_USERINFO = 2;
	int CVAR_SERVERINFO = 4;
	int CVAR_NOSET = 8;
	
	int CVAR_LATCH = 16;

	
	int CONTENTS_SOLID = 1;
	int CONTENTS_WINDOW = 2;
	int CONTENTS_AUX = 4;
	int CONTENTS_LAVA = 8;
	int CONTENTS_SLIME = 16;
	int CONTENTS_WATER = 32;
	int CONTENTS_MIST = 64;
	int LAST_VISIBLE_CONTENTS = 64;

	
	int CONTENTS_AREAPORTAL = 0x8000;

	int CONTENTS_PLAYERCLIP = 0x10000;
	int CONTENTS_MONSTERCLIP = 0x20000;

	
	int CONTENTS_CURRENT_0 = 0x40000;
	int CONTENTS_CURRENT_90 = 0x80000;
	int CONTENTS_CURRENT_180 = 0x100000;
	int CONTENTS_CURRENT_270 = 0x200000;
	int CONTENTS_CURRENT_UP = 0x400000;
	int CONTENTS_CURRENT_DOWN = 0x800000;

	int CONTENTS_ORIGIN = 0x1000000;

	int CONTENTS_MONSTER = 0x2000000;
	int CONTENTS_DEADMONSTER = 0x4000000;
	int CONTENTS_DETAIL = 0x8000000;
	int CONTENTS_TRANSLUCENT = 0x10000000;
	int CONTENTS_LADDER = 0x20000000;

	int SURF_LIGHT = 0x1;
	int SURF_SLICK = 0x2;

	int SURF_SKY = 0x4;
	int SURF_WARP = 0x8;
	int SURF_TRANS33 = 0x10;
	int SURF_TRANS66 = 0x20;
	int SURF_FLOWING = 0x40;
	int SURF_NODRAW = 0x80;

	
	
	
	int BUTTON_ATTACK = 1;
	int BUTTON_USE = 2;
	int BUTTON_ANY = 128;

	int MAXTOUCH = 32;

	
	
	
	
	
	int EF_ROTATE = 0x00000001;
	int EF_GIB = 0x00000002;
	int EF_BLASTER = 0x00000008;
	int EF_ROCKET = 0x00000010;
	int EF_GRENADE = 0x00000020;
	int EF_HYPERBLASTER = 0x00000040;
	int EF_BFG = 0x00000080;
	int EF_COLOR_SHELL = 0x00000100;
	int EF_POWERSCREEN = 0x00000200;
	int EF_ANIM01 = 0x00000400;
	int EF_ANIM23 = 0x00000800;
	int EF_ANIM_ALL = 0x00001000;
	int EF_ANIM_ALLFAST = 0x00002000;
	int EF_FLIES = 0x00004000;
	int EF_QUAD = 0x00008000;
	int EF_PENT = 0x00010000;
	int EF_TELEPORTER = 0x00020000;
	int EF_FLAG1 = 0x00040000;
	int EF_FLAG2 = 0x00080000;
	
	int EF_IONRIPPER = 0x00100000;
	int EF_GREENGIB = 0x00200000;
	int EF_BLUEHYPERBLASTER = 0x00400000;
	int EF_SPINNINGLIGHTS = 0x00800000;
	int EF_PLASMA = 0x01000000;
	int EF_TRAP = 0x02000000;

	
	int EF_TRACKER = 0x04000000;
	int EF_DOUBLE = 0x08000000;
	int EF_SPHERETRANS = 0x10000000;
	int EF_TAGTRAIL = 0x20000000;
	int EF_HALF_DAMAGE = 0x40000000;
	int EF_TRACKERTRAIL = 0x80000000;
	

	
	int RF_MINLIGHT = 1;
	int RF_VIEWERMODEL = 2;
	int RF_WEAPONMODEL = 4;
	int RF_FULLBRIGHT = 8;
	int RF_DEPTHHACK = 16;
	int RF_TRANSLUCENT = 32;
	int RF_FRAMELERP = 64;
	int RF_BEAM = 128;
	int RF_CUSTOMSKIN = 256;
	int RF_GLOW = 512;
	int RF_SHELL_RED = 1024;
	int RF_SHELL_GREEN = 2048;
	int RF_SHELL_BLUE = 4096;

	
	int RF_IR_VISIBLE = 0x00008000;
	int RF_SHELL_DOUBLE = 0x00010000;
	int RF_SHELL_HALF_DAM = 0x00020000;
	int RF_USE_DISGUISE = 0x00040000;
	

	
	int RDF_UNDERWATER = 1;
	int RDF_NOWORLDMODEL = 2;

	
	int RDF_IRGOGGLES = 4;
	int RDF_UVGOGGLES = 8;
	

	
	int MZ_BLASTER = 0;
	int MZ_MACHINEGUN = 1;
	int MZ_SHOTGUN = 2;
	int MZ_CHAINGUN1 = 3;
	int MZ_CHAINGUN2 = 4;
	int MZ_CHAINGUN3 = 5;
	int MZ_RAILGUN = 6;
	int MZ_ROCKET = 7;
	int MZ_GRENADE = 8;
	int MZ_LOGIN = 9;
	int MZ_LOGOUT = 10;
	int MZ_RESPAWN = 11;
	int MZ_BFG = 12;
	int MZ_SSHOTGUN = 13;
	int MZ_HYPERBLASTER = 14;
	int MZ_ITEMRESPAWN = 15;
	
	int MZ_IONRIPPER = 16;
	int MZ_BLUEHYPERBLASTER = 17;
	int MZ_PHALANX = 18;
	int MZ_SILENCED = 128;

	
	int MZ_ETF_RIFLE = 30;
	int MZ_UNUSED = 31;
	int MZ_SHOTGUN2 = 32;
	int MZ_HEATBEAM = 33;
	int MZ_BLASTER2 = 34;
	int MZ_TRACKER = 35;
	int MZ_NUKE1 = 36;
	int MZ_NUKE2 = 37;
	int MZ_NUKE4 = 38;
	int MZ_NUKE8 = 39;
	

	
	
	
	int MZ2_TANK_BLASTER_1 = 1;
	int MZ2_TANK_BLASTER_2 = 2;
	int MZ2_TANK_BLASTER_3 = 3;
	int MZ2_TANK_MACHINEGUN_1 = 4;
	int MZ2_TANK_MACHINEGUN_2 = 5;
	int MZ2_TANK_MACHINEGUN_3 = 6;
	int MZ2_TANK_MACHINEGUN_4 = 7;
	int MZ2_TANK_MACHINEGUN_5 = 8;
	int MZ2_TANK_MACHINEGUN_6 = 9;
	int MZ2_TANK_MACHINEGUN_7 = 10;
	int MZ2_TANK_MACHINEGUN_8 = 11;
	int MZ2_TANK_MACHINEGUN_9 = 12;
	int MZ2_TANK_MACHINEGUN_10 = 13;
	int MZ2_TANK_MACHINEGUN_11 = 14;
	int MZ2_TANK_MACHINEGUN_12 = 15;
	int MZ2_TANK_MACHINEGUN_13 = 16;
	int MZ2_TANK_MACHINEGUN_14 = 17;
	int MZ2_TANK_MACHINEGUN_15 = 18;
	int MZ2_TANK_MACHINEGUN_16 = 19;
	int MZ2_TANK_MACHINEGUN_17 = 20;
	int MZ2_TANK_MACHINEGUN_18 = 21;
	int MZ2_TANK_MACHINEGUN_19 = 22;
	int MZ2_TANK_ROCKET_1 = 23;
	int MZ2_TANK_ROCKET_2 = 24;
	int MZ2_TANK_ROCKET_3 = 25;

	int MZ2_INFANTRY_MACHINEGUN_1 = 26;
	int MZ2_INFANTRY_MACHINEGUN_2 = 27;
	int MZ2_INFANTRY_MACHINEGUN_3 = 28;
	int MZ2_INFANTRY_MACHINEGUN_4 = 29;
	int MZ2_INFANTRY_MACHINEGUN_5 = 30;
	int MZ2_INFANTRY_MACHINEGUN_6 = 31;
	int MZ2_INFANTRY_MACHINEGUN_7 = 32;
	int MZ2_INFANTRY_MACHINEGUN_8 = 33;
	int MZ2_INFANTRY_MACHINEGUN_9 = 34;
	int MZ2_INFANTRY_MACHINEGUN_10 = 35;
	int MZ2_INFANTRY_MACHINEGUN_11 = 36;
	int MZ2_INFANTRY_MACHINEGUN_12 = 37;
	int MZ2_INFANTRY_MACHINEGUN_13 = 38;

	int MZ2_SOLDIER_BLASTER_1 = 39;
	int MZ2_SOLDIER_BLASTER_2 = 40;
	int MZ2_SOLDIER_SHOTGUN_1 = 41;
	int MZ2_SOLDIER_SHOTGUN_2 = 42;
	int MZ2_SOLDIER_MACHINEGUN_1 = 43;
	int MZ2_SOLDIER_MACHINEGUN_2 = 44;

	int MZ2_GUNNER_MACHINEGUN_1 = 45;
	int MZ2_GUNNER_MACHINEGUN_2 = 46;
	int MZ2_GUNNER_MACHINEGUN_3 = 47;
	int MZ2_GUNNER_MACHINEGUN_4 = 48;
	int MZ2_GUNNER_MACHINEGUN_5 = 49;
	int MZ2_GUNNER_MACHINEGUN_6 = 50;
	int MZ2_GUNNER_MACHINEGUN_7 = 51;
	int MZ2_GUNNER_MACHINEGUN_8 = 52;
	int MZ2_GUNNER_GRENADE_1 = 53;
	int MZ2_GUNNER_GRENADE_2 = 54;
	int MZ2_GUNNER_GRENADE_3 = 55;
	int MZ2_GUNNER_GRENADE_4 = 56;

	int MZ2_CHICK_ROCKET_1 = 57;

	int MZ2_FLYER_BLASTER_1 = 58;
	int MZ2_FLYER_BLASTER_2 = 59;

	int MZ2_MEDIC_BLASTER_1 = 60;

	int MZ2_GLADIATOR_RAILGUN_1 = 61;

	int MZ2_HOVER_BLASTER_1 = 62;

	int MZ2_ACTOR_MACHINEGUN_1 = 63;

	int MZ2_SUPERTANK_MACHINEGUN_1 = 64;
	int MZ2_SUPERTANK_MACHINEGUN_2 = 65;
	int MZ2_SUPERTANK_MACHINEGUN_3 = 66;
	int MZ2_SUPERTANK_MACHINEGUN_4 = 67;
	int MZ2_SUPERTANK_MACHINEGUN_5 = 68;
	int MZ2_SUPERTANK_MACHINEGUN_6 = 69;
	int MZ2_SUPERTANK_ROCKET_1 = 70;
	int MZ2_SUPERTANK_ROCKET_2 = 71;
	int MZ2_SUPERTANK_ROCKET_3 = 72;

	int MZ2_BOSS2_MACHINEGUN_L1 = 73;
	int MZ2_BOSS2_MACHINEGUN_L2 = 74;
	int MZ2_BOSS2_MACHINEGUN_L3 = 75;
	int MZ2_BOSS2_MACHINEGUN_L4 = 76;
	int MZ2_BOSS2_MACHINEGUN_L5 = 77;
	int MZ2_BOSS2_ROCKET_1 = 78;
	int MZ2_BOSS2_ROCKET_2 = 79;
	int MZ2_BOSS2_ROCKET_3 = 80;
	int MZ2_BOSS2_ROCKET_4 = 81;

	int MZ2_FLOAT_BLASTER_1 = 82;

	int MZ2_SOLDIER_BLASTER_3 = 83;
	int MZ2_SOLDIER_SHOTGUN_3 = 84;
	int MZ2_SOLDIER_MACHINEGUN_3 = 85;
	int MZ2_SOLDIER_BLASTER_4 = 86;
	int MZ2_SOLDIER_SHOTGUN_4 = 87;
	int MZ2_SOLDIER_MACHINEGUN_4 = 88;
	int MZ2_SOLDIER_BLASTER_5 = 89;
	int MZ2_SOLDIER_SHOTGUN_5 = 90;
	int MZ2_SOLDIER_MACHINEGUN_5 = 91;
	int MZ2_SOLDIER_BLASTER_6 = 92;
	int MZ2_SOLDIER_SHOTGUN_6 = 93;
	int MZ2_SOLDIER_MACHINEGUN_6 = 94;
	int MZ2_SOLDIER_BLASTER_7 = 95;
	int MZ2_SOLDIER_SHOTGUN_7 = 96;
	int MZ2_SOLDIER_MACHINEGUN_7 = 97;
	int MZ2_SOLDIER_BLASTER_8 = 98;
	int MZ2_SOLDIER_SHOTGUN_8 = 99;
	int MZ2_SOLDIER_MACHINEGUN_8 = 100;

	
	int MZ2_MAKRON_BFG = 101;
	int MZ2_MAKRON_BLASTER_1 = 102;
	int MZ2_MAKRON_BLASTER_2 = 103;
	int MZ2_MAKRON_BLASTER_3 = 104;
	int MZ2_MAKRON_BLASTER_4 = 105;
	int MZ2_MAKRON_BLASTER_5 = 106;
	int MZ2_MAKRON_BLASTER_6 = 107;
	int MZ2_MAKRON_BLASTER_7 = 108;
	int MZ2_MAKRON_BLASTER_8 = 109;
	int MZ2_MAKRON_BLASTER_9 = 110;
	int MZ2_MAKRON_BLASTER_10 = 111;
	int MZ2_MAKRON_BLASTER_11 = 112;
	int MZ2_MAKRON_BLASTER_12 = 113;
	int MZ2_MAKRON_BLASTER_13 = 114;
	int MZ2_MAKRON_BLASTER_14 = 115;
	int MZ2_MAKRON_BLASTER_15 = 116;
	int MZ2_MAKRON_BLASTER_16 = 117;
	int MZ2_MAKRON_BLASTER_17 = 118;
	int MZ2_MAKRON_RAILGUN_1 = 119;
	int MZ2_JORG_MACHINEGUN_L1 = 120;
	int MZ2_JORG_MACHINEGUN_L2 = 121;
	int MZ2_JORG_MACHINEGUN_L3 = 122;
	int MZ2_JORG_MACHINEGUN_L4 = 123;
	int MZ2_JORG_MACHINEGUN_L5 = 124;
	int MZ2_JORG_MACHINEGUN_L6 = 125;
	int MZ2_JORG_MACHINEGUN_R1 = 126;
	int MZ2_JORG_MACHINEGUN_R2 = 127;
	int MZ2_JORG_MACHINEGUN_R3 = 128;
	int MZ2_JORG_MACHINEGUN_R4 = 129;
	int MZ2_JORG_MACHINEGUN_R5 = 130;
	int MZ2_JORG_MACHINEGUN_R6 = 131;
	int MZ2_JORG_BFG_1 = 132;
	int MZ2_BOSS2_MACHINEGUN_R1 = 133;
	int MZ2_BOSS2_MACHINEGUN_R2 = 134;
	int MZ2_BOSS2_MACHINEGUN_R3 = 135;
	int MZ2_BOSS2_MACHINEGUN_R4 = 136;
	int MZ2_BOSS2_MACHINEGUN_R5 = 137;

	
	int MZ2_CARRIER_MACHINEGUN_L1 = 138;
	int MZ2_CARRIER_MACHINEGUN_R1 = 139;
	int MZ2_CARRIER_GRENADE = 140;
	int MZ2_TURRET_MACHINEGUN = 141;
	int MZ2_TURRET_ROCKET = 142;
	int MZ2_TURRET_BLASTER = 143;
	int MZ2_STALKER_BLASTER = 144;
	int MZ2_DAEDALUS_BLASTER = 145;
	int MZ2_MEDIC_BLASTER_2 = 146;
	int MZ2_CARRIER_RAILGUN = 147;
	int MZ2_WIDOW_DISRUPTOR = 148;
	int MZ2_WIDOW_BLASTER = 149;
	int MZ2_WIDOW_RAIL = 150;
	int MZ2_WIDOW_PLASMABEAM = 151;
	int MZ2_CARRIER_MACHINEGUN_L2 = 152;
	int MZ2_CARRIER_MACHINEGUN_R2 = 153;
	int MZ2_WIDOW_RAIL_LEFT = 154;
	int MZ2_WIDOW_RAIL_RIGHT = 155;
	int MZ2_WIDOW_BLASTER_SWEEP1 = 156;
	int MZ2_WIDOW_BLASTER_SWEEP2 = 157;
	int MZ2_WIDOW_BLASTER_SWEEP3 = 158;
	int MZ2_WIDOW_BLASTER_SWEEP4 = 159;
	int MZ2_WIDOW_BLASTER_SWEEP5 = 160;
	int MZ2_WIDOW_BLASTER_SWEEP6 = 161;
	int MZ2_WIDOW_BLASTER_SWEEP7 = 162;
	int MZ2_WIDOW_BLASTER_SWEEP8 = 163;
	int MZ2_WIDOW_BLASTER_SWEEP9 = 164;
	int MZ2_WIDOW_BLASTER_100 = 165;
	int MZ2_WIDOW_BLASTER_90 = 166;
	int MZ2_WIDOW_BLASTER_80 = 167;
	int MZ2_WIDOW_BLASTER_70 = 168;
	int MZ2_WIDOW_BLASTER_60 = 169;
	int MZ2_WIDOW_BLASTER_50 = 170;
	int MZ2_WIDOW_BLASTER_40 = 171;
	int MZ2_WIDOW_BLASTER_30 = 172;
	int MZ2_WIDOW_BLASTER_20 = 173;
	int MZ2_WIDOW_BLASTER_10 = 174;
	int MZ2_WIDOW_BLASTER_0 = 175;
	int MZ2_WIDOW_BLASTER_10L = 176;
	int MZ2_WIDOW_BLASTER_20L = 177;
	int MZ2_WIDOW_BLASTER_30L = 178;
	int MZ2_WIDOW_BLASTER_40L = 179;
	int MZ2_WIDOW_BLASTER_50L = 180;
	int MZ2_WIDOW_BLASTER_60L = 181;
	int MZ2_WIDOW_BLASTER_70L = 182;
	int MZ2_WIDOW_RUN_1 = 183;
	int MZ2_WIDOW_RUN_2 = 184;
	int MZ2_WIDOW_RUN_3 = 185;
	int MZ2_WIDOW_RUN_4 = 186;
	int MZ2_WIDOW_RUN_5 = 187;
	int MZ2_WIDOW_RUN_6 = 188;
	int MZ2_WIDOW_RUN_7 = 189;
	int MZ2_WIDOW_RUN_8 = 190;
	int MZ2_CARRIER_ROCKET_1 = 191;
	int MZ2_CARRIER_ROCKET_2 = 192;
	int MZ2_CARRIER_ROCKET_3 = 193;
	int MZ2_CARRIER_ROCKET_4 = 194;
	int MZ2_WIDOW2_BEAMER_1 = 195;
	int MZ2_WIDOW2_BEAMER_2 = 196;
	int MZ2_WIDOW2_BEAMER_3 = 197;
	int MZ2_WIDOW2_BEAMER_4 = 198;
	int MZ2_WIDOW2_BEAMER_5 = 199;
	int MZ2_WIDOW2_BEAM_SWEEP_1 = 200;
	int MZ2_WIDOW2_BEAM_SWEEP_2 = 201;
	int MZ2_WIDOW2_BEAM_SWEEP_3 = 202;
	int MZ2_WIDOW2_BEAM_SWEEP_4 = 203;
	int MZ2_WIDOW2_BEAM_SWEEP_5 = 204;
	int MZ2_WIDOW2_BEAM_SWEEP_6 = 205;
	int MZ2_WIDOW2_BEAM_SWEEP_7 = 206;
	int MZ2_WIDOW2_BEAM_SWEEP_8 = 207;
	int MZ2_WIDOW2_BEAM_SWEEP_9 = 208;
	int MZ2_WIDOW2_BEAM_SWEEP_10 = 209;
	int MZ2_WIDOW2_BEAM_SWEEP_11 = 210;

	int SPLASH_UNKNOWN = 0;
	int SPLASH_SPARKS = 1;
	int SPLASH_BLUE_WATER = 2;
	int SPLASH_BROWN_WATER = 3;
	int SPLASH_SLIME = 4;
	int SPLASH_LAVA = 5;
	int SPLASH_BLOOD = 6;

	
	
	
	int CHAN_AUTO = 0;
	int CHAN_WEAPON = 1;
	int CHAN_VOICE = 2;
	int CHAN_ITEM = 3;
	int CHAN_BODY = 4;
	
	int CHAN_NO_PHS_ADD = 8;
	
	int CHAN_RELIABLE = 16;

	
	int ATTN_NONE = 0;
	int ATTN_NORM = 1;
	int ATTN_IDLE = 2;
	int ATTN_STATIC = 3;

	
	int STAT_HEALTH_ICON = 0;
	int STAT_HEALTH = 1;
	int STAT_AMMO_ICON = 2;
	int STAT_AMMO = 3;
	int STAT_ARMOR_ICON = 4;
	int STAT_ARMOR = 5;
	int STAT_SELECTED_ICON = 6;
	int STAT_PICKUP_ICON = 7;
	int STAT_PICKUP_STRING = 8;
	int STAT_TIMER_ICON = 9;
	int STAT_TIMER = 10;
	int STAT_HELPICON = 11;
	int STAT_SELECTED_ITEM = 12;
	int STAT_LAYOUTS = 13;
	int STAT_FRAGS = 14;
	int STAT_FLASHES = 15;
	int STAT_CHASE = 16;
	int STAT_SPECTATOR = 17;

	int MAX_STATS = 32;

	
	int DF_NO_HEALTH = 0x00000001;
	int DF_NO_ITEMS = 0x00000002;
	int DF_WEAPONS_STAY = 0x00000004;
	int DF_NO_FALLING = 0x00000008;
	int DF_INSTANT_ITEMS = 0x00000010;
	int DF_SAME_LEVEL = 0x00000020;
	int DF_SKINTEAMS = 0x00000040;
	int DF_MODELTEAMS = 0x00000080;
	int DF_NO_FRIENDLY_FIRE = 0x00000100;
	int DF_SPAWN_FARTHEST = 0x00000200;
	int DF_FORCE_RESPAWN = 0x00000400;
	int DF_NO_ARMOR = 0x00000800;
	int DF_ALLOW_EXIT = 0x00001000;
	int DF_INFINITE_AMMO = 0x00002000;
	int DF_QUAD_DROP = 0x00004000;
	int DF_FIXED_FOV = 0x00008000;

	
	int DF_QUADFIRE_DROP = 0x00010000;

	
	int DF_NO_MINES = 0x00020000;
	int DF_NO_STACK_DOUBLE = 0x00040000;
	int DF_NO_NUKES = 0x00080000;
	int DF_NO_SPHERES = 0x00100000;
	

	
	
	
	
	
	int CS_NAME = 0;
	int CS_CDTRACK = 1;
	int CS_SKY = 2;
	int CS_SKYAXIS = 3;
	int CS_SKYROTATE = 4;
	int CS_STATUSBAR = 5;

	int CS_AIRACCEL = 29;
	int CS_MAXCLIENTS = 30;
	int CS_MAPCHECKSUM = 31;

	int CS_MODELS = 32;
	int CS_SOUNDS = (CS_MODELS + MAX_MODELS);
	int CS_IMAGES = (CS_SOUNDS + MAX_SOUNDS);
	int CS_LIGHTS = (CS_IMAGES + MAX_IMAGES);
	int CS_ITEMS = (CS_LIGHTS + MAX_LIGHTSTYLES);
	int CS_PLAYERSKINS = (CS_ITEMS + MAX_ITEMS);
	int CS_GENERAL = (CS_PLAYERSKINS + MAX_CLIENTS);
	int MAX_CONFIGSTRINGS = (CS_GENERAL + MAX_GENERAL);

	int HEALTH_IGNORE_MAX = 1;
	int HEALTH_TIMED = 2;

	
	
	int AREA_SOLID = 1;
	int AREA_TRIGGERS = 2;

	int TE_GUNSHOT = 0;
	int TE_BLOOD = 1;
	int TE_BLASTER = 2;
	int TE_RAILTRAIL = 3;
	int TE_SHOTGUN = 4;
	int TE_EXPLOSION1 = 5;
	int TE_EXPLOSION2 = 6;
	int TE_ROCKET_EXPLOSION = 7;
	int TE_GRENADE_EXPLOSION = 8;
	int TE_SPARKS = 9;
	int TE_SPLASH = 10;
	int TE_BUBBLETRAIL = 11;
	int TE_SCREEN_SPARKS = 12;
	int TE_SHIELD_SPARKS = 13;
	int TE_BULLET_SPARKS = 14;
	int TE_LASER_SPARKS = 15;
	int TE_PARASITE_ATTACK = 16;
	int TE_ROCKET_EXPLOSION_WATER = 17;
	int TE_GRENADE_EXPLOSION_WATER = 18;
	int TE_MEDIC_CABLE_ATTACK = 19;
	int TE_BFG_EXPLOSION = 20;
	int TE_BFG_BIGEXPLOSION = 21;
	int TE_BOSSTPORT = 22;
	int TE_BFG_LASER = 23;
	int TE_GRAPPLE_CABLE = 24;
	int TE_WELDING_SPARKS = 25;
	int TE_GREENBLOOD = 26;
	int TE_BLUEHYPERBLASTER = 27;
	int TE_PLASMA_EXPLOSION = 28;
	int TE_TUNNEL_SPARKS = 29;
	
	int TE_BLASTER2 = 30;
	int TE_RAILTRAIL2 = 31;
	int TE_FLAME = 32;
	int TE_LIGHTNING = 33;
	int TE_DEBUGTRAIL = 34;
	int TE_PLAIN_EXPLOSION = 35;
	int TE_FLASHLIGHT = 36;
	int TE_FORCEWALL = 37;
	int TE_HEATBEAM = 38;
	int TE_MONSTER_HEATBEAM = 39;
	int TE_STEAM = 40;
	int TE_BUBBLETRAIL2 = 41;
	int TE_MOREBLOOD = 42;
	int TE_HEATBEAM_SPARKS = 43;
	int TE_HEATBEAM_STEAM = 44;
	int TE_CHAINFIST_SMOKE = 45;
	int TE_ELECTRIC_SPARKS = 46;
	int TE_TRACKER_EXPLOSION = 47;
	int TE_TELEPORT_EFFECT = 48;
	int TE_DBALL_GOAL = 49;
	int TE_WIDOWBEAMOUT = 50;
	int TE_NUKEBLAST = 51;
	int TE_WIDOWSPLASH = 52;
	int TE_EXPLOSION1_BIG = 53;
	int TE_EXPLOSION1_NP = 54;
	int TE_FLECHETTE = 55;

	
	int MASK_ALL = (-1);
	int MASK_SOLID = (CONTENTS_SOLID | CONTENTS_WINDOW);
	int MASK_PLAYERSOLID = (CONTENTS_SOLID | CONTENTS_PLAYERCLIP | CONTENTS_WINDOW | CONTENTS_MONSTER);
	int MASK_DEADSOLID = (CONTENTS_SOLID | CONTENTS_PLAYERCLIP | CONTENTS_WINDOW);
	int MASK_MONSTERSOLID = (CONTENTS_SOLID | CONTENTS_MONSTERCLIP | CONTENTS_WINDOW | CONTENTS_MONSTER);
	int MASK_WATER = (CONTENTS_WATER | CONTENTS_LAVA | CONTENTS_SLIME);
	int MASK_OPAQUE = (CONTENTS_SOLID | CONTENTS_SLIME | CONTENTS_LAVA);
	int MASK_SHOT = (CONTENTS_SOLID | CONTENTS_MONSTER | CONTENTS_WINDOW | CONTENTS_DEADMONSTER);
	int MASK_CURRENT =
		(CONTENTS_CURRENT_0
			| CONTENTS_CURRENT_90
			| CONTENTS_CURRENT_180
			| CONTENTS_CURRENT_270
			| CONTENTS_CURRENT_UP
			| CONTENTS_CURRENT_DOWN);

	
	int ITEM_TRIGGER_SPAWN = 0x00000001;
	int ITEM_NO_TOUCH = 0x00000002;
	
	
	int DROPPED_ITEM = 0x00010000;
	int DROPPED_PLAYER_ITEM = 0x00020000;
	int ITEM_TARGETS_USED = 0x00040000;

	
	int VIDREF_GL = 1;
	int VIDREF_SOFT = 2;
	int VIDREF_OTHER = 3;

	
	

	int FFL_SPAWNTEMP = 1;
	int FFL_NOSPAWN = 2;

	
	int F_INT = 0;
	int F_FLOAT = 1;
	int F_LSTRING = 2;
	int F_GSTRING = 3;
	int F_VECTOR = 4;
	int F_ANGLEHACK = 5;
	int F_EDICT = 6;
	int F_ITEM = 7;
	int F_CLIENT = 8;
	int F_FUNCTION = 9;
	int F_MMOVE = 10;
	int F_IGNORE = 11;

	int DEFAULT_BULLET_HSPREAD = 300;
	int DEFAULT_BULLET_VSPREAD = 500;
	int DEFAULT_SHOTGUN_HSPREAD = 1000;
	int DEFAULT_SHOTGUN_VSPREAD = 500;
	int DEFAULT_DEATHMATCH_SHOTGUN_COUNT = 12;
	int DEFAULT_SHOTGUN_COUNT = 12;
	int DEFAULT_SSHOTGUN_COUNT = 20;

	int ANIM_BASIC = 0;
	int ANIM_WAVE = 1;
	int ANIM_JUMP = 2;
	int ANIM_PAIN = 3;
	int ANIM_ATTACK = 4;
	int ANIM_DEATH = 5;
	int ANIM_REVERSE = 6;

	int AMMO_BULLETS = 0;
	int AMMO_SHELLS = 1;
	int AMMO_ROCKETS = 2;
	int AMMO_GRENADES = 3;
	int AMMO_CELLS = 4;
	int AMMO_SLUGS = 5;

	
	float DAMAGE_TIME = 0.5f;
	float FALL_TIME = 0.3f;

	
	int DAMAGE_RADIUS = 0x00000001;
	int DAMAGE_NO_ARMOR = 0x00000002;
	int DAMAGE_ENERGY = 0x00000004;
	int DAMAGE_NO_KNOCKBACK = 0x00000008;
	int DAMAGE_BULLET = 0x00000010;
	int DAMAGE_NO_PROTECTION = 0x00000020;
	

	int DAMAGE_NO = 0;
	int DAMAGE_YES = 1;
	int DAMAGE_AIM = 2;

	
	int MOD_UNKNOWN = 0;
	int MOD_BLASTER = 1;
	int MOD_SHOTGUN = 2;
	int MOD_SSHOTGUN = 3;
	int MOD_MACHINEGUN = 4;
	int MOD_CHAINGUN = 5;
	int MOD_GRENADE = 6;
	int MOD_G_SPLASH = 7;
	int MOD_ROCKET = 8;
	int MOD_R_SPLASH = 9;
	int MOD_HYPERBLASTER = 10;
	int MOD_RAILGUN = 11;
	int MOD_BFG_LASER = 12;
	int MOD_BFG_BLAST = 13;
	int MOD_BFG_EFFECT = 14;
	int MOD_HANDGRENADE = 15;
	int MOD_HG_SPLASH = 16;
	int MOD_WATER = 17;
	int MOD_SLIME = 18;
	int MOD_LAVA = 19;
	int MOD_CRUSH = 20;
	int MOD_TELEFRAG = 21;
	int MOD_FALLING = 22;
	int MOD_SUICIDE = 23;
	int MOD_HELD_GRENADE = 24;
	int MOD_EXPLOSIVE = 25;
	int MOD_BARREL = 26;
	int MOD_BOMB = 27;
	int MOD_EXIT = 28;
	int MOD_SPLASH = 29;
	int MOD_TARGET_LASER = 30;
	int MOD_TRIGGER_HURT = 31;
	int MOD_HIT = 32;
	int MOD_TARGET_BLASTER = 33;
	int MOD_FRIENDLY_FIRE = 0x8000000;

	
	
	int SPAWNFLAG_NOT_EASY = 0x00000100;
	int SPAWNFLAG_NOT_MEDIUM = 0x00000200;
	int SPAWNFLAG_NOT_HARD = 0x00000400;
	int SPAWNFLAG_NOT_DEATHMATCH = 0x00000800;
	int SPAWNFLAG_NOT_COOP = 0x00001000;

	
	int FL_FLY = 0x00000001;
	int FL_SWIM = 0x00000002;
	int FL_IMMUNE_LASER = 0x00000004;
	int FL_INWATER = 0x00000008;
	int FL_GODMODE = 0x00000010;
	int FL_NOTARGET = 0x00000020;
	int FL_IMMUNE_SLIME = 0x00000040;
	int FL_IMMUNE_LAVA = 0x00000080;
	int FL_PARTIALGROUND = 0x00000100;
	int FL_WATERJUMP = 0x00000200;
	int FL_TEAMSLAVE = 0x00000400;
	int FL_NO_KNOCKBACK = 0x00000800;
	int FL_POWER_ARMOR = 0x00001000;
	int FL_RESPAWN = 0x80000000;

	float FRAMETIME = 0.1f;

	
	int TAG_GAME = 765;
	int TAG_LEVEL = 766;

	int MELEE_DISTANCE = 80;

	int BODY_QUEUE_SIZE = 8;

	
	int DEAD_NO = 0;
	int DEAD_DYING = 1;
	int DEAD_DEAD = 2;
	int DEAD_RESPAWNABLE = 3;

	
	int RANGE_MELEE = 0;
	int RANGE_NEAR = 1;
	int RANGE_MID = 2;
	int RANGE_FAR = 3;

	
	int GIB_ORGANIC = 0;
	int GIB_METALLIC = 1;

	
	int AI_STAND_GROUND = 0x00000001;
	int AI_TEMP_STAND_GROUND = 0x00000002;
	int AI_SOUND_TARGET = 0x00000004;
	int AI_LOST_SIGHT = 0x00000008;
	int AI_PURSUIT_LAST_SEEN = 0x00000010;
	int AI_PURSUE_NEXT = 0x00000020;
	int AI_PURSUE_TEMP = 0x00000040;
	int AI_HOLD_FRAME = 0x00000080;
	int AI_GOOD_GUY = 0x00000100;
	int AI_BRUTAL = 0x00000200;
	int AI_NOSTEP = 0x00000400;
	int AI_DUCKED = 0x00000800;
	int AI_COMBAT_POINT = 0x00001000;
	int AI_MEDIC = 0x00002000;
	int AI_RESURRECTING = 0x00004000;

	
	int AS_STRAIGHT = 1;
	int AS_SLIDING = 2;
	int AS_MELEE = 3;
	int AS_MISSILE = 4;

	
	int ARMOR_NONE = 0;
	int ARMOR_JACKET = 1;
	int ARMOR_COMBAT = 2;
	int ARMOR_BODY = 3;
	int ARMOR_SHARD = 4;

	
	int POWER_ARMOR_NONE = 0;
	int POWER_ARMOR_SCREEN = 1;
	int POWER_ARMOR_SHIELD = 2;

	
	int RIGHT_HANDED = 0;
	int LEFT_HANDED = 1;
	int CENTER_HANDED = 2;

	
	int SFL_CROSS_TRIGGER_1 = 0x00000001;
	int SFL_CROSS_TRIGGER_2 = 0x00000002;
	int SFL_CROSS_TRIGGER_3 = 0x00000004;
	int SFL_CROSS_TRIGGER_4 = 0x00000008;
	int SFL_CROSS_TRIGGER_5 = 0x00000010;
	int SFL_CROSS_TRIGGER_6 = 0x00000020;
	int SFL_CROSS_TRIGGER_7 = 0x00000040;
	int SFL_CROSS_TRIGGER_8 = 0x00000080;
	int SFL_CROSS_TRIGGER_MASK = 0x000000ff;

	
	int PNOISE_SELF = 0;
	int PNOISE_WEAPON = 1;
	int PNOISE_IMPACT = 2;

	
	int IT_WEAPON = 1;
	int IT_AMMO = 2;
	int IT_ARMOR = 4;
	int IT_STAY_COOP = 8;
	int IT_KEY = 16;
	int IT_POWERUP = 32;

	
	int WEAP_BLASTER = 1;
	int WEAP_SHOTGUN = 2;
	int WEAP_SUPERSHOTGUN = 3;
	int WEAP_MACHINEGUN = 4;
	int WEAP_CHAINGUN = 5;
	int WEAP_GRENADES = 6;
	int WEAP_GRENADELAUNCHER = 7;
	int WEAP_ROCKETLAUNCHER = 8;
	int WEAP_HYPERBLASTER = 9;
	int WEAP_RAILGUN = 10;
	int WEAP_BFG = 11;

	
	int MOVETYPE_NONE = 0;
	int MOVETYPE_NOCLIP = 1;
	int MOVETYPE_PUSH = 2;
	int MOVETYPE_STOP = 3;

	int MOVETYPE_WALK = 4;
	int MOVETYPE_STEP = 5;
	int MOVETYPE_FLY = 6;
	int MOVETYPE_TOSS = 7;
	int MOVETYPE_FLYMISSILE = 8;
	int MOVETYPE_BOUNCE = 9;

	int MULTICAST_ALL = 0;
	int MULTICAST_PHS = 1;
	int MULTICAST_PVS = 2;
	int MULTICAST_ALL_R = 3;
	int MULTICAST_PHS_R = 4;
	int MULTICAST_PVS_R = 5;

	
	

	int SOLID_NOT = 0;
	int SOLID_TRIGGER = 1;
	int SOLID_BBOX = 2;
	int SOLID_BSP = 3;

	int GAME_API_VERSION = 3;

	
	int SVF_NOCLIENT = 0x00000001;
	int SVF_DEADMONSTER = 0x00000002;
	int SVF_MONSTER = 0x00000004;

	int MAX_ENT_CLUSTERS = 16;

	int sv_stopspeed = 100;
	int sv_friction = 6;
	int sv_waterfriction = 1;

	int PLAT_LOW_TRIGGER = 1;

	int STATE_TOP = 0;
	int STATE_BOTTOM = 1;
	int STATE_UP = 2;
	int STATE_DOWN = 3;

	int DOOR_START_OPEN = 1;
	int DOOR_REVERSE = 2;
	int DOOR_CRUSHER = 4;
	int DOOR_NOMONSTER = 8;
	int DOOR_TOGGLE = 32;
	int DOOR_X_AXIS = 64;
	int DOOR_Y_AXIS = 128;

	
	
	int MAX_DLIGHTS = 32;
	int MAX_ENTITIES = 128;
	int MAX_PARTICLES = 4096;

	
	int SURF_PLANEBACK = 2;
	int SURF_DRAWSKY = 4;
	int SURF_DRAWTURB = 0x10;
	int SURF_DRAWBACKGROUND = 0x40;
	int SURF_UNDERWATER = 0x80;

	float POWERSUIT_SCALE = 4.0f;

	int SHELL_RED_COLOR = 0xF2;
	int SHELL_GREEN_COLOR = 0xD0;
	int SHELL_BLUE_COLOR = 0xF3;

	int SHELL_RG_COLOR = 0xDC;

	int SHELL_RB_COLOR = 0x68;
	int SHELL_BG_COLOR = 0x78;

	
	int SHELL_DOUBLE_COLOR = 0xDF;
	int SHELL_HALF_DAM_COLOR = 0x90;
	int SHELL_CYAN_COLOR = 0x72;

	
	

	int svc_bad = 0;

	
	

	int svc_muzzleflash = 1;
	int svc_muzzleflash2 = 2;
	int svc_temp_entity = 3;
	int svc_layout = 4;
	int svc_inventory = 5;

	
	int svc_nop = 6;
	int svc_disconnect = 7;
	int svc_reconnect = 8;
	int svc_sound = 9;
	int svc_print = 10;
	int svc_stufftext = 11;
	
	int svc_serverdata = 12;
	int svc_configstring = 13;
	int svc_spawnbaseline = 14;
	int svc_centerprint = 15;
	int svc_download = 16;
	int svc_playerinfo = 17;
	int svc_packetentities = 18;
	int svc_deltapacketentities = 19;
	int svc_frame = 20;

	int NUMVERTEXNORMALS = 162;
	int PROTOCOL_VERSION = 34;
	int PORT_MASTER = 27900;
	int PORT_CLIENT = 27901;
	int PORT_SERVER = 27910;
	int PORT_ANY = -1;

	int PS_M_TYPE = (1 << 0);
	int PS_M_ORIGIN = (1 << 1);
	int PS_M_VELOCITY = (1 << 2);
	int PS_M_TIME = (1 << 3);
	int PS_M_FLAGS = (1 << 4);
	int PS_M_GRAVITY = (1 << 5);
	int PS_M_DELTA_ANGLES = (1 << 6);

	int UPDATE_BACKUP = 16;
	
	int UPDATE_MASK = (UPDATE_BACKUP - 1);

	int PS_VIEWOFFSET = (1 << 7);
	int PS_VIEWANGLES = (1 << 8);
	int PS_KICKANGLES = (1 << 9);
	int PS_BLEND = (1 << 10);
	int PS_FOV = (1 << 11);
	int PS_WEAPONINDEX = (1 << 12);
	int PS_WEAPONFRAME = (1 << 13);
	int PS_RDFLAGS = (1 << 14);

	int CM_ANGLE1 = (1 << 0);
	int CM_ANGLE2 = (1 << 1);
	int CM_ANGLE3 = (1 << 2);
	int CM_FORWARD = (1 << 3);
	int CM_SIDE = (1 << 4);
	int CM_UP = (1 << 5);
	int CM_BUTTONS = (1 << 6);
	int CM_IMPULSE = (1 << 7);

	
	int U_ORIGIN1 = (1 << 0);
	int U_ORIGIN2 = (1 << 1);
	int U_ANGLE2 = (1 << 2);
	int U_ANGLE3 = (1 << 3);
	int U_FRAME8 = (1 << 4);
	int U_EVENT = (1 << 5);
	int U_REMOVE = (1 << 6);
	int U_MOREBITS1 = (1 << 7);

	
	int U_NUMBER16 = (1 << 8);
	int U_ORIGIN3 = (1 << 9);
	int U_ANGLE1 = (1 << 10);
	int U_MODEL = (1 << 11);
	int U_RENDERFX8 = (1 << 12);
	int U_EFFECTS8 = (1 << 14);
	int U_MOREBITS2 = (1 << 15);

	
	int U_SKIN8 = (1 << 16);
	int U_FRAME16 = (1 << 17);
	int U_RENDERFX16 = (1 << 18);
	int U_EFFECTS16 = (1 << 19);
	int U_MODEL2 = (1 << 20);
	int U_MODEL3 = (1 << 21);
	int U_MODEL4 = (1 << 22);
	int U_MOREBITS3 = (1 << 23);

	
	int U_OLDORIGIN = (1 << 24);
	int U_SKIN16 = (1 << 25);
	int U_SOUND = (1 << 26);
	int U_SOLID = (1 << 27);

	int SHELL_WHITE_COLOR = 0xD7;

	int MAX_TRIANGLES = 4096;
	int MAX_VERTS = 2048;
	int MAX_FRAMES = 512;
	int MAX_MD2SKINS = 32;
	int MAX_SKINNAME = 64;

	int MAXLIGHTMAPS = 4;
	int MIPLEVELS = 4;

	int clc_bad = 0;
	int clc_nop = 1;
	int clc_move = 2;
	int clc_userinfo = 3;
	int clc_stringcmd = 4;

	int NS_CLIENT = 0;
	int NS_SERVER = 1;

	int NA_LOOPBACK = 0;
	int NA_BROADCAST = 1;
	int NA_IP = 2;
	int NA_IPX = 3;
	int NA_BROADCAST_IPX = 4;

	int SND_VOLUME = (1 << 0);
	int SND_ATTENUATION = (1 << 1);
	int SND_POS = (1 << 2);
	int SND_ENT = (1 << 3);
	int SND_OFFSET = (1 << 4);

	float DEFAULT_SOUND_PACKET_VOLUME = 1.0f;
	float DEFAULT_SOUND_PACKET_ATTENUATION = 1.0f;

	
	
	int MAX_PARSE_ENTITIES = 1024;
	int MAX_CLIENTWEAPONMODELS = 20;

	int CMD_BACKUP = 64;

	int ca_uninitialized = 0;
	int ca_disconnected = 1;
	int ca_connecting = 2;
	int ca_connected = 3;
	int ca_active = 4;

	int MAX_ALIAS_NAME = 32;
	int MAX_NUM_ARGVS = 50;

	int MAX_MSGLEN = 1400;

	
	
	int NUM_CON_TIMES = 4;
	int CON_TEXTSIZE = 32768;

	int BSPVERSION = 38;

	
	

	
	
	
	int MAX_MAP_MODELS = 1024;
	int MAX_MAP_BRUSHES = 8192;
	int MAX_MAP_ENTITIES = 2048;
	int MAX_MAP_ENTSTRING = 0x40000;
	int MAX_MAP_TEXINFO = 8192;

	int MAX_MAP_AREAS = 256;
	int MAX_MAP_AREAPORTALS = 1024;
	int MAX_MAP_PLANES = 65536;
	int MAX_MAP_NODES = 65536;
	int MAX_MAP_BRUSHSIDES = 65536;
	int MAX_MAP_LEAFS = 65536;
	int MAX_MAP_VERTS = 65536;
	int MAX_MAP_FACES = 65536;
	int MAX_MAP_LEAFFACES = 65536;
	int MAX_MAP_LEAFBRUSHES = 65536;
	int MAX_MAP_PORTALS = 65536;
	int MAX_MAP_EDGES = 128000;
	int MAX_MAP_SURFEDGES = 256000;
	int MAX_MAP_LIGHTING = 0x200000;
	int MAX_MAP_VISIBILITY = 0x100000;

	
	int MAX_KEY = 32;
	int MAX_VALUE = 1024;

	
	int PLANE_X = 0;
	int PLANE_Y = 1;
	int PLANE_Z = 2;

	
	int PLANE_ANYX = 3;
	int PLANE_ANYY = 4;
	int PLANE_ANYZ = 5;

	int LUMP_ENTITIES = 0;
	int LUMP_PLANES = 1;
	int LUMP_VERTEXES = 2;
	int LUMP_VISIBILITY = 3;
	int LUMP_NODES = 4;
	int LUMP_TEXINFO = 5;
	int LUMP_FACES = 6;
	int LUMP_LIGHTING = 7;
	int LUMP_LEAFS = 8;
	int LUMP_LEAFFACES = 9;
	int LUMP_LEAFBRUSHES = 10;
	int LUMP_EDGES = 11;
	int LUMP_SURFEDGES = 12;
	int LUMP_MODELS = 13;
	int LUMP_BRUSHES = 14;
	int LUMP_BRUSHSIDES = 15;
	int LUMP_POP = 16;
	int LUMP_AREAS = 17;
	int LUMP_AREAPORTALS = 18;
	int HEADER_LUMPS = 19;

	int DTRIVERTX_V0 = 0;
	int DTRIVERTX_V1 = 1;
	int DTRIVERTX_V2 = 2;
	int DTRIVERTX_LNI = 3;
	int DTRIVERTX_SIZE = 4;

	int ALIAS_VERSION = 8;
	String GAMEVERSION = "baseq2";
	int API_VERSION = 3;

	int DVIS_PVS = 0;
	int DVIS_PHS = 1;

	
	
	int key_game = 0;
	int key_console = 1;
	int key_message = 2;
	int key_menu = 3;

	
	
	int cs_free = 0;
	int cs_zombie = 1;
	
	int cs_connected = 2;
	int cs_spawned = 3;

	int MAX_CHALLENGES = 1024;

	int ss_dead = 0;
	int ss_loading = 1;
	int ss_game = 2;
	int ss_cinematic = 3;
	int ss_demo = 4;
	int ss_pic = 5;

	int SV_OUTPUTBUF_LENGTH = (MAX_MSGLEN - 16);
	int RD_NONE = 0;
	int RD_CLIENT = 1;
	int RD_PACKET = 2;

	int RATE_MESSAGES = 10;

	int LATENCY_COUNTS = 16;

	int MAXCMDLINE = 256;

	int MAX_MASTERS = 8;

	
	int AREA_DEPTH = 4;
	int AREA_NODES = 32;

	int EXEC_NOW = 0;
	int EXEC_INSERT = 1;
	int EXEC_APPEND = 2;

	
	int MAXMENUITEMS = 64;

	int MTYPE_SLIDER = 0;
	int MTYPE_LIST = 1;
	int MTYPE_ACTION = 2;
	int MTYPE_SPINCONTROL = 3;
	int MTYPE_SEPARATOR = 4;
	int MTYPE_FIELD = 5;

	int K_TAB = 9;
	int K_ENTER = 13;
	int K_ESCAPE = 27;
	int K_SPACE = 32;

	

	int K_BACKSPACE = 127;
	int K_UPARROW = 128;
	int K_DOWNARROW = 129;
	int K_LEFTARROW = 130;
	int K_RIGHTARROW = 131;

	int QMF_LEFT_JUSTIFY = 0x00000001;
	int QMF_GRAYED = 0x00000002;
	int QMF_NUMBERSONLY = 0x00000004;

	int RCOLUMN_OFFSET = 16;
	int LCOLUMN_OFFSET = -16;

	int MAX_DISPLAYNAME = 16;
	int MAX_PLAYERMODELS = 1024;

	int MAX_LOCAL_SERVERS = 8;
	String NO_SERVER_STRING = "<no server>";
	int NUM_ADDRESSBOOK_ENTRIES = 9;

	int STEPSIZE = 18;


	float MOVE_STOP_EPSILON = 0.1f;
	
	float MIN_STEP_NORMAL = 0.7f;


	
	int FILEISREADABLE = 1;

	int FILEISWRITABLE = 2;

	int FILEISFILE = 4;

	int FILEISDIRECTORY = 8;

	
	
    boolean LITTLE_ENDIAN = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);

	int SIZE_OF_SHORT = 2;

	int SIZE_OF_INT = 4;

	int SIZE_OF_LONG = 8;

	int SIZE_OF_FLOAT = 4;

	int SIZE_OF_DOUBLE = 8;


	//from Globals


	public static final String __DATE__ = "2003";
	public static final float VERSION = 3.21f;
	public static final String BASEDIRNAME = "baseq2";
	public static final sizebuf_t net_message = new sizebuf_t();
	public static final sizebuf_t cmd_text = new sizebuf_t();
	public static final byte[] defer_text_buf = new byte[8192];
	public static final byte[] cmd_text_buf = new byte[8192];
	public static final byte[] net_message_buffer = new byte[MAX_MSGLEN];
	public static final client_static_t cls = new client_static_t();
	public static final centity_t[] cl_entities = new centity_t[Defines.MAX_EDICTS];
	public static final entity_state_t[] cl_parse_entities = new entity_state_t[Defines.MAX_PARSE_ENTITIES];
	public static final float[][] bytedirs = {
			{-0.525731f, 0.000000f, 0.850651f}, {
			-0.442863f, 0.238856f, 0.864188f}, {
			-0.295242f, 0.000000f, 0.955423f}, {
			-0.309017f, 0.500000f, 0.809017f}, {
			-0.162460f, 0.262866f, 0.951056f}, {
			0.000000f, 0.000000f, 1.000000f}, {
			0.000000f, 0.850651f, 0.525731f}, {
			-0.147621f, 0.716567f, 0.681718f}, {
			0.147621f, 0.716567f, 0.681718f}, {
			0.000000f, 0.525731f, 0.850651f}, {
			0.309017f, 0.500000f, 0.809017f}, {
			0.525731f, 0.000000f, 0.850651f}, {
			0.295242f, 0.000000f, 0.955423f}, {
			0.442863f, 0.238856f, 0.864188f}, {
			0.162460f, 0.262866f, 0.951056f}, {
			-0.681718f, 0.147621f, 0.716567f}, {
			-0.809017f, 0.309017f, 0.500000f}, {
			-0.587785f, 0.425325f, 0.688191f}, {
			-0.850651f, 0.525731f, 0.000000f}, {
			-0.864188f, 0.442863f, 0.238856f}, {
			-0.716567f, 0.681718f, 0.147621f}, {
			-0.688191f, 0.587785f, 0.425325f}, {
			-0.500000f, 0.809017f, 0.309017f}, {
			-0.238856f, 0.864188f, 0.442863f}, {
			-0.425325f, 0.688191f, 0.587785f}, {
			-0.716567f, 0.681718f, -0.147621f}, {
			-0.500000f, 0.809017f, -0.309017f}, {
			-0.525731f, 0.850651f, 0.000000f}, {
			0.000000f, 0.850651f, -0.525731f}, {
			-0.238856f, 0.864188f, -0.442863f}, {
			0.000000f, 0.955423f, -0.295242f}, {
			-0.262866f, 0.951056f, -0.162460f}, {
			0.000000f, 1.000000f, 0.000000f}, {
			0.000000f, 0.955423f, 0.295242f}, {
			-0.262866f, 0.951056f, 0.162460f}, {
			0.238856f, 0.864188f, 0.442863f}, {
			0.262866f, 0.951056f, 0.162460f}, {
			0.500000f, 0.809017f, 0.309017f}, {
			0.238856f, 0.864188f, -0.442863f}, {
			0.262866f, 0.951056f, -0.162460f}, {
			0.500000f, 0.809017f, -0.309017f}, {
			0.850651f, 0.525731f, 0.000000f}, {
			0.716567f, 0.681718f, 0.147621f}, {
			0.716567f, 0.681718f, -0.147621f}, {
			0.525731f, 0.850651f, 0.000000f}, {
			0.425325f, 0.688191f, 0.587785f}, {
			0.864188f, 0.442863f, 0.238856f}, {
			0.688191f, 0.587785f, 0.425325f}, {
			0.809017f, 0.309017f, 0.500000f}, {
			0.681718f, 0.147621f, 0.716567f}, {
			0.587785f, 0.425325f, 0.688191f}, {
			0.955423f, 0.295242f, 0.000000f}, {
			1.000000f, 0.000000f, 0.000000f}, {
			0.951056f, 0.162460f, 0.262866f}, {
			0.850651f, -0.525731f, 0.000000f}, {
			0.955423f, -0.295242f, 0.000000f}, {
			0.864188f, -0.442863f, 0.238856f}, {
			0.951056f, -0.162460f, 0.262866f}, {
			0.809017f, -0.309017f, 0.500000f}, {
			0.681718f, -0.147621f, 0.716567f}, {
			0.850651f, 0.000000f, 0.525731f}, {
			0.864188f, 0.442863f, -0.238856f}, {
			0.809017f, 0.309017f, -0.500000f}, {
			0.951056f, 0.162460f, -0.262866f}, {
			0.525731f, 0.000000f, -0.850651f}, {
			0.681718f, 0.147621f, -0.716567f}, {
			0.681718f, -0.147621f, -0.716567f}, {
			0.850651f, 0.000000f, -0.525731f}, {
			0.809017f, -0.309017f, -0.500000f}, {
			0.864188f, -0.442863f, -0.238856f}, {
			0.951056f, -0.162460f, -0.262866f}, {
			0.147621f, 0.716567f, -0.681718f}, {
			0.309017f, 0.500000f, -0.809017f}, {
			0.425325f, 0.688191f, -0.587785f}, {
			0.442863f, 0.238856f, -0.864188f}, {
			0.587785f, 0.425325f, -0.688191f}, {
			0.688191f, 0.587785f, -0.425325f}, {
			-0.147621f, 0.716567f, -0.681718f}, {
			-0.309017f, 0.500000f, -0.809017f}, {
			0.000000f, 0.525731f, -0.850651f}, {
			-0.525731f, 0.000000f, -0.850651f}, {
			-0.442863f, 0.238856f, -0.864188f}, {
			-0.295242f, 0.000000f, -0.955423f}, {
			-0.162460f, 0.262866f, -0.951056f}, {
			0.000000f, 0.000000f, -1.000000f}, {
			0.295242f, 0.000000f, -0.955423f}, {
			0.162460f, 0.262866f, -0.951056f}, {
			-0.442863f, -0.238856f, -0.864188f}, {
			-0.309017f, -0.500000f, -0.809017f}, {
			-0.162460f, -0.262866f, -0.951056f}, {
			0.000000f, -0.850651f, -0.525731f}, {
			-0.147621f, -0.716567f, -0.681718f}, {
			0.147621f, -0.716567f, -0.681718f}, {
			0.000000f, -0.525731f, -0.850651f}, {
			0.309017f, -0.500000f, -0.809017f}, {
			0.442863f, -0.238856f, -0.864188f}, {
			0.162460f, -0.262866f, -0.951056f}, {
			0.238856f, -0.864188f, -0.442863f}, {
			0.500000f, -0.809017f, -0.309017f}, {
			0.425325f, -0.688191f, -0.587785f}, {
			0.716567f, -0.681718f, -0.147621f}, {
			0.688191f, -0.587785f, -0.425325f}, {
			0.587785f, -0.425325f, -0.688191f}, {
			0.000000f, -0.955423f, -0.295242f}, {
			0.000000f, -1.000000f, 0.000000f}, {
			0.262866f, -0.951056f, -0.162460f}, {
			0.000000f, -0.850651f, 0.525731f}, {
			0.000000f, -0.955423f, 0.295242f}, {
			0.238856f, -0.864188f, 0.442863f}, {
			0.262866f, -0.951056f, 0.162460f}, {
			0.500000f, -0.809017f, 0.309017f}, {
			0.716567f, -0.681718f, 0.147621f}, {
			0.525731f, -0.850651f, 0.000000f}, {
			-0.238856f, -0.864188f, -0.442863f}, {
			-0.500000f, -0.809017f, -0.309017f}, {
			-0.262866f, -0.951056f, -0.162460f}, {
			-0.850651f, -0.525731f, 0.000000f}, {
			-0.716567f, -0.681718f, -0.147621f}, {
			-0.716567f, -0.681718f, 0.147621f}, {
			-0.525731f, -0.850651f, 0.000000f}, {
			-0.500000f, -0.809017f, 0.309017f}, {
			-0.238856f, -0.864188f, 0.442863f}, {
			-0.262866f, -0.951056f, 0.162460f}, {
			-0.864188f, -0.442863f, 0.238856f}, {
			-0.809017f, -0.309017f, 0.500000f}, {
			-0.688191f, -0.587785f, 0.425325f}, {
			-0.681718f, -0.147621f, 0.716567f}, {
			-0.442863f, -0.238856f, 0.864188f}, {
			-0.587785f, -0.425325f, 0.688191f}, {
			-0.309017f, -0.500000f, 0.809017f}, {
			-0.147621f, -0.716567f, 0.681718f}, {
			-0.425325f, -0.688191f, 0.587785f}, {
			-0.162460f, -0.262866f, 0.951056f}, {
			0.442863f, -0.238856f, 0.864188f}, {
			0.162460f, -0.262866f, 0.951056f}, {
			0.309017f, -0.500000f, 0.809017f}, {
			0.147621f, -0.716567f, 0.681718f}, {
			0.000000f, -0.525731f, 0.850651f}, {
			0.425325f, -0.688191f, 0.587785f}, {
			0.587785f, -0.425325f, 0.688191f}, {
			0.688191f, -0.587785f, 0.425325f}, {
			-0.955423f, 0.295242f, 0.000000f}, {
			-0.951056f, 0.162460f, 0.262866f}, {
			-1.000000f, 0.000000f, 0.000000f}, {
			-0.850651f, 0.000000f, 0.525731f}, {
			-0.955423f, -0.295242f, 0.000000f}, {
			-0.951056f, -0.162460f, 0.262866f}, {
			-0.864188f, 0.442863f, -0.238856f}, {
			-0.951056f, 0.162460f, -0.262866f}, {
			-0.809017f, 0.309017f, -0.500000f}, {
			-0.864188f, -0.442863f, -0.238856f}, {
			-0.951056f, -0.162460f, -0.262866f}, {
			-0.809017f, -0.309017f, -0.500000f}, {
			-0.681718f, 0.147621f, -0.716567f}, {
			-0.681718f, -0.147621f, -0.716567f}, {
			-0.850651f, 0.000000f, -0.525731f}, {
			-0.688191f, 0.587785f, -0.425325f}, {
			-0.587785f, 0.425325f, -0.688191f}, {
			-0.425325f, 0.688191f, -0.587785f}, {
			-0.425325f, -0.688191f, -0.587785f}, {
			-0.587785f, -0.425325f, -0.688191f}, {
			-0.688191f, -0.587785f, -0.425325f}
	};
	public static final console_t con = new console_t();
	public static final viddef_t viddef = new viddef_t();
	public static final String[] keybindings = new String[256];
	public static final boolean[] keydown = new boolean[256];
	public static final byte[][] key_lines = new byte[32][];
	public static final vrect_t scr_vrect = new vrect_t();
	public static final int chat_bufferlen = 0;
	public static final netadr_t net_from = new netadr_t();
	public static final float[] vec3_origin = {0.0f, 0.0f, 0.0f};
	public static final int vidref_val = VIDREF_GL;
	public static final Random rnd = new Random();
}