package kashiki.view;

import com.jogamp.opengl.GL2;


public abstract class Base {

  private Position position = new Position();
  private Angle angle = new Angle();
  private Scale scale = new Scale();
  private Color color = new Color();

  public void draw(GL2 gl) {
    preDraw(gl);
    innerDraw(gl);
    postDraw(gl);
  }

  protected abstract void innerDraw(GL2 gl);

  void preDraw(GL2 gl) {
    gl.glPushMatrix();
    position.updateTranslate(gl);
    angle.updateRotate(gl);
    scale.updateScale(gl);
    color.updateColor(gl);
  }

  private static void postDraw(GL2 gl) {
    gl.glPopMatrix();
  }

  boolean isAnimated() {
    return position.isAnimated() && angle.isAnimated() && scale.isAnimated() && color.isAnimated();
  }

  Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public Angle getAngle() {
    return angle;
  }

  public void setAngle(Angle angle) {
    this.angle = angle;
  }

  Scale getScale() {
    return scale;
  }

  public void setScale(Scale scale) {
    this.scale = scale;
  }

  Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

}
