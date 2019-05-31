package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*; //Date

/**	Provides fast in-memory target data storage; used by TargetList.

	@author Jeremy J Yang
*/
/////////////////////////////////////////////////////////////////////////////
public class Target
	implements Comparable<Object>
{
  private Integer tid;
  private String name;	/**	Preferred name of target, used as hash key by TargetList. */
  private String type;
  private String species;
  private String descr;
  private Integer n_cpd;
  private Boolean is_empirical;
  private HashMap<String,HashSet<String> > ids;

  public Target(Integer _tid)
  {
    this.tid = _tid;
    this.ids = new HashMap<String,HashSet<String> >();
    this.is_empirical=false;
  }
  public Target(Target _tgt)
  {
    this.tid = _tgt.getID();
    this.name = _tgt.getName();
    this.type = _tgt.getType();
    this.species = _tgt.getSpecies();
    this.descr = _tgt.getDescription();
    this.n_cpd = _tgt.getCompoundCount();
    this.is_empirical=_tgt.isEmpirical();
    this.ids = new HashMap<String,HashSet<String> >(_tgt.getIDs());
//    for (String _idtype: _tgt.getIDTypes())
//      for (String _id: _tgt.getIDList(_idtype))
//        this.addID(_idtype,_id);
  }
  public Integer getID() { return this.tid; }
  public String getName() { return (this.name==null?"":this.name); }
  public String getType() { return this.type; }
  public String getSpecies() { return this.species; }
  public String getNST() { return this.getName()+":"+this.getSpecies()+":"+this.getType(); }
  public String getDescription() { return this.descr; }
  public Integer getCompoundCount() { return (this.n_cpd==null?0:this.n_cpd); }
  public Boolean isEmpirical() { return this.is_empirical==null?false:this.is_empirical; }

  public void setName(String _name) { this.name=_name; }
  public void setType(String _type) { this.type=_type; }
  public void setSpecies(String _species) { this.species=_species; }
  public void setDescription(String _descr) { this.descr=_descr; }
  public void setCompoundCount(int _n_cpd) { this.n_cpd=_n_cpd; }
  public void setEmpirical(boolean _is_empirical) { this.is_empirical=_is_empirical; }

  public HashMap<String,HashSet<String> > getIDs() { return this.ids; }
  public void addID(String idtype,String id)
  {
    if (!this.ids.containsKey(idtype)) this.ids.put(idtype,new HashSet<String>());
    this.ids.get(idtype).add(id);
  }
  public ArrayList<String> getIDTypes()
  {
    ArrayList<String> xxx = new ArrayList<String>(this.ids.keySet());
    Collections.sort(xxx);
    return xxx;
  }
  public ArrayList<String> getIDList(String idtype)
  {
    ArrayList<String> xxx = new ArrayList<String>();
    if (!this.ids.containsKey(idtype)) return xxx;
    for (String id: this.ids.get(idtype)) xxx.add(id);
    Collections.sort(xxx);
    return xxx;
  }
  /////////////////////////////////////////////////////////////////////////////
  public int compareTo(Object o)	//native-order (by tid)
  {
    return (tid>((Target)o).tid ? 1 : (tid<((Target)o).tid ? -1 : 0));
  }
}
