
.. _scraping:

##############
Scraping rules
##############

********
Scraping
********

After :ref:`Getting a HTML document <getting_a_html_document>` is done, we receive a jsoup `Document <https://jsoup.org/apidocs/org/jsoup/nodes/Document.html>`_ from it. Then, the jsoup CSS-like element `Selector <https://jsoup.org/apidocs/org/jsoup/select/Selector.html>`_ can be used to select parts of the HTML or XML document for filling corresponding fields of a :ref:`publication <content_of_publications>`, :ref:`webpage <content_of_webpages>` or :ref:`doc <content_of_docs>`. See also documentation at https://jsoup.org/cookbook/extracting-data/selector-syntax. Note that, extracting content from XMLs received from an API is not really scraping, just the same interface and jsoup methods are used for both website HTMLs and API XMLs for simplicity.

In case of :ref:`publications <publications>`, only :ref:`publication parts <publication_parts>` specified by the CLI :ref:`Get content modifiers <get_content_modifiers>` ``--fetch-part``, or not specified by ``--not-fetch-part``, will be extracted (except IDs, which are always extracted). If neither ``--fetch-part`` nor ``--not-fetch-part`` is specified then all publication parts will be extracted. All other publication fields (that are not publication parts) are always extracted when present.

In case of publication IDs (:ref:`pmid <fetcher_pmid>`, :ref:`pmcid <fetcher_pmcid>`, :ref:`doi <fetcher_doi>`), only the first ID found using the selector is extracted. Any "pmid:", "pmcid:", "doi:" prefix, case-insensitively and ignoring whitespace, is removed from the extracted ID and the ID set to the corresponding publication part, if valid. Problems, like not finding any content for the specified selector or an ID being invalid, are logged. In case of a publication :ref:`title <fetcher_title>`, also only the first found content using the selector is extracted. A few rare publications have also a subtitle -- this is extracted separately and appended to the title with separator " : ". If the publication title, or any other publication part besides the IDs, is already :ref:`final <final>` or the supplied selector is empty, then no extraction is attempted. For publication :ref:`keywords <fetcher_keywords>`, all elements found by the selector are used, each element being a separate keyword. But sometimes, the extracted string can be in the form "keywords: word1, word2", in which case the prefix is removed, case-insensitively and ignoring whitespace, and the keywords split on ",". The keyword separator could also be ";" or "|". For :ref:`abstract <fetcher_theabstract>`\ s, all elements found by the selector are also extracted and concatenated to a final string, with ``\n\n`` (two line breaks) separating the elements. The same is done for :ref:`fulltext <fetcher_fulltext>`\ s, but in addition the title, subtitle and abstract selectors are supplied to the fulltext selection method, because the fulltext part must also contain the title and abstract before the full text.

In case of publication fields :ref:`journalTitle <journaltitle>`, :ref:`pubDate <pubdate>` and :ref:`citationsCount <citationscount>`, the first element found by the supplied selector is extracted. The date in :ref:`pubDate <pubdate>` may be broken down to subelements "Year", "Month" and "Day" in the XML. For :ref:`correspAuthor <correspauthor>`, complex built-in selectors and extraction logic are needed, as the corresponding authors can be marked and additional data about them supplied in a variety of non-trivial ways.

For the resources :ref:`Europe PMC <europe_pmc>`, :ref:`Europe PMC fulltext <europe_pmc_fulltext>`, :ref:`Europe PMC mined <europe_pmc_mined>`, :ref:`PubMed XML <pubmed_xml>`, :ref:`PubMed HTML <pubmed_html>` and :ref:`PubMed Central <pubmed_central>`, the selector strings specifying how to extract the publication title, keywords etc are currently hardcoded, as the format of these resources is hopefully fairly static. But for the multitude of :ref:`DOI resources <doi_resource>` and sites found in :ref:`Links <links>`, the selectors are put in a configuration file (described in `Journals YAML`_), as fairly often the format of a few sites can change.

For :ref:`webpages <webpages>` and :ref:`docs <docs>`, there are no hardcoded selectors and all of them must be specified in a configuration file (described in `Webpages YAML`_).

.. _rules_in_yaml:

*************
Rules in YAML
*************

