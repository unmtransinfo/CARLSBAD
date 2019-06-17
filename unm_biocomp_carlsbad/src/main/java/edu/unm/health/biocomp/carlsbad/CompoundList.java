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

/**	Maps CIDs to Compounds, and compound names to CIDs.

	@author Jeremy J Yang
*/
public class CompoundList extends HashMap<Integer,Compound>
{
  private static final String DBHOST="habanero.health.unm.edu";
  private static final String DBNAME="carlsbad";
  private static final String DBUSR="dbc";
  private static final String DBPW="chem!nfo";
  public static final  String[] id_types = new String[]{"CAS Registry No.", "ChEBI", "ChEMBL ID",
	"ChEMBL Ligand", "DrugBank", "iPHACE", "IUPHAR Ligand ID", "NURSA Ligand", "PDSP Record Number",
	"PharmGKB Drug", "PubChem CID", "PubChem SID", "RCSB PDB Ligand", "SMDL ID"};

  private HashMap<String,Integer> name2id;
  private java.util.Date t_loaded;
  
  /////////////////////////////////////////////////////////////////////////////
  public CompoundList()
  {
    this.name2id = new HashMap<String,Integer>();
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
  public int compoundCount()
  {
    return this.size();
  }
  /////////////////////////////////////////////////////////////////////////////
  public int synonymCount()
  {
    return this.name2id.size();
  }
  /////////////////////////////////////////////////////////////////////////////
  public int namelessCount()
  {
    int n=0;
    for (Compound cpd: this.values())
    {
      if (cpd.getName().isEmpty()) n+=1;
    }
    return n;
  }
  /////////////////////////////////////////////////////////////////////////////
  public int empiricalCount()
  {
    int n=0;
    for (Compound cpd: this.values())
    {
      if (cpd.isEmpirical()) n+=1;
    }
    return n;
  }
  /////////////////////////////////////////////////////////////////////////////
  public int targetCount()
  {
    HashSet<Integer> tids = new HashSet<Integer>();
    for (Compound cpd: this.values())
    {
      for (int tid: cpd.getTIDs()) tids.add(tid);
    }
    return tids.size();
  }
  /////////////////////////////////////////////////////////////////////////////
  public int synonym2CID(String name)
  {
    return this.name2id.get(name);
  }
  /////////////////////////////////////////////////////////////////////////////
  public HashSet<String> getSynonyms(String name)
  {
    HashSet<String> names = new HashSet<String>();
    if (!this.name2id.containsKey(name)) return names;
    return this.get(this.name2id.get(name)).getSynonyms();
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Load from db.
	Also need IDs.
  */
  public Boolean load(HashSet<Integer> cids)
	throws IOException,SQLException
  {
    if (cids.size()==0) return false;
    Boolean human_only=true; //hard-coded for now
    DBCon dbcon = null;
    try { dbcon = new DBCon("postgres",DBHOST,5432,DBNAME,DBUSR,DBPW); }
    catch (SQLException e) { System.err.println("Connection failed:"+e.getMessage()); }
    catch (Exception e) { System.err.println("Connection failed:"+e.getMessage()); }
    if (dbcon==null) return false;

    ResultSet rset = carlsbad_utils.GetCompounds(dbcon,cids);
    while (rset.next()) //cid, smiles, synonym 
    {
      Integer cid=rset.getInt("cid");
      if (!this.containsKey(cid)) this.put(cid,new Compound(cid));
      Compound cpd = this.get(cid);
      String smi=rset.getString("smiles");
      if (smi!=null) cpd.setSmiles(smi);

      String synonym=rset.getString("synonym");
      if (synonym!=null)
      {
        synonym=synonym.replaceFirst("[\\s]+$","").replaceAll("\"","");
        cpd.addSynonym(synonym);
        if (!this.name2id.containsKey(synonym)) this.name2id.put(synonym,cid);
      }

      ResultSet rset2 = carlsbad_utils.GetCompoundTargets(dbcon,cid,human_only);
      while (rset2.next()) //tid
      {
        Integer tid=rset2.getInt("tid");
        this.get(cid).addTID(tid);
      }
    }
    rset = carlsbad_utils.GetCompoundsIDs(dbcon,cids);
    while (rset.next()) //cid,id_type,id
    {
      Integer cid=rset.getInt("cid");
      if (!this.containsKey(cid)) continue; //error, should not happen
      Compound cpd = this.get(cid);
      String id_type=rset.getString("id_type");
      String id=rset.getString("id");
      cpd.addIdentifier(id_type,id);
    }
    this.refreshTimestamp();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Load from file, normally SDF format.
	Also need IDs.
  */
  public Boolean load(File fin)
	throws Exception
  {
    if (!fin.exists())
      throw new Exception("ERROR: CompoundList.load(file); file does not exist: "+fin.getAbsolutePath());
    String smifmt="cxsmiles:u-L-l-e-d-D-p-R-f-w";
    MolImporter molReader = new MolImporter(new FileInputStream(fin));

    Molecule mol;
    int n_mol=0;
    int n_err=0;
    for (n_mol=0;true;)
    {
      try { mol=molReader.read(); }
      catch (IOException e)
      {
        System.err.println(e.getMessage());
        ++n_err;
        continue;
      }
      if (mol==null) break; //EOF
      ++n_mol;

      String cid_str = mol.getProperty("Carlsbad_ID");
      if (cid_str==null) continue; //must have CID
      Integer cid=Integer.parseInt(cid_str);

      if (!this.containsKey(cid)) this.put(cid,new Compound(cid));
      Compound cpd = this.get(cid);

      String smi=null;
      try { smi=MolExporter.exportToFormat(mol,smifmt); }
      catch (IOException e) { System.err.println(e.getMessage()); }
      if (smi!=null) cpd.setSmiles(smi);

      String val;
      val = mol.getProperty("synonyms");
      if (val!=null)
      {
        String[] synonyms=Pattern.compile("[\\n\\r]+").split(val);
        for (String synonym: synonyms)
        {
          synonym=synonym.replaceFirst("[\\s]+$","").replaceAll("\"","");
          cpd.addSynonym(synonym);
          if (!this.name2id.containsKey(synonym)) this.name2id.put(synonym,cid);
        }
      }
      for (String id_type: id_types)
      {
        val = mol.getProperty(id_type);
        if (val!=null)
        {
          cpd.addIdentifier(id_type,val);
        }
      }
      //String molname=mol.getName();
    }
    this.refreshTimestamp();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean loadAllDrugs()
	throws IOException,SQLException
  {
    DBCon dbcon = null;
    try { dbcon = new DBCon("postgres",DBHOST,5432,DBNAME,DBUSR,DBPW); }
    catch (SQLException e) { System.err.println("Connection failed:"+e.getMessage()); }
    catch (Exception e) { System.err.println("Connection failed:"+e.getMessage()); }
    if (dbcon==null) return false;
    return loadAllDrugs(dbcon);
  }

  /////////////////////////////////////////////////////////////////////////////
  public boolean loadAllDrugs(DBCon dbcon)
	throws IOException,SQLException
  {
    if (dbcon==null) return false;
    Boolean human_only=true;

    ResultSet rset = carlsbad_utils.GetDrugs(dbcon);
    while (rset.next()) //cid, smiles, drug_name 
    {
      // Only add reasonable names.
      String name=rset.getString("drug_name").replaceFirst("[\\s]+$","").replaceAll("\"","");
      if (name.isEmpty()) continue;
      if (!name.matches("^.*[A-Z][a-z].*$")) continue;
      if (name.length()<3) continue;
      if (name.length()>80) continue;

      // Name is ok.  Add if new CID.
      Integer cid=rset.getInt("cid");
      if (!this.containsKey(cid)) this.put(cid,new Drug(cid));
      Drug drug = (Drug) this.get(cid);
      drug.addSynonym(name);
      if (!this.name2id.containsKey(name)) this.name2id.put(name,cid);

      String smi=rset.getString("smiles");
      if (smi!=null) drug.setSmiles(smi);

      ResultSet rset2=carlsbad_utils.GetCompoundTargets(dbcon,cid,human_only);
      while (rset2.next()) //tid
      {
        Integer tid=rset2.getInt("tid");
        this.get(cid).addTID(tid);
      }
    }
    rset=carlsbad_utils.GetCompoundsIDs(dbcon,new HashSet<Integer>(this.keySet()));
    while (rset.next()) //cid,id_type,id
    {
      Integer cid=rset.getInt("cid");
      Drug drug = (Drug) this.get(cid);
      String id_type=rset.getString("id_type");
      String id=rset.getString("id");
      drug.addIdentifier(id_type,id);
    }
    this.refreshTimestamp();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  public ArrayList<Compound> getAllSortedBy(String field,Boolean desc)
  {
    ArrayList<Compound> cpds = new ArrayList<Compound>(this.values());
    if (field.equals("n_tgt"))
      Collections.sort(cpds,ByTargetCount);
    else if (field.equals("name"))
      Collections.sort(cpds,ByName);
    else if (field.equals("r2q"))
      Collections.sort(cpds,ByR2q);
    else
      Collections.sort(cpds);
    if (desc) Collections.reverse(cpds);
    return cpds;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Compound> ByTargetCount = new Comparator<Compound>()  { //Collections.sort(compounds,ByTargetCount)
    public int compare(Compound cA,Compound cB)
    { return (cA.getTargetCount()>cB.getTargetCount()?1:(cA.getTargetCount()<cB.getTargetCount()?-1:0)); }
    boolean equals(Compound cA,Compound cB)
    { return (cA.getTargetCount()==cB.getTargetCount()); }
  };
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Compound> ByR2q = new Comparator<Compound>()  {     //Collections.sort(cpds,ByR2q)
    public int compare(Compound cA,Compound cB)
    { return (cA.isEmpirical()==cB.isEmpirical())?0:(cA.isEmpirical()?-1:1); }
    boolean equals(Compound cA,Compound cB)
    { return (cA.isEmpirical()==(cB.isEmpirical())); }
  };
  /////////////////////////////////////////////////////////////////////////////
  /**	Empty last.
  */
  public static Comparator<Compound> ByName = new Comparator<Compound>()  { //Collections.sort(compounds,ByName)
    public int compare(Compound cA,Compound cB)
    {
      if (cA.getName().isEmpty() || cB.getName().isEmpty()) { return (!cA.getName().isEmpty()?-1:(!cB.getName().isEmpty()?1:0)); }
      return (cA.getName().equalsIgnoreCase(cB.getName())?cA.getName().compareTo(cB.getName()):cA.getName().compareToIgnoreCase(cB.getName()));
    }
    boolean equals(Compound cA,Compound cB)
    { return (cA.getName().equals(cB.getName())); }
  };
  /////////////////////////////////////////////////////////////////////////////
}
