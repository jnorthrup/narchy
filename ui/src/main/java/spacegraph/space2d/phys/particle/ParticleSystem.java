package spacegraph.space2d.phys.particle;

import jcog.math.v2;
import spacegraph.space2d.phys.callbacks.ParticleDestructionListener;
import spacegraph.space2d.phys.callbacks.ParticleQueryCallback;
import spacegraph.space2d.phys.callbacks.ParticleRaycastCallback;
import spacegraph.space2d.phys.collision.AABB;
import spacegraph.space2d.phys.collision.RayCastInput;
import spacegraph.space2d.phys.collision.RayCastOutput;
import spacegraph.space2d.phys.collision.shapes.Shape;
import spacegraph.space2d.phys.common.*;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.phys.dynamics.Fixture;
import spacegraph.space2d.phys.dynamics.TimeStep;
import spacegraph.space2d.phys.dynamics.contacts.Position;
import spacegraph.space2d.phys.dynamics.contacts.Velocity;
import spacegraph.space2d.phys.particle.VoronoiDiagram.VoronoiDiagramCallback;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Predicate;

public class ParticleSystem {
    /**
     * All particle types that require creating pairs
     */
    private static final int k_pairFlags = ParticleType.b2_springParticle;
    /**
     * All particle types that require creating triads
     */
    private static final int k_triadFlags = ParticleType.b2_elasticParticle;
    /**
     * All particle types that require computing depth
     */
    private static final int k_noPressureFlags = ParticleType.b2_powderParticle;

    private static final int xTruncBits = 12;
    private static final int yTruncBits = 12;
    private static final int tagBits = 8 * 4 - 1  /* sizeof(int) */;
    private static final long yOffset = 1 << (yTruncBits - 1);
    private static final int yShift = tagBits - yTruncBits;
    private static final int xShift = tagBits - yTruncBits - xTruncBits;
    private static final long xScale = 1 << xShift;
    private static final long xOffset = xScale * (1 << (xTruncBits - 1));
    static final int xMask = (1 << xTruncBits) - 1;
    static final int yMask = (1 << yTruncBits) - 1;

    private static long computeTag(float x, float y) {
        return (((long) (y + yOffset)) << yShift) + (((long) (xScale * x)) + xOffset);
    }

    private static long computeRelativeTag(long tag, int x, int y) {
        return tag + (y << yShift) + (x << xShift);
    }

    private static int limitCapacity(int capacity, int maxCount) {
        return maxCount != 0 && capacity > maxCount ? maxCount : capacity;
    }

    int m_timestamp;
    private int m_allParticleFlags;
    private int m_allGroupFlags;
    private float m_density;
    private float m_inverseDensity;
    private float m_gravityScale;
    private float m_particleDiameter;
    private float m_inverseDiameter;
    private float m_squaredDiameter;

    private int m_count;
    private int m_internalAllocatedCapacity;
    private int m_maxCount;
    private final ParticleBufferInt m_flagsBuffer;
    final ParticleBuffer<Position> m_positionBuffer;
    final ParticleBuffer<Velocity> m_velocityBuffer;
    private float[] m_accumulationBuffer;
    private Position[] m_accumulation2Buffer;
    private float[] m_depthBuffer;

    public final ParticleBuffer<ParticleColor> m_colorBuffer;
    private ParticleGroup[] m_groupBuffer;
    private final ParticleBuffer<Object> m_userDataBuffer;

    private int m_proxyCount;
    private int m_proxyCapacity;
    private Proxy[] m_proxyBuffer;

    public int m_contactCount;
    private int m_contactCapacity;
    public ParticleContact[] m_contactBuffer;

    public int m_bodyContactCount;
    private int m_bodyContactCapacity;
    public ParticleBodyContact[] m_bodyContactBuffer;

    private int m_pairCount;
    private int m_pairCapacity;
    private Pair[] m_pairBuffer;

    private int m_triadCount;
    private int m_triadCapacity;
    private Triad[] m_triadBuffer;

    private int m_groupCount;
    private ParticleGroup m_groupList;

    private final float m_pressureStrength;
    private float m_dampingStrength;
    private final float m_elasticStrength;
    private final float m_springStrength;
    private final float m_viscousStrength;
    private final float m_surfaceTensionStrengthA;
    private final float m_surfaceTensionStrengthB;
    private final float m_powderStrength;
    private final float m_ejectionStrength;
    private final float m_colorMixingStrength;

    private final Dynamics2D m_world;

    public ParticleSystem(Dynamics2D world) {
        m_world = world;
        m_timestamp = 0;
        m_allParticleFlags = 0;
        m_allGroupFlags = 0;
        m_density = 1;
        m_inverseDensity = 1;
        m_gravityScale = 1;
        m_particleDiameter = 1;
        m_inverseDiameter = 1;
        m_squaredDiameter = 1;

        m_count = 0;
        m_internalAllocatedCapacity = 0;
        m_maxCount = 0;

        m_proxyCount = 0;
        m_proxyCapacity = 0;

        m_contactCount = 0;
        m_contactCapacity = 0;

        m_bodyContactCount = 0;
        m_bodyContactCapacity = 0;

        m_pairCount = 0;
        m_pairCapacity = 0;

        m_triadCount = 0;
        m_triadCapacity = 0;

        m_groupCount = 0;

        m_pressureStrength = 0.05f;
        m_dampingStrength = 1.0f;
        m_elasticStrength = 0.25f;
        m_springStrength = 0.25f;
        m_viscousStrength = 0.25f;
        m_surfaceTensionStrengthA = 0.1f;
        m_surfaceTensionStrengthB = 0.2f;
        m_powderStrength = 0.5f;
        m_ejectionStrength = 0.5f;
        m_colorMixingStrength = 0.5f;

        m_flagsBuffer = new ParticleBufferInt();
        m_positionBuffer = new ParticleBuffer<>(Position.class);
        m_velocityBuffer = new ParticleBuffer<>(Velocity.class);
        m_colorBuffer = new ParticleBuffer<>(ParticleColor.class);
        m_userDataBuffer = new ParticleBuffer<>(Object.class);
    }











    public int createParticle(ParticleDef def) {
        if (m_count >= m_internalAllocatedCapacity) {
            var capacity = m_count != 0 ? 2 * m_count : Settings.minParticleBufferCapacity;
            capacity = limitCapacity(capacity, m_maxCount);
            capacity = limitCapacity(capacity, m_flagsBuffer.userSuppliedCapacity);
            capacity = limitCapacity(capacity, m_positionBuffer.userSuppliedCapacity);
            capacity = limitCapacity(capacity, m_velocityBuffer.userSuppliedCapacity);
            capacity = limitCapacity(capacity, m_colorBuffer.userSuppliedCapacity);
            capacity = limitCapacity(capacity, m_userDataBuffer.userSuppliedCapacity);
            if (m_internalAllocatedCapacity < capacity) {
                m_flagsBuffer.data =
                        reallocateBuffer(m_flagsBuffer, m_internalAllocatedCapacity, capacity, false);
                m_positionBuffer.data =
                        reallocateBuffer(m_positionBuffer, m_internalAllocatedCapacity, capacity, false);
                m_velocityBuffer.data =
                        reallocateBuffer(m_velocityBuffer, m_internalAllocatedCapacity, capacity, false);
                m_accumulationBuffer =
                        BufferUtils.reallocateBuffer(m_accumulationBuffer, 0, m_internalAllocatedCapacity,
                                capacity, false);
                m_accumulation2Buffer =
                        BufferUtils.reallocateBuffer(Position.class, m_accumulation2Buffer, 0,
                                m_internalAllocatedCapacity, capacity, true);
                m_depthBuffer =
                        BufferUtils.reallocateBuffer(m_depthBuffer, 0, m_internalAllocatedCapacity, capacity,
                                true);
                m_colorBuffer.data =
                        reallocateBuffer(m_colorBuffer, m_internalAllocatedCapacity, capacity, true);
                m_groupBuffer =
                        BufferUtils.reallocateBuffer(ParticleGroup.class, m_groupBuffer, 0,
                                m_internalAllocatedCapacity, capacity, false);
                m_userDataBuffer.data =
                        reallocateBuffer(m_userDataBuffer, m_internalAllocatedCapacity, capacity, true);
                m_internalAllocatedCapacity = capacity;
            }
        }
        if (m_count >= m_internalAllocatedCapacity) {
            return Settings.invalidParticleIndex;
        }
        var index = m_count++;
        m_flagsBuffer.data[index] = def.flags;
        m_positionBuffer.data[index].set(def.position);

        m_velocityBuffer.data[index].set(def.velocity);
        m_groupBuffer[index] = null;
        if (m_depthBuffer != null) {
            m_depthBuffer[index] = 0;
        }
        if (m_colorBuffer.data != null || def.color != null) {
            m_colorBuffer.data = requestParticleBuffer(m_colorBuffer.dataClass, m_colorBuffer.data);
            m_colorBuffer.data[index].set(def.color);
        }
        if (m_userDataBuffer.data != null || def.userData != null) {
            m_userDataBuffer.data =
                    requestParticleBuffer(m_userDataBuffer.dataClass, m_userDataBuffer.data);
            m_userDataBuffer.data[index] = def.userData;
        }
        if (m_proxyCount >= m_proxyCapacity) {
            var oldCapacity = m_proxyCapacity;
            var newCapacity = m_proxyCount != 0 ? 2 * m_proxyCount : Settings.minParticleBufferCapacity;
            m_proxyBuffer =
                    BufferUtils.reallocateBuffer(Proxy.class, m_proxyBuffer, oldCapacity, newCapacity);
            m_proxyCapacity = newCapacity;
        }
        m_proxyBuffer[m_proxyCount++].index = index;
        return index;
    }

    public void destroyParticle(int index, boolean callDestructionListener) {
        var flags = ParticleType.b2_zombieParticle;
        if (callDestructionListener) {
            flags |= ParticleType.b2_destructionListener;
        }
        m_flagsBuffer.data[index] |= flags;
    }

