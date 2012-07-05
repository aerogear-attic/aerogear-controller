package org.jboss.aerogear.controller.filter;

import org.jboss.aerogear.controller.log.AeroGearLogger;
import org.jboss.aerogear.controller.router.Router;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "aerogear-controller", urlPatterns = "/*")
public class AeroGear implements Filter {

    @Inject
    private Router router;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!isHttpServletContainer(request, response)) {
            throw AeroGearLogger.LOGGER.mustRunInsideAContainer();
        }
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        if (router.hasRouteFor(httpServletRequest)) {
            router.dispatch(httpServletRequest, httpServletResponse, chain);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isHttpServletContainer(ServletRequest request, ServletResponse response) {
        return request instanceof HttpServletRequest && response instanceof HttpServletResponse;
    }

    @Override
    public void destroy() {

    }
}
