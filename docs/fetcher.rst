
.. _fetcher:

##############
Fetching logic
##############

The functionality explained in this section is mostly implemented in the source code file `Fetcher.java <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/fetching/Fetcher.java>`_.

*****************
Low-level methods
*****************

.. _getting_a_html_document:

Getting a HTML document
=======================

Fetching HTML (or XML) resources for both :ref:`publications <publications>` and :ref:`webpages <webpages>`/:ref:`docs <docs>` is done in the same method, where either the `jsoup <https://jsoup.org/>`_ or `HtmlUnit <http://htmlunit.sourceforge.net/>`_ libraries are used for getting the document. The HtmlUnit library has the advantage of supporting JavaScript, which needs to be executed to get the proper output for many sites, and it also works for some sites with problematic SSL certificates. As a disadvantage, it is a lot slower than jsoup, which is why using jsoup is the default and HtmlUnit is used only if JavaScript support is requested (or switched to automatically in case of some SSL exceptions). Also, fetching with JavaScript can get stuck for a few rare sites, in which case the misbehaving HtmlUnit code is terminated.

Supplied :ref:`fetching <fetching>` parameters :ref:`timeout <timeout>` and :ref:`userAgent <useragent>` are used for setting the connect timeout and the read timeout and the User-Agent HTTP header of connections. If getting the HTML document for a publication is successful and a list of already fetched links is supplied, then the current URL will be added to that list so that it is not tried again for the current publication. The successfully fetched document is returned to the caller for further processing.

A number of exceptions can occur, in which case getting the HTML document has failed and the following is done:

MalformedURLException
  The protocol is not HTTP or HTTPS or the URL is malformed. Getting the URL is tried again as a PDF document, as a few of these exception are caused by URLs that point to PDFs accessible through the FTP protocol.
HttpStatusException or FailingHttpStatusCodeException
  The :ref:`fetchException <fetchexception>` of the publication, webpage or doc is set to ``true`` in case the HTTP status code in the response is ``503 Service Unavailable``. Setting :ref:`fetchException <fetchexception>` to ``true`` means the URL can be tried again in the future (depending on :ref:`retryCounter <retrycounter>` or :ref:`fetchExceptionCooldown <fetchexceptioncooldown>`) as the ``503`` code is usually a temporary condition. Additionally, in case of publications, :ref:`fetchException <fetchexception>` is set to ``true`` for all failing HTTP status codes if the URL is not from "doi.org" and it is not a URL pointing to a PDF, PS or GZIP file.
ConnectException or NoRouteToHostException
  An error occurred while attempting to connect a socket to a remote address and port. Set :ref:`fetchException <fetchexception>` to ``true``.
UnsupportedMimeTypeException
  The response MIME type is not supported. If the MIME type is determined to be a PDF type, then getting the URL is tried again, but as a PDF document.
SocketTimeoutException
  A timeout has occurred on a socket read or accept. A new attempt is made right away and if that also fails with a timeout, then :ref:`fetchException <fetchexception>` is set to ``true``.
SSLHandshakeException or SSLProtocolException
  Problem with SSL. If fetching was attempted with jsoup, then it is attempted once more, but with HtmlUnit.
IOException
  A connection or read error occurred, just issue a warning to the log.
Exception
  Some other checked exception has occurred, set :ref:`fetchException <fetchexception>` to ``true``.

The HTML document fetching method can be tested with the :ref:`CLI commands <print_a_web_page>` ``-fetch-document`` or ``-fetch-document-javascript`` (but without publications, webpages, docs and PDF support).

.. _getting_a_pdf_document:

Getting a PDF document
======================

Analogous to `getting a HTML document`_. The `Apache PDFBox <https://pdfbox.apache.org/>`_ library is used for extracting content and metadata from the PDF. The method for getting a PDF document is called upon if the URL is known in advance to point to a PDF file or if this fact is found out during the fetching of the URL as a HTML document.

Nothing is returned to the caller, as the supplied :ref:`publication <content_of_publications>`, :ref:`webpage <content_of_webpages>` or :ref:`doc <content_of_docs>` is filled directly. For webpages and docs, all the text extracted from the PDF is set as their content, and if a title is found among the PDF metadata, it is set as their title. For publications, the text extracted from the PDF is set to be the fulltext_. Also, title_, keywords_ or theAbstract_ are filled with content found among the PDF metadata, but as this happens very rarely, fetching of the PDF is not done at all if the fulltext_ is already :ref:`final <final>`.

