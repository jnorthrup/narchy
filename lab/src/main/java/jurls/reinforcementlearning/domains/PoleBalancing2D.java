/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.reinforcementlearning.domains;

import javax.swing.*;

/**
 *
 * @author thorsten
 */
public class PoleBalancing2D implements RLEnvironment {

    private final Physics2D physics2D;
    private final Point agentPoint;
    private final Point pendulumPoint;
    private final PhysicsRenderer physicsRenderer;

    double speed = 0.4;
    double dt = 0.2;
    double gravity = 0.2;

    int maxX = 800;
    int minAgentX = 50;
    int maxAgentX = 750;
    double poleLength = 175.0;
    double decay = 0.995;
    double decay2 = 0.995;
    double agentY = 300.0;


    public PoleBalancing2D() {
        physics2D = new Physics2D(gravity, 300.0);
        physicsRenderer = new PhysicsRenderer();
        physicsRenderer.physics2D = physics2D;
        agentPoint = new Point((double) (minAgentX + maxAgentX) / 2.0, agentY, (double) 0, (double) 0, decay2, (double) 0);
        pendulumPoint = new Point((double) (minAgentX + maxAgentX) /2.0, agentY-poleLength, (double) 0, (double) 0, decay, decay);
        Connection c = new Connection(poleLength, agentPoint, pendulumPoint);
        physics2D.points.add(agentPoint);
        physics2D.points.add(pendulumPoint);
        physics2D.connections.add(c);
    }

    @Override
    public double[] observe() {
        double[] o = {
            (agentPoint.x)/ (double) (maxX),
            
            (pendulumPoint.x)/ (double) (maxX),
            (pendulumPoint.y - agentPoint.y)/(poleLength*1.1),
            Math.atan2(pendulumPoint.y, pendulumPoint.x),

                
                
            
            
        };
        
        return o;
    }

    @Override
    public double getReward() {

        return (agentPoint.y - pendulumPoint.y)/poleLength;
    }

    @Override
    public boolean takeAction(int action) {
        takeAction3(action);
        return true;
    }






    /* -1, 0, +1 */
    double dvx = (double) 0;

    public void takeAction3(int action) {
        action--;

        dvx += speed * (double) (action);

        
    }

    @Override
    public int numActions() {
        return 3;
    }

    @Override
    public void frame() {

        agentPoint.vx += dvx;
        dvx = (double) 0;

        physics2D.step(dt);
        if (agentPoint.x < (double) minAgentX) {
            agentPoint.x = (double) minAgentX;
            agentPoint.vx = (double) 0;
            
        }
        if (agentPoint.x > (double) maxAgentX) {
            agentPoint.x = (double) maxAgentX;
            agentPoint.vx = (double) 0;
            
        }

    }

    @Override
    public JComponent component() {
        return physicsRenderer;
    }


}
