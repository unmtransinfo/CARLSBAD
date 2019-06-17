package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*;
import java.sql.*;
import java.util.concurrent.*;

import edu.unm.health.biocomp.util.threads.*;
import edu.unm.health.biocomp.util.db.*;
 
/**	CARLSBAD subnet extraction via threaded Callable task.
	@author Jeremy J Yang
*/
public class Extract2XGMML_Task
	implements Callable<Boolean>
{
  private String dbhost;
  private Integer dbport;
  private String dbid;
  private String dbusr;
  private String dbpw;
  private String fout_path;
  private String qcpd;
  private String matchtype_qcpd;
  private Float minsim;
  private String cname;
  private Boolean matchtype_cname_sub;
  private Integer mw_min;
  private Integer mw_max;
  private ArrayList<Integer> cids;
  private ArrayList<Integer> scafids;
  private ArrayList<Integer> mcesids;
  private Boolean neighbortargets;
  private ArrayList<Integer> tids;
  private String cpd_id;
  private String cpd_idtype;
  private HashMap<String,Integer> counts;
  private String errtxt;
  private String title;
  private Integer n_max_a;
  private Integer n_max_c;
  private ArrayList<String> sqls;
  private int n_total;
  private int n_done;
  public TaskStatus taskstatus;
  private java.util.Date t0;
  public Extract2XGMML_Task(
	String dbhost,
	Integer dbport,
	String dbid,
	String dbusr,
	String dbpw,
	String fout_path,
	ArrayList<Integer> tids,
	String cpd_id,String cpd_idtype,
	String qcpd,
	String matchtype_qcpd,
	Float minsim,
	String cname,
	Boolean matchtype_cname_sub,
	Integer mw_min,
	Integer mw_max,
	ArrayList<Integer> cids,
	ArrayList<Integer> scafids,
	ArrayList<Integer> mcesids,
	Boolean neighbortargets,
	String title,
	Integer n_max_a,
	Integer n_max_c,
	ArrayList<String> sqls)
  {
    this.dbhost=dbhost;
    this.dbport=dbport;
    this.dbid=dbid;
    this.dbusr=dbusr;
    this.dbpw=dbpw;
    this.fout_path=fout_path;
    this.tids=new ArrayList<Integer>(tids);
    this.cpd_id=cpd_id;
    this.cpd_idtype=cpd_idtype;
    this.qcpd=qcpd;
    this.matchtype_qcpd=matchtype_qcpd;
    this.minsim=minsim;
    this.cname=cname;
    this.matchtype_cname_sub=matchtype_cname_sub;
    this.mw_min=mw_min;
    this.mw_max=mw_max;
    this.cids=cids;
    this.scafids=scafids;
    this.mcesids=mcesids;
    this.neighbortargets=neighbortargets;
    this.taskstatus=new Status(this);
    this.n_total=0;
    this.n_done=0;
    this.t0 = new java.util.Date();
    this.counts=null;
    this.errtxt="";
    this.title=title;
    this.n_max_a=n_max_a;
    this.n_max_c=n_max_c;
    this.sqls=sqls;
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
      fout.setWritable(true,true);
      this.counts=carlsbad_utils.Extract2XGMML(
	dbcon,
	fout,
	this.tids,
	this.cpd_id,
	this.cpd_idtype,
	this.qcpd,
	this.matchtype_qcpd,
	this.minsim,
	this.cname,
	this.matchtype_cname_sub,
	this.mw_min,
	this.mw_max,
	this.cids,
	this.scafids,
	this.mcesids,
	this.neighbortargets,
	this.title,
	this.n_max_a,
	this.n_max_c,
	this.sqls);
      dbcon.close();
    }
    catch (SQLException e)
    {
      this.errtxt=e.getMessage();
      System.err.println("ERROR: Extract2XGMML_Task SQLException "+this.errtxt);
      return false;
    }
    catch (IOException e)
    {
      this.errtxt=e.getMessage();
      System.err.println("ERROR: Extract2XGMML_Task IOException "+this.errtxt);
      return false;
    }
    catch (Exception e)
    {
      this.errtxt=e.getMessage();
      System.err.println("ERROR: Extract2XGMML_Task Exception "+this.errtxt);
      return false;
    }
    return true;
  }
  class Status implements TaskStatus
  {
    private Extract2XGMML_Task task;
    public Status(Extract2XGMML_Task task) { this.task=task; }
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
