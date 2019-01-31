/*
 * The MIT License
 *
 * Copyright 2017 Kamnev Georgiy <nt.gocha@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jcog.reflect;

import jcog.reflect.graph.Edge;
import jcog.reflect.graph.Path;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author user
 */
public class TypeCastTest2 {
    


    // TODO addAt test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    @Test
    public void test01(){
        ExtendedCastGraph tc = new ExtendedCastGraph();
        
        List<Path<Class, Function>> lp1 = tc.paths(String.class, BigDecimal.class);
        System.out.println("lp1 size = "+lp1.size());
        
        for( Path<Class,Function> p : lp1 ){
            System.out.println("path");
            for( Edge<Class,Function> e : p.fetch(0, p.nodeCount()) ){
                System.out.println(""+e.getEdge());
            }
        }
        
        List<Path<Class,Function>> lp2 = tc.paths(BigDecimal.class, int.class);
        System.out.println("lp2 size = "+lp2.size());
        //lp2.forEach( p -> {
        for( Path<Class,Function> p : lp2 ){
            System.out.println("path");
            //p.forEach( e -> {
            for( Edge<Class,Function> e : p.fetch(0, p.nodeCount()) ){
                System.out.println(""+e.getEdge());
            }
        }
        
        // lp1.forEach( p1 -> {
        for( Path<Class,Function> p1 : lp1 ){
            // lp2.forEach( p2 -> {
            for( Path<Class,Function> p2 : lp2 ){
                List<Function> path = new LinkedList<Function>();
                //p1.forEach( e1 -> { path.addAt(e1.getEdge()); } );
                for( Edge<Class,Function> e1 : p1.fetch(0, p1.nodeCount()) ){
                    path.add(e1.getEdge());
                }
                //p2.forEach( e2 -> { path.addAt(e2.getEdge()); } );
                for( Edge<Class,Function> e2 : p2.fetch(0, p2.nodeCount()) ){
                    path.add(e2.getEdge());
                }
                
                Converter sc = new Converter( path );
                sc.setWeight(0.8);
                
                tc.set(String.class, int.class, sc);                
            }
        }
        // SequenceCaster sc = new SequenceCaster
        
        int v = tc.cast("1.0", int.class);
        System.out.println("v="+v);
    }
}
