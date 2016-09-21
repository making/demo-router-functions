package com.example;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.reactive.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpServer;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.client.reactive.ClientWebRequestBuilders.*;
import static org.springframework.web.client.reactive.ResponseExtractors.*;

// TODO use TestSubscriber
public class DemoLambdaRoutingApplicationTests {
	static WebClient webClient;
	static int port;

	@BeforeClass
	public static void setup() throws Exception {
		webClient = new WebClient(new ReactorClientHttpConnector());
		port = SocketUtils.findAvailableTcpPort();
		HttpServer httpServer = HttpServer.create("0.0.0.0", port);
		httpServer.startAndAwait(new ReactorHttpHandlerAdapter(
				DemoLambdaRoutingApplication.httpHandler()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shut down ...");
			httpServer.shutdown();
		}));
	}

	@Test
	public void root() {
		Mono<ResponseEntity<String>> result = webClient
				.perform(get("http://localhost:" + port)).extract(response(String.class));

		assertThat(result.block().getBody()).isEqualTo("Sample");
		assertThat(result.block().getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void hello() {
		Mono<ResponseEntity<String>> result = webClient
				.perform(get("http://localhost:" + port + "/hello"))
				.extract(response(String.class));

		assertThat(result.block().getBody()).isEqualTo("Hello World!");
		assertThat(result.block().getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void bar() {
		Mono<String> result1 = webClient.perform(get("http://localhost:" + port + "/bar"))
				.extract(body(String.class));
		Mono<String> result2 = webClient
				.perform(get("http://localhost:" + port + "/bar?foo=abc"))
				.extract(body(String.class));

		assertThat(result1.block()).isEqualTo("query[foo] = ???");
		assertThat(result2.block()).isEqualTo("query[foo] = abc");
	}

	@Test
	public void json() {
		Mono<ResponseEntity<Person>> result = webClient
				.perform(get("http://localhost:" + port + "/json"))
				.extract(response(Person.class));

		assertThat(result.block().getBody()).isEqualTo(new Person("John", 30));
		assertThat(result.block().getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.block().getHeaders().getContentType())
				.isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	public void reactive() {
		Flux<String> result = webClient
				.perform(get("http://localhost:" + port + "/reactive"))
				.extract(bodyStream(String.class));

		Iterable<String> iterable = result.toIterable();
		Iterator<String> iterator = iterable.iterator();
		assertThat(iterator.next()).isEqualTo("Hello");
		assertThat(iterator.next()).isEqualTo("World");
	}

	@Test
	public void echo() {
		Flux<String> result = webClient
				.perform(post("http://localhost:" + port + "/echo").body("abc"))
				.extract(bodyStream(String.class));

		Iterable<String> iterable = result.toIterable();
		Iterator<String> iterator = iterable.iterator();
		assertThat(iterator.next()).isEqualTo("Hi ");
		assertThat(iterator.next()).isEqualTo("abc");
	}

	@Test
	public void postJson() {
		Mono<ResponseEntity<Person>> result = webClient.perform(
				post("http://localhost:" + port + "/json").body(new Person("Josh", 20)))
				.extract(response(Person.class));

		assertThat(result.block().getBody()).isEqualTo(new Person("Josh", 20));
		assertThat(result.block().getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.block().getHeaders().getContentType())
				.isEqualTo(MediaType.APPLICATION_JSON_UTF8);
	}

	@Test
	public void sse() throws Exception {
		Flux<String> result = webClient.perform(get("http://localhost:" + port + "/sse"))
				.extract(bodyStream(String.class));
		Iterable<String> iterable = result.toIterable();
		Iterator<String> iterator = iterable.iterator();
		assertThat(iterator.next()).isEqualTo("id:0\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("0");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("id:1\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("1");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("id:2\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("2");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("id:3\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("3");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("id:4\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("4");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("id:5\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("5");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("id:6\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("6");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("id:7\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("7");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("id:8\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("8");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("id:9\n" + ":foo\n" + "data:");
		assertThat(iterator.next()).isEqualTo("9");
		assertThat(iterator.next()).isEqualTo("\n");
		assertThat(iterator.next()).isEqualTo("\n");
	}

	@Test
	public void person() {
		Mono<ResponseEntity<Person>> result = webClient
				.perform(get("http://localhost:" + port + "/person/1"))
				.extract(response(Person.class));

		assertThat(result.block().getBody()).isEqualTo(new Person("P1", 10));
		assertThat(result.block().getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.block().getHeaders().getContentType())
				.isEqualTo(MediaType.APPLICATION_JSON_UTF8);
	}

	@Test
	public void people() {
		Flux<Person> result = webClient
				.perform(get("http://localhost:" + port + "/person"))
				.extract(bodyStream(Person.class));

		Iterable<Person> iterable = result.toIterable();
		Iterator<Person> iterator = iterable.iterator();
		assertThat(iterator.next()).isEqualTo(new Person("p1", 11));
		assertThat(iterator.next()).isEqualTo(new Person("p2", 12));
	}
}
