package nars.experiment.minicraft.top;

import java.applet.Applet;
import java.awt.*;

public class GameApplet extends Applet {
	private static final long serialVersionUID = 1L;

	private final TopDownMinicraft game = new TopDownMinicraft();

	@Override
	public void init() {
		setLayout(new BorderLayout());
		add(game, BorderLayout.CENTER);
	}

	@Override
	public void start() {
		game.start();
	}

	@Override
	public void stop() {
		game.stop();
	}
}