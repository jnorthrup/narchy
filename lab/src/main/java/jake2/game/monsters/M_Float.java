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
import jake2.Globals;
import jake2.game.*;
import jake2.util.Lib;
import jake2.util.Math3D;

public class M_Float {

    

    public static final int FRAME_actvat01 = 0;

    public static final int FRAME_actvat02 = 1;

    public static final int FRAME_actvat03 = 2;

    public static final int FRAME_actvat04 = 3;

    public static final int FRAME_actvat05 = 4;

    public static final int FRAME_actvat06 = 5;

    public static final int FRAME_actvat07 = 6;

    public static final int FRAME_actvat08 = 7;

    public static final int FRAME_actvat09 = 8;

    public static final int FRAME_actvat10 = 9;

    public static final int FRAME_actvat11 = 10;

    public static final int FRAME_actvat12 = 11;

    public static final int FRAME_actvat13 = 12;

    public static final int FRAME_actvat14 = 13;

    public static final int FRAME_actvat15 = 14;

    public static final int FRAME_actvat16 = 15;

    public static final int FRAME_actvat17 = 16;

    public static final int FRAME_actvat18 = 17;

    public static final int FRAME_actvat19 = 18;

    public static final int FRAME_actvat20 = 19;

    public static final int FRAME_actvat21 = 20;

    public static final int FRAME_actvat22 = 21;

    public static final int FRAME_actvat23 = 22;

    public static final int FRAME_actvat24 = 23;

    public static final int FRAME_actvat25 = 24;

    public static final int FRAME_actvat26 = 25;

    public static final int FRAME_actvat27 = 26;

    public static final int FRAME_actvat28 = 27;

    public static final int FRAME_actvat29 = 28;

    public static final int FRAME_actvat30 = 29;

    public static final int FRAME_actvat31 = 30;

    public static final int FRAME_attak101 = 31;

    public static final int FRAME_attak102 = 32;

    public static final int FRAME_attak103 = 33;

    public static final int FRAME_attak104 = 34;

    public static final int FRAME_attak105 = 35;

    public static final int FRAME_attak106 = 36;

    public static final int FRAME_attak107 = 37;

    public static final int FRAME_attak108 = 38;

    public static final int FRAME_attak109 = 39;

    public static final int FRAME_attak110 = 40;

    public static final int FRAME_attak111 = 41;

    public static final int FRAME_attak112 = 42;

    public static final int FRAME_attak113 = 43;

    public static final int FRAME_attak114 = 44;

    public static final int FRAME_attak201 = 45;

    public static final int FRAME_attak202 = 46;

    public static final int FRAME_attak203 = 47;

    public static final int FRAME_attak204 = 48;

    public static final int FRAME_attak205 = 49;

    public static final int FRAME_attak206 = 50;

    public static final int FRAME_attak207 = 51;

    public static final int FRAME_attak208 = 52;

    public static final int FRAME_attak209 = 53;

    public static final int FRAME_attak210 = 54;

    public static final int FRAME_attak211 = 55;

    public static final int FRAME_attak212 = 56;

    public static final int FRAME_attak213 = 57;

    public static final int FRAME_attak214 = 58;

    public static final int FRAME_attak215 = 59;

    public static final int FRAME_attak216 = 60;

    public static final int FRAME_attak217 = 61;

    public static final int FRAME_attak218 = 62;

    public static final int FRAME_attak219 = 63;

    public static final int FRAME_attak220 = 64;

    public static final int FRAME_attak221 = 65;

    public static final int FRAME_attak222 = 66;

    public static final int FRAME_attak223 = 67;

    public static final int FRAME_attak224 = 68;

    public static final int FRAME_attak225 = 69;

    public static final int FRAME_attak301 = 70;

    public static final int FRAME_attak302 = 71;

    public static final int FRAME_attak303 = 72;

    public static final int FRAME_attak304 = 73;

    public static final int FRAME_attak305 = 74;

    public static final int FRAME_attak306 = 75;

    public static final int FRAME_attak307 = 76;

    public static final int FRAME_attak308 = 77;

    public static final int FRAME_attak309 = 78;

