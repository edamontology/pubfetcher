
.. _future:

################
Ideas for future
################

Sometimes ideas are emerging. These are written down here for future reference. A written down idea is not necessarily a good idea, thus not all points here should be implemented.

*****************
Structure changes
*****************

* Make publication :ref:`fulltext <fetcher_fulltext>` more structured (currently it is just one big string). For example, "Introduction", "Methods", etc could all be separate parts. Also, references could be added as a separate part (currently references are excluded altogether). It should be investigated, how feasible this is. For PDFs it is not, probably.
* Treat publication fields (like :ref:`oa <oa>` or :ref:`journalTitle <journaltitle>`) more akin to :ref:`publication parts <publication_parts>`. Like, :ref:`journalTitle <journaltitle>` could also be :ref:`final <final>` or not, etc.
* Additional metadata about a publication could be supported. Most importantly - authors (with first name, last name, orcid, affiliation, email, etc).
* :ref:`Webpages <content_of_webpages>` could also have an extra field about tags, for example those that occur in standard registries or code repositories. Analogous to the :ref:`keywords <keywords>` of publications.

*************
Logic changes
*************

* Currently, a publication is considered final when its title, abstract and fulltext are final. Keywords are not required for this, as they are often missing. But this means that, if we for some reason fail to fetch author-assigned keywords, or those keywords are added to the publication at some later date, then we will not try to fetch these keywords at some later date if the publication is already final. Note that, adding keywords to the finality requirement is probably still not a good idea.
* Currently, content found in meta tags of article web pages can be extracted by the Meta class (as described in :ref:`Meta <meta>`). However, all content extracted this way will have a non-final publication part type (see :ref:`Publication types <publication_types>`). As these tags are simply part of the HTML source, then for some of these tags (where we are certain of the quality of its content for the given site) the possibility to use explicit scraping rules (e.g. using a ScrapeSiteKey called "keywords_meta") in journals.yaml could be added. This way, content extracted from these tags (using a scraping rule) can have the final publication part type of "doi".

*************
Extra sources
*************

* The ouput of the Europe PMC search API has ``<fullTextUrlList>`` and PubMed has the LinkOut service with links to resources with full text available. But it should be determined if these provide extra value, i.e. find links not found with current resources.
* `DOI Content Negotiation <https://citation.crosscite.org/docs.html>`_ could be used as extra source of metadata. But again, it should be determined if this would provide extra value.
* Same for `Open Access Button <https://openaccessbutton.org/>`_.

****************
Extra extraction
****************

* Some article web pages state if the article is Open Source somewhere in the HTML (e.g., https://gsejournal.biomedcentral.com/articles/10.1186/1297-9686-44-9). So the ScrapeSiteKey "oa" could be added to extract this information using rules in journals.yaml.
* The publication field :ref:`pubDate <pubdate>` is currently extracted from the :ref:`Europe PMC <europe_pmc>` and :ref:`PubMed XML <pubmed_xml>` resources. But it could also potentially be found at :ref:`Europe PMC fulltext <europe_pmc_fulltext>` and :ref:`PubMed Central <pubmed_central>` (documentation at https://www.ncbi.nlm.nih.gov/pmc/pmcdoc/tagging-guidelines/article/tags.html#el-pubdate).
* Meta tags are currently not used to fill publication fields. E.g., the ``<meta>`` tag "citation_journal_title" could be used for :ref:`journalTitle <journaltitle>`.

********
Database
********

* With a new release of PubFetcher, the structure of the database content might change (in classes of org.edammap.pubfetcher.core.db). Currently, no database migration is supported, which means that content of existing database files will be become unreadable in such case. If that content is still required, it would need to be refetched to a new database file (created with the new version of PubFetcher). So implement support for migration of database content. Maybe through JSON.
* Is the separation of functionally equivalent webpages and docs really necessary?
* If performance or reliability of MapDB should become and issue, then alternative key-value stores, like `LMDB <https://github.com/lmdbjava/>`_ or `Chronicle-Map <https://github.com/OpenHFT/Chronicle-Map>`_ could be investigated.

********
Scraping
********

* The current quick and dirty and uniform approach for article web page scraping could be replaced with APIs for some publishers that provide one (there's a sample list at https://libraries.mit.edu/scholarly/publishing/apis-for-scholarly-resources/).
* Reuse of scraping rules from the `Zotero <https://www.zotero.org/>`_ reference management software could be attempted, either by using the JavaScript `translators <https://github.com/zotero/translators/>`_ directly or through the `translation server <https://github.com/zotero/translation-server>`_.
* Currently the CSS-like jsoup selector is used for extraction. But it has its limitations and sometimes the use of XPath could be better, for example when selecting parents is required.
* There is an extension to XPath called `OXPath <http://www.oxpath.org/>`_ which highlights another problem: more web pages might start to require some JavaScript interactions before any content can be obtained.
* The entire original web page should also be saved when scraping. Then, the web page would not need re-fetching if some scraping rules are changed or the actual source web page examined at some later date when debugging.
* The robots.txt should be respected.

****
Meta
****

* Currently, only scraping rules are tested. But proper unit testing (with JUnit for example) should also be implemented.
* Do comment more in code.
* Also, the whole API should be documented with Javadoc (currently only `PubFetcher <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/PubFetcher.java>`_ is covered).
* Make code more robust and secure.
* Do proper packaging of the code, maybe as a release ZIP, maybe including scripts for running the program. Also, take notice of the license requirements of used libraries.
* Deploy the PubFetcher library to a remote repository.

**************
Misc new stuff
**************

* Configurable proxy support for network code could be added.
* The querying capabilities of PubFetcher are rather rudimentary. Investigate if it can be improved using some existing library, like `JXPath <https://commons.apache.org/proper/commons-jxpath/>`_ or `CQEngine <https://github.com/npgall/cqengine>`_. Maybe a change in the database system would also be required.
* Maybe an interactive shell to type PubFetcher commands in could be implemented.
* A web app and API could be implemented. Look at `EDAMmap-Server <https://github.com/edamontology/edammap/tree/master/server>`_ as an example. If done, then the full text of non OA articles should probably not be exposed.
