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
package org.neo4j.cypher.internal.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import scala.None;
import scala.Product;
import scala.Some;
import scala.util.Left;
import scala.util.Right;

public class RewritableJavascript {
    public static boolean inAllowList(Class<?> cls) {
        return ASTNode.class.isAssignableFrom(cls)
                || Some.class.isAssignableFrom(cls)
                || None.class.isAssignableFrom(cls)
                || Left.class.isAssignableFrom(cls)
                || Right.class.isAssignableFrom(cls);
    }

    private static Method getCopyConstructor(Class<?> cls) {
        return Arrays.stream(cls.getMethods())
                .filter(m -> m.getName().equals("copy"))
                .findFirst()
                .orElse(null);
    }

    public static Object copyProduct(Product product, Object[] children) {
        return copyConstructor(product.getClass(), product, children);
    }

    public int numParameters(Product product) {
        return numParameters(product.getClass());
    }

    public boolean includesPosition(Product product) {
        return lastParamIsPosition(product.getClass());
    }

    public static Object copyConstructor(Class<?> cls, Object object, Object[] children) {
        if (!inAllowList(cls)) {
            throw new UnsupportedOperationException("Unsupported rewrite class: " + cls.getName());
        }
        Method method = getCopyConstructor(cls);
        if (method == null) {
            throw new IllegalStateException("No copy method found for " + cls.getName());
        }
        try {
            return method.invoke(object, children);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke copy method for " + cls.getName(), e);
        }
    }

    public static int numParameters(Class<?> cls) {
        if (!inAllowList(cls)) {
            throw new UnsupportedOperationException("Unsupported rewrite class: " + cls.getName());
        }
        Method method = getCopyConstructor(cls);
        if (method == null) {
            throw new IllegalStateException("No copy method found for " + cls.getName());
        }
        return method.getParameterTypes().length;
    }

    public static boolean lastParamIsPosition(Class<?> cls) {
        if (!inAllowList(cls)) {
            throw new UnsupportedOperationException("Unsupported rewrite class: " + cls.getName());
        }
        Method method = getCopyConstructor(cls);
        if (method == null) {
            throw new IllegalStateException("No copy method found for " + cls.getName());
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            return false;
        }
        Class<?> lastParam = paramTypes[paramTypes.length - 1];
        return lastParam.isAssignableFrom(InputPosition.class);
    }
}
