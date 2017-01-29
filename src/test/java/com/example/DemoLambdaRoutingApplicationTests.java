package com.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

import java.util.Iterator;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.SocketUtils;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientOperations;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

// TODO use TestSubscriber
public class DemoLambdaRoutingApplicationTests {
	static WebClientOperations operations;
	static int port;
	static String host;

	@BeforeClass
	public static void setup() throws Exception {

		port = SocketUtils.findAvailableTcpPort();
		host = "localhost";
		// port = 80;
		// host = "demo-router-functions.cfapps.io";

		if ("localhost".equals(host)) {
			HttpServer httpServer = HttpServer.create("0.0.0.0", port);
			Mono<? extends NettyContext> handler = httpServer
					.newHandler(new ReactorHttpHandlerAdapter(
							DemoLambdaRoutingApplication.httpHandler()));
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shut down ...");
			}));
			handler.block();
		}

		WebClient webClient = WebClient.builder(new ReactorClientHttpConnector()).build();
		operations = WebClientOperations.builder(webClient).uriBuilderFactory(
				new DefaultUriBuilderFactory(String.format("http://%s:%d", host, port)))
				.build();
	}

	@Test
	public void root() {
		Mono<ClientResponse> result = operations.get().uri("").exchange();

		assertThat(result.block().bodyToMono(String.class).block()).isEqualTo("Sample");
		assertThat(result.block().statusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void hello() {
		Mono<ClientResponse> result = operations.get().uri("hello").exchange();

		assertThat(result.block().bodyToMono(String.class).block())
				.isEqualTo("Hello World!");
		assertThat(result.block().statusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void bar() {
		Mono<ClientResponse> result1 = operations.get().uri("bar").exchange();
		Mono<ClientResponse> result2 = operations.get()
				.uri(b -> b.uriString("bar").queryParam("foo", "abc").build()).exchange();

		assertThat(result1.block().bodyToMono(String.class).block())
				.isEqualTo("query[foo] = ???");
		assertThat(result2.block().bodyToMono(String.class).block())
				.isEqualTo("query[foo] = abc");
	}

	@Test
	public void json() {
		Mono<ClientResponse> result = operations.get().uri("json")
				.accept(MediaType.APPLICATION_JSON).exchange();

		assertThat(result.block().bodyToMono(Person.class).block())
				.isEqualTo(new Person("John", 30));
		assertThat(result.block().statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.block().headers().contentType().get())
				.isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	public void reactive() {
		Flux<String> result = operations.get().uri("reactive").exchange()
				.flatMap(res -> res.bodyToFlux(String.class));

		Iterable<String> iterable = result.toIterable();
		Iterator<String> iterator = iterable.iterator();
		assertThat(result.toStream().collect(Collectors.joining()))
				.isEqualTo("HelloWorld");
	}

	@Test
	public void echo() {
		Flux<String> result = operations.post().uri("echo").exchange(fromObject("abc"))
				.flatMap(res -> res.bodyToFlux(String.class));
		Iterable<String> iterable = result.toIterable();
		Iterator<String> iterator = iterable.iterator();
		assertThat(iterator.next()).isEqualTo("Hi ");
		assertThat(iterator.next()).isEqualTo("abc");
	}

	@Test
	public void postJson() {
		Mono<ClientResponse> result = operations.post().uri("json")
				.exchange(fromObject(new Person("Josh", 20)));

		assertThat(result.block().bodyToMono(Person.class).block())
				.isEqualTo(new Person("Josh", 20));
		assertThat(result.block().statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.block().headers().contentType().get())
				.isEqualTo(MediaType.APPLICATION_JSON_UTF8);
	}

	@Test
	public void sse() throws Exception {
		Flux<ServerSentEvent<String>> result = operations.get().uri("sse").exchange()
				.flatMap(res -> res.body(BodyExtractors.toFlux(ResolvableType
						.forClassWithGenerics(ServerSentEvent.class, String.class))));

		Iterable<ServerSentEvent<String>> iterable = result
				.filter(x -> x.id().isPresent() /* TODO ?? */).toIterable();
		Iterator<ServerSentEvent<String>> iterator = iterable.iterator();
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("0").id("0").comment("foo").build().toString());
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("1").id("1").comment("foo").build().toString());
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("2").id("2").comment("foo").build().toString());
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("3").id("3").comment("foo").build().toString());
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("4").id("4").comment("foo").build().toString());
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("5").id("5").comment("foo").build().toString());
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("6").id("6").comment("foo").build().toString());
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("7").id("7").comment("foo").build().toString());
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("8").id("8").comment("foo").build().toString());
		assertThat(iterator.next().toString()).isEqualTo(
				ServerSentEvent.builder("9").id("9").comment("foo").build().toString());
	}

	@Test
	public void person() {
		Mono<ClientResponse> result = operations.get().uri("person/1").exchange();

		assertThat(result.block().bodyToMono(Person.class).block())
				.isEqualTo(new Person("P1", 10));
		assertThat(result.block().statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.block().headers().contentType().get())
				.isEqualTo(MediaType.APPLICATION_JSON_UTF8);
	}

	@Test
	public void people() {
		Flux<Person> result = operations.get().uri("person").exchange()
				.flatMap(res -> res.bodyToFlux(Person.class));

		Iterable<Person> iterable = result.toIterable();
		Iterator<Person> iterator = iterable.iterator();
		assertThat(iterator.next()).isEqualTo(new Person("p1", 11));
		assertThat(iterator.next()).isEqualTo(new Person("p2", 12));
	}
}
