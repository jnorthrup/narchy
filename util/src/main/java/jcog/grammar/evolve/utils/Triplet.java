/*
 * Copyright (C) 2015 Machine Learning Lab - University of Trieste, 
 * Italy (http:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package jcog.grammar.evolve.utils;

import java.util.Objects;

/**
 *
 * @author MaleLabTs
 */
public class Triplet<F, S, T> {
    private final F first;
    private final S second;
    private final T third;

    public Triplet(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
    
    public T getThird() {
        return third;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Triplet<F, S, T> other = (Triplet<F, S, T>) obj;
        if (!Objects.equals(first, first)) {
            return false;
        }
        if (!Objects.equals(second, second)) {
            return false;
        }
        return Objects.equals(third, third);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.first != null ? this.first.hashCode() : 0);
        hash = 29 * hash + (this.second != null ? this.second.hashCode() : 0);
        hash = 29 * hash + (this.third != null ? this.third.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "Triplet{" + "first=" + first + " second=" + second + " third= "+third+'}';
    }

}
