# INSTALL

[git](https://git-scm.com/), [JDK 8](https://openjdk.java.net/projects/jdk8/) (or later) and [Apache Maven](https://maven.apache.org/) are required.

On the command-line, go to the directory PubFetcher should be installed in and execute:

```shell
$ git clone https://github.com/edamontology/pubfetcher.git
$ cd pubfetcher/
$ mvn clean install
```

PubFetcher can now be run with:

```shell
$ java -jar /path/to/pubfetcher/target/pubfetcher-cli-0.2-SNAPSHOT.jar -h
```
