
.. _cli:

#############################
Command-line interface manual
#############################

The CLI of PubFetcher is provided by a Java executable packaged in a .jar file. If a Java Runtime Environment (JRE) capable of running version 8 of Java is installed on the system, then this .jar file can be executed using the ``java`` command. For example, executing PubFetcher-CLI with the parameter ``-h`` or ``--help`` outputs a list of all possible parameters:

.. code-block:: bash

  $ java -jar path/to/pubfetcher-cli-<version>.jar --help

Parsing of command line parameters is provided by `JCommander <http://jcommander.org/>`_.

.. _logging:

*******
Logging
*******

===================  ===========
Parameter            Description
===================  ===========
``-l`` or ``--log``  The path of the log file
===================  ===========

PubFetcher-CLI will output its log to the console (to stderr). With the ``--log`` parameter we can specify a text file location where this same log will be output. It will not be coloured as the console output, but will include a few DEBUG level messages omitted in the console (this includes the very first line listing all parameters the program was run with).

If the specified file already exists, then new log messages will be appended to its end. In case of a new log file creation, any missing parent directories will be created as necessary.

.. _general_parameters:

******************
General parameters
******************

Parameters affecting many other operations specified below. These parameters can be supplied to PubFetcher externally through programmatic means. When supplied on the command line (of PubFetcher-CLI), then two dashes (``--``) have to be added in front of the parameter names specified in the two following tables.

.. _fetching:

Fetching
========

Parameters that affect many of the operations specified further below, for example ``--timeout`` changes the timeouts of all attempted network connections. The cooldown and retryLimit_ parameters affect if we :ref:`can fetch <can_fetch>` (or rather, refetch) a :ref:`publication <content_of_publications>` or :ref:`webpage <content_of_webpages>`. The minimum length and size parameters affect whether an entry is usable and final.

=============================  =========  =====  ===========
Parameter                      Default    Min    Description
=============================  =========  =====  ===========
_`emptyCooldown`               ``720``           If that many minutes have passed since last fetching attempt of an :ref:`empty publication <publication_empty>` or :ref:`empty webpage <webpage_empty>`, then fetching can be attempted again, resetting the :ref:`retryCounter <retrycounter>`. Setting to ``0`` means fetching of empty :ref:`database <database>` entries will always be attempted again. Setting to a negative value means refetching will never be done (and retryCounter never reset) only because the entry is empty.
_`nonFinalCooldown`            ``10080``         If that many minutes have passed since last fetching attempt of a non-:ref:`final publication <publication_final>` or non-:ref:`final webpage <webpage_final>` (which are not empty), then fetching can be attempted again, resetting the :ref:`retryCounter <retrycounter>`. Setting to ``0`` means fetching of non-final database entries will always be attempted again. Setting to a negative value means refetching will never be done (and retryCounter never reset) only because the entry is non-final.
_`fetchExceptionCooldown`      ``1440``          If that many minutes have passed since last fetching attempt of a :ref:`publication <content_of_publications>` or :ref:`webpage <content_of_webpages>` with a :ref:`fetchException <fetchexception>`, then fetching can be attempted again, resetting the :ref:`retryCounter <retrycounter>`. Setting to ``0`` means fetching of database entries with fetchException will always be attempted again. Setting to a negative value means refetching will never be done (and retryCounter never reset) only because the fetchException of the entry is ``true``.
_`retryLimit`                  ``3``             How many times can fetching be retried for an entry that is still empty, non-final or has a :ref:`fetchException <fetchexception>` after the initial attempt. Setting to ``0`` will disable retrying, unless the :ref:`retryCounter <retrycounter>` is reset by a cooldown in which case one initial attempt is allowed again. Setting to a negative value will disable this upper limit.
_`titleMinLength`              ``4``      ``0``  Minimum length of a :ref:`usable <usable>` :ref:`publication <content_of_publications>` :ref:`title <fetcher_title>`
_`keywordsMinSize`             ``2``      ``0``  Minimum size of a :ref:`usable <usable>` :ref:`publication <content_of_publications>` :ref:`keywords <fetcher_keywords>`/:ref:`MeSH <fetcher_mesh>` list
_`minedTermsMinSize`           ``1``      ``0``  Minimum size of a :ref:`usable <usable>` :ref:`publication <content_of_publications>` :ref:`EFO <efo>`/:ref:`GO <go>` terms list
_`abstractMinLength`           ``200``    ``0``  Minimum length of a :ref:`usable <usable>` :ref:`publication <content_of_publications>` :ref:`abstract <fetcher_theabstract>`
_`fulltextMinLength`           ``2000``   ``0``  Minimum length of a :ref:`usable <usable>` :ref:`publication <content_of_publications>` :ref:`fulltext <fetcher_fulltext>`
_`webpageMinLength`            ``50``     ``0``  Minimum length of a :ref:`usable webpage <webpage_usable>` combined :ref:`title <webpage_title>` and :ref:`content <webpage_content>`
_`webpageMinLengthJavascript`  ``200``    ``0``  If the length of a the whole web page text fetched without JavaScript is below the specified limit and no :ref:`scraping rules <scraping>` are found for the corresponding URL, then refetching using JavaScript support will be attempted
_`timeout`                     ``15000``  ``0``  Connect and read timeout of connections, in milliseconds
=============================  =========  =====  ===========

.. _fetching_private:

Fetching private
================

These are like Fetching_ parameters in that they have a general effect, e.g. setting ``--userAgent`` changes the HTTP User-Agent of all HTTP connections. However, Fetching_ parameters are such parameters that we might want to expose via a web API to be changeable by a client (when extending or using the PubFetcher library), but the parameters below should probably only be configured locally and as such are separated in code.

=================  ===========
Parameter          Description
=================  ===========
_`europepmcEmail`  E-mail to send to the :ref:`Europe PMC <europe_pmc>` API
_`oadoiEmail`      E-mail to send to the oaDOI (:ref:`Unpaywall <unpaywall>`) API
_`userAgent`       HTTP User-Agent
_`journalsYaml`    YAML file containing custom :ref:`journals scrape rules <journals_yaml>` to add to default ones
_`webpagesYaml`    YAML file containing custom :ref:`webpages scrape rules <webpages_yaml>` to add to default ones
=================  ===========

.. _simple_one_off_operations:

*************************
Simple one-off operations
*************************

Some simple operations (represented by the parameters with one dash (``-``) below), that mostly should by the sole parameter supplied to PubFetcher, when used.

.. _cli_database:

Database
========

A collection of one-off database operations on a single :ref:`database <database>` file.

================================  =================  ===========
Parameter                         Parameter args     Description
================================  =================  ===========
``-db-init``                      *<database file>*  Create an empty database file. This is the only way to make new databases.
``-db-commit``                    *<database file>*  Commit all pending changes by merging all WAL files to the main database file. This has only an effect if WAL files are present beside the database file after an abrupt termination of the program, as normally committing is done in code where required.
``-db-compact``                   *<database file>*  Compaction reclaims space by removing deprecated records (left over after database updates)
``-db-publications-size``         *<database file>*  Output the number of :ref:`publications <publications>` stored in the database to stdout
``-db-webpages-size``             *<database file>*  Output the number of :ref:`webpages <webpages>` stored in the database to stdout
``-db-docs-size``                 *<database file>*  Output the number of :ref:`docs <docs>` stored in the database to stdout
``-db-publications-map``          *<database file>*  Output all :ref:`PMID <id_pmid>` to primary ID, :ref:`PMCID <id_pmcid>` to primary ID and :ref:`DOI <id_doi>` to primary ID mapping pairs stored in the database to stdout
``-db-publications-map-reverse``  *<database file>*  Output all mappings from primary ID to the triple [:ref:`PMID <id_pmid>`, :ref:`PMCID <id_pmcid>`, :ref:`DOI <id_doi>`] stored in the database to stdout
================================  =================  ===========

.. _print_a_web_page:

Print a web page
================

Methods for fetching and outputting a web page. Affected by timeout_ and userAgent_ parameters, ``-fetch-webpage-selector`` also by webpageMinLength_ and webpageMinLengthJavascript_.

==============================  =================================================================  ===========
Parameter                       Parameter args                                                     Description
==============================  =================================================================  ===========
``-fetch-document``             *<url>*                                                            Fetch a web page (without JavaScript support, i.e. using jsoup) and output its raw HTML to stdout
``-fetch-document-javascript``  *<url>*                                                            Fetch a web page (with JavaScript support, i.e. using HtmlUnit) and output its raw HTML to stdout
``-post-document``              *<url> <param name> <param value> <param name> <param value> ...*  Fetch a web resource using HTTP POST. The first parameter specifies the resource URL and is followed by the request data in the form of name/value pairs, with names and values separated by spaces.
``-fetch-webpage-selector``     *<url> <title selector> <content selector> <javascript support>*   Fetch a :ref:`webpage <content_of_webpages>` and output it to stdout in the format specified by the `Output modifiers`_ ``--plain`` and ``--format``. Works also for PDF files. *Title* and *content* args are CSS selectors as supported by jsoup. If the *title selector* is an empty string, then the :ref:`page title <webpage_title>` will be the text content of the document's ``<title>`` element. If the *content selector* is an empty string, then :ref:`content <webpage_content>` will be the :ref:`automatically cleaned <cleaning>` whole text content parsed from the HTML/XML. If javascript arg is ``true``, then fetching will be done using JavaScript support (HtmlUnit), if ``false``, then without JavaScript (jsoup). If javascript arg is empty, then fetching will be done without JavaScript and if the text length of the returned document is less than webpageMinLengthJavascript_ or if a ``<noscript>`` tag is found in it, a second fetch will happen with JavaScript support.
==============================  =================================================================  ===========

.. _scrape_rules:

Scrape rules
============

Print requested parts of currently effective :ref:`scraping rules <scraping>` loaded from default or custom scrape rules :ref:`YAML files <rules_in_yaml>`.

======================  =======================  ===========
Parameter               Parameter args           Description
======================  =======================  ===========
``-scrape-site``        *<url>*                  Output found journal site name for the given URL to stdout (or ``null`` if not found or URL invalid)
``-scrape-selector``    *<url> <ScrapeSiteKey>*  Output the CSS selector used for extracting the :ref:`publication part <publication_parts>` represented by :ref:`ScrapeSiteKey <scrapesitekey>` from the given URL
``-scrape-javascript``  *<url>*                  Output ``true`` or ``false`` depending on whether JavaScript will be used or not for fetching the given publication URL
``-scrape-webpage``     *<url>*                  Output all CSS selectors used for extracting webpage content and metadata from the given URL (or ``null`` if not found or URL invalid)
======================  =======================  ===========