Scraping rules for journal web pages (resolved from :ref:`DOI resource <doi_resource>` or in :ref:`Links <links>`) and :ref:`webpages/docs <webpages>` are specified in YAML configuration files. YAML parsing support is implemented using `SnakeYAML <https://bitbucket.org/asomov/snakeyaml>`_.

There are built-in rules for both journal sites (in `journals.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.yaml>`_) and webpages/docs (in `webpages.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/webpages.yaml>`_). For adding scraping rules to non-supported sites, the location of a custom configuration file can be specified by the user: for journals using the parameter :ref:`journalsYaml <journalsyaml>` and for webpages/docs using the parameter :ref:`webpagesYaml <webpagesyaml>`. In addition to adding rules, the default rules can be overridden. To do that, the top-level keys of the rules to be overridden must be repeated in the custom configuration file and the new desired values specified under those keys.

In case of problems in the configuration file -- either errors in the YAML syntax itself or mistakes in adhering to the configuration format specified below -- the starting of PubFetcher is aborted and a hopefully helpful error message output to the log.

The syntax of regular expressions used in the configuration file is as defined in Java, see documentation of the Pattern class: https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html. If the regex is meant to match a URL, then the string "(?i)^https?://(www\.)?" is automatically put in front of the specified regular expression, except when the regular expression already begins with "^".

.. _journals_yaml:

Journals YAML
=============

Scraping rules for journal web pages (resolved from :ref:`DOI resource <doi_resource>` or in :ref:`Links <links>`), used for filling corresponding :ref:`publications <publications>`. To see an example of a journals YAML, the built-in rules file `journals.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.yaml>`_ can be consulted.

One way to find journal web pages that are worth writing rules for is to use the :ref:`top hosts <top_hosts>` functionality of the CLI on an existing collection of publications.

A journals YAML configuration file must have three sections (separated by ``---``): regex_, site_ and javascript_.

regex
-----

Entries in this section are in the form "regex: site". If the specified regular expression "regex" has a match with the URL resolved from :ref:`DOI resource <doi_resource>` or the URL of the link taken from :ref:`Links <links>`, then the corresponding value "site" will be the name of the rules used for scraping the web page at the URL. The mentioned URL is the final URL, i.e. the URL obtained after following all potential redirections. The rules for "site" must be present in the next section site_. If multiple regular expressions have a match with the URL, then the "site" will be taken from the last such "regex".

If none of the regular expressions have a match with the URL, then no rules could be found for the site. In that case, the extracted :ref:`title <fetcher_title>` will have type "webpage" and as content the text value (until the first "|") of the document's ``<title>`` element and the extracted :ref:`fulltext <fetcher_fulltext>` will have type "webpage" and as content the entire text parsed from the document. The extracted :ref:`title <fetcher_title>` and :ref:`fulltext <fetcher_fulltext>` will fill corresponding publication parts if these parts were requested (as determined by ``--fetch-part`` or ``--not-fetch-part``) and conditions described at the end of :ref:`Publication types <publication_types>` are met. No other publication parts besides :ref:`title <fetcher_title>` and :ref:`fulltext <fetcher_fulltext>` can potentially be filled if no scraping rules are found for a site.

Different publishers might use a common platform, for example HighWire. In such cases the different keys "regex" for matching article web pages of these different publishers might point to a common "site" with rules for that common platform.

The ``-scrape-site`` command of the :ref:`CLI <scrape_rules>` can be used to test which "site" name is found from loaded configuration files for the supplied URL.

site
----

Entries in this section are in the form "site: rulesMap". Each rule name "site" in this section must be specified at least once in the previous section regex_. In case of duplicate rule names "site", the last one will be in effect. The custom configuration file specified using :ref:`journalsYaml <journalsyaml>` is parsed after the built-in rules in `journals.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.yaml>`_, which means that by using the same rule name in the custom configuration file the corresponding rules of the built-in file can be overridden.

The "rulesMap" must be in the form "ScrapeSiteKey: selector". The "selector" is the jsoup CSS-like element `Selector <https://jsoup.org/apidocs/org/jsoup/select/Selector.html>`_ for selecting one or multiple elements in the document, with what will be done with the extracted content depending on "ScrapeSiteKey". In case of duplicate "ScrapeSiteKey", the "selector" from the last one will be in effect.