    public static final int FRAME_attak310 = 79;

    public static final int FRAME_attak311 = 80;

    public static final int FRAME_attak312 = 81;

    public static final int FRAME_attak313 = 82;

    public static final int FRAME_attak314 = 83;

    public static final int FRAME_attak315 = 84;

    public static final int FRAME_attak316 = 85;

    public static final int FRAME_attak317 = 86;

    public static final int FRAME_attak318 = 87;

    public static final int FRAME_attak319 = 88;

    public static final int FRAME_attak320 = 89;

    public static final int FRAME_attak321 = 90;

    public static final int FRAME_attak322 = 91;

    public static final int FRAME_attak323 = 92;

    public static final int FRAME_attak324 = 93;

    public static final int FRAME_attak325 = 94;

    public static final int FRAME_attak326 = 95;

    public static final int FRAME_attak327 = 96;

    public static final int FRAME_attak328 = 97;

    public static final int FRAME_attak329 = 98;

    public static final int FRAME_attak330 = 99;

    public static final int FRAME_attak331 = 100;

    public static final int FRAME_attak332 = 101;

    public static final int FRAME_attak333 = 102;

    public static final int FRAME_attak334 = 103;

    public static final int FRAME_death01 = 104;

    public static final int FRAME_death02 = 105;

    public static final int FRAME_death03 = 106;

    public static final int FRAME_death04 = 107;

    public static final int FRAME_death05 = 108;

    public static final int FRAME_death06 = 109;

    public static final int FRAME_death07 = 110;

    public static final int FRAME_death08 = 111;

    public static final int FRAME_death09 = 112;

    public static final int FRAME_death10 = 113;

    public static final int FRAME_death11 = 114;

    public static final int FRAME_death12 = 115;

    public static final int FRAME_death13 = 116;

    public static final int FRAME_pain101 = 117;

    public static final int FRAME_pain102 = 118;

    public static final int FRAME_pain103 = 119;

    public static final int FRAME_pain104 = 120;

    public static final int FRAME_pain105 = 121;

    public static final int FRAME_pain106 = 122;

    public static final int FRAME_pain107 = 123;

    public static final int FRAME_pain201 = 124;

    public static final int FRAME_pain202 = 125;

    public static final int FRAME_pain203 = 126;

    public static final int FRAME_pain204 = 127;

    public static final int FRAME_pain205 = 128;

    public static final int FRAME_pain206 = 129;

    public static final int FRAME_pain207 = 130;

    public static final int FRAME_pain208 = 131;

    public static final int FRAME_pain301 = 132;

    public static final int FRAME_pain302 = 133;

    public static final int FRAME_pain303 = 134;

    public static final int FRAME_pain304 = 135;

    public static final int FRAME_pain305 = 136;

    public static final int FRAME_pain306 = 137;

    public static final int FRAME_pain307 = 138;

    public static final int FRAME_pain308 = 139;

    public static final int FRAME_pain309 = 140;

    public static final int FRAME_pain310 = 141;

    public static final int FRAME_pain311 = 142;

    public static final int FRAME_pain312 = 143;

    public static final int FRAME_stand101 = 144;

    public static final int FRAME_stand102 = 145;

    public static final int FRAME_stand103 = 146;

    public static final int FRAME_stand104 = 147;

    public static final int FRAME_stand105 = 148;

    public static final int FRAME_stand106 = 149;

    public static final int FRAME_stand107 = 150;

    public static final int FRAME_stand108 = 151;

    public static final int FRAME_stand109 = 152;

    public static final int FRAME_stand110 = 153;

    public static final int FRAME_stand111 = 154;

    public static final int FRAME_stand112 = 155;

    public static final int FRAME_stand113 = 156;

    public static final int FRAME_stand114 = 157;

    public static final int FRAME_stand115 = 158;

    public static final int FRAME_stand116 = 159;

    public static final int FRAME_stand117 = 160;

    public static final int FRAME_stand118 = 161;

    public static final int FRAME_stand119 = 162;

    public static final int FRAME_stand120 = 163;

    public static final int FRAME_stand121 = 164;

