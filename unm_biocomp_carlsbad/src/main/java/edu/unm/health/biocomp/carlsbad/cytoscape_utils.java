package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**	Static methods for Cytoscape (v3) file &amp; network processing.
	<br>
	@author Jeremy J Yang
*/
public class cytoscape_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**	Returns header for Cytoscape compatible XGMML (XML).
  */
  public static String XGMML_Header(String title,String bgcolor)
  {
    Calendar calendar=Calendar.getInstance();
    calendar.setTime(new Date());
    String ts=String.format("%04d-%02d-%02d %02d:%02d:%02d",
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH)+1,
      calendar.get(Calendar.DAY_OF_MONTH),
      calendar.get(Calendar.HOUR_OF_DAY),
      calendar.get(Calendar.MINUTE),
      calendar.get(Calendar.SECOND));
    String xml="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    xml+="<graph label=\""+title+"\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:cy=\"http://www.cytoscape.org\" xmlns=\"http://www.cs.rpi.edu/XGMML\" directed=\"1\">\n"
    +"  <att name=\"documentVersion\" value=\"1.1\"/>\n"
    +"  <att name=\"networkMetadata\">\n"
    +"    <rdf:RDF>\n"
    +"      <rdf:Description rdf:about=\"http://www.cytoscape.org/\">\n"
    +"      <dc:type>"+title+"</dc:type>\n"
    +"      <dc:description>N/A</dc:description>\n"
    +"      <dc:identifier>N/A</dc:identifier>\n"
    +"      <dc:date>"+ts+"</dc:date>\n"
    +"      <dc:title>"+title+"</dc:title>\n"
    +"      <dc:source>http://biocomp.health.unm.edu</dc:source>\n"
    +"      <dc:format>Cytoscape-XGMML</dc:format>\n" +"    </rdf:Description>\n" +"  </rdf:RDF>\n"
    +"  </att>\n"
    +"  <att type=\"string\" name=\"backgroundColor\" value=\""+bgcolor+"\" />\n";
    return xml;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Returns footer for Cytoscape compatible XGMML (XML).
  */
  public static String XGMML_Footer()
  {
    return "</graph>\n";
  }
}
