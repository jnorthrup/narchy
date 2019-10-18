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
import jake2.qcommon.Com;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.util.Arrays;
import java.util.stream.Stream;

public class M_Soldier {

    

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

    public static final int FRAME_attak201 = 12;

    public static final int FRAME_attak202 = 13;

    public static final int FRAME_attak203 = 14;

    public static final int FRAME_attak204 = 15;

    public static final int FRAME_attak205 = 16;

    public static final int FRAME_attak206 = 17;

    public static final int FRAME_attak207 = 18;

    public static final int FRAME_attak208 = 19;

    public static final int FRAME_attak209 = 20;

    public static final int FRAME_attak210 = 21;

    public static final int FRAME_attak211 = 22;

    public static final int FRAME_attak212 = 23;

    public static final int FRAME_attak213 = 24;

    public static final int FRAME_attak214 = 25;

    public static final int FRAME_attak215 = 26;

    public static final int FRAME_attak216 = 27;

    public static final int FRAME_attak217 = 28;

    public static final int FRAME_attak218 = 29;

    public static final int FRAME_attak301 = 30;

    public static final int FRAME_attak302 = 31;

    public static final int FRAME_attak303 = 32;

    public static final int FRAME_attak304 = 33;

    public static final int FRAME_attak305 = 34;

    public static final int FRAME_attak306 = 35;

    public static final int FRAME_attak307 = 36;

    public static final int FRAME_attak308 = 37;

    public static final int FRAME_attak309 = 38;

    public static final int FRAME_attak401 = 39;

    public static final int FRAME_attak402 = 40;

    public static final int FRAME_attak403 = 41;

    public static final int FRAME_attak404 = 42;

    public static final int FRAME_attak405 = 43;

    public static final int FRAME_attak406 = 44;

    public static final int FRAME_duck01 = 45;

    public static final int FRAME_duck02 = 46;

    public static final int FRAME_duck03 = 47;

    public static final int FRAME_duck04 = 48;

    public static final int FRAME_duck05 = 49;

    public static final int FRAME_pain101 = 50;

    public static final int FRAME_pain102 = 51;

    public static final int FRAME_pain103 = 52;

    public static final int FRAME_pain104 = 53;

    public static final int FRAME_pain105 = 54;

    public static final int FRAME_pain201 = 55;

    public static final int FRAME_pain202 = 56;

    public static final int FRAME_pain203 = 57;

    public static final int FRAME_pain204 = 58;

    public static final int FRAME_pain205 = 59;

    public static final int FRAME_pain206 = 60;

    public static final int FRAME_pain207 = 61;

    public static final int FRAME_pain301 = 62;

    public static final int FRAME_pain302 = 63;

    public static final int FRAME_pain303 = 64;

    public static final int FRAME_pain304 = 65;

    public static final int FRAME_pain305 = 66;

    public static final int FRAME_pain306 = 67;

    public static final int FRAME_pain307 = 68;

    public static final int FRAME_pain308 = 69;

    public static final int FRAME_pain309 = 70;

    public static final int FRAME_pain310 = 71;

    public static final int FRAME_pain311 = 72;

    public static final int FRAME_pain312 = 73;

    public static final int FRAME_pain313 = 74;

    public static final int FRAME_pain314 = 75;

    public static final int FRAME_pain315 = 76;

    public static final int FRAME_pain316 = 77;

    public static final int FRAME_pain317 = 78;

    public static final int FRAME_pain318 = 79;

    public static final int FRAME_pain401 = 80;

    public static final int FRAME_pain402 = 81;

    public static final int FRAME_pain403 = 82;

    public static final int FRAME_pain404 = 83;

    public static final int FRAME_pain405 = 84;

    public static final int FRAME_pain406 = 85;

    public static final int FRAME_pain407 = 86;

    public static final int FRAME_pain408 = 87;

    public static final int FRAME_pain409 = 88;

    public static final int FRAME_pain410 = 89;

    public static final int FRAME_pain411 = 90;

    public static final int FRAME_pain412 = 91;

    public static final int FRAME_pain413 = 92;

    public static final int FRAME_pain414 = 93;

    public static final int FRAME_pain415 = 94;

    public static final int FRAME_pain416 = 95;

    public static final int FRAME_pain417 = 96;

    public static final int FRAME_run01 = 97;

    public static final int FRAME_run02 = 98;

    public static final int FRAME_run03 = 99;

    public static final int FRAME_run04 = 100;

    public static final int FRAME_run05 = 101;

    public static final int FRAME_run06 = 102;

    public static final int FRAME_run07 = 103;

    public static final int FRAME_run08 = 104;

    public static final int FRAME_run09 = 105;

    public static final int FRAME_run10 = 106;

    public static final int FRAME_run11 = 107;

    public static final int FRAME_run12 = 108;

    public static final int FRAME_runs01 = 109;

    public static final int FRAME_runs02 = 110;

    public static final int FRAME_runs03 = 111;

    public static final int FRAME_runs04 = 112;

    public static final int FRAME_runs05 = 113;

    public static final int FRAME_runs06 = 114;

    public static final int FRAME_runs07 = 115;

    public static final int FRAME_runs08 = 116;

    public static final int FRAME_runs09 = 117;

    public static final int FRAME_runs10 = 118;

    public static final int FRAME_runs11 = 119;

    public static final int FRAME_runs12 = 120;

    public static final int FRAME_runs13 = 121;

    public static final int FRAME_runs14 = 122;

    public static final int FRAME_runs15 = 123;

    public static final int FRAME_runs16 = 124;

    public static final int FRAME_runs17 = 125;

    public static final int FRAME_runs18 = 126;

    public static final int FRAME_runt01 = 127;

    public static final int FRAME_runt02 = 128;

    public static final int FRAME_runt03 = 129;

    public static final int FRAME_runt04 = 130;

    public static final int FRAME_runt05 = 131;

    public static final int FRAME_runt06 = 132;

    public static final int FRAME_runt07 = 133;

    public static final int FRAME_runt08 = 134;

    public static final int FRAME_runt09 = 135;

    public static final int FRAME_runt10 = 136;

    public static final int FRAME_runt11 = 137;

    public static final int FRAME_runt12 = 138;

    public static final int FRAME_runt13 = 139;

    public static final int FRAME_runt14 = 140;

    public static final int FRAME_runt15 = 141;

    public static final int FRAME_runt16 = 142;

    public static final int FRAME_runt17 = 143;

    public static final int FRAME_runt18 = 144;

    public static final int FRAME_runt19 = 145;

    public static final int FRAME_stand101 = 146;

    public static final int FRAME_stand102 = 147;

    public static final int FRAME_stand103 = 148;

    public static final int FRAME_stand104 = 149;

