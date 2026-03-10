package javax.servlet.http;

import javax.servlet.ServletResponse;

public interface HttpServletResponse extends ServletResponse {
    void write(String value);
}
