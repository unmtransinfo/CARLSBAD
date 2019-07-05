package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;

import chemaxon.struc.*;
import chemaxon.formats.*;

import com.fasterxml.jackson.core.*; //JsonFactory, JsonGenerator
import com.fasterxml.jackson.databind.*; //ObjectMapper, JsonNode

import edu.unm.health.biocomp.util.http.*;
import edu.unm.health.biocomp.util.db.*;
import edu.unm.health.biocomp.kegg.*;

/**	Static methods for CARLSBAD "one-click" subnet queries.
	The general use case is to provide interesting subnets based on simple
	queries. (1) single protein target chosen from a browsable list.
	(2) single drug compound chosen from a browsable list.
	By employing CCPs such subnets are uniquely available via CARLSBAD.

	To do:  Any unknowns found?  Or are all targets|compounds already
	associated to the query without CCPs (T-C-T or C-T-C).

	@author Jeremy J Yang
*/
public class carlsbadone_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**	Given a single target, extract CCP-associated network.
	Comprised of:
	  - active compounds
	  - associated common chemical patterns (CCPs)
	  - CCP-associated compounds

	@param dbcon    database connection
	@param fout     output file
	@param fout_cpd     output file, compounds, normally SDF
	@param tid_query	query target ID
	@param scaf_min   scaf association threshold [0-1]
	@param title    title of subnet
	@param n_max_a  max number of activities
	@param n_max_c  max number of compounds
	@param sqls     return param: SQLs used
  */
  public static HashMap<String,Integer> Target2Network(
	DBCon dbcon,
	File fout_rgt,
	File fout_rgtp,
	File fout,
	File fout_cpd,
	Integer tid_query,
	Float scaf_min,
	String title,
	Integer n_max_a,Integer n_max_c,
	CompoundList cpdlist,	//return value
	CCPList ccplist,	//return value
	ArrayList<String> sqls)	//return value
	throws Exception
  {
    Boolean human_filter=true;
    HashMap<String,Integer> counts = new HashMap<String,Integer>(); //for return written entity counts
    PrintWriter fout_writer = (fout!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout,false)))) : null;
    PrintWriter fout_rgtp_writer = (fout_rgtp!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout_rgtp,false)))) : null;
    PrintWriter fout_rgt_writer = (fout_rgt!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout_rgt,false)))) : null;

    HashMap<String, Object> elements = new HashMap<String, Object>();
    ArrayList<HashMap<String, Object> > nodes = new ArrayList<HashMap<String, Object> >();
    ArrayList<HashMap<String, Object> > edges = new ArrayList<HashMap<String, Object> >();
    elements.put("nodes", nodes);
    elements.put("edges", edges);

    if (tid_query==null)
      throw new Exception("Target ID must be specified.");
    if (title==null || title.isEmpty()) title="CARLSBAD Subnet";

    ArrayList<Integer> tids = new ArrayList<Integer>(Arrays.asList(tid_query)); //one TID only
    ArrayList<String> wheres = new ArrayList<String>();
  
    HashMap<String,Object> root = new HashMap<String,Object>();
    root.put("format_version", "1.0");
    root.put("generated_by", "carlsbadone-0.0.1-SNAPSHOT");
    root.put("target_cytoscapejs_version", "~2.1");
    HashMap<String,Object> data = new HashMap<String,Object>();
    data.put("name", title);
    data.put("shared_name", title);
    data.put("SUID", new Integer(52)); //integer
    data.put("selected", new Boolean(true)); //boolean
    root.put("data", data);

    /// Targets (NB: wheres kept):
    String sql=carlsbad_utils.TargetIDs2SQL(tids,wheres);
    sqls.add(sql);
    ResultSet rset=dbcon.executeSql(sql);
    HashMap<Integer,HashMap<String,String> > tgtdata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,HashMap<String,Boolean> > > tgt_tgt_ids = new HashMap<Integer,HashMap<String,HashMap<String,Boolean> > >();
    carlsbad_utils.ReadTargetData(rset,tgtdata,tgt_tgt_ids);
    HashMap<Integer,HashSet<Integer> > t2c_global = new HashMap<Integer,HashSet<Integer> >();
    for (int tid: tgtdata.keySet()) t2c_global.put(tid,null);
    carlsbad_utils.Targets2Compounds(t2c_global,dbcon); //Get global-degree data (to all compounds, not just subnet).
    if (fout!=null)
      carlsbad_utils.WriteTargets2Elements(tgtdata, t2c_global, tgt_tgt_ids, counts, elements);

    /// Activities and Compounds (NB: wheres kept):
    sql=carlsbad_utils.ActivityCompoundsSQL(null,null,null,null,null,null,null,null,null,null,null,null,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);

    /// Populating HashMaps with activity and compound data.
    HashMap<Integer,HashMap<String,String> > actdata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,String> > cpddata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,Boolean> > cpdsynonyms = new HashMap<Integer,HashMap<String,Boolean> >();
    HashMap<Integer,HashMap<String,HashSet<String> > > cpd_sbs_ids = new HashMap<Integer,HashMap<String,HashSet<String> > >();
    carlsbad_utils.ReadCompoundData(rset,actdata,cpddata,cpdsynonyms,cpd_sbs_ids);
    HashMap<Integer,HashSet<Integer> > c2t_global = new HashMap<Integer,HashSet<Integer> >();
    for (int cid: cpddata.keySet()) c2t_global.put(cid,null);
    carlsbad_utils.Compounds2Targets(c2t_global,dbcon,human_filter); //Get global-degree data.

    /// Scaffolds (NB: wheres kept):
    sql=carlsbad_utils.ScaffoldSQL(null,null,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);
    HashMap<String,HashMap<String,String> > scafdata = new HashMap<String,HashMap<String,String> >();
    HashSet<Integer> scafids_read = new HashSet<Integer>();
    carlsbad_utils.ReadScaffoldData(rset,cpddata,scafdata,scafids_read);
    HashMap<Integer,HashSet<Integer> > s2c_global = new HashMap<Integer,HashSet<Integer> >();
    HashMap<Integer,HashSet<Integer> > s2t_global = new HashMap<Integer,HashSet<Integer> >();
    for (int scafid: scafids_read) s2c_global.put(scafid,null);
    for (int scafid: scafids_read) s2t_global.put(scafid,null);
    carlsbad_utils.Scaffolds2Compounds(s2c_global,dbcon); //Get global-degree data.
    carlsbad_utils.Scaffolds2Targets(s2t_global,dbcon); //Get global-degree data.

    /// MCESs (NB: wheres kept):
    sql=carlsbad_utils.McesSQL(null,null,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);
    HashSet<Integer> mcesids_read = new HashSet<Integer>();
    HashMap<String,HashMap<String,String> > mcesdata = new HashMap<String,HashMap<String,String> >();
    carlsbad_utils.ReadMcesData(rset,cpddata,mcesdata,mcesids_read);
    HashMap<Integer,HashSet<Integer> > m2c_global = new HashMap<Integer,HashSet<Integer> >();
    HashMap<Integer,HashSet<Integer> > m2t_global = new HashMap<Integer,HashSet<Integer> >();
    for (int mcesid: mcesids_read) m2c_global.put(mcesid,null);
    for (int mcesid: mcesids_read) m2t_global.put(mcesid,null);
    carlsbad_utils.Mcess2Compounds(m2c_global,dbcon);
    carlsbad_utils.Mcess2Targets(m2t_global,dbcon);

    /// Apply scaf_min filter to scafdata.
    int n_edge_removed = carlsbad_utils.Filter_By_S2C_Weight(scafdata,scaf_min);
    counts.put("n_edge_removed",n_edge_removed);

    /// Get other compounds associated by Scaffolds; IDs in s2c_global.
    /// Get other compounds associated by MCESs; IDs in m2c_global.

    HashSet<Integer> cid_global = new HashSet<Integer>();
    for (int scafid: s2c_global.keySet()) { for (int cid: s2c_global.get(scafid)) cid_global.add(cid); }
    for (int mcesid: m2c_global.keySet()) { for (int cid: m2c_global.get(mcesid)) cid_global.add(cid); }
    wheres.clear();
    if (cid_global.size()>0)
    {
      ArrayList<Integer> cid_other = new ArrayList<Integer>(cid_global);
      sql=carlsbad_utils.CompoundIDs2SQL(cid_other,wheres);
      sqls.add(sql);
      rset=dbcon.executeSql(sql);
      int n_cpd_neighbor = carlsbad_utils.ReadCompoundData(rset,null,cpddata,cpdsynonyms,cpd_sbs_ids);
      counts.put("n_cpd_neighbor",n_cpd_neighbor);

      //Adds new edges to scafdata
      ArrayList<Integer> scafids = new ArrayList<Integer>(s2c_global.keySet());
      if (scafids.size()>0)
      {
        sql=carlsbad_utils.ScafedgeSQL(cid_other,scafids);
        rset=dbcon.executeSql(sql);
        carlsbad_utils.ReadScaffoldData(rset,cpddata,scafdata,scafids_read);
      }

      //Adds new edges to mcesdata
      ArrayList<Integer> mcesids = new ArrayList<Integer>(m2c_global.keySet());
      if (mcesids.size()>0)
      {
        sql=carlsbad_utils.McesedgeSQL(cid_other,mcesids);
        rset=dbcon.executeSql(sql);
        carlsbad_utils.ReadMcesData(rset,cpddata,mcesdata,mcesids_read);
      }
      //Populate cpdlist here.
      cpdlist.load(new HashSet<Integer>(cid_global));

      //Populate ccplist here (scaffolds).
      ccplist.loadScaffolds(new HashSet<Integer>(scafids));

      //Populate ccplist here (mcess).
      ccplist.loadMCESs(new HashSet<Integer>(mcesids));
    }
    //else { System.err.println("DEBUG: No CCP-associated compounds found."); }

    ObjectMapper mapper = new ObjectMapper();
    JsonFactory jsf = mapper.getFactory();

    if (fout_rgt!=null) // Write targets-only reduced-graph to CYJS.  
    {
      carlsbad_utils.WriteReducedGraph2Elements(tgtdata,
	null,	//disease n.a.
	null,	//cid_query n.a.
	t2c_global, s2t_global, s2c_global, m2t_global, m2c_global, tgt_tgt_ids,
	actdata, cpddata,
	null,	//cpdsynonyms not needed?
	null,	//cpd_sbs_ids not needed?
	scafdata, mcesdata,
	false,	//include_ccps?
	counts, elements);

      root.put("elements", elements);
      JsonGenerator jsg = jsf.createGenerator(fout_rgt_writer);
      jsg.useDefaultPrettyPrinter();
      jsg.writeObject(root);
      jsg.close();
    }
    if (fout_rgtp!=null) // Write targets+CCPs reduced-graph to CYJS.  
    {
      carlsbad_utils.WriteReducedGraph2Elements(tgtdata,
	null,	//disease n.a.
	null,	//cid_query n.a.
	t2c_global, s2t_global, s2c_global, m2t_global, m2c_global, tgt_tgt_ids,
	actdata, cpddata,
	null,	//cpdsynonyms not needed?
	null,	//cpd_sbs_ids not needed?
	scafdata, mcesdata,
	true,	//include_ccps?
	counts, elements);

      root.put("elements", elements);
      JsonGenerator jsg = jsf.createGenerator(fout_rgtp_writer);
      jsg.useDefaultPrettyPrinter();
      jsg.writeObject(root);
      jsg.close();
    }
    if (fout!=null) // Write full graph to CYJS.  
    {
      carlsbad_utils.WriteCompounds2Elements(cpddata, c2t_global, cpd_sbs_ids, actdata, cpdsynonyms, n_max_c, n_max_a, counts, elements);
      HashSet<Integer> scafids_written = new HashSet<Integer>();
      carlsbad_utils.WriteScaffolds2Elements(scafdata, scafids_written, s2c_global, s2t_global, counts, elements);
      HashSet<Integer> mcesids_written = new HashSet<Integer>();
      carlsbad_utils.WriteMcess2Elements(mcesdata, mcesids_written, m2c_global, m2t_global, counts, elements);

      root.put("elements", elements);
      JsonGenerator jsg = jsf.createGenerator(fout_writer);
      jsg.useDefaultPrettyPrinter();
      jsg.writeObject(root);
      jsg.close();
    }

    // Write cpddata compounds to SDF for display and/or download.
    if (fout_cpd!=null) carlsbad_utils.WriteCompounds2SDF(cpdlist, fout_cpd);

    return counts;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Given a single compound, very likely a drug or compound of particular
	biological interest and for which significant bioactivity data exists,
	extract CCP-associated network.
	Comprised of:
	  - targets for which compound is active (knowns)
	  - associated common chemical patterns (CCPs)
	  - CCP-associated compounds
	  - targets for which CCP-associated compounds are active (hypothesis targets)

	Problem: dangling edges... For Ketoralac, "Edge 24 references unknown node: C102306"

	@param dbcon    database connection
	@param cid_query	query compound ID
	@param fout     output file
	@param fout_cpd     output file, compounds, normally SDF
	@param scaf_min   scaf association threshold [0-1]
	@param title    title of subnet
	@param n_max_a  max number of activities
	@param n_max_c  max number of compounds
	@param sqls     return param: SQLs used
  */
  public static HashMap<String,Integer> Compound2Network(
	DBCon dbcon,
	File fout_rgt,
	File fout_rgtp,
	File fout,
	File fout_cpd,
	Integer cid_query,
	Float scaf_min,
	String title,
	Integer n_max_a,Integer n_max_c,
	ArrayList<Integer> tids,	//return value
	CompoundList cpdlist,	//return value
	CCPList ccplist,	//return value
	ArrayList<String> sqls)	//return value
	throws Exception
  {
    Boolean human_filter=true;
    HashMap<String,Integer> counts = new HashMap<String,Integer>(); //for return counts
    PrintWriter fout_writer = (fout!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout, false)))) : null;
    PrintWriter fout_rgtp_writer = (fout_rgtp!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout_rgtp, false)))) : null;
    PrintWriter fout_rgt_writer = (fout_rgt!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout_rgt, false)))) : null;

    if (cid_query==null)
      throw new Exception("Compound ID must be specified.");
    if (title==null || title.isEmpty()) title="CARLSBAD Subnet";

    HashMap<String,Object> root = new HashMap<String,Object>();
    root.put("format_version", "1.0");
    root.put("generated_by", "carlsbadone-0.0.1-SNAPSHOT");
    root.put("target_cytoscapejs_version", "~2.1");
    HashMap<String,Object> data = new HashMap<String,Object>();
    data.put("name", title);
    data.put("shared_name", title);
    data.put("SUID", new Integer(52)); //integer
    data.put("selected", new Boolean(true)); //boolean
    root.put("data", data);

    ArrayList<Integer> cids_query = new ArrayList<Integer>(Arrays.asList(cid_query));
    ArrayList<String> wheres = new ArrayList<String>();
  
    if (human_filter)
      wheres.add("target.species='human'");

    /// Compounds (wheres kept):
    String sql=carlsbad_utils.Compounds2TargetsSQL(cids_query,wheres,human_filter);
    sqls.add(sql);
    
    ResultSet rset=dbcon.executeSql(sql);
    HashMap<Integer,HashMap<String,String> > tgtdata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,HashMap<String,Boolean> > > tgt_tgt_ids = new HashMap<Integer,HashMap<String,HashMap<String,Boolean> > >();
    carlsbad_utils.ReadTargetData(rset,tgtdata,tgt_tgt_ids);
    HashMap<Integer,HashSet<Integer> > t2c_global = new HashMap<Integer,HashSet<Integer> >();
    for (int tid: tgtdata.keySet()) t2c_global.put(tid,null);
    carlsbad_utils.Targets2Compounds(t2c_global,dbcon); //Get global-degree data (to all compounds, not just subnet).

    /// Activities and Compounds (wheres kept):
    sql=carlsbad_utils.ActivityCompoundsSQL(null,null,null,null,null,null,null,null,null,null,null,null,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);

    /// Populating HashMaps with activity and compound data.
    HashMap<Integer,HashMap<String,String> > actdata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,String> > cpddata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,Boolean> > cpdsynonyms = new HashMap<Integer,HashMap<String,Boolean> >();
    HashMap<Integer,HashMap<String,HashSet<String> > > cpd_sbs_ids = new HashMap<Integer,HashMap<String,HashSet<String> > >();
    carlsbad_utils.ReadCompoundData(rset,actdata,cpddata,cpdsynonyms,cpd_sbs_ids);

    HashMap<Integer,HashSet<Integer> > c2t_global = new HashMap<Integer,HashSet<Integer> >();
    for (int cid: cpddata.keySet()) c2t_global.put(cid,null);
    carlsbad_utils.Compounds2Targets(c2t_global,dbcon,human_filter); //Get global-degree data.

    /// Scaffolds (wheres kept):
    sql=carlsbad_utils.ScaffoldSQL(null,null,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);
    HashMap<String,HashMap<String,String> > scafdata = new HashMap<String,HashMap<String,String> >();
    HashSet<Integer> scafids_read = new HashSet<Integer>();
    carlsbad_utils.ReadScaffoldData(rset,cpddata,scafdata,scafids_read);
    HashMap<Integer,HashSet<Integer> > s2c_global = new HashMap<Integer,HashSet<Integer> >();
    HashMap<Integer,HashSet<Integer> > s2t_global = new HashMap<Integer,HashSet<Integer> >();
    for (int scafid: scafids_read) s2c_global.put(scafid,null);
    for (int scafid: scafids_read) s2t_global.put(scafid,null);
    carlsbad_utils.Scaffolds2Compounds(s2c_global,dbcon); //Get global-degree data.
    carlsbad_utils.Scaffolds2Targets(s2t_global,dbcon); //Get global-degree data.

    /// MCESs (NB: wheres kept):
    sql=carlsbad_utils.McesSQL(null,null,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);
    HashSet<Integer> mcesids_read = new HashSet<Integer>();
    HashMap<String,HashMap<String,String> > mcesdata = new HashMap<String,HashMap<String,String> >();
    carlsbad_utils.ReadMcesData(rset,cpddata,mcesdata,mcesids_read);
    HashMap<Integer,HashSet<Integer> > m2c_global = new HashMap<Integer,HashSet<Integer> >();
    HashMap<Integer,HashSet<Integer> > m2t_global = new HashMap<Integer,HashSet<Integer> >();
    for (int mcesid: mcesids_read) m2c_global.put(mcesid,null);
    for (int mcesid: mcesids_read) m2t_global.put(mcesid,null);
    carlsbad_utils.Mcess2Compounds(m2c_global,dbcon);
    carlsbad_utils.Mcess2Targets(m2t_global,dbcon);

    /// Get other compounds associated by Scaffolds; IDs in s2c_global.

    /// Get other compounds associated by MCESs; IDs in m2c_global.

    /// Apply scaf_min filter to scafdata.
    int n_edge_removed = carlsbad_utils.Filter_By_S2C_Weight(scafdata,scaf_min);
    counts.put("n_edge_removed", n_edge_removed);

    /// Find "other" compounds via CCPs:
    HashSet<Integer> cid_global = new HashSet<Integer>();
    for (int scafid: s2c_global.keySet())
    { for (int cid: s2c_global.get(scafid)) cid_global.add(cid); }
    for (int mcesid: m2c_global.keySet())
    { for (int cid: m2c_global.get(mcesid)) cid_global.add(cid); }
    wheres.clear();
    if (cid_global.size()>0)
    {
      ArrayList<Integer> cid_other = new ArrayList<Integer>(cid_global);
      sql=carlsbad_utils.CompoundIDs2SQL(cid_other,wheres);
      sqls.add(sql);
      rset=dbcon.executeSql(sql);
      int n_cpd_neighbor = carlsbad_utils.ReadCompoundData(rset,null,cpddata,cpdsynonyms,cpd_sbs_ids);
      counts.put("n_cpd_neighbor", n_cpd_neighbor);

      //Adds new edges to scafdata
      ArrayList<Integer> scafids = new ArrayList<Integer>(s2c_global.keySet());
      if (scafids.size()>0)
      {
        sql=carlsbad_utils.ScafedgeSQL(cid_other,scafids);
        rset=dbcon.executeSql(sql);
        carlsbad_utils.ReadScaffoldData(rset,cpddata,scafdata,scafids_read);
      }

      //Adds new edges to mcesdata
      ArrayList<Integer> mcesids = new ArrayList<Integer>(m2c_global.keySet());
      if (mcesids.size()>0)
      {
        sql=carlsbad_utils.McesedgeSQL(cid_other,mcesids);
        rset=dbcon.executeSql(sql);
        carlsbad_utils.ReadMcesData(rset,cpddata,mcesdata,mcesids_read);
      }

      /// Find "other" targets for "other" compounds (subnet terminal nodes):
      sql=carlsbad_utils.Compounds2TargetsSQL(cid_other,wheres,human_filter);
      sqls.add(sql);
      rset=dbcon.executeSql(sql);
      carlsbad_utils.ReadTargetData(rset,tgtdata,tgt_tgt_ids);
      for (int tid: tgtdata.keySet()) t2c_global.put(tid,null);
      carlsbad_utils.Targets2Compounds(t2c_global,dbcon); //Get global-degrees

      /// Adds new edges for existing compounds.  "wheres" propagate TIDs/CIDs.
      sql=carlsbad_utils.ActivityCompoundsSQL(null,null,null,null,null,null,null,null,null,null,null,null,wheres);
      sqls.add(sql);

      rset=dbcon.executeSql(sql);
      carlsbad_utils.ReadCompoundData(rset,actdata,cpddata,cpdsynonyms,cpd_sbs_ids);

      // Problem: Scafs and cpds removed by Filter_By_S2C_Weight() still in cid_global;
      // so may reappear and result in orphaned targets.
      carlsbad_utils.Remove_Orphan_Targets(tgtdata,actdata);
      // Problem: CCp-associated cpds may not be active, hence not interesting in this context.
      // But do not remove cpds also needed by mcesdata!
      carlsbad_utils.Remove_Orphan_Compounds(cpddata,scafdata,mcesdata,actdata);

      // Problem: Removed cpds result in dangling edges.
      carlsbad_utils.Remove_Dangling_Edges(scafdata,cpddata);

      //Populate cpdlist here.
      cpdlist.load(new HashSet<Integer>(cid_global));

      //Populate ccplist here (scaffolds).
      ccplist.loadScaffolds(new HashSet<Integer>(scafids));

      //Populate ccplist here (mcess).
      ccplist.loadMCESs(new HashSet<Integer>(mcesids));
    }
    //else { System.err.println("DEBUG: No CCP-associated compounds found."); }

    tids.addAll(tgtdata.keySet());

    HashMap<String, Object> elements = new HashMap<String, Object>();
    ObjectMapper mapper = new ObjectMapper();
    JsonFactory jsf = mapper.getFactory();

    if (fout_rgt!=null) // Write targets-only reduced-graph to CYJS.  
    {
      carlsbad_utils.WriteReducedGraph2Elements(tgtdata,
	null,	//disease n/a
	cid_query,
	t2c_global, s2t_global, s2c_global, m2t_global, m2c_global, tgt_tgt_ids,
	actdata, cpddata,
	cpdsynonyms,	//for cid_query only
	cpd_sbs_ids,	//for cid_query only
	scafdata, mcesdata,
	false,	//include_ccps?
	counts, elements);

      root.put("elements", elements);
      JsonGenerator jsg = jsf.createGenerator(fout_rgt_writer);
      jsg.useDefaultPrettyPrinter();
      jsg.writeObject(root);
      jsg.close();
    }
    if (fout_rgtp!=null) // Write targets+CCPs reduced-graph to CYJS.  
    {
      elements.clear();
      carlsbad_utils.WriteReducedGraph2Elements(tgtdata,
	null,	//disease n/a
	cid_query,
	t2c_global, s2t_global, s2c_global, m2t_global, m2c_global, tgt_tgt_ids,
	actdata, cpddata,
	cpdsynonyms,	//for cid_query only
	cpd_sbs_ids,	//for cid_query only
	scafdata, mcesdata,
	true,	//include_ccps?
	counts, elements);

      root.put("elements", elements);
      JsonGenerator jsg = jsf.createGenerator(fout_rgtp_writer);
      jsg.useDefaultPrettyPrinter();
      jsg.writeObject(root);
      jsg.close();
    }
    if (fout!=null) // Write full graph to CYJS.  
    {
      ArrayList<HashMap<String, Object> > nodes = new ArrayList<HashMap<String, Object> >();
      ArrayList<HashMap<String, Object> > edges = new ArrayList<HashMap<String, Object> >();
      elements.put("nodes", nodes);
      elements.put("edges", edges);

      carlsbad_utils.WriteTargets2Elements(tgtdata, t2c_global, tgt_tgt_ids, counts, elements);
      carlsbad_utils.WriteCompounds2Elements(cpddata, c2t_global, cpd_sbs_ids, actdata, cpdsynonyms, n_max_c, n_max_a, counts, elements);
      HashSet<Integer> scafids_written = new HashSet<Integer>();
      carlsbad_utils.WriteScaffolds2Elements(scafdata, scafids_written, s2c_global, s2t_global, counts, elements);
      HashSet<Integer> mcesids_written = new HashSet<Integer>();
      carlsbad_utils.WriteMcess2Elements(mcesdata, mcesids_written, m2c_global, m2t_global, counts, elements);

      root.put("elements", elements);
      JsonGenerator jsg = jsf.createGenerator(fout_writer);
      jsg.useDefaultPrettyPrinter();
      jsg.writeObject(root);
      jsg.close();
    }

    // Write cpddata compounds to SDF for display and/or download.
    if (fout_cpd!=null) carlsbad_utils.WriteCompounds2SDF(cpdlist,fout_cpd);

    return counts;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Given a single disease (KEGG), extract CCP-associated network.
	Derived from:
	  - KEGG genes linked to the disease
	  - NCBI_GIs for the KEGG genes
	  - targets in CARLSBAD linked by NCBI_GIs.
	  - active compounds
	  - associated common chemical patterns (CCPs)
	  - CCP-associated compounds

	@param dbcon    database connection
	@param kid_query	query disease KEGG ID
	@param fout     output file
	@param fout_cpd     output file, compounds, normally SDF
	@param scaf_min   scaf association threshold [0-1]
	@param title    title of subnet
	@param n_max_a  max number of activities
	@param n_max_c  max number of compounds
	@param sqls     return param: SQLs used
  */
  public static HashMap<String,Integer> Disease2Network(
	DBCon dbcon,
	File fout_rgt,
	File fout_rgtp,
	File fout,
	File fout_cpd,
	String kid_query,
	Float scaf_min,
	String title,
	Integer n_max_a,Integer n_max_c,
	ArrayList<Integer> tids,	//return value
	CompoundList cpdlist,	//return value
	CCPList ccplist,	//return value
	ArrayList<String> sqls)	//return value
	throws Exception
  {
    Boolean human_filter=true;
    HashMap<String,Integer> counts = new HashMap<String,Integer>(); //for return counts
    PrintWriter fout_writer = (fout!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout,false)))) : null;
    PrintWriter fout_rgtp_writer = (fout_rgtp!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout_rgtp,false)))) : null;
    PrintWriter fout_rgt_writer = (fout_rgt!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout_rgt,false)))) : null;

    ArrayList<String> wheres = new ArrayList<String>();
    if (kid_query==null)
      throw new Exception("Disease KID must be specified.");
    if (title==null || title.isEmpty()) title="CARLSBAD Subnet";

    HashMap<String,Object> root = new HashMap<String,Object>();
    root.put("format_version", "1.0");
    root.put("generated_by", "carlsbadone-0.0.1-SNAPSHOT");
    root.put("target_cytoscapejs_version", "~2.1");
    HashMap<String,Object> data = new HashMap<String,Object>();
    data.put("name", title);
    data.put("shared_name", title);
    data.put("SUID", new Integer(52)); //integer
    data.put("selected", new Boolean(true)); //boolean
    root.put("data", data);

    HashMap<Integer,HashMap<String,String> > tgtdata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,HashMap<String,Boolean> > > tgt_tgt_ids = new HashMap<Integer,HashMap<String,HashMap<String,Boolean> > >();
    ArrayList<Integer> tids_disease  = carlsbad_utils.ListDiseaseTargets(dbcon,kid_query);
    tids.addAll(tids_disease);
    System.err.println("DEBUG: (Disease2Network) tids_disease: "+tids_disease.toString());
    if (human_filter) wheres.add("target.species='human'");
    String sql=carlsbad_utils.TargetIDs2SQL(tids,wheres); //wheres needed later...
    ResultSet rset=dbcon.executeSql(sql);
    carlsbad_utils.ReadTargetData(rset,tgtdata,tgt_tgt_ids);

    HashMap<Integer,HashSet<Integer> > t2c_global = new HashMap<Integer,HashSet<Integer> >();
    for (int tid: tgtdata.keySet()) t2c_global.put(tid,null);
    carlsbad_utils.Targets2Compounds(t2c_global,dbcon); //Get global-degree data (to all compounds, not just subnet).

    HashMap<Integer,HashMap<String,String> > actdata = new HashMap<Integer,HashMap<String,String> >();

    HashMap<Integer,HashMap<String,String> > cpddata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,Boolean> > cpdsynonyms = new HashMap<Integer,HashMap<String,Boolean> >();
    HashMap<Integer,HashMap<String,HashSet<String> > > cpd_sbs_ids = new HashMap<Integer,HashMap<String,HashSet<String> > >();
    HashMap<Integer,HashSet<Integer> > c2t_global = new HashMap<Integer,HashSet<Integer> >();

    HashMap<String,HashMap<String,String> > scafdata = new HashMap<String,HashMap<String,String> >();
    HashSet<Integer> scafids_read = new HashSet<Integer>();
    HashMap<Integer,HashSet<Integer> > s2c_global = new HashMap<Integer,HashSet<Integer> >();
    HashMap<Integer,HashSet<Integer> > s2t_global = new HashMap<Integer,HashSet<Integer> >();

    HashSet<Integer> mcesids_read = new HashSet<Integer>();
    HashMap<String,HashMap<String,String> > mcesdata = new HashMap<String,HashMap<String,String> >();
    HashMap<Integer,HashSet<Integer> > m2c_global = new HashMap<Integer,HashSet<Integer> >();
    HashMap<Integer,HashSet<Integer> > m2t_global = new HashMap<Integer,HashSet<Integer> >();

    if (tids.size()==0) return counts;

    /// Activities and Compounds (NB: wheres kept):
    sql=carlsbad_utils.ActivityCompoundsSQL(null,null,null,null,null,null,null,null,null,null,null,null,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);

    /// Populating HashMaps with activity and compound data.
    carlsbad_utils.ReadCompoundData(rset,actdata,cpddata,cpdsynonyms,cpd_sbs_ids);
    for (int cid: cpddata.keySet()) c2t_global.put(cid,null);
    carlsbad_utils.Compounds2Targets(c2t_global,dbcon,human_filter); //Get global-degree data.
    
    /// Scaffolds (NB: wheres kept):
    sql=carlsbad_utils.ScaffoldSQL(null,null,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);
    carlsbad_utils.ReadScaffoldData(rset,cpddata,scafdata,scafids_read);
    for (int scafid: scafids_read) s2c_global.put(scafid,null);
    for (int scafid: scafids_read) s2t_global.put(scafid,null);
    carlsbad_utils.Scaffolds2Compounds(s2c_global,dbcon); //Get global-degree data.
    carlsbad_utils.Scaffolds2Targets(s2t_global,dbcon); //Get global-degree data.

    /// MCESs (NB: wheres kept):
    sql=carlsbad_utils.McesSQL(null,null,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);
    carlsbad_utils.ReadMcesData(rset,cpddata,mcesdata,mcesids_read);
    for (int mcesid: mcesids_read) m2c_global.put(mcesid,null);
    for (int mcesid: mcesids_read) m2t_global.put(mcesid,null);
    carlsbad_utils.Mcess2Compounds(m2c_global,dbcon);
    carlsbad_utils.Mcess2Targets(m2t_global,dbcon);

    /// Apply scaf_min filter to scafdata.
    int n_edge_removed=(scaf_min>0.0)?carlsbad_utils.Filter_By_S2C_Weight(scafdata,scaf_min):0;
    counts.put("edges removed by scaffold association cutoff",n_edge_removed);

    // Scafs & cpds removed by Filter_By_S2C_Weight() still in c2t_global; targets may be orphaned.
    // HOWEVER -- KEEP FOR DISEASE LINKS.
    // carlsbad_utils.Remove_Orphan_Targets(tgtdata,actdata);

    /// Get other compounds associated by Scaffolds; IDs in s2c_global.
    /// Get other compounds associated by MCESs; IDs in m2c_global.

    ArrayList<Integer> scafids = new ArrayList<Integer>(s2c_global.keySet());
    ArrayList<Integer> mcesids = new ArrayList<Integer>(m2c_global.keySet());

    HashSet<Integer> cid_global = new HashSet<Integer>();
    for (int scafid: s2c_global.keySet()) { for (int cid: s2c_global.get(scafid)) cid_global.add(cid); }
    for (int mcesid: m2c_global.keySet()) { for (int cid: m2c_global.get(mcesid)) cid_global.add(cid); }
    wheres.clear();
    if (cid_global.size()>0)
    {
      ArrayList<Integer> cid_other = new ArrayList<Integer>(cid_global);
      System.err.println("DEBUG: (Disease2Network) CompoundIDs2SQL, cid_other.size()="+cid_other.size());
      sql=carlsbad_utils.CompoundIDs2SQL(cid_other,wheres);
      sqls.add(sql);
      rset=dbcon.executeSql(sql);
      int n_cpd_neighbor = carlsbad_utils.ReadCompoundData(rset,null,cpddata,cpdsynonyms,cpd_sbs_ids);
      counts.put("n_cpd_neighbor",n_cpd_neighbor);

      //Adds new edges to scafdata
      if (scafids.size()>0)
      {
        sql=carlsbad_utils.ScafedgeSQL(cid_other,scafids);
        //System.err.println("DEBUG: (Disease2Network) ScafedgeSQL: "+sql);
        rset=dbcon.executeSql(sql);
        carlsbad_utils.ReadScaffoldData(rset,cpddata,scafdata,scafids_read);
      }

      //Adds new edges to mcesdata
      if (mcesids.size()>0)
      {
        sql=carlsbad_utils.McesedgeSQL(cid_other,mcesids);
        //System.err.println("DEBUG: (Disease2Network) McesedgeSQL: "+sql);
        rset=dbcon.executeSql(sql);
        carlsbad_utils.ReadMcesData(rset,cpddata,mcesdata,mcesids_read);
      }
    }
    //else { System.err.println("DEBUG: No CCP-associated compounds found."); }

    // Formerly CCP-associated cpds may be orphaned.
    carlsbad_utils.Remove_Orphan_Compounds(cpddata,scafdata,mcesdata,actdata);

    // Removed cpds result in orphaned edges.
    carlsbad_utils.Remove_Dangling_Edges(scafdata,cpddata);

    //Populate cpdlist here.
    for (int cid: cpddata.keySet()) cid_global.add(cid);
    cpdlist.load(new HashSet<Integer>(cid_global));

    //Populate ccplist here (scaffolds).
    ccplist.loadScaffolds(new HashSet<Integer>(scafids));

    //Populate ccplist here (mcess).
    ccplist.loadMCESs(new HashSet<Integer>(mcesids));

    HashMap<String, Object> elements = new HashMap<String, Object>();
    ArrayList<HashMap<String, Object> > nodes = new ArrayList<HashMap<String, Object> >();
    ArrayList<HashMap<String, Object> > edges = new ArrayList<HashMap<String, Object> >();
    elements.put("nodes", nodes);
    elements.put("edges", edges);
    ObjectMapper mapper = new ObjectMapper();
    JsonFactory jsf = mapper.getFactory();

    if (fout_rgt!=null) // Write targets-only reduced-graph to CYJS.  
    {
      String disease_name = carlsbad_utils.GetKIDName(dbcon,kid_query);
      Disease disease = new Disease(kid_query);
      disease.setName(disease_name);
      for (int tid: tids_disease) disease.addTID(tid);

      carlsbad_utils.WriteReducedGraph2Elements(tgtdata, disease,
	null,	//cid_query n.a.
	t2c_global, s2t_global, s2c_global, m2t_global, m2c_global, tgt_tgt_ids,
	actdata, cpddata,
	null,	//cpdsynonyms not needed?
	null,	//cpd_sbs_ids not needed?
	scafdata, mcesdata,
	false,	//include_ccps?
	counts, elements);

      root.put("elements", elements);
      JsonGenerator jsg = jsf.createGenerator(fout_rgt_writer);
      jsg.useDefaultPrettyPrinter();
      jsg.writeObject(root);
      jsg.close();
    }
    if (fout_rgtp!=null) // Write targets+CCPs reduced-graph to CYJS.  
    {
      String disease_name = carlsbad_utils.GetKIDName(dbcon,kid_query);
      Disease disease = new Disease(kid_query);
      disease.setName(disease_name);
      for (int tid: tids_disease) disease.addTID(tid);

      carlsbad_utils.WriteReducedGraph2Elements(tgtdata, disease,
	null,	//cid_query n.a.
	t2c_global, s2t_global, s2c_global, m2t_global, m2c_global, tgt_tgt_ids,
	actdata, cpddata,
	null,	//cpdsynonyms not needed?
	null,	//cpd_sbs_ids not needed?
	scafdata, mcesdata,
	true,	//include_ccps?
	counts, elements);

      root.put("elements", elements);
      JsonGenerator jsg = jsf.createGenerator(fout_rgtp_writer);
      jsg.useDefaultPrettyPrinter();
      jsg.writeObject(root);
      jsg.close();
    }
    if (fout!=null) // Write full graph to CYJS.
    {
      carlsbad_utils.WriteTargets2Elements(tgtdata,t2c_global,tgt_tgt_ids,counts, elements);
      carlsbad_utils.WriteCompounds2Elements(cpddata,c2t_global,cpd_sbs_ids,actdata,cpdsynonyms,n_max_c,n_max_a,counts, elements);
      HashSet<Integer> scafids_written = new HashSet<Integer>();
      carlsbad_utils.WriteScaffolds2Elements(scafdata,scafids_written,s2c_global,s2t_global,counts, elements);
      HashSet<Integer> mcesids_written = new HashSet<Integer>();
      carlsbad_utils.WriteMcess2Elements(mcesdata,mcesids_written,m2c_global,m2t_global,counts, elements);

      root.put("elements", elements);
      JsonGenerator jsg = jsf.createGenerator(fout_writer);
      jsg.useDefaultPrettyPrinter();
      jsg.writeObject(root);
      jsg.close();
    }

    // Write cpddata compounds to SDF for display and/or download.
    if (fout_cpd!=null) carlsbad_utils.WriteCompounds2SDF(cpdlist, fout_cpd);

    return counts;
  }
  /////////////////////////////////////////////////////////////////////////////  
}
