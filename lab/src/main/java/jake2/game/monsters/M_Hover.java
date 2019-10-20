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

public class M_Hover {

    

    public static final int FRAME_stand01 = 0;

    public static final int FRAME_stand02 = 1;

    public static final int FRAME_stand03 = 2;

    public static final int FRAME_stand04 = 3;

    public static final int FRAME_stand05 = 4;

    public static final int FRAME_stand06 = 5;

    public static final int FRAME_stand07 = 6;

    public static final int FRAME_stand08 = 7;

    public static final int FRAME_stand09 = 8;

    public static final int FRAME_stand10 = 9;

    public static final int FRAME_stand11 = 10;

    public static final int FRAME_stand12 = 11;

    public static final int FRAME_stand13 = 12;

    public static final int FRAME_stand14 = 13;

    public static final int FRAME_stand15 = 14;

    public static final int FRAME_stand16 = 15;

    public static final int FRAME_stand17 = 16;

    public static final int FRAME_stand18 = 17;

    public static final int FRAME_stand19 = 18;

    public static final int FRAME_stand20 = 19;

    public static final int FRAME_stand21 = 20;

    public static final int FRAME_stand22 = 21;

    public static final int FRAME_stand23 = 22;

    public static final int FRAME_stand24 = 23;

    public static final int FRAME_stand25 = 24;

    public static final int FRAME_stand26 = 25;

    public static final int FRAME_stand27 = 26;

    public static final int FRAME_stand28 = 27;

    public static final int FRAME_stand29 = 28;

    public static final int FRAME_stand30 = 29;

    public static final int FRAME_forwrd01 = 30;

    public static final int FRAME_forwrd02 = 31;

    public static final int FRAME_forwrd03 = 32;

    public static final int FRAME_forwrd04 = 33;

    public static final int FRAME_forwrd05 = 34;

    public static final int FRAME_forwrd06 = 35;

    public static final int FRAME_forwrd07 = 36;

    public static final int FRAME_forwrd08 = 37;

    public static final int FRAME_forwrd09 = 38;

    public static final int FRAME_forwrd10 = 39;

    public static final int FRAME_forwrd11 = 40;

    public static final int FRAME_forwrd12 = 41;

    public static final int FRAME_forwrd13 = 42;

    public static final int FRAME_forwrd14 = 43;

    public static final int FRAME_forwrd15 = 44;

    public static final int FRAME_forwrd16 = 45;

    public static final int FRAME_forwrd17 = 46;

    public static final int FRAME_forwrd18 = 47;

    public static final int FRAME_forwrd19 = 48;

    public static final int FRAME_forwrd20 = 49;

    public static final int FRAME_forwrd21 = 50;

    public static final int FRAME_forwrd22 = 51;

    public static final int FRAME_forwrd23 = 52;

    public static final int FRAME_forwrd24 = 53;

    public static final int FRAME_forwrd25 = 54;

    public static final int FRAME_forwrd26 = 55;

    public static final int FRAME_forwrd27 = 56;

    public static final int FRAME_forwrd28 = 57;

    public static final int FRAME_forwrd29 = 58;

    public static final int FRAME_forwrd30 = 59;

    public static final int FRAME_forwrd31 = 60;

    public static final int FRAME_forwrd32 = 61;

    public static final int FRAME_forwrd33 = 62;

    public static final int FRAME_forwrd34 = 63;

    public static final int FRAME_forwrd35 = 64;

    public static final int FRAME_stop101 = 65;

    public static final int FRAME_stop102 = 66;

    public static final int FRAME_stop103 = 67;

    public static final int FRAME_stop104 = 68;

    public static final int FRAME_stop105 = 69;

    public static final int FRAME_stop106 = 70;

    public static final int FRAME_stop107 = 71;

    public static final int FRAME_stop108 = 72;

    public static final int FRAME_stop109 = 73;

    public static final int FRAME_stop201 = 74;

    public static final int FRAME_stop202 = 75;

    public static final int FRAME_stop203 = 76;