    private final AABB temp = new AABB();
    private final DestroyParticlesInShapeCallback dpcallback = new DestroyParticlesInShapeCallback();

    public int destroyParticlesInShape(Shape shape, Transform xf, boolean callDestructionListener) {
        dpcallback.init(this, shape, xf, callDestructionListener);
        shape.computeAABB(temp, xf, 0);
        m_world.queryAABB(dpcallback, temp);
        return dpcallback.destroyed;
    }

    public void destroyParticlesInGroup(ParticleGroup group, boolean callDestructionListener) {
        for (var i = group.m_firstIndex; i < group.m_lastIndex; i++) {
            destroyParticle(i, callDestructionListener);
        }
    }

    private final AABB temp2 = new AABB();
    private final v2 tempVec = new v2();
    private final Transform tempTransform = new Transform();
    private final Transform tempTransform2 = new Transform();
    private final CreateParticleGroupCallback createParticleGroupCallback =
            new CreateParticleGroupCallback();
    private final ParticleDef tempParticleDef = new ParticleDef();

    public ParticleGroup createParticleGroup(Dynamics2D world, ParticleGroupDef groupDef) {
        var group = new ParticleGroup();
        group.m_system = this;
        group.m_groupFlags = groupDef.groupFlags;
        group.m_strength = groupDef.strength;
        group.m_userData = groupDef.userData;
        group.m_destroyAutomatically = groupDef.destroyAutomatically;



        world.invoke(() -> {
            var stride = getParticleStride();
            var identity = tempTransform;
            identity.setIdentity();
            var transform = tempTransform2;
            transform.setIdentity();
            var firstIndex = m_count;
            if (groupDef.shape != null) {
                var particleDef = tempParticleDef;
                particleDef.flags = groupDef.flags;
                particleDef.color = groupDef.color;
                particleDef.userData = groupDef.userData;
                var shape = groupDef.shape;
                transform.set(groupDef.position, groupDef.angle);
                var aabb = temp;
                var childCount = shape.getChildCount();
                for (var childIndex = 0; childIndex < childCount; childIndex++) {
                    if (childIndex == 0) {
                        shape.computeAABB(aabb, identity, 0);
                    } else {
                        var childAABB = temp2;
                        shape.computeAABB(childAABB, identity, childIndex);
                        aabb.combine(childAABB);
                    }
                }
                var upperBoundY = aabb.upperBound.y;
                var upperBoundX = aabb.upperBound.x;
                for (var y = MathUtils.floor(aabb.lowerBound.y / stride) * stride; y < upperBoundY; y +=
                        stride) {
                    for (var x = MathUtils.floor(aabb.lowerBound.x / stride) * stride; x < upperBoundX; x +=
                            stride) {
                        var p = tempVec;
                        p.x = x;
                        p.y = y;
                        if (shape.testPoint(identity, p)) {
                            Transform.mulToOut(transform, p, p);
                            particleDef.position.x = p.x;
                            particleDef.position.y = p.y;
                            p.subbed(groupDef.position);
                            v2.crossToOutUnsafe(groupDef.angularVelocity, p, particleDef.velocity);
                            particleDef.velocity.added(groupDef.linearVelocity);
                            createParticle(particleDef);
                        }
                    }
                }
            }
            var lastIndex = m_count;

            group.m_firstIndex = firstIndex;
            group.m_lastIndex = lastIndex;
            group.m_transform.set(transform);

            group.m_prev = null;
            group.m_next = m_groupList;
            if (m_groupList != null) {
                m_groupList.m_prev = group;
            }
            m_groupList = group;
            ++m_groupCount;
            
            for (var i = firstIndex; i < lastIndex; i++) {
                m_groupBuffer[i] = group;
            }

            updateContacts(true);
            if ((groupDef.flags & k_pairFlags) != 0) {
                for (var k = 0; k < m_contactCount; k++) {
                    var contact = m_contactBuffer[k];
                    var a = contact.indexA;
                    var b = contact.indexB;
                    if (a > b) {
                        var temp = a;
                        a = b;
                        b = temp;
                    }
                    if (firstIndex <= a && b < lastIndex) {
                        if (m_pairCount >= m_pairCapacity) {
                            var oldCapacity = m_pairCapacity;
                            var newCapacity =
                                    m_pairCount != 0 ? 2 * m_pairCount : Settings.minParticleBufferCapacity;
                            m_pairBuffer =
                                    BufferUtils.reallocateBuffer(Pair.class, m_pairBuffer, oldCapacity, newCapacity);
                            m_pairCapacity = newCapacity;
                        }
                        var pair = m_pairBuffer[m_pairCount];
                        pair.indexA = a;
                        pair.indexB = b;
                        pair.flags = contact.flags;
                        pair.strength = groupDef.strength;
                        pair.distance = MathUtils.distance(m_positionBuffer.data[a], m_positionBuffer.data[b]);
                        m_pairCount++;
                    }
                }
            }
            if ((groupDef.flags & k_triadFlags) != 0) {
                var diagram = new VoronoiDiagram(lastIndex - firstIndex);
                for (var i = firstIndex; i < lastIndex; i++) {
                    diagram.addGenerator(m_positionBuffer.data[i], i);
                }
                diagram.generate(stride / 2);
                createParticleGroupCallback.system = this;
                createParticleGroupCallback.def = groupDef;
                createParticleGroupCallback.firstIndex = firstIndex;
                diagram.getNodes(createParticleGroupCallback);
            }
            if ((groupDef.groupFlags & ParticleGroupType.b2_solidParticleGroup) != 0) {
                computeDepthForGroup(group);
            }
        });
        return group;
    }

    public void joinParticleGroups(ParticleGroup groupA, ParticleGroup groupB) {
        assert (groupA != groupB);
        RotateBuffer(groupB.m_firstIndex, groupB.m_lastIndex, m_count);
        assert (groupB.m_lastIndex == m_count);
        RotateBuffer(groupA.m_firstIndex, groupA.m_lastIndex, groupB.m_firstIndex);
        assert (groupA.m_lastIndex == groupB.m_firstIndex);

        var array = m_flagsBuffer.data;
        var bound = groupB.m_lastIndex;
        var particleFlags = Arrays.stream(array, groupA.m_firstIndex, bound).reduce(0, (a, b) -> a | b);

        updateContacts(true);
        if ((particleFlags & k_pairFlags) != 0) {
            for (var k = 0; k < m_contactCount; k++) {
                var contact = m_contactBuffer[k];
                var a = contact.indexA;
                var b = contact.indexB;
                if (a > b) {
                    var temp = a;
                    a = b;
                    b = temp;
                }
                if (groupA.m_firstIndex <= a && a < groupA.m_lastIndex && groupB.m_firstIndex <= b
                        && b < groupB.m_lastIndex) {
                    if (m_pairCount >= m_pairCapacity) {
                        var oldCapacity = m_pairCapacity;
                        var newCapacity =
                                m_pairCount != 0 ? 2 * m_pairCount : Settings.minParticleBufferCapacity;
                        m_pairBuffer =
                                BufferUtils.reallocateBuffer(Pair.class, m_pairBuffer, oldCapacity, newCapacity);
                        m_pairCapacity = newCapacity;
                    }
                    var pair = m_pairBuffer[m_pairCount];
                    pair.indexA = a;
                    pair.indexB = b;
                    pair.flags = contact.flags;
                    pair.strength = MathUtils.min(groupA.m_strength, groupB.m_strength);
                    pair.distance = MathUtils.distance(m_positionBuffer.data[a], m_positionBuffer.data[b]);
                    m_pairCount++;
                }
            }
        }
        if ((particleFlags & k_triadFlags) != 0) {
            var diagram = new VoronoiDiagram(groupB.m_lastIndex - groupA.m_firstIndex);
            for (var i = groupA.m_firstIndex; i < groupB.m_lastIndex; i++) {
                if ((m_flagsBuffer.data[i] & ParticleType.b2_zombieParticle) == 0) {
                    diagram.addGenerator(m_positionBuffer.data[i], i);
                }
            }
            diagram.generate(getParticleStride() / 2);
            var callback = new JoinParticleGroupsCallback();
            callback.system = this;
            callback.groupA = groupA;
            callback.groupB = groupB;
            diagram.getNodes(callback);
        }

        for (var i = groupB.m_firstIndex; i < groupB.m_lastIndex; i++) {
            m_groupBuffer[i] = groupA;
        }
        var groupFlags = groupA.m_groupFlags | groupB.m_groupFlags;
        groupA.m_groupFlags = groupFlags;
        groupA.m_lastIndex = groupB.m_lastIndex;
        groupB.m_firstIndex = groupB.m_lastIndex;
        destroyParticleGroup(groupB);

        if ((groupFlags & ParticleGroupType.b2_solidParticleGroup) != 0) {
            computeDepthForGroup(groupA);
        }
    }

    
    private void destroyParticleGroup(ParticleGroup group) {
        assert (m_groupCount > 0);
        assert (group != null);

        if (m_world.getParticleDestructionListener() != null) {
            m_world.getParticleDestructionListener().sayGoodbye(group);
        }

        for (var i = group.m_firstIndex; i < group.m_lastIndex; i++) {
            m_groupBuffer[i] = null;
        }

        if (group.m_prev != null) {
            group.m_prev.m_next = group.m_next;
        }
        if (group.m_next != null) {
            group.m_next.m_prev = group.m_prev;
        }
        if (group == m_groupList) {
            m_groupList = group.m_next;
        }

        --m_groupCount;
    }

