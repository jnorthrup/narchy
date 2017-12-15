//package java4k.vectorwars;
//
//import nars.op.data.string;
//
//import java.applet.Applet;
//import java.awt.AlphaComposite;
//import java.awt.BasicStroke;
//import java.awt.Canvas;
//import java.awt.Color;
//import java.awt.Composite;
//import java.awt.Event;
//import java.awt.Graphics2D;
//import java.awt.RadialGradientPaint;
//import java.awt.geom.AffineTransform;
//import java.awt.geom.Ellipse2D;
//import java.awt.image.BufferStrategy;
//import java.awt.image.BufferedImage;
//import java.util.ArrayList;
//
//public class A extends Applet implements Runnable {
//
//	private boolean[] keys = new boolean[32768];
//	private int mousex;
//	private int mousey;
//
//	// game states
//	private static final int STATE_FIRST_TIME = 0;
//	private static final int STATE_INIT = 1;
//	private static final int STATE_ACTIVE = 2;
//	private static final int STATE_GAME_OVER = 3;
//
//	// entity types
//	private static final int ENEMY0 = 0; // orbits the centre
//	private static final int ENEMY1 = 1; // slow homing/dodging enemy
//	private static final int ENEMY2 = 2; // turns in to an enemy generator
//	private static final int ENEMY3 = 3; // fast homing/dodging enemy
//	private static final int ENEMY4 = 4; // zig zagging homing enemy
//	private static final int ENEMY5 = 5; // fast homing enemy, doesn't dodge
//	private static final int ENEMY_GENERATOR = 6;
//	private static final int PLAYER = 7;
//	private static final int PARTICLE = 8;
//	private static final int BULLET = 9;
//	private static final int SCORE_TEXT = 10;
//	private static final int SPAWN_EFFECT = 11;
//
//	// enemyType variable indexes
//	private static final int MAX_SPEED = 1;
//	private static final int ACCELERATION = 2;
//	private static final int SPAWN_QUANTITY = 3;
//	private static final int SPAWN_DELAY = 4;
//	private static final int THINK_TIME = 5;
//	private static final int DODGE_ANGLE = 6;
//	private static final int NEXT_SPAWN = 7;
//	private static final double[][] enemyTypes = new double[][]{
//			new double[]{ENEMY0, 2, 0.1, 5, 700, 0, 0, 0},
//			new double[]{ENEMY1, 2.5, 0.1, 5, 900, 0, Math.PI/6, 0},
//			new double[]{ENEMY2, 2, 0.1, 1, 200, 1, 0, 0},
//			new double[]{ENEMY3, 4, 0.1, 1, 2300, 0, Math.PI/6, 0},
//			new double[]{ENEMY4, 2, 0.2, 1, 120, 90, Math.PI/6, 0},
//			new double[]{ENEMY5, 4, 0.5, 1, 300, 200, 0, 0},
//			new double[]{ENEMY_GENERATOR, 0, 0, 0, 0, 0, 0, 0}
//		};
//
//	// game parameters
//	private static final double FRICTION = 0.99;
//	private static final double BOUNCE = 0.9;
//	private static final double PLAYER_ACC = 0.3;
//	private static final double PLAYER_SHOT_SPEED = 10;
//	private static final int PLAYER_SHOT_DELAY = 10;
//	private static final double PLAYER_SIZE = 10;
//	private static final int ARENA_RADIUS = 512;
//	private static final int CENTRE_RADIUS = 50;
//	private static final double ENEMY_SIZE = 40;
//	private static final int SCREEN_WIDTH = 800;
//	private static final int SCREEN_HEIGHT = 600;
//	private static final int SPAWN_WARMUP_TIME = 60;
//	private static final int FLASH_TIME = 60;
//	private static final int DEATH_DELAY = 180;
//	private static final int IMMUNE_TIME = 180;
//	private static final int GENERATOR_DELAY = 300;
//	private static final int PERSPECTIVE_FACTOR = 20;
//	private static final int DIFFICULTY_INCREASE_PERIOD = 1800;
//	private static final double DIFFICULTY_SPAWN_FACTOR = 0.9;
//
//	// entity variable indexes
//	private static final int TYPE = 0;
//	private static final int DX2 = 1;
//	private static final int DY2 = 2;
//	private static final int DX = 3;
//	private static final int DY = 4;
//	private static final int X = 5;
//	private static final int Y = 6;
//	private static final int FACING = 7;
//	private static final int LIFESPAN = 8;
//	private static final int AGE = 9;
//	private static final int GROWTH = 10;
//	private static final int TARGET_FACING = 10;
//	private static final int STATE = 11;
//
//	public void start() {
//		new Thread(this).start();
//	}
//
//	public void run() {
//
//		double ratio;
//		double len;
//		double dist;
//		double distSq;
//		double distPlusVelSq;
//		double theta;
//		double x;
//		double y;
//		int i;
//		int j;
//		int k;
//		Graphics2D g2;
//		double[] bullet;
//		double[] particle;
//		double[] enemy;
//		double[] enemyType;
//		double[] entity;
//		BufferedImage image;
//		Color color;
//		final BasicStroke[] strokes = new BasicStroke[13];
//
//		boolean paused = false;
//		boolean canPause = true;
//		int deathTimer = 0;
//		int immuneTimer = 0;
//		int fireTimer = 0;
//		int flashTimer = 0;
//
//		int state = STATE_FIRST_TIME;
//
//		double[] player = new double[12];
//		final ArrayList<double[]> bullets = new ArrayList<double[]>();
//		final ArrayList<double[]> enemies = new ArrayList<double[]>();
//		final ArrayList<double[]> particles = new ArrayList<double[]>();
//		final BufferedImage images[] = new BufferedImage[10];
//		long levelTime = 0; // in frames
//		int lives = 0;
//		int score = 0;
//
//		for (i = 0; i < strokes.length; i++) {
//			strokes[i]="new";
//			basicstroke(i+1, basicstroke.cap_round, basicstroke.join_round); } setsize(800, 600); for appletviewer, remove when using an applet canvas canvas="new" canvas(); canvas.setsize(800, 600); add(canvas); canvas.createbufferstrategy(2); bufferstrategy strategy="canvas.getBufferStrategy();" load image data for each entity type final string s="\u0032\u0006\u0000\u00ff\u0000\u000f\u002d\u0005\u0019\u000f\u0005\u002d\u0019\u000f\u002d\u000f\u0005\u0032\u0005\u0000\u00ff\u00ff\u0019\u002d\u002d\u0019\u0019\u0005\u0005\u0019\u0019\u002d\u0032\u000e\u00ff\u0064\u0064\u0019\u0019\u000f\u002d\u0023\u002d\u0019\u0019\u002d\u0023\u002d\u000f\u0019\u0019\u0023\u0005\u000f\u0005\u0019\u0019\u0005\u000f\u0005\u0023\u0019\u0019\u0019\u0019\u0032\u0005\u00ff\u00ff\u0000\u002a\u0019\u0008\u0005\u0008\u002d\u002a\u0019\u0008\u0019\u0032\u0004\u00ff\u0000\u0000\u002a\u0019\u0008\u0005\u0008\u002d\u002a\u0019\u0032\u0008\u00ff\u0000\u00ff\u0019\u002d\u002d\u0019\u0019\u0005\u0005\u0019\u0019\u002d\u0019\u0005\u002d\u0019\u0005\u0019\u0032\u000e\u00ff\u00af\u00af\u0019\u0019\u000f\u002d\u0023\u002d\u0019\u0019\u002d\u0023\u002d\u000f\u0019\u0019\u0023\u0005\u000f\u0005\u0019\u0019\u0005\u000f\u0005\u0023\u0019\u0019\u0019\u0019\u0032\u0005\u00ff\u00ff\u00ff\u0005\u002d\u002d\u0019\u0005\u0005\u0019\u0019\u0005\u002d\u0018\u0002\u0000\u0000\u00ff\u0007\u000c\u000c\u000c\u000c\u0005\u00ff\u00ff\u00ff\u0005\u0005\u0007\u0005\u0007\u0007\u0005\u0007\u0005\u0005" ; for (i="k" = 0; i>< images.length; i++) { image="new" bufferedimage(s.charat(k), s.charat(k++), bufferedimage.type_4byte_abgr_pre); g2="(Graphics2D)image.getGraphics();" int[] xpoints="new" int[s.charat(k)], ypoints="new" int[s.charat(k++)]; color="new" color(s.charat(k++), s.charat(k++), s.charat(k++)); for (j="0;" j>< xpoints.length; j++) { xpoints[j]="s.charAt(k++);" ypoints[j]="s.charAt(k++);" } for (j="0;" j>< 10; j++) { g2.setcolor(j="=" 0? color.white: color); g2.setstroke(strokes[j="=" 0? 2: 13-j]); g2.setcomposite(alphacomposite.getinstance(alphacomposite.src_over, j="=" 0? 1.0f: 0.1f)); g2.drawpolyline(xpoints, ypoints, xpoints.length); } g2.dispose();
//			images[i]="image;" } int tick="0," fps="0," acc="0," acc2="0;" long lasttime=System.nanoTime();
//		while (true) { long now=System.nanoTime(); acc +=now - lasttime; tick++; if (acc>= 1000000000L) {
//				acc -= 1000000000L;
//				fps = tick;
//				tick = 0;
//			}
//
//			if (state == STATE_FIRST_TIME || state == STATE_GAME_OVER) {
//				// if player presses any mouse button, then start the game
//				if (keys[0] || keys[1] || keys[2]) {
//					state = STATE_INIT;
//				}
//
//			} else {
//
//				// initialise a new game
//				if (state == STATE_INIT) {
//					player = new double[12];
//					player[TYPE] = PLAYER;
//					player[Y] = 80;
//					lives = 3;
//					score = 0;
//					levelTime = 0;
//					immuneTimer = IMMUNE_TIME;
//					for (i = 0; i < enemytypes.length; i++) { enemytypes[i][next_spawn]="i" =="ENEMY0" || i="=" enemy1? 0: enemytypes[i][spawn_delay]; } bullets.clear(); enemies.clear(); particles.clear(); state="STATE_ACTIVE;" } if (!keys[112]) { 112="the" p key canpause="true;" } else if (canpause && keys[112]) { canpause="false;" paused="!paused;" } if (!paused) { acc2 +="now" - lasttime; while (acc2>= 16666667) {
//						acc2 -= 16666667;
//
//						// UPDATE
//
//						// update particles
//						for (i = particles.size()-1; i >= 0; i--) {
//							particle = particles.get(i);
//							if (++particle[AGE] > particle[LIFESPAN]) {
//								particles.remove(i);
//							}
//							// apply velocity
//							particle[X] += particle[DX];
//							particle[Y] += particle[DY];
//							if (particle[TYPE] == PARTICLE) {
//								// bounce off arena boundaries
//								distSq = particle[X]*particle[X] + particle[Y]*particle[Y];
//								distPlusVelSq = (particle[X]+particle[DX])*(particle[X]+particle[DX]) + (particle[Y]+particle[DY])*(particle[Y]+particle[DY]);
//								if (distSq > ARENA_RADIUS * ARENA_RADIUS && distPlusVelSq > distSq) {
//									bounce(particle, true);
//									particle[FACING] = Math.atan2(particle[DY], particle[DX]);
//								} else if (distSq < centre_radius * centre_radius && distplusvelsq>< distsq) { bounce(particle, false); particle[facing]="Math.atan2(particle[DY]," particle[dx]); } } } if (flashtimer> 0) flashTimer--;
//
//						if (deathTimer > 0) {
//							if (--deathTimer > 0) {
//								continue;
//							} else {
//								lives--;
//								immuneTimer = IMMUNE_TIME;
//							}
//						}
//						if (lives <= 0) { state="STATE_GAME_OVER;" continue; } if (immunetimer> 0) immuneTimer--;
//						if (fireTimer > 0) fireTimer--;
//						levelTime++;
//
//						// update player
//						x = mousex - 400 - (int)player[X]/2;
//						y = mousey - 300 - (int)player[Y]/2;
//						player[FACING] = Math.atan2(y, x);
//						player[DX2] = 0;
//						player[DY2] = 0;
//
//						// bounce off arena boundaries
//						distSq = player[X]*player[X] + player[Y]*player[Y];
//						distPlusVelSq = (player[X]+player[DX])*(player[X]+player[DX]) + (player[Y]+player[DY])*(player[Y]+player[DY]);
//						if (distSq > (ARENA_RADIUS - 20) * (ARENA_RADIUS - 20)) {
//							if (distPlusVelSq > distSq) {
//								bounce(player, true);
//							}
//						} else if (distSq < (centre_radius + 20) * (centre_radius + 20)) { if (distplusvelsq>< distsq) { bounce(player, false); } if player presses mouse button 1, accelerate in direction of the mouse pointer } else if (keys[0] && (x !="0" || y !="0))" { ratio="PLAYER_ACC" math.sqrt(x*x + y*y); player[dx2]="x" * ratio; player[dy2]="y" * ratio; } apply acceleration player[dx] +="player[DX2];" player[dy] +="player[DY2];" 'friction' player[dx] *="FRICTION;" player[dy] *="FRICTION;" apply velocity player[x] +="player[DX];" player[y] +="player[DY];" if player presses mouse button 3, then fire if (keys[2] && firetimer><= 0) { firetimer="PLAYER_SHOT_DELAY;" bullet="new" double[12]; bullet[type]="BULLET;" bullet[x]="player[X];" bullet[y]="player[Y];" bullet[dx]="PLAYER_SHOT_SPEED" * math.cos(player[facing]); bullet[dy]="PLAYER_SHOT_SPEED" * math.sin(player[facing]); bullet[facing]="Math.atan2(bullet[DY]," bullet[dx]); bullets.add(bullet); } spawn more enemies if required for (i="0;" i></=>< enemytypes.length; i++) { enemytype="enemyTypes[i];" if (enemytype[spawn_quantity]> 0 && levelTime >= enemyType[NEXT_SPAWN]) {
//								dist = CENTRE_RADIUS*2 + Math.random() * (ARENA_RADIUS - CENTRE_RADIUS*3);
//								theta = Math.random() * Math.PI * 2;
//								x = dist * Math.cos(theta);
//								y = dist * Math.sin(theta);
//								for (j = 0; j < enemytype[spawn_quantity]; j++) { enemy="new" double[12]; enemy[type]="enemyType[TYPE];" enemy[x]="x" + 0.1*j; enemy[y]="y" + 0.1*j; enemy[age]="-SPAWN_WARMUP_TIME;" enemies.add(enemy); } particle="new" double[12]; particle[type]="SPAWN_EFFECT;" particle[lifespan]="SPAWN_WARMUP_TIME;" particle[x]="x;" particle[y]="y;" particles.add(particle); enemytypes[i][next_spawn]="levelTime" + enemytypes[i][spawn_delay] * math.pow(difficulty_spawn_factor, (int)(leveltime/difficulty_increase_period)); } } update bullets for (i="bullets.size()-1;" i>= 0; i--) {
//							bullet = bullets.get(i);
//							// apply velocity
//							bullet[X] += bullet[DX];
//							bullet[Y] += bullet[DY];
//							distSq = bullet[X]*bullet[X] + bullet[Y]*bullet[Y];
//							if (distSq < centre_radius * centre_radius || distsq> ARENA_RADIUS * ARENA_RADIUS) {
//								bullets.remove(i);
//							}
//						}
//
//						// update enemies
//						for (i = enemies.size()-1; i >= 0; i--) {
//							enemy = enemies.get(i);
//							if (++enemy[AGE] < 0) continue; boolean dodge="false;" enemytype="enemyTypes[(int)enemy[TYPE]];" enemy[dx2]="0;" enemy[dy2]="0;" if (enemy[type]="=" enemy_generator) { rotate enemy[facing]="Math.PI" * 2 * (enemy[age]%(int)enemy[state]) enemy[state]; generate new enemy if required if (enemy[age]>= enemy[GROWTH]) {
//									entity = new double[12];
//									entity[TYPE] = ENEMY1;
//									entity[X] = enemy[X];
//									entity[Y] = enemy[Y];
//									entity[AGE] = -SPAWN_WARMUP_TIME;
//									enemies.add(entity);
//									particle = new double[12];
//									particle[TYPE] = SPAWN_EFFECT;
//									particle[LIFESPAN] = SPAWN_WARMUP_TIME;
//									particle[X] = enemy[X];
//									particle[Y] = enemy[Y];
//									particles.add(particle);
//									// speed up enemy generation time
//									if (enemy[STATE] > SPAWN_WARMUP_TIME*2) {
//										enemy[STATE] -= 20;
//									}
//									// set next generation time
//									enemy[GROWTH] = enemy[AGE] + enemy[STATE];
//								}
//
//							} else if (enemy[TYPE] == ENEMY0) {
//								// orbit the arena centre
//								len = Math.sqrt(enemy[X]*enemy[X] + enemy[Y]*enemy[Y]);
//								enemy[DX] = enemyType[MAX_SPEED] * enemy[Y]/len;
//								enemy[DY] = enemyType[MAX_SPEED] * -enemy[X]/len;
//
//							} else if (enemy[TYPE] == ENEMY1 || enemy[TYPE] == ENEMY3) {
//								// attempt to home in on player
//								x = player[X]-enemy[X];
//								y = player[Y]-enemy[Y];
//								dist = Math.sqrt(x*x + y*y);
//								ratio = enemyType[ACCELERATION] / dist;
//								theta = player[FACING] - Math.atan2(-y, -x);
//								if (enemyType[DODGE_ANGLE] > 0 && dist < 400 && math.abs(theta)>< enemytype[dodge_angle]) { player is close and facing enemy, attempt to dodge at 90 degrees, with double acceleration enemy[dx2]="(theta">= 0? -y: y) * ratio * 2;
//									enemy[DY2] = (theta < 0? -x: x) * ratio * 2; dodge="true;" } else { player is not close and facing enemy, home in on player enemy[dx2]="x" * ratio; enemy[dy2]="y" * ratio; } } else if (enemy[type]="=" enemy2) { attempt to tag arena centre, then escape to arena edge distsq="enemy[X]*enemy[X]" + enemy[y]*enemy[y]; dist="Math.sqrt(distSq);" if not tagged centre, the move towards the centre if (enemy[state]="=" 0) { ratio="enemyType[ACCELERATION]" dist; enemy[dx2]="-enemy[X]" * ratio; enemy[dy2]="-enemy[Y]" * ratio; if close enough, then mark this enemy as tagged if (dist>< enemy_size + centre_radius) { enemy[state]="1;" } } else { if already tagged, then attempt to reach arena edge if (distsq>= (ARENA_RADIUS - ENEMY_SIZE/2) * (ARENA_RADIUS - ENEMY_SIZE/2)) {
//										// enemy has tagged and escaped - convert to enemy generator.
//										enemy[TYPE] = ENEMY_GENERATOR;
//										enemy[STATE] = GENERATOR_DELAY;
//										enemy[GROWTH] = enemy[AGE] + enemy[STATE];
//									} else {
//										// move towards arena edge
//										ratio = enemyType[ACCELERATION] / dist;
//										enemy[DX2] = enemy[X] * ratio;
//										enemy[DY2] = enemy[Y] * ratio;
//									}
//								}
//
//							} else if (enemy[TYPE] == ENEMY4 || enemy[TYPE] == ENEMY5) {
//								// moves towards player, re-targetting every THINK_TIME frames.
//								// if DODGE_ANGLE is non-zero, then enemy zig-zags at an offset angle.
//								// if closer than 100 pixels then simply home in on the player.
//								int timer = (int)enemy[AGE]%(int)enemyType[THINK_TIME];
//								if (timer == 0) {
//									x = player[X]-enemy[X];
//									y = player[Y]-enemy[Y];
//									dist = Math.sqrt(x*x + y*y);
//									enemy[TARGET_FACING] = Math.atan2(y, x);
//									if (enemyType[DODGE_ANGLE] != 0) {
//										enemy[TARGET_FACING] += (dist < 100? 0: (enemytype[think_time]="=" 0 || ((int)enemy[age]%((int)enemytype[think_time]*2)="=" 0)? enemytype[dodge_angle]: -enemytype[dodge_angle])); } } enemy[dx2]="enemyType[ACCELERATION]" * math.cos(enemy[target_facing]); enemy[dy2]="enemyType[ACCELERATION]" * math.sin(enemy[target_facing]); } use repulsive force to move enemy away from other enemies of the same type for (j="i+1;" j>< enemies.size(); j++) { entity="enemies.get(j);" if (entity[type]="=" enemy[type]) { x="enemy[X]-entity[X];" y="enemy[Y]-entity[Y];" distsq="x*x" + y*y; if (distsq>< enemy_size * enemy_size) { dist="Math.sqrt(distSq);" ratio="0.5" * (enemy_size - dist) dist; accelerate away from other enemy enemy[dx2] +="x" * ratio; enemy[dy2] +="y" * ratio; } } } bounce off arena boundaries distsq="enemy[X]*enemy[X]" + enemy[y]*enemy[y]; distplusvelsq="(enemy[X]+enemy[DX])*(enemy[X]+enemy[DX])" + (enemy[y]+enemy[dy])*(enemy[y]+enemy[dy]); if (distsq> (ARENA_RADIUS - 20) * (ARENA_RADIUS - 20) && distPlusVelSq > distSq) {
//								bounce(enemy, true);
//							} else if (distSq < (centre_radius + 20) * (centre_radius + 20) && distplusvelsq>< distsq) { bounce(enemy, false); } apply acceleration enemy[dx] +="enemy[DX2];" enemy[dy] +="enemy[DY2];" cap velocity double maxspeed="dodge?" enemytype[max_speed] * 2: enemytype[max_speed]; if (enemy[dx]*enemy[dx] + enemy[dy]*enemy[dy]> maxSpeed*maxSpeed) {
//								len = Math.sqrt(enemy[DX]*enemy[DX] + enemy[DY]*enemy[DY]);
//								enemy[DX] *= maxSpeed/len;
//								enemy[DY] *= maxSpeed/len;
//							}
//
//							// apply velocity
//							enemy[X] += enemy[DX];
//							enemy[Y] += enemy[DY];
//
//							// face velocity direction
//							if (enemy[DX] != 0 && enemy[DY] != 0) {
//								enemy[FACING] = Math.atan2(enemy[DY], enemy[DX]);
//							}
//						}
//
//						// COLLISION
//
//						// player-enemy
//						if (immuneTimer <= 0) { for (i="0;" i></=>< enemies.size(); i++) { enemy="enemies.get(i);" if (enemy[age]>< 0) continue; if ((player[x] - enemy[x]) * (player[x] - enemy[x]) + (player[y] - enemy[y]) * (player[y] - enemy[y])>< (player_size/2 + enemy_size/2) * (player_size/2 + enemy_size/2)) { deathtimer="DEATH_DELAY;" flashtimer="FLASH_TIME;" bullets.clear(); enemies.clear(); player[dx]="0;" player[dy]="0;" explosion for (j="0;" j>< 25; j++) { particle="new" double[12]; particle[type]="PARTICLE;" particle[lifespan]="100;" particle[x]="player[X];" particle[y]="player[Y];" particle[dx]="Math.random()" * 16 - 8; particle[dy]="Math.random()" * 16 - 8; particle[facing]="Math.atan2(particle[DY]," particle[dx]); particle[growth]="0.02;" particles.add(particle); } } } } enemy-bullet for (i="bullets.size()-1;" i>= 0; i--) {
//							bullet = bullets.get(i);
//							for (j = enemies.size()-1; j >= 0; j--) {
//								enemy = enemies.get(j);
//								if (enemy[AGE] < 0) continue; if ((bullet[x] - enemy[x]) * (bullet[x] - enemy[x]) + (bullet[y] - enemy[y]) * (bullet[y] - enemy[y])>< enemy_size/2 * enemy_size/2) { flashtimer="FLASH_TIME;" bullets.remove(i); if this is an enemy generator, then slow its generation rate if (enemy[type]="=" enemy_generator) { if (enemy[state]>< generator_delay) { enemy[state]+="100;" } enemy[growth]="enemy[AGE]" + enemy[state]; } else { otherwise destroy the enemy and score points enemies.remove(j); dist="Math.sqrt((player[X]" - enemy[x]) * (player[x] - enemy[x]) + (player[y] - enemy[y]) * (player[y] - enemy[y])); ratio="dist">< 200? (210-dist)/10: 1; proximity bonus multiplier k="100*(int)ratio;" score+="k;" particle="new" double[12]; particle[type]="SCORE_TEXT;" particle[state]="k;" particle[lifespan]="50;" particle[x]="enemy[X];" particle[y]="enemy[Y];" particle[dy]="-1;" particles.add(particle); explosion for (k="0;" k>< 25; k++) { particle="new" double[12]; particle[type]="PARTICLE;" particle[lifespan]="100;" particle[x]="enemy[X];" particle[y]="enemy[Y];" particle[dx]="Math.random()" * 16 - 8; particle[dy]="Math.random()" * 16 - 8; particle[facing]="Math.atan2(particle[DY]," particle[dx]); particle[growth]="0.02;" particles.add(particle); } } break; } } } } } } lasttime="now;" render g2="(Graphics2D)strategy.getDrawGraphics();" affinetransform at="g2.getTransform();" composite ac="g2.getComposite();" background g2.setbackground(color.black); g2.clearrect(0, 0, screen_width, screen_height); g2.setclip(0, 0, screen_width, screen_height); cycle colours over time ratio="(50" + flashtimer)/255.0; color="Color.getHSBColor(levelTime%1530/1530.0f," 1.0f, (float)ratio); outer boundary + concentric circles with simple perspective transform for (i="PERSPECTIVE_FACTOR," j="0," k="6;" i>< 36; i+="2," k--) { g2.setcolor(i="=" perspective_factor? color.white: color); g2.setstroke(strokes[k>< 0? 0: k]); g2.settransform(at); g2.translate(screen_width/2 - (player[x]/2 * perspective_factor/i), screen_height/2 - (player[y]/2 * perspective_factor/i)); j="(int)(ARENA_RADIUS" * perspective_factor/i); g2.drawoval(-j, -j, 2*j, 2*j); } grid g2.setclip(new ellipse2d.double(-j, -j, 2*j, 2*j)); for (i="-j;" i>< j; i+="40)" { g2.drawline(i, -j, i, j); g2.drawline(-j, i, j, i); } translate to ensure player is on screen g2.settransform(at); g2.setclip(0, 0, screen_width, screen_height); g2.translate(screen_width/2 - player[x]/2, screen_height/2 - player[y]/2); affinetransform at2="g2.getTransform();" draw inner boundary color="Color.getHSBColor(levelTime%1530/1530.0f," 1.0f, (float)1.0f); g2.setpaint(new radialgradientpaint(0, 0, 2*centre_radius, new float[]{0.0f, 1.0f}, new color[]{color, new color(0, 0, 0, 0)})); g2.filloval(-centre_radius*2, -centre_radius*2, 2*centre_radius*2, 2*centre_radius*2); inner boundary g2.setcolor(color.white); g2.filloval(-centre_radius, -centre_radius, 2*centre_radius, 2*centre_radius); inner boundary draw the entities for (i="0;" i>< 4; i++) {><double[]> entities = i == 0? null: i==1? bullets: i==2? enemies: particles;
//				for (j = 0; j < (i="=" 0? 1: entities.size()); j++) { entity="i" =="0?" player: entities.get(j); only draw entity if on screen if (math.abs(entity[x] - player[x]/2)>< (screen_width/2 + 50) && math.abs(entity[y] - player[y]/2)>< (screen_height/2 + 50)) { g2.settransform(at2); g2.setcomposite(ac); g2.translate(entity[x], entity[y]); g2.rotate(entity[facing]); float alpha="1.0f;" switch (i) { case 0: alpha="deathTimer"> 0 || lives <= 0? 0.0f: immunetimer> 0? (levelTime%60)/60.0f: 1.0f;
//							break;
//						case 3:
//							if (entity[GROWTH] > 0) {
//								g2.scale(1 + (entity[GROWTH] * entity[AGE]), 1 + (entity[GROWTH] * entity[AGE]));
//							}
//							alpha = 1.0f-(float)(entity[AGE]/entity[LIFESPAN]);
//						}
//						g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
//						switch ((int)entity[TYPE]) {
//						case SCORE_TEXT:
//							g2.scale(3,3);
//							g2.setColor(Color.WHITE);
//							g2.drawString(String.valueOf((int)entity[STATE]), -10, 0);
//							break;
//						case SPAWN_EFFECT:
//							g2.setColor(Color.BLUE);
//							g2.fillRect((int)(-40),(int)(-40*alpha),(int)(80),(int)(80*alpha));
//							g2.fillRect((int)(-40*alpha),(int)(-40),(int)(80*alpha),(int)(80));
//							break;
//						default:
//							image = images[(int)entity[TYPE]];
//							g2.drawImage(image, -image.getWidth()/2, -image.getHeight()/2, null);
//						}
//					}
//				}
//			}
//			g2.setTransform(at);
//			g2.setComposite(ac);
//
//			// number of lives
//			for (i = 0; i < lives; i++) { g2.settransform(at); g2.translate(760 - i*50, 50); g2.rotate(-math.pi/2); g2.drawimage(images[player], -images[player].getwidth()/2, -images[player].getwidth()/2, null); } score g2.settransform(at); g2.setcolor(color.light_gray); g2.scale(5,5); g2.drawstring(string.valueof(score), 6, 14); if (state="=" state_first_time) { g2.drawstring("click to start", 30, 60); } else if (state="=" state_game_over) { g2.drawstring("game over", 43, 60); } else if (paused) { g2.drawstring("paused", 55, 60); } g2.settransform(at); g2.drawstring("fps: " + string.valueof(fps), 20, 580); g2.dispose(); strategy.show(); try { thread.sleep(1); } catch (exception e) {} while (system.nanotime() - lasttime>< 16666667) { thread.yield(); } if (!isactive()) { return; } } } bounce entity off arena centre/arena edge private void bounce(double[] entity, boolean inwards) { double len="Math.sqrt(entity[X]*entity[X]" + entity[y]*entity[y]); unit vector normal to the 'plane' i.e. perpendicular to circumference at approximate point of contact double normalx="(inwards?" 1: -1) * entity[x]/len; double normaly="(inwards?" 1: -1) * entity[y]/len; double normaldot="normalx" * entity[dx] + normaly * entity[dy]; velocity vector in direction of normal double velnormalx="normalx" * normaldot; double velnormaly="normaly" * normaldot; velocity vector perpendicular to normal. double velperpx="entity[DX]" - velnormalx; double velperpy="entity[DY]" - velnormaly; velocity in direction of normal switches direction; velocity in direction of perpendicular remains the same. entity[dx]="BOUNCE" * (-velnormalx + velperpx); entity[dy]="BOUNCE" * (-velnormaly + velperpy); } public boolean handleevent(event e) { switch (e.id) { case event.key_press: case event.key_release: keys[e.key]="e.id" =="Event.KEY_PRESS;" break; case event.mouse_down: case event.mouse_up: mouse button state: key[0]="left" button, key[1]="middle" button, key[2]="right" button keys[(e.modifiers & event.meta_mask) !="0?" 2: (e.modifiers & event.alt_mask) !="0?" 1: 0]="e.id" =="Event.MOUSE_DOWN;" break; case event.mouse_move: case event.mouse_drag: mousex="e.x;" mousey="e.y;" break; } return false; }></=></double[]></=></double[]></double[]></double[]></double[]></double[]></double[]>