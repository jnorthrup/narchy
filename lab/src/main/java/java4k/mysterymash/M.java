package java4k.mysterymash;

import java.applet.Applet;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Mystery Mash-up game.
 *
 * Starts as Pong, then adds elements from these games:
 *  * Pong
 *       http:
 *  * Break-Out
 *       Atari:
 *       http:
 *       Arcade:
 *       http:
 *  * Space Invaders
 *       http:
 *  * Tetris
 *  * Cave shooter (walls move in - what's the right game for a tunnel dodging
 *      game; is this the right game name for this play element?)
 *      Scramble
 *      http:
 *      Vanguard
 *      http:
 *  * Missile Command
 *      http:
 *  * Maybe Asteroids
 *  * Maybe Galaga (sucks up your paddle; Tetris already fulfils this
 *    gameplay mechanic)
 *
 * Pong: Requires ball, collision detection, AI for opponent
 * Break-Out: Requires add and remove item logic
 * Space Invaders: Requires image of alien, or definition of "blocks"
 *    layout. Requires Alien movement logic
 * Asteroids: either requires line-drawing or image of asteroid, and
 *    asteroid movement.
 * Tetris: requires stacking logic and shape maintenance.
 * Missile Command: requires line drawing and circles, and mouse position for
 *    clicks?  Explosion would destroy destroyable objects.
 *
 *
 * SIZEDO
 *   Constructing game:
 *      * See fixme sections
 *   Final cleanup:
 *      * Switch to String-packed data.
 *      * Change calculations in constants to a constant value.
 *
 * @author Matt Albrecht
 */