    public static final int FRAME_stop204 = 77;

    public static final int FRAME_stop205 = 78;

    public static final int FRAME_stop206 = 79;

    public static final int FRAME_stop207 = 80;

    public static final int FRAME_stop208 = 81;

    public static final int FRAME_takeof01 = 82;

    public static final int FRAME_takeof02 = 83;

    public static final int FRAME_takeof03 = 84;

    public static final int FRAME_takeof04 = 85;

    public static final int FRAME_takeof05 = 86;

    public static final int FRAME_takeof06 = 87;

    public static final int FRAME_takeof07 = 88;

    public static final int FRAME_takeof08 = 89;

    public static final int FRAME_takeof09 = 90;

    public static final int FRAME_takeof10 = 91;

    public static final int FRAME_takeof11 = 92;

    public static final int FRAME_takeof12 = 93;

    public static final int FRAME_takeof13 = 94;

    public static final int FRAME_takeof14 = 95;

    public static final int FRAME_takeof15 = 96;

    public static final int FRAME_takeof16 = 97;

    public static final int FRAME_takeof17 = 98;

    public static final int FRAME_takeof18 = 99;

    public static final int FRAME_takeof19 = 100;

    public static final int FRAME_takeof20 = 101;

    public static final int FRAME_takeof21 = 102;

    public static final int FRAME_takeof22 = 103;

    public static final int FRAME_takeof23 = 104;

    public static final int FRAME_takeof24 = 105;

    public static final int FRAME_takeof25 = 106;

    public static final int FRAME_takeof26 = 107;

    public static final int FRAME_takeof27 = 108;

    public static final int FRAME_takeof28 = 109;

    public static final int FRAME_takeof29 = 110;

    public static final int FRAME_takeof30 = 111;

    public static final int FRAME_land01 = 112;

    public static final int FRAME_pain101 = 113;

    public static final int FRAME_pain102 = 114;

    public static final int FRAME_pain103 = 115;

    public static final int FRAME_pain104 = 116;

    public static final int FRAME_pain105 = 117;

    public static final int FRAME_pain106 = 118;

    public static final int FRAME_pain107 = 119;

    public static final int FRAME_pain108 = 120;

    public static final int FRAME_pain109 = 121;

    public static final int FRAME_pain110 = 122;

    public static final int FRAME_pain111 = 123;

    public static final int FRAME_pain112 = 124;

    public static final int FRAME_pain113 = 125;

    public static final int FRAME_pain114 = 126;

    public static final int FRAME_pain115 = 127;

    public static final int FRAME_pain116 = 128;

    public static final int FRAME_pain117 = 129;

    public static final int FRAME_pain118 = 130;

    public static final int FRAME_pain119 = 131;

    public static final int FRAME_pain120 = 132;

    public static final int FRAME_pain121 = 133;

    public static final int FRAME_pain122 = 134;

    public static final int FRAME_pain123 = 135;

    public static final int FRAME_pain124 = 136;

    public static final int FRAME_pain125 = 137;

    public static final int FRAME_pain126 = 138;

    public static final int FRAME_pain127 = 139;

    public static final int FRAME_pain128 = 140;

    public static final int FRAME_pain201 = 141;

    public static final int FRAME_pain202 = 142;

    public static final int FRAME_pain203 = 143;

    public static final int FRAME_pain204 = 144;

    public static final int FRAME_pain205 = 145;

    public static final int FRAME_pain206 = 146;

    public static final int FRAME_pain207 = 147;

    public static final int FRAME_pain208 = 148;

    public static final int FRAME_pain209 = 149;

    public static final int FRAME_pain210 = 150;

    public static final int FRAME_pain211 = 151;

    public static final int FRAME_pain212 = 152;

    public static final int FRAME_pain301 = 153;

    public static final int FRAME_pain302 = 154;

    public static final int FRAME_pain303 = 155;

    public static final int FRAME_pain304 = 156;

    public static final int FRAME_pain305 = 157;

    public static final int FRAME_pain306 = 158;

