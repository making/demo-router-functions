package com.example;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.Router;
import org.springframework.web.reactive.function.RoutingFunction;
import reactor.ipc.netty.http.HttpServer;

import static org.springframework.web.reactive.function.RequestPredicates.GET;
import static org.springframework.web.reactive.function.Router.route;

@Configuration
@ComponentScan
public class DemoLambdaRoutingApplication {

	public static void main(String[] args) throws Exception {

		ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				DemoLambdaRoutingApplication.class);
		applicationContext.registerShutdownHook();

		PersonHandler ph = applicationContext.getBean(PersonHandler.class);
		RoutingFunction<?> route = route(GET("/person/{id}"), ph::findPerson)
				.andOther(route(GET("/person"), ph::findAll));

		ReactorHttpHandlerAdapter httpHandlerAdapter = new ReactorHttpHandlerAdapter(
				Router.toHttpHandler(route,
						applicationContext.getBean(Router.Configuration.class)));

		HttpServer httpServer = HttpServer.create(8080);
		httpServer.startAndAwait(httpHandlerAdapter);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shut down ...");
			httpServer.shutdown();
		}));
	}
}