When writing the "selector", then care should be taken to not select an element (or parts of it) multiple times. For example, the selector "p" will select a ``<p>`` element, but also any potential sub-paragraphs ``<p>`` of that element, thus resulting in duplicate extracted content.

Extracted content will have the type "doi" (from :ref:`Publication types <publication_types>`), if the site's URL was resolved from :ref:`DOI resource <doi_resource>`, or it will have the type attached to the link taken from :ref:`Links <links>`. The content could be used to fill the corresponding publication part, but only if the part is requested as determined by ``--fetch-part`` or ``--not-fetch-part`` and certain other condition are met (as described at the end of :ref:`Publication types <publication_types>`).

.. _ScrapeSiteKey:

The key "ScrapeSiteKey" must be one of the following:

pmid
  Its value is the selector string for the :ref:`pmid <fetcher_pmid>` of the publication. Only the first element found using the selector is extracted and any prefix in the form "pmid:", case-insensitively and ignoring whitespace, is removed from the element's text. A PMID is rarely found in journal article web pages.
pmcid
  Analogous to the pmid selector, except that meant for the :ref:`pmcid <fetcher_pmcid>` of the publication.
doi
  Analogous to the pmid selector, except that meant for the :ref:`doi <fetcher_doi>` of the publication. A DOI is quite often found in journal article web pages. Usually a DOI was used to arrive at the site, so the doi selector usually does not provide new information, but it can upgrade the type of the :ref:`doi <fetcher_doi>` part (from "external" to "doi" or "link_oadoi" for example).
title
  Selector string for the :ref:`title <fetcher_title>` of the publication. Only the first element found using the selector is extracted. If the :ref:`title <fetcher_title>` is already :ref:`final <final>` or the selector is empty, then no extraction is attempted (the same is also true for keywords, abstract and fulltext).
subtitle
  Selector string for a rarely occurring subtitle. Text from the first element found using this selector is appended to the text found using the title selector with separator " : ".
keywords
  Selector string for :ref:`keywords <fetcher_keywords>`. All elements found using the selector are extracted, each element being a separate keyword.
keywords_split
  Selector string for :ref:`keywords <fetcher_keywords>`. All elements found using the selector are extracted, but differently from the "keywords" selector above, the text in each element can be in the form "keywords: word1, word2". In that case, the potentially existing prefix "keywords:" is removed, case-insensitively and ignoring whitespace, and the following string split to separate keywords at the separator "," (and ";" and "|"). If both "keywords" and "keywords_split" are specified, then the "keywords" selector is attempted before.
abstract
  Selector string for :ref:`theAbstract <fetcher_theabstract>`.  All elements found using the selector are extracted and concatenated to a final string, with ``\n\n`` (two line breaks) separating the elements.
fulltext
  Selector string for :ref:`fulltext <fetcher_fulltext>`.  All elements found using the selector are extracted and concatenated to a final string, with ``\n\n`` (two line breaks) separating the elements. The selector must not extract the title and abstract from the site, as when putting together the :ref:`fulltext <fetcher_fulltext>` of the publication, content extracted using the "title", "subtitle" and "abstract" selectors are used for the title and abstract that must be present in the beginning of the :ref:`fulltext <fetcher_fulltext>` part and the "fulltext" selector is used for extracting the remaining full text from the site. Of the remaining full text, everything from introduction to conclusions should be extracted, however most following back matter and metadata, like acknowledgments, author information, author contributions and, most importantly, references, should be excluded. This is currently vaguely defined, but some content should still be included, like descriptions of supplementary materials and glossaries.
fulltext_src
  Sometimes, the full text of the publication is on a separate web page. So the URL of that separate page should be found out to later visit that page and extract the full text (and possibly other content) from it, using a different set of rules (mapped to by a different "regex"). In some cases, finding the URL of this separate page can be done by some simple transformations of the current URL. The transformation is done by replacing the first substring of the URL that matches the regular expression given in "fulltext_src" with the replacement string given in "fulltext_dst". If this replacement occurs and results in a new valid URL, then this URL is added to :ref:`Links <links>` (with type equal to the current type) for later visiting.
