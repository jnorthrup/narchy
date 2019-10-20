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





package jake2.game.monsters;

import jake2.Defines;
import jake2.game.*;
import jake2.util.Lib;
import jake2.util.Math3D;

public class M_Chick {

    public static final int FRAME_attak101 = 0;

    public static final int FRAME_attak102 = 1;

    public static final int FRAME_attak103 = 2;

    public static final int FRAME_attak104 = 3;

    public static final int FRAME_attak105 = 4;

    public static final int FRAME_attak106 = 5;

    public static final int FRAME_attak107 = 6;

    public static final int FRAME_attak108 = 7;

    public static final int FRAME_attak109 = 8;

    public static final int FRAME_attak110 = 9;

    public static final int FRAME_attak111 = 10;

    public static final int FRAME_attak112 = 11;

    public static final int FRAME_attak113 = 12;

    public static final int FRAME_attak114 = 13;

    public static final int FRAME_attak115 = 14;

    public static final int FRAME_attak116 = 15;

    public static final int FRAME_attak117 = 16;

    public static final int FRAME_attak118 = 17;

    public static final int FRAME_attak119 = 18;

    public static final int FRAME_attak120 = 19;

    public static final int FRAME_attak121 = 20;

    public static final int FRAME_attak122 = 21;

    public static final int FRAME_attak123 = 22;

    public static final int FRAME_attak124 = 23;

    public static final int FRAME_attak125 = 24;

    public static final int FRAME_attak126 = 25;

    public static final int FRAME_attak127 = 26;

    public static final int FRAME_attak128 = 27;

    public static final int FRAME_attak129 = 28;

    public static final int FRAME_attak130 = 29;

    public static final int FRAME_attak131 = 30;

    public static final int FRAME_attak132 = 31;

    public static final int FRAME_attak201 = 32;

    public static final int FRAME_attak202 = 33;

    public static final int FRAME_attak203 = 34;

    public static final int FRAME_attak204 = 35;

    public static final int FRAME_attak205 = 36;

    public static final int FRAME_attak206 = 37;

    public static final int FRAME_attak207 = 38;

    public static final int FRAME_attak208 = 39;

    public static final int FRAME_attak209 = 40;

    public static final int FRAME_attak210 = 41;

    public static final int FRAME_attak211 = 42;

    public static final int FRAME_attak212 = 43;

    public static final int FRAME_attak213 = 44;

    public static final int FRAME_attak214 = 45;

    public static final int FRAME_attak215 = 46;

    public static final int FRAME_attak216 = 47;

    public static final int FRAME_death101 = 48;

    public static final int FRAME_death102 = 49;

    public static final int FRAME_death103 = 50;

    public static final int FRAME_death104 = 51;

    public static final int FRAME_death105 = 52;

    public static final int FRAME_death106 = 53;

    public static final int FRAME_death107 = 54;

    public static final int FRAME_death108 = 55;

    public static final int FRAME_death109 = 56;

    public static final int FRAME_death110 = 57;

    public static final int FRAME_death111 = 58;

    public static final int FRAME_death112 = 59;

    public static final int FRAME_death201 = 60;

    public static final int FRAME_death202 = 61;

    public static final int FRAME_death203 = 62;

    public static final int FRAME_death204 = 63;

    public static final int FRAME_death205 = 64;

    public static final int FRAME_death206 = 65;

    public static final int FRAME_death207 = 66;

    public static final int FRAME_death208 = 67;

    public static final int FRAME_death209 = 68;

    public static final int FRAME_death210 = 69;

    public static final int FRAME_death211 = 70;

    public static final int FRAME_death212 = 71;

    public static final int FRAME_death213 = 72;

    public static final int FRAME_death214 = 73;

    public static final int FRAME_death215 = 74;

    public static final int FRAME_death216 = 75;

    public static final int FRAME_death217 = 76;

    public static final int FRAME_death218 = 77;

    public static final int FRAME_death219 = 78;

    public static final int FRAME_death220 = 79;

    public static final int FRAME_death221 = 80;

    public static final int FRAME_death222 = 81;

    public static final int FRAME_death223 = 82;

    public static final int FRAME_duck01 = 83;

    public static final int FRAME_duck02 = 84;

    public static final int FRAME_duck03 = 85;

    public static final int FRAME_duck04 = 86;

    public static final int FRAME_duck05 = 87;

    public static final int FRAME_duck06 = 88;

    public static final int FRAME_duck07 = 89;

    public static final int FRAME_pain101 = 90;

    public static final int FRAME_pain102 = 91;

