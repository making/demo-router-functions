package com.example;

import org.springframework.web.reactive.function.Request;
import org.springframework.web.reactive.function.Response;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class PersonHandler {

	public Response<?> findPerson(Request req) {
		Optional<String> id = req.pathVariable("id");
		return id.isPresent() ? Response.ok()
				.stream(Mono.just(new Person("P" + id.get(), 10)), Person.class)
				: Response.badRequest().build();
	}

	public Response<?> findAll(Request req) {
		return Response.ok().stream(Flux.just(new Person("p1", 11), new Person("p2", 12)),
				Person.class);
	}
}
