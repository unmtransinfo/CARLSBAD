package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.net.*; //URLEncoder,InetAddress
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

//import cytoscape.CytoscapeVersion;

import chemaxon.struc.*;
import chemaxon.formats.*;

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;
import edu.unm.health.biocomp.util.db.*;
import edu.unm.health.biocomp.util.threads.*;
import edu.unm.health.biocomp.smarts.smarts_utils;
import edu.unm.health.biocomp.cytoscape.*;

/**	Uses PostgreSQL JDBC &amp; chemical cartrige gNova Chord DB.
	<br>
	to do:<ul>
	  <li> n_mol column in target table
	  <li> some warning if query will return huge subnet.  Maybe a heuristic based
	      on avg connectivity.  Note that neighbortargets can be a huge increase.
	  <li> option: show compounds in a separate table, up to n_max_view
	</ul><br>
	Dependencies: Cytoscape, JChem, COS, PGSql, Log4j, commons-codec (install shared libs).
	<br>
	@author Jeremy J Yang
*/
public class snake_servlet extends HttpServlet
{
  //private static ServletConfig CONFIG=null;
  private static String SERVLETNAME=null;
  private static ServletContext CONTEXT=null;
  private static String CONTEXTPATH=null;
  private static String LOGDIR=null;
  private static String APPNAME=null;	// configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static Integer MAX_POST_SIZE=10*1024*1024; // configured in web.xml
  private static String SCRATCHDIR=null; // configured in web.xml
  private static Integer N_MAX=null;	// configured in web.xml
  private static Integer N_MAX_TARGETS=null;	// default configured in web.xml
  private static String DBHOST=null;    // configured in web.xml
  private static String DBID=null;      // configured in web.xml
  private static String DBSCHEMA=null;  // configured in web.xml
  private static Integer DBPORT=null;   // configured in web.xml
  private static String DBUSR=null;      // configured in web.xml
  private static String DBPW=null;      // configured in web.xml
  private static String CYWEBAPP=null;      // configured in web.xml
  private static String MARVWINCGI=null; // configured in web.xml
  private static String JSMEWINURL=null; // configured in web.xml
  private static String PREFIX=null;
  private static int scratch_retire_sec=3600;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  //private static int SERVERPORT=0;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static String DATESTR=null;
  private static File logfile=null;
  private static String color1="#EEEEEE";
  private static ArrayList<String> dbids=null;
  private static ArrayList<String> specieses=null;
  private static ArrayList<String> tgt_idtypes=null;
  private static ArrayList<String> cpd_idtypes=null;
  private static ArrayList<String> ttypes=null;
  private static ArrayList<String> tclasses=null;
  private static ArrayList<String> tclasses_selected=null;
  private static boolean DEBUG=false;

  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    //SERVERPORT=request.getServerPort();
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
    CONTEXTPATH=request.getContextPath();
    rb=ResourceBundle.getBundle("LocalStrings",request.getLocale());

    MultipartRequest mrequest=null;
    if (request.getMethod().equalsIgnoreCase("POST"))
    {
      try { mrequest=new MultipartRequest(request,UPLOADDIR,10*1024*1024,"ISO-8859-1",new DefaultFileRenamePolicy()); }
      catch (IOException lEx) { this.getServletContext().log("not a valid MultipartRequest",lEx); }
    }

