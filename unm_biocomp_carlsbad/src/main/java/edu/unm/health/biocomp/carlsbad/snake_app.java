package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;
import edu.unm.health.biocomp.util.db.*;
import edu.unm.health.biocomp.smarts.smarts_utils;
import edu.unm.health.biocomp.cytoscape.*;

/**	Utility SNAKE command-line app for CARLSBAD database system.
	<br>
	@author Jeremy J Yang
*/
public class snake_app
{
  private static String DBHOST=null;
  private static String DBNAME=null;
  private static String DBUSR=null;
  private static String DBPW=null;
  /////////////////////////////////////////////////////////////////////////////
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"snake_app - carlsbad subnet extraction application\n"
      +"usage: snake_app [options]\n"
      +"  required:\n"
      +"    -dbhost <HOST> ..... ["+DBHOST+"]\n"
      +"    -dbname <NAME> ..... ["+DBNAME+"] \n"
      +"    -dbusr <USR> ....... ["+DBUSR+"] \n"
      +"    -dbpw <PW> ......... \n"
      +"  mode (one of):\n"
      +"    -describe .............. describe database\n"
      +"    -listtargets ........... \n"
      +"    -dump2xgmml ............ extract full network\n"
      +"    -t2pan ................. target to CCP-associated (sub)net (1-click fnc), requires -tgt_id\n"
      +"    (default is subnet extraction)\n"
      +"  query:\n"
      +"    -tgt_id <ID> .......... selected target identifier\n"
      +"    -tgt_id_type <IDTYPE> ... target identifier type (\"ChEMBL ID\",\"NCBI gi\",\"PDB\",\"Swissprot\",\n"
      +"                                \"UniProt\",etc.)  If not specified, any.\n"
      +"    -sbs_id <ID> .......... selected substance identifier\n"
      +"    -sbs_id_type <IDTYPE>  ... substance identifier type (\"ChEMBL ID\",\"IUPHAR Ligand ID\",\n"
      +"                                \"PDSP Record Number\",\"PubChem CID\",\"PubChem SID\",\n"
      +"                                \"SMDL ID\").  If not specified, any.\n"
      +"    -tids <IDLIST> ......... selected CARLSBAD target IDs (comma separated)\n"
      +"    -cids <IDLIST> ......... selected CARLSBAD compound IDs (comma separated)\n"
      +"    -scafids <IDLIST> ...... selected CARLSBAD scaffold IDs (comma separated)\n"
      +"    -mcesids <IDLIST> ...... selected CARLSBAD MCS cluster IDs (comma separated)\n"
      +"    -cname_exact <NAME> .... compound name query, exact\n"
      +"    -cname_sub <NAME> ...... compound name query, substring\n"
      +"    -cpd_query <SMILES|SMARTS> ....... compound query\n"
      +"    -species <SPECIES> ..... target species (exact string, e.g. human)\n"
      +"    -tclass <CLASS> ........ target class (exact string)\n"
      +"  options:\n"
      +"    -o XMLFILE ............. subnet XGMML for Cytoscape import\n"
      +"    -neighbortargets ....... include in subnet other targets for which compounds are active\n"
      +"    -globaldegrees ......... include in subnet global node degrees\n"
      +"    -substruct ............. substructural compound query\n"
      +"    -v ..................... verbose\n"
      +"    -vv .................... very verbose\n"
      +"    -h ..................... this help\n");
    System.exit(1);
  }
  private static int verbose=0;
  private static String ofile=null;
  private static Boolean describe=false;
  private static Boolean listtargets=false;
  private static Boolean t2pan=false;
  private static String tidsstr=null;
  private static String cidsstr=null;
  private static String scafidsstr=null;
  private static String mcesidsstr=null;
  private static Boolean dump2xgmml=false;
  private static Boolean neighbortargets=false;
  private static Boolean globaldegrees=false;
  private static Boolean substruct=false;
  private static String cname_exact=null;
  private static String cname_sub=null;
  private static String tclass=null;
  private static String species=null;
  private static String tgt_id=null;
  private static String tgt_id_type=null;
  private static String sbs_id=null;
  private static String sbs_id_type=null;
  private static String cpd_query=null;

  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    if (args.length==0) Help("");
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-describe")) describe=true;
      else if (args[i].equals("-listtargets")) listtargets=true;
      else if (args[i].equals("-dump2xgmml")) dump2xgmml=true;
      else if (args[i].equals("-substruct")) substruct=true;
      else if (args[i].equals("-t2pan")) t2pan=true;
      else if (args[i].equals("-neighbortargets")) neighbortargets=true;
      else if (args[i].equals("-globaldegrees") || args[i].equals("-horizoncounts")) globaldegrees=true;
      else if (args[i].equals("-tgt_id")) tgt_id=args[++i];
      else if (args[i].equals("-tgt_id_type")) tgt_id_type=args[++i];
      else if (args[i].equals("-sbs_id")) sbs_id=args[++i];
      else if (args[i].equals("-sbs_id_type")) sbs_id_type=args[++i];
      else if (args[i].equals("-tids")) tidsstr=args[++i];
      else if (args[i].equals("-cids")) cidsstr=args[++i];
      else if (args[i].equals("-scafids")) scafidsstr=args[++i];
      else if (args[i].equals("-mcesids")) mcesidsstr=args[++i];
      else if (args[i].equals("-dbhost")) DBHOST=args[++i];
      else if (args[i].equals("-dbname")) DBNAME=args[++i];
      else if (args[i].equals("-dbusr")) DBUSR=args[++i];
      else if (args[i].equals("-dbpw")) DBPW=args[++i];
      else if (args[i].equals("-cname_exact")) cname_exact=args[++i];
      else if (args[i].equals("-cname_sub")) cname_sub=args[++i];
      else if (args[i].equals("-tclass")) tclass=args[++i];
      else if (args[i].equals("-species")) species=args[++i];
      else if (args[i].equals("-cpd_query")) cpd_query=args[++i];
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-vv")) verbose=2;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Main for utility application.  Run with no args for online help.
  */
  public static void main(String[] args)
    throws IOException
  {
    ParseCommand(args);

    Boolean human_only=(species!=null && species.equals("human"));

    //Connection dbcon=null;
    DBCon dbcon=null;
    try { dbcon = new DBCon("postgres",DBHOST,5432,DBNAME,DBUSR,DBPW); }
    catch (SQLException e) { Help("PostgreSQL connection failed:"+e.getMessage()); }
    catch (Exception e) { Help("PostgreSQL connection failed:"+e.getMessage()); }

    if (!describe && !dump2xgmml && !t2pan
	&& tidsstr==null && cname_exact==null && cname_sub==null
	&& cidsstr==null && scafidsstr==null && mcesidsstr==null
	&& tclass==null 
	&& tgt_id==null
	&& sbs_id==null
	&& cpd_query==null
	)
    {
      Help("ERROR: no task or query specified.");
    }
    if (describe)
    {
      try { System.err.println(carlsbad_utils.DBDescribeTxt(dbcon)); }
      catch (SQLException e) { Help("PostgreSQL error:"+e.getMessage()); }
      System.exit(0);
    }
    if (ofile==null) Help("-o required for output subnet XGMML.");
    if (t2pan && tgt_id==null) Help("-tgt_id required for -t2pan mode.");

    ArrayList<Integer> tids = null;
    if (tidsstr!=null && !tidsstr.isEmpty())
    {
      tidsstr.replaceAll("\\s","");
      tids = new ArrayList<Integer>();
      for (String s: tidsstr.split(","))
        tids.add(Integer.parseInt(s));
      if (tids.isEmpty()) Help("ERROR: no tids parsed: \""+tidsstr+"\"");
      if (verbose>0) System.err.println("tids count: "+tids.size());
    }
    ArrayList<Integer> cids = null;
    if (cidsstr!=null && !cidsstr.isEmpty())
    {
      cidsstr.replaceAll("\\s","");
      cids = new ArrayList<Integer>();
      for (String s: cidsstr.split(","))
        cids.add(Integer.parseInt(s));
      if (cids.isEmpty()) Help("ERROR: no cids parsed: \""+cidsstr+"\"");
      if (verbose>0) System.err.println("cids count: "+cids.size());
    }
    ArrayList<Integer> scafids = null;
    if (scafidsstr!=null && !scafidsstr.isEmpty())
    {
      scafidsstr.replaceAll("\\s","");
      scafids = new ArrayList<Integer>();
      for (String s: scafidsstr.split(","))
        scafids.add(Integer.parseInt(s));
      if (scafids.isEmpty()) Help("ERROR: no scafids parsed: \""+scafidsstr+"\"");
      if (verbose>0) System.err.println("scafids count: "+scafids.size());
    }
    ArrayList<Integer> mcesids = null;
    if (mcesidsstr!=null && !mcesidsstr.isEmpty())
    {
      mcesidsstr.replaceAll("\\s","");
      mcesids = new ArrayList<Integer>();
      for (String s: mcesidsstr.split(","))
        mcesids.add(Integer.parseInt(s));
      if (mcesids.isEmpty()) Help("ERROR: no mcesids parsed: \""+mcesidsstr+"\"");
      if (verbose>0) System.err.println("mcesids count: "+mcesids.size());
    }

    String ttype=null;
    ArrayList<String> tclasses_selected=null;
    if (tclass!=null)
      tclasses_selected = new ArrayList<String>(Arrays.asList(tclass));
    String tname=null;
    Boolean matchtype_tname_sub=false;
    String tdesc=null;
    Boolean matchtype_tdesc_sub=false;
    String cname=null;
    Boolean matchtype_cname_sub=false;
    if (cname_exact!=null)
    {
      cname=cname_exact;
      matchtype_cname_sub=false;
    }
    else if (cname_sub!=null)
    {
      cname=cname_sub;
      matchtype_cname_sub=true;
    }
    String matchtype_qcpd=null;
    if (cpd_query!=null)
    {
      matchtype_qcpd=(substruct?"sub":"exa");
      if (smarts_utils.IsSmarts(cpd_query) && matchtype_qcpd.equals("exa"))
      { Help("ERROR: SMARTS query requires -substruct."); }
    }
    Float minsim=null;
    Integer mw_min=null;
    Integer mw_max=null;
    Integer n_max_targets=1000;
    Integer tid=null;

    // If tids not specified, must query for tids first:
    if (tids==null)
    {
      ResultSet rset=null;
      String sql=carlsbad_utils.TargetQuerySQL(
          new ArrayList<Integer>(Arrays.asList(tid)),
          ttype,
          tclasses_selected,
          tname,matchtype_tname_sub,
          tdesc,matchtype_tdesc_sub,
          species,
          tgt_id,tgt_id_type,
          sbs_id,sbs_id_type,
          cpd_query,matchtype_qcpd,minsim,
          cname,matchtype_cname_sub,
          mw_min,mw_max,
          cids,scafids,mcesids,
          n_max_targets);

      String errtxt="";
      if (sql==null)
      { Help("ERROR: illegal query; this should not happen; please report."); }
      else
      {
        try { rset=dbcon.executeSql(sql); }
        catch (SQLException e) { System.err.println("PostgreSQL error: "+e.getMessage()); }
      }
      if (verbose>1) System.err.println(sql);
      if (rset!=null)
      {
        try {
          tids=rset2tids(rset);
          rset.getStatement().close();
          System.err.println("targets found: "+tids.size());
        }
        catch (SQLException e) { System.err.println("PostgreSQL error: "+e.getMessage()); }
      }
    }

    //From tids, retreive subnet.
    if (t2pan)
    {
      Help("ERROR: --t2pan not implemented yet.");
    }
    //FileOutputStream fos = new FileOutputStream(new File(ofile),false); //overwrite
    File fout = new File(ofile);
    HashMap<String,Integer> counts=null;
    ArrayList<String> sqls = new ArrayList<String>();
    Integer n_max_a=10000;
    Integer n_max_c=10000;
    try {
      counts=carlsbad_utils.Extract2XGMML(dbcon,
		fout,
		tids,
		sbs_id,sbs_id_type,
		cpd_query, matchtype_qcpd,
		minsim,
		cname, matchtype_cname_sub,
		mw_min, mw_max,
		cids, scafids, mcesids,
		neighbortargets,
		"CARLSBAD Target-Compound SubNetwork",
		n_max_a, n_max_c,
		sqls);
    }
    catch (SQLException e)
    {
      System.err.println("extract2XGMML failed: SQLException: "+e.getMessage());
    }
    catch (Exception e)
    {
      System.err.println("extract2XGMML failed: "+e.getMessage());
    }
    if (globaldegrees)
    {
      HashMap<Integer,HashSet<Integer> > t2c_global = new HashMap<Integer,HashSet<Integer> >();
      for (int tid_this: tids) t2c_global.put(tid_this,null);
      try { int n = carlsbad_utils.Targets2Compounds(t2c_global, dbcon); }
      catch (SQLException e) { System.err.println("targets2Compounds failed: SQLException: "+e.getMessage()); }
      for (int tid_this: tids) System.err.println("\ttid: "+tid_this+" : "+t2c_global.get(tid_this).size()+" compounds");
      if (cids!=null)
      {
        HashMap<Integer,HashSet<Integer> > c2t_global = new HashMap<Integer,HashSet<Integer> >();
        for (int cid_this: cids) c2t_global.put(cid_this,null);
        try { int n = carlsbad_utils.Compounds2Targets(c2t_global,dbcon,human_only); }
        catch (SQLException e) { System.err.println("targets2Targets failed: SQLException: "+e.getMessage()); }
        for (int cid_this: cids)
          System.err.println("\tcid: "+cid_this+" : "+((c2t_global.get(cid_this)!=null)?c2t_global.get(cid_this).size()+" targets":"(does not exist)"));
      }
      if (scafids!=null)
      {
        HashMap<Integer,HashSet<Integer> > s2c_global = new HashMap<Integer,HashSet<Integer> >();
        for (int scafid_this: scafids) s2c_global.put(scafid_this,null);
        try { int n = carlsbad_utils.Scaffolds2Compounds(s2c_global, dbcon); }
        catch (SQLException e) { System.err.println("scaffolds2Compounds failed: SQLException: "+e.getMessage()); }
        for (int scafid_this: scafids) 
          System.err.println("\tscafid: "+scafid_this+" : "+((s2c_global.get(scafid_this)!=null)?s2c_global.get(scafid_this).size()+" compounds":"(does not exist)"));
      }
      if (mcesids!=null)
      {
        HashMap<Integer,HashSet<Integer> > m2c_global = new HashMap<Integer,HashSet<Integer> >();
        for (int mcesid_this: mcesids) m2c_global.put(mcesid_this,null);
        try { int n = carlsbad_utils.Mcess2Compounds(m2c_global, dbcon); }
        catch (SQLException e) { System.err.println("mcess2Compounds failed: SQLException: "+e.getMessage()); }
        for (int mcesid_this: mcesids)
          System.err.println("\tmcesid: "+mcesid_this+" : "+((m2c_global.get(mcesid_this)!=null)?m2c_global.get(mcesid_this).size()+" compounds":"(does not exist)"));
      }
    }
    if (verbose>1)
    {
      if (sqls!=null)
      {
        for (String sql: sqls)
          System.err.println(sql);
      }
      //else { System.err.println("DEBUG: sqls==null."); }
    }
    if (counts!=null)
    {
      System.err.println("compound nodes: "+counts.get("n_node_cpd"));
      System.err.println("target nodes: "+counts.get("n_node_targ"));
      System.err.println("scaffold nodes: "+counts.get("n_node_scaf"));
      System.err.println("total nodes: "+(counts.get("n_node_cpd")+counts.get("n_node_targ")+counts.get("n_node_scaf")));
      System.err.println("activity edges: "+counts.get("n_edge_act"));
      System.err.println("scaffold edges: "+counts.get("n_edge_scaf"));
      System.err.println("total edges: "+(counts.get("n_edge_act")+counts.get("n_edge_scaf")));
      System.err.println("target external IDs: "+counts.get("n_tgt_ext_ids"));
      System.err.println("compound external IDs: "+counts.get("n_cpd_ext_ids"));
      System.err.println("compound synonyms: "+counts.get("n_csynonyms"));
      if (neighbortargets)
      {
        System.err.println("target nodes (neighbor): "+counts.get("n_node_targ_neigh"));
        System.err.println("activity edges (neighbor): "+counts.get("n_edge_act_neigh"));
      }
    }
  }
  private static ArrayList<Integer> rset2tids(ResultSet rset)
	throws SQLException
  {
    ArrayList<Integer> tids = new ArrayList<Integer>();
    while (rset.next()) // tid,name,descr,species,type,[class,]tgt_id_type,tgt_id
    {
      Integer tid=rset.getInt("tid");
      Integer tid_prev=(tids.isEmpty()?null:tids.get(tids.size()-1));

      if (tids.isEmpty() || !(tid.equals(tid_prev)))
      {
        tids.add(tid);
      }
    }
    return tids;
  }
}
