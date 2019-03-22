/*
 * The MIT License
 *
 * Copyright 2015 Kamnev Georgiy (nt.gocha@gmail.com).
 *
 * Данная лицензия разрешает, безвозмездно, лицам, получившим копию данного программного 
 * обеспечения и сопутствующей документации (в дальнейшем именуемыми "Программное Обеспечение"), 
 * использовать Программное Обеспечение без ограничений, включая неограниченное право на 
 * использование, копирование, изменение, объединение, публикацию, распространение, сублицензирование 
 * и/или продажу копий Программного Обеспечения, также как и лицам, которым предоставляется 
 * данное Программное Обеспечение, при соблюдении следующих условий:
 *
 * Вышеупомянутый копирайт и данные условия должны быть включены во все копии 
 * или значимые части данного Программного Обеспечения.
 *
 * ДАННОЕ ПРОГРАММНОЕ ОБЕСПЕЧЕНИЕ ПРЕДОСТАВЛЯЕТСЯ «КАК ЕСТЬ», БЕЗ ЛЮБОГО ВИДА ГАРАНТИЙ, 
 * ЯВНО ВЫРАЖЕННЫХ ИЛИ ПОДРАЗУМЕВАЕМЫХ, ВКЛЮЧАЯ, НО НЕ ОГРАНИЧИВАЯСЬ ГАРАНТИЯМИ ТОВАРНОЙ ПРИГОДНОСТИ, 
 * СООТВЕТСТВИЯ ПО ЕГО КОНКРЕТНОМУ НАЗНАЧЕНИЮ И НЕНАРУШЕНИЯ ПРАВ. НИ В КАКОМ СЛУЧАЕ АВТОРЫ 
 * ИЛИ ПРАВООБЛАДАТЕЛИ НЕ НЕСУТ ОТВЕТСТВЕННОСТИ ПО ИСКАМ О ВОЗМЕЩЕНИИ УЩЕРБА, УБЫТКОВ 
 * ИЛИ ДРУГИХ ТРЕБОВАНИЙ ПО ДЕЙСТВУЮЩИМ КОНТРАКТАМ, ДЕЛИКТАМ ИЛИ ИНОМУ, ВОЗНИКШИМ ИЗ, ИМЕЮЩИМ 
 * ПРИЧИНОЙ ИЛИ СВЯЗАННЫМ С ПРОГРАММНЫМ ОБЕСПЕЧЕНИЕМ ИЛИ ИСПОЛЬЗОВАНИЕМ ПРОГРАММНОГО ОБЕСПЕЧЕНИЯ 
 * ИЛИ ИНЫМИ ДЕЙСТВИЯМИ С ПРОГРАММНЫМ ОБЕСПЕЧЕНИЕМ.
 */
package jcog.reflect;

import jcog.data.graph.Node;
import jcog.data.graph.path.Path;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Kamnev Georgiy (nt.gocha@gmail.com)
 */
public class TypeCastTest {


    @Test
    public void testString2byteNewAPI(){
        //BaseCastGraph bcast = new BaseCastGraph();
        ExtendedCastGraph t = new ExtendedCastGraph();

        List<Function<String, Byte>> convertors = t.applicable(String.class, byte.class);
        convertors.forEach(System.out::println);

        assertEquals((byte)9, (Object)convertors.get(0).apply("9"));
    }

//    @Test
//    public void test1(){
//        System.out.println("test1");
//        System.out.println("=====");
//        
//        TypeCastGraph tcast = new TypeCastGraph();
//        
//        tcast.setAt(Integer.class, int.class, new Convertor<Object, Object>() {
//            @Override
//            public Object convert(Object from) {
//                return (int)((Integer)from);
//            }
//        });
//
//        tcast.setAt(int.class, Integer.class, new Convertor<Object, Object>() {
//            @Override
//            public Object convert(Object from) {
//                return (Integer)from;
//            }
//        });
//
//        tcast.setAt(Integer.class, String.class, new Convertor<Object, Object>() {
//            @Override
//            public Object convert(Object from) {
//                return ((Integer)from).toString();
//            }
//        });
//        
//        Object v = 100;
//        
//        Map<Class,Convertor> m = tcast.getConvertorsFrom(v.getClass(), false, true, true, true);
//        for( Class tc : m.keySet() ){
//            System.out.println(""+tc);
//        }
//    }
    
    public interface Interface1 {
    }
    
    public interface Interface2 {
    }
    
    public static class Intf1Impl implements Interface1 {
    }
    
//    @Test
//    public void test2(){
//        System.out.println("test2");
//        System.out.println("=====");
//        
//        TypeCastGraph tcast = new TypeCastGraph();
//        
//        tcast.setAt(Interface1.class, Interface2.class, new Convertor<Object, Object>() {
//            @Override
//            public Object convert(Object from) {
//                return null;
//            }
//        });
//        
//        Class clsFrom = Intf1Impl.class;
//
//        Map<Class,Convertor> m = tcast.getConvertorsFrom(clsFrom, false, true, true, false);
//        
//        for( Class tc : m.keySet() ){
//            System.out.println(""+tc);
//        }
//        
//        int co = 0;
//        for( Class c : tcast.getNodes() ){
//            if( c.equals(clsFrom) )co++;
//        }
//        
//        assertTrue( co==0 );
//        assertTrue( m.keySet().size()>0 );
//    }
    
    @Test
    public void testFindStart(){
        System.out.println("testFindStart");
        System.out.println("=============");
        
        CastGraph tcast = new CastGraph();

        tcast.addEdge(Interface1.class, from -> null, Interface2.class);

        Class clsFrom = Intf1Impl.class;

        List<Class> m = tcast.roots(clsFrom, false, true, true, false);
        
        for( Class tc : m ){
            System.out.println(""+tc);
        }
        
        int co = 0;
        for( Node<Class,Function> c : tcast.nodes() ){
            if( c.id().equals(clsFrom) )
                co++;
        }
        
        assertTrue( co==0 );
        assertTrue( m.size()>0 );
    }
    
    @Test
    public void testString2byte(){
        //BaseCastGraph bcast = new BaseCastGraph();
        ExtendedCastGraph bcast = new ExtendedCastGraph();
        
        Class cfrom = String.class;
        Class cto = byte.class;
        
        System.out.println("from="+cfrom+" to="+cto);
        
        List<Class> roots = bcast.roots(cfrom, false, true, true, false);
        assert(!roots.isEmpty());

            System.out.println("convert variants:");
            int i = -1;
            for( Class cf : roots ){
                Path<Class, Function> path = bcast.pathFirst(cf, cto);
                if( path!=null ){
                    i++;
                    Converter sc = Converter.the(path);
                    System.out.println(i+": "+ sc);

                    Object y = sc.apply("9");
                    assertEquals((byte)9,y);
                }
            }

    }


    @Test
    public void testString2byteCast(){
        System.out.println("String2byteCast");
        System.out.println("===============");
        
        ExtendedCastGraph bcast = new ExtendedCastGraph();
        byte res = bcast.cast("12", byte.class);
        byte cmpRes = 12;
        assertTrue( res==cmpRes );
    }
}
