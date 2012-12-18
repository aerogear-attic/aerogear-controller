/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.aerogear.controller.router;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.jboss.aerogear.controller.log.AeroGearLogger;
import org.jboss.aerogear.controller.util.RequestUtils;
import org.jboss.aerogear.controller.util.StringUtils;

import br.com.caelum.iogi.Iogi;
import br.com.caelum.iogi.parameters.Parameter;
import br.com.caelum.iogi.reflection.Target;
import br.com.caelum.iogi.util.DefaultLocaleProvider;
import br.com.caelum.iogi.util.NullDependencyProvider;

import com.google.common.collect.Sets;

/**
 * Default implementation of {@link RouteProcessor}.
 * </p>
 * This implementation uses Context and Dependency Injection (CDI) to have various parts injected into it. Of<br>
 * particular interest for end users is the {@link RoutingModule} which is described in more detail in the section below.
 * 
 * <h3> RoutingModule </h3>
 * The CDI implementation will scan for an instance of {@link RoutingModule} upon deployment, and its<br> 
 * {@link RoutingModule#build()} method will be called to assemble the routes configured for this application.<br>
 * To simplify this process {@link AbstractRoutingModule} is provided, please refer its javadoc for sample usage.
 */
public class DefaultRouteProcessor implements RouteProcessor {
    
    private BeanManager beanManager;
    private final Iogi iogi = new Iogi(new NullDependencyProvider(), new DefaultLocaleProvider());
    private ControllerFactory controllerFactory;
    private Set<Responder> responders = new HashSet<Responder>();
    
    public DefaultRouteProcessor() {
    }
    
    @Inject
    public DefaultRouteProcessor(BeanManager beanManager, Instance<Responder> responders, ControllerFactory controllerFactory) {
        this.beanManager = beanManager;
        this.controllerFactory = controllerFactory;
        for (Responder responder : responders) {
            this.responders.add(responder);
        }
    }

    @Override
    public void process(RouteContext routeContext) throws Exception {
        final HttpServletRequest request = routeContext.getRequest();
        final String requestPath = routeContext.getRequestPath();
        final Route route = routeContext.getRoute();
        Object[] params;

        if (route.isParameterized()) {
            params = extractPathParameters(requestPath, route);
        } else {
            params = extractParameters(request, route);
        }
        Object result = route.getTargetMethod().invoke(getController(route), params);
        
        for (String mediaType : Sets.intersection(route.produces(), RequestUtils.extractAcceptHeader(request))) {
            for (Responder responder : responders) {
                if (responder.accepts(mediaType)) {
                    responder.respond(result, routeContext);
                    return;
                }
            }
        }
    }
    
    private Object[] extractPathParameters(String requestPath, Route route) {
        // TODO: extract this from Resteasy
        final int paramOffset = route.getPath().indexOf('{');
        final CharSequence param = requestPath.subSequence(paramOffset, requestPath.length());
        return new Object[]{param.toString()};
    }

    private Object[] extractParameters(HttpServletRequest request, Route route) {
        LinkedList<Parameter> parameters = new LinkedList<Parameter>();
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] value = entry.getValue();
            if (value.length == 1) {
                parameters.add(new Parameter(entry.getKey(), value[0]));
            } else {
                AeroGearLogger.LOGGER.multivaluedParamsUnsupported();
            }
        }
        Class<?>[] parameterTypes = route.getTargetMethod().getParameterTypes();
        if (parameterTypes.length == 1) {
            Class<?> parameterType = parameterTypes[0];
            Target<?> target = Target.create(parameterType, StringUtils.downCaseFirst(parameterType.getSimpleName()));
            Object instantiate = iogi.instantiate(target, parameters.toArray(new Parameter[parameters.size()]));
            return new Object[]{instantiate};
        }

        return new Object[0];  
    }

    private Object getController(Route route) {
        return controllerFactory.createController(route.getTargetClass(), beanManager);
    }

}