package fixture.framework.web;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class XmlServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String cmd = req == null ? "" : req.getParameter("cmd");
        if (cmd != null && !cmd.isEmpty()) {
            resp.write(cmd);
        }
    }
}
