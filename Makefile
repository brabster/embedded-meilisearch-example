IMAGE_NAME ?= immutable-search-demo

.PHONY: test build docker-build docker-run

test:
	gradle --no-daemon test

build: test
	gradle --no-daemon shadowJar

docker-build:
	docker build -t $(IMAGE_NAME) .

docker-run:
	docker run --rm -p 8080:8080 $(IMAGE_NAME)
