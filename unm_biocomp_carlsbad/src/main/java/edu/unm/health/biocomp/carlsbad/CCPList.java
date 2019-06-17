package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*; //Date
import java.util.regex.*; // Pattern, Matcher
import java.text.*;
import java.sql.*;

import chemaxon.formats.*;
import chemaxon.struc.*; //Molecule
import chemaxon.marvin.io.*; //MolImportException

import edu.unm.health.biocomp.util.*; //GetURI2List
import edu.unm.health.biocomp.util.db.*; //DBCon

/**	Maps CCPs to CIDs, TIDs.

	@author Jeremy J Yang
*/
public class CCPList extends HashMap<Integer,CCP>
{
  private static final String DBHOST="habanero.health.unm.edu";
  private static final String DBNAME="carlsbad";
  private static final String DBUSR="dbc";
  private static final String DBPW="chem!nfo";

  private java.util.Date t_loaded;
  
  /////////////////////////////////////////////////////////////////////////////
  public CCPList()
  {
    this.refreshTimestamp();
  }
  /////////////////////////////////////////////////////////////////////////////
  public java.util.Date getTimestamp()
  {
    return this.t_loaded;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void setTimestamp(java.util.Date _ts)
  {
    this.t_loaded=_ts;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void refreshTimestamp()
  {
    this.t_loaded = new java.util.Date();
  }
  /////////////////////////////////////////////////////////////////////////////
  public int scaffoldCount()
  {
    int n=0;
    for (CCP ccp: this.values())
      if (ccp.getType().equals("scaffold")) ++n;
    return n;
  }
  /////////////////////////////////////////////////////////////////////////////
  public int mcesCount()
  {
    int n=0;
    for (CCP ccp: this.values())
      if (ccp.getType().equals("mces")) ++n;
    return n;
  }
  /////////////////////////////////////////////////////////////////////////////
  public int compoundCount()
  {
    HashSet<Integer> cids = new HashSet<Integer>();
    for (CCP ccp: this.values())
    {
      for (int cid: ccp.getCIDs()) cids.add(cid);
    }
    return cids.size();
  }
  /////////////////////////////////////////////////////////////////////////////
  public int targetCount()
  {
    HashSet<Integer> tids = new HashSet<Integer>();
    for (CCP ccp: this.values())
    {
      for (int tid: ccp.getTIDs()) tids.add(tid);
    }
    return tids.size();
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Load scaffolds from db.
  */
  public Boolean loadScaffolds(HashSet<Integer> ids)
	throws IOException,SQLException
  {
    if (ids.size()==0) return false;
    Boolean human_only=true; //hard-coded for now
    DBCon dbcon = null;
    try { dbcon = new DBCon("postgres",DBHOST,5432,DBNAME,DBUSR,DBPW); }
    catch (SQLException e) { System.err.println("Connection failed:"+e.getMessage()); }
    catch (Exception e) { System.err.println("Connection failed:"+e.getMessage()); }
    if (dbcon==null) return false;

    ResultSet rset = carlsbad_utils.GetScaffolds(dbcon,ids);
    while (rset.next()) //id, smiles, natoms
    {
      Integer id=rset.getInt("id");
      if (!this.containsKey(id)) this.put(id,new CCP(id,"scaffold"));
      CCP ccp = this.get(id);
      String smi=rset.getString("smiles");
      if (smi!=null) ccp.setSmiles(smi);
      ccp.setAtomCount(rset.getInt("natoms"));

      ResultSet rset2 = carlsbad_utils.GetScaffoldCompounds(dbcon,id);
      while (rset2.next()) //cid,smiles
      {
        Integer cid=rset2.getInt("cid");
        ccp.addCID(cid);
      }

      rset2 = carlsbad_utils.GetCompoundsTargets(dbcon,ccp.getCIDs(),human_only);
      while (rset2.next()) //tid
      {
        Integer tid=rset2.getInt("tid");
        ccp.addTID(tid);
      }
    }
    this.refreshTimestamp();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Load mcess from db.
  */
  public Boolean loadMCESs(HashSet<Integer> ids)
	throws IOException,SQLException
  {
    if (ids.size()==0) return false;
    Boolean human_only=true; //hard-coded for now
    DBCon dbcon = null;
    try { dbcon = new DBCon("postgres",DBHOST,5432,DBNAME,DBUSR,DBPW); }
    catch (SQLException e) { System.err.println("Connection failed:"+e.getMessage()); }
    catch (Exception e) { System.err.println("Connection failed:"+e.getMessage()); }
    if (dbcon==null) return false;

    ResultSet rset = carlsbad_utils.GetMCESs(dbcon,ids);
    while (rset.next()) //id, smarts
    {
      Integer id=rset.getInt("id");
      if (!this.containsKey(id)) this.put(id,new CCP(id,"mces"));
      CCP ccp = this.get(id);
      String sma=rset.getString("smarts");
      if (sma!=null) ccp.setSmarts(sma);
      ccp.setAtomCount(sma.replaceAll("[^A-Z#cnosa\\*]","").length()); //Kludge, for approx atom count.

      ResultSet rset2 = carlsbad_utils.GetMCESCompounds(dbcon,id);
      while (rset2.next()) //cid,smiles
      {
        Integer cid=rset2.getInt("cid");
        ccp.addCID(cid);
      }

      rset2 = carlsbad_utils.GetCompoundsTargets(dbcon,ccp.getCIDs(),human_only);
      while (rset2.next()) //tid
      {
        Integer tid=rset2.getInt("tid");
        ccp.addTID(tid);
      }
    }
    this.refreshTimestamp();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public ArrayList<CCP> getAllSortedBy(String field,Boolean desc)
  {
    ArrayList<CCP> ccps = new ArrayList<CCP>(this.values());
    if (field.equals("n_tgt"))
      Collections.sort(ccps,ByTargetCount);
    else if (field.equals("n_cpd"))
      Collections.sort(ccps,ByCompoundCount);
    else if (field.equals("n_atm"))
      Collections.sort(ccps,ByAtomCount);
    else if (field.equals("type"))
    {
      Collections.sort(ccps,ByID);
      Collections.sort(ccps,ByType);
    }
    else
      Collections.sort(ccps);
    if (desc) Collections.reverse(ccps);
    return ccps;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<CCP> ByTargetCount = new Comparator<CCP>()  { //Collections.sort(ccps,ByTargetCount)
    public int compare(CCP cA,CCP cB)
    { return (cA.getTargetCount()>cB.getTargetCount()?1:(cA.getTargetCount()<cB.getTargetCount()?-1:0)); }
    boolean equals(CCP cA,CCP cB)
    { return (cA.getTargetCount()==cB.getTargetCount()); }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<CCP> ByCompoundCount = new Comparator<CCP>()  { //Collections.sort(ccps,ByCompoundCount)
    public int compare(CCP cA,CCP cB)
    { return (cA.getCompoundCount()>cB.getCompoundCount()?1:(cA.getCompoundCount()<cB.getCompoundCount()?-1:0)); }
    boolean equals(CCP cA,CCP cB)
    { return (cA.getCompoundCount()==cB.getCompoundCount()); }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<CCP> ByAtomCount = new Comparator<CCP>()  { //Collections.sort(ccps,ByAtomCount)
    public int compare(CCP cA,CCP cB)
    { return (cA.getAtomCount()>cB.getAtomCount()?1:(cA.getAtomCount()<cB.getAtomCount()?-1:0)); }
    boolean equals(CCP cA,CCP cB)
    { return (cA.getAtomCount()==cB.getAtomCount()); }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<CCP> ByType = new Comparator<CCP>()  {     //Collections.sort(ccps,ByType)
    public int compare(CCP cA,CCP cB)
    { return (cA.getType().equalsIgnoreCase(cB.getType())?cA.getType().compareTo(cB.getType()):cA.getType().compareToIgnoreCase(cB.getType())); }
    boolean equals(CCP cA,CCP cB)
    { return (cA.getType().equals(cB.getType())); }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<CCP> ByID = new Comparator<CCP>()  { //Collections.sort(ccps,ByID)
    public int compare(CCP cA,CCP cB)
    { return (cA.getID()<cB.getID()?1:(cA.getID()>cB.getID()?-1:0)); }
    boolean equals(CCP cA,CCP cB)
    { return (cA.getID()==cB.getID()); }
  };
  /////////////////////////////////////////////////////////////////////////////
}
