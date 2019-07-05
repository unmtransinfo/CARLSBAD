package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;

import edu.unm.health.biocomp.cytoscape.*;
import edu.unm.health.biocomp.kegg.*;
import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.db.*;

/**	CARLSBAD "one-click" subnet query app.
	The general use case is to provide interesting subnets based on simple
	queries. (1) single protein target chosen from a browsable list.
	(2) single drug compound chosen from a browsable list.
	By employing CCPs such subnets are uniquely available via CARLSBAD.

	To do:  Any unknowns found?  Or are all targets|compounds already
	associated to the query without CCPs (T-C-T or C-T-C).

	@author Jeremy J Yang
*/
public class carlsbadone_app
{
  /////////////////////////////////////////////////////////////////////////////
  private static String DBHOST=null;
  private static String DBNAME=null;
  private static String DBUSR=null;
  private static String DBPW=null;
  private static Float scaf_min=0.0f;

  /////////////////////////////////////////////////////////////////////////////
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"carlsbadone_app - Carlsbad one-click subnet extraction application\n"
      +"usage: carlsbadone_app [options]\n"
      +"  required:\n"
      +"    -tid ID ................ query target ID (int)\n"
      +"   or\n"
      +"    -cid ID ................ query compound ID (int)\n"
      +"   or\n"
      +"    -kid ID ................ query disease (KEGG) ID\n"
      +"   and\n"
      +"    -o CYJSFILE ............ subnet CYJS for Cytoscape import\n"
      +"  options:\n"
      +"    -dbhost DBHOST ......... ["+DBHOST+"]\n"
      +"    -dbname DBNAME ......... ["+DBNAME+"] \n"
      +"    -dbusr DBUSR ........... ["+DBUSR+"] \n"
      +"    -dbpw DBPW ............. \n"
      +"    -rgt ................... output reduced-graph, targets+query\n"
      +"    -rgtp .................. output reduced-graph, targets+CCPs+query\n"
      +"    -scaf_min SCAFMIN ...... min scaffold weight [0-1] ("+String.format("%.1f%%",scaf_min)+")\n"
      +"    -v ..................... verbose\n"
      +"    -vv .................... very verbose\n"
      +"    -h ..................... this help\n");
    System.exit(1);
  }
  private static int verbose=0;
  private static String ofile=null;
  private static String ofile_rgt=null;
  private static String ofile_rgtp=null;
  private static String ofile_cpd=null;
  private static Integer tid=null;
  private static Integer cid=null;
  private static String kid=null;
  private static Boolean rgt=false;
  private static Boolean rgtp=false;
  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    if (args.length==0) Help("");
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-tid")) tid=Integer.parseInt(args[++i]);
      else if (args[i].equals("-cid")) cid=Integer.parseInt(args[++i]);
      else if (args[i].equals("-kid")) kid=args[++i];
      else if (args[i].equals("-scaf_min")) scaf_min=Float.parseFloat(args[++i]);
      else if (args[i].equals("-dbhost")) DBHOST=args[++i];
      else if (args[i].equals("-dbname")) DBNAME=args[++i];
      else if (args[i].equals("-dbusr")) DBUSR=args[++i];
      else if (args[i].equals("-dbpw")) DBPW=args[++i];
      else if (args[i].equals("-rgt")) rgt=true;
      else if (args[i].equals("-rgtp")) rgtp=true;
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-vv")) verbose=2;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void main(String [] args)
  {
    ParseCommand(args);

    java.util.Date t_0 = new java.util.Date();

    DBCon dbcon = null;

    try {
      dbcon = new DBCon("postgres",DBHOST,5432,DBNAME,DBUSR,DBPW);
    }
    catch (SQLException e) { Help("Connection failed:"+e.getMessage()); }
    catch (Exception e) { Help("Connection failed:"+e.getMessage()); }

    File fout = (ofile!=null) ? (new File(ofile)) : null;
    File fout_cpd = new File("/tmp/carlsbad.sdf");

    //Initialize lists:
    TargetList TARGETLIST = new TargetList();
    if (verbose>0) System.err.println("Loading targetlist...");
    try { TARGETLIST.loadAll(dbcon); }
    catch (Exception e) { Help("ERROR: problem reading targetlist: "+e.getMessage()); }
    System.err.println("targetlist count: "+TARGETLIST.size());

    CompoundList DRUGLIST = new CompoundList();
    if (verbose>0) System.err.println("Loading druglist...");
    try { DRUGLIST.loadAllDrugs(dbcon); }
    catch (Exception e) { Help("ERROR: problem reading druglist: "+e.getMessage()); }
    System.err.println("druglist count: "+DRUGLIST.size());

    DiseaseList DISEASELIST = new DiseaseList();
    if (verbose>0) System.err.println("Loading diseaselist...");
    try { DISEASELIST.loadAll(dbcon); }
    catch (Exception e) { Help("ERROR: problem reading diseaselist: "+e.getMessage()); }
    System.err.println("diseaselist count: "+DISEASELIST.size());

    HashMap<String,Integer> counts=null;
    ArrayList<String> sqls = new ArrayList<String>(); //return arg
    ArrayList<Integer> tids = new ArrayList<Integer>(); //return arg
    CompoundList cpdlist = new CompoundList(); //return arg
    CCPList ccplist = new CCPList(); //return arg

    Integer n_max_a=10000;
    Integer n_max_c=10000;
    String kgtype = rgt ? "rgt" : (rgtp ? "rgtp" : "full");
    try
    {
      if (tid!=null)
      {
        if (TARGETLIST.containsKey(tid))
          System.err.println("query target: ("+tid+") "+TARGETLIST.get(tid).getName());
        if (kgtype=="rgt")
          counts=carlsbadone_utils.Target2Network(dbcon, fout, null, null, fout_cpd, tid, scaf_min, "CARLSBAD Target2Network one-click Subnet", n_max_a, n_max_c, cpdlist, ccplist, sqls);
        else if (kgtype=="rgtp")
          counts=carlsbadone_utils.Target2Network(dbcon, null, fout, null, fout_cpd, tid, scaf_min, "CARLSBAD Target2Network one-click Subnet", n_max_a, n_max_c, cpdlist, ccplist, sqls);
        else 
          counts=carlsbadone_utils.Target2Network(dbcon, null, null, fout, fout_cpd, tid, scaf_min, "CARLSBAD Target2Network one-click Subnet", n_max_a, n_max_c, cpdlist, ccplist, sqls);
        tids.add(tid);
      }
      else if (cid!=null)
      {
        if (DRUGLIST.containsKey(cid))
          System.err.println("query drug: ("+cid+") "+DRUGLIST.get(cid).getName());
        if (kgtype=="rgt")
          counts=carlsbadone_utils.Compound2Network(dbcon, fout, null, null, fout_cpd, cid, scaf_min, "CARLSBAD Compound2Network one-click Subnet", n_max_a, n_max_c, tids, cpdlist, ccplist, sqls);
        else if (kgtype=="rgtp")
          counts=carlsbadone_utils.Compound2Network(dbcon, null, fout, null, fout_cpd, cid, scaf_min, "CARLSBAD Compound2Network one-click Subnet", n_max_a, n_max_c, tids, cpdlist, ccplist, sqls);
        else
          counts=carlsbadone_utils.Compound2Network(dbcon, null, null, fout, fout_cpd, cid, scaf_min, "CARLSBAD Compound2Network one-click Subnet", n_max_a, n_max_c, tids, cpdlist, ccplist, sqls);
      }
      else if (kid!=null)
      {
        if (DISEASELIST.containsKey(kid))
          System.err.println("query disease: ("+kid+") "+DISEASELIST.get(kid).getName());
        if (kgtype=="rgt")
          counts = carlsbadone_utils.Disease2Network(dbcon, fout, null, null, fout_cpd, kid, scaf_min, "CARLSBAD Disease2Network one-click Subnet", n_max_a, n_max_c, tids, cpdlist, ccplist, sqls);
        else if (kgtype=="rgtp")
          counts = carlsbadone_utils.Disease2Network(dbcon, null, fout, null, fout_cpd, kid, scaf_min, "CARLSBAD Disease2Network one-click Subnet", n_max_a, n_max_c, tids, cpdlist, ccplist, sqls);
        else
          counts = carlsbadone_utils.Disease2Network(dbcon, null, null, fout, fout_cpd, kid, scaf_min, "CARLSBAD Disease2Network one-click Subnet", n_max_a, n_max_c, tids, cpdlist, ccplist, sqls);
      }
      else
      { Help("-tid, -cid or -kid required."); }

      for (String k: counts.keySet())
      {
        System.err.print(k+": "+counts.get(k));
        if (  (k.equals("n_node_cpd") && counts.get(k)==n_max_c)
            ||(k.equals("n_edge_act") && counts.get(k)==n_max_a))
             System.err.print(" (MAX)");
        System.err.println("");
      }

    }
    catch (SQLException e)
    {
      //System.err.println("ERROR: SQLException: "+e.getMessage()+"\nsql:"+sqls.get(sqls.size()-1));
      System.err.println("ERROR: SQLException: "+e.getMessage());
    }
    catch (Exception e)
    {
      System.err.println("ERROR: "+e.getMessage());
      e.printStackTrace();
    }

    TargetList tgtlist=TARGETLIST.selectByIDs(new HashSet<Integer>(tids));
    //tid!=null if target-query; cid!=null if drug-query; kid!=null if disease-query
    webapp_utils.FlagTargetsEmpirical(tgtlist, DRUGLIST, tid, cid, DISEASELIST, kid);
    webapp_utils.FlagCompoundsEmpirical(cpdlist, TARGETLIST, DISEASELIST, kid); //kid!=null if disease-query
    webapp_utils.FlagCompoundsEmpirical(
	cpdlist,
	tgtlist, //hitlist
	tid, //tid!=null if target-query
	cid); //cid!=null if drug-query

    //ArrayList<String> countkeys = new ArrayList<String>(counts.keySet());
    //Collections.sort(countkeys);
    //for (String countkey: countkeys) System.err.println(countkey+": "+counts.get(countkey));

    System.err.println("elapsed time: "+time_utils.TimeDeltaStr(t_0, new java.util.Date()));
  }
}
