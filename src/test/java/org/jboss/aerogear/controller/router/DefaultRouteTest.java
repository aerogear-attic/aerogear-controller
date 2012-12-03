package org.jboss.aerogear.controller.router;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.fest.assertions.Fail;
import org.jboss.aerogear.controller.SampleController;
import org.junit.Test;

public class DefaultRouteTest {
    
    private static final RequestMethod[] GET = new RequestMethod[]{RequestMethod.GET};
    private static final Class<?> TARGET_CLASS = SampleController.class;
    private static final Method TARGET_METHOD = indexMethod(TARGET_CLASS, "index");
    
    @Test
    public void construct() throws Exception {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        assertThat(route.getPath()).isEqualTo("/car/{id}");
        assertThat(route.getMethods().size()).isEqualTo(1);
        assertThat(route.getMethods()).contains(RequestMethod.GET);
        assertThat(route.getTargetClass()).isEqualTo(SampleController.class);
        assertThat(route.getTargetMethod()).isEqualTo(TARGET_METHOD);
        assertThat(route.getRoles()).isEmpty();
        assertThat(route.isSecured()).isFalse();
        assertThat(route.hasExceptionsRoutes()).isFalse();
        assertThat(route.produces()).contains(MediaType.HTML.toString());
    }
    
    @Test
    public void createRouteWithNullHttpMethods() throws Exception {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").on((RequestMethod[])null).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        assertThat(route.getMethods()).isEmpty();
    }

    @Test
    public void constructWithNullRoles() throws Exception {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").roles((String[])null).on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        assertThat(route.getRoles()).isEmpty();
    }

    @Test
    public void constructWithNullExceptions() throws Exception {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").setThrowables((Set<Class<? extends Throwable>>) null).on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        assertThat(route.hasExceptionsRoutes()).isFalse();
    }

    @Test
    public void constructWithRoles() throws Exception {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").roles("admin").on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        assertThat(route.getRoles().size()).isEqualTo(1);
        assertThat(route.getRoles()).contains("admin");
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void constructWithExceptions() throws Exception {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").setThrowables(exceptions(IllegalArgumentException.class)).on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        assertThat(route.hasExceptionsRoutes()).isTrue();
    }
    
    @Test (expected = UnsupportedOperationException.class)
    public void tryToModifyRoles() throws Exception {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").roles("admin").on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        route.getRoles().remove("admin");
    }

    @Test (expected = UnsupportedOperationException.class)
    public void tryToModifyHttpMethods() throws Exception {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        route.getMethods().remove(RequestMethod.GET);
    }

    @Test
    public void matchesUnparameterizedRoutePaths() throws NoSuchMethodException {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/index").on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        assertThat(route.matches(RequestMethod.GET, "/index")).isTrue();
    }

    @Test
    public void matchesParameterizedRoutePaths() throws NoSuchMethodException {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        assertThat(route.matches(RequestMethod.GET, "/car/3")).isTrue();
    }
    
    @Test
    public void toStringTest() {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/car/{id}").on(GET).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        final String expected = "DefaultRoute[path=/car/{id}, " +
            "targetClass=class org.jboss.aerogear.controller.SampleController, " +
            "targetMethod=public void org.jboss.aerogear.controller.SampleController.index(), " +
            "roles=[], throwables=[]]";
        assertThat(route.toString()).isEqualTo(expected);
    }
    
    @Test
    public void produces() {
        final RouteDescriptor rd = new RouteDescriptor();
        rd.setPath("/home").on(GET).produces(MediaType.JSON.toString()).to(SampleController.class).index();
        final Route route = new DefaultRoute(rd);
        assertThat(route.produces()).contains(MediaType.JSON.toString());
    }

    private static Method indexMethod(final Class<?> targetClass, final String methodName) {
        Method m = null;
        try {
            m = targetClass.getDeclaredMethod(methodName);
        } catch (final Exception e) {
            Fail.fail("Could not find a method named '" + methodName + "' on target class", e);
        }
        return m;
    }
    
    private Set<Class<? extends Throwable>> exceptions(final Class<? extends Throwable>... exs) {
        return new HashSet<Class<? extends Throwable>>(Arrays.asList(exs));
    }
    
}
