/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors
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

package org.jboss.aerogear.controller.router.parameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;

import org.jboss.aerogear.controller.log.AeroGearLogger;
import org.jboss.aerogear.controller.log.LoggerMessages;
import org.jboss.aerogear.controller.router.Consumer;
import org.jboss.aerogear.controller.router.RouteContext;
import org.jboss.aerogear.controller.util.StringUtils;

import br.com.caelum.iogi.Iogi;
import br.com.caelum.iogi.reflection.Target;
import br.com.caelum.iogi.util.DefaultLocaleProvider;
import br.com.caelum.iogi.util.NullDependencyProvider;

import com.google.common.base.Optional;

public class Parameters {
    
    private static final Iogi IOGI = new Iogi(new NullDependencyProvider(), new DefaultLocaleProvider());
    
    private Parameters() {
    }
    
    public static <T> Parameter<T> param(final Class<T> type) {
        return new Parameter<T>(Parameter.Type.ENTITY, type);
    }
    
    public static <T> Parameter<T> param(final String name, final Class<T> type) {
        return new RequestParameter<T>(name, Parameter.Type.REQUEST, type);
    }
    
    public static <T> Parameter<T> param(final String name, final T defaultValue, final Class<T> type) {
        return new RequestParameter<T>(name, Parameter.Type.REQUEST, defaultValue, type);
    }
    
    /**
     * Extracts the arguments from the current request for the target route.
     * 
     * @param routeContext the {@link RouteContext}.
     * @return {@code Object[]} an array of Object matching the route targets parameters.
     */
    public static Object[] extractArguments(final RouteContext routeContext, final Map<String, Consumer> consumers) {
        final List<Object> args = new LinkedList<Object>();
        for (Parameter<?> parameter : routeContext.getRoute().getParameters()) {
            switch (parameter.getParameterType()) {
            case ENTITY:
                if (!addIfPresent(extractIogiParam(routeContext), args)) {
                    args.add(extractBody(routeContext, parameter, consumers));
                }
                break;
            case REQUEST:
                final RequestParameter<?> requestParameter = (RequestParameter<?>) parameter;
                if (addIfPresent(extractParam(routeContext, requestParameter), args)) {
                    break;
                }
                if (addIfPresent(extractHeaderParam(routeContext, requestParameter), args)) {
                    break;
                }
                if (addIfPresent(extractCookieParam(routeContext, requestParameter), args)) {
                    break;
                }
                if (addIfPresent(requestParameter.getDefaultValue(), args)) {
                    break;
                }
                if (addIfPresent(extractPathParam(routeContext), args)) {
                    break;
                } 
                throw LoggerMessages.MESSAGES.missingParameterInRequest(requestParameter, routeContext.getRoute());
            }
        }
        return args.toArray();
    }
    
    private static Object extractBody(final RouteContext routeContext, final Parameter<?> parameter, final Map<String, Consumer> consumers) {
        final Set<String> mediaTypes = routeContext.getRoute().consumes();
        for (String mediaType : mediaTypes) {
            final Consumer consumer = consumers.get(mediaType);
            if (consumer != null) {
                return consumer.unmarshall(routeContext.getRequest(), parameter.getType());
            }
        }
        throw LoggerMessages.MESSAGES.noConsumerForMediaType(parameter, consumers.values(), mediaTypes);
    }
            

    /**
     * Extracts a path parameter from the passed in request path.
     * 
     * @param routeContext  the {@link RouteContext} to extract a path parameter from.
     * @return {@code Optional<String>}  containing the extracted path param if present in the request path.
     */
    public static Optional<String> extractPathParam(final RouteContext routeContext) {
        final String requestPath = routeContext.getRequestPath();
        final int paramOffset = routeContext.getRoute().getPath().indexOf('{');
        if (paramOffset != -1 && paramOffset < requestPath.length()) {
            return Optional.of(requestPath.subSequence(paramOffset, requestPath.length()).toString());
        }
        return Optional.absent();
    }

    /**
     * Returns an instance of the type used in the parameter names using Iogi.
     * </p>
     * For example, having form parameters named 'car.color', 'car.brand', this method
     * would try to use those values to instantiate a new Car instance.
     * 
     * @return {@link Optional}  may contain the instantiated instance, else isPresent will return false.
     */
    public static Optional<?> extractIogiParam(final RouteContext routeContext) {
        final LinkedList<br.com.caelum.iogi.parameters.Parameter> parameters = new LinkedList<br.com.caelum.iogi.parameters.Parameter>();
        for (Map.Entry<String, String[]> entry : routeContext.getRequest().getParameterMap().entrySet()) {
            final String[] value = entry.getValue();
            if (value.length == 1) {
                parameters.add(new br.com.caelum.iogi.parameters.Parameter(entry.getKey(), value[0]));
            } else {
                AeroGearLogger.LOGGER.multivaluedParamsUnsupported();
            }
        }
        if (!parameters.isEmpty()) {
            final Class<?>[] parameterTypes = routeContext.getRoute().getTargetMethod().getParameterTypes();
            final Class<?> parameterType = parameterTypes[0];
            final Target<?> target = Target.create(parameterType, StringUtils.downCaseFirst(parameterType.getSimpleName()));
            return Optional.fromNullable(IOGI.instantiate(target, parameters.toArray(new br.com.caelum.iogi.parameters.Parameter[parameters.size()])));
        }
        return Optional.absent();
    }
    
    private static boolean addIfPresent(final Optional<?> op, final List<Object> args) {
        if (op.isPresent()) {
            args.add(op.get());
            return true;
        }
        return false;
    }
    
    private static Optional<?> extractHeaderParam(final RouteContext routeContext, final RequestParameter<?> parameter) {
        return Optional.fromNullable(routeContext.getRequest().getHeader(parameter.getName()));
    }
    
    private static Optional<?> extractCookieParam(final RouteContext routeContext, final RequestParameter<?> parameter) {
        final Cookie[] cookies = routeContext.getRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(parameter.getName())) {
                    return Optional.fromNullable(cookie.getValue());
                }
            }
        }
        return Optional.absent();
    }
    
    private static Optional<?> extractParam(final RouteContext routeContext, final RequestParameter<?> parameter) {
        final String[] values = routeContext.getRequest().getParameterMap().get(parameter.getName());
        if (values != null) {
            if (values.length == 1) {
                return Optional.of(values[0]);
            } else {
                throw LoggerMessages.MESSAGES.multivaluedParamsUnsupported(parameter.getName());
            }
        }
        return Optional.absent();
    }

}
