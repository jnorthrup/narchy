package jurls.reinforcementlearning.domains.wander.brain.actions;

import jurls.reinforcementlearning.domains.wander.Player;
import jurls.reinforcementlearning.domains.wander.brain.Action;

public class TurnRight implements Action {
	private static final long serialVersionUID = 1L;
	private final Player player;

	public TurnRight(Player player) {
		this.player = player;
	}

	public void execute() {
		player.turn(Player.TURNING_ANGLE);
	}

}
