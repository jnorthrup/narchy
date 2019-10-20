/*
 * C# / XNA  port of Bullet (c) 2011 Mark Neale <xexuxjy@hotmail.com>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http:
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.space3d.phys.shape;

import jcog.math.v3;
import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.Matrix3f;

import java.nio.ByteBuffer;

public class HeightfieldTerrainShape extends ConcaveShape
{
	public enum PHY_ScalarType
	{
		PHY_FLOAT, PHY_UCHAR, PHY_SHORT
	}

	private v3 m_localAabbMin;
	private v3 m_localAabbMax;
	private v3 m_localOrigin;
	private v3 m_localScaling;

	
	private int m_heightStickWidth;
	private int m_heightStickLength;
	private float m_minHeight;
	private float m_maxHeight;
	private float m_width;
	private float m_length;
	private float m_heightScale;

	private byte[] m_heightFieldDataByte;
	private float[] m_heightFieldDataFloat;

	private PHY_ScalarType m_heightDataType;
	private boolean m_flipQuadEdges;
	private boolean m_useDiamondSubdivision;

	private int m_upAxis;

	
	/**
	  This constructor supports a range of heightfield
	  data types, and allows for a non-zero minimum height value.
	  heightScale is needed for any integer-based heightfield data types.
	 */
	public HeightfieldTerrainShape(int heightStickWidth, int heightStickLength, byte[] heightfieldData,
			float heightScale, float minHeight, float maxHeight, int upAxis, PHY_ScalarType heightDataType,
			boolean flipQuadEdges)
	{
		initialize(heightStickWidth, heightStickLength, heightfieldData, heightScale, minHeight, maxHeight, upAxis,
				heightDataType, flipQuadEdges);
	}

	
	/**
	  The legacy constructor assumes the heightfield has a minimum height
	  of zero.  Only unsigned char or floats are supported.  For legacy
	  compatibility reasons, heightScale is calculated as maxHeight / 65535 
	  (and is only used when useFloatData = false).
	 */
	public HeightfieldTerrainShape(int heightStickWidth, int heightStickLength, byte[] heightfieldData,
			float maxHeight, int upAxis, boolean useFloatData, boolean flipQuadEdges)
	{


        PHY_ScalarType hdt = (useFloatData) ? PHY_ScalarType.PHY_FLOAT : PHY_ScalarType.PHY_UCHAR;
        float minHeight = 0.0f;


        float heightScale = maxHeight / 65535.0F;

		initialize(heightStickWidth, heightStickLength, heightfieldData, heightScale, minHeight, maxHeight, upAxis,
				hdt, flipQuadEdges);

	}

	public HeightfieldTerrainShape(int heightStickWidth, int heightStickLength, float[] heightfieldData,
			float heightScale, float minHeight, float maxHeight, int upAxis, boolean flipQuadEdges)
	{
		initialize(heightStickWidth, heightStickLength, heightfieldData, heightScale, minHeight, maxHeight, upAxis,
				PHY_ScalarType.PHY_FLOAT, flipQuadEdges);
	}

	private float GetRawHeightFieldValue(int x, int y)
	{
        float val = 0f;
		switch (m_heightDataType)
		{
		case PHY_FLOAT:
            if (m_heightFieldDataFloat != null)
            {

                int index = ((y * m_heightStickWidth) + x);
                val = m_heightFieldDataFloat[index];
                break;
            }
            else
            {


                int index = ((y * m_heightStickWidth) + x) * 4;


                int size = 4;
                ByteBuffer bb = ByteBuffer.allocate(size).put(m_heightFieldDataByte, index, size);
                bb.position(0);
                val = bb.getFloat();
                break;
            }
            case PHY_UCHAR:
                byte heightFieldValue = m_heightFieldDataByte[(y * m_heightStickWidth) + x];
            val = (float) heightFieldValue * m_heightScale;
            break;

            case PHY_SHORT:

                int index = ((y * m_heightStickWidth) + x) * 2;
            short hfValue = (short) 0;
            val = (float) hfValue * m_heightScale;
            break;
        }

		return val;
	}

	private void quantizeWithClamp(int[] output, v3 point, int isMax)
	{
		
		/**
		  This routine is basically determining the gridpoint indices for a given
		  input vector, answering the question: "which gridpoint is closest to the
		  provided point?".

		  "with clamp" means that we restrict the point to be in the heightfield's
		  axis-aligned bounding box.
		 */

        v3 clampedPoint = new v3();
		clampedPoint.set(point.x, point.y, point.z);
		VectorUtil.setMax(clampedPoint, m_localAabbMin);
		VectorUtil.setMin(clampedPoint, m_localAabbMax);

		output[0] = getQuantized(clampedPoint.x);
		output[1] = getQuantized(clampedPoint.y);
		output[2] = getQuantized(clampedPoint.z);
	}

	private static int getQuantized(float x)
	{
		if (x < 0.0f)
		{
			return (int) ((double) x - 0.5);
		}
		return (int) ((double) x + 0.5);
	}

	private void getVertex(int x, int y, v3 vertex)
	{
		assert (x >= 0);
		assert (y >= 0);
		assert (x < m_heightStickWidth);
		assert (y < m_heightStickLength);

        float height = GetRawHeightFieldValue(x, y);

		switch (m_upAxis)
		{
		case 0:
            vertex.set(height - m_localOrigin.x, (-m_width / 2f) + (float) x, (-m_length / 2f) + (float) y);
            break;
            case 1:
            vertex.set((-m_width / 2f) + (float) x, height - m_localOrigin.y, (-m_length / 2f) + (float) y);
            break;
            case 2:
            vertex.set((-m_width / 2f) + (float) x, (-m_length / 2f) + (float) y, height - m_localOrigin.z);
            break;
            default:
            
            assert (false);
            vertex.set(0f, 0f, 0f);
            break;
        }

		VectorUtil.mul(vertex, vertex, m_localScaling);
	}

	@Override
    public BroadphaseNativeType getShapeType()
	{
		return BroadphaseNativeType.TERRAIN_SHAPE_PROXYTYPE;
	}

	
	/**
	  Handles the work of constructors so that public constructors can be
	  backwards-compatible without a lot of copy/paste.
	 */
    private void initialize(int heightStickWidth, int heightStickLength, Object heightfieldData, float heightScale,
                            float minHeight, float maxHeight, int upAxis, PHY_ScalarType hdt, boolean flipQuadEdges)
	{
		
		assert heightStickWidth > 1 : "bad width";
		assert heightStickLength > 1 : "bad length";
		assert heightfieldData != null : "null heightfield data";
		
		assert minHeight <= maxHeight : "bad min/max height";
		assert upAxis >= 0 && upAxis < 3 : "bad upAxis--should be in range [0,2]";
		//assert hdt != PHY_ScalarType.PHY_UCHAR || hdt != PHY_ScalarType.PHY_FLOAT || hdt != PHY_ScalarType.PHY_SHORT : "Bad height data type enum";

		

		m_heightStickWidth = heightStickWidth;
		m_heightStickLength = heightStickLength;
		m_minHeight = minHeight;
		m_maxHeight = maxHeight;
		m_width = (float) (heightStickWidth - 1);
		m_length = (float) (heightStickLength - 1);
		m_heightScale = heightScale;
		
		if (heightfieldData instanceof byte[])
		{
			m_heightFieldDataByte = (byte[]) (heightfieldData);
		}
		else if (heightfieldData instanceof float[])
		{
			m_heightFieldDataFloat = (float[]) (heightfieldData);
		}
		m_heightDataType = hdt;

		m_flipQuadEdges = flipQuadEdges;
		m_useDiamondSubdivision = false;
		m_upAxis = upAxis;

		m_localScaling = new v3();
		m_localScaling.set(1f, 1f, 1f);
		

		m_localAabbMin = new v3();
		m_localAabbMax = new v3();
		switch (m_upAxis)
		{
		case 0:
            m_localAabbMin.set(m_minHeight, (float) 0, (float) 0);
            m_localAabbMax.set(m_maxHeight, m_width, m_length);
            break;
            case 1:
            m_localAabbMin.set((float) 0, m_minHeight, (float) 0);
            m_localAabbMax.set(m_width, m_maxHeight, m_length);
            break;
            case 2:
            m_localAabbMin.set((float) 0, (float) 0, m_minHeight);
            m_localAabbMax.set(m_width, m_length, m_maxHeight);
            break;
            default:
            
            assert false : "Bad m_upAxis";
            break;
        }

		
		m_localOrigin = new v3();
		m_localOrigin.set((float) 0, (float) 0, (float) 0);
		VectorUtil.add(m_localOrigin, m_localAabbMin, m_localAabbMax);
		VectorUtil.mul(m_localOrigin, m_localOrigin, 0.5f);

		for (int i = 0; i < vertices.length; ++i)
		{
			vertices[i] = new v3();
		}

	}

	public void setUseDiamondSubdivision(boolean useDiamondSubdivision)
	{
		m_useDiamondSubdivision = useDiamondSubdivision;
	}

	@Override
	public void getAabb(Transform trans, v3 aabbMin, v3 aabbMax)
	{
        v3 tmp = new v3();

        v3 localHalfExtents = new v3();
		localHalfExtents.sub(m_localAabbMax, m_localAabbMin);
		VectorUtil.mul(localHalfExtents,localHalfExtents,m_localScaling);
		
		localHalfExtents.scaled(0.5f);

        v3 localOrigin = new v3();
		localOrigin.set(0f,0f,0f);
		VectorUtil.setCoord(localOrigin,m_upAxis,(m_minHeight + m_maxHeight)*0.5f );
		VectorUtil.mul(localOrigin,localOrigin,m_localScaling);

        Matrix3f abs_b = new Matrix3f(trans.basis);
		MatrixUtil.absolute(abs_b);

        v3 center = new v3(trans);
        v3 extent = new v3();
		abs_b.getRow(0, tmp);
		extent.x = tmp.dot(localHalfExtents);
		abs_b.getRow(1, tmp);
		extent.y = tmp.dot(localHalfExtents);
		abs_b.getRow(2, tmp);
		extent.z = tmp.dot(localHalfExtents);

        v3 margin = new v3();
		margin.set(getMargin(), getMargin(), getMargin());
		extent.add(margin);

		aabbMin.sub(center, extent);
		aabbMax.add(center, extent);
	}

	
	/**
	  basic algorithm:
	    - convert input aabb to local coordinates (scale down and shift for local origin)
	    - convert input aabb to a range of heightfield grid points (quantize)
	    - iterate over all triangles in that subset of the grid
	 */

    private final int[] quantizedAabbMin = new int[3];
	private final int[] quantizedAabbMax = new int[3];
	private final v3[] vertices = new v3[3];

	private static void checkNormal(v3[] vertices1, TriangleCallback callback)
	{

        v3 tmp1 = new v3();
        v3 tmp2 = new v3();
        v3 normal = new v3();

		tmp1.sub(vertices1[1], vertices1[0]);
		tmp2.sub(vertices1[2], vertices1[0]);

		normal.cross(tmp1, tmp2);
		normal.normalize();

	}

	@Override
    public void processAllTriangles(TriangleCallback callback, v3 aabbMin, v3 aabbMax)
	{


        v3 invScale = new v3();
		invScale.set(1f / m_localScaling.x, 1f / m_localScaling.y, 1f / m_localScaling.z);

        v3 localAabbMin = new v3();
        v3 localAabbMax = new v3();

		VectorUtil.mul(localAabbMin, aabbMin, invScale);
		VectorUtil.mul(localAabbMax, aabbMax, invScale);

		
		VectorUtil.add(localAabbMin, localAabbMin, m_localOrigin);
		VectorUtil.add(localAabbMax, localAabbMax, m_localOrigin);

		quantizeWithClamp(quantizedAabbMin, localAabbMin, 0);
		quantizeWithClamp(quantizedAabbMax, localAabbMax, 1);

		
		
		for (int i = 0; i < 3; ++i)
		{
			quantizedAabbMin[i]--;
			quantizedAabbMax[i]++;
		}

        int startX = 0;
        int endX = m_heightStickWidth - 1;
        int startJ = 0;
        int endJ = m_heightStickLength - 1;

		switch (m_upAxis)
		{
		case 0:
            if (quantizedAabbMin[1] > startX)
                startX = quantizedAabbMin[1];
            if (quantizedAabbMax[1] < endX)
                endX = quantizedAabbMax[1];
            if (quantizedAabbMin[2] > startJ)
                startJ = quantizedAabbMin[2];
            if (quantizedAabbMax[2] < endJ)
                endJ = quantizedAabbMax[2];
            break;
            case 1:
            if (quantizedAabbMin[0] > startX)
                startX = quantizedAabbMin[0];
            if (quantizedAabbMax[0] < endX)
                endX = quantizedAabbMax[0];
            if (quantizedAabbMin[2] > startJ)
                startJ = quantizedAabbMin[2];
            if (quantizedAabbMax[2] < endJ)
                endJ = quantizedAabbMax[2];
            break;
            case 2:
            if (quantizedAabbMin[0] > startX)
                startX = quantizedAabbMin[0];
            if (quantizedAabbMax[0] < endX)
                endX = quantizedAabbMax[0];
            if (quantizedAabbMin[1] > startJ)
                startJ = quantizedAabbMin[1];
            if (quantizedAabbMax[1] < endJ)
                endJ = quantizedAabbMax[1];
            break;
            default:
            
            assert (false);
            break;
        }

		
		for (int j = startJ; j < endJ; j++)
		{
			for (int x = startX; x < endX; x++)
			{
				if (m_flipQuadEdges || (m_useDiamondSubdivision && (((j + x) & 1) > 0)))
				{
					
					getVertex(x, j, vertices[0]);
					getVertex(x + 1, j, vertices[1]);
					getVertex(x + 1, j + 1, vertices[2]);
					callback.processTriangle(vertices, x, j);
					
					getVertex(x, j, vertices[0]);
					getVertex(x + 1, j + 1, vertices[1]);
					getVertex(x, j + 1, vertices[2]);

				}
				else
				{
					
					getVertex(x, j, vertices[0]);
					getVertex(x, j + 1, vertices[1]);
					getVertex(x + 1, j, vertices[2]);
					checkNormal(vertices, callback);
					callback.processTriangle(vertices, x, j);

					
					getVertex(x + 1, j, vertices[0]);
					getVertex(x, j + 1, vertices[1]);
					getVertex(x + 1, j + 1, vertices[2]);
					checkNormal(vertices, callback);


				}
				callback.processTriangle(vertices, x, j);
			}
		}
	}

	@Override
    public void calculateLocalInertia(float mass, v3 inertia)
	{
		
		inertia.set(0f, 0f, 0f);
	}

	@Override
    public void setLocalScaling(v3 scaling)
	{
		m_localScaling.set(scaling);
	}

	@Override
    public v3 getLocalScaling(v3 localScaling)
	{
		localScaling.set(m_localScaling);
		return localScaling;
	}

	
	@Override
    public String getName()
	{
		return "HEIGHTFIELD";
	}
}
