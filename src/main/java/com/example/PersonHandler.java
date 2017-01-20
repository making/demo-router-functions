package com.example;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class PersonHandler {

	public Mono<ServerResponse> findPerson(ServerRequest req) {
		String id = req.pathVariable("id");
		return ServerResponse.ok().body(Mono.just(new Person("P" + id, 10)),
				Person.class);
	}

	public Mono<ServerResponse> findAll(ServerRequest req) {
		return ServerResponse.ok().body(
				Flux.just(new Person("p1", 11), new Person("p2", 12)), Person.class);
	}
}
