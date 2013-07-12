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

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.jboss.aerogear.controller.Car;
import org.jboss.aerogear.controller.SampleController;
import org.jboss.aerogear.controller.router.Consumer;
import org.jboss.aerogear.controller.router.MediaType;
import org.jboss.aerogear.controller.router.Route;
import org.jboss.aerogear.controller.router.RouteContext;
import org.jboss.aerogear.controller.util.ParameterExtractor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.base.Optional;

public class ParameterExtractorTest {

    @Mock
    private ServletContext servletContext;
    @Mock
    private HttpServletRequest request;
    @Mock
    private Route route;
    @Mock
    private RouteContext routeContext;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        when(request.getServletContext()).thenReturn(servletContext);
        when(routeContext.getRequest()).thenReturn(request);
        when(routeContext.getRoute()).thenReturn(route);
        when(route.getPath()).thenReturn("/cars");
        when(routeContext.getRequestPath()).thenReturn("/cars");
    }

    @Test
    public void extractPathParameter() throws Exception {
        when(route.getPath()).thenReturn("/cars/{id}");
        when(routeContext.getRequestPath()).thenReturn("/cars/2");
        final Optional<?> param = ParameterExtractor.extractPathParam(routeContext, "id", String.class);
        assertThat(param.get()).isEqualTo("2");
    }
    
    @Test
    public void extractPathParameters() throws Exception {
        when(route.getPath()).thenReturn("/cars/{color}/subpath/{brand}");
        when(routeContext.getRequestPath()).thenReturn("/cars/red/subpath/BMW");
        assertThat(ParameterExtractor.extractPathParam(routeContext, "color", String.class).get()).isEqualTo("red");
        assertThat(ParameterExtractor.extractPathParam(routeContext, "brand", String.class).get()).isEqualTo("BMW");
    }

    @Test
    public void extractPathParameterButNoParamInRequest() throws Exception {
        when(route.getPath()).thenReturn("/cars/{id}");
        when(routeContext.getRequestPath()).thenReturn("/c");
        assertThat(ParameterExtractor.extractPathParam(routeContext, "id", String.class).isPresent()).isFalse();
    }

    @Test
    public void extractIogiParams() throws Exception {
        when(route.getTargetMethod()).thenReturn(SampleController.class.getMethod("save", Car.class));
        final Map<String, String[]> paramMap = RequestParams.param("car.color", "red").add("car.brand", "Ferrari")
                .getParamMap();
        when(request.getParameterMap()).thenReturn(paramMap);
        final Optional<?> optional = ParameterExtractor.extractIogiParam(routeContext);
        assertThat(((Car) optional.get()).getColor()).isEqualTo("red");
    }

    @Test
    public void extractIogiParamsNone() throws Exception {
        when(route.getTargetMethod()).thenReturn(SampleController.class.getMethod("save", Car.class));
        final Map<String, String[]> paramMap = RequestParams.param("name", "Herbie").getParamMap();
        when(request.getParameterMap()).thenReturn(paramMap);
        final Optional<?> optional = ParameterExtractor.extractIogiParam(routeContext);
        assertThat(optional.isPresent()).isFalse();
    }

    @Test
    public void extractPathParam() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.empty());
        when(route.getParameters()).thenReturn(asList(Parameter.param("id", String.class)));
        when(route.getPath()).thenReturn("/cars/{id}");
        when(routeContext.getRequestPath()).thenReturn("/cars/2");
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("id")).isEqualTo("2");
    }

    @Test
    public void extractPathParamWithLongType() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.empty());
        when(route.getParameters()).thenReturn(asList(Parameter.param("id", Long.class)));
        when(route.getPath()).thenReturn("/cars/{id}");
        when(routeContext.getRequestPath()).thenReturn("/cars/2");
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("id")).isEqualTo(new Long(2));
    }

    @Test
    public void extractPathParamWithIntegerType() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.empty());
        when(route.getParameters()).thenReturn(asList(Parameter.param("id", Integer.class)));
        when(route.getPath()).thenReturn("/cars/{id}");
        when(routeContext.getRequestPath()).thenReturn("/cars/2");
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("id")).isEqualTo(new Integer(2));
    }

    @Test
    public void extractFormParams() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.param("name", "Newman").getParamMap());
        when(route.getParameters()).thenReturn(asList(Parameter.param("name", String.class)));
        when(route.getTargetMethod()).thenReturn(SampleController.class.getMethod("client", String.class));
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("name")).isEqualTo("Newman");
    }

    @Test
    public void extractFormParamsWithDefaultValue() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.empty());
        when(route.getParameters()).thenReturn(asList(Parameter.param("name", "defaultName", String.class)));
        when(route.getTargetMethod()).thenReturn(SampleController.class.getMethod("client", String.class));
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("name")).isEqualTo("defaultName");
    }

    @Test
    public void extractFormParamsWithDefaultValueWithType() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.empty());
        when(route.getParameters()).thenReturn(asList(Parameter.param("name", new Long(2), Long.class)));
        when(route.getTargetMethod()).thenReturn(SampleController.class.getMethod("client", Long.class));
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("name")).isEqualTo( new Long(2));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIfFormParamIsMissingFromRequest() throws Exception {
        when(route.getParameters()).thenReturn(asList(Parameter.param("name", null, String.class)));
        when(route.getTargetMethod()).thenReturn(SampleController.class.getMethod("client", String.class));
        ParameterExtractor.extractArguments(routeContext, Collections.<String, Consumer> emptyMap());
    }

    @Test
    public void extractFormParamIogi() throws Exception {
        final Map<String, String[]> paramMap = RequestParams.param("car.color", "red").add("car.brand", "Ferrari")
                .getParamMap();
        when(request.getParameterMap()).thenReturn(paramMap);
        when(route.getParameters()).thenReturn(asList(Parameter.param(Car.class)));
        when(route.getTargetMethod()).thenReturn(SampleController.class.getMethod("save", Car.class, String.class));
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("entityParam") instanceof Car).isTrue();
        assertThat(((Car) args.get("entityParam")).getColor()).isEqualTo("red");
    }

    @Test
    public void extractHeaderParam() throws Exception {
        when(request.getHeader("x-header")).thenReturn("headerValue");
        final List<Parameter<?>> parameters = asList(Parameter.param("x-header", "def", String.class));
        when(route.getParameters()).thenReturn(parameters);
        when(route.getPath()).thenReturn("/cars");
        when(routeContext.getRequestPath()).thenReturn("/cars");
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("x-header")).isEqualTo("headerValue");
    }

    @Test
    public void extractHeaderParamWithDefaultValue() throws Exception {
        final List<Parameter<?>> parameters = asList(Parameter.param("x-header", "defaultHeader", String.class));
        when(route.getParameters()).thenReturn(parameters);
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("x-header")).isEqualTo("defaultHeader");
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIsHeaderParamIsMissingFromRequest() throws Exception {
        final List<Parameter<?>> parameters = asList(Parameter.param("x-header", String.class));
        when(route.getParameters()).thenReturn(parameters);
        ParameterExtractor.extractArguments(routeContext, Collections.<String, Consumer> emptyMap());
    }

    @Test
    public void extractQueryParams() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.param("brand", "mini").add("year", "2006").getParamMap());
        final List<Parameter<?>> parameters = asList(Parameter.param("brand", String.class));
        parameters.add(Parameter.param("year", String.class));
        when(route.getParameters()).thenReturn(parameters);
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("brand")).isEqualTo("mini");
        assertThat(args.get("year")).isEqualTo("2006");
    }

    @Test
    public void extractQueryParamsWithType() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.param("price", "10").add("year", "2006").getParamMap());
        final List<Parameter<?>> parameters = asList(Parameter.param("price", Long.class));
        parameters.add(Parameter.param("year", Long.class));
        when(route.getParameters()).thenReturn(parameters);
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("price")).isEqualTo(new Long("10"));
        assertThat(args.get("year")).isEqualTo(new Long("2006"));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIfQueryParamsMissingFromRequest() throws Exception {
        final List<Parameter<?>> parameters = asList(Parameter.param("brand", String.class));
        parameters.add(Parameter.param("year", String.class));
        when(route.getParameters()).thenReturn(parameters);
        ParameterExtractor.extractArguments(routeContext, Collections.<String, Consumer> emptyMap());
    }

    @Test
    public void extractQueryParamsWithDefaultValue() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.param("brand", "mini").getParamMap());
        final List<Parameter<?>> parameters = asList(Parameter.param("brand", String.class));
        parameters.add(Parameter.param("year", "2012", String.class));
        when(route.getParameters()).thenReturn(parameters);
        when(route.getParameters()).thenReturn(parameters);
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("brand")).isEqualTo("mini");
        assertThat(args.get("year")).isEqualTo("2012");
    }

    @Test
    public void extractQueryParamsWithDefaultValueWithType() throws Exception {
        when(request.getParameterMap()).thenReturn(RequestParams.param("brand", "mini").getParamMap());
        final List<Parameter<?>> parameters = asList(Parameter.param("brand", String.class));
        parameters.add(Parameter.param("year", "2012", String.class));
        when(route.getParameters()).thenReturn(parameters);
        when(route.getParameters()).thenReturn(parameters);
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("brand")).isEqualTo("mini");
        assertThat(args.get("year")).isEqualTo("2012");
    }

    @Test
    public void extractCookieParam() throws Exception {
        final Cookie cookie = mock(Cookie.class);
        when(cookie.getName()).thenReturn("testCookie");
        when(cookie.getValue()).thenReturn("cookieValue");
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        final List<Parameter<?>> parameters = asList(Parameter.param("testCookie", String.class));
        when(route.getParameters()).thenReturn(parameters);
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("testCookie")).isEqualTo("cookieValue");
    }

    @Test
    public void extractCookieParamWithType() throws Exception {
        final Cookie cookie = mock(Cookie.class);
        when(cookie.getName()).thenReturn("testCookie");
        when(cookie.getValue()).thenReturn("2");
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        final List<Parameter<?>> parameters = asList(Parameter.param("testCookie", Long.class));
        when(route.getParameters()).thenReturn(parameters);
        final Map<String, Object> args = ParameterExtractor.extractArguments(routeContext,
                Collections.<String, Consumer> emptyMap());
        assertThat(args.get("testCookie")).isEqualTo(new Long(2));
    }

    @Test(expected = RuntimeException.class)
    public void extractCookieParamMissingFromRequest() throws Exception {
        final List<Parameter<?>> parameters = asList(Parameter.param("testCookie", String.class));
        when(route.getParameters()).thenReturn(parameters);
        ParameterExtractor.extractArguments(routeContext, Collections.<String, Consumer> emptyMap());
    }
    
    @Test
	public void extractArgumentsWithCharsetDefined() throws Exception {
    	// build parameters
    	final List<Parameter<?>> parameters = asList(Parameter.param(String.class));
        when(route.getParameters()).thenReturn(parameters);
    	when(request.getRequestURI()).thenReturn("/myapp/cars");
    	when(servletContext.getContextPath()).thenReturn("/myapp");
    	when(request.getContentType()).thenReturn("application/json; charset=utf-8");
    	Map<String, Consumer> consumers = new HashMap<String, Consumer>();

		// using a custom implementation to skip consumer logic
		consumers.put(MediaType.JSON.getType(), new Consumer() {

			@Override
			@SuppressWarnings("unchecked")
			public <T> T unmarshall(HttpServletRequest request, Class<T> type) {
				return (T) "test";
			}

			@Override
			public String mediaType() {
				return MediaType.JSON.getType();
			}
		});
    	
		//extract arguments
    	Map<String, Object> arguments = ParameterExtractor.extractArguments(
				routeContext, consumers);

		// verify result
		assertNotNull("parameter entityParam missing",
				arguments.get("entityParam"));
	}

    private List<Parameter<?>> asList(final Parameter<?>... p) {
        return new LinkedList<Parameter<?>>(Arrays.asList(p));
    }

    private static class RequestParams {

        final Map<String, String[]> paramMap = new HashMap<String, String[]>();

        private RequestParams(final String name, final String value) {
            add(name, value);
        }

        public static RequestParams param(final String name, final String value) {
            return new RequestParams(name, value);
        }

        public static Map<String, String[]> empty() {
            return Collections.emptyMap();
        }

        public RequestParams add(final String name, final String value) {
            paramMap.put(name, new String[] { value });
            return this;
        }

        public Map<String, String[]> getParamMap() {
            return paramMap;
        }

    }

}
