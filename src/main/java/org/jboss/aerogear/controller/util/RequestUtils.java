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

package org.jboss.aerogear.controller.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jboss.aerogear.controller.log.AeroGearLogger;
import org.jboss.aerogear.controller.router.RequestMethod;
import org.jboss.aerogear.controller.router.Route;

import br.com.caelum.iogi.Iogi;
import br.com.caelum.iogi.parameters.Parameter;
import br.com.caelum.iogi.reflection.Target;
import br.com.caelum.iogi.util.DefaultLocaleProvider;
import br.com.caelum.iogi.util.NullDependencyProvider;

import com.google.common.base.Splitter;

/**
 * Utility methods for various {@link HttpServletRequest} operation.
 */
public class RequestUtils {
    
    private static final Iogi IOGI = new Iogi(new NullDependencyProvider(), new DefaultLocaleProvider());
    
    private RequestUtils() {
    }
    
    /**
     * Returns the path of the current request with out the context path.
     * </p>
     * For example, if the web application was deployed with a context path of '/myapp',
     * and the request submitted was '/myapp/cars/12', this method would return '/cars/12'.
     * 
     * @param request the {@link HttpServletRequest}.
     * @return {@code String} the request path without the context path (suffix)
     */
    public static String extractPath(final HttpServletRequest request) {
        final String contextPath = request.getServletContext().getContextPath();
        return request.getRequestURI().substring(contextPath.length());
    }
    
    /**
     * Returns the {@link RequestMethod} for the passed-in {@link HttpServletRequest}.
     * 
     * @param httpServletRequest the {@link HttpServletRequest}
     * @return {@link RequestMethod} matching the Http Method of the request.
     */
    public static RequestMethod extractMethod(final HttpServletRequest httpServletRequest) {
        return RequestMethod.valueOf(httpServletRequest.getMethod());
    }
    
    /**
     * Returns the {@code Accept header} from the passed-in {@code HttpServletRequest}.
     * 
     * @param request the {@link HttpServletRequest}
     * @return {@code Set<String>} of the values of the Http Accept Header, or an empty list if
     * there was not Accept header
     */
    public static Set<String> extractAcceptHeader(final HttpServletRequest request) {
        final String acceptHeader = request.getHeader("Accept");
        if (acceptHeader == null) {
            return Collections.emptySet();
        }
        
        final Set<String> acceptHeaders = new LinkedHashSet<String>();
        for (String header : Splitter.on(',').trimResults().split(acceptHeader)) {
            acceptHeaders.add(header);
        }
        return acceptHeaders;
    }
    
    public static Object[] extractPathParameters(String requestPath, Route route) {
        // TODO: extract this from Resteasy
        final int paramOffset = route.getPath().indexOf('{');
        final CharSequence param = requestPath.subSequence(paramOffset, requestPath.length());
        return new Object[]{param.toString()};
    }

    public static Object[] extractParameters(HttpServletRequest request, Route route) {
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
            Object instantiate = IOGI.instantiate(target, parameters.toArray(new Parameter[parameters.size()]));
            return new Object[]{instantiate};
        }

        return new Object[0]; 
    }

}