    public static final int FRAME_stand105 = 150;

    public static final int FRAME_stand106 = 151;

    public static final int FRAME_stand107 = 152;

    public static final int FRAME_stand108 = 153;

    public static final int FRAME_stand109 = 154;

    public static final int FRAME_stand110 = 155;

    public static final int FRAME_stand111 = 156;

    public static final int FRAME_stand112 = 157;

    public static final int FRAME_stand113 = 158;

    public static final int FRAME_stand114 = 159;

    public static final int FRAME_stand115 = 160;

    public static final int FRAME_stand116 = 161;

    public static final int FRAME_stand117 = 162;

    public static final int FRAME_stand118 = 163;

    public static final int FRAME_stand119 = 164;

    public static final int FRAME_stand120 = 165;

    public static final int FRAME_stand121 = 166;

    public static final int FRAME_stand122 = 167;

    public static final int FRAME_stand123 = 168;

    public static final int FRAME_stand124 = 169;

    public static final int FRAME_stand125 = 170;

    public static final int FRAME_stand126 = 171;

    public static final int FRAME_stand127 = 172;

    public static final int FRAME_stand128 = 173;

    public static final int FRAME_stand129 = 174;

    public static final int FRAME_stand130 = 175;

    public static final int FRAME_stand301 = 176;

    public static final int FRAME_stand302 = 177;

    public static final int FRAME_stand303 = 178;

    public static final int FRAME_stand304 = 179;

    public static final int FRAME_stand305 = 180;

    public static final int FRAME_stand306 = 181;

    public static final int FRAME_stand307 = 182;

    public static final int FRAME_stand308 = 183;

    public static final int FRAME_stand309 = 184;

    public static final int FRAME_stand310 = 185;

    public static final int FRAME_stand311 = 186;

    public static final int FRAME_stand312 = 187;

    public static final int FRAME_stand313 = 188;

    public static final int FRAME_stand314 = 189;

    public static final int FRAME_stand315 = 190;

    public static final int FRAME_stand316 = 191;

    public static final int FRAME_stand317 = 192;

    public static final int FRAME_stand318 = 193;

    public static final int FRAME_stand319 = 194;

    public static final int FRAME_stand320 = 195;

    public static final int FRAME_stand321 = 196;

    public static final int FRAME_stand322 = 197;

    public static final int FRAME_stand323 = 198;

    public static final int FRAME_stand324 = 199;

    public static final int FRAME_stand325 = 200;

    public static final int FRAME_stand326 = 201;

    public static final int FRAME_stand327 = 202;

    public static final int FRAME_stand328 = 203;

    public static final int FRAME_stand329 = 204;

    public static final int FRAME_stand330 = 205;

    public static final int FRAME_stand331 = 206;

    public static final int FRAME_stand332 = 207;

    public static final int FRAME_stand333 = 208;

    public static final int FRAME_stand334 = 209;

    public static final int FRAME_stand335 = 210;

    public static final int FRAME_stand336 = 211;

    public static final int FRAME_stand337 = 212;

    public static final int FRAME_stand338 = 213;

    public static final int FRAME_stand339 = 214;

    public static final int FRAME_walk101 = 215;

    public static final int FRAME_walk102 = 216;

    public static final int FRAME_walk103 = 217;

    public static final int FRAME_walk104 = 218;

    public static final int FRAME_walk105 = 219;

    public static final int FRAME_walk106 = 220;

    public static final int FRAME_walk107 = 221;

    public static final int FRAME_walk108 = 222;

    public static final int FRAME_walk109 = 223;

    public static final int FRAME_walk110 = 224;

    public static final int FRAME_walk111 = 225;

    public static final int FRAME_walk112 = 226;

    public static final int FRAME_walk113 = 227;

    public static final int FRAME_walk114 = 228;

    public static final int FRAME_walk115 = 229;

    public static final int FRAME_walk116 = 230;

    public static final int FRAME_walk117 = 231;

    public static final int FRAME_walk118 = 232;

    public static final int FRAME_walk119 = 233;

    public static final int FRAME_walk120 = 234;

    public static final int FRAME_walk121 = 235;

    public static final int FRAME_walk122 = 236;

    public static final int FRAME_walk123 = 237;

    public static final int FRAME_walk124 = 238;

    public static final int FRAME_walk125 = 239;

    public static final int FRAME_walk126 = 240;

    public static final int FRAME_walk127 = 241;

    public static final int FRAME_walk128 = 242;

    public static final int FRAME_walk129 = 243;

    public static final int FRAME_walk130 = 244;

    public static final int FRAME_walk131 = 245;

    public static final int FRAME_walk132 = 246;

    public static final int FRAME_walk133 = 247;

    public static final int FRAME_walk201 = 248;

    public static final int FRAME_walk202 = 249;

    public static final int FRAME_walk203 = 250;

    public static final int FRAME_walk204 = 251;

    public static final int FRAME_walk205 = 252;

    public static final int FRAME_walk206 = 253;

    public static final int FRAME_walk207 = 254;

    public static final int FRAME_walk208 = 255;

    public static final int FRAME_walk209 = 256;

    public static final int FRAME_walk210 = 257;

    public static final int FRAME_walk211 = 258;

    public static final int FRAME_walk212 = 259;

    public static final int FRAME_walk213 = 260;

    public static final int FRAME_walk214 = 261;

    public static final int FRAME_walk215 = 262;

    public static final int FRAME_walk216 = 263;

    public static final int FRAME_walk217 = 264;

    public static final int FRAME_walk218 = 265;

    public static final int FRAME_walk219 = 266;

    public static final int FRAME_walk220 = 267;

    public static final int FRAME_walk221 = 268;

    public static final int FRAME_walk222 = 269;

    public static final int FRAME_walk223 = 270;

    public static final int FRAME_walk224 = 271;

    public static final int FRAME_death101 = 272;

    public static final int FRAME_death102 = 273;

    public static final int FRAME_death103 = 274;

    public static final int FRAME_death104 = 275;

    public static final int FRAME_death105 = 276;

    public static final int FRAME_death106 = 277;

    public static final int FRAME_death107 = 278;

    public static final int FRAME_death108 = 279;

    public static final int FRAME_death109 = 280;

    public static final int FRAME_death110 = 281;

    public static final int FRAME_death111 = 282;

    public static final int FRAME_death112 = 283;

    public static final int FRAME_death113 = 284;

    public static final int FRAME_death114 = 285;

    public static final int FRAME_death115 = 286;

    public static final int FRAME_death116 = 287;

    public static final int FRAME_death117 = 288;

    public static final int FRAME_death118 = 289;

    public static final int FRAME_death119 = 290;

    public static final int FRAME_death120 = 291;

