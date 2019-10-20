package nars.experiment.asteroids;

import java.awt.*;

public class Spacecraft extends VectorSprite {
    public Spacecraft() {
        shape = new Polygon();
        shape.addPoint(0, -25);
        shape.addPoint(15, 10);
        shape.addPoint(-15, 10);

        drawShape = new Polygon();
        drawShape.addPoint(0, -25);
        drawShape.addPoint(15, 10);
        drawShape.addPoint(-15, 10);

        xposition = 450.0;
        yposition = 300.0;

        ROTATION = 0.25;
        THRUST = 0.1;

        weaponType = 1;
        spreadModifier = 0.1;

        invincible = true;

        burstCounter = 0;
        bursting = false;

        for (int i = 0; i < 3; i++) {
            for (int n = 0; n < 4; n++) {
                upgrades[i][n] = 0;
            }
        }

        upgrades[0][1] = 0;
        upgrades[0][2] = 0;
        upgrades[0][0] = 0;

        upgrades[1][1] = 0;
        upgrades[1][2] = 0;
        upgrades[1][0] = 0;

        upgrades[2][1] = 0;
        upgrades[2][2] = 0;
        upgrades[2][0] = 0;

        active = true;
    }

    public void accelerate() {
        xspeed += Math.cos(angle - Math.PI / 2.0) * THRUST;
        yspeed += Math.sin(angle - Math.PI / 2.0) * THRUST;

    }

    public void rotateLeft() {
        angle -= ROTATION;
    }

    public void rotateRight() {
        angle += ROTATION;
    }

    public void hit() {
        active = false;
        counter = 0;
    }

    public void reset() {
        xposition = 450.0;
        yposition = 300.0;
        xspeed = (double) 0;
        yspeed = (double) 0;
        angle = (double) 0;
        active = true;
        invincCounter = 0;

    }

    public void changeWeapon() {
        if (weaponSwitched == false) {
            weaponType++;
            weaponSwitched = true;
        }

        if (weaponType > 3) {
            weaponType = 1;
        }

    }

    public void checkWeapon() {


        if (weaponType == 1) {
            fireDelay = Math.max(0, 4 - 4 * upgrades[0][1]);
            damage = (double) (3 + 3 * upgrades[0][2]);
        }

        if (weaponType == 2) {
            fireDelay = 6 - upgrades[1][1];
            damage = (double) (2 + 2 * upgrades[1][2]);
        }

        if (weaponType == 3) {
            fireDelay = 31 - 5 * upgrades[2][1];
            damage = (double) (10 + 8 * upgrades[2][2]);
        }

        upgradeCost[0][1] = 100 * (int) Math.pow(2.0, (double) upgrades[0][1]);
        upgradeCost[0][2] = 100 * (int) Math.pow(2.0, (double) upgrades[0][2]);
        upgradeCost[0][0] = 1000;

        upgradeCost[1][1] = 100 * (int) Math.pow(2.0, (double) upgrades[1][1]);
        upgradeCost[1][2] = 100 * (int) Math.pow(2.0, (double) upgrades[1][2]);
        upgradeCost[1][0] = 1000;

        upgradeCost[2][1] = 100 * (int) Math.pow(2.0, (double) upgrades[2][1]);
        upgradeCost[2][2] = 100 * (int) Math.pow(2.0, (double) upgrades[2][2]);
        upgradeCost[2][0] = 1000;
    }

    public void checkInvinc() {
        if (invincCounter > 200 && invincible == true) {
            invincible = false;
        }
    }

}


