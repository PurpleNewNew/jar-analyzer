/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.parser.common.ast.factory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.gqlstatus.ErrorGqlStatusObject;

public interface ASTExceptionFactory {
    Exception syntaxException(
            ErrorGqlStatusObject gqlCause,
            String got,
            List<String> expected,
            Exception source,
            int offset,
            int line,
            int column);

    Exception syntaxException(String got, List<String> expected, Exception source, int offset, int line, int column);

    Exception syntaxException(Exception source, int offset, int line, int column);

    Exception syntaxException(ErrorGqlStatusObject gqlStatusObject, Exception source, int offset, int line, int column);

    static String invalidHintIndexType(HintIndexType got) {
        final String HINT_TYPES = Arrays.stream(HintIndexType.values())
                .filter(hintIndexType -> !(hintIndexType == HintIndexType.BTREE || hintIndexType == HintIndexType.ANY))
                .map(Enum::name)
                .collect(Collectors.collectingAndThen(Collectors.toList(), joiningLastDelimiter(", ", " or ")));
        if (got == HintIndexType.BTREE) {
            return String.format(
                    "Index type %s is no longer supported for USING index hint. Use %s instead.",
                    got.name(), HINT_TYPES);
        } else {
            return String.format(
                    "Index type %s is not defined for USING index hint. Use %s instead.", got.name(), HINT_TYPES);
        }
    }

    // ---------Helper functions
    private static Function<List<String>, String> joiningLastDelimiter(String delimiter, String lastDelimiter) {
        return list -> {
            int last = list.size() - 1;
            return String.join(lastDelimiter, String.join(delimiter, list.subList(0, last)), list.get(last));
        };
    }
}