fulltext_dst
  The replacement string for the URL substring matched using "fulltext_src". Must be specified if "fulltext_src" is specified (and vice versa).
fulltext_a
  Sometimes, the separate web page of the publication's full text can be linked to somewhere on the current page. This key enables specifying a selector string to extract those links: all elements (usually ``<a>``) found using the selector are extracted and the value of their ``href`` attribute added to :ref:`Links <links>` with type equal to the current type, if the value of ``href`` is a valid URL.
pdf_src
  Sometimes, the full text of the publication can be found in a PDF file. The URL of that PDF could be constructed analogously to the "fulltext_src" and "fulltext_dst" system: the first substring of the current URL that matches the regular expression given in "pdf_src" is replaced with the replacement string given in "pdf_dst" and if the result is a new valid URL, it is added to :ref:`Links <links>`. The type (from :ref:`Publication types <publication_types>`) of the link will be the corresponding PDF type of the current type (e.g., type "pdf_doi" corresponds to type "doi").
pdf_dst
  The replacement string for the URL substring matched using "pdf_src". Must be specified if "pdf_src" is specified (and vice versa).
pdf_a
  Selector string to extract all full text PDF links on the current page. All elements (usually ``<a>``) found using the selector are extracted and the value of their ``href`` attribute added to :ref:`Links <links>`, if the value of ``href`` is a valid URL. The type (from :ref:`Publication types <publication_types>`) of the link will be the corresponding PDF type of the current type (e.g., type "pdf_doi" corresponds to type "doi"). If possible, the "pdf_a" selector should probably be preferred over "pdf_src" and "pdf_dst", as sometimes the PDF file can be missing or inaccessible and then the "pdf_a" selector will correctly fail to add any links, but "pdf_src" and "pdf_dst" will add a manually constructed, but non-existing link to :ref:`Links <links>`.
corresp_author_names
  Selector string for the names of :ref:`correspAuthor <correspauthor>`. All elements found using the selector are extracted, each name added as a separate corresponding author.
corresp_author_emails
  Selector string for the e-mails of :ref:`correspAuthor <correspauthor>`. All elements found using the selector are extracted, with e-mail addresses found in ``href`` attributes (after the prefix ``mailto:`` which is removed). E-mail addresses are added to the names extracted with "corresp_author_names" (in the same order), which means the number of names must match the number of e-mail addresses -- if they don't match, then names are discarded and corresponding authors are only created using the extracted e-mails.

The ``-scrape-selector`` command of the :ref:`CLI <scrape_rules>` can be used to test which selector string from loaded configuration files will be in effect for the supplied URL and "ScrapeSiteKey".

javascript
----------

As mentioned in :ref:`Getting a HTML document <getting_a_html_document>`, either the `jsoup <https://jsoup.org/>`_ or `HtmlUnit <http://htmlunit.sourceforge.net/>`_ library can be used for fetching a HTML document, with one difference being that HtmlUnit supports executing JavaScript, which jsoup does not. But as running JavaScript is very slow with HtmlUnit, then jsoup is the default and JavaScript is turned on only for sites from which content can't be extracted otherwise. This section enables the specification of such sites.

The section is made up of a list of regular expression. If the current URL has a match with any of the regexes, then HtmlUnit and JavaScript support is used for fetching the corresponding site, otherwise jsoup (without JavaScript support) is used. The current URL in this case is either the first URL resolved from :ref:`DOI resource <doi_resource>` (there might be additional redirection while fetching the site) or the URL of a link from :ref:`Links <links>` (again, this URL might change during fetching, so a different regex might be needed to apply scraping rules to the site at the final URL).

The ``-scrape-javascript`` command of the :ref:`CLI <scrape_rules>` can be used to test if JavaScript will be enabled for the supplied URL.

.. _webpages_yaml:

Webpages YAML
=============

Scraping rules for :ref:`webpages/docs <webpages>`. To see an example of a webpages YAML, the built-in rules file `webpages.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/webpages.yaml>`_ can be consulted.

In contrast to the journals YAML, there is only one section in the webpages YAML.