.. _selecting:

Selecting from the returned HTML document
=========================================

The fetched HTML is parsed to a jsoup `Document <https://jsoup.org/apidocs/org/jsoup/nodes/Document.html>`_ and returned to the caller.

Then, parts of the document can be selected to fill the corresponding fields of :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` using the jsoup CSS-like element `Selector <https://jsoup.org/apidocs/org/jsoup/select/Selector.html>`_. This is explained in more detail in the :ref:`Scraping rules <scraping>` section.

Testing fetching of HTML (and PDF) documents and selecting from them can be done with the :ref:`CLI operation <print_a_web_page>` ``-fetch-webpage-selector``.

.. _cleaning:

Cleaning the returned HTML document
===================================

If no :ref:`selectors <selecting>` are specified for the given HTML document, then automatic cleaning and formatting of the document will be done instead.

The purpose of cleaning is to only extract the main content, while discarding auxiliary content, like menus and other navigational elements, footers, search and login forms, social links, contents of ``<noscript>``, publication references, etc. We clean the document by deleting such elements and their children. The elements are found by tag names (for example ``<nav>`` or ``<footer>``), but also their IDs, class names and `ARIA <https://www.w3.org/WAI/standards-guidelines/aria/>`_ roles are matched with combinations of keywords. Some words (like "menu" or "navbar") are good enough to outright delete the matched element, either matching it by itself or with a specifier (like "left" or "main") or in combination with another word (like "tab" or "links"). Other words (like the mentioned "tab" and "links", but also "bar", "search", "hidden", etc), either by themselves or combined with specifiers, are not specific enough to delete the matched element without some extra confidence. So, for these words and combinations there is the extra condition that no children or parents of the matched element can be an element that we determine to be about the main content (``<main>``, ``<article>``, ``<h1>``, "content", "heading", etc).

After this cleaning has been done, the remaining text will be extracted from the document and formatted. Paragraphs and other blocks of text will be separated by empty lines in the output. If any text is found in the description ``<meta>`` tag, then it will be prepended to the output.

.. _multithreaded:

Multithreaded fetching
======================

Only one thread should be filling one publication or one webpage or one doc. But many threads can be filling different :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` in parallel. If many of these threads depend on the same resources, then what can happen is many parallel connections to the same host. To avoid such hammering, locking is implemented around each connection such that only one connection to one host is allowed at once (comparison of hosts is done case-insensitively and "www." is removed). Other threads wanting to connect to the same host will have to wait until the resource is free again.

*********************
Fetching publications
*********************

.. _resources:

Resources
=========

Unfortunately, all content pertaining to a :ref:`publication <content_of_publications>` is not available from one sole Internet resource. Therefore, a number of resources are consulted and the final publication might contain content from different resources, for example an abstract from one place and the full text from another.

What follows is a list of these resources. They are defined in the order they are tried: if after fetching a given resource all required `publication parts`_ become :ref:`final <final>`, or none of the subsequent resources can fill the missing parts, then the resources below the given resource are not fetched from.

But, if after going through all the resources below (as necessary) more IDs about the publication are known than before consulting the resources, then another run through all the resources is done, starting from the first (as knowing a new ID might enable us to query a resource that couldn't be queried before). In doing this we are keeping track of resources that have successfully been fetched to not fetch these a second time and of course, for each resource, we are still evaluating if the resource can provide us with anything useful before fetching is attempted.

Sometimes, publication IDs can change, e.g., when we find from a resource with better type (see `Publication types`_) that the DOI of the publication is different than what we currently have. In such cases all publication content (except IDs) is emptied and fetching restarted from scratch.

.. _europe_pmc:

Europe PMC
----------

`Europe PubMed Central <https://europepmc.org/>`_ is a repository containing, among other things, abstracts, full text and preprints of biomedical and life sciences articles. It is the primary resource used by PubFetcher and a majority of content can be obtained from there.

The endpoint of the API is https://www.ebi.ac.uk/europepmc/webservices/rest/search, documentation is at https://europepmc.org/RestfulWebService. The API accepts any of the publication IDs: either a PMID_, a PMCID_ or a DOI_. With parameter :ref:`europepmcEmail <europepmcemail>` an e-mail address can be supplied to the API.