.. _publication_ids:

Publication IDs
===============

Simple operations on :ref:`publication IDs <ids_of_publications>`, with result output to stdout.

===========================  ==============  ===========
Parameter                    Parameter args  Description
===========================  ==============  ===========
``-is-pmid``                 *<string>*      Output ``true`` or ``false`` depending on whether the given string is a valid :ref:`PMID <id_pmid>` or not
``-is-pmcid``                *<string>*      Output ``true`` or ``false`` depending on whether the given string is a valid :ref:`PMCID <id_pmcid>` or not
``-extract-pmcid``           *<pmcid>*       Remove the prefix "PMC" from a :ref:`PMCID <id_pmcid>` and output the rest. Output an empty string if the given string is not a valid PMCID.
``-is-doi``                  *<string>*      Output ``true`` or ``false`` depending on whether the given string is a valid :ref:`DOI <id_doi>` or not
``-normalise-doi``           *<doi>*         Remove any valid prefix (e.g. "https://doi.org/", "doi:") from a :ref:`DOI <id_doi>` and output the rest, converting letters from the 7-bit ASCII set to uppercase. The validity of the input DOI is not checked.
``-extract-doi-registrant``  *<doi>*         Output the registrant ID of a :ref:`DOI <id_doi>` (the substring after "10." and before "/"). Output an empty string if the given string is not a valid DOI.
===========================  ==============  ===========

.. _miscellaneous:

Miscellaneous
=============

Methods to test the escaping of HTML entities as done by PubFetcher (necessary when outputting raw input to HTML format) and test the validity of :ref:`publication IDs <ids_of_publications>` and :ref:`webpage URLs <urls_of_webpages>`.