The keys in the webpages YAML must be valid Java regular expressions. If a regex has a match with the :ref:`finalUrl <finalurl>` of a webpage or doc, then rules corresponding to that key are applied to extract content from the corresponding fetched document. If multiple regular expressions have a match with the URL, then the rules will be taken from the last such regex key (this enables overriding of built-in rules using the custom configuration file specified by :ref:`webpagesYaml <webpagesyaml>`). If no regular expressions have a match with the URL, then scraping rules for the webpage or doc are no found and the :ref:`webpage title <webpage_title>` will be the text value of the document's ``<title>`` element and :ref:`webpage content <webpage_content>` will be the entire text parsed from the document.

Each rule under the regex key must be in the form "ScrapeWebpageKey: selector". The "selector" is the jsoup CSS-like element `Selector <https://jsoup.org/apidocs/org/jsoup/select/Selector.html>`_ for selecting one or multiple elements in the document, with what will be done with the extracted content depending on "ScrapeWebpageKey". In case of duplicate "ScrapeWebpageKey", the "selector" from the last one will be in effect. When writing the "selector", then care should be taken to not select an element (or parts of it) multiple times. For example, the selector "p" will select a ``<p>`` element, but also any potential sub-paragraphs ``<p>`` of that element, thus resulting in duplicate extracted content.

The key "ScrapeWebpageKey" must be one of the following:

title
  Its value is the selector string for the :ref:`webpage title <webpage_title>` or doc title. Only the first element found using the selector is extracted. If the selector is empty, then the title will be empty. If the selector is missing, then the title will be the text content of the ``<title>`` element.
content
  Selector string for the :ref:`webpage content <webpage_content>` or doc content.  All elements found using the selector are extracted and concatenated to a final string, with ``\n\n`` (two line breaks) separating the elements. If the selector is empty, then all content of the fetched document will be discarded and the content will be empty. If the selector is missing, then the fetched document will be :ref:`automatically cleaned <cleaning>` and the resulting formatted text set as the content.
javascript
  This key enables turning on JavaScript support, similarly to the javascript_ section in the journals YAML. If its value is ``true`` (case-insensitively), then fetching will be done using HtmlUnit and JavaScript support is enabled, in case of any other value fetching will be done using jsoup and executing JavaScript is not supported. In contrast to other "ScrapeWebpageKey" keys, the value of this key is taken from the rule found using matching to the :ref:`startUrl <starturl>` (and not the :ref:`finalUrl <finalurl>`) of the webpage or doc. If the javascript key is missing (and not set explicitly to ``false``), then JavaScript support is not enabled, but if after fetching the document without JavaScript support there are no scraping rules corresponding to the found :ref:`finalUrl <finalurl>` and the entire text content of the fetched document is smaller than :ref:`webpageMinLengthJavascript <webpageminlengthjavascript>` or a ``<noscript>`` tag is found in it, or alternatively, scraping rules are present for the found :ref:`finalUrl <finalurl>` and the javascript key has a ``true`` value in those rules, then fetching of the document will be repeated, but this time with JavaScript support. If the javascript key is explicitly set to ``false``, then fetching with JavaScript support will not be done in any case.
license
  Selector string for :ref:`license <license>`. Only the first element found using the selector is extracted.
language
  Selector string for :ref:`language <language>`. Only the first element found using the selector is extracted.

The ``-scrape-webpage`` command of the :ref:`CLI <scrape_rules>` can be used to print the rules that would be used for the supplied URL.

.. _testing_of_rules:

****************
Testing of rules
****************

Currently, PubFetcher has no tests or any framework for testing its functionality, except for the scraping rule testing described here. Scraping rules should definitely be tested from time to time, because they depend on external factors, like publishers changing the coding of their web pages.

Tests for `journals.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.yaml>`_ are at `journals.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.csv>`_ and tests for `webpages.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/webpages.yaml>`_ are at `webpages.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/webpages.csv>`_. If new rules are added to a YAML, then tests covering them should be added to the corresponding CSV. In addition, tests for hardcoded rules of some other resources can be found in the `resources/test <https://github.com/edamontology/pubfetcher/tree/master/core/src/main/resources/test>`_ directory. All :ref:`Resources <resources>` except :ref:`Meta <meta>` are covered.

