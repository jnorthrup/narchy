// Copyright 2015-2016 Stanford University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package jcog.grammar.synthesize.util;

import java.util.function.Function;
import java.util.function.Predicate;

public class OracleUtils {

    public interface Oracle extends Function<String,String> {

    }

    public interface DiscriminativeOracle extends Predicate<String> {
    }

    public interface Wrapper extends Function<String,String> {

    }

    public static class IdentityWrapper implements Wrapper {
        @Override
        public String apply(String input) {
            return input;
        }
    }

    public static class WrappedOracle implements Oracle {
        private final Oracle oracle;
        private final Wrapper wrapper;

        public WrappedOracle(Oracle oracle, Wrapper wrapper) {
            this.oracle = oracle;
            this.wrapper = wrapper;
        }

        @Override
        public String apply(String query) {
            return this.oracle.apply(this.wrapper.apply(query));
        }
    }

    public static class WrappedDiscriminativeOracle implements DiscriminativeOracle {
        private final Predicate<String> oracle;
        private final Wrapper wrapper;

        public WrappedDiscriminativeOracle(Predicate<String> oracle, Wrapper wrapper) {
            this.oracle = oracle;
            this.wrapper = wrapper;
        }

        @Override
        public boolean test(String query) {
            return this.oracle.test(this.wrapper.apply(query));
        }
    }
}
