package com.example;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.Response;
import org.springframework.web.reactive.function.Router;
import org.springframework.web.reactive.function.RoutingFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpServer;

import java.time.Duration;

import static org.springframework.web.reactive.function.RequestPredicates.GET;
import static org.springframework.web.reactive.function.RequestPredicates.POST;
import static org.springframework.web.reactive.function.Router.route;

public class DemoLambdaRoutingApplication {

	public static void main(String[] args) throws Exception {

		// Simple
		RoutingFunction<?> route = route(GET("/"), req -> Response.ok().body("Sample"))
				.and(route(GET("/hello"), req -> Response.ok().body("Hello World!")))
				.and(route(GET("/bar"),
						req -> Response.ok()
								.body("query[foo] = "
										+ req.queryParam("foo").orElse("???"))))
				.and(route(GET("/bar/{foo}"),
						req -> Response.ok()
								.body("path[foo] = "
										+ req.pathVariable("foo").orElse("??"))))
				.andOther(
						route(GET("/json"),
								req -> Response.ok()
										.contentType(MediaType.APPLICATION_JSON)
										.body(new Person("John", 30))));

		// Reactive
		RoutingFunction<?> reactiveRoute = route(GET("/reactive"),
				req -> Response.ok().stream(Flux.just("Hello", "World"), String.class))
						.and(route(POST("/echo"),
								req -> Response.ok().stream(
										Flux.just("Hi ")
												.concatWith(req.body()
														.convertToMono(String.class)),
										String.class)))
						.andOther(route(POST("/json"), req -> {
							// DOES NOT WORK AT THIS MOMENT

							Mono<Person> personMono = req.body()
									.convertToMono(Person.class);
							return Response.ok().stream(personMono, Person.class);
						}));

		// Server Sent Event
		RoutingFunction<?> sseRoute = route(GET("/sse"),
				req -> Response
						.ok().sse(
								Flux.interval(Duration.ofSeconds(1))
										.map(l -> ServerSentEvent.builder(l)
												.id(String.valueOf(l)).comment("foo")
												.build())));

		// Method Reference
		PersonHandler ph = new PersonHandler();
		RoutingFunction<?> methodReference = route(GET("/person/{id}"), ph::findPerson)
				.andOther(route(GET("/person"), ph::findAll));

		ReactorHttpHandlerAdapter httpHandlerAdapter = new ReactorHttpHandlerAdapter(
				Router.toHttpHandler(route.andOther(reactiveRoute).andOther(sseRoute)
						.andOther(methodReference).filter((req, next) -> {
							System.out.println("==== Before... " + req.uri());
							Response<?> res = next.handle(req);
							System.out.println("==== After... " + req.uri());
							return res;
						})));

		HttpServer httpServer = HttpServer.create(8080);
		httpServer.startAndAwait(httpHandlerAdapter);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shut down ...");
			httpServer.shutdown();
		}));
	}
}
