package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;

import org.apache.commons.cli.*; // CommandLine, CommandLineParser, HelpFormatter, OptionBuilder, Options, ParseException, PosixParser
import org.apache.commons.cli.Option.*; // Builder

import edu.unm.health.biocomp.kegg.*;
import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.db.*;
import edu.unm.health.biocomp.util.jre.*; //JREUtils

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
  private static String APPNAME="CARLSBADONE";
  private static String dbhost=null;
  private static Integer dbport=5432;
  private static String dbname=null;
  private static String dbusr=null;
  private static String dbpw=null;
  private static Float scaf_min=0.0f;
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
  public static void main(String [] args) throws Exception
  {
    Options opts = new Options();
    String HELPHEADER = (APPNAME+": Carlsbad one-click subnet extraction application");
    String HELPFOOTER = ("UNM Translational Informatics Division");
    opts.addOption(Option.builder("tid").hasArg().type(Integer.class).desc("query target ID").build());
    opts.addOption(Option.builder("cid").hasArg().type(Integer.class).desc("query compound ID").build());
    opts.addOption(Option.builder("kid").hasArg().desc("query disease (KEGG) ID").build());
    opts.addOption(Option.builder("o").hasArg().argName("CYJSFILE").desc("subnet CYJS for Cytoscape import").build());
    opts.addOption(Option.builder("scaf_min").hasArg().type(Float.class).desc("min scaffold weight [0-1]").build());
    opts.addOption(Option.builder("rgt").desc("output reduced-graph, targets+query").build());
    opts.addOption(Option.builder("rgtp").desc("output reduced-graph, targets+CCPs+query").build());
    opts.addOption(Option.builder("dbhost").hasArg().desc("db host ["+dbhost+"]").build());
    opts.addOption(Option.builder("dbport").hasArg().type(Integer.class).desc("db port ["+dbport+"]").build());
    opts.addOption(Option.builder("dbname").hasArg().desc("db name ["+dbname+"]").build());
    opts.addOption(Option.builder("dbusr").hasArg().desc("db user ["+dbusr+"]").build());
    opts.addOption(Option.builder("dbpw").hasArg().desc("db password").build());
    opts.addOption("v", "verbose", false, "verbose.");
    opts.addOption("vv", "vverbose", false, "very verbose.");
    opts.addOption("h", "help", false, "Show this help.");
    HelpFormatter helper = new HelpFormatter();
    CommandLineParser clp = new PosixParser();
    CommandLine cl = null;
    try {
      cl = clp.parse(opts, args);
    } catch (ParseException e) {
      helper.printHelp(APPNAME, HELPHEADER, opts, e.getMessage(), true);
      System.exit(0);
    }

    if (cl.hasOption("tid")) tid = (Integer)(cl.getParsedOptionValue("tid"));
    if (cl.hasOption("cid")) cid = (Integer)(cl.getParsedOptionValue("cid"));
    if (cl.hasOption("kid")) kid = cl.getOptionValue("kid");
    if (cl.hasOption("o")) ofile = cl.getOptionValue("o");
    if (cl.hasOption("scaf_min")) scaf_min = (Float)(cl.getParsedOptionValue("scaf_min"));
    if (cl.hasOption("rgtp")) rgtp = true;
    if (cl.hasOption("rgt")) rgt = true;
    if (cl.hasOption("dbhost")) dbhost = cl.getOptionValue("dbhost");
    if (cl.hasOption("dbname")) dbname = cl.getOptionValue("dbname");
    if (cl.hasOption("dbusr")) dbusr = cl.getOptionValue("dbusr");
    if (cl.hasOption("dbpw")) dbpw = cl.getOptionValue("dbpw");
    if (cl.hasOption("vv")) verbose = 2;
    else if (cl.hasOption("v")) verbose = 1;
    if (cl.hasOption("h")) {
      helper.printHelp(APPNAME, HELPHEADER, opts, HELPFOOTER, true);
      System.exit(0);
    }

    if (verbose>0)
    {
      System.err.println("JRE_VERSION: "+JREUtils.JREVersion());
      System.err.println("JChem version: "+com.chemaxon.version.VersionInfo.getVersion());
    }

    java.util.Date t_0 = new java.util.Date();

    DBCon dbcon = null;

    try { dbcon = new DBCon("postgres", dbhost, dbport, dbname, dbusr, dbpw); }
    catch (Exception e) {
      helper.printHelp(APPNAME, HELPHEADER, opts, e.getMessage(), true);
    }

    File fout = (ofile!=null) ? (new File(ofile)) : null;
    File fout_cpd = new File("/tmp/carlsbad.sdf");

    //Initialize lists:
    TargetList TARGETLIST = new TargetList();
    if (verbose>0) System.err.println("Loading targetlist...");
    try { TARGETLIST.loadAll(dbcon); }
    catch (Exception e) {
      helper.printHelp(APPNAME, HELPHEADER, opts, ("ERROR: problem reading targetlist: "+e.getMessage()), true);
}
    System.err.println("targetlist count: "+TARGETLIST.size());

    CompoundList DRUGLIST = new CompoundList();
    if (verbose>0) System.err.println("Loading druglist...");
    try { DRUGLIST.loadAllDrugs(dbcon); }
    catch (Exception e) {
      helper.printHelp(APPNAME, HELPHEADER, opts, ("ERROR: problem reading druglist: "+e.getMessage()), true);
}
    System.err.println("druglist count: "+DRUGLIST.size());

    DiseaseList DISEASELIST = new DiseaseList();
    if (verbose>0) System.err.println("Loading diseaselist...");
    try { DISEASELIST.loadAll(dbcon); }
    catch (Exception e) {
      helper.printHelp(APPNAME, HELPHEADER, opts, ("ERROR: problem reading diseaselist: "+e.getMessage()), true);
    }
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
      {
        helper.printHelp(APPNAME, HELPHEADER, opts, ("-tid, -cid or -kid required."), true);
      }

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
