package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*; //Date
import java.util.regex.*; // Pattern, Matcher

import edu.unm.health.biocomp.text.*; //NameList, Name

/**	Provides fast in-memory compound data storage; used by CompoundList.
	Superclassed by Drug.

	@author Jeremy J Yang
*/
/////////////////////////////////////////////////////////////////////////////
public class Compound
	implements Comparable<Object>
{
  private Integer cid;
  private String smiles;
  private HashSet<Integer> tids;
  private HashSet<String> synonyms;
  private HashMap<String,HashSet<String> > identifiers; //external identifiers
  private Boolean is_empirical;

  private Compound() {} //not allowed
  public Compound(Integer _cid)
  {
    this.cid = _cid;
    this.tids = new HashSet<Integer>();
    this.synonyms = new HashSet<String>();
    this.identifiers = new HashMap<String,HashSet<String> >();
  }
  public Integer getID() { return this.cid; }
  public String getSmiles() { return this.smiles; }
  public Integer getTargetCount() { return this.tids.size(); }
  public Boolean isEmpirical() { return this.is_empirical==null?false:this.is_empirical; }
  public HashSet<String> getSynonyms() { return this.synonyms; }
  public HashSet<Integer> getTIDs() { return this.tids; }

  public void setSmiles(String _smiles) { this.smiles=_smiles; }
  public void setEmpirical(boolean _is_empirical) { this.is_empirical=_is_empirical; }

  public void addTID(Integer tid) { this.tids.add(tid); }
  public ArrayList<Integer> getTIDList()
  {
    ArrayList<Integer> _tids = new ArrayList<Integer>();
    for (int tid: this.tids) _tids.add(tid);
    return _tids;
  }
  public Boolean hasTID(Integer tid)
  {
    return this.tids.contains(tid);
  }
  public void addSynonym(String synonym)
  {
    this.synonyms.add(synonym);
  }
  public ArrayList<String> getSynonymsSorted()
  {
    //ArrayList<String> syns = new ArrayList<String>(this.synonyms);
    //Collections.sort(syns);
    //return syns;

    NameList nlist = new NameList();
    for (String s: this.synonyms)
    {
      if (!nlist.contains(s)) nlist.add(new Name(s));
      Collections.sort(nlist);
      Collections.reverse(nlist);
    }
    ArrayList<String> syns = new ArrayList<String>();
    for (Name name: nlist) syns.add(name.getValue());
    return syns;
  }
  public void addIdentifier(String id_type,String id)
  {
    if (!this.identifiers.containsKey(id_type)) this.identifiers.put(id_type,new HashSet<String>());
    this.identifiers.get(id_type).add(id);
  }
  public void addIdentifiers(String id_type,HashSet<String> ids)
  {
    for (String id: ids) this.addIdentifier(id_type,id);
  }
  public HashSet<String> getIdentifiers(String id_type)
  {
    return this.identifiers.get(id_type);
  }
  public ArrayList<String> getIdentifiersSorted(String id_type)
  {
    ArrayList<String> ids = new ArrayList<String>(this.getIdentifiers(id_type));
    Collections.sort(ids);
    return ids;
  }
  public Set<String> getIdentifierTypes()
  {
    return this.identifiers.keySet();
  }
  public ArrayList<String> getIdentifierTypesSorted()
  {
    ArrayList<String> id_types = new ArrayList<String>(this.getIdentifierTypes());
    Collections.sort(id_types);
    return id_types;
  }
  /**	Return name; try to find nice name.
  */ 
  public String getName()
  {
    //if (this.synonyms.size()==0) return "";
    //ArrayList<String> names = new ArrayList<String>(this.synonyms);
    //Collections.sort(names);
    //for (String name: names) { if (name.matches("[A-Z][a-z]+$")) return name; }
    //for (String name: names) { if (name.matches("[A-Za-z, ]+$")) return name; }
    //return synonyms.iterator().next(); //fallback to 1st

    NameList nlist = new NameList();
    for (String s: this.synonyms)
    {
      if (!nlist.contains(s)) nlist.add(new Name(s));
      Collections.sort(nlist);
      Collections.reverse(nlist);
    }
    ArrayList<String> syns = new ArrayList<String>();
    if (nlist.size()==0) return "";
    return nlist.get(0).getValue();
  }
  /////////////////////////////////////////////////////////////////////////////
  public int compareTo(Object o)        //native-order (by cid)
  {
    return (cid>((Compound)o).cid ? 1 : (cid<((Compound)o).cid ? -1 : 0));
  }
}
