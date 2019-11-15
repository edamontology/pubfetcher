
.. _api:

#####################
Programming reference
#####################

In addition to command line usage, documented in the section :ref:`Command-line interface manual <cli>`, PubFetcher can be used as a library. This section is a short overview of the public interface of the source code that constitutes PubFetcher. Documentation in the code itself is currently sparse.

*****************************************************************************************************************************************************
Package `pubfetcher.core.common <https://github.com/edamontology/pubfetcher/tree/master/core/src/main/java/org/edamontology/pubfetcher/core/common>`_
*****************************************************************************************************************************************************

`BasicArgs <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/BasicArgs.java>`_ is the abstract class used as base class for FetcherArgs and FetcherPrivateArgs and other command line argument classes in "org.edamontology" packages that use `JCommander <http://jcommander.org/>`_ for command line argument parsing and `Log4J2 <https://logging.apache.org/log4j/2.x/>`_ for logging. It provides the ``-h``/``--help`` and ``-l``/``--log`` keys and functionality.

`FetcherArgs <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/FetcherArgs.java>`_ and `FetcherPrivateArgs <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/FetcherPrivateArgs.java>`_ are classes encapsulating the parameters described in :ref:`Fetching <fetching>` and :ref:`Fetching private <fetching_private>`. `Arg <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/Arg.java>`_ and `Args <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/Args.java>`_ are used to store properties of each parameter, like the default value or description string (this comes in useful in `EDAMmap <https://github.com/edamontology/edammap>`_, where parameters, including fetching parameters, are displayed and controllable by the user).

`IllegalRequestException <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/IllegalRequestException.java>`_ is a custom Java runtime exception thrown if there are problems with the user's request. The exception message can be output back to the user, for example over a web API.

`Version <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/Version.java>`_ contains the name, URL and version of the program. These are read from the project's properties file, found at the absolute resource `/project.properties <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/resources/project.properties>`_.

The main class of interest for a potential library user is however `PubFetcher <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/PubFetcher.java>`_. This class contains most of the public methods making up the PubFetcher API. Currently, it is also the only class documented using Javadoc. Some of the methods (those described in :ref:`Publication IDs <publication_ids>` and :ref:`Miscellaneous <miscellaneous>`) can be called from PubFetcher-CLI.

***************************************************************************************************************************************************************
Package `pubfetcher.core.db <https://github.com/edamontology/pubfetcher/tree/master/core/src/main/java/org/edamontology/pubfetcher/core/db>`_ (and subpackages)
***************************************************************************************************************************************************************

The `Database class <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/db/Database.java>`_ can be used to initialise a database file, put content to or get or remove content from the database file, get IDs contained or ask if an ID is contained in the database file or compact a database file. The class abstracts away the currently used underlying database system (`MapDB <http://www.mapdb.org/>`_). The structure of the database is described in the :ref:`Database section of the output documentation <database>`. Some methods can be called from PubFetcher-CLI, these are described in the corresponding :ref:`Database section <cli_database>`.

`DatabaseEntry <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/db/DatabaseEntry.java>`_ is the base class for Publication and Webpage. It contains the methods "canFetch" and "updateCounters" whose logic is explained in :ref:`Can fetch <can_fetch>`. `DatabaseEntryType <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/db/DatabaseEntryType.java>`_ specifies whether a given DatabaseEntry is a :ref:`publication <content_of_publications>`, :ref:`webpage <content_of_webpages>` or :ref:`doc <content_of_docs>`.

`Publication <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/db/publication/Publication.java>`_, `Webpage <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/db/webpage/Webpage.java>`_ and most other classes in the "pubfetcher.core.db" packages are the entities stored in the database. These classes contain methods to get and set the value of their fields and methods to output content fields in plain text, HTML or JSON, with or without metadata fields. Their structure is explained in :ref:`Contents <contents>`.

The `PublicationIds class <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/db/publication/PublicationIds.java>`_ encapsulates publication IDs that can be stored in the database. Its structure is explained in :ref:`IDs of publications <ids_of_publications>`.

The `PublicationPartType enumeration <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/db/publication/PublicationPartType.java>`_ of possible publication types is explained in :ref:`Publication types <publication_types>`.

*********************************************************************************************************************************************************
Package `pubfetcher.core.fetching <https://github.com/edamontology/pubfetcher/tree/master/core/src/main/java/org/edamontology/pubfetcher/core/fetching>`_
*********************************************************************************************************************************************************

