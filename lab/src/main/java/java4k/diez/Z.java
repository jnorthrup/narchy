package java4k.diez;

/*
 * Zombie 4k Version 1.1
 *
 * Copyright 2012, Alan Waddington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * The names of its contributors may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java4k.GamePanel;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Z extends GamePanel {

	
	
	

	private final static int SCREENHEIGHT = 400; 
	private final static int SCREENWIDTH = 600; 
	private final static float SCREENDEPTH = 693f; 
	private final static int DEPTH = 750; 
	private final static float DEPTHRATIO = 1.5f; 

	private final static int MAPSIZE = 0x100; 
	private final static int TILETYPES = 5; 
	private final static int TILESIZE = 0x100; 
	private final static int ROADGRIDSIZE = 16; 
	private final static float PLAYERHEIGHT = 1.2f; 

	private final static int GRASS = 0; 
	private final static int FOREST = 1; 
	private final static int ROAD = 2; 
	private final static int BUILDING = 3; 
	private final static int BUILDINGINSIDE = 4; 

	private final static float DEGREES = 18000f / (float) Math.PI;
	private final static float ROTATERATE = 1e-9f; 
	private final static float TRANSLATERATE = 2e-7f; 

	private final static int ENEMIES = 64; 
	private final static int SUPPLIES = 32; 
	private final static int ENTITIES = ENEMIES + SUPPLIES;

	private final static int ENTITYTYPES = 2; 
	private final static int ENTITYZOMBIE = 0; 
	private final static int ENTITYBOX = 1; 

	private final static int ENTITYSIZE = 7; 
	private final static int ENTITYCOLOURS = 6; 
	private final static float ENTITYDEPTH = 9e6f; 
	private final static float ENTITYCOLLISION = 400f; 
	private final static float ENTITYRADIUS = 32f / TILESIZE * 32f / TILESIZE / 2f * 1e6f; 
	private final static float ENTITYCENTRE = ENTITYSIZE / 2 + 0.5f;
	private final static int ENTITYSCALE = 2; 

	private final static int ZOMBIEDEPTH = 3; 
	private final static int ZOMBIEWIDTH = 7; 
	private final static int ZOMBIEHEIGHT = 15; 

	private final static int DEAD = 0; 
	private final static int IDLE = 1; 
	private final static int ATTACK = 2; 

	private final static float SUPPLIESBOOST = 10f; 
	private final static float BLOODLOSS = 50f; 
	private final static float WATERLOSS = 25f; 
	private final static float STARVATION = 10f; 
	private final static float SUPPLYUSAGE = 5e-10f;
	private final static float ZOMBIEDAMAGE = 2e-8f; 
	private final static float ATTACKRANGE = 5e3f; 
	private final static float SPAWNOUTSIDE = 0.01f; 
	private final static long DEADTIME = 0x100000000L; 

	private final static float DETECTRANGE = 5e4f; 
	private final static float FORGETRANGE = 5e6f; 

	private final static int SOUNDS = 6; 
	private final static float RATE = 16000f;
	private final static float AMPLITUDE = 16000f; 

	private final static int PLAYERATTACKSOUND = 0; 
	private final static int BOXFOUNDSOUND = 1;
	private final static int ZOMBIEALERTSOUND = 2;
	private final static int ZOMBIEATTACKSOUND = 3;
	private final static int AMBIENTSOUND1 = 4;
	private final static int AMBIENTSOUND2 = 5;

	
	
	

	float[] sin = new float[36000]; 
	float[] cos = new float[36000]; 
	float[] rayAngleFix = new float[SCREENWIDTH]; 
	float[] cosRayAngleFix = new float[SCREENWIDTH]; 
	float[] zMap = new float[DEPTH]; 

	float[] heightX = new float[MAPSIZE * TILESIZE];
	float[] heightZ = new float[MAPSIZE * TILESIZE];

	float[][][] tileHeight = new float[TILETYPES][TILESIZE][TILESIZE]; 
	int[][][] tileColour = new int[TILETYPES][TILESIZE][TILESIZE]; 

	int[][] map = new int[MAPSIZE][MAPSIZE]; 
	float[] gauge = new float[3]; 
	byte[][] audioData = new byte[SOUNDS][]; 
	Clip[] audio = new Clip[SOUNDS];


    boolean[] keyboard = new boolean[0x10000];

	
	int i, j, k, m, n;
	float x, y, z, r;
	float dx, dz;

	int buildings;
	int[] buildingX;
	int[] buildingZ;

	
	long time; 
	long lastTime; 
	long deltaTime; 
	long deadTime; 

	
	float playerX, playerY, playerZ; 
	float playerA, sinPlayerA, cosPlayerA; 
	boolean attacking, lastAttacking; 
	int blood; 

	
	int px, pz; 
	int ex, ez; 
	int tx, tz; 

	
	int c; 
	int tileType; 
	float rx, rz; 
	boolean draw; 
	boolean outside; 
	boolean drawEntity; 

	
	float depth; 
	int ray; 
	float rayAngle; 
	float cosRayAngle; 
	float sinRayAngle; 

	
	int dst; 
	int sy, lsy, msy; 
	float[] heightY = new float[DEPTH]; 
	float[] heightYT = new float[DEPTH]; 
	int[] heightSY = new int[DEPTH]; 

	
	int[] drawList1 = new int[ENTITIES];
	int[] drawList2 = new int[ENTITIES];
	int drawCount1, drawCount2;

	
	int[] entityS = new int[ENTITIES]; 
	float[] entityA = new float[ENTITIES]; 
	float[] entityX = new float[ENTITIES]; 
	float[] entityZ = new float[ENTITIES]; 
	int[] entityLTX = new int[ENTITIES]; 
	int[] entityLTZ = new int[ENTITIES]; 
	float[] entityWX = new float[ENTITIES]; 
	float[] entityWZ = new float[ENTITIES]; 
	
	int[][][][] entityHeight = new int[ENTITYSIZE][ENTITYSIZE][ENTITYCOLOURS][ENTITYTYPES];
	int[][][][] entityColour = new int[ENTITYSIZE][ENTITYSIZE][ENTITYCOLOURS][ENTITYTYPES];
	int[][][] entityColours = new int[ENTITYSIZE][ENTITYSIZE][ENTITYTYPES];

	
	boolean[] sound = new boolean[SOUNDS]; 
	int[] audioLoop = new int[SOUNDS]; 

	BufferedImage screen;
	int[] screenData;

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(SCREENWIDTH, SCREENHEIGHT);
	}

	public Z() {
		super(true);

		
		screen = new BufferedImage(SCREENWIDTH, SCREENHEIGHT, BufferedImage.TYPE_INT_RGB);
		screenData = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();

		
		for (i = 0; i < 36000; i++) { 
			cos[i] = (float) Math.cos(i / DEGREES);
			sin[(i + 9000) % 36000] = cos[i];
		}
		for (i = 0; i < SCREENWIDTH; i++) { 
			rayAngleFix[i] = (float) Math.atan2((SCREENWIDTH / 2 - i), SCREENDEPTH);
			cosRayAngleFix[i] = (float) Math.cos(rayAngleFix[i]);
		}
		for (i = 0; i < DEPTH; i++)
			
			zMap[i] = DEPTH * DEPTHRATIO / (DEPTH * DEPTHRATIO - i) - 1f;

		heightSY[0] = SCREENHEIGHT - 1; 

		
		
		

		for (i = 0; i < SOUNDS; i++) {
			
			switch (i) {
			case PLAYERATTACKSOUND:
				r = 0.1f;
				break;
			case ZOMBIEATTACKSOUND:
			case AMBIENTSOUND2:
				r = 0.25f;
				break;
			default:
				r = 1f;
			}
			audioData[i] = new byte[2 * (int) (r * RATE)];
			for (j = 0; j < r * RATE; j++) {
				x = j / (r * RATE); 
				y = 2 * x - 1f;
				z = (float) Math.PI * j / RATE;
				
				switch (i) {
				case PLAYERATTACKSOUND: 
					k = (int) (AMPLITUDE / 5f * Math.random());
					break;
				case BOXFOUNDSOUND: 
					k = (int) (AMPLITUDE / 5f * (1f - x) * Math.cos(500 * z));
					break;
				case ZOMBIEALERTSOUND: 
					k = (int) (AMPLITUDE / 2f * Math.cos((300 - 20 * y * y) * z));
					break;
				case ZOMBIEATTACKSOUND: 
					k = (int) (AMPLITUDE / 2f * Math.cos((500 - 200 * x) * z) * (Math.cos(100 * z)));
					break;
				case AMBIENTSOUND1: 
					k = (int) (AMPLITUDE / 2f * Math.cos((250 - 20 * y * y) * z));
					break;
				default: 
					k = (int) (AMPLITUDE / 20f * x * Math.cos((500 + 500 * x) * z));
				}
				
				if (j < 1000)
					k *= j / 1000f;
				if (r * RATE - j < 1000)
					k *= (r * RATE - j) / 1000f;
				
				audioData[i][2 * j + 1] = (byte) (k & 0xff);
				audioData[i][2 * j] = (byte) ((k >> 8) & 0xff);
			}
		}

		
		try {
			AudioFormat audioFormat = new AudioFormat(RATE, 16, 1, true, true);
			DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
			for (i = 0; i < SOUNDS; i++) {
				audio[i] = (Clip) AudioSystem.getLine(info);
				audio[i].open(audioFormat, audioData[i], 0, audioData[i].length);
			}
		} catch (Exception e) {
		}

		
		
		

		
		for (i = 0; i < 0x10000; i++) {
			x = 2f * (float) Math.PI / 0x10000 * i;
			heightX[i] = (float) (1.0 - Math.cos(x) - 0.2 * Math.cos(x * 5f)) / 2f;
			heightZ[i] = (float) (1.0 - Math.cos(x) - 0.2 * Math.cos(x * 7f)) / 2f;
		}

		
		for (i = 0; i < TILETYPES; i++) {
			for (j = 0; j < TILESIZE; j++) {
				for (k = 0; k < TILESIZE; k++) {
					
					x = 0.5f - j / (float) TILESIZE;
					if (x < 0)
						x = -x;
					z = 0.5f - k / (float) TILESIZE;
					if (z < 0)
						z = -z;
					
					switch (i) {
					case GRASS:
					case FOREST:
						tileHeight[i][j][k] = (float) (Math.random() - 0.5);
						tileColour[i][j][k] = 0x000100 * (16 + (3 * j + 17 * k) % 32);
						if (i == FOREST) {
							y = 100f * ((0.5f - x) * (0.5f - z) - 0.15f);
							if (y > 0) {
								tileHeight[i][j][k] = y;
								tileColour[i][j][k] = 0x010100 + 0x000100 * (int) (15.0 * Math.random());
							}
						}
						break;
					case ROAD:
						tileColour[i][j][k] = 0x202020 + 0x010101 * (int) (15.0 * Math.random());
						break;
					case BUILDING:
					case BUILDINGINSIDE:
						tileHeight[i][j][k] = (3f * (1f - x) * (1f - z) + 1f);
						tileColour[i][j][k] = 0x404040 + (j > k ? 0x040404 : 0) + (j < TILESIZE - k - 1 ? 0x020202 : 0);
						if ((k > TILESIZE / 2 - 10 && k < TILESIZE / 2 + 10) && ((i == BUILDING && (j < 10 || j >= TILESIZE - 10)) || (i == BUILDINGINSIDE && (j >= 10 && j < TILESIZE - 10)))) {
							tileHeight[i][j][k] = 1.6f; 
							tileColour[i][j][k] = 0x010000;
						}
						if ((j > TILESIZE / 2 - 10 && j < TILESIZE / 2 + 10) && ((i == BUILDING && (k < 10 || k >= TILESIZE - 10)) || (i == BUILDINGINSIDE && (k >= 10 && k < TILESIZE - 10)))) {
							tileHeight[i][j][k] = 1.6f; 
							tileColour[i][j][k] = 0x010000;
						}
						if (j > 20 && j < TILESIZE - 20 && k > 20 && k < TILESIZE - 20)
							if (i == BUILDING) {
								tileColour[i][j][k] = 0x402020; 
							} else { 
								tileHeight[i][j][k] = 0;
								tileColour[i][j][k] = 0x401010 + 0x010101 * (int) (10.0 * Math.random());
							}
					}
				}
			}
		}

		
		
		
		

		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		

		
		
		
		
		
		String zombie = "000000844444000oDDDDDDDDDDDD00oDDDDDEEGGGGFjo"
		
		
		
		
		
				+ "000000EEGGGGFjo"

				
				
				
				
				
				+ "oDDDDDEEGGGGFjooDDDDDDDDDDDD00000000844444000";

		
		for (i = 0; i < ZOMBIEDEPTH; i++)
			for (j = 0; j < ZOMBIEWIDTH; j++) {
				n = -1; 
				c = -1;
				for (k = 0; k < ZOMBIEHEIGHT; k++) {
					
					m = ((zombie.charAt(k + ZOMBIEHEIGHT * j) - 48) >> (2 * i)) & 3;
					if (c != m) {
						n++; 
						entityColour[j][i + 2][n][ENTITYZOMBIE] = c = m;
					}
					entityHeight[j][i + 2][n][ENTITYZOMBIE] = k + 1; 
				}
				entityColours[j][i + 2][ENTITYZOMBIE] = n + (c > 0 ? 1 : 0);
			}

		
		
		
		for (i = 0; i < ENTITYSIZE; i++)
			for (j = 0; j < ENTITYSIZE; j++) {
				
				for (k = 0; k < 3; k += 2) {
					entityColour[j][i][k][ENTITYBOX] = (i == 0 || i == 6 || j == 0 || j == 6) ? 1 : 3;
					entityHeight[j][i][k][ENTITYBOX] = (k == 0) ? 1 : 5;
				}
				
				entityColour[j][i][1][ENTITYBOX] = ((i == 0 || i == 6) && (j == 0 || j == 6)) ? 1 : 2;
				entityHeight[j][i][1][ENTITYBOX] = 4;
				entityColours[j][i][ENTITYBOX] = 3;
			}

		
		
		

		
		for (i = ROADGRIDSIZE; i < MAPSIZE - ROADGRIDSIZE; i += MAPSIZE / ROADGRIDSIZE)
			for (j = ROADGRIDSIZE; j < MAPSIZE - ROADGRIDSIZE; j += MAPSIZE / ROADGRIDSIZE) {
				draw = Math.random() > 0.9; 
				if (Math.random() > 0.5 || draw) {
					for (k = 0; k <= ROADGRIDSIZE; k++)
						map[i + k][j] = ROAD;
					if (Math.random() > 0.5 || draw)
						map[i + 1][j - 1] = BUILDING;
					if (Math.random() > 0.5 || draw)
						map[i + 1][j + 1] = BUILDING;
				}
				if (Math.random() > 0.5 || draw) {
					for (k = 0; k <= ROADGRIDSIZE; k++)
						map[i][j + k] = ROAD;
					if (Math.random() > 0.5 || draw)
						map[i - 1][j + 1] = BUILDING;
					if (Math.random() > 0.5 || draw)
						map[i + 1][j + 1] = BUILDING;
				}
				if (draw)
					for (k = 1; k < ROADGRIDSIZE; k += 2) {
						map[i + k][j - 1] = BUILDING;
						map[i + k][j + 1] = BUILDING;
						map[i - 1][j + k] = BUILDING;
						map[i + 1][j + k] = BUILDING;
					}
			}

		
		buildings = 0;
		for (i = 0; i < MAPSIZE; i++)
			for (j = 0; j < MAPSIZE; j++) {
				if (map[i][j] == GRASS && Math.random() > 0.5 && !(i == 0x80 && j == 0x80)) 
					map[i][j] = FOREST;
				if (map[i][j] == BUILDING)
					buildings++;
			}

		
		buildingX = new int[buildings];
		buildingZ = new int[buildings];
		buildings = 0;
		for (i = 0; i < MAPSIZE; i++)
			for (j = 0; j < MAPSIZE; j++)
				if (map[i][j] == BUILDING) {
					buildingX[buildings] = i;
					buildingZ[buildings] = j;
					buildings++;
				}

		time = System.nanoTime();

	}

	@Override
	public void paintComponent(Graphics gs) {

		lastTime = time;
		time = System.nanoTime();
		deltaTime = time - lastTime;

		
		
		

		
		if (deadTime >= 0) {
			deadTime -= deltaTime;
			playerX = playerZ = 0x8080; 
			playerA = 0; 
			blood = 0; 
			for (i = 0; i < 3; i++)
				gauge[i] = 100f; 
			for (i = 0; i < ENTITIES; i++)
				entityS[i] = DEAD; 
		}

		
		
		

		
		if (keyboard[Event.LEFT] || keyboard['a']) 
			playerA += ROTATERATE * deltaTime;
		if (keyboard[Event.RIGHT] || keyboard['d'])
			playerA -= ROTATERATE * deltaTime;
		playerA = (playerA + 2f * (float) Math.PI) % (2f * (float) Math.PI);
		sinPlayerA = sin[(int) (DEGREES * playerA)];
		cosPlayerA = cos[(int) (DEGREES * playerA)];

		
		rx = playerX;
		rz = playerZ;
		r = deltaTime * TRANSLATERATE / (gauge[1] < STARVATION ? 2 : 1); 
		if (keyboard[Event.UP] || keyboard['w']) { 
			rx += -r * sinPlayerA;
			rz += -r * cosPlayerA;
		}
		if (keyboard[Event.DOWN] || keyboard['s']) { 
			rx += r * sinPlayerA;
			rz += r * cosPlayerA;
		}

		
		sound[PLAYERATTACKSOUND] = attacking = (draw = keyboard[' ']) && !lastAttacking;
		lastAttacking = draw;

		
		
		

		
		px = (int) rx & 0xffff;
		pz = (int) rz & 0xffff;
		tx = px & 0xff;
		tz = pz & 0xff;
		tileType = map[px >> 8][pz >> 8];
		playerY = 50f * (heightX[px] * heightZ[pz] - 0.1f);
		outside = true;
		if (playerY > -1) {
			if (tileType == FOREST) { 
				if (tx < 0x60 || tx > 0xA0 || tz < 0x60 || tz > 0xA0) {
					playerX = rx;
					playerZ = rz;
				}
			} else if (tileType == BUILDING) { 
				outside = false;
				if ((tx > 0x70 && tx < 0x90) || (tz > 0x70 && tz < 0x90) || (tx > 0x14 && tx < 0xEC && tz > 0x14 && tz < 0xEC)) {
					playerX = rx;
					playerZ = rz;
				}
			} else { 
				playerX = rx;
				playerZ = rz;
			}
		}

		
		px = (int) playerX & 0xffff;
		pz = (int) playerZ & 0xffff;
		playerY = 50f * (tileType == BUILDING ? (heightX[(px & 0xff00) + 0x80] 
				* heightZ[(pz & 0xff00) + 0x80] - 0.1f) : (heightX[px] * heightZ[pz] - 0.1f));

		
		
		

		drawCount1 = 0;
		for (i = 1; i < SOUNDS; i++)
			sound[i] = false;
		for (i = 0; i < ENTITIES; i++) {
			draw = drawEntity = false;
			if (entityS[i] > DEAD) {
				dx = entityX[i] - playerX;
				dz = entityZ[i] - playerZ;
				r = dx * dx + dz * dz;
				
				if (r < ENTITYDEPTH) {
					draw = true; 
					if (dx * sinPlayerA + dz * cosPlayerA < 0) { 
						drawEntity = true;
						drawList1[drawCount1++] = i;
					}
				}
				
				if (i < ENEMIES) {
					if (r < ATTACKRANGE && attacking && drawEntity) {
						attacking = false; 
						entityS[i] = DEAD;
						if (blood < 100)
							blood++;
					} else { 
						
						if (r < DETECTRANGE && entityS[i] != ATTACK) {
							entityS[i] = ATTACK;
							sound[ZOMBIEALERTSOUND] = true; 
						}
						if (r > FORGETRANGE)
							entityS[i] = IDLE;
						if (entityS[i] == ATTACK) {
							if ((int) ((time / 1e9) % 4) == 0)
								sound[ZOMBIEALERTSOUND] = true; 
							
							tx = (int) entityX[i] & 0xff00;
							tz = (int) entityZ[i] & 0xff00;
							rx = entityX[i] - entityWX[i];
							rz = entityZ[i] - entityWZ[i];
							if ((px & 0xff00) == tx && (pz & 0xff00) == tz) {
								entityWX[i] = playerX;
								entityWZ[i] = playerZ;
							} else {
								
								if (tx != entityLTX[i] || tz != entityLTZ[i]) {
									
									
									
									
									
									
									if (map[tx >> 8][tz >> 8] == FOREST ? ((int) entityWX[i] & 0xff) == 0x80 : dx * dx > dz * dz ^ Math.random() < 0.25) {
										if (dx > 0) { 
											entityWX[i] = tx;
											entityWZ[i] = tz + 0x80;
										} else {
											entityWX[i] = tx + 0x100;
											entityWZ[i] = tz + 0x80;
										}
									} else {
										if (dz > 0) { 
											entityWX[i] = tx + 0x80;
											entityWZ[i] = tz;
										} else {
											entityWX[i] = tx + 0x80;
											entityWZ[i] = tz + 0x100;
										}
									}
								}
							}
							
							for (j = 10; j > 0; j--) {
								x = sin[(int) (DEGREES * entityA[i])];
								z = cos[(int) (DEGREES * entityA[i])];
								y = rx * z - rz * x;
								if (y > 0)
									entityA[i] -= deltaTime * ROTATERATE * j / 10f;
								else
									entityA[i] += deltaTime * ROTATERATE * j / 10f;
								entityA[i] = (entityA[i] + 2f * (float) Math.PI) % (2f * (float) Math.PI);
							}
							
							if (r > ENTITYCOLLISION) {
								entityLTX[i] = (int) entityX[i] & 0xff00;
								entityLTZ[i] = (int) entityZ[i] & 0xff00;
								entityX[i] += deltaTime * TRANSLATERATE * x / 2;
								entityZ[i] += deltaTime * TRANSLATERATE * z / 2;
							}
						}
						
						entityX[i] += deltaTime * TRANSLATERATE * (Math.random() - 0.5f) / 10f;
						entityZ[i] += deltaTime * TRANSLATERATE * (Math.random() - 0.5f) / 10f;
						entityA[i] += deltaTime * ROTATERATE * (Math.random() - 0.5f);
						entityA[i] = (entityA[i] + 2f * (float) Math.PI) % (2f * (float) Math.PI);
					}
				}
				
				if (r < ENTITYCOLLISION) {
					if (i < ENEMIES) { 
						sound[ZOMBIEATTACKSOUND] = true; 
						gauge[2] -= deltaTime * ZOMBIEDAMAGE;
					} else { 
						j = (i & 3) % 3;
						entityS[i] = DEAD;
						sound[BOXFOUNDSOUND] = true; 
						gauge[j] += SUPPLIESBOOST;
						if (gauge[j] > 100f)
							gauge[j] = 100f;
					}
				}
			}
			
			if (!draw && i >= ENEMIES) { 
				
				n = (int) (buildings * (float) Math.random());
				x = TILESIZE * (buildingX[n] + 0.1f + 0.8f * (float) Math.random());
				z = TILESIZE * (buildingZ[n] + 0.1f + 0.8f * (float) Math.random());
				dx = x - playerX;
				dz = z - playerZ;
				if (dx * dx + dz * dz < ENTITYDEPTH) { 
					entityX[i] = x;
					entityZ[i] = z;
					entityLTX[i] = (int) entityX[i] & 0xff00;
					entityLTZ[i] = (int) entityZ[i] & 0xff00;
					entityS[i] = IDLE;
					draw = true;
				}
				
				if (Math.random() > SPAWNOUTSIDE)
					draw = true;
			}
			if (!draw) { 
				
				ray = (int) (36000 * Math.random());
				r = TILESIZE * (3f * (float) Math.random() + 6f);
				entityX[i] = playerX + r * sin[ray];
				entityZ[i] = playerZ + r * cos[ray];
				entityS[i] = IDLE;
				if (i < ENEMIES)
					entityA[i] = (int) (36000 * Math.random());
			}
		}

		
		
		

		
		for (ray = 0; ray < SCREENWIDTH; ray += 2) {
			rayAngle = playerA + rayAngleFix[ray];
			rayAngle = (rayAngle + 2f * (float) Math.PI) % (2f * (float) Math.PI);
			cosRayAngle = cos[(int) (DEGREES * rayAngle)] / cosRayAngleFix[ray];
			sinRayAngle = sin[(int) (DEGREES * rayAngle)] / cosRayAngleFix[ray];

			
			drawCount2 = 0;
			for (i = 0; i < drawCount1; i++) {
				
				x = entityX[drawList1[i]] - playerX;
				z = entityZ[drawList1[i]] - playerZ;
				r = x * cosRayAngle - z * sinRayAngle;
				if (r * r < ENTITYRADIUS)
					drawList2[drawCount2++] = drawList1[i];
			}

			
			for (i = 1; i < DEPTH; i++) {
				
				depth = zMap[i];
				x = playerX - sinRayAngle * depth * 1000f;
				z = playerZ - cosRayAngle * depth * 1000f;

				
				tx = (int) x & 0xffff;
				tz = (int) z & 0xffff;
				tileType = map[tx >> 8][tz >> 8];
				if (tileType == BUILDING && (px & 0xff00) == (tx & 0xff00) && (pz & 0xff00) == (tz & 0xff00))
					tileType = BUILDINGINSIDE;

				
				y = 50f * (tileType == BUILDINGINSIDE ? (heightX[(tx & 0xff00) + 0x80] 
						* heightZ[(tz & 0xff00) + 0x80] - 0.1f) : (heightX[tx] * heightZ[tz] - 0.1f));
				heightY[i] = y;

				
				if (y > -1f)
					y += tileHeight[tileType][tx & 0xff][tz & 0xff];
				
				if (y < 0f)
					y = 0f;
				heightYT[i] = y;

				
				sy = SCREENHEIGHT / 2 - (int) (20f * (y - playerY - PLAYERHEIGHT) / depth);
				if (sy < 0)
					sy = 0;
				
				
				if (sy > heightSY[i - 1])
					sy = heightSY[i - 1];
				heightSY[i] = sy;
			}

			
			msy = SCREENHEIGHT - 1; 
			for (i = DEPTH - 1; i > 0; i--) {
				
				depth = zMap[i];
				x = playerX - sinRayAngle * depth * 1000f;
				z = playerZ - cosRayAngle * depth * 1000f;

				
				tx = (int) x & 0xffff;
				tz = (int) z & 0xffff;
				tileType = map[tx >> 8][tz >> 8];
				if (tileType == BUILDING && (px & 0xff00) == (tx & 0xff00) && (pz & 0xff00) == (tz & 0xff00))
					tileType = BUILDINGINSIDE;

				
				
				

				
				if (heightYT[i] <= 0) 
					c = 0x000020 + 0x010101 * (0x20 * i) / DEPTH;
				else { 
					c = tileColour[tileType][tx & 0xff][tz & 0xff];
					
					if ((tileType == GRASS || tileType == FOREST) && ((tx & 0x100) == 0) ^ ((tz & 0x100) == 0))
						c += 0x100800;
				}
				c = (c / 2) & 0x7f7f7f; 

				if (gauge[2] < BLOODLOSS) 
					c = 0x010101 * (((c >> 16) & 0xff) + ((c >> 8) & 0xff) + (c & 0xff));

				
				draw = outside || tileType == BUILDINGINSIDE;

				
				sy = heightSY[i];
				if (draw && sy < heightSY[i - 1]) {
					dst = ray + heightSY[i - 1] * SCREENWIDTH;
					for (k = heightSY[i - 1]; k > sy; k--) {
						screenData[dst] = screenData[dst + 1] = c;
						dst -= SCREENWIDTH;
					}
				}

				
				drawEntity = false;
				for (j = 0; j < drawCount2; j++) {
					k = drawList2[j];
					dx = entityX[k] - x;
					dz = entityZ[k] - z;

					
					if (dx * dx + dz * dz < ENTITYRADIUS) {
						m = (int) (DEGREES * entityA[k]);

						
						dx = dx / ENTITYSCALE;
						dz = dz / ENTITYSCALE;

						
						ex = (int) (dx * cos[m] - dz * sin[m] + ENTITYCENTRE + 1) - 1;
						if (ex < 0 || ex >= ENTITYSIZE)
							continue;
						ez = (int) (dx * sin[m] + dz * cos[m] + ENTITYCENTRE + 1) - 1;
						if (ez < 0 || ez >= ENTITYSIZE)
							continue;

						
						n = k < ENEMIES ? ENTITYZOMBIE : ENTITYBOX;
						if (entityColours[ex][ez][n] > 0) {
							drawEntity = true;
							break; 
						}
					}
				}

				
				
				

				if (drawEntity) {
					
					lsy = heightSY[i - 1];
					for (j = 0; j < entityColours[ex][ez][n]; j++) {
						
						y = heightY[i] + 0.045f * (float) entityHeight[ex][ez][j][n] * ENTITYSCALE;
						m = entityColour[ex][ez][j][n];
						if (n == ENTITYZOMBIE)
							switch (m) { 
							case 0:
								c = 0x303050; 
								break;
							case 1: 
								c = 0x101020 + 0x080808 * (k & 3);
								break;
							case 2: 
								c = 0x203008;
								break;
							default: 
								c = 0x100808;
							}
						else
							switch (m) { 
							case 0:
								c = 0x303050; 
								break;
							case 1: 
								c = 0;
								break;
							case 2: 
								c = 0x100404;
								break;
							default: 
								c = 0x000040 << (8 * ((k & 3) % 3));
							}
						if (gauge[2] < BLOODLOSS) 
							c = 0x010101 * (((c >> 16) & 0xff) + ((c >> 8) & 0xff) + (c & 0xff));

						
						sy = SCREENHEIGHT / 2 - (int) (20f * (y - playerY - PLAYERHEIGHT) / depth);
						if (sy < 0)
							sy = 0;
						if (sy > SCREENHEIGHT - 1)
							sy = SCREENHEIGHT - 1;

						
						if (m == 0)
							lsy = msy;

						
						if (draw && sy < lsy) {
							dst = ray + lsy * SCREENWIDTH;
							for (m = lsy; m > sy; m--) {
								screenData[dst] = screenData[dst + 1] = c;
								dst -= SCREENWIDTH;
							}
						}
						lsy = sy;
					}
				}
				
				
				if (draw && sy < msy)
					msy = sy;
			}

			
			if (outside) { 
				m = 0x202040;
				n = 0x010101;
			} else { 
				m = 0x100808;
				n = 0;
			}
			if (gauge[2] < BLOODLOSS) { 
				m = outside ? 0xC0C0C0 : 0x101010;
				n = 0;
			}
			dst = ray + msy * SCREENWIDTH;
			for (j = msy; j >= 0; j--) {
				k = j > 380 ? 380 : j;
				screenData[dst] = screenData[dst + 1] = m + n * (k / 12);
				dst -= SCREENWIDTH;
			}
		}

		
		
		

		
		if (playerX != 0x8080 || playerZ != 0x8080) {
			gauge[0] -= deltaTime * SUPPLYUSAGE;
			gauge[1] -= deltaTime * SUPPLYUSAGE / 2;
			gauge[2] -= gauge[0] < WATERLOSS ? deltaTime * SUPPLYUSAGE * 2 : 0;
		}

		
		for (i = 0; i < 3; i++) {
			if (gauge[i] < 0) {
				if (deadTime <= 0)
					deadTime = DEADTIME;
				gauge[i] = 0; 
			}
			for (j = 0; j < 100; j++) {
				c = j < gauge[i] ? (0xff << (8 * i)) : 0;
				for (k = 0; k < 8; k++) {
					screenData[SCREENWIDTH * (10 + 10 * i + k) + 10 + j] = c;
				}

			}
		}

		
		draw = gauge[2] < BLOODLOSS; 
		for (i = 0; i < 150; i++) {
			for (j = 0; j < i; j++) {
				n = -80 + i + (keyboard[' '] ? 0 : 100) + SCREENWIDTH / 2;
				m = 40 + j + (keyboard[' '] ? 350 : 16);
				if (m < SCREENHEIGHT)
					screenData[SCREENWIDTH * m + n] = draw ? 0 : 0x040208 + 0x010101 * (150 - i + j) / 10 + 0x010000 * ((300 - i + j) * blood / 500);
				else
					break;
			}
		}
		
		for (i = 0; i < SCREENWIDTH / 2; i++) {
			n = SCREENWIDTH / 2 + i + (keyboard[' '] ? 0 : 100);
			if (n >= SCREENWIDTH)
				break;
			for (j = 0; j < 32 + i / 10; j++) {
				m = i - j + (keyboard[' '] ? 350 : 16);
				if (m >= 0 && m < SCREENHEIGHT && j < 2 * i) {
					screenData[SCREENWIDTH * m + n] = draw ? 0 : 0x040208 + 0x010101 * j + 0x010000 * ((300 - i + j) * blood / 1000);
				}
			}
		}

		
		if ((int) ((time / 1e9) % 9) == 0)
			sound[AMBIENTSOUND1] = true;
		if ((int) ((time / 1e9) % 25) == 0)
			sound[AMBIENTSOUND2] = true;

		
		for (i = 0; i < SOUNDS; i++) {
			if (sound[i] && !audio[i].isActive()) {
				audio[i].loop(audioLoop[i]);
				audioLoop[i] = 1;
			}
		}

		
		
		

		if (deadTime < 0 && gs != null) {
			gs.drawImage(screen, 0, 0, null);
		}
		Thread.yield();

	}

	@Override
	public void processAWTEvent(AWTEvent awtEvent) {
		if (awtEvent instanceof KeyEvent) {
			KeyEvent e = (KeyEvent) awtEvent;
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				keyboard[e.getKeyChar()] = true;
			} else if (e.getID() == KeyEvent.KEY_RELEASED) {
				keyboard[e.getKeyChar()] = false;
			}
		}
	}
}
