package spacegraph.slam.raytrace;

public final class Camera extends Ray3 {
    public double fov;
    public double size;

    public Camera(Vector3 position, Vector3 direction, double fov, double size) {
        super(position, direction.normalize());
        this.fov = fov;
        this.size = size;
    }

    
    public Ray3 getRay(double x, double y, double aspectRatio) {
        Vector3 xAxis = direction.cross(new Vector3(0, 0, 1)).normalize();
        Vector3 yAxis = xAxis.cross(direction);

        double widthNear = size;
        double heightNear = widthNear / aspectRatio;

        double widthFar = 2 * Math.tan(fov / 2 / 180 * Math.PI) + widthNear;
        double heightFar = widthFar / aspectRatio;

        Vector3 originNear = position.
            minus(xAxis.scale(widthNear / 2)).
            minus(yAxis.scale(heightNear / 2));
        Vector3 originFar = direction.
            plus(position).
            minus(xAxis.scale(widthFar / 2)).
            minus(yAxis.scale(heightFar / 2));

        Vector3 pointNear = originNear.
            plus(xAxis.scale(x * widthNear)).
            plus(yAxis.scale(y * heightNear));
        Vector3 pointFar = originFar.
            plus(xAxis.scale(x * widthFar)).
            plus(yAxis.scale(y * heightFar));

        return new Ray3(pointNear, pointFar.minus(pointNear).normalize());
    }

    public void move(Vector3 keyboardVector) {
        position = position.
            plus(direction.scale(keyboardVector.y())).
            plus(direction.cross(new Vector3(0, 0, 1)).normalize().scale(keyboardVector.x()));
    }

    
    
    
    public void rotate(double dx, double dy) {
        double sin = direction.z();
        double verticalAngle = Math.asin(sin) / Math.PI * 180;
        
        if (verticalAngle + dy > 89) {
            dy = 89 - verticalAngle;
        } else if (verticalAngle + dy < -89) {
            dy = -89 - verticalAngle;
        }
        double cos = Math.sqrt(1 - sin*sin);
        double sinSliver = Math.sin(dx / 2 / 180 * Math.PI);
        double cosSliver = Math.cos(dx / 2 / 180 * Math.PI);
        Vector3 hRotTangent = direction.cross(new Vector3(0, 0, 1)).normalize().scale(2 * cos * cosSliver * sinSliver);
        Vector3 hRotRadius = hRotTangent.cross(new Vector3(0, 0, 1)).normalize().scale(2 * cos * sinSliver * sinSliver);
        sinSliver = Math.sin(dy / 2 / 180 * Math.PI);
        cosSliver = Math.cos(dy / 2 / 180 * Math.PI);
        Vector3 vRotTangent = direction.cross(direction.cross(new Vector3(0, 0, 1))).normalize().scale(-2 * cosSliver * sinSliver);
        Vector3 vRotRadius = direction.scale(2 * sinSliver * sinSliver);
        direction = direction.
            plus(hRotTangent).
            plus(hRotRadius).
            plus(vRotTangent).
            plus(vRotRadius).
            normalize();
    }
}
