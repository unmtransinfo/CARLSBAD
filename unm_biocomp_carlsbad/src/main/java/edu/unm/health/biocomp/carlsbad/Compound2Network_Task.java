package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*;
import java.sql.*;
import java.util.concurrent.*;

import edu.unm.health.biocomp.util.threads.*;
import edu.unm.health.biocomp.util.db.*;
 
/**	CARLSBAD "one-click" subnet extraction via threaded Callable task.
	@author Jeremy J Yang
*/
public class Compound2Network_Task
	implements Callable<Boolean>
{
  private String dbhost;
  private Integer dbport;
  private String dbid;
  private String dbusr;
  private String dbpw;
  private String kgtype; //"rgt", "rgtp", or "full"
  private String fout_path;
  private String fout_cpd_path;
  private HashMap<String,Integer> counts;
  private String errtxt;
  private String title;
  private Integer n_max_a;
  private Integer cid;
  private Float scaf_min;
  private Boolean act_filter;
  private Integer n_max_c;
  private ArrayList<Integer> tids;
  private CompoundList cpdlist;
  private CCPList ccplist;
  private ArrayList<String> sqls;
  private int n_total;
  private int n_done;
  public TaskStatus taskstatus;
  private java.util.Date t0;
  public Compound2Network_Task(
	String _dbhost,
	Integer _dbport,
	String _dbid,
	String _dbusr,
	String _dbpw,
	String _kgtype,
	String _fout_path,
	String _fout_cpd_path,
	Integer _cid,
	Float _scaf_min,
	Boolean _act_filter,
	String _title,
	Integer _n_max_a,
	Integer _n_max_c,
	ArrayList<Integer> _tids,
	CompoundList _cpdlist,
	CCPList _ccplist,
	ArrayList<String> _sqls)
  {
    this.dbhost=_dbhost;
    this.dbport=_dbport;
    this.dbid=_dbid;
    this.dbusr=_dbusr;
    this.dbpw=_dbpw;
    this.kgtype=_kgtype;
    this.fout_path=_fout_path;
    this.fout_cpd_path=_fout_cpd_path;
    this.cid=_cid;
    this.scaf_min=_scaf_min;
    this.act_filter=_act_filter;
    this.taskstatus=new Status(this);
    this.n_total=0;
    this.n_done=0;
    this.t0 = new java.util.Date();
    this.counts=null;
    this.errtxt="";
    this.title=_title;
    this.n_max_a=_n_max_a;
    this.n_max_c=_n_max_c;
    this.tids=_tids;
    this.cpdlist=_cpdlist;
    this.ccplist=_ccplist;
    this.sqls=_sqls;
  }
  /////////////////////////////////////////////////////////////////////////
  public synchronized HashMap<String,Integer> getCounts() { return this.counts; }
  public synchronized String getErrtxt() { return this.errtxt; }
  public synchronized Boolean call()
  {
    try {
      DBCon dbcon = new DBCon("postgres",this.dbhost,this.dbport,this.dbid,this.dbusr,this.dbpw);
      File fout = new File(fout_path);
      fout.createNewFile();
      fout.setWritable(true, true);
      File fout_cpd = new File(fout_cpd_path);
      fout_cpd.createNewFile();
      fout_cpd.setWritable(true, true);
      this.counts=carlsbadone_utils.Compound2Network(
	dbcon,
	this.kgtype,
	fout,
	fout_cpd,
	this.cid,
	this.scaf_min,
	this.act_filter,
	this.title,
	this.n_max_a,
	this.n_max_c,
	this.tids,
	this.cpdlist,
	this.ccplist,
	this.sqls);
      dbcon.close();
    }
    catch (SQLException e)
    {
      this.errtxt=e.getMessage();
      System.err.println("ERROR: Compound2Network_Task SQLException "+this.errtxt);
      return false;
    }
    catch (IOException e)
    {
      this.errtxt=e.getMessage();
      System.err.println("ERROR: Compound2Network_Task IOException "+this.errtxt);
      return false;
    }
    catch (Exception e)
    {
      this.errtxt=e.getMessage();
      System.err.println("ERROR: Compound2Network_Task Exception "+this.errtxt);
      return false;
    }
    return true;
  }
  class Status implements TaskStatus
  {
    private Compound2Network_Task task;
    public Status(Compound2Network_Task task) { this.task=task; }
    public String status()
    {
      long t=(new java.util.Date()).getTime()-t0.getTime();
      int m=(int)(t/60000L);
      int s=(int)((t/1000L)%60L);
      String statstr=("["+String.format("%02d:%02d",m,s)+"]");
      if (task.n_done>0)
        statstr+=(String.format(" %5d;",task.n_done));
      if (task.n_total>0)
        statstr+=(String.format(" %.0f%%",100.0f*task.n_done/task.n_total));
      return statstr;
    }
  }
}
