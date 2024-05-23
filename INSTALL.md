# INSTALL

## Compiling from latest source

[git](https://git-scm.com/), [JDK 11](https://openjdk.org/) (or later) and [Apache Maven](https://maven.apache.org/) are required.

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

## Dependencies

By default, [Selenium WebDriver](https://www.selenium.dev/documentation/webdriver/) is used to get the content of web pages that require JavaScript. Currently, only the Firefox driver is supported, meaning that Firefox needs to be installed (or the firefox executable available somewhere and pointed to with ``--seleniumFirefox /path/to/firefox``), otherwise PubFetcher will fail to start. If this not desirable, then usage of Selenium can be disabled with ``--selenium false``, in which case the included [HtmlUnit](https://htmlunit.sourceforge.io/) library will be used instead (however, this will be slower and with worse quality results).
