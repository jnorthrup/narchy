//package spacegraph.widget.meta;
//
//import com.jogamp.opengl.GL2;
//import spacegraph.Surface;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
///** TODO partially ported from FailChamber */
//public class ParticleSystem  {
//
//    public static class Particle {
//
//        //seems to be {screen_foreground, particle trail, particle trail, ... , particle center }
//        public final static int[] FIRE_GRAD = {0x00000000, 0xaf9f1604, 0xffdf3509, 0xffef6a10, 0xfffc9b11, 0xffffaa22, 0xffffbb33, 0xffffdd66, 0xffffffaa, 0xffffffff};
//        public final static int[] SMOKE_GRAD = {0x00000000, 0x8f666666, 0x8f888888, 0x8f888888, 0x8f666666};
//        public final static int[] WATER_GRAD = {0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0x8f1144dd};
//        public final static int[] SAND_GRAD = {0x00000000, 0x8f666666, 0x8f888888, 0x8f888888, 0xfffc9b11};
//        public final static int[] EXP_GRAD = {0x00000000, 0xff0000ff, 0xffdf3509, 0xffef6a10, 0xfffc9b11, 0xffffaa22, 0xffffbb33, 0xffffdd66, 0xffffffaa, 0xffffffff};
//        public float xPos, yPos, xVel, yVel;
//        public int size;
//        public float pxVel, pyVel;
//        public int age = 0;
//        public int[] gradient;
//        public int rgba;
//
//        //0xff0000ff
//        /*I believe initializing the gradient within the particle class will more easily allow us to use
//         * more than one color of particle
//         */
//        public Particle(float xPos, float yPos, float xVel, float yVel) {
//            this.xPos = xPos;
//            this.yPos = yPos;
//            this.xVel = xVel;
//            this.yVel = yVel;
//        }
//
//        public Particle(int[] gradient, int size, float xPos, float yPos, float xVel, float yVel) {
//            this.size = size;
//            this.gradient = gradient;
//            this.xPos = xPos;
//            this.yPos = yPos;
//            this.xVel = xVel;
//            this.yVel = yVel;
//        }
//
//        public Particle() {
//            this.xPos = 0;
//            this.yPos = 0;
//            this.xVel = 0;
//            this.yVel = 0;
//        }
//
//        public void setParticle(float xPos, float yPos, float xVel, float yVel) {
//
//            this.xPos = xPos;
//            this.yPos = yPos;
//            this.xVel = xVel;
//            this.yVel = yVel;
//
//        }
//
//
//        public int[] getGradient() {
//            return gradient;
//        }
//
//
//    }
//
//    public static class Explosion extends Surface /*extends Geometric.Circle implements Collides*/ {
//
//
////        private final double minRadius;
////        private final double maxRadius;
////        final float tLife;
////        float tRemain;
////
////        public Explosion(double mass, double duration, double minRadius, double maxRadius) {
////            super(minRadius);
////
////            mass(mass);
////
////            tRemain = tLife = (float)duration;
////
////            this.minRadius = minRadius;
////            this.maxRadius = maxRadius;
////            r = 255f;
////            g = 127f;
////            b = 10f;
////            a = 127f;
////        }
////
////
////        @Override
////        public void update(View v, double dt) {
////
////            tRemain -= dt;
////
////            if (tRemain <= 0) {
////                v.space.remove(this);
////            } else {
////                float p = (1f - tRemain/tLife);
////                double nextRad = Util.lerp(p, maxRadius, minRadius);
////                geom().setRadius(nextRad);
////                r = (float)Math.random() * 50 + 200;
////                g = (float)Math.random() * 30 + 180;
////                a = 127 * (1f-p);
////            }
////        }
////
////        @Override
////        public boolean collide(@NotNull Body them, Space where, @NotNull Body me) {
////            if (them instanceof Explosion)
////                return false; //explosions dont collide with each other
////            if (them instanceof Burnable) {
////                double damage = (tRemain / tLife);
////                if (damage > 0.01f)
////                    ((Burnable)them).burn((float)(mass() * damage));
////            }
////            return true;
////        }
//
//        @Override
//        protected void paint(GL2 gl, int dtMS) {
//
//        }
//    }
//
//    public static class Graviton {
//
//        public float xPos = 0, yPos = 0, xPull = 0, yPull = 0;
//
//        public Graviton() {
//            this.xPos = 0;
//            this.yPos = 0;
//            this.xPull = 0;
//            this.yPull = 0;
//        }
//
//        public void setGraviton(float xPos, float yPos, float xPull, float yPull) {
//            this.xPos = xPos;
//            this.yPos = yPos;
//            this.xPull = xPull;
//            this.yPull = yPull;
//        }
//
//        public void setxPos(float xp) {
//            xPos = xp;
//        }
//
//        public void setyPos(float yp) {
//            yPos = yp;
//        }
//
//        public void setxPull(float xp) {
//            xPull = xp;
//        }
//
//        public void setyPull(float yp) {
//            yPull = yp;
//        }
//
//
//    }
//
//
//    public final List<Particle> p = new ArrayList<>();
//    final Random r = new Random();
//    final List<Graviton> gravitonAL = new ArrayList<>();
//    private final boolean pause = false;
//    private final Cell[][] cells;
//    //    private final Cell[][] blockArray;
//    private final int WIDTH;
//    private final int HEIGHT;
//    //    private final Hauto cells;
//    float lightDecayFactor = .9f;
//    float velocityDecayFactor = .97f;
//    float oldX, oldY;
//    private long lastTime;
//    private boolean emit = false;
//
//    static class Cell {
//        float light;
//        int density;
//
//    }
//
////    @Override
////    protected void paint(GL2 gl, int dtMS) {
////        tick();
////        for (Particle p : p) {
////            //fill(p.rgba);
////            gl.glColor4f(1,1,1,1); //HACK TODO color
////            Draw.rect(gl, p.xPos, p.yPos, 0.1f, 0.1f);
////        }
////    }
//
//    public ParticleSystem(int bw, int bh) {
////        int w = p.getWidth();
////        int h = p.getHeight();
//
//
//        gravitonAL.add(new Graviton());
//
////        this.cells = p;
////        this.blockArray = p.read;
//
//
//        WIDTH = bw;
//        HEIGHT = bh;
//        cells = new Cell[WIDTH][HEIGHT];
//
//
//    }
//
//    private static float InvSqrt(float x) {
//        return Float.intBitsToFloat(0x5f3759d5 - (Float.floatToIntBits(x) >> 1));
//    }
//
//    public static int additiveColor(int c1, int c2) {
//        int red = (c1 & 0x00ff0000) + (c2 & 0x00ff0000);
//        int grn = (c1 & 0x0000ff00) + (c2 & 0x0000ff00);
//        int blu = (c1 & 0x000000ff) + (c2 & 0x000000ff);
//        return 0xff000000 + (red > 0x00ff0000 ? 0x00ff0000 : red) + (grn > 0x0000ff00 ? 0x0000ff00 : grn) + (blu > 0x000000ff ? 0x000000ff : blu);
//    }
//
//    public static int makeIntColor(int A, int R, int G, int B) {
//        return (A > 255 ? 0xff000000 : A << 24) + (R > 255 ? 0xff0000 : R << 16) + (G > 255 ? 0xff00 : G << 8) + (B > 255 ? 0xff : B);
//    }
//
////    public void glow(int light, int x, int y) {
////
////        int lightCutoff = 90;
////        int lightCap = 2048;
////
////        if (light > lightCap) {
////            light = lightCap;
////        }
////
////        if (!blockArray[x][y].solid) {
////
////            if (blockArray[x][y].state.light <= light) {
////                blockArray[x][y].state.light = light;
////            }
////
////            if (light <= lightCutoff || x < 1 || x >= (WIDTH) || y < 1 || y >= (HEIGHT)/*if light <= 1 || location invalid*/) {
////                //base case
////                //do nothing
////            } else {
////
////                float lightAttenuation = .8f;
////
////                if (light > lightCutoff + blockArray[x + 1][y].state.light) {
////                    glow((int) (light * lightAttenuation), x + 1, y);
////                }
////
////                if (light > lightCutoff + blockArray[x - 1][y].state.light) {
////                    glow((int) (light * lightAttenuation), x - 1, y);
////                }
////
////                if (light > lightCutoff + blockArray[x][y + 1].state.light) {
////                    glow((int) (light * lightAttenuation), x, y + 1);
////                }
////
////                if (light > lightCutoff + blockArray[x][y - 1].state.light) {
////                    glow((int) (light * lightAttenuation), x, y - 1);
////                }
////
////            }
////        } else {
////            if (light <= lightCutoff || x <= 0 || x >= (WIDTH) || y <= 0 || y >= (HEIGHT)) {
////                //base case
////                //do nothing
////            } else {
////
////                float lightAttenuation = .3f;
////
////                if (light > lightCutoff + blockArray[x + 1][y].state.light) {
////                    glow((int) (light * lightAttenuation), x + 1, y);
////                }
////
////                if (light > lightCutoff + blockArray[x - 1][y].state.light) {
////                    glow((int) (light * lightAttenuation), x - 1, y);
////                }
////
////                if (light > lightCutoff + blockArray[x][y + 1].state.light) {
////                    glow((int) (light * lightAttenuation), x, y + 1);
////                }
////
////                if (light > lightCutoff + blockArray[x][y - 1].state.light) {
////                    glow((int) (light * lightAttenuation), x, y - 1);
////                }
////            }
////        }
////
////    }
////
////    public void emitParticles(float particleSpeed, float spread, float heading, float angle, float px, float py, int numberSquare) {
////
////        for (int x = 0; x <= numberSquare; x++) {
////
////            float xPos = (px + spread * (x - numberSquare / 2));
////            int ix = (int) ((xPos));
////
////            for (int y = 0; y <= numberSquare; y++) {
////
////                Particle p = new Particle();
////
////                p.rgba = Color.WHITE.getRGB();
////                float yPos = (py + spread * (y - numberSquare / 2));
////
////                float Vel = r.nextFloat() * particleSpeed;
////
////                double a = heading + angle * r.nextDouble();
////
////                float xVel = Vel * (float) Math.cos(a);
////                float yVel = Vel * (float) Math.sin(a);
////
////                p.pxVel = xVel;
////                p.pyVel = yVel;
////
////                p.age = 20000;
////
////                p.setParticle(xPos, yPos, xVel, yVel);
////                int iy = (int) ((yPos));
////                if (!blockArray[ix][iy].solid) { // if in air then add particle
////                    if (xPos < WIDTH && xPos > 0 && yPos < HEIGHT && yPos > 0) {
////                        cells[ix][iy] += 2;
////                        this.p.add(p);
////                    }
////                }
////
////            }
////        }
////    }
////
////
//////    public void paintBlock(int x, int y) {
//////        int light = (int) (.1 * blockArray[x][y].state.light);
//////        if (light > 200) {
//////            light = 200;
//////        }
//////        if (light < 0) {
//////            light = 0;
//////        }
//////
//////        if (blockArray[x][y].type == 0) {
//////            for (int x_I = 0; x_I < 8; x_I++) {
//////                for (int y_I = 0; y_I < 8; y_I++) {
//////
//////                    blockRaster[x * 8 + x_I + (y * 8 + y_I) * WIDTH / 4] = makeIntColor(255, light, light / 3, 0);
//////
//////                }
//////            }
//////        } else if (blockArray[x][y].type == Block.GRYSTONE) {
//////
//////            float leftBright = blockArray[x - 1][y].state.light + .5f * blockArray[x - 2][y].state.light + .25f * blockArray[x - 3][y].state.light + .125f * blockArray[x - 1][y - 1].state.light + .125f * blockArray[x - 1][y + 1].state.light;
//////            float rightBright = blockArray[x + 1][y].state.light + .5f * blockArray[x + 2][y].state.light + .25f * blockArray[x + 3][y].state.light + .125f * blockArray[x + 1][y - 1].state.light + .125f * blockArray[x + 1][y + 1].state.light;
//////            float upBright = blockArray[x][y - 1].state.light + .5f * blockArray[x][y - 2].state.light + .25f * blockArray[x][y - 3].state.light + .125f * blockArray[x - 1][y - 1].state.light + .125f * blockArray[x + 1][y - 1].state.light;
//////            float downBright = blockArray[x][y + 1].state.light + .5f * blockArray[x][y + 2].state.light + .25f * blockArray[x][y + 3].state.light + .125f * blockArray[x - 1][y + 1].state.light + .125f * blockArray[x - 1][y + 1].state.light;
//////
//////            for (int x_I = 0; x_I < 8; x_I++) {
//////                for (int y_I = 0; y_I < 8; y_I++) {
//////
//////                    int normal = Block.normalIndentMap[y_I][x_I];
//////
//////                    int bright = 0;
//////                    bright += leftBright * ((0xff000000 & normal) >>> 24) / 0xff;
//////                    bright += rightBright * ((0x00ff0000 & normal) >> 16) / 0xff;
//////                    bright += upBright * ((0x0000ff00 & normal) >> 8) / 0xff;
//////                    bright += downBright * (0x000000ff & normal) / 0xff;
//////
//////                    blockRaster[x * 8 + x_I + (y * 8 + y_I) * WIDTH / 4] = additiveColor(Block.GRYSTONEINDENT_TEX[y_I][x_I], makeIntColor(255, bright / 12, bright / 24, bright / 64));
//////                }
//////            }
//////        }
//////    }
////
////    public void tick() {
////
////        float xPos, yPos, xVel, yVel;
////        int life;
////        float ClickToX, ClickToY, InvClickToP;
////        int width = WIDTH;
////        int height = HEIGHT;
////
////
//////        for (int x_I = 0, lightWidth = (WIDTH); x_I < lightWidth; x_I++) {  //Draw previous frame's lighting, then clear lightArray
//////            for (int y_I = 0, lightHeight = (HEIGHT); y_I < lightHeight; y_I++) {
//////                paintBlock(x_I, y_I);
//////            }
//////        }
////
////
////        if (!pause) {
////
////            for (int x_I = 0, lightWidth = (WIDTH); x_I < lightWidth; x_I++) {  //clear lightArray
////                for (int y_I = 0, lightHeight = (HEIGHT); y_I < lightHeight; y_I++) {
////
////                    blockArray[x_I][y_I].state.light *= lightDecayFactor;
////                }
////            }
////
////
////            Iterator<Particle> pp = p.iterator();
////            while (pp.hasNext()) {
////                Particle p = pp.next();
////
////                xPos = p.xPos;
////                yPos = p.yPos;
////
////                float rand = r.nextFloat() * .5f + .5f;
////
////                xVel = p.xVel * (1 - rand) + p.pxVel * rand;
////                yVel = p.yVel * (1 - rand) + p.pyVel * rand;
////
////                life = p.age;
////
////                p.pxVel = xVel;
////                p.pyVel = yVel;
////
////                if (gravitonAL.isEmpty()) { // if not pulling, slow the particle down
////                    xVel = velocityDecayFactor * xVel;
////                    yVel = velocityDecayFactor * yVel;
////                } else {
////
////                    for (Graviton v : gravitonAL) { // for every graviton
////
////                        if (v != null) {
////
////                            ClickToX = v.xPos - xPos;
////                            ClickToY = v.yPos - yPos;
////                            float xPull = v.xPull;
////                            float yPull = v.yPull;
////
////                            InvClickToP = InvSqrt((ClickToX * ClickToX + ClickToY * ClickToY));
////
////                            xVel += xPull * ClickToX * InvClickToP;
////                            yVel += yPull * ClickToY * InvClickToP;
////
////                        }
////                    }
////                }
////
////                if (xPos <= width && xPos >= 0 && yPos <= height && yPos >= 0) {
////                    cells[(int) ((xPos))][(int) ((yPos))] -= 2;
////                }
////
////                if (xPos > 0 && xPos < WIDTH && yPos > 0 && yPos < HEIGHT) { // if visible
////
////                    int tx = (int) ((xPos + xVel));
////                    int ty = (int) ((yPos + yVel));
////                    tx = Math.max(0, Math.min(tx, cells.w - 1));
////                    ty = Math.max(0, Math.min(ty, cells.h - 1));
////                    if (!blockArray[tx][ty].solid) { // if no collision
////
////                        xPos += xVel;
////                        yPos += yVel;
////
////                    } else { // if collision
////
////                        if (r.nextFloat() <= .1f) {
////                            xPos += .5 * xVel;
////                            yPos += .5 * yVel;
////                        }
////
////                        float Vel = (float) Math.sqrt(xVel * xVel + yVel * yVel);
////
////                        if (Vel < 1) {
////                            life -= 10000;
////                        }
////
////                        if (blockArray[(int) (xPos)][ty].solid) {
////                            xVel = xVel > 0 ? Vel : -Vel;
////                            yVel = r.nextFloat() - .5f;
////                        } else {
////                            yPos += yVel;
////                        }
////
////                        if (blockArray[tx][(int) (yPos)].solid) {
////                            yVel = yVel > 0 ? Vel : -Vel;
////                            xVel = r.nextFloat() - .5f;
////                        } else {
////                            xPos += xVel;
////                        }
////                    }
////                } else { // if not visible
////                    xPos += xVel;
////                    yPos += yVel;
////                }
////
////                life -= 1;
////                final float axVel = Math.abs(xVel);
////                final float ayVel = Math.abs(yVel);
////
////                if ((axVel + ayVel) <= .3) {
////                    life -= 10 / (axVel + ayVel);
////                }
////
////                if (xPos < width && xPos >= 0 && yPos < height && yPos >= 0) { // in canvas
////
////                    /*
////                    for (int xi = -2; xi < 2; xi++) {
////                        float xoff = xPos + xi + width;
////                        for (int yi = -2; yi < 2; yi++) {
////                            particleRaster[(int) (xoff * (int) (yPos + yi))] = additiveColor(particleRaster[(int) (xoff * (int) (yPos + yi))], 0xff9f1604); // opaque, with blending
////                        }
////                    }
////                    */
////
////
////                    if (cells[(int) ((xPos))][(int) ((yPos))] < 400) {
////                        life -= 40 - cells[(int) ((xPos))][(int) ((yPos))];
////                    }
////
////
////                    if (life < 0) {
////                        pp.remove();
////                    } else {
////                        cells[(int) ((xPos))][(int) ((yPos))] += 2;
////                    }
////
////                } else { // outside of canvas
////                    life -= 20;
////                    if (life < 0) {
////                        pp.remove();
////                    }
////                }
////
////                p.setParticle(xPos, yPos, xVel, yVel);
////                p.age = life;
////
////            }
////        }
////
////        for (int x_I = 0, lightWidth = (WIDTH); x_I < lightWidth; x_I++) {
////            for (int y_I = 0, lightHeight = (HEIGHT); y_I < lightHeight; y_I++) {
////                glow(cells[x_I][y_I], x_I, y_I);
////            }
////        }
////
////    }
//
//    /*
//    public void paint(Graphics g) {
//        super.paintComponent(g);
//
//        g.drawImage(blockImage, 0, 0, 2 * WIDTH, 2 * HEIGHT, null);
//
//        g.drawImage(particleImage, 0, 0, 2 * WIDTH, 2 * HEIGHT, null);
//
//        g.setColor(Color.WHITE);
//        g.drawString("Framerate:" + (1000 / (System.currentTimeMillis() - lastTime)), 5, 15);
//        g.drawString("Particles : " + p.size(), 5, 28);
//        g.drawString("Gravity Well : " + gravitonAL.size(), 5, 41);
//
//        for (int gi = 0, gAL = gravitonAL.size(); gi < gAL; gi++) {
//            Graviton v = gravitonAL.get(gi);
//            g.fillRect((int) v.xPos * 2, 0, 2, 16);
//            g.fillRect(0, (int) v.yPos * 2, 16, 2);
//        }
//
//        lastTime = System.currentTimeMillis();
//    }
//    */
//
//    /*
//    @Override
//    public void mouseClicked(MouseEvent me) {
//
//        if (SwingUtilities.isRightMouseButton(me)) {
//
//            Block b = new Block();
//            b.setBlock(0, 0, 0, 0);
//            blockArray[(int) (oldX)][(int) (oldY)] = b;
//
//        }
//
//        if (SwingUtilities.isLeftMouseButton(me)) {
//
//            float mouseX = me.getX() / 2;
//            float mouseY = me.getY() / 2;
//
//            Graviton v = new Graviton();
//
//            v.setGraviton(mouseX, mouseY, .5f, .5f);
//            gravitonAL.add(v);
//        }
//
//    }
//
//    @Override
//    public void mousePressed(MouseEvent me) {
//    }
//
//    @Override
//    public void mouseReleased(MouseEvent me) {
//        if (SwingUtilities.isRightMouseButton(me)) {
//
//            emit = false;
//
//        }
//        if (SwingUtilities.isLeftMouseButton(me)) {
//
//            Graviton v = new Graviton();
//
//            gravitonAL.remove(0);
//
//            gravitonAL.add(0, v);
//
//        }
//    }
//
//    @Override
//    public void mouseEntered(MouseEvent me) {
//        requestFocusInWindow();
//    }
//
//    @Override
//    public void mouseExited(MouseEvent me) {
//    }
//
//    @Override
//    public void mouseDragged(MouseEvent me) {
//
//        float mouseX = me.getX() / 2;
//        float mouseY = me.getY() / 2;
//
//        if (SwingUtilities.isRightMouseButton(me)) {
//
//            emit = true;
//
//        }
//
//        if (SwingUtilities.isLeftMouseButton(me)) {
//
//            gravitonAL.remove(0);
//
//            Graviton v = new Graviton();
//
//            v.setGraviton(mouseX, mouseY, 1, 1);
//
//            gravitonAL.add(0, v);
//
//        }
//
//        oldX = mouseX;
//        oldY = mouseY;
//
//    }
//
//    @Override
//    public void mouseMoved(MouseEvent me) {
//
//        oldX = me.getX() / 2;
//        oldY = me.getY() / 2;
//
//    }
//
//    @Override
//    public void keyTyped(KeyEvent ke) {
//
//        int KeyChar = ke.getKeyChar();
//
//        System.out.println(KeyChar);
//
//        if (KeyChar == 27 ESC) {
//            pause = !pause;
//        }
//
//        if (KeyChar == 112 KeyEvent.VK_P) {
//            gravitonAL.clear();
//            gravitonAL.add(new Graviton());
//        }
//
//        if (KeyChar == 99 KeyEvent.VK_C) {
//            p.clear();
//        }
//
//        if (KeyChar == 120 KeyEvent.VK_X) {
//            for (int x_I = 0; x_I < blockArray.length; x_I++) {
//                for (int y_I = 0; y_I < blockArray[1].length; y_I++) {
//                    blockArray[x_I][y_I].setBlock(0, 0, 0, 0);
//                }
//            }
//        }
//
//    }
//
//    @Override
//    public void keyPressed(KeyEvent ke) {
//    }
//
//    @Override
//    public void keyReleased(KeyEvent ke) {
//    }
//*/
//}
