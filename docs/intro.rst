
###################
What is PubFetcher?
###################

A Java command-line tool and library to download and store publications with metadata by combining content from various online resources (Europe PMC, PubMed, PubMed Central, Unpaywall, journal web pages), plus extract content from general web pages.


********
Overview
********

PubFetcher used to be part of `EDAMmap <https://github.com/edamontology/edammap>`_ until its functionality was determined to be potentially useful on its own, thus PubFetcher is now an independently usable application. However, its features and structure are still influenced by EDAMmap, for example the supported :ref:`publication resources <resources>` are mainly from the biomedical and life sciences fields and getting the list of authors of a publication is currently not supported (as it's not needed in EDAMmap). Also, the functionality of extracting content from :ref:`general web pages <fetching_webpages_and_docs>` is geared towards web pages containing software tools descriptions and documentation (GitHub, BioConductor, etc), as PubFetcher has built-in rules to extract from these pages and it has fields to store the :ref:`software license <license>` and :ref:`programming language <language>`.

Ideally, all scientific literature would be open and easily accessible through one interface for text mining and other purposes. One interface for getting publications is `Europe PMC <https://europepmc.org/>`_, which PubFetcher uses as its main resource. In the middle of 2018, Europe PMC was able to provide almost all of the titles, around 95% of abstracts, 50% of full texts and only 10% of user-assigned keywords for the publications present in the `bio.tools <https://bio.tools/>`_ registry at that time. While some articles don't have keywords and some full texts can't be obtained, many of the gaps can be filled by other :ref:`resources <resources>`. And sometimes we need the maximum amount of content about each publication for better results, thus the need for PubFetcher, that extracts and combines data from these different resources.

The speed of downloading, when :ref:`multithreading <multithreaded>` is enabled, is roughly one publication per second. This limitation, along with the desire to not overburden the used APIs and publisher sites, means that PubFetcher is best used for medium-scale processing of publications, where the number of entries is in the thousands and not in the millions, but where the largest amount of completeness for these few thousand publications is desired. If millions of publications are required, then it is better to restrict oneself to the Open Access subset, which can be downloaded in bulk: https://europepmc.org/downloads.

In addition to the main content of a publication (:ref:`title <fetcher_title>`, :ref:`abstract <fetcher_theabstract>` and :ref:`full text <fetcher_fulltext>`), PubFetcher supports getting different keywords about the publication: the :ref:`user-assigned keywords <fetcher_keywords>`, the :ref:`MeSH terms <fetcher_mesh>` as assigned in PubMed and :ref:`EFO terms <fetcher_efo>` and :ref:`GO terms <fetcher_go>` as mined from the full text by Europe PMC. Each publication has up to three identificators: a :ref:`PMID <fetcher_pmid>`, a :ref:`PMCID <fetcher_pmcid>` and a :ref:`DOI <fetcher_doi>`. In addition, different metadata (found from the different :ref:`resources <resources>`) about a publication is saved, like whether the article is :ref:`Open Access <oa>`, the :ref:`journal <journaltitle>` where it was published, the :ref:`publication date <pubdate>`, etc. The :ref:`source <publication_types>` of each :ref:`publication part <publication_parts>` is remembered, with content from a higher confidence resource potentially overwriting the current content. It is possible to fetch only some :ref:`publication parts <publication_parts>` (thus avoiding querying some :ref:`resources <resources>`) and there is :ref:`an algorithm <can_fetch>` to determine if an already existing entry should be refetched or is it complete enough. Fetching and :ref:`extracting <selecting>` of content is done using various Java libraries with support for :ref:`JavaScript <getting_a_html_document>` and :ref:`PDF <getting_a_pdf_document>` files. The downloaded publications can be persisted to disk to a :ref:`key-value store <database>` for later analysis. A number of :ref:`built-in rules <rules_in_yaml>` are included (along with :ref:`tests <testing_of_rules>`) for :ref:`scraping <scraping>` publication parts from publisher sites, but additional rules can also be defined. Currently, there is support for around 50 publishers of journals and 25 repositories of tools and tools' metadata and documentation and around 750 test cases for the rules have been defined.

PubFetcher has an extensive :ref:`command-line tool <cli>` to use all of its functionality. It contains a few :ref:`helper operations <simple_one_off_operations>`, but the main use is the construction of a simple :ref:`pipeline <pipeline>` for querying, fetching and outputting of publications and general and documentation web pages: first IDs of interest are specified/loaded and filtered, then corresponding content fetched/loaded and filtered, and last it is possible to output the results or store them to a database. Among other functionality, content and all the metadata can be output in :ref:`HTML or plain text <html_and_plain_text_output>`, but also :ref:`exported <export_to_json>` to :ref:`JSON <json_output>`. All fetching operations can be influenced by a few :ref:`general parameters <general_parameters>`. Progress along with error messages is logged to the console and to a :ref:`log file <log_file>`, if specified. The command-line tool can be :ref:`extended <cli_extended>`, for example to add new ways of loading IDs.


*******
Outline
*******

* :ref:`cli` documents all parameters of the command-line interface, accompanied by many examples
* :ref:`output` describes different outputs: the database, the log file and the JSON output, through which the structure of publications, webpages and docs is also explained
* :ref:`fetcher` deals with fetching logic, describing for example the content fetching methods and the resources and filling logic of publication parts
* :ref:`scraping` is about scraping rules and how to define and test them
* :ref:`api` gives a short overview about the source code for those wanting to use the PubFetcher library
* :ref:`future` contains ideas how to improve PubFetcher


**********
Quickstart
**********

.. code-block:: bash

  # Create a new empty database
  $ java -jar pubfetcher-cli-<version>.jar -db-init database.db
  # Fetch two publications and store them to the database
  $ java -jar pubfetcher-cli-<version>.jar -pub 10.1093/nar/gkz369 10.1101/692905 -db-fetch-end database.db
  # Print the fetched publications
  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db -db database.db -out

For many more examples, see :ref:`Examples <examples>`.


****
Repo
****

PubFetcher is hosted at https://github.com/edamontology/pubfetcher.


*******
Support
*******

Should you need help installing or using PubFetcher, please get in touch with Erik Jaaniso (the lead developer) directly via the `tracker <https://github.com/edamontology/pubfetcher/issues>`_.


*******
License
*******

PubFetcher is free and open-source software licensed under the GNU General Public License v3.0, as seen in `COPYING <https://github.com/edamontology/pubfetcher/blob/master/COPYING>`_.
