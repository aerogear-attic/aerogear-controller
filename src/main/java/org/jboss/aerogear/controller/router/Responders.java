/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.aerogear.controller.log.LoggerMessages;
import org.jboss.aerogear.controller.util.RequestUtils;
import com.google.common.collect.Sets;

/**
 * Handles responding from a Route invocation by delegating to the appropriate {@link Responder}.
 * 
 * @see Responder
 */
public class Responders {
    
    private final Set<Responder> responders = new LinkedHashSet<Responder>();
    
    @Inject
    public Responders(final Instance<Responder> responders) {
        for (Responder responder : responders) {
            this.responders.add(responder); 
        }
    }
    
    /**
     * Responds to the Route in the passed-in RouteContext using an appropriate Responder.
     * </p>
     * The {@link Responder} used to respond is determined by inspecting the HTTP 
     * Accept header values and matching these with the media types that the Route
     * is capable of producing ({@link Route#produces()}) <br>
     * If no match is found for the values in the Accept header, or if the Accept
     * header was empty of "*&#47;*" then any Responder that accepts "*&#47;*" will 
     * be used to respond.
     * 
     * @param routeContext the current route context
     * @param result the result from invoking the Route's target endpoint method.
     * @throws Exception if an exception is thrown while trying to respond.
     */
    public void respond(final RouteContext routeContext, final Object result) throws Exception {
        final Set<String> acceptHeaders = RequestUtils.extractAcceptHeader(routeContext.getRequest());
        for (String mediaType : Sets.intersection(routeContext.getRoute().produces(), acceptHeaders)) {
            if (respond(mediaType, result, routeContext)) {
                return;
            }
        }
        if (acceptHeaders.contains(MediaType.ANY.toString()) || acceptHeaders.isEmpty()) {
            respond(MediaType.ANY.toString(), result, routeContext);
        } else {
            throw LoggerMessages.MESSAGES.noResponderForRequestedMediaType(routeContext.getRequest().getHeader("Accept"), this);
        }
    }

    private boolean respond(final String mediaType, final Object result, final RouteContext routeContext) throws Exception {
        for (Responder responder : responders) {
            if (responder.accepts(mediaType)) {
                responder.respond(result, routeContext);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "Responders[" + responders + "]";
    }

}