    private void computeDepthForGroup(ParticleGroup group) {
        for (var i = group.m_firstIndex; i < group.m_lastIndex; i++) {
            m_accumulationBuffer[i] = 0;
        }
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            var a = contact.indexA;
            var b = contact.indexB;
            if (a >= group.m_firstIndex && a < group.m_lastIndex && b >= group.m_firstIndex
                    && b < group.m_lastIndex) {
                var w = contact.weight;
                m_accumulationBuffer[a] += w;
                m_accumulationBuffer[b] += w;
            }
        }
        m_depthBuffer = requestParticleBuffer(m_depthBuffer);
        for (var i = group.m_firstIndex; i < group.m_lastIndex; i++) {
            var w = m_accumulationBuffer[i];
            m_depthBuffer[i] = w < 0.8f ? 0 : Float.MAX_VALUE;
        }
        var interationCount = group.getParticleCount();
        for (var t = 0; t < interationCount; t++) {
            var updated = false;
            for (var k = 0; k < m_contactCount; k++) {
                var contact = m_contactBuffer[k];
                var a = contact.indexA;
                var b = contact.indexB;
                if (a >= group.m_firstIndex && a < group.m_lastIndex && b >= group.m_firstIndex
                        && b < group.m_lastIndex) {
                    var r = 1 - contact.weight;
                    var ap0 = m_depthBuffer[a];
                    var bp0 = m_depthBuffer[b];
                    var ap1 = bp0 + r;
                    if (ap0 > ap1) {
                        m_depthBuffer[a] = ap1;
                        updated = true;
                    }
                    var bp1 = ap0 + r;
                    if (bp0 > bp1) {
                        m_depthBuffer[b] = bp1;
                        updated = true;
                    }
                }
            }
            if (!updated) {
                break;
            }
        }
        for (var i = group.m_firstIndex; i < group.m_lastIndex; i++) {
            var p = m_depthBuffer[i];
            if (p < Float.MAX_VALUE) {
                m_depthBuffer[i] *= m_particleDiameter;
            } else {
                m_depthBuffer[i] = 0;
            }
        }
    }

    private void addContact(int a, int b) {
        assert (a != b);
        v2 pa = m_positionBuffer.data[a];
        v2 pb = m_positionBuffer.data[b];
        var dx = pb.x - pa.x;
        var dy = pb.y - pa.y;
        var d2 = dx * dx + dy * dy;

        if (d2 < m_squaredDiameter) {
            if (m_contactCount >= m_contactCapacity) {
                var oldCapacity = m_contactCapacity;
                var newCapacity =
                        m_contactCount != 0 ? 2 * m_contactCount : Settings.minParticleBufferCapacity;
                m_contactBuffer =
                        BufferUtils.reallocateBuffer(ParticleContact.class, m_contactBuffer, oldCapacity,
                                newCapacity);
                m_contactCapacity = newCapacity;
            }
            var invD = d2 != 0 ? (float) Math.sqrt(1 / d2) : Float.MAX_VALUE;
            var contact = m_contactBuffer[m_contactCount];
            contact.indexA = a;
            contact.indexB = b;
            contact.flags = m_flagsBuffer.data[a] | m_flagsBuffer.data[b];
            contact.weight = 1 - d2 * invD * m_inverseDiameter;
            contact.normal.x = invD * dx;
            contact.normal.y = invD * dy;
            m_contactCount++;
        }
    }

    private void updateContacts(boolean exceptZombie) {
        for (var p = 0; p < m_proxyCount; p++) {
            var proxy = m_proxyBuffer[p];
            var i = proxy.index;
            v2 pos = m_positionBuffer.data[i];
            proxy.tag = computeTag(m_inverseDiameter * pos.x, m_inverseDiameter * pos.y);
        }


        Arrays.sort(m_proxyBuffer, 0, m_proxyCount);

        m_contactCount = 0;
        var c_index = 0;
        for (var i = 0; i < m_proxyCount; i++) {
            var a = m_proxyBuffer[i];
            var rightTag = computeRelativeTag(a.tag, 1, 0);
            for (var j = i + 1; j < m_proxyCount; j++) {
                var b = m_proxyBuffer[j];
                if (rightTag < b.tag) {
                    break;
                }
                addContact(a.index, b.index);
            }
            var bottomLeftTag = computeRelativeTag(a.tag, -1, 1);
            for (; c_index < m_proxyCount; c_index++) {
                var c = m_proxyBuffer[c_index];
                if (bottomLeftTag <= c.tag) {
                    break;
                }
            }
            var bottomRightTag = computeRelativeTag(a.tag, 1, 1);

            for (var b_index = c_index; b_index < m_proxyCount; b_index++) {
                var b = m_proxyBuffer[b_index];
                if (bottomRightTag < b.tag) {
                    break;
                }
                addContact(a.index, b.index);
            }
        }
        if (exceptZombie) {
            var j = m_contactCount;
            for (var i = 0; i < j; i++) {
                if ((m_contactBuffer[i].flags & ParticleType.b2_zombieParticle) != 0) {
                    --j;
                    var temp = m_contactBuffer[j];
                    m_contactBuffer[j] = m_contactBuffer[i];
                    m_contactBuffer[i] = temp;
                    --i;
                }
            }
            m_contactCount = j;
        }
    }

    private final UpdateBodyContactsCallback ubccallback = new UpdateBodyContactsCallback();

    private void updateBodyContacts() {
        var aabb = temp;
        aabb.lowerBound.x = Float.MAX_VALUE;
        aabb.lowerBound.y = Float.MAX_VALUE;
        aabb.upperBound.x = -Float.MAX_VALUE;
        aabb.upperBound.y = -Float.MAX_VALUE;
        for (var i = 0; i < m_count; i++) {
            v2 p = m_positionBuffer.data[i];
            v2.minToOut(aabb.lowerBound, p, aabb.lowerBound);
            v2.maxToOut(aabb.upperBound, p, aabb.upperBound);
        }
        aabb.lowerBound.x -= m_particleDiameter;
        aabb.lowerBound.y -= m_particleDiameter;
        aabb.upperBound.x += m_particleDiameter;
        aabb.upperBound.y += m_particleDiameter;
        m_bodyContactCount = 0;

        ubccallback.system = this;
        m_world.queryAABB(ubccallback, aabb);
    }

    private final SolveCollisionCallback sccallback = new SolveCollisionCallback();

    private void solveCollision(TimeStep step) {
        var aabb = temp;
        var lowerBound = aabb.lowerBound;
        var upperBound = aabb.upperBound;
        lowerBound.x = Float.MAX_VALUE;
        lowerBound.y = Float.MAX_VALUE;
        upperBound.x = -Float.MAX_VALUE;
        upperBound.y = -Float.MAX_VALUE;
        v2[] V = m_velocityBuffer.data;
        v2[] P = m_positionBuffer.data;
        var dt = step.dt;
        for (var i = 0; i < m_count; i++) {
            var v = V[i];
            var p1 = P[i];
            var p1x = p1.x;
            var p1y = p1.y;

            var p2x = p1x + dt * v.x;
            var p2y = p1y + dt * v.y;
            var bx = Math.min(p1x, p2x);
            lowerBound.x = Math.min(lowerBound.x, bx);
            var by = Math.min(p1y, p2y);
            lowerBound.y = Math.min(lowerBound.y, by);
            var b1x = Math.max(p1x, p2x);
            upperBound.x = Math.max(upperBound.x, b1x);
            var b1y = Math.max(p1y, p2y);
            upperBound.y = Math.max(upperBound.y, b1y);
        }
        sccallback.step = step;
        sccallback.system = this;
        m_world.queryAABB(sccallback, aabb);
    }

    public void solve(TimeStep step) {
        ++m_timestamp;
        if (m_count == 0) {
            return;
        }
        m_allParticleFlags = 0;
        for (var i = 0; i < m_count; i++) {
            m_allParticleFlags |= m_flagsBuffer.data[i];
        }
        if ((m_allParticleFlags & ParticleType.b2_zombieParticle) != 0) {
            solveZombie();
        }
        if (m_count == 0) {
            return;
        }
        m_allGroupFlags = 0;
        for (var group = m_groupList; group != null; group = group.getNext()) {
            m_allGroupFlags |= group.m_groupFlags;
        }
        var gravityx = step.dt * m_gravityScale * m_world.getGravity().x;
        var gravityy = step.dt * m_gravityScale * m_world.getGravity().y;
        var criticalVelocytySquared = getCriticalVelocitySquared(step);
        for (var i = 0; i < m_count; i++) {
            v2 v = m_velocityBuffer.data[i];
            v.x += gravityx;
            v.y += gravityy;
            var v2 = v.x * v.x + v.y * v.y;
            if (v2 > criticalVelocytySquared) {
                var a = v2 == 0 ? Float.MAX_VALUE : (float) Math.sqrt(criticalVelocytySquared / v2);
                v.x *= a;
                v.y *= a;
            }
        }
        solveCollision(step);
        if ((m_allGroupFlags & ParticleGroupType.b2_rigidParticleGroup) != 0) {
            solveRigid(step);
        }
        if ((m_allParticleFlags & ParticleType.b2_wallParticle) != 0) {
            solveWall(step);
        }
        for (var i = 0; i < m_count; i++) {
            v2 pos = m_positionBuffer.data[i];
            v2 vel = m_velocityBuffer.data[i];
            pos.x += step.dt * vel.x;
            pos.y += step.dt * vel.y;
        }
        updateBodyContacts();
        updateContacts(false);
        if ((m_allParticleFlags & ParticleType.b2_viscousParticle) != 0) {
            solveViscous(step);
        }
        if ((m_allParticleFlags & ParticleType.b2_powderParticle) != 0) {
            solvePowder(step);
        }
        if ((m_allParticleFlags & ParticleType.b2_tensileParticle) != 0) {
            solveTensile(step);
        }
        if ((m_allParticleFlags & ParticleType.b2_elasticParticle) != 0) {
            solveElastic(step);
        }
        if ((m_allParticleFlags & ParticleType.b2_springParticle) != 0) {
            solveSpring(step);
        }
        if ((m_allGroupFlags & ParticleGroupType.b2_solidParticleGroup) != 0) {
            solveSolid(step);
        }
        if ((m_allParticleFlags & ParticleType.b2_colorMixingParticle) != 0) {
            solveColorMixing(step);
        }
        solvePressure(step);
        solveDamping(step);
    }

    private void solvePressure(TimeStep step) {
        
        
        for (var i = 0; i < m_count; i++) {
            m_accumulationBuffer[i] = 0;
        }
        for (var k = 0; k < m_bodyContactCount; k++) {
            var contact = m_bodyContactBuffer[k];
            var a = contact.index;
            var w = contact.weight;
            m_accumulationBuffer[a] += w;
        }
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            var a = contact.indexA;
            var b = contact.indexB;
            var w = contact.weight;
            m_accumulationBuffer[a] += w;
            m_accumulationBuffer[b] += w;
        }
        
        if ((m_allParticleFlags & k_noPressureFlags) != 0) {
            for (var i = 0; i < m_count; i++) {
                if ((m_flagsBuffer.data[i] & k_noPressureFlags) != 0) {
                    m_accumulationBuffer[i] = 0;
                }
            }
        }

        var pressurePerWeight = m_pressureStrength * getCriticalPressure(step);
        for (var i = 0; i < m_count; i++) {
            var w = m_accumulationBuffer[i];
            var h =
                    pressurePerWeight
                            * MathUtils.max(0.0f, MathUtils.min(w, Settings.maxParticleWeight)
                            - Settings.minParticleWeight);
            m_accumulationBuffer[i] = h;
        }

        var velocityPerPressure = step.dt / (m_density * m_particleDiameter);
        for (var k = 0; k < m_bodyContactCount; k++) {
            var contact = m_bodyContactBuffer[k];
            var a = contact.index;
            var b = contact.body;
            var w = contact.weight;
            var m = contact.mass;
            var n = contact.normal;
            v2 p = m_positionBuffer.data[a];
            var h = m_accumulationBuffer[a] + pressurePerWeight * w;
            var f = tempVec;
            var coef = velocityPerPressure * w * m * h;
            f.x = coef * n.x;
            f.y = coef * n.y;
            v2 velData = m_velocityBuffer.data[a];
            var particleInvMass = getParticleInvMass();
            velData.x -= particleInvMass * f.x;
            velData.y -= particleInvMass * f.y;
            b.applyLinearImpulse(f, p, true);
        }
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            var a = contact.indexA;
            var b = contact.indexB;
            var w = contact.weight;
            var n = contact.normal;
            var h = m_accumulationBuffer[a] + m_accumulationBuffer[b];
            var fx = velocityPerPressure * w * h * n.x;
            var fy = velocityPerPressure * w * h * n.y;
            v2 velDataA = m_velocityBuffer.data[a];
            v2 velDataB = m_velocityBuffer.data[b];
            velDataA.x -= fx;
            velDataA.y -= fy;
            velDataB.x += fx;
            velDataB.y += fy;
        }
    }

    private void solveDamping(TimeStep step) {

        var damping = m_dampingStrength;
        for (var k = 0; k < m_bodyContactCount; k++) {
            var contact = m_bodyContactBuffer[k];
            var a = contact.index;
            var b = contact.body;
            var w = contact.weight;
            var m = contact.mass;
            var n = contact.normal;
            v2 p = m_positionBuffer.data[a];
            var tempX = p.x - b.sweep.c.x;
            var tempY = p.y - b.sweep.c.y;
            v2 velA = m_velocityBuffer.data[a];

            var vx = -b.velAngular * tempY + b.vel.x - velA.x;
            var vy = b.velAngular * tempX + b.vel.y - velA.y;

            var vn = vx * n.x + vy * n.y;
            if (vn < 0) {
                var f = tempVec;
                f.x = damping * w * m * vn * n.x;
                f.y = damping * w * m * vn * n.y;
                var invMass = getParticleInvMass();
                velA.x += invMass * f.x;
                velA.y += invMass * f.y;
                f.x = -f.x;
                f.y = -f.y;
                b.applyLinearImpulse(f, p, true);
            }
        }
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            var a = contact.indexA;
            var b = contact.indexB;
            var w = contact.weight;
            var n = contact.normal;
            v2 velA = m_velocityBuffer.data[a];
            v2 velB = m_velocityBuffer.data[b];
            var vx = velB.x - velA.x;
            var vy = velB.y - velA.y;
            var vn = vx * n.x + vy * n.y;
            if (vn < 0) {
                var fx = damping * w * vn * n.x;
                var fy = damping * w * vn * n.y;
                velA.x += fx;
                velA.y += fy;
                velB.x -= fx;
                velB.y -= fy;
            }
        }
    }

    private void solveWall(TimeStep step) {
        for (var i = 0; i < m_count; i++) {
            if ((m_flagsBuffer.data[i] & ParticleType.b2_wallParticle) != 0) {
                v2 r = m_velocityBuffer.data[i];
                r.x = 0.0f;
                r.y = 0.0f;
            }
        }
    }

    private final v2 tempv2 = new v2();
    private final Rot tempRot = new Rot();
    private final Transform tempXf = new Transform();
    private final Transform tempXf2 = new Transform();

    private void solveRigid(TimeStep step) {
        for (var group = m_groupList; group != null; group = group.getNext()) {
            if ((group.m_groupFlags & ParticleGroupType.b2_rigidParticleGroup) != 0) {
                group.updateStatistics();
                var temp = tempVec;
                var cross = tempv2;
                var rotation = tempRot;
                rotation.set(step.dt * group.m_angularVelocity);
                Rot.mulToOutUnsafe(rotation, group.m_center, cross);
                temp.set(group.m_linearVelocity).scaled(step.dt).added(group.m_center).subbed(cross);
                tempXf.pos.set(temp);
                tempXf.set(rotation);
                Transform.mulToOut(tempXf, group.m_transform, group.m_transform);
                var velocityTransform = tempXf2;
                velocityTransform.pos.x = step.inv_dt * tempXf.pos.x;
                velocityTransform.pos.y = step.inv_dt * tempXf.pos.y;
                velocityTransform.s = step.inv_dt * tempXf.s;
                velocityTransform.c = step.inv_dt * (tempXf.c - 1);
                for (var i = group.m_firstIndex; i < group.m_lastIndex; i++) {
                    Transform.mulToOutUnsafe(velocityTransform, m_positionBuffer.data[i],
                            m_velocityBuffer.data[i]);
                }
            }
        }
    }

    private void solveElastic(TimeStep step) {
        var elasticStrength = step.inv_dt * m_elasticStrength;
        for (var k = 0; k < m_triadCount; k++) {
            var triad = m_triadBuffer[k];
            if ((triad.flags & ParticleType.b2_elasticParticle) != 0) {
                var a = triad.indexA;
                var b = triad.indexB;
                var c = triad.indexC;
                var oa = triad.pa;
                var ob = triad.pb;
                var oc = triad.pc;
                v2 pa = m_positionBuffer.data[a];
                v2 pb = m_positionBuffer.data[b];
                v2 pc = m_positionBuffer.data[c];
                var px = 1f / 3 * (pa.x + pb.x + pc.x);
                var py = 1f / 3 * (pa.y + pb.y + pc.y);
                var rs = v2.cross(oa, pa) + v2.cross(ob, pb) + v2.cross(oc, pc);
                var rc = v2.dot(oa, pa) + v2.dot(ob, pb) + v2.dot(oc, pc);
                var r2 = rs * rs + rc * rc;
                var invR = r2 == 0 ? Float.MAX_VALUE : (float) Math.sqrt(1f / r2);
                rs *= invR;
                rc *= invR;
                var strength = elasticStrength * triad.strength;
                var roax = rc * oa.x - rs * oa.y;
                var roay = rs * oa.x + rc * oa.y;
                var robx = rc * ob.x - rs * ob.y;
                var roby = rs * ob.x + rc * ob.y;
                var rocx = rc * oc.x - rs * oc.y;
                var rocy = rs * oc.x + rc * oc.y;
                v2 va = m_velocityBuffer.data[a];
                v2 vb = m_velocityBuffer.data[b];
                v2 vc = m_velocityBuffer.data[c];
                va.x += strength * (roax - (pa.x - px));
                va.y += strength * (roay - (pa.y - py));
                vb.x += strength * (robx - (pb.x - px));
                vb.y += strength * (roby - (pb.y - py));
                vc.x += strength * (rocx - (pc.x - px));
                vc.y += strength * (rocy - (pc.y - py));
            }
        }
    }

    private void solveSpring(TimeStep step) {
        var springStrength = step.inv_dt * m_springStrength;
        for (var k = 0; k < m_pairCount; k++) {
            var pair = m_pairBuffer[k];
            if ((pair.flags & ParticleType.b2_springParticle) != 0) {
                var a = pair.indexA;
                var b = pair.indexB;
                v2 pa = m_positionBuffer.data[a];
                v2 pb = m_positionBuffer.data[b];
                var dx = pb.x - pa.x;
                var dy = pb.y - pa.y;
                var r0 = pair.distance;
                var r1 = (float) Math.sqrt(dx * dx + dy * dy);
                if (r1 == 0) r1 = Float.MAX_VALUE;
                var strength = springStrength * pair.strength;
                var fx = strength * (r0 - r1) / r1 * dx;
                var fy = strength * (r0 - r1) / r1 * dy;
                v2 va = m_velocityBuffer.data[a];
                v2 vb = m_velocityBuffer.data[b];
                va.x -= fx;
                va.y -= fy;
                vb.x += fx;
                vb.y += fy;
            }
        }
    }

    private void solveTensile(TimeStep step) {
        m_accumulation2Buffer = requestParticleBuffer(Position.class, m_accumulation2Buffer);
        for (var i = 0; i < m_count; i++) {
            m_accumulationBuffer[i] = 0;
            m_accumulation2Buffer[i].setZero();
        }
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            if ((contact.flags & ParticleType.b2_tensileParticle) != 0) {
                var a = contact.indexA;
                var b = contact.indexB;
                var w = contact.weight;
                var n = contact.normal;
                m_accumulationBuffer[a] += w;
                m_accumulationBuffer[b] += w;
                v2 a2A = m_accumulation2Buffer[a];
                v2 a2B = m_accumulation2Buffer[b];
                var inter = (1 - w) * w;
                a2A.x -= inter * n.x;
                a2A.y -= inter * n.y;
                a2B.x += inter * n.x;
                a2B.y += inter * n.y;
            }
        }
        var strengthA = m_surfaceTensionStrengthA * getCriticalVelocity(step);
        var strengthB = m_surfaceTensionStrengthB * getCriticalVelocity(step);
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            if ((contact.flags & ParticleType.b2_tensileParticle) != 0) {
                var a = contact.indexA;
                var b = contact.indexB;
                var w = contact.weight;
                var n = contact.normal;
                v2 a2A = m_accumulation2Buffer[a];
                v2 a2B = m_accumulation2Buffer[b];
                var h = m_accumulationBuffer[a] + m_accumulationBuffer[b];
                var sx = a2B.x - a2A.x;
                var sy = a2B.y - a2A.y;
                var fn = (strengthA * (h - 2) + strengthB * (sx * n.x + sy * n.y)) * w;
                var fx = fn * n.x;
                var fy = fn * n.y;
                v2 va = m_velocityBuffer.data[a];
                v2 vb = m_velocityBuffer.data[b];
                va.x -= fx;
                va.y -= fy;
                vb.x += fx;
                vb.y += fy;
            }
        }
    }

    private void solveViscous(TimeStep step) {
        var viscousStrength = m_viscousStrength;
        for (var k = 0; k < m_bodyContactCount; k++) {
            var contact = m_bodyContactBuffer[k];
            var a = contact.index;
            if ((m_flagsBuffer.data[a] & ParticleType.b2_viscousParticle) != 0) {
                var b = contact.body;
                var w = contact.weight;
                var m = contact.mass;
                v2 p = m_positionBuffer.data[a];
                v2 va = m_velocityBuffer.data[a];
                var tempX = p.x - b.sweep.c.x;
                var tempY = p.y - b.sweep.c.y;
                var vx = -b.velAngular * tempY + b.vel.x - va.x;
                var vy = b.velAngular * tempX + b.vel.y - va.y;
                var f = tempVec;
                var pInvMass = getParticleInvMass();
                f.x = viscousStrength * m * w * vx;
                f.y = viscousStrength * m * w * vy;
                va.x += pInvMass * f.x;
                va.y += pInvMass * f.y;
                f.x = -f.x;
                f.y = -f.y;
                b.applyLinearImpulse(f, p, true);
            }
        }
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            if ((contact.flags & ParticleType.b2_viscousParticle) != 0) {
                var a = contact.indexA;
                var b = contact.indexB;
                var w = contact.weight;
                v2 va = m_velocityBuffer.data[a];
                v2 vb = m_velocityBuffer.data[b];
                var vx = vb.x - va.x;
                var vy = vb.y - va.y;
                var fx = viscousStrength * w * vx;
                va.x += fx;
                var fy = viscousStrength * w * vy;
                va.y += fy;
                vb.x -= fx;
                vb.y -= fy;
            }
        }
    }

    private void solvePowder(TimeStep step) {
        var powderStrength = m_powderStrength * getCriticalVelocity(step);
        var minWeight = 1.0f - Settings.particleStride;
        for (var k = 0; k < m_bodyContactCount; k++) {
            var contact = m_bodyContactBuffer[k];
            var a = contact.index;
            if ((m_flagsBuffer.data[a] & ParticleType.b2_powderParticle) != 0) {
                var w = contact.weight;
                if (w > minWeight) {
                    var b = contact.body;
                    var m = contact.mass;
                    v2 p = m_positionBuffer.data[a];
                    var n = contact.normal;
                    var f = tempVec;
                    v2 va = m_velocityBuffer.data[a];
                    var inter = powderStrength * m * (w - minWeight);
                    var pInvMass = getParticleInvMass();
                    f.x = inter * n.x;
                    f.y = inter * n.y;
                    va.x -= pInvMass * f.x;
                    va.y -= pInvMass * f.y;
                    b.applyLinearImpulse(f, p, true);
                }
            }
        }
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            if ((contact.flags & ParticleType.b2_powderParticle) != 0) {
                var w = contact.weight;
                if (w > minWeight) {
                    var a = contact.indexA;
                    var b = contact.indexB;
                    var n = contact.normal;
                    v2 va = m_velocityBuffer.data[a];
                    v2 vb = m_velocityBuffer.data[b];
                    var inter = powderStrength * (w - minWeight);
                    var fx = inter * n.x;
                    var fy = inter * n.y;
                    va.x -= fx;
                    va.y -= fy;
                    vb.x += fx;
                    vb.y += fy;
                }
            }
        }
    }

    private void solveSolid(TimeStep step) {
        
        m_depthBuffer = requestParticleBuffer(m_depthBuffer);
        var ejectionStrength = step.inv_dt * m_ejectionStrength;
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            var a = contact.indexA;
            var b = contact.indexB;
            if (m_groupBuffer[a] != m_groupBuffer[b]) {
                var w = contact.weight;
                var n = contact.normal;
                var h = m_depthBuffer[a] + m_depthBuffer[b];
                v2 va = m_velocityBuffer.data[a];
                v2 vb = m_velocityBuffer.data[b];
                var inter = ejectionStrength * h * w;
                var fx = inter * n.x;
                var fy = inter * n.y;
                va.x -= fx;
                va.y -= fy;
                vb.x += fx;
                vb.y += fy;
            }
        }
    }

    private void solveColorMixing(TimeStep step) {
        
        m_colorBuffer.data = requestParticleBuffer(ParticleColor.class, m_colorBuffer.data);
        var colorMixing256 = (int) (256 * m_colorMixingStrength);
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            var a = contact.indexA;
            var b = contact.indexB;
            if ((m_flagsBuffer.data[a] & m_flagsBuffer.data[b] & ParticleType.b2_colorMixingParticle) != 0) {
                var colorA = m_colorBuffer.data[a];
                var colorB = m_colorBuffer.data[b];
                var dr = (colorMixing256 * (colorB.r - colorA.r)) >> 8;
                var dg = (colorMixing256 * (colorB.g - colorA.g)) >> 8;
                var db = (colorMixing256 * (colorB.b - colorA.b)) >> 8;
                var da = (colorMixing256 * (colorB.a - colorA.a)) >> 8;
                colorA.r += dr;
                colorA.g += dg;
                colorA.b += db;
                colorA.a += da;
                colorB.r -= dr;
                colorB.g -= dg;
                colorB.b -= db;
                colorB.a -= da;
            }
        }
    }

    private void solveZombie() {

        var newCount = 0;
        var newIndices = new int[m_count];
        for (var i = 0; i < m_count; i++) {
            var flags = m_flagsBuffer.data[i];
            if ((flags & ParticleType.b2_zombieParticle) != 0) {
                var destructionListener = m_world.getParticleDestructionListener();
                if ((flags & ParticleType.b2_destructionListener) != 0 && destructionListener != null) {
                    destructionListener.sayGoodbye(i);
                }
                newIndices[i] = Settings.invalidParticleIndex;
            } else {
                newIndices[i] = newCount;
                if (i != newCount) {
                    m_flagsBuffer.data[newCount] = m_flagsBuffer.data[i];
                    m_positionBuffer.data[newCount].set(m_positionBuffer.data[i]);
                    m_velocityBuffer.data[newCount].set(m_velocityBuffer.data[i]);
                    m_groupBuffer[newCount] = m_groupBuffer[i];
                    if (m_depthBuffer != null) {
                        m_depthBuffer[newCount] = m_depthBuffer[i];
                    }
                    if (m_colorBuffer.data != null) {
                        m_colorBuffer.data[newCount].set(m_colorBuffer.data[i]);
                    }
                    if (m_userDataBuffer.data != null) {
                        m_userDataBuffer.data[newCount] = m_userDataBuffer.data[i];
                    }
                }
                newCount++;
            }
        }

        
        for (var k = 0; k < m_proxyCount; k++) {
            var proxy = m_proxyBuffer[k];
            proxy.index = newIndices[proxy.index];
        }


        var j = m_proxyCount;
        for (var i = 0; i < j; i++) {
            if (Test.IsProxyInvalid(m_proxyBuffer[i])) {
                --j;
                var temp = m_proxyBuffer[j];
                m_proxyBuffer[j] = m_proxyBuffer[i];
                m_proxyBuffer[i] = temp;
                --i;
            }
        }
        m_proxyCount = j;

        
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            contact.indexA = newIndices[contact.indexA];
            contact.indexB = newIndices[contact.indexB];
        }
        
        
        
        
        j = m_contactCount;
        for (var i = 0; i < j; i++) {
            if (Test.IsContactInvalid(m_contactBuffer[i])) {
                --j;
                var temp = m_contactBuffer[j];
                m_contactBuffer[j] = m_contactBuffer[i];
                m_contactBuffer[i] = temp;
                --i;
            }
        }
        m_contactCount = j;

        
        for (var k = 0; k < m_bodyContactCount; k++) {
            var contact = m_bodyContactBuffer[k];
            contact.index = newIndices[contact.index];
        }
        
        
        
        
        j = m_bodyContactCount;
        for (var i = 0; i < j; i++) {
            if (Test.IsBodyContactInvalid(m_bodyContactBuffer[i])) {
                --j;
                var temp = m_bodyContactBuffer[j];
                m_bodyContactBuffer[j] = m_bodyContactBuffer[i];
                m_bodyContactBuffer[i] = temp;
                --i;
            }
        }
        m_bodyContactCount = j;

        
        for (var k = 0; k < m_pairCount; k++) {
            var pair = m_pairBuffer[k];
            pair.indexA = newIndices[pair.indexA];
            pair.indexB = newIndices[pair.indexB];
        }
        
        
        j = m_pairCount;
        for (var i = 0; i < j; i++) {
            if (Test.IsPairInvalid(m_pairBuffer[i])) {
                --j;
                var temp = m_pairBuffer[j];
                m_pairBuffer[j] = m_pairBuffer[i];
                m_pairBuffer[i] = temp;
                --i;
            }
        }
        m_pairCount = j;

        
        for (var k = 0; k < m_triadCount; k++) {
            var triad = m_triadBuffer[k];
            triad.indexA = newIndices[triad.indexA];
            triad.indexB = newIndices[triad.indexB];
            triad.indexC = newIndices[triad.indexC];
        }
        
        
        
        j = m_triadCount;
        for (var i = 0; i < j; i++) {
            if (Test.IsTriadInvalid(m_triadBuffer[i])) {
                --j;
                var temp = m_triadBuffer[j];
                m_triadBuffer[j] = m_triadBuffer[i];
                m_triadBuffer[i] = temp;
                --i;
            }
        }
        m_triadCount = j;

        
        for (var group = m_groupList; group != null; group = group.getNext()) {
            var firstIndex = newCount;
            var lastIndex = 0;
            var modified = false;
            for (var i = group.m_firstIndex; i < group.m_lastIndex; i++) {
                j = newIndices[i];
                if (j >= 0) {
                    firstIndex = MathUtils.min(firstIndex, j);
                    lastIndex = MathUtils.max(lastIndex, j + 1);
                } else {
                    modified = true;
                }
            }
            if (firstIndex < lastIndex) {
                group.m_firstIndex = firstIndex;
                group.m_lastIndex = lastIndex;
                if (modified) {
                    if ((group.m_groupFlags & ParticleGroupType.b2_rigidParticleGroup) != 0) {
                        group.m_toBeSplit = true;
                    }
                }
            } else {
                group.m_firstIndex = 0;
                group.m_lastIndex = 0;
                if (group.m_destroyAutomatically) {
                    group.m_toBeDestroyed = true;
                }
            }
        }

        
        m_count = newCount;
        

        
        for (var group = m_groupList; group != null; ) {
            var next = group.getNext();
            if (group.m_toBeDestroyed) {
                destroyParticleGroup(group);
            } else if (group.m_toBeSplit) {
                
            }
            group = next;
        }
    }

    private static class NewIndices {
        int start;
        int mid;
        int end;

        final int getIndex(int i) {
            if (i < start) {
                return i;
            } else if (i < mid) {
                return i + end - mid;
            } else if (i < end) {
                return i + start - mid;
            } else {
                return i;
            }
        }
    }

    private final NewIndices newIndices = new NewIndices();


    private void RotateBuffer(int start, int mid, int end) {
        
        if (start == mid || mid == end) {
            return;
        }
        newIndices.start = start;
        newIndices.mid = mid;
        newIndices.end = end;

        BufferUtils.rotate(m_flagsBuffer.data, start, mid, end);
        BufferUtils.rotate(m_positionBuffer.data, start, mid, end);
        BufferUtils.rotate(m_velocityBuffer.data, start, mid, end);
        BufferUtils.rotate(m_groupBuffer, start, mid, end);
        if (m_depthBuffer != null) {
            BufferUtils.rotate(m_depthBuffer, start, mid, end);
        }
        if (m_colorBuffer.data != null) {
            BufferUtils.rotate(m_colorBuffer.data, start, mid, end);
        }
        if (m_userDataBuffer.data != null) {
            BufferUtils.rotate(m_userDataBuffer.data, start, mid, end);
        }

        
        for (var k = 0; k < m_proxyCount; k++) {
            var proxy = m_proxyBuffer[k];
            proxy.index = newIndices.getIndex(proxy.index);
        }

        
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            contact.indexA = newIndices.getIndex(contact.indexA);
            contact.indexB = newIndices.getIndex(contact.indexB);
        }

        
        for (var k = 0; k < m_bodyContactCount; k++) {
            var contact = m_bodyContactBuffer[k];
            contact.index = newIndices.getIndex(contact.index);
        }

        
        for (var k = 0; k < m_pairCount; k++) {
            var pair = m_pairBuffer[k];
            pair.indexA = newIndices.getIndex(pair.indexA);
            pair.indexB = newIndices.getIndex(pair.indexB);
        }

        
        for (var k = 0; k < m_triadCount; k++) {
            var triad = m_triadBuffer[k];
            triad.indexA = newIndices.getIndex(triad.indexA);
            triad.indexB = newIndices.getIndex(triad.indexB);
            triad.indexC = newIndices.getIndex(triad.indexC);
        }

        
        for (var group = m_groupList; group != null; group = group.getNext()) {
            group.m_firstIndex = newIndices.getIndex(group.m_firstIndex);
            group.m_lastIndex = newIndices.getIndex(group.m_lastIndex - 1) + 1;
        }
    }

    public void setParticleRadius(float radius) {
        m_particleDiameter = 2 * radius;
        m_squaredDiameter = m_particleDiameter * m_particleDiameter;
        m_inverseDiameter = 1 / m_particleDiameter;
    }

    public void setParticleDensity(float density) {
        m_density = density;
        m_inverseDensity = 1 / m_density;
    }

    public float getParticleDensity() {
        return m_density;
    }

    public void setParticleGravityScale(float gravityScale) {
        m_gravityScale = gravityScale;
    }

    public float getParticleGravityScale() {
        return m_gravityScale;
    }

    public void setParticleDamping(float damping) {
        m_dampingStrength = damping;
    }

    public float getParticleDamping() {
        return m_dampingStrength;
    }

    public float getParticleRadius() {
        return m_particleDiameter / 2;
    }

    private float getCriticalVelocity(TimeStep step) {
        return m_particleDiameter * step.inv_dt;
    }

    private float getCriticalVelocitySquared(TimeStep step) {
        var velocity = getCriticalVelocity(step);
        return velocity * velocity;
    }

    private float getCriticalPressure(TimeStep step) {
        return m_density * getCriticalVelocitySquared(step);
    }

    private float getParticleStride() {
        return Settings.particleStride * m_particleDiameter;
    }

    float getParticleMass() {
        var stride = getParticleStride();
        return m_density * stride * stride;
    }

    private float getParticleInvMass() {
        return 1.777777f * m_inverseDensity * m_inverseDiameter * m_inverseDiameter;
    }

    public int[] getParticleFlagsBuffer() {
        return m_flagsBuffer.data;
    }

    public v2[] getParticlePositionBuffer() {
        return m_positionBuffer.data;
    }

    public v2[] getParticleVelocityBuffer() {
        return m_velocityBuffer.data;
    }

    public ParticleColor[] getParticleColorBuffer() {
        m_colorBuffer.data = requestParticleBuffer(ParticleColor.class, m_colorBuffer.data);
        return m_colorBuffer.data;
    }

    public Object[] getParticleUserDataBuffer() {
        m_userDataBuffer.data = requestParticleBuffer(Object.class, m_userDataBuffer.data);
        return m_userDataBuffer.data;
    }

    public int getParticleMaxCount() {
        return m_maxCount;
    }

    public void setParticleMaxCount(int count) {
        assert (m_count <= count);
        m_maxCount = count;
    }

    private static void setParticleBuffer(ParticleBufferInt buffer, int[] newData, int newCapacity) {
        assert ((newData != null && newCapacity != 0) || (newData == null && newCapacity == 0));
        if (buffer.userSuppliedCapacity != 0) {
            
        }
        buffer.data = newData;
        buffer.userSuppliedCapacity = newCapacity;
    }

    private static <T> void setParticleBuffer(ParticleBuffer<T> buffer, T[] newData, int newCapacity) {
        assert ((newData != null && newCapacity != 0) || (newData == null && newCapacity == 0));
        if (buffer.userSuppliedCapacity != 0) {
            
        }
        buffer.data = newData;
        buffer.userSuppliedCapacity = newCapacity;
    }

    public void setParticleFlagsBuffer(int[] buffer, int capacity) {
        setParticleBuffer(m_flagsBuffer, buffer, capacity);
    }

    public void setParticlePositionBuffer(Position[] buffer, int capacity) {
        setParticleBuffer(m_positionBuffer, buffer, capacity);
    }

    public void setParticleVelocityBuffer(Velocity[] buffer, int capacity) {
        setParticleBuffer(m_velocityBuffer, buffer, capacity);
    }

    public void setParticleColorBuffer(ParticleColor[] buffer, int capacity) {
        setParticleBuffer(m_colorBuffer, buffer, capacity);
    }

    public ParticleGroup[] getParticleGroupBuffer() {
        return m_groupBuffer;
    }

    public int getParticleGroupCount() {
        return m_groupCount;
    }

    public ParticleGroup[] getParticleGroupList() {
        return m_groupBuffer;
    }

    public int getParticleCount() {
        return m_count;
    }

    public void setParticleUserDataBuffer(Object[] buffer, int capacity) {
        setParticleBuffer(m_userDataBuffer, buffer, capacity);
    }

    private static int lowerBound(Proxy[] ray, int length, long tag) {
        var left = 0;
        while (length > 0) {
            var step = length / 2;
            var curr = left + step;
            if (ray[curr].tag < tag) {
                left = curr + 1;
                length -= step + 1;
            } else {
                length = step;
            }
        }
        return left;
    }

    private static int upperBound(Proxy[] ray, int length, long tag) {
        var left = 0;
        while (length > 0) {
            var step = length / 2;
            var curr = left + step;
            if (ray[curr].tag <= tag) {
                left = curr + 1;
                length -= step + 1;
            } else {
                length = step;
            }
        }
        return left;
    }

    public void queryAABB(ParticleQueryCallback callback, AABB aabb) {
        if (m_proxyCount == 0) {
            return;
        }

        var lowerBoundX = aabb.lowerBound.x;
        var lowerBoundY = aabb.lowerBound.y;
        var upperBoundX = aabb.upperBound.x;
        var upperBoundY = aabb.upperBound.y;
        var firstProxy =
                lowerBound(m_proxyBuffer, m_proxyCount,
                        computeTag(m_inverseDiameter * lowerBoundX, m_inverseDiameter * lowerBoundY));
        var lastProxy =
                upperBound(m_proxyBuffer, m_proxyCount,
                        computeTag(m_inverseDiameter * upperBoundX, m_inverseDiameter * upperBoundY));
        for (var proxy = firstProxy; proxy < lastProxy; ++proxy) {
            var i = m_proxyBuffer[proxy].index;
            v2 p = m_positionBuffer.data[i];
            if (lowerBoundX < p.x && p.x < upperBoundX && lowerBoundY < p.y && p.y < upperBoundY) {
                if (!callback.reportParticle(i)) {
                    break;
                }
            }
        }
    }

    /**
     * @param callback
     * @param point1
     * @param point2
     */
    public void raycast(ParticleRaycastCallback callback, v2 point1, v2 point2) {
        if (m_proxyCount == 0) {
            return;
        }
        var firstProxy =
                lowerBound(
                        m_proxyBuffer,
                        m_proxyCount,
                        computeTag(m_inverseDiameter * MathUtils.min(point1.x, point2.x) - 1, m_inverseDiameter
                                * MathUtils.min(point1.y, point2.y) - 1));
        var lastProxy =
                upperBound(
                        m_proxyBuffer,
                        m_proxyCount,
                        computeTag(m_inverseDiameter * MathUtils.max(point1.x, point2.x) + 1, m_inverseDiameter
                                * MathUtils.max(point1.y, point2.y) + 1));


        var vx = point2.x - point1.x;
        var vy = point2.y - point1.y;
        var v2 = vx * vx + vy * vy;
        if (v2 == 0) v2 = Float.MAX_VALUE;
        float fraction = 1;
        for (var proxy = firstProxy; proxy < lastProxy; ++proxy) {
            var i = m_proxyBuffer[proxy].index;
            jcog.math.v2 posI = m_positionBuffer.data[i];
            var px = point1.x - posI.x;
            var py = point1.y - posI.y;
            var pv = px * vx + py * vy;
            var p2 = px * px + py * py;
            var determinant = pv * pv - v2 * (p2 - m_squaredDiameter);
            if (determinant >= 0) {
                var sqrtDeterminant = (float) Math.sqrt(determinant);

                var t = (-pv - sqrtDeterminant) / v2;
                if (t > fraction) {
                    continue;
                }
                if (t < 0) {
                    t = (-pv + sqrtDeterminant) / v2;
                    if (t < 0 || t > fraction) {
                        continue;
                    }
                }
                var n = tempVec;
                tempVec.x = px + t * vx;
                tempVec.y = py + t * vy;
                n.normalize();
                var point = tempv2;
                point.x = point1.x + t * vx;
                point.y = point1.y + t * vy;
                var f = callback.reportParticle(i, point, n, t);
                fraction = MathUtils.min(fraction, f);
                if (fraction <= 0) {
                    break;
                }
            }
        }
    }

    public float computeParticleCollisionEnergy() {
        float sum_v2 = 0;
        for (var k = 0; k < m_contactCount; k++) {
            var contact = m_contactBuffer[k];
            var a = contact.indexA;
            var b = contact.indexB;
            var n = contact.normal;
            v2 va = m_velocityBuffer.data[a];
            v2 vb = m_velocityBuffer.data[b];
            var vx = vb.x - va.x;
            var vy = vb.y - va.y;
            var vn = vx * n.x + vy * n.y;
            if (vn < 0) {
                sum_v2 += vn * vn;
            }
        }
        return 0.5f * getParticleMass() * sum_v2;
    }

    
    private static <T> T[] reallocateBuffer(ParticleBuffer<T> buffer, int oldCapacity, int newCapacity,
                                            boolean deferred) {
        assert (newCapacity > oldCapacity);
        return BufferUtils.reallocateBuffer(buffer.dataClass, buffer.data, buffer.userSuppliedCapacity,
                oldCapacity, newCapacity, deferred);
    }

    private static int[] reallocateBuffer(ParticleBufferInt buffer, int oldCapacity, int newCapacity,
                                          boolean deferred) {
        assert (newCapacity > oldCapacity);
        return BufferUtils.reallocateBuffer(buffer.data, buffer.userSuppliedCapacity, oldCapacity,
                newCapacity, deferred);
    }

    @SuppressWarnings("unchecked")
    private <T> T[] requestParticleBuffer(Class<T> klass, T[] buffer) {
        if (buffer == null) {
            buffer = (T[]) Array.newInstance(klass, m_internalAllocatedCapacity);
            for (var i = 0; i < m_internalAllocatedCapacity; i++) {
                try {
                    buffer[i] = klass.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return buffer;
    }

    private float[] requestParticleBuffer(float[] buffer) {
        if (buffer == null) {
            buffer = new float[m_internalAllocatedCapacity];
        }
        return buffer;
    }

    public static class ParticleBuffer<T> {
        public T[] data;
        final Class<T> dataClass;
        int userSuppliedCapacity;

        ParticleBuffer(Class<T> dataClass) {
            this.dataClass = dataClass;
        }
    }

    public static class ParticleBufferInt {
        int[] data;
        int userSuppliedCapacity;
    }

    /**
     * Used for detecting particle contacts
     */
    public static class Proxy implements Comparable<Proxy> {
        int index;
        long tag;

        @Override
        public int compareTo(Proxy o) {
            return (tag - o.tag) < 0 ? -1 : (o.tag == tag ? 0 : 1);
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            var other = (Proxy) obj;
            return tag == other.tag;
        }
    }

    /**
     * Connection between two particles
     */
    static class Pair {
        int indexA;
        int indexB;
        int flags;
        float strength;
        float distance;
    }

    /**
     * Connection between three particles
     */
    static class Triad {
        int indexA;
        int indexB;
        int indexC;
        int flags;
        float strength;
        final v2 pa = new v2();
        final v2 pb = new v2();
        final v2 pc = new v2();
        float ka;
        float kb;
        float kc;
        float s;
    }

    
    static class CreateParticleGroupCallback implements VoronoiDiagramCallback {
        public void callback(int a, int b, int c) {
            v2 pa = system.m_positionBuffer.data[a];
            v2 pb = system.m_positionBuffer.data[b];
            v2 pc = system.m_positionBuffer.data[c];
            var dabx = pa.x - pb.x;
            var daby = pa.y - pb.y;
            var dbcx = pb.x - pc.x;
            var dbcy = pb.y - pc.y;
            var dcax = pc.x - pa.x;
            var dcay = pc.y - pa.y;
            var maxDistanceSquared = Settings.maxTriadDistanceSquared * system.m_squaredDiameter;
            if (dabx * dabx + daby * daby < maxDistanceSquared
                    && dbcx * dbcx + dbcy * dbcy < maxDistanceSquared
                    && dcax * dcax + dcay * dcay < maxDistanceSquared) {
                if (system.m_triadCount >= system.m_triadCapacity) {
                    var oldCapacity = system.m_triadCapacity;
                    var newCapacity =
                            system.m_triadCount != 0
                                    ? 2 * system.m_triadCount
                                    : Settings.minParticleBufferCapacity;
                    system.m_triadBuffer =
                            BufferUtils.reallocateBuffer(Triad.class, system.m_triadBuffer, oldCapacity,
                                    newCapacity);
                    system.m_triadCapacity = newCapacity;
                }
                var triad = system.m_triadBuffer[system.m_triadCount];
                triad.indexA = a;
                triad.indexB = b;
                triad.indexC = c;
                triad.flags =
                        system.m_flagsBuffer.data[a] | system.m_flagsBuffer.data[b]
                                | system.m_flagsBuffer.data[c];
                triad.strength = def.strength;
                var midPointx = 1f / 3f * (pa.x + pb.x + pc.x);
                var midPointy = 1f / 3f * (pa.y + pb.y + pc.y);
                triad.pa.x = pa.x - midPointx;
                triad.pa.y = pa.y - midPointy;
                triad.pb.x = pb.x - midPointx;
                triad.pb.y = pb.y - midPointy;
                triad.pc.x = pc.x - midPointx;
                triad.pc.y = pc.y - midPointy;
                triad.ka = -(dcax * dabx + dcay * daby);
                triad.kb = -(dabx * dbcx + daby * dbcy);
                triad.kc = -(dbcx * dcax + dbcy * dcay);
                triad.s = v2.cross(pa, pb) + v2.cross(pb, pc) + v2.cross(pc, pa);
                system.m_triadCount++;
            }
        }

        ParticleSystem system;
        ParticleGroupDef def; 
        int firstIndex;
    }

    
    static class JoinParticleGroupsCallback implements VoronoiDiagramCallback {
        public void callback(int a, int b, int c) {

            var countA =
                    ((a < groupB.m_firstIndex) ? 1 : 0) + ((b < groupB.m_firstIndex) ? 1 : 0)
                            + ((c < groupB.m_firstIndex) ? 1 : 0);
            if (countA > 0 && countA < 3) {
                var af = system.m_flagsBuffer.data[a];
                var bf = system.m_flagsBuffer.data[b];
                var cf = system.m_flagsBuffer.data[c];
                if ((af & bf & cf & k_triadFlags) != 0) {
                    v2 pa = system.m_positionBuffer.data[a];
                    v2 pb = system.m_positionBuffer.data[b];
                    v2 pc = system.m_positionBuffer.data[c];
                    var dabx = pa.x - pb.x;
                    var daby = pa.y - pb.y;
                    var dbcx = pb.x - pc.x;
                    var dbcy = pb.y - pc.y;
                    var dcax = pc.x - pa.x;
                    var dcay = pc.y - pa.y;
                    var maxDistanceSquared = Settings.maxTriadDistanceSquared * system.m_squaredDiameter;
                    if (dabx * dabx + daby * daby < maxDistanceSquared
                            && dbcx * dbcx + dbcy * dbcy < maxDistanceSquared
                            && dcax * dcax + dcay * dcay < maxDistanceSquared) {
                        if (system.m_triadCount >= system.m_triadCapacity) {
                            var oldCapacity = system.m_triadCapacity;
                            var newCapacity =
                                    system.m_triadCount != 0
                                            ? 2 * system.m_triadCount
                                            : Settings.minParticleBufferCapacity;
                            system.m_triadBuffer =
                                    BufferUtils.reallocateBuffer(Triad.class, system.m_triadBuffer, oldCapacity,
                                            newCapacity);
                            system.m_triadCapacity = newCapacity;
                        }
                        var triad = system.m_triadBuffer[system.m_triadCount];
                        triad.indexA = a;
                        triad.indexB = b;
                        triad.indexC = c;
                        triad.flags = af | bf | cf;
                        triad.strength = MathUtils.min(groupA.m_strength, groupB.m_strength);
                        var midPointx = (float) 1 / 3 * (pa.x + pb.x + pc.x);
                        var midPointy = (float) 1 / 3 * (pa.y + pb.y + pc.y);
                        triad.pa.x = pa.x - midPointx;
                        triad.pa.y = pa.y - midPointy;
                        triad.pb.x = pb.x - midPointx;
                        triad.pb.y = pb.y - midPointy;
                        triad.pc.x = pc.x - midPointx;
                        triad.pc.y = pc.y - midPointy;
                        triad.ka = -(dcax * dabx + dcay * daby);
                        triad.kb = -(dabx * dbcx + daby * dbcy);
                        triad.kc = -(dbcx * dcax + dbcy * dcay);
                        triad.s = v2.cross(pa, pb) + v2.cross(pb, pc) + v2.cross(pc, pa);
                        system.m_triadCount++;
                    }
                }
            }
        }

        ParticleSystem system;
        ParticleGroup groupA;
        ParticleGroup groupB;
    }

    static class DestroyParticlesInShapeCallback implements ParticleQueryCallback {
        ParticleSystem system;
        Shape shape;
        Transform xf;
        boolean callDestructionListener;
        int destroyed;

        DestroyParticlesInShapeCallback() {
            
        }

        void init(ParticleSystem system, Shape shape, Transform xf,
                  boolean callDestructionListener) {
            this.system = system;
            this.shape = shape;
            this.xf = xf;
            this.destroyed = 0;
            this.callDestructionListener = callDestructionListener;
        }

        @Override
        public boolean reportParticle(int index) {
            assert (index >= 0 && index < system.m_count);
            if (shape.testPoint(xf, system.m_positionBuffer.data[index])) {
                system.destroyParticle(index, callDestructionListener);
                destroyed++;
            }
            return true;
        }
    }

    static class UpdateBodyContactsCallback implements Predicate<Fixture> {
        ParticleSystem system;

        private final v2 tempVec = new v2();

        @Override
        public boolean test(Fixture fixture) {
            if (fixture.isSensor() || fixture.filter.maskBits==0) {
                return true;
            }
            var shape = fixture.shape();
            var b = fixture.getBody();
            var bp = b.getWorldCenter();
            var bm = b.getMass();
            var bI = b.getInertia() - bm * b.getLocalCenter().lengthSquared();
            var invBm = bm > 0 ? 1 / bm : 0;
            var invBI = bI > 0 ? 1 / bI : 0;
            var childCount = shape.getChildCount();
            for (var childIndex = 0; childIndex < childCount; childIndex++) {
                var aabb = fixture.getAABB(childIndex);
                var aabblowerBoundx = aabb.lowerBound.x - system.m_particleDiameter;
                var aabblowerBoundy = aabb.lowerBound.y - system.m_particleDiameter;
                var aabbupperBoundx = aabb.upperBound.x + system.m_particleDiameter;
                var aabbupperBoundy = aabb.upperBound.y + system.m_particleDiameter;
                var firstProxy =
                        lowerBound(
                                system.m_proxyBuffer,
                                system.m_proxyCount,
                                computeTag(system.m_inverseDiameter * aabblowerBoundx, system.m_inverseDiameter
                                        * aabblowerBoundy));
                var lastProxy =
                        upperBound(
                                system.m_proxyBuffer,
                                system.m_proxyCount,
                                computeTag(system.m_inverseDiameter * aabbupperBoundx, system.m_inverseDiameter
                                        * aabbupperBoundy));

                for (var proxy = firstProxy; proxy != lastProxy; ++proxy) {
                    var a = system.m_proxyBuffer[proxy].index;
                    v2 ap = system.m_positionBuffer.data[a];
                    if (aabblowerBoundx <= ap.x && ap.x <= aabbupperBoundx && aabblowerBoundy <= ap.y
                            && ap.y <= aabbupperBoundy) {
                        var n = tempVec;
                        var d = fixture.distance(ap, childIndex, n);
                        if (d < system.m_particleDiameter) {
                            var invAm =
                                    (system.m_flagsBuffer.data[a] & ParticleType.b2_wallParticle) != 0 ? 0 : system
                                            .getParticleInvMass();
                            var rpx = ap.x - bp.x;
                            var rpy = ap.y - bp.y;
                            var rpn = rpx * n.y - rpy * n.x;
                            if (system.m_bodyContactCount >= system.m_bodyContactCapacity) {
                                var oldCapacity = system.m_bodyContactCapacity;
                                var newCapacity =
                                        system.m_bodyContactCount != 0
                                                ? 2 * system.m_bodyContactCount
                                                : Settings.minParticleBufferCapacity;
                                system.m_bodyContactBuffer =
                                        BufferUtils.reallocateBuffer(ParticleBodyContact.class,
                                                system.m_bodyContactBuffer, oldCapacity, newCapacity);
                                system.m_bodyContactCapacity = newCapacity;
                            }
                            var contact = system.m_bodyContactBuffer[system.m_bodyContactCount];
                            contact.index = a;
                            contact.body = b;
                            contact.weight = 1 - d * system.m_inverseDiameter;
                            contact.normal.x = -n.x;
                            contact.normal.y = -n.y;
                            contact.mass = 1 / (invAm + invBm + invBI * rpn * rpn);
                            system.m_bodyContactCount++;
                        }
                    }
                }
            }
            return true;
        }
    }

    static class SolveCollisionCallback implements Predicate<Fixture> {
        ParticleSystem system;
        TimeStep step;

        private final RayCastInput input = new RayCastInput();
        private final RayCastOutput output = new RayCastOutput();
        private final v2 tempVec = new v2();
        private final v2 tempv2 = new v2();

        @Override
        public boolean test(Fixture fixture) {
            if (fixture.isSensor()) {
                return true;
            }
            var shape = fixture.shape();
            var body = fixture.getBody();
            var childCount = shape.getChildCount();
            for (var childIndex = 0; childIndex < childCount; childIndex++) {
                var aabb = fixture.getAABB(childIndex);
                var aabblowerBoundx = aabb.lowerBound.x - system.m_particleDiameter;
                var aabblowerBoundy = aabb.lowerBound.y - system.m_particleDiameter;
                var aabbupperBoundx = aabb.upperBound.x + system.m_particleDiameter;
                var aabbupperBoundy = aabb.upperBound.y + system.m_particleDiameter;
                var firstProxy =
                        lowerBound(
                                system.m_proxyBuffer,
                                system.m_proxyCount,
                                computeTag(system.m_inverseDiameter * aabblowerBoundx, system.m_inverseDiameter
                                        * aabblowerBoundy));
                var lastProxy =
                        upperBound(
                                system.m_proxyBuffer,
                                system.m_proxyCount,
                                computeTag(system.m_inverseDiameter * aabbupperBoundx, system.m_inverseDiameter
                                        * aabbupperBoundy));

                for (var proxy = firstProxy; proxy != lastProxy; ++proxy) {
                    var a = system.m_proxyBuffer[proxy].index;
                    v2 ap = system.m_positionBuffer.data[a];
                    if (aabblowerBoundx <= ap.x && ap.x <= aabbupperBoundx && aabblowerBoundy <= ap.y
                            && ap.y <= aabbupperBoundy) {
                        v2 av = system.m_velocityBuffer.data[a];
                        var temp = tempVec;
                        Transform.mulTransToOutUnsafe(body.transformPrev, ap, temp);
                        Transform.mulToOutUnsafe(body, temp, input.p1);
                        input.p2.x = ap.x + step.dt * av.x;
                        input.p2.y = ap.y + step.dt * av.y;
                        input.maxFraction = 1;
                        if (fixture.raycast(output, input, childIndex)) {
                            var p = tempVec;
                            p.x =
                                    (1 - output.fraction) * input.p1.x + output.fraction * input.p2.x
                                            + Settings.linearSlop * output.normal.x;
                            p.y =
                                    (1 - output.fraction) * input.p1.y + output.fraction * input.p2.y
                                            + Settings.linearSlop * output.normal.y;

                            var vx = step.inv_dt * (p.x - ap.x);
                            var vy = step.inv_dt * (p.y - ap.y);
                            av.x = vx;
                            av.y = vy;
                            var particleMass = system.getParticleMass();
                            var ax = particleMass * (av.x - vx);
                            var ay = particleMass * (av.y - vy);
                            var b = output.normal;
                            var fdn = ax * b.x + ay * b.y;
                            var f = tempv2;
                            f.x = fdn * b.x;
                            f.y = fdn * b.y;
                            body.applyLinearImpulse(f, p, true);
                        }
                    }
                }
            }
            return true;
        }
    }

    static class Test {
        static boolean IsProxyInvalid(Proxy proxy) {
            return proxy.index < 0;
        }

        static boolean IsContactInvalid(ParticleContact contact) {
            return contact.indexA < 0 || contact.indexB < 0;
        }

        static boolean IsBodyContactInvalid(ParticleBodyContact contact) {
            return contact.index < 0;
        }

        static boolean IsPairInvalid(Pair pair) {
            return pair.indexA < 0 || pair.indexB < 0;
        }

        static boolean IsTriadInvalid(Triad triad) {
            return triad.indexA < 0 || triad.indexB < 0 || triad.indexC < 0;
        }
    }
}
