package java4k.s23;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

public class A extends Applet implements Runnable {
    static boolean[] keys = new boolean[65536];
	static final double PI = 3.1415;
	static final int enemynum = 80;
	static final int bulletnum = 8000;
	static final int itemnum = 8000;
	static final int grazenum = 2000;

	@Override
    public void start() {
		enableEvents(AWTEvent.KEY_EVENT_MASK);
		new Thread(this).start();
	}

	@Override
    public void run() {
        for (boolean newGame = true; ;) {
			if (newGame) {
				l();
				newGame = false;
			}

			if (keys[KeyEvent.VK_R])
				newGame = true;

			if (!isActive())
				return;
		}
	}

	public void l() {
		setSize(480, 480); 

		Random rndLvl = new Random(23L), rndItemStar = new Random(1337L);

        BufferedImage screen = new BufferedImage(480, 480, BufferedImage.TYPE_INT_RGB);
        Graphics g = screen.getGraphics();
        Graphics appletGraphics = getGraphics();

		
		

		int i;


        int[][] enemy = new int[enemynum][4];
        int[] enemyStack = new int[enemynum];


        int[][] ibullet = new int[bulletnum][3];
        int[] bulletStack = new int[bulletnum];


        double[][] item = new double[itemnum][6];
        int[] itemStack = new int[itemnum];


        double[][] graze = new double[grazenum][5];
        int[] grazeStack = new int[grazenum];

        for (i = 0; i < itemnum; ++i) {
			if (i < enemynum) {
				enemy[i][2] = 0;
				enemyStack[i] = i;
			}
			if (i < bulletnum) {
				ibullet[i][0] = 0;
				bulletStack[i] = i;
			}
			if (i < grazenum) {
				graze[i][4] = (double) 0;
				grazeStack[i] = i;
			}
			item[i][1] = -10.0;
			itemStack[i] = i;
		}

		
		

		Graphics g2;
        BufferedImage gplayer = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        BufferedImage genemy = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        BufferedImage gitem = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage gbullet = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        BufferedImage gbg = new BufferedImage(480, 480, BufferedImage.TYPE_INT_RGB);


        long playerG = 35466898305473L, enemyG = 562949815469064L;
        int j;
        for (i = 6; i >= 0; --i)
			for (j = 6; j >= 0; --j) {
				g2 = gplayer.getGraphics();
				g2.setColor(new Color(128, 0, 16));
				if (playerG % 2L == 1L)
					g2.fillRect(4 * j, 4 * i, 4, 4);
				playerG /= 2L;

				g2.setColor(new Color(16, 128, 64));
				g2.fillRect(12, 8, 4, 4);

				g2 = genemy.getGraphics();
				g2.setColor(new Color(16, 128, 64));
				if (enemyG % 2L == 1L)
					g2.fillRect(4 * j, 4 * i, 4, 4);
				enemyG /= 2L;
			}

		
		g2 = gitem.getGraphics();
		g2.setColor(Color.blue);
		g2.fillRect(0, 0, 10, 10);

		
		g2 = gbullet.getGraphics();
		for (i = 0; i < 10; i++) {
			g2.setColor(Color.getHSBColor(0f, 0.625f - (float) i / 20f, 0.75f + (float) i / 40f));
			g2.fillOval(i, i, 20 - 2 * i, 20 - 2 * i);
		}


        double[][] star = new double[75][2];
        for (i = 0; i < 75; ++i) {
			star[i][0] = (double) rndItemStar.nextInt(480);
			star[i][1] = (double) rndItemStar.nextInt(480);
		}

		g2 = gbg.getGraphics();
		g2.setColor(Color.black);
		g2.fillRect(0, 0, 480, 480);

        double t2;
        double t1;
        int stari;
        for (stari = 700; stari > 0; --stari) {
			t2 = rndItemStar.nextDouble() * 320.0 - 60.0;
			t1 = t2 * t2 * 0.0034;

			i = (int) t2 + 60;
			t1 += (double) (rndItemStar.nextInt(i) - i / 2) / 1.5;

			g2.setColor(Color.getHSBColor(0.9f, 0.4f, 0.5f + (float) i / 640f));
			g2.fillRect((int) t1 + 160, (int) t2 + 220, 1, 1);
		}

        Color[] bgCol = {new Color(64, 20, 64), new Color(160, 60, 160), new Color(112, 112, 140)};
        for (stari = 1200; stari > 0; --stari) {
			i = rndItemStar.nextInt(300);
            int d = i - 150;
			if (d < 0)
				d *= -1;
			d = 180 - d;

			j = i + rndItemStar.nextInt(d) - d / 2;
			g2.setColor(bgCol[rndItemStar.nextInt(2)]);
			if (rndItemStar.nextInt(2) == 0)
				g2.setColor(Color.getHSBColor(0.8f, 0.2f, (float) d / 200f));
			g2.fillRect(i + 50, j + 50, 1, 1);

			if (rndItemStar.nextInt(3) == 0) {
				d /= 3;
				j = i + rndItemStar.nextInt(d) - d / 2;
				g2.setColor(bgCol[2]);
				g2.fillRect(i + 50, j + 50, 1, 1);
			}
		}

		for (stari = 480; stari > 0; --stari) {
			i = rndItemStar.nextInt(100);
			j = rndItemStar.nextInt(100);
            int d = (i - 50) * (i - 50) + (j - 50) * (j - 50);
			g2.setColor(Color.getHSBColor(0.8f, 0.2f, 1.0F - (float) d / 6000f));
			g2.fillRect(i + 140, j + 130, 1, 1);
			g2.fillRect(rndItemStar.nextInt(480), rndItemStar.nextInt(480), 1, 1);
		}

		
		

		int fps = 0, fpsout = 0, frame = 0;
        long lastfpsoutt = System.nanoTime();
        int grazeStacki = grazenum;
        int[] item2 = {0, 490, 600, 1};
        int itemStacki = itemnum;
        int bulletStacki = bulletnum;
        double[][] bullet = new double[bulletnum][8];
        double[][] enemyB = new double[enemynum][9];
        int enemyStacki = enemynum;
        int nextEnemyDiff = 180;
        int nextEnemy = 0;
        int multiplier = 1;
        int score = 0;
        int lastDeath = -120;
        int lives = 3;
        int lastShot = -1380;
        int playerSpeed = 1;
        int shotY = -30;
        int playerY = 400;
        int playerX = 225;
        while (lives >= 0) {
            long lastfpst = System.nanoTime();


            playerSpeed = 2;
			if (keys[KeyEvent.VK_Y] || keys[KeyEvent.VK_Z])
				playerSpeed = 4;
			if (keys[KeyEvent.VK_SHIFT])
				playerSpeed = 1;

			if (keys[KeyEvent.VK_ESCAPE])
				lives = -1;
			if (keys[KeyEvent.VK_UP] && playerY > 2)
				playerY -= playerSpeed;
			if (keys[KeyEvent.VK_DOWN] && playerY < 448)
				playerY += playerSpeed;
			if (keys[KeyEvent.VK_LEFT] && playerX > 2)
				playerX -= playerSpeed;
			if (keys[KeyEvent.VK_RIGHT] && playerX < 448)
				playerX += playerSpeed;

			
			if ((keys[KeyEvent.VK_X] || keys[KeyEvent.VK_SPACE]) && frame - lastShot >= 1380) {
				lastShot = frame;
				shotY = playerY;
				multiplier = 1;
			}

			if (shotY > -30)
				shotY -= 4;

			
			for (i = 0; i < itemnum; ++i) {
				if (item[i][1] > -1.0) {
					item[i][0] += item[i][2];
					item[i][1] += item[i][3];

                    int dir = item[i][2] > 0.0625 ? -1 : 1;
					item[i][2] += (double) dir * item[i][4];
					if (item[i][3] < 5.0)
						item[i][3] += item[i][5];

					if (item[i][1] > 480.0) {
						item[i][1] = -10.0;
						itemStack[itemStacki++] = i;
					}
					if (item[i][0] > (double) (playerX - 5) && item[i][0] < (double) (playerX + 25) && item[i][1] > (double) (playerY - 5) && item[i][1] < (double) (playerY + 25)) {
						score += 20;
						item[i][1] = -10.0;
						itemStack[itemStacki++] = i;
					}
				}
			}

			
			if (item2[1] <= 480) {
				item2[1]++;
				if (item2[1] > 480)
					item2[3] = 1;
			}
			if (item2[2] == frame) {
				item2[0] = rndItemStar.nextInt(450);
				item2[1] = -30;
				item2[2] += 900;
			}
			if (playerX + 18 > item2[0] && playerX < item2[0] + 18 && playerY + 18 > item2[1] && playerY < item2[1] + 18) {
				score += item2[3] * 500;
				if (item2[3] < 32)
					item2[3] *= 2;
				item2[1] = 490;
			}

			
			if (frame >= nextEnemy) {
				if (enemyStacki > 0)
					enemyStacki--;
                int ei = enemyStack[enemyStacki];
				enemy[ei][0] = rndLvl.nextInt(400) + 25;
				enemy[ei][1] = -30;
				enemy[ei][2] = 1;

				nextEnemy = frame + nextEnemyDiff + rndLvl.nextInt(60 + nextEnemyDiff);
				if (nextEnemyDiff >= 2)
					nextEnemyDiff -= 2;

				enemyB[ei][0] = (double) (rndLvl.nextInt(100) + 50);
				t1 = enemyB[ei][1] = (double) rndLvl.nextInt(6);
				enemyB[ei][2] = (double) (60 + rndLvl.nextInt(4) * 20);
				enemyB[ei][3] = 5.0;
				enemyB[ei][4] = 1.5;
				enemyB[ei][5] = 20.0;
				enemyB[ei][6] = (double) rndLvl.nextInt(2);
				enemyB[ei][7] = 1.5;
				enemyB[ei][8] = (double) (60 * (rndLvl.nextInt(2) + 1));

				if (t1 == 2.0)
					enemyB[ei][2] = (double) (25 * rndLvl.nextInt(5) + 100);
				if (t1 == 1.0 || t1 == 2.0) {
					enemyB[ei][5] = 10.0;
					enemyB[ei][6] = (double) 0;
					enemyB[ei][7] = (double) 0;
					enemyB[ei][8] = -1.0;
				}
			}

			
			for (i = 0; i < enemynum; ++i) {
				if (enemy[i][2] > 0) {
					if (enemy[i][1] > shotY && enemy[i][1] < shotY + 20 && shotY > -30) {
						enemy[i][2] = 0;
						enemyStack[enemyStacki++] = i;

						score += 100 * multiplier;
						if (frame - lastDeath > 300 && multiplier < 64)
							multiplier *= 2;

						for (j = 0; j < 100; ++j) {
							if (itemStacki > 0)
								itemStacki--;
                            int ii = itemStack[itemStacki];
							item[ii][0] = (double) enemy[i][0];
							item[ii][1] = (double) enemy[i][1];
							item[ii][2] = (double) rndItemStar.nextInt(100) / 40.0 - 1.25;
							item[ii][3] = (double) rndItemStar.nextInt(100) / 50.0 + 0.5;
							item[ii][4] = (double) rndItemStar.nextInt(10) * 0.002;
							item[ii][5] = (double) rndItemStar.nextInt(10) * 0.0005 + 0.01;
						}
					}

					
					if (enemy[i][2] == 1) {
						enemy[i][1]++;
						if ((double) enemy[i][1] > enemyB[i][0]) {
							enemy[i][2]++;
							enemy[i][3] = 0;
						}
					}

					
					if (enemy[i][2] == 2 && --enemy[i][3] <= 0) {
						enemy[i][3] = 500;
						for (j = 0; (double) j < enemyB[i][2]; ++j) {
							if (bulletStacki > 0)
								bulletStacki--;
                            int bi = bulletStack[bulletStacki];
							ibullet[bi][0] = 1;
							ibullet[bi][2] = 0;
							bullet[bi][0] = (double) enemy[i][0];
							bullet[bi][1] = (double) enemy[i][1];

							bullet[bi][3] = enemyB[i][4];
							bullet[bi][5] = enemyB[i][6];
							bullet[bi][6] = enemyB[i][7];
							bullet[bi][7] = enemyB[i][8];

							stari = (int) (enemyB[i][2] / enemyB[i][3]);

							
							if (enemyB[i][1] == (double) 0 || enemyB[i][1] == 4.0 || enemyB[i][1] == 5.0) {
								bullet[bi][2] = (double) j * 2.0 * PI * (enemyB[i][3] / enemyB[i][2]);
								bullet[bi][4] = enemyB[i][5] * (double) (j / stari);

								if (enemyB[i][1] == 4.0)
									bullet[bi][2] -= (double) (j / stari) * PI * 0.04;
								if (enemyB[i][1] == 5.0) {
									bullet[bi][6] = enemyB[i][7] * (1.0 + 0.2 * (double) j / (double) stari);
									bullet[bi][7] = enemyB[i][5] * (enemyB[i][3] - 1.0) - bullet[bi][4];
								}
							}
							if (enemyB[i][1] == 1.0) {
								bullet[bi][2] = ((double) j / enemyB[i][3]) * PI * 0.04 + ((double) j % enemyB[i][3]) * 2.0 * PI / enemyB[i][3];
								bullet[bi][4] = enemyB[i][5] * ((double) j / enemyB[i][3]);
							}
							if (enemyB[i][1] == 2.0) {
                                int wavenum = 5 * (int) enemyB[i][3];
								bullet[bi][2] = (double) (j / wavenum) * PI * 0.04 + ((double) j % enemyB[i][3]) * 2.0 * PI / enemyB[i][3];
								bullet[bi][4] = enemyB[i][5] * (((double) (j % wavenum) / enemyB[i][3]) + (double) (j / wavenum) * (enemyB[i][3] + 2.0));
							}
							if (enemyB[i][1] == 3.0) {
								bullet[bi][2] = (-PI / 8.0) - (double) ((j / 2) % 5) * (PI / 20.0);
								if (j % 2 == 1)
									bullet[bi][2] = -PI - bullet[bi][2];
								bullet[bi][4] = enemyB[i][5] * (double) (j / 10);
								bullet[bi][5] = 1.0;
							}

						}
					}
				}
			}

			
			for (i = 0; i < bulletnum; ++i) {
				if (ibullet[i][0] > 0) {
					if (bullet[i][1] > (double) shotY && bullet[i][1] < (double) (shotY + 20)) {
						ibullet[i][0] = 0;
						bulletStack[bulletStacki++] = i;
					}

					if (ibullet[i][0] == 1 && --bullet[i][4] <= (double) 0) {
						ibullet[i][0]++;
						ibullet[i][1] = 30;
						while (bullet[i][2] > PI)
							bullet[i][2] -= 2.0 * PI;
					}

					if (ibullet[i][0] == 2 && --bullet[i][7] == -1.0) {
						ibullet[i][0]++;
						if (bullet[i][5] == 1.0) {
							t1 = (double) playerX - bullet[i][0] + 5.0;
							t2 = (double) playerY - bullet[i][1] + 5.0;
							bullet[i][5] = Math.acos(t1 / Math.sqrt(t1 * t1 + t2 * t2)); 
							if (t2 > (double) 0)
								bullet[i][5] *= -1.0;
						} else
							bullet[i][5] = bullet[i][2];
						bullet[i][5] -= bullet[i][2];
					}

					if (ibullet[i][0] == 2 || ibullet[i][0] == 3) {
						if (ibullet[i][0] == 3 && ibullet[i][1] > 0)
							ibullet[i][1]--;
						t1 = bullet[i][2] + bullet[i][5] * (double) (30 - ibullet[i][1]) / 30.0;
						t2 = (double) ibullet[i][1] / 30.0 * bullet[i][3] + (double) (30 - ibullet[i][1]) / 30.0 * bullet[i][6];
						bullet[i][0] += Math.cos(t1) * t2;
						bullet[i][1] -= Math.sin(t1) * t2;

						if (bullet[i][0] < -20.0 || bullet[i][0] > 480.0 || bullet[i][1] < -20.0 || bullet[i][1] > 480.0) {
							ibullet[i][0] = 0;
							bulletStack[bulletStacki++] = i;
						}

						t1 = ((double) (playerX + 4) - bullet[i][0]) * ((double) (playerX + 4) - bullet[i][0]) + ((double) playerY - bullet[i][1]) * ((double) playerY - bullet[i][1]);
						if (t1 < 100.0 && frame - lastDeath > 120) {
							playerX = 225;
							playerY = 400;
							lives--;
							lastDeath = frame;
						}
						if (t1 < 1600.0 && ibullet[i][2] == 0) {
							ibullet[i][2] = 1;
							score += 20;

							if (grazeStacki > 0)
								grazeStacki--;
                            int gi = grazeStack[grazeStacki];
							graze[gi][0] = (double) (playerX + 16);
							graze[gi][1] = (double) (playerY + 12);
							graze[gi][2] = 2.0 * rndItemStar.nextDouble() - 1.0;
							graze[gi][3] = 2.0 * rndItemStar.nextDouble() - 1.0;
							graze[gi][4] = 60.0;
						}
					}
				}
			}

			
			for (i = 0; i < grazenum; ++i)
				if (graze[i][4] > (double) 0) {
					graze[i][0] += graze[i][2];
					graze[i][1] += graze[i][3];
					if (--graze[i][4] <= (double) 0)
						grazeStack[grazeStacki++] = i;
				}

			

			g.drawImage(gbg, 0, 0, null);

			
			for (i = 1, stari = 0; i <= 5; ++i)
				for (j = 0; j < i * 5; ++j, ++stari) {
					star[stari][1] += (double) (26 - i * i) * 0.01;
					if (star[stari][1] > 480.0) {
						star[stari][0] = (double) rndItemStar.nextInt(480);
						star[stari][1] = (double) (-rndItemStar.nextInt(30) - 10);
					}
					g.setColor(Color.white);
					g.fillRect((int) star[stari][0], (int) star[stari][1], (6 - i), (6 - i));
				}

			if (item2[1] <= 480) {
				g.setColor(Color.blue);
				g.fillRect(item2[0], item2[1], 30, 30);
			}
			for (i = 0; i < enemynum; ++i)
				if (enemy[i][2] > 0)
					g.drawImage(genemy, enemy[i][0], enemy[i][1], null);
			if (frame - lastDeath > 120 || (frame / 10) % 3 > 0)
				g.drawImage(gplayer, playerX, playerY, null);
			for (i = 0; i < itemnum; ++i)
				if (item[i][1] > -1.0)
					g.drawImage(gitem, (int) item[i][0], (int) item[i][1], null);
			for (i = 0; i < bulletnum; ++i)
				if (ibullet[i][0] > 1)
					g.drawImage(gbullet, (int) bullet[i][0], (int) bullet[i][1], null);
			g.setColor(Color.pink);
			for (i = 0; i < grazenum; ++i)
				if (graze[i][4] > (double) 0)
					g.fillRect((int) graze[i][0], (int) graze[i][1], 2, 2);
			if (shotY > -30)
				g.fillRect(0, shotY, 480, 20);

            int cd = (1439 - frame + lastShot) / 60;
			if (cd < 0)
				cd = 0;
			g.setColor(Color.white);
			g.drawString("Score: " + score, 5, 15);
			g.drawString("Lives: " + lives, 5, 30);
			g.drawString("Next Bomb: " + cd, 5, 45);
			g.drawString("FPS: " + fpsout, 430, 475);

			appletGraphics.drawImage(screen, 0, 0, null);

			do {
				Thread.yield();
			} while (System.nanoTime() - lastfpst < 16600000L);

			fps++;
			frame++;
			if (System.nanoTime() - lastfpsoutt >= 1000000000L) {
				fpsout = fps;
				fps = 0;
				lastfpsoutt += 1000000000L;
			}

			if (!isActive())
				return;
		}

		g.drawString("GAME OVER", 200, 220);
		g.drawString("Press 'R' to Restart", 190, 240);
		appletGraphics.drawImage(screen, 0, 0, null);
	}

	@Override
    public void processKeyEvent(KeyEvent e) {
		keys[e.getKeyCode()] = (e.getID() == KeyEvent.KEY_PRESSED);
	}
}