// TableToken.java 
// ---------
// part of YaCy
// (C) by Michael Peter Christen; mc@anomic.de
// first published on http://www.anomic.de
// Frankfurt, Germany, 2007
// Created 22.02.2007
//
// This file is contributed by Franz Brauße
//
// $LastChangedDate: $
// $LastChangedRevision: $
// $LastChangedBy: $
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Using this software in any meaning (reading, learning, copying, compiling,
// running) means that you agree that the Author(s) is (are) not responsible
// for cost, loss of data or any harm that may be caused directly or indirectly
// by usage of this softare or this documentation. The usage of this software
// is on your own risk. The installation and usage (starting/running) of this
// software may allow other people or application to access your computer and
// any attached devices and is highly dependent on the configuration of the
// software which must be done by the user of the software; the author(s) is
// (are) also not responsible for proper configuration and usage of the
// software, even if provoked by documentation provided together with
// the software.
//
// Any changes to this file according to the GPL as documented in the file
// gpl.txt aside this file in the shipment you received can be done to the
// lines that follows this copyright notice here, but changes must not be
// done inside the copyright notive above. A re-distribution must contain
// the intact and unchanged copyright notice.
// Contributions and changes to the program code must be marked as such.

package de.anomic.data.wiki.tokens;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

public class TableToken extends AbstractToken {
	
	private static final Pattern[] pattern = new Pattern[] {
		Pattern.compile(
				"\\{\\|" +					// "{|"
				"([^\n]|\n\\|[|-])*\n" +	// new line must start with "||" or "|-"
				"\\|\\}")					// "|}"
	};
	private static final String[] blockElementNames = new String[] { "table", "tr", "td" };
	
	protected boolean parse() {
		String[] t = text.split("\n");
		String[] tds;
		StringBuffer sb = new StringBuffer();
		sb.append("<table");
		if (t[0].length() > 2) sb.append(parseTableProperties(t[0].substring(2)));
		sb.append(">\n");
		boolean trOpen = false;
		for (int i=1, j, a; i<t.length-1; i++) {
			if (t[i].startsWith("|-")) {
				if (trOpen) sb.append("\t</tr>\n");
				if (trOpen = (i < t.length - 2)) sb.append("\t<tr>\n");
			} else if (t[i].startsWith("||")) {
				tds = t[i].split("\\|\\|");
				for (j=0; j<tds.length; j++) {
					if (tds[j].length() > (a = tds[j].indexOf('|')) + 1) {	// don't print empty td's
						sb.append("\t\t<td");
						if (a > -1) sb.append(parseTableProperties(tds[j].substring(0, a)));
						sb.append(">").append(tds[j].substring(a + 1)).append("</td>\n");
					}
				}
			}
		}
		if (trOpen) sb.append("\t</tr>\n");
		this.markup =  new String(sb.append("</table>"));
		this.parsed = true;
		return true;
	}
	
    // from de.anomic.data.wikiCode.java.parseTableProperties, modified by [FB]
	private static final String[] tps = { "rowspan", "colspan", "vspace", "hspace", "cellspacing", "cellpadding", "border" };
    private static final HashMap/* <String,String[]> */ ps = new HashMap();
    static {
    	ps.put("frame", 	new String[] { "void", "above", "below", "hsides", "lhs", "rhs", "vsides", "box", "border" });
    	ps.put("rules", 	new String[] { "none", "groups", "rows", "cols", "all" });
    	ps.put("valign", 	new String[] { "top", "middle", "bottom", "baseline" });
    	ps.put("align", 	new String[] { "left", "right", "center" });
    }
    
	// contributed by [MN]
    /** This method takes possible table properties and tests if they are valid.
      * Valid in this case means if they are a property for the table, tr or td
      * tag as stated in the HTML Pocket Reference by Jennifer Niederst (1st edition)
      * The method is important to avoid XSS attacks on the wiki via table properties.
      * @param str A string that may contain several table properties and/or junk.
      * @return A string that only contains table properties.
      */
    private static StringBuffer parseTableProperties(final String properties){
        String[] values = properties.replaceAll("&quot;", "").split("[= ]");     //splitting the string at = and blanks
        StringBuffer sb = new StringBuffer(properties.length());
        Iterator it;
        String key, valkey, value;
        int numberofvalues = values.length;
        main: for (int i=0; i<numberofvalues; i++) {
        	valkey = values[i].trim();
        	if (i + 1 < numberofvalues) {
        		value = values[++i].trim();
        		if (
        				valkey.equals("summary") ||
        				(valkey.equals("bgcolor") && value.matches("#{0,1}[0-9a-fA-F]{1,6}|[a-zA-Z]{3,}")) ||
        				((valkey.equals("width") || valkey.equals("height")) && value.matches("\\d+%{0,1}")) ||
        				(isInArray(tps, valkey) && value.matches("\\d+"))
        		) {
                	addPair(valkey, value, sb);
                	continue;
        		}
        		it = ps.keySet().iterator();
        		while (it.hasNext()) {
        			key = (String)it.next();
        			if (valkey.equals(key) && isInArray((String[])ps.get(key), (String)value)) {
        				addPair(valkey, value, sb);
        				continue main;
        			}
        		}
        	}
            if (valkey.equals("nowrap"))
                sb.append(" nowrap");
        }
        return sb;
    }
    
    private static StringBuffer addPair(String val1, String val2, StringBuffer sb) {
    	return sb.append(" ").append(val1).append("=\"").append(val2).append("\"");
    }
    
    private static boolean isInArray(Object[] array, Object find) {
    	for (int i=array.length-1; i>-1; i--)
    		if (array[i].equals(find)) return true;
    	return false;
    }
	
	public Pattern[] getRegex() { return pattern; }
	public String[] getBlockElementNames() { return blockElementNames; }
	
	public boolean setText(String text, int patternNr) {
		this.text = text;
		this.parsed = false;
		this.markup = null;
		return true;
	}
}
