package org.edamontology.pubfetcher.core.fetching;

import com.beust.jcommander.Parameter;

public class FetcherTestArgs {

	@Parameter(names = { "--print-europepmc-xml" }, description = "TODO")
	String printEuropepmcXml = null;

	@Parameter(names = { "--test-europepmc-xml" }, description = "TODO")
	boolean testEuropepmcXml = false;

	@Parameter(names = { "--print-europepmc-html" }, description = "TODO")
	String printEuropepmcHtml = null;

	@Parameter(names = { "--test-europepmc-html" }, description = "TODO")
	boolean testEuropepmcHtml = false;

	@Parameter(names = { "--print-pmc-xml" }, description = "TODO")
	String printPmcXml = null;

	@Parameter(names = { "--test-pmc-xml" }, description = "TODO")
	boolean testPmcXml = false;

	@Parameter(names = { "--print-pmc-html" }, description = "TODO")
	String printPmcHtml = null;

	@Parameter(names = { "--test-pmc-html" }, description = "TODO")
	boolean testPmcHtml = false;

	@Parameter(names = { "--print-pubmed-xml" }, description = "TODO")
	String printPubmedXml = null;

	@Parameter(names = { "--test-pubmed-xml" }, description = "TODO")
	boolean testPubmedXml = false;

	@Parameter(names = { "--print-pubmed-html" }, description = "TODO")
	String printPubmedHtml = null;

	@Parameter(names = { "--test-pubmed-html" }, description = "TODO")
	boolean testPubmedHtml = false;

	@Parameter(names = { "--print-europepmc" }, description = "TODO")
	String printEuropepmc = null;

	@Parameter(names = { "--test-europepmc" }, description = "TODO")
	boolean testEuropepmc = false;

	@Parameter(names = { "--print-europepmc-mined" }, description = "TODO")
	String printEuropepmcMined = null;

	@Parameter(names = { "--test-europepmc-mined" }, description = "TODO")
	boolean testEuropepmcMined = false;

	@Parameter(names = { "--print-oadoi" }, description = "TODO")
	String printOaDoi = null;

	@Parameter(names = { "--test-oadoi" }, description = "TODO")
	boolean testOaDoi = false;

	@Parameter(names = { "--print-site" }, description = "TODO")
	String printSite = null;

	@Parameter(names = { "--test-site" }, description = "TODO")
	boolean testSite = false;

	@Parameter(names = { "--test-site-regex" }, description = "TODO")
	String testSiteRegex = null;

	@Parameter(names = { "--print-webpage" }, description = "TODO")
	String printWebpage = null;

	@Parameter(names = { "--test-webpage" }, description = "TODO")
	boolean testWebpage = false;

	@Parameter(names = { "--test-webpage-regex" }, description = "TODO")
	String testWebpageRegex = null;
}
