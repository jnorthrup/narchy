package spacegraph.slam.raytrace;

public class Ray3 {
    public Vector3 position;
    public Vector3 direction;

    public Ray3() {

    }

    public void set(Vector3 position, Vector3 direction) {
        this.position = position;
        this.direction = direction;
    }

    public Ray3(Vector3 position, Vector3 direction) {
        this.position = position;
        this.direction = direction;
    }
}