    // main logic:
    ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList("biocomp.css"));
    //ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList("/marvin/marvin.js","biocomp.js","ddtip.js"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList("biocomp.js","ddtip.js"));
    boolean ok=Initialize(request,mrequest);
    String title=APPNAME+" (CARLSBAD subnet extractor)";
    if (!ok)
    {
      response.setContentType("text/html");
      out=response.getWriter();
      out.print(HtmUtils.HeaderHtm(title,jsincludes,cssincludes,JavaScript(),color1,request));
      out.print(HtmUtils.FooterHtm(errors,true));
    }
    else if (mrequest!=null)		//method=POST, normal operation
    {
      if (mrequest.getParameter("changemode").equalsIgnoreCase("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(FormHtm(mrequest,response,params.getVal("formmode")));
        out.println("<SCRIPT>go_init(window.document.mainform,'"+params.getVal("formmode")+"',true)</SCRIPT>");
        out.print(HtmUtils.FooterHtm(errors,true));
      }
      else if (mrequest.getParameter("restart").equalsIgnoreCase("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(FormHtm(mrequest,response,params.getVal("formmode")));
        out.println("<SCRIPT>go_init(window.document.mainform,'"+params.getVal("formmode")+"',false)</SCRIPT>");
        out.print(HtmUtils.FooterHtm(errors,true));
      }
      else if (mrequest.getParameter("search").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(FormHtm(mrequest,response,params.getVal("formmode")));
        out.flush();
        response.flushBuffer();
        java.util.Date t_i = new java.util.Date();
        Integer tid=null;	//for single target query
        try { tid=Integer.parseInt(params.getVal("tid")); } catch (Exception e) { }
        if (params.getVal("tgt_idtype").equalsIgnoreCase("CARLSBAD"))
        {
          try { tid=Integer.parseInt(params.getVal("tgt_id")); } catch (Exception e) { }
        }
        Integer mw_min=null;
        try { mw_min=Integer.parseInt(params.getVal("mw_min")); } catch (Exception e) { }
        Integer mw_max=null;
        try { mw_max=Integer.parseInt(params.getVal("mw_max")); } catch (Exception e) { }
        Integer c_id=null;
        if (params.getVal("cpd_idtype").equalsIgnoreCase("CARLSBAD"))
        {
          try { c_id=Integer.parseInt(params.getVal("cpd_id")); } catch (Exception e) { }
        }
        Integer s_id=null;
        try { s_id=Integer.parseInt(params.getVal("s_id")); } catch (Exception e) { }
        Integer m_id=null;
        try { m_id=Integer.parseInt(params.getVal("m_id")); } catch (Exception e) { }
        Float minsim=null;
        try { minsim=Float.parseFloat(params.getVal("minsim")); } catch (Exception e) { }

        ResultSet rset=null;
        ArrayList<Integer> tids = new ArrayList<Integer>();
        ArrayList<Integer> cids=null;
        ArrayList<Integer> scafids=null;
        ArrayList<Integer> mcesids=null;
        if (c_id!=null)
          cids = new ArrayList<Integer>(Arrays.asList(c_id));
        if (s_id!=null)
          scafids = new ArrayList<Integer>(Arrays.asList(s_id));
        if (m_id!=null)
          mcesids = new ArrayList<Integer>(Arrays.asList(m_id));

        String sql=carlsbad_utils.TargetQuerySQL(
          (tid==null?null:new ArrayList<Integer>(Arrays.asList(tid))),
          params.getVal("ttype"),
          (tclasses_selected.size()<tclasses.size()?tclasses_selected:null),
          params.getVal("tname"),params.getVal("matchtype_tname").equalsIgnoreCase("sub"),
          params.getVal("tdesc"),params.getVal("matchtype_tdesc").equalsIgnoreCase("sub"),
          params.getVal("species"),
          params.getVal("tgt_id"),params.getVal("tgt_idtype"),
          params.getVal("cpd_id"),params.getVal("cpd_idtype"),
          params.getVal("qcpd"),params.getVal("matchtype_qcpd"),minsim,
          params.getVal("cname"),params.getVal("matchtype_cname").equalsIgnoreCase("sub"),
          mw_min,mw_max,
          cids,scafids,mcesids,
          N_MAX_TARGETS);

        outputs.add("<H2>Results:</H2>");
        ArrayList<String> outputs_targetsearch = new ArrayList<String>();
        ArrayList<String> outputs_subnet = new ArrayList<String>();
        StringBuilder err_sb = new StringBuilder();
        if (sql==null)
        { outputs.add("ERROR: illegal query; this should not happen; please report."); }
        else
        {
          try { rset=app_utils.ExecuteSql_LaunchThread(DBHOST,DBPORT,params.getVal("dbid"),DBUSR,DBPW,sql,SERVLETNAME,response,out,N_MAX,err_sb); }
          catch (SQLException e) { outputs.add("PostgreSQL error: "+e.getMessage()); }
        }
        if (err_sb.length()>0) { outputs.add(err_sb.toString()); }
        if (params.isChecked("verbose")) errors.add("<PRE>"+sql+"</PRE>");
        if (rset!=null)
        {
          try {
            outputs_targetsearch.add(app_utils.TargetSearchResultsHtm(rset,mrequest,response,tids,params,SERVLETNAME,PREFIX,SCRATCHDIR,errors));
            rset.getStatement().close();
          }
          catch (SQLException e) { outputs.add("PostgreSQL error: "+e.getMessage()); }
        }

        HashMap<String,Integer> subnet_counts=null;
        String fout_subnet_path=null;
        String fout_rgt_path=null; //reduced-graph, tgts only (not impl in this app)
        String fout_rgtp_path=null; //reduced-graph, tgts+CCPs (not impl in this app)
        String fout_cpd_path=null;
        ArrayList<String> sqls = new ArrayList<String>();
        Integer n_max_a=100000;
        Integer n_max_c=100000;
        err_sb.setLength(0);
        if (tids!=null && tids.size()>0)
        {
          try {
            File dout=new File(SCRATCHDIR);
            File fout_subnet=File.createTempFile(PREFIX,"_subnet.xgmml",dout);
            fout_subnet_path=fout_subnet.getAbsolutePath();
            //File fout_rgt=File.createTempFile(PREFIX,"_rgt_subnet.xgmml",dout);
            //fout_rgt_path=fout_rgt.getAbsolutePath();
            //File fout_rgtp=File.createTempFile(PREFIX,"_rgtp_subnet.xgmml",dout);
            //fout_rgtp_path=fout_rgtp.getAbsolutePath();
            //File fout_cpd=File.createTempFile(PREFIX,"_cpd.sdf",dout);
            //fout_cpd_path=fout_cpd.getAbsolutePath();
            subnet_counts=app_utils.Extract2XGMML_LaunchThread(
		DBHOST,DBPORT,params.getVal("dbid"),DBUSR,DBPW,fout_subnet_path,tids,
		params.getVal("cpd_id"),params.getVal("cpd_idtype"),
		params.getVal("qcpd"),params.getVal("matchtype_qcpd"),minsim,
		params.getVal("cname"),params.getVal("matchtype_cname").equalsIgnoreCase("sub"),
		mw_min,mw_max,
		cids,scafids,mcesids,
		params.isChecked("neighbortargets"),
		params.getVal("subnet_title"),
		SERVLETNAME,
		response,
		out,
		n_max_a,n_max_c,
		sqls,err_sb);
          }
          catch (SQLException e) { outputs.add("PostgreSQL error: "+e.getMessage()); }
          catch (Exception e) { outputs.add("Extract2XGMML error: "+e.getMessage()); }
          if (err_sb.length()>0) { outputs.add(err_sb.toString()); }
          String subnet_results_htm="";
          if (subnet_counts!=null)
          {
            subnet_results_htm =
		app_utils.SubnetResultsHtm(
		subnet_counts,
		fout_subnet_path,
		fout_rgt_path,
		fout_rgtp_path,
		params.getVal("subnet_title"),
		response,
		CONTEXTPATH,
		SERVLETNAME,
		CYWEBAPP);
            outputs_subnet.add(subnet_results_htm);
            outputs.addAll(outputs_subnet);
            PrintWriter out_log=new PrintWriter(new BufferedWriter(new FileWriter(logfile,true)));
            out_log.printf("%s\t%s\t%d\t%d\t%d\n",DATESTR,REMOTEHOST,subnet_counts.get("n_node_tgt"),subnet_counts.get("n_node_cpd"),0);
            out_log.close();
          }
          else
            outputs.add("ERROR: Extract2XGMML failed (aack!).");
          if (outputs_targetsearch!=null)
            outputs.addAll(outputs_targetsearch);
        }
        else
        {
          outputs.add("ERROR: Extract2XGMML failed (no targets found).");
        }
        out.println("<SCRIPT>pwin.parent.focus(); pwin.focus(); pwin.close();</SCRIPT>");
        java.util.Date t_f = new java.util.Date();
        long t_d=t_f.getTime()-t_i.getTime();
        int t_d_min = (int)(t_d/60000L);
        int t_d_sec = (int)((t_d/1000L)%60L);
        errors.add(SERVLETNAME+": elapsed time: "+t_d_min+"m "+t_d_sec+"s");
        if (params.isChecked("verbose"))
        {
          if (sqls!=null)
          {
            for (String sql2: sqls)
              errors.add("<PRE>"+sql2+"</PRE>");
          }
        }
        out.println(HtmUtils.OutputHtm(outputs));
        out.println(HtmUtils.FooterHtm(errors,true));
        HtmUtils.PurgeScratchDirs(Arrays.asList(SCRATCHDIR),scratch_retire_sec,false,".",(HttpServlet) this);
      }
    }
    else
    {
      String help=request.getParameter("help");	// GET param
      String downloadtxt=request.getParameter("downloadtxt"); // POST param
      String downloadfile=request.getParameter("downloadfile"); // POST param
      if (help!=null)	// GET method, help=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(HelpHtm());
        out.println(HtmUtils.FooterHtm(errors,true));
      }
      else if (downloadtxt!=null && downloadtxt.length()>0) // POST param
      {
        ServletOutputStream ostream=response.getOutputStream();
        HtmUtils.DownloadString(response,ostream,downloadtxt,
          request.getParameter("fname"));
      }
      else if (downloadfile!=null && downloadfile.length()>0) // POST param
      {
        ServletOutputStream ostream=response.getOutputStream();
        HtmUtils.DownloadFile(response,ostream,downloadfile,
          request.getParameter("fname"));
        // Log size of downloaded data:
        PrintWriter out_log=new PrintWriter(new BufferedWriter(new FileWriter(logfile,true)));
        out_log.printf("%s\t%s\t%d\t%d\t%d\n",DATESTR,REMOTEHOST,0,0,((new File(downloadfile)).length()));
        out_log.close();
      }
      else	// GET method, initial invocation of servlet w/ no params
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(title,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(FormHtm(mrequest,response,"normal"));
        out.println("<SCRIPT>go_init(window.document.mainform,'normal',false)</SCRIPT>");
        out.println(HtmUtils.FooterHtm(errors,true));
      }
    }
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

    String logo_htm="<TABLE CELLSPACING=5 CELLPADDING=5><TR><TD>";
    String imghtm=("<IMG BORDER=0 SRC=\"/tomcat"+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");
    String tiphtm=(APPNAME+" web app from UNM Translational Informatics.");
    String href=("http://medicine.unm.edu/informatics/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 SRC=\"/tomcat"+CONTEXTPATH+"/images/BatAlone_48x36.png\">");
    tiphtm=("CARLSBAD Project, UNM Translational Informatics Division");
    href=("http://carlsbad.health.unm.edu");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 SRC=\"/tomcat"+CONTEXTPATH+"/images/gNovalogo.png\">");
    tiphtm=("Chord from gNova Inc.");
    href=("http://www.gnova.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 SRC=\"/tomcat"+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm=("JChem and Marvin from ChemAxon Ltd.");
    href=("http://www.chemaxon.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 HEIGHT=60 SRC=\"/tomcat"+CONTEXTPATH+"/images/oe_logo.png\">");
    tiphtm=("OEChem from OpenEye Scientific Software.");
    href=("http://www.eyesopen.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 HEIGHT=60 SRC=\"/tomcat"+CONTEXTPATH+"/images/cytoscape_logo.png\">");
    tiphtm=("Cytoscape");
    href=("http://www.cytoscape.org");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 HEIGHT=40 SRC=\"/tomcat"+CONTEXTPATH+"/images/cytoscapeweb_logo.png\">");
    tiphtm=("CytoscapeWeb");
    href=("http://cytoscapeweb.cytoscape.org/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 HEIGHT=40 SRC=\"/tomcat"+CONTEXTPATH+"/images/JSME_logo.png\">");
    tiphtm=("JSME Molecular Editor");
    href=("http://peter-ertl.com/jsme/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD></TR></TABLE>";
    errors.add(logo_htm);

    //Create webapp-specific log dir if necessary:
    File dout=new File(LOGDIR);
    if (!dout.exists())
    {
      boolean ok=dout.mkdir();
      System.err.println("LOGDIR creation "+(ok?"succeeded":"failed")+": "+LOGDIR);
      if (!ok)
      {
        errors.add("ERROR: could not create LOGDIR: "+LOGDIR);
        return false;
      }
    }

    String logpath=LOGDIR+"/"+SERVLETNAME+".log";
    logfile=new File(logpath);
    if (!logfile.exists())
    {
      try { logfile.createNewFile(); }
      catch (IOException e) {
        errors.add("ERROR: Logfile creation failed: "+logpath+"\n"+e.getMessage());
        return false;
      }
      logfile.setWritable(true,true);
      PrintWriter out_log=new PrintWriter(logfile);
      out_log.println("date\tIP\tN_tgt\tN_cpd\tdownload_bytes"); 
      out_log.flush();
      out_log.close();
    }
    if (!logfile.canWrite())
    {
      errors.add("ERROR: Log file not writable: "+logpath);
      return false;
    }
    BufferedReader buff=new BufferedReader(new FileReader(logfile));
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
      System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
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

    DBCon dbcon=null;
    try {
      dbcon = new DBCon("postgres",DBHOST,DBPORT,DBID,DBUSR,DBPW);
      if (dbcon==null)
      {
        errors.add("PostgreSQL connection failed.");
        return false;
      }
      if (request.getParameter("verbose")!=null)
      {
        errors.add("PostgreSQL connection ok ("+DBHOST+":"+DBPORT+"/"+DBID+").");
        errors.add(dbcon.serverStatusTxt());
        errors.add("<PRE>database: "+DBID+"\n"+carlsbad_utils.DBDescribeTxt(dbcon)+"</PRE>");
      }
      specieses=carlsbad_utils.ListTargetSpecies(dbcon); // (needed to generate form)
      tgt_idtypes=carlsbad_utils.ListTargetIDTypes(dbcon); // (needed to generate form)
      cpd_idtypes=carlsbad_utils.ListSubstanceIDTypes(dbcon); // (needed to generate form)
      //ttypes=carlsbad_utils.ListTargetTypes(dbcon); // (needed to generate form)
      tclasses=carlsbad_utils.ListTargetClasses(dbcon); // (needed to generate form)
      dbcon.close();
    }
    catch (SQLException e)
    {
      errors.add("PostgreSQL connection failed: "+e.getMessage());
      return false;
    }
    catch (Exception e)
    {
      errors.add("PostgreSQL connection failed: "+e.getMessage());
      return false;
    }
    tclasses_selected = new ArrayList<String>();

    dbids = new ArrayList<String>(Arrays.asList("carlsbad","cb2","cb3"));

    if (mrequest==null) return true;

    /// Stuff for a run:

    for (Enumeration e=mrequest.getParameterNames(); e.hasMoreElements(); )
    {
      String key=(String)e.nextElement();
      if (mrequest.getParameter(key)!=null)
        params.setVal(key,mrequest.getParameter(key));
    }
    if (mrequest.getParameterValues("tclass")!=null)
    {
      for (String tclass: mrequest.getParameterValues("tclass")) //handle multi-select:
        tclasses_selected.add(tclass);
    }
    try { N_MAX_TARGETS=Integer.parseInt(params.getVal("n_max_targets")); }
    catch (Exception e) { }

    if (mrequest.getParameter("changemode").equalsIgnoreCase("TRUE"))
      return true;
    if (mrequest.getParameter("restart").equalsIgnoreCase("TRUE"))
      return true;

    if (params.getVal("qcpd")!=null && params.getVal("qcpd").trim().length()>0)
    {
      if (smarts_utils.IsSmarts(params.getVal("qcpd")))
      {
        errors.add("Compound query \""+params.getVal("qcpd")+"\" perceived as SMARTS (invalid SMILES).  Match-type substructure auto-selected.");
        params.setVal("matchtype_qcpd","sub");
      }
      else
      {
        try {	// If query is valid smiles, parse as such and aromatize.
          Molecule mol = MolImporter.importMol(params.getVal("qcpd"),"smiles:d");
          String qsmi=mol.exportToFormat("smiles:+a");
        }
        catch (Exception e) {
          errors.add("Compound query \""+params.getVal("qcpd")+"\" is NOT valid SMILES.");
        }
      }
    }
    if (params.isChecked("verbose"))
    {
      errors.add(CONTEXT.getServerInfo()+" [API:"+CONTEXT.getMajorVersion()+"."+CONTEXT.getMinorVersion()+"]");
      //try { errors.add("Built with Cytoscape "+(new CytoscapeVersion().getFullVersion())); }
      //catch (Exception e) {errors.add(e.getMessage());}
    }
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm(MultipartRequest mrequest,HttpServletResponse response,
	String formmode)
  {
    String formmode_normal=""; String formmode_advanced="";
    if (formmode.equals("advanced")) formmode_advanced="CHECKED";
    else if (formmode.equals("normal")) formmode_normal="CHECKED";
    else formmode_normal="CHECKED";

    String dbidmenu="";
    for (String dbid : dbids)
      dbidmenu+=("<INPUT TYPE=RADIO NAME=\"dbid\" VALUE=\""+dbid+"\">"+dbid+"");
    dbidmenu=dbidmenu.replaceFirst("\""+params.getVal("dbid")+"\">","\""+params.getVal("dbid")+"\" CHECKED>");

    String speciesmenu="";
    for (String species : specieses)
      speciesmenu+=("<INPUT TYPE=RADIO NAME=\"species\" VALUE=\""+species+"\">"+species+"");
    speciesmenu+=("<INPUT TYPE=RADIO NAME=\"species\" VALUE=\"any\">any");
    speciesmenu=speciesmenu.replaceFirst("\""+params.getVal("species")+"\">","\""+params.getVal("species")+"\" CHECKED>");

    String tgt_idtypemenu="<SELECT NAME=\"tgt_idtype\">";
    tgt_idtypemenu+=("<OPTION VALUE=\"any\">any\n");
    for (String tgt_idtype : tgt_idtypes) tgt_idtypemenu+=("<OPTION VALUE=\""+tgt_idtype+"\">"+tgt_idtype+"\n");
    tgt_idtypemenu+=("<OPTION VALUE=\"CARLSBAD\">CARLSBAD\n");
    tgt_idtypemenu+="</SELECT>";
    tgt_idtypemenu=tgt_idtypemenu.replace(params.getVal("tgt_idtype")+"\">",params.getVal("tgt_idtype")+"\" SELECTED>\n");

    String cpd_idtypemenu="<SELECT NAME=\"cpd_idtype\">";
    cpd_idtypemenu+=("<OPTION VALUE=\"any\">any\n");
    for (String cpd_idtype : cpd_idtypes) cpd_idtypemenu+=("<OPTION VALUE=\""+cpd_idtype+"\">"+cpd_idtype+"\n");
    cpd_idtypemenu+=("<OPTION VALUE=\"CARLSBAD\">CARLSBAD\n");
    cpd_idtypemenu+="</SELECT>";
    cpd_idtypemenu=cpd_idtypemenu.replace(params.getVal("cpd_idtype")+"\">",params.getVal("cpd_idtype")+"\" SELECTED>\n");

    //String ttypemenu="";
    //for (String ttype : ttypes)
    //  ttypemenu+=("<INPUT TYPE=RADIO NAME=\"ttype\" VALUE=\""+ttype+"\">"+ttype+"");
    //ttypemenu+=("<INPUT TYPE=RADIO NAME=\"ttype\" VALUE=\"any\">any");
    //ttypemenu=ttypemenu.replaceFirst("\""+params.getVal("ttype")+"\">","\""+params.getVal("ttype")+"\" CHECKED>");

    String tclassmenu="<SELECT MULTIPLE SIZE=6 NAME=\"tclass\">";
    for (String tclass : tclasses)
    {
      tclassmenu+=("<OPTION VALUE=\""+tclass+"\"");
      if (tclasses_selected.contains(tclass)) tclassmenu+=" SELECTED";
      String itemtxt=tclass;
      //kluges:
      itemtxt=itemtxt.replaceAll("n-formyl methionyl peptide receptor  n-formyl methionyl peptide receptor","n-formyl methionyl peptide receptor");
      itemtxt=itemtxt.replaceAll("receptor","recp");
      itemtxt=itemtxt.replaceAll("membrane","membr");
      itemtxt=itemtxt.replaceAll("peptide","pept");
      itemtxt=itemtxt.replaceAll("transcription","transcr");
      itemtxt=itemtxt.replaceAll("nuclear","nuc");
      itemtxt=itemtxt.replaceAll("cysteine","cys");
      itemtxt=itemtxt.replaceAll("aspartate","asp");
      itemtxt=itemtxt.replaceAll("glutamate","glu");
      itemtxt=itemtxt.replaceAll("transferase","xfrase");
      itemtxt=itemtxt.replaceAll("protein","prot");
      itemtxt=itemtxt.replaceAll("camk-unique  camk-unique","camk-unique");
      itemtxt=itemtxt.replaceAll("other  other","other");
      itemtxt=itemtxt.replaceAll("ste-unique ste-unique","ste-unique");
      itemtxt=itemtxt.replaceAll("electrochemical","echem");
      itemtxt=itemtxt.replaceAll("transporter","transp");
      itemtxt=itemtxt.replaceAll("activated","act");
      itemtxt=itemtxt.replaceAll("nucleotide","ntide");
      itemtxt=itemtxt.replaceAll("conductance","cond");
      itemtxt=itemtxt.replaceAll("ligand","lig");
      itemtxt=itemtxt.replaceAll("small","sm");
      itemtxt=itemtxt.replaceAll("neurotransmitter","neurotrans");
      itemtxt=itemtxt.replaceAll("amino acid","aa");
      tclassmenu+=(">"+itemtxt+"\n");
    }
    tclassmenu+="</SELECT>";

    String matchtype_tname_exact=""; String matchtype_tname_sub="";
    if (params.getVal("matchtype_tname").equals("exact")) matchtype_tname_exact="CHECKED";
    else if (params.getVal("matchtype_tname").equals("sub")) matchtype_tname_sub="CHECKED";

    String matchtype_tdesc_exact=""; String matchtype_tdesc_sub="";
    if (params.getVal("matchtype_tdesc").equals("exact")) matchtype_tdesc_exact="CHECKED";
    else if (params.getVal("matchtype_tdesc").equals("sub")) matchtype_tdesc_sub="CHECKED";

    String matchtype_qcpd_sim=""; String matchtype_qcpd_sub=""; String matchtype_qcpd_exa="";
    if (params.getVal("matchtype_qcpd").equals("sim")) matchtype_qcpd_sim="CHECKED";
    else if (params.getVal("matchtype_qcpd").equals("sub")) matchtype_qcpd_sub="CHECKED";
    else if (params.getVal("matchtype_qcpd").equals("exa")) matchtype_qcpd_exa="CHECKED";

    String matchtype_cname_exact=""; String matchtype_cname_sub="";
    if (params.getVal("matchtype_cname").equals("exact")) matchtype_cname_exact="CHECKED";
    else if (params.getVal("matchtype_cname").equals("sub")) matchtype_cname_sub="CHECKED";

    String htm=(
    "<FORM NAME=\"mainform\" METHOD=POST ACTION=\""+response.encodeURL(SERVLETNAME)+"\""
    +" ENCTYPE=\"multipart/form-data\">\n"
    +("<TABLE WIDTH=\"100%\"><TR><TD><H1>"+APPNAME+"</H1></TD>\n")
    +"</TD><TD WIDTH=\"40%\" ALIGN=LEFT> - SubNet Application, Kit &amp; Extractor for CARLSBAD \n"
    +"<TD WIDTH=\"20%\">\n");
    if (DEBUG)
      htm+=("<B>db:</B>"+dbidmenu);
    else
      htm+="<INPUT TYPE=HIDDEN NAME=\"dbid\" VALUE=\""+params.getVal("dbid")+"\">\n";
    htm+=(
    "</TD>\n"
    +"<TD ALIGN=RIGHT>\n"
    +"<B>mode:</B>\n"
    +"<INPUT TYPE=RADIO NAME=\"formmode\" VALUE=\"normal\""
    +(" onClick=\"go_changemode(document.mainform)\" "+formmode_normal+">normal\n")
    +("<INPUT TYPE=RADIO NAME=\"formmode\" VALUE=\"advanced\"")
    +(" onClick=\"go_changemode(document.mainform)\" "+formmode_advanced+">advanced\n")
    +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE&verbose=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>help</B></BUTTON>\n")
    +"<BUTTON TYPE=BUTTON onClick=\"go_restart(document.mainform)\">\n"
    +"<B>reset</B></BUTTON>\n"
    +"</TD></TR></TABLE>\n"
    +"<INPUT TYPE=HIDDEN NAME=\"search\">\n"
    +"<INPUT TYPE=HIDDEN NAME=\"changemode\">\n"
    +"<INPUT TYPE=HIDDEN NAME=\"restart\">\n"
    +"<HR>\n"
    +"<TABLE WIDTH=100% CELLPADDING=5 CELLSPACING=5>\n"
    +"<TR BGCOLOR=\"#CCCCCC\">\n"
    +"<TD WIDTH=\"35%\" VALIGN=TOP>\n"
    +"<H3>target query:</H3>\n"
    +"<BR>\n"
    +"<TABLE>\n"
    );
    if (formmode.equals("advanced"))
    {
      htm+=(
      "<TR><TD ALIGN=RIGHT VALIGN=TOP><B>species:</B></TD><TD>"+speciesmenu+"</TD></TR>\n"
      //+"<TR><TD ALIGN=RIGHT VALIGN=TOP><B>type:</B></TD><TD>"+ttypemenu+"</TD></TR>\n"
      +"<TR><TD ALIGN=RIGHT VALIGN=TOP><B>class:</B></TD><TD VALIGN=TOP>"
      +"<BUTTON TYPE=BUTTON onClick=\"all_tclass(this.form)\">all</BUTTON>\n"
      +"</TD></TR>\n"
      +"<TR><TD VALIGN=TOP COLSPAN=2>"+tclassmenu+"</TD></TR>\n"
      );
    }
    else
    {
      htm+="<INPUT TYPE=HIDDEN NAME=\"species\" VALUE=\""+params.getVal("species")+"\">\n";
    }
    //String applet_jsfunc="StartMarvin";
    String applet_jsfunc="StartJSME";
    htm+=(
    "<TR><TD ALIGN=RIGHT><B>name:</B></TD>\n"
    +"<TD><INPUT TYPE=\"TEXT\" NAME=\"tname\" SIZE=24 VALUE=\""+params.getVal("tname")+"\">\n"
    +"<INPUT TYPE=RADIO NAME=\"matchtype_tname\" VALUE=\"exact\" "+matchtype_tname_exact+">exact"
    +"<INPUT TYPE=RADIO NAME=\"matchtype_tname\" VALUE=\"sub\" "+matchtype_tname_sub+">sub\n"
    +"</TD></TR>\n"
    +"<TR><TD ALIGN=RIGHT><B>descr:</B></TD>\n"
    +"<TD><INPUT TYPE=\"TEXT\" NAME=\"tdesc\" SIZE=24 VALUE=\""+params.getVal("tdesc")+"\">\n"
    +"<INPUT TYPE=RADIO NAME=\"matchtype_tdesc\" VALUE=\"exact\" "+matchtype_tdesc_exact+">exact\n"
    +"<INPUT TYPE=RADIO NAME=\"matchtype_tdesc\" VALUE=\"sub\" "+matchtype_tdesc_sub+">sub\n"
    +"</TD></TR>\n"
    +"<TR><TD ALIGN=RIGHT><B>id:</B></TD>\n"
    +"<TD><INPUT TYPE=\"TEXT\" NAME=\"tgt_id\" SIZE=8 VALUE=\""+params.getVal("tgt_id")+"\">\n"
    +"<B>type:</B>"+tgt_idtypemenu
    +"</TD></TR>\n"
    +"<TR><TD COLSPAN=2><a href=\"javascript:void(0)\" onClick=\"void window.open('http://carlsbad.health.unm.edu/dbinfo/data/profile_targets.html','carlsbad targets','width=600,height=400,scrollbars=1,resizable=1')\">browse targets...</a></TD></TR>\n"
    +"</TABLE>\n"
    +"</TD>\n"
    );

    htm+=(
    "<TD WIDTH=\"35%\" VALIGN=TOP>\n"
    +"<H3>compound query:</H3>\n"
    +"<BR>\n"
    +"<TABLE>\n"
    +"<TR><TD ALIGN=RIGHT><B>name:</B></TD>\n"
    +"<TD><INPUT TYPE=\"TEXT\" NAME=\"cname\" SIZE=24 VALUE=\""+params.getVal("cname")+"\">\n"
    +"<INPUT TYPE=RADIO NAME=\"matchtype_cname\" VALUE=\"exact\" "+matchtype_cname_exact+">exact"
    +"<INPUT TYPE=RADIO NAME=\"matchtype_cname\" VALUE=\"sub\" "+matchtype_cname_sub+">sub\n"
    +"</TD></TR>\n"
    );

    if (formmode.equals("advanced"))
    {
      htm+=(
        "<TR><TD VALIGN=TOP ALIGN=RIGHT><B>structure:</B></TD>\n"
        +"<TD><BUTTON TYPE=BUTTON onClick=\""+applet_jsfunc+"('qcpd')\">draw query...</BUTTON>\n"
        +"<I>...or enter smiles or smarts:</I><BR>\n"
        +"<INPUT TYPE=\"TEXT\" NAME=\"qcpd\" SIZE=24 VALUE=\""+params.getVal("qcpd")+"\"><BR>\n"
        +"</TD></TR>\n"
        +"<TR><TD VALIGN=TOP ALIGN=RIGHT></TD>\n"
        +"<TD>\n"
        +"<INPUT TYPE=RADIO NAME=\"matchtype_qcpd\" VALUE=\"exa\" "+matchtype_qcpd_exa+">exact&nbsp; \n"
        +"<INPUT TYPE=RADIO NAME=\"matchtype_qcpd\" VALUE=\"sub\" "+matchtype_qcpd_sub+">subst&nbsp; \n"
        +"<INPUT TYPE=RADIO NAME=\"matchtype_qcpd\" VALUE=\"sim\" "+matchtype_qcpd_sim+">simil"
        +" <B>&gt;</B><INPUT TYPE=\"TEXT\" NAME=\"minsim\" SIZE=4 VALUE=\""+params.getVal("minsim")+"\">\n"
        +"</TD></TR>\n"
        //"<TR><TD ALIGN=RIGHT><B>molweight:</B></TD>\n"
        //+"<TD>min:<INPUT TYPE=\"TEXT\" NAME=\"mw_min\" SIZE=5 VALUE=\""+params.getVal("mw_min")+"\">\n"
        //+"max:<INPUT TYPE=\"TEXT\" NAME=\"mw_max\" SIZE=5 VALUE=\""+params.getVal("mw_max")+"\">\n"
        //+"</TD></TR>\n"
        +"<TR><TD ALIGN=RIGHT><B>cpd id:</B></TD>\n"
        +"<TD><INPUT TYPE=\"TEXT\" NAME=\"cpd_id\" SIZE=10 VALUE=\""+params.getVal("cpd_id")+"\">\n"
        +"<B>type:</B>"+cpd_idtypemenu
        +"</TD></TR>\n"
        +"<TR><TD ALIGN=RIGHT><B>scaffold id:</B></TD>\n"
        +"<TD><INPUT TYPE=\"TEXT\" NAME=\"s_id\" SIZE=10 VALUE=\""+params.getVal("s_id")+"\">\n"
        +"<I>(CARLSBAD ID#)</I></TD></TR>\n"
        +"<TR><TD ALIGN=RIGHT><B>mces id:</B></TD>\n"
        +"<TD><INPUT TYPE=\"TEXT\" NAME=\"m_id\" SIZE=10 VALUE=\""+params.getVal("m_id")+"\">\n"
        +"<I>(CARLSBAD ID#)</I></TD></TR>\n"
	);
    }
    else
    {
      htm+=(
        "<TR><TD VALIGN=TOP ALIGN=RIGHT><B>substructure:</B></TD>\n"
        +"<TD><BUTTON TYPE=BUTTON onClick=\""+applet_jsfunc+"('qcpd')\">draw query...</BUTTON>\n"
        +"<I>...or enter smiles or smarts:</I><BR>\n"
        +"<INPUT TYPE=\"TEXT\" NAME=\"qcpd\" SIZE=24 VALUE=\""+params.getVal("qcpd")+"\"><BR>\n"
        +"</TD></TR>\n"
        +"<INPUT TYPE=HIDDEN NAME=\"matchtype_qcpd\" VALUE=\"sub\">\n"
        +"<INPUT TYPE=HIDDEN NAME=\"minsim\" VALUE=\""+params.getVal("minsim")+"\">\n"
	);
    }
    htm+=(
    "</TABLE>\n"
    +"</TD>\n"
    +"<TD VALIGN=TOP>\n"
    );
    if (formmode.equals("advanced"))
    {
      htm+=(
        "<H3>output:</H3><BR>\n"
        +"<INPUT TYPE=CHECKBOX NAME=\"view_tgts\" VALUE=\"CHECKED\" "+params.getVal("view_tgts")+">view target table\n"
        +"&nbsp;"
        +"<INPUT TYPE=CHECKBOX NAME=\"show_tgt_ids\" VALUE=\"CHECKED\" "+params.getVal("show_tgt_ids")+">show target IDs<BR>\n"
        +"<INPUT TYPE=CHECKBOX NAME=\"neighbortargets\" VALUE=\"CHECKED\" "+params.getVal("neighbortargets")+">neighbortargets<BR>\n"
        +"n_max_targets:\n"
        +"<INPUT TYPE=\"TEXT\" NAME=\"n_max_targets\" SIZE=3 VALUE=\""+params.getVal("n_max_targets")+"\">\n"
        +"<P>\n"
        +"<HR>\n"
        +"<H3>misc:</H3><BR>\n"
	);
    }
    else
    {
      htm+=(
        "<H3>misc:</H3><BR>\n"
        +"<INPUT TYPE=HIDDEN NAME=\"view_tgts\" VALUE=\"CHECKED\">\n"
        +"<INPUT TYPE=HIDDEN NAME=\"show_tgt_ids\" VALUE=\"\">\n"
        +"<INPUT TYPE=HIDDEN NAME=\"neighbortargets\" VALUE=\"\">\n"
        +"<INPUT TYPE=HIDDEN NAME=\"n_max_targets\" VALUE=\""+params.getVal("n_max_targets")+"\">\n"
	);
    }
    htm+=(
    "subnet_title:\n"
    +"<INPUT TYPE=\"TEXT\" NAME=\"subnet_title\" SIZE=32 VALUE=\""+params.getVal("subnet_title")+"\"><BR>\n"
    +"<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">verbose<BR>\n"
    +"</TD></TR>\n"
    +"<TR><TD COLSPAN=3 ALIGN=CENTER>\n"
    +"<BUTTON TYPE=BUTTON onClick=\"go_search(this.form,'"+formmode+"')\">\n"
    +"<B>&nbsp; Search Carlsbad &nbsp;</B>\n"
    +" &nbsp;</B></BUTTON>\n"
    +"</TD></TR>\n"
    +"</TABLE>\n"
    +"</FORM>\n"
    );
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript() throws IOException
  {
    String js=(
"function go_restart(form) //restart afresh\n"+
"{\n"+
"  form.restart.value='TRUE';\n"+
"  form.submit();\n"+
"}\n"+
"function go_changemode(form) //change mode, keep settings\n"+
"{\n"+
"  form.changemode.value='TRUE';\n"+
"  form.submit();\n"+
"}\n"+
"function go_init(form,formmode,changemode) //init as needed\n"+
"{\n"+
"  var i;\n"+
"  if (formmode=='advanced')\n"+
"  {\n"+
"    form.s_id.value='';\n"+
"    form.m_id.value='';\n"+
"    //for (i=0;i<form.dbid.length;++i)   //radio\n"+
"    //{ if (form.dbid[i].value=='"+DBID+"') form.dbid[i].checked=true; }\n"+
"    //for (i=0;i<form.ttype.length;++i)     //radio\n"+
"    //{ if (form.ttype[i].value=='any') form.ttype[i].checked=true; }\n"+
"    for (i=0;i<form.tclass.length;++i)  //multi-select (default=all)\n"+
"      form.tclass.options[i].selected=true;\n"+
"    for (i=0;i<form.matchtype_qcpd.length;++i)   //radio\n"+
"    { if (form.matchtype_qcpd[i].value=='sub') form.matchtype_qcpd[i].checked=true; }\n"+
"    for (i=0;i<form.cpd_idtype.length;++i)\n"+
"      if (form.cpd_idtype.options[i].value=='any')\n"+
"        form.cpd_idtype.options[i].selected=true;\n"+
"  }\n"+
"  //else\n"+
"  //{\n"+
"    form.dbid.value='"+DBID+"';\n"+
"  //}\n"+
"  if (changemode) return;\n"+
"  form.tname.value='';\n"+
"  form.tdesc.value='';\n"+
"  form.species.value='any';\n"+
"  form.tgt_id.value='';\n"+
"  form.view_tgts.checked=true;\n"+
"  form.show_tgt_ids.checked=false;\n"+
"  form.verbose.checked=false;\n"+
"  for (i=0;i<form.tgt_idtype.length;++i)\n"+
"    if (form.tgt_idtype.options[i].value=='any')\n"+
"      form.tgt_idtype.options[i].selected=true;\n"+
"  for (i=0;i<form.matchtype_tname.length;++i)   //radio\n"+
"  { if (form.matchtype_tname[i].value=='exact') form.matchtype_tname[i].checked=true; }\n"+
"  for (i=0;i<form.matchtype_tdesc.length;++i)   //radio\n"+
"  { if (form.matchtype_tdesc[i].value=='sub') form.matchtype_tdesc[i].checked=true; }\n"+
"  form.qcpd.value='';\n"+
"  form.cname.value='';\n"+
"  for (i=0;i<form.matchtype_cname.length;++i)   //radio\n"+
"  { if (form.matchtype_cname[i].value=='exact') form.matchtype_cname[i].checked=true; }\n"+
"  form.minsim.value='0.7';\n"+
"  form.n_max_targets.value='"+N_MAX_TARGETS+"';\n"+
"  form.subnet_title.value='CARLSBAD SubNet "+DATESTR.substring(0,8)+"';\n"+
"}\n"+
"function checkform(form,formmode)\n"+
"{\n"+
"  var ok_tquery=false;\n"+
"  ok_tquery=ok_tquery||form.tdesc.value;\n"+
"  ok_tquery=ok_tquery||form.tgt_id.value;\n"+
"  ok_tquery=ok_tquery||form.tname.value;\n"+
"  var i;\n"+
"  if (formmode=='advanced')\n"+
"  {\n"+
"    for (i=0;i<form.tclass.length;++i)  //multi-select (default=all)\n"+
"      ok_tquery=ok_tquery||(!form.tclass.options[i].selected);  //1+ deselected\n"+
"  }\n"+
"  var ok_cquery=false;\n"+
"  ok_cquery=ok_cquery||form.qcpd.value;\n"+
"  ok_cquery=ok_cquery||form.cname.value;\n"+
"  if (formmode=='advanced')\n"+
"  {\n"+
"    ok_cquery=ok_cquery||form.cpd_id.value;\n"+
"    ok_cquery=ok_cquery||form.s_id.value;\n"+
"    ok_cquery=ok_cquery||form.m_id.value;\n"+
"  }\n"+
"  if (!ok_tquery && !ok_cquery)\n"+
"  {\n"+
"    alert('ERROR: Invalid query; must specify target name, descr, class-subset, ID, or compound query.');\n"+
"    return false;\n"+
"  }\n"+
"  if (formmode=='advanced' && !(ok_tquery && ok_cquery) && form.neighbortargets.checked)\n"+
"  {\n"+
"    alert('ERROR: neighbortargets requires target query and compound query.');\n"+
"    return false;\n"+
"  }\n"+
"  var matchtype_qcpd='';\n"+
"  for (i=0;i<form.matchtype_qcpd.length;++i)   //radio\n"+
"  { if (form.matchtype_qcpd[i].checked) matchtype_qcpd=form.matchtype_qcpd[i].value; }\n"+
"  var qcpd_is_smarts=(form.qcpd.value.match('[&,;~]')||form.qcpd.value.match('#[0-9]'));\n"+
"  if (formmode=='advanced')\n"+
"  {\n"+
"    if (qcpd_is_smarts  && matchtype_qcpd!='sub')\n"+
"    {\n"+
"      alert('ERROR: Input SMARTS requires sub-structure search mode (\"'+form.qcpd.value+'\" not valid SMILES).');\n"+
"      return false;\n"+
"    }\n"+
"  }\n"+
"  else\n"+
"  {\n"+
"    if (qcpd_is_smarts)\n"+
"    {\n"+
"      var ok=confirm('Input is SMARTS (not valid SMILES), which may be slow.  Continue?');\n"+
"      if (!ok) return false;\n"+
"    }\n"+
"  }\n"+
"  if (formmode=='advanced')\n"+
"  {\n"+
"    if ((form.cpd_id.value && !form.cpd_id.value.match('^ *[0-9]* *$'))\n"+
"        ||  (form.s_id.value && !form.s_id.value.match('^ *[0-9]* *$'))\n"+
"        ||  (form.m_id.value && !form.m_id.value.match('^ *[0-9]* *$')))\n"+
"    {\n"+
"      alert('ERROR: Illegal cpd/scaffold/mces ID; should be integer.');\n"+
"      return false;\n"+
"    }\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"function all_tclass(form)\n"+
"{\n"+
"  for (i=0;i<form.tclass.length;++i)\n"+
"    form.tclass.options[i].selected=true;\n"+
"}\n"+
"function go_search(form,formmode)\n"+
"{\n"+
"  if (!checkform(form,formmode)) return;\n"+
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
"  pwin.document.writeln('<LINK REL=\"stylesheet\" type=\"text/css\" HREF=\"/tomcat"+CONTEXTPATH+"/css/biocomp.css\" />');\n"+
"  pwin.document.writeln('</HEAD><BODY BGCOLOR=\"#DDDDDD\">');\n"+
"  pwin.document.writeln('"+SERVLETNAME+"...<BR>');\n");
  js+=("  pwin.document.writeln('"+DateFormat.getDateInstance(DateFormat.FULL).format(new java.util.Date())+"<BR>');\n");
  js+=("\n"+
"  if (navigator.appName.indexOf('Explorer')<0)\n"+
"    pwin.document.title='"+SERVLETNAME+" progress'; //not-ok for IE\n"+
"\n"+
"  form.search.value='TRUE';\n"+
"  form.submit();\n"+
"}\n"+
"function StartJChemPaint(fieldname)\n"+
"{\n"+
"  var field=eval('document.mainform.'+fieldname);\n"+
"  var smiles=field.value;\n"+
"  var x=500;\n"+
"  var y=400;\n"+
"  var win=window.open('/cgi-bin/jcp_window.cgi?fieldname='+fieldname+'&smiles='+encodeURIComponent(smiles),\n"+
"	'JChemPaint','width=500,height=450,left='+x+',top='+y+',scrollbars=no,resizable=yes');\n"+
"  win.focus();\n"+
"}\n"+
"function fromJCP(smiles,molfile,form)\n"+
"{\n"+
"  // This function is called from jcp_window.cgi.\n"+
"  // Editor fills variable specified by form.fieldname.\n"+
"  if (!smiles)\n"+
"  {\n"+
"    alert('no molecule submitted');\n"+
"    return;\n"+
"  }\n"+
"  var field=eval('document.mainform.'+form.fieldname.value);\n"+
"  field.value=smiles;\n"+
"}\n"+
"/// JSME stuff:\n"+
"function StartJSME(intxt)\n"+
"{\n"+
"  window.open('"+JSMEWINURL+"','JSME','width=500,height=450,scrollbars=0,resizable=1,location=0');\n"+
"}\n"+
"function fromJSME(smiles)\n"+
"{\n"+
"  // this function is called from JSME window\n"+
"  if (smiles=='')\n"+
"  {\n"+
"    alert('ERROR: no molecule submitted');\n"+
"    return;\n"+
"  }\n"+
"  var form=document.mainform;\n"+
"  form.qcpd.value=smiles;\n"+
"}\n"
    );
    return js;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
  {
    String htm=
    ("<H1>"+APPNAME+" help</H1>\n"
    +"<HR>\n"
    +"<H3>Introduction</H3>\n"
    +"SNAKE = SubNet Application, Kit &amp; Extractor for CARLSBAD \n"
    +"<BR />\n"
    +"CARLSBAD = Confederated Annotated Research Libraries for Small molecule BioActivity Data \n"
    +"<P>\n"
    +"This web app queries the CARLSBAD database, which consists of targets, small molecules, \n"
    +"bioactivities, related data, and chemical pattern recognition data including scaffolds\n"
    +"and MCES clusters.\n"
    +"<P>\n"
    +"<H3>Database contents</H3>\n"
    +"A summary of CARLSBAD contents should be shown at the bottom of this help, below\n"
    +"the powered-by logos. For a detailed profile of database contents, \n"
    +"see the\n"
    +"<A HREF=\"http://carlsbad.health.unm.edu/\">CARLSBAD Project Homepage</A>.\n"
    +"Included is a browsable <a href=\"/dbinfo/data/profile_targets.html\">table of targets</a>.\n"
    +"<P>\n"
    +"<H3>Query logic</H3>\n"
    +"To be legal, the query must include target specifications  <B>or</B>\n"
    +"compound specifications <B>or</B> both.\n"
    +"For any legal search, the returned subnet includes all targets and compounds which\n"
    +"(1) Satisfy the query criteria, and (2) Have at least one activity (edge) in the subnet.\n"
    +"<P>\n"
    +"<H3>Output</H3>\n"
    +"<UL>\n"
    +"<LI> View a table of targets and summary of returned subnet.\n"
    +"<LI> Download an XGMML file of targets, small molecules, bioactivities and associated data.\n"
    +"This file comprises a subnetwork which can be directly imported by Cytoscape 2.8.0+.\n"
    +"<LI> View the subnet using the CytoscapeWeb (Flash app) in a popup window.  Not recommended for large\n"
    +"files.\n"
    +"<LI> Download a CSV file of targets and associated data.\n"
    +"</UL>\n"
    +"<P>\n"
    +"<H3>Scientific workflows, scenarios and examples</H3>\n"
    +"An inquiry can begin with a single target, class or set of targets, or with a single compound\n"
    +"or group of compounds.  A search for compound name \"diazepam\" returns that drug and the set of\n"
    +"targets against which the drug is active according to experiment.  A search for the target specified by\n"
    +"PDB ID \"2BXQ:A\" returns that one protein target and associated active compounds.\n"
    +"<P>\n"
    +"<H3>Notes</H3>\n"
    +"<UL>\n"
    +"<LI><I>Chemical Pattern Detection and Visualization in Biological Networks</I>,\n"
    +"funded by NIH R21 award, PI Tudor Oprea).\n"
    +"<LI><B>Regarding target classification:</B> In this beta version,\n"
    +"ChEMBL protein target classification is implemented; however, most but not all non-ChEMBL targets\n"
    +"are so classified, thus, queries specifying target classifications may miss\n"
    +"valid and suitable targets.  The CARLSBAD project plan includes classification of all\n"
    +"targets.\n"
    +"<LI>"+SERVLETNAME+" limits:<BR>\n"
    +"&nbsp; N_MAX = "+N_MAX+"<BR>\n"
    +"&nbsp; N_MAX_TARGETS = "+N_MAX_TARGETS+"<BR>\n"
    +"</UL>\n"
    +"<P>\n"
    +"<a href=\"http://carlsbad.health.unm.edu\"><img src=\"/tomcat"+CONTEXTPATH+"/images/carlsbad_logo.png\" align=right border=0></a>\n"
    +"<H3>Carlsbad Project Team:</H3>\n"
    +"<UL>\n"
    +"<LI> Tudor Oprea (PI)\n"
    +"<LI> Cristian Bologa\n"
    +"<LI> Stephen Mathias\n"
    +"<LI> Oleg Ursu\n"
    +"<LI> Jeremy Yang\n"
    +"<LI> Gergely Zahoransky-Kohalmi\n"
    +"<LI> Jarrett Hines-Kay\n"
    +"<LI> Jerome Abear\n"
    +"</UL>\n"
    +"<P>\n"
    +"<H3>For more information</H3>\n"
    +"<UL>\n"
    +"<LI>For more information on the CARLSBAD project, including the\n"
    +"<A HREF=\"http://carlsbad.health.unm.edu/manual/CARLSBADUserManual.html\">CARLSBAD User Manual</A>,\n"
    +"containing help with this web app\n"
    +"and other documentation, see the\n"
    +"<A HREF=\"http://carlsbad.health.unm.edu/\">CARLSBAD Project Homepage</A>.\n"
    +"</UL>\n"
    +"<BR CLEAR=RIGHT>\n"
	);
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    CONTEXT=getServletContext();
    CONTEXTPATH=CONTEXT.getContextPath();
    // read servlet parameters (from web.xml):
    try { APPNAME=conf.getInitParameter("APPNAME"); }
    catch (Exception e) { APPNAME=this.getServletName(); }
    UPLOADDIR=conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null)
      throw new ServletException("Please supply UPLOADDIR parameter");
    SCRATCHDIR=conf.getInitParameter("SCRATCHDIR");
    if (SCRATCHDIR==null) SCRATCHDIR="/tmp";
    LOGDIR=conf.getInitParameter("LOGDIR")+CONTEXTPATH;
    if (LOGDIR==null) LOGDIR="/usr/local/tomcat/logs"+CONTEXTPATH;
    DBHOST=conf.getInitParameter("DBHOST");
    if (DBHOST==null)
      throw new ServletException("Please supply DBHOST parameter");
    DBID=conf.getInitParameter("DBID");
    if (DBID==null)
      throw new ServletException("Please supply DBID parameter");
    DBSCHEMA=conf.getInitParameter("DBSCHEMA");
    if (DBSCHEMA==null)
      throw new ServletException("Please supply DBSCHEMA parameter");
    DBUSR=conf.getInitParameter("DBUSR");
    if (DBUSR==null)
      throw new ServletException("Please supply DBUSR parameter");
    DBPW=conf.getInitParameter("DBPW");
    if (DBPW==null)
      throw new ServletException("Please supply DBPW parameter");
    try { DBPORT=Integer.parseInt(conf.getInitParameter("DBPORT")); }
    catch (NumberFormatException e) { DBPORT=5432; }
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=10000; }
    try { N_MAX_TARGETS=Integer.parseInt(conf.getInitParameter("N_MAX_TARGETS")); }
    catch (Exception e) { N_MAX_TARGETS=100; }
    try { CYWEBAPP=conf.getInitParameter("CYWEBAPP"); }
    catch (Exception e) { CYWEBAPP="/cgi-bin/cytoscapeview.cgi"; }
    try { MARVWINCGI=conf.getInitParameter("MARVWINCGI"); }
    catch (Exception e) { MARVWINCGI="/cgi-bin/marv_window.cgi"; }
    try { JSMEWINURL=conf.getInitParameter("JSMEWINURL"); }
    catch (Exception e) { JSMEWINURL="/jsme_win.html"; }
  }

  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    doPost(request,response);
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
