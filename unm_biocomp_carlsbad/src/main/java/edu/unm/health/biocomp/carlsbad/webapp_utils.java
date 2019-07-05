package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.net.*; //URLEncoder,InetAddress
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.MultipartRequest;

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.db.*;
import edu.unm.health.biocomp.util.threads.*;
import edu.unm.health.biocomp.util.http.*;

/**	Static methods for Carlsbad applications: threads, results display.
	@author Jeremy J Yang
*/
public class webapp_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**   Executes SQL statement via separate thread.
  */
  public static ResultSet ExecuteSql_LaunchThread(String dbhost, Integer dbport, String dbid, String dbusr, String dbpw, String sql, String servletname, HttpServletResponse response, PrintWriter out, int n_max, StringBuilder err_sb)
      throws SQLException, IOException, ServletException
  {     
    ExecutorService exec=Executors.newSingleThreadExecutor();
    int tpoll=1000; //msec
      DBQuery_PG_Task dbquery_task =
        new DBQuery_PG_Task(dbhost, dbport, dbid, dbusr, dbpw, sql, n_max);
      TaskUtils.ExecTaskWeb(exec, dbquery_task, dbquery_task.taskstatus, servletname+" (sql-query)", tpoll, out, response, (servletname+"_progress_win"));

    ResultSet rset=dbquery_task.getRSet();
    err_sb.append(dbquery_task.getErrtxt());
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Extracts one-click subnet via separate thread.
  */
  public static HashMap<String,Integer> Target2Network_LaunchThread(
	String dbhost, Integer dbport, String dbid, String dbusr, String dbpw,
	String fout_rgt_path,
	String fout_rgtp_path,
	String fout_subnet_path,
	String fout_cpd_path,
	Integer tid,
	Float scaf_min,
	String subnet_title,
	String servletname,
	HttpServletResponse response,
	PrintWriter out,
	Integer n_max_a,Integer n_max_c,
	CompoundList cpdlist,
	CCPList ccplist,
	ArrayList<String> sqls,
	StringBuilder err_sb)
      throws SQLException,IOException,ServletException
  {
    ExecutorService exec=Executors.newSingleThreadExecutor();
    int tpoll=1000; //msec
    Target2Network_Task xsubnet_task =
      new Target2Network_Task(
		dbhost, dbport, dbid, dbusr, dbpw,
		fout_rgt_path,
		fout_rgtp_path,
		fout_subnet_path,
		fout_cpd_path,
		tid,
		scaf_min,
		subnet_title,
		n_max_a,n_max_c,
		cpdlist,
		ccplist,
		sqls);
    TaskUtils.ExecTaskWeb(exec, xsubnet_task, xsubnet_task.taskstatus, servletname+" (target2network)", tpoll, out, response, (servletname+"_progress_win"));
    /// Problem with ExecTaskWeb is exceptions can occur with only stderr logging.
    HashMap<String,Integer> counts=xsubnet_task.getCounts();
    err_sb.append(xsubnet_task.getErrtxt());
    return counts;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**   Extracts one-click subnet via separate thread.
  */
  public static HashMap<String,Integer> Compound2Network_LaunchThread(
	String dbhost, Integer dbport, String dbid, String dbusr, String dbpw,
	String fout_rgt_path,
	String fout_rgtp_path,
	String fout_subnet_path,
	String fout_cpd_path,
	Integer cid,
	Float scaf_min,
	String subnet_title,
	String servletname,
	HttpServletResponse response,
	PrintWriter out,
	Integer n_max_a,Integer n_max_c,
	ArrayList<Integer> tids,
	CompoundList cpdlist,
	CCPList ccplist,
	ArrayList<String> sqls,
	StringBuilder err_sb)
      throws SQLException, IOException, ServletException
  {
    ExecutorService exec=Executors.newSingleThreadExecutor();
    int tpoll=1000; //msec
    Compound2Network_Task xsubnet_task =
      new Compound2Network_Task(
		dbhost, dbport, dbid, dbusr, dbpw,
		fout_rgt_path,
		fout_rgtp_path,
		fout_subnet_path,
		fout_cpd_path,
		cid,
		scaf_min,
		subnet_title,
		n_max_a, n_max_c,
		tids,
		cpdlist,
		ccplist,
		sqls);
    TaskUtils.ExecTaskWeb(exec, xsubnet_task, xsubnet_task.taskstatus, servletname+" (compound2network)", tpoll, out, response, (servletname+"_progress_win"));
    /// Problem with ExecTaskWeb is exceptions can occur with only stderr logging.
    HashMap<String,Integer> counts=xsubnet_task.getCounts();
    err_sb.append(xsubnet_task.getErrtxt());
    return counts;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**   Extracts one-click subnet via separate thread.
  */
  public static HashMap<String,Integer> Disease2Network_LaunchThread(
	String dbhost, Integer dbport, String dbid, String dbusr, String dbpw,
	String fout_rgt_path,
	String fout_rgtp_path,
	String fout_subnet_path,
	String fout_cpd_path,
	String kid,
	Float scaf_min,
	String subnet_title,
	String servletname,
	HttpServletResponse response,
	PrintWriter out,
	Integer n_max_a, Integer n_max_c,
	ArrayList<Integer> tids,
	CompoundList cpdlist,
	CCPList ccplist,
	ArrayList<String> sqls,
	StringBuilder err_sb)
      throws SQLException, IOException, ServletException
  {
    ExecutorService exec=Executors.newSingleThreadExecutor();
    int tpoll=1000; //msec
    Disease2Network_Task xsubnet_task =
      new Disease2Network_Task(
		dbhost, dbport, dbid, dbusr, dbpw,
		fout_rgt_path,
		fout_rgtp_path,
		fout_subnet_path,
		fout_cpd_path,
		kid,
		scaf_min,
		subnet_title,
		n_max_a, n_max_c,
		tids,
		cpdlist,
		ccplist,
		sqls);
    TaskUtils.ExecTaskWeb(exec, xsubnet_task, xsubnet_task.taskstatus, servletname+" (disease2network)", tpoll,
	out,response,(servletname+"_progress_win"));
    /// Problem with ExecTaskWeb is exceptions can occur with only stderr logging.
    HashMap<String,Integer> counts=xsubnet_task.getCounts();
    err_sb.append(xsubnet_task.getErrtxt());
    return counts;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**   Target search results HTML: table, CSV download button.
  */
  public static String TargetSearchResultsHtm(
        ResultSet rset,
        MultipartRequest mrequest,
        HttpServletResponse response,
        ArrayList<Integer> tids,
	HttpParams params,
        String servletname,
        String prefix,
        String scratchdir,
	ArrayList<String> errors)
        throws SQLException,IOException
  {
    int n_max_targets=100;
    try { n_max_targets=Integer.parseInt(params.getVal("n_max_targets")); }
    catch (Exception e) { }
    File dout=new File(scratchdir);
    File fout_tgt=File.createTempFile(prefix,"_out.txt",dout);
    PrintWriter fout_tgt_writer=new PrintWriter(new BufferedWriter(new FileWriter(fout_tgt,true)));

    String htm="";
    String thtm=("<TABLE WIDTH=\"100%\" BORDER>\n");
    String rhtm=("");

    ResultSetMetaData rsmd=rset.getMetaData();
    int nCols = rsmd.getColumnCount();
    int n_col=0;
    for (int i=1;i<nCols+1;++i)
    {
      String colName=rsmd.getColumnName(i);
      fout_tgt_writer.printf(colName+((i<nCols)?",":""));
      if (colName.equals("tgt_id_type")) continue;
      if (colName.equals("tgt_id") && !params.isChecked("show_tgt_ids")) continue;
      //String tableName=rsmd.getTableName(i);
      rhtm+=("<TH>"+colName+"</TH>");
      ++n_col;
    }
    fout_tgt_writer.printf("\n");
    thtm+=("<TR><TH COLSPAN="+n_col+">targets</TH></TR>\n");
    thtm+=("<TR>"+rhtm+"</TR>\n");
    int n_row=0;
    int n_target=0;
    rhtm="";
    Integer tid_prev=null;
    Integer tid=null;
    while (rset.next()) // tid,name,descr,species,type,[class,]tgt_id_type,tgt_id
    {
      ++n_row;
      tid_prev=tid;
      tid=rset.getInt("tid");

      for (int i=1;i<nCols+1;++i)
        fout_tgt_writer.printf(rset.getString(i)+((i<nCols)?",":""));
      fout_tgt_writer.printf("\n");
      if (tids.isEmpty() || !(tid.equals(tid_prev)))
      {
        ++n_target;
        if (tids.size()==n_max_targets) continue;
        tids.add(tid);
        if (!rhtm.isEmpty())
        {
          rhtm+="</TT></TD></TR>\n";
          thtm+=rhtm;
        }
        rhtm="<TR>\n";
        for (int i=1;i<nCols-1;++i)
          rhtm+=("<TD VALIGN=\"top\" BGCOLOR=\"white\"><TT>"+rset.getString(i)+"</TT></TD>\n");
        if (params.isChecked("show_tgt_ids"))
          rhtm+=("<TD VALIGN=\"top\" BGCOLOR=\"white\"><TT>"+rset.getString("tgt_id_type")+":"+rset.getString("tgt_id")+"<BR>\n");
      }
      else
      {
        if (params.isChecked("show_tgt_ids"))
          rhtm+=(rset.getString("tgt_id_type")+":"+rset.getString("tgt_id")+"<BR>\n");
      }
    }
    if (!rhtm.isEmpty())
    {
      if (params.isChecked("show_tgt_ids"))
        rhtm+="</TT></TD>\n";
      rhtm+="</TR>\n";
      thtm+=rhtm;
    }
    thtm+=("</TABLE>\n");
    if (n_target>n_max_targets)
    {
      htm+=("targets found: "+tids.size()+" (truncated; total="+n_target+")<br>\n");
      errors.add("output truncated at N_MAX_TARGETS = "+n_max_targets+" (truncated; total="+n_target+")");
    }
    if (params.isChecked("show_tgt_ids") && params.isChecked("verbose"))
      htm+=("target synonyms found: "+(n_row-tids.size())+"<br>\n");
    fout_tgt_writer.close();
    if (params.isChecked("view_tgts") && tids.size()>0)
      htm+=(thtm+"<br>\n");
    if (tids.size()>0)
    {
      String fname=(servletname+"_targets_"+params.getVal("subnet_title")+".csv").replaceAll(" ","_");
      String bhtm=(
        "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(servletname)+"\">\n"+
        "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout_tgt.getAbsolutePath()+"\">\n"+
        "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
        "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">"+
        "<B>Targets CSV ("+file_utils.NiceBytes(fout_tgt.length())+")</B></BUTTON>\n</FORM>");
      htm+=(bhtm+"<br>\n");
    }
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Target search results CSV download button.
  */
  public static String TargetCSVButtonHtm(
	TargetList tlist,
	ArrayList<Integer> tids,
        MultipartRequest mrequest,
        HttpServletResponse response,
        String servletname,
        String prefix,
	String fname,
        String scratchdir
	)
	throws IOException
  {
    if (tids==null) return "";
    File dout=new File(scratchdir);
    File fout_tgt=File.createTempFile(prefix,"_out.txt",dout);
    PrintWriter fout_tgt_writer=new PrintWriter(new BufferedWriter(new FileWriter(fout_tgt,true)));

    fout_tgt_writer.printf("tid,name,species,descr,ids\n");
    for (Integer tid: tids)
    {
      if (tid==null) continue; //Aaack.
      Target tgt = tlist.get(tid);
      if (tgt==null)
      {
        System.err.println("ERROR: Aaack! no Target in TargetList for tid="+tid);
        continue;
      }
      fout_tgt_writer.printf(""+tid);
      fout_tgt_writer.printf(","+"\""+tgt.getName()+"\"");
      fout_tgt_writer.printf(","+"\""+tgt.getSpecies()+"\"");
      fout_tgt_writer.printf(","+"\""+tgt.getDescription()+"\"");
      fout_tgt_writer.printf("\n");
    }
    fout_tgt_writer.close();
    String bhtm=(
        "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(servletname)+"\">\n"+
        "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout_tgt.getAbsolutePath()+"\">\n"+
        "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
        "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">"+
        "<B>Targets CSV ("+file_utils.NiceBytes(fout_tgt.length())+")</B></BUTTON>\n</FORM>");
    return (bhtm);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Compound search results CSV download button.
  */
  public static String CompoundCSVButtonHtm(
	CompoundList cpdlist,
        MultipartRequest mrequest,
        HttpServletResponse response,
        String servletname,
        String prefix,
	String fname,
        String scratchdir
	)
	throws IOException
  {
    File dout=new File(scratchdir);
    File fout_cpd=File.createTempFile(prefix,"_out.txt",dout);
    PrintWriter fout_cpd_writer=new PrintWriter(new BufferedWriter(new FileWriter(fout_cpd,true)));

    fout_cpd_writer.printf("Carlsbad_CID,PubChem_SID,ChEMBL_ID,smiles\n");
    for (int cid: cpdlist.keySet())
    {
      Compound cpd = cpdlist.get(cid);
      if (cpd==null)
      {
        System.err.println("ERROR: Aaack! no Compound in CompoundList for cid="+cid);
        continue;
      }
      fout_cpd_writer.printf(""+cid);
      String pubchem_sid=(cpd.getIdentifiers("PubChem SID")!=null)?pubchem_sid=cpd.getIdentifiers("PubChem SID").iterator().next():""; //1st, only?
      fout_cpd_writer.printf(","+pubchem_sid);
      String chembl_id=(cpd.getIdentifiers("ChEMBL ID")!=null)?chembl_id=cpd.getIdentifiers("ChEMBL ID").iterator().next():""; //1st, only?
      fout_cpd_writer.printf(","+chembl_id);
      fout_cpd_writer.printf(","+"\""+cpd.getSmiles()+"\"");
      fout_cpd_writer.printf("\n");
    }

    fout_cpd_writer.close();
    String bhtm=(
        "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(servletname)+"\">\n"+
        "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout_cpd.getAbsolutePath()+"\">\n"+
        "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
        "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">"+
        "<B>Compounds CSV ("+file_utils.NiceBytes(fout_cpd.length())+")</B></BUTTON>\n</FORM>");
    return (bhtm);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Display target search results in HTML table.
  */
  public static String TargetTableHtm(
	DiseaseList diseaselist,
	String kid_query,
	CompoundList druglist,
	Integer cid_query,
	TargetList tlist,
	ArrayList<Integer> tids,
	Integer tid_query,
	HttpServletResponse response,
	String servletname)
  {
    int nmax=100;
    String thtm="<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n";
    thtm+="<TR><TH></TH><TH>tid</TH><TH>name</TH><TH>species</TH><TH>descr</TH><TH>r2q</TH></TR>\n";
    Collections.sort(tids);
    int n=0;
    for (int tid: tids)
    {
      Target tgt=tlist.get(tid);
      if (tgt==null)
      {
        System.err.println("ERROR: Aaack! No Target for tid="+tid);
        continue;
      }
      String rhtm="<TR>";
      rhtm+="<TD VALIGN=TOP ALIGN=RIGHT>"+(++n)+".</TD>";
      rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">";
      rhtm+=("<a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewtarget=TRUE&tid="+tid+"','tgtwin','width=600,height=600,scrollbars=1,resizable=1')\">"+tid+"</a>\n");
      rhtm+="</TD>";
      rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">"+tgt.getName()+"</TD>";
      rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">"+tgt.getSpecies()+"</TD>";
      rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">"+tgt.getDescription()+"</TD>";
      if (kid_query!=null && !kid_query.isEmpty())
      {
        String kegg_link="http://www.genome.jp/dbget-bin/get_linkdb?-t+8+"+kid_query;
        //String kegg_link="http://rest.kegg.jp/link/genes/"+kid_query;
        Disease disease=diseaselist.get(kid_query);
        if (disease==null)
          rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">error: null diseaselist["+kid_query+"]</TD>";
        else if (disease.hasTID(tid))
          rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\"><A HREF=\""+kegg_link+"\" TARGET=\"keggwin\">KEGG disease link</A></TD>";
        else
          rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">hypothesis</TD>";
      }
      else if (cid_query!=null)
      {
        Compound drug=druglist.get(cid_query);
        if (drug==null)
          rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">error: null druglist["+cid_query+"]</TD>";
        else if (drug.hasTID(tid))
          rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">empirical</TD>";
        else
          rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">hypothesis</TD>";
      }
      else if (tid_query!=null)
      {
        if (tid==tid_query)
          rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\"white\">self</TD>";
        else
          rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">hypothesis</TD>";
      }
      else
      {
        rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\"white\">?</TD>";
      }
      rhtm+="</TR>\n";
      thtm+=rhtm;
    }
    thtm+="</TABLE>\n";
    return thtm;
  }

  /////////////////////////////////////////////////////////////////////////////
  public static String CyviewButtonHtm(
	String fout_subnet_path,
	String cyview_mode,	//"rgt", "rgtp", "full" or null [full].
	HttpServletResponse response,
	String contextpath,
	String subnet_title,
        String cyview,
	String proxy_prefix)
  {
    String cyview_opts="edgesmerge=TRUE&title="+URLEncoder.encode(subnet_title);
    String cyview_winname="CyView";
    if (cyview_mode!=null)
    {
      cyview_opts+=("&mode="+cyview_mode+"&layout=Circle");
      cyview_winname+=("_"+cyview_mode);
    }
    String htm=(
	"<BUTTON TYPE=BUTTON onClick=\"void window.open('"+cyview+"?"+cyview_opts+"&infile="+fout_subnet_path+"','"+cyview_winname+"','width=900,height=700,scrollbars=1,resizable=1')\">")
	+("CyView<IMG BORDER=0 HEIGHT=30 SRC=\"/"+proxy_prefix+contextpath+"/images/cy3logoOrange.svg\"></BUTTON>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static String CYJSDownloadButtonHtm(
	String fout_path,
	String fname,
	HttpServletResponse response,
        String servletname)
  {
    File fout = new File(fout_path);
    String htm=(
      "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(servletname)+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout_path+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
      "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">"+
      "<B>Network CYJS ("+file_utils.NiceBytes(fout.length())+")</B></BUTTON>\n</FORM>");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Display extracted subnet results totals and download button.  
  */
  public static String SubnetResultsHtm(
	HashMap<String,Integer> subnet_counts,
	String fout_rgt_path, //reduced-graph, tgts only
	String fout_rgtp_path, //reduced-graph, tgts+CCPs
	String fout_subnet_path, //full-graph
	String title,
	HttpServletResponse response,
	String contextpath,
        String servletname,
        String cyview,
	String proxy_prefix)
  {
    String htm="";
    Integer n_node_cpd=((subnet_counts.get("n_node_cpd")!=null)?subnet_counts.get("n_node_cpd"):0);
    Integer n_node_tgt=((subnet_counts.get("n_node_tgt")!=null)?subnet_counts.get("n_node_tgt"):0);
    Integer n_node_scaf=((subnet_counts.get("n_node_scaf")!=null)?subnet_counts.get("n_node_scaf"):0);
    Integer n_node_mces=((subnet_counts.get("n_node_mces")!=null)?subnet_counts.get("n_node_mces"):0);
    Integer n_node_dis=((subnet_counts.get("n_node_dis")!=null)?subnet_counts.get("n_node_dis"):0);
    Integer n_node_total=n_node_cpd+n_node_tgt+n_node_scaf+n_node_mces+n_node_dis;
    Integer n_edge_act=((subnet_counts.get("n_edge_act")!=null)?subnet_counts.get("n_edge_act"):0);
    Integer n_edge_scaf=((subnet_counts.get("n_edge_scaf")!=null)?subnet_counts.get("n_edge_scaf"):0);
    Integer n_edge_mces=((subnet_counts.get("n_edge_mces")!=null)?subnet_counts.get("n_edge_mces"):0);
    Integer n_edge_total=n_edge_act+n_edge_scaf+n_edge_mces;
    Integer n_edge_tt=((subnet_counts.get("n_edge_tt")!=null)?subnet_counts.get("n_edge_tt"):0);
    Integer n_edge_tp=((subnet_counts.get("n_edge_tp")!=null)?subnet_counts.get("n_edge_tp"):0);
    if (n_node_total==0)
      return ("NOTE: subnet empty; no targets nor compounds found.");

    String advice_full=((n_node_total>1000)?"&larr;NOT recommended, since total nodes &gt;1000.":"");
    String advice_rgt=((n_node_tgt>100)?"&larr;NOT recommended, since target nodes &gt;100.":"");
    String advice_rgtp=(((n_node_tgt+n_node_scaf+n_node_mces)>100)?"&larr;NOT recommended, since target+CCPs nodes &gt;100.":"");

    String thtm_butts="<TABLE CELLSPACING=5 CELLPADDING=5>\n";
    if (fout_rgt_path!=null)
    {
      String bhtm_rgt=CyviewButtonHtm(fout_rgt_path, "rgt", response, contextpath, title, cyview, proxy_prefix);
      thtm_butts+=("<TR><TD ALIGN=RIGHT><H3>Lean:</H3>");
      thtm_butts+=("<B>(targets-only)</B><BR/></TD>");
      thtm_butts+=("<TD ALIGN=CENTER VALIGN=MIDDLE>"+bhtm_rgt+"</TD>\n");
      thtm_butts+=("<TD>nodes: "+n_node_tgt+"<BR/>edges: "+n_edge_tt+"</TD>");
      thtm_butts+=("<TD ALIGN=LEFT><B><I>"+advice_rgt+"</I></B></TD>\n");
      thtm_butts+=("</TR>\n");
    }
    if (fout_rgtp_path!=null)
    {
      String bhtm_rgtp=CyviewButtonHtm(fout_rgtp_path, "rgtp", response, contextpath, title, cyview, proxy_prefix);
      thtm_butts+=("<TR><TD ALIGN=RIGHT><H3>Medium:</H3>");
      thtm_butts+=("<B>(targets+CCPs)</B><BR/></TD>");
      thtm_butts+=("<TD ALIGN=CENTER VALIGN=MIDDLE>"+bhtm_rgtp+"</TD>\n");
      thtm_butts+=("<TD>nodes: "+(n_node_tgt+n_node_scaf+n_node_mces)+"<BR/>edges: "+(n_edge_tt+n_edge_tp)+"</TD>");
      thtm_butts+=("<TD ALIGN=LEFT><B><I>"+advice_rgtp+"</I></B></TD>\n");
      thtm_butts+=("</TR>\n");
    }
    String bhtm_full=CyviewButtonHtm(fout_subnet_path, null, response, contextpath, title, cyview, proxy_prefix);
    thtm_butts+=("<TR><TD ALIGN=RIGHT><H3>Full:</H3></TD>");
    thtm_butts+=("<TD ALIGN=CENTER VALIGN=MIDDLE>"+bhtm_full+"</TD>\n");
    thtm_butts+=("<TD>nodes: "+n_node_total+"<BR/>edges: "+n_edge_total+"</TD>");
    thtm_butts+=("<TD ALIGN=LEFT><B><I>"+advice_full+"</I></B></TD>\n");
    thtm_butts+=("</TR>\n");
    thtm_butts+=("</TABLE>\n");
    htm+=(thtm_butts);
    return htm;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For compounds download.
  */
  public static String CompoundSDFButtonHtm(
	String fout_cpd_path,
	HttpServletResponse response,
	String contextpath,
	HttpParams params,
	String fname,
        String servletname)
  {
    File fout_cpd = (fout_cpd_path==null?null:new File(fout_cpd_path));
    String htm="";
    if (fout_cpd_path!=null)
    {
      String bhtm_cpd=(
      "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(servletname)+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout_cpd_path+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
      "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">"+
      "<B>Compounds SDF ("+file_utils.NiceBytes(fout_cpd.length())+")</B></BUTTON>\n</FORM>");
      htm+=(bhtm_cpd);
    }
    return htm;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For popup window display of one target.
  */
  public static String ViewTargetHtm(Integer tid,TargetList tgtlist,
	HttpServletResponse response,String servletname)
  {
    String htm="<H3>Target Summary [ID = "+tid+"]:</H3>\n";
    if (!tgtlist.containsKey(tid)) return htm;
    Target tgt=tgtlist.get(tid);

    String thtm=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
    thtm+="<TR><TD ALIGN=RIGHT>target name</TD><TD BGCOLOR=\"white\">"+tgt.getName()+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>active compounds</TD><TD BGCOLOR=\"white\">"+tgt.getCompoundCount()+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>species</TD><TD BGCOLOR=\"white\">"+tgt.getSpecies()+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>type</TD><TD BGCOLOR=\"white\">"+tgt.getType()+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>description</TD><TD BGCOLOR=\"white\">"+tgt.getDescription()+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>IDs</TD><TD BGCOLOR=\"white\">";
    ArrayList<String> idtype_list = tgt.getIDTypes();
    for (String idtype: idtype_list)
    {
      thtm+=("<B>"+idtype+":</B><UL>\n");
      ArrayList<String> id_list = tgt.getIDList(idtype);
      for (String id: id_list) thtm+=("<LI>"+id+"\n");
      thtm+="</UL>\n";
    }
    thtm+="</TD></TR>\n";
    thtm+="</TABLE>\n";

    htm+=thtm;

    return htm;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For popup window display of one target.
  */
  public static String ViewTargetHtm(Integer tid,DBCon dbcon,
	HttpServletResponse response,String servletname)
	throws SQLException
  {
    String htm="<H3>Target Summary [ID = "+tid+"]:</H3>\n";

    ResultSet rset = carlsbad_utils.GetTarget(dbcon,tid);
    // Returned columns: tid,tname,species,ttype,descr,id_type,id
    if (rset==null || !rset.next()) return htm;

    String tname=rset.getString("tname");
    String species=rset.getString("species");
    String ttype=rset.getString("ttype");
    String descr=rset.getString("descr");
    HashMap<String, HashSet<String> > ids = new HashMap<String,HashSet<String> >();
    do {
      String id_type=rset.getString("id_type");
      String id=rset.getString("id");
      if (id_type!=null && id!=null)
      {
        if (!ids.containsKey(id_type)) ids.put(id_type,new HashSet<String>());
        ids.get(id_type).add(id.replaceFirst("[\\s]+$",""));
      }
    }
    while (rset.next());

    String thtm=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
    thtm+="<TR><TD ALIGN=RIGHT>target name</TD><TD BGCOLOR=\"white\">"+tname+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>species</TD><TD BGCOLOR=\"white\">"+species+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>type</TD><TD BGCOLOR=\"white\">"+ttype+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>description</TD><TD BGCOLOR=\"white\">"+descr+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>IDs</TD><TD BGCOLOR=\"white\">";
    ArrayList<String> idtype_list = new ArrayList<String>(ids.keySet());
    Collections.sort(idtype_list);
    for (String idtype: idtype_list)
    {
      thtm+=("<B>"+idtype+":</B><UL>\n");
      ArrayList<String> id_list = new ArrayList<String>(ids.get(idtype));
      Collections.sort(id_list);
      for (String id: id_list) thtm+=("<LI>"+id+"\n");
      thtm+="</UL>\n";
    }
    thtm+="</TD></TR>\n";
    thtm+="</TABLE>\n";

    htm+=thtm;

    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void FlagTargetsEmpirical(
	TargetList tgtlist,	//hit targets
	CompoundList druglist,	//all drugs
	Integer tid_query,	//non-null if tgt-query
	Integer cid_query,	//non-null if drug-query
	DiseaseList diseaselist,	//all diseases
	String kid_query	//non-null if disease-query
	)
  {
    if (tid_query!=null && tgtlist.containsKey(tid_query))
    {
      tgtlist.get(tid_query).setEmpirical(true);
    }
    if (kid_query!=null && diseaselist.containsKey(kid_query))
    {
      System.err.println("DEBUG: (FlagTargetsEmpirical) kid_query="+kid_query);
      for (int tid: diseaselist.get(kid_query).getTIDs())
      {
        System.err.println("DEBUG: (FlagTargetsEmpirical) tid="+tid);
        if (tgtlist.containsKey(tid)) tgtlist.get(tid).setEmpirical(true);
      }
    }
    if (cid_query!=null && druglist.containsKey(cid_query))
    {
      Drug drug = (Drug) druglist.get(cid_query);
      int i=0;
      for (int tid: tgtlist.keySet())
      {
        Target tgt=tgtlist.get(tid);
        tgt.setEmpirical(drug.hasTID(tid));
      }
    }
    return;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void FlagCompoundsEmpirical(
	CompoundList cpdlist,
	TargetList tgtlist,
	DiseaseList diseaselist,
	String kid_query)
  {
    if (kid_query==null) return;
    Disease disease=diseaselist.get(kid_query);
    if (disease==null) return; //error
    for (int tid: disease.getTIDs())
      FlagCompoundsEmpirical(cpdlist,tgtlist,tid,null);
    return;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	For drug-queries, targets must be flagged 1st.
	Target-query:
	Flag cpds active on target.
	Disease-query:
	Flag cpds active on targets linked to disease.
	Drug-query:
	After tgtlist has been flagged based on a drug-query, 
	flag cpdlist based on empirical drug-target-compound links.
  */
  public static void FlagCompoundsEmpirical(
	CompoundList cpdlist,
	TargetList tgtlist,
	Integer tid_query, //non-null if disease- or target-query
	Integer cid_query //non-null if drug-query
	)
  {
    if (tid_query!=null)
    {
      for (Compound cpd: cpdlist.values())
        cpd.setEmpirical(cpd.isEmpirical()||cpd.hasTID(tid_query));
    }
    if (cid_query!=null && cpdlist.containsKey(cid_query))
    {
      cpdlist.get(cid_query).setEmpirical(true);
      for (Compound cpd: cpdlist.values())
      {
        for (int tid: cpd.getTIDs())
        {
          if (tgtlist.containsKey(tid))
          {
            Target tgt=tgtlist.get(tid);
            cpd.setEmpirical(tgt.isEmpirical() && cpd.hasTID(tid));
          }
        }
      }
    }
    return;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	For popup window display of TargetList.
  */
  public static String ViewTargetsHtm(
	TargetList tgtlist,	//2nd choice
	HashMap<String,TargetList> tgtlistcache,	//1st choice
	String etag,	//1st choice; "all" is pseudo-etag
	String sortby,
	Integer skip,Integer nmax,
	HttpServletResponse response,String servletname)
	throws Exception
  {
    if (skip==null) skip=0;
    if (nmax==null) nmax=100;
    if (etag!=null && !etag.equals("all"))
    {
      if (tgtlistcache.containsKey(etag))
        tgtlist=tgtlistcache.get(etag);
      else
        //return ("ERROR: expired session, etag: "+etag);
        return ("ERROR: expired session.  Please re-query.");
    }
    int n_tgt=tgtlist.size();

    String url=response.encodeURL(servletname)+"?viewtargets=TRUE";
    if (etag!=null) url+=("&etag="+URLEncoder.encode(etag,"UTF-8"));
    if (sortby==null) sortby="id";

    String bhtm_1stpg=(skip>0)?("<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip=0&nmax="+nmax+"')\">1st</a>&nbsp;&gt;&nbsp;\n"):"";
    String bhtm_prevpg=(skip>nmax)?("<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip="+(Math.max(skip-nmax,0))+"&nmax="+nmax+"')\">prev</a>&nbsp;&gt;&nbsp;\n"):"";
    String bhtm_nextpg=(n_tgt>skip+2*nmax)?("&nbsp;&gt;&nbsp;<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip="+(skip+nmax)+"&nmax="+nmax+"')\">next</a>\n"):"";
    String bhtm_lastpg=(n_tgt>skip+nmax)?("&nbsp;&gt;&nbsp;<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip="+(n_tgt%nmax==0?(n_tgt-nmax):(n_tgt-(n_tgt%nmax)))+"&nmax="+nmax+"')\">last</a>\n"):"";

    String htm="<TABLE WIDTH=\"100%\"><TD><H1>Targets</H1></TD>\n";
    if (nmax<n_tgt) { htm+=("<TD ALIGN=CENTER><I>"+bhtm_1stpg+bhtm_prevpg+("<b>["+(skip+1)+"-"+Math.min(skip+nmax,n_tgt)+"]</b>")+bhtm_nextpg+bhtm_lastpg+"</I></TD>"); }
    htm+=("<TD ALIGN=RIGHT>count: "+n_tgt+"<BR>\n");
    htm+=("</TD></TR></TABLE>\n");
    String thtm=("<TABLE WIDTH=\"100%\" CELLPADDING=2 CELLSPACING=2>\n");

    //Sorting sets skip=0
    String bhtm_id=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=id')\">"+(sortby.equals ("id")?"<B>tid</B>":"tid")+"</BUTTON>\n");
    String bhtm_name=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=name')\">"+(sortby.equals("name")?"<B>name</B>":"name")+"</BUTTON>\n");
    String bhtm_type=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=type')\">"+(sortby.equals("type")?"<B>type</B>":"type")+"</BUTTON>\n");
    String bhtm_species=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=species')\">"+(sortby.equals("species")?"<B>species</B>":"species")+"</BUTTON>\n");
    String bhtm_cpd=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=n_cpd')\">"+(sortby.equals("n_cpd")?"<B>n_cpd</B>":"n_cpd")+"</BUTTON>\n");
    String bhtm_r2q=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=r2q')\">"+(sortby.equals("r2q")?"<B>r2q</B>":"r2q")+"</BUTTON>\n");

    thtm+="<TR><TD></TD><TD ALIGN=CENTER>"+bhtm_id+"</TD>";
    thtm+="<TD ALIGN=CENTER>"+bhtm_name+"</TD>\n";
    thtm+="<TD ALIGN=CENTER>"+bhtm_species+"</TD>\n";
    thtm+="<TD ALIGN=CENTER>"+bhtm_type+"</TD>\n";
    thtm+="<TD ALIGN=CENTER>"+bhtm_cpd+"</TD>\n";
    if (!etag.equals("all")) thtm+="<TD ALIGN=CENTER>"+bhtm_r2q+"</TD>\n";
    thtm+="</TR>\n";

    ArrayList<Target> tgts;
    if (sortby.equals("n_cpd"))   tgts=tgtlist.getTargetsSortedBy("n_cpd",true);
    else if (sortby.equals("name"))    tgts=tgtlist.getTargetsSortedBy("name",false);
    else if (sortby.equals("type"))    tgts=tgtlist.getTargetsSortedBy("type",false);
    else if (sortby.equals("species")) tgts=tgtlist.getTargetsSortedBy("species",false);
    else if (sortby.equals("r2q"))     tgts=tgtlist.getTargetsSortedBy("r2q",false);
    else                               tgts=tgtlist.getTargetsSortedBy("id",false);

    int i=0;
    for (Target tgt: tgts)
    {
      if (i++<skip) continue;
      Integer tid=tgt.getID();
      String bgcol=((etag.equals("all")||tgt.isEmpirical())?"white":"#ffbbbb");
      String rhtm="<TR><TD ALIGN=RIGHT>"+i+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">";
      rhtm+=("<a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewtarget=TRUE&tid="+tid+"','tgtwin','width=600,height=600,scrollbars=1,resizable=1')\">"+tid+"</a>\n");
      rhtm+="</TD>";
      rhtm+="<TD VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+tgt.getName()+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+tgt.getSpecies()+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+tgt.getType()+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+tgt.getCompoundCount()+"</TD>";
      if (!etag.equals("all"))
        rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+(tgt.isEmpirical()?"emp":"hyp")+"</TD>";
      rhtm+="</TR>\n";
      thtm+=rhtm;
      if (i-skip>=nmax) break;
    }
    thtm+="</TABLE>\n";
    htm+=thtm;
    tgtlist.refreshTimestamp(); //update TS
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	For popup window display of all diseases.
  */
  public static String ViewDiseasesHtm(DiseaseList dlist,TargetList tlist,String sortby,
	HttpServletResponse response,String servletname)
  {
    String htm=("<TABLE WIDTH=\"100%\"><TR><TD><H1>Diseases</H1></TD><TD ALIGN=RIGHT>count: "+dlist.size()+"</TD></TR></TABLE>\n");
    String thtm=("<TABLE WIDTH=\"100%\" CELLSPACING=2>\n");

    String url=response.encodeURL(servletname)+"?viewdiseases=TRUE";
    String bhtm_id=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=id')\">kid</BUTTON>\n");
    String bhtm_name=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=name')\">name</BUTTON>\n");
    String bhtm_tgt=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=n_tgt')\">n_tgt</BUTTON>\n");
    String bhtm_cpd=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=n_cpd')\">n_cpd</BUTTON>\n");

    thtm+="<TR><TD></TD><TD ALIGN=CENTER>"+bhtm_id+"</TD>";
    thtm+="<TD ALIGN=CENTER>"+bhtm_name+"</TD>";
    thtm+="<TD ALIGN=CENTER>"+bhtm_tgt+"</TD>";
    thtm+="<TD ALIGN=CENTER>"+bhtm_cpd+"</TD>";
    thtm+="</TR>\n";

    ArrayList<Disease> diseases;
    if (sortby.equals("n_tgt"))     diseases=dlist.getDiseasesSortedBy("n_tgt",true);
    if (sortby.equals("n_cpd"))     diseases=dlist.getDiseasesSortedBy("n_cpd",true);
    else if (sortby.equals("name")) diseases=dlist.getDiseasesSortedBy("name",false);
    else                            diseases=dlist.getDiseasesSortedBy("id",false);

    int i=0;
    for (Disease disease: diseases)
    {
      i++;
      String kid=disease.getID();
      String rhtm="<TR><TD ALIGN=RIGHT>"+i+"</TD>";
      rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">";
      rhtm+=("<a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewdisease=TRUE&kid="+kid+"','diseasewin','width=600,height=600,scrollbars=1,resizable=1')\">"+kid+"</a>\n");
      rhtm+="</TD>";
      rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">"+disease.getName()+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\"white\">"+disease.getTargetCount()+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\"white\">"+disease.getCompoundCount()+"</TD>";
      rhtm+="</TR>\n";
      thtm+=rhtm;
    }
    thtm+="</TABLE>\n";
    htm+=thtm;
    htm+=("diseaselist timestamp: "+dlist.getTimestamp().toString()+"<BR>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	For popup window display of one disease.
  */
  public static String ViewDiseaseHtm(String kid,
	DiseaseList dlist,
	TargetList tlist,
	DBCon dbcon,
	HttpServletResponse response,
	String contextpath,
	String servletname,
	String proxy_prefix)
	throws SQLException
  {
    String htm="<H3>Disease Summary [ID = "+kid+"]:</H3>\n";

    Disease disease = dlist.get(kid);
    String name = disease.getName();
    String kegg_url="http://www.kegg.jp/dbget-bin/www_bget";

    String imghtm=("<IMG ALIGN=MIDDLE BORDER=0 HEIGHT=50 SRC=\"/"+proxy_prefix+contextpath+"/images/kegg_logo.gif\">");
    String kegg_butt="<BUTTON TYPE=BUTTON onClick=\"void window.open('"+kegg_url+"?"+kid+"','keggwin','')\">View in KEGG"+imghtm+"</BUTTON>";

    String thtm=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
    thtm+="<TR><TD ALIGN=RIGHT>KEGG ID</TD><TD BGCOLOR=\"white\">"+kid+"&nbsp;"+kegg_butt+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>name</TD><TD BGCOLOR=\"white\">"+name+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>active compounds</TD><TD BGCOLOR=\"white\">"+disease.getCompoundCount()+"</TD></TR>\n";
    //thtm+="<TR><TD ALIGN=RIGHT>KEGG URL</TD><TD BGCOLOR=\"white\">";
    //thtm+="<A HREF=\"http://www.kegg.jp/dbget-bin/www_bget?"+kid+"\" target=\"keggwin\">"+kid+"</A></TD></TR>\n";

    String thtm2="<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n";
    thtm2+=("<TR><TH>tid</TH><TH>name</TH><TH>species</TH></TR>\n");
    for (int tid: disease.getTIDs())
    {
      String rhtm=("<TR><TD>");
      rhtm+=("<a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewtarget=TRUE&tid="+tid+"','tgtwin','width=600,height=600,scrollbars=1,resizable=1')\">"+tid+"</a>\n");
      rhtm+=("</TD>");
      Target tgt = tlist.get(tid);
      if (tgt==null)
      {
        rhtm+="<TD COLSPAN=2>Error: not found in TargetList</TD>\n";
      }
      else
      {
        rhtm+=("<TD>"+tgt.getName()+"</TD>\n");
        rhtm+=("<TD>"+tgt.getSpecies()+"</TD>\n");
      }
      rhtm+=("</TR>\n");
      thtm2+=rhtm;
    }
    thtm2+=("</TABLE>\n");

    thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>targets</TD><TD BGCOLOR=\"white\">"+thtm2+"</TD></TR>\n";
    thtm+="</TABLE>\n";

    htm+=thtm;

    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	For popup window display of one compound.
  */
  public static String ViewCompoundHtm(Integer cid,DBCon dbcon,String mol2img_servleturl,
	HttpServletResponse response,String servletname)
	throws SQLException
  {
    Boolean human_only=true;
    String htm="<H3>Compound Summary [ID = "+cid+"]:</H3>\n";

    ResultSet rset = carlsbad_utils.GetCompound(dbcon,cid);
    // Returned columns: smiles,mol_weight,mol_formula,
    // nass_tested,nass_active,nsam_tested,nsam_active, //not populated?
    // mcesid,iupac_name,is_drug,synonym
    if (rset==null || !rset.next()) return htm;
    String smi=rset.getString("smiles");
    Float mwt=rset.getFloat("mol_weight");
    String mf=rset.getString("mol_formula");
    Integer nass_tested=rset.getInt("nass_tested");
    Integer nass_active=rset.getInt("nass_active");
    Integer nsam_tested=rset.getInt("nsam_tested");
    Integer nsam_active=rset.getInt("nsam_active");
    Integer mcesid=rset.getInt("mcesid");
    String iupac_name=rset.getString("iupac_name");
    Boolean is_drug=rset.getBoolean("is_drug");
    HashSet<String> synonyms = new HashSet<String>();
    do {
      String synonym = rset.getString("synonym");
      if (synonym!=null) synonyms.add(synonym.replaceFirst("[\\s]+$",""));
      is_drug|=rset.getBoolean("is_drug");
    } while (rset.next());

    String depopts=("mode=cow&imgfmt=png&kekule=true");
    String imghtm=(smi!=null?HtmUtils.Smi2ImgHtm(smi,depopts,160,240,mol2img_servleturl,true,4,"go_zoom_smi2img"):"~");

    String thtm=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
    thtm+="<TR><TD ALIGN=RIGHT></TD><TD BGCOLOR=\"white\">"+imghtm+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>iupac name</TD><TD BGCOLOR=\"white\">"+iupac_name+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>smiles</TD><TD BGCOLOR=\"white\">"+smi+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>is_drug</TD><TD BGCOLOR=\"white\">"+is_drug+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>ids</TD><TD BGCOLOR=\"white\">";
    rset=carlsbad_utils.GetCompoundsIDs(dbcon,new HashSet<Integer>(Arrays.asList(cid)));
    Compound cpd = new Compound(cid);
    while (rset.next()) //cid,id_type,id
    {
      String id_type=rset.getString("id_type");
      String id=rset.getString("id");
      if (id_type!=null && id!=null) cpd.addIdentifier(id_type,id);
    }
    for (String id_type: cpd.getIdentifierTypesSorted())
      for (String id: cpd.getIdentifiersSorted(id_type))
        thtm+=(id_type+": "+id+"<br/>\n");
    thtm+="</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>synonyms</TD><TD BGCOLOR=\"white\">";
    ArrayList<String> synonym_list = new ArrayList<String>(synonyms);
    Collections.sort(synonym_list);
    for (String synonym: synonym_list) thtm+=(synonym+"<BR/>\n");
    thtm+="</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>molecular weight</TD><TD BGCOLOR=\"white\">"+mwt+"</TD></TR>\n";
    thtm+="<TR><TD ALIGN=RIGHT>molecular formula</TD><TD BGCOLOR=\"white\">"+mf+"</TD></TR>\n";
    rset=carlsbad_utils.GetCompoundTargets(dbcon,cid,human_only);
    // Returned columns: tid, tname, species, atype, avalue, confidence
    String thtm2="<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n";
    thtm2+="<TR><TD ALIGN=CENTER>Target ID</TD>";
    thtm2+="<TD ALIGN=CENTER>target</TD>";
    thtm2+="<TD ALIGN=CENTER>species</TD>";
    thtm2+="<TD ALIGN=CENTER>activity type</TD>";
    thtm2+="<TD ALIGN=CENTER>activity value<BR/>-log(M)</TD>";
    thtm2+="<TD ALIGN=CENTER>activity confidence</TD>";
    thtm2+="</TR>\n";
    while (rset!=null && rset.next())
    {
      String rhtm="<TR><TD ALIGN=CENTER>"+rset.getInt("tid")+"</TD>";
      rhtm+="<TD ALIGN=CENTER>"+rset.getString("tname")+"</TD>\n";
      rhtm+="<TD ALIGN=CENTER>"+rset.getString("species")+"</TD>\n";
      rhtm+="<TD ALIGN=CENTER>"+rset.getString("atype")+"</TD>\n";
      rhtm+="<TD ALIGN=CENTER>"+rset.getFloat("avalue")+"</TD>\n";
      rhtm+="<TD ALIGN=CENTER>"+rset.getInt("confidence")+"</TD>\n";
      rhtm+="</TR>\n";
      thtm2+=rhtm;
    }
    thtm2+="</TABLE>\n";
    thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>activity</TD><TD BGCOLOR=\"white\">"+thtm2+"</TD></TR>\n";

    rset=carlsbad_utils.GetCompoundScaffolds(dbcon,cid);
    // Returned columns: scafid, scafsmi, natoms, is_largest
    String thtm3="<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n";
    while (rset!=null && rset.next())
    {
      smi=rset.getString("scafsmi");
      String rhtm="<TR><TD ALIGN=CENTER>"+rset.getInt("scafid")+"</TD>";
      imghtm=(smi!=null?HtmUtils.Smi2ImgHtm(smi,depopts,80,120,mol2img_servleturl,true,4,"go_zoom_smi2img"):"~");
      rhtm+="<TD ALIGN=CENTER BGCOLOR=\"white\">"+imghtm+"</TD>\n";
      rhtm+="<TD VALIGN=TOP BGCOLOR=\"white\">"+smi+"</TD></TR>\n";
      thtm3+=rhtm;
    }
    thtm3+="</TABLE>\n";
    thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>scaffolds</TD><TD ALIGN=CENTER BGCOLOR=\"white\">"+thtm3+"</TD></TR>\n";

    rset=carlsbad_utils.GetCompoundMces(dbcon,cid);
    // Returned columns: mcesid, mcessma
    if (rset!=null && rset.next()) //0 or 1 only
    {
      String sma=rset.getString("mcessma");
      imghtm=(sma!=null?HtmUtils.Smi2ImgHtm(sma,depopts,80,120,mol2img_servleturl,true,4,"go_zoom_smi2img"):"~");

      String thtm4="<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n";
      thtm4+="<TR><TD ALIGN=CENTER>"+rset.getInt("mcesid")+"</TD>";
      thtm4+="<TD ALIGN=CENTER BGCOLOR=\"white\">"+imghtm+"</TD>\n";
      thtm4+="<TD VALIGN=TOP BGCOLOR=\"white\">"+sma+"</TD></TR>\n";
      thtm4+="</TABLE>\n";
      thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>MCES</TD><TD BGCOLOR=\"white\">"+thtm4+"</TD></TR>\n";
    }

    thtm+="</TABLE>\n";
    htm+=thtm;
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	For popup window display of hitlist compounds, from cache or file.
	Note that in viewdrugs mode: cpdlist is DRUGLIST, psudo-etag "drugs".
  */
  public static String ViewCompoundsHtm(
	CompoundList cpdlist,
	HashMap<String,CompoundList> cpdlistcache,
	String etag,
	String infile,
	String sortby,
	Integer skip,
	Integer nmax,
	String title,
	String mol2img_servleturl,
	HttpServletResponse response,String servletname)
	throws Exception
  {
    if (cpdlist==null)
    {
      if (etag!=null)
      {
        if (cpdlistcache.containsKey(etag))
          cpdlist=cpdlistcache.get(etag); //faster, preferred
        else
          //return ("ERROR: expired session, etag: "+etag);
          return ("ERROR: expired session.  Please re-query.");
      }
      else
      {
        cpdlist = new CompoundList();
        cpdlist.load(new File(infile)); //slower
      }
    }
    int n_cpd=cpdlist.size();

    if (skip==null) skip=0;
    if (nmax==null) nmax=100;

    String url=response.encodeURL(servletname)+"?viewcompounds=TRUE";
    if (etag!=null) url+=("&etag="+URLEncoder.encode(etag,"UTF-8"));
    if (infile!=null) url+=("&infile="+URLEncoder.encode(infile,"UTF-8"));
    if (sortby==null) sortby="id";

    String bhtm_1stpg=(skip>0)?("<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip=0&nmax="+nmax+"')\">1st</a>&nbsp;&gt;&nbsp;\n"):"";
    String bhtm_prevpg=(skip>nmax)?("<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip="+(Math.max(skip-nmax,0))+"&nmax="+nmax+"')\">prev</a>&nbsp;&gt;&nbsp;\n"):"";
    String bhtm_nextpg=(n_cpd>skip+2*nmax)?("&nbsp;&gt;&nbsp;<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip="+(skip+nmax)+"&nmax="+nmax+"')\">next</a>\n"):"";
    String bhtm_lastpg=(n_cpd>skip+nmax)?("&nbsp;&gt;&nbsp;<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip="+(n_cpd%nmax==0?(n_cpd-nmax):(n_cpd-(n_cpd%nmax)))+"&nmax="+nmax+"')\">last</a>\n"):"";

    String htm="<TABLE WIDTH=\"100%\"><TR><TD><H1>"+(title==null?"Compounds":title)+"</H1></TD>\n";
    if (nmax<n_cpd) { htm+=("<TD ALIGN=CENTER><I>"+bhtm_1stpg+bhtm_prevpg+("<b>["+(skip+1)+"-"+Math.min(skip+nmax,n_cpd)+"]</b>")+bhtm_nextpg+bhtm_lastpg+"</I></TD>"); }
    htm+=("<TD ALIGN=RIGHT>count: "+n_cpd+"</TD></TR></TABLE>\n");

    //Sorting sets skip=0
    String bhtm_id=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=id')\">"+(sortby.equals("id")?"<B>cid</B>":"cid")+"</BUTTON>\n");
    String bhtm_name=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=name')\">"+(sortby.equals("name")?"<B>name</B>":"name")+"</BUTTON>\n");
    String bhtm_tgt=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=n_tgt')\">"+(sortby.equals("n_tgt")?"<B>n_tgt</B>":"n_tgt")+"</BUTTON>\n");
    String bhtm_r2q=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=r2q')\">"+(sortby.equals("r2q")?"<B>r2q</B>":"r2q")+"</BUTTON>\n");

    String thtm=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
    thtm+="<TR><TD></TD><TD ALIGN=CENTER>"+bhtm_id+"</TD>";
    thtm+="<TD ALIGN=CENTER></TD>";
    thtm+="<TD ALIGN=CENTER>"+bhtm_name+"</TD>";
    thtm+="<TD ALIGN=CENTER>IDs</TD>";
    thtm+="<TD ALIGN=CENTER>"+bhtm_tgt+"</TD>\n";
    if (etag!=null && !etag.equals("drugs"))
      thtm+="<TD ALIGN=CENTER>"+bhtm_r2q+"</TD>\n";
    thtm+="</TR>\n";

    String depopts=("mode=cow&imgfmt=png&kekule=true");

    ArrayList<Compound> cpds;
    if (sortby.equals("n_tgt")) cpds=cpdlist.getAllSortedBy("n_tgt",true);
    else if (sortby.equals("name"))  cpds=cpdlist.getAllSortedBy("name",false);
    else if (sortby.equals("r2q"))   cpds=cpdlist.getAllSortedBy("r2q",false);
    else                             cpds=cpdlist.getAllSortedBy("id",false);

    int i=0;
    for (Compound cpd: cpds)
    {
      if (i++<skip) continue;
      String smi=cpd.getSmiles();
      Integer cid=cpd.getID();
      String bgcol=(((etag!=null && etag.equals("drugs"))||cpd.isEmpirical())?"white":"#ffbbbb");
      String imghtm=(smi!=null?HtmUtils.Smi2ImgHtm(smi,depopts,60,80,mol2img_servleturl,true,4,"go_zoom_smi2img"):"~");
      String rhtm="<TR><TD ALIGN=RIGHT VALIGN=TOP>"+i+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">";
      rhtm+=("<a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewcompound=TRUE&cid="+cid+"','cpdwin','width=600,height=600,scrollbars=1,resizable=1')\">"+cid+"</a>\n");
      rhtm+="</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+imghtm+"</TD>";
      rhtm+="<TD VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+cpd.getName()+"</TD>";

      rhtm+="<TD VALIGN=TOP BGCOLOR=\""+bgcol+"\">";
      for (String id_type: new String[]{"ChEMBL ID","PubChem SID"})
        if (cpd.getIdentifiers(id_type)!=null)
          for (String id: cpd.getIdentifiersSorted(id_type))
            { rhtm+=(id_type+": "+id+"<br/>\n"); break; } //1st only
      rhtm+="</TD>";

      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+cpd.getTargetCount()+"</TD>";
      if (etag!=null && !etag.equals("drugs"))
        rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+(cpd.isEmpirical()?"emp":"hyp")+"</TD>";
      rhtm+="</TR>\n";
      thtm+=rhtm;
      if (i-skip>=nmax) break;
    }
    thtm+="</TABLE>\n";
    htm+=thtm;
    cpdlist.refreshTimestamp(); //update TS
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	For popup window display of hitlist CCPs, from cache.
  */
  public static String ViewCCPsHtm(
	CCPList ccplist,
	HashMap<String,CCPList> ccplistcache,
	String etag,
	String sortby,
	Integer skip,
	Integer nmax,
	String title,
	String mol2img_servleturl,
	HttpServletResponse response,String servletname)
	throws Exception
  {
    if (ccplist==null)
    {
      if (etag==null) return ("ERROR: (aack!) No ccplist, no etag.");

      if (ccplistcache.containsKey(etag))
        ccplist=ccplistcache.get(etag); //faster, preferred
      else
        //return ("ERROR: expired session, etag: "+etag);
        return ("ERROR: expired session.  Please re-query.");
    }

    int n_ccp=ccplist.size();
    int n_scaf=ccplist.scaffoldCount();
    int n_mces=ccplist.mcesCount();

    if (skip==null) skip=0;
    if (nmax==null) nmax=100;

    String url=response.encodeURL(servletname)+"?viewccps=TRUE";
    if (etag!=null) url+=("&etag="+URLEncoder.encode(etag,"UTF-8"));
    if (sortby==null) sortby="id";

    String bhtm_1stpg=(skip>0)?("<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip=0&nmax="+nmax+"')\">1st</a>&nbsp;&gt;&nbsp;\n"):"";
    String bhtm_prevpg=(skip>nmax)?("<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip="+(Math.max(skip-nmax,0))+"&nmax="+nmax+"')\">prev</a>&nbsp;&gt;&nbsp;\n"):"";
    String bhtm_nextpg=(n_ccp>skip+2*nmax)?("&nbsp;&gt;&nbsp;<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip="+(skip+nmax)+"&nmax="+nmax+"')\">next</a>\n"):"";
    String bhtm_lastpg=(n_ccp>skip+nmax)?("&nbsp;&gt;&nbsp;<a href=\"javascript:void(0)\" onClick=\"window.location.replace('"+url+"&sortby="+sortby+"&skip="+(n_ccp%nmax==0?(n_ccp-nmax):(n_ccp-(n_ccp%nmax)))+"&nmax="+nmax+"')\">last</a>\n"):"";

    String htm="<TABLE WIDTH=\"100%\"><TR><TD><H1>"+(title==null?"CCPs":title)+"</H1></TD>\n";
    if (nmax<n_ccp) { htm+=("<TD ALIGN=CENTER><I>"+bhtm_1stpg+bhtm_prevpg+("<b>["+(skip+1)+"-"+Math.min(skip+nmax,n_ccp)+"]</b>")+bhtm_nextpg+bhtm_lastpg+"</I></TD>"); }
    htm+=("<TD ALIGN=RIGHT>scaffolds:"+n_scaf+"<BR>mcess:"+n_mces+"<BR>total ccps:"+n_ccp+"</TD></TR></TABLE>\n");

    //Sorting sets skip=0
    String bhtm_id=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=id')\">"+(sortby.equals("id")?"<B>id</B>":"id")+"</BUTTON>\n");
    String bhtm_atm=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=n_atm')\">"+(sortby.equals("n_atm")?"<B>n_atm</B>":"n_atm")+"</BUTTON>\n");
    String bhtm_cpd=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=n_cpd')\">"+(sortby.equals("n_cpd")?"<B>n_cpd</B>":"n_cpd")+"</BUTTON>\n");
    String bhtm_tgt=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=n_tgt')\">"+(sortby.equals("n_tgt")?"<B>n_tgt</B>":"n_tgt")+"</BUTTON>\n");
    String bhtm_type=("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+url+"&sortby=type')\">"+(sortby.equals("type")?"<B>type</B>":"type")+"</BUTTON>\n");

    String thtm=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
    thtm+="<TR><TD></TD><TD ALIGN=CENTER>"+bhtm_id+"</TD>";
    thtm+="<TD ALIGN=CENTER></TD>";
    thtm+="<TD ALIGN=CENTER>"+bhtm_atm+"</TD>\n";
    thtm+="<TD ALIGN=CENTER>"+bhtm_cpd+"</TD>\n";
    thtm+="<TD ALIGN=CENTER>"+bhtm_tgt+"</TD>\n";
    thtm+="<TD ALIGN=CENTER>"+bhtm_type+"</TD>\n";
    thtm+="</TR>\n";

    String depopts=("mode=cow&imgfmt=png&kekule=true");

    ArrayList<CCP> ccps;
    if (sortby.equals("n_atm")) ccps=ccplist.getAllSortedBy("n_atm",true);
    else if (sortby.equals("n_cpd")) ccps=ccplist.getAllSortedBy("n_cpd",true);
    else if (sortby.equals("n_tgt")) ccps=ccplist.getAllSortedBy("n_tgt",true);
    else if (sortby.equals("type")) ccps=ccplist.getAllSortedBy("type",true);
    else                             ccps=ccplist.getAllSortedBy("type",false);

    int i=0;
    for (CCP ccp: ccps)
    {
      if (i++<skip) continue;
      String smi=ccp.getSmiles();
      String sma=ccp.getSmarts();
      Integer id=ccp.getID();
      String ccptype=ccp.getType();

      String bgcol="white";
      String imghtm=
	(smi!=null? HtmUtils.Smi2ImgHtm(smi,depopts,60,80,mol2img_servleturl,true,4,"go_zoom_smi2img"):
		(sma!=null? HtmUtils.Smi2ImgHtm(sma,depopts,60,80,mol2img_servleturl,true,4,"go_zoom_smi2img"):
			"~"));
      String rhtm="<TR><TD ALIGN=RIGHT VALIGN=TOP>"+i+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">";
      rhtm+=("<a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewccp=TRUE&ccptype="+ccptype+"&id="+id+"','ccpwin','width=600,height=600,scrollbars=1,resizable=1')\">"+id+"</a>\n");
      rhtm+="</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+imghtm+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+ccp.getAtomCount()+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+ccp.getCompoundCount()+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+ccp.getTargetCount()+"</TD>";
      rhtm+="<TD ALIGN=CENTER VALIGN=TOP BGCOLOR=\""+bgcol+"\">"+ccptype+"</TD>";
      rhtm+="</TR>\n";
      thtm+=rhtm;
      if (i-skip>=nmax) break;
    }
    thtm+="</TABLE>\n";
    htm+=thtm;
    ccplist.refreshTimestamp(); //update TS
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	For popup window display of one CCP.
  */
  public static String ViewCCPHtm(Integer id,String ccptype,DBCon dbcon,String mol2img_servleturl,
	HttpServletResponse response,String servletname)
	throws SQLException
  {
    Boolean human_only=true;
    String htm="<H3>CCP Summary ["+ccptype+": ID = "+id+"]:</H3>\n";
    String thtm="";
    String imghtm;
    String depopts=("mode=cow&imgfmt=png&kekule=true");

    ResultSet rset;
    if (ccptype.equalsIgnoreCase("mces"))
    {
      rset = carlsbad_utils.GetMCES(dbcon,id);
      if (rset==null || !rset.next()) return htm;
      String sma=rset.getString("smarts");
      imghtm=(sma!=null?HtmUtils.Smi2ImgHtm(sma,depopts,160,240,mol2img_servleturl,true,4,"go_zoom_smi2img"):"~");
      thtm+=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
      thtm+="<TR><TD ALIGN=RIGHT>mces</TD><TD BGCOLOR=\"white\">"+imghtm+"</TD></TR>\n";
      thtm+="<TR><TD ALIGN=RIGHT>smarts</TD><TD BGCOLOR=\"white\">"+sma+"</TD></TR>\n";
      thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>compounds</TD><TD BGCOLOR=\"white\">";
      rset = carlsbad_utils.GetMCESCompounds(dbcon,id);
    }
    else //scaffold
    {
      rset = carlsbad_utils.GetScaffold(dbcon,id);
      if (rset==null || !rset.next()) return htm;
      String smi=rset.getString("smiles");
      imghtm=(smi!=null?HtmUtils.Smi2ImgHtm(smi,depopts,160,240,mol2img_servleturl,true,4,"go_zoom_smi2img"):"~");
      thtm+=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
      thtm+="<TR><TD ALIGN=RIGHT>scaffold</TD><TD BGCOLOR=\"white\">"+imghtm+"</TD></TR>\n";
      thtm+="<TR><TD ALIGN=RIGHT>smiles</TD><TD BGCOLOR=\"white\">"+smi+"</TD></TR>\n";
      thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>compounds</TD><TD BGCOLOR=\"white\">";
      rset = carlsbad_utils.GetScaffoldCompounds(dbcon,id);
    }

    HashSet<Integer> cids = new HashSet<Integer>();
    String thtm2=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
    while (rset.next()) //cid,smiles
    {
      Integer cid=rset.getInt("cid");
      cids.add(cid);
      String smi=rset.getString("smiles");
      imghtm=(smi!=null?HtmUtils.Smi2ImgHtm(smi,depopts,60,80,mol2img_servleturl,true,4,"go_zoom_smi2img"):"~");
      thtm2+="<TR><TD ALIGN=RIGHT WIDTH=\"25%\">";
      thtm2+=("<a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewcompound=TRUE&cid="+cid+"','cpdwin','width=600,height=600,scrollbars=1,resizable=1')\">"+cid+"</a>\n")+"</TD><TD BGCOLOR=\"white\">"+imghtm+"</TD></TR>\n";
    }
    thtm2+="</TABLE>";
    thtm+=(thtm2+"</TD></TR>");
    thtm+="<TR><TD ALIGN=RIGHT VALIGN=TOP>targets</TD><TD BGCOLOR=\"white\">";

    rset = carlsbad_utils.GetCompoundsTargets(dbcon,cids,human_only);
    HashSet<Integer> tids_visited = new HashSet<Integer>();
    thtm2=("<TABLE WIDTH=\"100%\" CELLSPACING=2 CELLPADDING=2>\n");
    while (rset.next()) //tid,tname,species,atype,avalue,confidence
    {
      Integer tid=rset.getInt("tid");
      if (tids_visited.contains(tid)) continue;
      else tids_visited.add(tid);
      thtm2+="<TR><TD ALIGN=RIGHT WIDTH=\"25%\">";
      thtm2+=("<a href=\"javascript:void(0)\" onClick=\"void window.open('"+response.encodeURL(servletname)+"?viewtarget=TRUE&tid="+tid+"','tgtwin','width=600,height=600,scrollbars=1,resizable=1')\">"+tid+"</a>\n")+"</TD><TD BGCOLOR=\"white\">"+rset.getString("tname")+"</TD></TR>\n";
    }
    thtm2+="</TABLE>";
    thtm+=(thtm2+"</TD></TR>");
    thtm+="</TABLE>\n";
    htm+=thtm;

    return htm;
  }
}