    public static final int FRAME_pain103 = 92;

    public static final int FRAME_pain104 = 93;

    public static final int FRAME_pain105 = 94;

    public static final int FRAME_pain201 = 95;

    public static final int FRAME_pain202 = 96;

    public static final int FRAME_pain203 = 97;

    public static final int FRAME_pain204 = 98;

    public static final int FRAME_pain205 = 99;

    public static final int FRAME_pain301 = 100;

    public static final int FRAME_pain302 = 101;

    public static final int FRAME_pain303 = 102;

    public static final int FRAME_pain304 = 103;

    public static final int FRAME_pain305 = 104;

    public static final int FRAME_pain306 = 105;

    public static final int FRAME_pain307 = 106;

    public static final int FRAME_pain308 = 107;

    public static final int FRAME_pain309 = 108;

    public static final int FRAME_pain310 = 109;

    public static final int FRAME_pain311 = 110;

    public static final int FRAME_pain312 = 111;

    public static final int FRAME_pain313 = 112;

    public static final int FRAME_pain314 = 113;

    public static final int FRAME_pain315 = 114;

    public static final int FRAME_pain316 = 115;

    public static final int FRAME_pain317 = 116;

    public static final int FRAME_pain318 = 117;

    public static final int FRAME_pain319 = 118;

    public static final int FRAME_pain320 = 119;

    public static final int FRAME_pain321 = 120;

    public static final int FRAME_stand101 = 121;

    public static final int FRAME_stand102 = 122;

    public static final int FRAME_stand103 = 123;

    public static final int FRAME_stand104 = 124;

    public static final int FRAME_stand105 = 125;

    public static final int FRAME_stand106 = 126;

    public static final int FRAME_stand107 = 127;

    public static final int FRAME_stand108 = 128;

    public static final int FRAME_stand109 = 129;

    public static final int FRAME_stand110 = 130;

    public static final int FRAME_stand111 = 131;

    public static final int FRAME_stand112 = 132;

    public static final int FRAME_stand113 = 133;

    public static final int FRAME_stand114 = 134;

    public static final int FRAME_stand115 = 135;

    public static final int FRAME_stand116 = 136;

    public static final int FRAME_stand117 = 137;

    public static final int FRAME_stand118 = 138;

    public static final int FRAME_stand119 = 139;

    public static final int FRAME_stand120 = 140;

    public static final int FRAME_stand121 = 141;

    public static final int FRAME_stand122 = 142;

    public static final int FRAME_stand123 = 143;

    public static final int FRAME_stand124 = 144;

    public static final int FRAME_stand125 = 145;

    public static final int FRAME_stand126 = 146;

    public static final int FRAME_stand127 = 147;

    public static final int FRAME_stand128 = 148;

    public static final int FRAME_stand129 = 149;

    public static final int FRAME_stand130 = 150;

    public static final int FRAME_stand201 = 151;

    public static final int FRAME_stand202 = 152;

    public static final int FRAME_stand203 = 153;

    public static final int FRAME_stand204 = 154;

    public static final int FRAME_stand205 = 155;

    public static final int FRAME_stand206 = 156;

    public static final int FRAME_stand207 = 157;

    public static final int FRAME_stand208 = 158;

    public static final int FRAME_stand209 = 159;

    public static final int FRAME_stand210 = 160;

    public static final int FRAME_stand211 = 161;

    public static final int FRAME_stand212 = 162;

    public static final int FRAME_stand213 = 163;

    public static final int FRAME_stand214 = 164;

    public static final int FRAME_stand215 = 165;

    public static final int FRAME_stand216 = 166;

    public static final int FRAME_stand217 = 167;

    public static final int FRAME_stand218 = 168;

    public static final int FRAME_stand219 = 169;

    public static final int FRAME_stand220 = 170;

    public static final int FRAME_stand221 = 171;

    public static final int FRAME_stand222 = 172;

    public static final int FRAME_stand223 = 173;

    public static final int FRAME_stand224 = 174;

    public static final int FRAME_stand225 = 175;

    public static final int FRAME_stand226 = 176;

    public static final int FRAME_stand227 = 177;

    public static final int FRAME_stand228 = 178;

    public static final int FRAME_stand229 = 179;

    public static final int FRAME_stand230 = 180;

    public static final int FRAME_walk01 = 181;

    public static final int FRAME_walk02 = 182;

    public static final int FRAME_walk03 = 183;

    public static final int FRAME_walk04 = 184;

    public static final int FRAME_walk05 = 185;

