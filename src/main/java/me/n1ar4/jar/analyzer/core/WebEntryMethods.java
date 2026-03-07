/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import java.util.Set;

public final class WebEntryMethods {
    public static final Set<WebEntryMethodSpec> SERVLET_ENTRY_METHODS = Set.of(
            entry("service", "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V"),
            entry("service", "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;)V"),
            entry("doGet", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
            entry("doGet", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V"),
            entry("doPost", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
            entry("doPost", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V"),
            entry("doPut", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
            entry("doPut", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V"),
            entry("doDelete", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
            entry("doDelete", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V"),
            entry("doHead", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
            entry("doHead", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V"),
            entry("doOptions", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
            entry("doOptions", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V"),
            entry("doTrace", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
            entry("doTrace", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V")
    );
    public static final Set<WebEntryMethodSpec> FILTER_ENTRY_METHODS = Set.of(
            entry("doFilter", "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V"),
            entry("doFilter", "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;Ljakarta/servlet/FilterChain;)V")
    );
    public static final Set<WebEntryMethodSpec> INTERCEPTOR_ENTRY_METHODS = Set.of(
            entry("preHandle", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z"),
            entry("preHandle", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z"),
            entry("postHandle", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;Lorg/springframework/web/servlet/ModelAndView;)V"),
            entry("postHandle", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/Object;Lorg/springframework/web/servlet/ModelAndView;)V"),
            entry("afterCompletion", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;Ljava/lang/Exception;)V"),
            entry("afterCompletion", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/Object;Ljava/lang/Exception;)V"),
            entry("afterConcurrentHandlingStarted", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)V"),
            entry("afterConcurrentHandlingStarted", "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/Object;)V")
    );
    public static final Set<WebEntryMethodSpec> LISTENER_ENTRY_METHODS = Set.of(
            entry("contextInitialized", "(Ljavax/servlet/ServletContextEvent;)V"),
            entry("contextInitialized", "(Ljakarta/servlet/ServletContextEvent;)V"),
            entry("contextDestroyed", "(Ljavax/servlet/ServletContextEvent;)V"),
            entry("contextDestroyed", "(Ljakarta/servlet/ServletContextEvent;)V"),
            entry("requestInitialized", "(Ljavax/servlet/ServletRequestEvent;)V"),
            entry("requestInitialized", "(Ljakarta/servlet/ServletRequestEvent;)V"),
            entry("requestDestroyed", "(Ljavax/servlet/ServletRequestEvent;)V"),
            entry("requestDestroyed", "(Ljakarta/servlet/ServletRequestEvent;)V"),
            entry("sessionCreated", "(Ljavax/servlet/http/HttpSessionEvent;)V"),
            entry("sessionCreated", "(Ljakarta/servlet/http/HttpSessionEvent;)V"),
            entry("sessionDestroyed", "(Ljavax/servlet/http/HttpSessionEvent;)V"),
            entry("sessionDestroyed", "(Ljakarta/servlet/http/HttpSessionEvent;)V"),
            entry("attributeAdded", "(Ljavax/servlet/ServletContextAttributeEvent;)V"),
            entry("attributeAdded", "(Ljakarta/servlet/ServletContextAttributeEvent;)V"),
            entry("attributeRemoved", "(Ljavax/servlet/ServletContextAttributeEvent;)V"),
            entry("attributeRemoved", "(Ljakarta/servlet/ServletContextAttributeEvent;)V"),
            entry("attributeReplaced", "(Ljavax/servlet/ServletContextAttributeEvent;)V"),
            entry("attributeReplaced", "(Ljakarta/servlet/ServletContextAttributeEvent;)V"),
            entry("attributeAdded", "(Ljavax/servlet/ServletRequestAttributeEvent;)V"),
            entry("attributeAdded", "(Ljakarta/servlet/ServletRequestAttributeEvent;)V"),
            entry("attributeRemoved", "(Ljavax/servlet/ServletRequestAttributeEvent;)V"),
            entry("attributeRemoved", "(Ljakarta/servlet/ServletRequestAttributeEvent;)V"),
            entry("attributeReplaced", "(Ljavax/servlet/ServletRequestAttributeEvent;)V"),
            entry("attributeReplaced", "(Ljakarta/servlet/ServletRequestAttributeEvent;)V"),
            entry("attributeAdded", "(Ljavax/servlet/http/HttpSessionBindingEvent;)V"),
            entry("attributeAdded", "(Ljakarta/servlet/http/HttpSessionBindingEvent;)V"),
            entry("attributeRemoved", "(Ljavax/servlet/http/HttpSessionBindingEvent;)V"),
            entry("attributeRemoved", "(Ljakarta/servlet/http/HttpSessionBindingEvent;)V"),
            entry("attributeReplaced", "(Ljavax/servlet/http/HttpSessionBindingEvent;)V"),
            entry("attributeReplaced", "(Ljakarta/servlet/http/HttpSessionBindingEvent;)V")
    );

    private WebEntryMethods() {
    }

    private static WebEntryMethodSpec entry(String name, String desc) {
        return new WebEntryMethodSpec(name, desc);
    }
}
