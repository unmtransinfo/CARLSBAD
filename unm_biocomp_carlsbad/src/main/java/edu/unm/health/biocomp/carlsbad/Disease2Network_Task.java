package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*;
import java.sql.*;
import java.util.concurrent.*;

import edu.unm.health.biocomp.util.threads.*;
import edu.unm.health.biocomp.util.db.*;
 
/**	CARLSBAD "one-click" subnet extraction via threaded Callable task.
	@author Jeremy J Yang
	@param	kid	KEGG ID for disease
*/
public class Disease2Network_Task
	implements Callable<Boolean>
{
  private String dbhost;
  private Integer dbport;
  private String dbid;
  private String dbusr;
  private String dbpw;
  private String fout_path_rgt;
  private String fout_path_rgtp;
  private String fout_path;
  private String fout_cpd_path;
  private HashMap<String,Integer> counts;
  private String errtxt;
  private String title;
  private Integer n_max_a;
  private String kid;
  private Float scaf_min;
  private Integer n_max_c;
  private ArrayList<Integer> tids;
  private CompoundList cpdlist;
  private CCPList ccplist;
  private ArrayList<String> sqls;
  private int n_total;
  private int n_done;
  public TaskStatus taskstatus;
  private java.util.Date t0;
  public Disease2Network_Task(
	String _dbhost,
	Integer _dbport,
	String _dbid,
	String _dbusr,
	String _dbpw,
	String _fout_path_rgt,
	String _fout_path_rgtp,
	String _fout_path,
	String _fout_cpd_path,
	String _kid,
	Float _scaf_min,
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
    this.fout_path_rgt=_fout_path_rgt;
    this.fout_path_rgtp=_fout_path_rgtp;
    this.fout_path=_fout_path;
    this.fout_cpd_path=_fout_cpd_path;
    this.kid=_kid;
    this.scaf_min=_scaf_min;
    this.title=_title;
    this.n_max_a=_n_max_a;
    this.n_max_c=_n_max_c;
    this.sqls=_sqls;
    this.tids=_tids;
    this.cpdlist=_cpdlist;
    this.ccplist=_ccplist;
    this.taskstatus=new Status(this);
    this.n_total=0;
    this.n_done=0;
    this.t0 = new java.util.Date();
    this.counts=null;
    this.errtxt="";
  }
  /////////////////////////////////////////////////////////////////////////
  public synchronized HashMap<String,Integer> getCounts() { return this.counts; }
  public synchronized String getErrtxt() { return this.errtxt; }
  public synchronized Boolean call()
  {
    try {
      DBCon dbcon = new DBCon("postgres",this.dbhost,this.dbport,this.dbid,this.dbusr,this.dbpw);
      File fout_rgt = new File(fout_path_rgt);
      fout_rgt.createNewFile();
      fout_rgt.setWritable(true, true);
      File fout_rgtp = new File(fout_path_rgtp);
      fout_rgtp.createNewFile();
      fout_rgtp.setWritable(true, true);
      File fout = new File(fout_path);
      fout.createNewFile();
      fout.setWritable(true, true);
      File fout_cpd = new File(fout_cpd_path);
      fout_cpd.createNewFile();
      fout_cpd.setWritable(true, true);
      this.counts=carlsbadone_utils.Disease2Network(
	dbcon,
	fout_rgt,
	fout_rgtp,
	fout,
	fout_cpd,
	this.kid,
	this.scaf_min,
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
      System.err.println("ERROR: Disease2Network_Task SQLException "+this.errtxt);
      return false;
    }
    catch (IOException e)
    {
      this.errtxt=e.getMessage();
      System.err.println("ERROR: Disease2Network_Task IOException "+this.errtxt);
      return false;
    }
    catch (Exception e)
    {
      this.errtxt=e.getMessage();
      System.err.println("ERROR: Disease2Network_Task Exception "+this.errtxt);
      return false;
    }
    return true;
  }
  class Status implements TaskStatus
  {
    private Disease2Network_Task task;
    public Status(Disease2Network_Task task) { this.task=task; }
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
