package spacegraph.space2d.phys.dynamics.joints;

import spacegraph.space2d.phys.common.MathUtils;
import spacegraph.space2d.phys.common.Rot;
import spacegraph.space2d.phys.common.Settings;
import spacegraph.space2d.phys.dynamics.SolverData;
import spacegraph.space2d.phys.dynamics.contacts.Velocity;
import spacegraph.space2d.phys.pooling.IWorldPool;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

/**
 * A rope joint enforces a maximum distance between two points on two bodies. It has no other
 * effect. Warning: if you attempt to change the maximum length during the simulation you will get
 * some non-physical behavior. A model that would allow you to dynamically modify the length would
 * have some sponginess, so I chose not to implement it that way. See DistanceJoint if you want to
 * dynamically control length.
 *
 * @author Daniel Murphy
 */
public class RopeJoint extends Joint {
    
    private final Tuple2f localAnchorA = new v2();
    private final Tuple2f localAnchorB = new v2();
    private float targetLength;
    private float length;
    private float m_impulse;

    
    private int indexA;
    private int indexB;
    private final Tuple2f m_u = new v2();
    private final Tuple2f m_rA = new v2();
    private final Tuple2f m_rB = new v2();
    private final Tuple2f m_localCenterA = new v2();
    private final Tuple2f m_localCenterB = new v2();
    private float m_invMassA;
    private float m_invMassB;
    private float m_invIA;
    private float m_invIB;
    private float m_mass;
    private LimitState state;
    private float positionFactor = 1f;

    public RopeJoint(IWorldPool worldPool, RopeJointDef def) {
        super(worldPool, def);
        localAnchorA.set(def.localAnchorA);
        localAnchorB.set(def.localAnchorB);

        targetLength = def.maxLength;

        m_mass = 0.0f;
        m_impulse = 0.0f;
        state = LimitState.INACTIVE;
        length = 0.0f;
    }

    protected float targetLength() {
        return targetLength;
    }

