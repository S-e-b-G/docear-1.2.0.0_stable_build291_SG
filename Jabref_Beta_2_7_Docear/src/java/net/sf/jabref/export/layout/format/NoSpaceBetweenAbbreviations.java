package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * <p>
 * LayoutFormatter that removes the space between abbreviated First names
 * </p>
 * <p>
 * What out that this regular expression might also remove other spaces that fit
 * the pattern.
 * </p>
 * <p>
 * Example: J. R. R. Tolkien becomes J.R.R. Tolkien.
 * </p>
 * <p>
 * See Testcase for more examples.
 * <p>
 * 
 * @author $Author: coezbek $
 * @version $Revision: 1748 $ ($Date: 2006-09-03 17:20:38 +0200 (So, 03 Sep 2006) $)
 * 
 */
public class NoSpaceBetweenAbbreviations implements LayoutFormatter {

	/*
	 * Match '.' followed by spaces followed by uppercase char followed by '.'
	 * but don't include the last dot into the capturing group.
	 * 
	 * Replace the match by removing the spaces.
	 * 
	 * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
	 */
	public String format(String fieldText) {
		return fieldText.replaceAll("\\.\\s+(\\p{Lu})(?=\\.)", "\\.$1");
	}
}
