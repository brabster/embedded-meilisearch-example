.PHONY: build test run clean

build:
	./gradlew --no-daemon test shadowJar

test:
	./gradlew --no-daemon test

run:
	docker compose up --build

clean:
	./gradlew --no-daemon clean
