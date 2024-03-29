package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*; //Pattern
import java.net.*; //InetAddress
import javax.servlet.*;
import javax.servlet.http.*;

import com.fasterxml.jackson.core.*; //JsonFactory, JsonGenerator
import com.fasterxml.jackson.databind.*; //ObjectMapper, JsonNode

import edu.unm.health.biocomp.util.http.*; //HtmUtils

/**	CytoscapeJS viewer for CARLSBAD.
	Previous version used CytoscapeWeb plugin (Flash).
	Accepts CYJS input files as exported by Cytoscape3.
	@author Jeremy J Yang
*/
public class cyview_servlet extends HttpServlet
{
  private static ServletContext CONTEXT=null;
  //private static ServletConfig CONFIG=null;
  private static String CONTEXTPATH=null;
  private static String SERVLETNAME=null;
  private static String SERVERNAME=null;
  private static Integer SERVERPORT=null;
  private static String SERVERSCHEME=null;
  private static String APPNAME=null;
  private static String NETNAME=null;
  private static String DEMOFILE=null;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  private static ArrayList<String> ERRORS=null;
  private static HttpParams params=null;
  private static String BGCOLOR="#EEEEEE";
  private static String CYJSTXT="";
  private static HashMap<String,String> MODES = null;
  private static String PROXY_PREFIX=null;

