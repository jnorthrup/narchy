package nars.experiment.minicraft.side;


import nars.experiment.minicraft.side.awtgraphics.AwtGraphicsHandler;

public abstract class GraphicsHandler {
    public static final boolean awtMode = true;

    protected static int screenWidth = 640;
    protected static int screenHeight = 480;

    private static GraphicsHandler single;

    public static int getScreenWidth() {
        return screenWidth;
    }

    public static int getScreenHeight() {
        return screenHeight;
    }

    public static GraphicsHandler get() {
        if (single == null) {
            if (awtMode) {
                single = new AwtGraphicsHandler();
            } else {

            }
        }
        return single;
    }

    public abstract void init(SideScrollMinicraft game);

    public abstract void startDrawing();

    public abstract void finishDrawing();

    public abstract void setColor(Color color);

    public abstract void fillRect(int x, int y, int width, int height);

    public abstract void drawString(String string, int x, int y);

    public abstract void fillOval(int x, int y, int width, int height);

    public abstract void drawImage(Sprite sprite, int x, int y);

    public abstract void drawImage(Sprite sprite, int x, int y, Color tint);

    public abstract void drawImage(Sprite sprite, int x, int y, int width, int height);

    public abstract void drawImage(Sprite sprite, int x, int y, int width, int height, Color tint);
}
