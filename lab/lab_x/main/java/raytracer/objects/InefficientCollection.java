/*
 * InefficientCollection.java             STATUS: Vorl�ufig abgeschlossen
 * ----------------------------------------------------------------------
 * 
 */

package raytracer.objects;

import com.jogamp.opengl.GLAutoDrawable;
import raytracer.basic.Ray;
import raytracer.basic.Transformation;

import javax.vecmath.Vector3d;
import java.util.Collection;
import java.util.Vector;

/**
 * Diese Sammlung von Szenen-Objekten ist (absichtlich) ineffizient.
 * 
 * @author Mathias Kosch
 *
 */
public class InefficientCollection extends SceneObjectCollection
{
    /** Liste aller Objekte. */
    private Vector<Shape> objects = new Vector<Shape>();
    

    @Override
    @SuppressWarnings("unchecked")
    public InefficientCollection clone()
    throws CloneNotSupportedException
    {
        InefficientCollection clone = (InefficientCollection)super.clone();
        clone.objects = new Vector<Shape>();
        
        
        for (Shape object : objects) clone.objects.add(object.clone());
        
        return clone;
    }

    
    @Override
    public void add(SceneObject object)
    {
        try
        {
            object.clone().getShapes(objects);
        }
        catch (CloneNotSupportedException e)
        {
            throw new IllegalStateException();
        }
    }
    
    @Override
    public void addAll(Collection<SceneObject> objects)
    {
        for (SceneObject object : objects) add(object);
    }
    
    
    @Override
    public void getShapes(Collection<Shape> shapes)
    {
        shapes.addAll(objects);
    }
    
    @Override
    public void getBoundingPoints(Collection<Vector3d> points)
    {
        for (Shape object : objects) object.getBoundingPoints(points);
    }

    
    @Override
    public void transform(Transformation t)
    {
        
        for (Shape object : objects) object.transform(t);
    }

    
    @Override
    public boolean intersect(Ray ray)
    {
        boolean intersection = false;

        
        for (Shape object : objects)
            if (object.intersect(ray))
                intersection = true;
        
        return intersection;
    }
    
    @Override
    public boolean occlude(Ray ray)
    {
        for (Shape object : objects)
            if (object.occlude(ray))
                return true;
        return false;
    }
    
    
    @Override
    public void display(GLAutoDrawable drawable)
    {
        
        for (Shape object : objects) object.display(drawable);
    }
}