  /////////////////////////////////////////////////////////////////////////////
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    CONTEXT = getServletContext();
    CONTEXTPATH = CONTEXT.getContextPath();
    try { APPNAME = conf.getInitParameter("APPNAME"); }
    catch (Exception e) { APPNAME = this.getServletName(); }
    try { PROXY_PREFIX = conf.getInitParameter("PROXY_PREFIX"); }
    catch (Exception e) { PROXY_PREFIX = "/tomcat"; }
    DEMOFILE = CONTEXT.getRealPath("")+"/data/"+conf.getInitParameter("DEMOFILE");
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
	throws IOException,ServletException
  {
    SERVERNAME = request.getServerName();
    if (SERVERNAME.equals("localhost")) SERVERNAME = InetAddress.getLocalHost().getHostAddress(); //May cause COR rule violation (cross-origin request).
    SERVERPORT = request.getServerPort();
    //SERVERSCHEME=request.getServerScheme(); //"http" or "https"
    SERVERSCHEME="http";
    rb = ResourceBundle.getBundle("LocalStrings",request.getLocale());

    ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList(((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/css/biocomp.css", ((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/css/cyview.css"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList(((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/js/cyview.js", ((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/js/biocomp.js", ((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/js/ddtip.js", ((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/js/cytoscape.min.js"));

    initialize(request);
    response.setContentType("text/html");
    out = response.getWriter();

    ERRORS.add("ServerInfo: "+CONTEXT.getServerInfo());
    ERRORS.add("PROXY_PREFIX: "+PROXY_PREFIX); 
    ERRORS.add("ContextPath: "+CONTEXT.getContextPath()); 

    String netjs=null;
    try {
      netjs = NetJS(CYJSTXT);
    } catch (Exception e) {
      ERRORS.add("ERROR: "+e.getMessage());
      netjs = "alert("+e.getMessage()+")";
    }
    String title = (params.hasVal("title")?params.getVal("title"):NETNAME);
    ERRORS.add("NetworkName: "+title);
    String cynethtm = CyViewHtm(title, MODES.get(params.getVal("mode")), netjs, response);
    out.println(HtmUtils.HeaderHtm(title, jsincludes, cssincludes,
	HeaderJS(SERVERNAME, SERVERPORT, SERVERSCHEME, ((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH), "", BGCOLOR, request));
    out.println(cynethtm);
    out.println("<SCRIPT>go_init(window.document.mainform)</SCRIPT>");
    HtmDivWrite("log", ERRORS, out);
    out.println("</BODY></HTML>");
  }

  /////////////////////////////////////////////////////////////////////////////
  private void HtmDivWrite(String div_id, List<String> lines, PrintWriter out)
  {
    String js="document.getElementById(\""+div_id+"\").innerHTML=\"\"+";
    for (String line: lines) {
      js+=("\""+line.replace("\"", "\\\"")+"<br>\"+\n");
    }
    js+=("\"\";\n");
    out.println("<SCRIPT>"+js+"</SCRIPT>");
  }
  /////////////////////////////////////////////////////////////////////////////
  private void HtmDivWrite(String div_id, String line, PrintWriter out)
  {
    String js="document.getElementById(\""+div_id+"\").innerHTML=\"\"+";
    js+=("\""+line.replace("\"", "\\\"")+"<br>\"+\n");
    js+=("\"\";\n");
    out.println("<SCRIPT>"+js+"</SCRIPT>");
  }

  /////////////////////////////////////////////////////////////////////////////
  private void initialize(HttpServletRequest request)
      throws IOException,ServletException
  {
    SERVLETNAME=this.getServletName();
    ERRORS=new ArrayList<String>();
    params=new HttpParams();

    // Modes needed?
    MODES = new HashMap<String,String>();
    MODES.put("rgt", "reduced graph tgts-only");
    MODES.put("rgtp", "reduced graph tgts+CCPs");
    MODES.put("full", "full graph");

    for (Enumeration e=request.getParameterNames(); e.hasMoreElements(); )
    {
      String key=(String)e.nextElement();
      if (request.getParameter(key)!=null)
        params.setVal(key,request.getParameter(key));
    }

    BufferedReader buff=null;
    if (params.hasVal("infile"))
    {
      File ifile = new File(params.getVal("infile"));
      if (!ifile.exists()) {
        ERRORS.add("ERROR: file not found: "+params.getVal("infile"));
        return;
      }
      else if (!ifile.isFile()) {
        ERRORS.add("ERROR: file not readable: "+params.getVal("infile"));
        return;
      }
      else {
        //ERRORS.add("DEBUG: infile: "+params.getVal("infile"));
        buff = new BufferedReader(new FileReader(ifile));
      }
    } else {
      //ERRORS.add("DEBUG: DEMOFILE="+DEMOFILE);
      buff = new BufferedReader(new FileReader(DEMOFILE));
    }
    CYJSTXT="";
    while (true)
    {
      String line=buff.readLine();
      if (line==null) break;
      CYJSTXT+=(line+"\n");
    }
    buff.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String CyViewHtm(String title, String mode, String netjs, HttpServletResponse response)
  {
    String htm=
       ("<DIV ID=\"titlebar\"><H2 ALIGN=CENTER>"+title+((mode!=null)?(" ["+mode+"]"):"")+"</H2></DIV>\n")
      +("<DIV ID=\"cy\"></DIV>\n")
      +("<DIV ID=\"info\">\n")
      +("<DIV ID=\"control\">\n")+FormHtm(response)+("</DIV><!-- end of control DIV -->\n")
      +("<DIV ID=\"log\">Click (or hover-over) nodes or edges for more info...</DIV>\n")
      +("<DIV ID=\"depict\"></DIV>\n")
      +("</DIV><!-- end of info DIV -->\n")
      +("<SCRIPT>"+netjs+"</SCRIPT>\n")
      +("<DIV style=\"clear: both\"></DIV>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String NetJS(String cyjstxt)
	throws Exception
  {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = null;
    try {
      root = mapper.readTree(cyjstxt);
    } catch (Exception e) {
      ERRORS.add("ERROR: "+e.getMessage());
      return("alert("+e.getMessage()+")");
    }

    JsonFactory jsf = mapper.getFactory();
    jsf.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);

    String js=
	("var cy = cytoscape({\n")
	+("container: document.getElementById('cy'),\n")
	+("style: [{\n")
	+("    selector: 'node',\n")
	+("    style: {\n")
	+("      shape: 'ellipse',\n")
	+("      label: 'data(name)'\n")
	+("    }\n")
	+("}],\n")
	+("layout: {\n")
	+("  name: 'grid'\n")
	+("},\n");
    Iterator<String> fitr = root.fieldNames();
    while (fitr.hasNext()) {
      String field=fitr.next();
      if (field=="data" || field=="elements") continue;
      else {
        js+=(field+": "+root.get(field).toString()+",\n");
      }
    }
    if (root.has("data")) {
      js+=("data: {\n");
      JsonNode root_data = root.get("data");
      fitr = root_data.fieldNames();
      while (fitr.hasNext()) {
        String field=fitr.next();
        js+=(field+": "+root_data.get(field).toString()+",\n");
        if (field=="name") NETNAME=root_data.get(field).toString();
      }
      js+=("},\n");
    }
    if (root.has("elements")) {
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();
      JsonGenerator jsg = jsf.createGenerator(ostream).useDefaultPrettyPrinter();
      jsg.writeObject(root.get("elements"));
      js+=("  elements: \n"+ostream.toString("UTF-8")+"\n");
    }
    js+=(" });\n");
    return js;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HeaderJS(String servername, Integer serverport, String serverscheme, String prefix)
  {
    //Global MOL2IMG must be relative URL to work in all environments (e.g. Docker).
    return(
"var MOL2IMG='"+prefix+"/mol2img';\n"+
"function servername() { return('"+servername+"'); }\n");
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm(HttpServletResponse response)
  {
    ArrayList<String> layouts = new ArrayList<String>(Arrays.asList("grid", "cose",
"circle", "concentric", "breadthfirst", "random", "preset")) ;
    String layout_menu = "<SELECT NAME=\"layout\" onChange=\"mod_layout(this.form)\" >";
    for (String layout: layouts)
      layout_menu+=("<OPTION VALUE=\""+layout+"\">"+layout+"\n");
    layout_menu+="</SELECT>";
    layout_menu=layout_menu.replace("\""+params.getVal("layout")+"\">","\""+params.getVal("layout")+"\" SELECTED>");

    ArrayList<String> tgt_idtypes = new ArrayList<String>(Arrays.asList("name", "ChEMBL_ID", "Ensembl", "Entrez_Gene", "Gene", "NCBI_gi", "UniProt"));
    String tgt_label_menu="<SELECT NAME=\"tgt_label\" onChange=\"mod_targetLabel(this.form)\" >";
    for (String tgt_idtype: tgt_idtypes)
      tgt_label_menu+=("<OPTION VALUE=\""+tgt_idtype+"\">"+tgt_idtype);
    tgt_label_menu+="</SELECT>";

    ArrayList<String> cpd_idtypes = new ArrayList<String>(Arrays.asList("name", "ChEBI", "ChEMBL_ID", "PubChem_CID", "synonyms"));
    String cpd_label_menu=("<SELECT NAME=\"cpd_label\" onChange=\"mod_compoundLabel(this.form)\" >");
    for (String cpd_idtype: cpd_idtypes)
      cpd_label_menu+=("<OPTION VALUE=\""+cpd_idtype+"\">"+cpd_idtype);
    cpd_label_menu+="</SELECT>";

    // See cyview.js custom functions.
    String htm=
       ("<FORM NAME=\"mainform\">\n")
      +("<TABLE WIDTH=\"100%\" CELLSPACING=\"0\" CELLPADDING=\"1\">\n")
      +("<INPUT TYPE=\"HIDDEN\" NAME=\"mode\" VALUE=\""+params.getVal("mode")+"\">\n")
      +("<TR><TD ALIGN=\"RIGHT\"><B>nodes:</B></TD><TD>\n")
      +("<INPUT TYPE=\"CHECKBOX\" NAME=\"nodestyle\" VALUE=\"CHECKED\" onChange=\"mod_nodeStyle(this.form)\" "+params.getVal("nodestyle")+">style\n")
      +("<TR><TD ALIGN=\"RIGHT\"><B>hide:</B></TD><TD>\n")
      +("<INPUT TYPE=\"CHECKBOX\" NAME=\"nodehide_mces\" VALUE=\"CHECKED\" onChange=\"mod_nodeHideClass(this.form)\" "+params.getVal("nodehide_mces")+">mcess\n")
      +("<INPUT TYPE=\"CHECKBOX\" NAME=\"nodehide_scaffold\" VALUE=\"CHECKED\" onChange=\"mod_nodeHideClass(this.form)\" "+params.getVal("nodehide_scaffold")+">scaffolds\n")
      +("<INPUT TYPE=\"CHECKBOX\" NAME=\"nodehide_compound\" VALUE=\"CHECKED\" onChange=\"mod_nodeHideClass(this.form)\" "+params.getVal("nodehide_compound")+">compounds\n")
      +("</TD></TR>\n")
      +("<TR><TD ALIGN=\"RIGHT\"><B>edges:</B></TD>")
      +("<TD><INPUT TYPE=\"CHECKBOX\" NAME=\"edgestyle\" VALUE=\"CHECKED\" onChange=\"mod_edgeStyle(this.form)\" "+params.getVal("edgestyle")+">style&nbsp;\n")
      +("<INPUT TYPE=\"CHECKBOX\" NAME=\"edgemerge\" VALUE=\"CHECKED\" onChange=\"mod_edgeMerge(this.form)\" "+params.getVal("edgemerge")+">merge </TD></TR>\n")
      +("<TR><TD ALIGN=\"RIGHT\"><B>layout:</B></TD><TD>\n"+layout_menu+"\n<BUTTON TYPE=\"BUTTON\" onClick=\"redo_layout(this.form)\"><B>ReLayout</B></BUTTON></TD></TR>\n")
      +("<TR><TD ALIGN=\"RIGHT\"><B>tgtLabels:</B><BR></TD><TD>"+tgt_label_menu+"</TD></TR>\n")
      +("<TR><TD ALIGN=\"RIGHT\"><B>cpdLabels:</B></TD><TD>"+cpd_label_menu+"</TD></TR>\n")
      +("<TR><TD ALIGN=\"RIGHT\"><BUTTON TYPE=\"BUTTON\" onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"')\"><B>Reset</B></BUTTON></TD>")
      +("<TD><BUTTON TYPE=\"BUTTON\" onClick=\"write2div('log', netSummary(), true)\"><B>NetSummary</B></BUTTON></TD></TR>\n")
      +("</TABLE>\n")
      +("</FORM>\n");
    return htm;
  }
}
