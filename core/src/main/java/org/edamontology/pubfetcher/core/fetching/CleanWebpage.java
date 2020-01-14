/*
 * Copyright © 2019 Erik Jaaniso
 *
 * This file is part of PubFetcher.
 *
 * PubFetcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PubFetcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PubFetcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.edamontology.pubfetcher.core.fetching;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public final class CleanWebpage {

	private static final Logger logger = LogManager.getLogger();

	private static final Pattern WHITESPACE = Pattern.compile("[\\p{Z}\\p{Cc}\\p{Cf}]+");
	private static final Pattern SEPARATOR_CAMEL = Pattern.compile("(\\p{Ll})(\\p{Lu})");
	private static final Pattern SEPARATOR_TO_NUMBER = Pattern.compile("(\\p{L})(\\p{N})");
	private static final Pattern SEPARATOR_FROM_NUMBER = Pattern.compile("(\\p{N})(\\p{L})");
	private static final Pattern SEPARATOR = Pattern.compile("[ _-]+");

	private static final String MENU_WORDS = "nav|navigation|menu|navbar|navigationbar|menubar|breadcrumb|breadcrumbs";
	private static final String WITH_WORDS = "top|bottom|left|right|side|sub|main|site|page|tool|my";
	private static final String MAYBE_WORDS = "bar|bars|tab|tabs|accordion|linklist|links|path|search|login|social|socialmedia|pagination|complementary|secondary|aside|related|invisible|hidden|skip|jump";
	private static final Pattern MENU = Pattern.compile("(?i)^(" + MENU_WORDS + ")$");
	private static final Pattern MENU_WITH = Pattern.compile("(?i)^(((" + MENU_WORDS + "|" + WITH_WORDS + "|" + MAYBE_WORDS + ")(" + MENU_WORDS + "))|((" + MENU_WORDS + ")(" + MENU_WORDS + "|" + WITH_WORDS + "|" + MAYBE_WORDS + ")))$");
	private static final Pattern MAYBE = Pattern.compile("(?i)^(" + MAYBE_WORDS + ")$");
	private static final Pattern MAYBE_WITH = Pattern.compile("(?i)^(((" + WITH_WORDS + "|" + MAYBE_WORDS + ")(" + MAYBE_WORDS + "))|((" + MAYBE_WORDS + ")(" + WITH_WORDS + "|" + MAYBE_WORDS + ")))$");
	private static final Pattern MENU_MAYBE_BEGIN = Pattern.compile("(?i)^((" + MENU_WORDS + ")|((" + WITH_WORDS + "|" + MAYBE_WORDS + ")(" + MENU_WORDS + ")))");
	private static final Pattern MENU_MAYBE_END = Pattern.compile("(?i)(((" + MENU_WORDS + ")(" + WITH_WORDS + "|" + MAYBE_WORDS + "))|(" + MENU_WORDS + "))$");
	private static final Pattern MENU_TAG = Pattern.compile("(?i)^(nav|menu|aside)$");
	private static final Pattern MENU_ARIA = Pattern.compile("(?i)^(navigation|menu|menubar|menuitem|complementary)$");
	private static final Pattern MAYBE_ARIA = Pattern.compile("(?i)^(tab|search)$");
	private static final Pattern FOOTER = Pattern.compile("(?i)^(footer)$");
	private static final Pattern FOOTER_TAG = Pattern.compile("(?i)^(footer)$");
	private static final Pattern FOOTER_ARIA = Pattern.compile("(?i)^(contentinfo)$");
	private static final Pattern MAIN = Pattern.compile("(?i)^[^\\p{L}]*(main|maincontent|maincontents|content|contents|article)[^\\p{L}]*$");
	private static final Pattern MAIN_TAG = Pattern.compile("(?i)^(main|article)$");
	private static final Pattern MAIN_ARIA = Pattern.compile("(?i)^(main|article)$");
	private static final Pattern TITLE = Pattern.compile("(?i)^[^\\p{L}]*(title|heading)[^\\p{L}]*$");
	private static final Pattern TITLE_TAG = Pattern.compile("(?i)^(h1|h2)$");
	private static final Pattern JAVASCRIPT = Pattern.compile("(?i)javascript");
	private static final String PUBLICATION_WORDS = "ref|refs|reflist|reference|references|related";
	private static final Pattern PUBLICATION = Pattern.compile("(?i)(^" + PUBLICATION_WORDS + ")|(" + PUBLICATION_WORDS + "$)");

	// https://www.w3.org/TR/CSS2/sample.html
	// https://www.w3.org/TR/html5/rendering.html
	private static final List<String> marginTags = Arrays.asList("blockquote", "body", "dir", "dl", "fieldset", "figure", "form", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "listing", "menu", "ol", "p", "plaintext", "pre", "ul", "xmp");

	private static final String DESCRIPTION_SELECTOR = HtmlMeta.selectorCombinations("description") + HtmlMeta.selectorCombinations("meta.description") + HtmlMeta.selectorCombinations("dc.description") + ", " + HtmlMeta.selectorCombinations("og.description") + HtmlMeta.selectorCombinations("twitter.description");

	private static String[] separate(String names) {
		if (names.isEmpty()) return new String[] {};
		names = WHITESPACE.matcher(names).replaceAll(" ");
		names = names.trim();
		names = SEPARATOR_CAMEL.matcher(names).replaceAll("$1 $2");
		names = SEPARATOR_TO_NUMBER.matcher(names).replaceAll("$1 $2");
		names = SEPARATOR_FROM_NUMBER.matcher(names).replaceAll("$1 $2");
		return SEPARATOR.split(names);
	}

	private static boolean isMain(Element element, boolean alsoTitle) {
		if (MAIN.matcher(element.id()).find()) {
			return true;
		}
		if (alsoTitle) {
			if (TITLE.matcher(element.id()).find()) {
				return true;
			}
		}
		if (MAIN.matcher(element.className()).find()) {
			return true;
		}
		if (alsoTitle) {
			if (TITLE.matcher(element.className()).find()) {
				return true;
			}
		}
		String tag = element.tagName();
		if (MAIN_TAG.matcher(tag).find()) {
			return true;
		}
		if (alsoTitle) {
			if (TITLE_TAG.matcher(tag).find()) {
				return true;
			}
		}
		String[] roles = separate(element.attr("role"));
		for (String role : roles) {
			if (MAIN_ARIA.matcher(role).find()) {
				return true;
			}
		}
		String[] labels = separate(element.attr("aria-label"));
		for (String label : labels) {
			if (MAIN_ARIA.matcher(label).find()) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsMain(Element element, boolean alsoTitle) {
		if (isMain(element, alsoTitle)) return true;
		for (Element child : element.children()) {
			if (containsMain(child, false)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasMain(Element element) {
		if (containsMain(element, true)) return true;
		for (Element parent : element.parents()) {
			if (isMain(parent, true)) return true;
		}
		return false;
	}

	private static boolean removeId(Element element, Pattern[] patterns, Pattern[] patternsMaybe) {
		String[] ids = separate(element.id());
		for (String id : ids) {
			for (Pattern pattern : patterns) {
				if (pattern.matcher(id).find()) {
					return true;
				}
			}
			for (Pattern patternMaybe : patternsMaybe) {
				if (patternMaybe.matcher(id).find() && !hasMain(element)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean removeClass(Element element, Pattern[] patterns, Pattern[] patternsMaybe) {
		String[] names = separate(element.className());
		for (String name : names) {
			for (Pattern pattern : patterns) {
				if (pattern.matcher(name).find()) {
					return true;
				}
			}
			for (Pattern patternMaybe : patternsMaybe) {
				if (patternMaybe.matcher(name).find() && !hasMain(element)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean removeTag(Element element, Pattern[] patterns, Pattern[] patternsMaybe) {
		String tag = element.tagName();
		for (Pattern pattern : patterns) {
			if (pattern.matcher(tag).find()) {
				return true;
			}
		}
		for (Pattern patternMaybe : patternsMaybe) {
			if (patternMaybe.matcher(tag).find() && !hasMain(element)) {
				return true;
			}
		}
		return false;
	}

	private static boolean removeAria(Element element, Pattern[] patterns, Pattern[] patternsMaybe) {
		String[] roles = separate(element.attr("role"));
		for (String role : roles) {
			for (Pattern pattern : patterns) {
				if (pattern.matcher(role).find()) {
					return true;
				}
			}
			for (Pattern patternMaybe : patternsMaybe) {
				if (patternMaybe.matcher(role).find() && !hasMain(element)) {
					return true;
				}
			}
		}
		String[] labels = separate(element.attr("aria-label"));
		for (String label : labels) {
			for (Pattern pattern : patterns) {
				if (pattern.matcher(label).find()) {
					return true;
				}
			}
			for (Pattern patternMaybe : patternsMaybe) {
				if (patternMaybe.matcher(label).find() && !hasMain(element)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean removeNoscript(Element element) {
		if (element.tagName().equals("noscript")) {
			if (JAVASCRIPT.matcher(element.text()).find()) {
				return true;
			}
		}
		return false;
	}

	private static boolean removePublication(Element element, boolean publication) {
		if (publication) {
			if (removeId(element, new Pattern[] { PUBLICATION }, new Pattern[] {})) return true;
			if (removeClass(element, new Pattern[] { PUBLICATION }, new Pattern[] {})) return true;
		}
		return false;
	}

	private static boolean remove(Element element, boolean publication) {
		if (removeId(element, new Pattern[] { MENU, MENU_WITH, FOOTER }, new Pattern[] { MAYBE, MAYBE_WITH, MENU_MAYBE_BEGIN, MENU_MAYBE_END })) return true;
		if (removeClass(element, new Pattern[] { MENU, MENU_WITH, FOOTER }, new Pattern[] { MAYBE, MAYBE_WITH, MENU_MAYBE_BEGIN, MENU_MAYBE_END })) return true;
		if (removeTag(element, new Pattern[] { MENU_TAG, FOOTER_TAG }, new Pattern[] {})) return true;
		if (removeAria(element, new Pattern[] { MENU_ARIA, FOOTER_ARIA }, new Pattern[] { MAYBE_ARIA })) return true;
		if (removeNoscript(element)) return true;
		if (removePublication(element, publication)) return true;
		return false;
	}

	private static void clean(Element element, boolean publication) {
		if (element == null) {
			logger.error("null Element given for cleaning");
			return;
		}
		if (remove(element, publication)) {
			element.remove();
		} else {
			for (Element child : element.children()) {
				clean(child, publication);
			}
		}
	}

	private static boolean lastCharIsSpace(StringBuilder sb) {
		return sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ';
	}

	private static boolean lastCharIsNewline(StringBuilder sb) {
		return sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n';
	}

	private static boolean secondToLastCharIsNewline(StringBuilder sb) {
		return sb.length() > 1 && sb.charAt(sb.length() - 2) == '\n';
	}

	// copied from org.jsoup.nodes.Element.appendNormalisedText(StringBuilder, TextNode)
	private static void appendNormalisedText(StringBuilder accum, TextNode textNode) {
		String text = textNode.getWholeText();

		if (preserveWhitespace(textNode.parentNode()) || textNode instanceof CDataNode) {
			accum.append(text);
		} else {
			StringUtil.appendNormalisedWhitespace(accum, text, lastCharIsSpace(accum) || lastCharIsNewline(accum) || accum.length() == 0);
		}
	}

	// copied from org.jsoup.nodes.Element.preserveWhitespace(Node)
	private static boolean preserveWhitespace(Node node) {
		// looks only at this element and five levels up, to prevent recursion & needless stack searches
		if (node != null && node instanceof Element) {
			Element el = (Element) node;
			int i = 0;
			do {
				if (el.tag().preserveWhitespace() && !el.tag().getName().equals("title")) {
					return true;
				}
				el = el.parent();
				i++;
			} while (i < 6 && el != null);
		}
		return false;
	}

	// adapted from org.jsoup.nodes.Element.text()
	static String formattedText(Element element) {
		if (element == null) {
			logger.error("null Element given for formatting");
			return "";
		}
		final StringBuilder accum = StringUtil.borrowBuilder();
		NodeTraversor.traverse(new NodeVisitor() {
			public void head(Node node, int depth) {
				if (node instanceof TextNode) {
					TextNode textNode = (TextNode) node;
					appendNormalisedText(accum, textNode);
				} else if (node instanceof Element) {
					Element element = (Element) node;
					if (accum.length() > 0 && (element.isBlock() || element.tagName().equals("br")) && !lastCharIsNewline(accum)) {
						accum.append('\n');
					}
					if (accum.length() > 1 && marginTags.contains(element.tagName()) && !secondToLastCharIsNewline(accum)) {
						accum.append('\n');
					}
				}
			}

			public void tail(Node node, int depth) {
				if (node instanceof Element) {
					Element element = (Element) node;
					if (accum.length() > 0 && element.isBlock() && !lastCharIsNewline(accum)) {
						accum.append('\n');
						if (marginTags.contains(element.tagName())) {
							accum.append('\n');
						}
					}
				}
			}
		}, element);
		return StringUtil.releaseBuilder(accum).trim();
	}

	// the supplied Document will be modified
	static String cleanedBody(Document doc, boolean publication) {
		if (doc == null) {
			logger.error("null Document given for cleaning");
			return "";
		}
		String text = "";
		for (Element description : doc.select(DESCRIPTION_SELECTOR)) {
			String descriptionText = StringUtil.normaliseWhitespace(description.attr("content")).trim();
			if (descriptionText.length() > text.length()) {
				text = descriptionText;
			}
		}
		if (!text.isEmpty()) {
			text += "\n\n";
		}
		if (doc.body() != null) {
			for (Element child : doc.body().children()) {
				clean(child, publication);
			}
			text += formattedText(doc.body());
		} else {
			logger.warn("Webpage " + doc.location() + " is missing a body");
		}
		return text;
	}
}
