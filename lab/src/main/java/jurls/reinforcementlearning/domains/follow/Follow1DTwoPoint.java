/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.reinforcementlearning.domains.follow;

import jurls.reinforcementlearning.domains.RLEnvironment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * 2 point controller version of follow1D
 */
public class Follow1DTwoPoint implements RLEnvironment {

    final int numActions = 2;

    

     
    
    double speed = 0.05;
    double targetSpeed = 1;
    double closeThresh = speed * 2; 

    private final int history = 64;


    final int historyPoints = 1; 
    
    final int historyInterval = history / (historyPoints+1); 
    
    @Override
    public int numActions() {
        return numActions;
    }

    private class RenderComponent extends JComponent {

        private final List<Double> _positions = Collections.synchronizedList(new ArrayList<>(history));
        private final List<Double> _targets = Collections.synchronizedList(new ArrayList<>(history));

        @Override
        protected void paintComponent(Graphics g) {
            _positions.clear();
            _positions.addAll(positions);
            
            _targets.clear();
            _targets.addAll(targets);
             
            g.setColor(Color.black);

            g.fillRect(
                    0, 0, getWidth(), getHeight());

            final int margin = 10;

            int prevX = 0;
            int prevY = 0;
            int i = 0;

            g.setColor(Color.green);
            for (double _y : _targets) {
                int x = i * getWidth() / history;
                int y = (int) (_y * (getHeight()-margin) / maxPos) + margin/2;
                g.drawLine(prevX, prevY, x, y);
                ++i;
                prevX = x;
                prevY = y;
            }

            prevX = 0;
            prevY = 0;
            i = 0;

            g.setColor(Color.white);
            for (double _y : _positions) {
                int x = i * getWidth() / history;
                int y = (int) (_y * (getHeight()-margin) / maxPos) + margin/2;
                g.drawLine(prevX, prevY, x, y);
                ++i;
                prevX = x;
                prevY = y;
            }
        }
    }

    private final List<Double> positions = Collections.synchronizedList(new ArrayList<>(history));
    private final List<Double> targets = Collections.synchronizedList(new ArrayList<>(history));
    private final double maxPos = 1.0;
    private double myPos = 0.5;
    private double targetPos = 0.5;
    private double targetV = 0;
    private final RenderComponent renderComponent = new RenderComponent();
    int time = 0;

    double[] observation;
    @Override
    public double[] observe() {
        if (observation == null) {
            observation = new double[historyPoints*2];
        }
        
        double my = 0, target = 0;
        if (positions.isEmpty()) return observation;

        for (int i = 0; i < historyPoints;) {
            int j = positions.size() - 1 - (i * historyInterval);
            my = positions.get(j);
            target = targets.get(j);
            
            





            observation[i++] = 2 * ( target - 0.5 );
            observation[i++] = 2 * ( my - 0.5 );
        }
        return observation;
    }

    double lastDist = Double.NaN;

    @Override
    public double getReward() {
        return getRewardAbsolute();
    }

    public double getRewardDelta() {
        double dist = Math.abs(myPos - targetPos) / maxPos;

        double delta;
        if (!Double.isFinite(lastDist)) {
            delta = 0;
        }
        else {
            delta = dist - lastDist;
        }

        lastDist = dist;

        double reward = -delta * 10;
        return reward;
    }



    @Override
    public float getMaxReward() {
        return (float)closeThresh;
    }

    public double getRewardAbsolute() {

        double dist = Math.abs(myPos - targetPos) / maxPos;
        if (dist < closeThresh) {
            return closeThresh-dist;
        }
        else {
            return -(dist);
        }
    }

    public void updateTarget(int time) {        
        
        updateTargetXOR(time);
        
    }

            
    public void updateTargetRandom(int cycle) {        
        final double targetAcceleration = 0.002;
        targetPos += targetV * speed;
        targetV += (Math.random() - 0.5) * targetAcceleration;        
    }
    public void updateTargetXOR(int cycle) {        
        int complexity = 10;
        double scale = 1.0;
        double v = ( ((int)(speed * targetSpeed * cycle )%complexity ^ 0xf3f24f)%complexity * scale / complexity);
        targetPos = v;
    }

    public void updateTargetSine(int cycle) {
        double scale = 1.0f;
        double v = (0.5f + 0.5f * Math.sin( (speed * cycle / (Math.PI*2)) )) * scale;
        targetPos = v;
    }

    @Override
    public boolean takeAction(int action) {
        int direction = action == 0 ? -1: 1;
        return takeActionVelocity(direction);
    }

    protected boolean takeActionVelocity(int direction) {

        double myV;
        if (direction==0) {
            
            
            myV = 0;
        }
        else {
            myV = direction * speed;
        }
        myPos += myV;

        
        return true;

    }

    @Override
    public void frame() {

        if (myPos > maxPos) {
            myPos = maxPos;
            
        }
        if (myPos < 0) {
            myPos = 0;
            
        }


        updateTarget(time);
        if (targetPos > maxPos) {
            targetPos = maxPos;
            targetV = 0;
        }
        if (targetPos < 0) {
            targetPos = 0;
            targetV = 0;
        }

        positions.add(myPos);
        while (positions.size() > history) {
            positions.remove(0);
        }

        targets.add(targetPos);
        while (targets.size() > history) {
            targets.remove(0);
        }
        
        time++;
    }

    @Override
    public JComponent component() {
        return renderComponent;
    }

}
