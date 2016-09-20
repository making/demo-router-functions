package com.example;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.Response;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpServer;

import java.time.Duration;
import java.util.Optional;

import static org.springframework.web.reactive.function.RequestPredicates.*;
import static org.springframework.web.reactive.function.RouterFunctions.*;
import static org.springframework.web.reactive.function.BodyInserters.*;
import static org.springframework.web.reactive.function.BodyExtractors.*;

import org.springframework.web.reactive.function.RouterFunction;

public class DemoLambdaRoutingApplication {

	public static void main(String[] args) throws Exception {

		// Simple
		RouterFunction<?> route = route(GET("/"),
				req -> Response.ok().body(fromObject("Sample")))
						.and(route(GET("/hello"),
								req -> Response.ok().body(fromObject("Hello World!"))))
						.and(route(GET("/bar"),
								req -> Response.ok()
										.body(fromObject("query[foo] = "
												+ req.queryParam("foo").orElse("???")))))
						.and(route(GET("/bar/{foo}"),
								req -> Response.ok()
										.body(fromObject("path[foo] = "
												+ req.pathVariable("foo").orElse("??")))))
						.and(route(GET("/json"),
								req -> Response.ok()
										.contentType(MediaType.APPLICATION_JSON)
										.body(fromObject(new Person("John", 30)))));

		// Reactive
		RouterFunction<?> reactiveRoute = route(GET("/reactive"),
				req -> Response.ok()
						.body(fromPublisher(Flux.just("Hello", "World"), String.class)))
								.and(route(POST("/echo"),
										req -> Response.ok()
												.body(fromPublisher(
														Flux.just("Hi ")
																.concatWith(req.body(
																		toMono(String.class))),
														String.class))))
								.and(route(POST("/json"), req -> {
									Mono<Person> personMono = req
											.body(toMono(Person.class));
									return Response.ok().body(
											fromPublisher(personMono, Person.class));
								}));

		// Server Sent Event
		RouterFunction<?> sseRoute = route(GET("/sse"),
				req -> Response.ok()
						.body(fromServerSentEvents(
								Flux.interval(Duration.ofSeconds(1))
										.map(l -> ServerSentEvent.builder(l)
												.id(String.valueOf(l)).comment("foo")
												.build()))));

		// Method Reference
		PersonHandler ph = new PersonHandler();
		RouterFunction<?> methodReference = route(GET("/person/{id}"), ph::findPerson)
				.and(route(GET("/person"), ph::findAll));

		ReactorHttpHandlerAdapter httpHandlerAdapter = new ReactorHttpHandlerAdapter(
				toHttpHandler(route.and(reactiveRoute).and(sseRoute).and(methodReference)
						.filter((req, next) -> {
							System.out.println("==== Before... " + req.uri());
							Response<?> res = next.handle(req);
							System.out.println("==== After... " + req.uri());
							return res;
						})));

		int port = Optional.ofNullable(System.getenv("PORT")).map(Integer::parseInt)
				.orElse(8080);
		HttpServer httpServer = HttpServer.create("0.0.0.0", port);
		httpServer.startAndAwait(httpHandlerAdapter);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shut down ...");
			httpServer.shutdown();
		}));
	}
}
