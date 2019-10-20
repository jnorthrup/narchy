package java4k.thebattleforhoth4k;

/*
 * Star Wars: The Battle for Hoth 4K
 * Copyright (C) 2011 meatfighter.com
 *
 * This file is part of Star Wars: The Battle for Hoth 4K.
 *
 * Star Wars: The Battle for Hoth 4K is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Star Wars: The Battle for Hoth 4K is distributed in the hope that it will be useful,
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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class a extends GamePanel {

	
	private final boolean[] a = new boolean[32768];

	@Override
	public void start() {
		enableEvents(8L);
		new Thread(this).start();
	}

	@Override
    public void run() {

        final int SPRITE_ATST = 1;

		int i;

        final AffineTransform transform = new AffineTransform();
        final BufferedImage image = new BufferedImage(362, 362, 1);
        final BufferedImage image2 = new BufferedImage(512, 512, 1);
        final Graphics2D g = (Graphics2D) image.getGraphics();
        final Graphics2D g2 = (Graphics2D) image2.getGraphics();

        final Random random = new Random();

        final Color[] firePalette = new Color[32];

		
		for (i = 0; i < 32; i++) {
			firePalette[i] = new Color(255 - ((255 * i) >> 5), 80 - ((80 * i) >> 5), 0, 255 - (i << 3));
		}


        final BufferedImage[] sprites = new BufferedImage[2];
        final int[] pixels = new int[362];
        int y;
        int x;
        int k;
        int j;
        final String S = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u4000\u4000\ud000\ud000\ud000\u5000\u5400\u4000\u0000\u0000\u4000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u555c\ub7a9\ua9ae\u69a5\uda65\uf95b\ub5bb\u6a6d\u5eed\u5599\u5555\u5454\u0005\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uc000\u4000\u4000\u7000\u6000\ud010\ud436\ud91a\ud7d5\ub5bf\ub767\ub679\ub559\ub6d5\u5559\u0d55\u4b6a\u4015\u4000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u4000\u4000\u4000\u4000\u4000\ud000\ua000\ud800\u7800\u9400\ufc00\ub400\ua500\u0100\u0000\u6c00\ue568\ubfe5\uaabf\uaaab\uaffb\u9abb\u9aba\u9aba\u9aba\u956a\uaaaa\ubabb\uaaaa\u9a99\uaaaa\u5555\u5555\u5665\u5655\u5ed5\u6a55\u5a56\u7559\u1557\u1955\u1990\u1590\u1994\u5994\u7654\u75f5\u57a9\u57a9\u55a5\u5555\u5559\u5599\u5599\u5565\u5766\u5766\u976a\u5955\u597d\u55a9\u5e9a\u565f\u5675\u555a\u55af\u56aa\u1565\u0005\u555c\ufff5\ubbb7\u99b6\uaab6\u5ab6\ufeb6\u5ab6\uaab6\u5ab6\ufeb6\u5ada\uaada\uaada\uaada\uaada\uffda\uaad5\u6f55\u6a55\u6f55\ubd5a\u5555\u155a\u1a95\u0540\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0001\u0001\u0001\u0001\u0005\u0015\u001d\u0015\u0015\u0029\u0055\u0055\u0000\u0000\u5555\uffff\ueebe\u6696\uaaaa\uaa55\uabff\uaa55\uaaaa\uaa55\ubbff\ua955\ubaea\ua9aa\ubaaa\uaaaa\uafff\uaaaa\u7e59\u6a59\u7e59\u5eaa\u5555\u9560\u5a90\u0100\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u6000\u5000\u0000\u0000\u700d\ud397\u5556\uaa96\uaa56\uaa96\u5a56\u9a96\u9a56\u9a96\uea96\uaa95\uaaa5\ufa95\ubaa5\u6a95\uaaa5\u5555\u5555\u6555\u6555\ued69\uaa55\u5a56\u7556\u5a64\u6690\u6690\u6650\u6550\u66e4\u6678\u5964\uf554\uad50\uad50\ua550\u5550\u5550\u6550\u6550\u9550\u9550\u9650\u9564\u5564\ud555\ud55d\u6555\u6555\u6555\u5555\ud595\u9000\u5400\u0035\u015b\u0015\u00d5\u00aa\u006a\u036a\u026b\u01ab\u01ab\u0dab\u1fea\u1aaa\u1aaf\u1a9a\u1695\u06aa\u0555\u0015\u0056\u0055\u0055\u0017\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0001\u0001\u0005\u0006\u0006\u0005\u0005\u0006\u0006\u0006\u0005\u0019\u0019\u0019\u0015\u006d\u015a\u0675\u19df\u15af\u1ff5\u5baf\u555a\u0155\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u002a\uaa7f\u556a\u0015\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ua000\ua800\uee00\uf780\ubbe0\ubae0\ue7f8\ub6f8\udbf8\u6efe\ubbfd\ueffd\ubf5d\u7dad\u1ead\u1fad\u07fd\u07d4\u0140\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u9000\u5400\u0000\u0000\u0000\ua800\ufea0\ueafa\u76aa\udbaf\u6faf\u6eff\u5bba\u6efa\u5aae\u66ab\u999a\ua666\ua955\ueaaa\u5555\u1554\u1554\u5554\u5550\u5540\u9900\u5500\ue900\ube40\uef90\uef90\udf90\u9f50\u7d00\u9400\u4000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u4000\u5400\ua900\u9b90\uaab9\u6aeb\u5555\u0000\u0000\u0000\u02aa\u0bff\u2fff\u2abf\ubfa9\u7ff6\ubff6\u7ff6\ubff6\u7ea6\uaa56\u55ae\u6abb\u6eef\u17bb\u0155\u0000\u0000\u0000\u0000\u0000\u0001\uafa9\ud69e\ua7f7\udffd\udf7d\udeed\ue7f7\uf95a\uaaa5\u5555\u9954\u6550\u9500\u5000\u4000\u0000\u0000\u0000\u0000\u4000\u4000\u4000\u4000\u5000\u9000\u5000\u9000\u6400\u9400\u6540\u97d0\u6ab5\u156b\u0016\u0001\u0001\u0001\u0001\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0002\u002f\u001d\u005b\u01bf\u17ff\u6fff\uffff\ubffe\u7fa5\u5905\u001a\u0156\u01a9\u0199\u05a9\u0565\u05a9\u05a9\u016a\u006a\u006a\u001a\u0016\u0005\u0006\u0005\u0001\u0015\u006a\u0016\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0016\u007f\u017b\u067f\u0bd9\u1ff4\ubff8\ubfe0\uff40\ufd00\uf400\ud000\u8000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufa00\uff80\u9940\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0002\u008b\u02cb\u02ef\u02ef\u02ee\u03ed\u03b8\u03b8\u0ba8\u0ba0\u0be0\u07e0\u07e0\u0b80\u0b80\u0f80\u2e80\u0680\u0580\u0740\u1fd0\u2eda\u1fff\u16ef\u6bbb\u1666";
        for (k = 0; k < 2; k++) {
            int width = 8;
            int height = 55;
			if (k == 1) {
				width = 7;
				height = 64;
			}
			sprites[k] = new BufferedImage(width << 3, height, 2);
			for (x = 0; x < width; x++) {
				for (y = 0; y < height; y++) {
					for (i = 0; i < 8; i++) {
						j = ((int) S.charAt((k == 0 ? 0 : 440) + height * x + y) >> (i << 1)) & 3;
						pixels[i] = j == 0 ? 0 : 0xFF000000 | (j == 1 ? 0 : j == 2 ? 0x7F7F7F : 0xBFBFBF);
					}
					sprites[k].setRGB(x << 3, y, 8, 1, pixels, 0, 8);
				}
			}
		}


        final int[][] floorTexture = new int[256][256];
        for (y = 0; y < 256; y++) {
			for (x = 0; x < 256; x++) {
				floorTexture[y][x] = 128;
			}
		}
		for (int z = 0; z < 8; z++) {
			for (y = 0; y < 256; y++) {
				for (x = 0; x < 256; x++) {
					floorTexture[y][x] += random.nextInt(1 << (8 - (z >> 2))) - (1 << (7 - (z >> 2)));
					if (floorTexture[y][x] < 0) {
						floorTexture[y][x] = 0;
					} else if (floorTexture[y][x] > 255) {
						floorTexture[y][x] = 255;
					}
				}
			}
			for (y = 0; y < 256; y++) {
				for (x = 0; x < 256; x++) {
					k = 0;
					for (i = -1; i <= 1; i++) {
						for (j = -1; j <= 1; j++) {
							k += floorTexture[(y + i) & 0xFF][(x + j) & 0xFF];
						}
					}
					floorTexture[y][x] = k / 9;
				}
			}
		}
        final int[][] skyTexture = new int[256][256];
        for (y = 0; y < 256; y++) {
			for (x = 0; x < 256; x++) {
				i = floorTexture[y][x];
				floorTexture[y][x] = ((0xF7 + (0x9D - 0xF7) * i / 255) << 16) | ((0xF8 + (0xA4 - 0xF8) * i / 255) << 8) | (0xFD + (0xD8 - 0xFD) * i / 255);
				skyTexture[y][x] = ((0x84 + (0xBE - 0x84) * i / 255) << 16) | ((0xB5 + (0xDE - 0xB5) * i / 255) << 8) | (0xF7 + (0xF3 - 0xF7) * i / 255);
			}
		}

        long nextFrameStartTime = System.nanoTime();
        final int[][] scaledSprites = new int[4096][9];
        final int[] yPoints = new int[4];
        final int[] xPoints = new int[4];
        Graphics2D g3 = null;
        final ArrayList<int[]> enemies = new ArrayList<int[]>();
        final ArrayList<float[]> lasers = new ArrayList<float[]>();
        float yawSin = (float) 0;
        float yawCos = (float) 0;
        float heightVelocity = (float) 0;
        float yawVelocity = (float) 0;
        float roll = (float) 0;
        float yaw = (float) 0;
        float zo = (float) 0;
        float yo = (float) 0;
        float xo = (float) 0;
        boolean paused = true;
        boolean fadingIn = false;
        boolean fading = true;
        boolean dying = false;
        int playerPower = 0;
        int level = 1;
        int fadeIntensity = 255;
        int lastEnemyDrawn = 0;
        int enemyLaserDelay = 0;
        int enemyHitCountdown = 0;
        int enemyHitPower = 0;
        int enemyHitSpriteIndex = 0;
        int spriteCount = 0;
        int laserDelay = 0;
        final int MAX_LEVEL = 6;
        final int MAX_BATTLE_DISTANCE_2 = 268435456;
		final float MAX_YAW_VELOCITY = 0.02f;
		final float ROLL_SCALE = -1f / MAX_YAW_VELOCITY;
        final int HEIGHT_ACCELERATION = 1;
        final int MAX_HEIGHT_VELOCITY = 32;
        final float YAW_ACCELERATION = 0.0005f;
        final int LOW_SPEED = 16;
        final int HIGH_SPEED = 32;
        final int LASER_MAX_COUNT = 64;
        final int LASER_DELAY = 8;
		final int LASER_WIDTH = 32;
		final int LASER_HALF_WIDTH = LASER_WIDTH / 2;
        final int LASER_LENGTH = 1024;
        final int LASER_SPEED = 512;
		final int SKY_Y = 8192;
		final int MAX_Y = SKY_Y / 2;
		final int SCREEN_SIZE = 362;
		final int HALF_SCREEN_SIZE = SCREEN_SIZE / 2;
		final int MIN_Y = HALF_SCREEN_SIZE;
        final int SCREEN_DISTANCE = SCREEN_SIZE;
        final int QUARTER_SCREEN_SIZE = SCREEN_SIZE / 4;
        final int ENEMY_RZ = 7;
        final int ENEMY_RY = 6;
        final int ENEMY_RX = 5;
        final int ENEMY_Y = 4;
        final int ENEMY_POWER = 3;
        final int ENEMY_TYPE = 2;
        final int ENEMY_Z = 1;
        final int ENEMY_X = 0;
        final int ENEMY_TYPE_EXPLOSION = 2;
        final int ENEMY_TYPE_ATST = 1;
        final int ENEMY_TYPE_ATAT = 0;
        final int SPRITE_ATAT = 0;
        final int SPRITE_PALETTE = 8;
        final int SPRITE_ENEMY_INDEX = 7;
        final int SPRITE_DRAWN = 6;
        final int SPRITE_INDEX = 5;
        final int SPRITE_HEIGHT = 4;
        final int SPRITE_WIDTH = 3;
        final int SPRITE_Z = 2;
        final int SPRITE_Y = 1;
        final int SPRITE_X = 0;
        final int LASER_TYPE_ENEMY = 1;
        final int LASER_TYPE_PLAYER = 0;
        final int LASER_TYPE = 7;
        final int LASER_COUNT = 6;
        final int LASER_RZ = 5;
        final int LASER_RY = 4;
        final int LASER_RX = 3;
        final int LASER_Z = 2;
        final int LASER_Y = 1;
        final int LASER_X = 0;
        final int VK_ATTACK = 0x42;
        final int VK_DOWN = 0x28;
        final int VK_UP = 0x26;
        final int VK_RIGHT = 0x27;
        final int VK_LEFT = 0x25;
        while (true) {

			do {
				nextFrameStartTime += 16666667L;


                float Y;
                float X;
                if (fading) {
					

					if (fadingIn) {
						fadeIntensity -= 4;
						if (fadeIntensity <= 0) {
							fadeIntensity = 0;
							fading = false;
						}
					} else if (dying) {
						fadeIntensity += 2;
					} else {
						fadeIntensity += 4;
					}
					if (fadeIntensity >= 255) {
						fadeIntensity = 255;

						if (level == MAX_LEVEL) {
							
							continue;
						}

						fadingIn = true;
						dying = false;

						playerPower = 64;
						lastEnemyDrawn = 0;
						xo = (float) 0;
						yo = (float) MIN_Y;
						zo = 16384.0F;
						yaw = (float) 0;
						roll = (float) 0;
						yawCos = 1.0F;
						yawSin = (float) 0;
						yawVelocity = (float) 0;
						heightVelocity = (float) 0;

						lasers.clear();
						enemies.clear();

						
						for (i = 0; i < (level << 2) + 3; i++) {
							X = random.nextFloat() * 6.28f;
							Y = random.nextFloat() * 16384.0F;
                            final int[] atat = new int[8];
							atat[ENEMY_X] = (int) ((double) Y * Math.cos((double) X));
							atat[ENEMY_Z] = (int) ((double) Y * Math.sin((double) X));
							atat[ENEMY_TYPE] = random.nextInt(2);
							atat[ENEMY_POWER] = 64;
							enemies.add(atat);
						}

						nextFrameStartTime = System.nanoTime();
					}
					continue;
				}

				if (enemies.size() == 0) {
					
					level++;
					fading = true;
					fadingIn = false;
					continue;
				}

				
				
				if (paused) {
					if (a[VK_ATTACK]) {
						paused = false;
					} else {
						continue;
					}
				}

				if (enemyHitCountdown > 0) {
					enemyHitCountdown--;
				}

				yawCos = (float) Math.cos((double) yaw);
				yawSin = (float) Math.sin((double) yaw);

				if (enemyLaserDelay > 0) {
					enemyLaserDelay--;
				} else {
					enemyLaserDelay = LASER_DELAY;
					if (lastEnemyDrawn >= 0 && enemies.size() > 0) {

                        final int[] enemy = enemies.get(lastEnemyDrawn);
                        final float[] laser = new float[8];
						laser[LASER_X] = (float) enemy[ENEMY_X];
						laser[LASER_Y] = (float) (enemy[ENEMY_TYPE] == ENEMY_TYPE_ATAT ? 640 : 128);
						laser[LASER_Z] = (float) enemy[ENEMY_Z];
						laser[LASER_RX] = (float) (random.nextInt(768) - 384) + xo - (float) enemy[ENEMY_X];
						laser[LASER_RY] = (float) (random.nextInt(768) - 384) + yo - laser[LASER_Y];
						laser[LASER_RZ] = (float) (random.nextInt(768) - 384) + zo - (float) enemy[ENEMY_Z];
                        final float mag = (float) Math.sqrt((double) (laser[LASER_RX] * laser[LASER_RX] + laser[LASER_RY] * laser[LASER_RY] + laser[LASER_RZ] * laser[LASER_RZ]));
						laser[LASER_RX] /= mag;
						laser[LASER_RY] /= mag;
						laser[LASER_RZ] /= mag;
						laser[LASER_TYPE] = (float) LASER_TYPE_ENEMY;
						lasers.add(laser);
					}
				}

				if (laserDelay > 0) {
					k = LOW_SPEED;
					laserDelay--;
				} else {
					k = HIGH_SPEED;
					if (a[VK_ATTACK]) {
						
						laserDelay = LASER_DELAY;
                        float[] laser = new float[8];
						laser[LASER_RX] = yawSin;
						laser[LASER_RZ] = -yawCos;
						laser[LASER_X] = xo - (float) QUARTER_SCREEN_SIZE * yawCos;
						laser[LASER_Y] = yo - (float) HALF_SCREEN_SIZE;
						laser[LASER_Z] = zo - (float) QUARTER_SCREEN_SIZE * yawSin;
						lasers.add(laser);
						laser = new float[8];
						laser[LASER_RX] = yawSin;
						laser[LASER_RZ] = -yawCos;
						laser[LASER_X] = xo + (float) QUARTER_SCREEN_SIZE * yawCos;
						laser[LASER_Y] = yo - (float) HALF_SCREEN_SIZE;
						laser[LASER_Z] = zo + (float) QUARTER_SCREEN_SIZE * yawSin;
						lasers.add(laser);
					}
				}

				
				xo += (float) k * yawSin;
				zo -= (float) k * yawCos;

				yaw += yawVelocity;
				roll = ROLL_SCALE * yawVelocity;

				if (xo * xo + zo * zo > (float) MAX_BATTLE_DISTANCE_2) {
					
					

					X = yaw + 0.01f;
					Y = yaw - 0.01f;

					if ((double) zo * Math.cos((double) X) - (double) xo * Math.sin((double) X) > (double) zo * Math.cos((double) Y) - (double) xo * Math.sin((double) Y)) {
						if (yawVelocity < MAX_YAW_VELOCITY) {
							yawVelocity += YAW_ACCELERATION;
						}
					} else {
						if (yawVelocity > -MAX_YAW_VELOCITY) {
							yawVelocity -= YAW_ACCELERATION;
						}
					}
				} else {

					

					
					if (a[VK_LEFT]) {
						if (yawVelocity > -MAX_YAW_VELOCITY) {
							yawVelocity -= YAW_ACCELERATION;
						}
					} else if (a[VK_RIGHT]) {
						if (yawVelocity < MAX_YAW_VELOCITY) {
							yawVelocity += YAW_ACCELERATION;
						}
					} else if (yawVelocity > YAW_ACCELERATION) {
						yawVelocity -= YAW_ACCELERATION;
					} else if (yawVelocity < -YAW_ACCELERATION) {
						yawVelocity += YAW_ACCELERATION;
					} else {
						yawVelocity = (float) 0;
					}

					
					if (a[VK_DOWN]) {
						if (heightVelocity > (float) -MAX_HEIGHT_VELOCITY) {
                            heightVelocity = heightVelocity - (float) HEIGHT_ACCELERATION;
						}
					} else if (a[VK_UP] && yo < (float) MAX_Y) {
						if (heightVelocity < (float) MAX_HEIGHT_VELOCITY) {
                            heightVelocity = heightVelocity + (float) HEIGHT_ACCELERATION;
						}
					} else if (heightVelocity > (float) HEIGHT_ACCELERATION) {
                        heightVelocity = heightVelocity - (float) HEIGHT_ACCELERATION;
					} else if (heightVelocity < (float) -HEIGHT_ACCELERATION) {
                        heightVelocity = heightVelocity + (float) HEIGHT_ACCELERATION;
					} else {
						heightVelocity = (float) 0;
					}

					yo += heightVelocity;
					if (yo > (float) MAX_Y) {
						yo = (float) MAX_Y;
						heightVelocity = (float) 0;
					} else if (yo < (float) MIN_Y) {
						yo = (float) MIN_Y;
						heightVelocity = (float) 0;
					}
				}


                float Z;
                for (i = lasers.size() - 1; i >= 0; i--) {
                    final float[] laser = lasers.get(i);
					laser[LASER_X] += (float) LASER_SPEED * laser[LASER_RX];
					laser[LASER_Y] += (float) LASER_SPEED * laser[LASER_RY];
					laser[LASER_Z] += (float) LASER_SPEED * laser[LASER_RZ];

					if (laser[LASER_TYPE] == (float) LASER_TYPE_ENEMY) {
						X = xo - laser[LASER_X];
						Y = yo - laser[LASER_Y];
						Z = zo - laser[LASER_Z];

						if (X * X + Y * Y + Z * Z < 32768.0F && --playerPower <= 0) {
							dying = true;
							fading = true;
							fadingIn = false;
						}
					}

					if (++laser[LASER_COUNT] > (float) LASER_MAX_COUNT || laser[LASER_Y] < (float) 0 || laser[LASER_Y] > (float) SKY_Y) {
						lasers.remove(i);
					}
				}

				
				outter2: for (j = enemies.size() - 1; j >= 0; j--) {
                    final int[] enemy = enemies.get(j);
					if (enemy[ENEMY_TYPE] == ENEMY_TYPE_EXPLOSION) {
						
						if (++enemy[ENEMY_POWER] >= 32) {
							enemies.remove(j);
						} else {
							enemy[ENEMY_X] += enemy[ENEMY_RX];
							enemy[ENEMY_Y] += enemy[ENEMY_RY];
							enemy[ENEMY_Z] += enemy[ENEMY_RZ];
						}
					} else {
						
						X = (float) enemy[ENEMY_X] - xo;
						Z = (float) enemy[ENEMY_Z] - zo;

                        float xe = yawCos;
                        float ze = yawSin;
                        float dot = X * xe + Z * ze;
						xe *= dot;
						ze *= dot;
                        float x2 = X - xe;
                        float z2 = Z - ze;

                        int q;
                        int p;
                        int w;
                        int v;
                        int u;
                        if (x2 * x2 + z2 * z2 < 1024.0F) {
							
							if (enemy[ENEMY_TYPE] == ENEMY_TYPE_ATAT) {
								u = 16;
								v = 40;
								w = 53;
								p = 64;
								q = 55;
							} else {
								u = 4;
								v = 36;
								w = 59;
								p = 56;
								q = 64;
							}
							x = (int) ((float) v - dot / (float) u);
							y = (int) ((float) w - yo / (float) u);

							
							if (x >= 0 && y >= 0 && x < p && y < q && (enemy[ENEMY_TYPE] == ENEMY_TYPE_ATST || sprites[SPRITE_ATAT].getRGB(x, y) != 0)) {
								dying = true;
								fading = true;
								fadingIn = false;
							}
						}

						
						outter: for (i = lasers.size() - 1; i >= 0; i--) {
                            final float[] laser = lasers.get(i);
							if (laser[LASER_TYPE] == (float) LASER_TYPE_PLAYER) {
								for (k = 0; k < 512; k += 32) {
									X = (float) enemy[ENEMY_X] - (laser[LASER_X] + (float) k * laser[LASER_RX]);
									Z = (float) enemy[ENEMY_Z] - (laser[LASER_Z] + (float) k * laser[LASER_RZ]);

									xe = yawCos;
									ze = yawSin;
									dot = X * xe + Z * ze; 
									xe *= dot;
									ze *= dot;
									x2 = X - xe;
									z2 = Z - ze;

									if (x2 * x2 + z2 * z2 < 1024.0F) {
										
										
										if (enemy[ENEMY_TYPE] == ENEMY_TYPE_ATAT) {
											u = 16;
											v = 40;
											w = 53;
											p = 64;
											q = 55;
										} else {
											u = 4;
											v = 36;
											w = 59;
											p = 56;
											q = 64;
										}
										x = (int) ((float) v - dot / (float) u);
										y = (int) ((float) w - laser[LASER_Y] / (float) u);

										
										if (x >= 0 && y >= 0 && x < p && y < q && (enemy[ENEMY_TYPE] == ENEMY_TYPE_ATST || sprites[SPRITE_ATAT].getRGB(x, y) != 0)) {

											lasers.remove(i);

											enemy[ENEMY_POWER] -= 2;
											if (enemy[ENEMY_POWER] <= 0) {
												
												v = 128;
												w = 45;
												p = 22;
											} else {
												
												v = 8;
												w = 9;
												p = 5;
											}

											
											for (u = 0; u < v; u++) {
                                                final int[] explosion = new int[8];
												explosion[ENEMY_TYPE] = ENEMY_TYPE_EXPLOSION;
												explosion[ENEMY_X] = (int) (laser[LASER_X] + (float) k * laser[LASER_RX]);
												explosion[ENEMY_Y] = (int) (laser[LASER_Y] + (float) k * laser[LASER_RY]);
												explosion[ENEMY_Z] = (int) (laser[LASER_Z] + (float) k * laser[LASER_RZ]);
												explosion[ENEMY_RX] = random.nextInt(w) - p;
												explosion[ENEMY_RY] = random.nextInt(w) - p;
												explosion[ENEMY_RZ] = random.nextInt(w) - p;
												enemies.add(explosion);
											}

											if (enemy[ENEMY_POWER] <= 0) {
												enemies.remove(j);
												enemyHitCountdown = 0;
												continue outter2;
											} else {
												enemyHitSpriteIndex = enemy[ENEMY_TYPE];
												enemyHitPower = enemy[ENEMY_POWER];
												enemyHitCountdown = 128;
											}

											continue outter;
										}
									}
								}
							}
						}
					}
				}

				

			} while (nextFrameStartTime < System.nanoTime());

			

			
			for (i = 0; i < 362; i++) { 

				
				float ry = (float) (HALF_SCREEN_SIZE - i);
				float rz = (float) -SCREEN_DISTANCE;
                final float mag = (float) Math.sqrt((double) (ry * ry + rz * rz));
				ry /= mag;
				rz /= mag;

				
				for (k = 0; k < 2; k++) {
                    final float t = ((float) (SKY_Y * k) - yo) / ry;
                    final int[][] texture = k == 0 ? floorTexture : skyTexture;
                    final int textureScale = k == 0 ? 4 : 8;
					if (t > (float) 0) {

                        final float scale = t / (float) SCREEN_DISTANCE;
                        final float floorX = -scale * (float) HALF_SCREEN_SIZE;
                        final float floorZ = rz * t;


                        float fx = floorX * yawCos - floorZ * yawSin;
                        float fz = floorX * yawSin + floorZ * yawCos;
                        final float dfx = scale * yawCos;
                        final float dfz = scale * yawSin;

						
						for (j = 0; j < 362; j++) {
							pixels[j] = texture[((int) (zo + fz) >> textureScale) & 0xFF][((int) (xo + fx) >> textureScale) & 0xFF];

							fx += dfx;
							fz += dfz;
						}

						image.setRGB(0, i, SCREEN_SIZE, 1, pixels, 0, SCREEN_SIZE);
					}
				}
			}

			
			spriteCount = 0;
			for (i = enemies.size() - 1; i >= 0; i--) {
                final int[] enemy = enemies.get(i);

                final float x0 = (float) enemy[ENEMY_X] - xo;
                final float z0 = (float) enemy[ENEMY_Z] - zo;


                final float x1 = yawCos * x0 + yawSin * z0;
                final float z1 = yawCos * z0 - yawSin * x0;

				if (z1 < (float) 0) {
                    final float K = (float) SCREEN_DISTANCE / -z1;

                    int width = 1024;
                    int height = 864;
                    int offsetX = 640;
                    int offsetY = 832;

                    switch (enemy[ENEMY_TYPE]) {
                        case ENEMY_TYPE_ATST:
                            width = 224;
                            height = 256;
                            offsetX = 144;
                            offsetY = 236;
                            break;
                        case ENEMY_TYPE_EXPLOSION:
                            width = 64 - enemy[ENEMY_POWER];
                            height = width;
                            offsetX = width >> 1;
                            offsetY = enemy[ENEMY_Y] + width;
                            break;
                    }

					if (spriteCount < 4096) {
                        final int[] scaledSprite = scaledSprites[spriteCount++];
						scaledSprite[SPRITE_X] = 181 + (int) ((x1 - (float) offsetX) * K);
						scaledSprite[SPRITE_Y] = 181 - (int) (((float) offsetY - yo) * K);
						scaledSprite[SPRITE_Z] = (int) z1;
						scaledSprite[SPRITE_WIDTH] = (int) (K * (float) width);
						scaledSprite[SPRITE_HEIGHT] = (int) (K * (float) height);
						scaledSprite[SPRITE_INDEX] = enemy[ENEMY_TYPE];
						scaledSprite[SPRITE_DRAWN] = 0;
						scaledSprite[SPRITE_ENEMY_INDEX] = i;
						scaledSprite[SPRITE_PALETTE] = enemy[ENEMY_POWER];
					}
				}
			}

			
			lastEnemyDrawn = -1;
			for (i = 0; i < spriteCount; i++) {
				int[] scaledSprite = null;
				for (j = 0; j < spriteCount; j++) {
					if (scaledSprites[j][SPRITE_DRAWN] == 0 && (scaledSprite == null || scaledSprites[j][SPRITE_Z] < scaledSprite[SPRITE_Z])) {
						scaledSprite = scaledSprites[j];
					}
				}
				scaledSprite[SPRITE_DRAWN] = 1;
				if (scaledSprite[SPRITE_INDEX] == ENEMY_TYPE_EXPLOSION) {
					
					g.setColor(firePalette[scaledSprite[SPRITE_PALETTE]]);
					g.fillOval(scaledSprite[SPRITE_X], scaledSprite[SPRITE_Y], scaledSprite[SPRITE_WIDTH], scaledSprite[SPRITE_HEIGHT]);
				} else {
					g.drawImage(sprites[scaledSprite[SPRITE_INDEX]], scaledSprite[SPRITE_X], scaledSprite[SPRITE_Y], scaledSprite[SPRITE_WIDTH], scaledSprite[SPRITE_HEIGHT], null);
				}
				if (enemies.get(scaledSprite[SPRITE_ENEMY_INDEX])[ENEMY_TYPE] != ENEMY_TYPE_EXPLOSION) {
					lastEnemyDrawn = scaledSprite[SPRITE_ENEMY_INDEX];
				}
			}

			
			g.setColor(Color.RED);
			for (i = lasers.size() - 1; i >= 0; i--) {
                final float[] laser = lasers.get(i);


                final float x1 = laser[LASER_X] - xo;
                final float y1 = laser[LASER_Y] - yo;
                final float z1 = laser[LASER_Z] - zo;

                final float x2 = x1 + (float) LASER_LENGTH * laser[LASER_RX];
                final float y2 = y1 + (float) LASER_LENGTH * laser[LASER_RY];
                final float z2 = z1 + (float) LASER_LENGTH * laser[LASER_RZ];


                final float _x1 = yawCos * x1 + yawSin * z1;
                final float _x2 = yawCos * x2 + yawSin * z2;

                final float _z1 = yawCos * z1 - yawSin * x1;
                final float _z2 = yawCos * z2 - yawSin * x2;

				
				if (_z1 < (float) 0 && _z2 < (float) 0) {
                    final float k1 = (float) SCREEN_DISTANCE / -_z1;
                    final float k2 = (float) SCREEN_DISTANCE / -_z2;

					xPoints[0] = 181 + (int) ((_x1 - (float) LASER_HALF_WIDTH) * k1);
					xPoints[1] = 181 + (int) ((_x1 + (float) LASER_HALF_WIDTH) * k1);
					xPoints[2] = 181 + (int) ((_x2 + (float) LASER_HALF_WIDTH) * k2);
					xPoints[3] = 181 + (int) ((_x2 - (float) LASER_HALF_WIDTH) * k2);
					yPoints[0] = yPoints[1] = 181 - (int) (y1 * k1);
					yPoints[2] = yPoints[3] = 181 - (int) (y2 * k2);

					yPoints[1] += 2;
					yPoints[3] += 2;

					g.fillPolygon(xPoints, yPoints, 4);
				}
			}

			
			g2.translate(-107, -107);
			g2.rotate((double) roll, 362.0, 362.0);
			g2.drawImage(image, 0, 0, 724, 724, null);
			g2.setTransform(transform);

			
			g2.setColor(Color.GREEN);
			g2.drawRect(240, 250, 32, 32);

			if (enemyHitCountdown > 0) {
				
				g2.drawImage(sprites[enemyHitSpriteIndex], 432, 16, null);
				g2.drawRect(428, 96, 64, 8);
				g2.fillRect(428, 96, enemyHitPower, 8);
			} else {
				
				g2.translate(464, 48);
				g2.drawOval(-32, -32, 64, 64);
				g2.fillOval(-2, -2, 4, 4);
				g2.drawLine(0, 0, 14, -29);
				g2.drawLine(0, 0, -14, -29);
				g2.rotate((double) -yaw);
				g2.drawLine(0, 0, ((int) (-xo)) >> 9, ((int) (-zo)) >> 9);
				g2.drawOval((((int) (-xo)) >> 9) - 4, (((int) (-zo)) >> 9) - 4, 6, 6);
				for (i = 0; i < enemies.size(); i++) {
                    final int[] enemy = enemies.get(i);
					j = ((int) ((float) enemy[ENEMY_X] - xo)) >> 9;
					k = ((int) ((float) enemy[ENEMY_Z] - zo)) >> 9;
					if (j * j + k * k <= 1024) {
						g2.fillOval(j - 2, k - 2, 4, 4);
					}
				}
				g2.setTransform(transform);
			}

			
			g2.drawRect(20, 20, 64, 8);
			g2.fillRect(20, 20, playerPower, 8);

			if (fading) {
				
				g2.setColor(new Color(dying ? 255 : 0, 0, 0, fadeIntensity));
				g2.fillRect(0, 0, 512, 512);
			}

			

			
			if (g3 == null) {
				g3 = (Graphics2D) getGraphics();
				requestFocus();
			} else {
				g3.drawImage(image2, 0, 0, null);
			}

			
			while (nextFrameStartTime - System.nanoTime() > 0L) {
				Thread.yield();
			}
		}
	}

	@Override
	public void processKeyEvent(final KeyEvent keyEvent) {

        int k = keyEvent.getKeyCode();
		if (k > 0) {
            final int VK_D = 0x44;
            final int VK_A = 0x41;
            final int VK_S = 0x53;
            final int VK_W = 0x57;
            final int VK_DOWN = 0x28;
            final int VK_UP = 0x26;
            final int VK_RIGHT = 0x27;
            final int VK_LEFT = 0x25;
            k = k == VK_W ? VK_UP : k == VK_D ? VK_RIGHT : k == VK_A ? VK_LEFT : k == VK_S ? VK_DOWN : k;
            final int VK_ATTACK = 0x42;
            a[(k >= VK_LEFT && k <= VK_DOWN) ? k : VK_ATTACK] = keyEvent.getID() != 402;
		}
	}

	
	public static void main(final String[] args) throws Throwable {
        final JFrame frame = new javax.swing.JFrame(
	      "Star Wars: The Battle for Hoth 4K");
	  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        final java4k.thebattleforhoth4k.a applet = new a();
	  applet.setPreferredSize(new java.awt.Dimension(512, 512));
	  frame.add(applet, java.awt.BorderLayout.CENTER);
	  frame.setResizable(false);
	  frame.pack();
	  frame.setLocationRelativeTo(null);
	  frame.setVisible(true);
	  Thread.sleep(250L);
	  applet.start();
	}
}