    public static final int FRAME_walk06 = 186;

    public static final int FRAME_walk07 = 187;

    public static final int FRAME_walk08 = 188;

    public static final int FRAME_walk09 = 189;

    public static final int FRAME_walk10 = 190;

    public static final int FRAME_walk11 = 191;

    public static final int FRAME_walk12 = 192;

    public static final int FRAME_walk13 = 193;

    public static final int FRAME_walk14 = 194;

    public static final int FRAME_walk15 = 195;

    public static final int FRAME_walk16 = 196;

    public static final int FRAME_walk17 = 197;

    public static final int FRAME_walk18 = 198;

    public static final int FRAME_walk19 = 199;

    public static final int FRAME_walk20 = 200;

    public static final int FRAME_walk21 = 201;

    public static final int FRAME_walk22 = 202;

    public static final int FRAME_walk23 = 203;

    public static final int FRAME_walk24 = 204;

    public static final int FRAME_walk25 = 205;

    public static final int FRAME_walk26 = 206;

    public static final int FRAME_walk27 = 207;

    public static final int FRAME_recln201 = 208;

    public static final int FRAME_recln202 = 209;

    public static final int FRAME_recln203 = 210;

    public static final int FRAME_recln204 = 211;

    public static final int FRAME_recln205 = 212;

    public static final int FRAME_recln206 = 213;

    public static final int FRAME_recln207 = 214;

    public static final int FRAME_recln208 = 215;

    public static final int FRAME_recln209 = 216;

    public static final int FRAME_recln210 = 217;

    public static final int FRAME_recln211 = 218;

    public static final int FRAME_recln212 = 219;

    public static final int FRAME_recln213 = 220;

    public static final int FRAME_recln214 = 221;

    public static final int FRAME_recln215 = 222;

    public static final int FRAME_recln216 = 223;

    public static final int FRAME_recln217 = 224;

    public static final int FRAME_recln218 = 225;

    public static final int FRAME_recln219 = 226;

    public static final int FRAME_recln220 = 227;

    public static final int FRAME_recln221 = 228;

    public static final int FRAME_recln222 = 229;

    public static final int FRAME_recln223 = 230;

    public static final int FRAME_recln224 = 231;

    public static final int FRAME_recln225 = 232;

    public static final int FRAME_recln226 = 233;

    public static final int FRAME_recln227 = 234;

    public static final int FRAME_recln228 = 235;

    public static final int FRAME_recln229 = 236;

    public static final int FRAME_recln230 = 237;

    public static final int FRAME_recln231 = 238;

    public static final int FRAME_recln232 = 239;

    public static final int FRAME_recln233 = 240;

    public static final int FRAME_recln234 = 241;

    public static final int FRAME_recln235 = 242;

    public static final int FRAME_recln236 = 243;

    public static final int FRAME_recln237 = 244;

    public static final int FRAME_recln238 = 245;

    public static final int FRAME_recln239 = 246;

    public static final int FRAME_recln240 = 247;

    public static final int FRAME_recln101 = 248;

    public static final int FRAME_recln102 = 249;

    public static final int FRAME_recln103 = 250;

    public static final int FRAME_recln104 = 251;

    public static final int FRAME_recln105 = 252;

    public static final int FRAME_recln106 = 253;

    public static final int FRAME_recln107 = 254;

    public static final int FRAME_recln108 = 255;

    public static final int FRAME_recln109 = 256;

    public static final int FRAME_recln110 = 257;

    public static final int FRAME_recln111 = 258;

    public static final int FRAME_recln112 = 259;

    public static final int FRAME_recln113 = 260;

    public static final int FRAME_recln114 = 261;

    public static final int FRAME_recln115 = 262;

    public static final int FRAME_recln116 = 263;

    public static final int FRAME_recln117 = 264;

    public static final int FRAME_recln118 = 265;

    public static final int FRAME_recln119 = 266;

    public static final int FRAME_recln120 = 267;

    public static final int FRAME_recln121 = 268;

    public static final int FRAME_recln122 = 269;

    public static final int FRAME_recln123 = 270;

    public static final int FRAME_recln124 = 271;

    public static final int FRAME_recln125 = 272;

    public static final int FRAME_recln126 = 273;

    public static final int FRAME_recln127 = 274;

    public static final int FRAME_recln128 = 275;

    public static final int FRAME_recln129 = 276;

    public static final int FRAME_recln130 = 277;

    public static final int FRAME_recln131 = 278;

    public static final int FRAME_recln132 = 279;

    public static final int FRAME_recln133 = 280;

