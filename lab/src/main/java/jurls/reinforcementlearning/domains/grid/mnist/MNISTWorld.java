/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jurls.reinforcementlearning.domains.grid.mnist;

import jurls.reinforcementlearning.domains.grid.World;

import java.io.IOException;

/**
 *
 * @author me
 */
public class MNISTWorld extends MNIST implements World {

    int currentImage = 0;
    int currentFrame = -1;
    int cycle = 0;
    static final int maxDigit = 2;
    
    public MNISTWorld(String path, int maxImages, int maxDigit) throws IOException {
        super("/home/me/Downloads", maxImages, maxDigit);
    }

    
    @Override
    public String getName() {
        return "MNIST";
    }

    @Override
    public int getNumSensors() {
        return 28*28;
    }

    @Override
    public int getNumActions() {
        return maxDigit+1;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    
    
    MNISTImage i;
    MNISTImage blank = new MNISTImage(28,28);
    
    MNISTImage retina = new MNISTImage(28,28);
    int nextColumn = 0;
    
    @Override
    public double step(double[] action, double[] sensor) {
        
        if (cycle % trainingCyclesPerImage == 0) {
            currentFrame++;
            if (currentFrame % 10 == 0) {
                i = blank;
                if (trainingCyclesPerImage < maxTrainingCyclesPerImage)
                    trainingCyclesPerImage+=5;
            }
            else {
                i = images.get(currentImage++);                
                currentImage %= images.size();
            }
            nextColumn = 0;
        }
        
        /*
        if (nextColumn<28) {
            if (cycle % scrollCycles == 0) {
                retina.scrollRight(i, nextColumn);
                nextColumn++;
            }
        }
        else {
        }
        retina.toArray(sensor, noise);
        */
        i.toArray(sensor, noise);


        double threshold = 0.75;

        int a = -1;
        for (int x = 0; x < action.length; x++) {
            boolean active = (action[x] > threshold);
            if (active) {
                a = x;
            }            
        }
        

        
        double r;
        
        if ((int) i.label == -1) {
            if (a == -1) r = 1.0;
            else r = -1.0;
        }
        else {
            if ((a < 0) || (a > 9)) r = -1.0; 
            else {

                
                

                
                r = 1.0 / (double) (1 + Math.abs(a - (int) i.label));
                
                
                
                
            }
        }
      
        
        System.out.print(cycle + " " + currentFrame + " " + currentImage + " label=" + i.label + ": " + a + " " + r + " [");
        
        
        
        cycle++;
        
        return r;
    }
    
    
    int maxTrainingCyclesPerImage = 256;
    int trainingCyclesPerImage = 1;
    static final double noise = 0.01;
    
    public static void main(String[] args) throws Exception {

        MNISTWorld m = new MNISTWorld("/home/me/Downloads", 800, maxDigit);

















































        
    }

}
