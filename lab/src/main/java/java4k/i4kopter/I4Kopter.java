package java4k.i4kopter;

/*
 * i4kopter
 * Copyright (C) 2009 Bjarne Holen
 *
 * This file is part of i4kopter.
 *
 * i4kopter is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * F-Zero 4K is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http:
 *
 */

import java.applet.Applet;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A simple icopter clone.
 *
 * @author bjarneh@ifi.uio.no
 */

public class I4Kopter extends Applet {

    private BufferedImage bufferImage;
    private BufferedImage backgroundImage;
    private BufferedImage helicopterImage1, helicopterImage2;
    private static final int w = 800;
    private static final int h = 400;
    private final Random random = new Random();
    private boolean keyPressed, paused;
    private int heliY, heliX;                       
    private double speed, gravity = 1.6;
    private Point[] obstacles;                      
    private static final int pathHeight = 250;
    private final int backgroundLength = 17*this.w;       
    private static final int widthBar = 20;
    private static final int oheight  = 59;
    private long lasted;                       

    public I4Kopter(){
        super();
    }

    @Override
    public void init(){
        this.setSize(this.w, this.h);
        this.enableEvents(AWTEvent.KEY_EVENT_MASK);
        this.setVisible(true);
    }

    @Override
    public void start(){

        this.heliY = this.h /2;
        this.heliX = this.w /5;

        bufferImage = new BufferedImage(this.w, this.h, BufferedImage.TYPE_INT_RGB);
        backgroundImage = new BufferedImage(backgroundLength, this.h, BufferedImage.TYPE_INT_RGB);

        /* ******* start draw helicopters ********* */

        helicopterImage1 = new BufferedImage(50, 25, BufferedImage.TYPE_INT_ARGB); 
        Graphics2D helicopterBuffer1 = helicopterImage1.createGraphics();

        helicopterBuffer1.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                           RenderingHints.VALUE_ANTIALIAS_ON);
        
        helicopterBuffer1.setColor(new Color(0,0,0,0.0f)); 
        helicopterBuffer1.fillRect(0,0, 50, 25);

        helicopterBuffer1.setColor(Color.WHITE);
        helicopterBuffer1.fillOval(16,10, 21, 12);  
        helicopterBuffer1.fillPolygon(new int[] {2,22,17}, new int[] {10,15,19}, 3); 
        helicopterBuffer1.setColor(new Color(0,0,1,0.55f));
        helicopterBuffer1.fillOval(9,4, 35, 10);    
        helicopterBuffer1.fillOval(0,7, 7,7);       
        helicopterBuffer1.setColor(Color.GREEN);
        helicopterBuffer1.drawLine(22,23,33,23);    

        helicopterImage2 = new BufferedImage(52, 28, BufferedImage.TYPE_INT_ARGB);
        Graphics2D helicopterBuffer2 = helicopterImage2.createGraphics();

        AffineTransform transform = helicopterBuffer2.getTransform();
        
        transform.rotate(Math.toRadians(-12), 26, 14);
        helicopterBuffer2.setTransform(transform);
        helicopterBuffer2.drawImage(helicopterImage1, 2, 2, null);
        helicopterBuffer2.setTransform(helicopterBuffer1.getTransform());
    
        /* ******* end draw helicopters ********* */


        /* ******* start draw background ********* */

        Graphics2D backgroundBuffer = backgroundImage.createGraphics();

        backgroundBuffer.setColor(Color.YELLOW);
        backgroundBuffer.fillRect(0,0, backgroundLength, this.h);

        boolean up = false;
        int currentY = 80;
        int ocounter = 0;
        obstacles = new Point[ backgroundLength / widthBar ];

        
        for(int i =0; i < this.w; i += widthBar){
            obstacles[ocounter++] = new Point(-1,-1);
        }

        
        BufferedImage heading = new BufferedImage(100,50, BufferedImage.TYPE_INT_RGB);
        Graphics2D headingG = heading.createGraphics();
        headingG.setColor(Color.BLACK);
        headingG.fillRect(0,0, 100,50);
        headingG.setColor(Color.WHITE);
        headingG.drawString("I4KOPTER",32,20);
        backgroundBuffer.drawImage(heading,0,0,this.w,this.h,null);
        backgroundBuffer.setColor(Color.BLACK);
        

