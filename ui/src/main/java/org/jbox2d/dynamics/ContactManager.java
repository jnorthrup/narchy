/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.jbox2d.dynamics;

import org.jbox2d.callbacks.ContactFilter;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.callbacks.PairCallback;
import org.jbox2d.collision.broadphase.BroadPhase;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.contacts.ContactEdge;
import org.jbox2d.dynamics.contacts.ContactRegister;
import org.jbox2d.fracture.FractureListener;
import org.jbox2d.pooling.IDynamicStack;

/**
 * Delegate of World.
 *
 * @author Daniel Murphy
 */
public class ContactManager implements PairCallback {

    public final BroadPhase m_broadPhase;
    public Contact m_contactList;
    public int m_contactCount;
    public ContactFilter m_contactFilter;
    public ContactListener m_contactListener;
    public FractureListener m_fractureListener;

    private final Dynamics2D dyn;

    private final ContactRegister[][] contactStacks =
            new ContactRegister[ShapeType.values().length][ShapeType.values().length];

    public ContactManager(Dynamics2D dyn, BroadPhase broadPhase) {
        m_contactList = null;
        m_contactCount = 0;
        m_contactFilter = new ContactFilter();
        m_contactListener = null;
        m_broadPhase = broadPhase;
        this.dyn = dyn;

        addType(this.dyn.pool.getCircleContactStack(), ShapeType.CIRCLE, ShapeType.CIRCLE);
        addType(this.dyn.pool.getPolyCircleContactStack(), ShapeType.POLYGON, ShapeType.CIRCLE);
        addType(this.dyn.pool.getPolyContactStack(), ShapeType.POLYGON, ShapeType.POLYGON);
        addType(this.dyn.pool.getEdgeCircleContactStack(), ShapeType.EDGE, ShapeType.CIRCLE);
        addType(this.dyn.pool.getEdgePolyContactStack(), ShapeType.EDGE, ShapeType.POLYGON);
        addType(this.dyn.pool.getChainCircleContactStack(), ShapeType.CHAIN, ShapeType.CIRCLE);
        addType(this.dyn.pool.getChainPolyContactStack(), ShapeType.CHAIN, ShapeType.POLYGON);
    }
    private void addType(IDynamicStack<Contact> creator, ShapeType type1, ShapeType type2) {
        ContactRegister register = new ContactRegister();
        register.creator = creator;
        register.primary = true;
        contactStacks[type1.ordinal()][type2.ordinal()] = register;

        if (type1 != type2) {
            ContactRegister register2 = new ContactRegister();
            register2.creator = creator;
            register2.primary = false;
            contactStacks[type2.ordinal()][type1.ordinal()] = register2;
        }
    }
    /**
     * Broad-phase callback.
     *
     * @param proxyUserDataA
     * @param proxyUserDataB
     */
    public void addPair(Object proxyUserDataA, Object proxyUserDataB) {
        FixtureProxy proxyA = (FixtureProxy) proxyUserDataA;
        FixtureProxy proxyB = (FixtureProxy) proxyUserDataB;

        Fixture fixtureA = proxyA.fixture;
        Fixture fixtureB = proxyB.fixture;

        int indexA = proxyA.childIndex;
        int indexB = proxyB.childIndex;

        Body bodyA = fixtureA.getBody();
        Body bodyB = fixtureB.getBody();

        // Are the fixtures on the same body?
        if (bodyA == bodyB) {
            return;
        }

        // TODO_ERIN use a hash table to remove a potential bottleneck when both
        // bodies have a lot of contacts.
        // Does a contact already exist?
        ContactEdge edge = bodyB.getContactList();
        while (edge != null) {
            if (edge.other == bodyA) {
                Fixture fA = edge.contact.getFixtureA();
                Fixture fB = edge.contact.getFixtureB();
                int iA = edge.contact.getChildIndexA();
                int iB = edge.contact.getChildIndexB();

                if (fA == fixtureA && iA == indexA && fB == fixtureB && iB == indexB) {
                    // A contact already exists.
                    return;
                }

                if (fA == fixtureB && iA == indexB && fB == fixtureA && iB == indexA) {
                    // A contact already exists.
                    return;
                }
            }

            edge = edge.next;
        }

        // Does a joint override collision? is at least one body dynamic?
        if (bodyB.shouldCollide(bodyA) == false) {
            return;
        }

        // Check user filtering.
        if (m_contactFilter != null && ContactFilter.shouldCollide(fixtureA, fixtureB) == false) {
            return;
        }

        // Call the factory.
        Contact c = popContact(fixtureA, indexA, fixtureB, indexB);
        if (c == null) {
            return;
        }

        // Contact creation may swap fixtures.
        fixtureA = c.getFixtureA();
        fixtureB = c.getFixtureB();
        indexA = c.getChildIndexA();
        indexB = c.getChildIndexB();
        bodyA = fixtureA.getBody();
        bodyB = fixtureB.getBody();

        // Insert into the world.
        c.m_prev = null;
        c.m_next = m_contactList;
        if (m_contactList != null) {
            m_contactList.m_prev = c;
        }
        m_contactList = c;

        // Connect to island graph.

        // Connect to body A
        c.m_nodeA.contact = c;
        c.m_nodeA.other = bodyB;

        c.m_nodeA.prev = null;
        c.m_nodeA.next = bodyA.m_contactList;
        if (bodyA.m_contactList != null) {
            bodyA.m_contactList.prev = c.m_nodeA;
        }
        bodyA.m_contactList = c.m_nodeA;

        // Connect to body B
        c.m_nodeB.contact = c;
        c.m_nodeB.other = bodyA;

        c.m_nodeB.prev = null;
        c.m_nodeB.next = bodyB.m_contactList;
        if (bodyB.m_contactList != null) {
            bodyB.m_contactList.prev = c.m_nodeB;
        }
        bodyB.m_contactList = c.m_nodeB;

        // wake up the bodies
        if (!fixtureA.isSensor() && !fixtureB.isSensor()) {
            bodyA.setAwake(true);
            bodyB.setAwake(true);
        }

        ++m_contactCount;
    }

