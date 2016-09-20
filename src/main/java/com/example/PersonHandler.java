package com.example;

import org.springframework.web.reactive.function.Request;
import org.springframework.web.reactive.function.Response;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.springframework.web.reactive.function.BodyInserters.*;

public class PersonHandler {

	public Response<?> findPerson(Request req) {
		Optional<String> id = req.pathVariable("id");
		return id.isPresent()
				? Response.ok().body(fromPublisher(
						Mono.just(new Person("P" + id.get(), 10)), Person.class))
				: Response.badRequest().build();
	}

	public Response<?> findAll(Request req) {
		return Response.ok().body(fromPublisher(
				Flux.just(new Person("p1", 11), new Person("p2", 12)), Person.class));
	}
}
