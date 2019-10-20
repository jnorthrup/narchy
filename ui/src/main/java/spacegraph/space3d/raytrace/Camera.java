package spacegraph.space3d.raytrace;

import jcog.math.vv3;

import static jcog.math.vv3.Z_AXIS;

public final class Camera extends Ray3 {
    public double fov;
    private double size;

    public Camera(vv3 position, vv3 direction, double fov, double size) {
        super(position, direction.normalize());
        this.fov = fov;
        this.size = size;
    }

    
    public Ray3 ray(double x, double y, double aspectRatio) {
        var xAxis = direction.cross(vv3.Z_AXIS).normalizeThis();
        var yAxis = xAxis.cross(direction);

        var widthNear = size;
        var heightNear = widthNear / aspectRatio;

        var widthFar = 2 * Math.tan(fov / 2 / 180 * Math.PI) + widthNear;
        var heightFar = widthFar / aspectRatio;

        var originNear = position.
            minus(xAxis.scale(widthNear / 2)).
            minus(yAxis.scale(heightNear / 2));
        var originFar = direction.
                add(position).
            minus(xAxis.scale(widthFar / 2)).
            minus(yAxis.scale(heightFar / 2));

        var pointNear = originNear.
                add(xAxis.scale(x * widthNear)).
                add(yAxis.scale(y * heightNear));
        var pointFar = originFar.
                add(xAxis.scale(x * widthFar)).
                add(yAxis.scale(y * heightFar));

        return new Ray3(pointNear, pointFar.minus(pointNear).normalizeThis());
    }

    public void move(vv3 keyboardVector) {
        position.addThis(direction.scale(keyboardVector.y));
        position.addThis(direction.cross(vv3.Z_AXIS).normalize().scale(keyboardVector.x));
    }

    public void rotate(double dx, double dy) {
        var sin = direction.z;
        var verticalAngle = Math.asin(sin) / Math.PI * 180;
        
        if (verticalAngle + dy > 89) {
            dy = 89 - verticalAngle;
        } else if (verticalAngle + dy < -89) {
            dy = -89 - verticalAngle;
        }
        var cos = Math.sqrt(1 - sin*sin);
        var sinSliver = Math.sin(dx / 2 / 180 * Math.PI);
        var cosSliver = Math.cos(dx / 2 / 180 * Math.PI);
        var hRotTangent = direction.cross(Z_AXIS).normalizeThis(2 * cos * cosSliver * sinSliver);
        var hRotRadius = hRotTangent.cross(Z_AXIS).normalizeThis(2 * cos * sinSliver * sinSliver);
        sinSliver = Math.sin(dy / 2 / 180 * Math.PI);
        cosSliver = Math.cos(dy / 2 / 180 * Math.PI);
        var vRotTangent = direction.cross(direction.cross(Z_AXIS)).normalizeThis(-2 * cosSliver * sinSliver);
        var vRotRadius = direction.scale(2 * sinSliver * sinSliver);
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