    @Override
    public void initVelocityConstraints(final SolverData data) {
        indexA = A.island;
        indexB = B.island;
        m_localCenterA.set(A.sweep.localCenter);
        m_localCenterB.set(B.sweep.localCenter);
        m_invMassA = A.m_invMass;
        m_invMassB = B.m_invMass;
        m_invIA = A.m_invI;
        m_invIB = B.m_invI;

        Tuple2f cA = data.positions[indexA];
        float aA = data.positions[indexA].a;
        Tuple2f vA = data.velocities[indexA];
        float wA = data.velocities[indexA].w;

        Tuple2f cB = data.positions[indexB];
        float aB = data.positions[indexB].a;
        Tuple2f vB = data.velocities[indexB];
        float wB = data.velocities[indexB].w;

        final Rot qA = new Rot();
        final Rot qB = new Rot();
        final Tuple2f temp = new v2();

        qA.set(aA);
        qB.set(aB);

        
        Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subbed(m_localCenterA), m_rA);
        Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subbed(m_localCenterB), m_rB);

        m_u.set(cB).added(m_rB).subbed(cA).subbed(m_rA);

        length = m_u.length();

        float C = length - targetLength();
        float ca = Math.abs(C);

        if (length > Settings.linearSlop) {

            m_u.scaled(1.0f / length);

                state = LimitState.AT_UPPER;











        } else {
            state = LimitState.INACTIVE;
            m_u.setZero();
            m_mass = 0.0f;
            m_impulse = 0.0f;
            length = 0;
            return;
        }

        
        float crA = Tuple2f.cross(m_rA, m_u);
        float crB = Tuple2f.cross(m_rB, m_u);
        float invMass = m_invMassA + m_invIA * crA * crA + m_invMassB + m_invIB * crB * crB;

        m_mass = invMass != 0.0f ? 1.0f / invMass : 0.0f;


            
            m_impulse *= data.step.dtRatio * positionFactor;

            float Px = m_impulse * m_u.x;
            float Py = m_impulse * m_u.y;
            vA.x -= m_invMassA * Px;
            vA.y -= m_invMassA * Py;
            wA -= m_invIA * (m_rA.x * Py - m_rA.y * Px);

            vB.x += m_invMassB * Px;
            vB.y += m_invMassB * Py;
            wB += m_invIB * (m_rB.x * Py - m_rB.y * Px);






        
        data.velocities[indexA].w = wA;
        
        data.velocities[indexB].w = wB;
    }



    @Override
    public void solveVelocityConstraints(final SolverData data) {

        float targetLength = targetLength();

        Velocity VA = data.velocities[indexA];
        Tuple2f vA = VA;
        float wA = VA.w;
        Velocity VB = data.velocities[indexB];
        Tuple2f vB = VB;
        float wB = VB.w;

        
        Tuple2f vpA = pool.popVec2();
        Tuple2f vpB = pool.popVec2();
        Tuple2f temp = pool.popVec2();

        Tuple2f.crossToOutUnsafe(wA, m_rA, vpA);
        vpA.added(vA);
        Tuple2f.crossToOutUnsafe(wB, m_rB, vpB);
        vpB.added(vB);

        float dLen = length - targetLength;
        float Cdot = Tuple2f.dot(m_u, temp.set(vpB).subbed(vpA))
                
        ;

        
        
            Cdot += data.step.inv_dt * Math.abs(dLen) * positionFactor;
        

        float impulse = -m_mass * Cdot;
        float oldImpulse = m_impulse;
        m_impulse = MathUtils.min(0.0f, m_impulse + impulse);
        impulse = m_impulse - oldImpulse;

        float Px = impulse * m_u.x;
        float Py = impulse * m_u.y;

        vA.x -= m_invMassA * Px;
        vA.y -= m_invMassA * Py;

        VA.w = wA - m_invIA * (m_rA.x * Py - m_rA.y * Px);
        vB.x += m_invMassB * Px;
        vB.y += m_invMassB * Py;
        VB.w = wB + m_invIB * (m_rB.x * Py - m_rB.y * Px);

        pool.pushVec2(3);

        
        
    }

    @Override
    public boolean solvePositionConstraints(final SolverData data) {

        final float targetLength = targetLength();

        Tuple2f cA = data.positions[indexA];
        float aA = data.positions[indexA].a;
        Tuple2f cB = data.positions[indexB];
        float aB = data.positions[indexB].a;

        final Rot qA = pool.popRot();
        final Rot qB = pool.popRot();
        final Tuple2f u = pool.popVec2();
        final Tuple2f rA = pool.popVec2();
        final Tuple2f rB = pool.popVec2();
        final Tuple2f temp = pool.popVec2();

        qA.set(aA);
        qB.set(aB);

        
        Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subbed(m_localCenterA), rA);
        Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subbed(m_localCenterB), rB);
        u.set(cB).added(rB).subbed(cA).subbed(rA);

        float length = u.normalize();
        float C = length - targetLength;



        float impulse = -m_mass * C;
        float Px = impulse * u.x;
        float Py = impulse * u.y;

        cA.x -= m_invMassA * Px;
        cA.y -= m_invMassA * Py;
        aA -= m_invIA * (rA.x * Py - rA.y * Px);
        cB.x += m_invMassB * Px;
        cB.y += m_invMassB * Py;
        aB += m_invIB * (rB.x * Py - rB.y * Px);

        pool.pushRot(2);
        pool.pushVec2(4);

        
        data.positions[indexA].a = aA;
        
        data.positions[indexB].a = aB;

        return Math.abs(length - targetLength) < Settings.linearSlop;
    }

    @Override
    public void getAnchorA(Tuple2f argOut) {
        A.getWorldPointToOut(localAnchorA, argOut);
    }

    @Override
    public void getAnchorB(Tuple2f argOut) {
        B.getWorldPointToOut(localAnchorB, argOut);
    }

    @Override
    public void getReactionForce(float inv_dt, Tuple2f argOut) {
        argOut.set(m_u).scaled(inv_dt).scaled(m_impulse * positionFactor);
    }

    @Override
    public float getReactionTorque(float inv_dt) {
        return 0f;
    }

    public Tuple2f getLocalAnchorA() {
        return localAnchorA;
    }

    public Tuple2f getLocalAnchorB() {
        return localAnchorB;
    }

    public void setTargetLength(float targetLength) {
        this.targetLength = targetLength;
    }

    public void setPositionFactor(float positionFactor) {
        this.positionFactor = positionFactor;
    }





}
