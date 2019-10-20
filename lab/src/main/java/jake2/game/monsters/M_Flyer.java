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

import java.util.stream.IntStream;

public class M_Flyer {
    

    public static final int ACTION_nothing = 0;

    public static final int ACTION_attack1 = 1;

    public static final int ACTION_attack2 = 2;

    public static final int ACTION_run = 3;

    public static final int ACTION_walk = 4;

    public static final int FRAME_start01 = 0;

    public static final int FRAME_start02 = 1;

    public static final int FRAME_start03 = 2;

    public static final int FRAME_start04 = 3;

    public static final int FRAME_start05 = 4;

    public static final int FRAME_start06 = 5;

    public static final int FRAME_stop01 = 6;

    public static final int FRAME_stop02 = 7;

    public static final int FRAME_stop03 = 8;

    public static final int FRAME_stop04 = 9;

    public static final int FRAME_stop05 = 10;

    public static final int FRAME_stop06 = 11;

    public static final int FRAME_stop07 = 12;

    public static final int FRAME_stand01 = 13;

    public static final int FRAME_stand02 = 14;

    public static final int FRAME_stand03 = 15;

    public static final int FRAME_stand04 = 16;

    public static final int FRAME_stand05 = 17;

    public static final int FRAME_stand06 = 18;

    public static final int FRAME_stand07 = 19;

    public static final int FRAME_stand08 = 20;

    public static final int FRAME_stand09 = 21;

    public static final int FRAME_stand10 = 22;

    public static final int FRAME_stand11 = 23;

    public static final int FRAME_stand12 = 24;

    public static final int FRAME_stand13 = 25;

    public static final int FRAME_stand14 = 26;

    public static final int FRAME_stand15 = 27;

    public static final int FRAME_stand16 = 28;

    public static final int FRAME_stand17 = 29;

    public static final int FRAME_stand18 = 30;

    public static final int FRAME_stand19 = 31;

    public static final int FRAME_stand20 = 32;

    public static final int FRAME_stand21 = 33;

    public static final int FRAME_stand22 = 34;

    public static final int FRAME_stand23 = 35;

    public static final int FRAME_stand24 = 36;

    public static final int FRAME_stand25 = 37;

    public static final int FRAME_stand26 = 38;

    public static final int FRAME_stand27 = 39;

    public static final int FRAME_stand28 = 40;

    public static final int FRAME_stand29 = 41;

    public static final int FRAME_stand30 = 42;

    public static final int FRAME_stand31 = 43;

    public static final int FRAME_stand32 = 44;

    public static final int FRAME_stand33 = 45;

    public static final int FRAME_stand34 = 46;

    public static final int FRAME_stand35 = 47;

    public static final int FRAME_stand36 = 48;

    public static final int FRAME_stand37 = 49;

    public static final int FRAME_stand38 = 50;

    public static final int FRAME_stand39 = 51;

    public static final int FRAME_stand40 = 52;

    public static final int FRAME_stand41 = 53;

    public static final int FRAME_stand42 = 54;

    public static final int FRAME_stand43 = 55;

    public static final int FRAME_stand44 = 56;

    public static final int FRAME_stand45 = 57;

    public static final int FRAME_attak101 = 58;

    public static final int FRAME_attak102 = 59;

    public static final int FRAME_attak103 = 60;

    public static final int FRAME_attak104 = 61;

    public static final int FRAME_attak105 = 62;

    public static final int FRAME_attak106 = 63;

    public static final int FRAME_attak107 = 64;

    public static final int FRAME_attak108 = 65;

    public static final int FRAME_attak109 = 66;

    public static final int FRAME_attak110 = 67;

    public static final int FRAME_attak111 = 68;

    public static final int FRAME_attak112 = 69;

    public static final int FRAME_attak113 = 70;

    public static final int FRAME_attak114 = 71;

    public static final int FRAME_attak115 = 72;

    public static final int FRAME_attak116 = 73;

    public static final int FRAME_attak117 = 74;

    public static final int FRAME_attak118 = 75;

    public static final int FRAME_attak119 = 76;

    public static final int FRAME_attak120 = 77;

    public static final int FRAME_attak121 = 78;

    public static final int FRAME_attak201 = 79;

    public static final int FRAME_attak202 = 80;

    public static final int FRAME_attak203 = 81;

    public static final int FRAME_attak204 = 82;

