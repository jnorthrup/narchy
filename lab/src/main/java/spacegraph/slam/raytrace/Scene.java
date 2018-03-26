package spacegraph.slam.raytrace;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class Scene {
    public static final int MAX_REFLECTIONS = 20;

    public Camera camera;
    public final List<RayTracer.Light> lights = new ArrayList<RayTracer.Light>();
    public final List<Entity> entities = new ArrayList<Entity>();

    public Scene(String src)  {
        Scanner scanner;
        try {
            scanner = new Scanner(new StringReader(src));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String line = Utils.nextLineOrEmpty(scanner);
        while (line != "") {
            switch (line) {
            case "camera:": {
                Vector3 position = new Vector3(0, 0, 0);
                Vector3 direction = new Vector3(1, 0, 0);
                double fov = 90;
                double size = 0;
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
                Vector3 position = new Vector3(0, 0, 0);
                double sideLength = 1;
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
                Vector3 position = new Vector3(0, 0, 0);
                double radius = 1;
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
                Vector3 position = new Vector3(0, 0, 0);
                int color = 0xffffff;
                while (Utils.isIndented(line = Utils.nextLineOrEmpty(scanner))) {
                    Scanner s = new Scanner(line);
                    switch (s.next()) {
                    case "position:":
                        position = Utils.readVector3(s);
                        if (position == null) {
                            return;
                        }
                        break;
                    case "color:":
                        color = s.nextInt(16);
                        break;
                    }
                }
                lights.add(new RayTracer.Light(position, color));
                break;
            }
            }
        }
    }

    // Check a ray against all the entities in the world and return the closest collision.
    // Returns null if the ray collides with nothing.
    public Collision castRay(Ray3 ray) {
        Collision closestCollision = null;
        double closestCollisionDistanceSquared = Double.POSITIVE_INFINITY;
        for (int i = 0, entitiesSize = entities.size(); i < entitiesSize; i++) {
            Entity entity = entities.get(i);
            Ray3 normal = entity.collide(ray);
            if (normal != null) {
                double distanceSquared = normal.position.minus(ray.position).lengthSquared();
                if (distanceSquared < closestCollisionDistanceSquared) {
                    closestCollision = new Collision(entity, normal);
                    closestCollisionDistanceSquared = distanceSquared;
                }
            }
        }
        return closestCollision;
    }

    public int getRayColor(Ray3 ray) {
        Collision collision;
        int reflections = 0;
        do {
            collision = castRay(ray);
            if (collision == null) {
                // If the ray didn't collide with anything, return black.
                return 0x000000;
            }
            if (collision.entity.surface == Entity.Surface.Transparent) {
                // Calculates deflection of a ray due to refraction if the entity is transparent.
                // Currently the index of refraction is hard-coded at 1.5 (glass).
                // The effect doesn't actually look that cool, probably it doesn't look much like
                // glass since I still need to implement an added glassy reflection on the surface
                // of transparent objects. Nevertheless, surfaces of entities in the world definition
                // file can be specified to be "transparent".
                Vector3 tangent = collision.normal.direction.cross(collision.normal.direction.cross(ray.direction)).normalize();
                double nProj = -ray.direction.dot(collision.normal.direction);
                ray.direction = ray.direction.scale(1 / nProj);
                double tProj = ray.direction.dot(tangent);
                double r = 1.5;
                ray = new Ray3(
                    collision.normal.position.minus(collision.normal.direction.scale(0.001)),
                    collision.normal.direction.scale(-1).plus(tangent.scale(tProj / r)).normalize()
                );
                collision = castRay(ray);
                tangent = collision.normal.direction.cross(collision.normal.direction.cross(ray.direction)).normalize();
                nProj = -ray.direction.dot(collision.normal.direction);
                ray.direction = ray.direction.scale(1 / nProj);
                tProj = ray.direction.dot(tangent);
                ray = new Ray3(
                    collision.normal.position.minus(collision.normal.direction.scale(0.001)),
                    collision.normal.direction.scale(-1).plus(tangent.scale(tProj * r)).normalize()
                );
                continue;
            }
            if (collision.entity.surface == Entity.Surface.Diffuse) {
                // If the collision is a diffuse surface, then there are no further reflections, and the
                // final color can be calculated from the point on the surface of the diffuse entity
                // where the collision took place, and the locations of the lights in the scene.
                break;
            }
            if (collision.entity.surface == Entity.Surface.Specular) {
                // If the collision is a specular surface, calculate the new ray to be from
                // the point of collision and with a direction that is a bounce off the surface.
                ray = new Ray3(
                    collision.normal.position,
                    ray.direction.minus(collision.normal.direction.scale(2 * ray.direction.dot(collision.normal.direction)))
                );
            } else {
                // Surface type isn't accounted for. Just return black.
                return 0x000000;
            }
            // Loop while total reflections is less than maximum reflections.
        } while (++reflections < MAX_REFLECTIONS);
        return getDiffuseColor(collision);
    }

    // Calculates the color of a collision point on a diffuse surface based on the distances and
    // locations of lights in the scene. A light is not added to the color if it is obstructed by an entity
    // which thereby implements shadows.
    private int getDiffuseColor(Collision collision) {
        double intensityR = 0;
        double intensityG = 0;
        double intensityB = 0;
        for (RayTracer.Light light : lights) {
            Vector3 lightVector = light.position.minus(collision.normal.position);
            Vector3 lightDirection = lightVector.normalize();
            Collision c = castRay(new Ray3(collision.normal.position, lightDirection));
            if (c == null || c.normal.position.minus(collision.normal.position).lengthSquared() > lightVector.lengthSquared() || c.entity.surface == Entity.Surface.Transparent) {
                double intensity = Math.abs(collision.normal.direction.dot(lightDirection)) / lightVector.lengthSquared();
                intensityR += (double)(light.color >> 16) / 255 * intensity;
                intensityG += (double)((light.color >> 8) & 0xff) / 255 * intensity;
                intensityB += (double)(light.color & 0xff) / 255 * intensity;
            }
        }
        // Intensity multiplier chosen to make the brightness level of the scene found in world.txt look good.
        double m = 10;
        intensityR *= m;
        intensityG *= m;
        intensityB *= m;
        // Add an ambient light factor.
        intensityR += 0.05;
        intensityG += 0.05;
        intensityB += 0.05;
        if (collision.entity.texture != null && collision.entity.surface == Entity.Surface.Diffuse) {
            // If the entity has a texture and is diffuse, calculate the texture color at the point
            // of collision and incorporate it into the light intensities.
            int textureColor = -1;
            if (collision.entity instanceof Entity.Cube) {
                Entity.Cube cube = (Entity.Cube)collision.entity;
                Vector3 fp = collision.normal.position.minus(cube.position);
                Vector3 afp = new Vector3(Math.abs(fp.x()), Math.abs(fp.y()), Math.abs(fp.z()));
                Vector3 axis1 = null;
                Vector3 axis2 = null;
                if (afp.x() < afp.z() && afp.y() < afp.z()) {
                    axis1 = new Vector3(1, 0, 0);
                    axis2 = new Vector3(0, 1, 0);
                } else if (afp.x() < afp.y() && afp.z() < afp.y()) {
                    axis1 = new Vector3(1, 0, 0);
                    axis2 = new Vector3(0, 0, 1);
                } else if (afp.y() < afp.x() && afp.z() < afp.x()) {
                    axis1 = new Vector3(0, 1, 0);
                    axis2 = new Vector3(0, 0, 1);
                }
                double x = 5 * (fp.dot(axis1)/cube.sideLength + 0.5) % 1;
                double y = 5 * (fp.dot(axis2)/cube.sideLength + 0.5) % 1;
                textureColor = cube.texture.getRGB(
                    (int)(x * cube.texture.getWidth()),
                    (int)(y * cube.texture.getHeight())
                );
            } else if (collision.entity instanceof Entity.Sphere) {
                Entity.Sphere sphere = (Entity.Sphere)collision.entity;
                Vector3 rp = collision.normal.position.minus(sphere.position);
                double x = Math.atan2(rp.y(), rp.x()) / (2 * Math.PI) + 0.5;
                double y = Math.asin(rp.z() / rp.length()) / Math.PI + 0.5;
                textureColor = sphere.texture.getRGB(
                    (int)(x * sphere.texture.getWidth()),
                    (int)((1 - y) * sphere.texture.getHeight())
                );
            }
            if (textureColor != -1) {
                intensityR *= (double)((textureColor >> 16) & 0xff) / 255;
                intensityG *= (double)((textureColor >> 8) & 0xff) / 255;
                intensityB *= (double)(textureColor & 0xff) / 255;
            }
        }
        int r = (int)(intensityR * 256);
        int g = (int)(intensityG * 256);
        int b = (int)(intensityB * 256);
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

    // Describes a collision in the world of a ray against an entity.
    // The normal is a vector perpendicular to the surface at the point
    // of collision, and should be normalized.
    static final class Collision {
        public final Entity entity;
        public final Ray3 normal;

        public Collision(Entity entity, Ray3 normal) {
            this.entity = entity;
            this.normal = normal;
        }
    }
}
