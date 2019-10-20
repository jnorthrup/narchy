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

public class M_Boss32 {

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

    public static final int FRAME_attak201 = 18;

    public static final int FRAME_attak202 = 19;

    public static final int FRAME_attak203 = 20;

    public static final int FRAME_attak204 = 21;

    public static final int FRAME_attak205 = 22;

    public static final int FRAME_attak206 = 23;

    public static final int FRAME_attak207 = 24;

    public static final int FRAME_attak208 = 25;

    public static final int FRAME_attak209 = 26;

    public static final int FRAME_attak210 = 27;

    public static final int FRAME_attak211 = 28;

    public static final int FRAME_attak212 = 29;

    public static final int FRAME_attak213 = 30;

    public static final int FRAME_death01 = 31;

    public static final int FRAME_death02 = 32;

    public static final int FRAME_death03 = 33;

    public static final int FRAME_death04 = 34;

    public static final int FRAME_death05 = 35;

    public static final int FRAME_death06 = 36;

    public static final int FRAME_death07 = 37;

    public static final int FRAME_death08 = 38;

    public static final int FRAME_death09 = 39;

    public static final int FRAME_death10 = 40;

    public static final int FRAME_death11 = 41;

    public static final int FRAME_death12 = 42;

    public static final int FRAME_death13 = 43;

    public static final int FRAME_death14 = 44;

    public static final int FRAME_death15 = 45;

    public static final int FRAME_death16 = 46;

    public static final int FRAME_death17 = 47;

    public static final int FRAME_death18 = 48;

    public static final int FRAME_death19 = 49;

    public static final int FRAME_death20 = 50;

    public static final int FRAME_death21 = 51;

    public static final int FRAME_death22 = 52;

    public static final int FRAME_death23 = 53;

    public static final int FRAME_death24 = 54;

    public static final int FRAME_death25 = 55;

    public static final int FRAME_death26 = 56;

    public static final int FRAME_death27 = 57;

    public static final int FRAME_death28 = 58;

    public static final int FRAME_death29 = 59;

    public static final int FRAME_death30 = 60;

    public static final int FRAME_death31 = 61;

    public static final int FRAME_death32 = 62;

    public static final int FRAME_death33 = 63;

    public static final int FRAME_death34 = 64;

    public static final int FRAME_death35 = 65;

    public static final int FRAME_death36 = 66;

    public static final int FRAME_death37 = 67;

    public static final int FRAME_death38 = 68;

    public static final int FRAME_death39 = 69;

    public static final int FRAME_death40 = 70;

    public static final int FRAME_death41 = 71;

    public static final int FRAME_death42 = 72;

    public static final int FRAME_death43 = 73;

    public static final int FRAME_death44 = 74;

    public static final int FRAME_death45 = 75;

    public static final int FRAME_death46 = 76;

    public static final int FRAME_death47 = 77;

    public static final int FRAME_death48 = 78;

    public static final int FRAME_death49 = 79;

    public static final int FRAME_death50 = 80;

    public static final int FRAME_pain101 = 81;

    public static final int FRAME_pain102 = 82;

    public static final int FRAME_pain103 = 83;

    public static final int FRAME_pain201 = 84;

    public static final int FRAME_pain202 = 85;

    public static final int FRAME_pain203 = 86;

    public static final int FRAME_pain301 = 87;

    public static final int FRAME_pain302 = 88;

    public static final int FRAME_pain303 = 89;

    public static final int FRAME_pain304 = 90;

    public static final int FRAME_pain305 = 91;

    public static final int FRAME_pain306 = 92;

    public static final int FRAME_pain307 = 93;

    public static final int FRAME_pain308 = 94;

    public static final int FRAME_pain309 = 95;

    public static final int FRAME_pain310 = 96;

    public static final int FRAME_pain311 = 97;

    public static final int FRAME_pain312 = 98;

    public static final int FRAME_pain313 = 99;

    public static final int FRAME_pain314 = 100;

    public static final int FRAME_pain315 = 101;

    public static final int FRAME_pain316 = 102;

    public static final int FRAME_pain317 = 103;

    public static final int FRAME_pain318 = 104;

    public static final int FRAME_pain319 = 105;

    public static final int FRAME_pain320 = 106;

    public static final int FRAME_pain321 = 107;

    public static final int FRAME_pain322 = 108;

    public static final int FRAME_pain323 = 109;

    public static final int FRAME_pain324 = 110;

    public static final int FRAME_pain325 = 111;

    public static final int FRAME_stand01 = 112;

    public static final int FRAME_stand02 = 113;

    public static final int FRAME_stand03 = 114;

    public static final int FRAME_stand04 = 115;

    public static final int FRAME_stand05 = 116;

    public static final int FRAME_stand06 = 117;

    public static final int FRAME_stand07 = 118;

    public static final int FRAME_stand08 = 119;

    public static final int FRAME_stand09 = 120;

    public static final int FRAME_stand10 = 121;

    public static final int FRAME_stand11 = 122;

    public static final int FRAME_stand12 = 123;

    public static final int FRAME_stand13 = 124;

    public static final int FRAME_stand14 = 125;

    public static final int FRAME_stand15 = 126;

    public static final int FRAME_stand16 = 127;

    public static final int FRAME_stand17 = 128;

    public static final int FRAME_stand18 = 129;

    public static final int FRAME_stand19 = 130;

    public static final int FRAME_stand20 = 131;

    public static final int FRAME_stand21 = 132;

    public static final int FRAME_stand22 = 133;

    public static final int FRAME_stand23 = 134;

    public static final int FRAME_stand24 = 135;

    public static final int FRAME_stand25 = 136;

    public static final int FRAME_stand26 = 137;

    public static final int FRAME_stand27 = 138;

    public static final int FRAME_stand28 = 139;

    public static final int FRAME_stand29 = 140;

    public static final int FRAME_stand30 = 141;

    public static final int FRAME_stand31 = 142;

    public static final int FRAME_stand32 = 143;