    public static final int FRAME_death121 = 292;

    public static final int FRAME_death122 = 293;

    public static final int FRAME_death123 = 294;

    public static final int FRAME_death124 = 295;

    public static final int FRAME_death125 = 296;

    public static final int FRAME_death126 = 297;

    public static final int FRAME_death127 = 298;

    public static final int FRAME_death128 = 299;

    public static final int FRAME_death129 = 300;

    public static final int FRAME_death130 = 301;

    public static final int FRAME_death131 = 302;

    public static final int FRAME_death132 = 303;

    public static final int FRAME_death133 = 304;

    public static final int FRAME_death134 = 305;

    public static final int FRAME_death135 = 306;

    public static final int FRAME_death136 = 307;

    public static final int FRAME_death201 = 308;

    public static final int FRAME_death202 = 309;

    public static final int FRAME_death203 = 310;

    public static final int FRAME_death204 = 311;

    public static final int FRAME_death205 = 312;

    public static final int FRAME_death206 = 313;

    public static final int FRAME_death207 = 314;

    public static final int FRAME_death208 = 315;

    public static final int FRAME_death209 = 316;

    public static final int FRAME_death210 = 317;

    public static final int FRAME_death211 = 318;

    public static final int FRAME_death212 = 319;

    public static final int FRAME_death213 = 320;

    public static final int FRAME_death214 = 321;

    public static final int FRAME_death215 = 322;

    public static final int FRAME_death216 = 323;

    public static final int FRAME_death217 = 324;

    public static final int FRAME_death218 = 325;

    public static final int FRAME_death219 = 326;

    public static final int FRAME_death220 = 327;

    public static final int FRAME_death221 = 328;

    public static final int FRAME_death222 = 329;

    public static final int FRAME_death223 = 330;

    public static final int FRAME_death224 = 331;

    public static final int FRAME_death225 = 332;

    public static final int FRAME_death226 = 333;

    public static final int FRAME_death227 = 334;

    public static final int FRAME_death228 = 335;

    public static final int FRAME_death229 = 336;

    public static final int FRAME_death230 = 337;

    public static final int FRAME_death231 = 338;

    public static final int FRAME_death232 = 339;

    public static final int FRAME_death233 = 340;

    public static final int FRAME_death234 = 341;

    public static final int FRAME_death235 = 342;

    public static final int FRAME_death301 = 343;

    public static final int FRAME_death302 = 344;

    public static final int FRAME_death303 = 345;

    public static final int FRAME_death304 = 346;

    public static final int FRAME_death305 = 347;

    public static final int FRAME_death306 = 348;

    public static final int FRAME_death307 = 349;

    public static final int FRAME_death308 = 350;

    public static final int FRAME_death309 = 351;

    public static final int FRAME_death310 = 352;

    public static final int FRAME_death311 = 353;

    public static final int FRAME_death312 = 354;

    public static final int FRAME_death313 = 355;

    public static final int FRAME_death314 = 356;

    public static final int FRAME_death315 = 357;

    public static final int FRAME_death316 = 358;

    public static final int FRAME_death317 = 359;

    public static final int FRAME_death318 = 360;

    public static final int FRAME_death319 = 361;

    public static final int FRAME_death320 = 362;

    public static final int FRAME_death321 = 363;

    public static final int FRAME_death322 = 364;

    public static final int FRAME_death323 = 365;

    public static final int FRAME_death324 = 366;

    public static final int FRAME_death325 = 367;

    public static final int FRAME_death326 = 368;

    public static final int FRAME_death327 = 369;

    public static final int FRAME_death328 = 370;

    public static final int FRAME_death329 = 371;

    public static final int FRAME_death330 = 372;

    public static final int FRAME_death331 = 373;

    public static final int FRAME_death332 = 374;

    public static final int FRAME_death333 = 375;

    public static final int FRAME_death334 = 376;

    public static final int FRAME_death335 = 377;

    public static final int FRAME_death336 = 378;

    public static final int FRAME_death337 = 379;

    public static final int FRAME_death338 = 380;

    public static final int FRAME_death339 = 381;

    public static final int FRAME_death340 = 382;

    public static final int FRAME_death341 = 383;

    public static final int FRAME_death342 = 384;

    public static final int FRAME_death343 = 385;

    public static final int FRAME_death344 = 386;

    public static final int FRAME_death345 = 387;

    public static final int FRAME_death401 = 388;

    public static final int FRAME_death402 = 389;

    public static final int FRAME_death403 = 390;

    public static final int FRAME_death404 = 391;

    public static final int FRAME_death405 = 392;

    public static final int FRAME_death406 = 393;

    public static final int FRAME_death407 = 394;

    public static final int FRAME_death408 = 395;

    public static final int FRAME_death409 = 396;

    public static final int FRAME_death410 = 397;

    public static final int FRAME_death411 = 398;

    public static final int FRAME_death412 = 399;

    public static final int FRAME_death413 = 400;

    public static final int FRAME_death414 = 401;

    public static final int FRAME_death415 = 402;

    public static final int FRAME_death416 = 403;

    public static final int FRAME_death417 = 404;

    public static final int FRAME_death418 = 405;

    public static final int FRAME_death419 = 406;

    public static final int FRAME_death420 = 407;

    public static final int FRAME_death421 = 408;

    public static final int FRAME_death422 = 409;

    public static final int FRAME_death423 = 410;

    public static final int FRAME_death424 = 411;

    public static final int FRAME_death425 = 412;

    public static final int FRAME_death426 = 413;

    public static final int FRAME_death427 = 414;

    public static final int FRAME_death428 = 415;

    public static final int FRAME_death429 = 416;

    public static final int FRAME_death430 = 417;

    public static final int FRAME_death431 = 418;

    public static final int FRAME_death432 = 419;

    public static final int FRAME_death433 = 420;

    public static final int FRAME_death434 = 421;

    public static final int FRAME_death435 = 422;

    public static final int FRAME_death436 = 423;

    public static final int FRAME_death437 = 424;

    public static final int FRAME_death438 = 425;

    public static final int FRAME_death439 = 426;

    public static final int FRAME_death440 = 427;

    public static final int FRAME_death441 = 428;

    public static final int FRAME_death442 = 429;

    public static final int FRAME_death443 = 430;

    public static final int FRAME_death444 = 431;

    public static final int FRAME_death445 = 432;

    public static final int FRAME_death446 = 433;

    public static final int FRAME_death447 = 434;

    public static final int FRAME_death448 = 435;

    public static final int FRAME_death449 = 436;

    public static final int FRAME_death450 = 437;

    public static final int FRAME_death451 = 438;

    public static final int FRAME_death452 = 439;

    public static final int FRAME_death453 = 440;