==========================  ======================  ===========
Parameter                   Parameter args          Description
==========================  ======================  ===========
``-escape-html``            *<string>*              Output the result of escaping necessary characters in the given string such that it can safely by used as text in a HTML document (without the string interacting with the document's markup)
``-escape-html-attribute``  *<string>*              Output the result of escaping necessary characters in the given string such that it can safely by used as an HTML attribute value (without the string interacting with the document's markup)
``-check-publication-id``   *<string>*              Given one publication ID, output it in publication IDs form (``<pmid>\t<pmcid>\t<doi>``) if it is a valid :ref:`PMID <id_pmid>`, :ref:`PMCID <id_pmcid>` or :ref:`DOI <id_doi>`, or throw an exception if it is an invalid publication ID
``-check-publication-ids``  *<pmid> <pmcid> <doi>*  Given a :ref:`PMID <id_pmid>`, a :ref:`PMCID <id_pmcid>` and a :ref:`DOI <id_doi>`, output them in publication IDs form (``<pmid>\t<pmcid>\t<doi>``) if given IDs are a valid PMID, PMCID and DOI, or throw an exception if at least one is invalid
``-check-url``              *<string>*              Given a webpage ID (i.e. a URL), output the parsed URL, or throw an exception if it is an invalid URL
==========================  ======================  ===========

.. _pipeline:

**********************
Pipeline of operations
**********************

**A simple pipeline that allows for more complex querying, fetching and outputting of** :ref:`publications <publications>` **,** :ref:`webpages <webpages>` **and** :ref:`docs <docs>` **: first IDs of interest are specified/loaded and filtered, then corresponding content fetched/loaded and filtered, and last it is possible to output or store the results.** Component operations of the pipeline are specified as command-line parameters with one dash (``-``). In addition, there are some parameters modifying some aspect of the pipeline, these will have two dashes (``--``). The Fetching_ and `Fetching private`_ parameters will also have an effect (on fetching and determining the finality of content).

Add IDs
=======

:ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` can be specified on the command-line and can be loaded from text and :ref:`database <database>` files. The resultant list of IDs is actually a set, meaning that if duplicate IDs are encountered, they'll be ignored and not added to the list.

=============  =======================  ===========
Parameter      Parameter args           Description
=============  =======================  ===========
``-pub``       *<string> <string> ...*  A space-separated list of :ref:`publication IDs <ids_of_publications>` (either :ref:`PMID <id_pmid>`, :ref:`PMCID <id_pmcid>` or :ref:`DOI <id_doi>`) to add
``-web``       *<string> <string> ...*  A space-separated list of :ref:`webpage URLs <urls_of_webpages>` to add
``-doc``       *<string> <string> ...*  A space-separated list of :ref:`doc URLs <urls_of_docs>` to add
``-pub-file``  *<text file> ...*        Load all :ref:`publication IDs <ids_of_publications>` from the specified list of text files containing publication IDs in the form ``<pmid>\t<pmcid>\t<doi>``, one per line. Empty lines and lines beginning with ``#`` are ignored.
``-web-file``  *<text file> ...*        Load all :ref:`webpage URLs <urls_of_webpages>` from the specified list of text files containing webpage URLs, one per line. Empty lines and lines beginning with ``#`` are ignored.
``-doc-file``  *<text file> ...*        Load all :ref:`doc URLs <urls_of_docs>` from the specified list of text files containing doc URLs, one per line. Empty lines and lines beginning with ``#`` are ignored.
``-pub-db``    *<database file> ...*    Load all :ref:`publication IDs <ids_of_publications>` found in the specified :ref:`database <database>` files
``-web-db``    *<database file> ...*    Load all :ref:`webpage URLs <urls_of_webpages>` found in the specified :ref:`database <database>` files
``-doc-db``    *<database file> ...*    Load all :ref:`doc URLs <urls_of_docs>` found in the specified :ref:`database <database>` files
=============  =======================  ===========

Filter IDs
==========

Conditions that :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` must meet to be retained in the pipeline_.

=======================  =======================  ===========
Parameter                Parameter args           Description
=======================  =======================  ===========
``-has-pmid``                                     Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMID <id_pmid>` is present
``-not-has-pmid``                                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMID <id_pmid>` is empty
``-pmid``                <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMID <id_pmid>` has a match with the given regular expression
``-not-pmid``            <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMID <id_pmid>` does not have a match with the given regular expression
``-pmid-url``            <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMID provenance URL <pmidurl>` has a match with the given regular expression
``-not-pmid-url``        <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMID provenance URL <pmidurl>` does not have a match with the given regular expression
``-has-pmcid``                                    Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMCID <id_pmcid>` is present
``-not-has-pmcid``                                Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMCID <id_pmcid>` is empty
``-pmcid``               <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMCID <id_pmcid>` has a match with the given regular expression
``-not-pmcid``           <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMCID <id_pmcid>` does not have a match with the given regular expression
``-pmcid-url``           <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMCID provenance URL <pmcidurl>` has a match with the given regular expression
``-not-pmcid-url``       <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`PMCID provenance URL <pmcidurl>` does not have a match with the given regular expression
``-has-doi``                                      Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`DOI <id_doi>` is present
``-not-has-doi``                                  Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`DOI <id_doi>` is empty
``-doi``                 <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`DOI <id_doi>` has a match with the given regular expression
``-not-doi``             <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`DOI <id_doi>` does not have a match with the given regular expression
``-doi-url``             <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`DOI provenance URL <doiurl>` has a match with the given regular expression
``-not-doi-url``         <regex_>                 Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`DOI provenance URL <doiurl>` does not have a match with the given regular expression
``-doi-registrant``      *<string> <string> ...*  Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`DOI <id_doi>` registrant code (the bit after "10." and before "/") is present in the given list of strings
``-not-doi-registrant``  *<string> <string> ...*  Only keep :ref:`publication IDs <ids_of_publications>` whose :ref:`DOI <id_doi>` registrant code (the bit after "10." and before "/") is not present in the given list of strings
``-url``                 <regex_>                 Only keep :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` that have a match with the given regular expression
``-not-url``             <regex_>                 Only keep :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` that don't have a match with the given regular expression
``-url-host``            *<string> <string> ...*  Only keep :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` whose host part is present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-not-url-host``        *<string> <string> ...*  Only keep :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` whose host part is not present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-in-db``               *<database file>*        Only keep :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` that are present in the given :ref:`database <database>` file
``-not-in-db``           *<database file>*        Only keep :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` that are not present in the given :ref:`database <database>` file
=======================  =======================  ===========

Sort IDs
========

Sorting of added and filtered IDs. :ref:`publication IDs <ids_of_publications>` are first sorted by :ref:`PMID <id_pmid>`, then by :ref:`PMCID <id_pmcid>` (if PMID is absent), then by :ref:`DOI <id_doi>` (if PMID and PMCID are absent). Internally, the PMID, the PMCID and the DOI registrant are sorted numerically, DOIs within the same registrant alphabetically. :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` are sorted alphabetically.

=============  ==============  ===========
Parameter      Parameter args  Description
=============  ==============  ===========
``-asc-ids``                   Sort :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` in ascending order
``-desc-ids``                  Sort :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` is descending order
=============  ==============  ===========

Limit IDs
=========

Added, filtered and sorted IDs can be limited to a given number of IDs either in the front or back.

=============  ====================  ===========
Parameter      Parameter args        Description
=============  ====================  ===========
``-head-ids``  *<positive integer>*  Only keep the first given number of :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>`
``-tail-ids``  *<positive integer>*  Only keep the last given number of :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>`
=============  ====================  ===========

Remove from database by IDs
===========================

The resulting list of IDs can be used to remove corresponding entries from a :ref:`database <database>`.

===============  =================  ===========
Parameter        Parameter args     Description
===============  =================  ===========
``-remove-ids``  *<database file>*  From the given :ref:`database <database>`, remove content corresponding to :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>`
===============  =================  ===========

Output IDs
==========

Outputs the final list of loaded IDs to stdout or the specified text files in the format specified by the `Output modifiers`_ ``--plain`` and ``--format``. Without ``--plain`` :ref:`publication IDs <ids_of_publications>` are output with their corresponding provenance URLs, with ``--plain`` these are omitted. :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` are not affected by ``--plain``. Specifying ``--format`` as ``text`` (the default) and using ``--plain`` will output :ref:`publication IDs <ids_of_publications>` in the form ``<pmid>\t<pmcid>\t<doi>``.

================  ==============  ===========
Parameter         Parameter args  Description
================  ==============  ===========
``-out-ids``                      Output :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` to stdout in the format specified by the `Output modifiers`_ ``--plain`` and ``--format``
``-txt-ids-pub``  *<file>*        Output :ref:`publication IDs <ids_of_publications>` to the given file in the format specified by the `Output modifiers`_ ``--plain`` and ``--format``
``-txt-ids-web``  *<file>*        Output :ref:`webpage URLs <urls_of_webpages>` to the given file in the format specified by ``--format``
``-txt-ids-doc``  *<file>*        Output :ref:`doc URLs <urls_of_docs>` to the given file in the format specified by ``--format``
``-count-ids``                    Output count numbers for :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>` to stdout
================  ==============  ===========

Get content
===========

Operations to get :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` corresponding to the final list of loaded :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>`. Content will be fetched from the Internet, loaded from a :ref:`database <database>` file, or both, with updated content possibly saved back to the database. In case multiple content getting operations are used, first everything with ``-db`` is got, then ``-fetch``, ``-fetch-put``, ``-db-fetch`` and last ``-db-fetch-end``. The list of entries will have the order in which entries were got, duplicates are allowed. When saved to a database file, duplicates will be merged, in other cases (e.g. when outputting content) duplicates will be present.

=================  =================  ===========
Parameter          Parameter args     Description
=================  =================  ===========
``-db``            *<database file>*  Get :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` from the given :ref:`database <database>`
``-fetch``                            Fetch :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` from the Internet. All entries for which some :ref:`fetchException <fetchexception>` happens are fetched again in the end (this is done only once).
``-fetch-put``     *<database file>*  Fetch :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` from the Internet and put each entry in the given :ref:`database <database>` right after it has been fetched, ignoring any filters and overwriting any existing entries with equal IDs/URLs. All entries for which some :ref:`fetchException <fetchexception>` happens are fetched and put to the database again in the end (this is done only once).
``-db-fetch``      *<database file>*  First, get an entry from the given :ref:`database <database>` (if found), then fetch the entry (if the entry :ref:`can be fetched <can_fetch>`), then put the entry back to the database while ignoring any filters (if the entry was updated). All entries which have the :ref:`fetchException <fetchexception>` set are got again in the end (this is done only once). This operation is multithreaded (in contrast to ``-fetch`` and ``-fetch-put``), with ``--threads`` number of threads, thus it should be preferred for larger amounts of content.
``-db-fetch-end``  *<database file>*  Like ``-db-fetch``, except no content is kept in memory (saving back to the given :ref:`database <database>` still happens), thus no further processing down the pipeline_ is possible. This is useful for avoiding large memory usage if only fetching and saving of content to the database is to be done and no further operations on content (like outputting it) are required.
=================  =================  ===========

.. _get_content_modifiers:

Get content modifiers
=====================

Some parameters to influence the behaviour of content getting operations.

====================  ====================================================  =======  ===========
Parameter             Parameter args                                        Default  Description
====================  ====================================================  =======  ===========
``--fetch-part``      <:ref:`PublicationPartName <publication_parts>`> ...           List of publication parts that will be fetched from the Internet. All other parts will be :ref:`empty <empty>` (except the publication IDs which will be filled whenever possible). Fetching of :ref:`resources <resources>` not containing any specified parts will be skipped. If used, then ``--not-fetch-part`` must not be used. If neither of ``--fetch-part`` and ``--not-fetch-part`` is used, then all parts will be fetched.
``--not-fetch-part``  <:ref:`PublicationPartName <publication_parts>`> ...           List of publication parts that will not be fetched from the Internet. All other parts will be fetched. Fetching of :ref:`resources <resources>` not containing any not specified parts will be skipped. If used, then ``--fetch-part`` must not be used.
``--pre-filter``                                                                     Normally, all content is loaded into memory before filtering specified in `Filter content`_ is applied. This option ties the filtering step to the loading/fetching step for each individual entry, discarding entries not passing the filter right away, thus reducing memory usage. As a tradeoff, in case multiple filters are used, it won't be possible to see in the log how many entries were discarded by each filter.
``--limit``           *<positive integer>*                                  ``0``    Maximum number of :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` that can be loaded/fetched. In case the limit is applied, the concrete returned content depends on the order it is loaded/fetched, which depends on the order of content getting operations, then on whether there was a :ref:`fetchException <fetchexception>` and last on the ordering of received IDs. If the multithreaded ``-db-fetch`` is used or a fetchException happen, then the concrete returned content can vary slightly between equal applications of limit. If ``--pre-filter`` is also used, then the filters of `Filter content`_ will be applied before the limit, otherwise the limit is applied beforehand and the filters can reduce the number of entries further. Set to ``0`` to disable.
``--threads``         *<positive integer>*                                  ``8``    Number of threads used for getting content with ``-db-fetch`` and ``-db-fetch-end``. Should not be bound by actual processor core count, as mostly threads sit idle, waiting for an answer from a remote host or waiting behind another thread to finish communicating with the same host.
====================  ====================================================  =======  ===========

Filter content
==============

Conditions that :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` must meet to be retained in the pipeline_. All filters will be ANDed together.

========================  ========================  ===========
Parameter                 Parameter args            Description
========================  ========================  ===========
``-fetch-time-more``      <`ISO-8601 time`_>        Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`fetchTime <fetchtime>` is more than or equal to the given time
``-fetch-time-less``      <`ISO-8601 time`_>        Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`fetchTime <fetchtime>` is less than or equal to the given time
``-retry-counter``        *<positive integer> ...*  Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`retryCounter <retrycounter>` is equal to one of given counts
``-not-retry-counter``    *<positive integer> ...*  Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`retryCounter <retrycounter>` is not equal to any of given counts
``-retry-counter-more``   *<positive integer>*      Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`retryCounter <retrycounter>` is more than the given count
``-retry-counter-less``   *<positive integer>*      Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`retryCounter <retrycounter>` is less than the given count
``-fetch-exception``                                Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` with a :ref:`fetchException <fetchexception>`
``-not-fetch-exception``                            Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` without a :ref:`fetchException <fetchexception>`
``-empty``                                          Only keep :ref:`empty publication <publication_empty>`\ s, :ref:`empty webpage <webpage_empty>`\ s and empty docs
``-not-empty``                                      Only keep non-:ref:`empty publication <publication_empty>`\ s, non-:ref:`empty webpage <webpage_empty>`\ s and non-empty docs
``-usable``                                         Only keep :ref:`usable publication <publication_usable>`\ s, :ref:`usable webpage <webpage_usable>`\ s and usable docs
``-not-usable``                                     Only keep non-:ref:`usable publication <publication_usable>`\ s, non-:ref:`usable webpage <webpage_usable>`\ s and non-usable docs
``-final``                                          Only keep :ref:`final publication <publication_final>`\ s, :ref:`final webpage <webpage_final>`\ s and final docs
``-not-final``                                      Only keep non-:ref:`final publication <publication_final>`\ s, non-:ref:`final webpage <webpage_final>`\ s and non-final docs
``-grep``                 <regex_>                  Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` whose whole content (as output using ``--plain``) has a match with the given regular expression
``-not-grep``             <regex_>                  Only keep :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` whose whole content (as output using ``--plain``) does not have a match with the given regular expression
========================  ========================  ===========

Filter publications
===================

Conditions that :ref:`publications <publications>` must meet to be retained in the pipeline_.

===================================  ====================================================  ===========
Parameter                            Parameter args                                        Description
===================================  ====================================================  ===========
``-totally-final``                                                                         Only keep :ref:`publications <publications>` whose content is :ref:`totally final <totallyfinal>`
``-not-totally-final``                                                                     Only keep :ref:`publications <publications>` whose content is not :ref:`totally final <totallyfinal>`
``-oa``                                                                                    Only keep :ref:`publications <publications>` that are :ref:`Open Access <oa>`
``-not-oa``                                                                                Only keep :ref:`publications <publications>` that are not :ref:`Open Access <oa>`
``-journal-title``                   <regex_>                                              Only keep :ref:`publications <publications>` whose :ref:`journal title <journaltitle>` has a match with the given regular expression
``-not-journal-title``               <regex_>                                              Only keep :ref:`publications <publications>` whose :ref:`journal title <journaltitle>` does not have a match with the given regular expression
``-journal-title-empty``                                                                   Only keep :ref:`publications <publications>` whose :ref:`journal title <journaltitle>` is empty
``-not-journal-title-empty``                                                               Only keep :ref:`publications <publications>` whose :ref:`journal title <journaltitle>` is not empty
``-pub-date-more``                   <`ISO-8601 time`_>                                    Only keep :ref:`publications <publications>` whose :ref:`publication date <pubdate>` is more than or equal to given time (add "T00:00:00Z" to the end to get an ISO-8601 time from a date)
``-pub-date-less``                   <`ISO-8601 time`_>                                    Only keep :ref:`publications <publications>` whose :ref:`publication date <pubdate>` is less than or equal to given time (add "T00:00:00Z" to the end to get an ISO-8601 time from a date)
``-citations-count``                 *<positive integer> ...*                              Only keep :ref:`publications <publications>` whose :ref:`citations count <citationscount>` is equal to one of given counts
``-not-citations-count``             *<positive integer> ...*                              Only keep :ref:`publications <publications>` whose :ref:`citations count <citationscount>` is not equal to any of given counts
``-citations-count-more``            *<positive integer>*                                  Only keep :ref:`publications <publications>` whose :ref:`citations count <citationscount>` is more than the given count
``-citations-count-less``            *<positive integer>*                                  Only keep :ref:`publications <publications>` whose :ref:`citations count <citationscount>` is less than the given count
``-citations-timestamp-more``        <`ISO-8601 time`_>                                    Only keep :ref:`publications <publications>` whose :ref:`citations count last update timestamp <citationstimestamp>` is more than or equal to the given time
``-citations-timestamp-less``        <`ISO-8601 time`_>                                    Only keep :ref:`publications <publications>` whose :ref:`citations count last update timestamp <citationstimestamp>` is less than or equal to the given time
``-corresp-author-name``             <regex_>                                              Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` name having a match with the given regular expression
``-not-corresp-author-name``         <regex_>                                              Only keep :ref:`publications <publications>` with no :ref:`corresponding author <correspauthor>`\ s names having a match with the given regular expression
``-corresp-author-name-empty``                                                             Only keep :ref:`publications <publications>` whose :ref:`corresponding author <correspauthor>`\ s names are empty
``-not-corresp-author-name-empty``                                                         Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` name that is not empty
``-corresp-author-orcid``            <regex_>                                              Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` ORCID iD having a match with the given regular expression
``-not-corresp-author-orcid``        <regex_>                                              Only keep :ref:`publications <publications>` with no :ref:`corresponding author <correspauthor>`\ s ORCID iDs having a match with the given regular expression
``-corresp-author-orcid-empty``                                                            Only keep :ref:`publications <publications>` whose :ref:`corresponding author <correspauthor>`\ s ORCID iDs are empty
``-not-corresp-author-orcid-empty``                                                        Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` ORCID iD that is not empty
``-corresp-author-email``            <regex_>                                              Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` e-mail address having a match with the given regular expression
``-not-corresp-author-email``        <regex_>                                              Only keep :ref:`publications <publications>` with no :ref:`corresponding author <correspauthor>`\ s e-mail addresses having a match with the given regular expression
``-corresp-author-email-empty``                                                            Only keep :ref:`publications <publications>` whose :ref:`corresponding author <correspauthor>`\ s e-mail addresses are empty
``-not-corresp-author-email-empty``                                                        Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` e-mail address that is not empty
``-corresp-author-phone``            <regex_>                                              Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` telephone number having a match with the given regular expression
``-not-corresp-author-phone``        <regex_>                                              Only keep :ref:`publications <publications>` with no :ref:`corresponding author <correspauthor>`\ s telephone numbers having a match with the given regular expression
``-corresp-author-phone-empty``                                                            Only keep :ref:`publications <publications>` whose :ref:`corresponding author <correspauthor>`\ s telephone numbers are empty
``-not-corresp-author-phone-empty``                                                        Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` telephone number that is not empty
``-corresp-author-uri``              <regex_>                                              Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` web page address having a match with the given regular expression
``-not-corresp-author-uri``          <regex_>                                              Only keep :ref:`publications <publications>` with no :ref:`corresponding author <correspauthor>`\ s web page addresses having a match with the given regular expression
``-corresp-author-uri-empty``                                                              Only keep :ref:`publications <publications>` whose :ref:`corresponding author <correspauthor>`\ s web page addresses are empty
``-not-corresp-author-uri-empty``                                                          Only keep :ref:`publications <publications>` with a :ref:`corresponding author <correspauthor>` web page address that is not empty
``-corresp-author-size``             *<positive integer> ...*                              Only keep :ref:`publications <publications>` whose :ref:`corresponding author <correspauthor>`\ s size is equal to one of given sizes
``-not-corresp-author-size``         *<positive integer> ...*                              Only keep :ref:`publications <publications>` whose :ref:`corresponding author <correspauthor>`\ s size is not equal to any of given sizes
``-corresp-author-size-more``        *<positive integer>*                                  Only keep :ref:`publications <publications>` whose :ref:`corresponding author <correspauthor>`\ s size is more than given size
``-corresp-author-size-less``        *<positive integer>*                                  Only keep :ref:`publications <publications>` whose :ref:`corresponding author <correspauthor>`\ s size is less than given size
``-visited``                         <regex_>                                              Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` whose URL has a match with the given regular expression
``-not-visited``                     <regex_>                                              Only keep :ref:`publications <publications>` with no :ref:`visited site <visitedsites>`\ s whose URL has a match with the given regular expression
``-visited-host``                    *<string> <string> ...*                               Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` whose URL host part is present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-not-visited-host``                *<string> <string> ...*                               Only keep :ref:`publications <publications>` with no :ref:`visited site <visitedsites>`\ s whose URL host part is present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-visited-type``                    <:ref:`PublicationPartType <publication_types>`> ...  Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` of type equal to one of given types
``-not-visited-type``                <:ref:`PublicationPartType <publication_types>`> ...  Only keep :ref:`publications <publications>` with no :ref:`visited site <visitedsites>`\ s of type equal to any of given types
``-visited-type-more``               <:ref:`PublicationPartType <publication_types>`>      Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` of better type than the given type
``-visited-type-less``               <:ref:`PublicationPartType <publication_types>`>      Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` of lesser type than the given type
``-visited-type-final``                                                                    Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` of final type
``-not-visited-type-final``                                                                Only keep :ref:`publications <publications>` with no :ref:`visited site <visitedsites>`\ s of final type
``-visited-type-pdf``                                                                      Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` of PDF type
``-not-visited-type-pdf``                                                                  Only keep :ref:`publications <publications>` with no :ref:`visited site <visitedsites>`\ s of PDF type
``-visited-from``                    <regex_>                                              Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` whose provenance URL has a match with the given regular expression
``-not-visited-from``                <regex_>                                              Only keep :ref:`publications <publications>` with no :ref:`visited site <visitedsites>`\ s whose provenance URL has a match with the given regular expression
``-visited-from-host``               *<string> <string> ...*                               Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` whose provenance URL host part is present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-not-visited-from-host``           *<string> <string> ...*                               Only keep :ref:`publications <publications>` with no :ref:`visited site <visitedsites>`\ s whose provenance URL host part is present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-visited-time-more``               <`ISO-8601 time`_>                                    Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` whose visit time is more than or equal to the given time
``-visited-time-less``               <`ISO-8601 time`_>                                    Only keep :ref:`publications <publications>` with a :ref:`visited site <visitedsites>` whose visit time is less than or equal to the given time
``-visited-size``                    *<positive integer> ...*                              Only keep :ref:`publications <publications>` whose :ref:`visited site <visitedsites>`\ s size is equal to one of given sizes
``-not-visited-size``                *<positive integer> ...*                              Only keep :ref:`publications <publications>` whose :ref:`visited site <visitedsites>`\ s size is not equal to any of given sizes
``-visited-size-more``               *<positive integer>*                                  Only keep :ref:`publications <publications>` whose :ref:`visited site <visitedsites>`\ s size is more than the given size
``-visited-size-less``               *<positive integer>*                                  Only keep :ref:`publications <publications>` whose :ref:`visited site <visitedsites>`\ s size is less than the given size
===================================  ====================================================  ===========

Filter publication parts
========================

Conditions that :ref:`publication part <publication_parts>`\ s must meet for the publication to be retained in the pipeline_.

Each parameter (except ``-part-empty``, ``-not-part-empty``, ``-part-usable``, ``-not-part-usable``, ``-part-final``, ``-not-part-final``) has a corresponding parameter specifying the publication parts that need to meet the condition given by the parameter. For example, ``-part-content`` gives a regular expression and ``-part-content-part`` lists all publication parts that must have a match with the given regular expression. If ``-part-content`` is specified, then ``-part-content-part`` must also be specified (and vice versa).

A publication part is any of: :ref:`the pmid <fetcher_pmid>`, :ref:`the pmcid <fetcher_pmcid>`, :ref:`the doi <fetcher_doi>`, :ref:`title <fetcher_title>`, :ref:`keywords <fetcher_keywords>`, :ref:`MeSH <fetcher_mesh>`, :ref:`EFO <efo>`, :ref:`GO <go>`, :ref:`theAbstract <fetcher_theabstract>`, :ref:`fulltext <fetcher_fulltext>`.

========================  ====================================================  ===========
Parameter                 Parameter args                                        Description
========================  ====================================================  ===========
``-part-empty``           <:ref:`PublicationPartName <publication_parts>`> ...  Only keep :ref:`publications <publications>` with specified parts being :ref:`empty <empty>`
``-not-part-empty``       <:ref:`PublicationPartName <publication_parts>`> ...  Only keep :ref:`publications <publications>` with specified parts not being :ref:`empty <empty>`
``-part-usable``          <:ref:`PublicationPartName <publication_parts>`> ...  Only keep :ref:`publications <publications>` with specified parts being :ref:`usable <usable>`
``-not-part-usable``      <:ref:`PublicationPartName <publication_parts>`> ...  Only keep :ref:`publications <publications>` with specified parts not being :ref:`usable <usable>`
``-part-final``           <:ref:`PublicationPartName <publication_parts>`> ...  Only keep :ref:`publications <publications>` with specified parts being :ref:`final <final>`
``-not-part-final``       <:ref:`PublicationPartName <publication_parts>`> ...  Only keep :ref:`publications <publications>` with specified parts not being :ref:`final <final>`
``-part-content``         <regex_>                                              Only keep :ref:`publications <publications>` where the :ref:`contents <content>` of all parts specified with ``-part-content-part`` have a match with the given regular expression
``-not-part-content``     <regex_>                                              Only keep :ref:`publications <publications>` where the :ref:`contents <content>` of all parts specified with ``-not-part-content-part`` do not have a match with the given regular expression
``-part-size``            *<positive integer> ...*                              Only keep :ref:`publications <publications>` where the :ref:`size <size>`\ s of all parts specified with ``-part-size-part`` are equal to any of given sizes
``-not-part-size``        *<positive integer> ...*                              Only keep :ref:`publications <publications>` where the :ref:`size <size>`\ s of all parts specified with ``-not-part-size-part`` are not equal to any of given sizes
``-part-size-more``       *<positive integer>*                                  Only keep :ref:`publications <publications>` where the :ref:`size <size>`\ s of all parts specified with ``-part-size-more-part`` are more than the given size
``-part-size-less``       *<positive integer>*                                  Only keep :ref:`publications <publications>` where the :ref:`size <size>`\ s of all parts specified with ``-part-size-less-part`` are less than the given size
``-part-type``            <:ref:`PublicationPartType <publication_types>`> ...  Only keep :ref:`publications <publications>` where the :ref:`type <type>`\ s of all parts specified with ``-part-type-part`` are equal to any of given types
``-not-part-type``        <:ref:`PublicationPartType <publication_types>`> ...  Only keep :ref:`publications <publications>` where the :ref:`type <type>`\ s of all parts specified with ``-not-part-type-part`` are not equal to any of given types
``-part-type-more``       <:ref:`PublicationPartType <publication_types>`>      Only keep :ref:`publications <publications>` where the :ref:`type <type>`\ s of all parts specified with ``-part-type-more-type`` are better than the given type
``-part-type-less``       <:ref:`PublicationPartType <publication_types>`>      Only keep :ref:`publications <publications>` where the :ref:`type <type>`\ s of all parts specified with ``-part-type-less-type`` are lesser than the given type
``-part-type-final``      <:ref:`PublicationPartType <publication_types>`>      Only keep :ref:`publications <publications>` where the :ref:`type <type>`\ s of all parts specified with ``-part-type-final`` are of final type
``-not-part-type-final``  <:ref:`PublicationPartType <publication_types>`>      Only keep :ref:`publications <publications>` where the :ref:`type <type>`\ s of all parts specified with ``-not-part-type-final`` are not of final type
``-part-type-pdf``        <:ref:`PublicationPartType <publication_types>`>      Only keep :ref:`publications <publications>` where the :ref:`type <type>`\ s of all parts specified with ``-part-type-pdf-part`` are of PDF type
``-not-part-type-pdf``    <:ref:`PublicationPartType <publication_types>`>      Only keep :ref:`publications <publications>` where the :ref:`type <type>`\ s of all parts specified with ``-not-part-type-pdf-part`` are not of PDF type
``-part-url``             <regex_>                                              Only keep :ref:`publications <publications>` where the :ref:`URL <url>`\ s of all parts specified with ``-part-url-part`` have a match with the given regular expression
``-not-part-url``         <regex_>                                              Only keep :ref:`publications <publications>` where the :ref:`URL <url>`\ s of all parts specified with ``-not-part-url-part`` do not have a match with the given regular expression
``-part-url-host``        *<string> <string> ...*                               Only keep :ref:`publications <publications>` where the :ref:`URL <url>` host parts of all parts specified with ``-part-url-host-part`` are present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-not-part-url-host``    *<string> <string> ...*                               Only keep :ref:`publications <publications>` where the :ref:`URL <url>` host parts of all parts specified with ``-not-part-url-host-part`` are not present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-part-time-more``       <`ISO-8601 time`_>                                    Only keep :ref:`publications <publications>` where the :ref:`timestamp <timestamp>`\ s of all parts specified with ``-part-time-more-part`` are more than or equal to the given time
``-part-time-less``       <`ISO-8601 time`_>                                    Only keep :ref:`publications <publications>` where the :ref:`timestamp <timestamp>`\ s of all parts specified with ``-part-time-less-part`` are less than or equal to the given time
========================  ====================================================  ===========

Filter webpages and docs
========================

Conditions that :ref:`webpages <webpages>` and :ref:`docs <docs>` must meet to be retained in the pipeline_.

================================  =========================  ===========
Parameter                         Parameter args             Description
================================  =========================  ===========
``-broken``                                                  Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` that are :ref:`broken <broken>`
``-not-broken``                                              Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` that are not :ref:`broken <broken>`
``-start-url``                    <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`start URL <starturl>` has a match with the given regular expression
``-not-start-url``                <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`start URL <starturl>` does not have a match with the given regular expression
``-start-url-host``               *<string> <string> ...*    Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`start URL <starturl>` host part is present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-not-start-url-host``           *<string> <string> ...*    Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`start URL <starturl>` host part is not present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-final-url``                    <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`final URL <finalurl>` has a match with the given regular expression
``-not-final-url``                <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`final URL <finalurl>` does not have a match with the given regular expression
``-final-url-host``               *<string> <string> ...*    Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`final URL <finalurl>` host part is present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-not-final-url-host``           *<string> <string> ...*    Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`final URL <finalurl>` host part is not present in the given list of strings (comparison is done case-insensitively and "www." is removed)
``-final-url-empty``                                         Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`final URL <finalurl>` is empty
``-not-final-url-empty``                                     Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`final URL <finalurl>` is not empty
``-content-type``                 <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`HTTP Content-Type <contenttype>` has a match with the given regular expression
``-not-content-type``             <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`HTTP Content-Type <contenttype>` does not have a match with the given regular expression
``-content-type-empty``                                      Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`HTTP Content-Type <contenttype>` is empty
``-not-content-type-empty``                                  Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`HTTP Content-Type <contenttype>` is not empty
``-status-code``                  *<integer> <integer> ...*  Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`HTTP status code <statuscode>` is equal to one of given codes
``-not-status-code``              *<integer> <integer> ...*  Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`HTTP status code <statuscode>` is not equal to any of given codes
``-status-code-more``             *<integer>*                Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`HTTP status code <statuscode>` is bigger than the given code
``-status-code-less``             *<integer>*                Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`HTTP status code <statuscode>` is smaller than the given code
``-title``                        <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`page title <webpage_title>` has a match with the given regular expression
``-not-title``                    <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`page title <webpage_title>` does not have a match with the given regular expression
``-title-size``                   *<positive integer> ...*   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`title length <titlelength>` is equal to one of given lengths
``-not-title-size``               *<positive integer> ...*   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`title length <titlelength>` is not equal to any of given lengths
``-title-size-more``              *<positive integer>*       Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`title length <titlelength>` is more than the given length
``-title-size-less``              *<positive integer>*       Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`title length <titlelength>` is less than the given length
``-content``                      <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`content <webpage_content>` has a match with the given regular expression
``-not-content``                  <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`content <webpage_content>` does not have a match with the given regular expression
``-content-size``                 *<positive integer> ...*   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`content length <contentlength>` is equal to one of given lengths
``-not-content-size``             *<positive integer> ...*   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`content length <contentlength>` is not equal to any of given lengths
``-content-size-more``            *<positive integer>*       Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`content length <contentlength>` is more than the given length
``-content-size-less``            *<positive integer>*       Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`content length <contentlength>` is less than the given length
``-content-time-more``            <`ISO-8601 time`_>         Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`content time <contenttime>` is more than or equal to the given time
``-content-time-less``            <`ISO-8601 time`_>         Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`content time <contenttime>` is less than or equal to the given time
``-license``                      <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`software license <license>` has a match with the given regular expression
``-not-license``                  <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`software license <license>` does not have a match with the given regular expression
``-license-empty``                                           Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`software license <license>` is empty
``-not-license-empty``                                       Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`software license <license>` is not empty
``-language``                     <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`programming language <language>` has a match with the given regular expression
``-not-language``                 <regex_>                   Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`programming language <language>` does not have a match with the given regular expression
``-language-empty``                                          Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`programming language <language>` is empty
``-not-language-empty``                                      Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`programming language <language>` is not empty
``-has-scrape``                                              Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` that have :ref:`scraping rules <scraping>` (based on :ref:`final URL <finalurl>`)
``-not-has-scrape``                                          Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` that do not have :ref:`scraping rules <scraping>` (based on :ref:`final URL <finalurl>`)
================================  =========================  ===========

Sort content
============

Sorting of fetched/loaded and filtered content. If sorted by their ID, then :ref:`publications <publications>` are first sorted by :ref:`the PMID <pmid>`, then by :ref:`the PMCID <pmcid>` (if PMID is absent), then by :ref:`the DOI <doi>` (if PMID and PMCID are absent). Internally, the PMID, the PMCID and the DOI registrant are sorted numerically, DOIs within the same registrant alphabetically. If sorted by their URL, then :ref:`webpages <webpages>` and :ref:`docs <docs>` are sorted alphabetically according to their :ref:`startUrl <starturl>`.

==============  ==============  ===========
Parameter       Parameter args  Description
==============  ==============  ===========
``-asc``                        Sort :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` by their ID/URL in ascending order
``-desc``                       Sort :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` by their ID/URL in descending order
``-asc-time``                   Sort :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` by their :ref:`fetchTime <fetchtime>` in ascending order
``-desc-time``                  Sort :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` by their :ref:`fetchTime <fetchtime>` in descending order
==============  ==============  ===========

Limit content
=============

Fetched/loaded, filtered and sorted content can be limited to a given number of entries either in the front or back. The list of `top hosts`_ will also be limited.

=========  ====================  ===========
Parameter  Parameter args        Description
=========  ====================  ===========
``-head``  *<positive integer>*  Only keep the first given number of :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` (same for `top hosts`_ from :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>`)
``-tail``  *<positive integer>*  Only keep the last given number of :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` (same for `top hosts`_ from :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>`)
=========  ====================  ===========

