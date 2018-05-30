/*
 * EfficientCollection.java               STATUS: Vorl�ufig abgeschlossen
 * ----------------------------------------------------------------------
 * 
 * NOTE: Es ist m�glich, den Speicherbedarf zu verringern. Dazu wird eine
 *       Array mit allen endlichen Objekten verwendet. Das Array wird
 *       so sortiert, dass alle Objekte < Trennline links, alle Objekte
 *       > Trennlinie rechts sind und alle Objekte = Trennlinie in der Mitte
 *       sind.
 *       Anschlie�end werden in den Unterb�umen keine Referenzen auf neue
 *       Listen gespeichert, sondern Indizes auf den Start und das Ende des
 *       Bereichs im Array.
 */

package raytracer.objects;

import com.jogamp.opengl.GLAutoDrawable;
import org.apache.commons.math3.util.FastMath;
import raytracer.basic.Ray;
import raytracer.basic.Transformation;
import raytracer.util.FloatingPoint;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

/**
 * Diese Sammlung von Szenen-Objekten implementiert einen schnellen KD-Tree.
 * 
 * @author Mathias Kosch
 *
 */
public class EfficientCollection extends SceneObjectCollection
{
    /** Maximale Anzahl von Szenen-Objekten pro Blatt. (Mindestens <code>1</code>.). */
    protected final static int MAX_OBJECTS_IN_LEAF = 12;
    /** Maximale Rate von duplizierten Objekten in den Teilb�umen. */
    protected final static float MAX_DUPLICATE_RATIO = 1.0f/3.0f;
    /** Minimaler Anteil, um einen leeren Achsenabschnitt abzuschneiden. */
    protected final static float MIN_CUT_EMPTY_RATIO = 0.0001f;

    public static final Shape[] emptyShapes = new Shape[0];


    /** Liste aller Objekte im KD-Tree. */
    protected List<Shape> finiteObjects = new ArrayList<Shape>();
    protected List<Shape> infiniteObjects = new ArrayList<Shape>();
    
    /** Wurzel des KD-Trees, oder <code>null</code>. */
    protected Object root = null;
    
    
    @Override
    public EfficientCollection clone()
    throws CloneNotSupportedException
    {
        EfficientCollection clone = (EfficientCollection)super.clone();
        clone.finiteObjects = new ArrayList<Shape>();
        clone.infiniteObjects = new ArrayList<Shape>();
        clone.root = null;

        
        Iterator<Shape> it = finiteObjects.iterator();
        while (it.hasNext())
            clone.finiteObjects.add(it.next().clone());

        
        it = infiniteObjects.iterator();
        while (it.hasNext())
            clone.infiniteObjects.add(it.next().clone());
        
        return clone;
    }

    
    @Override
    public void getShapes(Collection<Shape> shapes)
    {
        

        shapes.addAll(finiteObjects);
        shapes.addAll(infiniteObjects);
    }
    