    public static final int FRAME_stand33 = 144;

    public static final int FRAME_stand34 = 145;

    public static final int FRAME_stand35 = 146;

    public static final int FRAME_stand36 = 147;

    public static final int FRAME_stand37 = 148;

    public static final int FRAME_stand38 = 149;

    public static final int FRAME_stand39 = 150;

    public static final int FRAME_stand40 = 151;

    public static final int FRAME_stand41 = 152;

    public static final int FRAME_stand42 = 153;

    public static final int FRAME_stand43 = 154;

    public static final int FRAME_stand44 = 155;

    public static final int FRAME_stand45 = 156;

    public static final int FRAME_stand46 = 157;

    public static final int FRAME_stand47 = 158;

    public static final int FRAME_stand48 = 159;

    public static final int FRAME_stand49 = 160;

    public static final int FRAME_stand50 = 161;

    public static final int FRAME_stand51 = 162;

    public static final int FRAME_walk01 = 163;

    public static final int FRAME_walk02 = 164;

    public static final int FRAME_walk03 = 165;

    public static final int FRAME_walk04 = 166;

    public static final int FRAME_walk05 = 167;

    public static final int FRAME_walk06 = 168;

    public static final int FRAME_walk07 = 169;

    public static final int FRAME_walk08 = 170;

    public static final int FRAME_walk09 = 171;

    public static final int FRAME_walk10 = 172;

    public static final int FRAME_walk11 = 173;

    public static final int FRAME_walk12 = 174;

    public static final int FRAME_walk13 = 175;

    public static final int FRAME_walk14 = 176;

    public static final int FRAME_walk15 = 177;

    public static final int FRAME_walk16 = 178;

    public static final int FRAME_walk17 = 179;

    public static final int FRAME_walk18 = 180;

    public static final int FRAME_walk19 = 181;

    public static final int FRAME_walk20 = 182;

    public static final int FRAME_walk21 = 183;

    public static final int FRAME_walk22 = 184;

    public static final int FRAME_walk23 = 185;

    public static final int FRAME_walk24 = 186;

    public static final int FRAME_walk25 = 187;

    public static final int FRAME_active01 = 188;

    public static final int FRAME_active02 = 189;

    public static final int FRAME_active03 = 190;

    public static final int FRAME_active04 = 191;

    public static final int FRAME_active05 = 192;

    public static final int FRAME_active06 = 193;

    public static final int FRAME_active07 = 194;

    public static final int FRAME_active08 = 195;

    public static final int FRAME_active09 = 196;

    public static final int FRAME_active10 = 197;

    public static final int FRAME_active11 = 198;

    public static final int FRAME_active12 = 199;

    public static final int FRAME_active13 = 200;

    public static final int FRAME_attak301 = 201;

    public static final int FRAME_attak302 = 202;

    public static final int FRAME_attak303 = 203;

    public static final int FRAME_attak304 = 204;

    public static final int FRAME_attak305 = 205;

    public static final int FRAME_attak306 = 206;

    public static final int FRAME_attak307 = 207;

    public static final int FRAME_attak308 = 208;

    public static final int FRAME_attak401 = 209;

    public static final int FRAME_attak402 = 210;

    public static final int FRAME_attak403 = 211;

    public static final int FRAME_attak404 = 212;

    public static final int FRAME_attak405 = 213;

    public static final int FRAME_attak406 = 214;

    public static final int FRAME_attak407 = 215;

    public static final int FRAME_attak408 = 216;

    public static final int FRAME_attak409 = 217;

    public static final int FRAME_attak410 = 218;

    public static final int FRAME_attak411 = 219;

    public static final int FRAME_attak412 = 220;

    public static final int FRAME_attak413 = 221;

    public static final int FRAME_attak414 = 222;

    public static final int FRAME_attak415 = 223;

    public static final int FRAME_attak416 = 224;

    public static final int FRAME_attak417 = 225;

    public static final int FRAME_attak418 = 226;

    public static final int FRAME_attak419 = 227;

    public static final int FRAME_attak420 = 228;

    public static final int FRAME_attak421 = 229;

    public static final int FRAME_attak422 = 230;

    public static final int FRAME_attak423 = 231;

    public static final int FRAME_attak424 = 232;

    public static final int FRAME_attak425 = 233;

    public static final int FRAME_attak426 = 234;

    public static final int FRAME_attak501 = 235;

    public static final int FRAME_attak502 = 236;

    public static final int FRAME_attak503 = 237;

    public static final int FRAME_attak504 = 238;

    public static final int FRAME_attak505 = 239;

    public static final int FRAME_attak506 = 240;

    public static final int FRAME_attak507 = 241;

    public static final int FRAME_attak508 = 242;

    public static final int FRAME_attak509 = 243;

    public static final int FRAME_attak510 = 244;

    public static final int FRAME_attak511 = 245;

    public static final int FRAME_attak512 = 246;

    public static final int FRAME_attak513 = 247;

    public static final int FRAME_attak514 = 248;

    public static final int FRAME_attak515 = 249;

    public static final int FRAME_attak516 = 250;

    public static final int FRAME_death201 = 251;

    public static final int FRAME_death202 = 252;

    public static final int FRAME_death203 = 253;

    public static final int FRAME_death204 = 254;

    public static final int FRAME_death205 = 255;

    public static final int FRAME_death206 = 256;

    public static final int FRAME_death207 = 257;

    public static final int FRAME_death208 = 258;

    public static final int FRAME_death209 = 259;

    public static final int FRAME_death210 = 260;

    public static final int FRAME_death211 = 261;

    public static final int FRAME_death212 = 262;

    public static final int FRAME_death213 = 263;

    public static final int FRAME_death214 = 264;

    public static final int FRAME_death215 = 265;

    public static final int FRAME_death216 = 266;

    public static final int FRAME_death217 = 267;

    public static final int FRAME_death218 = 268;

    public static final int FRAME_death219 = 269;

    public static final int FRAME_death220 = 270;