We can possibly get all `publication parts`_ from the Europe PMC API, except for fulltext_, efo_ and go_ for which we get a ``Y`` or ``N`` indicating if the corresponding part is available at the `Europe PMC fulltext`_ or `Europe PMC mined`_ resource. In addition, we can possibly get values for the publication fields :ref:`oa <oa>`, :ref:`journalTitle <journaltitle>`, :ref:`pubDate <pubdate>` and :ref:`citationsCount <citationscount>`. Europe PMC is currently the only resource we can get the :ref:`citationsCount <citationscount>` value from.

Europe PMC itself has content from multiple sources (see https://europepmc.org/Help#contentsources) and in some cases multiple results are returned for a query (each from a different source). In that case the MED (MEDLINE) source is preferred, then PMC (PubMed Central), then PPR (preprints) and then whichever source is first in the list of results.

.. _europe_pmc_fulltext:

Europe PMC fulltext
-------------------

Full text from the `Europe PMC`_ API is obtained from a separate endpoint: https://www.ebi.ac.uk/europepmc/webservices/rest/{PMCID}/fullTextXML. The PMCID_ of the publication must be known to query the API.

The API is primarily meant for getting the fulltext_, but it can also be used to get the parts pmid_, pmcid_, doi_, title_, keywords_, theAbstract_ if these were requested and are still non-:ref:`final <final>` (for some reason not obtained from the main resource of `Europe PMC`_). In addition, :ref:`journalTitle <journaltitle>` and :ref:`correspAuthor <correspauthor>` can be obtained.

.. _europe_pmc_mined:

Europe PMC mined
----------------

Europe PMC has text-mined terms from publication full texts. Such EFO terms can be obtained from https://www.ebi.ac.uk/europepmc/webservices/rest/PMC/{PMCID}/textMinedTerms/EFO or https://www.ebi.ac.uk/europepmc/webservices/rest/MED/{PMID}/textMinedTerms/EFO and GO terms can be obtained from the same URLs where "EFO" is replaced with "GO_TERM". These resources are the only way to fill the `publication parts`_ efo_ and go_ and only those publication parts can be obtained from these resources. Either a PMID_ or a PMCID_ is required to query these resources.

.. _pubmed_xml:

PubMed XML
----------

The `PubMed <https://www.ncbi.nlm.nih.gov/pubmed/>`_ resource is used to access abstracts of biomedical and life sciences literature from the MEDLINE database.

The following URL is used for retrieving data in XML format for an article: https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?retmode=xml&db=pubmed&id={PMID}. As seen, a PMID_ is required to query the resource. Documentation is at https://www.ncbi.nlm.nih.gov/books/NBK25500/.

In addition to theAbstract_, the `publication parts`_ pmid_, pmcid_, doi_, title_ and mesh_ can possibly be obtained from PubMed. Also, the publication part keywords_ can seldom be obtained, but if keywords_ is the only still missing publication part, then the resource is not fetched (instead, `PubMed Central`_ is relied upon for keywords_). In addition, we can possibly get values for the publication fields :ref:`journalTitle <journaltitle>` and :ref:`pubDate <pubdate>`.

.. _pubmed_html:

PubMed HTML
-----------

Information from PubMed can be ouput in different formats, including in HTML (to be viewed in the browser) from the URL: https://www.ncbi.nlm.nih.gov/pubmed/?term={PMID}. By scraping the resultant page we can get the same `publication parts`_ as from the XML obtained through PubMed E-utilities, however the HTML version of PubMed is only fetched if by that point title_ or theAbstract_ are still non-:ref:`final <final>` (i.e., `PubMed XML`_, but also `Europe PMC`_, failed to fetch these for some reason). So this is more of a redundant resource, that is rarely used and even more rarely useful.

.. _pubmed_central:

PubMed Central
--------------

`PubMed Central <https://www.ncbi.nlm.nih.gov/pmc/>`_ contains full text articles, which can be obtained in XML format from the URL: https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?retmode=xml&db=pmc&id={PMCID}, where {PMCID} is the PMCID_ of the publication with the "PMC" prefix removed.

It is analogous to `Europe PMC fulltext`_ and used as a backup to that resource for getting content for articles available in the PMC system.

.. _doi_resource:

DOI resource
------------

Sometimes, some `publication parts`_ must be fetched directly from the publisher. A DOI_ (Digital Object Identifier) of a publication is a persistent identifier which, when resolved, should point to the correct URL of the journal article.

First, the DOI is resolved to the URL it redirects to and this URL is fed to the `Getting a HTML document`_ method. If the URL has a match in the JavaScript section of the :ref:`Journals YAML <journals_yaml>` scraping configuration, then the HTML document will be fetched using JavaScript support. The publication parts that can possibly be scraped from the article's page are doi_, title_, keywords_, theAbstract_, fulltext_ and possibly (but very rarely) pmid_ and pmcid_. These publication parts are extracted from the web page using corresponding :ref:`scraping rules <scraping>`. If no :ref:`scraping rules <scraping>` are found, then the content of the HTML ``<title>`` element will be set as the value of the publication part title_ (if title_ is still non-:ref:`final <final>`) and the whole text of the HTML set as the value of fulltext_ (if fulltext_ is still non-:ref:`final <final>`). Additionally, a link to the web page containing the full text of the article and a link pointing to the article PDF might be added to `Links`_, if specified by the :ref:`scraping rules <scraping>`, and in addition names and e-mails for :ref:`correspAuthor <correspauthor>` can be found.

In contrast to the other resources, ``<meta>`` elements are looked for in the HTML as these might contain the publication parts pmid_, pmcid_, doi_, title_, keywords_ and also theAbstract_, plus `Links`_ to additional web pages or PDFs containing the article and sometimes also e-mail addresses for :ref:`correspAuthor <correspauthor>`. More about these meta tags is described in `Meta`_.

Also, in contrast to other resources, the final URL resolved from the DOI is added to :ref:`visitedSites <visitedsites>`.

.. _unpaywall:

Unpaywall
---------

The Unpaywall service helps to find Open Access content. It us mainly useful for finding PDFs for some articles for which no full text content was found using the above resources, but it can help in filling a few other `publication parts`_ and fields also, such as :ref:`oa <oa>`. The service was recently called oaDOI.

The API is queried as follows: https://api.unpaywall.org/v2/{DOI}?email={:ref:`oadoiEmail <oadoiemail>`}, documentation is at https://unpaywall.org/products/api. As seen, the DOI_ of the publication must be known to query the service.

The response will be in JSON format, which is why the method of `Getting a HTML document`_ is not used (but the process of obtaining the resource is analogous). Unpaywall will be called if title_, theAbstract_ of fulltext_ are non-:ref:`final <final>` (or pmid_, pmcid_, doi_ are non-:ref:`final <final>`, but only if these are the only publication parts requested). From the response we can possibly directly fill the publication part title_ and the fields :ref:`oa <oa>` and :ref:`journalTitle <journaltitle>`. But in addition we can find `Links`_ to web pages containing the article or to PDFs of the article.

.. _meta:

Meta
----

The web pages of journal articles can have metadata embedded in the HTML in ``<meta>`` elements. Sometimes this can be used to fill `publication parts`_ which have not been found elsewhere.

There are a few standard meta tag formats, those supported by PubFetcher are: HighWire, EPrints, bepress, Dublin Core, Open Graph, Twitter and generic tag (without any prefix). An example of a HighWire tag: ``<meta name="citation_keyword" content="foo">``. An example of a Open Graph tag: ``<meta property="og:title" content="bar" />``.

Publication parts potentially found in ``<meta>`` elements (depending on format) are: pmid_, pmcid_, doi_, title_, keywords_, theAbstract_. Additionally, `Links`_ to web pages containing the article or to PDFs of the article can be found in some meta tags.

In web pages of articles of some journals the standard ``<meta>`` tags are filled with content that is not entirely correct (for our purposes), so some exceptions to not use these tags for these journals have been defined.

``<meta>`` elements are only searched for in all web pages resolved from DOI and also in all web pages added to `Links`_.

.. _links:

Links
-----

Links to web pages containing an article or to PDFs of the article can be found in the `Unpaywall`_ resource, in some `Meta`_ tags and in web pages (resolved from `DOI`_ or from `Links`_) that have :ref:`scraping rules <scraping>` specifying how to extract links. In addition to its URL, a publication type (see `Publication types`_) corresponding to the resource the link was found from, the URL of the web page the link was found from and a timestamp, are saved for each link.

These links are collected in a list that will be looked through only after all other resources above have been exhausted. DOI_ links (with host "doi.org" or "dx.doi.org") and links to web pages of articles in the PMC system (either Europe PMC or PubMed Central) are not added to this list. But, in case of PMC links, a missing PMCID_ (or PMID_) of the publication can sometimes be extracted from the URL string itself. In addition, links that have already been tried or links already present in the list are not added to the list a second time.

Links are sorted according to `publication types`_ in the list they are collected to, with links of :ref:`final <final>` type on top. Which means, that once fetching of resources has reached this list of links then links of higher types are visited first. If `publication parts`_ title_, keywords_, theAbstract_ and fulltext_ are :ref:`final <final>` or with types that are better or equal to types of any of the remaining links in the list, then the remaining links are discarded.

In case of links to web pages the content is fetched and the publication is filled the same way as in the `DOI resource`_ (including the addition of the link to :ref:`visitedSites <visitedsites>`), except the resolving of the DOI to URL step is not done (the supplied URL of the link is treated the same as a URL resolved from a DOI). In case of links to PDFs the content is fetched and the publication is filled as described in `Getting a PDF document`_.

.. _publication_types:

Publication types
=================

Publication part types are the following, ordered from better to lower type:

europepmc
  Type given to parts got from `Europe PMC`_ and `Europe PMC mined`_ resources
europepmc_xml
  From `Europe PMC fulltext`_ resource
europepmc_html
  Currently disabled
pubmed_xml
  From `PubMed XML`_ resource
pubmed_html
  From `PubMed HTML`_ resource
pmc_xml
  From `PubMed Central`_ resource
pmc_html
  Currently disabled
doi
  From `DOI resource`_ (excluding PDF links)
link
  Link to publication. Not used in PubFetcher itself. Meant as an option in applications extending or using PubFetcher.
link_oadoi
  Given to `Links`_ found in Unpaywall_ resource (excluding PDF links)
citation
  From HighWire Meta_ tags (excluding links)
eprints
  From EPrints Meta_ tags (excluding links)
bepress
  From bepress Meta_ tags (excluding PDF links)
link_citation
  Links_ from Highwire Meta_ tags (excluding PDF links)
link_eprints
  Links_ from EPrints Meta_ tags (excluding PDF links)
dc
  From Dublin Core Meta_ tags
og
  From Open Graph Meta_ tags
twitter
  From Twitter Meta_ tags
meta
  From generic Meta_ tags (excluding links)
link_meta
  Links_ from generic Meta_ tags (excluding PDF links)
external
  Type given to externally supplied pmid_, pmcid_ or doi_
oadoi
  From Unpaywall_ resource (excluding links, currently only title_)
pdf_europepmc
  Currently disabled
pdf_pmc
  Currently disabled
pdf_doi
  Type given to PDF Links_ extracted from a `DOI resource`_ or if the DOI itself resolves to a PDF file (which is fetched as described in `Getting a PDF document`_)
pdf_link
  PDF from link to publication. Not used in PubFetcher itself. Meant as an option in applications extending or using PubFetcher.
pdf_oadoi
  PDF Links_ from Unpaywall_ resource
pdf_citation
  PDF Links_ from HighWire Meta_ tags
pdf_eprints
  PDF Links_ from EPrints Meta_ tags
pdf_bepress
  PDF Links_ from bepress Meta_ tags
pdf_meta
  PDF Links_ from generic Meta_ tags
webpage
  Type given to title_ and fulltext_ set from an article web page with no :ref:`scraping rules <scraping>`
na
  Initial type of a publication part

Types "europepmc", "europepmc_xml", "europepmc_html", "pubmed_xml", "pubmed_html", "pmc_xml", "pmc_html", "doi", "link" and "link_oadoi" are final types. Final types are the best type and they are equivalent with each other (meaning that one final type is not better than some other final type and their ordering does not matter).

The type of the publication part being final is a necessary condition for the publication part to be :ref:`final <final>`. The other condition is for the publication part to be large enough (as specified by :ref:`titleMinLength <titleminlength>`, :ref:`keywordsMinSize <keywordsminsize>`, :ref:`minedTermsMinSize <minedtermsminsize>`, :ref:`abstractMinLength <abstractminlength>` or :ref:`fulltextMinLength <fulltextminlength>` in :ref:`fetching <fetching>` parameters). The fulltext_ part has the additional requirement of being better than "webpage" type to be considered :ref:`final <final>`.

When filling a publication part then the type of the new content must be better than the type of the old content. Or, if both types are final but the publication part itself is not yet :ref:`final <final>` (because the content is not large enough), then new content will override old content if new content is larger. Publication parts which are :ref:`final <final>` can't be overwritten. Also, the publication fields (these are not publication parts) :ref:`journalTitle <journaltitle>`, :ref:`pubDate <pubdate>` and :ref:`correspAuthor <correspauthor>` can only be set once with non-empty content, after which they can't be overwritten anymore.

.. _publication_parts:

Publication parts
=================

:ref:`publication <content_of_publications>` parts have :ref:`content <content>` and contain the fields :ref:`type <type>`, :ref:`url <url>` and :ref:`timestamp <timestamp>` as described in the :ref:`JSON output <json_output>` of the publication part pmid_. The publication fields :ref:`oa <oa>`, :ref:`journalTitle <journaltitle>`, :ref:`pubDate <pubdate>`, etc do not contain extra information besides content and are not publication parts.

The publication parts are as follows:

_`pmid`
  .. _fetcher_pmid:

  The PubMed ID of the publication. Only articles available in PubMed can have this. Only a valid PMID can be set to the part. The :ref:`pmid structure <pmid>`.
_`pmcid`
  .. _fetcher_pmcid:

  The PubMed Central ID of the publication. Only articles available in PMC can have this. Only a valid PMCID can be set to the part. The :ref:`pmcid structure <pmcid>`.
_`doi`
  .. _fetcher_doi:

  The Digital Object Identifier of the publication. Only a valid DOI can be set to the part. The DOI will be normalised in the process, i.e. any valid prefix (e.g. "https://doi.org/", "doi:") is removed and letters from the 7-bit ASCII set are converted to uppercase. The :ref:`doi structure <doi>`.
_`title`
  .. _fetcher_title:

  The title of the publication.  The :ref:`title structure <title>`.
_`keywords`
  .. _fetcher_keywords:

  Author-assigned keywords of the publication. Often missing or not found. Empty and duplicate keywords are removed. The :ref:`keywords structure <keywords>`.
_`mesh`
  .. _fetcher_mesh:

  `Medical Subject Headings <https://www.nlm.nih.gov/mesh/>`_ terms of the publication. Assigned to articles in PubMed (with some delay after publication). The :ref:`mesh structure <mesh>`.
_`efo`
  .. _fetcher_efo:

  `Experimental factor ontology <https://www.ebi.ac.uk/efo/>`_ terms of the publication. Text-mined by the `Europe PMC <https://europepmc.org/>`_ project from the full text of the article. The :ref:`efo structure <efo>`.
_`go`
  .. _fetcher_go:

  `Gene ontology <http://geneontology.org/>`_ terms of the publication. Text-mined by the `Europe PMC <https://europepmc.org/>`_ project from the full text of the article. The :ref:`go structure <go>`.
_`theAbstract`
  .. _fetcher_theAbstract:

  The abstract of the publication. The part is called "theAbstract" instead of just "abstract", because "abstract" is a reserved keyword in the Java programming language. The :ref:`abstract structure <abstract>`.
_`fulltext`
  .. _fetcher_fulltext:

  The full text of the publication. The part includes the title and abstract of the publication in the beginning of the content string. All the main content of the article's full text is included, from introduction to conclusions. Captions of figures and tables and descriptions of supplementary materials are also included. From back matter, the glossary, notes and misc sections are usually included. But acknowledgements, appendices, biographies, footnotes, copyrights and, most importantly, references are excluded, whenever possible. If fulltext is obtained from a PDF, then everything is included. In the future, it could be useful to include all these parts of full text, like references, but in a structured way. The :ref:`fulltext structure <fulltext>`.

.. _fetching_webpages_and_docs:

**************************
Fetching webpages and docs
**************************

A :ref:`webpage <content_of_webpages>` or :ref:`doc <content_of_docs>` is also got using the method described in `Getting a HTML document`_ (or `Getting a PDF document`_ if the webpage or doc URL turns out to be a link to a PDF file). Webpage and doc fields that can be filled from the fetched content using :ref:`scraping rules <scraping>` are the :ref:`webpage title <webpage_title>`, the :ref:`webpage content <webpage_content>`, :ref:`license <license>` and :ref:`language <language>`. Other fields are filled with metadata during the fetching process, the whole structure can be seen in :ref:`webpages <webpages>` section of the output documentation. If no :ref:`scraping rules <scraping>` are present for the webpage or doc then the :ref:`webpage content <webpage_content>` will be the entire string parsed from the fetched HTML and the :ref:`webpage title <webpage_title>` will be the content inside the ``<title>`` tag. Whether the webpage or doc is fetched with JavaScript support or not can also be influenced with :ref:`scraping rules <scraping>`. A webpage or doc can also be fetch using rules specified on the command line with the command ``-fetch-webpage-selector`` (see :ref:`Print a web page <print_a_web_page>`).

The same publication can be fetched multiple times, with each fetching potentially adding some missing content to the existing publication. In contrast, a webpage or doc is always fetched from scratch. If the resulting :ref:`webpage or doc is final <webpage_final>` and a corresponding webpage or doc already exists, then this existing entry will be overwritten. An existing webpage or doc will also be overwritten, if the new entry is non-final (but not empty) and the old entry is non-final (and potentially empty) and if both new and old entries are empty.

.. _can_fetch:

*********
Can fetch
*********

The methods for fetching :ref:`publications <publications>`, :ref:`webpages <webpages>` and :ref:`docs <docs>` are always given a publication, webpage or doc as parameter. If a publication, webpage or doc is fetched from scratch, then an initial empty entry is supplied. Each time, these methods have to determine if a publication, webpage or doc can be fetched or should the fetching be skipped this time. The fetching will happen if any of the following conditions is met:

* :ref:`fetchTime <fetchtime>` is ``0``, this is only true for initial empty entries;
* the :ref:`publication is empty <publication_empty>` or the :ref:`webpage or doc is empty <webpage_empty>` and :ref:`emptyCooldown <emptycooldown>` is not negative and at least :ref:`emptyCooldown <emptycooldown>` minutes have passed since :ref:`fetchTime <fetchtime>`;
* the :ref:`publication is final <publication_final>` or the :ref:`webpage or doc is final <webpage_final>` (and they are not empty) and :ref:`nonFinalCooldown <nonfinalcooldown>` is not negative and at least :ref:`nonFinalCooldown <nonfinalcooldown>` minutes have passed since :ref:`fetchTime <fetchtime>`;
* the entry has a :ref:`fetchException <fetchexception>` and :ref:`fetchExceptionCooldown <fetchexceptioncooldown>` is not negative and at least :ref:`fetchExceptionCooldown <fetchexceptioncooldown>` minutes have passed since :ref:`fetchTime <fetchtime>`;
* the entry is empty or non-final or has a :ref:`fetchException <fetchexception>` and either :ref:`retryCounter <retrycounter>` is less than :ref:`retryLimit <retrylimit>` or :ref:`retryLimit <retrylimit>` is negative.

If it was determined that fetching happens, then :ref:`fetchTime <fetchtime>` is set to the current time and :ref:`retryCounter <retrycounter>` is reset to ``0`` if any condition except the last is met. If only the last condition (about :ref:`retryCounter <retrycounter>` and :ref:`retryLimit <retrylimit>`) is met, then :ref:`retryCounter <retrycounter>` is incremented by ``1`` (and :ref:`fetchTime <fetchtime>` is left as is, meaning that :ref:`fetchTime <fetchtime>` does not necessarily show the time of the last fetching, but only the time of the initial fetching or the time when fetching happened because one of the cooldown timers expired).

The :ref:`fetchException <fetchexception>` is set to ``false`` in the beginning of each fetching and it is set to ``true`` if some certain types of errors happen during fetching, some such error conditions are described in `Getting a HTML document`_. :ref:`fetchException <fetchexception>` can be set to ``true`` also by the method described in `Getting a PDF document`_ and the custom method getting the `Unpaywall`_ resource.
