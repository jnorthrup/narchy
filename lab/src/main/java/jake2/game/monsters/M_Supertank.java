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

public class M_Supertank {

    

    public static final int FRAME_attak1_1 = 0;

    public static final int FRAME_attak1_2 = 1;

    public static final int FRAME_attak1_3 = 2;

    public static final int FRAME_attak1_4 = 3;

    public static final int FRAME_attak1_5 = 4;

    public static final int FRAME_attak1_6 = 5;

    public static final int FRAME_attak1_7 = 6;

    public static final int FRAME_attak1_8 = 7;

    public static final int FRAME_attak1_9 = 8;

    public static final int FRAME_attak1_10 = 9;

    public static final int FRAME_attak1_11 = 10;

    public static final int FRAME_attak1_12 = 11;

    public static final int FRAME_attak1_13 = 12;

    public static final int FRAME_attak1_14 = 13;

    public static final int FRAME_attak1_15 = 14;

    public static final int FRAME_attak1_16 = 15;

    public static final int FRAME_attak1_17 = 16;

    public static final int FRAME_attak1_18 = 17;

    public static final int FRAME_attak1_19 = 18;

    public static final int FRAME_attak1_20 = 19;

    public static final int FRAME_attak2_1 = 20;

    public static final int FRAME_attak2_2 = 21;

    public static final int FRAME_attak2_3 = 22;

    public static final int FRAME_attak2_4 = 23;

    public static final int FRAME_attak2_5 = 24;

    public static final int FRAME_attak2_6 = 25;

    public static final int FRAME_attak2_7 = 26;

    public static final int FRAME_attak2_8 = 27;

    public static final int FRAME_attak2_9 = 28;

    public static final int FRAME_attak2_10 = 29;

    public static final int FRAME_attak2_11 = 30;

    public static final int FRAME_attak2_12 = 31;

    public static final int FRAME_attak2_13 = 32;

    public static final int FRAME_attak2_14 = 33;

    public static final int FRAME_attak2_15 = 34;

    public static final int FRAME_attak2_16 = 35;

    public static final int FRAME_attak2_17 = 36;

    public static final int FRAME_attak2_18 = 37;

    public static final int FRAME_attak2_19 = 38;

    public static final int FRAME_attak2_20 = 39;

    public static final int FRAME_attak2_21 = 40;

    public static final int FRAME_attak2_22 = 41;

    public static final int FRAME_attak2_23 = 42;

    public static final int FRAME_attak2_24 = 43;

    public static final int FRAME_attak2_25 = 44;

    public static final int FRAME_attak2_26 = 45;

    public static final int FRAME_attak2_27 = 46;

    public static final int FRAME_attak3_1 = 47;

    public static final int FRAME_attak3_2 = 48;

    public static final int FRAME_attak3_3 = 49;

    public static final int FRAME_attak3_4 = 50;

    public static final int FRAME_attak3_5 = 51;

    public static final int FRAME_attak3_6 = 52;

    public static final int FRAME_attak3_7 = 53;

    public static final int FRAME_attak3_8 = 54;

    public static final int FRAME_attak3_9 = 55;

    public static final int FRAME_attak3_10 = 56;

    public static final int FRAME_attak3_11 = 57;

    public static final int FRAME_attak3_12 = 58;

    public static final int FRAME_attak3_13 = 59;

    public static final int FRAME_attak3_14 = 60;

    public static final int FRAME_attak3_15 = 61;

    public static final int FRAME_attak3_16 = 62;

    public static final int FRAME_attak3_17 = 63;

    public static final int FRAME_attak3_18 = 64;

    public static final int FRAME_attak3_19 = 65;

    public static final int FRAME_attak3_20 = 66;

    public static final int FRAME_attak3_21 = 67;

    public static final int FRAME_attak3_22 = 68;

    public static final int FRAME_attak3_23 = 69;

    public static final int FRAME_attak3_24 = 70;

    public static final int FRAME_attak3_25 = 71;

    public static final int FRAME_attak3_26 = 72;

    public static final int FRAME_attak3_27 = 73;

    public static final int FRAME_attak4_1 = 74;

    public static final int FRAME_attak4_2 = 75;

    public static final int FRAME_attak4_3 = 76;

    public static final int FRAME_attak4_4 = 77;