    public static final int FRAME_pain307 = 159;

    public static final int FRAME_pain308 = 160;

    public static final int FRAME_pain309 = 161;

    public static final int FRAME_death101 = 162;

    public static final int FRAME_death102 = 163;

    public static final int FRAME_death103 = 164;

    public static final int FRAME_death104 = 165;

    public static final int FRAME_death105 = 166;

    public static final int FRAME_death106 = 167;

    public static final int FRAME_death107 = 168;

    public static final int FRAME_death108 = 169;

    public static final int FRAME_death109 = 170;

    public static final int FRAME_death110 = 171;

    public static final int FRAME_death111 = 172;

    public static final int FRAME_backwd01 = 173;

    public static final int FRAME_backwd02 = 174;

    public static final int FRAME_backwd03 = 175;

    public static final int FRAME_backwd04 = 176;

    public static final int FRAME_backwd05 = 177;

    public static final int FRAME_backwd06 = 178;

    public static final int FRAME_backwd07 = 179;

    public static final int FRAME_backwd08 = 180;

    public static final int FRAME_backwd09 = 181;

    public static final int FRAME_backwd10 = 182;

    public static final int FRAME_backwd11 = 183;

    public static final int FRAME_backwd12 = 184;

    public static final int FRAME_backwd13 = 185;

    public static final int FRAME_backwd14 = 186;

    public static final int FRAME_backwd15 = 187;

    public static final int FRAME_backwd16 = 188;

    public static final int FRAME_backwd17 = 189;

    public static final int FRAME_backwd18 = 190;

    public static final int FRAME_backwd19 = 191;

    public static final int FRAME_backwd20 = 192;

    public static final int FRAME_backwd21 = 193;

    public static final int FRAME_backwd22 = 194;

    public static final int FRAME_backwd23 = 195;

    public static final int FRAME_backwd24 = 196;

    public static final int FRAME_attak101 = 197;

    public static final int FRAME_attak102 = 198;

    public static final int FRAME_attak103 = 199;

    public static final int FRAME_attak104 = 200;

    public static final int FRAME_attak105 = 201;

    public static final int FRAME_attak106 = 202;

    public static final int FRAME_attak107 = 203;

    public static final int FRAME_attak108 = 204;

    public static final float MODEL_SCALE = 1.000000f;

    static int sound_pain1;

    static int sound_pain2;

    static int sound_death1;

    static int sound_death2;

    static int sound_sight;

    static int sound_search1;

    static int sound_search2;

