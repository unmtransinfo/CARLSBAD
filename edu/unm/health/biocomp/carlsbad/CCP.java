package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*; //Date
import java.util.regex.*; // Pattern, Matcher


/**	Provides fast in-memory CCP (Common Chemical Pattern) data storage; used by CCPList.
	A CCP may be a scaffold or MCES.

	@author Jeremy J Yang
*/
/////////////////////////////////////////////////////////////////////////////
public class CCP
	implements Comparable<Object>
{
  private Integer id;
  private Boolean is_mces; //scaf or mces
  private String smiles;
  private String smarts;
  private Integer natoms;
  private HashSet<Integer> cids;
  private HashSet<Integer> tids;

  private CCP() {} //not allowed
  public CCP(int _id,boolean _is_mces)
  {
    this.id = _id;
    this.is_mces = _is_mces;
    this.cids = new HashSet<Integer>();
    this.tids = new HashSet<Integer>();
  }
  public CCP(int _id,String _type)
  {
    this.id = _id;
    this.is_mces = _type.equalsIgnoreCase("mces");
    this.cids = new HashSet<Integer>();
    this.tids = new HashSet<Integer>();
  }
  public Integer getID() { return this.id; }
  /**	Normally for scaffolds.	*/
  public String getSmiles() { return this.smiles; }
  /**	Normally for MCESs.	*/
  public String getSmarts() { return this.smarts; }
  public Integer getCompoundCount() { return this.cids.size(); }
  public HashSet<Integer> getCIDs() { return this.cids; }
  public void setAtomCount(int _natoms) { this.natoms=_natoms; }
  public Integer getAtomCount() { return this.natoms; }

  /**	"scaffold" or "mces".	*/
  public String getType() { return (this.is_mces?"mces":"scaffold"); }
  /**	"scaffold" or "mces".	*/
  public void setType(String type) { this.is_mces = type.equalsIgnoreCase("mces"); }

  /**	Normally for scaffolds.	*/
  public void setSmiles(String _smiles) { this.smiles=_smiles; }
  /**	Normally for MCESs.	*/
  public void setSmarts(String _smarts) { this.smarts=_smarts; }

  public void addCID(Integer cid) { this.cids.add(cid); }
  public ArrayList<Integer> getCIDList()
  {
    ArrayList<Integer> _cids = new ArrayList<Integer>();
    for (int cid: this.cids) _cids.add(cid);
    return _cids;
  }
  public Boolean hasCID(Integer cid)
  {
    return this.cids.contains(cid);
  }
  public Integer getTargetCount() { return this.tids.size(); }
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

  /////////////////////////////////////////////////////////////////////////////
  public int compareTo(Object o)        //native-order (scafs first, then by id)
  {
    return (
      is_mces && !((CCP)o).is_mces ?
        1 : (!is_mces && ((CCP)o).is_mces ?
          -1 : (id > ((CCP)o).id ?
            1 : (id < ((CCP)o).id ? -1 : 0))));
  }
}
