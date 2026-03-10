package fixture.framework.jsp;

public final class JspSink {
    private JspSink() {
    }

    public static String render(String value) {
        return value == null ? "" : value;
    }
}
