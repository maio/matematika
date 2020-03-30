.PHONY: build

deploy: build
	scp ./build/distributions/* unlessquit.com:/mnt/caddy/sites/maio.cz/kids/math/

build:
	./gradlew build
