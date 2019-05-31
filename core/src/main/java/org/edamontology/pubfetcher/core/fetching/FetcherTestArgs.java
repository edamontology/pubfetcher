package org.edamontology.pubfetcher.core.fetching;

import com.beust.jcommander.Parameter;

public class FetcherTestArgs {

	@Parameter(names = { "-print-europepmc-xml" }, description = "Fetch the publication with the given PMCID from the Europe PMC fulltext resource and output it to stdout")
	String printEuropepmcXml = null;

	@Parameter(names = { "-test-europepmc-xml" }, description = "Run all tests for the Europe PMC fulltext resource (from europepmc-xml.csv)")
	boolean testEuropepmcXml = false;

	@Parameter(names = { "-print-europepmc-html" }, description = "Fetch the publication with the given PMCID from the Europe PMC HTML resource and output it to stdout")
	String printEuropepmcHtml = null;

	@Parameter(names = { "-test-europepmc-html" }, description = "Run all tests for the Europe PMC HTML resource (from europepmc-html.csv)")
	boolean testEuropepmcHtml = false;

	@Parameter(names = { "-print-pmc-xml" }, description = "Fetch the publication with the given PMCID from the PubMed Central resource and output it to stdout")
	String printPmcXml = null;

	@Parameter(names = { "-test-pmc-xml" }, description = "Run all tests for the PubMed Central resource (from pmc-xml.csv)")
	boolean testPmcXml = false;

	@Parameter(names = { "-print-pmc-html" }, description = "Fetch the publication with the given PMCID from the PubMed Central HTML resource and output it to stdout")
	String printPmcHtml = null;

	@Parameter(names = { "-test-pmc-html" }, description = "Run all tests for the PubMed Central HTML resource (from pmc-html.csv)")
	boolean testPmcHtml = false;

	@Parameter(names = { "-print-pubmed-xml" }, description = "Fetch the publication with the given PMID from the PubMed XML resource and output it to stdout")
	String printPubmedXml = null;

	@Parameter(names = { "-test-pubmed-xml" }, description = "Run all tests for the PubMed XML resource (from pubmed-xml.csv)")
	boolean testPubmedXml = false;

	@Parameter(names = { "-print-pubmed-html" }, description = "Fetch the publication with the given PMID from the PubMed HTML resource and output it to stdout")
	String printPubmedHtml = null;

	@Parameter(names = { "-test-pubmed-html" }, description = "Run all tests for the PubMed HTML resource (from pubmed-html.csv)")
	boolean testPubmedHtml = false;

	@Parameter(names = { "-print-europepmc" }, description = "Fetch the publication with the given PMID from the Europe PMC resource and output it to stdout")
	String printEuropepmc = null;

	@Parameter(names = { "-test-europepmc" }, description = "Run all tests for the Europe PMC resource (from europepmc.csv)")
	boolean testEuropepmc = false;

	@Parameter(names = { "-print-europepmc-mined" }, description = "Fetch the publication with the given PMID from the Europe PMC mined resource and output it to stdout")
	String printEuropepmcMined = null;

	@Parameter(names = { "-test-europepmc-mined" }, description = "Run all tests for the Europe PMC mined resource (from europepmc-mined.csv)")
	boolean testEuropepmcMined = false;

	@Parameter(names = { "-print-oadoi" }, description = "Fetch the publication with the given DOI from the Unpaywall resource and output it to stdout")
	String printOaDoi = null;

	@Parameter(names = { "-test-oadoi" }, description = "Run all tests for the Unpaywall resource (from oadoi.csv)")
	boolean testOaDoi = false;

	@Parameter(names = { "-print-site" }, description = "Fetch the publication from the given article web page URL (which can be a DOI link) and output it to stdout. Fetching happens using the built-in rules in journals.yaml and custom rules specified using --journalsYaml.")
	String printSite = null;

	@Parameter(names = { "-test-site" }, description = "Run all tests written for the built-in rules journals.yaml (from journals.csv)")
	boolean testSite = false;

	@Parameter(names = { "-test-site-regex" }, description = "From all tests written for the built-in rules journals.yaml (from journals.csv), run only those whose site URL has a match with the given regular expression")
	String testSiteRegex = null;

	@Parameter(names = { "-print-webpage" }, description = "Fetch the webpage from the given URL, using the built-in rules in webpages.yaml and custom rules specified using --webpagesYaml, and output it to stdout")
	String printWebpage = null;

	@Parameter(names = { "-test-webpage" }, description = "Run all tests written for the built-in rules webpages.yaml (from webpages.csv)")
	boolean testWebpage = false;

	@Parameter(names = { "-test-webpage-regex" }, description = "From all tests written for the built-in rules webpages.yaml (from webpages.csv), run only those whose URL has a match with the given regular expression")
	String testWebpageRegex = null;
}
