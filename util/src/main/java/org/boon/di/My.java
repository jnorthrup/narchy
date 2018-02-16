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

import jcog.list.FasterList;
import org.boon.Lists;
import org.boon.core.Supplier;
import org.boon.di.impl.ThatImpl;
import org.boon.di.modules.InstanceThing;
import org.boon.di.modules.SupplierThing;

import java.util.List;
import java.util.Map;

public class My {


    public static That of(final Thing... things ) {
        return new ThatImpl( things );
    }


    public static Thing clazz(Class c) {
        return classes(c);
    }

    public static Thing classes(Class... classes ) {
        List<Supply> wrap = Lists.wrap(Supply.class, classes);
        return new SupplierThing(wrap);
    }



    public static Thing object(Object objects ) {
        return objects(objects);
    }

    public static Thing objects(Object... objects ) {


        List<Supply> wrap = (List<Supply>) new FasterList(Supply.class, "objectProviderOf", objects);

        return new SupplierThing( wrap );
    }


    public static Thing prototype(Object o) {
        return prototypes(o);
    }

    public static Thing prototypes(Object... objects ) {

        List<Supply> wrap = (List<Supply>) new FasterList(Supply.class, "prototypeProviderOf", objects);

        return new SupplierThing( wrap );
    }

    public static Thing thing(Object module ) {

        return new InstanceThing( module );
    }

    public static <T> Thing supply(Class<T> type, Supplier<T> supplier) {
        return supply(Supply.of(type, supplier));
    }
    public static <T> Thing supply(String name, Supplier<T> supplier) {
        return supply(Supply.of(name, supplier));
    }

    public static Thing supply(Supply... suppliers ) {
        return new SupplierThing( suppliers );
    }

    public static That fromMap(Map<?, ?> map ) {
        return new ThatImpl(new SupplierThing( map ));
    }


}
