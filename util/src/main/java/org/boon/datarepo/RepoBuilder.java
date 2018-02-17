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
package org.boon.datarepo;

import org.boon.core.Function;
import org.boon.core.Supplier;
import org.boon.datarepo.modification.ModificationListener;
import org.boon.datarepo.spi.RepoComposer;
import org.boon.datarepo.spi.SearchIndex;

import java.util.Comparator;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Provides a builder for Repos.
 */
public interface RepoBuilder {


    RepoBuilder searchIndexFactory(Function<Class, SearchIndex> factory);

    RepoBuilder lookupIndexFactory(Function<Class, LookupIndex> factory);

    RepoBuilder uniqueLookupIndexFactory(Function<Class, LookupIndex> factory);

    RepoBuilder uniqueSearchIndexFactory(Function<Class, SearchIndex> factory);

    RepoBuilder repoFactory(Supplier<RepoComposer> factory);

    RepoBuilder primaryKey(String propertyName);

    RepoBuilder lookupIndex(String propertyName);

    RepoBuilder uniqueLookupIndex(String propertyName);

    RepoBuilder searchIndex(String propertyName);

    RepoBuilder uniqueSearchIndex(String propertyName);

    RepoBuilder collateIndex(String propertyName, Comparator collator);

    RepoBuilder collateIndex(String propertyName);

    RepoBuilder collateIndex(String propertyName, Locale locale);

    RepoBuilder keyGetter(String propertyName, Function<?, ?> key);

    RepoBuilder filterFactory(Supplier<Filter> factory);


    RepoBuilder usePropertyForAccess(boolean useProperty);

    RepoBuilder useFieldForAccess(boolean useField);

    RepoBuilder useUnsafe(boolean useUnSafe);

    RepoBuilder nullChecks(boolean nullChecks);

    RepoBuilder addLogging(boolean logging);

    RepoBuilder cloneEdits(boolean cloneEdits);

    RepoBuilder useCache();

    RepoBuilder storeKeyInIndexOnly();

    RepoBuilder events( ModificationListener... listeners );

    RepoBuilder debug();


    <KEY, ITEM> Repo<KEY, ITEM> build( Class<KEY> key, Class<ITEM> clazz, Class<?>... all );


    RepoBuilder level( Level info );

    RepoBuilder upperCaseIndex( String property );

    RepoBuilder lowerCaseIndex( String property );

    RepoBuilder camelCaseIndex( String property );

    RepoBuilder underBarCaseIndex( String property );

    @Deprecated
    RepoBuilder nestedIndex( String... propertyPath );

    RepoBuilder indexHierarchy();

    RepoBuilder indexBucketSize( String propertyName, int size );

    RepoBuilder hashCodeOptimizationOn();


    RepoBuilder removeDuplication( boolean removeDuplication );
}