    public static final int FRAME_attak4_5 = 78;

    public static final int FRAME_attak4_6 = 79;

    public static final int FRAME_backwd_1 = 80;

    public static final int FRAME_backwd_2 = 81;

    public static final int FRAME_backwd_3 = 82;

    public static final int FRAME_backwd_4 = 83;

    public static final int FRAME_backwd_5 = 84;

    public static final int FRAME_backwd_6 = 85;

    public static final int FRAME_backwd_7 = 86;

    public static final int FRAME_backwd_8 = 87;

    public static final int FRAME_backwd_9 = 88;

    public static final int FRAME_backwd_10 = 89;

    public static final int FRAME_backwd_11 = 90;

    public static final int FRAME_backwd_12 = 91;

    public static final int FRAME_backwd_13 = 92;

    public static final int FRAME_backwd_14 = 93;

    public static final int FRAME_backwd_15 = 94;

    public static final int FRAME_backwd_16 = 95;

    public static final int FRAME_backwd_17 = 96;

    public static final int FRAME_backwd_18 = 97;

    public static final int FRAME_death_1 = 98;

    public static final int FRAME_death_2 = 99;

    public static final int FRAME_death_3 = 100;

    public static final int FRAME_death_4 = 101;

    public static final int FRAME_death_5 = 102;

    public static final int FRAME_death_6 = 103;

    public static final int FRAME_death_7 = 104;

    public static final int FRAME_death_8 = 105;

    public static final int FRAME_death_9 = 106;

    public static final int FRAME_death_10 = 107;

    public static final int FRAME_death_11 = 108;

    public static final int FRAME_death_12 = 109;

    public static final int FRAME_death_13 = 110;

    public static final int FRAME_death_14 = 111;

    public static final int FRAME_death_15 = 112;

    public static final int FRAME_death_16 = 113;

    public static final int FRAME_death_17 = 114;

    public static final int FRAME_death_18 = 115;

    public static final int FRAME_death_19 = 116;

    public static final int FRAME_death_20 = 117;

    public static final int FRAME_death_21 = 118;

    public static final int FRAME_death_22 = 119;

    public static final int FRAME_death_23 = 120;

    public static final int FRAME_death_24 = 121;

    public static final int FRAME_death_31 = 122;

    public static final int FRAME_death_32 = 123;

    public static final int FRAME_death_33 = 124;

    public static final int FRAME_death_45 = 125;

    public static final int FRAME_death_46 = 126;

    public static final int FRAME_death_47 = 127;

    public static final int FRAME_forwrd_1 = 128;

    public static final int FRAME_forwrd_2 = 129;

    public static final int FRAME_forwrd_3 = 130;

    public static final int FRAME_forwrd_4 = 131;

    public static final int FRAME_forwrd_5 = 132;

    public static final int FRAME_forwrd_6 = 133;

    public static final int FRAME_forwrd_7 = 134;

    public static final int FRAME_forwrd_8 = 135;

    public static final int FRAME_forwrd_9 = 136;

    public static final int FRAME_forwrd_10 = 137;

    public static final int FRAME_forwrd_11 = 138;

    public static final int FRAME_forwrd_12 = 139;

    public static final int FRAME_forwrd_13 = 140;

    public static final int FRAME_forwrd_14 = 141;

    public static final int FRAME_forwrd_15 = 142;

    public static final int FRAME_forwrd_16 = 143;

    public static final int FRAME_forwrd_17 = 144;

    public static final int FRAME_forwrd_18 = 145;

    public static final int FRAME_left_1 = 146;

    public static final int FRAME_left_2 = 147;

    public static final int FRAME_left_3 = 148;

    public static final int FRAME_left_4 = 149;

    public static final int FRAME_left_5 = 150;

    public static final int FRAME_left_6 = 151;

    public static final int FRAME_left_7 = 152;

    public static final int FRAME_left_8 = 153;

    public static final int FRAME_left_9 = 154;

    public static final int FRAME_left_10 = 155;

    public static final int FRAME_left_11 = 156;

    public static final int FRAME_left_12 = 157;

    public static final int FRAME_left_13 = 158;

    public static final int FRAME_left_14 = 159;

    public static final int FRAME_left_15 = 160;

    public static final int FRAME_left_16 = 161;

    public static final int FRAME_left_17 = 162;

