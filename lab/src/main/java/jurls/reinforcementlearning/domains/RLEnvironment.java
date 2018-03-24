/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.reinforcementlearning.domains;

import javax.swing.*;
import java.awt.*;

/**
 * Reinforcement Learning interface
 * @author thorsten
 */
public interface RLEnvironment {

    /** current observation */
    public double[] observe();

    /** current reward */
    public double getReward();

    /** set the next action (0 <= action < numActions)
     *  returns false if the action was not successfully applied
     * */
    public boolean takeAction(int action);

    /** advance world simulation by 1 frame */
    public void frame();

    public int numActions();

    default public int numStates() { return observe().length; }


    @Deprecated public Component component();
    @Deprecated default public JFrame newWindow() {
        JFrame j = new JFrame(getClass().toString());
        j.setSize(800,600);
        j.getContentPane().setLayout(new BorderLayout());
        j.getContentPane().add(component(), BorderLayout.CENTER);
        return j;
    }


    default float getMaxReward() { return 1f; }
    default float getMinReward() { return -1f; }



}