    @Override
    public void getBoundingPoints(Collection<Vector3d> points)
    {

        Iterator<Shape> it = finiteObjects.iterator();
        while (it.hasNext())
            it.next().getBoundingPoints(points);
        
        it = infiniteObjects.iterator();
        while (it.hasNext())
            it.next().getBoundingPoints(points);
    }

        
    @Override
    public void add(SceneObject object)
    {
        try
        {
            
            List<Shape> shapes = new ArrayList<Shape>();
            object.getShapes(shapes);
            
            
            for (Shape shape : shapes) {
                Shape s = shape.clone();
                if (s.isFinite())
                    finiteObjects.add(s);
                else
                    infiniteObjects.add(s);
            }
            
            
            root = null;
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
    public void transform(Transformation t)
    {

        
        Iterator<Shape> it = finiteObjects.iterator();
        while (it.hasNext())
            it.next().transform(t);
        
        
        it = infiniteObjects.iterator();
        while (it.hasNext())
            it.next().transform(t);
    }

    
    @Override
    public boolean intersect(Ray ray)
    {
        boolean intersection = false;

        
        for (Shape infiniteObject : infiniteObjects)
            if (infiniteObject.intersect(ray))
                intersection = true;

        
        Recursion recursion = new Recursion(ray, true);
        try
        {
            
            if (root == null)
                CreateTree();
            
            
            traceRecursive(root, recursion);
        }
        catch (Result r) {}
        
        
        return intersection || recursion.intersection;
    }
    
    @Override
    public boolean occlude(Ray ray)
    {
        for (Shape infiniteObject : infiniteObjects)
            if (infiniteObject.occlude(ray))
                return true;

        Recursion recursion = new Recursion(ray, false);
        try
        {
            if (root == null)
                CreateTree();
            traceRecursive(root, recursion);
        }
        catch (Result r) {}
        
        
        return recursion.intersection;
    }
    
    
    @Override
    public void display(GLAutoDrawable drawable)
    {

        
        Iterator<Shape> it = finiteObjects.iterator();
        while (it.hasNext())
            it.next().display(drawable);
        
        
        it = infiniteObjects.iterator();
        while (it.hasNext())
            it.next().display(drawable);
    }
    
    
    /**
     * Verfolgt einen Strahl rekursiv durch alle Aufteilungen des KD-Trees.<br>
     * Diese Methode stellt sicher, dass nur die (endlichen) Szenen-Objekte
     * auf Schnitt mit dem Strahl gepr�ft werden, deren umschlie�ender Quader
     * im KD-Tree auf der Bahn des Strahles liegt.
     * 
     * @param node Knoten im KD-Tree, f�r den und dessen Kinder der effiziente
     *        Schnitt-Test ausgef�hrt wird.
     * @param r Informationen zu der Rekursion.
     * @throws raytracer.objects.EfficientCollection.Result
     */
    private static void traceRecursive(final Object node, final Recursion r)
    throws Result
    {
        
        
        
        
        
        
        if (!(node instanceof Node))
        {
            
            boolean intersection = false;
            int count = ((SceneObject[])node).length;
            for (int i = 0; i < count; i++)
            {
                final SceneObject obj = ((SceneObject[])node)[i];
                if (r.shortestIntersection)
                {
                    if (obj.intersect(r.ray))
                        intersection = true;
                }
                else
                {
                    if (obj.occlude(r.ray))
                    {
                        r.intersection = true;
                        throw new Result();
                    }
                }
            }
            
            
            
            if (!intersection)
                return;
            r.intersection = true;
        
            
            
            
            
            
            r.temp.scaleAdd(r.ray.length, r.ray.dir, r.ray.org);
            if ((r.temp.x < r.min[0]) || (r.temp.x > r.max[0]) ||
                    (r.temp.y < r.min[1]) || (r.temp.y > r.max[1]) ||
                    (r.temp.z < r.min[2]) || (r.temp.z > r.max[2]))
                return;
            
            
            throw new Result();
        }

        final Node nn = (Node)node;

        
        double saved;
        
        
        
        
        boolean first = true, second = true;
        
        if (r.dir[((int) nn.axisId)] == 0.0)
        {
            
            
            
            second = false;
        }
        else
        {
            
            double t = ((double) nn.axisValue -r.org[((int) nn.axisId)])/r.dir[((int) nn.axisId)];
            
            
            
            
            byte tCount = (byte) 0;
            for (byte axisId = (byte) 0; (int) axisId < 3; axisId++)
            {
                if (r.dir[((int) axisId)] == 0.0)
                    continue;
                
                saved = (r.min[((int) axisId)]-r.org[((int) axisId)])/r.dir[((int) axisId)];
                if (saved < t) tCount--;
                if (saved > t) tCount++;
                saved = (r.max[((int) axisId)]-r.org[((int) axisId)])/r.dir[((int) axisId)];
                if (saved < t) tCount--;
                if (saved > t) tCount++;
            }
        
            
            
            if (tCount == 0)
            {
                
                
                if (t < 0.0)
                {
                    
                    
                    second = false;
                }
            }
            else
            {
                
                
                
                if (t* tCount <= 0.0)
                    second = false;
                else
                    first = false;
            }
        }
        
        
        
        if (r.org[((int) nn.axisId)] < (double) nn.axisValue)
        {
            
            
            if (first)
            {
                saved = r.max[((int) nn.axisId)];
                r.max[((int) nn.axisId)] = nn.axisValue;
                traceRecursive(nn.left, r);
                r.max[((int) nn.axisId)] = (float)saved;
            }
            if (second)
            {
                saved = r.min[((int) nn.axisId)];
                r.min[((int) nn.axisId)] = nn.axisValue;
                traceRecursive(nn.right, r);
                r.min[((int) nn.axisId)] = (float)saved;
            }
        }
        else
        {
            
            
            if (first)
            {
                saved = r.min[((int) nn.axisId)];
                r.min[((int) nn.axisId)] = nn.axisValue;
                traceRecursive(nn.right, r);
                r.min[((int) nn.axisId)] = (float)saved;
            }
            if (second)
            {
                saved = r.max[((int) nn.axisId)];
                r.max[((int) nn.axisId)] = nn.axisValue;
                traceRecursive(nn.left, r);
                r.max[((int) nn.axisId)] = (float)saved;
            }
        }
    }
    
    
    /**
     * Erzeugt einen neuen KD-Tree aus allen Szenen-Objekten.
     */
    protected void CreateTree()
    {
        
        float min[] = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        float max[] = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
        root = CreateTree(finiteObjects.toArray(new Shape[finiteObjects.size()]), min, max, (byte)0, 0, 0);
    }
    
    /**
     * Erzeugt einen neuen KD-Tree aus einer Menge von Szenen-Objekten.
     *
     * @param objects Szenen-Objekte, die anhand des Medians geteilt werden.
     * @param min Kleinste x-, y- und z-Koordinate des betrachteten Quaders.
     * @param max Gr��te x-, y- und z-Koordinate des betrachteten Quaders.
     * @param axisId ID der Achse, beginnend bei <code>0</code> f�r die x-Achse.
     * @param updateCount Gibt an, wie viele Schritte die Anzahl der Objekte
     *        im aktuellen Teilbaum bez�glich der Eltern-Teilb�ume unver�ndert
     *        geblieben ist.
     * @return <code>Node</code>-Objekt der Wurzel des erzeugten Baumes oder
     *         eine Menge von Szenen-Objekten, falls keine weitere Aufteilung
     *         statt findet.
     */
    private Object CreateTree(Shape objects[], float min[], float max[],
            byte axisId, int cutUpdateCount, int medianUpdateCount)
    {
        
        
        if (objects.length <= MAX_OBJECTS_IN_LEAF)
            return objects;
        
        
        
        
        if ((cutUpdateCount > 20) || (medianUpdateCount > 3))
            return objects;
        
        byte j;

        Vector3f result[] = new Vector3f[3];
        for (j = (byte) 0; (int) j < 3; j++)
            result[((int) j)] = FindPartition(objects, min[((int) j)], max[((int) j)], j);
        
        
        float length = Float.NEGATIVE_INFINITY;
        float axisValue = Float.NaN;

        byte id = -1;
        for (j = (byte) 0; (int) j < 3; j++)
        {
            if (result[((int) j)].x > length)
            {
                id = j;
                length = result[((int) j)].x;
                axisValue = result[((int) j)].y;
            }
        }
        
        
        
        
        if (((int) id >= 0) && (length >= MIN_CUT_EMPTY_RATIO*(max[((int) id)]-min[((int) id)])))
        {
            System.out.println(id + " : " + length + " : " + axisValue);
            return PartitionObjects(objects, min, max, id, axisValue, cutUpdateCount+1, 0);
        }
        
        
        return PartitionObjects(objects, min, max, axisId, result[((int) axisId)].z, 0, medianUpdateCount+1);
    }
        
    /**
     * Ermittelt eine Aufteilung einer Menge von Objekten bei einem Wert
     * an einer bestimmten Achse.<br>
     * Die Aufteilung wird verwendet, um daraus zwei Teilb�ume des KD-Trees
     * zu erzeugen.
     * 
     * @param objects Szenen-Objekte, die anhand des Medians geteilt werden.
     * @param min Kleinste x-, y- und z-Koordinate des betrachteten Quaders.
     * @param max Gr��te x-, y- und z-Koordinate des betrachteten Quaders.
     * @param axisId ID der Achse, beginnend bei <code>0</code> f�r die x-Achse.
     * @param axisValue Wert an der Achse <code>axisId</code>, bei dem die
     *        Objekte aufgeteilt werden.
     * @param updateCount Gibt an, wie viele Schritte die Anzahl der Objekte
     *        im aktuellen Teilbaum bez�glich der Eltern-Teilb�ume unver�ndert
     *        geblieben ist.
     * @return <code>Node</code>-Objekt der Wurzel des erzeugten Baumes oder
     *         eine Menge von Szenen-Objekten, falls keine weitere Aufteilung
     *         statt findet.
     */
    private Object PartitionObjects(Shape objects[], float min[], float max[],
            byte axisId, float axisValue, int cutUpdateCount, int medianUpdateCount)
    {
        int duplicateCount = 0;
        List<Shape> lowerEqual = new ArrayList<Shape>(objects.length/2);
        List<Shape> higherEqual = new ArrayList<Shape>(objects.length/2);
        
        
        int count = objects.length;
        for (int i = 0; i < count; i++)
        {
            byte result = objects[i].compareAxis(axisId, axisValue);
            if ((int) result <= 0)
                lowerEqual.add(objects[i]);
            if ((int) result >= 0)
                higherEqual.add(objects[i]);
            
            
            if (result == 0)
                duplicateCount++;
        }
        
        
        Node node = new Node(axisId, axisValue);

        

        
        byte newAxisId = (byte)((axisId +1) % 3);

        float saved = max[axisId];
        max[((int) axisId)] = axisValue;
        int newCount = lowerEqual.size();
        node.left = CreateTree((newCount == count) ? objects : lowerEqual.toArray(new Shape[lowerEqual.size()]),
                min, max, newAxisId, (newCount == count) ? cutUpdateCount : 0,
                (newCount == count) ? medianUpdateCount : 0);
        max[((int) axisId)] = saved;
        
        saved = min[((int) axisId)];
        min[((int) axisId)] = axisValue;
        newCount = higherEqual.size();
        node.right = CreateTree((newCount == count) ? objects : higherEqual.toArray(new Shape[higherEqual.size()]),
                min, max, newAxisId, (newCount == count) ? cutUpdateCount : 0,
                (newCount == count) ? medianUpdateCount : 0);
        min[((int) axisId)] = saved;
        
        
        if (node.left == node.right)
        {
            
            
            return objects;
        }
        if ((!(node.left instanceof Node)) && (!(node.right instanceof Node)))
        {
            
            if ((float)duplicateCount/ (float) objects.length > MAX_DUPLICATE_RATIO)
                return objects;
        }
        
        
        return node;
    }
    
    
    
    /**
     * Findet eine Partition am gr��ten Zwischenraum.<br>
     * Die gefudnene Partition ist unter Umst�nden nicht optimal und muss nicht
     * unbedingt an einem Zwischenraum liegen.<br>
     * Dabei wird der unendliche Bereich an beiden Achsenenden unterst�tzt.<br>
     * <br>
     * In der letzten Komponente des Ergebnis-Vektors wird die Position des
     * Medians zur�ckgeliefert.
     * 
     * @param objects
     * @param minRange
     * @param maxRange
     * @param axisId
     */
    private static Vector3f FindPartition(Shape objects[],
            float minRange, float maxRange, byte axisId)
    {
        Arrays.sort(objects, new ShapeComparator(axisId));
        
        
        float length = Float.NEGATIVE_INFINITY;
        float axisValue = Float.NaN;
        
        float middle = (maxRange-minRange)/2.0f;
        
        float maxValue = minRange;
        for (int i = 0; i < objects.length; i++)
        {
            
            float minValue = (float) objects[i].minAxisValue(axisId);

            if ((maxValue < minValue) && (minValue < maxRange))
            {
                
                
                
                if (minValue-maxValue > length)
                {
                    length = minValue-maxValue;
                    if (FastMath.abs(middle-maxValue) < FastMath.abs(middle - minValue))
                        axisValue = maxValue+FloatingPoint.floatEpsilon(maxValue);
                    else
                        axisValue = minValue-FloatingPoint.floatEpsilon(minValue);
                }
            }
            
            
            maxValue = (float)objects[i].maxAxisValue(axisId);
        }
        if ((maxValue < maxRange) && (maxValue > minRange))
        {
            
            
            
            if (maxRange-maxValue > length)
            {
                length = maxRange-maxValue;
                axisValue = maxValue+FloatingPoint.floatEpsilon(maxValue);
            }
        }
        
        
        return new Vector3f(length, axisValue,
                (float)objects[objects.length/2].getCentroid(axisId));
    }
    
    
    /**
     * Ermittelt den Medien aus einer Menge von Szenen-Objekten entlang einer
     * Achse im Koordinatensystem.<br>
     * Der berechnete Median bezieht sich dabei auf die Schwerpunkte der
     * Szenen-Objekte. Falls kein eindeutiger Medien existiert, wird ein Wert
     * zur�ck geliefert, der m�glichst dicht in der N�he des Mediens liegt.
     * 
     * @param objects Szenen-Objekte, von denen der Median berechnet wird.
     * @param axisId ID der Achse, beginnend bei <code>0</code> f�r die x-Achse.
     * @return Ermittelter Median.
     */
    private static float FindMedian(Shape objects[], byte axisId)
    {
        int count = objects.length;     
        int k = count/2;                

        
        if (count < 1)
            throw new IllegalArgumentException();
        
        
        
        int beginIndex = 0;
        int endIndex = count - 1;
        float referenceValue;
        for (;;)
        {
            
            Shape reference = objects[beginIndex + (int) (Math.random() * (endIndex - beginIndex + 1))];
            referenceValue = (float)reference.getCentroid(axisId);
            
            
            int leftIndex = beginIndex;
            int rightIndex = endIndex;
            do
            {
                while ((float)objects[leftIndex].getCentroid(axisId) < referenceValue)
                    leftIndex++;
                while ((float)objects[rightIndex].getCentroid(axisId) > referenceValue)
                    rightIndex--;
                
                
                
                
                
                if (leftIndex > rightIndex)
                    break;
                
                
                Shape temp = objects[leftIndex];
                objects[leftIndex] = objects[rightIndex];
                objects[rightIndex] = temp;
                
                leftIndex++;
                rightIndex--;
            }
            while (leftIndex <= rightIndex);
            
            
            
            
            if (rightIndex+1 < k)       
            {
                
                beginIndex = leftIndex;
            }
            else if (rightIndex+1 > k)  
            {
                
                endIndex = rightIndex;
            }
            else break;                 
        }
        
        
        
        return referenceValue;
    }
    
    
    /**
     * Diese Datenstruktur stellt einen Knoten im KD-Tree dar.<br>
     * Falls Ein Knoten in einem Teilbaum ein Blatt hat, wird f�r das Blatt kein
     * neuer <code>Node</code> erzeugt. Stattdessen werden die Daten des Blattes
     * im jeweiligen Zeiger auf den Knoten gespeichert.
     * 
     * @author Mathias Kosch
     *
     */
    private static class Node
    {
        /** ID der Achse, beginnend bei <code>0</code> f�r die x-Achse. */
        public final byte axisId;
        /** Position der Trennebene. */
        public final float axisValue;

        public Node(byte axisId, float axisValue) {
            this.axisId = axisId;
            this.axisValue = axisValue;
        }

        /**
         * Zeiger auf den linken Teilbaum.<br>
         * Falls vom Typ <code>Node</code>, dann ist dieser Zeiger ein Verweis
         * auf den linken Teilbaum. Andernfalls ist dieser Zeiger ein Verweis
         * auf alle Szenen-Objekte im linken Teilbaum.
         */
        public Object left = null;
        /**
         * Zeiger auf den rechten Teilbaum.<br>
         * Falls vom Typ <code>Node</code>, dann ist dieser Zeiger ein Verweis
         * auf den rechten Teilbaum. Andernfalls ist dieser Zeiger ein Verweis
         * auf alle Szenen-Objekte im linken Teilbaum.
         */
        public Object right = null;
    }
    
    
    /**
     * Diese Datenstruktur speichert Informationen, die f�r die rekursive
     * Ermittlung eines Schnittpunktes mit einem Strahl notwendig sind.
     * 
     * @author Mathias Kosch
     *
     */
    private static class Recursion
    {
        /** Wenn <code>true</code>, wird nach dem ersten Schnitt gesucht. */
        public final boolean shortestIntersection;
        /** Gibt an, ob bereits ein Schnittpunnkt gefunden wurde. */
        public boolean intersection = false;
        
        /** Minimum-Begrenzung des Quaders an der x-, y- und z-Achse. */
        public final float[] min = {
                Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
            };
        /** Maximum-Begrenzung des Quaders an der x-, y- und z-Achse. */
        public final float[] max = {
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY,
            };
        
        /** Ursprung und Richtung des Strahls. */
        public final Ray ray;
        /** Ursprung des Strahls an der x-, y- und z-Achse. */
        public final double[] org;
        /** Richtung des Strahls in die x-, y- und z-Achse. */
        public final double[] dir;
        
        /** Tempor�rer Speicherplatz. */
        public final Vector3d temp = new Vector3d();


        /**
         * Erzeugt eine neue Datenstruktur vom Typ <code>Recursion</code>.
         * 
         * @param ray Strahl, auf den sich die Rekursion bezieht.
         * @param shortestIntersection Falls <code>true</code>, wird der n�chste
         *        Schnittpunkt bestimmt. Andernfalls wird irgendein Schnittpunkt
         *        bestimmt.
         */
        public Recursion(Ray ray, boolean shortestIntersection)
        {
            
            this.ray = ray;
            
            
            org = new double[3];
            org[0] = ray.org.x;
            org[1] = ray.org.y;
            org[2] = ray.org.z;
            
            
            dir = new double[3];
            dir[0] = ray.dir.x;
            dir[1] = ray.dir.y;
            dir[2] = ray.dir.z;
            
            this.shortestIntersection = shortestIntersection;
        }
    }
    
    
    /**
     * Dieses Objekt wird mittels <code>throw</code> geworfen, sobald ein
     * Ergebnis vorliegt.<br>
     * Dadurch wird es m�glich, Ergebnisse aus einer tiefen, verschachtelten
     * Rekursionsebene heraus einfach nach Au�en zu tragen.
     * 
     * @author Mathias Kosch
     *
     */
    private static class Result extends Throwable
    {
        /** Serielle Standardversions-ID. */
        private static final long serialVersionUID = 1L;

        public Result() {
            super();
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this; 
        }
    }
    
    
    private static class ShapeComparator implements Comparator<Shape>
    {
        protected final byte axisId;
        
        
        public ShapeComparator(byte axisId)
        {
            this.axisId = axisId;
        }
        
        
        @Override
        public int compare(final Shape o1, final Shape o2)
        {
            final double centroid1 = o1.getCentroid(axisId);
            final double centroid2 = o2.getCentroid(axisId);
            return (centroid1 < centroid2) ? -1 : (centroid1 > centroid2) ? 1 : 0;
        }
        
        public boolean equals(final Object obj)
        {
            if (!(obj instanceof ShapeComparator))
                return false;
            return axisId == ((ShapeComparator)obj).axisId;
        }
    }
    
    
    /*public void test()
    {
        Partition partition = new Partition();
        
        SceneObject objects[] = finiteObjects.toArray(new SceneObject[0]);
        PartitionMedian(objects, (byte)0, partition);

        System.out.println("Median: " + partition.axisValue);
        System.out.println();
        System.out.println("Anzahl Objekte:   " + objects.length);
        System.out.println("Kleinere Objekte: " + partition.lowerEqual.length);
        System.out.println("Gr��ere Objekte:  " + partition.higherEqual.length);
        
        
        CreateTree();
        
        
        
        
        Object node = root;
        while (node instanceof Node)
        {
            node = ((Node)node).left;
        }
        
        System.out.println(((SceneObject[])node).length);
    }

    public Vector2d test(Recursion r)
    {
        boolean intersect = false;
        double t;
        double t_min = Double.POSITIVE_INFINITY;
        double t_max = Double.NEGATIVE_INFINITY;
        Vector3d v = new Vector3d();
        
        t = (r.min[0]-r.org[0])/r.dir[0];
        v.scaleAdd(t, r.ray.dir, r.ray.org);
        if ((v.y >= r.min[1]) && (v.y <= r.max[1]) && (v.z >= r.min[2]) && (v.z <= r.max[2]))
        {
            intersect = true;
            if ((t < t_min) && (t >= 0.0)) t_min = t;
            if ((t > t_max) && (t >= 0.0)) t_max = t;
        }
        t = (r.max[0]-r.org[0])/r.dir[0];
        v.scaleAdd(t, r.ray.dir, r.ray.org);
        if ((v.y >= r.min[1]) && (v.y <= r.max[1]) && (v.z >= r.min[2]) && (v.z <= r.max[2]))
        {
            intersect = true;
            if ((t < t_min) && (t >= 0.0)) t_min = t;
            if ((t > t_max) && (t >= 0.0)) t_max = t;
        }
        
        t = (r.min[1]-r.org[1])/r.dir[1];
        v.scaleAdd(t, r.ray.dir, r.ray.org);
        if ((v.x >= r.min[0]) && (v.x <= r.max[0]) && (v.z >= r.min[2]) && (v.z <= r.max[2]))
        {
            intersect = true;
            if ((t < t_min) && (t >= 0.0)) t_min = t;
            if ((t > t_max) && (t >= 0.0)) t_max = t;
        }
        t = (r.max[1]-r.org[1])/r.dir[1];
        v.scaleAdd(t, r.ray.dir, r.ray.org);
        if ((v.x >= r.min[0]) && (v.x <= r.max[0]) && (v.z >= r.min[2]) && (v.z <= r.max[2]))
        {
            intersect = true;
            if ((t < t_min) && (t >= 0.0)) t_min = t;
            if ((t > t_max) && (t >= 0.0)) t_max = t;
        }
        
        t = (r.min[2]-r.org[2])/r.dir[2];
        v.scaleAdd(t, r.ray.dir, r.ray.org);
        if ((v.y >= r.min[1]) && (v.y <= r.max[1]) && (v.x >= r.min[0]) && (v.x <= r.max[0]))
        {
            intersect = true;
            if ((t < t_min) && (t >= 0.0)) t_min = t;
            if ((t > t_max) && (t >= 0.0)) t_max = t;
        }
        t = (r.max[2]-r.org[2])/r.dir[2];
        v.scaleAdd(t, r.ray.dir, r.ray.org);
        if ((v.y >= r.min[1]) && (v.y <= r.max[1]) && (v.x >= r.min[0]) && (v.x <= r.max[0]))
        {
            intersect = true;
            if ((t < t_min) && (t >= 0.0)) t_min = t;
            if ((t > t_max) && (t >= 0.0)) t_max = t;
        }
        
        return intersect ? new Vector2d(t_min, t_max) : null;
    }*/    
}