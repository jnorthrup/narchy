/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */

package org.boon.di.modules;

import org.boon.Exceptions;
import org.boon.Sets;
import org.boon.collections.MultiMapImpl;
import org.boon.core.Supplier;
import org.boon.core.reflection.ClassMeta;
import org.boon.core.reflection.MethodAccess;
import org.boon.di.Supply;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class InstanceThing extends BaseThing {

    private final Map<Class, Supply> supplierTypeMap = new ConcurrentHashMap<>();
    private final MultiMapImpl<String, Supply> supplierNameMap = new MultiMapImpl<>();

    private final Object module;


    public InstanceThing(Object object ) {
        module = object;

        ClassMeta classMeta = ClassMeta.classMeta(object.getClass());
        Iterable<MethodAccess> methods =  (Iterable<MethodAccess>)classMeta.methods();

        for ( MethodAccess method : methods) {

            if ( !method.isStatic() && method.name().startsWith( "provide" ) ) {
                addCreationMethod( method, false );
            } else if (!method.isStatic() && method.name().startsWith( "create" )) {
                addCreationMethod( method, true );

            }
        }


    }

    private static class InternalSupplier implements Supplier<Object> {

        private final Object module;

        private final MethodAccess method;

        InternalSupplier( MethodAccess method, Object module ) {
            this.method = method;
            this.module = module;
        }

        @Override
        public Object get() {
            try {
                return method.invoke( module );
            } catch ( Exception e ) {
                return Exceptions.handle( Object.class, e );
            }
        }
    }

    private Supplier<Object> createSupplier( final MethodAccess method ) {
        return new InternalSupplier( method, module );
    }

    @Override
    public <T> T a(Class<T> type ) {


        Supply pi = supplierTypeMap.get(type);
        if (pi!=null) {
            return (T)pi.supplier().get();
        }
        return null;


    }

    @Override
    public Object a(String name ) {


        Supply pi = supplierNameMap.get(name);
        if (pi!=null) {
            return pi.supplier().get();
        }
        return null;

    }


    @Override
    public <T> T a(Class<T> type, String name ) {

        return supplying( type, name ).get();
    }

    @Override
    public Supply supply(Class<?> type) {
        return this.supplierTypeMap.get(type);
    }

    @Override
    public Supply supply(String name) {
        return this.supplierNameMap.get(name);
    }

    @Override
    public Supply supply(Class<?> type, String name) {
        return doGetProvider(type, name);
    }



    private Supply doGetProvider(final Class<?> type, final String name ) {


        Set<Supply> set = Sets.set( supplierNameMap.getAll( name ) );


        Supply nullTypeInfo = null;

        for ( Supply info : set ) {

            if ( info.type() == null ) {
                nullTypeInfo = info;

                continue;
            }
            if ( type.isAssignableFrom( info.type() ) ) {
                return info;
            }
        }
        return nullTypeInfo;
    }


    @Override
    public <T> Supplier<T> supplying(final Class<T> type, final String name ) {

        try {
            Set<Supply> set = Sets.set(supplierNameMap.getAll(name));
            for ( Supply s : set ) {
                InternalSupplier supplier = ( InternalSupplier ) s.supplier();
                if ( type.isAssignableFrom( supplier.method.returnType() ) ) {
                    return (Supplier<T>)supplier;
                }
            }

            return () -> null;

        } catch ( Exception e ) {
            Exceptions.handle( e );
            return null;
        }

    }

    @Override
    public <T> Supplier<T> supplying(Class<T> type ) {
        Supplier<T> supplier = ( Supplier<T> ) supplierTypeMap.get( type );
        if (supplier == null ) {
            supplier = () -> null;
        }

        return supplier;
    }

    @Override
    public Iterable<Object> values() {
        return (Iterable<Object>) (Object) supplierTypeMap.values();
    }

    @Override
    public Iterable<String> names() {

        return supplierNameMap.keySet();
    }

    @Override
    public Iterable types() {
        return supplierTypeMap.keySet();
    }

    @Override
    public boolean has( Class type ) {
        return supplierTypeMap.containsKey( type );
    }

    @Override
    public boolean has( String name ) {
        return supplierNameMap.containsKey( name );
    }


    private void addCreationMethod(MethodAccess method, boolean creates) {

        Object providerInfo=creates ? null : new Object();

        /** See if the name is in the method and that one takes precedence if found. */
        String named = NamedUtils.namedValueForMethod( method );
        boolean foundName = named != null;

        Class cls = method.returnType();


        /* Next see if named is in the class. */
        if ( !foundName ) {
            named = NamedUtils.namedValueForClass( cls );
            foundName = named != null;
        }

        Supplier<Object> supplier = createSupplier( method );
        this.supplierTypeMap.put(cls, new Supply(named, cls, supplier, providerInfo));

        Class superClass = cls.getSuperclass();


        Class[] superTypes = cls.getInterfaces();

        for ( Class superType : superTypes ) {
            this.supplierTypeMap.put(superType, new Supply(named, cls, supplier, providerInfo));
        }

        if ( superClass != null ) {
            while ( superClass != Object.class ) {
                this.supplierTypeMap.put(superClass, new Supply(named, cls, supplier, providerInfo));

                  /* Next see if named is in the super if not found. */
                if ( !foundName ) {
                    named = NamedUtils.namedValueForClass( cls );
                    foundName = named != null;
                }
                superTypes = cls.getInterfaces();
                for ( Class superType : superTypes ) {
                    this.supplierTypeMap.put(superType, new Supply(named, cls, supplier, providerInfo));
                }
                superClass = superClass.getSuperclass();
            }
        }


        if ( foundName ) {
            this.supplierNameMap.put(named, new Supply(named, cls, supplier, providerInfo));
        }

    }
}