The test files are in a simplified CSV format. The very first line is always skipped and should contain a header explaining the columns. Empty lines, lines containing only whitespace and lines starting with ``#`` are also ignored. Otherwise, each line describes a test and columns are separated using ",". Any quoting of fields is not possible and not necessary, as fields are assumed to not contain the "," symbol. Or actually, the number of columns for a given CSV file is fixed in advance, meaning that the last field can contain the "," symbol as its value is taken to be everything from the separating "," to the end of the line.

One field must be the publication ID (pmid, pmcid or doi), or URL in case of webpages.csv, defining the entry to be fetched. The other fields are mostly numbers specifying the lengths and sizes that the different entry parts must have. Only comparing the sizes of contents (instead of the content itself or instead of using checksums) is rather simplistic, but easy to specify and probably enough for detecting changes in resources that need correcting. What fields (besides the ID) are present in a concrete test depend on what can be obtained from the corresponding resource.

Possible fields for publications are the following: length of publication parts :ref:`pmid <fetcher_pmid>`, :ref:`pmcid <fetcher_pmcid>`, :ref:`doi <fetcher_doi>`, :ref:`title <fetcher_title>`, :ref:`theAbstract <fetcher_theabstract>` and :ref:`fulltext <fetcher_fulltext>`; size (i.e., number of keywords) of publication parts :ref:`keywords <fetcher_keywords>`, :ref:`mesh <fetcher_mesh>`, :ref:`efo <efo>` and :ref:`go <go>`; length of the entire :ref:`correspAuthor <correspauthor>` string (containing all corresponding authors separated by ";") and length of the :ref:`journalTitle <journaltitle>`; number of :ref:`visitedSites <visitedsites>`; value of the string :ref:`pubDate <pubdate>`; value of the Boolean :ref:`oa <oa>` (``1`` for ``true`` and ``0`` for ``false``). Every field is a number, except :ref:`pubDate <pubdate>` where the actual date string must be specified (e.g., ``2018-08-24``). Also, in the tests, the number of :ref:`visitedSites <visitedsites>` is not the actual number of sites visited, but the number of links that were found on the tested page and added manually to the publication by the test routine. For webpages.csv, the fields (beside the ID/URL) are the following: length of the :ref:`webpage title <webpage_title>`, the :ref:`webpage content <webpage_content>`, the :ref:`software license <license>` name and length of the :ref:`programming language <language>` name.

The progress of running tests of a CSV is logged. If all tests pass, then the very last log message will be "OK". Otherwise, the last message will be the number of mismatches, i.e. number of times an actual value was not equal to the value in the corresponding field of a test. The concrete failed tests can be found by searching for "ERROR" level messages in the log.

Tests can be run using PubFetcher-CLI by supplying a parameter specified in the following table. In addition to the ``-test`` parameters there are ``-print`` parameters that will fetch the publication or webpage and output it to stdout in plain text and with metadata. This enables seeing the exact content that will be used for testing the entry. Publications are filled using only the specified resource (e.g., usage of the :ref:`Meta <meta>` resource is also disabled). Additionally, :ref:`visitedSites <visitedsites>` will be filled manually by the ``-test`` and ``-print`` methods with all links found from the one specified resource (when applicable).