    public static final int FRAME_left_18 = 163;

    public static final int FRAME_pain1_1 = 164;

    public static final int FRAME_pain1_2 = 165;

    public static final int FRAME_pain1_3 = 166;

    public static final int FRAME_pain1_4 = 167;

    public static final int FRAME_pain2_5 = 168;

    public static final int FRAME_pain2_6 = 169;

    public static final int FRAME_pain2_7 = 170;

    public static final int FRAME_pain2_8 = 171;

    public static final int FRAME_pain3_9 = 172;

    public static final int FRAME_pain3_10 = 173;

    public static final int FRAME_pain3_11 = 174;

    public static final int FRAME_pain3_12 = 175;

    public static final int FRAME_right_1 = 176;

    public static final int FRAME_right_2 = 177;

    public static final int FRAME_right_3 = 178;

    public static final int FRAME_right_4 = 179;

    public static final int FRAME_right_5 = 180;

    public static final int FRAME_right_6 = 181;

    public static final int FRAME_right_7 = 182;

    public static final int FRAME_right_8 = 183;

    public static final int FRAME_right_9 = 184;

    public static final int FRAME_right_10 = 185;

    public static final int FRAME_right_11 = 186;

    public static final int FRAME_right_12 = 187;

    public static final int FRAME_right_13 = 188;

    public static final int FRAME_right_14 = 189;

    public static final int FRAME_right_15 = 190;

    public static final int FRAME_right_16 = 191;

    public static final int FRAME_right_17 = 192;

    public static final int FRAME_right_18 = 193;

    public static final int FRAME_stand_1 = 194;

    public static final int FRAME_stand_2 = 195;

    public static final int FRAME_stand_3 = 196;

    public static final int FRAME_stand_4 = 197;

    public static final int FRAME_stand_5 = 198;

    public static final int FRAME_stand_6 = 199;

    public static final int FRAME_stand_7 = 200;

    public static final int FRAME_stand_8 = 201;

    public static final int FRAME_stand_9 = 202;

    public static final int FRAME_stand_10 = 203;

    public static final int FRAME_stand_11 = 204;

    public static final int FRAME_stand_12 = 205;

    public static final int FRAME_stand_13 = 206;

    public static final int FRAME_stand_14 = 207;

    public static final int FRAME_stand_15 = 208;

    public static final int FRAME_stand_16 = 209;

    public static final int FRAME_stand_17 = 210;

    public static final int FRAME_stand_18 = 211;

    public static final int FRAME_stand_19 = 212;

    public static final int FRAME_stand_20 = 213;

    public static final int FRAME_stand_21 = 214;

    public static final int FRAME_stand_22 = 215;

    public static final int FRAME_stand_23 = 216;

    public static final int FRAME_stand_24 = 217;

    public static final int FRAME_stand_25 = 218;

    public static final int FRAME_stand_26 = 219;

    public static final int FRAME_stand_27 = 220;

    public static final int FRAME_stand_28 = 221;

    public static final int FRAME_stand_29 = 222;

    public static final int FRAME_stand_30 = 223;

    public static final int FRAME_stand_31 = 224;

    public static final int FRAME_stand_32 = 225;

    public static final int FRAME_stand_33 = 226;

    public static final int FRAME_stand_34 = 227;

    public static final int FRAME_stand_35 = 228;

    public static final int FRAME_stand_36 = 229;

    public static final int FRAME_stand_37 = 230;

    public static final int FRAME_stand_38 = 231;

    public static final int FRAME_stand_39 = 232;

    public static final int FRAME_stand_40 = 233;

    public static final int FRAME_stand_41 = 234;

    public static final int FRAME_stand_42 = 235;

    public static final int FRAME_stand_43 = 236;

    public static final int FRAME_stand_44 = 237;

    public static final int FRAME_stand_45 = 238;

    public static final int FRAME_stand_46 = 239;

    public static final int FRAME_stand_47 = 240;

    public static final int FRAME_stand_48 = 241;

    public static final int FRAME_stand_49 = 242;

    public static final int FRAME_stand_50 = 243;

    public static final int FRAME_stand_51 = 244;

    public static final int FRAME_stand_52 = 245;

    public static final int FRAME_stand_53 = 246;

    public static final int FRAME_stand_54 = 247;