public class M extends Applet implements Runnable {
	/**
	 * SIZEDO
	 * Load the OBJECT_TYPE array.  For now, hard code the values.  As this
	 * gets closer to being finished, replace the invocation of this function
	 * with a string decoding structure.
	 */
	private static final void loadTypeArray(int[] obj) {


        int p = OBJECT_TYPE_BREAKOUT_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = FLAGMSK_DESTROYABLE | FLAGMSK_STOPS_TETRIS | FLAGMSK_REFLECTS_BALL;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_BREAKOUT_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = BREAKOUT_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = BREAKOUT_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = BREAKOUT_BLOCK_V_COUNT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = BREAKOUT_BLOCK_H_COUNT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = BREAKOUT_BLOCK_V_OFFSET;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = BREAKOUT_BLOCK_H_OFFSET;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = BREAKOUT_BLOCK_V_SPACING + BREAKOUT_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = BREAKOUT_BLOCK_H_SPACING + BREAKOUT_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX] = 0xff;
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0xff;

		
		p = OBJECT_TYPE_INVADER1_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = FLAGMSK_DESTROYABLE | FLAGMSK_STOPS_TETRIS | FLAGMSK_REFLECTS_BALL | FLAGMSK_CAUSES_DENTS;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_INVADER1_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = INVADER_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = INVADER_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = INVADER1_BLOCK_V_COUNT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = INVADER_BLOCK_H_COUNT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = INVADER1_BLOCK_V_OFFSET;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = INVADER1_BLOCK_H_OFFSET;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = INVADER_BLOCK_V_SPACING;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = INVADER_BLOCK_H_SPACING;

		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0x69; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0x6d; 

		
		
		
		

		
		p = OBJECT_TYPE_INVADER2_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = FLAGMSK_DESTROYABLE | FLAGMSK_STOPS_TETRIS | FLAGMSK_REFLECTS_BALL;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_INVADER2_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = INVADER_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = INVADER_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = INVADER2_BLOCK_V_COUNT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = INVADER_BLOCK_H_COUNT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = INVADER2_BLOCK_V_OFFSET;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = INVADER2_BLOCK_H_OFFSET;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = INVADER_BLOCK_V_SPACING;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = INVADER_BLOCK_H_SPACING;

		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0x66; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0x6b; 

		
		
		
		

		
		p = OBJECT_TYPE_NPADDLE_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = FLAGMSK_REFLECTS_BALL;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_NPADDLE_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = NPADDLE_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = NPADDLE_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 1;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 1;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = NPADDLE_BLOCK_Y;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = NPADDLE_BLOCK_STARTX;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX] = 0xff;

		
		p = OBJECT_TYPE_SPADDLE_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = FLAGMSK_REFLECTS_BALL | FLAGMSK_DENTABLE | FLAGMSK_STOPS_TETRIS;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_SPADDLE_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = SPADDLE_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = SPADDLE_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 1;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 1;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = SPADDLE_BLOCK_Y;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = SPADDLE_BLOCK_START_X;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX] = 0xff;

		
		p = OBJECT_TYPE_BALL_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = FLAGMSK_CAUSES_DENTS | FLAGMSK_CAUSES_DESTROY;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_BALL_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = BALL_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = BALL_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 1;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 1;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = BALL_BLOCK_START_Y;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = BALL_BLOCK_START_X;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX] = 0x1;

		
		p = OBJECT_TYPE_ENEMYFIRE_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = ENEMYFIRE_FLAGS; 
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_ENEMYFIRE_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = ENEMYFIRE_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = ENEMYFIRE_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX] = 0x3;

		
		p = OBJECT_TYPE_TETRIS_LHOOK_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = TETRIS_FLAGS; 
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_TETRIS_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = TETRIS_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = TETRIS_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0xf0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0xf0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 2] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 3] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 4] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 5] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 6] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 7] = 0x00; 
		
		
		
		
		
		
		
		

		
		p = OBJECT_TYPE_TETRIS_RHOOK_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = TETRIS_FLAGS; 
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_TETRIS_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = TETRIS_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = TETRIS_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 2] = 0x03; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 3] = 0x03; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 4] = 0x03; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 5] = 0x03; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 6] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 7] = 0x00; 
		
		
		
		
		
		
		
		

		
		p = OBJECT_TYPE_TETRIS_BAR_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = TETRIS_FLAGS; 
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_TETRIS_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = TETRIS_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = TETRIS_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0xff; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0xff; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 2] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 3] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 4] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 5] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 6] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 7] = 0x00; 
		
		
		
		
		
		
		
		

		
		p = OBJECT_TYPE_TETRIS_BOX_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = TETRIS_FLAGS; 
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_TETRIS_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = TETRIS_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = TETRIS_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 2] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 3] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 4] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 5] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 6] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 7] = 0x00; 
		
		
		
		
		
		
		
		

		
		p = OBJECT_TYPE_TETRIS_BUMP_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = TETRIS_FLAGS; 
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_TETRIS_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = TETRIS_BLOCK_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = TETRIS_BLOCK_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0x3f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0x3f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 2] = 0x0c; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 3] = 0x0c; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 4] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 5] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 6] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 7] = 0x00; 
		
		
		
		
		
		
		
		

		
		p = OBJECT_TYPE_MISSILE_HEAD_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = MISSILE_HEAD_FLAGS;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_MISSILE_HEAD_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = MISSILE_HEAD_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = MISSILE_HEAD_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0x01; 

		
		p = OBJECT_TYPE_MISSILE_EXPLODE_1_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = MISSILE_EXPLODE_FLAGS;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_MISSILE_EXPLODE_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = MISSILE_EXPLODE_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = MISSILE_EXPLODE_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 2] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 3] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 4] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 5] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 6] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 7] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 8] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 9] = 0x03; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 10] = 0xe0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 11] = 0x07; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 12] = 0xe0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 13] = 0x07; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 14] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 15] = 0x03; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 16] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 17] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 18] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 19] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 20] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 21] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 22] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 23] = 0x00; 
		
		
		
		
		
		
		
		
		
		
		
		

		
		p = OBJECT_TYPE_MISSILE_EXPLODE_2_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = MISSILE_EXPLODE_FLAGS;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_MISSILE_EXPLODE_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = MISSILE_EXPLODE_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = MISSILE_EXPLODE_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 2] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 3] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 4] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 5] = 0x03; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 6] = 0xf0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 7] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 8] = 0xf8; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 9] = 0x1f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 10] = 0xf8; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 11] = 0x1f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 12] = 0xf8; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 13] = 0x1f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 14] = 0xf8; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 15] = 0x1f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 16] = 0xf0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 17] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 18] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 19] = 0x03; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 20] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 21] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 22] = 0x00; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 23] = 0x00; 
		
		
		
		
		
		
		
		
		
		
		
		

		
		p = OBJECT_TYPE_MISSILE_EXPLODE_3_POS;
		obj[p + OBJECT_TYPE_RECORD_FLAG_INDEX] = MISSILE_EXPLODE_FLAGS;
		obj[p + OBJECT_TYPE_RECORD_COLOR_INDEX] = PALETTE_MISSILE_EXPLODE_POS;
		obj[p + OBJECT_TYPE_RECORD_WIDTH_INDEX] = MISSILE_EXPLODE_WIDTH;
		obj[p + OBJECT_TYPE_RECORD_HEIGHT_INDEX] = MISSILE_EXPLODE_HEIGHT;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX] = 0;
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX] = 0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 0] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 1] = 0x03; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 2] = 0xf0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 3] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 4] = 0xf8; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 5] = 0x1f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 6] = 0xf8; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 7] = 0x1f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 8] = 0xfc; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 9] = 0x3f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 10] = 0xfc; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 11] = 0x3f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 12] = 0xfc; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 13] = 0x3f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 14] = 0xfc; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 15] = 0x3f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 16] = 0xf8; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 17] = 0x1f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 18] = 0xf8; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 19] = 0x1f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 20] = 0xf0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 21] = 0x0f; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 22] = 0xc0; 
		obj[p + OBJECT_TYPE_RECORD_IMAGE_INDEX + 23] = 0x03; 
		
		
		
		
		
		
		
		
		
		
		
		

		/*
		    
		    private static final int FLAGMSK_DESTROYABLE = 0x0001;
		    
		    
		    
		    private static final int FLAGMSK_CAUSES_DENTS = 0x0002;
		    
		    
		    private static final int FLAGMSK_CAUSES_DESTROY = 0x0004;
		    
		    
		    private static final int FLAGMSK_DENTABLE = 0x0008;
		    
		    
		    private static final int FLAGMSK_REFLECTS_BALL = 0x0010;
		    
		    
		    
		    private static final int FLAGMSK_STOPS_TETRIS = 0x0020;
		    
		    
		    private static final int FLAGMSK_IS_TETRIS = 0x0040;
		*/

	}

	private int mousePos;
	
	
	private boolean mouseClick;

	@Override
    public void start() {
		new Thread(this).start();
	}

	@Override
    public void run() {
		while (!isActive()) {
			Thread.yield();
		}

		
		int x7;


        BufferedImage image = new BufferedImage(RENDER_WIDTH, RENDER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics ogr = image.getGraphics();


        int[] OBJECT_TYPES = new int[OBJECT_TYPE_SIZE];
		
		loadTypeArray(OBJECT_TYPES);

		
		
		
		OBJECT_TYPES[WALL_TYPE_HEIGHT_POS] = WALL_BLOCK_HEIGHT;
		OBJECT_TYPES[WALL_TYPE_WIDTH_POS] = WALL_IMAGE_BLOCK_WIDTH;


        Color[] COLORS = new Color[PALETTE_COUNT];

		
		
		
		
		/*x1 = 0;*/
		/*y1 = 0;*/
		/*y2 = 0;*/
		/*y3 = 0;*/
        int y3 = 0;
        int y2 = 0;
        int y1 = 0;
        int x1 = 0;
        while (true) {
			COLORS[x1++] = new Color(y1 << PALETTE_COMPONENT_SHL, y2 << PALETTE_COMPONENT_SHL, y3 << PALETTE_COMPONENT_SHL);
			if (++y1 >= PALETTE_COMPONENT_MAX) {
				y1 = 0;
				if (++y2 >= PALETTE_COMPONENT_MAX) {
					y2 = 0;
					if (++y3 >= PALETTE_COMPONENT_MAX) {
						break;
					}
				}
			}
		}


        int playerScore = 0;
        int computerScore = 0;
        int playerScoreColor = PALETTE_SCORE_NEUTRAL_POS;
        int computerScoreColor = PALETTE_SCORE_NEUTRAL_POS;

		startLife: while (true) {


            long lastTime = System.nanoTime();


			y1 = WALL_START_Y;


            int[] OBJECT_INST = new int[OBJECT_INST_SIZE];
            int x2;
            int p1 = OBJECT_INST_IMAGE_BASE_START_POS;
            int p0 = WALL_OBJECT_INST_START_POS;
			for (x1 = 0; x1 < WALL_TOTAL_COUNT; x1++) {
				
				OBJECT_INST[p0 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_YES;
				OBJECT_INST[p0 + OBJECT_INST_RECORD_IMAGE_POS_INDEX] = p1;
				OBJECT_INST[p0 + OBJECT_INST_RECORD_X_INDEX] = WALL_START_X;
				OBJECT_INST[p0 + OBJECT_INST_RECORD_Y_INDEX] = y1;
				OBJECT_INST[p0 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] = NEEDSCOPY_NOOP;
				OBJECT_INST[p0 + OBJECT_INST_RECORD_FLAG_INDEX] = WALL_FLAGS;
				OBJECT_INST[p0 + OBJECT_INST_RECORD_TYPE_INDEX] = OBJECT_TYPE_WALL_INDEX;

				
				x2 = (x1 % WALL_PALETTE_DELTA) + PALETTE_WALL_START;
				OBJECT_INST[p1 + WALL_IMAGE_LBLOCK_INDEX] = x2;
				OBJECT_INST[p1 + WALL_IMAGE_RBLOCK_INDEX] = x2;
				
				
				
				OBJECT_INST[p1 + WALL_IMAGE_DATA_RBREAK_IPOS] = RENDER_WIDTH;

				y1 -= WALL_SEGMENT_HEIGHT;
				p0 += OBJECT_INST_RECORD_SIZE;
				p1 += OBJECT_INST_IMAGE_SIZE;
			}


            int x4 = OBJECT_TYPE_BREAKOUT_POS;


            int x3;
            for (x3 = OBJECT_TYPE_BREAKOUT_INDEX; x3 < OBJECT_TYPE_COUNT; x3++) {
				
				
				y1 = OBJECT_TYPES[x4 + OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX];
				
				
				for (y2 = 0; y2 < OBJECT_TYPES[x4 + OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX]; y2++) {
					
					
					x1 = OBJECT_TYPES[x4 + OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX];
					
					
					for (x2 = 0; x2 < OBJECT_TYPES[x4 + OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX]; x2++) {
						

						OBJECT_INST[p0 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_NO;
						OBJECT_INST[p0 + OBJECT_INST_RECORD_IMAGE_POS_INDEX] = p1;
						OBJECT_INST[p0 + OBJECT_INST_RECORD_X_INDEX] = x1 * GR_BLOCK_WIDTH;
						OBJECT_INST[p0 + OBJECT_INST_RECORD_Y_INDEX] = y1 * GR_BLOCK_HEIGHT;
						OBJECT_INST[p0 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] = NEEDSCOPY_COPY;
						OBJECT_INST[p0 + OBJECT_INST_RECORD_FLAG_INDEX] = OBJECT_TYPES[x4 + OBJECT_TYPE_RECORD_FLAG_INDEX];
						OBJECT_INST[p0 + OBJECT_INST_RECORD_TYPE_INDEX] = x3;

						x1 += OBJECT_TYPES[x4 + OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX];
						p0 += OBJECT_INST_RECORD_SIZE;
						p1 += OBJECT_INST_IMAGE_SIZE;
					}
					
					
					
					y1 += OBJECT_TYPES[x4 + OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX];
				}

				x4 += OBJECT_TYPE_RECORD_SIZE;
			}
			
			
			

			
			
			
			
			p0 += OBJECT_INST_RECORD_IMAGE_POS_INDEX;
			
			while (p0 < OBJECT_INST_IMAGE_START_POS) {
				OBJECT_INST[p0] = p1;

				p0 += OBJECT_INST_RECORD_SIZE;
				p1 += OBJECT_INST_IMAGE_SIZE;
			}
			

			
			
			
			OBJECT_INST[OBJECT_INST[BALL_OBJECT_INST_IMAGE_POS_POS] + BALL_IMAGE_DATA_DX_IPOS] = BALL_START_DX;
			OBJECT_INST[OBJECT_INST[BALL_OBJECT_INST_IMAGE_POS_POS] + BALL_IMAGE_DATA_DY_IPOS] = BALL_START_DY;

			
			OBJECT_INST[NPADDLE_OBJECT_INST_VISIBLE_POS] = VISIBLE_YES;
			OBJECT_INST[SPADDLE_OBJECT_INST_VISIBLE_POS] = VISIBLE_YES;
			OBJECT_INST[BALL_OBJECT_INST_VISIBLE_POS] = VISIBLE_YES;

            int objectCount = BALL_OBJECT_INST_START_INDEX + 1;

            boolean ballSticky = true;
            boolean ballReflect = false;
            int wallRight = MAX_WALL_RIGHT;
            int wallLeft = MIN_WALL_LEFT;
            int wallCount = 0;
            int reflectCount = 0;
            int frameCount = 0;
            loopGame: while (true) {
                Graphics sg = getGraphics();


                boolean mousePressed = this.mouseClick;
				this.mouseClick = false;
				if (mousePressed && frameCount > 0) {
					ballSticky = false;
					OBJECT_INST[BALL_OBJECT_INST_Y_POS] -= BALL_HEIGHT;
				}
				frameCount++;

				
				

				
				
				ogr.setColor(COLORS[PALETTE_BACKGROUND_POS]);
				ogr.fillRect(0, 0, RENDER_WIDTH, RENDER_HEIGHT);

				
				
				
				ogr.setColor(COLORS[PALETTE_NET_POS]);
				for (x1 = 0; x1 < RENDER_WIDTH; x1 += DR_NET_SEP) {
					ogr.fillRect(x1, DR_NET_Y, DR_NET_WIDTH, DR_NET_HEIGHT);
				}


                int p2;
                int y4;
                if (ballReflect && (reflectCount & REFLECT_SPECIAL_BITS) == SPECIAL_COMPUTE_EQUALS) {
					ballReflect = false;
					
					p2 = BASE_EVENT_MOD + playerScore + computerScore + (reflectCount >> REFLECT_SPECIAL_SHR);
					if (p2 > MAX_EVENT_MOD) {
						p2 = MAX_EVENT_MOD;
					}

					
					y4 = 0;
					

					switch (frameCount % p2) {
					
					
					
					case 1:
						y4 += BREAKOUT_BLOCK_V_COUNT / 2;
					case 2:
						
						

						
						x1 = BREAKOUT_OBJECT_INST_START_POS + ((frameCount >>> 3) % (y4 + (BREAKOUT_BLOCK_V_COUNT / 2))) * BREAKOUT_ROW_OBJECT_INST_SIZE + OBJECT_INST_RECORD_VISIBLE_INDEX;
						
						for (x4 = 0; x4 < BREAKOUT_BLOCK_H_COUNT; x4++, x1 += OBJECT_INST_RECORD_SIZE) {
							OBJECT_INST[x1] = VISIBLE_YES;
						}
						
						

						break;
					case 3:
						
						
						wallCount += WALL_ADD_COUNT_1;
					case 4:
						
						
						wallCount += WALL_ADD_COUNT_2;
						break;

					case 5:
						
						
						y4 = (frameCount >>> 3) % TETRIS_TYPE_COUNT;

						
						x1 = objectCount * OBJECT_INST_RECORD_SIZE;
						
						objectCount++;
						
						x4 = OBJECT_INST[OBJECT_INST[NPADDLE_OBJECT_INST_IMAGE_POS_POS] + NPADDLE_IMAGE_DATA_WALL_RIGHT_IPOS] - TETRIS_WIDTH
								- OBJECT_INST[OBJECT_INST[NPADDLE_OBJECT_INST_IMAGE_POS_POS] + NPADDLE_IMAGE_DATA_WALL_LEFT_IPOS];
						
						
						x4 = (((frameCount * objectCount * reflectCount) >>> 3) % x4) + OBJECT_INST[OBJECT_INST[NPADDLE_OBJECT_INST_IMAGE_POS_POS] + NPADDLE_IMAGE_DATA_WALL_LEFT_IPOS];
						OBJECT_INST[x1 + OBJECT_INST_RECORD_X_INDEX] = x4;
						
						OBJECT_INST[x1 + OBJECT_INST_RECORD_Y_INDEX] = TETRIS_START_Y;
						OBJECT_INST[x1 + OBJECT_INST_RECORD_FLAG_INDEX] = TETRIS_FLAGS;
						OBJECT_INST[x1 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] = NEEDSCOPY_COPY;
						OBJECT_INST[x1 + OBJECT_INST_RECORD_TYPE_INDEX] = OBJECT_TYPE_TETRIS_LHOOK_INDEX + y4;
						OBJECT_INST[x1 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_YES;
						
						break;

					case 6:
						
						
						y4++;
					case 7:
						
						
						
						x1 = INVADER_OBJECT_INST_START_POS + (y4 * INVADER_BLOCK_H_COUNT * OBJECT_INST_RECORD_SIZE) + OBJECT_INST_RECORD_VISIBLE_INDEX;
						
						
						for (x4 = 0; x4 < INVADER_BLOCK_H_COUNT; x4++, x1 += OBJECT_INST_RECORD_SIZE) {
							OBJECT_INST[x1] = VISIBLE_YES;
						}
						
						
						break;
					case 8:
					case 9:
						
						

						
						x1 = objectCount * OBJECT_INST_RECORD_SIZE;
						
						objectCount++;

						OBJECT_INST[x1 + OBJECT_INST_RECORD_Y_INDEX] = MISSILE_START_Y;
						OBJECT_INST[x1 + OBJECT_INST_RECORD_FLAG_INDEX] = MISSILE_HEAD_FLAGS;
						OBJECT_INST[x1 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] = NEEDSCOPY_COPY;
						OBJECT_INST[x1 + OBJECT_INST_RECORD_TYPE_INDEX] = OBJECT_TYPE_MISSILE_HEAD_INDEX;
						OBJECT_INST[x1 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_YES;

						
						
						p0 = OBJECT_INST[x1 + OBJECT_INST_RECORD_IMAGE_POS_INDEX];
						
						y1 = (((frameCount * objectCount * reflectCount) >>> 3) % MISSILE_START_X_COUNT) * MISSILE_START_X_MULT;
						OBJECT_INST[x1 + OBJECT_INST_RECORD_X_INDEX] = y1;
						
						OBJECT_INST[p0 + MISSILE_HEAD_DATA_PATH_START_X_IPOS] = y1;
						OBJECT_INST[p0 + MISSILE_HEAD_DATA_PATH_START_Y_IPOS] = MISSILE_START_Y;
						y1 <<= MISSILE_MOVEMENT_ENCODE_SHL;

						OBJECT_INST[p0 + MISSILE_HEAD_DATA_X_IPOS] = y1;
						OBJECT_INST[p0 + MISSILE_HEAD_DATA_Y_IPOS] = MISSILE_START_Y_ENCODED;

						
						
						x1 = ((((MISSILE_RANDOM_MULT * frameCount * objectCount) + reflectCount) % MISSILE_START_X_COUNT) * MISSILE_START_X_MULT) << MISSILE_MOVEMENT_ENCODE_SHL;
						if (y1 == x1) {
							x1++;
						}

						OBJECT_INST[p0 + MISSILE_HEAD_DATA_DX_IPOS] = RENDER_HEIGHT / (x1 - y1);
						OBJECT_INST[p0 + MISSILE_HEAD_DATA_DY_IPOS] = 1 << MISSILE_MOVEMENT_ENCODE_SHL;
						
						

						break;
					}
					
					
				}

				if (wallCount > 0) {
					if (wallCount % GR_BLOCK_HEIGHT == 0) {
						
						x1 = wallRight - wallLeft;
						if (wallCount < (GR_SCREEN_BLOCK_WIDTH - x1) * GR_BLOCK_HEIGHT) {
							wallLeft--;
							wallRight++;
						} else if (x1 > WALL_MOVEMENT_1) {
							wallLeft++;
							wallRight--;
						} else if (x1 > WALL_MOVEMENT_2) {
							if ((((frameCount >> 6) + reflectCount) & 1) == 0) {
								wallLeft--;
								wallRight--;
							} else {
								wallLeft++;
								wallRight++;
							}
						} else {
							wallLeft--;
							wallRight++;
						}
						if (wallLeft < MIN_WALL_LEFT) {
							wallLeft = MIN_WALL_LEFT;
						}
						if (wallLeft > MAX_WALL_LEFT) {
							wallLeft = MAX_WALL_LEFT;
						}
						if (wallRight > MAX_WALL_RIGHT) {
							wallRight = MAX_WALL_RIGHT;
						}
						if (wallRight < MIN_WALL_RIGHT) {
							wallRight = MIN_WALL_RIGHT;
						}
						
					}
					wallCount--;
				} else {
					wallLeft = MIN_WALL_LEFT;
					wallRight = MAX_WALL_RIGHT;
				}

				
				
				x1 = ((frameCount >> 2) % PALETTE_MISSILE_EXPLODE_DELTA) + PALETTE_MISSILE_EXPLODE_BASE_POS;
				OBJECT_TYPES[MISSILE_EXPLODE_1_TYPE_COLOR_POS] = x1;
				OBJECT_TYPES[MISSILE_EXPLODE_2_TYPE_COLOR_POS] = x1;
				OBJECT_TYPES[MISSILE_EXPLODE_3_TYPE_COLOR_POS] = x1;


                int p3 = 0;


                boolean b1;
                for (x2 = 0; x2 < objectCount; x2++, p3 += OBJECT_INST_RECORD_SIZE) {
					
					if (OBJECT_INST[p3 + OBJECT_INST_RECORD_VISIBLE_INDEX] != VISIBLE_YES) {
						continue;
					}

					
					p1 = OBJECT_INST[p3 + OBJECT_INST_RECORD_FLAG_INDEX];
					
					
					x3 = OBJECT_INST[p3 + OBJECT_INST_RECORD_TYPE_INDEX];
					

					
					y3 = x3 * OBJECT_TYPE_RECORD_SIZE;
					

					
					p0 = OBJECT_INST[p3 + OBJECT_INST_RECORD_IMAGE_POS_INDEX];


                    int nextXPos = OBJECT_INST[p3 + OBJECT_INST_RECORD_X_INDEX];
                    int nextYPos = OBJECT_INST[p3 + OBJECT_INST_RECORD_Y_INDEX];


                    int y5;
                    if (OBJECT_INST[p3 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] == NEEDSCOPY_COPY) {
						OBJECT_INST[p3 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] = NEEDSCOPY_NOOP;

						
						y4 = y3 + OBJECT_TYPE_RECORD_IMAGE_INDEX;
						
						
						x1 = OBJECT_TYPES[y4];
						
						
						x4 = 0;
						

						
						for (y1 = 0; y1 < OBJECT_TYPES[y3 + OBJECT_TYPE_RECORD_HEIGHT_INDEX] * OBJECT_TYPES[y3 + OBJECT_TYPE_RECORD_WIDTH_INDEX]; y1++) {
							
							
							y5 = OBJECT_TYPES[y3 + OBJECT_TYPE_RECORD_COLOR_INDEX];
							if (x3 == OBJECT_TYPE_BREAKOUT_INDEX) {
								
								y5 = ((OBJECT_INST[p3 + OBJECT_INST_RECORD_Y_INDEX]) % PALETTE_BREAKOUT_MOD) + PALETTE_BREAKOUT_POS;
							}
							if ((x1 & 1) == 0) {
								y5 = PALETTE_BACKGROUND_POS;
							}
							OBJECT_INST[p0 + y1] = y5;
							
							x1 >>>= 1;
							x4++;
							if (x4 >= IMAGE_BITS_PER_POS) {
								x4 = 0;
								y4++;
								x1 = OBJECT_TYPES[y4];
							}
						}
						
						
						
						
					}


                    int x5;
                    switch (x3) {

					case OBJECT_TYPE_WALL_INDEX:
						
						

						nextYPos++;
						
						
						if (nextYPos >= WALL_START_Y) {
							
							
							nextYPos = OBJECT_INST[p3 + OBJECT_INST_RECORD_Y_INDEX] = WALL_END_Y + (nextYPos - WALL_START_Y);

							OBJECT_INST[p0 + WALL_IMAGE_DATA_LBREAK_IPOS] = wallLeft * GR_BLOCK_WIDTH;
							OBJECT_INST[p0 + WALL_IMAGE_DATA_RBREAK_IPOS] = wallRight * GR_BLOCK_WIDTH;

							
							for (x1 = 1; x1 < wallLeft; x1++) {
								OBJECT_INST[p0 + x1] = OBJECT_INST[p0];
							}
							for (; x1 <= wallRight; x1++) {
								
								
								OBJECT_INST[p0 + x1] = PALETTE_BACKGROUND_POS;
							}
							for (; x1 <= GR_SCREEN_BLOCK_WIDTH; x1++) {
								OBJECT_INST[p0 + x1] = OBJECT_INST[p0];
							}

							
						}

						
						
						
						
						p2 = nextYPos / GR_BLOCK_HEIGHT;
						
						
						x1 = OBJECT_INST[p0 + WALL_IMAGE_DATA_LBREAK_IPOS];
						
						
						y1 = OBJECT_INST[p0 + WALL_IMAGE_DATA_RBREAK_IPOS];
						

						
						
						
						if (p2 == NPADDLE_BLOCK_Y) {
							OBJECT_INST[OBJECT_INST[NPADDLE_OBJECT_INST_IMAGE_POS_POS] + NPADDLE_IMAGE_DATA_WALL_LEFT_IPOS] = x1;
							OBJECT_INST[OBJECT_INST[NPADDLE_OBJECT_INST_IMAGE_POS_POS] + NPADDLE_IMAGE_DATA_WALL_RIGHT_IPOS] = y1;
						}
						if (p2 == OBJECT_INST[BALL_OBJECT_INST_Y_POS] / GR_BLOCK_HEIGHT) {
							
							
							x4 = OBJECT_INST[BALL_OBJECT_INST_X_POS];
							
							
							y4 = OBJECT_INST[OBJECT_INST[BALL_OBJECT_INST_IMAGE_POS_POS] + BALL_IMAGE_DATA_DX_IPOS];
							
							x5 = x1;
							
							
							b1 = false;
							
							if (x4 < x1) {
								b1 = true;
								if (y4 < 0) {
									y4 = 0 - y4;
								}
							}
							if (x4 > y1 - GR_BLOCK_WIDTH) {
								b1 = true;
								x5 = y1 - GR_BLOCK_WIDTH;
								if (y4 > 0) {
									y4 = 0 - y4;
								}
							}

							if (b1) {
								
								OBJECT_INST[BALL_OBJECT_INST_X_POS] = x5;
								OBJECT_INST[BALL_IMAGE_DATA_X_IPOS + OBJECT_INST[BALL_OBJECT_INST_IMAGE_POS_POS]] = x5 << BALL_MOVEMENT_ENCODE_SHL;
								
								
								OBJECT_INST[BALL_IMAGE_DATA_DX_IPOS + OBJECT_INST[BALL_OBJECT_INST_IMAGE_POS_POS]] = y4;
							}
							
							
							
							
						}
						
						
						
						break;

					case OBJECT_TYPE_BREAKOUT_INDEX:
						
						
						break;

					case OBJECT_TYPE_INVADER1_INDEX:
					case OBJECT_TYPE_INVADER2_INDEX:
						
						

						if ((frameCount % INVADER_CHANGE_IMAGE_MOD) == 0) {
							OBJECT_INST[p3 + OBJECT_INST_RECORD_TYPE_INDEX] = (OBJECT_TYPE_INVADER2_INDEX - x3) + OBJECT_TYPE_INVADER1_INDEX;
							OBJECT_INST[p3 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] = NEEDSCOPY_COPY;
						}

						
						y1 = (frameCount >>> INVADER_FRAMES_PER_MOVE_SHR) % INVADER_FRAME_MOVE_MOD;
						

						
						if (OBJECT_INST[p0 + INVADER_DATA_BASE_Y_IPOS] == 0) {
							OBJECT_INST[p0 + INVADER_DATA_BASE_X_IPOS] = nextXPos;
							OBJECT_INST[p0 + INVADER_DATA_BASE_Y_IPOS] = nextYPos;
							OBJECT_INST[p0 + INVADER_DATA_FIRE_IPOS] = x2 << 2;
						}

						
						if (y1 > INVADER_UP_MOVEMENT_START) {
							
							nextXPos = OBJECT_INST[p0 + INVADER_DATA_BASE_X_IPOS];
							nextYPos = OBJECT_INST[p0 + INVADER_DATA_BASE_Y_IPOS] + (INVADER_FRAME_MOVE_MOD - y1);
						} else if (y1 > INVADER_RIGHT_MOVEMENT_START) {
							
							nextXPos = OBJECT_INST[p0 + INVADER_DATA_BASE_X_IPOS] + y1 - INVADER_RIGHT_MOVEMENT_START - INVADER_MAX_H_MOVEMENT;
							nextYPos = OBJECT_INST[p0 + INVADER_DATA_BASE_Y_IPOS] + INVADER_MAX_V_MOVEMENT;
						} else if (y1 > INVADER_DOWN_MOVEMENT_START) {
							
							nextXPos = OBJECT_INST[p0 + INVADER_DATA_BASE_X_IPOS] - INVADER_MAX_H_MOVEMENT;
							nextYPos = OBJECT_INST[p0 + INVADER_DATA_BASE_Y_IPOS] + (y1 - INVADER_DOWN_MOVEMENT_START);
						} else {
							
							nextXPos = OBJECT_INST[p0 + INVADER_DATA_BASE_X_IPOS] - y1;
							nextYPos = OBJECT_INST[p0 + INVADER_DATA_BASE_Y_IPOS];
						}
						

						

						if ((OBJECT_INST[p0 + INVADER_DATA_FIRE_IPOS] + frameCount * reflectCount + nextXPos) % INVADER_FIRE_FREQUENCY == 0 && objectCount < MAX_OBJECT_INST_RECORDS) {
							OBJECT_INST[p0 + INVADER_DATA_FIRE_IPOS] += frameCount >> 3;
							
							y1 = objectCount * OBJECT_INST_RECORD_SIZE;
							
							objectCount++;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_X_INDEX] = nextXPos + INVADER_BULLET_XDELTA;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_Y_INDEX] = nextYPos + ENEMYFIRE_HEIGHT;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_FLAG_INDEX] = ENEMYFIRE_FLAGS;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] = NEEDSCOPY_COPY;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_TYPE_INDEX] = OBJECT_TYPE_ENEMYFIRE_INDEX;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_YES;
							
						}

						break;

					case OBJECT_TYPE_NPADDLE_INDEX:
						
						

						
						
						y1 = OBJECT_INST[BALL_OBJECT_INST_X_POS] - nextXPos;
						
						
						x4 = NPADDLE_BASE_SPEED + ((playerScore - computerScore) >> 1);
						
						
						if (x4 < NPADDLE_MIN_SPEED) {
							x4 = NPADDLE_MIN_SPEED;
						}
						if (x4 > NPADDLE_MAX_SPEED) {
							x4 = NPADDLE_MAX_SPEED;
						}
						if (y1 > x4) {
							y1 = x4;
						}
						if (y1 < 0 - x4) {
							y1 = 0 - x4;
						}
						nextXPos += y1;
						
						

						
						if (nextXPos < OBJECT_INST[p0 + NPADDLE_IMAGE_DATA_WALL_LEFT_IPOS]) {
							nextXPos = OBJECT_INST[p0 + NPADDLE_IMAGE_DATA_WALL_LEFT_IPOS];
						}
						if (nextXPos + NPADDLE_WIDTH >= OBJECT_INST[p0 + NPADDLE_IMAGE_DATA_WALL_RIGHT_IPOS]) {
							nextXPos = OBJECT_INST[p0 + NPADDLE_IMAGE_DATA_WALL_RIGHT_IPOS] - NPADDLE_WIDTH;
						}
						break;

					case OBJECT_TYPE_SPADDLE_INDEX:
						
						

						
						
						
						
						

						
						x1 = SPADDLE_BLOCK_WIDTH;
						
						
						x4 = 0;
						
						
						for (p2 = 0; p2 < SPADDLE_BLOCK_WIDTH; p2++) {
							if (OBJECT_INST[p0 + p2] != PALETTE_BACKGROUND_POS) {
								if (p2 < x1) {
									x1 = p2;
								}
								if (p2 > x4) {
									x4 = p2;
								}
							}
						}
						x1 *= GR_BLOCK_WIDTH;
						x4 *= GR_BLOCK_WIDTH;
						

						
						
						y1 = (this.mousePos & MOUSE_X_MASK) - x1 - ((x4 - x1) >> 1);
						

						if (y1 + x1 < SPADDLE_MIN_X) {
							y1 = SPADDLE_MIN_X - x1;
						} else if (y1 + x4 > SPADDLE_MAX_X) {
							y1 = SPADDLE_MAX_X - x4;
						}

						
						
						nextXPos = y1;

						
						
						
						if (ballSticky) {
							OBJECT_INST[BALL_OBJECT_INST_X_POS] = y1 + BALL_STICKY_X_OFFSET;
							OBJECT_INST[BALL_OBJECT_INST_Y_POS] = BALL_STICKY_Y;
							OBJECT_INST[OBJECT_INST[BALL_OBJECT_INST_IMAGE_POS_POS] + BALL_IMAGE_DATA_X_IPOS] = (y1 + BALL_STICKY_X_OFFSET) << BALL_MOVEMENT_ENCODE_SHL;
							OBJECT_INST[OBJECT_INST[BALL_OBJECT_INST_IMAGE_POS_POS] + BALL_IMAGE_DATA_Y_IPOS] = BALL_STICKY_Y << BALL_MOVEMENT_ENCODE_SHL;
						}
						
						break;

					case OBJECT_TYPE_BALL_INDEX:
						
						if (ballSticky) {
							
							
							
							p1 = BALL_STICKY_FLAGS;
						} else {
							nextXPos = (OBJECT_INST[p0 + BALL_IMAGE_DATA_X_IPOS] + OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS]) >> BALL_MOVEMENT_DECODE_SHR;
							nextYPos = (OBJECT_INST[p0 + BALL_IMAGE_DATA_Y_IPOS] + OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS]) >> BALL_MOVEMENT_DECODE_SHR;
						}

						break;

					case OBJECT_TYPE_MISSILE_HEAD_INDEX:
						
						x1 = DISPLAY_WIDTH;
						
						
						y1 = DISPLAY_HEIGHT;
						

						
						OBJECT_INST[p0 + MISSILE_HEAD_DATA_X_IPOS] += OBJECT_INST[p0 + MISSILE_HEAD_DATA_DX_IPOS];
						
						x4 = (OBJECT_INST[p0 + MISSILE_HEAD_DATA_X_IPOS]) >> MISSILE_MOVEMENT_DECODE_SHR;
						OBJECT_INST[p0 + MISSILE_HEAD_DATA_Y_IPOS] += OBJECT_INST[p0 + MISSILE_HEAD_DATA_DY_IPOS];
						
						y4 = (OBJECT_INST[p0 + MISSILE_HEAD_DATA_Y_IPOS]) >> MISSILE_MOVEMENT_DECODE_SHR;

						if (mousePressed) {
							
							x1 = (this.mousePos & MOUSE_X_MASK) - nextXPos;
							y1 = (this.mousePos >>> MOUSE_Y_SHR) - nextYPos;
						}
						nextXPos = x4;
						nextYPos = y4;
						
						

						
						if (((frameCount >> 3) & 1) == 0) {
							OBJECT_INST[p0] = (OBJECT_INST[p0] == PALETTE_MISSILE_HEAD_POS) ? PALETTE_MISSILE_HEAD2_POS : PALETTE_MISSILE_HEAD_POS;
						}

						if (((x1 < MISSILE_MAX_DIST_X && x1 > MISSILE_MIN_DIST_X && y1 < MISSILE_MAX_DIST_Y && y1 > MISSILE_MIN_DIST_Y) || nextYPos > MISSILE_HEAD_EXPLODE_Y || nextXPos < 0 || nextXPos > RENDER_WIDTH)
								&& objectCount < MAX_OBJECT_INST_RECORDS) {
							
							
							
							OBJECT_INST[p3 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_DESTROY;

							
							y1 = objectCount * OBJECT_INST_RECORD_SIZE;
							
							objectCount++;
							
							OBJECT_INST[y1 + OBJECT_INST_RECORD_X_INDEX] = nextXPos - MISSILE_EXPLODE_MID_X;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_Y_INDEX] = nextXPos - MISSILE_EXPLODE_MID_Y;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_FLAG_INDEX] = MISSILE_EXPLODE_FLAGS;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] = NEEDSCOPY_COPY;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_TYPE_INDEX] = OBJECT_TYPE_MISSILE_EXPLODE_1_INDEX;
							OBJECT_INST[y1 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_YES;
							y1 = OBJECT_INST[y1 + OBJECT_INST_RECORD_IMAGE_POS_INDEX];
							
							OBJECT_INST[y1 + MISSILE_EXPLODE_DATA_ALIVECOUNT_IPOS] = MISSILE_EXPLODE_LIFESPAN;
							OBJECT_INST[y1 + MISSILE_EXPLODE_DATA_BASE_X_IPOS] = nextXPos - MISSILE_EXPLODE_MID_X;
							OBJECT_INST[y1 + MISSILE_EXPLODE_DATA_BASE_Y_IPOS] = nextYPos + MISSILE_EXPLODE_MID_Y;
						}
						

						break;

					case OBJECT_TYPE_MISSILE_EXPLODE_1_INDEX:
					case OBJECT_TYPE_MISSILE_EXPLODE_2_INDEX:
					case OBJECT_TYPE_MISSILE_EXPLODE_3_INDEX:

						
						
						y1 = --OBJECT_INST[p0 + MISSILE_EXPLODE_DATA_ALIVECOUNT_IPOS];
						if (y1 <= 0) {
							OBJECT_INST[p3 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_DESTROY;
							
							
						} else {
							
							
							y1 = -1 + (MISSILE_EXPLODE_LIFESPAN - y1 + MISSILE_EXPLODE_SWITCH_IMAGE_DURATION) / MISSILE_EXPLODE_SWITCH_IMAGE_DURATION;
							OBJECT_INST[p3 + OBJECT_INST_RECORD_TYPE_INDEX] = OBJECT_TYPE_MISSILE_EXPLODE_1_INDEX + y1;
							OBJECT_INST[p3 + OBJECT_INST_RECORD_NEEDSCOPY_INDEX] = NEEDSCOPY_COPY;
						}
						
						nextXPos = OBJECT_INST[p0 + MISSILE_EXPLODE_DATA_BASE_X_IPOS];
						nextYPos = OBJECT_INST[p0 + MISSILE_EXPLODE_DATA_BASE_Y_IPOS];

						break;

					case OBJECT_TYPE_ENEMYFIRE_INDEX:
						
						nextYPos += ENEMYFIRE_MOVEMENT_Y;

						break;

					/*
					case OBJECT_TYPE_TETRIS_LHOOK_INDEX:
					case OBJECT_TYPE_TETRIS_RHOOK_INDEX:
					case OBJECT_TYPE_TETRIS_BAR_INDEX:
					case OBJECT_TYPE_TETRIS_BOX_INDEX:
					case OBJECT_TYPE_TETRIS_BUMP_INDEX:
					*/
					default:
						

						if (mousePressed && OBJECT_INST[p0 + TETRIS_IMAGE_DATA_IS_STICKY_IPOS] == 0) {
							
							
							

							
							for (x1 = 0; x1 < TETRIS_IMAGE_SIZE; x1++) {
								
								x4 = 56 - ((x1 & 7) << 3) + (x1 >> 3);
								/* 4x4
								x4 = 12 - ((x1 & 3) << 2) + (x1 >> 2);
								*/
								OBJECT_INST[p0 + TETRIS_IMAGE_TEMP_DATA_IPOS + x1] = OBJECT_INST[p0 + x4];
								
							}
							
							for (x1 = 0; x1 < TETRIS_IMAGE_SIZE; x1++) {
								OBJECT_INST[p0 + x1] = OBJECT_INST[p0 + TETRIS_IMAGE_TEMP_DATA_IPOS + x1];
							}
							
						}

						if (frameCount % TETRIS_MOVEMENT_MOD == 0) {
							nextYPos += TETRIS_MOVEMENT_RATE;
						}

						break;
					}

					
					
					
					
					
					
					if ((p1 & COLLIDES_FLAGS) != 0) {


                        int xpos = OBJECT_INST[p3 + OBJECT_INST_RECORD_X_INDEX];
                        int xdelta = nextXPos - xpos;
                        int xdir = 0;
                        int withXOverlapSubt = GR_BLOCK_WIDTH;
						if (xdelta > 0) {
							xdir++;
							withXOverlapSubt = 0;
						}
						if (xdelta < 0) {
							
							
							xdir--;

							
							xdelta = 0 - xdelta;

							
							
						}

                        int ypos = OBJECT_INST[p3 + OBJECT_INST_RECORD_Y_INDEX];
                        int ydelta = nextYPos - ypos;
                        int ydir = 0;
                        if (ydelta > 0) {
							ydir++;
						}
                        int withYOverlapAdd = GR_BLOCK_HEIGHT;
                        if (ydelta < 0) {
							
							
							ydir--;

							
							ydelta = 0 - ydelta;
							withYOverlapAdd = 0;
						}
                        int movementError = xdelta - ydelta;

                        int sourceWidth = OBJECT_TYPES[y3 + OBJECT_TYPE_RECORD_WIDTH_INDEX];
                        int sourceHeight = OBJECT_TYPES[y3 + OBJECT_TYPE_RECORD_HEIGHT_INDEX];

                        int maxP2 = objectCount * OBJECT_INST_RECORD_SIZE;

						movementLoop: while (true) {
                            boolean collision = false;
                            boolean ballReflectNow = false;
                            int collisionXPixels = 0;
                            int collisionYPixels = 0;
                            int highestTetrisPos = DISPLAY_HEIGHT;
                            int collisionPaddle = 0;

							
							

							
							withObjectLoop: for (p2 = 0; p2 < maxP2; p2 += OBJECT_INST_RECORD_SIZE) {
								

								
								
								y1 = OBJECT_INST[p2 + OBJECT_INST_RECORD_FLAG_INDEX];
								
								
								b1 = ((p1 & FLAGMSK_CAUSES_DESTROY) != 0) && ((y1 & FLAGMSK_DESTROYABLE) != 0);


                                boolean b2 = ((p1 & FLAGMSK_CAUSES_DENTS) != 0) && ((y1 & FLAGMSK_DENTABLE) != 0) && !((x3 == OBJECT_TYPE_BALL_INDEX) && (p2 == SPADDLE_OBJECT_INST_START_POS))
                                        && !((x3 == OBJECT_TYPE_WALL_INDEX) && ((y1 & FLAGMSK_IS_TETRIS) != 0));


                                boolean b3 = (x3 == OBJECT_TYPE_BALL_INDEX) && ((y1 & FLAGMSK_REFLECTS_BALL) != 0);


                                boolean b4 = ((p1 & FLAGMSK_IS_TETRIS) != 0) && ((y1 & FLAGMSK_STOPS_TETRIS) != 0);


                                x1 = OBJECT_INST[p2 + OBJECT_INST_RECORD_X_INDEX] /*+ withXPlane */;
								
								
								y1 = OBJECT_INST[p2 + OBJECT_INST_RECORD_Y_INDEX] /*+ withYPlane*/;
								

								
								
								
								
								if (p2 != p3 && OBJECT_INST[p2 + OBJECT_INST_RECORD_VISIBLE_INDEX] == VISIBLE_YES && (b1 || b2 || b3 || b4)) {


                                    int withType = OBJECT_INST[p2 + OBJECT_INST_RECORD_TYPE_INDEX];

                                    int p4 = OBJECT_INST[p2 + OBJECT_INST_RECORD_IMAGE_POS_INDEX];

                                    int withTypePos = withType * OBJECT_TYPE_RECORD_SIZE;
                                    int withWidth = OBJECT_TYPES[withTypePos + OBJECT_TYPE_RECORD_WIDTH_INDEX];
                                    int withHeight = OBJECT_TYPES[withTypePos + OBJECT_TYPE_RECORD_HEIGHT_INDEX];
                                    int withX2 = x1 + withWidth * GR_BLOCK_WIDTH;
                                    int withY2 = y1 - withHeight * GR_BLOCK_HEIGHT;


                                    int p5 = p0;

                                    for (int sourceY = 0; sourceY < sourceHeight; sourceY++) {
										
										y5 = ypos + sourceY * GR_BLOCK_WIDTH;
										for (int sourceX = 0; sourceX < sourceWidth; sourceX++, p5++) {
											
											x5 = xpos + sourceX * GR_BLOCK_WIDTH;
											if (OBJECT_INST[p5] != PALETTE_BACKGROUND_POS) {
												
												
												
												for (int corner = 0; corner < 4; corner++) {

                                                    int x6 = x5 + ((corner & 1) * (GR_BLOCK_WIDTH - 1));

                                                    int y6 = y5 - (((corner >> 1) & 1) * (GR_BLOCK_HEIGHT - 1));


                                                    int overlapX = (x6 - x1) / GR_BLOCK_WIDTH;
													if (overlapX >= withWidth || overlapX < 0) {
														continue;
													}

                                                    int p6 = p4 + overlapX;


                                                    int overlapY = (y1 - y6) / GR_BLOCK_HEIGHT;
													if (overlapY >= withHeight || overlapY < 0) {
														continue;
													}
													p6 += overlapY * withWidth;

													if (x6 >= x1 && x6 < withX2 && y6 <= y1 && y6 > withY2 && OBJECT_INST[p6] != PALETTE_BACKGROUND_POS) {
														collision = true;

														
														overlapX = xdir * (x6 - x1 - (overlapX * GR_BLOCK_WIDTH) - withXOverlapSubt);
														overlapY = (0 - ydir) * (y1 - (overlapY * GR_BLOCK_HEIGHT) - y6 - withYOverlapAdd);

														
														
														

														if (b1) {


                                                            int y7 = VISIBLE_DESTROY;
                                                            if (p2 < OBJECT_INST_FIRST_DESTROYABLE_POS) {
																y7 = VISIBLE_NO;
															}
															OBJECT_INST[p2 + OBJECT_INST_RECORD_VISIBLE_INDEX] = y7;
															

															if (x3 == OBJECT_TYPE_ENEMYFIRE_INDEX) {
																
																
																OBJECT_INST[p3 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_DESTROY;
															}

															
															
														}
														if (b2) {
															
															
															OBJECT_INST[p6] = PALETTE_BACKGROUND_POS;
														}
														if (b3) {
															
															ballReflect = true;
															ballReflectNow = true;
															if (p2 == NPADDLE_OBJECT_INST_START_POS) {
																collisionPaddle = -1;
															}
															if (p2 == SPADDLE_OBJECT_INST_START_POS) {
																collisionPaddle = 1;
															}
															collisionXPixels += overlapX;
															collisionYPixels += overlapY;

															
															
															nextXPos = xpos;
															nextYPos = ypos;
														}
														if (b4) {
															
															
															
															
															if (y6 < highestTetrisPos) {
																highestTetrisPos = y6;
															}
														}
														
													}
													
													
												}
											}
											
										}
										
									}

									

									
								}

								
								
							}
							

							
							
							if (collision) {
								if (highestTetrisPos < nextYPos) {
									nextYPos = highestTetrisPos;
									OBJECT_INST[p0 + TETRIS_IMAGE_DATA_IS_STICKY_IPOS] = 1;
								}
								if (ballReflectNow) {
									
									
									
									
									y1 = OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS];
									
									
									
									if ((collisionPaddle < 0 && y1 < 0) || 
											(collisionPaddle > 0 && y1 > 0) || 
											(collisionPaddle == 0 && collisionXPixels >= collisionYPixels) 
									) {
										
										
										
										if (collisionXPixels > collisionYPixels) {
											OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] += (GR_BLOCK_WIDTH / 2) - collisionXPixels;
										}
										if (collisionPaddle != 0) {
											y1 += ydir;
											OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] += xdir;
										}
										OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS] = 0 - y1;
									}
									
									if (collisionYPixels > collisionXPixels) {
										
										
										
										OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS] += (GR_BLOCK_HEIGHT / 2) - collisionYPixels;
										OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] *= -1;
									}

									
									if (OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] > BALL_MAX_X_SPEED) {
										OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] = BALL_MAX_X_SPEED;
									}
									if (OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] < 0 - BALL_MAX_X_SPEED) {
										OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] = 0 - BALL_MAX_X_SPEED;
									}
									if (OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] > 0 - BALL_MIN_X_SPEED && OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] < BALL_MIN_X_SPEED) {
										OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS] = BALL_MIN_X_SPEED * xdir;
									}
									if (OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS] > BALL_MAX_Y_SPEED) {
										OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS] = BALL_MAX_Y_SPEED;
									}
									if (OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS] < 0 - BALL_MAX_Y_SPEED) {
										OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS] = 0 - BALL_MAX_Y_SPEED;
									}
									if (OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS] > 0 - BALL_MIN_Y_SPEED && OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS] < BALL_MIN_Y_SPEED) {
										OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS] = BALL_MIN_Y_SPEED * ydir;
									}

									reflectCount++;
								}

								
								break movementLoop;
							}
							if (((p1 & FLAGMSK_IS_TETRIS) != 0) && highestTetrisPos != nextYPos) {
								
								OBJECT_INST[p0 + TETRIS_IMAGE_DATA_IS_STICKY_IPOS] = 0;
							}
							if (xpos == nextXPos && ypos == nextYPos) {
								break movementLoop;
							}
							
							
							y1 = movementError << 1;
							
							if (y1 > -ydelta) {
								movementError -= ydelta;
								xpos += xdir;
							}
							if (y1 < xdelta) {
								movementError += xdelta;
								ypos += ydir;
							}
							
						}

					}

					
					

					if (x3 == OBJECT_TYPE_BALL_INDEX) {
						if (nextYPos < BALL_N_BOUND) {
							
							playerScore++;
							computerScoreColor = PALETTE_SCORE_NEUTRAL_POS;
							playerScoreColor = PALETTE_SCORE_SCORED_POS;
							continue startLife;
						}
						if (nextYPos > BALL_S_BOUND) {
							
							computerScore++;
							computerScoreColor = PALETTE_SCORE_SCORED_POS;
							playerScoreColor = PALETTE_SCORE_NEUTRAL_POS;
							continue startLife;
						}
						OBJECT_INST[p0 + BALL_IMAGE_DATA_X_IPOS] += OBJECT_INST[p0 + BALL_IMAGE_DATA_DX_IPOS];
						OBJECT_INST[p0 + BALL_IMAGE_DATA_Y_IPOS] += OBJECT_INST[p0 + BALL_IMAGE_DATA_DY_IPOS];
						nextXPos = OBJECT_INST[p0 + BALL_IMAGE_DATA_X_IPOS] >> BALL_MOVEMENT_DECODE_SHR;
						nextYPos = OBJECT_INST[p0 + BALL_IMAGE_DATA_Y_IPOS] >> BALL_MOVEMENT_DECODE_SHR;
					} else if (nextYPos - (OBJECT_TYPES[x3 + OBJECT_TYPE_RECORD_HEIGHT_INDEX] * GR_BLOCK_HEIGHT) > RENDER_HEIGHT) {
						OBJECT_INST[p3 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_DESTROY;
					}
					OBJECT_INST[p3 + OBJECT_INST_RECORD_X_INDEX] = nextXPos;
					OBJECT_INST[p3 + OBJECT_INST_RECORD_Y_INDEX] = nextYPos;

					

					
					
					
				}
				
				

				
				
				
				for (p0 = 0; p0 < objectCount * OBJECT_INST_RECORD_SIZE; p0 += OBJECT_INST_RECORD_SIZE) {
					
					y3 = OBJECT_INST[p0 + OBJECT_INST_RECORD_TYPE_INDEX] * OBJECT_TYPE_RECORD_SIZE;
					

					if (OBJECT_INST[p0 + OBJECT_INST_RECORD_VISIBLE_INDEX] == VISIBLE_YES) {
						
						
						
						p3 = OBJECT_INST[p0 + OBJECT_INST_RECORD_IMAGE_POS_INDEX];
						

						if (y3 == OBJECT_TYPE_MISSILE_HEAD_POS) {
							
							ogr.setColor(COLORS[PALETTE_MISSILE_PATH_POS]);
							ogr.drawLine(OBJECT_INST[p3 + MISSILE_HEAD_DATA_PATH_START_X_IPOS], OBJECT_INST[p3 + MISSILE_HEAD_DATA_PATH_START_Y_IPOS],
									OBJECT_INST[p0 + OBJECT_INST_RECORD_X_INDEX] + 2, OBJECT_INST[p0 + OBJECT_INST_RECORD_Y_INDEX] - 2);
						}

						
						b1 = p0 <= BALL_OBJECT_INST_START_POS && p0 != SPADDLE_OBJECT_INST_START_POS;
						
						

						
						y4 = OBJECT_TYPES[y3 + OBJECT_TYPE_RECORD_HEIGHT_INDEX] * GR_BLOCK_HEIGHT;
						
						x4 = OBJECT_TYPES[y3 + OBJECT_TYPE_RECORD_WIDTH_INDEX] * GR_BLOCK_WIDTH;

						
						
						
						for (y1 = GR_BLOCK_HEIGHT; y1 <= y4; y1 += GR_BLOCK_HEIGHT) {
							
							
							for (x1 = 0; x1 < x4; x1 += GR_BLOCK_WIDTH) {
								
								if (OBJECT_INST[p3] != PALETTE_BACKGROUND_POS) {
									b1 = true;
									ogr.setColor(COLORS[OBJECT_INST[p3]]);
									ogr.fillRect(x1 + OBJECT_INST[p0 + OBJECT_INST_RECORD_X_INDEX], OBJECT_INST[p0 + OBJECT_INST_RECORD_Y_INDEX] - y1, GR_BLOCK_WIDTH, GR_BLOCK_HEIGHT);
								}
								p3++;
							}
							
						}
						
						if (!b1) {
							
							OBJECT_INST[p0 + OBJECT_INST_RECORD_VISIBLE_INDEX] = VISIBLE_DESTROY;
						}
						
						
						
						
					}

					if (OBJECT_INST[p0 + OBJECT_INST_RECORD_VISIBLE_INDEX] == VISIBLE_DESTROY) {
						
						

						
						
						
						
						
						if (p0 == SPADDLE_OBJECT_INST_START_POS) {
							
							computerScore++;
							computerScoreColor = PALETTE_SCORE_SCORED_POS;
							playerScoreColor = PALETTE_SCORE_NEUTRAL_POS;
							continue startLife;
						}

						
						p3 = OBJECT_INST[p0 + OBJECT_INST_RECORD_IMAGE_POS_INDEX];
						

						
						
						objectCount--;
						
						
						
						for (y1 = p0; y1 < objectCount * OBJECT_INST_RECORD_SIZE; y1++) {
							
							OBJECT_INST[y1] = OBJECT_INST[y1 + OBJECT_INST_RECORD_SIZE];
						}

						
						OBJECT_INST[y1 + OBJECT_INST_RECORD_IMAGE_POS_INDEX] = p3;
						
						

						
						
						p0 -= OBJECT_INST_RECORD_VISIBLE_INDEX;
					}
					
				}
				
				

				
				
				ogr.setColor(COLORS[computerScoreColor]);
				ogr.drawString(Integer.toString(computerScore), SCORE_COMP_X, SCORE_COMP_Y);
				ogr.setColor(COLORS[playerScoreColor]);
				ogr.drawString(Integer.toString(playerScore), SCORE_PLAYER_X, SCORE_PLAYER_Y);

				
				
				
				sg.drawImage(image, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT, 
						0, 0, RENDER_WIDTH, RENDER_HEIGHT, 
						null);

				
				
				do {
					Thread.yield();
				} while (System.nanoTime() - lastTime < 0);
				if (!isActive()) {
					
					
					
					
					return;
				}
				lastTime += FRAME_WAIT_TIME;
			}
		}
	}

	@Override
    public boolean handleEvent(Event e) {
		
		
		
		
		

		

		switch (e.id) {
		
		
		
		
		case Event.MOUSE_UP:
			
			this.mouseClick = true;
			
		case Event.MOUSE_MOVE:
		case Event.MOUSE_DRAG:
			this.mousePos = (e.x >>> ENCODE_MOUSE_X_SHR)

			
			
			
					+ ((e.y >>> ENCODE_MOUSE_Y_SHR) << ENCODE_MOUSE_Y_SHL);
		}
		return false;
	}

	
	private static final int DISPLAY_WIDTH = 408;
	private static final int DISPLAY_HEIGHT = 512;
	private static final int RENDER_SCALING_FACTOR = 2; 
	private static final int RENDER_SHR_FACTOR = 1; 
	private static final int RENDER_WIDTH = DISPLAY_WIDTH / RENDER_SCALING_FACTOR;
	private static final int RENDER_HEIGHT = DISPLAY_HEIGHT / RENDER_SCALING_FACTOR;

	
	private static final int FRAMES_PER_SECOND = 30;
	private static final long NANOS_PER_SECOND = 1000000000;
	private static final long FRAME_WAIT_TIME = NANOS_PER_SECOND / FRAMES_PER_SECOND;
	private static final int ENCODE_MOUSE_X_SHR = RENDER_SHR_FACTOR;
	private static final int ENCODE_MOUSE_Y_SHL = 8; 
	private static final int ENCODE_MOUSE_Y_SHR = RENDER_SHR_FACTOR;
	
	
	
	
	
	private static final int MOUSE_X_MASK = 0xff; 
	private static final int MOUSE_Y_SHR = ENCODE_MOUSE_Y_SHL;

	private static final int PALETTE_COMPONENT_MAX = 4;
	private static final int PALETTE_COMPONENT_SHL = 6;
	private static final int PALETTE_COUNT = PALETTE_COMPONENT_MAX * PALETTE_COMPONENT_MAX * PALETTE_COMPONENT_MAX;

	
	private static final int PALETTE_BACKGROUND_POS = 0;
	private static final int PALETTE_SCORE_NEUTRAL_POS = 63;
	private static final int PALETTE_SCORE_SCORED_POS = 14;
	private static final int PALETTE_NET_POS = 63;
	private static final int PALETTE_BREAKOUT_POS = 20;
	private static final int PALETTE_BREAKOUT_MOD = PALETTE_COUNT - PALETTE_BREAKOUT_POS - 1;
	private static final int PALETTE_INVADER1_POS = 14;
	private static final int PALETTE_INVADER2_POS = PALETTE_INVADER1_POS;
	private static final int PALETTE_NPADDLE_POS = 63;
	private static final int PALETTE_SPADDLE_POS = 63;
	private static final int PALETTE_BALL_POS = 63;
	private static final int PALETTE_ENEMYFIRE_POS = 3;
	private static final int PALETTE_TETRIS_POS = 11;
	private static final int PALETTE_MISSILE_HEAD_POS = 5;
	private static final int PALETTE_MISSILE_HEAD2_POS = 63;
	private static final int PALETTE_MISSILE_PATH_POS = 3;
	private static final int PALETTE_MISSILE_EXPLODE_POS = 63;
	private static final int PALETTE_MISSILE_EXPLODE_BASE_POS = 58;
	private static final int PALETTE_MISSILE_EXPLODE_DELTA = PALETTE_MISSILE_EXPLODE_POS - PALETTE_MISSILE_EXPLODE_BASE_POS;

	
	private static final int GR_BLOCK_WIDTH = 4;
	private static final int GR_BLOCK_HEIGHT = 4;
	private static final int GR_SCREEN_BLOCK_WIDTH = RENDER_WIDTH / GR_BLOCK_WIDTH;
	private static final int GR_SCREEN_BLOCK_HEIGHT = RENDER_HEIGHT / GR_BLOCK_HEIGHT;

	private static final int GR_PADDLE_BLOCK_WIDTH = 8;

	private static final int DR_NET_WIDTH = 5;
	private static final int DR_NET_HEIGHT = 2;
	private static final int DR_NET_SPACING = 6;
	private static final int DR_NET_SEP = DR_NET_WIDTH + DR_NET_SPACING;
	private static final int DR_NET_Y = RENDER_HEIGHT / 2;

	private static final int SCORE_PLAYER_X = GR_BLOCK_WIDTH;
	private static final int SCORE_PLAYER_Y = DR_NET_Y + GR_BLOCK_HEIGHT * 4;
	private static final int SCORE_COMP_X = GR_BLOCK_WIDTH;
	private static final int SCORE_COMP_Y = DR_NET_Y - GR_BLOCK_HEIGHT - 2;

	private static final int BASE_EVENT_MOD = 1;
	
	private static final int MAX_EVENT_MOD = 10;

	private static final int REFLECT_SPECIAL_BITS = 0x03;
	private static final int SPECIAL_COMPUTE_EQUALS = 3;
	private static final int REFLECT_SPECIAL_SHR = 3;

	
	
	

	private static final int OBJECT_TYPE_WALL_INDEX = 0;

	private static final int OBJECT_TYPE_BREAKOUT_INDEX = 1;
	private static final int OBJECT_TYPE_INVADER1_INDEX = 2;
	private static final int OBJECT_TYPE_INVADER2_INDEX = 3;
	private static final int OBJECT_TYPE_NPADDLE_INDEX = 4;
	private static final int OBJECT_TYPE_SPADDLE_INDEX = 5;
	private static final int OBJECT_TYPE_BALL_INDEX = 6;

	private static final int OBJECT_TYPE_MISSILE_HEAD_INDEX = 7;
	private static final int OBJECT_TYPE_MISSILE_EXPLODE_1_INDEX = 8;
	private static final int OBJECT_TYPE_MISSILE_EXPLODE_2_INDEX = 9;
	private static final int OBJECT_TYPE_MISSILE_EXPLODE_3_INDEX = 10;
	private static final int OBJECT_TYPE_ENEMYFIRE_INDEX = 11;

	
	private static final int OBJECT_TYPE_TETRIS_LHOOK_INDEX = 12;
	
	
	

	private static final int OBJECT_TYPE_TETRIS_RHOOK_INDEX = 13;
	
	
	

	private static final int OBJECT_TYPE_TETRIS_BAR_INDEX = 14;
	

	private static final int OBJECT_TYPE_TETRIS_BOX_INDEX = 15;
	
	

	private static final int OBJECT_TYPE_TETRIS_BUMP_INDEX = 16;
	
	

	private static final int OBJECT_TYPE_COUNT = 17;

	
	

	
	
	

	

	
	
	
	
	
	
	
	
	private static final int OBJECT_TYPE_RECORD_FLAG_INDEX = 0;
	private static final int OBJECT_TYPE_RECORD_COLOR_INDEX = 1;
	private static final int OBJECT_TYPE_RECORD_WIDTH_INDEX = 2;
	private static final int OBJECT_TYPE_RECORD_HEIGHT_INDEX = 3;
	private static final int OBJECT_TYPE_RECORD_PREPOPULATE_VCOUNT_INDEX = 4;
	private static final int OBJECT_TYPE_RECORD_PREPOPULATE_HCOUNT_INDEX = 5;
	private static final int OBJECT_TYPE_RECORD_PREPOPULATE_VINDENT_INDEX = 6;
	private static final int OBJECT_TYPE_RECORD_PREPOPULATE_HINDENT_INDEX = 7;
	private static final int OBJECT_TYPE_RECORD_PREPOPULATE_VSPACE_INDEX = 8;
	private static final int OBJECT_TYPE_RECORD_PREPOPULATE_HSPACE_INDEX = 9;
	private static final int OBJECT_TYPE_RECORD_IMAGE_INDEX = 10;

	private static final int IMAGE_BITS_PER_POS = 8;
	private static final int MISSILE_EXPLODE_WIDTH = 16; 
	private static final int MISSILE_EXPLODE_HEIGHT = 12;
	private static final int OBJECT_TYPE_RECORD_MAX_IMAGE_SIZE = (MISSILE_EXPLODE_WIDTH * MISSILE_EXPLODE_WIDTH) / IMAGE_BITS_PER_POS;
	
	

	private static final int OBJECT_TYPE_RECORD_IMAGE_SIZE_UNPACKED = OBJECT_TYPE_RECORD_MAX_IMAGE_SIZE * IMAGE_BITS_PER_POS;

	private static final int OBJECT_TYPE_RECORD_SIZE = OBJECT_TYPE_RECORD_IMAGE_INDEX + OBJECT_TYPE_RECORD_MAX_IMAGE_SIZE;
	private static final int OBJECT_TYPE_SIZE = OBJECT_TYPE_RECORD_SIZE * OBJECT_TYPE_COUNT + 1;

	

	private static final int OBJECT_TYPE_WALL_POS = 0;
	private static final int OBJECT_TYPE_BREAKOUT_POS = OBJECT_TYPE_WALL_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_INVADER1_POS = OBJECT_TYPE_BREAKOUT_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_INVADER2_POS = OBJECT_TYPE_INVADER1_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_NPADDLE_POS = OBJECT_TYPE_INVADER2_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_SPADDLE_POS = OBJECT_TYPE_NPADDLE_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_BALL_POS = OBJECT_TYPE_SPADDLE_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_MISSILE_HEAD_POS = OBJECT_TYPE_BALL_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_MISSILE_EXPLODE_1_POS = OBJECT_TYPE_MISSILE_HEAD_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_MISSILE_EXPLODE_2_POS = OBJECT_TYPE_MISSILE_EXPLODE_1_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_MISSILE_EXPLODE_3_POS = OBJECT_TYPE_MISSILE_EXPLODE_2_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_ENEMYFIRE_POS = OBJECT_TYPE_MISSILE_EXPLODE_3_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_TETRIS_LHOOK_POS = OBJECT_TYPE_ENEMYFIRE_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_TETRIS_RHOOK_POS = OBJECT_TYPE_TETRIS_LHOOK_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_TETRIS_BAR_POS = OBJECT_TYPE_TETRIS_RHOOK_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_TETRIS_BOX_POS = OBJECT_TYPE_TETRIS_BAR_POS + OBJECT_TYPE_RECORD_SIZE;
	private static final int OBJECT_TYPE_TETRIS_BUMP_POS = OBJECT_TYPE_TETRIS_BOX_POS + OBJECT_TYPE_RECORD_SIZE;

	
	

	
	
	
	
	
	
	
	

	
	

	
	

	
	
	private static final int OBJECT_INST_RECORD_VISIBLE_INDEX = 0;
	private static final int OBJECT_INST_RECORD_IMAGE_POS_INDEX = 1;
	private static final int OBJECT_INST_RECORD_X_INDEX = 2;
	private static final int OBJECT_INST_RECORD_Y_INDEX = 3;
	private static final int OBJECT_INST_RECORD_NEEDSCOPY_INDEX = 4;
	private static final int OBJECT_INST_RECORD_FLAG_INDEX = 5;
	private static final int OBJECT_INST_RECORD_TYPE_INDEX = 6;
	private static final int OBJECT_INST_RECORD_SIZE = 7;

	
	
	private static final int OBJECT_INST_RECORD_COUNT = 800;
	private static final int MAX_OBJECT_INST_RECORDS = OBJECT_INST_RECORD_COUNT - 1;

	private static final int OBJECT_INST_IMAGE_START_POS = OBJECT_INST_RECORD_COUNT * OBJECT_INST_RECORD_SIZE;

	private static final int OBJECT_INST_IMAGE_SIZE = RENDER_WIDTH * 2; 
	

	private static final int OBJECT_INST_IMAGE_BASE_START_POS = OBJECT_INST_IMAGE_START_POS;

	private static final int OBJECT_INST_IMAGEPART_SIZE = (OBJECT_INST_IMAGE_SIZE * (OBJECT_INST_RECORD_COUNT));
	

	private static final int OBJECT_INST_SIZE = OBJECT_INST_IMAGE_START_POS + OBJECT_INST_IMAGEPART_SIZE;

	
	private static final int NEEDSCOPY_NOOP = 0;
	private static final int NEEDSCOPY_COPY = 1;

	
	private static final int VISIBLE_NO = 0;
	private static final int VISIBLE_YES = 1;
	private static final int VISIBLE_DESTROY = 2;

	
	
	
	
	
	
	
	
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	
	

	
	
	
	
	
	
	
	
	
	
	
	

	
	private static final int FLAGMSK_DESTROYABLE = 0x0001;
	private static final int FLAGSHR_DESTROYABLE = 0;

	
	
	private static final int FLAGMSK_CAUSES_DENTS = 0x0002;
	private static final int FLAGSHR_CAUSES_DENTS = 1;

	
	private static final int FLAGMSK_CAUSES_DESTROY = 0x0004;
	private static final int FLAGSHR_CAUSES_DESTROY = 2;

	
	private static final int FLAGMSK_DENTABLE = 0x0008;
	private static final int FLAGSHR_DENTABLE = 3;

	
	private static final int FLAGMSK_REFLECTS_BALL = 0x0010;
	private static final int FLAGSHR_REFLECTS_BALL = 4;

	
	
	private static final int FLAGMSK_STOPS_TETRIS = 0x0020;
	private static final int FLAGSHR_STOPS_TETRIS = 5;

	
	
	private static final int FLAGMSK_IS_TETRIS = 0x0040;
	private static final int FLAGSHR_IS_TETRIS = 6;

	
	private static final int WALL_FLAGS = FLAGMSK_REFLECTS_BALL | FLAGMSK_CAUSES_DENTS;
	private static final int BALL_STICKY_FLAGS = 0;
	private static final int COLLIDES_FLAGS = FLAGMSK_CAUSES_DENTS | FLAGMSK_CAUSES_DESTROY | FLAGMSK_IS_TETRIS;

	
	
	
	
	
	private static final int WALL_BLOCK_HEIGHT = 1;
	private static final int WALL_SEGMENT_HEIGHT = WALL_BLOCK_HEIGHT * GR_BLOCK_HEIGHT;
	private static final int WALL_SEGMENT_MIN_BLOCK_WIDTH = 1;
	private static final int WALL_SEGMENT_MIN_WIDTH = WALL_SEGMENT_MIN_BLOCK_WIDTH * GR_BLOCK_WIDTH;
	private static final int WALL_IMAGE_BLOCK_WIDTH = GR_SCREEN_BLOCK_WIDTH + 2;
	
	private static final int WALL_IMAGE_LBLOCK_INDEX = 0;
	private static final int WALL_IMAGE_RBLOCK_INDEX = WALL_IMAGE_BLOCK_WIDTH - 1;
	private static final int WALL_SEGMENT_MAX_BLOCK_WIDTH = WALL_IMAGE_BLOCK_WIDTH - GR_PADDLE_BLOCK_WIDTH - 2;
	private static final int WALL_VISIBLE_COUNT = RENDER_HEIGHT / WALL_SEGMENT_HEIGHT;
	
	private static final int WALL_TOTAL_COUNT = WALL_VISIBLE_COUNT + 1;
	private static final int WALL_START_Y = RENDER_HEIGHT + WALL_SEGMENT_HEIGHT;
	
	private static final int WALL_END_Y = WALL_START_Y - (WALL_TOTAL_COUNT * WALL_SEGMENT_HEIGHT);
	private static final int WALL_START_X = 0 - WALL_SEGMENT_MIN_WIDTH;
	private static final int MIN_WALL_LEFT = 1;
	private static final int MAX_WALL_LEFT = GR_SCREEN_BLOCK_WIDTH - GR_PADDLE_BLOCK_WIDTH;
	private static final int MIN_WALL_RIGHT = GR_PADDLE_BLOCK_WIDTH;
	private static final int MAX_WALL_RIGHT = GR_SCREEN_BLOCK_WIDTH;

	private static final int WALL_MOVEMENT_1 = GR_SCREEN_BLOCK_WIDTH >> 1;
	private static final int WALL_MOVEMENT_2 = GR_PADDLE_BLOCK_WIDTH << 1;

	private static final int WALL_ADD_COUNT_1 = 12 * GR_BLOCK_HEIGHT;
	private static final int WALL_ADD_COUNT_2 = 24 * GR_BLOCK_HEIGHT;

	
	
	private static final int WALL_IMAGE_DATA_START_IPOS = WALL_IMAGE_BLOCK_WIDTH * WALL_BLOCK_HEIGHT;
	private static final int WALL_IMAGE_DATA_LBREAK_INDEX = 0;
	private static final int WALL_IMAGE_DATA_LBREAK_IPOS = WALL_IMAGE_DATA_START_IPOS + WALL_IMAGE_DATA_LBREAK_INDEX;
	private static final int WALL_IMAGE_DATA_RBREAK_INDEX = 1;
	private static final int WALL_IMAGE_DATA_RBREAK_IPOS = WALL_IMAGE_DATA_START_IPOS + WALL_IMAGE_DATA_RBREAK_INDEX;

	private static final int PALETTE_WALL_START = 48;
	private static final int PALETTE_WALL_END = 60;
	private static final int WALL_PALETTE_DELTA = (PALETTE_WALL_END - PALETTE_WALL_START);

	private static final int WALL_TYPE_WIDTH_POS = OBJECT_TYPE_WALL_POS + OBJECT_TYPE_RECORD_WIDTH_INDEX;
	private static final int WALL_TYPE_HEIGHT_POS = OBJECT_TYPE_WALL_POS + OBJECT_TYPE_RECORD_HEIGHT_INDEX;

	private static final int WALL_OBJECT_INST_START_INDEX = 0;
	private static final int WALL_OBJECT_INST_START_POS = WALL_OBJECT_INST_START_INDEX * OBJECT_INST_RECORD_SIZE;

	
	
	private static final int BREAKOUT_BLOCK_HEIGHT = 2;
	private static final int BREAKOUT_BLOCK_WIDTH = 8;

	private static final int BREAKOUT_BLOCK_H_SPACING = 1;
	private static final int BREAKOUT_BLOCK_V_SPACING = 1;

	private static final int BREAKOUT_BLOCK_ACTUAL_WIDTH = BREAKOUT_BLOCK_WIDTH + BREAKOUT_BLOCK_H_SPACING;
	private static final int BREAKOUT_BLOCK_ACTUAL_HEIGHT = BREAKOUT_BLOCK_HEIGHT + BREAKOUT_BLOCK_V_SPACING;

	private static final int BREAKOUT_BLOCK_H_COUNT = GR_SCREEN_BLOCK_WIDTH / BREAKOUT_BLOCK_ACTUAL_WIDTH;
	private static final int BREAKOUT_BLOCK_V_COUNT = GR_SCREEN_BLOCK_HEIGHT / 5;

	private static final int BREAKCOUT_COUNT = BREAKOUT_BLOCK_H_COUNT * BREAKOUT_BLOCK_V_COUNT;

	private static final int BREAKOUT_ROW_OBJECT_INST_SIZE = BREAKOUT_BLOCK_H_COUNT * OBJECT_INST_RECORD_SIZE;

	
	private static final int BREAKOUT_BLOCK_V_OFFSET = 6;

	private static final int BREAKOUT_BLOCK_H_OFFSET = (GR_SCREEN_BLOCK_WIDTH - (BREAKOUT_BLOCK_ACTUAL_WIDTH * BREAKOUT_BLOCK_H_COUNT)) / 2;

	private static final int BREAKOUT_OBJECT_INST_START_INDEX = WALL_OBJECT_INST_START_INDEX + WALL_TOTAL_COUNT;
	private static final int BREAKOUT_OBJECT_INST_START_POS = BREAKOUT_OBJECT_INST_START_INDEX * OBJECT_INST_RECORD_SIZE;

	
	
	private static final int INVADER_BLOCK_HEIGHT = 4;
	private static final int INVADER_BLOCK_WIDTH = 4;
	private static final int INVADER_BLOCK_H_SPACING = 4 + INVADER_BLOCK_WIDTH;
	private static final int INVADER_BLOCK_V_SPACING = 4 + INVADER_BLOCK_HEIGHT;
	private static final int INVADER_BLOCK_H_COUNT = 6;
	private static final int INVADER1_BLOCK_V_COUNT = 1;
	private static final int INVADER1_BLOCK_V_OFFSET = 20; 
	private static final int INVADER1_BLOCK_H_OFFSET = INVADER_BLOCK_WIDTH;

	private static final int INVADER2_BLOCK_V_COUNT = 1;
	private static final int INVADER2_BLOCK_V_OFFSET = INVADER1_BLOCK_V_OFFSET + ((INVADER_BLOCK_V_SPACING + INVADER_BLOCK_HEIGHT) * INVADER1_BLOCK_V_COUNT);
	private static final int INVADER2_BLOCK_H_OFFSET = INVADER1_BLOCK_H_OFFSET + GR_BLOCK_WIDTH;

	private static final int INVADER_CHANGE_IMAGE_MOD = FRAMES_PER_SECOND;

	private static final int INVADER_FRAMES_PER_MOVE = 4;
	private static final int INVADER_FRAMES_PER_MOVE_SHR = 2;
	private static final int INVADER_MAX_H_MOVEMENT = 12;
	private static final int INVADER_MAX_V_MOVEMENT = 4;
	private static final int INVADER_HL_MOVEMENT_START = 0;
	private static final int INVADER_DOWN_MOVEMENT_START = INVADER_HL_MOVEMENT_START + INVADER_MAX_H_MOVEMENT;
	private static final int INVADER_RIGHT_MOVEMENT_START = INVADER_DOWN_MOVEMENT_START + INVADER_MAX_V_MOVEMENT;
	private static final int INVADER_UP_MOVEMENT_START = INVADER_RIGHT_MOVEMENT_START + INVADER_MAX_H_MOVEMENT;
	private static final int INVADER_FRAME_MOVE_MOD = INVADER_UP_MOVEMENT_START + INVADER_MAX_V_MOVEMENT;

	private static final int INVADER_BULLET_XDELTA = (INVADER_BLOCK_WIDTH * GR_BLOCK_WIDTH) / 2;
	private static final int INVADER_FIRE_FREQUENCY = RENDER_HEIGHT;

	private static final int INVADER_OBJECT_INST_START_INDEX = BREAKOUT_OBJECT_INST_START_INDEX + BREAKCOUT_COUNT;
	private static final int INVADER_OBJECT_INST_START_POS = INVADER_OBJECT_INST_START_INDEX * OBJECT_INST_RECORD_SIZE;

	private static final int INVADER_IMAGE_DATA_START_IPOS = INVADER_BLOCK_HEIGHT * INVADER_BLOCK_WIDTH;
	private static final int INVADER_DATA_BASE_X_IPOS = INVADER_IMAGE_DATA_START_IPOS;
	private static final int INVADER_DATA_BASE_Y_IPOS = INVADER_DATA_BASE_X_IPOS + 1;
	private static final int INVADER_DATA_FIRE_IPOS = INVADER_DATA_BASE_Y_IPOS + 1;

	private static final int INVADER_COUNT = (INVADER_BLOCK_H_COUNT * INVADER1_BLOCK_V_COUNT) + (INVADER_BLOCK_H_COUNT * INVADER2_BLOCK_V_COUNT);

	
	
	private static final int NPADDLE_BASE_SPEED = 3;
	private static final int NPADDLE_MIN_SPEED = 2;
	private static final int NPADDLE_MAX_SPEED = 8;
	private static final int NPADDLE_BLOCK_HEIGHT = 1;
	private static final int NPADDLE_BLOCK_WIDTH = GR_PADDLE_BLOCK_WIDTH;
	private static final int NPADDLE_WIDTH = NPADDLE_BLOCK_WIDTH * GR_BLOCK_WIDTH;
	private static final int NPADDLE_BLOCK_STARTX = (GR_SCREEN_BLOCK_WIDTH - NPADDLE_BLOCK_WIDTH) / 2;
	private static final int NPADDLE_BLOCK_Y = 1;
	private static final int NPADDLE_Y = NPADDLE_BLOCK_Y * NPADDLE_BLOCK_HEIGHT;

	private static final int NPADDLE_COUNT = 1;

	private static final int NPADDLE_OBJECT_INST_START_INDEX = INVADER_OBJECT_INST_START_INDEX + INVADER_COUNT;
	private static final int NPADDLE_OBJECT_INST_START_POS = NPADDLE_OBJECT_INST_START_INDEX * OBJECT_INST_RECORD_SIZE;
	private static final int NPADDLE_OBJECT_INST_VISIBLE_POS = NPADDLE_OBJECT_INST_START_POS + OBJECT_INST_RECORD_VISIBLE_INDEX;
	private static final int NPADDLE_OBJECT_INST_X_POS = NPADDLE_OBJECT_INST_START_POS + OBJECT_INST_RECORD_X_INDEX;
	private static final int NPADDLE_OBJECT_INST_Y_POS = NPADDLE_OBJECT_INST_START_POS + OBJECT_INST_RECORD_Y_INDEX;

	private static final int NPADDLE_OBJECT_INST_IMAGE_POS_POS = NPADDLE_OBJECT_INST_START_POS + OBJECT_INST_RECORD_IMAGE_POS_INDEX;

	private static final int NPADDLE_IMAGE_DATA_START_IPOS = NPADDLE_BLOCK_HEIGHT * NPADDLE_BLOCK_WIDTH;
	private static final int NPADDLE_IMAGE_DATA_WALL_LEFT_IPOS = NPADDLE_IMAGE_DATA_START_IPOS; 
	
	private static final int NPADDLE_IMAGE_DATA_WALL_RIGHT_IPOS = NPADDLE_IMAGE_DATA_START_IPOS + 1; 
	

	
	
	private static final int SPADDLE_BLOCK_HEIGHT = 1;
	private static final int SPADDLE_BLOCK_WIDTH = GR_PADDLE_BLOCK_WIDTH;
	private static final int SPADDLE_WIDTH = SPADDLE_BLOCK_WIDTH * GR_BLOCK_WIDTH;
	private static final int SPADDLE_BLOCK_START_X = (GR_SCREEN_BLOCK_WIDTH - SPADDLE_BLOCK_WIDTH) / 2;
	private static final int SPADDLE_BLOCK_Y = GR_SCREEN_BLOCK_HEIGHT;
	private static final int SPADDLE_Y = SPADDLE_BLOCK_Y * GR_BLOCK_HEIGHT;
	private static final int SPADDLE_MIN_X = 0;
	private static final int SPADDLE_MAX_X = RENDER_WIDTH - GR_BLOCK_WIDTH;
	private static final int SPADDLE_MOUSE_OFFSET = SPADDLE_WIDTH / 2;
	private static final int SPADDLE_COUNT = 1;

	private static final int SPADDLE_OBJECT_INST_START_INDEX = NPADDLE_OBJECT_INST_START_INDEX + NPADDLE_COUNT;
	private static final int SPADDLE_OBJECT_INST_START_POS = SPADDLE_OBJECT_INST_START_INDEX * OBJECT_INST_RECORD_SIZE;
	private static final int SPADDLE_OBJECT_INST_VISIBLE_POS = SPADDLE_OBJECT_INST_START_POS + OBJECT_INST_RECORD_VISIBLE_INDEX;
	private static final int SPADDLE_OBJECT_INST_X_POS = SPADDLE_OBJECT_INST_START_POS + OBJECT_INST_RECORD_X_INDEX;
	private static final int SPADDLE_OBJECT_INST_Y_POS = SPADDLE_OBJECT_INST_START_POS + OBJECT_INST_RECORD_Y_INDEX;
	
	

	
	
	private static final int SPADDLE_IMAGE_DATA_START_IPOS = SPADDLE_BLOCK_WIDTH * SPADDLE_BLOCK_HEIGHT;

	
	
	private static final int BALL_BLOCK_HEIGHT = 1;
	private static final int BALL_BLOCK_WIDTH = 1;
	private static final int BALL_HEIGHT = BALL_BLOCK_HEIGHT * GR_BLOCK_HEIGHT;

	
	
	private static final int BALL_BLOCK_START_X = SPADDLE_BLOCK_START_X + 2;
	private static final int BALL_BLOCK_START_Y = SPADDLE_BLOCK_Y - 1;
	private static final int BALL_STICKY_Y = SPADDLE_Y - 1;
	private static final int BALL_STICKY_X_OFFSET = GR_BLOCK_WIDTH;
	private static final int BALL_Y = BALL_BLOCK_START_Y * BALL_BLOCK_HEIGHT;

	private static final int BALL_N_BOUND = 0;
	private static final int BALL_S_BOUND = RENDER_HEIGHT + GR_BLOCK_HEIGHT;

	
	
	private static final int BALL_MOVEMENT_DECODE_SHR = 4;
	private static final int BALL_MOVEMENT_ENCODE_SHL = 4;

	private static final int BALL_START_DX = 1 << BALL_MOVEMENT_ENCODE_SHL;
	private static final int BALL_START_DY = -3 << BALL_MOVEMENT_ENCODE_SHL;

	private static final int BALL_MIN_X_SPEED = 1 << BALL_MOVEMENT_ENCODE_SHL;
	private static final int BALL_MIN_Y_SPEED = (1 << BALL_MOVEMENT_ENCODE_SHL) + (3 << (BALL_MOVEMENT_ENCODE_SHL - 1));
	private static final int BALL_MAX_X_SPEED = (GR_BLOCK_WIDTH << BALL_MOVEMENT_ENCODE_SHL);
	private static final int BALL_MAX_Y_SPEED = (GR_BLOCK_HEIGHT << BALL_MOVEMENT_ENCODE_SHL) + (GR_BLOCK_HEIGHT << (BALL_MOVEMENT_ENCODE_SHL - 1));

	private static final int BALL_COUNT = 1;

	private static final int BALL_OBJECT_INST_START_INDEX = SPADDLE_OBJECT_INST_START_INDEX + SPADDLE_COUNT;
	private static final int BALL_OBJECT_INST_START_POS = BALL_OBJECT_INST_START_INDEX * OBJECT_INST_RECORD_SIZE;
	private static final int BALL_OBJECT_INST_VISIBLE_POS = BALL_OBJECT_INST_START_POS + OBJECT_INST_RECORD_VISIBLE_INDEX;
	private static final int BALL_OBJECT_INST_X_POS = BALL_OBJECT_INST_START_POS + OBJECT_INST_RECORD_X_INDEX;
	private static final int BALL_OBJECT_INST_Y_POS = BALL_OBJECT_INST_START_POS + OBJECT_INST_RECORD_Y_INDEX;

	private static final int BALL_OBJECT_INST_IMAGE_POS_POS = BALL_OBJECT_INST_START_POS + OBJECT_INST_RECORD_IMAGE_POS_INDEX;
	private static final int BALL_IMAGE_DATA_START_IPOS = BALL_BLOCK_HEIGHT * BALL_BLOCK_WIDTH;
	private static final int BALL_IMAGE_DATA_DX_IPOS = BALL_IMAGE_DATA_START_IPOS;
	private static final int BALL_IMAGE_DATA_DY_IPOS = BALL_IMAGE_DATA_DX_IPOS + 1;
	private static final int BALL_IMAGE_DATA_X_IPOS = BALL_IMAGE_DATA_DY_IPOS + 1;
	private static final int BALL_IMAGE_DATA_Y_IPOS = BALL_IMAGE_DATA_X_IPOS + 1;

	

	private static final int OBJECT_INST_FIRST_DESTROYABLE_INDEX = BALL_OBJECT_INST_START_INDEX + BALL_COUNT;
	private static final int OBJECT_INST_FIRST_DESTROYABLE_POS = OBJECT_INST_FIRST_DESTROYABLE_INDEX * OBJECT_INST_RECORD_SIZE;

	
	
	private static final int ENEMYFIRE_BLOCK_HEIGHT = 2;
	private static final int ENEMYFIRE_BLOCK_WIDTH = 1;
	private static final int ENEMYFIRE_HEIGHT = ENEMYFIRE_BLOCK_HEIGHT * GR_BLOCK_HEIGHT;
	private static final int ENEMYFIRE_FLAGS = FLAGMSK_CAUSES_DENTS | FLAGMSK_CAUSES_DESTROY | FLAGMSK_DESTROYABLE;
	

	private static final int ENEMYFIRE_MOVEMENT_Y = 3;

	
	
	private static final int TETRIS_BLOCK_HEIGHT = 8;
	private static final int TETRIS_BLOCK_WIDTH = 8;
	private static final int TETRIS_WIDTH = TETRIS_BLOCK_WIDTH * GR_BLOCK_WIDTH;
	private static final int TETRIS_FLAGS = FLAGMSK_IS_TETRIS | FLAGMSK_DENTABLE | FLAGMSK_REFLECTS_BALL | FLAGMSK_STOPS_TETRIS;
	private static final int TETRIS_START_Y = NPADDLE_Y + (NPADDLE_BLOCK_HEIGHT * GR_BLOCK_HEIGHT);
	private static final int TETRIS_IMAGE_SIZE = TETRIS_BLOCK_HEIGHT * TETRIS_BLOCK_WIDTH;
	private static final int TETRIS_TYPE_COUNT = 5;

	private static final int TETRIS_MOVEMENT_MOD = FRAMES_PER_SECOND;
	private static final int TETRIS_MOVEMENT_RATE = GR_BLOCK_HEIGHT * 2;

	private static final int TETRIS_IMAGE_DATA_START_IPOS = TETRIS_IMAGE_SIZE;
	private static final int TETRIS_IMAGE_DATA_IS_STICKY_IPOS = TETRIS_IMAGE_DATA_START_IPOS;
	private static final int TETRIS_IMAGE_TEMP_DATA_IPOS = TETRIS_IMAGE_DATA_IS_STICKY_IPOS + 1;

	
	

	private static final int MISSILE_HEAD_FLAGS = 0;
	private static final int MISSILE_HEAD_WIDTH = 1;
	private static final int MISSILE_HEAD_HEIGHT = 1;
	private static final int MISSILE_HEAD_IMAGE_SIZE = MISSILE_HEAD_WIDTH * MISSILE_HEAD_HEIGHT;

	private static final int MISSILE_HEAD_EXPLODE_Y = SPADDLE_Y - (GR_BLOCK_HEIGHT * 2);

	private static final int MISSILE_MIN_DIST_X = -10;
	private static final int MISSILE_MIN_DIST_Y = -10;
	private static final int MISSILE_MAX_DIST_X = 10;
	private static final int MISSILE_MAX_DIST_Y = 10;

	private static final int MISSILE_START_Y = 0;
	private static final int MISSILE_START_Y_ENCODED = 0;
	private static final int MISSILE_START_X_COUNT = 6;
	private static final int MISSILE_START_X_MULT = RENDER_WIDTH / MISSILE_START_X_COUNT;
	private static final int MISSILE_START_X_MIDDLE = RENDER_WIDTH / 2;

	
	
	private static final int MISSILE_MOVEMENT_DECODE_SHR = 8;
	private static final int MISSILE_MOVEMENT_ENCODE_SHL = 8;

	private static final int MISSILE_MAX_DX = 1 << MISSILE_MOVEMENT_ENCODE_SHL;
	private static final int MISSILE_MAX_DY = 1 << MISSILE_MOVEMENT_ENCODE_SHL;
	private static final int MISSILE_MIN_DX = 4;
	private static final int MISSILE_MIN_DY = 4;

	private static final int MISSILE_DX_MOD = MISSILE_MAX_DX - MISSILE_MIN_DX;
	private static final int MISSILE_DX_ADD = MISSILE_MIN_DX;
	private static final int MISSILE_DY_MOD = MISSILE_MAX_DY - MISSILE_MIN_DY;
	private static final int MISSILE_DY_ADD = MISSILE_MIN_DY;

	private static final int MISSILE_RANDOM_MULT = 7;

	private static final int MISSILE_HEAD_IMAGE_DATA_START_IPOS = MISSILE_HEAD_IMAGE_SIZE;
	private static final int MISSILE_HEAD_DATA_PATH_START_X_IPOS = MISSILE_HEAD_IMAGE_DATA_START_IPOS;
	private static final int MISSILE_HEAD_DATA_PATH_START_Y_IPOS = MISSILE_HEAD_DATA_PATH_START_X_IPOS + 1;
	private static final int MISSILE_HEAD_DATA_X_IPOS = MISSILE_HEAD_DATA_PATH_START_Y_IPOS + 1;
	private static final int MISSILE_HEAD_DATA_Y_IPOS = MISSILE_HEAD_DATA_X_IPOS + 1;
	private static final int MISSILE_HEAD_DATA_DX_IPOS = MISSILE_HEAD_DATA_Y_IPOS + 1;
	private static final int MISSILE_HEAD_DATA_DY_IPOS = MISSILE_HEAD_DATA_DX_IPOS + 1;

	
	

	private static final int MISSILE_EXPLODE_TYPE_COUNT = 3;

	private static final int MISSILE_EXPLODE_FLAGS = FLAGMSK_CAUSES_DENTS | FLAGMSK_CAUSES_DESTROY;

	private static final int MISSILE_EXPLODE_MID_X = (MISSILE_EXPLODE_WIDTH * GR_BLOCK_WIDTH) / 2;
	private static final int MISSILE_EXPLODE_MID_Y = (MISSILE_EXPLODE_HEIGHT * GR_BLOCK_HEIGHT) / 2;

	private static final int MISSILE_EXPLODE_IMAGE_SIZE = MISSILE_EXPLODE_WIDTH * MISSILE_EXPLODE_HEIGHT;

	private static final int MISSILE_EXPLODE_SWITCH_IMAGE_DURATION = FRAMES_PER_SECOND / 2;

	private static final int MISSILE_EXPLODE_LIFESPAN = MISSILE_EXPLODE_SWITCH_IMAGE_DURATION * MISSILE_EXPLODE_TYPE_COUNT;

	private static final int MISSILE_EXPLODE_IMAGE_DATA_START_IPOS = MISSILE_EXPLODE_IMAGE_SIZE;
	private static final int MISSILE_EXPLODE_DATA_ALIVECOUNT_IPOS = MISSILE_EXPLODE_IMAGE_DATA_START_IPOS;
	private static final int MISSILE_EXPLODE_DATA_BASE_X_IPOS = MISSILE_EXPLODE_DATA_ALIVECOUNT_IPOS + 1;
	private static final int MISSILE_EXPLODE_DATA_BASE_Y_IPOS = MISSILE_EXPLODE_DATA_BASE_X_IPOS + 1;

	private static final int MISSILE_EXPLODE_1_TYPE_COLOR_POS = OBJECT_TYPE_MISSILE_EXPLODE_1_POS + OBJECT_TYPE_RECORD_COLOR_INDEX;
	private static final int MISSILE_EXPLODE_2_TYPE_COLOR_POS = OBJECT_TYPE_MISSILE_EXPLODE_2_POS + OBJECT_TYPE_RECORD_COLOR_INDEX;
	private static final int MISSILE_EXPLODE_3_TYPE_COLOR_POS = OBJECT_TYPE_MISSILE_EXPLODE_3_POS + OBJECT_TYPE_RECORD_COLOR_INDEX;
}