`Fetcher <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/fetching/Fetcher.java>`_ is the main class dealing with fetching. Its logic is explained in :ref:`Fetching logic <fetcher>`.

Fetcher contains the public method "getDoc", which is described in :ref:`Getting a HTML document <getting_a_html_document>`. The "getDoc" method, but also the "getWebpage" method and the "updateCitationsCount" method can be called from PubFetcher-CLI as seen in :ref:`Print a web page <print_a_web_page>` and :ref:`Update citations count <update_citations_count>`.

The Fetcher methods "initPublication" and "initWebpage" must be used to construct a Publication and Webpage. Then, the methods "getPublication" and "getWebpage" can be used to fetch the Publication and Webpage. But instead of these "init" and "get" methods, the "getPublication", "getWebpage" and "getDoc" methods of class `PubFetcher <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/common/PubFetcher.java>`_ should be used, when possible.

Because executing JavaScript is prone to serious bugs in the used `HtmlUnit <http://htmlunit.sourceforge.net/>`_ library, fetching a HTML document with JavaScript support turned on is done in a separate `JavaScriptThread <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/fetching/JavaScriptThread.java>`_, that can be killed if it gets stuck.

The `HtmlMeta class <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/fetching/HtmlMeta.java>`_ is explained in :ref:`Meta <meta>` and the `Links class <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/fetching/Links.java>`_ in :ref:`Links <links>`.

Automatic :ref:`cleaning and formatting <cleaning>` of web pages without :ref:`scraping rules <scraping>` has been implemented in the `CleanWebpage class <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/fetching/CleanWebpage.java>`_.

The "pubfetcher.core.fetching" package also contains the classes related to testing: `FetcherTest <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/fetching/FetcherTest.java>`_ and `FetcherTestArgs <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/fetching/FetcherTestArgs.java>`_. These are explained in :ref:`Testing of rules <testing_of_rules>`.

*****************************************************************************************************************************************************
Package `pubfetcher.core.scrape <https://github.com/edamontology/pubfetcher/tree/master/core/src/main/java/org/edamontology/pubfetcher/core/scrape>`_
*****************************************************************************************************************************************************

Classes in this package deal with scraping, as explained in the :ref:`Scraping rules <scraping>` section.

The public methods of the `Scrape class <https://github.com/edamontology/pubfetcher/blob/master/core/src/main/java/org/edamontology/pubfetcher/core/scrape/Scrape.java>`_ can be called from PubFetcher-CLI using the parameters shown in :ref:`Scrape rules <scrape_rules>`.

************************************************************************************************************************************
Package `pubfetcher.cli <https://github.com/edamontology/pubfetcher/tree/master/cli/src/main/java/org/edamontology/pubfetcher/cli>`_
************************************************************************************************************************************

The command line interface of PubFetcher, that is PubFetcher-CLI, is implemented in package "pubfetcher.cli". Its usage is the topic of the first section :ref:`Command-line interface manual <cli>`.

.. _cli_extended:

The functionality of PubFetcher-CLI can be **extended** by implementing new operations in a new command line tool, where the public "run" method of the `PubFetcherMethods class <https://github.com/edamontology/pubfetcher/blob/master/cli/src/main/java/org/edamontology/pubfetcher/cli/PubFetcherMethods.java>`_ can then be called to pull in all the functionality of PubFetcher-CLI. One of the main reasons to do this is to implement some new way of getting publication IDs and webpage/doc URLs. These IDs and URLs can then be passed to the "run" method of PubFetcherMethods as the lists "externalPublicationIds", "externalWebpageUrls" and "externalDocUrls". One example of such functionality extension is the `EDAMmap-Util <https://github.com/edamontology/edammap/tree/master/util>`_ tool (see its `UtilMain class <https://github.com/edamontology/edammap/blob/master/util/src/main/java/org/edamontology/edammap/util/UtilMain.java>`_).

********************************************************************************************************************************
Configuration `resources/log4j2.xml <https://github.com/edamontology/pubfetcher/blob/master/cli/src/main/resources/log4j2.xml>`_
********************************************************************************************************************************

The PubFetcher-CLI :ref:`Logging <logging>` configuration file `log4j2.xml <https://github.com/edamontology/pubfetcher/blob/master/cli/src/main/resources/log4j2.xml>`_ specifies how logging is done and how the :ref:`Log file <log_file>` will look like.
