package java4k.magewars4k;

/*
 * MageWars 4k
 *
 * Copyright 2010, Alan Waddington
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

/* Minor bugfix 22/02/11 */

import java.applet.Applet;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;





public class M extends Applet implements Runnable {

	
	
	

	private static final int PLAYERS = 4;
	private static final int ENTITIES = 21;
	private static final int VARIABLES = 8;
	private static final int IDSIZE = 4;

	
	private static final int PORT = 6789;
	private static final String GROUP = "228.5.6.7";
	private static final int RATE = 100000000;
	private static final long TIMEOUT = 1000000000;

	
	
	
	
	

	
	
	
	
	
	
	
	
	

	
	
	

	private static final int SCREENHEIGHT = 400;
	private static final int SCREENWIDTH = 600;
	private static final float SCREENDEPTH = 693f;
	private static final int DEPTH = 750;
	private static final float DEPTHRATIO = 1.3f;
	private static final float ENTITYDEPTH = 9f;
	private static final float PLAYERHEIGHT = 2f;
	private static final int TILETYPES = 4;
	private static final int TILESIZE = 1024;
	private static final int ENTITYSIZE = 8;
	private static final int MAPSIZE = 7;

	private static final int DAPPLE = 32;
	private static final float ENTITYRADIUS = 32f / TILESIZE * 32f / TILESIZE / 2f;
	private static final float COLLISIONRADIUS = 48f / TILESIZE * 48f / TILESIZE / 2f;
	private static final float CENTRE = ENTITYSIZE / 2 - 0.5f;
	private static final int ENTITYSCALE = 4;

	private static final int PLAIN = 0;
	private static final int GRASS = 1;
	private static final int MOUNTAIN = 2;
	private static final int CASTLE = 3;

	
	
	

	
	private static final float SCALEXYZ = TILESIZE * 100f;
	private static final float DEGREES = 18000f / (float) Math.PI;

	private static final float ROTATERATE = 1e-9f;
	private static final float TRANSLATERATE = 4e-10f;

	private static final long FIREBALLTIMEOUT1 = 2000000000l;
	private static final long FIREBALLTIMEOUT2 = 10000000l;
	private static final int ENEMIES = 6;
	private static final int PLAYERFIREBALLS = 8;

	private static final int PLAYERHEALTH = 100;
	private static final int ENEMYHEALTH = 20;

	private static final float TOOCLOSE = 100f / TILESIZE;
	private static final float TOOFAR = 400f / TILESIZE;

	private static final int X = 0;
	private static final int Y = 1;
	private static final int Z = 2;
	private static final int A = 3;
	private static final int T = 4;
	private static final int VX = 5;
	private static final int VY = 6;
	private static final int VZ = 7;

	
	
	

	
	private MulticastSocket socket;
    private DatagramPacket rxPacket;
	private final byte[] txMsg = new byte[IDSIZE + 4 * ENTITIES * VARIABLES];
	private final byte[] rxMsg = new byte[IDSIZE + 4 * ENTITIES * VARIABLES];
	private InetAddress group; 

	
	private final InetAddress[] address = new InetAddress[PLAYERS];
	private final int[] port = new int[PLAYERS];
	private final long[] timestamp = new long[PLAYERS];
	private final long[] process = new long[PLAYERS];
	private final int[][][] LANdata = new int[PLAYERS][ENTITIES][VARIABLES];
	private final int[][][] LANbuffer = new int[PLAYERS][ENTITIES][VARIABLES];

	
	private final int processID = (int) ((System.nanoTime() >> 16) & 0xffffffff);
	private boolean receiveThread; 

	
	
	

	private final float[] sin = new float[36000]; 
	private final float[] cos = new float[36000]; 
	private final float[] rayAngleFix = new float[SCREENWIDTH]; 
	private final float[] cosRayAngleFix = new float[SCREENWIDTH]; 
	private final float[] zMap = new float[DEPTH]; 
	private final float[][][] tiles = new float[TILETYPES][TILESIZE][TILESIZE]; 
	private final int[][] map = new int[MAPSIZE][MAPSIZE]; 

	
	
	

	private final float[][][] localData = new float[PLAYERS][ENTITIES][VARIABLES];
	private final long[] fireballTimeout = new long[ENEMIES + PLAYERFIREBALLS];
	private final boolean[] castle = new boolean[4];