    public static final int FRAME_attak205 = 83;

    public static final int FRAME_attak206 = 84;

    public static final int FRAME_attak207 = 85;

    public static final int FRAME_attak208 = 86;

    public static final int FRAME_attak209 = 87;

    public static final int FRAME_attak210 = 88;

    public static final int FRAME_attak211 = 89;

    public static final int FRAME_attak212 = 90;

    public static final int FRAME_attak213 = 91;

    public static final int FRAME_attak214 = 92;

    public static final int FRAME_attak215 = 93;

    public static final int FRAME_attak216 = 94;

    public static final int FRAME_attak217 = 95;

    public static final int FRAME_bankl01 = 96;

    public static final int FRAME_bankl02 = 97;

    public static final int FRAME_bankl03 = 98;

    public static final int FRAME_bankl04 = 99;

    public static final int FRAME_bankl05 = 100;

    public static final int FRAME_bankl06 = 101;

    public static final int FRAME_bankl07 = 102;

    public static final int FRAME_bankr01 = 103;

    public static final int FRAME_bankr02 = 104;

    public static final int FRAME_bankr03 = 105;

    public static final int FRAME_bankr04 = 106;

    public static final int FRAME_bankr05 = 107;

    public static final int FRAME_bankr06 = 108;

    public static final int FRAME_bankr07 = 109;

    public static final int FRAME_rollf01 = 110;

    public static final int FRAME_rollf02 = 111;

    public static final int FRAME_rollf03 = 112;

    public static final int FRAME_rollf04 = 113;

    public static final int FRAME_rollf05 = 114;

    public static final int FRAME_rollf06 = 115;

    public static final int FRAME_rollf07 = 116;

    public static final int FRAME_rollf08 = 117;

    public static final int FRAME_rollf09 = 118;

    public static final int FRAME_rollr01 = 119;

    public static final int FRAME_rollr02 = 120;

    public static final int FRAME_rollr03 = 121;

    public static final int FRAME_rollr04 = 122;

    public static final int FRAME_rollr05 = 123;

    public static final int FRAME_rollr06 = 124;

    public static final int FRAME_rollr07 = 125;

    public static final int FRAME_rollr08 = 126;

    public static final int FRAME_rollr09 = 127;

    public static final int FRAME_defens01 = 128;

    public static final int FRAME_defens02 = 129;

    public static final int FRAME_defens03 = 130;

    public static final int FRAME_defens04 = 131;

    public static final int FRAME_defens05 = 132;

    public static final int FRAME_defens06 = 133;

    public static final int FRAME_pain101 = 134;

    public static final int FRAME_pain102 = 135;

    public static final int FRAME_pain103 = 136;

    public static final int FRAME_pain104 = 137;

    public static final int FRAME_pain105 = 138;

    public static final int FRAME_pain106 = 139;

    public static final int FRAME_pain107 = 140;

    public static final int FRAME_pain108 = 141;

    public static final int FRAME_pain109 = 142;

    public static final int FRAME_pain201 = 143;

    public static final int FRAME_pain202 = 144;

    public static final int FRAME_pain203 = 145;

    public static final int FRAME_pain204 = 146;

    public static final int FRAME_pain301 = 147;

    public static final int FRAME_pain302 = 148;

    public static final int FRAME_pain303 = 149;

    public static final int FRAME_pain304 = 150;

    public static final float MODEL_SCALE = 1.000000f;

    static int nextmove; 

    static int sound_sight;

    static int sound_idle;

    static int sound_pain1;

    static int sound_pain2;

    static int sound_slash;

    static int sound_sproing;

    static int sound_die;

