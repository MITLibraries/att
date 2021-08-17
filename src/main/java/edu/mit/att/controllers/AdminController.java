package edu.mit.att.controllers;

import edu.mit.att.authz.Role;
import edu.mit.att.authz.Subject;
import edu.mit.att.service.UserService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.slf4j.LoggerFactory.getLogger;


@Controller
public class AdminController {

    private final Logger logger = getLogger(this.getClass());


    // TODO: for testing
    @Resource
    private Environment env;

    @Autowired
    private UserService userService;

    @Autowired
    private Subject subject;

    /**
     * Handles all requests
     *
     * @return model and view
     */


    @RequestMapping(value = {"admin", "Admin"}, method = RequestMethod.GET)
    public ModelAndView showItemsPage(HttpSession httpSession, HttpServletRequest httpServletRequest) {

        logger.info("Test mode:{}", env.getRequiredProperty("testing.status"));

        // Get session information:

        String principal = (String) httpServletRequest.getHeader("mail");

        // logger.info("Mail attribute:{}", principal);

        if (env.getRequiredProperty("testing.status").equals("true")) {
            principal = env.getRequiredProperty("testing.mail");
        }


        Role role = null;
        role = subject.getRole(principal);

        if ((!role.equals(Role.siteadmin)) && !role.equals(Role.deptadmin)) { //TODO restrict further dept admin
            logger.debug("User does not have permissions to access the admin page");
            return new ModelAndView("Permissions");
        }


        return new ModelAndView("Admin");
    }


}
