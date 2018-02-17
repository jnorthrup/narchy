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

package org.boon.datarepo.spi;

import org.boon.core.Function;
import org.boon.core.Typ;
import org.boon.datarepo.Filter;
import org.boon.datarepo.LookupIndex;
import org.boon.datarepo.RepoBuilder;
import org.boon.datarepo.impl.*;
import org.boon.datarepo.impl.indexes.LookupIndexDefault;
import org.boon.datarepo.impl.indexes.SearchIndexDefault;
import org.boon.datarepo.impl.indexes.UniqueLookupIndex;
import org.boon.datarepo.impl.indexes.UniqueSearchIndex;
import org.boon.datarepo.impl.maps.MapCreatorImpl;


/**
 * Helper class for SPIFactory interface.
 */
public class SPIFactory {

    static java.util.function.Supplier<MapCreator> mapCreatorFactory = null;
    static java.util.function.Supplier<RepoBuilder> repoBuilderFactory = null;
    static Function<Class, SearchIndex> searchIndexFactory = null;
    static Function<Class, LookupIndex> uniqueLookupIndexFactory = null;
    static Function<Class, SearchIndex> uniqueSearchIndexFactory = null;
    static Function<Class, LookupIndex> lookupIndexFactory = null;
    static java.util.function.Supplier<RepoComposer> repoFactory = null;
    static java.util.function.Supplier<Filter> filterFactory = null;
    static java.util.function.Supplier<SearchableCollectionComposer> searchableCollectionFactory = null;
    static java.util.function.Supplier<ObjectEditorComposer> objectEditorFactory;

    public static java.util.function.Supplier<MapCreator> getMapCreatorFactory() {
        return mapCreatorFactory;
    }

    public static java.util.function.Supplier<SearchableCollectionComposer> getSearchableCollectionFactory() {
        return searchableCollectionFactory;
    }

    public static java.util.function.Supplier<RepoBuilder> getRepoBuilderFactory() {
        return repoBuilderFactory;
    }

    public static Function<Class, SearchIndex> getSearchIndexFactory() {
        return searchIndexFactory;
    }

    public static Function<Class, SearchIndex> getUniqueSearchIndexFactory() {
        return uniqueSearchIndexFactory;
    }

    public static Function<Class, LookupIndex> getLookupIndexFactory() {
        return lookupIndexFactory;
    }

    public static Function<Class, LookupIndex> getUniqueLookupIndexFactory() {
        return uniqueLookupIndexFactory;
    }

    public static java.util.function.Supplier<RepoComposer> getRepoFactory() {
        return repoFactory;
    }

    public static java.util.function.Supplier<Filter> getFilterFactory() {
        return filterFactory;
    }

    public static void init() {

        if ( mapCreatorFactory == null ) {
            mapCreatorFactory = () -> new MapCreatorImpl();
        }
        if ( repoBuilderFactory == null ) {
            repoBuilderFactory = () -> new RepoBuilderDefault();
        }
        if ( searchIndexFactory == null ) {
            searchIndexFactory = keyType -> {
                if (keyType == Typ.string) {
                    return new SearchIndexDefault(keyType);
                } else {
                    return new SearchIndexDefault(keyType);
                }
            };
        }
        if ( lookupIndexFactory == null ) {
            lookupIndexFactory = keyType -> new LookupIndexDefault(keyType);
        }
        if ( uniqueLookupIndexFactory == null ) {
            uniqueLookupIndexFactory = keyType -> new UniqueLookupIndex(keyType);
        }
        if ( uniqueSearchIndexFactory == null ) {
            uniqueSearchIndexFactory = keyType -> new UniqueSearchIndex(keyType);
        }

        if ( repoFactory == null ) {
            repoFactory = () -> new RepoDefault<>();
        }

        if ( filterFactory == null ) {
            filterFactory = () -> new FilterDefault();
        }

        if ( searchableCollectionFactory == null ) {
            searchableCollectionFactory = () -> new SearchableCollectionDefault();
        }

        if ( objectEditorFactory == null ) {
            objectEditorFactory = () -> new ObjectEditorDefault();
        }

    }

    static {
        init();
    }


    public static void setMapCreatorFactory( java.util.function.Supplier<MapCreator> mapCreatorFactory ) {
        SPIFactory.mapCreatorFactory = mapCreatorFactory;
    }

    public static void setRepoBuilderFactory( java.util.function.Supplier<RepoBuilder> repoBuilderFactory ) {
        SPIFactory.repoBuilderFactory = repoBuilderFactory;
    }

    public static void setSearchIndexFactory( Function<Class, SearchIndex> searchIndexFactory ) {
        SPIFactory.searchIndexFactory = searchIndexFactory;
    }

    public static void setLookupIndexFactory( Function<Class, LookupIndex> lookupIndexFactory ) {
        SPIFactory.lookupIndexFactory = lookupIndexFactory;
    }


    public static void setUniqueLookupIndexFactory( Function<Class, LookupIndex> lookupIndexFactory ) {
        SPIFactory.uniqueLookupIndexFactory = lookupIndexFactory;
    }

    public static void setUniqueSearchIndexFactory( Function<Class, SearchIndex> factory ) {
        SPIFactory.uniqueSearchIndexFactory = factory;
    }

    public static void setRepoFactory( java.util.function.Supplier<RepoComposer> repoFactory ) {
        SPIFactory.repoFactory = repoFactory;
    }

    public static void setFilterFactory( java.util.function.Supplier<Filter> filterFactory ) {
        SPIFactory.filterFactory = filterFactory;
    }

    public static java.util.function.Supplier<ObjectEditorComposer> getObjectEditorFactory() {
        return objectEditorFactory;
    }
}
