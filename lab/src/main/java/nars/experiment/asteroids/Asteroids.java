package nars.experiment.asteroids;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Asteroids extends JFrame implements KeyListener, ActionListener {

    public static final int WIDTH = 512;
    public static final int HEIGHT = 512;

    final BufferedImage offscreen;

    Graphics2D offg;
    Spacecraft ship;
    Rock rock;
    ArrayList<Rock> rockList;
    ArrayList<Bullet> bulletList;
    ArrayList<Debris> explosionList;
    Timer timer;
    int shopSelection;
    int level;
    int credits;
    int lives;
    int numAsteroids;
    int numDebris;
    int bulletDeathCounter;

    int starPositionSeed;
    boolean upKey;
    boolean downKey;
    boolean leftKey;
    boolean rightKey;
    boolean spaceKey;
    boolean shiftKey;
    boolean SKey;
    boolean DKey;
    boolean PKey;
    boolean FKey;
    boolean escKey;
    boolean RKey;
    boolean isExplosionShip;
    boolean isMainInstr;
    boolean instrSwitched;
    boolean pauseKeyActivated;
    boolean selectionMoved;
    boolean spaceKeyActivated;
    int gameState;
    DecimalFormat df = new DecimalFormat("#.##");

    private final Color clearColor =


            Color.BLACK;


    public static void main(String[] args) {
        new Asteroids(true);
    }

    public Asteroids(boolean autostart) {
        super();


        setBackground(Color.BLACK);
        setIgnoreRepaint(true);


        offscreen =

                new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        offg = (Graphics2D) offscreen.getGraphics();

        init();

        if (autostart) {
            timer = new Timer(20, this);
            start();
        }
    }

    public void init() {


        this.setSize(WIDTH, HEIGHT);
        setResizable(false);

        this.addKeyListener(this);

        ship = new Spacecraft();

        shopSelection = 0;


        rockList = new ArrayList();
        bulletList = new ArrayList();
        explosionList = new ArrayList();

        numAsteroids = 4;
        numDebris = 20;

        selectionMoved = false;
        spaceKeyActivated = false;

        gameState = 0;


        bulletDeathCounter = 30;

        level = 1;
        credits = 0;
        lives = 3;

        for (int i = 0; i < numAsteroids; i++) {
            rockList.add(new Rock());
        }
        setVisible(true);

    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(offscreen, 0, 0, this);
    }

    @Override
    public void keyPressed(KeyEvent e) {


        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_UP) {
            upKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            downKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            spaceKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_S) {
            SKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_P) {
            PKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shiftKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            escKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_D) {
            DKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_F) {
            FKey = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_R) {
            RKey = true;
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {


        if (e.getKeyCode() == KeyEvent.VK_UP) {
            upKey = false;
            selectionMoved = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            downKey = false;
            selectionMoved = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightKey = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftKey = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            spaceKey = false;
            spaceKeyActivated = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_S) {
            SKey = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_P) {
            PKey = false;
            pauseKeyActivated = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shiftKey = false;
            ship.weaponSwitched = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            escKey = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_D) {
            DKey = false;
            instrSwitched = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_F) {
            FKey = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_R) {
            RKey = false;
        }

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void update(Graphics g) {

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame();

    }

    public float frame() {
        switch (gameState) {
            case 0:
                keyCheck();
                break;
            case 1:

                ship.updatePosition(WIDTH, HEIGHT);
                ship.checkWeapon();
                ship.checkInvinc();
                respawnShip();
                keyCheck();


                for (int i = 0; i < rockList.size(); i++) {
                    rockList.get(i).updatePosition(WIDTH, HEIGHT);
                }

                for (int i = 0; i < bulletList.size(); i++) {
                    bulletList.get(i).updatePosition(WIDTH, HEIGHT);
                    if (bulletList.get(i).counter == bulletDeathCounter) {
                        bulletList.remove(i);
                    }
                }

                for (int i = 0; i < explosionList.size(); i++) {
                    explosionList.get(i).updatePosition(WIDTH, HEIGHT);
                    if (explosionList.get(i).counter == 25) {
                        explosionList.remove(i);
                    }
                }

                checkCollisions();
                checkDestruction();

                break;
            case 2:
                keyCheck();
                ship.checkWeapon();
                break;
            case 3:
                keyCheck();
                break;
            case 4:
                keyCheck();
                break;
        }


        render();

        return (float) credits;
    }

    private void render() {
        switch (gameState) {
            case 0:


                gameState = 1;
                break;
            case 1:

                offg.setColor(clearColor);
                offg.fillRect(0, 0, WIDTH, HEIGHT);

                offg.setColor(Color.WHITE);


                offg.setColor(Color.WHITE);

                for (int i = 0; i < rockList.size(); i++) {
                    Rock rock = rockList.get(i);
                    for (int n = 0; n < 5; n++) {
                        for (int j = 0; j < 5; j++) {
                            offg.drawLine((int) Math.round(((double) rock.shape.xpoints[n] * Math.cos(rock.angle) - (double) rock.shape.ypoints[n] * Math.sin(rock.angle) + rock.xposition)),
                                    (int) Math.round(((double) rock.shape.xpoints[n] * Math.sin(rock.angle) + (double) rock.shape.ypoints[n] * Math.cos(rock.angle) + rock.yposition)),
                                    (int) Math.round(((double) rock.shape.xpoints[j] * Math.cos(rock.angle) - (double) rock.shape.ypoints[j] * Math.sin(rock.angle) + rock.xposition)),
                                    (int) Math.round(((double) rock.shape.xpoints[j] * Math.sin(rock.angle) + (double) rock.shape.ypoints[j] * Math.cos(rock.angle) + rock.yposition)));
                        }
                    }

                    if (rock.active == true) {
                        rock.paint(offg, false);
                    }
                }


                offg.setColor(Color.YELLOW);
                for (int i = 0; i < bulletList.size(); i++) {
                    Bullet bullet = bulletList.get(i);
                    if (bullet.active == true) {
                        bullet.paint(offg, false);
                    }
                }

                drawExplosions();
                try {
                    drawHUD();
                } catch (Exception e) {
                }
                drawShip();

                break;
            case 2:

                newLevel();

                gameState = 1;

                break;
            case 3:

                gameState = 1;

                break;
            case 4:
                offg.setColor(Color.GREEN);
                offg.fillRect(390, 75, 105, 40);

                offg.setColor(Color.BLACK);
                offg.fillRect(395, 80, 95, 30);

                offg.setColor(Color.GREEN);
                offg.drawString("GAME PAUSED", 400, 100);
                break;
        }

        repaint();

    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void keyCheck() {

        if (ship.bursting == true && gameState == 1) {
            fireBullet();
        }

        if (upKey == true) {
            ship.accelerate();
            if (gameState == 2 && selectionMoved == false) {
                shopSelection--;
                selectionMoved = true;
            }
        }

        if (downKey == true && gameState == 2 && selectionMoved == false) {
            shopSelection++;
            selectionMoved = true;
        }

        if (rightKey == true) {
            ship.rotateRight();
        }

        if (leftKey == true) {
            ship.rotateLeft();
        }

        if (spaceKey == true) {
            switch (gameState) {
                case 1:
                    fireBullet();
                    break;
                case 3:
                    gameState = 0;
                    init();
                    break;
            }


        }

        if (shiftKey == true) {
            ship.changeWeapon();
        }

        if (SKey == true) {
            switch (gameState) {
                case 2:
                    gameState = 1;
                    newLevel();
                    break;
                case 0:
                    gameState = 1;
                    break;
            }
        }

        if (escKey == true) {
            System.exit(0);
        }

        if (FKey == true) {
            if (gameState == 1) {
                for (int i = 0; i < rockList.size(); i++) {
                    rockList.get(i).active = false;
                }
            }
        }

        if (PKey == true && pauseKeyActivated == false) {
            switch (gameState) {
                case 1:
                    gameState = 4;
                    pauseKeyActivated = true;
                    break;
                case 4:
                    gameState = 1;
                    pauseKeyActivated = true;
                    break;
            }
        }

        if (gameState == 0 && DKey == true && instrSwitched == false) {
            isMainInstr = !isMainInstr;
            instrSwitched = true;
        }
        
        /* debugging tool so i don't have to actually kill asteroids just to test things
        if (RKey == true && gameState == 1)
        {
            for (Asteroid a : asteroidList)
            {
                a.active = false;
            }
        }*/
    }

    public static boolean collision(VectorSprite object1, VectorSprite object2) {


        int bound1 = object1.drawShape.npoints;
        for (int i1 = 0; i1 < bound1; i1++) {
            if (object2.drawShape.contains(object1.drawShape.xpoints[i1], object1.drawShape.ypoints[i1]) && object1.active && object2.active) {
                return true;
            }
        }
        int bound = object2.drawShape.npoints;
        for (int i = 0; i < bound; i++) {
            if (object1.drawShape.contains(object2.drawShape.xpoints[i], object2.drawShape.ypoints[i]) && object1.active && object2.active) {
                return true;
            }
        }
        return false;

    }

    public void checkCollisions() {
        for (int i = 0; i < rockList.size(); i++) {

            if (collision(ship, rockList.get(i)) == true && ship.invincible == false) {
                ship.hit();
                lives -= 1;
                credits -= 50;
                for (int e = 0; e < 10 * numDebris; e++) {
                    explosionList.add(new Debris(ship.xposition, ship.yposition));
                    isExplosionShip = true;
                }
            }

            for (int j = 0; j < bulletList.size(); j++) {
                if (collision(bulletList.get(j), rockList.get(i)) == true) {
                    bulletList.get(j).active = false;
                    rockList.get(i).health -= ship.damage;

                    if (rockList.get(i).health <= (double) 0) {

                        rockList.get(i).active = false;

                        for (int e = 0; e < numDebris; e++) {
                            explosionList.add(new Debris(rockList.get(i).xposition, rockList.get(i).yposition));
                            isExplosionShip = false;
                        }

                        credits += 10;

                    }
                }
            }
        }
    }

    public void respawnShip() {
        if (ship.active == false && ship.counter >= 50) {
            ship.reset();
            ship.invincible = true;
        }
    }

    public void fireBullet() {
        if (ship.counter > ship.fireDelay && ship.active == true) {
            if (ship.weaponType == 1) {
                switch (ship.upgrades[0][0]) {
                    case 0:
                        if (Math.random() < 0.25)
                            bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0 + ship.spreadModifier, ship.weaponType));
                        if (Math.random() < 0.25)
                            bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0, ship.weaponType));
                        if (Math.random() < 0.25)
                            bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0 - ship.spreadModifier, ship.weaponType));
                        break;
                    case 1:
                        bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0 + 0.5 * ship.spreadModifier, ship.weaponType));
                        bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0 + ship.spreadModifier, ship.weaponType));
                        bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0, ship.weaponType));
                        bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0 - ship.spreadModifier, ship.weaponType));
                        bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0 - 0.5 * ship.spreadModifier, ship.weaponType));
                        break;
                }

                ship.counter = 0;
            }

            if (ship.weaponType == 2) {
                switch (ship.upgrades[1][0]) {
                    case 0:
                        bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0, ship.weaponType));
                        break;
                    case 1:
                        bulletList.add(new Bullet(ship.xposition + 10.0 * Math.cos(ship.angle), ship.yposition + 10.0 * Math.sin(ship.angle), ship.angle - Math.PI / 2.0, ship.weaponType));
                        bulletList.add(new Bullet(ship.xposition - 10.0 * Math.cos(ship.angle), ship.yposition - 10.0 * Math.sin(ship.angle), ship.angle - Math.PI / 2.0, ship.weaponType));
                        break;
                }

                ship.counter = 0;
            }

            if (ship.weaponType == 3) {
                switch (ship.upgrades[2][0]) {
                    case 0:
                        bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0, ship.weaponType));
                        ship.counter = 0;
                        break;
                    case 1:
                        ship.bursting = true;
                        if (ship.burstCounter < 3) {
                            bulletList.add(new Bullet(ship.xposition, ship.yposition, ship.angle - Math.PI / 2.0, ship.weaponType));
                            ship.counter = ship.fireDelay - 2;
                            ship.burstCounter += 1;
                        } else {
                            ship.bursting = false;
                            ship.counter = 0;
                            ship.burstCounter = 0;
                        }
                        break;
                }


            }
        }
    }

    public void checkDestruction() {
        for (int i = 0; i < rockList.size(); i++) {
            if (rockList.get(i).active == false) {
                if (rockList.get(i).size > 4.0) {
                    rockList.add(new Rock(rockList.get(i).xposition, rockList.get(i).yposition, rockList.get(i).size -= 1.0, rockList.get(i).xspeed, rockList.get(i).yspeed));
                    rockList.add(new Rock(rockList.get(i).xposition, rockList.get(i).yposition, rockList.get(i).size -= 1.0, rockList.get(i).xspeed, rockList.get(i).yspeed));
                }
                rockList.remove(i);
            }
        }
    }

    public void drawHUD() {
        offg.setColor(Color.RED);

        if (rockList.isEmpty() == true) {
            endLevel();
        }

        if (lives == 0 && ship.active == true) {
            gameState = 3;
        }

        offg.setColor(Color.CYAN);


    }

    public void drawExplosions() {
        if (isExplosionShip == true) {
            offg.setColor(Color.ORANGE);
        }

        if (isExplosionShip == false) {
            offg.setColor(Color.WHITE);
        }

        for (int i = 0; i < explosionList.size(); i++) {
            explosionList.get(i).paint(offg, false);
        }

    }

    public void drawShip() {

        if (ship.invincible == true && (ship.invincCounter % 10) > 4) {
            offg.setColor(Color.GREEN);
        }

        if (ship.invincible == true && (ship.invincCounter % 10) <= 4) {
            offg.setColor(Color.gray);
        }

        if (ship.invincible == false) {
            offg.setColor(Color.GREEN);
        }

        if (lives == 0) {
            offg.setColor(Color.BLACK);
        }

        if (ship.active == true) {
            ship.paint(offg, true);
        }
    }


    public void drawShop() {

        offg.setColor(Color.BLACK);
        offg.fillRect(0, 0, WIDTH, HEIGHT);

        try {
            drawHUD();
        } catch (Exception e) {
        }

        if (shopSelection > 9) {
            shopSelection = 0;
        } else if (shopSelection < 0) {
            shopSelection = 9;
        }

        offg.setColor(Color.CYAN);
        offg.drawString("Congrats, you completed level " + level + "! Press S to advance to the next level", 300, 100);

        offg.setColor(Color.YELLOW);
        offg.drawString("Use the arrow keys and spacebar to select upgrades. Use the shift key to cycle through weapons and look at stats.", 120, 480);

        offg.drawString("DE-82 DISRUPTOR", 290, 140);
        if (shopSelection == 0) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Rate of Fire upgrades: " + ship.upgrades[0][1] + " - Pay " + ship.upgradeCost[0][1] + " credits to upgrade.", 300, 170);
        if (shopSelection == 1) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Damage upgrades: " + ship.upgrades[0][2] + " - Pay " + ship.upgradeCost[0][2] + " credits to upgrade.", 300, 190);
        if (shopSelection == 2) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Quintuple Shot: " + (ship.upgrades[0][0] == 0 ? "- Pay 1000 credits to upgrade." : "Already upgraded!"), 300, 210);

        offg.setColor(Color.YELLOW);
        offg.drawString("Z-850 VULCAN", 290, 240);
        if (shopSelection == 3) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Rate of Fire upgrades: " + ship.upgrades[1][1] + " - Pay " + ship.upgradeCost[1][1] + " credits to upgrade.", 300, 270);
        if (shopSelection == 4) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Damage upgrades: " + ship.upgrades[1][2] + " - Pay " + ship.upgradeCost[1][2] + " credits to upgrade.", 300, 290);
        if (shopSelection == 5) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Twin Barrels: " + (ship.upgrades[1][0] == 0 ? "- Pay 1000 credits to upgrade." : "Already upgraded!"), 300, 310);

        offg.setColor(Color.YELLOW);
        offg.drawString("C-86 ION CANNON", 290, 340);
        if (shopSelection == 6) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Rate of Fire upgrades: " + ship.upgrades[2][1] + " - Pay " + ship.upgradeCost[2][1] + " credits to upgrade.", 300, 370);
        if (shopSelection == 7) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Damage upgrades: " + ship.upgrades[2][2] + " - Pay " + ship.upgradeCost[2][2] + " credits to upgrade.", 300, 390);
        if (shopSelection == 8) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Piercing Burst: " + (ship.upgrades[2][0] == 0 ? "- Pay 1000 credits to upgrade." : "Already upgraded!"), 300, 410);

        if (shopSelection == 9) {
            offg.setColor(Color.WHITE);
        } else {
            offg.setColor(Color.YELLOW);
        }
        offg.drawString("Pay 100 credits to buy an extra life", 290, 440);
    }

    public void whichBulletType() {

        for (int i = 0; i < bulletList.size(); i++) {
            bulletList.get(i).weaponType = ship.weaponType;
        }
    }

    public void endLevel() {

        for (int i = 0; i < rockList.size(); i++) {
            rockList.get(i).active = false;
        }

        for (int i = 0; i < rockList.size(); i++) {
            rockList.remove(rockList.get(i));
        }

        for (int i = 0; i < explosionList.size(); i++) {
            explosionList.get(i).active = false;
        }

        for (int i = 0; i < explosionList.size(); i++) {
            explosionList.remove(explosionList.get(i));
        }

        for (int i = 0; i < bulletList.size(); i++) {
            bulletList.get(i).active = false;
        }

        for (int i = 0; i < bulletList.size(); i++) {
            bulletList.remove(bulletList.get(i));
        }

        gameState = 2;

    }

    public void newLevel() {
        level++;
        lives += Math.round((float) (level / 10)) + 1;

        ship.invincible = true;

        ship.reset();

        numAsteroids = 2 + (level * 2);

        for (int i = 0; i < numAsteroids; i++) {
            rockList.add(new Rock());
        }
    }


}
