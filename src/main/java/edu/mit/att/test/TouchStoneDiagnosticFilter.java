package edu.mit.att.test;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * Could also be used for displaying variables set up by Touchstone
 *
 * The filter is used when Touchstone is not in effect (e.g., for local development)
 */

@Component
@Order(1)
//@WebFilter(urlPatterns = {"/", "/Logout", "/*", "/**", "/Admin"})
@WebFilter
public class TouchStoneDiagnosticFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(TouchStoneDiagnosticFilter.class);

    //  NOTE - *** COMMENT OUT THIS ANNOTATION WHEN DEPLOYING TO PRODUCTION ***
    // TODO: Remove reliance on this annotation, as not removing this will make the application not boot in prod.
    @Resource
    private Environment env;

    @Value("${testing.mail:test}")
    private String email;


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.debug("Init filter");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        logger.debug("Request to page: {}",  ((HttpServletRequest) servletRequest).getRequestURL());


        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        // Information from TouchStone:
        // other Attributes are displayName, mail, nickname
        logger.info("Touchstone passed attribute {}:{}", "mail", httpServletRequest.getAttribute("mail"));

        if (env == null) {
            logger.info("Spring injected environment annotation null.");
        } else {
            logger.info("Injected injected annotation: {}",  env.getProperty("test.status"));
        }


        if (env != null && env.getRequiredProperty("testing.status").equals("true")) {
            logger.info("In test mode. Setting mail value to: {}", email);
            httpServletRequest.setAttribute("mail", email);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}