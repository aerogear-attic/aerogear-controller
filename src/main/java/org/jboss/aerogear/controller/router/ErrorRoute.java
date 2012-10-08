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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.annotation.WebServlet;

import org.jboss.aerogear.controller.RequestMethod;

/**
 * A singleton {@link Route} that acts as a catch-all error {@link Route} which 
 * is will be used when no explicit error route has been defined.
 */
public enum ErrorRoute {
    
    DEFAULT;
    
    private final Route route;
    
    @SuppressWarnings("unchecked")
    private ErrorRoute() {
        route = new DefaultRoute(ErrorServlet.class.getAnnotation(WebServlet.class).value()[0], 
            new RequestMethod[]{RequestMethod.GET}, 
            ErrorHandler.class, targetMethod("error"),
            new HashSet<Class<? extends Throwable>>(Arrays.asList(Throwable.class)));
    }
    
    /**
     * Returns an {@link Route} which is configured to route to an instance of {@link ErrorHandler}.
     * 
     * @return {@link Route} provided as a fallback when a route has no explicit error route  defined.
     */
    public Route getRoute() {
        return route;
    }
    
    private static final Method targetMethod(final String methodName) {
        try {
            return ErrorHandler.class.getDeclaredMethod(methodName, Throwable.class);
        } catch (final Exception e) {
            throw new AeroGearException("Could not find a method named '" + methodName + "' on target class", e);
        }
    }

}
