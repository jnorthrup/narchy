package spacegraph.space3d.raytrace;

import jcog.math.vv3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public abstract class Entity {

    public Surface surface;
    public vv3 position;
    public BufferedImage texture;

    public abstract Ray3 collide(Ray3 ray);

    public static final class Cube extends Entity {
        double sideLength;

        private final Ray3[] faces;

        Cube(vv3 position, double sideLength, Surface surface, String texture) {
            this.position = position;
            this.sideLength = sideLength;
            this.surface = surface;
            try {
                this.texture = ImageIO.read(new File(texture));
            } catch (IOException e) {
                e.printStackTrace();
            }
            double hs = sideLength / 2;
            faces = new Ray3[]{
                new Ray3(
                    position.add(new vv3(-hs, 0, 0)),
                    new vv3(-1, 0, 0)
                ),
                new Ray3(
                    position.add(new vv3(hs, 0, 0)),
                    new vv3(1, 0, 0)
                ),
                new Ray3(
                    position.add(new vv3(0, -hs, 0)),
                    new vv3(0, -1, 0)
                ),
                new Ray3(
                    position.add(new vv3(0, hs, 0)),
                    new vv3(0, 1, 0)
                ),
                new Ray3(
                    position.add(new vv3(0, 0, -hs)),
                    new vv3(0, 0, -1)
                ),
                new Ray3(
                    position.add(new vv3(0, 0, hs)),
                    new vv3(0, 0, 1)
                )
            };
        }

        @Override
        public Ray3 collide(Ray3 ray) {
            Ray3 closestNormal = null;
            double distanceSquared = 0;
            for (Ray3 face : faces) {
                vv3 faceNormal = face.direction;
                double distance = ray.position.minus(face.position).dot(faceNormal);
                if (distance < 0) {
                    faceNormal = faceNormal.scale(-1);
                    distance = -distance;
                }
                Ray3 normal = new Ray3(
                    ray.position.minus(
                        ray.direction.scale(distance / ray.direction.dot(faceNormal))
                    ),
                    faceNormal
                );
                if (normal.position.minus(ray.position).dot(ray.direction) < Scene.Epsilon) {
                    continue;
                }
                vv3 fp = normal.position.minus(face.position);
                double hs = sideLength / 2;
                if (Math.abs(fp.x) > hs || Math.abs(fp.y) > hs || Math.abs(fp.z) > hs) {
                    continue;
                }
                if (closestNormal == null ||
                        normal.position.minus(ray.position).lengthSquared() < distanceSquared) {
                    closestNormal = normal;
                    distanceSquared = normal.position.minus(ray.position).lengthSquared();
                }
            }
            return closestNormal;
        }
    }

    public static final class Sphere extends Entity {


        double radius;

        Sphere(vv3 position, double radius, Surface surface, String texture) {
            this.position = position;
            this.radius = radius;
            this.surface = surface;
            try {
                this.texture = ImageIO.read(new File(texture));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Ray3 collide(Ray3 ray) {
            vv3 closestPoint = ray.direction.scale(
                position.minus(ray.position).dot(ray.direction)
            ).add(ray.position);

            vv3 perpendicular = closestPoint.minus(position);
            if (perpendicular.lengthSquared() >= radius * radius)
                return null;

            vv3 opposite = ray.direction.scale(
                Math.sqrt(radius*radius - perpendicular.lengthSquared())
            );
            vv3 posPerp = position.add(perpendicular);
            vv3 intersection1 = posPerp.minus(opposite);
            vv3 intersection2 = posPerp.add(opposite);
            double distance1 = intersection1.minus(ray.position).dot(ray.direction);
            double distance2 = intersection2.minus(ray.position).dot(ray.direction);

            if (distance1 <= Scene.Epsilon && distance2 <= Scene.Epsilon)
                return null;

            vv3 intersection;
            if (distance1 > 0 && distance2 <= Scene.Epsilon) {
                intersection = intersection1;
            } else if (distance2 > 0 && distance1 <= Scene.Epsilon) {
                intersection = intersection2;
            } else if (distance1 < distance2) {
                intersection = intersection1;
            } else {
                intersection = intersection2;
            }
            Ray3 normal = new Ray3(intersection, intersection.minus(position));

            if (ray.position.minus(position).lengthSquared() < radius * radius)
                normal.direction.invertThis();

            normal.direction.normalizeThis();
            return normal;
        }
    }

    public enum Surface {
        Specular, Diffuse, Transparent
    }
}