    public static final int FRAME_stand122 = 165;

    public static final int FRAME_stand123 = 166;

    public static final int FRAME_stand124 = 167;

    public static final int FRAME_stand125 = 168;

    public static final int FRAME_stand126 = 169;

    public static final int FRAME_stand127 = 170;

    public static final int FRAME_stand128 = 171;

    public static final int FRAME_stand129 = 172;

    public static final int FRAME_stand130 = 173;

    public static final int FRAME_stand131 = 174;

    public static final int FRAME_stand132 = 175;

    public static final int FRAME_stand133 = 176;

    public static final int FRAME_stand134 = 177;

    public static final int FRAME_stand135 = 178;

    public static final int FRAME_stand136 = 179;

    public static final int FRAME_stand137 = 180;

    public static final int FRAME_stand138 = 181;

    public static final int FRAME_stand139 = 182;

    public static final int FRAME_stand140 = 183;

    public static final int FRAME_stand141 = 184;

    public static final int FRAME_stand142 = 185;

    public static final int FRAME_stand143 = 186;

    public static final int FRAME_stand144 = 187;

    public static final int FRAME_stand145 = 188;

    public static final int FRAME_stand146 = 189;

    public static final int FRAME_stand147 = 190;

    public static final int FRAME_stand148 = 191;

    public static final int FRAME_stand149 = 192;

    public static final int FRAME_stand150 = 193;

    public static final int FRAME_stand151 = 194;

    public static final int FRAME_stand152 = 195;

    public static final int FRAME_stand201 = 196;

    public static final int FRAME_stand202 = 197;

    public static final int FRAME_stand203 = 198;

    public static final int FRAME_stand204 = 199;

    public static final int FRAME_stand205 = 200;

    public static final int FRAME_stand206 = 201;

    public static final int FRAME_stand207 = 202;

    public static final int FRAME_stand208 = 203;

    public static final int FRAME_stand209 = 204;

    public static final int FRAME_stand210 = 205;

    public static final int FRAME_stand211 = 206;

    public static final int FRAME_stand212 = 207;

    public static final int FRAME_stand213 = 208;

    public static final int FRAME_stand214 = 209;

    public static final int FRAME_stand215 = 210;

    public static final int FRAME_stand216 = 211;

    public static final int FRAME_stand217 = 212;

    public static final int FRAME_stand218 = 213;

    public static final int FRAME_stand219 = 214;

    public static final int FRAME_stand220 = 215;

    public static final int FRAME_stand221 = 216;

    public static final int FRAME_stand222 = 217;

    public static final int FRAME_stand223 = 218;

    public static final int FRAME_stand224 = 219;

    public static final int FRAME_stand225 = 220;

    public static final int FRAME_stand226 = 221;

    public static final int FRAME_stand227 = 222;

    public static final int FRAME_stand228 = 223;

    public static final int FRAME_stand229 = 224;

    public static final int FRAME_stand230 = 225;

    public static final int FRAME_stand231 = 226;

    public static final int FRAME_stand232 = 227;

    public static final int FRAME_stand233 = 228;

    public static final int FRAME_stand234 = 229;

    public static final int FRAME_stand235 = 230;

    public static final int FRAME_stand236 = 231;

    public static final int FRAME_stand237 = 232;

    public static final int FRAME_stand238 = 233;

    public static final int FRAME_stand239 = 234;

    public static final int FRAME_stand240 = 235;

    public static final int FRAME_stand241 = 236;

    public static final int FRAME_stand242 = 237;

    public static final int FRAME_stand243 = 238;

    public static final int FRAME_stand244 = 239;

    public static final int FRAME_stand245 = 240;

    public static final int FRAME_stand246 = 241;

    public static final int FRAME_stand247 = 242;

    public static final int FRAME_stand248 = 243;

    public static final int FRAME_stand249 = 244;

    public static final int FRAME_stand250 = 245;

    public static final int FRAME_stand251 = 246;

    public static final int FRAME_stand252 = 247;

    public static final float MODEL_SCALE = 1.000000f;

    static int sound_attack2;

    static int sound_attack3;

