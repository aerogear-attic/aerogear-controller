# aerogear-controller - very lean mvc controller

## how to create a new project

### basic use case
1. add the maven dependency

        <dependency>
            <groupId>org.jboss.aerogear</groupId>
            <artifactId>aerogear-controller</artifactId>
            <version>1.0.0.M1</version>
            <scope>compile</scope>
        </dependency>

1. create a pojo controller

        public class Home {
            public void index() {
            }
        }

1. create a Java class containing the routes (must extend `AbstractRoutingModule`)

        public class Routes extends AbstractRoutingModule {

        @Override
        public void configuration() {
            route()
                   .from("/")
                   .on(RequestMethod.GET)
                   .to(Home.class).index();
            }
        }

1. create a jsp page at `/WEB-INF/pages/<Controller Class Name>/<method>.jsp`

        <!-- /WEB-INF/pages/Home/index.jsp -->
        <html>
            <body>
                <p>hello from index!</p>
            </body>
        </html>
        
### populating parameters

You can use immutable beans straight away as controller parameters:

        public class Store {
            public Car save(Car car) {
                return car;
            }
        }

This can be populated by putting a route to it (preferably via post, of course)

        route()
               .from("/cars")
               .on(RequestMethod.POST)
               .to(Store.class).save(param(Car.class));


And you can use a simple html form for it, by just following the convention:

            <input type="text" name="car.color"/>
            <input type="text" name="car.brand"/>

The car object will be automatically populated with the provided values - note that it supports deep linking, so this would work fine too:

            <input type="text" name="car.brand.owner"/>

All the intermediate objects are created automatically.
You can find a slightly better example at <https://github.com/aerogear/aerogear-controller-demo> 

## Error handling
You can configure a route as an error handler for a type of exception:

        route()
               .on(YourException.class)
               .to(ExceptionHandler.class).errorPage(); 

You can specify multiple exceptions if needed:

        route()
               .on(YourException.class, SomeOtherException.class)
               .to(ExceptionHandler.class).errorPage();
               
If you'd like to log the exception in the error route, you can specify that the target method, errorPage() above, should 
take a parameter:

        route()
               .on(YourException.class, SomeOtherException.class)
               .to(ExceptionHandler.class).errorPage(param(Exception.class));

As mentioned previously in this document a view for ExceptionHandler would in this case be located in:

        /WEB-INF/pages/ExceptionHandler/errorPage.jsp   

The exception is made available to the jsp page:

        ${requestScope['org.jboss.aerogear.controller.exception']}

        
## Cross Origin Resource Sharing  (CORS) Support
[CORS](http://www.w3.org/TR/cors/) is supported by default but can be disabled, or configured differently by implementing a CDI Producer:

    @Produces
    public CorsConfiguration demoConfig() {
        return CorsConfig.enableCorsSupport()
                .anyOrigin()
                .enableCookies()
                .exposeHeaders("accept, links")
                .maxAge(20)
                .enableAllRequestMethods()
                .validRequestHeaders("accept, links");
    }
The CDI container will detect the above annotation and inject it into AeroGear Controller.    
For more information regarding the configuration options available, please refer to the javadocs for 
[CorsConfiguration](https://github.com/aerogear/aerogear-controller/blob/master/src/main/java/org/jboss/aerogear/controller/router/decorators/cors/CorsConfiguration.java)

### Disable CORS support ###
Example of disabling CORS support:

    @Produces
    public CorsConfiguration demoConfig() {
        return CorsConfig.disableCorsSupport();
    }
   
---