package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.net.*; //URLEncoder,InetAddress
import java.text.*;
import java.util.*;
import java.util.regex.*; //Pattern
import java.util.concurrent.*;
import java.net.*; //InetAddress
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import chemaxon.struc.*;
import chemaxon.formats.*;

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;
import edu.unm.health.biocomp.util.db.*;
import edu.unm.health.biocomp.util.threads.*;
import edu.unm.health.biocomp.smarts.smarts_utils;

/**	CarlsbadOne, a one-step, bioactivity data-mining, evidence-driven application for
	biophamaceutical knowledge discovery via cheminformatics, chemogenomics and systems
	pharmaco-informatics.

	@author Jeremy J Yang
*/
public class carlsbadone_servlet extends HttpServlet
{
  //static, owned by class/webapp:
  private static String SERVLETNAME=null;
  private static String CONTEXTPATH=null;
  private static String LOGDIR=null;	// configured in web.xml
  private static String APPNAME=null;	// configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static Integer MAX_POST_SIZE=10*1024*1024; // configured in web.xml
  private static String SCRATCHDIR=null; // configured in web.xml
  private static Integer N_MAX=null;	// configured in web.xml
  private static Integer N_MAX_TARGETS=null;	// default configured in web.xml
  private static String DBHOST=null;    // configured in web.xml
  private static String DBNAME=null;      // configured in web.xml
  private static String DBSCHEMA=null;  // configured in web.xml
  private static Integer DBPORT=null;   // configured in web.xml
  private static String DBUSR=null;      // configured in web.xml
  private static String DBPW=null;      // configured in web.xml
  private static String CYVIEW=null;      // configured in web.xml
  private static String TARGETCSVURL=null; // configured in web.xml
  private static String DRUGCSVURL=null; // configured in web.xml
  private static String HELP_FILE=null; // configured in web.xml
  private static Boolean DEBUG=null; // configured in web.xml
  private static String PROXY_PREFIX=null; // configured in web.xml
  private static String PREFIX=null;
  private static int SCRATCH_RETIRE_SEC=3600;
  private static ServletContext CONTEXT=null;
  private static ServletConfig config=null;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static String REMOTEAGENT=null;
  private static String DATESTR=null;
  private static File LOGFILE=null;
  private static String color1="#EEEEEE";
  private static String MOL2IMG_SERVLETURL=null;
  private static ArrayList<String> dbids=null;
  private static DiseaseList DISEASELIST=null;	//Parsed once by init().
  private static CompoundList DRUGLIST=null;	//Parsed once by init().
  private static TargetList TARGETLIST=null;	//Parsed once by init().
  private static HashMap<String,CompoundList> CPDLISTCACHE=null;	//Init by init(); for caching cpd hitlists.
  private static HashMap<String,TargetList> TGTLISTCACHE=null;	//Init by init(); for caching cpd hitlists.
  private static HashMap<String,CCPList> CCPLISTCACHE=null;	//Init by init(); for caching ccp hitlists.
  private static final int AUTOSUGGESTMINLEN=3;

  //Non-static, owned by object/servlet-instance:
  private HttpParams params=null;
  private PrintWriter out=null;
  private DBCon DBCON=null; //Each instantiation should have one DBCon which is closed at end of doPost().
  private ArrayList<String> outputs=null;
  private ArrayList<String> errors=null;

  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    SERVERNAME=request.getServerName();
    if (SERVERNAME.equals("localhost")) SERVERNAME=InetAddress.getLocalHost().getHostAddress();
    REMOTEHOST=request.getHeader("X-Forwarded-For"); // client (original)
    if (REMOTEHOST!=null)
    {
      String[] addrs=Pattern.compile(",").split(REMOTEHOST);
      if (addrs.length>0) REMOTEHOST=addrs[addrs.length-1];
    }
    else
    {
      REMOTEHOST=request.getRemoteAddr(); // client (may be proxy)
    }
    REMOTEAGENT=request.getHeader("User-Agent");
    ResourceBundle rb=ResourceBundle.getBundle("LocalStrings", request.getLocale());

    MultipartRequest mrequest=null;
    if (request.getMethod().equalsIgnoreCase("POST"))
    {
      try { mrequest = new MultipartRequest(request, UPLOADDIR, 10*1024*1024, "ISO-8859-1", new DefaultFileRenamePolicy()); }
      catch (IOException e) { this.getServletContext().log("not a valid MultipartRequest", e); }
    }

    //PROXY_PREFIX = (Pattern.compile(".*Jetty.*$", Pattern.CASE_INSENSITIVE).matcher(CONTEXT.getServerInfo()).matches())?"/jetty":"/tomcat";