    public void findNewContacts() {
        m_broadPhase.updatePairs(this);
    }

    public void destroy(final Contact c) {
        Fixture a = c.getFixtureA();
        Fixture b = c.getFixtureB();
        Body aa = a.getBody();
        Body bb = b.getBody();

        if (m_contactListener != null && c.isTouching()) {
            m_contactListener.endContact(c);
        }

        // Remove from the world.
        if (c.m_prev != null) {
            c.m_prev.m_next = c.m_next;
        }

        if (c.m_next != null) {
            c.m_next.m_prev = c.m_prev;
        }

        if (c == m_contactList) {
            m_contactList = c.m_next;
        }

        // Remove from body 1
        if (c.m_nodeA.prev != null) {
            c.m_nodeA.prev.next = c.m_nodeA.next;
        }

        if (c.m_nodeA.next != null) {
            c.m_nodeA.next.prev = c.m_nodeA.prev;
        }

        if (c.m_nodeA == aa.m_contactList) {
            aa.m_contactList = c.m_nodeA.next;
        }

        // Remove from body 2
        if (c.m_nodeB.prev != null) {
            c.m_nodeB.prev.next = c.m_nodeB.next;
        }

        if (c.m_nodeB.next != null) {
            c.m_nodeB.next.prev = c.m_nodeB.prev;
        }

        if (c.m_nodeB == bb.m_contactList) {
            bb.m_contactList = c.m_nodeB.next;
        }

        if (c.m_manifold.pointCount > 0 && !a.isSensor() && !b.isSensor()) {
            aa.setAwake(true);
            bb.setAwake(true);
        }

        //call the factory
        contactStacks[a.getType().ordinal()][b.getType().ordinal()].creator.push(c);

        --m_contactCount;
    }

    /**
     * This is the top level collision call for the time step. Here all the narrow phase collision is
     * processed for the world contact list.
     */
    public void collide() {
        // Update awake contacts.
        Contact c = m_contactList;
        while (c != null) {
            Fixture fixtureA = c.getFixtureA();
            Fixture fixtureB = c.getFixtureB();
            int indexA = c.getChildIndexA();
            int indexB = c.getChildIndexB();
            Body bodyA = fixtureA.getBody();
            Body bodyB = fixtureB.getBody();

            // is this contact flagged for filtering?
            if ((c.m_flags & Contact.FILTER_FLAG) == Contact.FILTER_FLAG) {
                // Should these bodies collide?
                if (bodyB.shouldCollide(bodyA) == false) {
                    Contact cNuke = c;
                    c = cNuke.getNext();
                    destroy(cNuke);
                    continue;
                }

                // Check user filtering.
                if (m_contactFilter != null && ContactFilter.shouldCollide(fixtureA, fixtureB) == false) {
                    Contact cNuke = c;
                    c = cNuke.getNext();
                    destroy(cNuke);
                    continue;
                }

                // Clear the filtering flag.
                c.m_flags &= ~Contact.FILTER_FLAG;
            }

            boolean activeA = bodyA.isAwake() && bodyA.m_type != BodyType.STATIC;
            boolean activeB = bodyB.isAwake() && bodyB.m_type != BodyType.STATIC;

            // At least one body must be awake and it must be dynamic or kinematic.
            if (activeA == false && activeB == false) {
                c = c.getNext();
                continue;
            }

            int proxyIdA = fixtureA.m_proxies[indexA].proxyId;
            int proxyIdB = fixtureB.m_proxies[indexB].proxyId;
            boolean overlap = m_broadPhase.testOverlap(proxyIdA, proxyIdB);

            // Here we destroy contacts that cease to overlap in the broad-phase.
            if (overlap == false) {
                Contact cNuke = c;
                c = cNuke.getNext();
                destroy(cNuke);
                continue;
            }

            // The contact persists.
            c.update(m_contactListener);
            c = c.getNext();
        }
    }

    public Contact popContact(Fixture fixtureA, int indexA, Fixture fixtureB, int indexB) {
        final ShapeType type1 = fixtureA.getType();
        final ShapeType type2 = fixtureB.getType();

        final ContactRegister reg = contactStacks[type1.ordinal()][type2.ordinal()];
        if (reg != null) {
            if (reg.primary) {
                Contact c = reg.creator.pop();
                c.init(fixtureA, indexA, fixtureB, indexB);
                return c;
            } else {
                Contact c = reg.creator.pop();
                c.init(fixtureB, indexB, fixtureA, indexA);
                return c;
            }
        } else {
            return null;
        }
    }

}
