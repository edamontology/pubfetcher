# INSTALL

## Compiling from latest source

[git](https://git-scm.com/), [JDK 8](https://openjdk.java.net/projects/jdk8/) (or later) and [Apache Maven](https://maven.apache.org/) are required.

Execute:

```shell
$ cd ~/foo/bar/
$ git clone https://github.com/edamontology/pubfetcher.git
$ cd pubfetcher/
$ git checkout develop
$ mvn clean install
```

PubFetcher can now be run with:

```shell
$ java -jar ~/foo/bar/pubfetcher/target/pubfetcher-cli-<version>.jar -h
```

A packaged version of PubFetcher can be found as `~/foo/bar/pubfetcher/dist/target/pubfetcher-<version>.zip`.

## Compiling latest release

Same as previous section, except `git checkout develop` must be replaced with `git checkout master`.

## Using a pre-compiled release

Pre-built releases can be found from https://github.com/edamontology/pubfetcher/releases. A downloaded release package can be unzipped in the desired location, where `pubfetcher-cli-<version>.jar` can again be run with `java -jar`.
