.PHONY: build test run seed clean

build:
	./gradlew --no-daemon test shadowJar

test:
	./gradlew --no-daemon test

run:
	docker compose up

seed:
	MEILI_HOST=http://localhost:7700 sh scripts/seed.sh

clean:
	./gradlew --no-daemon clean
