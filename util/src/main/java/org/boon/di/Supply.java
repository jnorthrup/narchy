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

package org.boon.di;

/**
 * Created by Richard on 2/3/14.
 */
public class Supply<T> {

    private String name;
    private final Class<T> type;
    private java.util.function.Supplier<T> source;
    private T object;


    private boolean postConstructCalled;


    boolean prototype;


    public Supply(Class<T> type) {
        this.type = type;
    }

//    public ProviderInfo( T object) {
//        this.object = object;
//    }

    public Supply(String name, Class<T> type, java.util.function.Supplier<T> source, T object ) {
        this.name = name;
        this.type = type;
        this.source = source;
        this.object = object;
    }


    public Supply(String name, Class<T> type, java.util.function.Supplier<T> source, T object, boolean prototype ) {
        this.name = name;
        this.type = type;
        this.source = source;
        this.object = object;
        this.prototype = prototype;
    }

    public static <T> Supply<T> of(Class<T> type, java.util.function.Supplier<T> supplier ) {
        return new Supply<>( null, type, supplier, null );
    }


    public static <T> Supply<T> of(String name, Class<T> type, java.util.function.Supplier<T> supplier ) {
        return new Supply<>( name, type, supplier, null );
    }


    public static <T> Supply<T> of(String name, java.util.function.Supplier<T> supplier ) {
        return new Supply<>( name, null, supplier, null );
    }


    public static <T> Supply<T> of(String name, Class<T> type ) {
        return new Supply<>( name, type, null, null );
    }


    public static <T> Supply<T> of(Class<T> type ) {
        return new Supply<>( null, type, null, null );
    }


    public static <T> Supply<T> of(String name, T object ) {
        return new Supply<>( name, null, null, object );
    }


    public static <T> Supply<T> of(T object ) {
        return new Supply<>( null, null, null, object );
    }


    public static <T> Supply<T> objectProviderOf(T object ) {
        return new Supply<>( null, null, null, object );
    }


    public static <T> Supply<T> prototypeProviderOf(T object ) {
        return new Supply<>( null, null, null, object, true );
    }

    public static <T> Supply<T> provider(Object name, Object value ) {

        Supply info;

        if (value instanceof Supply) {
            Supply valueInfo = (Supply) value;
            info  = new Supply( name.toString(), valueInfo.type(), valueInfo.supplier(), valueInfo.value()  );
        } else if (value instanceof Class) {
            info = new Supply( name.toString(), (Class) value, null, null  );
        } else if (value instanceof java.util.function.Supplier) {
            info = new Supply( name.toString(), null, (java.util.function.Supplier) value, null  );
        } else  {
            if (value == null) {
                info = new Supply( name.toString(), null, null, value  );
            }  else {
                info = new Supply( name.toString(), value.getClass(), null, value  );
            }
        }
        return info;
    }

    public Class<T> type() {
        return type;
    }

    public java.util.function.Supplier<T> supplier() {
        return source;
    }

    public String name() {
        return name;
    }

    public Object value() {
        return object;
    }


    public boolean isPostConstructCalled() {
        return postConstructCalled;
    }

    public void setPostConstructCalled(boolean postConstructCalled) {
        this.postConstructCalled = postConstructCalled;
    }


    public boolean prototype() {
        return prototype;
    }
}