.. _update_citations_count:

Update citations count
======================

===========================  =================  ===========
Parameter                    Parameter args     Description
===========================  =================  ===========
``-update-citations-count``  *<database file>*  Fetch and update the :ref:`citations count <citationscount>` and :ref:`citations count last update timestamp <citationstimestamp>` of all :ref:`publications <publications>` resulting from the pipeline_ and put successfully updated :ref:`publications <publications>` to the given :ref:`database <database>`
===========================  =================  ===========

Put to database
===============

=========  =================  ===========
Parameter  Parameter args     Description
=========  =================  ===========
``-put``   *<database file>*  Put all :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` resulting from the pipeline_ to the given :ref:`database <database>`, overwriting any existing entries that have equal IDs/URLs
=========  =================  ===========

Remove from database
====================

===========  =================  ===========
Parameter    Parameter args     Description
===========  =================  ===========
``-remove``  *<database file>*  From the given :ref:`database <database>`, remove all :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` with IDs corresponding to IDs of :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` resulting from the pipeline_
===========  =================  ===========

.. _cli_output:

Output
======

Output final list of :ref:`publications <publications>` (or :ref:`publication part <publication_parts>`\ s specified by ``--out-part``), :ref:`webpages <webpages>` and :ref:`docs <docs>` resulting from the pipeline_ to stdout or the specified text files in the format specified by the `Output modifiers`_ ``--plain`` and ``--format``.

If ``--format text`` (the default) and ``--plain`` are specified and ``--out-part`` specifies only :ref:`publication IDs <ids_of_publications>`, then publications will be output in the form ``<pmid>\t<pmcid>\t<doi>``, one per line. Also in case of ``--format text --plain``, if ``--out-part`` specifies only one publication part (that is not :ref:`theAbstract <fetcher_theabstract>` or :ref:`fulltext <fetcher_fulltext>`), then for each publication there will be only one line in the output, containing the plain text output of that publication part. Otherwise, there will be separator lines separating different publications in the output.

If ``--format html`` and ``--plain`` are specified and ``--out-part`` specifies only publication IDs, then the output will be a HTML table of publication IDs, with one row corresponding to one publication.

The full output format of ``--format json`` is specified later in :ref:`JSON format <json_output>`. There is also a short description about the :ref:`HTML and plain text outputs <html_and_plain_text_output>`.

.. _top_hosts:

Additionally, there are operations to get the so-called _`top hosts`: all host parts of URLs of :ref:`visited site <visitedsites>`\ s of publications, of URLs of webpages and of URLs of docs, starting from the most common and including count numbers. This can be useful for example for finding hosts to write :ref:`scraping rules <scraping>` for. When counting different hosts, comparison of hosts is done case-insensitively and "www." is removed. Parameter ``-has-scrape`` can be added to only output hosts for which scraping rules could be found and parameter ``-not-has-scrape`` added to only output hosts for which no scraping rules could be found. Parameters ``-head`` and ``-tail`` can be used to limit the size of top hosts output.

For analysing the different sources of publication part content, there is an option to print a :ref:`PublicationPartType <publication_types>` vs :ref:`PublicationPartName <publication_parts>` table in CSV format.

======================  ==============  ===========
Parameter               Parameter args  Description
======================  ==============  ===========
``-out``                                Output :ref:`publications <publications>` (or :ref:`publication part <publication_parts>`\ s specified by ``--out-part``), :ref:`webpages <webpages>` and :ref:`docs <docs>` to stdout in the format specified by the `Output modifiers`_ ``--plain`` and ``--format``
``-txt-pub``            *<file>*        Output :ref:`publications <publications>` (or :ref:`publication part <publication_parts>`\ s specified by ``--out-part``) to the given file in the format specified by the `Output modifiers`_ ``--plain`` and ``--format``
``-txt-web``            *<file>*        Output :ref:`webpages <webpages>` to the given file in the format specified by the `Output modifiers`_ ``--plain`` and ``--format``
``-txt-doc``            *<file>*        Output :ref:`docs <docs>` to the given file in the format specified by the `Output modifiers`_ ``--plain`` and ``--format``
``-count``                              Output count numbers for :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` to stdout
``-out-top-hosts``                      Output all host parts of URLs of visited sites of :ref:`publications <publications>`, of URLs of :ref:`webpages <webpages>` and of URLs of :ref:`docs <docs>` to stdout, starting from most common and including count number
``-txt-top-hosts-pub``  *<file>*        Output all host parts of URLs of :ref:`visited site <visitedsites>`\ s of :ref:`publications <publications>` to the given file, starting from the most common and including count numbers
``-txt-top-hosts-web``  *<file>*        Output all host parts of URLs of :ref:`webpages <webpages>` to the given file, starting from the most common and including count numbers
``-txt-top-hosts-doc``  *<file>*        Output all host parts of URLs of :ref:`docs <docs>` to the given file, starting from the most common and including count numbers
``-count-top-hosts``                    Output number of different host parts of URLs of :ref:`visited site <visitedsites>`\ s of :ref:`publications <publications>`, of URLs of :ref:`webpages <webpages>` and of URLs of :ref:`docs <docs>` to stdout
``-part-table``                         Output a :ref:`PublicationPartType <publication_types>` vs :ref:`PublicationPartName <publication_parts>` table in CSV format to stdout, i.e. how many :ref:`publications <publications>` have content for the given publication part fetched from the given resource type
======================  ==============  ===========

