# PubFetcher

A Java command-line tool and library to download and store publications with metadata by combining content from various online resources (Europe PMC, PubMed, PubMed Central, Unpaywall, journal web pages), plus extract content from general web pages.

PubFetcher used to be part of [EDAMmap](https://github.com/edamontology/edammap) until its functionality was determined to be potentially useful on its own, thus PubFetcher is now an independently usable application. However its features and structure are still influenced by EDAMmap, for example the supported publication resources are mainly from the biomedical and life sciences fields and getting the list of authors of a publication is currently not supported (as it's not needed in EDAMmap). Also, the functionality of extracting content from general web pages is geared towards web pages containing software tools descriptions and documentation (GitHub, BioConductor, etc), as PubFetcher has built-in rules to extract from these pages and it has fields to store the software license and programming language.

Ideally, all scientific literature would be open and easily accessible through one interface for text mining and other purposes. One interface for getting publications is [Europe PMC](https://europepmc.org/), which PubFetcher uses as its main resource. In the middle of 2018, Europe PMC was able to provide almost all of the titles, around 95% of abstracts, 50% of full texts and only 10% of user-assigned keywords for the publications present in the [bio.tools](https://bio.tools/) registry at that time. While some articles don't have keywords and some full texts can't be obtained, many of the gaps can be filled by other resources. And sometimes we need the maximum amount of content about each publication for better results, thus the need for PubFetcher, that extracts and combines data from these different resources.

The speed of downloading, when multithreading is enabled, is roughly one publication per second. This limitation, along with the desire to not overburden the used APIs and publisher sites, means that PubFetcher is best used for medium-scale processing of publications, where the number of entries is in the thousands and not in the millions, but where the largest amount of completeness for these few thousand publications is desired. If millions of publications are required, then it is better to restrict oneself to the Open Access subset, which can be downloaded in bulk: https://europepmc.org/downloads.

In addition to the main content of a publication (title, abstract and full text), PubFetcher supports getting different keywords about the publication: the user-assigned keywords, the MeSH terms as assigned in PubMed and EFO and GO terms as mined from the full text by Europe PMC. Each publication has up to three identificators: a PMID, a PMCID and a DOI. In addition, different metadata (found from the different resources) about a publication is saved, like whether the article is Open Access, the journal where it was published, the publication date, etc. The source of each publication part is remembered, with content from a higher confidence resource potentially overwriting the current content. It is possible to fetch only some publication parts (thus avoiding querying some resources) and there is an algorithm to determine if an already existing entry should be refetched or is it complete enough. Fetching and extracting of content is done using various Java libraries with support for JavaScript and PDF files. The downloaded publications can be persisted to disk to a key-value store for later analysis. A number of built-in rules are included (along with tests) for scraping publication parts from publisher sites, but additional rules can also be defined. Currently, there is support for around 50 publishers of journals and 25 repositories of tools and tools' metadata and documentation and around 750 test cases for the rules have been defined.

PubFetcher has an extensive command-line tool to use all of its functionality. A simple pipeline can be constructed in the tool for querying, fetching and outputting of publications and general and documentation web pages: first IDs of interest are specified/loaded and filtered, then corresponding content fetched/loaded and filtered, and last it is possible to output the results or store them to a database. Among others, content and all the metadata can be output in JSON. Progress along with error messages is logged to the console and to a log file, if specified. The command-line tool can be extended, for example to add new ways of loading IDs.

Installation instructions are provided in [INSTALL](INSTALL.md).

Documentation can be found in the wiki: [PubFetcher documentation](https://github.com/edamontology/pubfetcher/wiki). [Section 1](https://github.com/edamontology/pubfetcher/wiki/cli) documents all parameters of the command-line interface, accompanied by many examples. [Section 2](https://github.com/edamontology/pubfetcher/wiki/output) describes different outputs: the database, the log file and the JSON output, through which the structure of publications, webpages and docs is also explained. [Section 3](https://github.com/edamontology/pubfetcher/wiki/fetcher) deals with fetching logic, describing for example the content fetching methods and the resources and filling logic of publication parts. [Section 4](https://github.com/edamontology/pubfetcher/wiki/scraping) is about scraping rules and how to define and test them. [Section 5](https://github.com/edamontology/pubfetcher/wiki/api) gives a short overview about the source code for those wanting to use the PubFetcher library. [Section 6](https://github.com/edamontology/pubfetcher/wiki/future) contains ideas how to improve PubFetcher.
