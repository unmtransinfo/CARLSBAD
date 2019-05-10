package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*; //Date,Collections
import java.util.regex.*; // Pattern, Matcher
import java.text.*;
import java.sql.*;

import edu.unm.health.biocomp.db.*; //DBCon
import edu.unm.health.biocomp.util.*; //GetURI2List

/**	Maps target TIDs to Targets with names, etc., and names to TIDs.

	PROBLEM: Same name for different targets. Most with different species, not all.
	Some with same species, different type.  Some with same species, same type, e.g.,
	"Estrogen-related receptor gamma", TIDs 960, 2363, both human receptors.

	Maybe use hash key: "NAME:SPECIES:TYPE"
	Name first for readability.

	@author Jeremy J Yang
*/
public class TargetList extends HashMap<Integer,Target>
{
  private static final String DBHOST="habanero.health.unm.edu";
  private static final String DBNAME="carlsbad";
  private static final String DBUSR="dbc";
  private static final String DBPW="chem!nfo";

  /**	For fast lookup, using Target nst (name:species:type).
	(Reminder: This property must not be static!)
  */
  private HashMap<String,Integer> nst2id;
  private java.util.Date t_loaded;
  
  /////////////////////////////////////////////////////////////////////////////
  public TargetList()
  {
    this.nst2id = new HashMap<String,Integer>();
  }
  /////////////////////////////////////////////////////////////////////////////
  public TargetList(TargetList tlist) //copy constructor
  {
    this.nst2id = new HashMap<String,Integer>();
    for (int tid: tlist.keySet())
      this.add(new Target(tlist.get(tid)));
    this.t_loaded = new java.util.Date();
  }
  /////////////////////////////////////////////////////////////////////////////
  public java.util.Date getTimestamp() { return this.t_loaded; }
  public void setTimestamp(java.util.Date _ts) { this.t_loaded=_ts; }
  public void refreshTimestamp() { this.t_loaded = new java.util.Date(); }
  /////////////////////////////////////////////////////////////////////////////
  public boolean add(Target tgt)
  {
    Integer tid=tgt.getID();
    if (this.containsKey(tid)) return false;
    this.put(tid,tgt);
    String name=tgt.getName();
    String species=tgt.getSpecies();
    String type=tgt.getType();
    if (name!=null && !name.isEmpty() && species!=null && type!=null)
    {
      String key = name+":"+species+":"+type;
      if (!this.nst2id.containsKey(key))
        this.nst2id.put(key,tid);
    }
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public Integer nst2ID(String name,String species,String type)
  {
    return nst2ID(name+":"+species+":"+type);
  }
  /////////////////////////////////////////////////////////////////////////////
  public Integer nst2ID(String nst)
  {
    if (!this.nst2id.containsKey(nst)) return null;
    return this.nst2id.get(nst);
  }
  /////////////////////////////////////////////////////////////////////////////
  public int empiricalCount()
  {
    int n=0;
    for (Target tgt: this.values())
    {
      if (tgt.isEmpirical()) n+=1;
    }
    return n;
  }
  /////////////////////////////////////////////////////////////////////////////
  public int nstCount() { return this.nst2id.size(); }
  /////////////////////////////////////////////////////////////////////////////
  public int nameCount()
  {
    HashSet<String> names = new HashSet<String>();
    for (String nst: this.nst2id.keySet())
    {
      String[] fields=Pattern.compile(":").split(nst);
      if (fields.length>0) names.add(fields[0]);
    }
    return names.size();
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean loadAll()
    throws IOException,SQLException
  {
    return loadAll(DBHOST,DBNAME,DBUSR,DBPW);
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean loadAll(String dbhost,String dbname,String dbusr,String dbpw)
    throws IOException,SQLException
  {
    DBCon dbcon = null;
    try { dbcon = new DBCon("postgres",dbhost,5432,dbname,dbusr,dbpw); }
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

    ResultSet rset = carlsbad_utils.GetTargets(dbcon);
    int n_badname=0;
    while (rset.next()) //tid, tname, species, ttype, descr, id_type, id
    {
      Integer tid=rset.getInt("tid");
      String species=rset.getString("species");
      String type=rset.getString("ttype");
      String name=rset.getString("tname").replaceFirst("[\\s]+$","").replaceAll("\"","");
      if ( name.isEmpty()
	//|| name.length()<3
	//|| name.length()>100
	)
      {
        System.err.println("NOTE: TID="+tid+"; name rejected by TargetList: \""+name+"\"");
        ++n_badname;
        continue;
      }

      if (!this.containsKey(tid)) this.put(tid,new Target(tid));
      Target tgt = this.get(tid);
      tgt.setName(name);
      tgt.setSpecies(species);
      tgt.setType(type);
      tgt.setDescription(rset.getString("descr"));
      if (!tgt.getIDs().containsKey(rset.getString("id_type")))
        tgt.getIDs().put(rset.getString("id_type"),new HashSet<String>());
      tgt.getIDs().get(rset.getString("id_type")).add(rset.getString("id"));

      if (name!=null && !name.isEmpty() && species!=null && type!=null)
      {
        String key = name+":"+species+":"+type;
        if (this.nst2id.containsKey(key) && !this.nst2id.get(key).equals(tid))
          System.err.println("DEBUG: TID="+tid+"; nst: \""+key+"\" collision with TID: "+this.nst2id.get(key));
        else
          this.nst2id.put(key,tid);
      }
    }
    if (n_badname>0) System.err.println("NOTE: bad names rejected by TargetList: "+n_badname);
    rset = carlsbad_utils.GetTargetCompoundCounts(dbcon);
    while (rset.next()) //tid, cpd_count
    {
      Integer tid=rset.getInt("tid");
      Target tgt = this.get(tid);
      if (tgt==null) continue; //Must have been skipped due to no name, short name, etc.
      tgt.setCompoundCount(rset.getInt("cpd_count"));
    }
    this.refreshTimestamp();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public int speciesCount(String species)
  {
    int n=0;
    for (int tid: this.keySet())
      if (this.get(tid).getSpecies().equals(species)) ++n;
    return n;
  }
  /////////////////////////////////////////////////////////////////////////////
  public TargetList selectBySpecies(Set<String> specieses)
  {
    TargetList tgtlist = new TargetList();
    for (int tid: this.keySet())
      if (specieses.contains(this.get(tid).getSpecies()))
        tgtlist.add(new Target(this.get(tid)));
    tgtlist.refreshTimestamp();
    return tgtlist;
  }
  /////////////////////////////////////////////////////////////////////////////
  public TargetList selectByIDs(HashSet<Integer> tids)
  {
    TargetList tgtlist = new TargetList();
    for (int tid: this.keySet())
      if (tids.contains(tid))
        tgtlist.add(new Target(this.get(tid)));
    tgtlist.refreshTimestamp();
    return tgtlist;
  }
  /////////////////////////////////////////////////////////////////////////////
  public ArrayList<Target> getTargetsSortedBy(String field,Boolean desc)
  {
    ArrayList<Target> tgts = new ArrayList<Target>(this.values());
    if (field.equals("n_cpd"))
      Collections.sort(tgts,ByCompoundCount);
    else if (field.equals("name"))
      Collections.sort(tgts,ByName);
    else if (field.equals("type"))
      Collections.sort(tgts,ByType);
    else if (field.equals("species"))
      Collections.sort(tgts,BySpecies);
    else if (field.equals("r2q"))
      Collections.sort(tgts,ByR2q);
    else
      Collections.sort(tgts);
    if (desc) Collections.reverse(tgts);
    return tgts;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Target> ByCompoundCount = new Comparator<Target>()  {      //Collections.sort(tgts,ByCompoundCount)
    public int compare(Target tA,Target tB)
    { return (tA.getCompoundCount()>tB.getCompoundCount()?1:(tA.getCompoundCount()<tB.getCompoundCount()?-1:0)); }
    boolean equals(Target tA,Target tB)
    { return (tA.getCompoundCount()==tB.getCompoundCount()); }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Target> ByName = new Comparator<Target>()  {     //Collections.sort(tgts,ByName)
    public int compare(Target tA,Target tB)
    { return (tA.getName().equalsIgnoreCase(tB.getName())?tA.getName().compareTo(tB.getName()):tA.getName().compareToIgnoreCase(tB.getName())); }
    boolean equals(Target tA,Target tB)
    { return (tA.getName().equals(tB.getName())); }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Target> BySpecies = new Comparator<Target>()  {     //Collections.sort(tgts,BySpecies)
    public int compare(Target tA,Target tB)
    { return (tA.getSpecies().equalsIgnoreCase(tB.getSpecies())?tA.getSpecies().compareTo(tB.getSpecies()):tA.getSpecies().compareToIgnoreCase(tB.getSpecies())); }
    boolean equals(Target tA,Target tB)
    { return (tA.getSpecies().equals(tB.getSpecies())); }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Target> ByType = new Comparator<Target>()  {     //Collections.sort(tgts,ByType)
    public int compare(Target tA,Target tB)
    { return (tA.getType().equalsIgnoreCase(tB.getType())?tA.getType().compareTo(tB.getType()):tA.getType().compareToIgnoreCase(tB.getType())); }
    boolean equals(Target tA,Target tB)
    { return (tA.getType().equals(tB.getType())); }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Target> ByR2q = new Comparator<Target>()  {     //Collections.sort(tgts,ByR2q)
    public int compare(Target tA,Target tB)
    { return (tA.isEmpirical()==tB.isEmpirical())?0:(tA.isEmpirical()?-1:1); }
    boolean equals(Target tA,Target tB)
    { return (tA.isEmpirical()==(tB.isEmpirical())); }
  };
  /////////////////////////////////////////////////////////////////////////////
  /*	Testing purposes only.
  */
  public static void main(String[] args)
	throws IOException,SQLException
  {
    java.util.Date t_0 = new java.util.Date();
    TargetList tlist = new TargetList();
    tlist.loadAll();
    for (int tid: tlist.keySet())
    {
      Target tgt = tlist.get(tid);
      String nst = tgt.getNST();
      System.out.println(""+tid+",\""+nst+"\","+tgt.getCompoundCount());
      if (tlist.nst2ID(nst)==null) 
        System.out.println("ERROR: nst2ID(\""+nst+"\")==null");
      else if (!tlist.nst2ID(nst).equals(tid)) 
        System.out.println("ERROR: nst2ID(\""+nst+"\")!="+tid+", (="+tlist.nst2ID(nst)+")");
    }
    System.err.println("target count: "+tlist.size());
    System.err.println("human target count: "+tlist.speciesCount("human"));
    System.err.println("timestamp: "+tlist.getTimestamp().toString());
    System.err.println("total elapsed time: "+time_utils.TimeDeltaStr(t_0,new java.util.Date()));
    System.err.println("sort tests...");
    System.err.println("n_cpd...");
    ArrayList<Target> tgts = tlist.getTargetsSortedBy("n_cpd",true);
    System.err.println("name...");
    tgts = tlist.getTargetsSortedBy("name",false);
    System.err.println("default...");
    tgts = tlist.getTargetsSortedBy("",false);
  }
}
