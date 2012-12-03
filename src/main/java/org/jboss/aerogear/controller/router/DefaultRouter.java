package org.jboss.aerogear.controller.router;

import static org.jboss.aerogear.controller.util.RequestUtils.extractMethod;
import static org.jboss.aerogear.controller.util.RequestUtils.extractPath;
import static org.jboss.aerogear.controller.util.RequestUtils.extractAcceptHeader;

import java.util.Collections;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.aerogear.controller.util.RequestUtils;

/**
 * Default implementation of {@link Router}.
 * </p>
 * This implementation uses Context and Dependency Injection (CDI) to have various parts injected into it. Of<br>
 * particular interest for end users is the {@link RoutingModule} which is described in more detail in the section below.
 * 
 * <h3> RoutingModule </h3>
 * The CDI implementation will scan for an instance of {@link RoutingModule} upon deployment, and its<br> 
 * {@link RoutingModule#build()} method will be called to assemble the routes configured for this application.<br>
 * To simplify this process {@link AbstractRoutingModule} is provided, please refer its javadoc for sample usage.
 */
public class DefaultRouter implements Router {
    
    private Routes routes;
    private RouteProcessor routeProcessor;
    
    public DefaultRouter() {
    }
    
    @Inject
    public DefaultRouter(Instance<RoutingModule> instance, RouteProcessor routeProcessor) {
        this.routes = instance.isUnsatisfied() ? Routes.from(Collections.<RouteBuilder>emptyList()) : instance.get().build();
        this.routeProcessor = routeProcessor;
    }

    @Override
    public boolean hasRouteFor(HttpServletRequest request) {
        return routes.hasRouteFor(extractMethod(request), extractPath(request), extractAcceptHeader(request));
    }

    @Override
    public void dispatch(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException {
        try {
            String requestPath = RequestUtils.extractPath(request);
            Route route = routes.routeFor(extractMethod(request), requestPath, extractAcceptHeader(request));
            routeProcessor.process(new RouteContext(route, requestPath, request, response, routes));
        } catch (Exception e) {
            throw new ServletException(e.getMessage(), e);
        }
    }
    
}
