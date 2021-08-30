package edu.mit.att.test;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Filter to set http servlet variable when in development.
 *
 * The filter is used when Touchstone is not in effect (e.g., for local development)
 *
 * This filter is not needed in production. In fact that's when TouchStone filter should be used
 */

@Component
@Order(1)
//@WebFilter(urlPatterns = {"/", "/Logout", "/*", "/**", "/Admin"})
@ConditionalOnProperty(
        value="spring.profiles.active",
        havingValue = "dev",
        matchIfMissing = false)
@WebFilter
public class EnvironmentHttpFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentHttpFilter.class);

    //  this annotation is required to inject the environment. but it should not be injected
    // in touchstone class. adding annotation there makes the class loading fail. hence this class.
    //@Resource
    private Environment env;

    @Value("${testing.mail:test}")
    private String email;


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.debug("Init test filter");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        //logger.debug("Request to page: {}",  ((HttpServletRequest) servletRequest).getRequestURL());


        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        // Information from TouchStone:
        // other Attributes are displayName, mail, nickname
        //logger.info("Touchstone passed attribute {}:{}", "mail", httpServletRequest.getAttribute("mail"));

        if (env == null) {
            logger.info("Spring injected environment annotation null.");
        } else {
            //logger.info("Injected injected annotation: {}",  env.getProperty("test.status"));
        }


       // if (env != null && env.getRequiredProperty("testing.status").equals("true")) {
            logger.info("In test mode. Setting mail value to: {}", email);
            httpServletRequest.setAttribute("mail", email);
        //}

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}