        for(int i = this.w; i < (backgroundLength - (2*this.w)); i += widthBar){

            currentY += (up)? -10 : 10;

            backgroundBuffer.fillRect(i, currentY, widthBar, pathHeight);
            
            up = random.nextBoolean();

            if(currentY > 110){ up = true; }
            if(currentY < 40 ){ up = false; }

            obstacles[ocounter++] = new Point(currentY, -1);

            
            if(ocounter % 17 == 0){
                int middleOffset = random.nextInt(pathHeight - currentY) + currentY;
                backgroundBuffer.setColor(Color.YELLOW);
                backgroundBuffer.fillRect(i,
                                          middleOffset,
                                          widthBar,
                                          oheight);
                backgroundBuffer.setColor(Color.BLACK);
                obstacles[ocounter - 1].y = middleOffset;
            }
        }

        
        for(int i = (backgroundLength - (2*this.w)); i < backgroundLength; i += widthBar){
            obstacles[ocounter++] = new Point(-1,-1);
        }

        
        heading = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);
        headingG = heading.createGraphics();
        headingG.setColor(Color.BLACK);
        headingG.fillRect(0,0, 200,50);
        headingG.setColor(Color.WHITE);
        headingG.drawString("The end",75,20);
        backgroundBuffer.drawImage(heading, backgroundLength - (2*this.w),0,2*this.w,this.h, null);

        /* ******* end draw background ********* */

        
        while(true){
            speed = 0.0;
            gravity = 1.6;
            heliY = this.h/2;
            lasted = 0;
            loop();
            Graphics g =  this.getGraphics();
            g.setColor(Color.YELLOW);
            g.drawString(" you lasted: "+ lasted /1000+" seconds ", heliX+200, 200);
            try{ Thread.sleep(1400); }catch(Exception e){}
        }
    }

    boolean collides(int bufferOffset, BufferedImage image){

        

        int i = (bufferOffset+heliX) /widthBar + 1;

        
        if(obstacles[i].x == -1 || obstacles[i+1].x == -1){ return false; }

        
        
        if(heliY < obstacles[i].x || heliY < obstacles[i+1].x)
        { return true ;}

        int imageHeight = image.getHeight();
        int imageWidth  = image.getWidth();

        
        if(heliY + imageHeight > obstacles[i].x + pathHeight ||
           heliY + imageHeight > obstacles[i+1].x + pathHeight)
        { return true; }

        
        
        if(obstacles[i].y != -1 || obstacles[i+1].y != -1){
            Rectangle copter = new Rectangle(heliX, heliY, imageWidth, imageHeight);
            Rectangle oRectangle;

            if(obstacles[i].y != -1){
                oRectangle = new Rectangle(heliX, obstacles[i].y, widthBar, oheight);  
                if(copter.intersects(oRectangle)){ return true; }
            }

            if(obstacles[i+1].y != -1){
                oRectangle = new Rectangle(heliX, obstacles[i+1].y, widthBar, oheight);
                return copter.intersects(oRectangle);
            }
        }

        return false;
    }

    void updateHeliY(){
        if(keyPressed){
            speed -= gravity;
        }else{
            speed += (gravity*1.2);
        }
        heliY += speed;
    }

    void loop(){

        BufferedImage tmp;
        Graphics frameGraphics, bufferGraphics;
        int dx = 12, offset = 0, levelLength = this.w*2;
        int maxLen = backgroundLength - this.w;

        
        while(offset < maxLen){

            while(offset <= levelLength){

                if(! paused){

                    bufferGraphics = bufferImage.getGraphics();
                    bufferGraphics.drawImage(backgroundImage,0,0,this.w,this.h,
                                             offset, 0, (offset + this.w),this.h, null);

                    updateHeliY(); 

                    bufferGraphics.setClip(heliX, heliY, 57, 57); 

                    tmp = (keyPressed)? helicopterImage2 : helicopterImage1; 

                    if(collides(offset, tmp)){ 

                        int oldOffset, newOffset;

                        for(int i = 1; i < 1000; i++){
                            oldOffset = (i-1)*2 /2;
                            newOffset = i*2 /2;
                            frameGraphics = this.getGraphics();
                            frameGraphics.setColor(Color.YELLOW);
                            frameGraphics.drawOval(heliX - newOffset, heliY - newOffset, i*2, i*2);
                            frameGraphics.setColor(Color.BLACK);
                            frameGraphics.fillOval(heliX - oldOffset, heliY - oldOffset, (i-1)*2, (i-1)*2);
                            frameGraphics.dispose();
                        }

                        offset = maxLen*2; 

                        continue;  

                    }else{

                        bufferGraphics.drawImage(tmp, heliX, heliY, null);

                    }
                    
                    frameGraphics = this.getGraphics();
                    frameGraphics.drawImage(bufferImage,0,0,this.w,this.h, this);

                    
                    frameGraphics.dispose();

                    if(offset == 0){ paused = true;}

                    offset += dx;
                    lasted += 49; 
                }
                try{
                    Thread.sleep(40);
                }catch(Exception ex){}
                
            }
            
            dx++;
            gravity += .17;
            levelLength += 2*this.w;
        }

    }

    @Override
    public void processKeyEvent(KeyEvent k){

        int keyID = k.getID();

        if(keyID == KeyEvent.KEY_PRESSED){
            keyPressed = true;
            paused=false;
        }else if(keyID == KeyEvent.KEY_RELEASED){
            keyPressed=false;
        }

        
        if(k.getKeyChar() == ' '){
            paused = true;
        }
    }
}