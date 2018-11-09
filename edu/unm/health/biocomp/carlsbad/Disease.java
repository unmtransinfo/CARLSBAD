package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*; //Date

/**	Provides fast in-memory drug (compound) data storage; used by DiseaseList.
	@author Jeremy J Yang
*/
/////////////////////////////////////////////////////////////////////////////
public class Disease
	implements Comparable<Object>
{
  private String kid;
  private String name;
  private HashSet<Integer> tids;
  private Integer n_cpd; //union of tgt n_cpds

  private Disease() {} //not allowed
  public Disease(String _kid)
  {
    this.kid = _kid;
    this.tids = new HashSet<Integer>();
  }
  public String getID() { return this.kid; }
  public Integer getTargetCount() { return this.tids.size(); }
  public void setName(String _name) { this.name=_name; }
  public String getName() { return this.name; }
  public HashSet<Integer> getTIDs() { return this.tids; }
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
  public Integer getCompoundCount() { return (this.n_cpd==null?0:this.n_cpd); }
  public void setCompoundCount(int _n_cpd) { this.n_cpd=_n_cpd; }

  /////////////////////////////////////////////////////////////////////////////
  public int compareTo(Object o)        //native-order (by kid)
  {
    return (this.kid.compareTo(((Disease)o).kid));
  }
}