    static final EntThinkAdapter hover_reattack = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_reattack"; }
        @Override
        public boolean think(edict_t self) {
            if (self.enemy.health > 0)
                if (GameUtil.visible(self, self.enemy))
                    if ((double) Lib.random() <= 0.6) {
                        self.monsterinfo.currentmove = hover_move_attack1;
                        return true;
                    }
            self.monsterinfo.currentmove = hover_move_end_attack;
            return true;
        }
    };

    static final EntThinkAdapter hover_fire_blaster = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_fire_blaster"; }
        @Override
        public boolean think(edict_t self) {
            int effect;

            if (self.s.frame == FRAME_attak104)
                effect = Defines.EF_HYPERBLASTER;
            else
                effect = 0;

            float[] right = {(float) 0, (float) 0, (float) 0};
            float[] forward = {(float) 0, (float) 0, (float) 0};
            Math3D.AngleVectors(self.s.angles, forward, right, null);
            float[] start = {(float) 0, (float) 0, (float) 0};
            Math3D.G_ProjectSource(self.s.origin,
                    M_Flash.monster_flash_offset[Defines.MZ2_HOVER_BLASTER_1],
                    forward, right, start);

            float[] end = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorCopy(self.enemy.s.origin, end);
            end[2] = end[2] + (float) self.enemy.viewheight;
            float[] dir = {(float) 0, (float) 0, (float) 0};
            Math3D.VectorSubtract(end, start, dir);

            Monster.monster_fire_blaster(self, start, dir, 1, 1000,
                    Defines.MZ2_HOVER_BLASTER_1, effect);
            return true;
        }
    };

    static final EntThinkAdapter hover_stand = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_stand"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = hover_move_stand;
            return true;
        }
    };

    static final EntThinkAdapter hover_run = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_run"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0)
                self.monsterinfo.currentmove = hover_move_stand;
            else
                self.monsterinfo.currentmove = hover_move_run;
            return true;
        }
    };

    static final EntThinkAdapter hover_walk = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_walk"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = hover_move_walk;
            return true;
        }
    };

    static final EntThinkAdapter hover_start_attack = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_start_attack"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = hover_move_start_attack;
            return true;
        }
    };

    static final EntThinkAdapter hover_attack = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_attack"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = hover_move_attack1;
            return true;
        }
    };

    static final EntPainAdapter hover_pain = new EntPainAdapter() {
    	@Override
        public String getID() { return "hover_pain"; }
        @Override
        public void pain(edict_t self, edict_t other, float kick, int damage) {
            if (self.health < (self.max_health / 2))
                self.s.skinnum = 1;

            if (GameBase.level.time < self.pain_debounce_time)
                return;

            self.pain_debounce_time = GameBase.level.time + 3.0F;

            if (GameBase.skill.value == 3.0F)
                return; 

            if (damage <= 25) {
                if ((double) Lib.random() < 0.5) {
                    game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain1, 1.0F,
                            (float) Defines.ATTN_NORM, (float) 0);
                    self.monsterinfo.currentmove = hover_move_pain3;
                } else {
                    game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain2, 1.0F,
                            (float) Defines.ATTN_NORM, (float) 0);
                    self.monsterinfo.currentmove = hover_move_pain2;
                }
            } else {
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain1, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
                self.monsterinfo.currentmove = hover_move_pain1;
            }
        }
    };

    static final EntThinkAdapter hover_deadthink = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_deadthink"; }
        @Override
        public boolean think(edict_t self) {
            if (null == self.groundentity
                    && GameBase.level.time < self.timestamp) {
                self.nextthink = GameBase.level.time + Defines.FRAMETIME;
                return true;
            }
            GameMisc.BecomeExplosion1(self);
            return true;
        }
    };

    static final EntThinkAdapter hover_dead = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_dead"; }
        @Override
        public boolean think(edict_t self) {
            Math3D.VectorSet(self.mins, -16.0F, -16.0F, -24.0F);
            Math3D.VectorSet(self.maxs, 16.0F, 16.0F, -8.0F);
            self.movetype = Defines.MOVETYPE_TOSS;
            self.think = hover_deadthink;
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;
            self.timestamp = GameBase.level.time + 15.0F;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static final EntDieAdapter hover_die = new EntDieAdapter() {
    	@Override
        public String getID() { return "hover_die"; }
        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {


            if (self.health <= self.gib_health) {
                game_import_t
                        .sound(self, Defines.CHAN_VOICE, game_import_t
                                .soundindex("misc/udeath.wav"), 1.0F,
                                (float) Defines.ATTN_NORM, (float) 0);
                int n;
                for (n = 0; n < 2; n++)
                    GameMisc.ThrowGib(self, "models/objects/gibs/bone/tris.md2",
                            damage, Defines.GIB_ORGANIC);
                for (n = 0; n < 2; n++)
                    GameMisc.ThrowGib(self,
                            "models/objects/gibs/sm_meat/tris.md2", damage,
                            Defines.GIB_ORGANIC);
                GameMisc.ThrowHead(self, "models/objects/gibs/sm_meat/tris.md2",
                        damage, Defines.GIB_ORGANIC);
                self.deadflag = Defines.DEAD_DEAD;
                return;
            }

            if (self.deadflag == Defines.DEAD_DEAD)
                return;

            
            if ((double) Lib.random() < 0.5)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_death1, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
            else
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_death2, 1.0F,
                        (float) Defines.ATTN_NORM, (float) 0);
            self.deadflag = Defines.DEAD_DEAD;
            self.takedamage = Defines.DAMAGE_YES;
            self.monsterinfo.currentmove = hover_move_death1;
        }
    };

    static final EntInteractAdapter hover_sight = new EntInteractAdapter() {
    	@Override
        public String getID() { return "hover_sight"; }
        @Override
        public boolean interact(edict_t self, edict_t other) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_sight, 1.0F,
                    (float) Defines.ATTN_NORM, (float) 0);
            return true;
        }
    };

    static final EntThinkAdapter hover_search = new EntThinkAdapter() {
    	@Override
        public String getID() { return "hover_search"; }
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

    static final mframe_t[] hover_frames_stand = {
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

    static final mmove_t hover_move_stand = new mmove_t(FRAME_stand01, FRAME_stand30,
            hover_frames_stand, null);

    static final mframe_t[] hover_frames_stop1 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static mmove_t hover_move_stop1 = new mmove_t(FRAME_stop101, FRAME_stop109,
            hover_frames_stop1, null);

    static final mframe_t[] hover_frames_stop2 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static mmove_t hover_move_stop2 = new mmove_t(FRAME_stop201, FRAME_stop208,
            hover_frames_stop2, null);

    static final mframe_t[] hover_frames_takeoff = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, -2.0F, null),
            new mframe_t(GameAI.ai_move, 5.0F, null),
            new mframe_t(GameAI.ai_move, -1.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, -1.0F, null),
            new mframe_t(GameAI.ai_move, -1.0F, null),
            new mframe_t(GameAI.ai_move, -1.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, -6.0F, null),
            new mframe_t(GameAI.ai_move, -9.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static mmove_t hover_move_takeoff = new mmove_t(FRAME_takeof01,
            FRAME_takeof30, hover_frames_takeoff, null);

    static final mframe_t[] hover_frames_pain3 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static final mmove_t hover_move_pain3 = new mmove_t(FRAME_pain301, FRAME_pain309,
            hover_frames_pain3, hover_run);

    static final mframe_t[] hover_frames_pain2 = {
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

    static final mmove_t hover_move_pain2 = new mmove_t(FRAME_pain201, FRAME_pain212,
            hover_frames_pain2, hover_run);

    static final mframe_t[] hover_frames_pain1 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, -8.0F, null),
            new mframe_t(GameAI.ai_move, -4.0F, null),
            new mframe_t(GameAI.ai_move, -6.0F, null),
            new mframe_t(GameAI.ai_move, -4.0F, null),
            new mframe_t(GameAI.ai_move, -3.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, 7.0F, null),
            new mframe_t(GameAI.ai_move, 1.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 2.0F, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, 5.0F, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 4.0F, null) };

    static final mmove_t hover_move_pain1 = new mmove_t(FRAME_pain101, FRAME_pain128,
            hover_frames_pain1, hover_run);

    static final mframe_t[] hover_frames_land = { new mframe_t(
            GameAI.ai_move, (float) 0, null) };

    static mmove_t hover_move_land = new mmove_t(FRAME_land01, FRAME_land01,
            hover_frames_land, null);

    static final mframe_t[] hover_frames_forward = {
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
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null) };

    static mmove_t hover_move_forward = new mmove_t(FRAME_forwrd01,
            FRAME_forwrd35, hover_frames_forward, null);

    static final mframe_t[] hover_frames_walk = {
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
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null),
            new mframe_t(GameAI.ai_walk, 4.0F, null) };

    static final mmove_t hover_move_walk = new mmove_t(FRAME_forwrd01,
            FRAME_forwrd35, hover_frames_walk, null);

    static final mframe_t[] hover_frames_run = {
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null),
            new mframe_t(GameAI.ai_run, 10.0F, null) };

    static final mmove_t hover_move_run = new mmove_t(FRAME_forwrd01, FRAME_forwrd35,
            hover_frames_run, null);

    static final mframe_t[] hover_frames_death1 = {
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, (float) 0, null),
            new mframe_t(GameAI.ai_move, -10.0F, null),
            new mframe_t(GameAI.ai_move, 3.0F, null),
            new mframe_t(GameAI.ai_move, 5.0F, null),
            new mframe_t(GameAI.ai_move, 4.0F, null),
            new mframe_t(GameAI.ai_move, 7.0F, null) };

    static final mmove_t hover_move_death1 = new mmove_t(FRAME_death101,
            FRAME_death111, hover_frames_death1, hover_dead);

    static final mframe_t[] hover_frames_backward = {
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

    static mmove_t hover_move_backward = new mmove_t(FRAME_backwd01,
            FRAME_backwd24, hover_frames_backward, null);

    static final mframe_t[] hover_frames_start_attack = {
            new mframe_t(GameAI.ai_charge, 1.0F, null),
            new mframe_t(GameAI.ai_charge, 1.0F, null),
            new mframe_t(GameAI.ai_charge, 1.0F, null) };

    static final mmove_t hover_move_start_attack = new mmove_t(FRAME_attak101,
            FRAME_attak103, hover_frames_start_attack, hover_attack);

    static final mframe_t[] hover_frames_attack1 = {
            new mframe_t(GameAI.ai_charge, -10.0F, hover_fire_blaster),
            new mframe_t(GameAI.ai_charge, -10.0F, hover_fire_blaster),
            new mframe_t(GameAI.ai_charge, (float) 0, hover_reattack), };

    static final mmove_t hover_move_attack1 = new mmove_t(FRAME_attak104,
            FRAME_attak106, hover_frames_attack1, null);

    static final mframe_t[] hover_frames_end_attack = {
            new mframe_t(GameAI.ai_charge, 1.0F, null),
            new mframe_t(GameAI.ai_charge, 1.0F, null) };

    static final mmove_t hover_move_end_attack = new mmove_t(FRAME_attak107,
            FRAME_attak108, hover_frames_end_attack, hover_run);

    /*
     * QUAKED monster_hover (1 .5 0) (-16 -16 -24) (16 16 32) Ambush
     * Trigger_Spawn Sight
     */
    public static void SP_monster_hover(edict_t self) {
        if (GameBase.deathmatch.value != (float) 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        sound_pain1 = game_import_t.soundindex("hover/hovpain1.wav");
        sound_pain2 = game_import_t.soundindex("hover/hovpain2.wav");
        sound_death1 = game_import_t.soundindex("hover/hovdeth1.wav");
        sound_death2 = game_import_t.soundindex("hover/hovdeth2.wav");
        sound_sight = game_import_t.soundindex("hover/hovsght1.wav");
        sound_search1 = game_import_t.soundindex("hover/hovsrch1.wav");
        sound_search2 = game_import_t.soundindex("hover/hovsrch2.wav");

        game_import_t.soundindex("hover/hovatck1.wav");

        self.s.sound = game_import_t.soundindex("hover/hovidle1.wav");

        self.movetype = Defines.MOVETYPE_STEP;
        self.solid = Defines.SOLID_BBOX;
        self.s.modelindex = game_import_t
                .modelindex("models/monsters/hover/tris.md2");
        Math3D.VectorSet(self.mins, -24.0F, -24.0F, -24.0F);
        Math3D.VectorSet(self.maxs, 24.0F, 24.0F, 32.0F);

        self.health = 240;
        self.gib_health = -100;
        self.mass = 150;

        self.pain = hover_pain;
        self.die = hover_die;

        self.monsterinfo.stand = hover_stand;
        self.monsterinfo.walk = hover_walk;
        self.monsterinfo.run = hover_run;
        
        self.monsterinfo.attack = hover_start_attack;
        self.monsterinfo.sight = hover_sight;
        self.monsterinfo.search = hover_search;

        game_import_t.linkentity(self);

        self.monsterinfo.currentmove = hover_move_stand;
        self.monsterinfo.scale = MODEL_SCALE;

        GameAI.flymonster_start.think(self);
    }
}