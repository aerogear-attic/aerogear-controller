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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.aerogear.controller.router.RequestMethod;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RequestUtilsTest {
    
    @Mock
    private ServletContext servletContext;
    @Mock
    private HttpServletRequest request;
    
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        when(request.getServletContext()).thenReturn(servletContext);
    }
    
    @Test
    public void extractPath() {
        when(servletContext.getContextPath()).thenReturn("/myapp");
        when(request.getRequestURI()).thenReturn("/myapp/cars/1");
        assertThat(RequestUtils.extractPath(request)).isEqualTo("/cars/1");
    }
    
    @Test
    public void extractPathDefaultWebApp() {
        when(servletContext.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/cars/1");
        assertThat(RequestUtils.extractPath(request)).isEqualTo("/cars/1");
    }
    
    @Test
    public void extractMethod() {
        when(request.getMethod()).thenReturn("GET");
        assertThat(RequestUtils.extractMethod(request)).isEqualTo(RequestMethod.GET);
        when(request.getMethod()).thenReturn("PUT");
        assertThat(RequestUtils.extractMethod(request)).isEqualTo(RequestMethod.PUT);
        when(request.getMethod()).thenReturn("POST");
        assertThat(RequestUtils.extractMethod(request)).isEqualTo(RequestMethod.POST);
        when(request.getMethod()).thenReturn("DELETE");
        assertThat(RequestUtils.extractMethod(request)).isEqualTo(RequestMethod.DELETE);
        when(request.getMethod()).thenReturn("OPTIONS");
        assertThat(RequestUtils.extractMethod(request)).isEqualTo(RequestMethod.OPTIONS);
        when(request.getMethod()).thenReturn("HEAD");
        assertThat(RequestUtils.extractMethod(request)).isEqualTo(RequestMethod.HEAD);
        when(request.getMethod()).thenReturn("PATCH");
        assertThat(RequestUtils.extractMethod(request)).isEqualTo(RequestMethod.PATCH);
    }
    
}