    public static final int FRAME_recln134 = 281;

    public static final int FRAME_recln135 = 282;

    public static final int FRAME_recln136 = 283;

    public static final int FRAME_recln137 = 284;

    public static final int FRAME_recln138 = 285;

    public static final int FRAME_recln139 = 286;

    public static final int FRAME_recln140 = 287;

    public static final float MODEL_SCALE = 1.000000f;

    static int sound_missile_prelaunch;

    static int sound_missile_launch;

    static int sound_melee_swing;

    static int sound_melee_hit;

    static int sound_missile_reload;

    static int sound_death1;

    static int sound_death2;

    static int sound_fall_down;

    static int sound_idle1;

    static int sound_idle2;

    static int sound_pain1;

    static int sound_pain2;

    static int sound_pain3;

    static int sound_sight;

    static int sound_search;

    static final EntThinkAdapter ChickMoan = new EntThinkAdapter() {
    	@Override
        public String getID() { return "ChickMoan"; }
        @Override
        public boolean think(edict_t self) {
            if ((double) Lib.random() < 0.5)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_idle1, 1.0F,
                        (float) Defines.ATTN_IDLE, (float) 0);
            else
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_idle2, 1.0F,
                        (float) Defines.ATTN_IDLE, (float) 0);
            return true;
        }
    };

    static final mframe_t[] chick_frames_fidget = {
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, ChickMoan),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null) };

    static final EntThinkAdapter chick_stand = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_stand"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = chick_move_stand;
            return true;
        }
    };

    static final mmove_t chick_move_fidget = new mmove_t(FRAME_stand201,
            FRAME_stand230, chick_frames_fidget, chick_stand);

    static final EntThinkAdapter chick_fidget = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_fidget"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0)
                return true;
            if ((double) Lib.random() <= 0.3)
                self.monsterinfo.currentmove = chick_move_fidget;
            return true;
        }
    };

    static final mframe_t[] chick_frames_stand = {
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, chick_fidget), };

    static final mmove_t chick_move_stand = new mmove_t(FRAME_stand101,
            FRAME_stand130, chick_frames_stand, null);

    static final EntThinkAdapter chick_run = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_run"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0) {
                self.monsterinfo.currentmove = chick_move_stand;
                return true;
            }

            if (self.monsterinfo.currentmove == chick_move_walk
                    || self.monsterinfo.currentmove == chick_move_start_run) {
                self.monsterinfo.currentmove = chick_move_run;
            } else {
                self.monsterinfo.currentmove = chick_move_start_run;
            }
            return true;
        }
    };

    static final mframe_t[] chick_frames_start_run = {
            new mframe_t(GameAI.ai_run, 1.0F, null),
            new mframe_t(GameAI.ai_run, (float) 0, null),
            new mframe_t(GameAI.ai_run, (float) 0, null),
            new mframe_t(GameAI.ai_run, -1.0F, null),
            new mframe_t(GameAI.ai_run, -1.0F, null),
            new mframe_t(GameAI.ai_run, (float) 0, null),
            new mframe_t(GameAI.ai_run, 1.0F, null),
            new mframe_t(GameAI.ai_run, 3.0F, null),
            new mframe_t(GameAI.ai_run, 6.0F, null),
            new mframe_t(GameAI.ai_run, 3.0F, null) };

    static final mmove_t chick_move_start_run = new mmove_t(FRAME_walk01,
            FRAME_walk10, chick_frames_start_run, chick_run);

    static final mframe_t[] chick_frames_run = {
            new mframe_t(GameAI.ai_run, 6.0F, null),
            new mframe_t(GameAI.ai_run, 8.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 5.0F, null),
            new mframe_t(GameAI.ai_run, 7.0F, null),
            new mframe_t(GameAI.ai_run, 4.0F, null),
            new mframe_t(GameAI.ai_run, 11.0F, null),
            new mframe_t(GameAI.ai_run, 5.0F, null),
            new mframe_t(GameAI.ai_run, 9.0F, null),
            new mframe_t(GameAI.ai_run, 7.0F, null) };

    static final mmove_t chick_move_run = new mmove_t(FRAME_walk11, FRAME_walk20,
            chick_frames_run, null);

    static final mframe_t[] chick_frames_walk = {
            new mframe_t(GameAI.ai_walk, 6.0F, null),
            new mframe_t(GameAI.ai_walk, 8.0F, null),
            new mframe_t(GameAI.ai_walk, 13.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 7.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 11.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 9.0F, null),
            new mframe_t(GameAI.ai_walk, 7.0F, null) };

    static final mmove_t chick_move_walk = new mmove_t(FRAME_walk11, FRAME_walk20,
            chick_frames_walk, null);

    static final EntThinkAdapter chick_walk = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_walk"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = chick_move_walk;
            return true;
        }
    };

    static final mframe_t[] chick_frames_pain1 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final mmove_t chick_move_pain1 = new mmove_t(FRAME_pain101, FRAME_pain105,
            chick_frames_pain1, chick_run);

    static final mframe_t[] chick_frames_pain2 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final mmove_t chick_move_pain2 = new mmove_t(FRAME_pain201, FRAME_pain205,
            chick_frames_pain2, chick_run);

    static final mframe_t[] chick_frames_pain3 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, -6.0F, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 11.0F, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 4.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, -3.0F, null),
            new mframe_t(GameAI.ai_move, -4.0F, null),
            new mframe_t(GameAI.ai_move, 5.0F, null),
            new mframe_t(GameAI.ai_move, 7.0F, null),
            new mframe_t(GameAI.ai_move, -2.0F, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, -5.0F, null),
            new mframe_t(GameAI.ai_move, -2.0F, null),
            new mframe_t(GameAI.ai_move, -8.0F, null),
            new mframe_t(GameAI.ai_move, 2.0F, null) };

    static final mmove_t chick_move_pain3 = new mmove_t(FRAME_pain301, FRAME_pain321,
            chick_frames_pain3, chick_run);

    static final EntPainAdapter chick_pain = new EntPainAdapter() {
    	@Override
        public String getID() { return "chick_pain"; }
        @Override
        public void pain(edict_t self, edict_t other, float kick, int damage) {

            if (self.health < (self.max_health / 2))
                self.s.skinnum = 1;

            if (GameBase.level.time < self.pain_debounce_time)
                return;

            self.pain_debounce_time = GameBase.level.time + 3.0F;

            float r = Lib.random();
            if ((double) r < 0.33)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain1, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
            else if ((double) r < 0.66)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain2, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
            else
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain3, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);

            if (GameBase.skill.value == 3.0F)
                return; 

            if (damage <= 10)
                self.monsterinfo.currentmove = chick_move_pain1;
            else if (damage <= 25)
                self.monsterinfo.currentmove = chick_move_pain2;
            else
                self.monsterinfo.currentmove = chick_move_pain3;
        }
    };

    static final EntThinkAdapter chick_dead = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_dead"; }
        @Override
        public boolean think(edict_t self) {
            Math3D.VectorSet(self.mins, -16.0F, -16.0F, (float) 0);
            Math3D.VectorSet(self.maxs, 16.0F, 16.0F, 16.0F);
            self.movetype = Defines.MOVETYPE_TOSS;
            self.svflags |= Defines.SVF_DEADMONSTER;
            self.nextthink = (float) 0;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static final mframe_t[] chick_frames_death2 = {
            new mframe_t(GameAI.ai_move, -6.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, -1.0F, null),
            new mframe_t(GameAI.ai_move, -5.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, -1.0F, null),
            new mframe_t(GameAI.ai_move, -2.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, 10.0F, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, -3.0F, null),
            new mframe_t(GameAI.ai_move, -5.0F, null),
            new mframe_t(GameAI.ai_move, 4.0F, null),
            new mframe_t(GameAI.ai_move, 15.0F, null),
            new mframe_t(GameAI.ai_move, 14.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null) };

    static final mmove_t chick_move_death2 = new mmove_t(FRAME_death201,
            FRAME_death223, chick_frames_death2, chick_dead);

    static final mframe_t[] chick_frames_death1 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, -7.0F, null),
            new mframe_t(GameAI.ai_move, 4.0F, null),
            new mframe_t(GameAI.ai_move, 11.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final mmove_t chick_move_death1 = new mmove_t(FRAME_death101,
            FRAME_death112, chick_frames_death1, chick_dead);

    static final EntDieAdapter chick_die = new EntDieAdapter() {
    	@Override
        public String getID() { return "chick_die"; }

        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {
            int n;

            
            if (self.health <= self.gib_health) {
                game_import_t
                        .sound(self, Defines.CHAN_VOICE, game_import_t
                                .soundindex("misc/udeath.wav"), 1.0F,
                                (float) Defines.ATTN_NORM, (float) 0);
                for (n = 0; n < 2; n++)
                    GameMisc.ThrowGib(self, "models/objects/gibs/bone/tris.md2",
                            damage, Defines.GIB_ORGANIC);
                for (n = 0; n < 4; n++)
                    GameMisc.ThrowGib(self,
                            "models/objects/gibs/sm_meat/tris.md2", damage,
                            Defines.GIB_ORGANIC);
                GameMisc.ThrowHead(self, "models/objects/gibs/head2/tris.md2",
                        damage, Defines.GIB_ORGANIC);
                self.deadflag = Defines.DEAD_DEAD;
                return;
            }

            if (self.deadflag == Defines.DEAD_DEAD)
                return;

            
            self.deadflag = Defines.DEAD_DEAD;
            self.takedamage = Defines.DAMAGE_YES;

            n = (int) Lib.rand() % 2;
            if (n == 0) {
                self.monsterinfo.currentmove = chick_move_death1;
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_death1, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
            } else {
                self.monsterinfo.currentmove = chick_move_death2;
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_death2, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
            }
        }

    };

    static final EntThinkAdapter chick_duck_down = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_duck_down"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.aiflags & Defines.AI_DUCKED) != 0)
                return true;
            self.monsterinfo.aiflags |= Defines.AI_DUCKED;
            self.maxs[2] -= 32.0F;
            self.takedamage = Defines.DAMAGE_YES;
            self.monsterinfo.pausetime = GameBase.level.time + 1.0F;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static final EntThinkAdapter chick_duck_hold = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_duck_hold"; }
        @Override
        public boolean think(edict_t self) {
            if (GameBase.level.time >= self.monsterinfo.pausetime)
                self.monsterinfo.aiflags &= ~Defines.AI_HOLD_FRAME;
            else
                self.monsterinfo.aiflags |= Defines.AI_HOLD_FRAME;
            return true;
        }
    };

    static final EntThinkAdapter chick_duck_up = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_duck_up"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.aiflags &= ~Defines.AI_DUCKED;
            self.maxs[2] += 32.0F;
            self.takedamage = Defines.DAMAGE_AIM;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static final mframe_t[] chick_frames_duck = {
            new mframe_t(GameAI.ai_move, (float) 0, chick_duck_down),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, 4.0F, chick_duck_hold),
            new mframe_t(GameAI.ai_move, -4.0F, null),
            new mframe_t(GameAI.ai_move, -5.0F, chick_duck_up),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null) };

    static final mmove_t chick_move_duck = new mmove_t(FRAME_duck01, FRAME_duck07,
            chick_frames_duck, chick_run);

    static final EntDodgeAdapter chick_dodge = new EntDodgeAdapter() {
    	@Override
        public String getID() { return "chick_dodge"; }
        @Override
        public void dodge(edict_t self, edict_t attacker, float eta) {
            if ((double) Lib.random() > 0.25)
                return;

            if (self.enemy != null)
                self.enemy = attacker;

            self.monsterinfo.currentmove = chick_move_duck;
        }
    };

    static final EntThinkAdapter ChickSlash = new EntThinkAdapter() {
    	@Override
        public String getID() { return "ChickSlash"; }
        @Override
        public boolean think(edict_t self) {
            float[] aim = {(float) 0, (float) 0, (float) 0};

            Math3D.VectorSet(aim, (float) Defines.MELEE_DISTANCE, self.mins[0], 10.0F);
            game_import_t.sound(self, Defines.CHAN_WEAPON, sound_melee_swing, 1.0F,
                    (float) Defines.ATTN_NORM, (float) 0);
            GameWeapon.fire_hit(self, aim, (10 + ((int) Lib.rand() % 6)), 100);
            return true;
        }
    };

    static final EntThinkAdapter ChickRocket = new EntThinkAdapter() {
    	@Override
        public String getID() { return "ChickRocket"; }
        @Override
        public boolean think(edict_t self) {
            float[] forward = {(float) 0, (float) 0, (float) 0}, right = {(float) 0, (float) 0, (float) 0};

            Math3D.AngleVectors(self.s.angles, forward, right, null);
            float[] start = {(float) 0, (float) 0, (float) 0};
            Math3D.G_ProjectSource(self.s.origin,
                    M_Flash.monster_flash_offset[Defines.MZ2_CHICK_ROCKET_1],
                    forward, right, start);

            float[] vec = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorCopy(self.enemy.s.origin, vec);
            vec[2] = vec[2] + (float) self.enemy.viewheight;
            float[] dir = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorSubtract(vec, start, dir);
            Math3D.VectorNormalize(dir);

            Monster.monster_fire_rocket(self, start, dir, 50, 500,
                    Defines.MZ2_CHICK_ROCKET_1);
            return true;
        }
    };

    static final EntThinkAdapter Chick_PreAttack1 = new EntThinkAdapter() {
    	@Override
        public String getID() { return "Chick_PreAttack1"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_VOICE,
                    sound_missile_prelaunch, 1.0F, (float) Defines.ATTN_NORM, (float) 0);
            return true;
        }
    };

    static final EntThinkAdapter ChickReload = new EntThinkAdapter() {
    	@Override
        public String getID() { return "ChickReload"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_missile_reload,
                    1.0F, (float) Defines.ATTN_NORM, (float) 0);
            return true;
        }
    };

    static final EntThinkAdapter chick_attack1 = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_attack1"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = chick_move_attack1;
            return true;
        }
    };

    static final EntThinkAdapter chick_rerocket = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_rerocket"; }
        @Override
        public boolean think(edict_t self) {
            if (self.enemy.health > 0) {
                if (GameUtil.range(self, self.enemy) > Defines.RANGE_MELEE)
                    if (GameUtil.visible(self, self.enemy))
                        if ((double) Lib.random() <= 0.6) {
                            self.monsterinfo.currentmove = chick_move_attack1;
                            return true;
                        }
            }
            self.monsterinfo.currentmove = chick_move_end_attack1;
            return true;
        }
    };

    static final mframe_t[] chick_frames_start_attack1 = {
            new mframe_t(GameAI.ai_charge, (float) 0, Chick_PreAttack1),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, 4.0F, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, -3.0F, null),
            new mframe_t(GameAI.ai_charge, 3.0F, null),
            new mframe_t(GameAI.ai_charge, 5.0F, null),
            new mframe_t(GameAI.ai_charge, 7.0F, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, chick_attack1) };

    static final mmove_t chick_move_start_attack1 = new mmove_t(FRAME_attak101,
            FRAME_attak113, chick_frames_start_attack1, null);

    static final mframe_t[] chick_frames_attack1 = {
            new mframe_t(GameAI.ai_charge, 19.0F, ChickRocket),
            new mframe_t(GameAI.ai_charge, -6.0F, null),
            new mframe_t(GameAI.ai_charge, -5.0F, null),
            new mframe_t(GameAI.ai_charge, -2.0F, null),
            new mframe_t(GameAI.ai_charge, -7.0F, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, 1.0F, null),
            new mframe_t(GameAI.ai_charge, 10.0F, ChickReload),
            new mframe_t(GameAI.ai_charge, 4.0F, null),
            new mframe_t(GameAI.ai_charge, 5.0F, null),
            new mframe_t(GameAI.ai_charge, 6.0F, null),
            new mframe_t(GameAI.ai_charge, 6.0F, null),
            new mframe_t(GameAI.ai_charge, 4.0F, null),
            new mframe_t(GameAI.ai_charge, 3.0F, chick_rerocket) };

    static final mmove_t chick_move_attack1 = new mmove_t(FRAME_attak114,
            FRAME_attak127, chick_frames_attack1, null);

    static final mframe_t[] chick_frames_end_attack1 = {
            new mframe_t(GameAI.ai_charge, -3.0F, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, -6.0F, null),
            new mframe_t(GameAI.ai_charge, -4.0F, null),
            new mframe_t(GameAI.ai_charge, -2.0F, null) };

    static final mmove_t chick_move_end_attack1 = new mmove_t(FRAME_attak128,
            FRAME_attak132, chick_frames_end_attack1, chick_run);

    static final EntThinkAdapter chick_reslash = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_reslash"; }
        @Override
        public boolean think(edict_t self) {
            if (self.enemy.health > 0) {
                if (GameUtil.range(self, self.enemy) == Defines.RANGE_MELEE)
                    if ((double) Lib.random() <= 0.9) {
                        self.monsterinfo.currentmove = chick_move_slash;
                        return true;
                    } else {
                        self.monsterinfo.currentmove = chick_move_end_slash;
                        return true;
                    }
            }
            self.monsterinfo.currentmove = chick_move_end_slash;
            return true;
        }
    };

    static final mframe_t[] chick_frames_slash = {
            new mframe_t(GameAI.ai_charge, 1.0F, null),
            new mframe_t(GameAI.ai_charge, 7.0F, ChickSlash),
            new mframe_t(GameAI.ai_charge, -7.0F, null),
            new mframe_t(GameAI.ai_charge, 1.0F, null),
            new mframe_t(GameAI.ai_charge, -1.0F, null),
            new mframe_t(GameAI.ai_charge, 1.0F, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, 1.0F, null),
            new mframe_t(GameAI.ai_charge, -2.0F, chick_reslash) };

    static final mmove_t chick_move_slash = new mmove_t(FRAME_attak204,
            FRAME_attak212, chick_frames_slash, null);

    static final mframe_t[] chick_frames_end_slash = {
            new mframe_t(GameAI.ai_charge, -6.0F, null),
            new mframe_t(GameAI.ai_charge, -1.0F, null),
            new mframe_t(GameAI.ai_charge, -6.0F, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null) };

    static final mmove_t chick_move_end_slash = new mmove_t(FRAME_attak213,
            FRAME_attak216, chick_frames_end_slash, chick_run);

    static final EntThinkAdapter chick_slash = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_slash"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = chick_move_slash;
            return true;
        }
    };

    static final mframe_t[] chick_frames_start_slash = {
            new mframe_t(GameAI.ai_charge, 1.0F, null),
            new mframe_t(GameAI.ai_charge, 8.0F, null),
            new mframe_t(GameAI.ai_charge, 3.0F, null) };

    static final mmove_t chick_move_start_slash = new mmove_t(FRAME_attak201,
            FRAME_attak203, chick_frames_start_slash, chick_slash);

    static final EntThinkAdapter chick_melee = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_melee"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = chick_move_start_slash;
            return true;
        }
    };

    static final EntThinkAdapter chick_attack = new EntThinkAdapter() {
    	@Override
        public String getID() { return "chick_attack"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = chick_move_start_attack1;
            return true;
        }
    };

    static final EntInteractAdapter chick_sight = new EntInteractAdapter() {
    	@Override
        public String getID() { return "chick_sight"; }
        @Override
        public boolean interact(edict_t self, edict_t other) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_sight, 1.0F,
                    (float) Defines.ATTN_NORM, (float) 0);
            return true;
        }
    };

    /*
     * QUAKED monster_chick (1 .5 0) (-16 -16 -24) (16 16 32) Ambush
     * Trigger_Spawn Sight
     */
    public static void SP_monster_chick(edict_t self) {
        if (GameBase.deathmatch.value != (float) 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        sound_missile_prelaunch = game_import_t.soundindex("chick/chkatck1.wav");
        sound_missile_launch = game_import_t.soundindex("chick/chkatck2.wav");
        sound_melee_swing = game_import_t.soundindex("chick/chkatck3.wav");
        sound_melee_hit = game_import_t.soundindex("chick/chkatck4.wav");
        sound_missile_reload = game_import_t.soundindex("chick/chkatck5.wav");
        sound_death1 = game_import_t.soundindex("chick/chkdeth1.wav");
        sound_death2 = game_import_t.soundindex("chick/chkdeth2.wav");
        sound_fall_down = game_import_t.soundindex("chick/chkfall1.wav");
        sound_idle1 = game_import_t.soundindex("chick/chkidle1.wav");
        sound_idle2 = game_import_t.soundindex("chick/chkidle2.wav");
        sound_pain1 = game_import_t.soundindex("chick/chkpain1.wav");
        sound_pain2 = game_import_t.soundindex("chick/chkpain2.wav");
        sound_pain3 = game_import_t.soundindex("chick/chkpain3.wav");
        sound_sight = game_import_t.soundindex("chick/chksght1.wav");
        sound_search = game_import_t.soundindex("chick/chksrch1.wav");

        self.movetype = Defines.MOVETYPE_STEP;
        self.solid = Defines.SOLID_BBOX;
        self.s.modelindex = game_import_t
                .modelindex("models/monsters/bitch/tris.md2");
        Math3D.VectorSet(self.mins, -16.0F, -16.0F, (float) 0);
        Math3D.VectorSet(self.maxs, 16.0F, 16.0F, 56.0F);

        self.health = 175;
        self.gib_health = -70;
        self.mass = 200;

        self.pain = chick_pain;
        self.die = chick_die;

        self.monsterinfo.stand = chick_stand;
        self.monsterinfo.walk = chick_walk;
        self.monsterinfo.run = chick_run;
        self.monsterinfo.dodge = chick_dodge;
        self.monsterinfo.attack = chick_attack;
        self.monsterinfo.melee = chick_melee;
        self.monsterinfo.sight = chick_sight;

        game_import_t.linkentity(self);

        self.monsterinfo.currentmove = chick_move_stand;
        self.monsterinfo.scale = MODEL_SCALE;

        GameAI.walkmonster_start.think(self);
    }
}