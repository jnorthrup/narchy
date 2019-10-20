package jurls.reinforcementlearning.domains.wander;

import java.awt.*;
import java.util.stream.IntStream;

public class World {
	public static final int SIZE = 300;
	private static final int OBSTACLES_NO = 20;
	private final Player player;
	private final Obstacle[] obstacles;
	private int time;

	public World() {
		obstacles = new Obstacle[OBSTACLES_NO];
		for (int i = 0; i < obstacles.length; i++) {
			obstacles[i] = new Obstacle();
		}
		player = new Player(this);
		player.randomizePosition();
		time=0;
	}

	public void update() {
		time++;
                player.update();
	}

	public Player getPlayer() {
		return player;
	}

	public Obstacle[] getObstacles() {
		return obstacles;
	}
	public static boolean inside(double x, double dMax) {
		return x > -dMax && x < dMax;
	}
	public boolean isCollision() {
		if(!inside(player.x, SIZE-player.r) || !inside(player.y, SIZE-player.r)) {
			return true;
		}
            
		for (int i = 0; i < obstacles.length; i++) {
			if(obstacles[i].circleCollides(player.x, player.y, player.r)) {
				obstacles[i].c = Color.gray;
				return true;
			}
		}
		return false;
	}

	public boolean pointInObstacle(double x, double y) {
		if(!inside(x, SIZE) || !inside(y, SIZE)) {
			return true;
		}
        int bound = obstacles.length;
		for (int i = 0; i < bound; i++) {
			if (obstacles[i].pointCollides(x, y)) {
				return true;
			}
		}
		return false;
	}

	public int getTime() {
		return time;
	}
	
}
