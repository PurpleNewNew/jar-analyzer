/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.rules;

import java.util.ArrayList;
import java.util.List;

public final class MethodSemanticFlags {
    public static final int ENTRY = 1;
    public static final int WEB_ENTRY = 1 << 1;
    public static final int RPC_ENTRY = 1 << 2;
    public static final int SPRING_ENDPOINT = 1 << 3;
    public static final int JSP_ENDPOINT = 1 << 4;
    public static final int STRUTS_ACTION = 1 << 5;
    public static final int SERVLET_CALLBACK = 1 << 6;
    public static final int NETTY_HANDLER = 1 << 7;
    public static final int MYBATIS_DYNAMIC_SQL = 1 << 8;
    public static final int SERIALIZABLE_OWNER = 1 << 9;
    public static final int DESERIALIZATION_CALLBACK = 1 << 10;
    public static final int INVOCATION_HANDLER = 1 << 11;
    public static final int COMPARATOR_CALLBACK = 1 << 12;
    public static final int TRANSFORMER_CALLBACK = 1 << 13;
    public static final int COLLECTION_CONTAINER = 1 << 14;
    public static final int COMPARABLE_CALLBACK = 1 << 15;
    public static final int TOSTRING_TRIGGER = 1 << 16;
    public static final int HASHCODE_TRIGGER = 1 << 17;
    public static final int EQUALS_TRIGGER = 1 << 18;

    private MethodSemanticFlags() {
    }

    public static List<String> describe(int flags) {
        if (flags <= 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        addIfSet(out, flags, ENTRY, "Entry");
        addIfSet(out, flags, WEB_ENTRY, "WebEntry");
        addIfSet(out, flags, RPC_ENTRY, "RpcEntry");
        addIfSet(out, flags, SPRING_ENDPOINT, "SpringEndpoint");
        addIfSet(out, flags, JSP_ENDPOINT, "JspEndpoint");
        addIfSet(out, flags, STRUTS_ACTION, "StrutsAction");
        addIfSet(out, flags, SERVLET_CALLBACK, "ServletCallback");
        addIfSet(out, flags, NETTY_HANDLER, "NettyHandler");
        addIfSet(out, flags, MYBATIS_DYNAMIC_SQL, "MyBatisDynamicSql");
        addIfSet(out, flags, SERIALIZABLE_OWNER, "SerializableOwner");
        addIfSet(out, flags, DESERIALIZATION_CALLBACK, "DeserializationCallback");
        addIfSet(out, flags, INVOCATION_HANDLER, "InvocationHandler");
        addIfSet(out, flags, COMPARATOR_CALLBACK, "ComparatorCallback");
        addIfSet(out, flags, TRANSFORMER_CALLBACK, "TransformerCallback");
        addIfSet(out, flags, COLLECTION_CONTAINER, "CollectionContainer");
        addIfSet(out, flags, COMPARABLE_CALLBACK, "ComparableCallback");
        addIfSet(out, flags, TOSTRING_TRIGGER, "ToStringTrigger");
        addIfSet(out, flags, HASHCODE_TRIGGER, "HashCodeTrigger");
        addIfSet(out, flags, EQUALS_TRIGGER, "EqualsTrigger");
        return List.copyOf(out);
    }

    private static void addIfSet(List<String> out, int flags, int mask, String label) {
        if ((flags & mask) != 0) {
            out.add(label);
        }
    }
}
