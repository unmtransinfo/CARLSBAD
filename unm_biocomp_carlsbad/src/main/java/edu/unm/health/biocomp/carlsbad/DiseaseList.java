package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*; //Date
import java.util.regex.*; // Pattern, Matcher
import java.text.*;
import java.sql.*;

import edu.unm.health.biocomp.util.*; //GetURI2List
import edu.unm.health.biocomp.util.db.*; //DBCon

/**	Handles KEGG disease list, names and KIDs.
	Superclassed HashMap provides id2name.

	@author Jeremy J Yang
*/
public class DiseaseList extends HashMap<String,Disease>
{

  private static String DBHOST="habanero.health.unm.edu";
  private static String DBNAME="carlsbad";
  //private static String DBUSR="dbc";
  //private static String DBPW="chem!nfo";
  private static String DBUSR="jjyang";
  private static String DBPW="assword";

  private HashMap<String,String> name2id;
  private java.util.Date t_loaded;
  
  public DiseaseList()
  {
    this.name2id = new HashMap<String,String>();
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Lookup KEGG ID from full exact disease name.
  */
  public String name2ID(String name)
  {
    if (this.name2id==null) return null;
    else if (!this.name2id.containsKey(name)) return null;
    return this.name2id.get(name);
  }
  /////////////////////////////////////////////////////////////////////////////
  public int nameCount()
  {
    return this.name2id.size();
  }
  /////////////////////////////////////////////////////////////////////////////
  public java.util.Date getTimestamp()
  {
    return this.t_loaded;
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean loadAll()
    throws IOException,SQLException
  {
    DBCon dbcon = null;
    try { dbcon = new DBCon("postgres",DBHOST,5432,DBNAME,DBUSR,DBPW); }
    catch (SQLException e) { System.err.println("Connection failed:"+e.getMessage()); }
    catch (Exception e) { System.err.println("Connection failed:"+e.getMessage()); }
    if (dbcon==null) return false;
    return loadAll(dbcon);
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean loadAll(DBCon dbcon)
    throws IOException,SQLException
  {
    if (dbcon==null) return false;

    ResultSet rset = carlsbad_utils.GetDiseases(dbcon);
    while (rset.next()) //kid, name, tid
    {
      String kid=rset.getString("kid");
      String name=rset.getString("name").replaceFirst("[\\s\\n\\r]+$","");
      Integer tid=rset.getInt("tid");
      if (!this.containsKey(kid)) this.put(kid,new Disease(kid));
      Disease disease = this.get(kid);
      disease.addTID(tid);
      disease.setName(name);
      this.name2id.put(name,kid); //assumes name-kid is 1:1. 
    }
    for (String kid: this.keySet())
    {
      Disease disease=this.get(kid);
      Integer n_cpd = carlsbad_utils.GetTargetsCompoundCount(disease.getTIDs(),dbcon);
      disease.setCompoundCount(n_cpd);
    }
    this.t_loaded = new java.util.Date();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public ArrayList<Disease> getDiseasesSortedBy(String field,Boolean desc)
  {
    ArrayList<Disease> diseases = new ArrayList<Disease>(this.values());

    if (field.equals("n_tgt"))
      Collections.sort(diseases,ByTargetCount);
    else if (field.equals("n_cpd"))
      Collections.sort(diseases,ByCompoundCount);
    else if (field.equals("name"))
      Collections.sort(diseases,ByName);
    else
      Collections.sort(diseases);
    if (desc) Collections.reverse(diseases);

    return diseases;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Disease> ByName     //Collections.sort(diseasess,ByName)
	= new Comparator<Disease>()  {
    public int compare(Disease dA,Disease dB)
    {
      return (dA.getName().equalsIgnoreCase(dB.getName())?dA.getName().compareTo(dB.getName()):dA.getName().compareToIgnoreCase(dB.getName()));
    }
    boolean equals(Disease dA,Disease dB)
    {
      return (dA.getName().equals(dB.getName()));
    }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Disease> ByTargetCount      //Collections.sort(diseases,ByTargetCount)
	= new Comparator<Disease>()  {
    public int compare(Disease dA,Disease dB)
    {
      return (dA.getTargetCount()>dB.getTargetCount() ?
         1 : (dA.getTargetCount()<dB.getTargetCount() ? -1 : 0));
    }
    boolean equals(Disease dA,Disease dB)
    {
      return (dA.getTargetCount()==dB.getTargetCount());
    }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Disease> ByCompoundCount      //Collections.sort(diseases,ByCompoundCount)
	= new Comparator<Disease>()  {
    public int compare(Disease dA,Disease dB)
    {
      return (dA.getCompoundCount()>dB.getCompoundCount() ?
         1 : (dA.getCompoundCount()<dB.getCompoundCount() ? -1 : 0));
    }
    boolean equals(Disease dA,Disease dB)
    {
      return (dA.getCompoundCount()==dB.getCompoundCount());
    }
  };
  /////////////////////////////////////////////////////////////////////////////
  /*	Testing purposes only.
  */
  public static void main(String[] args)
	throws IOException,SQLException
  {
    java.util.Date t_0 = new java.util.Date();
    DiseaseList dlist = new DiseaseList();
    dlist.loadAll();
    for (String kid: dlist.keySet())
    {
      Disease disease = dlist.get(kid);
      System.err.println(kid+": \""+disease.getName()+"\" [n_tgt="+disease.getTargetCount()+"]");
    }
    System.err.println("disease count: "+dlist.size());
    System.err.println("name count: "+dlist.nameCount());
    System.err.println("timestamp: "+dlist.getTimestamp().toString());
    System.err.println("Total elapsed time: "+time_utils.TimeDeltaStr(t_0,new java.util.Date()));
  }
}
