/*
 * Sphere.java                            STATUS: Vorl�ufig abgeschlossen
 * ----------------------------------------------------------------------
 * 
 */
package raytracer.objects;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import raytracer.basic.Ray;
import raytracer.basic.RaytracerConstants;
import raytracer.basic.Transformation;
import raytracer.shader.Shader;
import raytracer.util.FloatingPoint;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;
import java.util.Collection;


/**
 * Dieses Objekt stellt eine Kugel dar.
 * 
 * @author Mathias Kosch
 *
 */
public class Sphere extends Shape
{
    /** Mittelpunkt der Kugel. */
	private Vector3d center = new Vector3d();
    /** Radius der Kugel. */
    private double radius = 0.0;
    
    /** Nordpol der Kugel. */
    private Vector3d textureUp = new Vector3d(0.0, 1.0, 0.0);
    /** Startkoordinate der Textur, orthogonal zu <code>textureUp</code>.*/
    private Vector3d textureStart = new Vector3d(0.0, 0.0, -1.0);
	
    
    /**
     * Erzeugt eine neue Kugel.
     * 
     * @param center Mittelpunkt der Kugel.
     * @param radius Radius der Kugel.
     * @param shader Shader, mit dem die Kugel gezeichnet wird.
     */
	public Sphere(Vector3d center, double radius, Shader shader)
    {
        super(shader);
        
		this.center.set(center);
		this.radius = Math.abs(radius);
	}

    public Vector3d getCenter() {
        return center;
    }

    @Override
    public Sphere clone()
    throws CloneNotSupportedException 
    {
        Sphere clone = (Sphere)super.clone();
        
        clone.center = new Vector3d(center);
        clone.textureUp = new Vector3d(textureUp);
        clone.textureStart = new Vector3d(textureStart);
        
        return clone;
    }
    
    
    @Override
    public void getBoundingPoints(Collection<Vector3d> points)
    {
        
        points.add(new Vector3d(center.x-radius, center.y-radius, center.z-radius));
        points.add(new Vector3d(center.x+radius, center.y-radius, center.z-radius));
        points.add(new Vector3d(center.x-radius, center.y+radius, center.z-radius));
        points.add(new Vector3d(center.x+radius, center.y+radius, center.z-radius));
        points.add(new Vector3d(center.x-radius, center.y-radius, center.z+radius));
        points.add(new Vector3d(center.x+radius, center.y-radius, center.z+radius));
        points.add(new Vector3d(center.x-radius, center.y+radius, center.z+radius));
        points.add(new Vector3d(center.x+radius, center.y+radius, center.z+radius));
    }

    
	@Override
    public Vector3d getNormal(Vector3d point)
    {
        Vector3d normal = new Vector3d(point);
        normal.sub(center);
        return adjustNormal(normal, point);
	}

    
    @Override
    public boolean isFinite()
    {
        return true;
    }
        
    @Override
    public double getCentroid(final byte axisId)
    {
        switch (axisId)
        {
        case 0: return center.x;
        case 1: return center.y;
        case 2: return center.z;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public byte compareAxis(final byte axisId, final double axisValue)
    {
        
        double distance;
        switch (axisId)
        {
        case 0:
            distance = center.x-axisValue;
            break;
        case 1:
            distance = center.y-axisValue;
            break;
        case 2:
            distance = center.z-axisValue;
            break;
        default:
            throw new IllegalArgumentException();
        }
        
        
        if (distance < -radius)
            return (byte) -1;
        if (distance > radius)
            return (byte) 1;
        return (byte) 0;
    }

    @Override
    public double minAxisValue(final byte axisId)
    {
        switch (axisId)
        {
        case 0: return center.x-radius;
        case 1: return center.y-radius;
        case 2: return center.z-radius;
        default:
            throw new IllegalArgumentException();
        }
    }
    
    @Override
    public double maxAxisValue(final byte axisId)
    {
        switch (axisId)
        {
        case 0: return center.x+radius;
        case 1: return center.y+radius;
        case 2: return center.z+radius;
        default:
            throw new IllegalArgumentException();
        }
    }
    
    
    @Override
    public void transform(Transformation t)
    {
        t.transformPoint(center);
    }
    
    
    @Override
    public boolean intersect(Ray ray)
    {
        
        if ((isLight) && (ray.ignoreLights))
            return false;
        
        double ddd = ray.dir.dot(ray.dir);
        Vector3d p = new Vector3d();
        
        
        
        p.sub(center, ray.org);
        double t = ray.dir.dot(p) / ddd;
        p.scaleAdd(t, ray.dir, ray.org);
        
        
        p.sub(center);
        double x = radius * radius - p.dot(p);
        if (x < 0.0)
            return false;
        x /= ddd;
        x = Math.sqrt(x);
        
        
        if ((double) FloatingPoint.compareTolerated(t, x) <= 0.0)
        {
            if ((double) FloatingPoint.compareTolerated(t, -x) <= 0.0)
                return false;   
            t += x;             
        }
        else
            t -= x;             

        
        
        if (ray.length <= t)
            return false;
        
        ray.length = t;     
        ray.hit = this;     
        return true;
	}

	@Override
    public boolean occlude(Ray ray)
    {
        return intersect(new Ray(ray));
    }
    
    
    @Override
    public Vector2d getTextureCoords(Vector3d point)
    {
        
        Vector3d normal = new Vector3d();
        normal.sub(point, center);
        
        Vector3d temp = new Vector3d();
        
        temp.cross(normal, textureUp);
        double alpha = Math.acos((double) (float) (-normal.dot(textureUp) / (normal.length() * textureUp.length())));

        
        normal.cross(textureUp, normal);
        normal.cross(normal, textureUp);
        
        temp.cross(textureStart, textureUp);
        double beta = Math.acos((double) (float) (normal.dot(textureStart) / (normal.length() * textureStart.length())));
        
        
        if (Math.signum(temp.dot(normal)) < 0.0)
        {
            beta = 2.0 *Math.PI-beta;
        }
        
        double y = alpha/Math.PI;
        double x = (2.0 *Math.PI-beta)/(2.0 *Math.PI);
        
        
        
        
        return new Vector2d(x, y);
    }
    
    @Override
    public void transformTexture(Transformation t)
    {
        
        t.transformVector(textureUp);
        t.transformVector(textureStart);
    }
    
    
    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL2 gl = (GL2)drawable.getGL();
        GLU glu = GLU.createGLU(gl);
        
        
        GLUquadric quadric = glu.gluNewQuadric();

        gl.glPushMatrix();
        gl.glTranslated(center.x, center.y, center.z);
        if (RaytracerConstants.GL_GRID_MODE_ENABLED)
            glu.gluQuadricDrawStyle(quadric, GLU.GLU_LINE);
        glu.gluSphere(quadric, radius, RaytracerConstants.GL_RESOLUTION, RaytracerConstants.GL_RESOLUTION);
        gl.glPopMatrix();
        glu.gluDeleteQuadric(quadric);
    }    
}