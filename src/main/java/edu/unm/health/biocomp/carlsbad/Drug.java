package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*; //Date
import java.util.regex.*; // Pattern, Matcher


/**	Provides fast in-memory drug (compound) data storage; used by DrugList.
	For now this is identical to Compound.  May add features later.
	@author Jeremy J Yang
*/
/////////////////////////////////////////////////////////////////////////////
public class Drug
	extends Compound
	implements Comparable<Object>
{
  public Drug(Integer _cid)
  {
    super(_cid);
  }

  public int compareTo(Object o)        //native-order (by cid)
  {
    return (this.getID()>((Drug)o).getID() ? 1 : (this.getID()<((Drug)o).getID() ? -1 : 0));
  }
}
