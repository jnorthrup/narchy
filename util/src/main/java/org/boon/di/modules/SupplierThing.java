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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.boon.Exceptions;
import org.boon.core.reflection.BeanUtils;
import org.boon.core.reflection.MapObjectConversion;
import org.boon.core.reflection.Reflection;
import org.boon.di.Supply;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.boon.di.modules.NamedUtils.namedValueForClass;

/**
 * Created by Richard on 2/3/14.
 */
public class SupplierThing extends BaseThing {

    private final Map<Class, Supply> supplierTypeMap = new ConcurrentHashMap<>();

    private final Multimap<String, Supply> supplierNameMap = HashMultimap.create();

    public SupplierThing(Supply... suppliers ) {
        supplierExtraction( suppliers );
    }


    public SupplierThing(List<Supply> suppliers ) {
        supplierExtraction( suppliers.toArray(new Supply[suppliers.size()]) );
    }



    @Override
    public Iterable<Object> values() {
        return (Iterable<Object>) (Object)supplierNameMap.values();
    }

    @Override
    public Iterable<String> names() {
        return supplierNameMap.keySet();
    }

    @Override
    public Iterable types() {
        return supplierTypeMap.keySet();
    }

    public SupplierThing(Map<?, ?> map ) {
        List<Supply> list = new ArrayList<>(  );

        for (Map.Entry<?,?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map <String, Object> valueMap = (Map <String, Object>) value;
                Supply pi = addProviderFromMapToList(key, valueMap);
                list.add( pi );

            } else {
                list.add( Supply.provider( key, value ));
            }
        }
        supplierExtraction( list.toArray( new Supply[list.size()] ) );
    }

    private static Supply addProviderFromMapToList(final Object key, final Map<String, Object> valueMap) {
        if (valueMap.containsKey( "class" )) {
            CharSequence className = (CharSequence) valueMap.get("class");
            if (className!=null) {
                try {

                    final Class<Object> type = (Class<Object>) Class.forName( className.toString());

                    final java.util.function.Supplier<Object> supplier = () -> MapObjectConversion.fromMap(valueMap);
                    return Supply.of( key.toString(),  type, supplier );
                } catch ( ClassNotFoundException e ) {
                    return Supply.provider( key, valueMap ) ;
                }

            }

        }
        return Supply.provider( key, valueMap ) ;

    }


    @Override
    public <T> T a(Class<T> type ) {
        Supply pi = supplierTypeMap.get(type);
        if (pi!=null) {
            return (T) pi.supplier().get();
        }
        return null;

    }

    @Override
    public Object a(String name ) {

        Supply pi = supplierNameMap.get(name).iterator().next();
        if (pi!=null) {
            return pi.supplier().get();
        }
        return null;


    }

    @Override
    public <T> T a(Class<T> type, String name ) {
        Supply providerInfo = supply(type, name);
        if (providerInfo!=null) {
            return (T)providerInfo.supplier().get();
        }
        return null;
    }

    @Override
    public Supply supply(Class<?> type) {
        return supplierTypeMap.get(type);
    }

    @Override
    public Supply supply(String name) {
        return supplierNameMap.get(name).iterator().next();
    }

    @Override
    public Supply supply(Class<?> type, String name) {
        return doGetProvider(type, name);
    }


    @Override
    public <T> java.util.function.Supplier<T> supplying(final Class<T> type, final String name ) {


        Supply nullInfo = null;

        try {
            Collection<Supply> set = /*Sets.set*/( supplierNameMap.get( name ) ); //Set<>

            for ( Supply info : set ) {

                if ( info.type() == null ) {
                    nullInfo = info;
                    continue;
                }
                if ( type.isAssignableFrom( info.type() ) ) {
                    return info.supplier();
                }
            }

            return ( nullInfo != null ? nullInfo.supplier() : () -> null);

        } catch ( Exception e ) {
            Exceptions.handle( e );
            return null;
        }
    }


    private Supply doGetProvider(final Class<?> type, final String name ) {


        Collection<Supply> set = ( supplierNameMap.get( name ) ); //Set<>


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
    public <T> java.util.function.Supplier<T> supplying(Class<T> type ) {
        java.util.function.Supplier<T> supplier = (java.util.function.Supplier<T>) supplierTypeMap.get( type );
        if (supplier == null ) {
            supplier = () -> null;
        }

        return supplier;
    }


    @Override
    public boolean has( Class type ) {
        return supplierTypeMap.containsKey( type );
    }

    @Override
    public boolean has( String name ) {
        return supplierNameMap.containsKey( name );
    }


    private void extractClassIntoMaps(Supply info, Class type, boolean foundName, java.util.function.Supplier supplier ) {

        if ( type == null ) {
            return;
        }
        String named = null;


        Class superClass = type.getSuperclass();


        Class[] superTypes = type.getInterfaces();

        for ( Class superType : superTypes ) {
            this.supplierTypeMap.put( superType, info );
        }

        while ( superClass != Object.class ) {
            this.supplierTypeMap.put( superClass, info );

            if ( !foundName ) {
                named = NamedUtils.namedValueForClass( superClass );
                if ( named != null ) {
                    supplierNameMap.put( named, new Supply( named, type, supplier, null ) );
                    foundName = true;
                }
            }

            superTypes = type.getInterfaces();
            for ( Class superType : superTypes ) {
                this.supplierTypeMap.put( superType, info );
            }
            superClass = superClass.getSuperclass();
        }


    }


    private void supplierExtraction( Supply[] suppliers ) {
        for ( Supply providerInfo : suppliers ) {

            Class<?> type = providerInfo.type();
            /* Get type from value. */
            if ( type == null && providerInfo.value() != null ) {
                type = providerInfo.value().getClass();

            }
            String named = providerInfo.name();
            java.util.function.Supplier supplier = providerInfo.supplier();

            if ( supplier == null ) {
                supplier = createSupplier( providerInfo.prototype(), type, providerInfo.value() );
                providerInfo = new Supply(named, type, supplier, providerInfo.value());
            }

            if ( type != null ) {

                supplierTypeMap.put( type, providerInfo );


                /* Named passed in overrides name in class annotation @Named. */
                if ( named == null ) {

                    named = namedValueForClass( type );
                }
            }

            extractClassIntoMaps( providerInfo, type, named != null, supplier );
            if ( named != null ) {
                supplierNameMap.put( named, new Supply( named, type, supplier, providerInfo.value() ) );
            }
        }
    }


    private static java.util.function.Supplier createSupplier(final boolean prototype, final Class<?> type, final Object value) {
        if ( value != null && !prototype) {
            return () -> value;
        } else if (value!=null && prototype) {
            return () -> BeanUtils.copy(value);
        } else if ( type != null ) {
            return () -> Reflection.newInstance( type );
        } else {
            return () -> null;
        }
    }



}