/*
 * Copyright 2012 Jonathan Leahey
 *
 * This file is part of Minicraft
 *
 * Minicraft is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Minicraft is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Minicraft. If not, see http:
 */

package nars.experiment.minicraft.side;

public abstract class Entity implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    protected static final float gravityAcceleration = .03f;
    protected static final float waterAcceleration = .03f;
    protected static final float maxWaterDY = .05f;

    public float x;
    public float y;
    public float dx;
    public float dy;

    protected Sprite sprite;
    protected final boolean gravityApplies;
    protected int widthPX;
    protected int heightPX;

    public Entity(String ref, boolean gravityApplies, float x, float y, int width, int height) {
        if (ref != null) {
            this.sprite = SpriteStore.get().getSprite(ref);
        }
        this.gravityApplies = gravityApplies;
        this.x = x;
        this.y = y;
        this.widthPX = width;
        this.heightPX = height;
        this.dx = this.dy = 0;
    }

    @Override
    protected Entity clone() throws CloneNotSupportedException {
        return (Entity) super.clone();
    }

    public void updatePosition(World world, int tileSize) {
        var pixels = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) * tileSize);

        var favorVertical = (Math.abs(dy) > Math.abs(dx));
        var hitTop = false;
        var hitBottom = false;

        var left = this.getLeft(tileSize);
        var right = this.getRight(tileSize);
        var top = this.getTop(tileSize);
        var bottom = this.getBottom(tileSize);

        var topLeft = true;
        var topRight = true;
        var bottomLeft = true;
        var bottomRight = true;
        var middleLeft = true;
        var middleRight = true;

        var scale = 1.f / pixels;

        if (favorVertical) {
            for (var i = 1; i <= pixels && topLeft && topRight && bottomLeft && bottomRight; i++) {
                top += dy * scale;
                bottom += dy * scale;

                topLeft = world.passable((int) left, (int) top);
                topRight = world.passable((int) right, (int) top);
                bottomLeft = world.passable((int) left, (int) bottom);
                bottomRight = world.passable((int) right, (int) bottom);
                middleLeft = world.passable((int) left, (int) (top + (bottom - top) / 2));
                middleRight = world.passable((int) right, (int) (top + (bottom - top) / 2));

                if (!(topLeft && topRight && bottomLeft && bottomRight && middleLeft && middleRight)) {
                    hitTop |= !topLeft || !topRight;
                    hitBottom |= !bottomLeft || !bottomRight;
                    top -= dy * scale;
                    bottom -= dy * scale;
                }
            }
            for (var i = 1; i <= pixels && topLeft && topRight && bottomLeft && bottomRight; i++) {
                left += dx * scale;
                right += dx * scale;

                topLeft = world.passable((int) left, (int) top);
                topRight = world.passable((int) right, (int) top);
                bottomLeft = world.passable((int) left, (int) bottom);
                bottomRight = world.passable((int) right, (int) bottom);
                middleLeft = world.passable((int) left, (int) (top + (bottom - top) / 2));
                middleRight = world.passable((int) right, (int) (top + (bottom - top) / 2));

                if (!(topLeft && topRight && bottomLeft && bottomRight && middleLeft && middleRight)) {
                    left -= dx * scale;
                    right -= dx * scale;
                }
            }
        } else {
            for (var i = 1; i <= pixels && topLeft && topRight && bottomLeft && bottomRight; i++) {
                left += dx * scale;
                right += dx * scale;

                topLeft = world.passable((int) left, (int) top);
                topRight = world.passable((int) right, (int) top);
                bottomLeft = world.passable((int) left, (int) bottom);
                bottomRight = world.passable((int) right, (int) bottom);
                middleLeft = world.passable((int) left, (int) (top + (bottom - top) / 2));
                middleRight = world.passable((int) right, (int) (top + (bottom - top) / 2));

                if (!(topLeft && topRight && bottomLeft && bottomRight && middleLeft && middleRight)) {
                    left -= dx * scale;
                    right -= dx * scale;
                }
            }
            for (var i = 1; i <= pixels && topLeft && topRight && bottomLeft && bottomRight; i++) {
                top += dy * scale;
                bottom += dy * scale;

                topLeft = world.passable((int) left, (int) top);
                topRight = world.passable((int) right, (int) top);
                bottomLeft = world.passable((int) left, (int) bottom);
                bottomRight = world.passable((int) right, (int) bottom);
                middleLeft = world.passable((int) left, (int) (top + (bottom - top) / 2));
                middleRight = world.passable((int) right, (int) (top + (bottom - top) / 2));

                if (!(topLeft && topRight && bottomLeft && bottomRight && middleLeft && middleRight)) {
                    hitTop |= !topLeft || !topRight;
                    hitBottom |= !bottomLeft || !bottomRight;
                    top -= dy * scale;
                    bottom -= dy * scale;
                }
            }
        }

        if (gravityApplies) {
            if (world.isClimbable((int) left, (int) top)
                    || world.isClimbable((int) right, (int) top)
                    || world.isClimbable((int) left, (int) bottom)
                    || world.isClimbable((int) right, (int) bottom)) {
                dy += waterAcceleration;
                if (dy > 0) {
                    dy = Math.min(maxWaterDY, dy);
                } else {
                    dy = Math.max(-maxWaterDY, dy);
                }
            } else {
                dy += gravityAcceleration;
            }
            if (hitBottom) {


                var dmg = ((int) (114 * dy)) - 60;
                if (dmg > 0) {
                    this.takeDamage(dmg);
                }
                dx *= 0.9;
            }
        }
        if (hitTop) {
            dy = 0.0000001f;
        } else if (hitBottom) {
            dy = 0;
        }

        x = left;
        y = top;
    }

    public float getCenterY(int tileSize) {
        return y + (float) heightPX / (2 * tileSize);
    }

    public float getCenterX(int tileSize) {
        return x + (float) widthPX / (2 * tileSize);
    }

    public float getTop(int tileSize) {
        return y;
    }

    public float getBottom(int tileSize) {
        return (y + (float) (heightPX) / tileSize);
    }

    public float getLeft(int tileSize) {
        return x;
    }

    public float getRight(int tileSize) {
        return x + (float) (widthPX) / tileSize;
    }

    public boolean isInWater(World world, int tileSize) {
        var left = (int) this.getLeft(tileSize);
        var right = (int) this.getRight(tileSize);
        var top = (int) this.getTop(tileSize);
        var bottom = (int) this.getBottom(tileSize);
        return (world.isLiquid(left, top) || world.isLiquid(right, top)
                || world.isLiquid(left, bottom) || world.isLiquid(right, bottom));
    }

    public boolean isHeadUnderWater(World world, int tileSize) {
        var top = (int) this.getTop(tileSize);
        var centerX = (int) this.getCenterX(tileSize);
        return world.isLiquid(centerX, top);
    }

    public boolean isInWaterOrClimbable(World world, int tileSize) {
        var left = (int) this.getLeft(tileSize);
        var right = (int) this.getRight(tileSize);
        var top = (int) this.getTop(tileSize);
        var bottom = (int) this.getBottom(tileSize);
        return (world.isLiquid(left, top) || world.isLiquid(right, top)
                || world.isLiquid(left, bottom) || world.isLiquid(right, bottom)
                || world.isClimbable(left, top) || world.isClimbable(right, top)
                || world.isClimbable(left, bottom) || world.isClimbable(right, bottom));
    }

    public boolean collidesWith(Entity entity, int tileSize) {

        var left1 = this.x;
        var left2 = entity.x;
        var right1 = this.getRight(tileSize);
        var right2 = entity.getRight(tileSize);
        var top1 = this.y;
        var top2 = entity.y;
        var bottom1 = this.getBottom(tileSize);
        var bottom2 = entity.getBottom(tileSize);

        return !(bottom1 < top2 || top1 > bottom2 || right1 < left2 || left1 > right2);
    }

    public boolean inBoundingBox(Int2 pos, int tileSize) {
        var left = (int) this.getLeft(tileSize);
        if (pos.x < left) {
            return false;
        }
        var right = (int) this.getRight(tileSize);
        if (pos.x > right) {
            return false;
        }
        var top = (int) this.getTop(tileSize);
        if (pos.y < top) {
            return false;
        }
        var bottom = (int) this.getBottom(tileSize);
        return pos.y <= bottom;
    }

    public void draw(GraphicsHandler g, float cameraX, float cameraY, int screenWidth,
                     int screenHeight, int tileSize) {
        var pos = StockMethods.computeDrawLocationInPlace(cameraX, cameraY, screenWidth,
                screenHeight, tileSize, x, y);
        if (StockMethods.onScreen) {
            sprite.draw(g, pos.x, pos.y, widthPX, heightPX);
        }
    }


    public void takeDamage(int amount) {
    }

    public void heal(int amount) {
    }
}
