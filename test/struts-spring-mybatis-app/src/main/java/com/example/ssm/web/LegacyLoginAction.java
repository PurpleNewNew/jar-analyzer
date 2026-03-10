package com.example.ssm.web;

import com.example.ssm.service.UserServiceImpl;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LegacyLoginAction extends Action {
    private final UserServiceImpl userService = new UserServiceImpl();

    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        userService.search(request.getParameter("keyword"));
        return new ActionForward("/META-INF/resources/dashboard.jsp");
    }
}
