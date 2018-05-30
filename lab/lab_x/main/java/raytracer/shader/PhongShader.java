/*
 * PhongShader.java                       STATUS: Vorl�ufig abgeschlossen
 * ----------------------------------------------------------------------
 * 
 */

package raytracer.shader;

import raytracer.basic.ColorEx;
import raytracer.basic.Intersection;
import raytracer.basic.Ray;
import raytracer.basic.RaytracerConstants;
import raytracer.lights.Light;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import java.util.List;

/**
 * Dieser Shader arbeitet nach dem Phong-Blinn-Modell und ber�cksichtigt dabei
 * Lichter und Schatten.
 * 
 * @author Sassan Torabi-Goudarzi
 * 
 */
public class PhongShader implements Shader
{
    /** Haupt-Shader, der die Farbwerte liefert. */
    protected final Shader mainShader;
    
    /** Anteil des ambienten Lichts, den das Objekt reflektiert. */
    protected final Vector3f ambientRatio = new Vector3f();
    /** Anteil des diffusen Lichts, den das Objekt reflektiert. */
    protected final Vector3f diffuseRatio = new Vector3f();
    /** Anteil des spekularen Lichts, den das Objekt reflektiert. */
    protected final Vector3f specularRatio = new Vector3f();
    /** Anteil des emissiven Lichts, den das Objekt reflektiert. */
    protected final Vector3f emissionRatio = new Vector3f();
    /** Shininess-Parameter. */
    protected final float shininess;
	
	
    /**
     * Erzeugt einen neuen <code>PhongShader</code>.
     * 
     * @param mainShader Haupt-Shader, der die Farbwerte liefert.
     * @param ambientRatio Ambienter Anteil am Farbwert des Objekts.
     * @param diffuseRatio Diffuser Anteil am Farbwert des Objekts.
     * @param specularRatio Spekularer Anteil am Farbwert des Objekts.
     */
    public PhongShader(Shader mainShader, Vector3f ambientRatio,
            Vector3f diffuseRatio, Vector3f specularRatio)
    {
        this.mainShader = mainShader;
        
        
        this.ambientRatio.set(ambientRatio);
        this.diffuseRatio.set(diffuseRatio);
        this.specularRatio.set(specularRatio);
        emissionRatio.x = 0.0f;
        emissionRatio.y = 0.0f;
        emissionRatio.z = 0.0f;
        this.shininess = 1.0f;
    }
    
    
    /**
     * Erzeugt einen neuen <code>PhongShader</code>.
     * 
     * @param mainShader Haupt-Shader, der die Farbwerte liefert.
     * @param ambientRatio Ambienter Anteil am Farbwert des Objekts.
     * @param diffuseRatio Diffuser Anteil am Farbwert des Objekts.
     * @param specularRatio Spekularer Anteil am Farbwert des Objekts.
     * @param emissionRatio Emissiver Anteil am Farbwert des Objekts.
     */
    public PhongShader(Shader mainShader, Vector3f ambientRatio,
            Vector3f diffuseRatio, Vector3f specularRatio, Vector3f emissionRatio)
    {
        this.mainShader = mainShader;
        
        
        this.ambientRatio.set(ambientRatio);
        this.diffuseRatio.set(diffuseRatio);
        this.specularRatio.set(specularRatio);
        this.emissionRatio.set(emissionRatio);
        this.shininess = 1.0f;
    }
    
    
    /**
     * Erzeugt einen neuen <code>PhongShader</code>.
     * 
     * @param mainShader Haupt-Shader, der die Farbwerte liefert.
     * @param ambientRatio Ambienter Anteil am Farbwert des Objekts.
     * @param diffuseRatio Diffuser Anteil am Farbwert des Objekts.
     * @param specularRatio Spekularer Anteil am Farbwert des Objekts.
     * @param emissionRatio Emissiver Anteil am Farbwert des Objekts.
     * @param shininess Shininess-Parameter.
     */
    public PhongShader(Shader mainShader, Vector3f ambientRatio,
            Vector3f diffuseRatio, Vector3f specularRatio, Vector3f emissionRatio,
            float shininess)
    {
        this.mainShader = mainShader;
        
        
        this.ambientRatio.set(ambientRatio);
        this.diffuseRatio.set(diffuseRatio);
        this.specularRatio.set(specularRatio);
        this.emissionRatio.set(emissionRatio);
        this.shininess = shininess;
    }
    
    
	@Override
    public ColorEx shade(Intersection intersection)
	{
        
        ColorEx color = mainShader.shade(intersection);

        Vector3d eyelight = new Vector3d(intersection.ray.dir);
        Vector3d normal = intersection.getNormal();     
        Vector3d point = intersection.getPoint();
        ColorEx result = new ColorEx(), lightResult = new ColorEx();

        
        
        byte sign = (byte)-Math.signum(normal.dot(eyelight));
        
        
        eyelight.normalize();

        List<Light> lights = intersection.scene.getLights();
        final int numLights = lights.size();
        for (int i = 0; i < numLights; i++) {
            final Light light = lights.get(i);
            if (!light.isIlluminated(point))
                continue;
            light.startRay(point);
        	
            
            int lightRayCount = 0;
            lightResult.x = 0.0f; lightResult.y = 0.0f; lightResult.z = 0.0f;
            boolean noshadow;
            boolean shadow = noshadow = false;
            for (;;)
            {
                Ray lightRay = light.genRay();
                if (lightRay == null)
                {
                    if ((!shadow) || (!noshadow))
                        break;
                    shadow = noshadow = false;
                    
                    
                    
                    light.increaseNumberOfRays();
                    continue;
                }
                
                lightRayCount++;
                
                
                
        		if (intersection.scene.occlude(lightRay))
                {
                    shadow = true;
        			continue;
                }
                else
                    noshadow = true;
        		
        		
                lightRay.dir.normalize();
        		
        		
                float cosalpha = (float) (sign * normal.dot(lightRay.dir));
                float cosbeta;
                if ((double) cosalpha <= 0.0)
                {
                    cosalpha = 0.0f;
                    cosbeta = 0.0f;
                }
                else
                {
            		eyelight.sub(lightRay.dir);
            		cosbeta = (float)((double) -(int) sign *normal.dot(eyelight)/eyelight.length());
            		eyelight.add(lightRay.dir);
            		
                    
                    if (shininess != 1.0f)
                        cosbeta = (float)Math.pow((double) cosbeta, (double) shininess);
                }
                
                
                ColorEx lightColor = light.getIlluminance();
                lightResult.mul2Add(cosalpha, diffuseRatio, lightColor);
                lightResult.mul2Add(cosbeta, specularRatio, lightColor);   
        	}
            result.scaleAdd(1.0f/ (float) lightRayCount, lightResult, result);
        }
    
        
        result.add(emissionRatio);
        result.mul2Add(ambientRatio, intersection.scene.getAmbientLight());
        result.mul(color);
        
        
        if (RaytracerConstants.LIMIT_COLOR_INTENSITY)
            result.clampMax(1.0f);
		return result;
	}
}