Output modifiers
================

Some parameters to influence the behaviour of outputting operations.

==============  ====================================================  ========  ===========
Parameter       Parameter args                                        Default   Description
==============  ====================================================  ========  ===========
``--plain``                                                                     If specified, then any potential metadata will be omitted from the output
``--format``    *<Format>*                                            ``text``  Can choose between plain text output format (``text``), HTML format (``html``) and :ref:`JSON format <json_output>` (``json``)
``--out-part``  <:ref:`PublicationPartName <publication_parts>`> ...            If specified, then only the specified publication parts will be output (:ref:`webpages <webpages>` and :ref:`docs <docs>` are not affected). Independent from the ``--fetch-part`` parameter.
==============  ====================================================  ========  ===========

****
Test
****

Operations for testing built-in and configurable scraping rules (e.g., ``-print-europepmc-xml`` and ``-test-europepmc-xml``; ``-print-site`` and ``-test-site``) are described in the :ref:`scraping rules <scraping>` section.

.. _examples:

********
Examples
********

Operations with IDs
===================

As a first step in the `pipeline of operations`_, some :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` or :ref:`doc URLs <urls_of_docs>` must be loaded (and possibly filtered). How to create and populate the :ref:`database <database>` files used in this section is explained in the next section.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar \
  -pub 12345678 10.1093/nar/gkw199 -pub-file pub1.txt pub2.txt \
  -pub-db database.db new.db \
  -has-pmcid -doi '(?i)nmeth' \
  -doi-url '^https://www.ebi.ac.uk/europepmc/' -doi-registrant 1038 \
  -out-ids --plain

First, add two :ref:`publication IDs <ids_of_publications>` from the command-line: a publication ID where the :ref:`PMID <id_pmid>` is ``12345678`` and a publication ID where the :ref:`DOI <id_doi>` is ``10.1093/nar/gkw199``. Then add publication IDs from the text files ``pub1.txt`` and ``pub2.txt``, where each line must be in the form ``<pmid>\t<pmcid>\t<doi>`` (except empty lines and lines beginning with ``#`` which are ignored). As last, add all publication IDs found in the :ref:`database <database>` files ``database.db`` and ``new.db``. The resulting list of publication IDs is actually a set, meaning duplicate IDs will be merged.

Then, the publication IDs will be filtered. Parameter ``-has-pmcid`` means that only publication IDs that have a non-:ref:`empty <empty>` :ref:`PMCID <id_pmcid>` (probably meaning that the fulltext is available in :ref:`PubMed Central <pubmed_central>`) will be kept. Specifying ``-doi '(?i)nmeth'`` means that, in addition, the DOI part of the ID must have a match with "nmeth" (Nature Methods) case-insensitively (we specify case-insensitivity with "(?i)" because we are converting the DOIs to upper-case). With ``-doi-url`` we specify that the DOI was found first from the :ref:`Europe PMC <europe_pmc>` API and with ``-doi-registrant`` we specify that the DOI registrant code must be ``1038`` (Nature).

The resultant list of filtered publication IDs will be output to standard output as plain text with the parameter ``-out-ids``. Specifying the modifier ``--plain`` means that the ID provenance URLs will not be output and the output of IDs will be in the form ``<pmid>\t<pmcid>\t<doi>``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar \
  -pub-db new.db -web-db new.db -not-in-db database.db \
  -url '^https' -not-url-host bioconductor.org github.com \
  -txt-ids-pub pub.json -txt-ids-web web.json --format json

First, add all :ref:`publication IDs <ids_of_publications>` and all :ref:`webpage URLs <urls_of_webpages>` from the :ref:`database <database>` file ``new.db``. With ``-not-in-db`` we remove all publication IDs and webpage URLs that are already present in the database file ``database.db``. With the regex ``^https`` specified using the ``-url`` parameter only webpage URLs whose schema is HTTPS are kept. And with ``-not-url-host`` we remove all webpage URLs whose host part is ``bioconductor.org`` or ``github.com`` (or "www.bioconductor.org" or "www.github.com") case-insensitively. The resultant list of publication IDs will be output to the file ``pub.json`` and the resultant list of webpage URLs will be output to the file ``web.json``. The output will be in :ref:`JSON format <json_output>` because it was specified using the ``--format`` modifier. By using ``--format html`` or ``--format html --plain`` we would get a HTML file instead, which when opened in a web browser would list the IDs and URLs as clickable links.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -has-pmid -asc-ids -head-ids 10 -txt-ids-pub oldpmid.txt --plain

Add all :ref:`publication IDs <ids_of_publications>` from the :ref:`database <database>` file ``database.db``, only keep publication IDs that have a non-:ref:`empty <empty>` :ref:`PMID <id_pmid>` part, order the publication IDs (smallest PMID first) and only keep the ``10`` first IDs. The resultant 10 publication IDs will be output to the file ``oldpmid.txt``, where each line is in the form ``<pmid>\t<pmcid>\t<doi>``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar \
  -pub-file oldpmid.txt -pub 12345678 -remove-ids database.db

:ref:`publications <publications>` that have a small :ref:`PMID <id_pmid>` are in the :ref:`database <database>` possibly by mistake. So we can review the file ``oldpmid.txt`` generated in the previous step and keep entries we want to remove from the database listed in that file. Then, with the last command, we add :ref:`publication IDs <ids_of_publications>` from the file ``oldpmid.txt``, manually add an extra publication ID with PMID ``12345678`` from the command-line and with ``-remove-ids`` remove all publications corresponding to the resultant list of publication IDs from the database file ``database.db``.

----

Get content
===========

Next, we'll see how content can be fetched/loaded and how :ref:`database <database>` files (such as those used in the previous section) can be populated with content.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -db-init database.db

This creates a new empty :ref:`database <database>` file called ``database.db``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -fetch --timeout 30000 -usable -put database.db

Add all :ref:`publication IDs <ids_of_publications>` from the file ``pub.txt`` (where each line is in the form ``<pmid>\t<pmcid>\t<doi>``) and for each ID put together a :ref:`publication <content_of_publications>` with content fetched from different :ref:`resources <resources>`, thus getting a list of :ref:`publications <publications>`. The connect and read timeout is changed from the default value of ``15`` seconds to ``30`` seconds with the general Fetching_ parameter timeout_. Filter out non-:ref:`usable publication <publication_usable>`\ s from the list with parameter ``-usable`` and put all publications from the resultant list to the database file ``database.db``. Any existing publication with an ID equal to an ID of a new publication will be overwritten.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -fetch-put database.db --timeout 30000

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -not-usable -remove database.db

If parameters ``-fetch`` and ``-put`` are used, then first all :ref:`publications <publications>` are fetched and loaded into memory, and only then all publications are saved to the :ref:`database <database>` file at once. This is not optimal if there are a lot of publications to fetch, as if some severe error occurs, all content will be lost. Using the parameter ``-fetch-put``, each publication will be put to the database right after it has been fetched. This has the downside of not being able to filter publications before they are put to the database. One way around this is to put all content to the database while fetching and then remove some of the entries from the database based on required filters, as illustrated by the second command.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -db-fetch database.db --threads 16 -usable -count

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -count

With parameter ``-db-fetch`` the following happens for each :ref:`publication <content_of_publications>`: first the publication is looked for in the :ref:`database <database>`; if found, it will be updated with fetched content, if possible and required, and saved back to the database file; if not found, a new publication will be put together with fetched content and put to the database file. This potentially enables less fetching in the future and enables progressive betterment of some :ref:`publications <publications>` over time. Additionally, in contrast to ``-fetch`` and ``-fetch-put``, operation ``-db-fetch`` is multithreaded (with the number of threads specified using ``--threads``), thus much quicker.

Like with ``-fetch-put``, publications can't be filtered before they are put to the database. Any specified filter parameters will only have an effect on which content is retained in memory for further processing (like outputting) down the pipeline_. For example, with ``-usable -count``, the number of :ref:`usable publication <publication_usable>`\ s is output to stdout after fetching is done, but both usable and non-usable publications were saved to the database file, as can be seen with the ``-count`` of the seconds command.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -db-init new.db

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -db-fetch new.db --threads 16 -usable -count

  $ java -jar pubfetcher-cli-<version>.jar -pub-db new.db \
  -db new.db -not-usable -remove new.db

  $ java -jar pubfetcher-cli-<version>.jar -pub-db new.db \
  -db new.db -put database.db

Sometimes, we may want only "fresh" entries (fetched only once and not updated), like ``-fetch`` and ``-fetch-put`` provide, but with multithreading support, like ``-db-fetch`` provides, and with filtering support, like ``-fetch`` provides. Then, the above sequence of commands can be used: make a new :ref:`database <database>` file called ``new.db``; fetch entries to ``new.db`` using ``16`` threads; filter out non-usable entries from ``new.db``; and put content from ``new.db`` to our main database file, overwriting any existing entries there.

Another similar option would be to disable updating of entries by setting the retryLimit_ to ``0`` and emptyCooldown_, nonFinalCooldown_, fetchExceptionCooldown_ to a negative number.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -db-fetch-end database.db --threads 16

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -usable -count

Parameter ``-db-fetch`` will, in addition to saving entries to the :ref:`database <database>` file, load all entries into memory while fetching for further processing (like outputting) down the pipeline_. This might cause excessive memory usage if a lot of entries are fetched. Thus, parameter ``-db-fetch-end`` is provided, which is like ``-db-fetch`` except it does not retain any of the entries in memory. Any further filtering, outputting, etc can be done on the database file after fetching with ``-db-fetch-end`` is done, as shown with the provided second command.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar \
  -pub-file pub.txt -web-file web.txt -doc-file doc.txt \
  -db-fetch-end database.db --threads 16 --log database.log

An example of a comprehensive and quick fetching command: add all provided :ref:`publication IDs <ids_of_publications>`, :ref:`webpage URLs <urls_of_webpages>` and :ref:`doc URLs <urls_of_docs>`, fetch all corresponding :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>`, using ``16`` threads for this process and saving the content to the :ref:`database <database>` file ``database.db``, and append all log messages to the file ``database.log`` for possible future reference and analysis.

