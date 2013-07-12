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

package org.jboss.aerogear.controller.util;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.Cookie;

import org.jboss.aerogear.controller.log.AeroGearLogger;
import org.jboss.aerogear.controller.log.ExceptionBundle;
import org.jboss.aerogear.controller.router.Consumer;
import org.jboss.aerogear.controller.router.RouteContext;
import org.jboss.aerogear.controller.router.parameter.ConstantParameter;
import org.jboss.aerogear.controller.router.parameter.Parameter;
import org.jboss.aerogear.controller.router.parameter.ReplacementParameter;
import org.jboss.aerogear.controller.router.parameter.RequestParameter;
import org.jboss.aerogear.controller.router.rest.pagination.PaginationInfo;

import br.com.caelum.iogi.Iogi;
import br.com.caelum.iogi.reflection.Target;
import br.com.caelum.iogi.util.DefaultLocaleProvider;
import br.com.caelum.iogi.util.NullDependencyProvider;

import com.google.common.base.Optional;

public class ParameterExtractor {

    private static final Iogi IOGI = new Iogi(new NullDependencyProvider(), new DefaultLocaleProvider());

    /**
     * Extracts the arguments from the current request for the target route.
     * 
     * @param routeContext the {@link org.jboss.aerogear.controller.router.RouteContext}.
     * @param consumers the {@link Consumer}s that will be used to try to unmarshall the request body.
     * @return {@code Map<String, Object} containing parameter name -> value mapping.
     */
    public static Map<String, Object> extractArguments(final RouteContext routeContext, final Map<String, Consumer> consumers) throws Exception {
        final Map<String, Object> argsMap = new LinkedHashMap<String, Object>();
        final List<Parameter<?>> parameters = routeContext.getRoute().getParameters();
        final int size = parameters.size();
        for (int i = 0; i < size; i++) {
            final Parameter<?> parameter = parameters.get(i);
            switch (parameter.getParameterType()) {
                case ENTITY:
                    if (PaginationInfo.class.isAssignableFrom(parameter.getType())) {
                        break;
                    }
                    if (!addIfPresent(extractIogiParam(routeContext), "entityParam", argsMap)) {
                        argsMap.put("entityParam", extractBody(routeContext, parameter, getConsumer(routeContext, consumers, parameter)));
                    }
                    break;
                case REQUEST:
                    final RequestParameter<?> rp = (RequestParameter<?>) parameter;
                    extractRequestParam(rp.getName(), rp.getType(), rp.getDefaultValue(), argsMap, routeContext);
                    break;
                case CONSTANT:
                    final ConstantParameter<?> cp = (ConstantParameter<?>) parameter;
                    argsMap.put("constantParam-" + i, cp.getValue());
                    break;
                case REPLACEMENT:
                    final ReplacementParameter<?> replacementParam = (ReplacementParameter<?>) parameter;
                    final Map<String, Object> paramsMap = extractRequestParams(replacementParam, routeContext);
                    argsMap.put("replacementParam-" + i, RequestUtils.injectParamValues(replacementParam.getString(), paramsMap));
                    break;
            }
        }
        return argsMap;
    }
    
    private static Map<String, Object> extractRequestParam(
            final String paramName, 
            final Class<?> type, 
            final Optional<?> defaultValue,
            final Map<String, Object> map,
            final RouteContext routeContext) throws Exception {
        if (addIfPresent(extractParam(routeContext, paramName, type), paramName, map)) {
            return map;
        }
        if (addIfPresent(extractHeaderParam(routeContext, paramName), paramName, map)) {
            return map;
        }
        if (addIfPresent(extractCookieParam(routeContext, paramName, type), paramName, map)) {
            return map;
        }
        if (addIfPresent(extractDefaultParam(type, defaultValue), paramName, map)) {
            return map;
        }
        if (addIfPresent(extractPathParam(routeContext, paramName, type), paramName, map)) {
             return map;
        } else {
            throw ExceptionBundle.MESSAGES.missingParameterInRequest(paramName);
        }
    }
    
    private static Map<String, Object> extractRequestParams(final ReplacementParameter<?> replacementParam, final RouteContext routeContext) throws Exception {
        final Map<String, Object> map = new HashMap<String, Object>();
        for (String paramName : replacementParam.getParamNames()) {
            extractRequestParam(paramName, String.class, Optional.absent(), map, routeContext);
        }
        return map;
    }
    
