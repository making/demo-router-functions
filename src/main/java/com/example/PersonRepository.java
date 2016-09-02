package com.example;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class PersonRepository {
	ConcurrentMap<String, Person> map = new ConcurrentHashMap<>();

	public Mono<Person> findOne(String id) {
		return Mono.justOrEmpty(map.get(id));
	}

	public Flux<Person> findAll() {
		return Flux.fromIterable(map.values());
	}

	@PostConstruct
	void init() {
		map.put("1", new Person("a", 10));
		map.put("2", new Person("b", 12));
		map.put("3", new Person("c", 14));
		map.put("4", new Person("d", 16));
		map.put("5", new Person("e", 18));
		map.put("6", new Person("f", 20));
	}
}