----

Loading content
===============

After content has been fetched, e.g. using one of commands in the previous section, it can be loaded and explored.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db --pre-filter -oa -journal-title 'Nature' \
  -not-part-empty fulltext -out | less

From the :ref:`database <database>` file ``database.db``, load all :ref:`publications <publications>` that are Open Access, that are from a journal whose title has a match with the regular expression ``Nature`` and whose :ref:`fulltext <fetcher_fulltext>` part is not :ref:`empty <empty>`, and output these publications with metadata and in plain text to stdout, from where output is piped to the pager ``less``. Specifying ``--pre-filter`` means that content is filtered while being loaded from the database, meaning that entries not passing the filter will not be retained in memory. If ``--pre-filter`` would not be specified, then first all entries corresponding to the added :ref:`publication IDs <ids_of_publications>` would be loaded to memory at once and only then would the entries start to be removed with the specified filters. This has the advantage of being able to see in log messages how many entries pass each filter, however, if the number of added and filtered publication IDs is very big, it could be better to use ``--pre-filter`` to not cause excessive memory usage.

----

Limit fetching/loading
======================

For testing or memory reduction purposes the number of fetched/loaded entries can be limited with ``--limit``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -fetch --limit 3 -out | less

Only fetch and output the first ``3`` :ref:`publications <publications>` listed in ``pub.txt``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -fetch --limit 3 --pre-filter -oa -out | less