    public static final int FRAME_death221 = 271;

    public static final int FRAME_death222 = 272;

    public static final int FRAME_death223 = 273;

    public static final int FRAME_death224 = 274;

    public static final int FRAME_death225 = 275;

    public static final int FRAME_death226 = 276;

    public static final int FRAME_death227 = 277;

    public static final int FRAME_death228 = 278;

    public static final int FRAME_death229 = 279;

    public static final int FRAME_death230 = 280;

    public static final int FRAME_death231 = 281;

    public static final int FRAME_death232 = 282;

    public static final int FRAME_death233 = 283;

    public static final int FRAME_death234 = 284;

    public static final int FRAME_death235 = 285;

    public static final int FRAME_death236 = 286;

    public static final int FRAME_death237 = 287;

    public static final int FRAME_death238 = 288;

    public static final int FRAME_death239 = 289;

    public static final int FRAME_death240 = 290;

    public static final int FRAME_death241 = 291;

    public static final int FRAME_death242 = 292;

    public static final int FRAME_death243 = 293;

    public static final int FRAME_death244 = 294;

    public static final int FRAME_death245 = 295;

    public static final int FRAME_death246 = 296;

    public static final int FRAME_death247 = 297;

    public static final int FRAME_death248 = 298;

    public static final int FRAME_death249 = 299;

    public static final int FRAME_death250 = 300;

    public static final int FRAME_death251 = 301;

    public static final int FRAME_death252 = 302;

    public static final int FRAME_death253 = 303;

    public static final int FRAME_death254 = 304;

    public static final int FRAME_death255 = 305;

    public static final int FRAME_death256 = 306;

    public static final int FRAME_death257 = 307;

    public static final int FRAME_death258 = 308;

    public static final int FRAME_death259 = 309;

    public static final int FRAME_death260 = 310;

    public static final int FRAME_death261 = 311;

    public static final int FRAME_death262 = 312;

    public static final int FRAME_death263 = 313;

    public static final int FRAME_death264 = 314;

    public static final int FRAME_death265 = 315;

    public static final int FRAME_death266 = 316;

    public static final int FRAME_death267 = 317;

    public static final int FRAME_death268 = 318;

    public static final int FRAME_death269 = 319;

    public static final int FRAME_death270 = 320;

    public static final int FRAME_death271 = 321;

    public static final int FRAME_death272 = 322;

    public static final int FRAME_death273 = 323;

    public static final int FRAME_death274 = 324;

    public static final int FRAME_death275 = 325;

    public static final int FRAME_death276 = 326;

    public static final int FRAME_death277 = 327;

    public static final int FRAME_death278 = 328;

    public static final int FRAME_death279 = 329;

    public static final int FRAME_death280 = 330;

    public static final int FRAME_death281 = 331;

    public static final int FRAME_death282 = 332;

    public static final int FRAME_death283 = 333;

    public static final int FRAME_death284 = 334;

    public static final int FRAME_death285 = 335;

    public static final int FRAME_death286 = 336;

    public static final int FRAME_death287 = 337;

    public static final int FRAME_death288 = 338;

    public static final int FRAME_death289 = 339;

    public static final int FRAME_death290 = 340;

    public static final int FRAME_death291 = 341;

    public static final int FRAME_death292 = 342;

    public static final int FRAME_death293 = 343;

    public static final int FRAME_death294 = 344;

    public static final int FRAME_death295 = 345;

    public static final int FRAME_death301 = 346;

    public static final int FRAME_death302 = 347;

    public static final int FRAME_death303 = 348;

    public static final int FRAME_death304 = 349;

    public static final int FRAME_death305 = 350;

    public static final int FRAME_death306 = 351;

    public static final int FRAME_death307 = 352;

    public static final int FRAME_death308 = 353;

    public static final int FRAME_death309 = 354;

    public static final int FRAME_death310 = 355;

    public static final int FRAME_death311 = 356;

    public static final int FRAME_death312 = 357;

    public static final int FRAME_death313 = 358;

    public static final int FRAME_death314 = 359;

    public static final int FRAME_death315 = 360;

    public static final int FRAME_death316 = 361;

    public static final int FRAME_death317 = 362;

    public static final int FRAME_death318 = 363;

    public static final int FRAME_death319 = 364;

    public static final int FRAME_death320 = 365;

    public static final int FRAME_jump01 = 366;

    public static final int FRAME_jump02 = 367;

    public static final int FRAME_jump03 = 368;

    public static final int FRAME_jump04 = 369;

    public static final int FRAME_jump05 = 370;

    public static final int FRAME_jump06 = 371;

    public static final int FRAME_jump07 = 372;

    public static final int FRAME_jump08 = 373;

    public static final int FRAME_jump09 = 374;

    public static final int FRAME_jump10 = 375;

    public static final int FRAME_jump11 = 376;

    public static final int FRAME_jump12 = 377;

    public static final int FRAME_jump13 = 378;

    public static final int FRAME_pain401 = 379;

    public static final int FRAME_pain402 = 380;

    public static final int FRAME_pain403 = 381;

    public static final int FRAME_pain404 = 382;

    public static final int FRAME_pain501 = 383;

    public static final int FRAME_pain502 = 384;

    public static final int FRAME_pain503 = 385;

    public static final int FRAME_pain504 = 386;

    public static final int FRAME_pain601 = 387;

    public static final int FRAME_pain602 = 388;

    public static final int FRAME_pain603 = 389;

    public static final int FRAME_pain604 = 390;

    public static final int FRAME_pain605 = 391;

    public static final int FRAME_pain606 = 392;

    public static final int FRAME_pain607 = 393;

    public static final int FRAME_pain608 = 394;

    public static final int FRAME_pain609 = 395;

    public static final int FRAME_pain610 = 396;

    public static final int FRAME_pain611 = 397;

    public static final int FRAME_pain612 = 398;

    public static final int FRAME_pain613 = 399;

    public static final int FRAME_pain614 = 400;

