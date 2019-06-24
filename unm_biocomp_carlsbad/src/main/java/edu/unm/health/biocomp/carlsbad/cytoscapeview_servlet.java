package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*; //Pattern
import java.net.*; //InetAddress
import javax.servlet.*;
import javax.servlet.http.*;

import edu.unm.health.biocomp.util.http.*;

/**	CytoscapeWeb container for Carlsbad.
	<br>
	Based on Python version cytoscapeview.cgi.
	Note that CytoscapeWeb must be installed and served by HTTPD at /cytoscapeweb/.
	<br>
	Currently hard-coded to use /carlsbad/mol2img.
	<br>
	XGMML files read normally in scratch dirs periodically purged by the responsible
	web app (e.g. CarlsbadOne).
	<br>
	@author Jeremy J Yang
*/
public class cytoscapeview_servlet extends HttpServlet
{
  //private static ServletContext CONTEXT=null;
  //private static ServletConfig CONFIG=null;
  private static String CONTEXTPATH=null;
  private static String SERVLETNAME=null;
  private static String SERVERNAME=null;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  //private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  private static String color1="#EEEEEE";
  private static String xgmml="";
  private static String CYWEB_BGCOLOR="#CCDDFF";
  private static HashMap<String,String> MODES = null;

  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
	throws IOException,ServletException
  {
    SERVERNAME=request.getServerName();
    if (SERVERNAME.equals("localhost")) SERVERNAME=InetAddress.getLocalHost().getHostAddress();
    CONTEXTPATH=request.getContextPath();

    rb=ResourceBundle.getBundle("LocalStrings",request.getLocale());

    //ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList("biocomp.css"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList(
	"biocomp.js",
	"ddtip.js",
	"/cytoscapeweb/js/min/json2.min.js",
	"/cytoscapeweb/js/min/AC_OETags.min.js",
	"/cytoscapeweb/js/min/cytoscapeweb.min.js"));

    boolean ok=initialize(request);
    response.setContentType("text/html");
    out=response.getWriter();
    out.println(HeaderHtm(SERVLETNAME+":"+params.getVal("title"), JavaScript(xgmml), jsincludes, CSS(), request));

    if (ok)
    {
      out.println(NetworkViewHtm(params.getVal("title"),MODES.get(params.getVal("mode")),xgmml));
    }
    else
    {
      for (String error: errors)
        out.println(error);
    }
    out.println("</BODY></HTML>");
  }

  /////////////////////////////////////////////////////////////////////////////
  private boolean initialize(HttpServletRequest request)
      throws IOException,ServletException
  {
    SERVLETNAME=this.getServletName();
    //outputs=new ArrayList<String>();
    errors=new ArrayList<String>();
    params=new HttpParams();

    MODES = new HashMap<String,String>();
    MODES.put("rgt","reduced graph tgts-only");
    MODES.put("rgtp","reduced graph tgts+CCPs");
    MODES.put("full","full graph");

    for (Enumeration e=request.getParameterNames(); e.hasMoreElements(); )
    {
      String key=(String)e.nextElement();
      if (request.getParameter(key)!=null)
        params.setVal(key,request.getParameter(key));
    }

    //Set defaults:
    params.setVal("mode", params.getVal("mode","full"));
    params.setVal("layout", params.getVal("layout","ForceDirected"));
    params.setVal("nodescale","CHECKED");
    params.setVal("nodecolor","CHECKED");
    params.setVal("nodeshape","CHECKED");
    params.setVal("edgescale","CHECKED");
    params.setVal("edgesmerge",params.isChecked("edgesmerge")?"CHECKED":"");

    File ifile = new File(params.getVal("infile"));
    if (!ifile.exists())
    {
      errors.add("ERROR: file does not exist: "+params.getVal("infile"));
      return false;
    }
    BufferedReader buff=new BufferedReader(new FileReader(ifile));
    xgmml="";
    while (true)
    {
      String line=buff.readLine();
      if (line==null) break;
      xgmml+=(line+"\n");
    }
    buff.close();

    return true;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String HeaderHtm(String title,String js,List<String> jsincludes,String css,
	HttpServletRequest request)
  {
    String htm="<HTML>\n";
    htm+=("<HEAD><TITLE>"+title+"</TITLE>\n");
    htm+=("<SCRIPT SRC=\"/js/Mol2Img.js\"></SCRIPT>\n");
    if (jsincludes!=null)
    {
      for (String jsinclude: jsincludes)
      {
        if (jsinclude.contains("/"))
          htm+=("<SCRIPT SRC=\""+jsinclude+"\"></SCRIPT>\n");
        else
          htm+=("<SCRIPT SRC=\"/tomcat"+request.getContextPath()+"/js/"+jsinclude+"\"></SCRIPT>\n");
      }
    }
    if (js!=null)
      htm+=("<SCRIPT>\n"+js+"\n</SCRIPT>\n");
    htm+=("<LINK REL=\"stylesheet\" type=\"text/css\" HREF=\"/css/biocomp.css\" />\n");
    if (css!=null)
      htm+=("<STYLE TYPE=\"text/css\">\n"+css+"</STYLE>\n");
    htm+=("</HEAD>\n<BODY BGCOLOR=\"#DDDDDD\">\n");
    return htm;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String NetworkViewHtm(String title,String mode,String xgmml)
  {
    String htm=
       ("<DIV ID=\"titlebar\"><H2 ALIGN=CENTER>"+title+" ["+mode+"]</H2></DIV>\n")
      +("<DIV ID=\"cytoscapeweb\">Cytoscape Web: "+title+"</DIV>\n")
      +("<DIV ID=\"info\">\n")
      +("  <DIV ID=\"control\">\n");
    htm+=FormHtm();
    htm+=(
       ("  </DIV>\n")
      +("  <DIV ID=\"note\"><P>Click (or hover-over) nodes or edges for more info...</P>\n")
      +("  </DIV>\n")
      +("  <DIV ID=\"depict\">\n")
      +("  </DIV>\n")
      +("</DIV>\n")
      +("<DIV style=\"clear: both\"></DIV>\n"));
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm()
  {
    ArrayList<String> layouts = new ArrayList<String>(Arrays.asList("ForceDirected","Circle","Radial","Tree","CompoundSpringEmbedder")) ;
    String layout_menu = "<SELECT NAME=\"layout\" onChange=\"mod_Layout(this.form)\" >";
    for (String layout: layouts)
      layout_menu+=("<OPTION VALUE=\""+layout+"\">"+layout+"\n");
    layout_menu+="</SELECT>";
    layout_menu=layout_menu.replace("\""+params.getVal("layout")+"\">","\""+params.getVal("layout")+"\" SELECTED>");

    ArrayList<String> tgt_idtypes = new ArrayList<String>(Arrays.asList("ChEMBL ID","ChEMBL Target","EC Number","Ensembl","Entrez Gene","Gene","GeneCards","HomoloGene", "NCBI gi","OMIM","PDB","PharmGKB Gene","Protein Ontology (PRO)","RefSeq Nucleotide","RefSeq Protein", "Swissprot","UniGene","UniGene Hs.","UniProt"));
    String tgt_label_type_menu="<SELECT NAME=\"tgt_label_type\" onChange=\"mod_TargetLabel(this.form)\" >";
    tgt_label_type_menu+=("<OPTION VALUE=\"default\">default");
    for (String tgt_idtype: tgt_idtypes)
      tgt_label_type_menu+=("<OPTION VALUE=\""+tgt_idtype+"\">"+tgt_idtype);
    tgt_label_type_menu+="</SELECT>";

    ArrayList<String> cpd_idtypes = new ArrayList<String>(Arrays.asList("CAS Registry No.","ChEBI","ChEMBL ID","ChEMBL Ligand","DrugBank","iPHACE","IUPHAR Ligand ID", "NURSA Ligand","PDSP Record Number","PharmGKB Drug","PubChem CID","PubChem SID","RCSB PDB Ligand","SMDL ID"));
    String cpd_label_type_menu=("<SELECT NAME=\"cpd_label_type\" onChange=\"mod_CompoundLabel(this.form)\" >");
    cpd_label_type_menu+=("<OPTION VALUE=\"default\">default");
    for (String cpd_idtype: cpd_idtypes)
      cpd_label_type_menu+=("<OPTION VALUE=\""+cpd_idtype+"\">"+cpd_idtype);
    cpd_label_type_menu+="</SELECT>";

    String htm=
       ("<FORM NAME=\"mainform\">\n")
      +("<TABLE WIDTH=\"100%\" CELLSPACING=0 CELLPADDING=1>\n")
      +("<INPUT TYPE=\"HIDDEN\" NAME=\"mode\" VALUE=\""+params.getVal("mode")+"\">\n")
      +("<TR><TD ALIGN=RIGHT><B>nodes:</B></TD><TD>\n")
      +("<INPUT TYPE=CHECKBOX NAME=\"nodescale\" VALUE=\"CHECKED\" onChange=\"mod_NodeSize(this.form)\" "+params.getVal("nodescale")+">scale \n")
      +("<INPUT TYPE=CHECKBOX NAME=\"nodeshape\" VALUE=\"CHECKED\" onChange=\"mod_NodeShape(this.form)\" "+params.getVal("nodeshape")+">shape \n")
      +("<INPUT TYPE=CHECKBOX NAME=\"nodecolor\" VALUE=\"CHECKED\" onChange=\"mod_NodeColor(this.form)\" "+params.getVal("nodecolor")+">color \n")
      +("</TD></TR>\n")
      +("<TR><TD ALIGN=RIGHT><B>edges:</B></TD><TD>\n")
      +("<INPUT TYPE=CHECKBOX NAME=\"edgescale\" VALUE=\"CHECKED\" onChange=\"mod_EdgeWidth(this.form)\" "+params.getVal("edgescale")+">scale \n")
      +("&nbsp;\n")
      +("<INPUT TYPE=CHECKBOX NAME=\"edgesmerge\" VALUE=\"CHECKED\" onChange=\"mod_EdgeMerge(this.form)\" "+params.getVal("edgesmerge")+">merge \n")
      +("</TD></TR>\n")
      +("<TR><TD ALIGN=RIGHT><B>layout:</B></TD><TD>\n")
      +(layout_menu+"\n<BUTTON TYPE=BUTTON onClick=\"ReLayout()\">ReLayout</BUTTON>")
      +("</TD></TR>\n<TR><TD ALIGN=RIGHT><B>tgt labels:</B><BR></TD><TD>")
      +tgt_label_type_menu
      +("</TD></TR>\n<TR><TD ALIGN=RIGHT><B>cpd labels:</B></TD><TD>")
      +cpd_label_type_menu
      +("</TD></TR>\n")
      +("<TR><TD ALIGN=RIGHT></TD><TD>\n")
      +("<BUTTON TYPE=BUTTON onClick=\"clear_div('note'); print2div('note',netSummary())\">network summary</BUTTON>\n")
      +("<!-- BUTTON TYPE=BUTTON onClick=\"DEBUG2Log()\">DEBUG</BUTTON -->\n")
      +("</TD></TR>\n")
      +("</TABLE>\n")
      +("</FORM>\n");
    return htm;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String CSS()
  {
    String css=
      ("* {\n")
     +("  margin:0; padding:0; font-family:Helvetica, Arial, Verdana, sans-serif;\n")
     +("}\n")
     +("html,body {\n")
     +("  height:100%; width:100%; padding:0; margin:0;\n")
     +("}\n")
     +("#titlebar {\n")
     +("  width:98%; height:5%; background-color:"+CYWEB_BGCOLOR+"; margin-left:auto; margin-right:auto;\n")
     +("}\n")
     +("/* The Cytoscape Web container must have its dimensions set. */\n")
     +("#cytoscapeweb {\n")
     +("  width:98%; height:75%; margin-left:auto; margin-right:auto;\n")
     +("}\n")
     +("#info {\n")
     +("  width:98%; height:20%; background-color:#FFFFFF; margin-left:auto; margin-right:auto;\n")
     +("}\n")
     +("#control {\n")
     +("  float:left; width:30%; height:100%; background-color:#DDDDDD; border:solid overflow:auto; \n")
     +("}\n")
     +("#note {\n")
     +("  float:left; width:43%; background-color:#FEFEFE; border:solid overflow:auto; \n")
     +("}\n")
     +("p {\n")
     +("  padding:0 0.5em; margin:0;\n")
     +("}\n")
     +("p:first-child {\n")
     +("  padding-top:0.5em;\n")
     +("}\n")
     +("#depict {\n")
     +("  float:left; width:20%; background-color:#FFFFFF;\n")
     +("}\n")
     +("p {\n")
     +("  padding:0 0.5em; margin:0;\n")
     +("}\n")
     +("p:first-child {\n")
     +("  padding-top:0.5em;\n")
     +("}\n");
    return css;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript(String xgmml)
  {
    String js=
       ("var vis; //global\n")
      +("var tgt_label_type=''; //global\n")
      +("var cpd_label_type=''; //global\n")
      +("function DEBUG2Log()\n")
      +("{\n")
      +("  var visual_style = vis.visualStyle();\n")
      +("  //console.log('DEBUG: visual_style.nodes.size=\"'+JSON.stringify(visual_style.nodes.size)+'\"');\n")
      +("\n")
      +("  var layout_options = vis.layout().options;\n")
      +("  console.log('DEBUG: layout_options=\"'+JSON.stringify(layout_options)+'\"');\n")
      +("}\n")
      +("function mod_EdgeMerge(form)\n")
      +("{\n")
      +("  vis.edgesMerged(form.edgesmerge.checked);\n")
      +("  return;\n")
      +("}\n")
      +("function mod_NodeSize(form)\n")
      +("{\n")
      +("  visual_style = vis.visualStyle();\n")
      +("  visual_style.nodes.size = NodeSize(form.mode.value,form.nodescale.checked);\n")
      +("  vis.visualStyle(visual_style);\n")
      +("}\n")
      +("function NodeSize(mode,nodescale)\n")
      +("{\n")
      +("  if ((mode=='rgt'||mode=='rgtp') && nodescale)\n")
      +("  {\n")
      +("    size = {\n")
      +("          defaultValue: 100,\n")
      +("\n")
      +("////////////////////////////////////////////\n")
      +("//CANNOT HAVE BOTH discreteMapper and continuousMapper...\n")
      +("//          discreteMapper: {\n")
      +("//            attrName: \"class\",\n")
      +("//            entries: [\n")
      +("//              { attrValue: \"compound\", value: 50 }\n")
      +("//            ]\n")
      +("//          },\n")
      +("////////////////////////////////////////////\n")
      +("\n")
      +("          continuousMapper: { attrName: 'deg_cpd', minValue: 20, maxValue: 160 }\n")
      +("\n")
      +("//      customMapper: { functionName: \"customNodeSizeRGT\" } //HOW?\n")
      +("\n")
      +("        };\n")
      +("  }\n")
      +("  else\n")
      +("  {\n")
      +("    size = {\n")
      +("        defaultValue: 25,\n")
      +("        discreteMapper: {\n")
      +("          attrName: \"class\",\n")
      +("          entries: [\n")
      +("            { attrValue: \"target\", value: 60 },\n")
      +("            { attrValue: \"compound\", value: 25 },\n")
      +("            { attrValue: \"scaffold\", value: 35 },\n")
      +("            { attrValue: \"mces\", value: 35 }\n")
      +("          ]\n")
      +("        }\n")
      +("        };\n")
      +("  }\n")
      +("  return size;\n")
      +("}\n")
      +("function mod_NodeColor(form)\n")
      +("{\n")
      +("  visual_style = vis.visualStyle();\n")
      +("  visual_style.nodes.color = NodeColor(form.mode.value,form.nodecolor.checked);\n")
      +("  vis.visualStyle(visual_style);\n")
      +("}\n")
      +("function NodeColor(mode,nodecolor)\n")
      +("{\n")
      +("  if ((mode=='rgt'||mode=='rgtp') && nodecolor)\n")
      +("  {\n")
      +("    //console.log('DEBUG: mode=\"'+mode+'\"; nodecolor=\"'+nodecolor+'\"');\n")
      +("    color = {\n")
      +("          defaultValue: \"#FFFFFF\",\n")
      +("          discreteMapper: {\n")
      +("                attrName: 'type',\n")
      +("                entries: [\n")
      +("                        { attrValue: \"enzyme\", value:  \"#0B94B1\" },\n")
      +("                        { attrValue: \"receptor\", value: \"#FF0000\" },\n")
      +("                        { attrValue: \"protein\", value: \"#EE00EE\" },\n")
      +("                        { attrValue: \"ion channel\", value: \"#4444FF\" },\n")
      +("                        { attrValue: \"membrane receptor\", value: \"#DDDD00\" }\n")
      +("                ]\n")
      +("        }\n")
      +("        };\n")
      +("  }\n")
      +("  else\n")
      +("  {\n")
      +("    color = {\n")
      +("        defaultValue: \"#0B94B1\",\n")
      +("        discreteMapper: {\n")
      +("        attrName: \"class\",\n")
      +("        entries: [\n")
      +("            { attrValue: \"target\", value: \"#0B94B1\" },\n")
      +("            { attrValue: \"compound\", value: \"#FFFFFF\" },\n")
      +("            { attrValue: \"scaffold\", value: \"#FFAF00\" },\n")
      +("            { attrValue: \"mces\", value: \"#FFDD00\" }\n")
      +("        ]\n")
      +("      }\n")
      +("        };\n")
      +("  }\n")
      +("  return color;\n")
      +("}\n")
      +("function mod_NodeShape(form)\n")
      +("{\n")
      +("  visual_style = vis.visualStyle();\n")
      +("  visual_style.nodes.shape = NodeShape(form.mode.value,form.nodeshape.checked);\n")
      +("  vis.visualStyle(visual_style);\n")
      +("}\n")
      +("function NodeShape(mode,nodeshape)\n")
      +("{\n")
      +("  if ((mode=='rgt'||mode=='rgtp') && nodeshape)\n")
      +("  {\n")
      +("//    shape = {\n")
      +("//          defaultValue: \"OCTAGON\",\n")
      +("//          discreteMapper: {\n")
      +("//              attrName: 'species',\n")
      +("//              entries: [\n")
      +("//                      { attrValue: \"human\", value:  \"CIRCLE\" },\n")
      +("//                      { attrValue: \"mouse\", value: \"TRIANGLE\" },\n")
      +("//                      { attrValue: \"rat\", value: \"VEE\" }\n")
      +("//              ]\n")
      +("//        }\n")
      +("//        };\n")
      +("\n")
      +("    shape = {\n")
      +("          defaultValue: \"CIRCLE\",\n")
      +("          discreteMapper: {\n")
      +("          attrName: 'class',\n")
      +("          entries: [\n")
      +("            { attrValue: \"target\", value: \"CIRCLE\" },\n")
      +("            { attrValue: \"disease\", value: \"RECTANGLE\" },\n")
      +("            { attrValue: \"compound\", value: \"RECTANGLE\" }\n")
      +("          ]\n")
      +("        }\n")
      +("      };\n")
      +("  }\n")
      +("  else\n")
      +("  {\n")
      +("    shape = {\n")
      +("          defaultValue: \"CIRCLE\",\n")
      +("          discreteMapper: {\n")
      +("          attrName: 'class',\n")
      +("          entries: [\n")
      +("            { attrValue: \"target\", value: \"OCTAGON\" },\n")
      +("            { attrValue: \"compound\", value: \"RECTANGLE\" },\n")
      +("            { attrValue: \"scaffold\", value: \"HEXAGON\" },\n")
      +("            { attrValue: \"mces\", value: \"ELLIPSE\" }\n")
      +("          ]\n")
      +("        }\n")
      +("      };\n")
      +("  }\n")
      +("  return shape;\n")
      +("}\n")
      +("function mod_EdgeWidth(form)\n")
      +("{\n")
      +("  visual_style = vis.visualStyle();\n")
      +("  visual_style.edges.width = EdgeWidth(form.mode.value,form.edgescale.checked);\n")
      +("  vis.visualStyle(visual_style);\n")
      +("}\n")
      +("function EdgeWidth(mode,edgescale)\n")
      +("{\n")
      +("  if ((mode=='rgt'||mode=='rgtp') && edgescale)\n")
      +("  {\n")
      +("    attr='shared_cpd';\n")
      +("    width = {\n")
      +("          defaultValue: 2, \n")
      +("          continuousMapper: { attrName: attr, minValue: 1, maxValue: 10 }\n")
      +("        };\n")
      +("  }\n")
      +("  else\n")
      +("  {\n")
      +("    attr='shared_cpd';\n")
      +("    width = {\n")
      +("        defaultValue: 2, \n")
      +("        continuousMapper: { attrName: attr, minValue: 1, maxValue: 3 }\n")
      +("        };\n")
      +("  }\n")
      +("  return width;\n")
      +("}\n")
      +("\n")
      +("function ReLayout()\n")
      +("{\n")
      +("  vis.layout(vis.layout());\n")
      +("}\n")
      +("function LayoutOptions(mode,method)\n")
      +("{\n")
      +("  var layout_options = { name: method };\n")
      +("\n")
      +("  if (mode=='rgt'||mode=='rgtp')\n")
      +("  {\n")
      +("    //console.log('DEBUG: mode=\"'+mode+'\"; method=\"'+method+'\"');\n")
      +("    //layout_options.weightAttr = null;\n")
      +("    layout_options.weightAttr = \"shared_cpd\";\n")
      +("    layout_options.gravitation = -400;\n")
      +("    layout_options.minDistance = 1;\n")
      +("    layout_options.maxDistance = 500;\n")
      +("    //layout_options.restLength =    \"auto\",\n")
      +("    layout_options.restLength = 30;\n")
      +("    layout_options.tension = 0.1;\n")
      +("    layout_options.weightNorm = \"linear\";\n")
      +("    layout_options.mass =          3,\n")
      +("    layout_options.iterations =    400,\n")
      +("    layout_options.autoStabilize = false;\n")
      +("  }\n")
      +("  else\n")
      +("  {\n")
      +("    layout_options.weightAttr = \"val_std\";\n")
      +("\n")
      +("    //layout_options.weightAttr =    null,\n")
      +("    //layout_options.drag =          0.2,\n")
      +("    //layout_options.gravitation =   -200,\n")
      +("    //layout_options.minDistance =   1,\n")
      +("    //layout_options.maxDistance =   400,\n")
      +("    //layout_options.mass =          3,\n")
      +("    //layout_options.tension =       0.2,\n")
      +("    //layout_options.weightNorm =    \"linear\",\n")
      +("    //layout_options.restLength =    \"auto\",\n")
      +("    //layout_options.iterations =    400,\n")
      +("    //layout_options.maxTime =       30000,\n")
      +("    //layout_options.autoStabilize = false\n")
      +("  }\n")
      +("  //console.log('DEBUG: layout_options=\"'+JSON.stringify(layout_options)+'\"');\n")
      +("  return layout_options;\n")
      +("}\n")
      +("function mod_Layout(form)\n")
      +("{\n")
      +("  vis.layout({ name: form.layout.value, options: LayoutOptions(form.mode.value,form.layout.value) });\n")
      +("  return;\n")
      +("}\n")
      +("function mod_TargetLabel(form)\n")
      +("{\n")
      +("  var i;\n")
      +("  for (i=0;i<form.tgt_label_type.length;++i)\n")
      +("    if (form.tgt_label_type.options[i].selected)\n")
      +("      tgt_label_type=form.tgt_label_type.options[i].value;\n")
      +("  vis.visualStyle(vis.visualStyle());\n")
      +("  return;\n")
      +("}\n")
      +("function mod_CompoundLabel(form)\n")
      +("{\n")
      +("  var i;\n")
      +("  for (i=0;i<form.cpd_label_type.length;++i)\n")
      +("    if (form.cpd_label_type.options[i].selected)\n")
      +("      cpd_label_type=form.cpd_label_type.options[i].value;\n")
      +("  vis.visualStyle(vis.visualStyle());\n")
      +("  return;\n")
      +("}\n")
      +("function netSummary()\n")
      +("{\n")
      +("  var net = vis.networkModel(); //plugin network object\n")
      +("  var htm='<B>network summary:</B><P>';\n")
      +("  if (net.data!=null) {\n")
      +("    if (net.data.nodes!=null) {\n")
      +("      htm+='nodes: '+net.data.nodes.length;\n")
      +("    }\n")
      +("    if (net.data.edges!=null) {\n")
      +("      htm+='<br />edges: '+net.data.edges.length;\n")
      +("    }\n")
      +("    var n_tar=0;\n")
      +("    var n_cpd=0;\n")
      +("    var n_scaf=0;\n")
      +("    var n_mces=0;\n")
      +("    for (var i=0;i<net.data.nodes.length;++i)\n")
      +("    {\n")
      +("      var node=net.data.nodes[i];\n")
      +("      if (node.class!=null)\n")
      +("      {\n")
      +("        if (node.class == 'target') ++n_tar;\n")
      +("        else if (node.class == 'compound') ++n_cpd;\n")
      +("        else if (node.class == 'scaffold') ++n_scaf;\n")
      +("        else if (node.class == 'mces') ++n_mces;\n")
      +("      }\n")
      +("    }\n")
      +("    htm+=('<br />targets: '+n_tar);\n")
      +("    htm+=('<br />compounds: '+n_cpd);\n")
      +("    htm+=('<br />scaffolds: '+n_scaf);\n")
      +("    htm+=('<br />mces: '+n_mces);\n")
      +("  }\n")
      +("  htm+='</p>';\n")
      +("  return htm;\n")
      +("}\n")
      +("function clear_div(divname)\n")
      +("{\n")
      +("  document.getElementById(divname).innerHTML=\"\";\n")
      +("}\n")
      +("function print2div(divname,msg)\n")
      +("{\n")
      +("  msg=msg.replace(/,/g,', ');\n")
      +("  msg=msg.replace(/;/g,'; ');\n")
      +("  document.getElementById(divname).innerHTML+=msg;\n")
      +("}\n")
      +("function preferred_synonym(synonyms) // Find \"name-like\"\n")
      +("{\n")
      +("  if (synonyms.length==0) return '';\n")
      +("  var i_best=0;\n")
      +("  for (i_best=synonyms.length-1;i_best>=0;--i_best)\n")
      +("    if (synonyms[i_best].match(/^[A-Z][a-z]+$/)) return synonyms[i_best];\n")
      +("  for (i_best=synonyms.length-1;i_best>=0;--i_best)\n")
      +("    if (synonyms[i_best].match(/^[a-zA-Z ]+$/)) return synonyms[i_best];\n")
      +("  return synonyms[i_best];\n")
      +("}\n")
      +("window.onload=function() {\n")
      +("  var div_id=\"cytoscapeweb\"; // id of Cytoscape Web container div\n")
      +("  var init_options = {\n")
      +("    swfPath:\"/cytoscapeweb/swf/CytoscapeWeb\",\n")
      +("    flashInstallerPath:\"/cytoscapeweb/swf/playerProductInstall\",\n")
      +("    flashAlternateContent: \"El Flash Player es necesario.\"\n")
      +("  };\n")
      +("  vis = new org.cytoscapeweb.Visualization(div_id,init_options);\n")
      +("  var visual_style = { global:{} };\n")
      +("  visual_style.global.backgroundColor = \""+CYWEB_BGCOLOR+"\";\n")
      +("  visual_style.nodes = \n")
      +("    {\n")
      +("      borderWidth: {\n")
      +("        discreteMapper: {\n")
      +("          attrName: \"is_drug\",\n")
      +("          entries: [\n")
      +("            { attrValue: true, value: 3 }\n")
      +("          ]\n")
      +("        },\n")
      +("        defaultValue: 1\n")
      +("      },\n")
      +("      borderColor: {\n")
      +("        discreteMapper: {\n")
      +("          attrName: \"is_drug\",\n")
      +("          entries: [\n")
      +("            { attrValue: true, value: \"#FF0000\" }\n")
      +("          ]\n")
      +("        },\n")
      +("        defaultValue: \"#FFFFFF\"\n")
      +("      },\n")
      +("      shape: NodeShape('"+params.getVal("mode")+"','"+params.getVal("nodeshape")+"'),\n")
      +("      size: NodeSize('"+params.getVal("mode")+"','"+params.getVal("nodescale")+"'),\n")
      +("      color: NodeColor('"+params.getVal("mode")+"','"+params.getVal("nodecolor")+"'),\n")
      +("      tooltipText: {\n")
      +("        discreteMapper: {\n")
      +("          attrName: \"class\",\n")
      +("          entries: [\n")
      +("            { attrValue: \"disease\",\n")
      +("                value: \"<b>${name}<br />${id}<br />\" },\n")
      +("            { attrValue: \"target\",\n")
      +("                value: \"<b>${id}<br />${name}<br />${species}, ${type}<br />deg_cpd: ${deg_cpd}</b>\" },\n")
      +("            { attrValue: \"compound\",\n")
      +("                value: \"<b>${id}<br />deg_tgt: ${deg_tgt}</b>\" },\n")
      +("                //value: \"<b>${id}<br /><img src='/tomcat/carlsbad/mol2img?h=60&w=60&smiles=${smiles}'><br />deg_tgt: ${deg_tgt}</b>\" },\n")
      +("            { attrValue: \"scaffold\",\n")
      +("                value: \"<b>${id}<br />deg_tgt: ${deg_tgt}<br />deg_cpd: ${deg_cpd}</b>\" },\n")
      +("            { attrValue: \"mces\", value:\n")
      +("                \"<b>${id}<br />deg_tgt: ${deg_tgt}<br />deg_cpd: ${deg_cpd}</b>\" },\n")
      +("          ]\n")
      +("        }\n")
      +("      },\n")
      +("      labelFontSize: 10,\n")
      +("      labelFontWeight: \"bold\"\n")
      +("    };\n")
      +("  visual_style.edges = \n")
      +("    {\n")
      +("      width: EdgeWidth('"+params.getVal("mode")+"','"+params.getVal("edgescale")+"'),\n")
      +("      style: {\n")
      +("        discreteMapper: {\n")
      +("          attrName: \"class\",\n")
      +("          entries: [\n")
      +("            { attrValue: \"activity\", value:  \"SOLID\" },\n")
      +("            { attrValue: \"tt\", value:  \"EQUAL_DASH\" },\n")
      +("            { attrValue: \"cpd2scaf\", value: \"DOT\" },\n")
      +("            { attrValue: \"cpd2mces\", value: \"DOT\" }\n")
      +("          ]\n")
      +("        },\n")
      +("        defaultValue: \"SOLID\"\n")
      +("      },\n")
      +("      tooltipText: {\n")
      +("        discreteMapper: {\n")
      +("          attrName: \"class\",\n")
      +("          entries: [\n")
      +("            { attrValue: \"activity\", value: \"<b>${class}: ${id}<br />val_std: ${val_std}</b>\" },\n")
      +("            { attrValue: \"tt\", value: \"<b>${class}: ${id}<br />shared_cpd: ${shared_cpd}</b>\" }\n")
      +("          ]\n")
      +("        },\n")
      +("        defaultValue: \"<b>${class}: ${id}</b>\"\n")
      +("      },\n")
      +("      targetArrowShape: \"NONE\",\n")
      +("      color: \"#0B94B1\"\n")
      +("    };\n")
      +("  visual_style.edges.mergeWidth = EdgeWidth('"+params.getVal("mode")+"','"+params.getVal("edgescale")+"');\n")
      +("  visual_style.edges.mergeStyle =\n")
      +("      {\n")
      +("        discreteMapper: {\n")
      +("          attrName: \"class\",\n")
      +("          entries: [\n")
      +("            { attrValue: \"activity\", value:  \"SOLID\" },\n")
      +("            { attrValue: \"cpd2scaf\", value: \"DOT\" },\n")
      +("            { attrValue: \"cpd2mces\", value: \"DOT\" },\n")
      +("            { attrValue: \"tt\", value: \"DOT\" }\n")
      +("          ]\n")
      +("        },\n")
      +("        defaultValue: \"EQUAL_DASH\"\n")
      +("      };\n")
      +("  visual_style.edges.mergeColor = \"#0B94B1\";\n")
      +("\n")
      +("// now load the network via xgmml:\n")
      +("  var xgmml='';\n");
    
    String[] lines = Pattern.compile("[\n\r]+").split(xgmml);
    for (String line: lines)
    {
      if (line==null) continue;
      line=line.replace("\'","\\\'");
      js+=("  xgmml+='"+line+"\\n';\n");
    }
    
    js+=(
       ("// interaction functionality\n")
      +("  var layout_options = LayoutOptions('"+params.getVal("mode")+"','"+params.getVal("layout")+"');\n")
      +("  var draw_options = {\n")
      +("        network:xgmml,\n")
      +("        visualStyle: visual_style,\n")
      +("        layout: layout_options,\n")
      +("        panZoomControlVisible:true,\n")
      +("        edgesMerged:'"+params.getVal("edgesmerge")+"'\n")
      +("        };\n")
      +("  // customMapper label function:\n")
      +("  vis[\"customLabel\"] = function (data) {\n")
      +("    var value=data[\"id\"];\n")
      +("    if (data[\"class\"]=='target')\n")
      +("    {\n")
      +("      if (tgt_label_type && tgt_label_type!='default')\n")
      +("      {\n")
      +("        if (typeof data[tgt_label_type] != 'undefined' && data[tgt_label_type]!=null)\n")
      +("        {\n")
      +("          value=new String(data[tgt_label_type]);\n")
      +("          value=value.replace(/,/g,', ');\n")
      +("        }\n")
      +("      }\n")
      +("      else\n")
      +("      {\n")
      +("        value=data[\"name\"]; //default\n")
      +("      }\n")
      +("    }\n")
      +("    else if (data[\"class\"]=='compound')\n")
      +("    {\n")
      +("      if (cpd_label_type && cpd_label_type!='default')\n")
      +("      {\n")
      +("        if (typeof data[cpd_label_type] != 'undefined' && data[cpd_label_type]!=null)\n")
      +("        {\n")
      +("          value=new String(data[cpd_label_type]);\n")
      +("          value=value.replace(/,/g,', ');\n")
      +("        }\n")
      +("      }\n")
      +("      if (typeof data['synonym'] != 'undefined' && data['synonym']!=null)\n")
      +("      {\n")
      +("        var synonyms_str=new String(data['synonym']);\n")
      +("        var synonyms = synonyms_str.split(\",\");\n")
      +("        var synonym=preferred_synonym(synonyms);\n")
      +("        value+=(' ('+synonym+')');\n")
      +("      }\n")
      +("    }\n")
      +("    else if (data[\"class\"]=='disease')\n")
      +("    {\n")
      +("      value=data[\"name\"]+' '+data['id'];\n")
      +("    }\n")
      +("    else\n")
      +("      value=data[\"id\"];\n")
      +("    value=value.replace(/\\s/g,'\\n');\n")
      +("    return value;\n")
      +("  };\n")
      +("\n")
      +("  // callback when Cytoscape Web has finished drawing\n")
      +("  vis.ready(\n")
      +("    function()\n")
      +("    {\n")
      +("      visual_style.nodes.label = { customMapper: { functionName: \"customLabel\" } };\n")
      +("\n")
      +("      vis.visualStyle(visual_style);\n")
      +("\n")
      +("      vis.nodeTooltipsEnabled(true);\n")
      +("      vis.edgeTooltipsEnabled(true);\n")
      +("\n")
      +("      // add listeners  for when nodes and edges are clicked\n")
      +("      vis.addListener(\"click\",\"nodes\",function(event) { handle_click(event); })\n")
      +("      vis.addListener(\"click\",\"edges\",function(event) { handle_click(event); });\n")
      +("      function handle_click(event)\n")
      +("      {\n")
      +("        var target = event.target;\n")
      +("        var cls = target.data['class']; //target,compound,activity,scaffold,mces\n")
      +("        clear_div(\"note\");\n")
      +("        clear_div(\"depict\");\n")
      +("        //print2div(\"note\",\"event.group = \" + event.group);  //nodes, edges\n")
      +("        if (typeof cls != 'undefined')\n")
      +("          print2div(\"note\",\"<B>\"+cls+\"</B>:<BR />\" );\n")
      +("        var smiles='';\n")
      +("        for (var v in target.data)\n")
      +("        {\n")
      +("          if (target.data[v]==null) continue;\n")
      +("          else if (v=='smiles' || v=='smarts')\n")
      +("          {\n")
      +("            smiles=target.data[v];\n")
      +("          }\n")
      +("          else if (v=='class') continue;\n")
      +("          else if (v=='label') continue;\n")
      +("          else if (v=='ID') continue;\n")
      +("          else if (v.match(/^canonical/i)) continue;\n")
      +("          else if (v.match(/^_/)) continue;\n")
      +("          else if (cls=='target' && v=='descr') continue;\n")
      +("          else if (cls=='target' && v=='deg_tgt') continue;\n")
      +("          else if (cls=='compound' && v=='deg_cpd') continue;\n")
      +("          else if (cls!='compound' && v=='is_drug') continue;\n")
      +("          print2div(\"note\",\"&nbsp;&nbsp;\"+v+\": \"+target.data[v]+\"<BR />\" );\n")
      +("        }\n")
      +("        if (cls=='activity')          //find cpd node, smiles\n")
      +("        {\n")
      +("          var cid=target.data['source']; //e.g. \"C261280\"\n")
      +("          var net = vis.networkModel(); //plugin network object\n")
      +("          var nodes = net.data.nodes;\n")
      +("          var i;\n")
      +("          for (i=0;i<nodes.length;++i)\n")
      +("          {\n")
      +("            var node=nodes[i];\n")
      +("            if (typeof node != 'undefined' && typeof node['id'] != 'undefined' && node['id']==cid)\n")
      +("            {\n")
      +("              if (typeof node['smiles'] != 'undefined' && node['smiles'])\n")
      +("                smiles=node['smiles'];\n")
      +("            }\n")
      +("          }\n")
      +("        }\n")
      +("        if (smiles)\n")
      +("        {\n")
      +("          var imgurl='/tomcat/carlsbad/mol2img';\n")
      +("          imgurl+='?h=300&w=400&smiles='+encodeURIComponent(smiles);\n")
      +("          var imghtm='<IMG HEIGHT=\"100%\" SRC=\"'+imgurl+'\">';\n")
      +("          document.getElementById(\"depict\").innerHTML=imghtm;\n")
      +("        }\n")
      +("      }\n")
      +("      //alert(\"DEBUG: \"+netSummary(vis));\n")
      +("    }\n")
      +("  );\n")
      +("\n")
      +("  vis.draw(draw_options);\n")
      +("};\n"));

    return js;
  }
}