    public static final EntInteractAdapter flyer_sight = new EntInteractAdapter() {
    	@Override
        public String getID() { return "flyer_sight"; }
        @Override
        public boolean interact(edict_t self, edict_t other) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_sight, 1,
                    Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static final EntThinkAdapter flyer_idle = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_idle"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_idle, 1,
                    Defines.ATTN_IDLE, 0);
            return true;
        }
    };

    static final EntThinkAdapter flyer_pop_blades = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_pop_blades"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_sproing, 1,
                    Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static final mframe_t[] flyer_frames_stand = {
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null) };

    static final mmove_t flyer_move_stand = new mmove_t(FRAME_stand01, FRAME_stand45,
            flyer_frames_stand, null);

    static final mframe_t[] flyer_frames_walk = {
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 5, null) };

    static final mmove_t flyer_move_walk = new mmove_t(FRAME_stand01, FRAME_stand45,
            flyer_frames_walk, null);

    static final mframe_t[] flyer_frames_run = {
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null) };

    static final mmove_t flyer_move_run = new mmove_t(FRAME_stand01, FRAME_stand45,
            flyer_frames_run, null);

    static final EntThinkAdapter flyer_run = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_run"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0)
                self.monsterinfo.currentmove = flyer_move_stand;
            else
                self.monsterinfo.currentmove = flyer_move_run;
            return true;
        }
    };

    static final EntThinkAdapter flyer_walk = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_walk"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = flyer_move_walk;
            return true;
        }
    };

    static final EntThinkAdapter flyer_stand = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_stand"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = flyer_move_stand;
            return true;
        }
    };

    static final EntThinkAdapter flyer_nextmove = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_nextmove"; }
        @Override
        public boolean think(edict_t self) {
            switch (nextmove) {
                case ACTION_attack1:
                    self.monsterinfo.currentmove = flyer_move_start_melee;
                    break;
                case ACTION_attack2:
                    self.monsterinfo.currentmove = flyer_move_attack2;
                    break;
                case ACTION_run:
                    self.monsterinfo.currentmove = flyer_move_run;
                    break;
            }
            return true;
        }
    };

    static final mframe_t[] flyer_frames_start = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, flyer_nextmove) };

    static final mmove_t flyer_move_start = new mmove_t(FRAME_start01, FRAME_start06,
            flyer_frames_start, null);

    static final mframe_t[] flyer_frames_stop = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, flyer_nextmove) };

    static final mmove_t flyer_move_stop = new mmove_t(FRAME_stop01, FRAME_stop07,
            flyer_frames_stop, null);

    static EntThinkAdapter flyer_stop = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_stop"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = flyer_move_stop;
            return true;
        }
    };

    static EntThinkAdapter flyer_start = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_start"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = flyer_move_start;
            return true;
        }
    };

    static final mframe_t[] flyer_frames_rollright = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t flyer_move_rollright = new mmove_t(FRAME_rollr01,
            FRAME_rollr09, flyer_frames_rollright, null);

    static final mframe_t[] flyer_frames_rollleft = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t flyer_move_rollleft = new mmove_t(FRAME_rollf01,
            FRAME_rollf09, flyer_frames_rollleft, null);

    static final mframe_t[] flyer_frames_pain3 = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t flyer_move_pain3 = new mmove_t(FRAME_pain301, FRAME_pain304,
            flyer_frames_pain3, flyer_run);

    static final mframe_t[] flyer_frames_pain2 = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t flyer_move_pain2 = new mmove_t(FRAME_pain201, FRAME_pain204,
            flyer_frames_pain2, flyer_run);

    static final mframe_t[] flyer_frames_pain1 = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t flyer_move_pain1 = new mmove_t(FRAME_pain101, FRAME_pain109,
            flyer_frames_pain1, flyer_run);

    static final mframe_t[] flyer_frames_defense = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t flyer_move_defense = new mmove_t(FRAME_defens01,
            FRAME_defens06, flyer_frames_defense, null);

    static final mframe_t[] flyer_frames_bankright = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t flyer_move_bankright = new mmove_t(FRAME_bankr01,
            FRAME_bankr07, flyer_frames_bankright, null);

    static final mframe_t[] flyer_frames_bankleft = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t flyer_move_bankleft = new mmove_t(FRAME_bankl01,
            FRAME_bankl07, flyer_frames_bankleft, null);

    static final EntThinkAdapter flyer_fireleft = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_fireleft"; }
        @Override
        public boolean think(edict_t self) {
            flyer_fire(self, Defines.MZ2_FLYER_BLASTER_1);
            return true;
        }
    };

    static final EntThinkAdapter flyer_fireright = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_fireright"; }
        @Override
        public boolean think(edict_t self) {
            flyer_fire(self, Defines.MZ2_FLYER_BLASTER_2);
            return true;
        }
    };

    static final mframe_t[] flyer_frames_attack2 = {
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, -10, flyer_fireleft),
            
            new mframe_t(GameAI.ai_charge, -10, flyer_fireright), 
            new mframe_t(GameAI.ai_charge, -10, flyer_fireleft), 
            new mframe_t(GameAI.ai_charge, -10, flyer_fireright), 
            new mframe_t(GameAI.ai_charge, -10, flyer_fireleft), 
            new mframe_t(GameAI.ai_charge, -10, flyer_fireright), 
            new mframe_t(GameAI.ai_charge, -10, flyer_fireleft), 
            new mframe_t(GameAI.ai_charge, -10, flyer_fireright), 
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null) };

    static final mmove_t flyer_move_attack2 = new mmove_t(FRAME_attak201,
            FRAME_attak217, flyer_frames_attack2, flyer_run);

    static final EntThinkAdapter flyer_slash_left = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_slash_left"; }
        @Override
        public boolean think(edict_t self) {
            float[] aim = { 0, 0, 0 };

            Math3D.VectorSet(aim, Defines.MELEE_DISTANCE, self.mins[0], 0);
            GameWeapon.fire_hit(self, aim, 5, 0);
            game_import_t.sound(self, Defines.CHAN_WEAPON, sound_slash, 1,
                    Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static final EntThinkAdapter flyer_slash_right = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_slash_right"; }
        @Override
        public boolean think(edict_t self) {
            float[] aim = { 0, 0, 0 };

            Math3D.VectorSet(aim, Defines.MELEE_DISTANCE, self.maxs[0], 0);
            GameWeapon.fire_hit(self, aim, 5, 0);
            game_import_t.sound(self, Defines.CHAN_WEAPON, sound_slash, 1,
                    Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static final EntThinkAdapter flyer_loop_melee = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_loop_melee"; }
        @Override
        public boolean think(edict_t self) {
            /*
             * if (random() <= 0.5) self.monsterinfo.currentmove =
             * flyer_move_attack1; else
             */
            self.monsterinfo.currentmove = flyer_move_loop_melee;
            return true;
        }
    };

    static final mframe_t[] flyer_frames_start_melee = {
            new mframe_t(GameAI.ai_charge, 0, flyer_pop_blades),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null) };

    static final mmove_t flyer_move_start_melee = new mmove_t(FRAME_attak101,
            FRAME_attak106, flyer_frames_start_melee, flyer_loop_melee);

    static final mframe_t[] flyer_frames_end_melee = {
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null) };

    static final mmove_t flyer_move_end_melee = new mmove_t(FRAME_attak119,
            FRAME_attak121, flyer_frames_end_melee, flyer_run);

    static final mframe_t[] flyer_frames_loop_melee = {
            new mframe_t(GameAI.ai_charge, 0, null), 
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, flyer_slash_left),
            
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, flyer_slash_right),
            
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null) 

    };

    static final EntThinkAdapter flyer_check_melee = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_check_melee"; }
        @Override
        public boolean think(edict_t self) {
            if (GameUtil.range(self, self.enemy) == Defines.RANGE_MELEE)
                if (Lib.random() <= 0.8)
                    self.monsterinfo.currentmove = flyer_move_loop_melee;
                else
                    self.monsterinfo.currentmove = flyer_move_end_melee;
            else
                self.monsterinfo.currentmove = flyer_move_end_melee;
            return true;
        }
    };

    static final mmove_t flyer_move_loop_melee = new mmove_t(FRAME_attak107,
            FRAME_attak118, flyer_frames_loop_melee, flyer_check_melee);

    static final EntThinkAdapter flyer_attack = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_attack"; }
        @Override
        public boolean think(edict_t self) {
            /*
             * if (random() <= 0.5) self.monsterinfo.currentmove =
             * flyer_move_attack1; else
             */
            self.monsterinfo.currentmove = flyer_move_attack2;

            return true;
        }
    };

    static EntThinkAdapter flyer_setstart = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_setstart"; }
        @Override
        public boolean think(edict_t self) {
            nextmove = ACTION_run;
            self.monsterinfo.currentmove = flyer_move_start;
            return true;
        }
    };

    static final EntThinkAdapter flyer_melee = new EntThinkAdapter() {
    	@Override
        public String getID() { return "flyer_melee"; }
        @Override
        public boolean think(edict_t self) {
            
            
            self.monsterinfo.currentmove = flyer_move_start_melee;
            return true;
        }
    };

    static final EntPainAdapter flyer_pain = new EntPainAdapter() {
    	@Override
        public String getID() { return "flyer_pain"; }
        @Override
        public void pain(edict_t self, edict_t other, float kick, int damage) {

            if (self.health < (self.max_health / 2))
                self.s.skinnum = 1;

            if (GameBase.level.time < self.pain_debounce_time)
                return;

            self.pain_debounce_time = GameBase.level.time + 3;
            if (GameBase.skill.value == 3)
                return;

            int n = Lib.rand() % 3;
            switch (n) {
                case 0:
                    game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain1, 1,
                            Defines.ATTN_NORM, 0);
                    self.monsterinfo.currentmove = flyer_move_pain1;
                    break;
                case 1:
                    game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain2, 1,
                            Defines.ATTN_NORM, 0);
                    self.monsterinfo.currentmove = flyer_move_pain2;
                    break;
                default:
                    game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain1, 1,
                            Defines.ATTN_NORM, 0);
                    self.monsterinfo.currentmove = flyer_move_pain3;
                    break;
            }

        }
    };

    static final EntDieAdapter flyer_die = new EntDieAdapter() {
    	@Override
        public String getID() { return "flyer_die"; }
        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_die, 1,
                    Defines.ATTN_NORM, 0);
            GameMisc.BecomeExplosion1(self);
        }
    };

    static void flyer_fire(edict_t self, int flash_number) {

        int effect;

        boolean b = false;
        for (int i : new int[]{FRAME_attak204, FRAME_attak207, FRAME_attak210}) {
            if ((self.s.frame == i)) {
                b = true;
                break;
            }
        }
        if (b)
            effect = Defines.EF_HYPERBLASTER;
        else
            effect = 0;
        float[] right = {0, 0, 0};
        float[] forward = {0, 0, 0};
        Math3D.AngleVectors(self.s.angles, forward, right, null);
        float[] start = {0, 0, 0};
        Math3D.G_ProjectSource(self.s.origin,
                M_Flash.monster_flash_offset[flash_number], forward, right,
                start);

        float[] end = {0, 0, 0};
        Math3D.VectorCopy(self.enemy.s.origin, end);
        end[2] += self.enemy.viewheight;
        float[] dir = {0, 0, 0};
        Math3D.VectorSubtract(end, start, dir);

        Monster.monster_fire_blaster(self, start, dir, 1, 1000, flash_number,
                effect);
    }

    /*
     * QUAKED monster_flyer (1 .5 0) (-16 -16 -24) (16 16 32) Ambush
     * Trigger_Spawn Sight
     */
    public static void SP_monster_flyer(edict_t self) {
        if (GameBase.deathmatch.value != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        
        if ("jail5".equalsIgnoreCase(GameBase.level.mapname)
                && (self.s.origin[2] == -104)) {
            self.targetname = self.target;
            self.target = null;
        }

        sound_sight = game_import_t.soundindex("flyer/flysght1.wav");
        sound_idle = game_import_t.soundindex("flyer/flysrch1.wav");
        sound_pain1 = game_import_t.soundindex("flyer/flypain1.wav");
        sound_pain2 = game_import_t.soundindex("flyer/flypain2.wav");
        sound_slash = game_import_t.soundindex("flyer/flyatck2.wav");
        sound_sproing = game_import_t.soundindex("flyer/flyatck1.wav");
        sound_die = game_import_t.soundindex("flyer/flydeth1.wav");

        game_import_t.soundindex("flyer/flyatck3.wav");

        self.s.modelindex = game_import_t
                .modelindex("models/monsters/flyer/tris.md2");
        Math3D.VectorSet(self.mins, -16, -16, -24);
        Math3D.VectorSet(self.maxs, 16, 16, 32);
        self.movetype = Defines.MOVETYPE_STEP;
        self.solid = Defines.SOLID_BBOX;

        self.s.sound = game_import_t.soundindex("flyer/flyidle1.wav");

        self.health = 50;
        self.mass = 50;

        self.pain = flyer_pain;
        self.die = flyer_die;

        self.monsterinfo.stand = flyer_stand;
        self.monsterinfo.walk = flyer_walk;
        self.monsterinfo.run = flyer_run;
        self.monsterinfo.attack = flyer_attack;
        self.monsterinfo.melee = flyer_melee;
        self.monsterinfo.sight = flyer_sight;
        self.monsterinfo.idle = flyer_idle;

        game_import_t.linkentity(self);

        self.monsterinfo.currentmove = flyer_move_stand;
        self.monsterinfo.scale = MODEL_SCALE;

        GameAI.flymonster_start.think(self);
    }
}