    public static final int FRAME_pain615 = 401;

    public static final int FRAME_pain616 = 402;

    public static final int FRAME_pain617 = 403;

    public static final int FRAME_pain618 = 404;

    public static final int FRAME_pain619 = 405;

    public static final int FRAME_pain620 = 406;

    public static final int FRAME_pain621 = 407;

    public static final int FRAME_pain622 = 408;

    public static final int FRAME_pain623 = 409;

    public static final int FRAME_pain624 = 410;

    public static final int FRAME_pain625 = 411;

    public static final int FRAME_pain626 = 412;

    public static final int FRAME_pain627 = 413;

    public static final int FRAME_stand201 = 414;

    public static final int FRAME_stand202 = 415;

    public static final int FRAME_stand203 = 416;

    public static final int FRAME_stand204 = 417;

    public static final int FRAME_stand205 = 418;

    public static final int FRAME_stand206 = 419;

    public static final int FRAME_stand207 = 420;

    public static final int FRAME_stand208 = 421;

    public static final int FRAME_stand209 = 422;

    public static final int FRAME_stand210 = 423;

    public static final int FRAME_stand211 = 424;

    public static final int FRAME_stand212 = 425;

    public static final int FRAME_stand213 = 426;

    public static final int FRAME_stand214 = 427;

    public static final int FRAME_stand215 = 428;

    public static final int FRAME_stand216 = 429;

    public static final int FRAME_stand217 = 430;

    public static final int FRAME_stand218 = 431;

    public static final int FRAME_stand219 = 432;

    public static final int FRAME_stand220 = 433;

    public static final int FRAME_stand221 = 434;

    public static final int FRAME_stand222 = 435;

    public static final int FRAME_stand223 = 436;

    public static final int FRAME_stand224 = 437;

    public static final int FRAME_stand225 = 438;

    public static final int FRAME_stand226 = 439;

    public static final int FRAME_stand227 = 440;

    public static final int FRAME_stand228 = 441;

    public static final int FRAME_stand229 = 442;

    public static final int FRAME_stand230 = 443;

    public static final int FRAME_stand231 = 444;

    public static final int FRAME_stand232 = 445;

    public static final int FRAME_stand233 = 446;

    public static final int FRAME_stand234 = 447;

    public static final int FRAME_stand235 = 448;

    public static final int FRAME_stand236 = 449;

    public static final int FRAME_stand237 = 450;

    public static final int FRAME_stand238 = 451;

    public static final int FRAME_stand239 = 452;

    public static final int FRAME_stand240 = 453;

    public static final int FRAME_stand241 = 454;

    public static final int FRAME_stand242 = 455;

    public static final int FRAME_stand243 = 456;

    public static final int FRAME_stand244 = 457;

    public static final int FRAME_stand245 = 458;

    public static final int FRAME_stand246 = 459;

    public static final int FRAME_stand247 = 460;

    public static final int FRAME_stand248 = 461;

    public static final int FRAME_stand249 = 462;

    public static final int FRAME_stand250 = 463;

    public static final int FRAME_stand251 = 464;

    public static final int FRAME_stand252 = 465;

    public static final int FRAME_stand253 = 466;

    public static final int FRAME_stand254 = 467;

    public static final int FRAME_stand255 = 468;

    public static final int FRAME_stand256 = 469;

    public static final int FRAME_stand257 = 470;

    public static final int FRAME_stand258 = 471;

    public static final int FRAME_stand259 = 472;

    public static final int FRAME_stand260 = 473;

    public static final int FRAME_walk201 = 474;

    public static final int FRAME_walk202 = 475;

    public static final int FRAME_walk203 = 476;

    public static final int FRAME_walk204 = 477;

    public static final int FRAME_walk205 = 478;

    public static final int FRAME_walk206 = 479;

    public static final int FRAME_walk207 = 480;

    public static final int FRAME_walk208 = 481;

    public static final int FRAME_walk209 = 482;

    public static final int FRAME_walk210 = 483;

    public static final int FRAME_walk211 = 484;

    public static final int FRAME_walk212 = 485;

    public static final int FRAME_walk213 = 486;

    public static final int FRAME_walk214 = 487;

    public static final int FRAME_walk215 = 488;

    public static final int FRAME_walk216 = 489;

    public static final int FRAME_walk217 = 490;

    public static final float MODEL_SCALE = 1.000000f;

    static int sound_pain4;

    static int sound_pain5;

    static int sound_pain6;

    static int sound_death;

    static int sound_step_left;

    static int sound_step_right;

    static int sound_attack_bfg;

    static int sound_brainsplorch;

    static int sound_prerailgun;

    static int sound_popup;

    static int sound_taunt1;

    static int sound_taunt2;

    static int sound_taunt3;

    static int sound_hit;

