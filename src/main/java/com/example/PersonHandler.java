package com.example;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.Request;
import org.springframework.web.reactive.function.Response;

import java.util.Optional;

@Component
public class PersonHandler {
	private final PersonRepository personRepository;

	public PersonHandler(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	public Response<?> findPerson(Request req) {
		Optional<String> id = req.pathVariable("id");
		return id.isPresent()
				? Response.ok().stream(personRepository.findOne(id.get()), Person.class)
				: Response.badRequest().build();
	}

	public Response<?> findAll(Request req) {
		return Response.ok().stream(personRepository.findAll(), Person.class);
	}
}
