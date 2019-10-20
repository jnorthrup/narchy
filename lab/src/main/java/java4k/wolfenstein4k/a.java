package java4k.wolfenstein4k;

/*
 * Wolfenstein 4K
 * Copyright (C) 2011 meatfighter.com
 *
 * This file is part of Wolfenstein 4K.
 *
 * Wolfenstein 4K is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wolfenstein 4K is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http:
 *
 */

import java4k.GamePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class a extends GamePanel {

	
	private final boolean[] a = new boolean[32768];

	@Override
    public void start() {
		enableEvents(8);
		new Thread(this).start();
	}

	@Override
    public void run() {


        final var COLOR_TRANSPARENT = -1;
		final var COLOR_FLESH = 0;
		final var COLOR_BROWN = 1;
        final var COLOR_LIGHT_RED = 3;
        final var COLOR_DARK_GRAY = 5;
		final var COLOR_YELLOW = 6;
        final var COLOR_RED = 8;
		final var COLOR_DARK_GOLD = 9;
		final var COLOR_GOLD = 10;


        final var SYMBOL_KEY = 2;

        final var MAP_WALL = 1;

        final var VK_W = 0x57;
		final var VK_S = 0x53;
		final var VK_A = 0x41;
		final var VK_D = 0x44;

		var reds = new Color[256];
		var blacks = new Color[256];

		var i = 0;
		var image = new BufferedImage(256, 256, 1);
		var g = image.createGraphics();
		var random = new Random();


        for (i = 0; i < 256; i++) {
			reds[i] = new Color(0xFF, 0, 0, i);
			blacks[i] = new Color(0, 0, 0, i);
		}

		var s = "\u3310\u5400\u6900\uaa40\u5640\ud500\u5500\udf40\uff40\u7f40\u5d00\u5d54\u56a9\u5aa9\u56a9\uf6a9\uf5a9\ud5a5\u5a94\ua964\u65a4\u56a4\uaaa4\u6a90\ud550\ud550\u6aa4\u6aa4\u6aa4\u6aa4\u6954\u66a4\u6aa4\u6aa4\u6aa4\u6aa4\u6a90\u6a90\u6aa4\u6aa4\u1550\u5550\u5550\u5550\u5550\u5550\u5550\u1550\u1540\u5550\u5554\u1554\u0c11\u06a9\u06a9\u0555\u0555\u0555\u0155\u0155\u0155\u0155\u0054\u0000\u0000\u1550\u4000\u5000\ua400\ua400\u9900\u5a40\uaa40\uaa40\ua940\ua940\u5950\u59f4\u57f4\ufffd\ufffd\ufffd\ufff4\uff54\uf5a4\u5aa9\u6aa9\u1067\u5000\ua540\ua640\u6a90\uda94\uf6a4\ud6a4\udaa5\uf6a5\uf654\uda90\u6a94\ua554\ua900\u6a40\u1540\u8b11\u0014\u0055\u5155\ua555\ua555\u9555\u5554\u5554\u5550\u5540\u5500\u5400\ua940\uaa95\uaaaa\uaaaa\uaaaa\uaaa9\uaaa5\u55a5\u0055\u0000\u8a37\ud000\uf400\ufd00\uff40\uffd0\uffd0\uffd0\u5550\uffd0\uffd0\u7fff\u957f\uaa9f\uaaa7\ua955\u55ff\uffff\u5555\uffff\uffff\u0887\u2aaa\u2576\u2f76\u2556\u277e\u2756\u2aaa\u0000\u889a\u0000\u0000\ufffb\ufffb\uaaa0\u0be0\u0be0\u5aa5\u2ff8\ubebc\ub82e\ub82e\ubffe\u2ff8\u0aa0\u5555\uffff\u8041\ub441\ua401\ue7c1\u807f\ua001\ufffd\u8005\u8105\u8101\ufbff\u9101\u8141\u9041\uffff\t\u01d1\u00b2\u0163\u02e6\u03ab\u016c\u052d\u007d\u01cd\uffff\u8001\ubef1\ua09f\ube81\ua085\ua2fd\ube05\ua095\ubefd\ua215\ua295\ua085\ubffd\u8001\uffff\n\u0141\u0193\u0424\u0554\u0195\u0177\u0198\u023a\u017a\u019c\uffff\u9411\u8481\u9411\udffb\u9021\u9033\ubde1\ua533\ub521\u9533\u9521\ud573\u8001\u8021\uffff\f\u0442\u0583\u02b3\u0024\u00d4\u00e7\u00e9\u007c\u009c\u00bc\u00dc\u015d\uffff\u8011\uccd1\u8001\uccdf\u8011\ufdf1\u8001\ufdff\u8021\uaa2d\ubb81\ue0e1\uc06d\u8021\uffff\t\u0143\u0096\u0147\u01d7\u02e7\u0098\u015b\u03ab\u05ad\uffff\ua081\ua0a1\ubba1\u803f\ubb81\ua0bd\ua0a5\ubf85\u8025\ubfbd\ua081\ua8bd\ubba1\u8001\uffff\n\u00a3\u0155\u00a5\u0538\u0458\u015b\u001c\u02cc\u00ad\u015e\uffff\u8141\ubc01\u8141\uf76b\u8149\u8149\uff7f\u8001\uba75\u8a15\ube7d\ua851\uae5d\u8001\uffff\u0010\u0162\u0482\u0024\u0044\u03b4\u05c6\u0077\u0039\u00a9\u025a\u016a\u01da\u012c\u019c\u005d\u00cd";


		var value = 0;
		var k = 0;
		var y = 0;
		var x = 0;
		var sprites = new int[16][64][64];
        final var COLOR_WHITE = 7;
        final var COLOR_LIGHT_GRAY = 4;
        final var COLOR_BLUE = 2;
        int[] PALETTE = {0xFCC4A4, 0xD08050, 0x0000AD, 0xFF4F4F, 0xA8A8A8, 0x2C2C2C, 0xFCF400, 0xFFFFFF, 0xFF0000, 0x4F4F00, 0xE4DA00};
        for (i = 0; i < 8; i++) {
			value = s.charAt(k++);
			var width = (0x8000 & value) == 0 ? 8 : 16;
			var height = (value >> 8) & 0x7F;
			var palette0 = (value >> 4) & 0x0F;
			var palette1 = value & 0x0F;
			for (var offset = 0; offset < width; offset += 8) {
				for (y = 0; y < height; y++) {
					if (i == 2 && y > 16) {
						palette0 = COLOR_LIGHT_GRAY;
					}
					if (i == 5) {
						palette1 = y < 7 ? COLOR_WHITE : COLOR_BLUE;
					}
					value = s.charAt(k++);
					for (x = 0; x < 8; x++) {
						var color = value & 0x03;
						value >>= 2;
						sprites[i][y][width * 2 - 1 - x - offset] = sprites[i][y][x + offset] = (color == 0) ? -1 : (color == 1) ? 0 : (color == 2) ? PALETTE[palette0] : PALETTE[palette1];
					}
				}
			}
		}


        final var SPRITE_LEG = 1;
        final var SPRITE_MAN = 0;
        for (y = 0; y < 12; y++) {
			for (x = 0; x < 8; x++) {
				sprites[SPRITE_MAN][y + 39][x + 8] = sprites[SPRITE_LEG][y][x];
			}
		}


        final var SPRITE_MAN_SHOOTING = 9;
        for (y = 0; y < 51; y++) {
			for (x = 0; x < 16; x++) {
				sprites[SPRITE_MAN_SHOOTING][y][x] = sprites[SPRITE_MAN][y][x];
			}
		}
        final var SPRITE_FIRE = 3;
        for (y = 0; y < 8; y++) {
			for (x = 0; x < 8; x++) {
				if (sprites[SPRITE_FIRE][y << 1][x << 1] >= 0) {
					sprites[SPRITE_MAN_SHOOTING][y + 5][x + 4] = sprites[SPRITE_FIRE][y << 1][x << 1];
				}
			}
		}


		var j = 0;
		var textures = new int[16][64][64];
        final var TEXTURE_BRICK_WALL = 0;
        for (y = 0; y < 14; y++) {
			for (x = 0; x < 30; x++) {
				for (i = 0; i < 4; i++) {
					for (j = 0; j < 3; j++) {
						textures[TEXTURE_BRICK_WALL][(y + i * 16) & 63][16 * (i & 1) + (x + j * 32) & 63] = (int) ((1f - random.nextFloat() * 0.2f) * (227 - 148 * y / 12f));
					}
				}
			}
		}


        final var TEXTURE_GRAY_SWASTIKA_WALL = 3;
        final var TEXTURE_GRAY_BRICK_WALL = 2;
        final var TEXTURE_SWASTIKA_WALL = 1;
        final var SPRITE_DEAD_BOSS = 13;
        final var SPRITE_BOSS_SHOOTING = 12;
        final var SPRITE_BOSS = 11;
        final var SPRITE_YELLOW_DOOR = 10;
        final var SPRITE_BLUE_DOOR = 8;
        final var SPRITE_DEAD_MAN = 4;
        for (y = 0; y < 64; y++) {
			for (x = 0; x < 64; x++) {
				var color = textures[TEXTURE_BRICK_WALL][y][x] & 0xFF;
				textures[TEXTURE_GRAY_SWASTIKA_WALL][y][x] = textures[TEXTURE_GRAY_BRICK_WALL][y][x] = (color << 16) | (color << 8) | color;
				textures[TEXTURE_SWASTIKA_WALL][y][x] = textures[TEXTURE_BRICK_WALL][y][x];
				color = (((x == 8 || x == 55) && y >= 8 && y <= 55) || ((y == 8 || y == 55) && x >= 8 && x <= 55) ? 0x59 : 0xBB - y - x) + random.nextInt(4);
				sprites[SPRITE_YELLOW_DOOR][y][x] = (color << 16) | (color << 8);
				sprites[SPRITE_BLUE_DOOR][y][x] = (color << 8) | color;

				sprites[SPRITE_BOSS][y][x] = sprites[SPRITE_MAN][y][x] == 0xD08050 ? 0xFF0000 : sprites[SPRITE_MAN][y][x];
				sprites[SPRITE_BOSS_SHOOTING][y][x] = sprites[SPRITE_MAN_SHOOTING][y][x] == 0xD08050 ? 0xFF0000 : sprites[SPRITE_MAN_SHOOTING][y][x];
				sprites[SPRITE_DEAD_BOSS][y][x] = sprites[SPRITE_DEAD_MAN][y][x] == 0xD08050 ? 0xFF0000 : sprites[SPRITE_DEAD_MAN][y][x];
			}
		}


        final var SPRITE_SWASTIKA = 6;
        for (y = 0; y < 32; y++) {
			for (x = 0; x < 32; x++) {
				if (sprites[SPRITE_SWASTIKA][y >> 2][x >> 2] >= 0) {
					textures[TEXTURE_GRAY_SWASTIKA_WALL][16 + y][16 + x] = textures[TEXTURE_SWASTIKA_WALL][16 + y][16 + x] = sprites[SPRITE_SWASTIKA][y >> 2][x >> 2];
				}
			}
		}


		var nextFrameStartTime = System.nanoTime();
		var playerHealth = 0;
		var playerHasKey = false;
		var playerTriggerReleased = false;
        float playerAngle = 0;
        float playerY = 0;
        float playerX = 0;
		var playerWinning = 0;
		var playerDying = 0;
		var level = 0;
		var playerFiring = 0;
		var queue = new ArrayList<float[]>();
        Graphics2D g2 = null;
		var pixels = new int[65536];
		var zbuffer = new float[256][256];
		var map = new int[16][16];
        ArrayList<float[]>[][] objectMap = new ArrayList[16][16];
        final var MED_PACK_HEALTH = 16;
        final var PLAYER_SPEED = 4f;
        final var PLAYER_ANGLE_SPEED = 0.04f;
        final var VK_SPACE = 0x20;
        final var VK_SHOOT = 0x42;
        final var VK_DOWN = 0x28;
        final var VK_UP = 0x26;
        final var VK_RIGHT = 0x27;
        final var VK_LEFT = 0x25;
        final var DOOR_STATE_CLOSING = 3;
        final var DOOR_STATE_OPENED = 2;
        final var DOOR_STATE_OPENING = 1;
        final var DOOR_STATE_CLOSED = 0;
        final var MAP_DOOR = 2;
        final var MAP_EMPTY = 0;
        final var SYMBOL_BOSS = 5;
        final var SYMBOL_YELLOW_VERTICAL_DOOR = 4;
        final var SYMBOL_YELLOW_HORIZONTAL_DOOR = 3;
        final var SYMBOL_BLUE_VERTICAL_DOOR = 1;
        final var SYMBOL_BLUE_HORIZONTAL_DOOR = 0;
        final var SPRITE_KEY = 7;
        final var SPRITE_MED_PACK = 5;
        final var SPRITE_GUN = 2;
        final var TYPE_KEY = 5;
        final var TYPE_VERTICAL_DOOR = 4;
        final var TYPE_HORIZONTAL_DOOR = 3;
        final var TYPE_MED_PACK = 2;
        final var TYPE_DEAD_MAN = 1;
        final var TYPE_MAN = 0;
        final var MAN_STATE_RUNNING = 1;
        final var MAN_STATE_PAUSED = 0;
        final var PARALLEL_Y = 2;
        final var PARALLEL_X = 1;
        final var PARALLEL_NOT = 0;
        final var OBJ_BOSS = 17;
        final var OBJ_HITS = 16;
        final var OBJ_TIMER2 = 15;
        final var OBJ_Y2 = 14;
        final var OBJ_X2 = 13;
        final var OBJ_TIMER = 12;
        final var OBJ_STATE = 11;
        final var OBJ_VY = 10;
        final var OBJ_VX = 9;
        final var OBJ_Y = 8;
        final var OBJ_X = 7;
        final var OBJ_WIDTH = 6;
        final var OBJ_HEIGHT = 5;
        final var OBJ_PARALLEL = 4;
        final var OBJ_FLIPPED = 3;
        final var OBJ_SPRITE = 2;
        final var OBJ_TYPE = 1;
        final var OBJ_REMOVE = 0;
        final var FLOOR_COLOR = 0x707070;
        final var CEILING_COLOR = 0x383838;
        int[] LEVEL_OFFSETS = {174, 200, 227, 256, 282, 309};
        while (true) {

			do {
				nextFrameStartTime += 16666667;

				

				
				if (playerDying > 0) {
					playerDying += 2;
					if (playerDying >= 256) {
						playerDying = 0;
					} else {
						continue;
					}
				}

				
				if (playerWinning > 0) {
					if (level != 6) {
						playerWinning += 2;
						if (playerWinning == 256) {
							playerHealth = 0;
							level++;
						} else if (playerWinning >= 512) {
							playerWinning = 0;
							playerHealth = 0;
						} else {
							continue;
						}
					} else {
						continue;
					}
				}

				
				if (playerHealth <= 0 && level != 6) {

					queue.clear();

					
					for (i = 0; i < 16; i++) {
						for (j = 0; j < 16; j++) {
							objectMap[i][j] = new ArrayList<>();
						}
					}

					
					playerX = 96;
					playerY = 96;
					playerAngle = 0;
					playerHasKey = false;
					playerHealth = 64;
					playerFiring = 0;

					
					k = LEVEL_OFFSETS[level];
					for (i = 0; i < 16; i++) {
						value = s.charAt(k++);
						for (j = 0; j < 16; j++) {
							map[i][j] = value & 1;
							value >>= 1;
						}
					}

					
					int count = s.charAt(k++);
					for (i = 0; i < count; i++) {
						value = s.charAt(k++);
						x = (value >> 4) & 0x0F;
						y = value & 0x0F;
						var object = new float[32];
						queue.add(object);
						objectMap[y][x].add(object);
						map[y][x] = MAP_DOOR;
						object[OBJ_SPRITE] = SPRITE_BLUE_DOOR;
						object[OBJ_WIDTH] = 64;
						object[OBJ_HEIGHT] = 64;
						object[OBJ_X] = 32 + 64 * x;
						object[OBJ_Y] = 32 + 64 * y;
						object[OBJ_X2] = x;
						object[OBJ_Y2] = y;
						value >>= 8;
                        switch (value) {
                            case SYMBOL_BLUE_HORIZONTAL_DOOR:
                                object[OBJ_TYPE] = TYPE_HORIZONTAL_DOOR;
                                object[OBJ_PARALLEL] = PARALLEL_X;
                                break;
                            case SYMBOL_YELLOW_HORIZONTAL_DOOR:
                                object[OBJ_TYPE] = TYPE_HORIZONTAL_DOOR;
                                object[OBJ_PARALLEL] = PARALLEL_X;
                                object[OBJ_SPRITE] = SPRITE_YELLOW_DOOR;
                                object[OBJ_HITS] = 1;
                                break;
                            case SYMBOL_BLUE_VERTICAL_DOOR:
                                object[OBJ_TYPE] = TYPE_VERTICAL_DOOR;
                                object[OBJ_PARALLEL] = PARALLEL_Y;
                                break;
                            case SYMBOL_YELLOW_VERTICAL_DOOR:
                                object[OBJ_TYPE] = TYPE_VERTICAL_DOOR;
                                object[OBJ_PARALLEL] = PARALLEL_Y;
                                object[OBJ_SPRITE] = SPRITE_YELLOW_DOOR;
                                object[OBJ_HITS] = 1;
                                break;
                            case SYMBOL_BOSS:
                                object[OBJ_TYPE] = TYPE_MAN;
                                object[OBJ_SPRITE] = SPRITE_BOSS;
                                object[OBJ_HEIGHT] = 51;
                                object[OBJ_WIDTH] = 16;
                                object[OBJ_HITS] = 32;
                                object[OBJ_BOSS] = 1;
                                map[y][x] = MAP_EMPTY;
                                break;
                            default:
                                object[OBJ_TYPE] = TYPE_KEY;
                                object[OBJ_WIDTH] = 16;
                                object[OBJ_HEIGHT] = 8;
                                object[OBJ_SPRITE] = SPRITE_KEY;
                                map[y][x] = MAP_EMPTY;
                                break;
                        }
					}

					
					k = 16 + level * 4;
					while (k > 0) {
						x = 4 + random.nextInt(12);
						y = 4 + random.nextInt(12);
						if (map[y][x] == MAP_EMPTY && objectMap[y][x].size() == 0) {
							k--;
							var object = new float[32];
							object[OBJ_X] = 32 + (x << 6);
							object[OBJ_Y] = 32 + (y << 6);
							object[OBJ_TYPE] = TYPE_MAN;
							object[OBJ_SPRITE] = SPRITE_MAN;
							object[OBJ_HEIGHT] = 51;
							object[OBJ_WIDTH] = 16;
							object[OBJ_HITS] = 5;
							queue.add(object);
							objectMap[y][x].add(object);
						}
					}

					if (playerWinning > 0) {
						continue;
					}
				}


				var nextX = playerX;
				var nextY = playerY;
				var vx = (float) Math.cos(playerAngle);
				var vy = (float) Math.sin(playerAngle);

				if (playerFiring > 0) {
					playerFiring--;
				} else if (a[VK_SHOOT]) {
					if (playerTriggerReleased) {
						playerFiring = 5;
						playerTriggerReleased = false;


						var bx = playerX;
						var by = playerY;
						outter: while (map[(int) by >> 6][(int) bx >> 6] == 0) {
							bx += vx;
							by += vy;
							for (i = 0; i < objectMap[(int) by >> 6][(int) bx >> 6].size(); i++) {
								var object = objectMap[(int) by >> 6][(int) bx >> 6].get(i);
								if (object[OBJ_TYPE] == TYPE_MAN) {
									if (--object[OBJ_HITS] == 0) {

										
										object[OBJ_TYPE] = TYPE_DEAD_MAN;
										object[OBJ_SPRITE] = object[OBJ_BOSS] == 1 ? SPRITE_DEAD_BOSS : SPRITE_DEAD_MAN;
										object[OBJ_HEIGHT] = 11;
										object[OBJ_WIDTH] = 32;
										object[OBJ_VX] = 0;
										object[OBJ_VY] = 0;

										x = (int) object[OBJ_X] >> 6;
										y = (int) object[OBJ_Y] >> 6;

										if (object[OBJ_BOSS] == 1) {
											
											playerWinning = 2;
										} else if (random.nextInt(4) == 0 || playerHealth < 16) {

											var medPack = new float[32];
											medPack[OBJ_TYPE] = TYPE_MED_PACK;
											medPack[OBJ_SPRITE] = SPRITE_MED_PACK;
											medPack[OBJ_HEIGHT] = 10;
											medPack[OBJ_WIDTH] = 32;
											medPack[OBJ_X] = (x << 6) + 32;
											medPack[OBJ_Y] = (y << 6) + 32;
											queue.add(medPack);
											objectMap[y][x].add(medPack);
										}
									}
									break outter;
								}
							}
						}
					}
				} else {
					playerTriggerReleased = true;
				}

				
				if (a[VK_LEFT]) {
					playerAngle += PLAYER_ANGLE_SPEED;
				} else if (a[VK_RIGHT]) {
					playerAngle -= PLAYER_ANGLE_SPEED;
				} else if (a[VK_UP]) {
					nextX += PLAYER_SPEED * vx;
					nextY += PLAYER_SPEED * vy;
				} else if (a[VK_DOWN]) {
					nextX -= PLAYER_SPEED * vx;
					nextY -= PLAYER_SPEED * vy;
				}


				var testX = vx < 0 ? -5 : 5;
				var testY = vy < 0 ? -5 : 5;

				if (map[((int) (nextY + testY) >> 6) & 15][((int) (nextX + testX) >> 6) & 15] == 0 && map[((int) nextY >> 6) & 15][((int) (nextX + testX) >> 6) & 15] == 0
						&& map[((int) (nextY + testY) >> 6) & 15][((int) nextX >> 6) & 15] == 0) {
					playerX = nextX;
					playerY = nextY;
				}

				
				for (i = queue.size() - 1; i >= 0; i--) {
					var object = queue.get(i);

					if (object[OBJ_REMOVE] == 1) {
						queue.remove(i);
						objectMap[(int) object[OBJ_Y] >> 6][(int) object[OBJ_X] >> 6].remove(object);
						continue;
					}

					if (object[OBJ_TYPE] == TYPE_MAN) {

						
						if (object[OBJ_STATE] == MAN_STATE_PAUSED) {
							if (--object[OBJ_TIMER2] < 0) {
								object[OBJ_SPRITE] = object[OBJ_BOSS] == 1 ? SPRITE_BOSS : SPRITE_MAN;
							}
							if (--object[OBJ_TIMER] < 0) {
								object[OBJ_TIMER] = 60 + object[OBJ_BOSS] == 1 ? random.nextInt(60) : random.nextInt(180);
								object[OBJ_STATE] = MAN_STATE_RUNNING;

								var angle = 6.28f * random.nextFloat();
								object[OBJ_VX] = (float) Math.cos(angle);
								object[OBJ_VY] = (float) Math.sin(angle);
								if (object[OBJ_BOSS] == 1) {
									object[OBJ_VX] *= 2;
									object[OBJ_VY] *= 2;
								}
							}
						} else { 

							
							if (((int) object[OBJ_TIMER] & 7) == 0) {
								object[OBJ_FLIPPED] = object[OBJ_FLIPPED] == 1 ? 0 : 1;
							}

							
							testX = object[OBJ_VX] < 0 ? -24 : 24;
							testY = object[OBJ_VY] < 0 ? -24 : 24;

							if (map[((int) (object[OBJ_Y] + testY) >> 6) & 15][((int) (object[OBJ_X] + testX) >> 6) & 15] != 0
									|| map[((int) (object[OBJ_Y]) >> 6) & 15][((int) (object[OBJ_X] + testX) >> 6) & 15] != 0
									|| map[((int) (object[OBJ_Y] + testY) >> 6) & 15][((int) (object[OBJ_X]) >> 6) & 15] != 0 || --object[OBJ_TIMER] < 0) {
								object[OBJ_STATE] = MAN_STATE_PAUSED;
								object[OBJ_TIMER] = 30 + random.nextInt(30);
								object[OBJ_TIMER2] = 10;
								object[OBJ_SPRITE] = object[OBJ_BOSS] == 1 ? SPRITE_BOSS_SHOOTING : SPRITE_MAN_SHOOTING;
								object[OBJ_VX] = 0;
								object[OBJ_VY] = 0;


								var bx = object[OBJ_X];
								var by = object[OBJ_Y];
								var dx = playerX - bx;
								var dy = playerY - by;
								var mag = (float) Math.sqrt(dx * dx + dy * dy);
								dx /= mag;
								dy /= mag;
								while (map[(int) by >> 6][(int) bx >> 6] == 0) {
									bx += dx;
									by += dy;
									var ex = playerX - bx;
									var ey = playerY - by;
									if (ex * ex + ey * ey < 4096) {
										playerHealth -= 2;
										if (playerHealth <= 0) {
											playerDying = 2;
										}
										break;
									}
								}
							}
						}
					} else if (object[OBJ_TYPE] == TYPE_HORIZONTAL_DOOR || object[OBJ_TYPE] == TYPE_VERTICAL_DOOR) {

						
						if (object[OBJ_STATE] == DOOR_STATE_CLOSED) {
							var dx = object[OBJ_X] - playerX;
							var dy = object[OBJ_Y] - playerY;
							if (dx * dx + dy * dy < 9216 && a[VK_SPACE] && (object[OBJ_HITS] == 0 || playerHasKey)) {
								object[OBJ_STATE] = DOOR_STATE_OPENING;
								object[OBJ_TIMER] = 16;
							}
						} else if (object[OBJ_STATE] == DOOR_STATE_OPENING) {
							if (object[OBJ_TYPE] == TYPE_HORIZONTAL_DOOR) {
								object[OBJ_X] -= 4;
							} else {
								object[OBJ_Y] -= 4;
							}
							if (--object[OBJ_TIMER] == 0) {
								object[OBJ_TIMER] = 180;
								object[OBJ_STATE] = DOOR_STATE_OPENED;
								map[(int) object[OBJ_Y2]][(int) object[OBJ_X2]] = MAP_EMPTY;
							}
						} else if (object[OBJ_STATE] == DOOR_STATE_OPENED) {
							if (--object[OBJ_TIMER] <= 0 && !(((int) playerX >> 6) == object[OBJ_X2] && ((int) playerY >> 6) == object[OBJ_Y2])
									&& objectMap[(int) object[OBJ_Y2]][(int) object[OBJ_X2]].size() == 1) {
								object[OBJ_TIMER] = 16;
								object[OBJ_STATE] = DOOR_STATE_CLOSING;
								map[(int) object[OBJ_Y2]][(int) object[OBJ_X2]] = MAP_DOOR;
							}
						} else {
							if (object[OBJ_TYPE] == TYPE_HORIZONTAL_DOOR) {
								object[OBJ_X] += 4;
							} else {
								object[OBJ_Y] += 4;
							}
							if (--object[OBJ_TIMER] == 0) {
								object[OBJ_STATE] = DOOR_STATE_CLOSED;
							}
						}
					} else if (object[OBJ_TYPE] == TYPE_MED_PACK || object[OBJ_TYPE] == TYPE_KEY) {
						var dx = object[OBJ_X] - playerX;
						var dy = object[OBJ_Y] - playerY;
						if (dx * dx + dy * dy < 1024) {
							object[OBJ_REMOVE] = 1;
							if (object[OBJ_TYPE] == TYPE_MED_PACK) {
								playerHealth += MED_PACK_HEALTH;
								if (playerHealth > 64) {
									playerHealth = 64;
								}
							} else {
								playerHasKey = true;
							}
						}
					}

					
					if (object[OBJ_VX] != 0 && object[OBJ_VY] != 0) {
						for (y = -1; y <= 1; y++) {
							for (x = -1; x <= 1; x++) {
								objectMap[((int) (object[OBJ_Y] + 16 * y) >> 6) & 15][((int) (object[OBJ_X] + 16 * x) >> 6) & 15].remove(object);
							}
						}
						object[OBJ_X] += object[OBJ_VX];
						object[OBJ_Y] += object[OBJ_VY];
						for (y = -1; y <= 1; y++) {
							for (x = -1; x <= 1; x++) {
								if (!objectMap[((int) (object[OBJ_Y] + 16 * y) >> 6) & 15][((int) (object[OBJ_X] + 16 * x) >> 6) & 15].contains(object)) {
									objectMap[((int) (object[OBJ_Y] + 16 * y) >> 6) & 15][((int) (object[OBJ_X] + 16 * x) >> 6) & 15].add(object);
								}
							}
						}
					}
				}

				

			} while (nextFrameStartTime < System.nanoTime());

			

			
			for (i = 0; i < 32768; i++) {
				pixels[i] = CEILING_COLOR;
			}
			for (; i < 65536; i++) {
				pixels[i] = FLOOR_COLOR;
			}


			var gx = ((int) playerX >> 6) & 15;
			var gy = ((int) playerY >> 6) & 15;

			
			float ix = (int) playerX & 63;
			float iy = (int) playerY & 63;


			var ax = (float) Math.cos(playerAngle);
			var ay = (float) Math.sin(playerAngle);


			var bx = playerX + 256 * ax;
			var by = playerY + 256 * ay;


			var cx = bx - 128 * ay;
			var cy = by + 128 * ax;


			var ex = ay;
			var ey = -ax;

			
			for (i = 0; i < 256; i++) {

				
				for (y = 0; y < 256; y++) {
					for (x = 0; x < 256; x++) {
						zbuffer[y][x] = 0x1.fffffeP+127f;
					}
				}


				var fx = cx + i * ex - playerX;
				var fy = cy + i * ey - playerY;
				var mag = (float) Math.sqrt(fx * fx + fy * fy);
				fx /= mag;
				fy /= mag;


				var hx = fx > 0 ? 1 : -1;
				var hy = fy > 0 ? 1 : -1;


				var jx = 64 / fx;
				var jy = 64 / fy;
				if (jx < 0) {
					jx = -jx;
				}
				if (jy < 0) {
					jy = -jy;
				}


				var kx = (fx >= 0 ? 64 - ix : -ix) / fx;
				var ky = (fy >= 0 ? 64 - iy : -iy) / fy;


				var lx = gx;
				var ly = gy;

				
				float t = 0;

				var xHit = false;

				
				while (map[ly & 15][lx & 15] != 1) {


					var objects = objectMap[ly & 15][lx & 15];
					if (objects.size() > 0) {
						for (k = 0; k < objects.size(); k++) {
							var object = objects.get(k);

							float ox = 0;
							float oy = 0;
							float qx = 0;
							float qy = 0;

							if (object[OBJ_PARALLEL] == PARALLEL_NOT) {

								
								ox = object[OBJ_X] - 0.5f * object[OBJ_WIDTH] * ay;
								oy = object[OBJ_Y] + 0.5f * object[OBJ_WIDTH] * ax;

								
								qx = object[OBJ_WIDTH] * ay;
								qy = -object[OBJ_WIDTH] * ax;
							} else if (object[OBJ_PARALLEL] == PARALLEL_X) {
								
								ox = object[OBJ_X] - 0.5f * object[OBJ_WIDTH];
								oy = object[OBJ_Y];

								
								qx = object[OBJ_WIDTH];
							} else {
								
								ox = object[OBJ_X];
								oy = object[OBJ_Y] - 0.5f * object[OBJ_WIDTH];

								
								qy = object[OBJ_WIDTH];
							}


							var rx = playerX - ox;
							var ry = playerY - oy;
							var det = fx * qy - fy * qx;
							var S = (fx * ry - fy * rx) / det;
							var T = (qx * ry - qy * rx) / det;
							if (S >= 0 && S < 1 && T > 0) {
								var spriteX = (int) (object[OBJ_FLIPPED] == 1 ? object[OBJ_WIDTH] - object[OBJ_WIDTH] * S : object[OBJ_WIDTH] * S);

								var h1 = 128 + mag * (32 - object[OBJ_HEIGHT]) / T;
								var h2 = 128 + mag * 32 / T;

								var hs = h2 - h1;
								var jMin = (int) h1;
								var jMax = (int) h2;

								for (j = jMin; j <= jMax; j++) {
									if (j >= 0 && j < 256 && zbuffer[i][j] > T) {
										var spriteY = (int) (object[OBJ_HEIGHT] * (j - jMin) / hs);
										if (spriteX >= 0 && spriteY >= 0 && spriteX < object[OBJ_WIDTH] && spriteY < object[OBJ_HEIGHT] && sprites[(int) object[OBJ_SPRITE]][spriteY][spriteX] >= 0) {
											pixels[(j << 8) + i] = sprites[(int) object[OBJ_SPRITE]][spriteY][spriteX];
											zbuffer[i][j] = T;
										}
									}
								}
							}
						}
					}

					if (kx < ky) {
						t = kx;
						kx += jx;
						lx += hx;
						xHit = true;
					} else {
						t = ky;
						ky += jy;
						ly += hy;
						xHit = false;
					}
				}


				var mx = playerX + fx * t;
				var my = playerY + fy * t;


				var textureX = (int) (xHit ? my : mx) & 63;
				if (xHit ? fx > 0 : fy < 0) {
					textureX = 63 - textureX;
				}


				var halfHeight = 32 * mag / t;

				
				float textureY = 0;
				var textureVy = t / mag;


				var jMin = 128 - (int) halfHeight;
				var jMax = 255 - jMin;

				
				for (j = jMin; j <= jMax; j++, textureY += textureVy) {
					if (j >= 0 && j < 256) {
						if (t < zbuffer[i][j]) {
							pixels[(j << 8) + i] = textures[lx > 8 ? ly < 8 ? TEXTURE_GRAY_BRICK_WALL : TEXTURE_GRAY_SWASTIKA_WALL : ly < 8 ? TEXTURE_BRICK_WALL : TEXTURE_SWASTIKA_WALL][(int) textureY & 63][textureX];
						}
					}
				}
			}

			

			if (playerFiring > 0) {
				
				for (y = 0; y < 64; y++) {
					for (x = 0; x < 64; x++) {
						if (sprites[SPRITE_FIRE][y >> 2][x >> 2] >= 0) {
							pixels[((y + 182) << 8) + x + 96] = sprites[SPRITE_FIRE][y >> 2][x >> 2];
						}
					}
				}
			}

			
			for (y = 0; y < 42; y++) {
				for (x = 0; x < 32; x++) {
					if (sprites[SPRITE_GUN][y >> 1][x >> 1] >= 0) {
						pixels[((y + 214) << 8) + x + 112] = sprites[SPRITE_GUN][y >> 1][x >> 1];
					}
				}
			}

			if (playerHasKey) {
				
				for (y = 0; y < 16; y++) {
					for (x = 0; x < 32; x++) {
						if (sprites[SPRITE_KEY][y >> 1][x >> 1] >= 0) {
							pixels[((y + 231) << 8) + x + 8] = sprites[SPRITE_KEY][y >> 1][x >> 1];
						}
					}
				}
			}

			
			image.setRGB(0, 0, 256, 256, pixels, 0, 256);

			
			g.setColor(Color.RED);
			g.drawRect(183, 239, 64, 8);
			g.fillRect(183, 239, playerHealth, 8);

			
			if (playerDying > 0) {
				g.setColor(reds[playerDying]);
				g.fillRect(0, 0, 256, 256);
			}

			
			if (playerWinning > 0) {
				g.setColor(blacks[playerWinning >= 256 ? 511 - playerWinning : playerWinning]);
				g.fillRect(0, 0, 256, 256);
			}

			

			
			if (g2 == null) {
				g2 = (Graphics2D) getGraphics();
				requestFocus();
			} else {
				g2.drawImage(image, 0, 0, 512, 512, null);
			}

			
			while (nextFrameStartTime - System.nanoTime() > 0) {
				Thread.yield();
			}
		}
	}

	@Override
    public void processKeyEvent(KeyEvent keyEvent) {

		var k = keyEvent.getKeyCode();
		if (k > 0) {
            final var VK_D = 0x44;
            final var VK_A = 0x41;
            final var VK_S = 0x53;
            final var VK_W = 0x57;
            final var VK_DOWN = 0x28;
            final var VK_LEFT = 0x25;
            switch (k) {
                case VK_W:
                    final var VK_UP = 0x26;
                    k = VK_UP;
                    break;
                case VK_D:
                    final var VK_RIGHT = 0x27;
                    k = VK_RIGHT;
                    break;
                case VK_A:
                    k = VK_LEFT;
                    break;
                case VK_S:
                    k = VK_DOWN;
                    break;
            }
            final var VK_SPACE = 0x20;
            final var VK_SHOOT = 0x42;
            a[(k >= VK_LEFT && k <= VK_DOWN) || k == VK_SPACE ? k : VK_SHOOT] = keyEvent.getID() != 402;
		}
	}

	
	public static void main(String[] args) throws Throwable {
		var frame = new javax.swing.JFrame("Wolfenstein 4K");
	  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		var applet = new a();
	  applet.setPreferredSize(new java.awt.Dimension(512, 512));
	  frame.add(applet, java.awt.BorderLayout.CENTER);
	  frame.setResizable(false);
	  frame.pack();
	  frame.setLocationRelativeTo(null);
	  frame.setVisible(true);
	  Thread.sleep(250);
	  applet.start();
	}
}
