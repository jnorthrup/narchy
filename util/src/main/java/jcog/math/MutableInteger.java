/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package jcog.math;


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.function.IntSupplier;

/**
 * A mutable <code>integer</code> wrapper.
 */
public class MutableInteger extends Number implements Comparable, Mutable,IntSupplier {


    /**
     * The mutable value.
     */
    private int value;

    /**
     * Constructs a new MutableDouble with the default value of zero.
     */
    public MutableInteger() {
    }

    /**
     * Constructs a new MutableDouble with the specified value.
     *
     * @param value a value.
     */
    public MutableInteger(int value) {
        this.value = value;
    }

    /**
     * Constructs a new MutableDouble with the specified value.
     *
     * @param value a value.
     * @throws NullPointerException if the object is null
     */
    public MutableInteger(Number value) {
        this.value = value.intValue();
    }

    

    /**
     * Gets the value as a Double instance.
     *
     * @return the value as a Double
     */
    @Override
    public Object get() {
        return (double) value;
    }

    @Override
    public void set(Object value) {
        if (value instanceof Number)
            set((Number) value);
        else
            throw new RuntimeException("not number: " + value.getClass());
    }


    /**
     * Sets the value.
     *
     * @param value the value to set
     */
    public void set(int value) {
        this.value = value;
    }

    /**
     * Sets the value from any Number instance.
     *
     * @param value the value to set
     * @throws NullPointerException if the object is null
     * @throws ClassCastException   if the type is not a {@link Number}
     */
    public void set(Number value) {
        set(Math.round(value.floatValue()));
    }

    
    

    /**
     * Returns the value of this MutableDouble as a int.
     *
     * @return the numeric value represented by this object after conversion to
     * type int.
     */
    @Override
    public int intValue() {
        return value;
    }

    @Override
    public final int getAsInt() {
        return intValue();
    }

    /**
     * Returns the value of this MutableDouble as a long.
     *
     * @return the numeric value represented by this object after conversion to
     * type long.
     */
    @Override
    public long longValue() {
        return value;
    }

    /**
     * Returns the value of this MutableDouble as a float.
     *
     * @return the numeric value represented by this object after conversion to
     * type float.
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableDouble as a double.
     *
     * @return the numeric value represented by this object after conversion to
     * type double.
     */
    @Override
    public double doubleValue() {
        return value;
    }

    /**
     * Checks whether the double value is the special NaN value.
     *
     * @return true if NaN
     */
    public boolean isNaN() {
        return false;
    }

    /**
     * Checks whether the double value is infinite.
     *
     * @return true if infinite
     */
    public static boolean isInfinite() {
        return false;
    }

    

    /**
     * Gets this mutable as an instance of Double.
     *
     * @return a Double instance containing the value from this mutable
     */
    public Double toDouble() {
        return doubleValue();
    }

    

    /**
     * Increments the value.
     *
     * @since Commons Lang 2.2
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value.
     *
     * @since Commons Lang 2.2
     */
    public void decrement() {
        value--;
    }

    

    /**
     * Adds a value.
     *
     * @param operand the value to add
     * @since Commons Lang 2.2
     */
    public void add(double operand) {
        value += operand;
    }

    /**
     * Adds a value.
     *
     * @param operand the value to add
     * @throws NullPointerException if the object is null
     * @since Commons Lang 2.2
     */
    public void add(Number operand) {
        value += operand.doubleValue();
    }

    /**
     * Subtracts a value.
     *
     * @param operand the value to add
     * @since Commons Lang 2.2
     */
    public void subtract(double operand) {
        value -= operand;
    }

    /**
     * Subtracts a value.
     *
     * @param operand the value to add
     * @throws NullPointerException if the object is null
     * @since Commons Lang 2.2
     */
    public void subtract(Number operand) {
        value -= operand.doubleValue();
    }

    

    /**
     * Compares this object against the specified object. The result is
     * <code>true</code> if and only if the argument is not <code>null</code>
     * and is a <code>Double</code> object that represents a double that has the
     * identical bit pattern to the bit pattern of the double represented by this
     * object. For this purpose, two <code>double</code> values are considered
     * to be the same if and only if the method
     * {@link Double#doubleToLongBits(double)}returns the same long value when
     * applied to each.
     * <p>
     * Note that in most cases, for two instances of class <code>Double</code>,<code>d1</code>
     * and <code>d2</code>, the value of <code>d1.equals(d2)</code> is
     * <code>true</code> if and only if <blockquote>
     * <p>
     * <pre>
     * d1.doubleValue() == d2.doubleValue()
     * </pre>
     * <p>
     * </blockquote>
     * <p>
     * also has the value <code>true</code>. However, there are two exceptions:
     * <ul>
     * <li>If <code>d1</code> and <code>d2</code> both represent
     * <code>Double.NaN</code>, then the <code>equals</code> method returns
     * <code>true</code>, even though <code>Double.NaN==Double.NaN</code> has
     * the value <code>false</code>.
     * <li>If <code>d1</code> represents <code>+0.0</code> while
     * <code>d2</code> represents <code>-0.0</code>, or vice versa, the
     * <code>equal</code> test has the value <code>false</code>, even though
     * <code>+0.0==-0.0</code> has the value <code>true</code>. This allows
     * hashtables to operate properly.
     * </ul>
     *
     * @param obj the object to compare with.
     * @return <code>true</code> if the objects are the same; <code>false</code>
     * otherwise.
     */
    public boolean equals(Object obj) {
        return ((MutableInteger) obj).value == value;
    }

    /**
     * Returns a suitable hashcode for this mutable.
     *
     * @return a suitable hashcode
     */
    public int hashCode() {
        return Integer.hashCode(value);
    }

    /**
     * Compares this mutable to another in ascending order.
     *
     * @param obj the mutable to compare to
     * @return negative if this is less, zero if equal, positive if greater
     * @throws ClassCastException if the argument is not a MutableDouble
     */
    @Override
    public int compareTo(Object obj) {
        return Integer.compare(value, ((MutableInteger) obj).value);
    }

    /**
     * Returns the String value of this mutable.
     *
     * @return the mutable value as a string
     */
    public String toString() {
        return String.valueOf(value);
    }

    public int getAndSet(int x) {
        int p = this.value;
        this.value = x;
        return p;
    }














}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

    