==========================  ==============  ===========
Parameter                   Parameter args  Description
==========================  ==============  ===========
``-print-europepmc-xml``    *<pmcid>*       Fetch the publication with the given PMCID from the :ref:`Europe PMC fulltext <europe_pmc_fulltext>` resource and output it to stdout
``-test-europepmc-xml``                     Run all tests for the :ref:`Europe PMC fulltext <europe_pmc_fulltext>` resource (from `europepmc-xml.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/test/europepmc-xml.csv>`_)
``-print-europepmc-html``   *<pmcid>*       Fetch the publication with the given PMCID from the Europe PMC HTML resource and output it to stdout
``-test-europepmc-html``                    Run all tests for the Europe PMC HTML resource (from `europepmc-html.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/test/europepmc-html.csv>`_)
``-print-pmc-xml``          *<pmcid>*       Fetch the publication with the given PMCID from the :ref:`PubMed Central <pubmed_central>` resource and output it to stdout
``-test-pmc-xml``                           Run all tests for the :ref:`PubMed Central <pubmed_central>` resource (from `pmc-xml.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/test/pmc-xml.csv>`_)
``-print-pmc-html``         *<pmcid>*       Fetch the publication with the given PMCID from the PubMed Central HTML resource and output it to stdout
``-test-pmc-html``                          Run all tests for the PubMed Central HTML resource (from `pmc-html.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/test/pmc-html.csv>`_)
``-print-pubmed-xml``       *<pmid>*        Fetch the publication with the given PMID from the :ref:`PubMed XML <pubmed_xml>` resource and output it to stdout
``-test-pubmed-xml``                        Run all tests for the :ref:`PubMed XML <pubmed_xml>` resource (from `pubmed-xml.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/test/pubmed-xml.csv>`_)
``-print-pubmed-html``      *<pmid>*        Fetch the publication with the given PMID from the :ref:`PubMed HTML <pubmed_html>` resource and output it to stdout
``-test-pubmed-html``                       Run all tests for the :ref:`PubMed HTML <pubmed_html>` resource (from `pubmed-html.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/test/pubmed-html.csv>`_)
``-print-europepmc``        *<pmid>*        Fetch the publication with the given PMID from the :ref:`Europe PMC <europe_pmc>` resource and output it to stdout
``-test-europepmc``                         Run all tests for the :ref:`Europe PMC <europe_pmc>` resource (from `europepmc.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/test/europepmc.csv>`_)
``-print-europepmc-mined``  *<pmid>*        Fetch the publication with the given PMID from the :ref:`Europe PMC mined <europe_pmc_mined>` resource and output it to stdout
``-test-europepmc-mined``                   Run all tests for the :ref:`Europe PMC mined <europe_pmc_mined>` resource (from `europepmc-mined.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/test/europepmc-mined.csv>`_)
``-print-oadoi``            *<doi>*         Fetch the publication with the given DOI from the :ref:`Unpaywall <unpaywall>` resource and output it to stdout
``-test-oadoi``                             Run all tests for the :ref:`Unpaywall <unpaywall>` resource (from `oadoi.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/test/oadoi.csv>`_)
``-print-site``             *<url>*         Fetch the publication from the given article web page URL (which can be a DOI link) and output it to stdout. Fetching happens like described in the :ref:`DOI resource <doi_resource>` using the built-in rules in `journals.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.yaml>`_ and custom rules specified using :ref:`journalsYaml <journalsyaml>`.
``-test-site``                              Run all tests written for the built-in rules `journals.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.yaml>`_ (from `journals.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.csv>`_)
``-test-site-regex``        *<regex>*       From all tests written for the built-in rules `journals.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.yaml>`_ (from `journals.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/journals.csv>`_), run only those whose site URL has a match with the given regular expression
``-print-webpage``          *<url>*         Fetch the webpage from the given URL, using the built-in rules in `webpages.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/webpages.yaml>`_ and custom rules specified using :ref:`webpagesYaml <webpagesyaml>`, and output it to stdout
``-test-webpage``                           Run all tests written for the built-in rules `webpages.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/webpages.yaml>`_ (from `webpages.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/webpages.csv>`_)
``-test-webpage-regex``     *<regex>*       From all tests written for the built-in rules `webpages.yaml <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/webpages.yaml>`_ (from `webpages.csv <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/scrape/webpages.csv>`_), run only those whose URL has a match with the given regular expression
==========================  ==============  ===========

If ``--fetch-part`` or ``--not-fetch-part`` are specified then only the selected :ref:`publication parts <publication_parts>` are filled and printed using the ``-print`` methods or tested using the ``-test`` methods. Publication fields like :ref:`correspAuthor <correspauthor>` are always included in the printout or testing. The printing and testing operations are also affected by the :ref:`Fetching <fetching>` parameters. If one of the ``-test`` methods is used, then the ``--log`` parameter should also be used to specify a log file which can later be checked for testing results.

If any larger fetching of content is planned and tests have not been run recently, then tests should be repeated (especially ``-test-site`` and ``-test-webpage``) to find outdated rules that need updating. If testing in a different network environment then some tests might fail because of different access rights to journal content.

For testing the effect of custom selectors, the ``-fetch-webpage-selector`` operation can be used to specify the desired selectors on the command line. This operation ignores all rules loaded from YAML configuration files.
