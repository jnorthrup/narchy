package codi;


import jcog.Util;
import jcog.math.random.XoRoShiRo128PlusRandom;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class CA extends JFrame implements Runnable {

    protected int stepDelayMS = 100;

    protected boolean NoStepDelay = false;
    protected long MaxSteps;
    protected int SpaceSizeX;
    protected int SpaceSizeY;
    protected int SpaceSizeZ; 
    protected int SpaceSize;
    protected final int Offset = 10; 

    protected int CASpace[][];
    protected int CASpaceOld[][];

    protected long CountCAStps;
    protected int CellSizeX;
    protected int CellSizeY;
    protected int CAFrameSizeX;
    protected int CAFrameSizeY;
    protected Image offImage;
    protected Graphics offGraphics;
    protected Dimension offDimension;
    protected boolean CLRGraphicsAfterStep;
    final protected Random random = new XoRoShiRo128PlusRandom(1);
    protected boolean bFirstStart;
    protected boolean bInitedNew;


    final static Color foreground = Color.WHITE; 
    final static Color background = Color.BLACK; 
    private final boolean running;

    public CA() {


        running = true;
        MaxSteps = 10;
        SpaceSizeX = 30;
        SpaceSizeY = 30;
        SpaceSizeZ = 1;
        SpaceSize = SpaceSizeX * SpaceSizeY * SpaceSizeZ;
        CASpace = new int[SpaceSizeX][SpaceSizeY];
        CASpaceOld = new int[SpaceSizeX][SpaceSizeY];
        CountCAStps = 0;
        CellSizeX = 2;
        CellSizeY = 2;
        CAFrameSizeX = SpaceSizeX * CellSizeX;
        CAFrameSizeY = SpaceSizeY * CellSizeY;
        bFirstStart = true;
        CLRGraphicsAfterStep = true;
    }

    public void init() {

        setIgnoreRepaint(true);

        
        setBackground(background);
        setForeground(foreground);
        
        InitCA();
        bInitedNew = true;
        repaint(); 
    }










    @Override
    public void run() {
        
        
        
        
        
        long startTime = System.currentTimeMillis();
        

        
        while (running) {
            repaint(); 
            
            if (!NoStepDelay) {
                startTime += stepDelayMS;
                Util.sleepMS(Math.max(0, startTime - System.currentTimeMillis()));
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        update(g);
    }

    @Override
    public void update(Graphics g) {
        

         StepCA();

        
        Dimension d = size();
        if ((offGraphics == null)
                || (d.width != offDimension.width)
                || (d.height != offDimension.height)) {
            offDimension = d;
            offImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
            offImage.setAccelerationPriority(1f);
            offGraphics = offImage.getGraphics();
        }
        
        if (CLRGraphicsAfterStep || bFirstStart) {
            offGraphics.setColor(getBackground());
            offGraphics.fillRect(0, 0, d.width, d.height);
        } else { 
            offGraphics.setColor(getBackground());
            offGraphics.fillRect(Offset, Offset + (SpaceSizeY + 1) * CellSizeY,
                    d.width, d.height);
        }
        
        DrawCA(offGraphics);
        offGraphics.setColor(getForeground());
        offGraphics.drawString("CA-Step: " + CountCAStps,
                Offset, Offset + 10 + (SpaceSizeY + 1) * CellSizeY);
        
        g.drawImage(offImage, 0, 0, this);
    }

    protected void InitCA() {
        CountCAStps = 0;
        for (int i = 0; i < SpaceSizeX; i++)
            for (int ii = 0; ii < SpaceSizeY; ii++) {
                CASpace[i][ii] = (random.nextInt() % 2) * (random.nextInt() % 2);
            }
        

        for (int ix = 0; ix < SpaceSizeX; ix++)
            System.arraycopy(CASpace[ix], 0, CASpaceOld[ix], 0, SpaceSizeY);

    }

    protected void StepCA() {
        CountCAStps++;
        
        
        long CountZeroCells = 0;
        int[][] c = CASpaceOld;
        for (int i = 0; i < SpaceSizeX; i++) {
            for (int ii = 0; ii < SpaceSizeY; ii++) {
                if (c[i][ii] == 0) {
                    CountZeroCells++;
                    int iNeighbourSum = (c[(i + SpaceSizeX - 1) % SpaceSizeX]
                            [(ii + SpaceSizeY + 1) % SpaceSizeY] & 1)
                            + (c[(i + SpaceSizeX - 1) % SpaceSizeX]
                            [(ii + SpaceSizeY + 0) % SpaceSizeY] & 1)
                            + (c[(i + SpaceSizeX - 1) % SpaceSizeX]
                            [(ii + SpaceSizeY - 1) % SpaceSizeY] & 1)
                            + (c[(i + SpaceSizeX + 0) % SpaceSizeX]
                            [(ii + SpaceSizeY + 1) % SpaceSizeY] & 1)
                            + (c[(i + SpaceSizeX + 0) % SpaceSizeX]
                            [(ii + SpaceSizeY - 1) % SpaceSizeY] & 1)
                            + (c[(i + SpaceSizeX + 1) % SpaceSizeX]
                            [(ii + SpaceSizeY + 1) % SpaceSizeY] & 1)
                            + (c[(i + SpaceSizeX + 1) % SpaceSizeX]
                            [(ii + SpaceSizeY + 0) % SpaceSizeY] & 1)
                            + (c[(i + SpaceSizeX + 1) % SpaceSizeX]
                            [(ii + SpaceSizeY - 1) % SpaceSizeY] & 1);
                    if (iNeighbourSum >= 2) CASpace[i][ii] = 1;
                } else {
                    CASpace[i][ii] = (c[i][ii] * 2) % 4;
                }
            }
        }

        if ((CountZeroCells == SpaceSize) || (CountCAStps > MaxSteps))
            InitCA();

        
        for (int ix = 0; ix < SpaceSizeX; ix++)
            System.arraycopy(CASpace[ix], 0, c[ix], 0, SpaceSizeY);

    }

    protected void DrawCAFrame(Graphics g) {







    }

    protected void DrawCA(Graphics g) {
        DrawCAFrame(g);
        
        int PosX = Offset - CellSizeX;
        for (int i = 0; i < SpaceSizeX; i++) {
            PosX += CellSizeX;
            int PosY = Offset - CellSizeY;
            for (int ii = 0; ii < SpaceSizeY; ii++) {
                PosY += CellSizeY;
                
                if (CASpace[i][ii] > 0) {
                    Color c;
                    switch (CASpace[i][ii]) {
                        
                        case 1:
                            c = (Color.black);
                            break;
                        case 2:
                            c = (Color.blue);
                            break;
                        case 4:
                            c = (Color.CYAN); 
                            break;
                        case 8:
                            
                            c = Color.MAGENTA;
                            break;
                        default:
                            c = Color.DARK_GRAY;
                            break;
                    }
                    g.setColor(c);
                    g.fillRect(PosX, PosY, CellSizeX, CellSizeY);
                }
            }
        }
    }


}
