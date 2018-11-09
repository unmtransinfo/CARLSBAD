package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*; //Date
import java.util.regex.*; // Pattern, Matcher
import java.text.*;
import java.sql.*;

import edu.unm.health.biocomp.kegg.*; //kegg_utils
import edu.unm.health.biocomp.db.*; //DBCon
import edu.unm.health.biocomp.rest.*; //GetURI2List
import edu.unm.health.biocomp.util.*; //GetURI2List

/**	Handles KEGG disease list, names and KIDs.
	Superclassed HashMap provides id2name.

	THIS VERSION ACCESSES KEGG VIA REST.  SLOW. THUS PROBABLY OBSOLETE.  SEE DiseaseList.java
	WHICH USES NEW kegg_disease TABLE IN CARLSBAD.

	Currently we require and expect:
	KID&lt;TAB&gt;NAME

	@author Jeremy J Yang
*/
public class DiseaseList_kegg extends HashMap<String,String>
{

  private static String DBHOST="habanero.health.unm.edu";
  private static String DBNAME="carlsbad";
  private static String DBUSR="dbc";
  private static String DBPW="chem!nfo";

  private HashMap<String,String> _name2id;
  private HashMap<String, HashSet<Integer> > _id2tids; //Carlsbad TIDs
  private java.util.Date _t_loaded;
  
