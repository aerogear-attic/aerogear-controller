package org.jboss.aerogear.controller.log;

import java.util.Set;

import javax.servlet.ServletException;

import org.jboss.aerogear.controller.router.RequestMethod;
import org.jboss.aerogear.controller.router.Responders;
import org.jboss.aerogear.controller.router.Route;
import org.jboss.aerogear.controller.router.parameter.Parameter;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * A JBoss-Logging MessageBundle containing translated Strings, Exceptions etc.
 * </p>
 * Contains no methods that perform logging. Refer to {@link AeroGearLogger} for that.
 */
@MessageBundle(projectCode = "AG_CONTROLLER")
public interface LoggerMessages {
    LoggerMessages MESSAGES = Messages.getBundle(LoggerMessages.class);

    @LogMessage(level = Logger.Level.FATAL)
    @Message(id = 1, value = "must be run inside a Servlet container")
    ServletException mustRunInsideAContainer();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 4, value = "No route found for method: '%s', requested URI: '%s', Accept: '%s'")
    RuntimeException routeNotFound(RequestMethod method, String requestURI, Set<String> acceptHeaders);
    
    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 9, value = "oops, multivalued params not supported yet. Parameter name: '%s'")
    RuntimeException multivaluedParamsUnsupported(String parameterName);
    
    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 10, value = "Parameter: '%s' was missing from Request destined for route: '%s'")
    RuntimeException missingParameterInRequest(Parameter<?> parameter, Route route);
    
    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 11, value = "No Responder was found that matched the Accept Header: '%s'. The following Responders are registered: '%s'")
    RuntimeException noResponderForRequestedMediaType(String acceptHeader, Responders responders);
}
