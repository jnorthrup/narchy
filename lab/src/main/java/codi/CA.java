package codi;


import jcog.random.XoRoShiRo128PlusRandom;

import java.util.Random;

public abstract class CA {


    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final int spaceVolume;

//    protected final int[][] CASpace;
//    protected final int[][] CASpaceOld;

    protected long step;

//    protected Image offImage;
//    protected Graphics offGraphics;
//    protected Dimension offDimension;

    protected final Random random = new XoRoShiRo128PlusRandom(1);
    protected boolean uninitialized;


    public CA(int w, int h, int d) {



        sizeX = w;
        sizeY = h;
        sizeZ = d;
        spaceVolume = sizeX * sizeY * sizeZ;

//        CASpace = new int[sizeX][sizeY];
//        CASpaceOld = new int[sizeX][sizeY];
        step = 0;

        uninitialized = true;
    }

//    public void init() {
//
//        setIgnoreRepaint(true);
//
//
//        setBackground(background);
//        setForeground(foreground);
//
//        InitCA();
//        bInitedNew = true;
//        repaint();
//    }









//
//    @Override
//    public void run() {
//
//
//
//
//
//        long startTime = System.currentTimeMillis();
//
//
//
//        while (running) {
//            repaint();
//
//            if (stepDelayMS > 0) {
//                startTime += stepDelayMS;
//                Util.sleepMS(Math.max(0, startTime - System.currentTimeMillis()));
//            }
//        }
//    }

//    @Override
//    public void paint(Graphics g) {
//        update(g);
//    }
//
//    @Override
//    public void update(Graphics g) {
//
//
//         StepCA();
//
//
//        Dimension d = size();
//        if ((offGraphics == null)
//                || (d.width != offDimension.width)
//                || (d.height != offDimension.height)) {
//            offDimension = d;
//            offImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
//            offImage.setAccelerationPriority(1);
//            offGraphics = offImage.getGraphics();
//        }
//
//        if (CLRGraphicsAfterStep || bFirstStart) {
//            offGraphics.setColor(getBackground());
//            offGraphics.fillRect(0, 0, d.width, d.height);
//        } else {
//            offGraphics.setColor(getBackground());
//            offGraphics.fillRect(Offset, Offset + (sizeY + 1) * CellHeightPx,
//                    d.width, d.height);
//        }
//
//        DrawCA(offGraphics);
//        offGraphics.setColor(getForeground());
////        offGraphics.drawString("CA-Step: " + CountCAStps,
////                Offset, Offset + 10 + (sizeY + 1) * CellHeightPx);
//
//        g.drawImage(offImage, 0, 0, this);
//    }

    protected abstract void InitCA();
//    protected void InitCA() {
//        CountCAStps = 0;
//        for (int i = 0; i < sizeX; i++)
//            for (int ii = 0; ii < sizeY; ii++) {
//                CASpace[i][ii] = (random.nextInt() % 2) * (random.nextInt() % 2);
//            }
//
//
//        for (int ix = 0; ix < sizeX; ix++)
//            System.arraycopy(CASpace[ix], 0, CASpaceOld[ix], 0, sizeY);
//
//    }

    protected abstract void next();
//    protected void StepCA() {
//        CountCAStps++;
//
//
//        long CountZeroCells = 0;
//        int[][] c = CASpaceOld;
//        for (int i = 0; i < sizeX; i++) {
//            for (int ii = 0; ii < sizeY; ii++) {
//                if (c[i][ii] == 0) {
//                    CountZeroCells++;
//                    int iNeighbourSum = (c[(i + sizeX - 1) % sizeX]
//                            [(ii + sizeY + 1) % sizeY] & 1)
//                            + (c[(i + sizeX - 1) % sizeX]
//                            [(ii + sizeY + 0) % sizeY] & 1)
//                            + (c[(i + sizeX - 1) % sizeX]
//                            [(ii + sizeY - 1) % sizeY] & 1)
//                            + (c[(i + sizeX + 0) % sizeX]
//                            [(ii + sizeY + 1) % sizeY] & 1)
//                            + (c[(i + sizeX + 0) % sizeX]
//                            [(ii + sizeY - 1) % sizeY] & 1)
//                            + (c[(i + sizeX + 1) % sizeX]
//                            [(ii + sizeY + 1) % sizeY] & 1)
//                            + (c[(i + sizeX + 1) % sizeX]
//                            [(ii + sizeY + 0) % sizeY] & 1)
//                            + (c[(i + sizeX + 1) % sizeX]
//                            [(ii + sizeY - 1) % sizeY] & 1);
//                    if (iNeighbourSum >= 2) CASpace[i][ii] = 1;
//                } else {
//                    CASpace[i][ii] = (c[i][ii] * 2) % 4;
//                }
//            }
//        }
//
//        if ((CountZeroCells == spaceVolume) || (CountCAStps > MaxSteps))
//            InitCA();
//
//
//        for (int ix = 0; ix < sizeX; ix++)
//            System.arraycopy(CASpace[ix], 0, c[ix], 0, sizeY);
//
//    }
//    abstract protected void DrawCA(Graphics g);
//    protected void DrawCA(Graphics g) {
//
//        int PosX = Offset - CellWidthPx;
//        for (int i = 0; i < sizeX; i++) {
//            PosX += CellWidthPx;
//            int PosY = Offset - CellHeightPx;
//            for (int ii = 0; ii < sizeY; ii++) {
//                PosY += CellHeightPx;
//
//                if (CASpace[i][ii] > 0) {
//                    Color c;
//                    switch (CASpace[i][ii]) {
//
//                        case 1:
//                            c = (Color.black);
//                            break;
//                        case 2:
//                            c = (Color.blue);
//                            break;
//                        case 4:
//                            c = (Color.CYAN);
//                            break;
//                        case 8:
//
//                            c = Color.MAGENTA;
//                            break;
//                        default:
//                            c = Color.DARK_GRAY;
//                            break;
//                    }
//                    g.setColor(c);
//                    g.fillRect(PosX, PosY, CellWidthPx, CellHeightPx);
//                }
//            }
//        }
//    }


}