    public static final int FRAME_death501 = 441;

    public static final int FRAME_death502 = 442;

    public static final int FRAME_death503 = 443;

    public static final int FRAME_death504 = 444;

    public static final int FRAME_death505 = 445;

    public static final int FRAME_death506 = 446;

    public static final int FRAME_death507 = 447;

    public static final int FRAME_death508 = 448;

    public static final int FRAME_death509 = 449;

    public static final int FRAME_death510 = 450;

    public static final int FRAME_death511 = 451;

    public static final int FRAME_death512 = 452;

    public static final int FRAME_death513 = 453;

    public static final int FRAME_death514 = 454;

    public static final int FRAME_death515 = 455;

    public static final int FRAME_death516 = 456;

    public static final int FRAME_death517 = 457;

    public static final int FRAME_death518 = 458;

    public static final int FRAME_death519 = 459;

    public static final int FRAME_death520 = 460;

    public static final int FRAME_death521 = 461;

    public static final int FRAME_death522 = 462;

    public static final int FRAME_death523 = 463;

    public static final int FRAME_death524 = 464;

    public static final int FRAME_death601 = 465;

    public static final int FRAME_death602 = 466;

    public static final int FRAME_death603 = 467;

    public static final int FRAME_death604 = 468;

    public static final int FRAME_death605 = 469;

    public static final int FRAME_death606 = 470;

    public static final int FRAME_death607 = 471;

    public static final int FRAME_death608 = 472;

    public static final int FRAME_death609 = 473;

    public static final int FRAME_death610 = 474;

    public static final float MODEL_SCALE = 1.200000f;

    static int sound_idle;

    static int sound_sight1;

    static int sound_sight2;

    static int sound_pain_light;

    static int sound_pain;

    static int sound_pain_ss;

    static int sound_death_light;

    static int sound_death;

    static int sound_death_ss;

    static int sound_cock;

    static final EntThinkAdapter soldier_dead = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_dead"; }
        @Override
        public boolean think(edict_t self) {

            Math3D.VectorSet(self.mins, -16, -16, -24);
            Math3D.VectorSet(self.maxs, 16, 16, -8);
            self.movetype = Defines.MOVETYPE_TOSS;
            self.svflags |= Defines.SVF_DEADMONSTER;
            self.nextthink = 0;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static final EntDieAdapter soldier_die = new EntDieAdapter() {
    	@Override
        public String getID(){ return "soldier_die"; }
        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {
            int n;

            
            if (self.health <= self.gib_health) {
                game_import_t
                        .sound(self, Defines.CHAN_VOICE, game_import_t
                                .soundindex("misc/udeath.wav"), 1,
                                Defines.ATTN_NORM, 0);
                for (n = 0; n < 3; n++)
                    GameMisc.ThrowGib(self,
                            "models/objects/gibs/sm_meat/tris.md2", damage,
                            Defines.GIB_ORGANIC);
                GameMisc.ThrowGib(self, "models/objects/gibs/chest/tris.md2",
                        damage, Defines.GIB_ORGANIC);
                GameMisc.ThrowHead(self, "models/objects/gibs/head2/tris.md2",
                        damage, Defines.GIB_ORGANIC);
                self.deadflag = Defines.DEAD_DEAD;
                return;
            }

            if (self.deadflag == Defines.DEAD_DEAD)
                return;

            
            self.deadflag = Defines.DEAD_DEAD;
            self.takedamage = Defines.DAMAGE_YES;
            self.s.skinnum |= 1;

            if (self.s.skinnum == 1)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_death_light,
                        1, Defines.ATTN_NORM, 0);
            else if (self.s.skinnum == 3)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_death, 1,
                        Defines.ATTN_NORM, 0);
            else
                
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_death_ss, 1,
                        Defines.ATTN_NORM, 0);

            if (Math.abs((self.s.origin[2] + self.viewheight) - point[2]) <= 4) {
                
                self.monsterinfo.currentmove = soldier_move_death3;
                return;
            }