    public static final int FRAME_stand_55 = 248;

    public static final int FRAME_stand_56 = 249;

    public static final int FRAME_stand_57 = 250;

    public static final int FRAME_stand_58 = 251;

    public static final int FRAME_stand_59 = 252;

    public static final int FRAME_stand_60 = 253;

    public static final float MODEL_SCALE = 1.000000f;

    static int sound_pain1;

    static int sound_pain2;

    static int sound_pain3;

    static int sound_death;

    static int sound_search1;

    static int sound_search2;

    static int tread_sound;

    static final EntThinkAdapter TreadSound = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "TreadSound"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_VOICE, tread_sound, 1.0F,
                    (float) Defines.ATTN_NORM, (float) 0);
            return true;
        }
    };

    static final EntThinkAdapter supertank_search = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "supertank_search"; }
        @Override
        public boolean think(edict_t self) {
            if ((double) Lib.random() < 0.5)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_search1, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
            else
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_search2, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
            return true;
        }
    };

    
    
    

    static final mframe_t[] supertank_frames_stand = {
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
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null),
            new mframe_t(GameAI.ai_stand, (float) 0, null) };

    static final mmove_t supertank_move_stand = new mmove_t(FRAME_stand_1,
            FRAME_stand_60, supertank_frames_stand, null);

    static final EntThinkAdapter supertank_stand = new EntThinkAdapter() {
    	@Override
        public String getID() { return "supertank_stand"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = supertank_move_stand;
            return true;
        }
    };

    static final mframe_t[] supertank_frames_run = {
            new mframe_t(GameAI.ai_run, 12.0F, TreadSound),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null),
            new mframe_t(GameAI.ai_run, 12.0F, null) };

    static final mmove_t supertank_move_run = new mmove_t(FRAME_forwrd_1,
            FRAME_forwrd_18, supertank_frames_run, null);

    
    
    

    static final mframe_t[] supertank_frames_forward = {
            new mframe_t(GameAI.ai_walk, 4.0F, TreadSound),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null) };

    static final mmove_t supertank_move_forward = new mmove_t(FRAME_forwrd_1,
            FRAME_forwrd_18, supertank_frames_forward, null);

    static EntThinkAdapter supertank_forward = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "supertank_forward"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = supertank_move_forward;
            return true;
        }
    };

    static final EntThinkAdapter supertank_walk = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "supertank_walk"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = supertank_move_forward;
            return true;
        }
    };

    static final EntThinkAdapter supertank_run = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "supertank_run"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0)
                self.monsterinfo.currentmove = supertank_move_stand;
            else
                self.monsterinfo.currentmove = supertank_move_run;
            return true;
        }
    };

    
    
    
    static final EntThinkAdapter supertank_dead = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "supertank_dead"; }
        @Override
        public boolean think(edict_t self) {
            Math3D.VectorSet(self.mins, -60.0F, -60.0F, (float) 0);
            Math3D.VectorSet(self.maxs, 60.0F, 60.0F, 72.0F);
            self.movetype = Defines.MOVETYPE_TOSS;
            self.svflags |= Defines.SVF_DEADMONSTER;
            self.nextthink = (float) 0;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static final EntThinkAdapter supertankRocket = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "supertankRocket"; }
        @Override
        public boolean think(edict_t self) {
            int flash_number;

            switch (self.s.frame) {
                case FRAME_attak2_8:
                    flash_number = Defines.MZ2_SUPERTANK_ROCKET_1;
                    break;
                case FRAME_attak2_11:
                    flash_number = Defines.MZ2_SUPERTANK_ROCKET_2;
                    break;
                default:
                    flash_number = Defines.MZ2_SUPERTANK_ROCKET_3;
                    break;
            }

            float[] right = {(float) 0, (float) 0, (float) 0};
            float[] forward = {(float) 0, (float) 0, (float) 0};
            Math3D.AngleVectors(self.s.angles, forward, right, null);
            float[] start = {(float) 0, (float) 0, (float) 0};
            Math3D.G_ProjectSource(self.s.origin,
                    M_Flash.monster_flash_offset[flash_number], forward, right,
                    start);

            float[] vec = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorCopy(self.enemy.s.origin, vec);
            vec[2] = vec[2] + (float) self.enemy.viewheight;
            float[] dir = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorSubtract(vec, start, dir);
            Math3D.VectorNormalize(dir);

            Monster
                    .monster_fire_rocket(self, start, dir, 50, 500,
                            flash_number);
            return true;
        }
    };

    static final EntThinkAdapter supertankMachineGun = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "supertankMachineGun"; }
        @Override
        public boolean think(edict_t self) {
            float[] dir = {(float) 0, (float) 0, (float) 0};

            int flash_number = Defines.MZ2_SUPERTANK_MACHINEGUN_1
                    + (self.s.frame - FRAME_attak1_1);

            
            dir[0] = (float) 0;
            dir[1] = self.s.angles[1];
            dir[2] = (float) 0;

            float[] right = {(float) 0, (float) 0, (float) 0};
            float[] forward = {(float) 0, (float) 0, (float) 0};
            Math3D.AngleVectors(dir, forward, right, null);
            float[] start = {(float) 0, (float) 0, (float) 0};
            Math3D.G_ProjectSource(self.s.origin,
                    M_Flash.monster_flash_offset[flash_number], forward, right,
                    start);

            if (self.enemy != null) {
                float[] vec = {(float) 0, (float) 0, (float) 0};
                Math3D.VectorCopy(self.enemy.s.origin, vec);
                Math3D.VectorMA(vec, (float) 0, self.enemy.velocity, vec);
                vec[2] = vec[2] + (float) self.enemy.viewheight;
                Math3D.VectorSubtract(vec, start, forward);
                Math3D.VectorNormalize(forward);
            }

            Monster.monster_fire_bullet(self, start, forward, 6, 4,
                    Defines.DEFAULT_BULLET_HSPREAD,
                    Defines.DEFAULT_BULLET_VSPREAD, flash_number);
            return true;
        }
    };

    static final EntThinkAdapter supertank_attack = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "supertank_attack"; }
        @Override
        public boolean think(edict_t self) {
            float[] vec = {(float) 0, (float) 0, (float) 0};


            Math3D.VectorSubtract(self.enemy.s.origin, self.s.origin, vec);
            float range = Math3D.VectorLength(vec);

            

            
            

            if (range <= 160.0F) {
                self.monsterinfo.currentmove = supertank_move_attack1;
            } else { 
                if ((double) Lib.random() < 0.3)
                    self.monsterinfo.currentmove = supertank_move_attack1;
                else
                    self.monsterinfo.currentmove = supertank_move_attack2;
            }
            return true;
        }
    };

    static final mframe_t[] supertank_frames_turn_right = {
            new mframe_t(GameAI.ai_move, (float) 0, TreadSound),
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

    static mmove_t supertank_move_turn_right = new mmove_t(FRAME_right_1,
            FRAME_right_18, supertank_frames_turn_right, supertank_run);

    static final mframe_t[] supertank_frames_turn_left = {
            new mframe_t(GameAI.ai_move, (float) 0, TreadSound),
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

    static mmove_t supertank_move_turn_left = new mmove_t(FRAME_left_1,
            FRAME_left_18, supertank_frames_turn_left, supertank_run);

    static final mframe_t[] supertank_frames_pain3 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final mmove_t supertank_move_pain3 = new mmove_t(FRAME_pain3_9,
            FRAME_pain3_12, supertank_frames_pain3, supertank_run);

    static final mframe_t[] supertank_frames_pain2 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final mmove_t supertank_move_pain2 = new mmove_t(FRAME_pain2_5,
            FRAME_pain2_8, supertank_frames_pain2, supertank_run);

    static final mframe_t[] supertank_frames_pain1 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final mmove_t supertank_move_pain1 = new mmove_t(FRAME_pain1_1,
            FRAME_pain1_4, supertank_frames_pain1, supertank_run);

    static final mframe_t[] supertank_frames_death1 = {
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
            new mframe_t(GameAI.ai_move, (float) 0, M_Supertank.BossExplode) };

    static final mmove_t supertank_move_death = new mmove_t(FRAME_death_1,
            FRAME_death_24, supertank_frames_death1, supertank_dead);

    static final mframe_t[] supertank_frames_backward = {
            new mframe_t(GameAI.ai_walk, (float) 0, TreadSound),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null),
            new mframe_t(GameAI.ai_walk, (float) 0, null) };

    static mmove_t supertank_move_backward = new mmove_t(FRAME_backwd_1,
            FRAME_backwd_18, supertank_frames_backward, null);

    static final mframe_t[] supertank_frames_attack4 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static mmove_t supertank_move_attack4 = new mmove_t(FRAME_attak4_1,
            FRAME_attak4_6, supertank_frames_attack4, supertank_run);

    static final mframe_t[] supertank_frames_attack3 = {
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

    static mmove_t supertank_move_attack3 = new mmove_t(FRAME_attak3_1,
            FRAME_attak3_27, supertank_frames_attack3, supertank_run);

    static final mframe_t[] supertank_frames_attack2 = {
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, null),
            new mframe_t(GameAI.ai_charge, (float) 0, supertankRocket),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, supertankRocket),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, supertankRocket),
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

    static final mmove_t supertank_move_attack2 = new mmove_t(FRAME_attak2_1,
            FRAME_attak2_27, supertank_frames_attack2, supertank_run);

    static final EntThinkAdapter supertank_reattack1 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "supertank_reattack1"; }
        @Override
        public boolean think(edict_t self) {
            if (GameUtil.visible(self, self.enemy))
                if ((double) Lib.random() < 0.9)
                    self.monsterinfo.currentmove = supertank_move_attack1;
                else
                    self.monsterinfo.currentmove = supertank_move_end_attack1;
            else
                self.monsterinfo.currentmove = supertank_move_end_attack1;
            return true;
        }
    };

    static final mframe_t[] supertank_frames_attack1 = {
            new mframe_t(GameAI.ai_charge, (float) 0, supertankMachineGun),
            new mframe_t(GameAI.ai_charge, (float) 0, supertankMachineGun),
            new mframe_t(GameAI.ai_charge, (float) 0, supertankMachineGun),
            new mframe_t(GameAI.ai_charge, (float) 0, supertankMachineGun),
            new mframe_t(GameAI.ai_charge, (float) 0, supertankMachineGun),
            new mframe_t(GameAI.ai_charge, (float) 0, supertankMachineGun), };

    static final mmove_t supertank_move_attack1 = new mmove_t(FRAME_attak1_1,
            FRAME_attak1_6, supertank_frames_attack1, supertank_reattack1);

    static final mframe_t[] supertank_frames_end_attack1 = {
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

    static final mmove_t supertank_move_end_attack1 = new mmove_t(FRAME_attak1_7,
            FRAME_attak1_20, supertank_frames_end_attack1, supertank_run);

    static final EntPainAdapter supertank_pain = new EntPainAdapter() {
    	@Override
        public String getID(){ return "supertank_pain"; }
        @Override
        public void pain(edict_t self, edict_t other, float kick, int damage) {
            if (self.health < (self.max_health / 2))
                self.s.skinnum = 1;

            if (GameBase.level.time < self.pain_debounce_time)
                return;

            
            if (damage <= 25)
                if ((double) Lib.random() < 0.2)
                    return;

            
            if (GameBase.skill.value >= 2.0F)
                if ((self.s.frame >= FRAME_attak2_1)
                        && (self.s.frame <= FRAME_attak2_14))
                    return;

            self.pain_debounce_time = GameBase.level.time + 3.0F;

            if (GameBase.skill.value == 3.0F)
                return; 

            if (damage <= 10) {
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain1, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
                self.monsterinfo.currentmove = supertank_move_pain1;
            } else if (damage <= 25) {
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain3, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
                self.monsterinfo.currentmove = supertank_move_pain2;
            } else {
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain2, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
                self.monsterinfo.currentmove = supertank_move_pain3;
            }
        }
    };

    static final EntDieAdapter supertank_die = new EntDieAdapter() {
    	@Override
        public String getID(){ return "supertank_die"; }
        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_death, 1.0F,
                    (float) Defines.ATTN_NORM, (float) 0);
            self.deadflag = Defines.DEAD_DEAD;
            self.takedamage = Defines.DAMAGE_NO;
            self.count = 0;
            self.monsterinfo.currentmove = supertank_move_death;
        }
    };

    
    
    

    /*
     * QUAKED monster_supertank (1 .5 0) (-64 -64 0) (64 64 72) Ambush
     * Trigger_Spawn Sight
     */
    public static final EntThinkAdapter SP_monster_supertank = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "SP_monster_supertank"; }
        @Override
        public boolean think(edict_t self) {
            if (GameBase.deathmatch.value != (float) 0) {
                GameUtil.G_FreeEdict(self);
                return true;
            }

            sound_pain1 = game_import_t.soundindex("bosstank/btkpain1.wav");
            sound_pain2 = game_import_t.soundindex("bosstank/btkpain2.wav");
            sound_pain3 = game_import_t.soundindex("bosstank/btkpain3.wav");
            sound_death = game_import_t.soundindex("bosstank/btkdeth1.wav");
            sound_search1 = game_import_t.soundindex("bosstank/btkunqv1.wav");
            sound_search2 = game_import_t.soundindex("bosstank/btkunqv2.wav");

            
            tread_sound = game_import_t.soundindex("bosstank/btkengn1.wav");

            self.movetype = Defines.MOVETYPE_STEP;
            self.solid = Defines.SOLID_BBOX;
            self.s.modelindex = game_import_t
                    .modelindex("models/monsters/boss1/tris.md2");
            Math3D.VectorSet(self.mins, -64.0F, -64.0F, (float) 0);
            Math3D.VectorSet(self.maxs, 64.0F, 64.0F, 112.0F);

            self.health = 1500;
            self.gib_health = -500;
            self.mass = 800;

            self.pain = supertank_pain;
            self.die = supertank_die;
            self.monsterinfo.stand = supertank_stand;
            self.monsterinfo.walk = supertank_walk;
            self.monsterinfo.run = supertank_run;
            self.monsterinfo.dodge = null;
            self.monsterinfo.attack = supertank_attack;
            self.monsterinfo.search = supertank_search;
            self.monsterinfo.melee = null;
            self.monsterinfo.sight = null;

            game_import_t.linkentity(self);

            self.monsterinfo.currentmove = supertank_move_stand;
            self.monsterinfo.scale = MODEL_SCALE;

            GameAI.walkmonster_start.think(self);
            return true;
        }
    };

    /** Common Boss explode animation. */
    
    public static final EntThinkAdapter BossExplode = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "BossExplode"; }
        @Override
        public boolean think(edict_t self) {

            self.think = BossExplode;
            float[] org = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorCopy(self.s.origin, org);
            org[2] = org[2] + (float) 24 + ((int) Lib.rand() & 15);
            int n;
            switch (self.count++) {
            case 0:
                org[0] -= 24.0F;
                org[1] -= 24.0F;
                break;
            case 1:
                org[0] += 24.0F;
                org[1] += 24.0F;
                break;
            case 2:
                org[0] += 24.0F;
                org[1] -= 24.0F;
                break;
            case 3:
                org[0] -= 24.0F;
                org[1] += 24.0F;
                break;
            case 4:
                org[0] -= 48.0F;
                org[1] -= 48.0F;
                break;
            case 5:
                org[0] += 48.0F;
                org[1] += 48.0F;
                break;
            case 6:
                org[0] -= 48.0F;
                org[1] += 48.0F;
                break;
            case 7:
                org[0] += 48.0F;
                org[1] -= 48.0F;
                break;
            case 8:
                self.s.sound = 0;
                for (n = 0; n < 4; n++)
                    GameMisc.ThrowGib(self, "models/objects/gibs/sm_meat/tris.md2", 500,
                            Defines.GIB_ORGANIC);
                for (n = 0; n < 8; n++)
                    GameMisc.ThrowGib(self, "models/objects/gibs/sm_metal/tris.md2",
                            500, Defines.GIB_METALLIC);
                GameMisc.ThrowGib(self, "models/objects/gibs/chest/tris.md2", 500,
                        Defines.GIB_ORGANIC);
                GameMisc.ThrowHead(self, "models/objects/gibs/gear/tris.md2", 500,
                        Defines.GIB_METALLIC);
                self.deadflag = Defines.DEAD_DEAD;
                return true;
            }
    
            game_import_t.WriteByte(Defines.svc_temp_entity);
            game_import_t.WriteByte(Defines.TE_EXPLOSION1);
            game_import_t.WritePosition(org);
            game_import_t.multicast(self.s.origin, Defines.MULTICAST_PVS);
    
            self.nextthink = GameBase.level.time + 0.1f;
            return true;
        }
    };

}