    static int sound_death1;

    static int sound_idle;

    static int sound_pain1;

    static int sound_pain2;

    static int sound_sight;

    static final EntInteractAdapter floater_sight = new EntInteractAdapter() {
    	@Override
        public String getID() { return "floater_sight"; }
        @Override
        public boolean interact(edict_t self, edict_t other) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_sight, 1.0F,
                    (float) Defines.ATTN_NORM, (float) 0);
            return true;
        }
    };

    static final EntThinkAdapter floater_idle = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_idle"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_idle, 1.0F,
                    (float) Defines.ATTN_IDLE, (float) 0);
            return true;
        }
    };

    static final EntThinkAdapter floater_fire_blaster = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_fire_blaster"; }
        @Override
        public boolean think(edict_t self) {
            int effect;

            if ((self.s.frame == FRAME_attak104)
                    || (self.s.frame == FRAME_attak107))
                effect = Defines.EF_HYPERBLASTER;
            else
                effect = 0;
            float[] right = {(float) 0, (float) 0, (float) 0};
            float[] forward = {(float) 0, (float) 0, (float) 0};
            Math3D.AngleVectors(self.s.angles, forward, right, null);
            float[] start = {(float) 0, (float) 0, (float) 0};
            Math3D.G_ProjectSource(self.s.origin,
                    M_Flash.monster_flash_offset[Defines.MZ2_FLOAT_BLASTER_1],
                    forward, right, start);

            float[] end = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorCopy(self.enemy.s.origin, end);
            end[2] = end[2] + (float) self.enemy.viewheight;
            float[] dir = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorSubtract(end, start, dir);

            Monster.monster_fire_blaster(self, start, dir, 1, 1000,
                    Defines.MZ2_FLOAT_BLASTER_1, effect);

            return true;
        }
    };

    static final mframe_t[] floater_frames_stand1 = {
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
            new mframe_t(GameAI.ai_stand, (float) 0, null) };

    static final mmove_t floater_move_stand1 = new mmove_t(FRAME_stand101,
            FRAME_stand152, floater_frames_stand1, null);

    static final mframe_t[] floater_frames_stand2 = {
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
            new mframe_t(GameAI.ai_stand, (float) 0, null) };

    static final mmove_t floater_move_stand2 = new mmove_t(FRAME_stand201,
            FRAME_stand252, floater_frames_stand2, null);

    static final EntThinkAdapter floater_stand = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_stand"; }
        @Override
        public boolean think(edict_t self) {
            if ((double) Lib.random() <= 0.5)
                self.monsterinfo.currentmove = floater_move_stand1;
            else
                self.monsterinfo.currentmove = floater_move_stand2;
            return true;
        }
    };

    static final mframe_t[] floater_frames_activate = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static mmove_t floater_move_activate = new mmove_t(FRAME_actvat01,
            FRAME_actvat31, floater_frames_activate, null);

    static final EntThinkAdapter floater_run = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_run"; }
        @Override
        public boolean think(edict_t self) {

            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0)
                self.monsterinfo.currentmove = floater_move_stand1;
            else
                self.monsterinfo.currentmove = floater_move_run;

            return true;
        }
    };

    static final mframe_t[] floater_frames_attack1 = {
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, floater_fire_blaster),
            
            new mframe_t(GameAI.ai_charge, (float) 0, floater_fire_blaster),
            new mframe_t(GameAI.ai_charge, (float) 0, floater_fire_blaster),
            new mframe_t(GameAI.ai_charge, (float) 0, floater_fire_blaster),
            new mframe_t(GameAI.ai_charge, (float) 0, floater_fire_blaster),
            new mframe_t(GameAI.ai_charge, (float) 0, floater_fire_blaster),
            new mframe_t(GameAI.ai_charge, (float) 0, floater_fire_blaster),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null)
    
    };

    static final mmove_t floater_move_attack1 = new mmove_t(FRAME_attak101,
            FRAME_attak114, floater_frames_attack1, floater_run);

    static final float[] aim = {(float) Defines.MELEE_DISTANCE, (float) 0, (float) 0};

    static final EntThinkAdapter floater_wham = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_wham"; }
        @Override
        public boolean think(edict_t self) {

            game_import_t.sound(self, Defines.CHAN_WEAPON, sound_attack3, 1.0F,
                    (float) Defines.ATTN_NORM, (float) 0);
            GameWeapon.fire_hit(self, aim, 5 + (int) Lib.rand() % 6, -50);
            return true;
        }
    };

    static final mframe_t[] floater_frames_attack2 = {
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, floater_wham),
            
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null) };

    static final mmove_t floater_move_attack2 = new mmove_t(FRAME_attak201,
            FRAME_attak225, floater_frames_attack2, floater_run);

    static final EntThinkAdapter floater_zap = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_zap"; }
        @Override
        public boolean think(edict_t self) {
            float[] dir = {(float) 0, (float) 0, (float) 0};

            Math3D.VectorSubtract(self.enemy.s.origin, self.s.origin, dir);

            float[] right = {(float) 0, (float) 0, (float) 0};
            float[] forward = {(float) 0, (float) 0, (float) 0};
            Math3D.AngleVectors(self.s.angles, forward, right, null);


            float[] offset = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorSet(offset, 18.5f, -0.9f, 10f);
            float[] origin = {(float) 0, (float) 0, (float) 0};
            Math3D.G_ProjectSource(self.s.origin, offset, forward, right,
                    origin);
            
            

            game_import_t.sound(self, Defines.CHAN_WEAPON, sound_attack2, 1.0F,
                    (float) Defines.ATTN_NORM, (float) 0);

            
            game_import_t.WriteByte(Defines.svc_temp_entity);
            game_import_t.WriteByte(Defines.TE_SPLASH);
            game_import_t.WriteByte(32);
            game_import_t.WritePosition(origin);
            game_import_t.WriteDir(dir);
            game_import_t.WriteByte(1); 
            game_import_t.multicast(origin, Defines.MULTICAST_PVS);

            GameCombat.T_Damage(self.enemy, self, self, dir, self.enemy.s.origin,
                    Globals.vec3_origin, 5 + (int) Lib.rand() % 6, -10,
                    Defines.DAMAGE_ENERGY, Defines.MOD_UNKNOWN);
            return true;
        }
    };

    static final mframe_t[] floater_frames_attack3 = {
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, floater_zap),
            
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null) };

    static final mmove_t floater_move_attack3 = new mmove_t(FRAME_attak301,
            FRAME_attak334, floater_frames_attack3, floater_run);

    static final mframe_t[] floater_frames_death = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final EntThinkAdapter floater_dead = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_dead"; }
        @Override
        public boolean think(edict_t self) {
            Math3D.VectorSet(self.mins, -16.0F, -16.0F, -24.0F);
            Math3D.VectorSet(self.maxs, 16.0F, 16.0F, -8.0F);
            self.movetype = Defines.MOVETYPE_TOSS;
            self.svflags |= Defines.SVF_DEADMONSTER;
            self.nextthink = (float) 0;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static mmove_t floater_move_death = new mmove_t(FRAME_death01,
            FRAME_death13, floater_frames_death, floater_dead);

    static final mframe_t[] floater_frames_pain1 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final mmove_t floater_move_pain1 = new mmove_t(FRAME_pain101,
            FRAME_pain107, floater_frames_pain1, floater_run);

    static final mframe_t[] floater_frames_pain2 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final mmove_t floater_move_pain2 = new mmove_t(FRAME_pain201,
            FRAME_pain208, floater_frames_pain2, floater_run);

    static final mframe_t[] floater_frames_pain3 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static mmove_t floater_move_pain3 = new mmove_t(FRAME_pain301,
            FRAME_pain312, floater_frames_pain3, floater_run);

    static final mframe_t[] floater_frames_walk = {
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null),
            new mframe_t(GameAI.ai_walk, 5.0F, null) };

    static final mmove_t floater_move_walk = new mmove_t(FRAME_stand101,
            FRAME_stand152, floater_frames_walk, null);

    static final mframe_t[] floater_frames_run = {
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null),
            new mframe_t(GameAI.ai_run, 13.0F, null) };

    static final mmove_t floater_move_run = new mmove_t(FRAME_stand101,
            FRAME_stand152, floater_frames_run, null);

    static final EntThinkAdapter floater_walk = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_walk"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = floater_move_walk;
            return true;
        }
    };

    static final EntThinkAdapter floater_attack = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_attack"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = floater_move_attack1;
            return true;
        }
    };

    static final EntThinkAdapter floater_melee = new EntThinkAdapter() {
    	@Override
        public String getID() { return "floater_melee"; }
        @Override
        public boolean think(edict_t self) {

            if ((double) Lib.random() < 0.5)
                self.monsterinfo.currentmove = floater_move_attack3;
            else
                self.monsterinfo.currentmove = floater_move_attack2;
            return true;
        }
    };

    static final EntPainAdapter floater_pain = new EntPainAdapter() {
    	@Override
        public String getID() { return "floater_pain"; }
        @Override
        public void pain(edict_t self, edict_t other, float kick, int damage) {

            if (self.health < (self.max_health / 2))
                self.s.skinnum = 1;

            if (GameBase.level.time < self.pain_debounce_time)
                return;

            self.pain_debounce_time = GameBase.level.time + 3.0F;
            if (GameBase.skill.value == 3.0F)
                return;

            int n = ((int) Lib.rand() + 1) % 3;
            if (n == 0) {
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain1, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
                self.monsterinfo.currentmove = floater_move_pain1;
            } else {
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain2, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
                self.monsterinfo.currentmove = floater_move_pain2;
            }
        }
    };

    static final EntDieAdapter floater_die = new EntDieAdapter() {
    	@Override
        public String getID() { return "floater_die"; }

        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_death1, 1.0F,
                    (float) Defines.ATTN_NORM, (float) 0);
            GameMisc.BecomeExplosion1(self);

        }
    };

    /*
     * QUAKED monster_floater (1 .5 0) (-16 -16 -24) (16 16 32) Ambush
     * Trigger_Spawn Sight
     */
    public static void SP_monster_floater(edict_t self) {
        if (GameBase.deathmatch.value != (float) 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        sound_attack2 = game_import_t.soundindex("floater/fltatck2.wav");
        sound_attack3 = game_import_t.soundindex("floater/fltatck3.wav");
        sound_death1 = game_import_t.soundindex("floater/fltdeth1.wav");
        sound_idle = game_import_t.soundindex("floater/fltidle1.wav");
        sound_pain1 = game_import_t.soundindex("floater/fltpain1.wav");
        sound_pain2 = game_import_t.soundindex("floater/fltpain2.wav");
        sound_sight = game_import_t.soundindex("floater/fltsght1.wav");

        game_import_t.soundindex("floater/fltatck1.wav");

        self.s.sound = game_import_t.soundindex("floater/fltsrch1.wav");

        self.movetype = Defines.MOVETYPE_STEP;
        self.solid = Defines.SOLID_BBOX;
        self.s.modelindex = game_import_t
                .modelindex("models/monsters/float/tris.md2");
        Math3D.VectorSet(self.mins, -24.0F, -24.0F, -24.0F);
        Math3D.VectorSet(self.maxs, 24.0F, 24.0F, 32.0F);

        self.health = 200;
        self.gib_health = -80;
        self.mass = 300;

        self.pain = floater_pain;
        self.die = floater_die;

        self.monsterinfo.stand = floater_stand;
        self.monsterinfo.walk = floater_walk;
        self.monsterinfo.run = floater_run;
        
        self.monsterinfo.attack = floater_attack;
        self.monsterinfo.melee = floater_melee;
        self.monsterinfo.sight = floater_sight;
        self.monsterinfo.idle = floater_idle;

        game_import_t.linkentity(self);

        if ((double) Lib.random() <= 0.5)
            self.monsterinfo.currentmove = floater_move_stand1;
        else
            self.monsterinfo.currentmove = floater_move_stand2;

        self.monsterinfo.scale = MODEL_SCALE;

        GameAI.flymonster_start.think(self);
    }
}