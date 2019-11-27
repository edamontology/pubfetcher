# PubFetcher

A Java command-line tool and library to download and store publications with metadata by combining content from various online resources (Europe PMC, PubMed, PubMed Central, Unpaywall, journal web pages), plus extract content from general web pages.

The main resource is [Europe PMC](https://europepmc.org/), but in case it cannot provide parts of the required content, then other repositories can be consulted. As last resort, there is support to scrape journal articles directly from publisher web sites -- around 50 site scraping rules are built in, mainly for journals in the biomedical and life sciences fields. To not overburden the used APIs and sites, PubFetcher is best used for medium-scale processing of publications, where the number of entries is in the thousands and not in the millions, but where the largest amount of completeness for these few thousand publications is desired.

In addition to the main content of publications (title, abstract, full text), PubFetcher supports different keywords: the user-assigned keywords of the article, MeSH terms from PubMed and GO/EFO terms as mined by Europe PMC. Some extra metadata is saved, like journal title, publication date, etc, however the list of authors is currently missing. Content from higher quality resources is prioritised and good enough publication parts are not re-fetched. There is support for JavaScript while scraping, and content can be extracted from PDF files. Downloaded publications can be persisted to disk to a key-value store for later analysis, or exported to JSON.

In addition to publications, PubFetcher can scrape general web pages. This functionality is geared towards web pages containing software tools descriptions and documentation (GitHub, BioConductor, etc), as PubFetcher has built-in rules (around 25) to extract from these pages and it has fields to store the software license and programming language. If no rules are defined for a given web page, then an automatic extraction of the main content of the page is attempted.

PubFetcher is used in [EDAMmap](https://github.com/edamontology/edammap) and [Pub2Tools](https://github.com/bio-tools/pub2tools).

Documentation for PubFetcher can be found at https://pubfetcher.readthedocs.io/.
