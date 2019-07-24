package spacegraph.space3d.raytrace;

import jcog.math.vv3;

import static jcog.math.vv3.Z_AXIS;

public final class Camera extends Ray3 {
    private double fov;
    private double size;

    public Camera(vv3 position, vv3 direction, double fov, double size) {
        super(position, direction.normalize());
        this.fov = fov;
        this.size = size;
    }

    
    public Ray3 ray(double x, double y, double aspectRatio) {
        vv3 xAxis = direction.cross(vv3.Z_AXIS).normalizeThis();
        vv3 yAxis = xAxis.cross(direction);

        double widthNear = size;
        double heightNear = widthNear / aspectRatio;

        double widthFar = 2 * Math.tan(fov / 2 / 180 * Math.PI) + widthNear;
        double heightFar = widthFar / aspectRatio;

        vv3 originNear = position.
            minus(xAxis.scale(widthNear / 2)).
            minus(yAxis.scale(heightNear / 2));
        vv3 originFar = direction.
                add(position).
            minus(xAxis.scale(widthFar / 2)).
            minus(yAxis.scale(heightFar / 2));

        vv3 pointNear = originNear.
                add(xAxis.scale(x * widthNear)).
                add(yAxis.scale(y * heightNear));
        vv3 pointFar = originFar.
                add(xAxis.scale(x * widthFar)).
                add(yAxis.scale(y * heightFar));

        return new Ray3(pointNear, pointFar.minus(pointNear).normalizeThis());
    }

    public void move(vv3 keyboardVector) {
        position.addThis(direction.scale(keyboardVector.y));
        position.addThis(direction.cross(vv3.Z_AXIS).normalize().scale(keyboardVector.x));
    }

    public void rotate(double dx, double dy) {
        double sin = direction.z;
        double verticalAngle = Math.asin(sin) / Math.PI * 180;
        
        if (verticalAngle + dy > 89) {
            dy = 89 - verticalAngle;
        } else if (verticalAngle + dy < -89) {
            dy = -89 - verticalAngle;
        }
        double cos = Math.sqrt(1 - sin*sin);
        double sinSliver = Math.sin(dx / 2 / 180 * Math.PI);
        double cosSliver = Math.cos(dx / 2 / 180 * Math.PI);
        vv3 hRotTangent = direction.cross(Z_AXIS).normalizeThis(2 * cos * cosSliver * sinSliver);
        vv3 hRotRadius = hRotTangent.cross(Z_AXIS).normalizeThis(2 * cos * sinSliver * sinSliver);
        sinSliver = Math.sin(dy / 2 / 180 * Math.PI);
        cosSliver = Math.cos(dy / 2 / 180 * Math.PI);
        vv3 vRotTangent = direction.cross(direction.cross(Z_AXIS)).normalizeThis(-2 * cosSliver * sinSliver);
        vv3 vRotRadius = direction.scale(2 * sinSliver * sinSliver);
        direction.addThis(hRotTangent);
        direction.addThis(hRotRadius);
        direction.addThis(vRotTangent);
        direction.addThis(vRotRadius);
        direction.normalizeThis();
    }

    public boolean update(Input input, double CAMERA_EPSILON) {
        vv3 cameraPos = position.clone(), cameraDir = direction.clone();
        rotate(input.getDeltaMouseX() / 2.0, -input.getDeltaMouseY() / 2.0);
        move(input.getKeyboardVector());

        return !cameraPos.equals(position, CAMERA_EPSILON) || !cameraDir.equals(direction, CAMERA_EPSILON);
    }
}