Only fetch and output the first ``3`` Open Access :ref:`publications <publications>` listed in ``pub.txt``. Using ``--pre-filter`` means that filtering is done before limiting the entries, meaning that more than 3 entries might be fetched, because fetching happens until a third Open Access publication is encountered, but exactly 3 entries are output (if there are enough publications listed in ``pub.txt``). If ``--pre-filter`` was not used, then exactly 3 entries would be fetched (if there are enough publications listed in ``pub.txt``), meaning that less than 3 entries might be output, because not all of the publications might be Open Access.

----

Fetch only some publication parts
=================================

If we are only interested in some :ref:`publication part <publication_parts>`\ s, it might be advantageous to list them explicitly. This might make fetching faster, because we can skip Internet :ref:`resources <resources>` that can't provide us with any missing parts we are interested in or we can stop fetching of new resources altogether if all parts we are interested in are :ref:`final <final>`.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -fetch --fetch-part title theAbstract -out | less

Only fetch the :ref:`title <fetcher_title>` and :ref:`theAbstract <fetcher_theabstract>` for the added publication IDs, all other :ref:`publication part <publication_parts>`\ s (except IDs) will be :ref:`empty <empty>` in the output.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -fetch --fetch-part title theAbstract \
  -out --out-part title theAbstract --plain | less

If only :ref:`title <fetcher_title>` and :ref:`theAbstract <fetcher_theabstract>` are fetched, then all other :ref:`publication part <publication_parts>`\ s (except IDs) will be :ref:`empty <empty>`, thus we might not want to output these empty parts. This can be done be specifying the ``title`` and ``theAbstract`` parts with ``--out-part``. Additionally specifying ``--plain`` means no metadata is output either, thus the output will consist of only plain text :ref:`publication <content_of_publications>` titles and abstracts with separating characters between different publications.

----

Converting IDs
==============

As a special case of the ability to only fetch some :ref:`publication part <publication_parts>`\ s, PubFetcher can be used as an ID converter between :ref:`PMID <id_pmid>`/:ref:`PMCID <id_pmcid>`/:ref:`DOI <id_doi>`.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -fetch --fetch-part pmid pmcid doi --out-part pmid pmcid doi \
  -txt-pub newpub.txt --plain

Take all :ref:`publication IDs <ids_of_publications>` from ``pub.txt`` (where each line is in the form ``<pmid>\t<pmcid>\t<doi>``) and for each ID fetch only :ref:`publication part <publication_parts>`\ s :ref:`the PMID <pmid>`, :ref:`the PMCID <pmcid>` and :ref:`the DOI <doi>` and output only these parts to the file ``newpub.txt``. In the output file each line will be in the form ``<pmid>\t<pmcid>\t<doi>``, because ID provenance URLs are excluded with ``--plain`` and no other publication parts are output. If the goal is to convert only DOI to PMID and PMCID, for example, then each line in ``pub.txt`` could be in the form ``\t\t<doi>`` and parameters specified as ``--fetch-part pmid pmcid --out-part pmid pmcid``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -db-init newpub.db

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -db-fetch-end newpub.db --threads 16 --fetch-part pmid pmcid doi

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt \
  -db newpub.db --out-part pmid pmcid doi -txt-pub newpub.txt --plain

If a lot of :ref:`publication IDs <ids_of_publications>` are to be converted, it would be better to first fetch all :ref:`publications <publications>` to a resumable temporary :ref:`database <database>` file, using the multithreaded ``-db-fetch-end``, and only then output the parts :ref:`the PMID <pmid>`, :ref:`the PMCID <pmcid>` and :ref:`the DOI <doi>` to the file ``newpub.txt``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db newpub.db \
  -db newpub.db -part-table

