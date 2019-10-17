package nars.experiment.checkers;

/**
 * @author Arjen Hoogesteger
 * @version 0.1
 */
@FunctionalInterface
public interface PlayerListener {
    void finishedTurn(Player p);
}
