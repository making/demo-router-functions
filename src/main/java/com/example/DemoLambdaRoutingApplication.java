package com.example;

import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.BodyInserters.fromServerSentEvents;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RouterFunctions.toHttpHandler;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.netty.channel.Channel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

public class DemoLambdaRoutingApplication {

	public static void main(String[] args) throws Exception {
		int port = Optional.ofNullable(System.getenv("PORT")).map(Integer::parseInt)
				.orElse(8080);
		Channel channel = null;
		try {

			HttpServer httpServer = HttpServer.create("0.0.0.0", port);
			Mono<? extends NettyContext> handler = httpServer
					.newHandler(new ReactorHttpHandlerAdapter(httpHandler()));
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shut down ...");
			}));
			channel = handler.block().channel();
			channel.closeFuture().sync();
		}
		finally {
			if (channel != null) {
				channel.eventLoop().shutdownGracefully();
			}
		}
	}

	static HttpHandler httpHandler() {
		// Simple
		RouterFunction<?> route = route(GET("/"),
				req -> ok().body(Mono.just("Sample"), String.class))
						.andRoute(GET("/hello"),
								req -> ok().body(Mono.just("Hello World!"), String.class))
						.andRoute(GET("/bar"),
								req -> ok().body(
										Mono.just("query[foo] = "
												+ req.queryParam("foo").orElse("???")),
										String.class))
						.andRoute(GET("/bar/{foo}"),
								req -> ok().body(
										Mono.just(
												"path[foo] = " + req.pathVariable("foo")),
										String.class))
						.andRoute(GET("/json"),
								req -> ok().contentType(MediaType.APPLICATION_JSON).body(
										Mono.just(new Person("John", 30)), Person.class));

		// Reactive
		RouterFunction<?> reactiveRoute = route(GET("/reactive"),
				req -> ok().body(Flux.just("Hello", "World"), String.class)).andRoute(
						POST("/echo"),
						req -> ok().body(Flux.just("Hi ").concatWith(
								req.body(toMono(String.class))), String.class))
						.andRoute(POST("/json"), req -> {
							Mono<Person> personMono = req.body(toMono(Person.class));
							return ok().body(personMono, Person.class);
						});

		// Server Sent Event
		RouterFunction<?> sseRoute = route(GET("/sse"),
				req -> ok().body(fromServerSentEvents(Flux.interval(Duration.ofSeconds(1))
						.take(10).map(l -> ServerSentEvent.builder(l)
								.id(String.valueOf(l)).comment("foo").build()))));

		// Method Reference
		PersonHandler ph = new PersonHandler();
		RouterFunction<?> methodReference = route(GET("/person/{id}"), ph::findPerson)
				.and(route(GET("/person"), ph::findAll));

		// Composed and Filter
		RouterFunction<?> composed = route.and(reactiveRoute).and(sseRoute)
				.and(methodReference).filter((req, next) -> {
					System.out.println("==== Before... " + req.uri());
					Mono<? extends ServerResponse> res = next.handle(req);
					System.out.println("==== After... " + req.uri());
					return res;
				});

		return toHttpHandler(composed);
	}
}