    static final EntThinkAdapter makron_taunt = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_taunt"; }
        @Override
        public boolean think(edict_t self) {

            var r = Lib.random();
            if (r <= 0.3)
                game_import_t.sound(self, Defines.CHAN_AUTO, sound_taunt1, 1,
                        Defines.ATTN_NONE, 0);
            else if (r <= 0.6)
                game_import_t.sound(self, Defines.CHAN_AUTO, sound_taunt2, 1,
                        Defines.ATTN_NONE, 0);
            else
                game_import_t.sound(self, Defines.CHAN_AUTO, sound_taunt3, 1,
                        Defines.ATTN_NONE, 0);
            return true;
        }
    };

    
    
    
    static final EntThinkAdapter makron_stand = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_stand"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = makron_move_stand;
            return true;
        }
    };

    /*
     * static EntThinkAdapter xxx = new EntThinkAdapter() { public boolean
     * think(edict_t self) { return true; } };
     */

    static final EntThinkAdapter makron_hit = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_hit"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_AUTO, sound_hit, 1,
                    Defines.ATTN_NONE, 0);
            return true;
        }
    };

    static final EntThinkAdapter makron_popup = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_popup"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_BODY, sound_popup, 1,
                    Defines.ATTN_NONE, 0);
            return true;
        }
    };

    static final EntThinkAdapter makron_step_left = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_step_left"; }

        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_BODY, sound_step_left, 1,
                    Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static final EntThinkAdapter makron_step_right = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_step_right"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_BODY, sound_step_right, 1,
                    Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static final EntThinkAdapter makron_brainsplorch = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_brainsplorch"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_brainsplorch, 1,
                    Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static final EntThinkAdapter makron_prerailgun = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_prerailgun"; }
        @Override
        public boolean think(edict_t self) {
            game_import_t.sound(self, Defines.CHAN_WEAPON, sound_prerailgun, 1,
                    Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static final mframe_t[] makron_frames_stand = {
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
            new mframe_t(GameAI.ai_stand, 0, null) 
    };

    static final mmove_t makron_move_stand = new mmove_t(FRAME_stand201,
            FRAME_stand260, makron_frames_stand, null);

    static final mframe_t[] makron_frames_run = {
            new mframe_t(GameAI.ai_run, 3, makron_step_left),
            new mframe_t(GameAI.ai_run, 12, null),
            new mframe_t(GameAI.ai_run, 8, null),
            new mframe_t(GameAI.ai_run, 8, null),
            new mframe_t(GameAI.ai_run, 8, makron_step_right),
            new mframe_t(GameAI.ai_run, 6, null),
            new mframe_t(GameAI.ai_run, 12, null),
            new mframe_t(GameAI.ai_run, 9, null),
            new mframe_t(GameAI.ai_run, 6, null),
            new mframe_t(GameAI.ai_run, 12, null) };

    static final mmove_t makron_move_run = new mmove_t(FRAME_walk204, FRAME_walk213,
            makron_frames_run, null);

    static mframe_t[] makron_frames_walk = {
            new mframe_t(GameAI.ai_walk, 3, makron_step_left),
            new mframe_t(GameAI.ai_walk, 12, null),
            new mframe_t(GameAI.ai_walk, 8, null),
            new mframe_t(GameAI.ai_walk, 8, null),
            new mframe_t(GameAI.ai_walk, 8, makron_step_right),
            new mframe_t(GameAI.ai_walk, 6, null),
            new mframe_t(GameAI.ai_walk, 12, null),
            new mframe_t(GameAI.ai_walk, 9, null),
            new mframe_t(GameAI.ai_walk, 6, null),
            new mframe_t(GameAI.ai_walk, 12, null)};

    static final mmove_t makron_move_walk = new mmove_t(FRAME_walk204, FRAME_walk213,
            makron_frames_run, null);

    
    
    
    static final EntThinkAdapter makron_dead = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_dead"; }
        @Override
        public boolean think(edict_t self) {
            Math3D.VectorSet(self.mins, -60, -60, 0);
            Math3D.VectorSet(self.maxs, 60, 60, 72);
            self.movetype = Defines.MOVETYPE_TOSS;
            self.svflags |= Defines.SVF_DEADMONSTER;
            self.nextthink = 0;
            game_import_t.linkentity(self);
            return true;
        }
    };

    static final EntThinkAdapter makron_walk = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_walk"; }
        @Override
        public boolean think(edict_t self) {
            self.monsterinfo.currentmove = makron_move_walk;
            return true;
        }
    };

    static final EntThinkAdapter makron_run = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_run"; }
        @Override
        public boolean think(edict_t self) {
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0)
                self.monsterinfo.currentmove = makron_move_stand;
            else
                self.monsterinfo.currentmove = makron_move_run;
            return true;
        }
    };

    static final mframe_t[] makron_frames_pain6 = {
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
            new mframe_t(GameAI.ai_move, 0, makron_popup),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, makron_taunt),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t makron_move_pain6 = new mmove_t(FRAME_pain601,
            FRAME_pain627, makron_frames_pain6, makron_run);

    static final mframe_t[] makron_frames_pain5 = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t makron_move_pain5 = new mmove_t(FRAME_pain501,
            FRAME_pain504, makron_frames_pain5, makron_run);

    static final mframe_t[] makron_frames_pain4 = {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t makron_move_pain4 = new mmove_t(FRAME_pain401,
            FRAME_pain404, makron_frames_pain4, makron_run);

    static final mframe_t[] makron_frames_death2 = {
            new mframe_t(GameAI.ai_move, -15, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, -12, null),
            new mframe_t(GameAI.ai_move, 0, makron_step_left),
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
            new mframe_t(GameAI.ai_move, 11, null),
            new mframe_t(GameAI.ai_move, 12, null),
            new mframe_t(GameAI.ai_move, 11, makron_step_right),
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
            new mframe_t(GameAI.ai_move, 5, null),
            new mframe_t(GameAI.ai_move, 7, null),
            new mframe_t(GameAI.ai_move, 6, makron_step_left),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, -1, null),
            new mframe_t(GameAI.ai_move, 2, null),
            
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
            new mframe_t(GameAI.ai_move, -6, null),
            new mframe_t(GameAI.ai_move, -4, null),
            new mframe_t(GameAI.ai_move, -6, makron_step_right),
            new mframe_t(GameAI.ai_move, -4, null),
            new mframe_t(GameAI.ai_move, -4, makron_step_left),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, -2, null),
            new mframe_t(GameAI.ai_move, -5, null),
            new mframe_t(GameAI.ai_move, -3, makron_step_right),
            new mframe_t(GameAI.ai_move, -8, null),
            new mframe_t(GameAI.ai_move, -3, makron_step_left),
            new mframe_t(GameAI.ai_move, -7, null),
            new mframe_t(GameAI.ai_move, -4, null),
            new mframe_t(GameAI.ai_move, -4, makron_step_right),
            
            new mframe_t(GameAI.ai_move, -6, null),
            new mframe_t(GameAI.ai_move, -7, null),
            new mframe_t(GameAI.ai_move, 0, makron_step_left),
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
            new mframe_t(GameAI.ai_move, -2, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 0, null),
            
            new mframe_t(GameAI.ai_move, 27, makron_hit),
            new mframe_t(GameAI.ai_move, 26, null),
            new mframe_t(GameAI.ai_move, 0, makron_brainsplorch),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) 
    };

    static final mmove_t makron_move_death2 = new mmove_t(FRAME_death201,
            FRAME_death295, makron_frames_death2, makron_dead);

    static final mframe_t[] makron_frames_death3 = {
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

    static mmove_t makron_move_death3 = new mmove_t(FRAME_death301,
            FRAME_death320, makron_frames_death3, null);

    static final mframe_t[] makron_frames_sight = {
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

    static final mmove_t makron_move_sight = new mmove_t(FRAME_active01,
            FRAME_active13, makron_frames_sight, makron_run);

    static final EntThinkAdapter makronBFG = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makronBFG"; }
        @Override
        public boolean think(edict_t self) {
            float[] forward = { 0, 0, 0 }, right = { 0, 0, 0 };

            Math3D.AngleVectors(self.s.angles, forward, right, null);
            float[] start = {0, 0, 0};
            Math3D.G_ProjectSource(self.s.origin,
                    M_Flash.monster_flash_offset[Defines.MZ2_MAKRON_BFG],
                    forward, right, start);

            float[] vec = {0, 0, 0};
            Math3D.VectorCopy(self.enemy.s.origin, vec);
            vec[2] += self.enemy.viewheight;
            float[] dir = {0, 0, 0};
            Math3D.VectorSubtract(vec, start, dir);
            Math3D.VectorNormalize(dir);
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_attack_bfg, 1,
                    Defines.ATTN_NORM, 0);
            Monster.monster_fire_bfg(self, start, dir, 50, 300, 100, 300,
                    Defines.MZ2_MAKRON_BFG);
            return true;
        }
    };

    static final EntThinkAdapter MakronSaveloc = new EntThinkAdapter() {
    	@Override
        public String getID() { return "MakronSaveloc"; }
        @Override
        public boolean think(edict_t self) {
            Math3D.VectorCopy(self.enemy.s.origin, self.pos1); 
                                                               
                                                               
            self.pos1[2] += self.enemy.viewheight;
            return true;
        }
    };

    

    static final EntThinkAdapter MakronRailgun = new EntThinkAdapter() {
    	@Override
        public String getID() { return "MakronRailgun"; }
        @Override
        public boolean think(edict_t self) {
            float[] forward = { 0, 0, 0 }, right = { 0, 0, 0 };

            Math3D.AngleVectors(self.s.angles, forward, right, null);
            float[] start = {0, 0, 0};
            Math3D.G_ProjectSource(self.s.origin,
                    M_Flash.monster_flash_offset[Defines.MZ2_MAKRON_RAILGUN_1],
                    forward, right, start);


            float[] dir = {0, 0, 0};
            Math3D.VectorSubtract(self.pos1, start, dir);
            Math3D.VectorNormalize(dir);

            Monster.monster_fire_railgun(self, start, dir, 50, 100,
                    Defines.MZ2_MAKRON_RAILGUN_1);

            return true;
        }
    };

    

    static final EntThinkAdapter MakronHyperblaster = new EntThinkAdapter() {
    	@Override
        public String getID() { return "MakronHyperblaster"; }
        @Override
        public boolean think(edict_t self) {
            float[] forward = { 0, 0, 0 }, right = { 0, 0, 0 };

            var flash_number = Defines.MZ2_MAKRON_BLASTER_1
                    + (self.s.frame - FRAME_attak405);

            Math3D.AngleVectors(self.s.angles, forward, right, null);
            float[] start = {0, 0, 0};
            Math3D.G_ProjectSource(self.s.origin,
                    M_Flash.monster_flash_offset[flash_number], forward, right,
                    start);

            float[] dir = {0, 0, 0};
            if (self.enemy != null) {
                float[] vec = {0, 0, 0};
                Math3D.VectorCopy(self.enemy.s.origin, vec);
                vec[2] += self.enemy.viewheight;
                Math3D.VectorSubtract(vec, start, vec);
                Math3D.vectoangles(vec, vec);
                dir[0] = vec[0];
            } else {
                dir[0] = 0;
            }
            if (self.s.frame <= FRAME_attak413)
                dir[1] = self.s.angles[1] - 10
                        * (self.s.frame - FRAME_attak413);
            else
                dir[1] = self.s.angles[1] + 10
                        * (self.s.frame - FRAME_attak421);
            dir[2] = 0;

            Math3D.AngleVectors(dir, forward, null, null);

            Monster.monster_fire_blaster(self, start, forward, 15, 1000,
                    Defines.MZ2_MAKRON_BLASTER_1, Defines.EF_BLASTER);

            return true;
        }
    };

    static final EntPainAdapter makron_pain = new EntPainAdapter() {
    	@Override
        public String getID() { return "makron_pain"; }
        @Override
        public void pain(edict_t self, edict_t other, float kick, int damage) {

            if (self.health < (self.max_health / 2))
                self.s.skinnum = 1;

            if (GameBase.level.time < self.pain_debounce_time)
                return;

            
            if (damage <= 25)
                if (Lib.random() < 0.2)
                    return;

            self.pain_debounce_time = GameBase.level.time + 3;
            if (GameBase.skill.value == 3)
                return; 

            if (damage <= 40) {
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain4, 1,
                        Defines.ATTN_NONE, 0);
                self.monsterinfo.currentmove = makron_move_pain4;
            } else if (damage <= 110) {
                game_import_t.sound(self, Defines.CHAN_VOICE, sound_pain5, 1,
                        Defines.ATTN_NONE, 0);
                self.monsterinfo.currentmove = makron_move_pain5;
            } else {
                if (damage <= 150)
                    if (Lib.random() <= 0.45) {
                        game_import_t.sound(self, Defines.CHAN_VOICE,
                                sound_pain6, 1, Defines.ATTN_NONE, 0);
                        self.monsterinfo.currentmove = makron_move_pain6;
                    } else if (Lib.random() <= 0.35) {
                        game_import_t.sound(self, Defines.CHAN_VOICE,
                                sound_pain6, 1, Defines.ATTN_NONE, 0);
                        self.monsterinfo.currentmove = makron_move_pain6;
                    }
            }
        }

    };

    static final EntInteractAdapter makron_sight = new EntInteractAdapter() {
    	@Override
        public String getID() { return "makron_sight"; }
        @Override
        public boolean interact(edict_t self, edict_t other) {
            self.monsterinfo.currentmove = makron_move_sight;
            return true;
        }
    };

    static final EntThinkAdapter makron_attack = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_attack"; }
        @Override
        public boolean think(edict_t self) {
            float[] vec = { 0, 0, 0 };

            var r = Lib.random();

            Math3D.VectorSubtract(self.enemy.s.origin, self.s.origin, vec);
            var range = Math3D.VectorLength(vec);

            if (r <= 0.3)
                self.monsterinfo.currentmove = makron_move_attack3;
            else if (r <= 0.6)
                self.monsterinfo.currentmove = makron_move_attack4;
            else
                self.monsterinfo.currentmove = makron_move_attack5;

            return true;
        }
    };

    /*
     * --- Makron Torso. This needs to be spawned in ---
     */

    static final EntThinkAdapter makron_torso_think = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_torso_think"; }
        @Override
        public boolean think(edict_t self) {
            if (++self.s.frame < 365)
                self.nextthink = GameBase.level.time + Defines.FRAMETIME;
            else {
                self.s.frame = 346;
                self.nextthink = GameBase.level.time + Defines.FRAMETIME;
            }
            return true;
        }
    };

    static final EntThinkAdapter makron_torso = new EntThinkAdapter() {
    	@Override
        public String getID() { return "makron_torso"; }
        @Override
        public boolean think(edict_t ent) {
            ent.movetype = Defines.MOVETYPE_NONE;
            ent.solid = Defines.SOLID_NOT;
            Math3D.VectorSet(ent.mins, -8, -8, 0);
            Math3D.VectorSet(ent.maxs, 8, 8, 8);
            ent.s.frame = 346;
            ent.s.modelindex = game_import_t
                    .modelindex("models/monsters/boss3/rider/tris.md2");
            ent.think = makron_torso_think;
            ent.nextthink = GameBase.level.time + 2 * Defines.FRAMETIME;
            ent.s.sound = game_import_t.soundindex("makron/spine.wav");
            game_import_t.linkentity(ent);
            return true;
        }
    };

    static final EntDieAdapter makron_die = new EntDieAdapter() {
    	@Override
        public String getID() { return "makron_die"; }
        @Override
        public void die(edict_t self, edict_t inflictor, edict_t attacker,
                        int damage, float[] point) {

            self.s.sound = 0;
            
            if (self.health <= self.gib_health) {
                game_import_t
                        .sound(self, Defines.CHAN_VOICE, game_import_t
                                .soundindex("misc/udeath.wav"), 1,
                                Defines.ATTN_NORM, 0);
                int n;
                for (n = 0; n < 1 /* 4 */; n++)
                    GameMisc.ThrowGib(self,
                            "models/objects/gibs/sm_meat/tris.md2", damage,
                            Defines.GIB_ORGANIC);
                for (n = 0; n < 4; n++)
                    GameMisc.ThrowGib(self,
                            "models/objects/gibs/sm_metal/tris.md2", damage,
                            Defines.GIB_METALLIC);
                GameMisc.ThrowHead(self, "models/objects/gibs/gear/tris.md2",
                        damage, Defines.GIB_METALLIC);
                self.deadflag = Defines.DEAD_DEAD;
                return;
            }

            if (self.deadflag == Defines.DEAD_DEAD)
                return;

            
            game_import_t.sound(self, Defines.CHAN_VOICE, sound_death, 1,
                    Defines.ATTN_NONE, 0);
            self.deadflag = Defines.DEAD_DEAD;
            self.takedamage = Defines.DAMAGE_YES;

            var tempent = GameUtil.G_Spawn();
            Math3D.VectorCopy(self.s.origin, tempent.s.origin);
            Math3D.VectorCopy(self.s.angles, tempent.s.angles);
            tempent.s.origin[1] -= 84;
            makron_torso.think(tempent);

            self.monsterinfo.currentmove = makron_move_death2;
        }
    };

    static final EntThinkAdapter Makron_CheckAttack = new EntThinkAdapter() {
    	@Override
        public String getID() { return "Makron_CheckAttack"; }
        @Override
        public boolean think(edict_t self) {

            if (self.enemy.health > 0) {

                float[] spot1 = {0, 0, 0};
                Math3D.VectorCopy(self.s.origin, spot1);
                spot1[2] += self.viewheight;
                float[] spot2 = {0, 0, 0};
                Math3D.VectorCopy(self.enemy.s.origin, spot2);
                spot2[2] += self.enemy.viewheight;

                var tr = game_import_t.trace(spot1, null, null, spot2, self,
                        Defines.CONTENTS_SOLID | Defines.CONTENTS_MONSTER
                                | Defines.CONTENTS_SLIME
                                | Defines.CONTENTS_LAVA);


                if (tr.ent != self.enemy)
                    return false;
            }

            var enemy_range = GameUtil.range(self, self.enemy);
            float[] temp = {0, 0, 0};
            Math3D.VectorSubtract(self.enemy.s.origin, self.s.origin, temp);
            var enemy_yaw = Math3D.vectoyaw(temp);

            self.ideal_yaw = enemy_yaw;

            
            if (enemy_range == Defines.RANGE_MELEE) {
                if (self.monsterinfo.melee != null)
                    self.monsterinfo.attack_state = Defines.AS_MELEE;
                else
                    self.monsterinfo.attack_state = Defines.AS_MISSILE;
                return true;
            }

            
            if (null != self.monsterinfo.attack)
                return false;

            if (GameBase.level.time < self.monsterinfo.attack_finished)
                return false;

            if (enemy_range == Defines.RANGE_FAR)
                return false;

            float chance;
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0) {
                chance = 0.4f;
            } else if (enemy_range == Defines.RANGE_MELEE) {
                chance = 0.8f;
            } else if (enemy_range == Defines.RANGE_NEAR) {
                chance = 0.4f;
            } else if (enemy_range == Defines.RANGE_MID) {
                chance = 0.2f;
            } else {
                return false;
            }

            if (Lib.random() < chance) {
                self.monsterinfo.attack_state = Defines.AS_MISSILE;
                self.monsterinfo.attack_finished = GameBase.level.time + 2
                        * Lib.random();
                return true;
            }

            if ((self.flags & Defines.FL_FLY) != 0) {
                if (Lib.random() < 0.3)
                    self.monsterinfo.attack_state = Defines.AS_SLIDING;
                else
                    self.monsterinfo.attack_state = Defines.AS_STRAIGHT;
            }

            return false;
        }
    };

    static final mframe_t[] makron_frames_attack3 = {
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, makronBFG),
            
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t makron_move_attack3 = new mmove_t(FRAME_attak301,
            FRAME_attak308, makron_frames_attack3, makron_run);

    static final mframe_t[] makron_frames_attack4 = {
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster),
            
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, MakronHyperblaster), 
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t makron_move_attack4 = new mmove_t(FRAME_attak401,
            FRAME_attak426, makron_frames_attack4, makron_run);

    static final mframe_t[] makron_frames_attack5 = {
            new mframe_t(GameAI.ai_charge, 0, makron_prerailgun),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, null),
            new mframe_t(GameAI.ai_charge, 0, MakronSaveloc),
            new mframe_t(GameAI.ai_move, 0, MakronRailgun),
            
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static final mmove_t makron_move_attack5 = new mmove_t(FRAME_attak501,
            FRAME_attak516, makron_frames_attack5, makron_run);

    /*
     * ================= MakronSpawn
     * 
     * =================
     */
    static final EntThinkAdapter MakronSpawn = new EntThinkAdapter() {
    	@Override
        public String getID() { return "MakronSpawn"; }
        @Override
        public boolean think(edict_t self) {

            SP_monster_makron(self);


            var player = GameBase.level.sight_client;
            if (player == null)
                return true;

            float[] vec = {0, 0, 0};
            Math3D.VectorSubtract(player.s.origin, self.s.origin, vec);
            self.s.angles[Defines.YAW] = Math3D.vectoyaw(vec);
            Math3D.VectorNormalize(vec);
            Math3D.VectorMA(Globals.vec3_origin, 400, vec, self.velocity);
            self.velocity[2] = 200;
            self.groundentity = null;

            return true;
        }
    };

    static final EntThinkAdapter MakronToss = new EntThinkAdapter() {
    	@Override
        public String getID() { return "MakronToss"; }
        @Override
        public boolean think(edict_t self) {

            var ent = GameUtil.G_Spawn();
            ent.nextthink = GameBase.level.time + 0.8f;
            ent.think = MakronSpawn;
            ent.target = self.target;
            Math3D.VectorCopy(self.s.origin, ent.s.origin);
            return true;
        }
    };

    
    
    

    static void MakronPrecache() {
        sound_pain4 = game_import_t.soundindex("makron/pain3.wav");
        sound_pain5 = game_import_t.soundindex("makron/pain2.wav");
        sound_pain6 = game_import_t.soundindex("makron/pain1.wav");
        sound_death = game_import_t.soundindex("makron/death.wav");
        sound_step_left = game_import_t.soundindex("makron/step1.wav");
        sound_step_right = game_import_t.soundindex("makron/step2.wav");
        sound_attack_bfg = game_import_t.soundindex("makron/bfg_fire.wav");
        sound_brainsplorch = game_import_t.soundindex("makron/brain1.wav");
        sound_prerailgun = game_import_t.soundindex("makron/rail_up.wav");
        sound_popup = game_import_t.soundindex("makron/popup.wav");
        sound_taunt1 = game_import_t.soundindex("makron/voice4.wav");
        sound_taunt2 = game_import_t.soundindex("makron/voice3.wav");
        sound_taunt3 = game_import_t.soundindex("makron/voice.wav");
        sound_hit = game_import_t.soundindex("makron/bhit.wav");

        game_import_t.modelindex("models/monsters/boss3/rider/tris.md2");
    }

    /*
     * QUAKED monster_makron (1 .5 0) (-30 -30 0) (30 30 90) Ambush
     * Trigger_Spawn Sight
     */
    static void SP_monster_makron(edict_t self) {
        if (GameBase.deathmatch.value != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        MakronPrecache();

        self.movetype = Defines.MOVETYPE_STEP;
        self.solid = Defines.SOLID_BBOX;
        self.s.modelindex = game_import_t
                .modelindex("models/monsters/boss3/rider/tris.md2");
        Math3D.VectorSet(self.mins, -30, -30, 0);
        Math3D.VectorSet(self.maxs, 30, 30, 90);

        self.health = 3000;
        self.gib_health = -2000;
        self.mass = 500;

        self.pain = makron_pain;
        self.die = makron_die;
        self.monsterinfo.stand = makron_stand;
        self.monsterinfo.walk = makron_walk;
        self.monsterinfo.run = makron_run;
        self.monsterinfo.dodge = null;
        self.monsterinfo.attack = makron_attack;
        self.monsterinfo.melee = null;
        self.monsterinfo.sight = makron_sight;
        self.monsterinfo.checkattack = Makron_CheckAttack;

        game_import_t.linkentity(self);

        
        self.monsterinfo.currentmove = makron_move_sight;
        self.monsterinfo.scale = MODEL_SCALE;

        GameAI.walkmonster_start.think(self);
    }
}