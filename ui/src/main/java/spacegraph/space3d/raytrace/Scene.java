package spacegraph.space3d.raytrace;

import jcog.data.list.FasterList;
import jcog.math.vv3;

import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

final class Scene {

    private static final int MAX_REFLECTIONS = 3;
    static final double Epsilon = 0.000001;

    private final Collection<Light> lights = new FasterList<>();
    private final List<Entity> entities = new FasterList<>();
    public Camera camera;

    public Scene(String src) {
        Scanner scanner;
        try {
            scanner = new Scanner(new StringReader(src));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String line = Utils.nextLineOrEmpty(scanner);
        while (!line.isEmpty()) {
            switch (line) {
                case "camera:": {
                    vv3 position = new vv3((double) 0, (double) 0, (double) 0);
                    vv3 direction = new vv3(1.0, (double) 0, (double) 0);
                    double fov = 90.0;
                    double size = (double) 0;
                    while (Utils.isIndented(line = Utils.nextLineOrEmpty(scanner))) {
                        Scanner s = new Scanner(line);
                        switch (s.next()) {
                            case "position:":
                                position = Utils.readVector3(s);
                                break;
                            case "direction:":
                                direction = Utils.readVector3(s);
                                break;
                            case "fov:":
                                fov = s.nextDouble();
                                break;
                            case "size:":
                                size = s.nextDouble();
                                break;
                        }
                    }
                    camera = new Camera(position, direction, fov, size);
                }
                case "cube:": {
                    vv3 position = new vv3((double) 0, (double) 0, (double) 0);
                    double sideLength = 1.0;
                    Entity.Surface surface = null;
                    String texture = "";
                    while (Utils.isIndented(line = Utils.nextLineOrEmpty(scanner))) {
                        Scanner s = new Scanner(line);
                        switch (s.next()) {
                            case "position:":
                                position = Utils.readVector3(s);
                                break;
                            case "sideLength:":
                                sideLength = s.nextDouble();
                                break;
                            case "surface:":
                                surface = Utils.readSurface(s);
                                break;
                            case "texture:":
                                texture = s.next();
                                break;
                        }
                    }
                    entities.add(new Entity.Cube(position, sideLength, surface, texture));
                    break;
                }
                case "sphere:": {
                    vv3 position = new vv3((double) 0, (double) 0, (double) 0);
                    double radius = 1.0;
                    Entity.Surface surface = null;
                    String texture = "";
                    while (Utils.isIndented(line = Utils.nextLineOrEmpty(scanner))) {
                        Scanner s = new Scanner(line);
                        switch (s.next()) {
                            case "position:":
                                position = Utils.readVector3(s);
                                break;
                            case "radius:":
                                radius = s.nextDouble();
                                break;
                            case "surface:":
                                surface = Utils.readSurface(s);
                                break;
                            case "texture:":
                                texture = s.next();
                                break;
                        }
                    }
                    entities.add(new Entity.Sphere(position, radius, surface, texture));
                    break;
                }
                case "light:": {
                    vv3 position = new vv3((double) 0, (double) 0, (double) 0);
                    int color = 0xffffff;
                    while (Utils.isIndented(line = Utils.nextLineOrEmpty(scanner))) {
                        Scanner s = new Scanner(line);
                        switch (s.next()) {
                            case "position:":
                                position = Utils.readVector3(s);
                                break;
                            case "color:":
                                color = s.nextInt(16);
                                break;
                        }
                    }
                    lights.add(new Light(position, color));
                    break;
                }
            }
        }
    }


    private Collision castRay(Ray3 ray) {
        double closestCollisionDistanceSquared = Double.POSITIVE_INFINITY;
        Entity closestEntity = null;
        Ray3 closestNormal = null;
        for (int i = 0, entitiesSize = entities.size(); i < entitiesSize; i++) {
            Entity entity = entities.get(i);

            Ray3 normal = entity.collide(ray);
            if (normal == null)
                continue;

            double distanceSquared = normal.position.distanceSquared(ray.position);
            if (distanceSquared < closestCollisionDistanceSquared) {
                closestEntity = entity;
                closestNormal = normal;
                closestCollisionDistanceSquared = distanceSquared;
            }
        }
        return closestEntity != null ? new Collision(closestEntity, closestNormal) : null;
    }

    int rayColor(Ray3 ray) {
        Collision collision;
        int reflections = 0;
        do {
            collision = castRay(ray);
            if (collision == null)
                return 0x000000;

            Entity.Surface surface = collision.entity.surface;
            if (surface == Entity.Surface.Transparent) {


                vv3 tangent = collision.normal.direction.cross(collision.normal.direction.cross(ray.direction)).normalizeThis();
                double nProj = -ray.direction.dot(collision.normal.direction);
                ray.direction.scaleThis(1.0 / nProj);
                double tProj = ray.direction.dot(tangent);
                double r = 1.5;
                ray = new Ray3(
                        collision.normal.position.minus(collision.normal.direction.scale(0.001)),
                        collision.normal.direction.scale(-1.0).add(tangent.scale(tProj / r)).normalizeThis()
                );
                collision = castRay(ray);
                tangent = collision.normal.direction.cross(collision.normal.direction.cross(ray.direction)).normalizeThis();
                nProj = -ray.direction.dot(collision.normal.direction);
                ray.direction.scaleThis(1.0 / nProj);
                tProj = ray.direction.dot(tangent);
                ray = new Ray3(
                        collision.normal.position.minus(collision.normal.direction.scale(0.001)),
                        collision.normal.direction.scale(-1.0).add(tangent.scale(tProj * r)).normalizeThis()
                );
                continue;
            }
            if (surface == Entity.Surface.Diffuse) {
                break;
            }
            if (surface == Entity.Surface.Specular) {
                ray = new Ray3(
                        collision.normal.position,
                        ray.direction.minus(collision.normal.direction.scale(2.0 * ray.direction.dot(collision.normal.direction)))
                );
            } else {
                //return 0x000000;
                throw new UnsupportedOperationException();
            }

        } while (++reflections < MAX_REFLECTIONS);
        return getDiffuseColor(collision);
    }


    private int getDiffuseColor(Collision collision) {
        double intensityR = (double) 0;
        double intensityG = (double) 0;
        double intensityB = (double) 0;
        for (Light light : lights) {
            vv3 lightVector = light.position.minus(collision.normal.position);
            double lightVectorLenSq = lightVector.lengthSquared();
            vv3 lightDirection = lightVector.normalizeThis();
            Collision c = castRay(new Ray3(collision.normal.position, lightDirection));
            if (c == null || c.normal.position.minus(collision.normal.position).lengthSquared() > lightVectorLenSq || c.entity.surface == Entity.Surface.Transparent) {
                double intensity = Math.abs(collision.normal.direction.dot(lightDirection)) / lightVectorLenSq;
                intensityR += (double) (light.color >> 16) / 255.0 * intensity;
                intensityG += (double) ((light.color >> 8) & 0xff) / 255.0 * intensity;
                intensityB += (double) (light.color & 0xff) / 255.0 * intensity;
            }
        }

        double m = 10.0;
        intensityR *= m;
        intensityG *= m;
        intensityB *= m;

        intensityR += 0.05;
        intensityG += 0.05;
        intensityB += 0.05;
        if (collision.entity.texture != null && collision.entity.surface == Entity.Surface.Diffuse) {


            int textureColor = -1;
            if (collision.entity instanceof Entity.Cube) {
                Entity.Cube cube = (Entity.Cube) collision.entity;
                vv3 fp = collision.normal.position.minus(cube.position);
                vv3 afp = new vv3(Math.abs(fp.x), Math.abs(fp.y), Math.abs(fp.z));
                vv3 axis1 = null;
                vv3 axis2 = null;
                if (afp.x < afp.z && afp.y < afp.z) {
                    axis1 = vv3.X_AXIS;
                    axis2 = vv3.Y_AXIS;
                } else if (afp.x < afp.y && afp.z < afp.y) {
                    axis1 = vv3.X_AXIS;
                    axis2 = vv3.Z_AXIS;
                } else if (afp.y < afp.x && afp.z < afp.x) {
                    axis1 = vv3.Y_AXIS;
                    axis2 = vv3.Z_AXIS;
                }
                double x = 5.0 * (fp.dot(axis1) / cube.sideLength + 0.5) % 1.0;
                double y = 5.0 * (fp.dot(axis2) / cube.sideLength + 0.5) % 1.0;
                textureColor = cube.texture.getRGB(
                        (int) (x * (double) cube.texture.getWidth()),
                        (int) (y * (double) cube.texture.getHeight())
                );
            } else if (collision.entity instanceof Entity.Sphere) {
                Entity.Sphere sphere = (Entity.Sphere) collision.entity;
                vv3 rp = collision.normal.position.minus(sphere.position);
                double x = Math.atan2(rp.y, rp.x) / (2.0 * Math.PI) + 0.5;
                double y = Math.asin(rp.z / rp.length()) / Math.PI + 0.5;
                textureColor = sphere.texture.getRGB(
                        (int) (x * (double) sphere.texture.getWidth()),
                        (int) ((1.0 - y) * (double) sphere.texture.getHeight())
                );
            }
            if (textureColor != -1) {
                intensityR *= (double) ((textureColor >> 16) & 0xff) / 255.0;
                intensityG *= (double) ((textureColor >> 8) & 0xff) / 255.0;
                intensityB *= (double) (textureColor & 0xff) / 255.0;
            }
        }
        int r = (int) (intensityR * 256.0);
        int g = (int) (intensityG * 256.0);
        int b = (int) (intensityB * 256.0);
        if (r > 255) {
            r = 255;
        }
        if (g > 255) {
            g = 255;
        }
        if (b > 255) {
            b = 255;
        }
        return (r << 16) + (g << 8) + b;
    }


    static final class Collision {
        final Entity entity;
        final Ray3 normal;

        Collision(Entity entity, Ray3 normal) {
            this.entity = entity;
            this.normal = normal;
        }
    }

    static class Utils {
        static String nextLineOrEmpty(Scanner scanner) {
            return scanner.hasNextLine() ? scanner.nextLine() : "";
        }

        static boolean isIndented(String line) {
            return !line.isEmpty() && ((int) line.charAt(0) == (int) '\t' || (int) line.charAt(0) == (int) ' ');
        }

        static Entity.Surface readSurface(Scanner scanner) {
            switch (scanner.next()) {
                case "diffuse":
                    return Entity.Surface.Diffuse;
                case "specular":
                    return Entity.Surface.Specular;
                case "transparent":
                    return Entity.Surface.Transparent;
            }
            throw new RuntimeException("Non-existent surface!");
        }

        static vv3 readVector3(Scanner scanner) {
            String str = scanner.nextLine().trim();
            if ((int) str.charAt(0) != (int) '(' || (int) str.charAt(str.length() - 1) != (int) ')') {
                throw new RuntimeException("Coordinates must be parenthesized!");
            }
            str = str.substring(1, str.length() - 1);
            String[] coords = str.split(",");
            if (coords.length != 3) {
                throw new RuntimeException("A coordinates must have exactly 3 components!");
            }
            for (int i = 0; i < coords.length; i++) {
                coords[i] = coords[i].trim();
            }
            double[] parsedCoords = new double[coords.length];
            for (int i = 0; i < parsedCoords.length; i++) {
                try {
                    parsedCoords[i] = Double.parseDouble(coords[i]);
                } catch (Exception e) {
                    throw new RuntimeException("Components of coordinate must be numbers!");
                }
            }
            return new vv3(parsedCoords[0], parsedCoords[1], parsedCoords[2]);
        }
    }

	public static final class Light {
		public final vv3 position;
		public final int color;

		public Light(vv3 position, int color) {
			this.position = position;
			this.color = color;
		}
	}
}
