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
 * @author thorsten
 */
public class Follow1D implements RLEnvironment {

    final int numActions = 3;

    

     
    
    final double acceleration = 0.005;
    final double decelerationFactor = 0.25;
    double speed = 0.1;


    private final int history = 64;


    final int historyPoints = 1; 
    
    final int historyInterval = history / (historyPoints+1); 
    
    @Override
    public int numActions() {
        return numActions;
    }

    private class RenderComponent extends JComponent {

        private final List<Double> _positions = Collections.synchronizedList(new ArrayList<Double>(history));
        private final List<Double> _targets = Collections.synchronizedList(new ArrayList<Double>(history));

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

    private final List<Double> positions = Collections.synchronizedList(new ArrayList<Double>(history));
    private final List<Double> targets = Collections.synchronizedList(new ArrayList<Double>(history));
    private final double maxPos = 1.0;
    private double myPos = 0.5;
    private double targetPos = 0.5;
    private double myV = 0;
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

    @Override
    public double getReward() {
        double dist = Math.abs(myPos - targetPos) / maxPos;
        return -(dist*dist)*4;
    }

    public void updateTarget(int time) {        
        updateTargetSine(time);
        
        
    }

            
    public void updateTargetRandom(int cycle) {        
        final double targetAcceleration = 0.002;
        targetPos += targetV * speed;
        targetV += (Math.random() - 0.5) * targetAcceleration;        
    }
    public void updateTargetXOR(int cycle) {        
        int complexity = 10;
        double scale = 1.0;
        double s = 0.25;
        double v = ( ((int)(speed  * s * cycle )%complexity ^ 0xf3f24f)%complexity * scale / complexity);
        targetPos = v;
    }

    public void updateTargetSine(int cycle) {
        double scale = 1.0f;
        double v = (0.5f + 0.5f * Math.sin( (speed * cycle / (Math.PI*2)) )) * scale;
        targetPos = v;
    }

    @Override
    public boolean takeAction(int action) {
        







        takeActionAccelerate(action);
        return true;
    }
    protected void takeActionPosition(int action) {
        myPos = (action / ((double)(numActions-1))) * maxPos;
    }

    protected boolean takeActionVelocity2(int action) {
        if (action == 0) takeActionVelocity3(0);
        else takeActionVelocity3(2);
        return true;
    }

    protected boolean takeActionVelocity3(int action) {
        double a = Math.round(action - (numActions/2d));
        double direction = (a)/(numActions/2d);

        if (direction==0) {
            
            
            myV = 0;
        }
        else {
            myV = direction * acceleration;
        }
        myPos += myV;

        
        return true;

    }
    protected void takeActionAccelerate(int action) {
        double a = Math.round(action - (numActions/2d));
        double direction = (a)/(numActions/2d);

        if (direction==0) {
            
            
            myV = 0;
        }
        else {
            myV += direction * acceleration;
        }
        myPos += myV;

    }

    @Override
    public void frame() {

        if (myPos > maxPos) {
            myPos = maxPos;
            myV = 0;
            
        }
        if (myPos < 0) {
            myPos = 0;
            myV = 0;
            
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
