package com.example;

import java.io.Serializable;

public class Person implements Serializable {
	private final String name;
	private final int age;

	public Person(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public int getAge() {
		return age;
	}
}