  public DiseaseList_kegg()
  {
    this._name2id = new HashMap<String,String>();
    this._id2tids = new HashMap<String, HashSet<Integer> >(); //Carlsbad TIDs
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Lookup KEGG ID from full exact disease name.
  */
  public String name2id(String name)
  {
    if (this._name2id==null) return null;
    else if (!this._name2id.containsKey(name)) return null;
    return this._name2id.get(name);
  }
  /////////////////////////////////////////////////////////////////////////////
  public java.util.Date getTimestamp()
  {
    return this._t_loaded;
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean loadFromKegg()
    throws IOException
  {
    String uri="http://rest.kegg.jp/list/disease";
    ArrayList<String> lines = rest_utils.GetURI2List(uri);
    System.err.println("DEBUG: linecount("+uri+"): "+lines.size());
    for (String line: lines)
    {
      if (!line.matches("[^\\s]+\\t[^\\s].*$"))
      {
        System.err.println("ERROR: bad line: "+line);
        continue;
      }
      String[] fields=Pattern.compile("\\t").split(line);
      if (fields.length!=2)
      {
        System.err.println("ERROR: fields.length!=2 ("+fields.length+") ; bad line: "+line);
        continue;
      }
      this.put(fields[0],fields[1]);
      this._id2tids.put(fields[0],new HashSet<Integer>());
      this._name2id.put(fields[1],fields[0]);
    }
    this._t_loaded = new java.util.Date();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void addTargets(String kid,List<Integer> tids)
  {
    for (int tid: tids) addTarget(kid,tid);
  }
  /////////////////////////////////////////////////////////////////////////////
  public void addTargets(String kid,HashSet<Integer> tids)
  {
    for (int tid: tids) addTarget(kid,tid);
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean addTarget(String kid,Integer tid)
  {
    if (!this.containsKey(kid)) return false;
    if (!this._id2tids.containsKey(kid)) this._id2tids.put(kid,new HashSet<Integer>());
    this._id2tids.get(kid).add(tid);
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public List<Integer> getTargets(String kid)
  {
    if (!this.containsKey(kid)) return null;
    return new ArrayList<Integer>(this._id2tids.get(kid));
  }
  /////////////////////////////////////////////////////////////////////////////
  public Integer getTargetCount(String kid)
  {
    if (!this.containsKey(kid)) return null;
    return (this._id2tids.get(kid).size());
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Loads CARLSBAD targets (TIDs) using Kegg gene links mapped to
	Uniprots and NCBI GIs.
  */
  public boolean loadTargets()
    throws IOException,SQLException
  {
    DBCon dbcon = null;
    try { dbcon = new DBCon("postgres",DBHOST,5432,DBNAME,DBUSR,DBPW); }
    catch (SQLException e) { System.err.println("Connection failed:"+e.getMessage()); }
    catch (Exception e) { System.err.println("Connection failed:"+e.getMessage()); }
    if (dbcon==null) return false;

    int n_linked_uniprot=0;
    int n_linked_ncbi=0;
    int n_kid=0;
    for (String kid: this.keySet())
    {
      ++n_kid;
      HashSet<String> ncbi_gis = new HashSet<String>();
      HashSet<String> uniprots = new HashSet<String>();
      ArrayList<String> genes = new ArrayList<String>();

      List<String> genes_this = kegg_utils.Link2Genes(kid); //disease2gene links
      if (genes_this!=null) genes.addAll(genes_this);
      //for (int i=genes.size()-1;i>=0;--i) { if (genes.indexOf(genes.get(i))<i) genes.remove(i); }
      for (String gene: genes)
      {
        try {
          List<String> gis = kegg_utils.KID2NCBIGIs(gene);
          //System.err.println("DEBUG: gene = "+gene+"; gis: "+gis.size());
          for (String gi: gis) ncbi_gis.add(gi);
        } catch (Exception e)
        { System.err.println("ERROR: (Disease2Network): kegg_utils.KID2NCBIGIs(gene="+gene+"); "+e.getMessage()); }
        try {
          List<String> ups = kegg_utils.KID2Uniprots(gene);
          //System.err.println("DEBUG: gene = "+gene+"; ups: "+ups.size());
          for (String up: ups) uniprots.add(up);
        } catch (Exception e)
        { System.err.println("ERROR: (Disease2Network): kegg_utils.KID2Uniprots(gene="+gene+"); "+e.getMessage()); }
        //System.err.println("DEBUG: (Disease2Network): kid: "+kid+"; gene: "+gene);
      }
      if (uniprots.size()==0 && ncbi_gis.size()==0) continue;

      HashSet<Integer> tids = new HashSet<Integer>();

      ArrayList<Integer> tids_uniprot = carlsbad_utils.TargetExternalIDs2TIDs(uniprots,"UniProt",dbcon);
      if (tids_uniprot.size()>0)
      {
        this.addTargets(kid,tids_uniprot);
        for (int tid: tids_uniprot) tids.add(tid);
        ++n_linked_uniprot;
      }

      ArrayList<Integer> tids_ncbi = carlsbad_utils.TargetExternalIDs2TIDs(ncbi_gis,"NCBI gi",dbcon);
      if (tids_ncbi.size()>0)
      {
        this.addTargets(kid,tids_ncbi);
        for (int tid: tids_ncbi) tids.add(tid);
        ++n_linked_ncbi;
      }

      System.err.println("DEBUG: "+n_kid+". "+kid+
	"; kids/uniprots: "+tids_uniprot.size()+" / "+uniprots.size()+
	": kids/ncbi_gis: "+tids_ncbi.size()+" / "+ncbi_gis.size()+
	"; uniprot-linked: "+n_linked_uniprot+
	"; ncbi_gi-linked : "+n_linked_ncbi+
	"; tids.size(): "+tids.size());
    }
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public int removeTargetless()
    throws IOException
  {
    int n_rm=0;
    HashSet<String> kids = new HashSet<String>();
    for (String kid: this.keySet())
    {
      Integer n_tgt = this.getTargetCount(kid);
      if (n_tgt==null || n_tgt==0) kids.add(kid);
    }
    for (String kid: kids)
    {
      this._name2id.remove(this.get(kid));
      this.remove(kid);
      ++n_rm;
    }
    return n_rm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /*	Testing purposes only.
  */
  public static void main(String[] args)
	throws IOException,SQLException
  {
    java.util.Date t_0 = new java.util.Date();
    DiseaseList_kegg dlist = new DiseaseList_kegg();
    dlist.loadFromKegg();
    System.err.println("DEBUG: disease count (all): "+dlist.size());
    int n_all = dlist.size();
    System.err.println("DEBUG: loading targets...");
    dlist.loadTargets();
    dlist.removeTargetless();
    System.err.println("DEBUG: disease count (with targets): "+dlist.size());
    System.err.println("DEBUG: timestamp: "+dlist.getTimestamp().toString());
    System.err.println("Total elapsed time: "+time_utils.TimeDeltaStr(t_0,new java.util.Date()));
  }
}