	private final boolean[] keyboard = new boolean[0x10000]; 
	private boolean mouse; 
	private volatile int mouseX;
    private volatile int mouseY;

	@Override
	public void start() {
		new Thread(this).start(); 
	}

	@Override
    public void run() {


		while (!isActive())
			Thread.yield();
		long time;
		int y1;
		var x1 = 0;
		int k;
		int j;
		int i;
		if (receiveThread) {


			do {
				try {
					socket.receive(rxPacket);
					var pa = rxPacket.getAddress();
					j = rxPacket.getPort(); 
					
					k = ((rxMsg[3] & 0xff) << 24) + ((rxMsg[2] & 0xff) << 16) + ((rxMsg[1] & 0xff) << 8) + (rxMsg[0] & 0xff);
					
					if (k == processID)
						continue;
					time = System.nanoTime();
					
					x1 = 0; 
					y1 = 0; 
					for (i = 1; i < PLAYERS; i++) {
						if (address[i] != null && address[i].equals(pa) && port[i] == j && process[i] == k && timestamp[i] + TIMEOUT >= time)
							x1 = i; 
						if (timestamp[i] + TIMEOUT < time)
							y1 = i; 
					}
					
					if (x1 == 0) { 
						x1 = y1; 
						
						if (x1 != 0) {
							
							address[x1] = InetAddress.getByAddress(pa.getAddress());
							port[x1] = j; 
							process[x1] = k; 
						}
					}
					
					if (x1 != 0) {
						
						
						
						
						

						
						
						for (i = 0; i < ENTITIES; i++) {
							var LANvars = LANbuffer[x1][i];
							for (j = 0; j < VARIABLES; j++) {
								k = IDSIZE + 4 * (i * VARIABLES + j);
								LANvars[j] = ((rxMsg[k + 3] & 0xff) << 24) + ((rxMsg[k + 2] & 0xff) << 16) + ((rxMsg[k + 1] & 0xff) << 8) + (rxMsg[k] & 0xff);
							}
							LANbuffer[x1][i] = LANdata[x1][i];
							LANdata[x1][i] = LANvars; 
						}

						
						
						
						timestamp[x1] = time;
					}
				} catch (Exception e) {
				}
			} while (isActive());
			return;
		}

		
		
		

		
		
		

		try {
			
			socket = new MulticastSocket(PORT);
			group = InetAddress.getByName(GROUP);
			socket.joinGroup(group);
			
			txMsg[0] = (byte) (processID & 0xff);
			txMsg[1] = (byte) ((processID >> 8) & 0xff);
			txMsg[2] = (byte) ((processID >> 16) & 0xff);
			txMsg[3] = (byte) ((processID >> 24) & 0xff);
		} catch (Exception e) {
		}
		var txPacket = new DatagramPacket(txMsg, txMsg.length, group, PORT);
		rxPacket = new DatagramPacket(rxMsg, rxMsg.length, group, PORT);

		
		receiveThread = true;
		new Thread(this).start();


		var screen = new BufferedImage(SCREENWIDTH, SCREENHEIGHT, BufferedImage.TYPE_INT_RGB);
		var screenData = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();
		var gs = getGraphics();

		
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


		var cMap = new int[8];
		for (i = 0; i < 8; i++) {
			cMap[i] = 0x3fc000 * (i & 4) + 0x7f80 * (i & 2) + 0xff * (i & 1);
		}

		
		
		
		
		
		for (i = 0; i < MAPSIZE; i++) {
			for (j = 0; j < MAPSIZE; j++) {
				if ((i == 3 && (j <= 2 || j >= 4)) || ((i <= 2 || i >= 4) && j == 3))
					map[i][j] = MOUNTAIN; 
				if (((i == 1 || i == MAPSIZE - 2)) && (j == 1 || j == MAPSIZE - 2))
					map[i][j] = CASTLE; 
			}
		}


		float z;
		float y;
		for (i = 0; i < TILESIZE; i++) {
			var p1 = (float) Math.cos(i / (float) TILESIZE * 2 * (float) Math.PI);
			var m1 = (2f * i) / TILESIZE - 1f;
			if (m1 < 0)
				m1 = -m1;
			m1 = 3f * (1f - m1);
			for (j = 0; j < TILESIZE; j++) {
				var p2 = (float) Math.cos(j / (float) TILESIZE * 2 * (float) Math.PI);
				var m2 = (2f * j) / TILESIZE - 1f;
				if (m2 < 0)
					m2 = -m2;
				m2 = 3f * (1f - m2);
				
				y = 2f;
				if (p1 < y)
					y = p1; 
				if (p2 < y)
					y = p2; 
				tiles[PLAIN][i][j] = y;
				
				tiles[GRASS][i][j] = y + 0.2f * (float) Math.random();
				
				z = m1 + m2;
				tiles[MOUNTAIN][i][j] = (y > z) ? y : z; 
				
				z = 3f; 
				if (y < -1.75f / 2f)
					z = -1f; 
				if (p1 < 1.9f / 2f && p2 < 1.9f / 2f)
					z = -1f; 
				if (y > 1f / 2f)
					z = 10f; 
				tiles[CASTLE][i][j] = z;
			}
		}


		var entityHeight = new int[ENTITYSIZE][ENTITYSIZE];
		var entityColour = new int[ENTITYSIZE][ENTITYSIZE];


		var wizard = "000000000066660006777760067II760067YY7600567765004U44U40044444g0";
		for (i = 0; i < ENTITYSIZE; i++) {
			for (j = 0; j < ENTITYSIZE; j++) {
				k = wizard.charAt(i + ENTITYSIZE * j) - 48;
				entityHeight[i][j] = k & 15;
				entityColour[i][j] = k / 16;
			}
		}


		LANdata[0][0][T] = 7;


		requestFocus();
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));


		time = System.nanoTime();


		var allowSpawn = false;
		var health = new int[ENEMIES + 1];
		float angleY = 0;
		float playerZ = 0;
		float playerY = 0;
		float playerX = 0;
		var zBuffer = new float[SCREENHEIGHT * SCREENWIDTH];
		float rayAngle = 0;
		float[] entity1 = null;
		var drawList2 = new float[PLAYERS * (ENEMIES + 1)][];
		var drawList1 = new float[PLAYERS * (ENEMIES + 1)][];
		long sendTime = 0;
		var z1 = 0;
		do {
			var lastTime = time;
			time = System.nanoTime();
			var deltaTime = time - lastTime;
			/*            if (printTime+1000000000<time) {
			                printTime = time;
			                System.out.println("Frame:"+deltaTime/1000000);
			            }
			*/
			
			
			

			if (health[0] == 0) { 
				health[0] = PLAYERHEALTH;
				playerX = playerZ = 3.5f;
				angleY = 0.5f;
				for (i = 0; i < 4; i++)
					castle[i] = false; 
			}

			
			
			

			if (time > sendTime) {
				sendTime = time + RATE;

				
				for (i = 0; i < ENTITIES; i++) {
					for (j = 0; j < VARIABLES; j++) {
						k = IDSIZE + 4 * (i * VARIABLES + j);
						var m = LANdata[0][i][j];
						txMsg[k + 3] = (byte) ((m >> 24) & 0xff);
						txMsg[k + 2] = (byte) ((m >> 16) & 0xff);
						txMsg[k + 1] = (byte) ((m >> 8) & 0xff);
						txMsg[k] = (byte) (m & 0xff);
					}
				}
				try {
					socket.send(txPacket);
				} catch (Exception e) {
				}
			}


			int[] LANentity;
			for (i = 0; i < PLAYERS; i++) {
				if (timestamp[i] + TIMEOUT > time) { 
					for (j = 0; j < ENTITIES; j++) {
						LANentity = LANdata[i][j];
						entity1 = localData[i][j];
						if ((entity1[T] = LANentity[T]) != 0) {
							var LANdeltaTime = time - timestamp[i];
							for (k = X; k <= Z; k++) {
								
								entity1[k] = (LANentity[k] + LANdeltaTime * TRANSLATERATE * LANentity[VX + k]) / SCALEXYZ;
							}
							entity1[A] = LANentity[A];
						}
					}
				}
			}


			float[] entity2;
			float dz;
			float dx;
			for (i = 0; i < PLAYERS; i++) {
				if (timestamp[i] + TIMEOUT > time) { 
					for (j = 0; j <= ENEMIES; j++) {
						entity1 = localData[i][j];
						if (entity1[T] != 0) { 
							
							for (k = ENEMIES + 1; k < ENTITIES; k++) {
								
								if (i == 0 && j == 0 && k <= ENEMIES + PLAYERFIREBALLS)
									continue;
								
								if (j != 0 && k > ENEMIES + PLAYERFIREBALLS)
									continue;
								entity2 = localData[0][k]; 
								if (entity2[T] != 0) { 
									
									dx = entity1[X] - entity2[X];
									dz = entity1[Z] - entity2[Z];
									
									if (dx * dx + dz * dz < COLLISIONRADIUS && entity2[Y] < entity1[Y] + 2f) {
										
										fireballTimeout[k - ENEMIES - 1] = time + FIREBALLTIMEOUT2;
										
										if (i == 0) {
											if (health[j] > 0)
												health[j]--;
											if (health[j] == 0 && j > 0)
												LANdata[0][j][T] = 0;
										}
									}
								}
							}
						}
					}
				}
			}

			
			
			

			for (i = 1; i < PLAYERS; i++) {
				if (timestamp[i] + TIMEOUT > time) { 
					for (j = ENEMIES + 1; j < ENTITIES; j++) {
						entity2 = localData[i][j];
						if (entity2[T] != 0) { 
							
							for (k = 0; k <= ENEMIES; k++) {
								
								if (j > ENEMIES + PLAYERFIREBALLS && k != 0)
									continue;
								entity1 = localData[0][k]; 
								if (entity1[T] != 0) { 
									
									dx = entity1[X] - entity2[X];
									dz = entity1[Z] - entity2[Z];
									
									if (dx * dx + dz * dz < COLLISIONRADIUS && entity2[Y] < entity1[Y] + 2f) {
										
										
										if (health[k] > 0)
											health[k]--;
										if (health[k] == 0 && k > 0)
											LANdata[0][k][T] = 0;
									}
								}
							}
						}
					}
				}
			}


			int tileZ;
			int tileX;
			int mapZ;
			int mapX;
			int tileType;
			for (i = ENEMIES + 1; i < ENTITIES; i++) {
				entity1 = localData[0][i];
				
				mapX = (int) (entity1[X] + 1000) - 1000;
				mapZ = (int) (entity1[Z] + 1000) - 1000;
				tileX = (int) (TILESIZE * (entity1[X] - mapX));
				tileZ = (int) (TILESIZE * (entity1[Z] - mapZ));

				
				tileType = MOUNTAIN;
				if (mapX >= 0 && mapX < MAPSIZE && mapZ >= 0 && mapZ < MAPSIZE)
					tileType = map[mapX][mapZ];

				
				if (tiles[tileType][tileX][tileZ] > entity1[Y]) {
					
					LANdata[0][i][T] = 0;
				}
			}

			
			
			

			
			if (keyboard[Event.LEFT] || keyboard['a']) 
				angleY += ROTATERATE * deltaTime;
			if (keyboard[Event.RIGHT] || keyboard['d']) 
				angleY -= ROTATERATE * deltaTime;
			angleY = (angleY + 2f * (float) Math.PI) % (2f * (float) Math.PI);
			var sinAngleY = sin[(int) (DEGREES * angleY)];
			var cosAngleY = cos[(int) (DEGREES * angleY)];


			float rx = 0;
			float rz = 0;
			if (keyboard[Event.UP] || keyboard['w']) { 
				rx = -sinAngleY;
				rz = -cosAngleY;
			}
			if (keyboard[Event.DOWN] || keyboard['s']) { 

				rx = sinAngleY;
				rz = cosAngleY;
			}


			var x = playerX + TRANSLATERATE * deltaTime * rx;
			z = playerZ + TRANSLATERATE * deltaTime * rz;
			mapX = (int) (x + 1000) - 1000;
			mapZ = (int) (z + 1000) - 1000;
			tileX = (int) (TILESIZE * (x - mapX));
			tileZ = (int) (TILESIZE * (z - mapZ));

			
			tileType = MOUNTAIN;
			if (mapX >= 0 && mapX < MAPSIZE && mapZ >= 0 && mapZ < MAPSIZE)
				tileType = map[mapX][mapZ];

			
			y = tiles[tileType][tileX][tileZ];

			if (y >= 2) {
				rx = rz = 0;
			} else {
				playerX = x;
				playerY = y;
				playerZ = z;
			}
			LANentity = LANdata[0][0];
			LANentity[X] = (int) (SCALEXYZ * playerX);
			LANentity[Y] = (int) (SCALEXYZ * playerY);
			LANentity[Z] = (int) (SCALEXYZ * playerZ);
			LANentity[A] = (int) (DEGREES * angleY);
			LANentity[VX] = (int) (SCALEXYZ * rx);
			LANentity[VZ] = (int) (SCALEXYZ * rz);

			
			
			

			
			
			
			entity2 = localData[0][0];
			int angle;
			float depth;
			for (j = 1; j <= ENEMIES; j++) {
				entity1 = localData[0][j];
				if (entity1[T] != 0) { 
					dx = entity1[X] - entity2[X];
					var dy = entity1[Y] - entity2[Y];
					dz = entity1[Z] - entity2[Z];
					depth = dx * dx + dz * dz;

					
					k = ((entity1[X] - 3.5f) * (entity1[Z] - 3.5f) > 0) ? -1 : 1;
					x = dx + (j - (ENEMIES + 1) / 2f) * 0.1f * k;
					z = dz + (j - (ENEMIES + 1) / 2f) * 0.1f;

					
					
					angle = (int) (Math.atan2(x, z) * DEGREES);
					angle = (angle + 36000) % 36000;
					x = sin[angle]; 
					z = cos[angle]; 

					
					
					dy = (dy - 1f) / dx;
					angle = (int) (Math.atan2(dx, dz) * DEGREES);
					angle = (angle + 36000) % 36000;
					dx = sin[angle]; 
					dy *= dx;
					dz = cos[angle]; 

					
					angle = (int) entity1[A] - angle; 
					angle = (angle + 36000) % 36000; 

					
					LANdata[0][j][VX] = 0; 
					LANdata[0][j][VZ] = 0; 

					
					if (depth < TOOCLOSE) {
						
						LANdata[0][j][VX] = +(int) (x * SCALEXYZ);
						LANdata[0][j][VZ] = +(int) (z * SCALEXYZ);
						
						if (angle > 19000) {
							LANdata[0][j][A] -= (int) (ROTATERATE * 10f * deltaTime * DEGREES);
						} else if (angle < 17000) {
							LANdata[0][j][A] += (int) (ROTATERATE * 10f * deltaTime * DEGREES);
						}
						LANdata[0][j][A] = (LANdata[0][j][A] + 36000) % 36000;

						
					} else if (depth > TOOFAR) {
						
						LANdata[0][j][VX] = -(int) (x * SCALEXYZ);
						LANdata[0][j][VZ] = -(int) (z * SCALEXYZ);
						
						if (angle > 1000 && angle < 18000) {
							LANdata[0][j][A] -= (int) (ROTATERATE * 10f * deltaTime * DEGREES);
						} else if (angle < 35000 && angle > 18000) {
							LANdata[0][j][A] += (int) (ROTATERATE * 10f * deltaTime * DEGREES);
						}
						LANdata[0][j][A] = (LANdata[0][j][A] + 36000) % 36000;

						
					} else {
						
						if (angle > 1000 && angle < 18000) {
							LANdata[0][j][A] -= (int) (ROTATERATE * 10f * deltaTime * DEGREES);
						} else if (angle < 35000 && angle > 18000) {
							LANdata[0][j][A] += (int) (ROTATERATE * 10f * deltaTime * DEGREES);
						}
						LANdata[0][j][A] = (LANdata[0][j][A] + 36000) % 36000;

						
						
						if (localData[0][j + ENEMIES + PLAYERFIREBALLS][T] == 0 && (angle < 1000 || angle > 35000)) {

							
							LANdata[0][j + ENEMIES + PLAYERFIREBALLS][X] = LANdata[0][j][X];
							LANdata[0][j + ENEMIES + PLAYERFIREBALLS][Y] = LANdata[0][j][Y] + (int) SCALEXYZ;
							LANdata[0][j + ENEMIES + PLAYERFIREBALLS][Z] = LANdata[0][j][Z];

							
							LANdata[0][j + ENEMIES + PLAYERFIREBALLS][VX] = -(int) (dx * SCALEXYZ);
							LANdata[0][j + ENEMIES + PLAYERFIREBALLS][VY] = -(int) (dy * SCALEXYZ);
							LANdata[0][j + ENEMIES + PLAYERFIREBALLS][VZ] = -(int) (dz * SCALEXYZ);

							
							LANdata[0][j + ENEMIES + PLAYERFIREBALLS][T] = LANdata[0][j][T];
							fireballTimeout[j + PLAYERFIREBALLS - 1] = time + FIREBALLTIMEOUT1;
						}
					}
				}
			}


			float sinRayAngle;
			float cosRayAngle;
			if (mouse) {
				for (i = ENEMIES + 1; i <= ENEMIES + PLAYERFIREBALLS; i++) {
					if (localData[0][i][T] == 0) {
						
						mouse = false;
						if (mouseX >= 00 && mouseX < SCREENWIDTH) {
							
							LANdata[0][i][X] = LANdata[0][0][X]; 
							LANdata[0][i][Y] = LANdata[0][0][Y] + (int) (1.5f * SCALEXYZ);
							LANdata[0][i][Z] = LANdata[0][0][Z];

							
							rayAngle = angleY + rayAngleFix[mouseX];
							rayAngle = (rayAngle + 2f * (float) Math.PI) % (2f * (float) Math.PI);
							cosRayAngle = cos[(int) (DEGREES * rayAngle)];
							sinRayAngle = sin[(int) (DEGREES * rayAngle)];

							LANdata[0][i][VX] = -(int) (sinRayAngle * 2f * SCALEXYZ);
							
							LANdata[0][i][VY] = -(int) (40f * ((float) (mouseY - SCREENHEIGHT / 2) / SCREENHEIGHT) * SCALEXYZ);
							LANdata[0][i][VZ] = -(int) (cosRayAngle * 2f * SCALEXYZ);

							
							LANdata[0][i][T] = 7;
							fireballTimeout[i - ENEMIES - 1] = time + FIREBALLTIMEOUT1;
						}
						break;
					}
				}
			}

			
			
			

			for (i = ENEMIES + 1; i < ENTITIES; i++) {
				if (localData[0][i][T] != 0) { 
					if (fireballTimeout[i - ENEMIES - 1] < time) {
						LANdata[0][i][T] = 0;
					}
				}
			}

			
			
			

			for (i = 1; i < ENTITIES; i++) {
				if (localData[0][i][T] != 0) { 
					LANentity = LANdata[0][i];
					for (j = X; j <= Z; j++) {
						LANentity[j] += deltaTime * TRANSLATERATE * LANentity[j + VX];
					}
				}
			}

			
			
			

			for (i = 1; i <= ENEMIES; i++) {
				x = localData[0][i][X];
				z = localData[0][i][Z];
				mapX = (int) (x + 1000) - 1000;
				mapZ = (int) (z + 1000) - 1000;
				tileX = (int) (TILESIZE * (x - mapX));
				tileZ = (int) (TILESIZE * (z - mapZ));

				
				tileType = MOUNTAIN;
				if (mapX >= 0 && mapX < MAPSIZE && mapZ >= 0 && mapZ < MAPSIZE)
					tileType = map[mapX][mapZ];

				
				y = tiles[tileType][tileX][tileZ];
				LANdata[0][i][Y] = (int) (y * SCALEXYZ);
			}

			
			timestamp[0] = time;


			var found = false;
			for (i = 0; i < PLAYERS; i++) {
				if (timestamp[i] + TIMEOUT > time) { 
					for (j = 1; j <= ENEMIES; j++) {
						entity1 = localData[i][j];
						if (entity1[T] > 0) {
							if ((playerX > 3.5f) == (entity1[X] > 3.5f) && (playerZ > 3.5f) == (entity1[Z] > 3.5f))
								found = true;
							break;
						}
					}
				}
			}
			if (!found) {
				
				for (i = 0; i < PLAYERS; i++) {
					if (timestamp[i] + TIMEOUT > time) { 
						j = (int) localData[i][0][X];
						k = (int) localData[i][0][Z];
						if ((j == 1 || j == 5) && (k == 1 || k == 5)) {
							castle[(j - 1) / 4 + 2 * ((k - 1) / 4)] = (i == 0);
						}
					}
				}
			}

			
			
			

			
			if ((int) playerX == 3 && (int) playerZ == 3) {
				for (i = 1; i <= ENEMIES; i++)
					LANdata[0][i][T] = 0;
				allowSpawn = true; 
			} else {
				j = playerX < 3.5f ? 1 : 5;
				k = playerZ < 3.5f ? 1 : 5;
				if (allowSpawn && !castle[(j - 1) / 4 + 2 * ((k - 1) / 4)]) {
					
					for (i = 1; i <= ENEMIES; i++) {
						LANdata[0][i][X] = (int) ((j + 0.5f) * SCALEXYZ);
						LANdata[0][i][Z] = (int) ((k + 0.5f) * SCALEXYZ);
						LANdata[0][i][T] = (i % 6) + 1;
						health[i] = ENEMYHEALTH;
					}
				}
				allowSpawn = false; 
			}


			var drawCount1 = 0;
			for (i = 0; i < PLAYERS; i++) {
				if (timestamp[i] + TIMEOUT > time) { 
					for (j = (i == 0) ? 1 : 0; j <= ENEMIES; j++) {
						entity1 = localData[i][j];
						if (entity1[T] != 0f) { 
							
							
							dx = entity1[X] - playerX;
							dz = entity1[Z] - playerZ;
							if (dx * dx + dz * dz < ENTITYDEPTH && dx * sinAngleY + dz * cosAngleY < 0)
								drawList1[drawCount1++] = entity1;
						}
					}
				}
			}

			float radius;
			int sy;
			int dst;
			int colour;
			for (var ray = 0; ray < SCREENWIDTH; ray += 2) {
				rayAngle = angleY + rayAngleFix[ray];
				rayAngle = (rayAngle + 2f * (float) Math.PI) % (2f * (float) Math.PI);
				cosRayAngle = cos[(int) (DEGREES * rayAngle)] / cosRayAngleFix[ray];
				sinRayAngle = sin[(int) (DEGREES * rayAngle)] / cosRayAngleFix[ray];


				var drawCount2 = 0;
				for (i = 0; i < drawCount1; i++) {
					
					dx = drawList1[i][X] - playerX;
					dz = drawList1[i][Z] - playerZ;
					radius = dx * cosRayAngle - dz * sinRayAngle;

					if (radius * radius < ENTITYRADIUS)
						drawList2[drawCount2++] = drawList1[i];
				}

				var lsy = SCREENHEIGHT - 1;
				
				for (i = 1; i < DEPTH; i++) {
					depth = zMap[i];
					x = playerX - sinRayAngle * depth;
					z = playerZ - cosRayAngle * depth;

					
					mapX = (int) (x + 1000) - 1000;
					mapZ = (int) (z + 1000) - 1000;
					tileX = (int) (TILESIZE * (x - mapX));
					tileZ = (int) (TILESIZE * (z - mapZ));

					
					tileType = MOUNTAIN;
					if (mapX >= 0 && mapX < MAPSIZE && mapZ >= 0 && mapZ < MAPSIZE)
						tileType = map[mapX][mapZ];

					
					y = tiles[tileType][tileX][tileZ];

					var drawObject = false;

					
					
					for (j = 0; j < drawCount2; j++) {
						entity1 = drawList2[j];
						dx = entity1[X] - x;
						dz = entity1[Z] - z;
						
						if (dx * dx + dz * dz < ENTITYRADIUS) {
							angle = (int) entity1[A];

							
							dx = dx * TILESIZE / ENTITYSCALE;
							dz = dz * TILESIZE / ENTITYSCALE;

							
							x1 = (int) (dx * cos[angle] - dz * sin[angle] + CENTRE);
							if (x1 < 0 || x1 >= ENTITYSIZE)
								continue;
							z1 = (int) (dx * sin[angle] + dz * cos[angle] + CENTRE);
							if (z1 < 0 || z1 >= ENTITYSIZE)
								continue;

							
							if (entityHeight[x1][z1] > 0) {
								drawObject = true;
								break; 
							}
						}
					}

					colour = 0x000200; 
					if (drawObject) {
						
						y += (float) entityHeight[x1][z1] / ENTITYSCALE;
						switch (entityColour[x1][z1]) {
						case 0:
							colour = (cMap[(int) entity1[T]] & 0x007f7f7f) + 0x080808 * (x1 + z1);
							break;
						case 1:
							colour = 0;
							break;
						case 2:
							colour = 0x800000 + 0x080808 * (x1 + z1);
							break;
						default:
							colour = 0x801010;
						}
					} else {
						
						switch (tileType) {
						case PLAIN:
							
							y = tiles[GRASS][tileX][tileZ];
							break;
						case MOUNTAIN:
							if (tiles[PLAIN][tileX][tileZ] < y)
								colour = 0x020201;
							else
								y = tiles[GRASS][tileX][tileZ];
							break;
						default: 
							if (mapX == 1) {
								if (mapZ == 1)
									colour = 0x000001; 
								else
									colour = 0x010000; 
							} else {
								if (mapZ == 1)
									colour = 0x000100; 
								else
									colour = 0x010100; 
							}

							
							if (((tileX / DAPPLE) & 1) == ((tileZ / DAPPLE) & 1))
								colour += 0x010101;
						}
						
						colour *= (int) (16 * ((4f + (y < 2f ? y / 1f : 2f)) / (1f + depth))) & 255;
					}

					

					
					sy = SCREENHEIGHT / 2 - (int) (20f * (y - playerY - PLAYERHEIGHT) / depth);
					if (sy < 0)
						sy = 0;
					if (sy > SCREENHEIGHT - 1)
						sy = SCREENHEIGHT - 1;

					if (sy < lsy) {
						dst = ray + lsy * SCREENWIDTH;
						for (j = lsy; j > sy; j--) {
							screenData[dst] = screenData[dst + 1] = colour;
							zBuffer[dst] = zBuffer[dst + 1] = -depth;
							dst -= SCREENWIDTH;
						}
						lsy = sy;
					}
				}

				
				dst = ray + lsy * SCREENWIDTH;
				for (j = lsy; j >= 0; j--) {
					screenData[dst] = screenData[dst + 1] = ((health[0] < 10) ? 0x400040 : 0x200080) + 0x010101 * (j / 3);
					zBuffer[dst] = zBuffer[dst + 1] = -Float.MAX_VALUE;
					dst -= SCREENWIDTH;
				}
			}

			
			for (i = 0; i < PLAYERS; i++) {
				if (timestamp[i] + TIMEOUT > time) { 
					for (j = ENEMIES + 1; j < ENTITIES; j++) {
						entity1 = localData[i][j];
						if (entity1[T] != 0) { 

							
							dx = entity1[X] - playerX;
							dz = entity1[Z] - playerZ;
							rx = dx * cosAngleY - dz * sinAngleY;
							rz = dx * sinAngleY + dz * cosAngleY;

							
							if (dx * dx + dz * dz < ENTITYDEPTH && rz < 0) {
								var sx = SCREENWIDTH / 2 - (int) (SCREENDEPTH * rx / rz);
								sy = SCREENHEIGHT / 2 + (int) (20f * (entity1[Y] - playerY - PLAYERHEIGHT) / rz);
								
								drawCount1 = (int) (50 / (rz * rz));
								if (drawCount1 > 50000)
									drawCount1 = 50000;
								colour = cMap[(int) entity1[T]];
								for (k = 0; k < drawCount1; k++) {
									radius = 5f * (float) Math.random() / rz;
									angle = (int) (36000 * Math.random());
									x1 = sx + (int) (radius * sin[angle]);
									y1 = sy + (int) (radius * cos[angle]);
									if (x1 > 0 && x1 < SCREENWIDTH && y1 >= 0 && y1 < SCREENHEIGHT) {
										dst = x1 + SCREENWIDTH * y1;
										if (zBuffer[dst] < rz)
											screenData[dst] = colour;
									}
								}
							}
						}
					}
				}
			}

			
			
			

			for (i = 10; i < 20; i++) { 
				for (j = 0; j < health[0]; j++) { 
					screenData[10 + j + SCREENWIDTH * i] = 0x00ff00;
				}
				for (k = 0; k < 4; k++) { 
					colour = 0xff << (8 * k);
					if (k == 3)
						colour = 0xffff00;
					if (!castle[k])
						colour = 0;
					for (j = 0; j < 10; j++) {
						screenData[120 + k * 20 + j + SCREENWIDTH * i] = colour;
					}
				}
			}

			
			
			

			if (gs != null) {
				gs.drawImage(screen, 0, 0, null);
			}
			Thread.yield();
		} while (isActive());

		
		
		socket.close();
	}

	/** Process Keyboard and Mouse Events */
	@Override
	public boolean handleEvent(Event e) {
		switch ((e.id - 1) | 1) {
		case Event.KEY_PRESS:
		case Event.KEY_ACTION:
			keyboard[e.key] = (e.id & 1) == 1;
			return true;
		case Event.MOUSE_DOWN:
			mouse = (e.id & 1) == 1;
			mouseX = e.x;
			mouseY = e.y;
			return true;
		default:
		}
		return false;
	}
}