    private static Optional<?> extractDefaultParam(final Class<?> type, final Optional<?> defaultValue) throws Exception {
        if(defaultValue.isPresent()) {
            return Optional.of(createInstance(type, defaultValue.get().toString()));
        }
        return Optional.absent();
    }

    private static Object extractBody(final RouteContext routeContext, final Parameter<?> parameter, final Consumer consumer) {
        return consumer.unmarshall(routeContext.getRequest(), parameter.getType());
    }
    
    private static Consumer getConsumer(final RouteContext routeContext, final Map<String, Consumer> consumers, 
            final Parameter<?> parameter) {
        final Set<String> mediaTypes = routeContext.getRoute().consumes();
        final Optional<String> contentType = extractContentType(routeContext);
        if (contentType.isPresent()) {
            final Consumer consumer = consumers.get(contentType.get());
            if (consumer != null) {
                return consumer;
            }
        } else {
            for (String mediaType : mediaTypes) {
                final Consumer consumer = consumers.get(mediaType);
                if (consumer != null) {
                    return consumer;
                }
            }
        }
        throw ExceptionBundle.MESSAGES.noConsumerForMediaType(parameter, consumers.values(), mediaTypes);
    }
    
    private static Optional<String> extractContentType(final RouteContext routeContext) {
    	final String contentType = routeContext.getRequest().getContentType();
    	if (contentType != null) {
    		try {
				MimeType mimeType = new MimeType(contentType);
				return Optional.of(mimeType.getBaseType());
			} catch (MimeTypeParseException e) {
				throw new RuntimeException("Error parsing content-type " + contentType, e);
			}
    	}
    	return Optional.absent();
    }
    
    public static Optional<?> extractPathParam(final RouteContext routeContext, final RequestParameter<?> param) throws Exception {
        return extractPathParam(routeContext, param.getName(), param.getType());
    }
    
    /**
     * Extracts a path parameter from the passed in request path.
     * 
     * @param routeContext the {@link org.jboss.aerogear.controller.router.RouteContext} to extract a path parameter from.
     * @param paramName the name of the parameter to be extracted.
     * @param type the parameter type.
     * @return {@code Optional<String>} containing the extracted path param if present in the request path.
     */
    public static Optional<?> extractPathParam(final RouteContext routeContext, final String paramName, final Class<?> type) throws Exception {
        final String requestPath = routeContext.getRequestPath();
        final Map<String, String> pathParams = RequestUtils.mapPathParams(requestPath, routeContext.getRoute().getPath());
        if (pathParams.containsKey(paramName)) {
            final String value = pathParams.get(paramName);
            if (value != null) {
                return Optional.of(createInstance(type, pathParams.get(paramName)));
            }
        }
        return Optional.absent();
    }

    /**
     * Returns an instance of the type used in the parameter names using Iogi. </p> For example, having form parameters named
     * 'car.color', 'car.brand', this method would try to use those values to instantiate a new Car instance.
     * 
     * @return {@link com.google.common.base.Optional} may contain the instantiated instance, else isPresent will return false.
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
            return Optional.fromNullable(IOGI.instantiate(target,
                    parameters.toArray(new br.com.caelum.iogi.parameters.Parameter[parameters.size()])));
        }
        return Optional.absent();
    }

    private static boolean addIfPresent(final Optional<?> op, final String paramName, final Map<String, Object> args) {
        if (op.isPresent()) {
            args.put(paramName, op.get());
            return true;
        }
        return false;
    }

    private static Optional<?> extractHeaderParam(final RouteContext routeContext, final String paramName) {
        return Optional.fromNullable(routeContext.getRequest().getHeader(paramName));
    }
    
    private static Optional<?> extractCookieParam(final RouteContext routeContext, final String paramName, final Class<?> type) throws Exception {
        final Cookie[] cookies = routeContext.getRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(paramName)) {
                    return Optional.fromNullable(createInstance(type, cookie.getValue()));
                }
            }
        }
        return Optional.absent();
    }
    
    private static Optional<?> extractParam(final RouteContext routeContext, final String name, final Class<?> type) throws Exception {
        final String[] values = routeContext.getRequest().getParameterMap().get(name);
        if (values != null) {
            if (values.length == 1) {
                return Optional.of(createInstance(type, values[0]));
            } else {
                throw ExceptionBundle.MESSAGES.multivaluedParamsUnsupported(name);
            }
        }
        return Optional.absent();
    }

    private static Object createInstance(Class<?> type, String arg) throws Exception {
        final Constructor<?> constructor = type.getDeclaredConstructor(String.class);
        return constructor.newInstance(arg);
    }
 }