    // main logic:
    try { DBCON = new DBCon("postgres", DBHOST, DBPORT, DBNAME, DBUSR, DBPW); }
    catch (Exception e) {
      CONTEXT.log("ERROR: PostgreSQL connection failed.",e);
      throw new ServletException("ERROR: PostgreSQL connection failed.",e);
    }
    boolean ok=Initialize(request,mrequest);
    ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList(((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/css/carlsbad.css", ((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/css/jquery-ui.css"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList(((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/js/biocomp.js", ((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/js/ddtip.js", ((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/js/jquery-1.9.1.js", ((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/js/jquery-ui.js"));

    String title=APPNAME;
    if (!params.getVal("formmode").isEmpty()) title+=":"+params.getVal("formmode");
    if (!ok)
    {
      response.setContentType("text/html");
      out=response.getWriter();
      out.print(HtmUtils.HeaderHtm(title, jsincludes, cssincludes, JavaScript(), "", color1, request));
      out.print(HtmUtils.FooterHtm(errors,true));
    }
    else if (mrequest!=null)		//method=POST, normal operation
    {
      if (mrequest.getParameter("changemode").equalsIgnoreCase("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response,params,params.getVal("formmode")));
        out.println("<SCRIPT>go_init(window.document.mainform,true)</SCRIPT>");
        out.print(HtmUtils.FooterHtm(errors,true));
      }
      else if (mrequest.getParameter("search").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest, response, params, params.getVal("formmode")));
        out.flush();
        response.flushBuffer();
        java.util.Date t_0 = new java.util.Date();
        Integer tid=null;	//for target query
        try { tid=Integer.parseInt(params.getVal("tid")); } catch (Exception e) { }
        Integer cid=null;	//for drug query
        try { cid=Integer.parseInt(params.getVal("cid")); } catch (Exception e) { }
        Float scaf_min=null;
        try { scaf_min=Float.parseFloat(params.getVal("scaf_min")); } catch (Exception e) { scaf_min=0.0f; }
        String kid=params.getVal("kid");

        if (params.getVal("formmode").equals("disease"))
        {
          if (kid==null || kid.isEmpty() || kid.equals("0")) kid=DISEASELIST.name2ID(params.getVal("qbuff"));
          if (kid==null)
            outputs.add("ERROR: Aaack!  kid==null.  This should not happen!");
          else
            outputs.add("<H2>Query:</H2><BLOCKQUOTE>"+DiseaseQueryHtm(kid, response, SERVLETNAME)+"</BLOCKQUOTE>");
        }
        else if (params.getVal("formmode").equals("target"))
        {
          if (tid==null || tid==0) tid=TARGETLIST.nst2ID(params.getVal("qbuff"));
          if (tid==null)
            outputs.add("ERROR: Aaack!  tid==null.  This should not happen!");
          else
            outputs.add("<H2>Query:</H2><BLOCKQUOTE>"+TargetQueryHtm(tid, response, SERVLETNAME)+"</BLOCKQUOTE>");
        }
        else if (params.getVal("formmode").equals("drug"))
        {
          if (cid==null || cid==0) cid=DRUGLIST.synonym2CID(params.getVal("qbuff"));
          if (cid==null)
            outputs.add("ERROR: Aaack!  cid==null.  This should not happen!");
          else
            outputs.add("<H2>Query:</H2><BLOCKQUOTE>"+CompoundQueryHtm(cid, DRUGLIST, DBCON, MOL2IMG_SERVLETURL, response, SERVLETNAME)+"</BLOCKQUOTE>");
        }
        else
        {
          outputs.add("ERROR: Aaack! formmode=\""+params.getVal("formmode")+"\" not allowed.");
        }

        StringBuilder err_sb = new StringBuilder();

        HashMap<String,Integer> subnet_counts=null;
        String fout_rgt_path=null; //reduced-graph, tgts only
        String fout_rgtp_path=null; //reduced-graph, tgts+scaffolds
        String fout_full_path=null; //full-graph
        String fout_cpd_path=null; //compounds
        ArrayList<String> sqls = new ArrayList<String>();
        Integer n_max_a=100000;
        Integer n_max_c=100000;
        ArrayList<Integer> tids = new ArrayList<Integer>(); //for return vals
        CompoundList cpdlist = new CompoundList(); //for return vals
        CCPList ccplist = new CCPList(); //for return vals

        String query="";
        try {
          File dout=new File(SCRATCHDIR);
          File fout_rgt=File.createTempFile(PREFIX, "_subnet_rgt.cyjs", dout); //reduced-graph, tgts only
          fout_rgt_path=fout_rgt.getAbsolutePath();
          File fout_rgtp=File.createTempFile(PREFIX, "_subnet_rgtp.cyjs", dout); //reduced-graph, tgts+scaffolds
          fout_rgtp_path=fout_rgtp.getAbsolutePath();
          File fout_full=File.createTempFile(PREFIX, "_subnet_full.cyjs", dout);
          fout_full_path=fout_full.getAbsolutePath();
          File fout_cpd=File.createTempFile(PREFIX, "_cpds.sdf", dout); //compounds
          fout_cpd_path=fout_cpd.getAbsolutePath();

          if (params.getVal("formmode").equals("disease"))
          {
            query=params.getVal("diseasename");
            subnet_counts=webapp_utils.Disease2Network_LaunchThread(
		DBHOST, DBPORT, params.getVal("dbid"), DBUSR, DBPW,
		fout_rgt_path, fout_rgtp_path, fout_full_path, fout_cpd_path,
		kid, scaf_min,
		APPNAME+" ["+params.getVal("formmode")+"]: "+params.getVal("subnet_title"),
		SERVLETNAME, response, out,
		n_max_a,n_max_c,
		tids, cpdlist, ccplist, sqls, err_sb	//return vals
		);
          }
          else if (params.getVal("formmode").equals("drug"))
          {
            query=params.getVal("drugname");
            subnet_counts=webapp_utils.Compound2Network_LaunchThread(
		DBHOST, DBPORT, params.getVal("dbid"), DBUSR, DBPW,
		fout_rgt_path, fout_rgtp_path, fout_full_path, fout_cpd_path,
		cid, scaf_min,
		APPNAME+" ["+params.getVal("formmode")+"]: "+params.getVal("subnet_title"),
		SERVLETNAME, response, out,
		n_max_a, n_max_c,
		tids, cpdlist, ccplist, sqls, err_sb	//return vals
		);
          }
          else if (params.getVal("formmode").equals("target"))
          {
            if (tid!=null) tids.add(tid);
            query=params.getVal("tname");
            subnet_counts=webapp_utils.Target2Network_LaunchThread(
		DBHOST, DBPORT, params.getVal("dbid"), DBUSR, DBPW,
		fout_rgt_path, fout_rgtp_path, fout_full_path, fout_cpd_path,
		tid, scaf_min,
		APPNAME+" ["+params.getVal("formmode")+"]: "+params.getVal("subnet_title"),
		SERVLETNAME, response, out,
		n_max_a,n_max_c,
		cpdlist, ccplist, sqls, err_sb	//return vals
		);
          }
        }
        catch (SQLException e) { outputs.add("PostgreSQL error: "+e.getMessage()); }
        catch (Exception e) { outputs.add("ERROR: "+e.getMessage()); }

        if (err_sb.length()>0) { outputs.add("ERROR: (err_sb): "+err_sb.toString()); }
        String subnet_results_htm="";
        if (subnet_counts!=null)
        {
          subnet_results_htm = 
		webapp_utils.SubnetResultsHtm(subnet_counts,
		fout_rgt_path, fout_rgtp_path, fout_full_path,
		APPNAME+" ["+params.getVal("formmode")+"]:"+params.getVal("subnet_title"),
		response, CONTEXTPATH, SERVLETNAME,
		((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/"+CYVIEW, null);
          PrintWriter out_log=new PrintWriter(new BufferedWriter(new FileWriter(LOGFILE,true)));
          out_log.printf("%s\t%s\t%s\t%d\t%d\t%d\n",DATESTR,REMOTEHOST,params.getVal("formmode"),subnet_counts.get("n_node_tgt"),subnet_counts.get("n_node_cpd"),0);
          out_log.close();
          if (DEBUG)
          {
            ArrayList<String> countkeys = new ArrayList<String>(subnet_counts.keySet());
            Collections.sort(countkeys);
            for (String countkey: countkeys) errors.add(countkey+": "+subnet_counts.get(countkey));
          }
        }
        else
        {
          subnet_results_htm=("ERROR: "+SERVLETNAME+" (Aack!) subnet_counts==null.");
        }

        // Save tgtlist in cache, using PREFIX as unique etag:
        TargetList tgtlist=TARGETLIST.selectByIDs(new HashSet<Integer>(tids));
        //tid!=null if target-query; cid!=null if drug-query; kid!=null if disease-query
        webapp_utils.FlagTargetsEmpirical(tgtlist, DRUGLIST, tid, cid, DISEASELIST, kid);
        TGTLISTCACHE.put(PREFIX, tgtlist);

        webapp_utils.FlagCompoundsEmpirical(cpdlist, TARGETLIST, DISEASELIST, kid); //kid!=null if disease-query
        webapp_utils.FlagCompoundsEmpirical(cpdlist, tgtlist, //hitlist
		tid, //tid!=null if target-query
		cid); //cid!=null if drug-query

        CPDLISTCACHE.put(PREFIX,cpdlist); // Save cpdlist in cache, using PREFIX as etag:
        CCPLISTCACHE.put(PREFIX,ccplist); // Save ccplist in cache, using PREFIX as etag:

        //Done with processing.  Now output:

        // View targets button:
        String tgts_bhtm=("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?viewtargets=TRUE&etag="+URLEncoder.encode(PREFIX,"UTF-8")+"','targetswin','width=600,height=800,scrollbars=1,resizable=1')\"><B>View</B></BUTTON>");

        // View compounds button:
        String cpds_bhtm=("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?viewcompounds=TRUE&etag="+URLEncoder.encode(PREFIX,"UTF-8")+"&infile="+URLEncoder.encode(fout_cpd_path,"UTF-8")+"','cpdswin','width=600,height=800,scrollbars=1,resizable=1')\"><B>View</B></BUTTON>");

        // View ccps button:
        String ccps_bhtm=("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?viewccps=TRUE&etag="+URLEncoder.encode(PREFIX,"UTF-8")+"&etag="+URLEncoder.encode(PREFIX,"UTF-8")+"','ccpswin','width=600,height=800,scrollbars=1,resizable=1')\"><B>View</B></BUTTON>");

        String thtm="<TABLE WIDTH=\"100%\" CELLSPACING=5 CELLPADDING=5><TR>\n";

        thtm+=("<TD WIDTH=\"33%\" ALIGN=CENTER VALIGN=TOP>");
        thtm+=("<TABLE><TR><TD><H3>Targets: "+tgtlist.size()+"</H3></TD><TD>"+(tgtlist.size()>0?tgts_bhtm:"")+"</TD></TR>\n");
        thtm+=("<TR><TD COLSPAN=2 ALIGN=CENTER>empirical: "+tgtlist.empiricalCount()+" hypotheses: "+(tgtlist.size()-tgtlist.empiricalCount())+"</TD></TR>\n");
        thtm+=("</TABLE></TD>\n");

        thtm+=("<TD WIDTH=\"33%\" ALIGN=CENTER VALIGN=TOP>");
        thtm+=("<TABLE><TR><TD><H3>Scaffolds: "+ccplist.size()+"</H3></TD><TD>"+(ccplist.size()>0?ccps_bhtm:"")+"</TD></TR>");
        thtm+=("<TR><TD COLSPAN=2 ALIGN=CENTER>count: "+ccplist.size()+"</TD></TR>\n");
        thtm+=("</TABLE></TD>\n");

        thtm+=("<TD ALIGN=CENTER VALIGN=TOP>");
        thtm+=("<TABLE><TR><TD><H3>Compounds: "+cpdlist.size()+"</H3></TD><TD>"+(cpdlist.size()>0?cpds_bhtm:"")+"</TD></TR>");
        thtm+=("<TR><TD COLSPAN=2 ALIGN=CENTER>empirical: "+cpdlist.empiricalCount()+" hypotheses: "+(cpdlist.size()-cpdlist.empiricalCount())+"</TD></TR>\n");
        thtm+=("</TABLE></TD>\n");
        thtm+=("</TR></TABLE>\n");

        outputs.add("<H2>Results:</H2><BLOCKQUOTE>"+thtm+"</BLOCKQUOTE>");

        // Output subnet results (and Cyweb buttons):
        outputs.add("<H2>Knowledge graph:</H2>\n"+subnet_results_htm);

        // Targets CSV download button:
        String tgt_csv_bhtm=webapp_utils.TargetCSVButtonHtm(TARGETLIST, tids,
		mrequest, response, SERVLETNAME, PREFIX,
		((SERVLETNAME+"_"+params.getVal("subnet_title")+"_targets.csv").replaceAll(" ", "_")), SCRATCHDIR);

        // Compounds CSV download button:
        String cpd_csv_bhtm=webapp_utils.CompoundCSVButtonHtm(cpdlist,
		mrequest, response, SERVLETNAME, PREFIX,
		((SERVLETNAME+"_"+params.getVal("subnet_title")+"_cpds.csv").replaceAll(" ", "_")), SCRATCHDIR);

        // Compounds SDF download button:
        String cpd_sdf_bhtm=webapp_utils.CompoundSDFButtonHtm(fout_cpd_path,
		response, CONTEXTPATH, params,
		((SERVLETNAME+"_"+params.getVal("subnet_title")+"_cpds.sdf").replaceAll(" ", "_")), SERVLETNAME);

        //CYJS download buttons:
        String bhtm_cyjs_rgt=webapp_utils.CYJSDownloadButtonHtm(fout_rgt_path, ((SERVLETNAME+"_"+params.getVal("subnet_title")+"_rgt.cyjs").replaceAll(" ","_")), response, SERVLETNAME);
        String bhtm_cyjs_rgtp=webapp_utils.CYJSDownloadButtonHtm(fout_rgtp_path, ((SERVLETNAME+"_"+params.getVal("subnet_title")+"_rgtp.cyjs").replaceAll(" ","_")), response, SERVLETNAME);
        String bhtm_cyjs_full=webapp_utils.CYJSDownloadButtonHtm(fout_full_path, ((SERVLETNAME+"_"+params.getVal("subnet_title")+".cyjs").replaceAll(" ","_")), response, SERVLETNAME);

        thtm="<TABLE CELLSPACING=2 CELLPADDING=2>\n";
        thtm+="<TR><TD ALIGN=RIGHT>"+tgt_csv_bhtm+"</TD><TD>&larr; IDs, names, etc.</TD></TR>\n";
        thtm+="<TR><TD ALIGN=RIGHT>"+cpd_csv_bhtm+"</TD><TD>&larr; SMILES with IDs</TD></TR>\n";
        thtm+="<TR><TD ALIGN=RIGHT>"+cpd_sdf_bhtm+"</TD><TD>&larr; SDF with IDs, data</TD></TR>\n";

        thtm+="<TR><TD ALIGN=RIGHT>"+bhtm_cyjs_rgt+"</TD><TD>&larr; <B>Lean (targets)</B></TD></TR>\n";
        thtm+="<TR><TD ALIGN=RIGHT>"+bhtm_cyjs_rgtp+"</TD><TD>&larr; <B>Medium (targets, scaffolds)</B></TD></TR>\n";
        thtm+="<TR><TD ALIGN=RIGHT>"+bhtm_cyjs_full+"</TD><TD>&larr; <B>Full (targets, scaffolds, compounds)</B></TD></TR>\n";
        thtm+="<TR><TD COLSPAN=2 ALIGN=CENTER>(CYJS can be imported by Cytoscape.)</TD></TR>\n";

        thtm+="</TABLE>\n";

        outputs.add("<H2>Downloads:</H2>\n<BLOCKQUOTE>"+thtm+"</BLOCKQUOTE>");

        out.println("<SCRIPT>progresswin_close()</SCRIPT>");
        errors.add(SERVLETNAME+": elapsed time: "+time_utils.TimeDeltaStr(t_0,new java.util.Date()));
        if (DEBUG && sqls!=null) { for (String s: sqls) errors.add("<PRE>"+s+"</PRE>"); }
        out.println(HtmUtils.OutputHtm(outputs));
        out.println(HtmUtils.FooterHtm(errors,true));
        HtmUtils.PurgeScratchDirs(Arrays.asList(SCRATCHDIR),SCRATCH_RETIRE_SEC,false,".",(HttpServlet) this);
      }
    }
    else
    {
      String downloadtxt=request.getParameter("downloadtxt"); // POST param
      String downloadfile=request.getParameter("downloadfile"); // POST param
      if (request.getParameter("help")!=null)	// GET method, help=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(HelpHtm(DBCON));
        out.println(HtmUtils.FooterHtm(errors,true));
      }
      else if (request.getParameter("test")!=null)      // GET method, test=TRUE
      {
        response.setContentType("text/plain");
        out=response.getWriter();
        HashMap<String,String> t = new HashMap<String,String>();
        out.print(HtmUtils.TestTxt(APPNAME,t));
      }
      else if (request.getParameter("viewcompounds")!=null)	// GET method, viewcompounds=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        String etag=request.getParameter("etag");
        out.print(HtmUtils.HeaderHtm(
		((etag!=null && etag.equals("drugs"))?title+":ViewDrugs":title+":ViewCompounds"),
		jsincludes, cssincludes, "", "", color1, request));
        Integer skip=null;
        try { skip=Integer.parseInt(request.getParameter("skip")); } catch (Exception e) {};
        Integer nmax=null;
        try { nmax=Integer.parseInt(request.getParameter("nmax")); } catch (Exception e) {};
        try {
          out.print(webapp_utils.ViewCompoundsHtm(
		((etag!=null && etag.equals("drugs"))?DRUGLIST:null),
		((etag!=null && etag.equals("drugs"))?null:CPDLISTCACHE),
		etag,
		request.getParameter("infile"),
		request.getParameter("sortby"),
		skip,
		nmax,
		((etag!=null && etag.equals("drugs"))?"Drug Compounds":"Compounds"),
		MOL2IMG_SERVLETURL,
		response,
		SERVLETNAME));
        }
        catch (Exception e) { errors.add("ERROR: "+e.getMessage()); }
        out.println(HtmUtils.FooterHtm(errors,false));
      }
      else if (request.getParameter("viewcompound")!=null)	// GET method, viewcompound=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title+":ViewCompound", jsincludes, cssincludes, "", "", color1, request));
        Integer cid=Integer.parseInt(request.getParameter("cid"));
        try { out.print(webapp_utils.ViewCompoundHtm(cid,DBCON,MOL2IMG_SERVLETURL,response,SERVLETNAME)); }
        catch (Exception e) { errors.add("ERROR: "+e.getMessage()); }
        out.println(HtmUtils.FooterHtm(errors,false));
      }
      else if (request.getParameter("viewccps")!=null)	// GET method, viewccps=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        String etag=request.getParameter("etag");
        out.print(HtmUtils.HeaderHtm(title+":ViewScaffolds", jsincludes, cssincludes, "", "", color1, request));
        Integer skip=null;
        try { skip=Integer.parseInt(request.getParameter("skip")); } catch (Exception e) {};
        Integer nmax=null;
        try { nmax=Integer.parseInt(request.getParameter("nmax")); } catch (Exception e) {};
        try {
          out.print(webapp_utils.ViewCCPsHtm(
		null,
		CCPLISTCACHE,
		etag,
		request.getParameter("sortby"),
		skip,
		nmax,
		"Scaffolds",
		MOL2IMG_SERVLETURL,
		response,
		SERVLETNAME));
        }
        catch (Exception e) { errors.add("ERROR: "+e.getMessage()); }
        out.println(HtmUtils.FooterHtm(errors,false));
      }
      else if (request.getParameter("viewccp")!=null)	// GET method, viewccp=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title+":ViewScaffold", jsincludes, cssincludes, "", "", color1, request));
        Integer id=Integer.parseInt(request.getParameter("id"));
        String ccptype=request.getParameter("ccptype");
        try { out.print(webapp_utils.ViewCCPHtm(id,ccptype,DBCON,MOL2IMG_SERVLETURL,response,SERVLETNAME)); }
        catch (SQLException e) { errors.add("ERROR: "+e.getMessage()); }
        out.println(HtmUtils.FooterHtm(errors,false));
      }
      else if (request.getParameter("viewtargets")!=null)	// GET method, viewtargets=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title+":ViewTargets", jsincludes, cssincludes, "", "", color1, request));
        String etag=request.getParameter("etag");
        String species=request.getParameter("species");
        Integer skip=null;
        try { skip=Integer.parseInt(request.getParameter("skip")); } catch (Exception e) {};
        Integer nmax=null;
        try { nmax=Integer.parseInt(request.getParameter("nmax")); } catch (Exception e) {};
        TargetList tgtlist=TARGETLIST;
        if (species!=null)
        {
          if (TGTLISTCACHE.containsKey(species))
          {
            tgtlist=TGTLISTCACHE.get(species);
            tgtlist.refreshTimestamp();
          }
          else
          {
            tgtlist=TARGETLIST.selectBySpecies(new HashSet<String>(Arrays.asList(species)));
            TGTLISTCACHE.put(species,tgtlist);
          }
        }
        try { out.print(webapp_utils.ViewTargetsHtm(
		tgtlist,
		TGTLISTCACHE,
		etag,
		request.getParameter("sortby"),
		skip,
		nmax,
		response,
		SERVLETNAME)); }
        catch (Exception e) { errors.add("ERROR: "+e.getMessage()); }
        out.println(HtmUtils.FooterHtm(errors,false));
      }
      else if (request.getParameter("viewtarget")!=null)	// GET method, viewtarget=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title+":ViewTarget", jsincludes, cssincludes, "", "", color1, request));
        Integer tid=Integer.parseInt(request.getParameter("tid"));
        out.print(webapp_utils.ViewTargetHtm(tid,TARGETLIST,response,SERVLETNAME));
        out.println(HtmUtils.FooterHtm(errors,false));
      }
      else if (request.getParameter("viewdiseases")!=null)	// GET method, viewdiseases=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title+":ViewDiseases", jsincludes, cssincludes, "", "", color1, request));
        out.print(webapp_utils.ViewDiseasesHtm(DISEASELIST,TARGETLIST,
		(request.getParameter("sortby")==null?"id":request.getParameter("sortby")),
		response,SERVLETNAME));
        out.println(HtmUtils.FooterHtm(errors,false));
      }
      else if (request.getParameter("viewdisease")!=null)	// GET method, viewdisease=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title+":ViewDisease", jsincludes, cssincludes, "", "", color1, request));
        String kid=request.getParameter("kid");
        try { out.print(webapp_utils.ViewDiseaseHtm(kid, DISEASELIST, TARGETLIST, DBCON, response, CONTEXTPATH, SERVLETNAME, null)); }
        catch (SQLException e) { errors.add("ERROR: "+e.getMessage()); }
        out.println(HtmUtils.FooterHtm(errors,false));
      }
      else if (downloadtxt!=null && downloadtxt.length()>0) // POST param
      {
        ServletOutputStream ostream=response.getOutputStream();
        HtmUtils.DownloadString(response,ostream,downloadtxt,request.getParameter("fname"));
      }
      else if (downloadfile!=null && downloadfile.length()>0) // POST param
      {
        ServletOutputStream ostream=response.getOutputStream();
        HtmUtils.DownloadFile(response,ostream,downloadfile,request.getParameter("fname"));
        PrintWriter out_log=new PrintWriter(new BufferedWriter(new FileWriter(LOGFILE,true)));
        out_log.printf("%s\t%s\t\t%d\t%d\t%.1f\n",DATESTR,REMOTEHOST,0,0,((new File(downloadfile)).length()/1024.0));
        out_log.close();
      }
      else	// GET method, initial invocation of servlet w/ no params
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title, jsincludes, cssincludes, JavaScript(), "", color1, request));
//        if (REMOTEAGENT!=null && (REMOTEAGENT.contains("Explorer")||REMOTEAGENT.contains("MSIE")))
//          out.println("<CENTER><H2>Sorry, "+APPNAME+" NOT compatible with Internet Explorer. Limited functionality may be available.</H2></CENTER><HR>");
        out.println(FormHtm(mrequest,response,params,request.getParameter("formmode")));
        out.println("<SCRIPT>go_init(window.document.mainform,false)</SCRIPT>");
        out.println(HtmUtils.FooterHtm(errors,true));
      }
    }
    if (out!=null) out.flush();
    try { if (DBCON!=null) DBCON.close(); }
    catch (Exception e) { CONTEXT.log("ERROR: problem closing connection.",e); }
  }
  /////////////////////////////////////////////////////////////////////////////
  private boolean Initialize(HttpServletRequest request,MultipartRequest mrequest)
      throws IOException,ServletException
  {
    SERVLETNAME=this.getServletName();
    outputs = new ArrayList<String>();
    errors = new ArrayList<String>();
    params = new HttpParams();
    Calendar calendar=Calendar.getInstance();
    MOL2IMG_SERVLETURL=(((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/mol2img");
    
    String logo_htm="<TABLE CELLSPACING=5 CELLPADDING=5><TR><TD>";
    String imghtm=("<IMG BORDER=\"0\" SRC=\""+((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");

    String tiphtm=(APPNAME+" web app from UNM Translational Informatics.");
    String href=("http://medicine.unm.edu/informatics/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm, tiphtm, href, 200, "white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 SRC=\""+((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm=("JChem from ChemAxon Ltd.");
    href=("http://www.chemaxon.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm, tiphtm, href, 200, "white"));
    logo_htm+="</TD><TD>";
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=\"0\" HEIGHT=\"60\" SRC=\""+((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/images/cy3logoOrange.svg\">");
    tiphtm=("Cytoscape and Cytoscape.JS");
    href=("http://cytoscape.org/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm, tiphtm, href, 200, "white"));
    logo_htm+="</TD></TR></TABLE>";
    errors.add("<CENTER>"+logo_htm+"</CENTER>");

    errors.add(APPNAME+" diseases: "+DISEASELIST.size()+" [loaded: "+DISEASELIST.getTimestamp().toString()+"]");
    errors.add(APPNAME+" drugs: "+DRUGLIST.size()+"; synonyms: "+DRUGLIST.synonymCount()+" [loaded: "+DRUGLIST.getTimestamp().toString()+"]");
    errors.add(APPNAME+" targets (human): "+TARGETLIST.speciesCount("human")+" [loaded: "+TARGETLIST.getTimestamp().toString()+"]");

    //Create webapp-specific log dir if necessary:
    File dout=new File(LOGDIR);
    if (!dout.exists())
    {
      boolean ok=dout.mkdir();
      CONTEXT.log("LOGDIR creation "+(ok?"succeeded":"failed")+": "+LOGDIR);
      if (!ok)
      {
        errors.add("ERROR: could not create LOGDIR: "+LOGDIR);
        return false;
      }
    }

    String logpath=LOGDIR+"/"+SERVLETNAME+".log";
    LOGFILE=new File(logpath);
    if (!LOGFILE.exists())
    {
      try { LOGFILE.createNewFile(); }
      catch (IOException e) {
        errors.add("ERROR: Logfile creation failed: "+logpath+"\n"+e.getMessage());
        return false;
      }
      LOGFILE.setWritable(true,true);
      PrintWriter out_log=new PrintWriter(LOGFILE);
      out_log.println("date\tIP\tquery_mode\tN_tgt\tN_cpd\tKB"); 
      out_log.flush();
      out_log.close();
    }
    if (!LOGFILE.canWrite())
    {
      errors.add("ERROR: Log file not writable: "+logpath);
      return false;
    }
    BufferedReader buff=new BufferedReader(new FileReader(LOGFILE));
    if (buff==null)
    {
      errors.add("ERROR: Cannot open log file.");
      return false;
    }
    int n_lines=0;
    String line=null;
    String startdate=null;
    while ((line=buff.readLine())!=null)
    {
      ++n_lines;
      String[] fields=Pattern.compile("\\t").split(line);
      if (n_lines==2) startdate=fields[0];
    }
    buff.close(); //Else can result in error: "Too many open files"
    if (n_lines>2)
    {
      calendar.set(Integer.parseInt(startdate.substring(0,4)),
               Integer.parseInt(startdate.substring(4,6))-1,
               Integer.parseInt(startdate.substring(6,8)),
               Integer.parseInt(startdate.substring(8,10)),
               Integer.parseInt(startdate.substring(10,12)),0);
      DateFormat df=DateFormat.getDateInstance(DateFormat.FULL,Locale.US);
      errors.add("since "+df.format(calendar.getTime())+", times used: "+(n_lines-1));
    }
    File scratchdir=new File(SCRATCHDIR);
    if (!scratchdir.exists())
    {
      boolean ok=false;
      try {
        ok=scratchdir.mkdir();
        scratchdir.setWritable(true,true);
      }
      catch (Exception e) { errors.add("ERROR: SCRATCHDIR creation failed: "+e.getMessage()); return false; }
      if (!ok) { errors.add("ERROR: SCRATCHDIR creation failed."); return false; }
      CONTEXT.log("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
    }

    calendar.setTime(new java.util.Date());
    DATESTR=String.format("%04d%02d%02d%02d%02d",
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH)+1,
      calendar.get(Calendar.DAY_OF_MONTH),
      calendar.get(Calendar.HOUR_OF_DAY),
      calendar.get(Calendar.MINUTE));
    Random rand = new Random();
    PREFIX=SERVLETNAME+"."+DATESTR+"."+String.format("%03d",rand.nextInt(1000));

    if (DEBUG) errors.add("DEBUG MODE ON.");
    if (DBCON==null)
    {
      errors.add("PostgreSQL connection failed.");
      return false;
    }
    try {
      errors.add("PostgreSQL connection ok ("+DBHOST+":"+DBPORT+"/"+DBNAME+").");
      errors.add(DBCON.serverStatusTxt());
      errors.add("<PRE>database: "+DBNAME+"\n"+carlsbad_utils.DBDescribeTxt(DBCON)+"</PRE>");
    }
    catch (Exception e)
    {
      errors.add("PostgreSQL connection failed: "+e.getMessage());
      return false;
    }

    dbids = new ArrayList<String>(Arrays.asList("carlsbad","cb2","cb3"));

    PurgeTgtListCache(TGTLISTCACHE,SCRATCH_RETIRE_SEC);
    PurgeCpdListCache(CPDLISTCACHE,SCRATCH_RETIRE_SEC);
    PurgeCcpListCache(CCPLISTCACHE,SCRATCH_RETIRE_SEC);

    if (mrequest==null) return true;

    /// Stuff for a run:

    for (Enumeration e=mrequest.getParameterNames(); e.hasMoreElements(); )
    {
      String key=(String)e.nextElement();
      if (mrequest.getParameter(key)!=null)
        params.setVal(key,mrequest.getParameter(key));
    }

    if (mrequest.getParameter("changemode").equalsIgnoreCase("TRUE"))
      return true;

    if (DEBUG)
    {
      errors.add(CONTEXT.getServerInfo()+" [API:"+CONTEXT.getMajorVersion()+"."+CONTEXT.getMinorVersion()+"]");

      //errors.add("DEBUG: user-agent: "+REMOTEAGENT);
      //errors.add("DEBUG: cpdlists remaining in cache: "+CPDLISTCACHE.size());
      //for (String key: CPDLISTCACHE.keySet()) errors.add("DEBUG: remaining cpdlist: "+key);
      //errors.add("DEBUG: tgtlists remaining in cache: "+TGTLISTCACHE.size());
      //for (String key: TGTLISTCACHE.keySet()) errors.add("DEBUG: remaining tgtlist: "+key);

      errors.add("DEBUG: TargetList count: "+TARGETLIST.size());
      errors.add("DEBUG: TargetList nsts: "+TARGETLIST.nstCount());
      errors.add("DEBUG: TargetList names: "+TARGETLIST.nameCount());
      errors.add("DEBUG: TargetList timestamp: "+TARGETLIST.getTimestamp().toString());
      int n_err=0;
      for (int tid: TARGETLIST.keySet())
      {
        Target tgt=TARGETLIST.get(tid);
        String nst=tgt.getName()+":"+tgt.getSpecies()+":"+tgt.getType();
        if (TARGETLIST.nst2ID(nst)==null || !TARGETLIST.nst2ID(nst).equals(tid))
        {
          errors.add("ERROR: TargetList tid: "+tid+", nst2ID(\""+nst+"\") incorrect: "+TARGETLIST.nst2ID(nst));
          ++n_err;
        }
      }
      errors.add("TargetList nst errors: "+n_err);
    }
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void PurgeTgtListCache(HashMap<String,TargetList> tgtlistcache,int expire_sec)
  {
    HashSet<String> purgekeys = new HashSet<String>();
    for (String key: tgtlistcache.keySet())
    {
      TargetList tgtlist=tgtlistcache.get(key);
      if (((new java.util.Date()).getTime()-tgtlist.getTimestamp().getTime()) > 1000*expire_sec) purgekeys.add(key);
    }
    for (String key: purgekeys) tgtlistcache.remove(key);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void PurgeCpdListCache(HashMap<String,CompoundList> cpdlistcache,int expire_sec)
  {
    HashSet<String> purgekeys = new HashSet<String>();
    for (String key: cpdlistcache.keySet())
    {
      CompoundList cpdlist=cpdlistcache.get(key);
      if (((new java.util.Date()).getTime()-cpdlist.getTimestamp().getTime()) > 1000*expire_sec) purgekeys.add(key);
    }
    for (String key: purgekeys) cpdlistcache.remove(key);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void PurgeCcpListCache(HashMap<String,CCPList> ccplistcache,int expire_sec)
  {
    HashSet<String> purgekeys = new HashSet<String>();
    for (String key: ccplistcache.keySet())
    {
      CCPList ccplist=ccplistcache.get(key);
      if (((new java.util.Date()).getTime()-ccplist.getTimestamp().getTime()) > 1000*expire_sec) purgekeys.add(key);
    }
    for (String key: purgekeys) ccplistcache.remove(key);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm(MultipartRequest mrequest,HttpServletResponse response,HttpParams params,
	String formmode)
  {
    if (formmode==null) formmode="disease"; //default

    String formmode_target=""; String formmode_drug=""; String formmode_disease="";
    if (formmode.equals("drug")) formmode_drug="CHECKED";
    else if (formmode.equals("disease")) formmode_disease="CHECKED";
    else if (formmode.equals("target")) formmode_target="CHECKED";
    else formmode_disease="CHECKED";

    String imghtm=("<IMG BORDER=0 SRC=\""+((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/images/BatAlone_48x36.png\" HEIGHT=28>");
    String tiphtm=("CARLSBAD Project, UNM Translational Informatics Division");
    String href=("http://carlsbad.health.unm.edu");
    String logo_htm=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));

    String htm=(
    "<FORM NAME=\"mainform\" METHOD=POST ACTION=\""+response.encodeURL(SERVLETNAME)+"\""
    +" ENCTYPE=\"multipart/form-data\">\n"
    +"<TABLE WIDTH=\"100%\"><TR><TD VALIGN=MIDDLE><H1>"+APPNAME+" &nbsp; "+logo_htm+"</H1></TD>\n"
    +"<TD ALIGN=LEFT> - bioactivity knowlegebase &amp; hypotheses via chemical patterns</TD>\n");
    htm+="<INPUT TYPE=HIDDEN NAME=\"dbid\" VALUE=\""+params.getVal("dbid")+"\">\n";
    htm+="<INPUT TYPE=HIDDEN NAME=\"scaf_min\" VALUE=\""+params.getVal("scaf_min")+"\">\n";
    htm+=(
    "<TD ALIGN=RIGHT>\n"
    +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>Help</B></BUTTON>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"?formmode="+formmode+"')\"><B>Reset</B></BUTTON>\n")
    +"</TD></TR></TABLE>\n"
    +"<INPUT TYPE=HIDDEN NAME=\"search\">\n"
    +"<INPUT TYPE=HIDDEN NAME=\"changemode\">\n"
    +"<INPUT TYPE=HIDDEN NAME=\"qbuff\">\n" //qbuff query buffer used because the auto-suggest JS loses query text.
    +"<INPUT TYPE=HIDDEN NAME=\"cid\">\n"
    +"<INPUT TYPE=HIDDEN NAME=\"kid\">\n"
    +"<INPUT TYPE=HIDDEN NAME=\"tid\">\n"
    +"<INPUT TYPE=HIDDEN NAME=\"subnet_title\">\n"
    +"<HR>\n"
    +"<TABLE WIDTH=100% CELLPADDING=5 CELLSPACING=5>\n"
    +"<TR BGCOLOR=\"#CCCCCC\">\n"
    );
    htm+=(
    "<TD VALIGN=TOP><FIELDSET STYLE=\"height:100%;\"><LEGEND><B>mode:</B></LEGEND>\n"
    +"<TABLE WIDTH=\"100%\">\n"
    +("<TR><TD><INPUT TYPE=RADIO NAME=\"formmode\" VALUE=\"disease\"")
    +(" onClick=\"javascript:go_changemode(document.mainform)\" "+formmode_disease+">disease</TD>")
    +("<TD><a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?viewdiseases=TRUE','diseaseswin','width=600,height=800,scrollbars=1,resizable=1')\"><I>[browse]</I></a>")
    +"</TD></TR>\n"
    +("<TR><TD><INPUT TYPE=RADIO NAME=\"formmode\" VALUE=\"drug\"")
    +(" onClick=\"javascript:go_changemode(document.mainform)\" "+formmode_drug+">drug</TD>")
    +("<TD><a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?viewcompounds=TRUE&etag=drugs','cpdsswin','width=600,height=800,scrollbars=1,resizable=1')\"><I>[browse]</I></a>")
    +"</TD></TR>\n"
    +"<TR><TD><INPUT TYPE=RADIO NAME=\"formmode\" VALUE=\"target\""
    +(" onClick=\"javascript:go_changemode(document.mainform)\" "+formmode_target+">target</TD>")
    +("<TD><a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?viewtargets=TRUE&species=human&etag=all','targetswin','width=600,height=800,scrollbars=1,resizable=1')\"><I>[browse]</I></a>")
    +"</TD></TR></TABLE></FIELDSET>\n"
    +"</TD>\n"
    );
    htm+=(
      "<TD WIDTH=\"50%\" VALIGN=TOP>\n"
      +"<FIELDSET STYLE=\"height:100%;\"><LEGEND><B>"+formmode+" query:</B></LEGEND>\n"
      +"&nbsp;name:<BR>\n"
    );
    if (formmode.equals("drug"))
    {
      htm+=(
      "<div class=\"ui-widget\">\n"
      +"  &nbsp;<INPUT SIZE=40 ID=\"drugname\" VALUE=\""+params.getVal("qbuff")+"\" />\n"
      +"</div>\n"
      +"&nbsp;<I>("+AUTOSUGGESTMINLEN+"+ chars triggers auto-suggest...)<BR>\n"
      +"<INPUT TYPE=HIDDEN NAME=\"diseasename\">\n"
      +"<INPUT TYPE=HIDDEN NAME=\"tgtnst\">\n"
      +"<BR>\n"
      +"<BR>\n"
      +"</TD>\n"
      );
    }
    else if (formmode.equals("disease"))
    {
      htm+=(
      "<div class=\"ui-widget\">\n"
      +"  &nbsp;<INPUT SIZE=40 ID=\"diseasename\" VALUE=\""+params.getVal("qbuff")+"\" />\n"
      +"</div>\n"
      +"&nbsp;<I>("+AUTOSUGGESTMINLEN+"+ chars triggers auto-suggest...)<BR>\n"
      +"<INPUT TYPE=HIDDEN NAME=\"drugname\">\n"
      +"<INPUT TYPE=HIDDEN NAME=\"tgtnst\">\n"
      +"<BR>\n"
      +"<BR>\n"
      +"</TD>\n"
      );
    }
    else
    {
      htm+=(
      "<div class=\"ui-widget\">\n"
      +"  &nbsp;<INPUT SIZE=40 ID=\"tgtnst\" VALUE=\""+params.getVal("qbuff")+"\" />\n"
      +"</div>\n"
      +"&nbsp;<I>("+AUTOSUGGESTMINLEN+"+ chars triggers auto-suggest...)<BR>\n"
      +"<INPUT TYPE=HIDDEN NAME=\"diseasename\">\n"
      +"<INPUT TYPE=HIDDEN NAME=\"drugname\">\n"
      +"<BR>\n"
      +"<BR>\n"
      +"</FIELDSET></TD>\n"
      );
    }

//    htm+=(
//      "<TD WIDTH=\"30%\" VALIGN=TOP>\n"
//      +"<FIELDSET STYLE=\"height:100%;\"><LEGEND><B>filters:</B></LEGEND>\n"
//      +"<TABLE WIDTH=\"100%\">\n"
//      +"<TR><TD ALIGN=RIGHT>scaffold weight [0-1]:</TD>\n"
//      +"<TD><INPUT TYPE=\"TEXT\" NAME=\"scaf_min\" SIZE=3 VALUE=\""+params.getVal("scaf_min")+"\"></TD></TR>\n"
//      +"</TR></TABLE>\n"
//      +"<P></P><P></P><P></P>\n" //kludge to expand fieldset box
//      +"</FIELDSET></TD>\n"
//      );

    htm+=(
    "</TR>\n"
    +"<TR><TD COLSPAN=3 ALIGN=CENTER>"
    +"<BUTTON CLASS=\"cb_button\" TYPE=BUTTON onClick=\"javascript:go_search(this.form,'"+formmode+"')\">Search Carlsbad</BUTTON>"
    +"</TD></TR>\n"
    +"</TABLE>\n"
    +"</FORM>\n"
    );
    htm+=("<HR>\n<CENTER>"+IntroHtm()+"</CENTER>");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /*	TARGETNSTS associates TIDs with <NAME>:<SPECIES>:<TYPE> keys.
  */
  private static String JavaScript() throws IOException
  {
    String js=("");
    js+=("var TARGETNSTS = {\n");
    int i=0;
    for (int tid: TARGETLIST.keySet())
    {
      Target tgt=TARGETLIST.get(tid);
      String name=tgt.getName();
      String species=tgt.getSpecies();
      String ttype=tgt.getType();
      String key=name+":"+species+":"+ttype;
      if (species==null || !species.equals("human")) continue;
      js+=(i++>0?",\n":"");
      js+=("\""+key+"\":"+tid+"");
    }
    js+=("\n};\n");
    js+=("var DRUGNAMES = {\n");
    i=0;
    for (int cid: DRUGLIST.keySet())
    {
      for (String drugname: DRUGLIST.get(cid).getSynonymsSorted())
      {
        js+=(i++>0?",\n":"");
        js+=("\""+(drugname)+"\":"+cid+"");
      }
    }
    js+=("\n};\n");
    js+=("var DISEASENAMES = {\n");
    i=0;
    for (String kid: DISEASELIST.keySet())
    {
      js+=(i++>0?",\n":"");
      js+=("\""+(DISEASELIST.get(kid).getName().replaceAll("\"",""))+"\":\""+kid+"\"");
    }
    js+=("\n};\n");
    js+=(
"function kid2name(kid)\n"+
"{\n"+
"  for (name in DISEASENAMES)\n"+
"  {\n"+
"    if (kid==DISEASENAMES[name]) { return name; }\n"+
"  }\n"+
"  return '';\n"+
"}\n"+
"function cid2name(cid)\n"+
"{\n"+
"  //Try to return best name among several.\n"+
"  for (name in DRUGNAMES) { if (cid==DRUGNAMES[name] && name.match(/^[A-Z][a-z]+$/)) { return name; } }\n"+
"  for (name in DRUGNAMES) { if (cid==DRUGNAMES[name] && name.match(/^[A-Za-z ]+$/)) { return name; } }\n"+
"  for (name in DRUGNAMES) { if (cid==DRUGNAMES[name]) { return name; } }\n"+
"  return '';\n"+
"}\n"+
"function tid2nst(tid)\n"+
"{\n"+
"  for (nst in TARGETNSTS)\n"+
"  {\n"+
"    if (tid==TARGETNSTS[nst]) { return nst; }\n"+
"  }\n"+
"  return '';\n"+
"}\n"+
"function go_changemode(form) //change mode, keep settings\n"+
"{\n"+
"  form.changemode.value='TRUE';\n"+
"  form.submit();\n"+
"}\n"+
"function go_init(form,changemode) //init as needed\n"+
"{\n"+
"  var i;\n"+
"  if (changemode) return;\n"+
"  form.dbid.value='"+DBNAME+"';\n"+
"  form.scaf_min.value='0.2';\n"+
"}\n"+
"function checkform(form,formmode)\n"+
"{\n"+
"  if (formmode=='target')\n"+
"  {\n"+
"    if (!form.tgtnst.value && !form.tid.value)\n"+
"    {\n"+
"      alert('ERROR: query target required.');\n"+
"      return false;\n"+
"    }\n"+
"    else if (form.tgtnst.value && !(form.tgtnst.value in TARGETNSTS))\n"+
"    {\n"+
"      alert('ERROR: query target \"'+form.tgtnst.value+'\" not found; select from autosuggest recommended.');\n"+
"      return false;\n"+
"    }\n"+
"  }\n"+
"  else if (formmode=='drug')\n"+
"  {\n"+
"    if (!form.drugname.value && !form.cid.value)\n"+
"    {\n"+
"      alert('ERROR: query drug required.');\n"+
"      return false;\n"+
"    }\n"+
"    else if (form.drugname.value && !(form.drugname.value in DRUGNAMES))\n"+
"    {\n"+
"      alert('ERROR: query drug \"'+form.drugname.value+'\" not found; select from autosuggest recommended.');\n"+
"      return false;\n"+
"    }\n"+
"  }\n"+
"  else if (formmode=='disease')\n"+
"  {\n"+
"    if (!form.diseasename.value && !form.kid.value)\n"+
"    {\n"+
"      alert('ERROR: query disease required.');\n"+
"      return false;\n"+
"    }\n"+
"    else if (form.diseasename.value && !(form.diseasename.value in DISEASENAMES))\n"+
"    {\n"+
"      alert('ERROR: query disease \"'+form.diseasename.value+'\" not found; select from autosuggest recommended.');\n"+
"      return false;\n"+
"    }\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"function go_search(form,formmode)\n"+
"{\n"+
"  if (!checkform(form,formmode)) return;\n"+
"  //NOTE: console.log() NOT valid for IE.\n"+
"  //if (typeof form.drugname!='undefined') console.log('DEBUG: drugname='+form.drugname.value);\n"+
"  //if (typeof form.diseasename!='undefined') console.log('DEBUG: diseasename='+form.diseasename.value);\n"+
"  //if (typeof form.tgtnst!='undefined') console.log('DEBUG: tgtnst='+form.tgtnst.value);\n"+
"\n"+
"  var x=300;\n"+
"  if (typeof window.screenX!='undefined') x+=window.screenX;\n"+
"  else x+=window.screenLeft; //IE\n"+
"  var y=300;\n"+
"  if (typeof window.screenY!='undefined') y+=window.screenY;\n"+
"  else y+=window.screenTop; //IE\n"+
"  var pwin=window.open('','"+(SERVLETNAME+"_progress_win")+"',\n"+
"  'width=400,height=100,left='+x+',top='+y+',scrollbars=1,resizable=1,location=0,status=0,toolbar=0');\n"+
"  if (!pwin) {\n"+
"    alert('ERROR: popup windows must be enabled for progress indicator.');\n"+
"    return false;\n"+
"  }\n"+
"  pwin.focus();\n"+
"  pwin.document.close(); //if window exists, clear\n"+
"  pwin.document.open('text/html');\n"+
"  pwin.document.writeln('<HTML><HEAD>');\n"+
"  pwin.document.writeln('<LINK REL=\"stylesheet\" type=\"text/css\" HREF=\""+((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/css/biocomp.css\" />');\n"+
"  pwin.document.writeln('</HEAD><BODY BGCOLOR=\"#DDDDDD\">');\n"+
"  pwin.document.writeln('"+APPNAME+"...<BR>');\n"+
"  pwin.document.writeln('"+DateFormat.getDateInstance(DateFormat.FULL).format(new java.util.Date())+"<BR>');\n"+
"\n"+
"  if (navigator.appName.indexOf('Explorer')<0)\n"+
"    pwin.document.title='"+APPNAME+" progress'; //not-ok for IE\n"+
"\n"+
"  form.search.value='TRUE';\n"+
"  if (formmode=='drug')\n"+
"  {\n"+
"    //console.log('DEBUG: form.drugname.value:\\''+form.drugname.value+'\\'');\n"+
"    form.qbuff.value=form.drugname.value;\n"+
"  }\n"+
"  else if (formmode=='disease')\n"+
"  {\n"+
"    //console.log('DEBUG: form.diseasename.value:\\''+form.diseasename.value+'\\'');\n"+
"    form.qbuff.value=form.diseasename.value;\n"+
"  }\n"+
"  else if (formmode=='target')\n"+
"  {\n"+
"    //console.log('DEBUG: form.tgtnst.value:\\''+form.tgtnst.value+'\\'');\n"+
"    form.qbuff.value=form.tgtnst.value;\n"+
"  }\n"+
"  form.subnet_title.value=form.qbuff.value.replace('\\'','\\\\\\'');\n"+
"  //console.log('DEBUG: form.qbuff.value:\\''+form.qbuff.value+'\\'');\n"+
"  pwin.document.writeln('query: '+form.qbuff.value.replace('\\'','\\\\\\'')+'<BR>');\n"+
"  form.submit();\n"+
"}\n"+
"function go_demoquery(form,formmode,id)\n"+
"{\n"+
"  for (i=0;i<form.formmode.length;++i) //radio\n"+
"    if (form.formmode[i].value==formmode)\n"+
"      form.formmode[i].checked=true;\n"+
"  if (formmode=='drug')\n"+
"  {\n"+
"    form.cid.value=id;\n"+
"    form.drugname.value=cid2name(id);\n"+
"  }\n"+
"  else if (formmode=='disease')\n"+
"  {\n"+
"    form.kid.value=id;\n"+
"    form.diseasename.value=kid2name(id);\n"+
"  }\n"+
"  else if (formmode=='target')\n"+
"  {\n"+
"    form.tid.value=id;\n"+
"    form.tgtnst.value=tid2nst(id);\n"+
"  }\n"+
"  go_search(form,formmode);\n"+
"}\n"+
"function progresswin_close()\n"+
"{\n"+
"  if (typeof pwin != 'undefined')\n"+
"  {\n"+
"    if (typeof pwin.parent != 'undefined')\n"+
"      pwin.parent.focus();\n"+
"    pwin.focus();\n"+
"    pwin.close();\n"+
"  }\n"+
"}\n"+
"function keys(o)\n"+
"{\n"+
"  var ks=[];\n"+
"  for (var k in o) { ks.push(k); }\n"+
"  return ks;\n"+
"}\n"+
"function values(o)\n"+
"{\n"+
"  var vs=[];\n"+
"  for (var k in o) { vs.push(o[k]); }\n"+
"  return vs;\n"+
"}\n"+
"$(function() { //jquery widget\n"+
"  $( \"#tgtnst\" ).autocomplete({\n"+
"    source: keys(TARGETNSTS),\n"+
"    minLength: "+AUTOSUGGESTMINLEN+",\n"+
"    delay: 500,\n"+
"    autoFocus: false\n"+
"  });\n"+
"  $( \"#drugname\" ).autocomplete({\n"+
"    source: keys(DRUGNAMES),\n"+
"    minLength: "+AUTOSUGGESTMINLEN+",\n"+
"    delay: 500,\n"+
"    autoFocus: false\n"+
"  });\n"+
"  $( \"#diseasename\" ).autocomplete({\n"+
"    source: keys(DISEASENAMES),\n"+
"    minLength: "+AUTOSUGGESTMINLEN+",\n"+
"    delay: 500,\n"+
"    autoFocus: false\n"+
"  });\n"+
"});\n");
    return js;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm(DBCon dbcon) throws IOException
  {
    String htm="";
    if (HELP_FILE!=null)
    {
      File helpfile = new File(HELP_FILE);
      BufferedReader buff = new BufferedReader(new FileReader(helpfile));
      String line;
      while (buff!=null && (line=buff.readLine())!=null) htm+=(line+"\n");
    }

    String dbinfo="";
    dbinfo+=(APPNAME+" Diseases: "+DISEASELIST.size()+" loaded:"+DISEASELIST.getTimestamp()+"\n");
    dbinfo+=(APPNAME+" Drugs: "+DRUGLIST.size()+" synonyms: "+DRUGLIST.synonymCount()+" loaded: "+DRUGLIST.getTimestamp()+"\n");
    dbinfo+=(APPNAME+" Targets: "+TARGETLIST.size()+" loaded: "+TARGETLIST.getTimestamp()+"\n");
    try { dbinfo+=carlsbad_utils.DBDescribeTxt(dbcon); }
    catch (SQLException e) { dbinfo+="ERROR: "+e.getMessage(); }

    htm+=(
    "<HR><H3>Database contents</H3>\n"
    +"<BLOCKQUOTE><TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5><TR><TD BGCOLOR=WHITE>"
    +"<PRE>"+dbinfo+"</PRE>"
    +"</TD></TR></TABLE></BLOCKQUOTE>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String IntroHtm()
  {
    ArrayList<String> kids_demo = new ArrayList<String>(Arrays.asList(
	"ds:H00003",	//Acute myeloid leukemia (AML)
	//"ds:H00083",	//Allograft rejection
	//"ds:H00022",	//Bladder cancer
	"ds:H00043",	//Neuroblastoma
	"ds:H00013",	//Small cell lung cancer
	//"ds:H00032"	//Thyroid cancer
	"ds:H00408"	//Type I diabetes mellitus
	));
    ArrayList<Integer> tids_demo = new ArrayList<Integer>(Arrays.asList(
	1730,	//GABA A Alpha4Beta3Gamma2
	//140,	//Histamine H2 receptor
	3072,	//NADP-dependent malic enzyme
	//445,	//Prostaglandin F2-alpha receptor
	3274	//Rhodopsin kinase
	));
    ArrayList<Integer> cids_demo = new ArrayList<Integer>(Arrays.asList(
	16563,	//Claritin
	55042,	//Ketorolac
	8797,	//Ketotifen
	5442,	//Marinol
	6070,	//Methotrexate
	70123,	//Midazolam
	874,	//Rofecoxib
	1550,	//Sildenafil
	14856	//Tamoxifen
	));

    String htm=
      "Introducing "+APPNAME+", bioactivity data-driven knowledge discovery portal. Try example queries:<BR/>\n"
      +"<TABLE CELLPADDING=1>" +"<TR><TD ALIGN=RIGHT>diseases:</TD><TD ALIGN=CENTER BGCOLOR=WHITE>";

   for (String kid: kids_demo)
     htm+=" &sdot; <a href=\"javascript:void(0)\" onClick=\"javascript:go_demoquery(window.document.mainform,'disease','"+kid+"')\">"+DISEASELIST.get(kid).getName()+"</a>\n";

    htm+= " &sdot;</TD</TR>\n"+"<TR><TD ALIGN=RIGHT>drugs:</TD><TD ALIGN=CENTER BGCOLOR=WHITE>";

   for (Integer cid: cids_demo)
     htm+=" &sdot; <a href=\"javascript:void(0)\" onClick=\"javascript:go_demoquery(window.document.mainform,'drug','"+cid+"')\">"+DRUGLIST.get(cid).getName()+"</a>\n";

    htm+= " &sdot;</TD</TR>\n"+"<TR><TD ALIGN=RIGHT>targets:</TD><TD ALIGN=CENTER BGCOLOR=WHITE>";

   for (Integer tid: tids_demo)
     htm+=" &sdot; <a href=\"javascript:void(0)\" onClick=\"javascript:go_demoquery(window.document.mainform,'target','"+tid+"')\">"+TARGETLIST.get(tid).getName()+"</a>\n";

    htm+= " &sdot;</TD</TR>\n"+"</TABLE>\n";
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String TargetQueryHtm(int tid,HttpServletResponse response,String servletname)
  {
    Target tgt=TARGETLIST.get(tid);
    String htm="<TABLE WIDTH=\"80%\" BORDER=0 CELLSPACING=2 CELLPADDING=2>";
    htm+=("<TR><TH WIDTH=\"20%\"></TH><TH WIDTH=\"40%\"></TH></TR>\n");
    htm+=("<TR><TD ALIGN=\"right\"><B>tid</B></TD><TD BGCOLOR=\"white\">");
    htm+=("<A HREF=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewtarget=TRUE&tid="+tid+"','tgtwin','width=400,height=600,scrollbars=1,resizable=1')\">"+tid+"</A></TD></TR>\n");
    htm+=("<TR><TD ALIGN=\"right\"><B>name</B></TD><TD BGCOLOR=\"white\">"+tgt.getName()+"</TD></TR>\n");
    htm+=("<TR><TD ALIGN=\"right\"><B>type</B></TD><TD BGCOLOR=\"white\">"+tgt.getType()+"</TD></TR>\n");
    htm+=("<TR><TD ALIGN=\"right\"><B>species</B></TD><TD BGCOLOR=\"white\">"+tgt.getSpecies()+"</TD></TR>\n");
    htm+=("<TR><TD ALIGN=\"right\"><B>descr</B></TD><TD BGCOLOR=\"white\">"+tgt.getDescription()+"</TD></TR>\n");
    htm+=("</TABLE>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private String CompoundQueryHtm(int cid,CompoundList druglist,DBCon dbcon,String mol2img_servleturl,
	HttpServletResponse response,String servletname)
  {
    String htm="<TABLE CELLSPACING=5 CELLPADDING=5 WIDTH=\"60%\">";
    String imghtm="";
    try {
      String smiles=carlsbad_utils.CID2Smiles(dbcon,cid);
      String depopts=("mode=cow&imgfmt=png&kekule=true");
      imghtm=HtmUtils.Smi2ImgHtm(smiles, depopts, 100, 140, mol2img_servleturl, true, 4, "go_zoom_smi2img");
    }
    catch (SQLException e) { this.errors.add("CID2Smiles error: "+e.getMessage()); }
    catch (Exception e) { this.errors.add("CID2Smiles error: "+e.getMessage()); }
    htm+=("<TR><TD WIDTH=\"20%\" ALIGN=\"center\" BGCOLOR=\"white\">"+imghtm+"</TD>\n");
    htm+=("<TD WIDTH=\"10%\" BGCOLOR=\"white\" ALIGN=\"center\" VALIGN=\"top\">");

    htm+=("<A HREF=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewcompound=TRUE&cid="+cid+"','cpdwin','width=600,height=600,scrollbars=1,resizable=1')\">"+cid+"</A>\n");
    htm+=("</TD>\n");

    htm+=("<TD WIDTH=\"30%\" BGCOLOR=\"white\" VALIGN=\"top\"><UL>\n");
    for (String drugname: druglist.get(cid).getSynonymsSorted()) htm+=("<LI>"+drugname+"<BR>\n");
    htm+=("</UL></TD></TR>\n");
    htm+=("</TABLE>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Output disease query.
  */
  private String DiseaseQueryHtm(String kid,HttpServletResponse response,String servletname)
  {
    String diseasename=DISEASELIST.get(kid).getName();
    String kegg_url="http://www.kegg.jp/dbget-bin/www_bget";
    String imghtm=("<IMG ALIGN=MIDDLE BORDER=0 HEIGHT=50 SRC=\""+((PROXY_PREFIX!=null)?PROXY_PREFIX:"")+CONTEXTPATH+"/images/kegg_logo.gif\">");
    String kegg_butt="<BUTTON TYPE=BUTTON onClick=\"void window.open('"+kegg_url+"?"+kid+"','keggwin','')\">View in KEGG"+imghtm+"</BUTTON>";

    String htm="<TABLE WIDTH=\"80%\" CELLPADDING=5 CELLSPACING=5>";
    htm+=("<TR><TH WIDTH=\"50%\">disease</TH><TH>KEGG ID</TH></TR>\n");
    htm+=("<TR BGCOLOR=\"white\">");
    htm+=("<TD ALIGN=\"center\"><H3>"+diseasename+"</H3></TD>\n");
    htm+=("<TD ALIGN=\"center\">");
    htm+=("<A HREF=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewdisease=TRUE&kid="+kid+"','diseasewin','width=600,height=600,scrollbars=1,resizable=1')\">"+kid+"</A>\n");
    htm+=(kegg_butt+"</TD>");
    htm+=("</TR></TABLE>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Reads diseases, drugs, targets into container objects.
	By doing this here in init(), then it happens only once
	for each servlet deployment and not each instantiation.
  */
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    CONTEXT=getServletContext();
    CONTEXTPATH=CONTEXT.getContextPath();

    // read servlet parameters (from web.xml):
    UPLOADDIR=conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null)
      throw new ServletException("Please supply UPLOADDIR parameter");
    SCRATCHDIR=conf.getInitParameter("SCRATCHDIR");
    if (SCRATCHDIR==null) SCRATCHDIR="/tmp";
    LOGDIR=conf.getInitParameter("LOGDIR");
    if (LOGDIR==null) LOGDIR="/tmp"+CONTEXTPATH+"_logs";
    HELP_FILE=CONTEXT.getRealPath("")+"/"+conf.getInitParameter("HELP_FILE");
    DBHOST=conf.getInitParameter("DBHOST");
    if (DBHOST==null)
      throw new ServletException("Please supply DBHOST parameter");
    DBNAME=conf.getInitParameter("DBNAME");
    if (DBNAME==null)
      throw new ServletException("Please supply DBNAME parameter");
    DBSCHEMA=conf.getInitParameter("DBSCHEMA");
    if (DBSCHEMA==null)
      throw new ServletException("Please supply DBSCHEMA parameter");
    DBUSR=conf.getInitParameter("DBUSR");
    if (DBUSR==null)
      throw new ServletException("Please supply DBUSR parameter");
    DBPW=conf.getInitParameter("DBPW");
    if (DBPW==null)
      throw new ServletException("Please supply DBPW parameter");
    APPNAME=conf.getInitParameter("APPNAME");
    if (APPNAME==null) { APPNAME=this.getServletName(); }
    try { DBPORT=Integer.parseInt(conf.getInitParameter("DBPORT")); }
    catch (NumberFormatException e) { DBPORT=5432; }
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=10000; }
    try { CYVIEW=conf.getInitParameter("CYVIEW"); }
    catch (Exception e) { CYVIEW="cyview"; }
    DEBUG=(conf.getInitParameter("DEBUG")!=null && conf.getInitParameter("DEBUG").equalsIgnoreCase("true"));
    PROXY_PREFIX=((conf.getInitParameter("PROXY_PREFIX")!=null)?conf.getInitParameter("PROXY_PREFIX"):"");

    // This connection only used for deployment and one-time initialization of lists.
    DBCon dbcon=null;
    try { dbcon = new DBCon("postgres", DBHOST, DBPORT, DBNAME, DBUSR, DBPW); }
    catch (Exception e) {
      CONTEXT.log("ERROR: PostgreSQL connection failed.",e);
      throw new ServletException("ERROR: PostgreSQL connection failed.",e);
    }

    // Read target names & TIDs, etc. from Carlsbad db.
    // Parse into persistent TargetList object.
    try {
      TARGETLIST = new TargetList();
      TARGETLIST.loadAll(dbcon);
      CONTEXT.log("targetlist count: "+TARGETLIST.size());
      CONTEXT.log("targetlist name count: "+TARGETLIST.nameCount());
      CONTEXT.log("targetlist nst count: "+TARGETLIST.nstCount());
      CONTEXT.log("targetlist timestamp: "+TARGETLIST.getTimestamp().toString());
      int n_err=0;
      for (int tid: TARGETLIST.keySet())
      {
        Target tgt=TARGETLIST.get(tid);
        String nst=tgt.getName()+":"+tgt.getSpecies()+":"+tgt.getType();
        if (TARGETLIST.nst2ID(nst)==null || !TARGETLIST.nst2ID(nst).equals(tid))
        {
          CONTEXT.log("ERROR: tid: "+tid+", nst2ID(\""+nst+"\") incorrect: "+TARGETLIST.nst2ID(nst));
          ++n_err;
        }
      }
      CONTEXT.log("targetlist nst errors: "+n_err);
    }
    catch (Exception e) {
      CONTEXT.log("ERROR: problem reading targetlist.",e);
      throw new ServletException("ERROR: problem reading targetlist.",e);
    }

    // Read drug names & CIDs, etc. from Carlsbad db.
    // Parse into persistent DrugList object.
    try {
      DRUGLIST = new CompoundList();
      DRUGLIST.loadAllDrugs(dbcon);
      //for (int cid: DRUGLIST.keySet())
      //  for (String drugname: DRUGLIST.get(cid).getSynonymList())
      //    CONTEXT.log("\""+drugname+"\","+cid);
      CONTEXT.log("druglist count: "+DRUGLIST.size());
      CONTEXT.log("druglist synonym count: "+DRUGLIST.synonymCount());
      CONTEXT.log("druglist timestamp: "+DRUGLIST.getTimestamp().toString());
    }
    catch (Exception e) {
      CONTEXT.log("ERROR: problem reading druglist.",e);
      throw new ServletException("ERROR: problem reading druglist.",e);
    }

    // Get disease names, KIDs, & TIDs from Carlsbad db.
    // Parse into persistent DiseaseList object.
    try {
      DISEASELIST = new DiseaseList();
      DISEASELIST.loadAll(dbcon);
      //for (String kid: DISEASELIST.keySet())
      //  CONTEXT.log(kid+",\""+DISEASELIST.get(kid).getName()+"\"");
      CONTEXT.log("diseaselist count: "+DISEASELIST.size());
      CONTEXT.log("diseaselist timestamp: "+DISEASELIST.getTimestamp().toString());
    }
    catch (Exception e) {
      CONTEXT.log("ERROR: problem reading diseaselist.",e);
      throw new ServletException("ERROR: problem reading diseaselist.",e);
    }

    TGTLISTCACHE = new HashMap<String,TargetList>(); //For caching tgtlists, reuse via cachekey.
    CPDLISTCACHE = new HashMap<String,CompoundList>(); //For caching cpdlists, reuse via cachekey.
    CCPLISTCACHE = new HashMap<String,CCPList>(); //For caching ccplists, reuse via cachekey.

    try { if (dbcon!=null) dbcon.close(); }
    catch (Exception e) { CONTEXT.log("ERROR: problem closing connection.",e); }
  }

  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    doPost(request, response);
  }
  /////////////////////////////////////////////////////////////////////////////
  public String getServletInfo()
  {
    return
    "Author: Jeremy J Yang,\n"+
    "Translational Informatics Division,\n"+
    "http://medicine.unm.edu/informatics/,\n"+
    "University of New Mexico,\n"+
    "Albuquerque, New Mexico, USA.";
  }
}
