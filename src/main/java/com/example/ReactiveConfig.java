package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.reactive.config.WebReactiveConfiguration;
import org.springframework.web.reactive.function.Router;

import java.util.function.Supplier;
import java.util.stream.Stream;

@Configuration
public class ReactiveConfig extends WebReactiveConfiguration {
	@Bean
	Router.Configuration configuration() {
		return new Router.Configuration() {
			@Override
			public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
				return () -> getMessageReaders().stream();
			}

			@Override
			public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
				return () -> getMessageWriters().stream();
			}
		};
	}
}
