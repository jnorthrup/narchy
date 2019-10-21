package jurls.reinforcementlearning.domains.wander.brain.actions;

import jurls.reinforcementlearning.domains.wander.Player;
import jurls.reinforcementlearning.domains.wander.brain.Action;

public class MoveBackward implements Action {
	private static final long serialVersionUID = 1L;
	private final Player player;

	public MoveBackward(Player player) {
		this.player = player;
	}

	public void execute() {
		player.moveForward(- Player.STEP_SIZE);
	}
}
