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

package org.jboss.aerogear.controller.router.decorators;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.aerogear.controller.SampleController;
import org.jboss.aerogear.controller.SampleControllerException;
import org.jboss.aerogear.controller.router.AbstractRoutingModule;
import org.jboss.aerogear.controller.router.ControllerFactory;
import org.jboss.aerogear.controller.router.MediaType;
import org.jboss.aerogear.controller.router.MvcResponder;
import org.jboss.aerogear.controller.router.RequestMethod;
import org.jboss.aerogear.controller.router.Responder;
import org.jboss.aerogear.controller.router.Responders;
import org.jboss.aerogear.controller.router.Route;
import org.jboss.aerogear.controller.router.RouteContext;
import org.jboss.aerogear.controller.router.RouteProcessor;
import org.jboss.aerogear.controller.router.Routes;
import org.jboss.aerogear.controller.router.RoutingModule;
import org.jboss.aerogear.controller.router.error.ErrorTarget;
import org.jboss.aerogear.controller.router.rest.JsonResponder;
import org.jboss.aerogear.controller.view.ViewResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ErrorHandlerTest {

    @Mock
    private BeanManager beanManager;
    @Mock
    private ViewResolver viewResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private ControllerFactory controllerFactory;
    @Mock
    private ServletContext servletContext;
    @Mock
    private RequestDispatcher requestDispatcher;
    @Mock
    private RouteProcessor routeProcessor;
    @Mock
    private Routes routes;
    private SampleController controller;
    private Responders responders;
    
    @Mock
    private Instance<Responder> responderInstance;
    @Mock
    private JsonResponder jsonResponder;
    @Mock
    private MvcResponder mvcResponder;
    
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        controller = spy(new SampleController());
        configureExceptionTestMocks(controller);
        instrumentResponders();
    }
    
    private void instrumentResponders() {
        when(mvcResponder.accepts(MediaType.HTML.toString())).thenReturn(true);
        when(mvcResponder.accepts(MediaType.ANY.toString())).thenReturn(true);
        when(jsonResponder.accepts(MediaType.JSON.toString())).thenReturn(true);
        final List<Responder> responders = new LinkedList<Responder>();
        responders.add(mvcResponder);
        responders.add(jsonResponder);
        when(this.responderInstance.iterator()).thenReturn(responders.iterator());
        this.responders = new Responders(responderInstance);
    }

    @Test 
    public void testOnException() throws Exception {
        final RoutingModule routingModule = new AbstractRoutingModule() {
            @Override
            public void configuration() throws Exception {
                route()
                        .on(IllegalStateException.class)
                        .to(SampleController.class).errorPage();
                route()
                        .from("/home")
                        .on(RequestMethod.GET, RequestMethod.POST)
                        .to(SampleController.class).throwIllegalStateException();
            }
        };
        final Routes routes = routingModule.build();
        final Route route = routes.routeFor(RequestMethod.GET, "/home", MediaType.defaultAcceptHeader());
        final ErrorHandler errorHandler = new ErrorHandler(routeProcessor, responders, controllerFactory, beanManager);
        doThrow(IllegalStateException.class).when(routeProcessor).process(any(RouteContext.class));
        errorHandler.process(new RouteContext(route, request, response, routes));
        verify(controller).errorPage();
        verify(mvcResponder).respond(anyObject(), any(RouteContext.class));
    }
    
    @Test 
    public void testOnExceptions() throws Exception {
        final RoutingModule routingModule = new AbstractRoutingModule() {
            @Override
            public void configuration() throws Exception {
                route()
                        .on(SampleControllerException.class, IllegalStateException.class)
                        .to(SampleController.class).error(param(Exception.class));
                route()
                        .from("/home")
                        .on(RequestMethod.GET, RequestMethod.POST)
                        .to(SampleController.class).throwSampleControllerException();
            }
        };
        final ErrorHandler errorHandler = new ErrorHandler(routeProcessor, responders, controllerFactory, beanManager);
        doThrow(IllegalStateException.class).when(routeProcessor).process(any(RouteContext.class));
        final Route route = routes.routeFor(RequestMethod.GET, "/home", MediaType.defaultAcceptHeader());
        errorHandler.process(new RouteContext(route, request, response, routingModule.build()));
        verify(controller).error(any(IllegalArgumentException.class));
        verify(mvcResponder).respond(anyObject(), any(RouteContext.class));
        verify(jsonResponder, never()).respond(anyObject(), any(RouteContext.class));
    }
    
    @Test
    public void testDefaultErrorRoute() throws Exception {
        final ErrorTarget errorTarget = mock(ErrorTarget.class);
        final RoutingModule routingModule = new AbstractRoutingModule() {
            @Override
            public void configuration() throws Exception {
                route()
                        .from("/home")
                        .on(RequestMethod.GET, RequestMethod.POST)
                        .to(SampleController.class).throwSampleControllerException();
            }
        };
        final Routes routes = routingModule.build();
        final Route route = routes.routeFor(RequestMethod.GET, "/home", MediaType.defaultAcceptHeader());
        final ErrorHandler errorHandler = new ErrorHandler(routeProcessor, responders, controllerFactory, beanManager);
        doThrow(SampleControllerException.class).when(routeProcessor).process(any(RouteContext.class));
        when(controllerFactory.createController(eq(ErrorTarget.class), eq(beanManager))).thenReturn(errorTarget);
        errorHandler.process(new RouteContext(route, request, response, routes));
        verify(errorTarget).error(any(SampleControllerException.class));
        verify(mvcResponder).respond(anyObject(), any(RouteContext.class));
        verify(jsonResponder, never()).respond(anyObject(), any(RouteContext.class));
    }
    
    @Test 
    public void testJsonResponseOnException() throws Exception {
        final RoutingModule routingModule = new AbstractRoutingModule() {
            @Override
            public void configuration() throws Exception {
                route()
                        .on(IllegalStateException.class)
                        .produces(MediaType.JSON.toString())
                        .to(SampleController.class).errorPage();
                route()
                        .from("/home")
                        .on(RequestMethod.GET, RequestMethod.POST)
                        .produces(MediaType.JSON.toString())
                        .to(SampleController.class).throwIllegalStateException();
            }
        };
        final Routes routes = routingModule.build();
        when(request.getHeader("Accept")).thenReturn(MediaType.JSON.toString());
        final Route route = routes.routeFor(RequestMethod.GET, "/home", new HashSet<String>(Arrays.asList(MediaType.JSON.toString())));
        final ErrorHandler errorHandler = new ErrorHandler(routeProcessor, responders, controllerFactory, beanManager);
        doThrow(IllegalStateException.class).when(routeProcessor).process(any(RouteContext.class));
        errorHandler.process(new RouteContext(route, request, response, routes));
        verify(controller).errorPage();
        verify(mvcResponder, never()).respond(anyObject(), any(RouteContext.class));
        verify(jsonResponder).respond(anyObject(), any(RouteContext.class));
    }
    
    private void configureExceptionTestMocks(final SampleController controller) {
        when(controllerFactory.createController(eq(SampleController.class), eq(beanManager))).thenReturn(controller);
        when(request.getMethod()).thenReturn(RequestMethod.GET.toString());
        when(request.getServletContext()).thenReturn(servletContext);
        when(servletContext.getContextPath()).thenReturn("/webapp");
        when(request.getRequestURI()).thenReturn("/webapp/home");
        when(request.getRequestDispatcher(anyString())).thenReturn(requestDispatcher);
        when(viewResolver.resolveViewPathFor((Route)anyObject())).thenReturn("WEB-INF/Home/error.jsp");
        when(request.getRequestDispatcher("WEB-INF/Home/error.jsp")).thenReturn(requestDispatcher);
     }

}