We can output a :ref:`PublicationPartType <publication_types>` vs :ref:`PublicationPartName <publication_parts>` table in CSV format to see from which :ref:`resources <resources>` the converted IDs were got from. Most likely the large majority will be from :ref:`Europe PMC <europe_pmc>` (e.g., https://www.ebi.ac.uk/europepmc/webservices/rest/search?resulttype=core&format=xml&query=ext_id:17478515%20src:med). :ref:`DOI <id_doi>`\ s with types other than the "europepmc", "pubmed" or "pmc" types were not converted to DOI by the corresponding resource but just confirmed by it (as fetching that resource required the knowledge of a DOI in the first place). Type "external" means that the supplied ID was not found and confirmed in any resource.

----

In one instance of around 10000 :ref:`publications <publications>`, the usefulness of PubFetcher for only ID conversion manifested itself mostly in the case of finding :ref:`PMCID <id_pmcid>`\ s. But even then, around 97% of PMCIDs were present in :ref:`Europe PMC <europe_pmc>`. As to the rest, around 2% were of type "link_oadoi" (i.e., found using :ref:`Unpaywall <unpaywall>`) and around 1% were of type "pubmed_xml" (i.e., present in :ref:`PubMed <pubmed_xml>`, but not Europe PMC, although it was mostly articles which had been assigned a PMCID but were actually not yet available due to delayed release (embargo)). In the case of :ref:`PMID <id_pmid>`\ s the usefulness is even less and mostly in finding a corresponding PMID (if missing) to the PMCID found using a source other than Europe PMC. And in the case of DOIs, only a couple (out of 10000) were found from :ref:`resources <resources>` other than Europe PMC (mostly because initially only a PMCID was supplied and that PMCID was not present in Europe PMC).

So in conclusion, PubFetcher gives an advantage of a few percent over simply using an XML returned by the Europe PMC API when finding PMCIDs for articles (but also when converting from DOI to PMID), but gives almost no advantage when converting from PMID to DOI.

Filtering content
=================

The are many possible filters, all of which are defined above in the section `Filter content`_.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db -web-db \
  database.db -doc-db database.db -db database.db -usable -grep 'DNA' \
  -oa -pub-date-more 2018-08-15T00:00:00Z -citations-count-more 9 \
  -corresp-author-size 1 2 -part-size-more 2 -part-size-more-part \
  keywords mesh -part-type europepmc_xml pmc_xml doi -part-type-part \
  fulltext -part-time-more 2018-08-15T12:00:00Z -part-time-more-part \
  fulltext -title '(?i)software|database' -status-code-more 199 \
  -status-code-less 300 -not-license-empty -has-scrape -asc -out | less

This example will load all content (:ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>`) from the :ref:`database <database>` file ``database.db`` and apply the following filters (ANDed together) to remove content before it is sorted in ascending order and output:

======================================================================  ===========
Parameter                                                               Description
======================================================================  ===========
``-usable``                                                             Only :ref:`usable publication <publication_usable>`\ s, :ref:`usable webpage <webpage_usable>`\ s and usable docs will be kept
``-grep 'DNA'``                                                         Only :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` whose whole content (excluding metadata) has a match with the regular expression "DNA" (i.e., contains the string "DNA")
``-oa``                                                                 Only keep :ref:`publications <publications>` that are Open Access
``-pub-date-more 2018-08-15T00:00:00Z``                                 Only keep :ref:`publications <publications>` whose :ref:`publication date <pubdate>` is ``2018-08-15`` or later
``-citations-count-more 9``                                             Only keep :ref:`publications <publications>` that are cited more than ``9`` times
``-corresp-author-size 1 2``                                            Only keep :ref:`publications <publications>` for whose ``1`` or ``2`` :ref:`corresponding author <correspauthor>`\ s were found (i.e., publications with no found corresponding authors or more that 2 corresponding authors are discarded)
``-part-size-more 2 -part-size-more-part keywords mesh``                Only keep :ref:`publications <publications>` that have more than ``2`` :ref:`keywords <fetcher_keywords>` and more than ``2`` :ref:`MeSH terms <fetcher_mesh>`
``-part-type europepmc_xml pmc_xml doi -part-type-part fulltext``       Only keep :ref:`publications <publications>` whose :ref:`fulltext <fetcher_fulltext>` part is of type "europepmc_xml", "pmc_xml" or "doi"
``-part-time-more 2018-08-15T12:00:00Z -part-time-more-part fulltext``  Only keep :ref:`publications <publications>` whose :ref:`fulltext <fetcher_fulltext>` part has been obtained at ``2018-08-15 noon (UTC)`` or later
``-title '(?i)software|database'``                                      Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`page title <webpage_title>` has a match with the regular expression ``(?i)software|database`` (i.e., contains case-insensitively "software" or "database")
``-status-code-more 199 -status-code-less 300``                         Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` whose :ref:`status code <statuscode>` is ``2xx``
``-not-license-empty``                                                  Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` that have a non-empty :ref:`software license <license>` name present
``-has-scrape``                                                         Only keep :ref:`webpages <webpages>` and :ref:`docs <docs>` for which :ref:`scraping rules <scraping>` are present
======================================================================  ===========

----

Terminal operations
===================

Operations that are done on the final list of entries. If multiple such operations are specified in one command, then they will be performed in the order they are defined in this reference.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -oa -update-citations-count database.db

Load all :ref:`publications <publications>` from the :ref:`database <database>` file ``database.db``, update the :ref:`citations count <citationscount>` of all Open Access publications and save successfully updated publications back to the database file ``database.db``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -db-init oapub.db

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -oa -put oapub.db

Copy all Open Access :ref:`publications <publications>` from the :ref:`database <database>` file ``database.db`` to the new database file ``oapub.db``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db new.db -db new.db \
  -put database.db

Copy all :ref:`publications <publications>` from the :ref:`database <database>` file ``new.db`` to the database file ``database.db``, overwriting any existing entries in ``database.db``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -not-oa -remove database.db

Remove all not Open Access :ref:`publications <publications>` from the :ref:`database <database>` file ``database.db``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db other.db \
  -remove-ids database.db

Remove all :ref:`publications <publications>` that are also present in the :ref:`database <database>` file ``other.db`` from the database file ``database.db``. As removal is done based on all IDs found in ``other.db`` and no filtering based on the content of entries needs to be done, then loading of content from the database file ``other.db`` is not done and ``-remove-ids`` must be used instead of ``-remove`` for removal from the database file ``database.db``.

----

Output
======

Output can happen to stdout or text files in plain text, HTML or JSON, with or without metadata.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -out | less

Output all :ref:`publications <publications>` from the :ref:`database <database>` file ``database.db`` to stdout in plain text and with metadata and pipe stdout to the pager ``less``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -web-db database.db -db database.db \
  -txt-pub pub.html -txt-web web.html --format html

Output all :ref:`publications <publications>` and :ref:`webpages <webpages>` from the :ref:`database <database>` file ``database.db`` in HTML format and with metadata to the files ``pub.html`` and ``web.html`` respectively.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db \
  -txt-pub pubids.html --out-part pmid pmcid doi --format html --plain

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -txt-ids-pub pubids.html --format html --plain

Both commands will output all :ref:`publication IDs <ids_of_publications>` from the :ref:`database <database>` file ``database.db`` as an HTML table to the file ``pubids.html``.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -out --out-part mesh --format text --plain

Output the :ref:`MeSH terms <fetcher_mesh>` of all :ref:`publications <publications>` from the :ref:`database <database>` file ``database.db`` to stdout in plain text and without metadata. As only one :ref:`publication part <publication_parts>` (that is not :ref:`theAbstract <fetcher_theabstract>` or :ref:`fulltext <fetcher_fulltext>`) is output without metadata, then there will be one line of output (a list of MeSH terms) for each publication.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -web-db database.db -db database.db \
  -out-top-hosts -head 10 -not-has-scrape

From the :ref:`database <database>` file ``database.db``, output host parts of URLs of :ref:`visited site <visitedsites>`\ s of :ref:`publications <publications>` and of URLs of :ref:`webpages <webpages>` for which no :ref:`scraping rules <scraping>` could be found, starting from the most common and including count numbers and limiting output to the ``10`` first hosts for both cases. This could be useful for finding hosts to add scraping rules for.

----

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -part-table > part-table.csv

From all :ref:`publications <publications>` in the :ref:`database <database>` file ``database.db``, generate a :ref:`PublicationPartType <publication_types>` vs :ref:`PublicationPartName <publication_parts>` table in CSV format and output it to the file ``part-table.csv``.

----

.. _export_to_json:

Export to JSON
--------------

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -web-db database.db -doc-db database.db -db database.db \
  -txt-pub pub.json -txt-web web.json -txt-doc doc.json --format json

Output all :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` from the :ref:`database <database>` file ``database.db`` in :ref:`JSON format <json_output>` and with metadata to the files ``pub.json``, ``web.json`` and ``doc.json`` respectively. That is, export all content in JSON, so that the database file and PubFetcher itself would not be needed again for further work with the data.

----

*****
Notes
*****

.. _regex:

The syntax of regular expressions is as defined in Java, see documentation of the Pattern class: https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html.

.. _`ISO-8601 time`:

The `ISO-8601 <https://en.wikipedia.org/wiki/ISO_8601>`_ times must be specified like "2018-08-31T13:37:51Z" or "2018-08-31T13:37:51.123Z".

All :ref:`publication <content_of_publications>` :ref:`DOI <id_doi>`\ s are normalised, this effect can be tested with the ``-normalise-doi`` method.

:ref:`webpages <webpages>` and :ref:`docs <docs>` have the same structure, equivalent methods and common :ref:`scraping rules <scraping>`, they just provide separate stores for saving general web pages and documentation web pages respectively.

If an entry is final (and without a fetching exception) in a :ref:`database <database>`, then it can never be refetched again (only the :ref:`citations count <citationscount>` can be updated). If that entry needs to be refreshed for some reason, then ``-fetch`` or ``-fetch-put`` must be used to fetch a completely new entry and overwrite the old one in the database.

On the other hand, ``-db-fetch`` or ``-db-fetch-end`` could be used multiple times after some interval to try to complete non-final entries, e.g. web servers that were offline might be up again, some resources have been updated with extra content or we have updated some scraping rules. For example, the command ``java -jar pubfetcher-cli-<version>.jar -pub-file pub.txt -db-fetch-end database.db`` could be run a week after the same command was initially run.

Limitations
===========

The querying capabilities of PubFetcher are rather rudimentary (unlike SQL), but hopefully enough for most use cases.

----

For example, different filters are ANDed together and there is no support for OR. As a workaround, different conditions can be output to temporary files of IDs/URLs that can then be put together. For example, output all :ref:`publications <publications>` from the :ref:`database <database>` file ``database.db`` that are cited more than ``9`` times or that have been published on ``2018-01-01`` or later to stdout:

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -citations-count-more 9 \
  -txt-pub pub_citations.txt --out-part pmid pmcid doi --plain

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -pub-date-more 2018-01-01T00:00:00Z \
  -txt-pub pub_pubdate.txt --out-part pmid pmcid doi --plain

  $ java -jar pubfetcher-cli-<version>.jar -pub-file pub_citations.txt \
  pub_pubdate.txt -db database.db -out | less

----

Some advanced filtering might not be possible, because some command-line switches can't be specified twice. For example, the filter ``-part-size-more 2 -part-size-more-part keywords -part-size-more 999 -part-size-more-part theAbstract`` will not filter out entries that have more than ``2`` :ref:`keywords <fetcher_keywords>` and whose :ref:`theAbstract <fetcher_theabstract>` length is more than ``999``, but instead result in an error. As a workaround, the filter might be broken down and the result of the different conditions saved in temporary database files that can then be ANDed together:

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar -db-init pub_keywords.db

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -part-size-more 2 -part-size-more-part keywords \
  -put pub_keywords.db

  $ java -jar pubfetcher-cli-<version>.jar -db-init pub_abstract.db

  $ java -jar pubfetcher-cli-<version>.jar -pub-db database.db \
  -db database.db -part-size-more 999 -part-size-more-part theAbstract \
  -put pub_abstract.db

  $ java -jar pubfetcher-cli-<version>.jar -pub-db pub_keywords.db \
  -in-db pub_abstract.db -db database.db -out | less

----

In the pipeline_ the operations are done in the order they are defined in this reference and with one command the pipeline is run only once. Which means, for example, that it is not possible to filter some content and then refetch the filtered entries using only one command, because content loading/fetching happens before content filtering. In such cases, intermediate results can be saved to temporary files, which can be used by the next command to get the desired outcome. For example, get all :ref:`publications <publications>` from the :ref:`database <database>` file ``database.db`` that have a :ref:`visited site <visitedsites>` whose URL has a match with the regular expression ``academic\.oup\.com|[a-zA-Z0-9.-]*sciencemag\.org`` and refetch those publications from scratch, overwriting the corresponding old publications in ``database.db``:

.. code-block:: bash

  $ java -jar pubfetcher-cli-<version>.jar \
  -pub-db database.db -db database.db \
  -visited 'academic\.oup\.com|[a-zA-Z0-9.-]*sciencemag\.org' \
  -txt-pub oup_science.txt --out-part pmid pmcid doi --plain

  $ java -jar pubfetcher-cli-<version>.jar -pub-file oup_science.txt \
  -fetch-put database.db