            n = Lib.rand() % 5;
            if (n == 0)
                self.monsterinfo.currentmove = soldier_move_death1;
            else if (n == 1)
                self.monsterinfo.currentmove = soldier_move_death2;
            else if (n == 2)
                self.monsterinfo.currentmove = soldier_move_death4;
            else if (n == 3)
                self.monsterinfo.currentmove = soldier_move_death5;
            else
                self.monsterinfo.currentmove = soldier_move_death6;
        }
    };

    static final EntThinkAdapter soldier_attack1_refire1 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_attack1_refire1"; }
        @Override
        public boolean think(edict_t self) {
            if (self.s.skinnum > 1)
                return true;

            if (self.enemy.health <= 0)
                return true;

            if (((GameBase.skill.value == 3) && (Lib.random() < 0.5))
                    || (GameUtil.range(self, self.enemy) == Defines.RANGE_MELEE))
                self.monsterinfo.nextframe = FRAME_attak102;
            else
                self.monsterinfo.nextframe = FRAME_attak110;
            return true;
        }
    };

    static final EntThinkAdapter soldier_attack1_refire2 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_attack1_refire2"; }
        @Override
        public boolean think(edict_t self) {
            if (self.s.skinnum < 2)
                return true;

            if (self.enemy.health <= 0)
                return true;

            if (((GameBase.skill.value == 3) && (Lib.random() < 0.5))
                    || (GameUtil.range(self, self.enemy) == Defines.RANGE_MELEE))
                self.monsterinfo.nextframe = FRAME_attak102;
            return true;
        }
    };

    static final EntThinkAdapter soldier_attack2_refire1 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_attack2_refire1"; }
        @Override
        public boolean think(edict_t self) {
            if (self.s.skinnum > 1)
                return true;

            if (self.enemy.health <= 0)
                return true;

            if (((GameBase.skill.value == 3) && (Lib.random() < 0.5))
                    || (GameUtil.range(self, self.enemy) == Defines.RANGE_MELEE))
                self.monsterinfo.nextframe = FRAME_attak204;
            else
                self.monsterinfo.nextframe = FRAME_attak216;
            return true;
        }
    };

    static final EntThinkAdapter soldier_attack2_refire2 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_attack2_refire2"; }
        @Override
        public boolean think(edict_t self) {
            if (self.s.skinnum < 2)
                return true;

            if (self.enemy.health <= 0)
                return true;

            if (((GameBase.skill.value == 3) && (Lib.random() < 0.5))
                    || (GameUtil.range(self, self.enemy) == Defines.RANGE_MELEE))
                self.monsterinfo.nextframe = FRAME_attak204;
            return true;
        }
    };

    static final EntThinkAdapter soldier_attack3_refire = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_attack3_refire"; }
        @Override
        public boolean think(edict_t self) {
            if ((GameBase.level.time + 0.4) < self.monsterinfo.pausetime)
                self.monsterinfo.nextframe = FRAME_attak303;
            return true;
        }
    };

    static final EntThinkAdapter soldier_attack6_refire = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_attack6_refire"; }
        @Override
        public boolean think(edict_t self) {
            if (self.enemy.health <= 0)
                return true;

            if (GameUtil.range(self, self.enemy) < Defines.RANGE_MID)
                return true;

            if (GameBase.skill.value == 3)
                self.monsterinfo.nextframe = FRAME_runs03;
            return true;
        }
    };

    
    static final EntThinkAdapter soldier_fire8 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_fire8"; }
        @Override
        public boolean think(edict_t self) {
            soldier_fire(self, 7);
            return true;
        }
    };

    

    static final EntThinkAdapter soldier_fire1 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_fire1"; }
        @Override
        public boolean think(edict_t self) {
            soldier_fire(self, 0);
            return true;
        }
    };

    

    static final EntThinkAdapter soldier_fire2 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_fire2"; }
        @Override
        public boolean think(edict_t self) {
            soldier_fire(self, 1);
            return true;
        }
    };

    static final EntThinkAdapter soldier_duck_down = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_duck_down"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.aiflags & Defines.AI_DUCKED) != 0)
                return true;
            self.monsterinfo.aiflags |= Defines.AI_DUCKED;
            self.maxs[2] -= 32;
            self.takedamage = Defines.DAMAGE_YES;
            self.monsterinfo.pausetime = GameBase.level.time + 1;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static final EntThinkAdapter soldier_fire3 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_fire3"; }
        @Override
        public boolean think(edict_t self) {
            soldier_duck_down.think(self);
            soldier_fire(self, 2);
            return true;
        }
    };

    

    static final EntThinkAdapter soldier_fire4 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_fire4"; }
        @Override
        public boolean think(edict_t self) {
            soldier_fire(self, 3);
            
            
            
            
            
            
            
            return true;
        }
    };

    
    
    

    static final EntThinkAdapter soldier_fire6 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_fire6"; }
        @Override
        public boolean think(edict_t self) {
            soldier_fire(self, 5);
            return true;
        }
    };

    static final EntThinkAdapter soldier_fire7 = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_fire7"; }
        @Override
        public boolean think(edict_t self) {
            soldier_fire(self, 6);
            return true;
        }
    };

    static final EntThinkAdapter soldier_idle = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_idle"; }
        @Override
        public boolean think(edict_t self) {
            if (Lib.random() > 0.8)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_idle, 1,
                        Defines.ATTN_IDLE, 0);
            return true;
        }
    };

    static final EntThinkAdapter soldier_stand = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_stand"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.currentmove == soldier_move_stand3)
                    || (Lib.random() < 0.8))
                self.monsterinfo.currentmove = soldier_move_stand1;
            else
                self.monsterinfo.currentmove = soldier_move_stand3;
            return true;
        }
    };

    
    
    
    static final EntThinkAdapter soldier_walk1_random = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_walk1_random"; }
        @Override
        public boolean think(edict_t self) {
            if (Lib.random() > 0.1)
                self.monsterinfo.nextframe = FRAME_walk101;
            return true;
        }
    };

    static final EntThinkAdapter soldier_walk = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_walk"; }
        @Override
        public boolean think(edict_t self) {
            if (Lib.random() < 0.5)
                self.monsterinfo.currentmove = soldier_move_walk1;
            else
                self.monsterinfo.currentmove = soldier_move_walk2;
            return true;
        }
    };

    static final EntThinkAdapter soldier_run = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_run"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0) {
                self.monsterinfo.currentmove = soldier_move_stand1;
                return true;
            }

            boolean b = Stream.of(soldier_move_walk1, soldier_move_walk2, soldier_move_start_run).anyMatch(mmove_t -> self.monsterinfo.currentmove == mmove_t);
            if (b) {
                self.monsterinfo.currentmove = soldier_move_run;
            } else {
                self.monsterinfo.currentmove = soldier_move_start_run;
            }
            return true;
        }
    };

    static final EntPainAdapter soldier_pain = new EntPainAdapter() {
    	@Override
        public String getID(){ return "soldier_pain"; }
        @Override
        public void pain(edict_t self, edict_t other, float kick, int damage) {

            if (self.health < (self.max_health / 2))
                self.s.skinnum |= 1;

            if (GameBase.level.time < self.pain_debounce_time) {
                if ((self.velocity[2] > 100)) {
                    if (Stream.of(soldier_move_pain1, soldier_move_pain2, soldier_move_pain3).anyMatch(mmove_t -> (self.monsterinfo.currentmove == mmove_t))) {
                        self.monsterinfo.currentmove = soldier_move_pain4;
                    }
                }
                return;
            }

            self.pain_debounce_time = GameBase.level.time + 3;

            int n = self.s.skinnum | 1;
            if (n == 1)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain_light,
                        1, Defines.ATTN_NORM, 0);
            else if (n == 3)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain, 1,
                        Defines.ATTN_NORM, 0);
            else
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain_ss, 1,
                        Defines.ATTN_NORM, 0);

            if (self.velocity[2] > 100) {
                self.monsterinfo.currentmove = soldier_move_pain4;
                return;
            }

            if (GameBase.skill.value == 3)
                return;

            float r = Lib.random();

            if (r < 0.33)
                self.monsterinfo.currentmove = soldier_move_pain1;
            else if (r < 0.66)
                self.monsterinfo.currentmove = soldier_move_pain2;
            else
                self.monsterinfo.currentmove = soldier_move_pain3;
        }
    };

    
    
    

    static final EntThinkAdapter soldier_duck_up = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_duck_up"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.aiflags &= ~Defines.AI_DUCKED;
            self.maxs[2] += 32;
            self.takedamage = Defines.DAMAGE_AIM;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static final EntInteractAdapter soldier_sight = new EntInteractAdapter() {
    	@Override
        public String getID(){ return "soldier_sight"; }
        @Override
        public boolean interact(edict_t self, edict_t other) {
            if (Lib.random() < 0.5)
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_sight1, 1,
                        Defines.ATTN_NORM, 0);
            else
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_sight2, 1,
                        Defines.ATTN_NORM, 0);

            if ((GameBase.skill.value > 0)
                    && (GameUtil.range(self, self.enemy) >= Defines.RANGE_MID)) {
                if (Lib.random() > 0.5)
                    self.monsterinfo.currentmove = soldier_move_attack6;
            }
            return true;
        }
    };

    
    
    

    static final EntThinkAdapter SP_monster_soldier_x = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "SP_monster_soldier_x"; }
        @Override
        public boolean think(edict_t self) {

            self.s.modelindex = game_import_t
                    .modelindex("models/monsters/soldier/tris.md2");
            self.monsterinfo.scale = MODEL_SCALE;
            Math3D.VectorSet(self.mins, -16, -16, -24);
            Math3D.VectorSet(self.maxs, 16, 16, 32);
            self.movetype = Defines.MOVETYPE_STEP;
            self.solid = Defines.SOLID_BBOX;

            sound_idle = game_import_t.soundindex("soldier/solidle1.wav");
            sound_sight1 = game_import_t.soundindex("soldier/solsght1.wav");
            sound_sight2 = game_import_t.soundindex("soldier/solsrch1.wav");
            sound_cock = game_import_t.soundindex("infantry/infatck3.wav");

            self.mass = 100;

            self.pain = soldier_pain;
            self.die = soldier_die;

            self.monsterinfo.stand = soldier_stand;
            self.monsterinfo.walk = soldier_walk;
            self.monsterinfo.run = soldier_run;
            self.monsterinfo.dodge = soldier_dodge;
            self.monsterinfo.attack = soldier_attack;
            self.monsterinfo.melee = null;
            self.monsterinfo.sight = soldier_sight;

            game_import_t.linkentity(self);

            self.monsterinfo.stand.think(self);

            GameAI.walkmonster_start.think(self);
            return true;
        }
    };

    /*
     * QUAKED monster_soldier_light (1 .5 0) (-16 -16 -24) (16 16 32) Ambush
     * Trigger_Spawn Sight
     */
    public static final EntThinkAdapter SP_monster_soldier_light = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "SP_monster_soldier_light"; }
        @Override
        public boolean think(edict_t self) {
            if (GameBase.deathmatch.value != 0) {
                GameUtil.G_FreeEdict(self);
                return true;
            }

            SP_monster_soldier_x.think(self);

            sound_pain_light = game_import_t.soundindex("soldier/solpain2.wav");
            sound_death_light = game_import_t.soundindex("soldier/soldeth2.wav");
            game_import_t.modelindex("models/objects/laser/tris.md2");
            game_import_t.soundindex("misc/lasfly.wav");
            game_import_t.soundindex("soldier/solatck2.wav");

            self.s.skinnum = 0;
            self.health = 20;
            self.gib_health = -30;
            return true;
        }
    };

    /*
     * QUAKED monster_soldier (1 .5 0) (-16 -16 -24) (16 16 32) Ambush
     * Trigger_Spawn Sight
     */

    public static final EntThinkAdapter SP_monster_soldier = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "SP_monster_soldier"; }
        @Override
        public boolean think(edict_t self) {
            Com.DPrintf("Spawning a soldier at " + self.s.origin[0] + ' ' +
                    self.s.origin[1] + ' ' +
                    self.s.origin[2] + ' ' +
                    '\n');
            
            if (GameBase.deathmatch.value != 0) {
                GameUtil.G_FreeEdict(self);
                return true;
            }

            SP_monster_soldier_x.think(self);

            sound_pain = game_import_t.soundindex("soldier/solpain1.wav");
            sound_death = game_import_t.soundindex("soldier/soldeth1.wav");
            game_import_t.soundindex("soldier/solatck1.wav");

            self.s.skinnum = 2;
            self.health = 30;
            self.gib_health = -30;
            return true;
        }
    };

    /**
     * QUAKED monster_soldier_ss (1 .5 0) (-16 -16 -24) (16 16 32) Ambush
     * Trigger_Spawn Sight
     */
    public static final EntThinkAdapter SP_monster_soldier_ss = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "SP_monster_soldier_ss"; }
        @Override
        public boolean think(edict_t self) {
            if (GameBase.deathmatch.value != 0) {
                GameUtil.G_FreeEdict(self);
                return true;
            }

            SP_monster_soldier_x.think(self);

            sound_pain_ss = game_import_t.soundindex("soldier/solpain3.wav");
            sound_death_ss = game_import_t.soundindex("soldier/soldeth3.wav");
            game_import_t.soundindex("soldier/solatck3.wav");

            self.s.skinnum = 4;
            self.health = 40;
            self.gib_health = -30;
            return true;
        }
    };

    static void soldier_fire(edict_t self, int flash_number) {
        int flash_index;

        if (self.s.skinnum < 2)
            flash_index = blaster_flash[flash_number];
        else if (self.s.skinnum < 4)
            flash_index = shotgun_flash[flash_number];
        else
            flash_index = machinegun_flash[flash_number];

        float[] right = {0, 0, 0};
        float[] forward = {0, 0, 0};
        Math3D.AngleVectors(self.s.angles, forward, right, null);
        float[] start = {0, 0, 0};
        Math3D.G_ProjectSource(self.s.origin,
                M_Flash.monster_flash_offset[flash_index], forward, right,
                start);

        float[] aim = {0, 0, 0};
        if (flash_number == 5 || flash_number == 6) {
            Math3D.VectorCopy(forward, aim);
        } else {
            float[] end = {0, 0, 0};
            Math3D.VectorCopy(self.enemy.s.origin, end);
            end[2] += self.enemy.viewheight;
            Math3D.VectorSubtract(end, start, aim);
            float[] dir = {0, 0, 0};
            Math3D.vectoangles(aim, dir);
            float[] up = {0, 0, 0};
            Math3D.AngleVectors(dir, forward, right, up);

            float r = Lib.crandom() * 1000;
            float u = Lib.crandom() * 500;
            Math3D.VectorMA(start, 8192, forward, end);
            Math3D.VectorMA(end, r, right, end);
            Math3D.VectorMA(end, u, up, end);

            Math3D.VectorSubtract(end, start, aim);
            Math3D.VectorNormalize(aim);
        }

        if (self.s.skinnum <= 1) {
            Monster.monster_fire_blaster(self, start, aim, 5, 600, flash_index,
                    Defines.EF_BLASTER);
        } else if (self.s.skinnum <= 3) {
            Monster.monster_fire_shotgun(self, start, aim, 2, 1,
                    Defines.DEFAULT_SHOTGUN_HSPREAD,
                    Defines.DEFAULT_SHOTGUN_VSPREAD,
                    Defines.DEFAULT_SHOTGUN_COUNT, flash_index);
        } else {
            if (0 == (self.monsterinfo.aiflags & Defines.AI_HOLD_FRAME))
                self.monsterinfo.pausetime = GameBase.level.time
                        + (3 + Lib.rand() % 8) * Defines.FRAMETIME;

            Monster.monster_fire_bullet(self, start, aim, 2, 4,
                    Defines.DEFAULT_BULLET_HSPREAD,
                    Defines.DEFAULT_BULLET_VSPREAD, flash_index);

            if (GameBase.level.time >= self.monsterinfo.pausetime)
                self.monsterinfo.aiflags &= ~Defines.AI_HOLD_FRAME;
            else
                self.monsterinfo.aiflags |= Defines.AI_HOLD_FRAME;
        }
    }

    static final EntThinkAdapter soldier_cock = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_cock"; }
        @Override
        public boolean think(edict_t self) {
            if (self.s.frame == FRAME_stand322)
                game_import_t.sound(self, Defines.CHAN_WEAPON, sound_cock, 1,
                        Defines.ATTN_IDLE, 0);
            else
                game_import_t.sound(self, Defines.CHAN_WEAPON, sound_cock, 1,
                        Defines.ATTN_NORM, 0);
            return true;
        }
    };

    
    static final mframe_t[] soldier_frames_stand1 = {
            new mframe_t(GameAI.ai_stand, 0, soldier_idle),
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

    static final mmove_t soldier_move_stand1 = new mmove_t(FRAME_stand101,
            FRAME_stand130, soldier_frames_stand1, soldier_stand);

    static final mframe_t[] soldier_frames_stand3 = {
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
            new mframe_t(GameAI.ai_stand, 0, soldier_cock),
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

    static final mmove_t soldier_move_stand3 = new mmove_t(FRAME_stand301,
            FRAME_stand339, soldier_frames_stand3, soldier_stand);

    static final mframe_t[] soldier_frames_walk1 = {
            new mframe_t(GameAI.ai_walk, 3, null),
            new mframe_t(GameAI.ai_walk, 6, null),
            new mframe_t(GameAI.ai_walk, 2, null),
            new mframe_t(GameAI.ai_walk, 2, null),
            new mframe_t(GameAI.ai_walk, 2, null),
            new mframe_t(GameAI.ai_walk, 1, null),
            new mframe_t(GameAI.ai_walk, 6, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 3, null),
            new mframe_t(GameAI.ai_walk, -1, soldier_walk1_random),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null),
            new mframe_t(GameAI.ai_walk, 0, null) };

    static final mmove_t soldier_move_walk1 = new mmove_t(FRAME_walk101,
            FRAME_walk133, soldier_frames_walk1, null);

    static final mframe_t[] soldier_frames_walk2 = {
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 9, null),
            new mframe_t(GameAI.ai_walk, 8, null),
            new mframe_t(GameAI.ai_walk, 5, null),
            new mframe_t(GameAI.ai_walk, 1, null),
            new mframe_t(GameAI.ai_walk, 3, null),
            new mframe_t(GameAI.ai_walk, 7, null),
            new mframe_t(GameAI.ai_walk, 6, null),
            new mframe_t(GameAI.ai_walk, 7, null) };

    static final mmove_t soldier_move_walk2 = new mmove_t(FRAME_walk209,
            FRAME_walk218, soldier_frames_walk2, null);

    
    
    

    static final mframe_t[] soldier_frames_start_run = {
            new mframe_t(GameAI.ai_run, 7, null),
            new mframe_t(GameAI.ai_run, 5, null) };

    static final mmove_t soldier_move_start_run = new mmove_t(FRAME_run01,
            FRAME_run02, soldier_frames_start_run, soldier_run);

    static final mframe_t[] soldier_frames_run = {
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 11, null),
            new mframe_t(GameAI.ai_run, 11, null),
            new mframe_t(GameAI.ai_run, 16, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 15, null) };

    static final mmove_t soldier_move_run = new mmove_t(FRAME_run03, FRAME_run08,
            soldier_frames_run, null);

    
    
    

    static final EntThinkAdapter soldier_duck_hold = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_duck_hold"; }
        @Override
        public boolean think(edict_t self) {
            if (GameBase.level.time >= self.monsterinfo.pausetime)
                self.monsterinfo.aiflags &= ~Defines.AI_HOLD_FRAME;
            else
                self.monsterinfo.aiflags |= Defines.AI_HOLD_FRAME;
            return true;
        }
    };

    
    
    

    static final mframe_t[] soldier_frames_pain1 = {
            new mframe_t(GameAI.ai_move, -3, null),
            new mframe_t(GameAI.ai_move, 4, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t soldier_move_pain1 = new mmove_t(FRAME_pain101,
            FRAME_pain105, soldier_frames_pain1, soldier_run);

    static final mframe_t[] soldier_frames_pain2 = {
            new mframe_t(GameAI.ai_move, -13, null),
            new mframe_t(GameAI.ai_move, -1, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 4, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, 2, null) };

    static final mmove_t soldier_move_pain2 = new mmove_t(FRAME_pain201,
            FRAME_pain207, soldier_frames_pain2, soldier_run);

    static final mframe_t[] soldier_frames_pain3 = {
            new mframe_t(GameAI.ai_move, -8, null),
            new mframe_t(GameAI.ai_move, 10, null),
            new mframe_t(GameAI.ai_move, -4, null),
            new mframe_t(GameAI.ai_move, -1, null),
            new mframe_t(GameAI.ai_move, -3, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 4, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, 2, null) };

    static final mmove_t soldier_move_pain3 = new mmove_t(FRAME_pain301,
            FRAME_pain318, soldier_frames_pain3, soldier_run);

    static final mframe_t[] soldier_frames_pain4 = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, -10, null),
            new mframe_t(GameAI.ai_move, -6, null),
            new mframe_t(GameAI.ai_move, 8, null),
            new mframe_t(GameAI.ai_move, 4, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 5, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, -1, null),
            new mframe_t(GameAI.ai_move, -1, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t soldier_move_pain4 = new mmove_t(FRAME_pain401,
            FRAME_pain417, soldier_frames_pain4, soldier_run);

    
    
    

    static final int[] blaster_flash = { Defines.MZ2_SOLDIER_BLASTER_1,
            Defines.MZ2_SOLDIER_BLASTER_2, Defines.MZ2_SOLDIER_BLASTER_3,
            Defines.MZ2_SOLDIER_BLASTER_4, Defines.MZ2_SOLDIER_BLASTER_5,
            Defines.MZ2_SOLDIER_BLASTER_6, Defines.MZ2_SOLDIER_BLASTER_7,
            Defines.MZ2_SOLDIER_BLASTER_8 };

    static final int[] shotgun_flash = { Defines.MZ2_SOLDIER_SHOTGUN_1,
            Defines.MZ2_SOLDIER_SHOTGUN_2, Defines.MZ2_SOLDIER_SHOTGUN_3,
            Defines.MZ2_SOLDIER_SHOTGUN_4, Defines.MZ2_SOLDIER_SHOTGUN_5,
            Defines.MZ2_SOLDIER_SHOTGUN_6, Defines.MZ2_SOLDIER_SHOTGUN_7,
            Defines.MZ2_SOLDIER_SHOTGUN_8 };

    static final int[] machinegun_flash = { Defines.MZ2_SOLDIER_MACHINEGUN_1,
            Defines.MZ2_SOLDIER_MACHINEGUN_2, Defines.MZ2_SOLDIER_MACHINEGUN_3,
            Defines.MZ2_SOLDIER_MACHINEGUN_4, Defines.MZ2_SOLDIER_MACHINEGUN_5,
            Defines.MZ2_SOLDIER_MACHINEGUN_6, Defines.MZ2_SOLDIER_MACHINEGUN_7,
            Defines.MZ2_SOLDIER_MACHINEGUN_8 };

    static final mframe_t[] soldier_frames_attack1 = {
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_fire1),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_attack1_refire1),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_cock),
            new mframe_t(GameAI.ai_charge, 0, soldier_attack1_refire2),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null) };

    static final mmove_t soldier_move_attack1 = new mmove_t(FRAME_attak101,
            FRAME_attak112, soldier_frames_attack1, soldier_run);

    static final mframe_t[] soldier_frames_attack2 = {
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_fire2),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_attack2_refire1),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_cock),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_attack2_refire2),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null) };

    static final mmove_t soldier_move_attack2 = new mmove_t(FRAME_attak201,
            FRAME_attak218, soldier_frames_attack2, soldier_run);

    static final mframe_t[] soldier_frames_attack3 = {
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_fire3),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_attack3_refire),
            new mframe_t(GameAI.ai_charge, 0, soldier_duck_up),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null) };

    static final mmove_t soldier_move_attack3 = new mmove_t(FRAME_attak301,
            FRAME_attak309, soldier_frames_attack3, soldier_run);

    static final mframe_t[] soldier_frames_attack4 = {
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, soldier_fire4),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null) };

    static final mmove_t soldier_move_attack4 = new mmove_t(FRAME_attak401,
            FRAME_attak406, soldier_frames_attack4, soldier_run);

    static final mframe_t[] soldier_frames_attack6 = {
            new mframe_t(GameAI.ai_charge, 10, null),
            new mframe_t(GameAI.ai_charge, 4, null),
            new mframe_t(GameAI.ai_charge, 12, null),
            new mframe_t(GameAI.ai_charge, 11, soldier_fire8),
            new mframe_t(GameAI.ai_charge, 13, null),
            new mframe_t(GameAI.ai_charge, 18, null),
            new mframe_t(GameAI.ai_charge, 15, null),
            new mframe_t(GameAI.ai_charge, 14, null),
            new mframe_t(GameAI.ai_charge, 11, null),
            new mframe_t(GameAI.ai_charge, 8, null),
            new mframe_t(GameAI.ai_charge, 11, null),
            new mframe_t(GameAI.ai_charge, 12, null),
            new mframe_t(GameAI.ai_charge, 12, null),
            new mframe_t(GameAI.ai_charge, 17, soldier_attack6_refire) };

    static final mmove_t soldier_move_attack6 = new mmove_t(FRAME_runs01,
            FRAME_runs14, soldier_frames_attack6, soldier_run);

    static final mframe_t[] soldier_frames_duck = {
            new mframe_t(GameAI.ai_move, 5, soldier_duck_down),
            new mframe_t(GameAI.ai_move, -1, soldier_duck_hold),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 0, soldier_duck_up),
            new mframe_t(GameAI.ai_move, 5, null) };

    static final mmove_t soldier_move_duck = new mmove_t(FRAME_duck01, FRAME_duck05,
            soldier_frames_duck, soldier_run);

    static final mframe_t[] soldier_frames_death1 = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, -10, null),
            new mframe_t(GameAI.ai_move, -10, null),
            new mframe_t(GameAI.ai_move, -10, null),
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, soldier_fire6),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, soldier_fire7),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t soldier_move_death1 = new mmove_t(FRAME_death101,
            FRAME_death136, soldier_frames_death1, soldier_dead);

    static final mframe_t[] soldier_frames_death2 = {
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t soldier_move_death2 = new mmove_t(FRAME_death201,
            FRAME_death235, soldier_frames_death2, soldier_dead);

    static final mframe_t[] soldier_frames_death3 = {
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null), };

    static final mmove_t soldier_move_death3 = new mmove_t(FRAME_death301,
            FRAME_death345, soldier_frames_death3, soldier_dead);

    static final mframe_t[] soldier_frames_death4 = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t soldier_move_death4 = new mmove_t(FRAME_death401,
            FRAME_death453, soldier_frames_death4, soldier_dead);

    static final mframe_t[] soldier_frames_death5 = {
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t soldier_move_death5 = new mmove_t(FRAME_death501,
            FRAME_death524, soldier_frames_death5, soldier_dead);

    static final mframe_t[] soldier_frames_death6 = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t soldier_move_death6 = new mmove_t(FRAME_death601,
            FRAME_death610, soldier_frames_death6, soldier_dead);

    

    static final EntThinkAdapter soldier_attack = new EntThinkAdapter() {
    	@Override
        public String getID(){ return "soldier_attack"; }
        @Override
        public boolean think(edict_t self) {
            if (self.s.skinnum < 4) {
                if (Lib.random() < 0.5)
                    self.monsterinfo.currentmove = soldier_move_attack1;
                else
                    self.monsterinfo.currentmove = soldier_move_attack2;
            } else {
                self.monsterinfo.currentmove = soldier_move_attack4;
            }
            return true;
        }
    };

    static final EntDodgeAdapter soldier_dodge = new EntDodgeAdapter() {
    	@Override
        public String getID(){ return "soldier_dodge"; }
        @Override
        public void dodge(edict_t self, edict_t attacker, float eta) {

            float r = Lib.random();
            if (r > 0.25)
                return;

            if (self.enemy == null)
                self.enemy = attacker;

            if (GameBase.skill.value == 0) {
                self.monsterinfo.currentmove = soldier_move_duck;
                return;
            }

            self.monsterinfo.pausetime = GameBase.level.time + eta + 0.3f;
            r = Lib.random();

            if (GameBase.skill.value == 1) {
                if (r > 0.33)
                    self.monsterinfo.currentmove = soldier_move_duck;
                else
                    self.monsterinfo.currentmove = soldier_move_attack3;
                return;
            }

            if (GameBase.skill.value >= 2) {
                if (r > 0.66)
                    self.monsterinfo.currentmove = soldier_move_duck;
                else
                    self.monsterinfo.currentmove = soldier_move_attack3;
                return;
            }

            self.monsterinfo.currentmove = soldier_move_attack3;
        }
    };

}