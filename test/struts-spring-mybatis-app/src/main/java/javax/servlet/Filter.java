package javax.servlet;

public interface Filter {
    default void init(FilterConfig filterConfig) {
    }

    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain);
}
