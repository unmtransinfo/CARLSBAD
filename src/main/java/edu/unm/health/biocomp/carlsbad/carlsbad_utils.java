package edu.unm.health.biocomp.carlsbad;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;

import chemaxon.struc.*;
import chemaxon.formats.*;
import chemaxon.marvin.io.*;

import edu.unm.health.biocomp.util.http.*;
import edu.unm.health.biocomp.db.*;
import edu.unm.health.biocomp.smarts.smarts_utils;
import edu.unm.health.biocomp.cytoscape.*;

/**	Static utility methods for CARLSBAD database system.
	<br>
	Uses PostgreSQL JDBC driver (org.postgresql.Driver).
	For chemical cartrige gNova Chord DB.
	<br>
	Issue: Cytoscape 2.8.1 fails to import string values containing unescaped &lt; or &gt;
	So, we HTM escape these fields.
	<br>
	Issue: When searching by substance synonym (e.g. drug name "adderall"), it may be there are 
	other stereoisomer substances (same compound, same smiles, different iso_smiles) which are not
	included.
	<br>
	Issue: to add scaffold weighting, perhaps AtomCount(scaf)/AtomCount(mol).
	alpha-char-count is close enough (ignoring non-alpha chars in smiles).
	<br>
	@author Jeremy J Yang
*/
public class carlsbad_utils
{
  private static String outlinecolor="#666666";
  private static String fillcolor="#FFFFFF";
  private static String cpd_color="#DD8888";
  private static String drug_color="#FF0000";
  private static String tgt_color="#00FFFF";
  private static String scaf_color="#8888FF";
  private static String mces_color="#8888FF";
  //private static String target_classifier_type="Uniprot Family";
  private static String target_classifier_type="ChEMBL Class";

  /////////////////////////////////////////////////////////////////////////////
  /**	Return text with DB description information.
  */
  public static String DBDescribeTxt(DBCon dbcon)
	throws SQLException
  {
    String txt="";
    DatabaseMetaData meta = dbcon.getConnection().getMetaData();
    txt+=(meta.getDatabaseProductName()+" "+meta.getDatabaseMajorVersion()+"."+meta.getDatabaseMinorVersion()+"\n");
    txt+=(meta.getDriverName()+" "+meta.getDriverVersion()+"\n");
    ResultSet rset=dbcon.executeSql("SELECT name,version,to_char(load_date,'YYYY-MM-DD') AS load_date FROM dataset");
    while (rset.next())
      txt+=(rset.getString(1)+" version "+rset.getString(2)+" (loaded "+rset.getString(3)+")\n");
    rset=dbcon.executeSql("SELECT count(*) FROM compound");
    if (rset.next())
      txt+=("total CARLSBAD compounds: "+rset.getString(1)+"\n");
    rset.getStatement().close();
    rset=dbcon.executeSql("SELECT count(*) FROM target");
    if (rset.next())
      txt+=("total CARLSBAD targets: "+rset.getString(1)+"\n");
    rset.getStatement().close();
    rset=dbcon.executeSql("SELECT count(*) FROM cbactivity");
    if (rset.next())
      txt+=("total CARLSBAD activities: "+rset.getString(1)+"\n");
    rset.getStatement().close();
    rset=dbcon.executeSql("SELECT count(*) FROM scaffold");
    if (rset.next())
      txt+=("total CARLSBAD scaffolds: "+rset.getString(1)+"\n");
    rset.getStatement().close();
    rset=dbcon.executeSql("SELECT count(*) FROM mces");
    if (rset.next())
      txt+=("total CARLSBAD mces clusters: "+rset.getString(1)+"\n");
    rset.getStatement().close();
    rset=dbcon.executeSql("SELECT count(*) FROM kegg_disease");
    if (rset.next())
      txt+=("total CARLSBAD diseases: "+rset.getString(1)+"\n");
    rset.getStatement().close();
    return txt;
  }
  /////////////////////////////////////////////////////////////////////////////
  /** Return result set of all targets, multiple fields.
  */
  public static ResultSet GetTargets(DBCon dbcon)
      throws SQLException
  {
    String sql="SELECT target.id AS tid,target.name AS tname,target.species,target.type AS ttype,target.descr,identifier.id_type,identifier.id FROM target LEFT OUTER JOIN identifier ON (identifier.target_id=target.id)";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /** Return result set of one target, multiple fields.
  */
  public static ResultSet GetTarget(DBCon dbcon,int tid)
      throws SQLException
  {
    String sql="SELECT target.id AS tid,target.name AS tname,target.species,target.type AS ttype,target.descr,identifier.id_type,identifier.id FROM target LEFT OUTER JOIN identifier ON (identifier.target_id=target.id) WHERE target.id="+tid;
    ResultSet rset = dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /** Return result set of all targets with compound counts.
  */
  public static ResultSet GetTargetCompoundCounts(DBCon dbcon)
      throws SQLException
  {
    String sql="SELECT target.id AS tid,count(compound.id) AS cpd_count FROM target JOIN cbactivity ON (cbactivity.target_id=target.id) JOIN substance ON (substance.id=cbactivity.substance_id) JOIN s2c ON (s2c.substance_id=substance.id) JOIN compound ON (compound.id=s2c.compound_id) WHERE s2c.is_active GROUP BY target.id";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /** Return compound count for given target set.
  */
  public static Integer GetTargetsCompoundCount(Collection<Integer> tids,DBCon dbcon)
      throws SQLException
  {
    String sql="SELECT count(compound.id) AS cpd_count FROM target JOIN cbactivity ON (cbactivity.target_id=target.id) JOIN substance ON (substance.id=cbactivity.substance_id) JOIN s2c ON (s2c.substance_id=substance.id) JOIN compound ON (compound.id=s2c.compound_id) WHERE s2c.is_active AND target.id IN (";
    int i=0;
    for (int tid: tids) { sql+=(""+tid+(++i<tids.size()?",":"")); }
    sql+=")";
    ResultSet rset=dbcon.executeSql(sql);
    rset.next();
    return (rset.getInt("cpd_count"));
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set of all diseases, with associated targets, multiple fields.
	Currently these are all Kegg diseases, linked via Uniprots and NCBI GIs.
	Returned columns: kid, disease_name, tid (where kid = Kegg disease ID).
	Problem: a few targets (e.g. 159) have no (cb)activities.
  */
  public static ResultSet GetDiseases(DBCon dbcon)
      throws SQLException
  {
    String sql="SELECT DISTINCT kegg_disease.id AS kid,kegg_disease.name,target.id AS tid FROM kegg_disease JOIN target_classifier ON (kegg_disease.id=target_classifier.id) JOIN target ON (target_classifier.target_id=target.id) WHERE target_classifier.type='KEGG Disease' ORDER BY kegg_disease.id";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set for one disease, with associated targets, for specified KID.
  */
  public static ResultSet GetDisease(DBCon dbcon,String kid)
      throws SQLException
  {
    String sql="SELECT DISTINCT kegg_disease.id AS kid,kegg_disease.name,target.id AS tid FROM kegg_disease JOIN target_classifier ON (kegg_disease.id=target_classifier.id) JOIN target ON (target_classifier.target_id=target.id) WHERE target_classifier.type='KEGG Disease' AND kegg_disease.id='"+kid+"'";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set of all drugs, all synonyms.
	Returned columns: cid, smiles, drug_name.
  */
  public static ResultSet GetDrugs(DBCon dbcon)
	throws SQLException
  {
    String sql="SELECT DISTINCT compound.id AS cid,compound.smiles,synonym.name AS drug_name FROM compound JOIN s2c ON (s2c.compound_id=compound.id) JOIN substance ON (substance.id=s2c.substance_id) LEFT OUTER JOIN synonym ON (substance.id=synonym.substance_id) WHERE substance.is_drug AND s2c.is_active ORDER BY compound.id,synonym.name";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with targets for specified compound.
	Returned columns: tid, tname, species, atype, avalue, confidence
  */
  public static ResultSet GetCompoundTargets(DBCon dbcon,int cid,Boolean human)
	throws SQLException
  {
    String sql;
    if (human)
      sql="SELECT DISTINCT target.id AS tid,target.name AS tname,target.species,cbactivity.type AS atype,cbactivity.value AS avalue,cbactivity.confidence  FROM target JOIN cbactivity ON (target.id=cbactivity.target_id) JOIN substance ON (cbactivity.substance_id=substance.id) JOIN s2c ON (substance.id=s2c.substance_id) JOIN compound ON (s2c.compound_id=compound.id) WHERE target.species='human' AND compound.id="+cid+" AND s2c.is_active ORDER BY target.id";
    else
      sql="SELECT DISTINCT target.id AS tid,target.name AS tname,target.species,cbactivity.type AS atype,cbactivity.value AS avalue,cbactivity.confidence  FROM target JOIN cbactivity ON (target.id=cbactivity.target_id) JOIN substance ON (cbactivity.substance_id=substance.id) JOIN s2c ON (substance.id=s2c.substance_id) JOIN compound ON (s2c.compound_id=compound.id) WHERE compound.id="+cid+" AND s2c.is_active ORDER BY target.id";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with targets for specified compounds.
	Returned columns: tid, tname, species, atype, avalue, confidence
  */
  public static ResultSet GetCompoundsTargets(DBCon dbcon,HashSet<Integer> cids,Boolean human)
	throws SQLException
  {
    String sql;
    if (human)
      sql="SELECT DISTINCT target.id AS tid,target.name AS tname,target.species,cbactivity.type AS atype,cbactivity.value AS avalue,cbactivity.confidence  FROM target JOIN cbactivity ON (target.id=cbactivity.target_id) JOIN substance ON (cbactivity.substance_id=substance.id) JOIN s2c ON (substance.id=s2c.substance_id) JOIN compound ON (s2c.compound_id=compound.id) WHERE s2c.is_active AND target.species='human' AND compound.id IN (";
    else
      sql="SELECT DISTINCT target.id AS tid,target.name AS tname,target.species,cbactivity.type AS atype,cbactivity.value AS avalue,cbactivity.confidence  FROM target JOIN cbactivity ON (target.id=cbactivity.target_id) JOIN substance ON (cbactivity.substance_id=substance.id) JOIN s2c ON (substance.id=s2c.substance_id) JOIN compound ON (s2c.compound_id=compound.id) WHERE s2c.is_active AND compound.id IN (";
    int i=0;
    for (int cid: cids) { sql+=(""+cid+(++i<cids.size()?",":"")); }
    sql+=")";
    sql+=(" ORDER BY target.id");
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set for specified scaffold.
	Returned columns: id,smiles,natoms
  */
  public static ResultSet GetScaffold(DBCon dbcon,int scafid)
	throws SQLException
  {
    String sql="SELECT id,smiles,natoms FROM scaffold WHERE scaffold.id="+scafid;
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with compounds for specified scaffold.
	Returned columns: cid,smiles
  */
  public static ResultSet GetScaffoldCompounds(DBCon dbcon,int scafid)
	throws SQLException
  {
    String sql="SELECT DISTINCT compound.id AS cid,compound.smiles FROM compound,scafid2cid,scaffold WHERE scafid2cid.compound_id = compound.id AND scaffold.id = scafid2cid.scaffold_id AND scaffold.id="+scafid+" ORDER BY cid";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set for specified mces.
	Returned columns: id,smarts
  */
  public static ResultSet GetMCES(DBCon dbcon,int mcesid)
	throws SQLException
  {
    String sql="SELECT id,mces AS smarts FROM mces WHERE mces.id="+mcesid;
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with compounds for specified mces.
	Returned columns: cid,smiles
  */
  public static ResultSet GetMCESCompounds(DBCon dbcon,int mcesid)
	throws SQLException
  {
    String sql="SELECT DISTINCT compound.id AS cid,compound.smiles FROM compound WHERE cluster_id = "+mcesid+" ORDER BY cid";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with scaffolds for specified compound.
	Returned columns: scafid, scafsmi, natoms, is_largest.
	1st should be largest scaffold.
  */
  public static ResultSet GetCompoundScaffolds(DBCon dbcon,int cid)
	throws SQLException
  {
    String sql="SELECT DISTINCT scaffold.id AS scafid, scaffold.smiles AS scafsmi, scaffold.natoms, scafid2cid.is_largest FROM scaffold, scafid2cid, compound WHERE scaffold.id = scafid2cid.scaffold_id AND scafid2cid.compound_id = compound.id AND compound.id="+cid+" ORDER BY natoms DESC";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with largest (Bemis-Murko) scaffold for specified compound.
	Returned columns: scafid, scafsmi, natoms.
  */
  public static ResultSet GetCompoundBMScaffold(DBCon dbcon,int cid)
	throws SQLException
  {
    String sql="SELECT DISTINCT scaffold.id AS scafid, scaffold.smiles AS scafsmi, scaffold.natoms FROM scaffold, scafid2cid, compound WHERE scaffold.id = scafid2cid.scaffold_id AND scafid2cid.compound_id = compound.id AND scafid2cid.is_largest AND compound.id="+cid;
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with MCES for specified compound.
	Returned columns: mcesid, mcessma
  */
  public static ResultSet GetCompoundMces(DBCon dbcon,int cid)
	throws SQLException
  {
    String sql="SELECT DISTINCT mces.id AS mcesid,mces.mces AS mcessma FROM mces,compound WHERE compound.cluster_id=mces.id AND compound.id="+cid;
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with data for specified compound.
	Returned columns: smiles,mol_weight,mol_formula,mcesid,iupac_name,is_drug,synonym
  */
  public static ResultSet GetCompound(DBCon dbcon,int cid)
	throws SQLException
  {
    String sql="SELECT DISTINCT compound.iso_smiles AS smiles,compound.mol_weight,compound.mol_formula,compound.nass_tested,compound.nass_active,compound.nsam_tested,compound.nsam_active,compound.cluster_id AS mcesid,substance.iupac_name,substance.is_drug,synonym.name AS synonym FROM compound JOIN s2c ON (s2c.compound_id=compound.id) JOIN substance ON (substance.id=s2c.substance_id) LEFT OUTER JOIN synonym ON (substance.id=synonym.substance_id) WHERE s2c.is_active AND compound.id="+cid;
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with data for specified compounds.
	Returned columns: cid,smiles,mol_weight,mol_formula,mcesid,iupac_name,is_drug,synonym
  */
  public static ResultSet GetCompounds(DBCon dbcon,HashSet<Integer> cids)
	throws SQLException
  {
    String sql="SELECT DISTINCT compound.id AS cid,compound.iso_smiles AS smiles,compound.mol_weight,compound.mol_formula,compound.nass_tested,compound.nass_active,compound.nsam_tested,compound.nsam_active,compound.cluster_id AS mcesid,substance.iupac_name,substance.is_drug,synonym.name AS synonym FROM compound JOIN s2c ON (s2c.compound_id=compound.id) JOIN substance ON (substance.id=s2c.substance_id) LEFT OUTER JOIN synonym ON (substance.id=synonym.substance_id) WHERE s2c.is_active AND compound.id IN (";
    int i=0;
    for (int cid: cids) { sql+=(""+cid+(++i<cids.size()?",":"")); }
    sql+=")";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with data for specified scaffolds.
	Returned columns: id,smiles,natoms
  */
  public static ResultSet GetScaffolds(DBCon dbcon,HashSet<Integer> scafids)
	throws SQLException
  {
    String sql="SELECT DISTINCT id,smiles,natoms from scaffold WHERE id IN (";
    int i=0;
    for (int scafid: scafids) { sql+=(""+scafid+(++i<scafids.size()?",":"")); }
    sql+=")";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with data for specified mcess.
	Returned columns: id,smarts
  */
  public static ResultSet GetMCESs(DBCon dbcon,HashSet<Integer> mcesids)
	throws SQLException
  {
    String sql="SELECT DISTINCT id,mces AS smarts FROM mces WHERE id IN (";
    int i=0;
    for (int mcesid: mcesids) { sql+=(""+mcesid+(++i<mcesids.size()?",":"")); }
    sql+=")";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return result set with data for specified compounds.
	Returned columns: cid,id_type,id
  */
  public static ResultSet GetCompoundsIDs(DBCon dbcon,HashSet<Integer> cids)
	throws SQLException
  {
    String sql="SELECT DISTINCT compound.id AS cid, identifier.id_type, identifier.id FROM compound JOIN s2c ON (s2c.compound_id=compound.id) JOIN substance ON (substance.id=s2c.substance_id) LEFT OUTER JOIN identifier ON (identifier.substance_id=s2c.substance_id) WHERE s2c.is_active AND compound.id IN (";
    int i=0;
    for (int cid: cids) { sql+=(""+cid+(++i<cids.size()?",":"")); }
    sql+=")";
    ResultSet rset=dbcon.executeSql(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return Kegg disease name for given Kegg ID.
  */
  public static String GetKIDName(DBCon dbcon,String kid)
      throws SQLException
  {
    String sql="SELECT kegg_disease.name FROM kegg_disease WHERE kegg_disease.id='"+kid+"'";
    ResultSet rset=dbcon.executeSql(sql);
    rset.next();
    String name=(rset.getString("name").replaceFirst("\\s+$",""));
    return name;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return list of all associated targets (TIDs), for given KID (Kegg ID).
  */
  public static ArrayList<Integer> ListDiseaseTargets(DBCon dbcon,String kid)
      throws SQLException
  {
    ArrayList<Integer> tids = new ArrayList<Integer>();
    String sql="SELECT DISTINCT target.id AS tid FROM target JOIN target_classifier ON (target_classifier.target_id=target.id) JOIN kegg_disease ON (kegg_disease.id=target_classifier.id) WHERE target_classifier.id = '"+kid+"' AND target_classifier.type='KEGG Disease'";
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next()) tids.add(rset.getInt("tid"));
    return tids;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Return list of all species.
  */
  public static ArrayList<String> ListTargetSpecies(DBCon dbcon)
      throws SQLException
  {
    ArrayList<String> specieses = new ArrayList<String>();
    String sql="SELECT DISTINCT target.species FROM target";
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next())
      specieses.add(rset.getString("species"));
    return specieses;
  }
  /////////////////////////////////////////////////////////////////////////////
  /** Return list of all target ID types.
  */
  public static ArrayList<String> ListTargetIDTypes(DBCon dbcon)
      throws SQLException
  {
    ArrayList<String> idtypes = new ArrayList<String>();
    String sql="SELECT DISTINCT identifier.id_type FROM identifier WHERE identifier.target_id IS NOT NULL";
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next())
      idtypes.add(rset.getString("id_type"));
    return idtypes;
  }
  /////////////////////////////////////////////////////////////////////////////
  /** Return list of all compound (substance) ID types.
  */
  public static ArrayList<String> ListSubstanceIDTypes(DBCon dbcon)
      throws SQLException
  {
    ArrayList<String> idtypes = new ArrayList<String>();
    String sql="SELECT DISTINCT identifier.id_type FROM identifier WHERE identifier.substance_id IS NOT NULL";
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next())
      idtypes.add(rset.getString("id_type"));
    return idtypes;
  }
  /////////////////////////////////////////////////////////////////////////////
  /** Return list of all target types.
  */
  public static ArrayList<String> ListTargetTypes(DBCon dbcon)
      throws SQLException
  {
    ArrayList<String> ttypes = new ArrayList<String>();
    String sql="SELECT DISTINCT target.type FROM target";
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next())
      ttypes.add(rset.getString("type"));
    return ttypes;
  }
  /////////////////////////////////////////////////////////////////////////////
  /** Return list of all target classes.
  */
  public static ArrayList<String> ListTargetClasses(DBCon dbcon)
      throws SQLException
  {
    ArrayList<String> tclasses = new ArrayList<String>();
    String sql="SELECT DISTINCT id FROM target_classifier WHERE type='"+target_classifier_type+"'";
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next())
      tclasses.add(rset.getString(1));
    return tclasses;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	
  */
  public static String CID2Smiles(DBCon dbcon,int cid)
      throws SQLException
  {
    String smiles=null;
    String sql=("SELECT\n"
    +"\tcompound.iso_smiles AS smiles\n"
    +"FROM\n"
    +"\tcompound\n"
    +"WHERE\n"
    +"\tcompound.id = "+cid);
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next())	//smiles
      smiles=rset.getString("smiles");
    return smiles;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Compose target query SQL.  Return null if no query.
	Query FOR targets using target-params and/or compound-params.
	If we go with this target classification scheme, should return
	all target classifications (overlapping) as we do target ids.
  */
  public static String TargetQuerySQL(
	List<Integer> tids,
	String ttype,
	ArrayList<String> tclasses,
	String tname,
	Boolean matchtype_tname_sub,
	String tdesc,
	Boolean matchtype_tdesc_sub,
	String species,
	String tgt_id,
	String tgt_idtype,
	String sbs_id,
	String sbs_idtype,
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
	Integer n_max_t
	)
  {
    ArrayList<String> wheres = new ArrayList<String>();
    ArrayList<String> wheres_tgt = new ArrayList<String>();
    if (tids!=null && tids.size()>0)
    {
      String where=("target.id IN (");
      for (int i=0;i<tids.size();++i)
      {
        where+=(((i==0)?"":",")+tids.get(i));
      }
      where+=")";
      wheres_tgt.add(where); //expect tgt_idtype=CARLSBAD
    }
    else
    {
      if (tgt_id!=null && !tgt_id.isEmpty())
      {
        if (tgt_idtype!=null && !tgt_idtype.isEmpty() && tgt_idtype.equals("any") && tgt_id.matches("^[0-9]*$"))
        {
          wheres.add("(identifier_tgt.id='"+tgt_id+"' OR target.id="+tgt_id+")");
        }
        else
        {
          wheres.add("identifier_tgt.id='"+tgt_id+"'");
          if (tgt_idtype!=null && !tgt_idtype.isEmpty() && !tgt_idtype.equals("any"))
            wheres.add("identifier_tgt.id_type='"+tgt_idtype+"'");
        }
      }
    }
    if (tname!=null && !tname.isEmpty())
      wheres_tgt.add(matchtype_tname_sub?"target.name ILIKE '%"+tname+"%'":"LOWER(target.name)=LOWER('"+tname+"')");
    if (tdesc!=null && !tdesc.isEmpty())
      wheres_tgt.add(matchtype_tdesc_sub?"target.descr ILIKE '%"+tdesc+"%'":"LOWER(target.descr)=LOWER('"+tdesc+"')");
    if (species!=null && !species.isEmpty() && !species.equals("any"))
      wheres_tgt.add("target.species='"+species+"'");
    if (ttype!=null && !ttype.isEmpty() && !ttype.equals("any"))
      wheres_tgt.add("target.type='"+ttype+"'");
    if (tclasses!=null && tclasses.size()>0)
    {
      wheres.add("target_classifier.type='"+target_classifier_type+"'");
      String where=("target_classifier.id IN (");
      for (int i=0;i<tclasses.size();++i)
      {
        String tclass=tclasses.get(i);
        tclass=tclass.replaceAll("'","''");
        where+=(((i==0)?"":",")+"'"+tclass+"'");
      }
      where+=")";
      wheres.add(where);
    }
    ArrayList<String> wheres_cpd = new ArrayList<String>();
    if (qcpd!=null && !qcpd.isEmpty())
    {
      if (matchtype_qcpd!=null && matchtype_qcpd.equalsIgnoreCase("sim") && minsim!=null)
      {
        wheres_cpd.add("gnova.tanimoto(gnova.fp('"+qcpd+"'),compound.gfp)>"+minsim);
      }
      else if (matchtype_qcpd!=null && matchtype_qcpd.equalsIgnoreCase("exa"))
      {
        wheres_cpd.add("compound.gfp=gnova.fp('"+qcpd+"')"); //fp-screen for speed
        wheres_cpd.add("compound.smiles=gnova.cansmiles('"+qcpd+"')");
      }
      else if (matchtype_qcpd!=null && matchtype_qcpd.equalsIgnoreCase("sub"))
      {
        // If query is valid smiles, parse as such and aromatize, else handle as smarts.
        try {
          Molecule mol = MolImporter.importMol(qcpd,"smiles:d");
          if (mol.isQuery() || qcpd.matches(".*[&,;~].*$") || qcpd.matches(".*#[0-9].*$"))
          {
            wheres_cpd.add("gnova.matches(compound.smiles,'"+qcpd+"')");
          }
          else
          {
            String qsmi=mol.exportToFormat("smiles:+a");
            wheres_cpd.add("gnova.bit_contains(compound.gfp,gnova.fp('"+qsmi+"'))"); //fp-screen for speed
            wheres_cpd.add("gnova.matches(compound.smiles,'"+qsmi+"')");
          }
        }
        catch (Exception e) {
          wheres_cpd.add("gnova.matches(compound.smiles,'"+qcpd+"')");
        }
      }
    }
    if (cname!=null && !cname.isEmpty())
    {
      wheres_cpd.add(matchtype_cname_sub?"synonym.name ILIKE '%"+cname+"%'":"LOWER(synonym.name)=LOWER('"+cname+"')");
    }
    if (mw_min!=null)
      wheres_cpd.add("compound.mol_weight>"+mw_min);
    if (mw_max!=null)
      wheres_cpd.add("compound.mol_weight<"+mw_max);

    if (cids!=null)
    {
      String where="compound.id IN (";
      for (int i=0;i<cids.size();++i)
        where+=(((i==0)?"":",")+cids.get(i));
      where+=")";
      wheres_cpd.add(where);
    }
    if (sbs_id!=null && !sbs_id.isEmpty())
    {
      if (sbs_idtype!=null && !sbs_idtype.isEmpty() && sbs_idtype.equals("CARLSBAD"))
      {
        // Handled by (cids)
      }
      else if (sbs_idtype!=null && !sbs_idtype.isEmpty() && sbs_idtype.equals("any") && sbs_id.matches("^[0-9]*$"))
      {
        wheres_cpd.add("(identifier_sbs.id='"+sbs_id+"' OR substance.id="+sbs_id+" OR compound.id="+sbs_id+")");
      }
      else
      {
        wheres_cpd.add("identifier_sbs.id='"+sbs_id+"'");
        if (sbs_idtype!=null && !sbs_idtype.isEmpty() && !sbs_idtype.equals("any"))
          wheres_cpd.add("identifier_sbs.id_type='"+sbs_idtype+"'");
      }
    }
    if (scafids!=null)
    {
      String where="scaffold.id IN (";
      for (int i=0;i<scafids.size();++i)
        where+=(((i==0)?"":",")+scafids.get(i));
      where+=")";
      wheres_cpd.add(where);
    }
    if (mcesids!=null)
    {
      String where="compound.cluster_id IN (";
      for (int i=0;i<mcesids.size();++i)
        where+=(((i==0)?"":",")+mcesids.get(i));
      where+=")";
      wheres_cpd.add(where);
    }

    String sql="SELECT DISTINCT\n"
      +"\ttarget.id AS tid,\n"
      +"\ttarget.name,\n"
      +"\ttarget.type,\n"
      +"\ttarget.species,\n"
      +"\ttarget.descr,\n";
    sql+=
      "\tidentifier_tgt.id_type AS tgt_id_type,\n"
      +"\tidentifier_tgt.id AS tgt_id\n"
      +"FROM\n"
      +"\ttarget\n"
      +"LEFT OUTER JOIN\n"
      +"\tidentifier AS identifier_tgt ON (identifier_tgt.target_id=target.id)\n";

    String subsql="\t\tSELECT DISTINCT\n"
      +"\t\t\ttarget.id\n"
      +"\t\tFROM\n"
      +"\t\t\ttarget\n"
      +"\t\tLEFT OUTER JOIN\n"
      +"\t\t\tidentifier AS identifier_tgt ON (identifier_tgt.target_id=target.id)\n";
    if (wheres_tgt.size()>0)
    {
      wheres.addAll(wheres_tgt);
    }
    if (wheres_cpd.size()>0)
    {
      wheres.addAll(wheres_cpd);
      wheres.add("s2c.is_active");
      subsql+=("\t\tJOIN\n"
        +"\t\t\tcbactivity ON (target.id=cbactivity.target_id)\n"
        +"\t\tJOIN\n"
        +"\t\t\tsubstance ON (cbactivity.substance_id=substance.id)\n"
        +"\t\tJOIN\n"
        +"\t\t\ts2c ON (substance.id=s2c.substance_id)\n"
        +"\t\tJOIN\n"
        +"\t\t\tcompound ON (s2c.compound_id=compound.id)\n");
      if (cname!=null && !cname.isEmpty())
        subsql+=("\t\tLEFT OUTER JOIN\n"
          +"\t\t\tsynonym ON (synonym.substance_id=substance.id)\n");
      if (sbs_id!=null && !sbs_id.isEmpty())
        subsql+=("\t\tJOIN\n"
          +"\t\t\tidentifier AS identifier_sbs ON (identifier_sbs.substance_id=substance.id)\n");
      if (scafids!=null)
        subsql+=("\t\tJOIN\n"
          +"\t\t\tscafid2cid ON (compound.id=scafid2cid.compound_id)\n"
          +"\t\tJOIN\n"
          +"\t\t\tscaffold ON (scafid2cid.scaffold_id=scaffold.id)\n");
    }
    if (wheres.size()==0) return null;
    if (tclasses!=null && tclasses.size()>0)
    {
      //THIS NEEDS TO BE SEPARATE QUERY OR WE GET CARTESIAN PRODUCT W/ ID JOIN.
      subsql+=("\t\tLEFT OUTER JOIN\n"
        +"\t\t\ttarget_classifier ON (target_classifier.target_id=target.id)\n");
    }
    subsql+="\t\tWHERE\n";
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
      subsql+="\t\t\t"+((i>0)?"AND ":"")+wheres.get(i)+"\n";
    if (n_max_t!=null) subsql+=("\t\tLIMIT "+n_max_t);
    sql+=(
      "WHERE\n"
      +"\ttarget.id IN (\n"
      +subsql+"\n"
      +"\t)\n"
    );
    sql+=("ORDER BY target.id,target.name ASC\n");
    return sql;
  }
  public static String TargetQuerySQL(List<Integer> tids)
  {
    return TargetQuerySQL(
	tids, null, null, null, null, null, null, null, null, null,
	null, null, null, null, null, null, null, null, null, null,
	null, null, null);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Given a set of targets and compound query criteria, extract subnet comprised of targets,
	compounds, scaffolds, mces, and activities.
	NOTE: XGMML attribute types: string,real,integer,boolean,list,map,complex
	TO DO:
	  [x] target synonyms must be in lists, since multiple PDBs, Uniprots, etc may exist.
	  [ ] adjustable limits on tids, cids or act_ids.
	<br/>
	Note that no HTML escape codes of the form &amp;foo; are permitted in the
	XGMML file (e.g. by Cytoscape 2.8.2) and will result in a fatal  error on import.

	@param dbcon	database connection
	@param fout	XGMML output file
	@param tids	target IDs specified
	@param sbs_id_query	substance ID
	@param sbs_idtype_query	substance ID type
	@param qcpd	compound query SMILES/SMARTS
	@param matchtype_qcpd	compound query type (sim|exa|sub)
	@param minsim	similarity threshold
	@param cname	compound name
	@param matchtype_cname_sub	compound name query as substring
	@param mw_min	min molecular weight
	@param mw_max	max molecular weight
	@param cids_query	compound IDs specified
	@param scafids_query	scaffold IDs specified
	@param mcesids_query	MCES IDs specified
	@param neighbortargets	Should targets associated with subnet compounds also be included?
	@param title	title of subnet
	@param n_max_a	max number of activities
	@param n_max_c	max number of compounds
	@param sqls	return param: SQLs used
  */
  public static HashMap<String,Integer> Extract2XGMML(DBCon dbcon,File fout,
	ArrayList<Integer> tids,
	String sbs_id_query,String sbs_idtype_query,
	String qcpd, String matchtype_qcpd,Float minsim,
	String cname,boolean matchtype_cname_sub,
	Integer mw_min,Integer mw_max,
	ArrayList<Integer> cids_query,
	ArrayList<Integer> scafids_query,
	ArrayList<Integer> mcesids_query,
	Boolean neighbortargets,
	String title,
	Integer n_max_a,Integer n_max_c,
	ArrayList<String> sqls)
	throws Exception
  {
    HashMap<String,Integer> counts = new HashMap<String,Integer>(); //for return counts
    PrintWriter fout_writer=new PrintWriter(new BufferedWriter(new FileWriter(fout,false))); //overwrite
    ArrayList<String> wheres = new ArrayList<String>();

    if (tids.isEmpty())
      throw new Exception("Target IDs must be specified.");
    if (title==null || title.isEmpty()) title="CARLSBAD Target-Compound Sub-Network";
    fout_writer.printf(cytoscape_utils.XGMML_Header(title,"#CCDDFF"));
  
    /// Targets:
    String sql=TargetIDs2SQL(tids,wheres);
    sqls.add(sql);
    ResultSet rset=dbcon.executeSql(sql);
    HashMap<Integer,HashMap<String,String> > tgtdata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,HashMap<String,Boolean> > > tgt_tgt_ids = new HashMap<Integer,HashMap<String,HashMap<String,Boolean> > >();
    ReadTargetData(rset,tgtdata,tgt_tgt_ids);
    HashMap<Integer,HashSet<Integer> > t2c_global = new HashMap<Integer,HashSet<Integer> >();
    for (int tid: tgtdata.keySet()) t2c_global.put(tid,null);
    Targets2Compounds(t2c_global,dbcon); //Get global-degree data (to all compounds, not just subnet).
    WriteTargets(tgtdata,t2c_global,tgt_tgt_ids,counts,fout_writer);

    /// Activities and Compounds:
    /// From this search, create CID list for subsequent search filtering.
    sql=ActivityCompoundsSQL(sbs_id_query,sbs_idtype_query,qcpd,matchtype_qcpd,minsim,cname,matchtype_cname_sub,
	mw_min,mw_max,cids_query,scafids_query,mcesids_query,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);

    /// Populating HashMaps with activity and compound data.
    HashMap<Integer,HashMap<String,String> > actdata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,String> > cpddata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,Boolean> > cpdsynonyms = new HashMap<Integer,HashMap<String,Boolean> >();
    HashMap<Integer,HashMap<String,HashSet<String> > > cpd_sbs_ids = new HashMap<Integer,HashMap<String,HashSet<String> > >();
    ReadCompoundData(rset,actdata,cpddata,cpdsynonyms,cpd_sbs_ids);
    HashMap<Integer,HashSet<Integer> > c2t_global = new HashMap<Integer,HashSet<Integer> >();
    for (int cid: cpddata.keySet()) c2t_global.put(cid,null);
    Compounds2Targets(c2t_global,dbcon,true); //Get global-degree data. (human only)
    WriteCompounds(cpddata,c2t_global,cpd_sbs_ids,actdata,cpdsynonyms,n_max_c,n_max_a,counts,fout_writer);
    
    /// Scaffolds:
    sql=ScaffoldSQL(sbs_id_query,scafids_query,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);
    HashMap<String,HashMap<String,String> > scafdata = new HashMap<String,HashMap<String,String> >();
    HashSet<Integer> scafids_visited = new HashSet<Integer>();
    ReadScaffoldData(rset,cpddata,scafdata,scafids_visited);
    HashMap<Integer,HashSet<Integer> > s2c_global = new HashMap<Integer,HashSet<Integer> >();
    HashMap<Integer,HashSet<Integer> > s2t_global = new HashMap<Integer,HashSet<Integer> >();
    for (int scafid: scafids_visited) s2c_global.put(scafid,null);
    for (int scafid: scafids_visited) s2t_global.put(scafid,null);
    scafids_visited.clear();
    Scaffolds2Compounds(s2c_global,dbcon); //Get global-degree data.
    Scaffolds2Targets(s2t_global,dbcon); //Get global-degree data.
    WriteScaffolds(scafdata,scafids_visited,s2c_global,s2t_global,counts,fout_writer);

    /// MCESs:
    sql=McesSQL(sbs_id_query,mcesids_query,wheres);
    sqls.add(sql);
    rset=dbcon.executeSql(sql);
    HashSet<Integer> mcesids_visited = new HashSet<Integer>();
    HashMap<String,HashMap<String,String> > mcesdata = new HashMap<String,HashMap<String,String> >();
    ReadMcesData(rset,cpddata,mcesdata,mcesids_visited);
    HashMap<Integer,HashSet<Integer> > m2c_global = new HashMap<Integer,HashSet<Integer> >();
    HashMap<Integer,HashSet<Integer> > m2t_global = new HashMap<Integer,HashSet<Integer> >();
    for (int mcesid: mcesids_visited) m2c_global.put(mcesid,null);
    for (int mcesid: mcesids_visited) m2t_global.put(mcesid,null);
    mcesids_visited.clear();
    Mcess2Compounds(m2c_global,dbcon);
    Mcess2Targets(m2t_global,dbcon);
    WriteMcess(mcesdata,mcesids_visited,m2c_global,m2t_global,counts,fout_writer);

    /// Neighbor targets (ALL targets for which subnet cpds active):
    if (neighbortargets)
    {
      sql=NeighborSQL(cpddata,scafids_query,mcesids_query);
      sqls.add(sql);
      rset=dbcon.executeSql(sql);
      tgtdata.clear();
      tgt_tgt_ids.clear();
      actdata.clear();
      ReadNeighborData(rset,tids,tgtdata,tgt_tgt_ids,actdata);
      t2c_global.clear();
      for (int tid: tgtdata.keySet()) t2c_global.put(tid,null);
      Targets2Compounds(t2c_global,dbcon); //Get global-degree data.
      int n_node_tgt_neigh=WriteTargets(tgtdata,t2c_global,tgt_tgt_ids,counts,fout_writer);
      int n_edge_act_neigh=WriteActivityEdges(actdata,null,null,n_max_a,counts,fout_writer);
      counts.put("n_node_tgt_neigh",n_node_tgt_neigh);
      counts.put("n_edge_act_neigh",n_edge_act_neigh);
    }

    fout_writer.printf(cytoscape_utils.XGMML_Footer());
    fout_writer.close();
    return counts;
  }

  /////////////////////////////////////////////////////////////////////////////
  public static ArrayList<Integer> TargetExternalIDs2TIDs(Collection<String> ids,String id_type,DBCon dbcon)
	throws SQLException
  {
    ArrayList<String> wheres = new ArrayList<String>();
    String sql=carlsbad_utils.TargetExternalIDs2SQL(ids,id_type,wheres);
    ResultSet rset=dbcon.executeSql(sql);
    HashMap<Integer,HashMap<String,String> > tgtdata = new HashMap<Integer,HashMap<String,String> >();
    HashMap<Integer,HashMap<String,HashMap<String,Boolean> > > tgt_tgt_ids = new HashMap<Integer,HashMap<String,HashMap<String,Boolean> > >();
    carlsbad_utils.ReadTargetData(rset,tgtdata,tgt_tgt_ids);

    ArrayList<Integer> tids = new ArrayList<Integer>();
    tids.addAll(tgtdata.keySet());

    return tids;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Compose SQL to get target data for specified external IDs,
	e.g. "UniProt", "NCBI gi", etc.
	@param wheres	query logic propagated here
  */
  public static String TargetExternalIDs2SQL(Collection<String> ids,String id_type,
	ArrayList<String> wheres)
  {
    if (ids==null || ids.size()==0) return null;
    String sql="SELECT DISTINCT\n"
	+"\ttarget.id AS tid,\n"
	+"\ttarget.name AS tname,\n"
	+"\ttarget.descr,\n"
	+"\ttarget.species,\n"
	+"\ttarget.type,\n"
	+"\tidentifier.id_type AS tgt_id_type,\n"
	+"\tidentifier.id AS tgt_id\n"
	+"FROM\n"
	+"\ttarget,\n"
	+"\tidentifier\n";
    String where="identifier.target_id=target.id";
    wheres.add(where);
    where="identifier.id_type='"+id_type+"'";
    wheres.add(where);
    where="identifier.id IN (";
    //for (int i=0;i<ids.size();++i)
    int ii=0;
    for (String id: ids)
    {
      where+=(((ii==0)?"":",")+("'"+id+"'"));
      ++ii;
    }
    where+=")";
    wheres.add(where);
    sql+=("WHERE\n");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
      sql+="\t"+((i>0)?"AND ":"")+wheres.get(i)+"\n";
    sql+=("ORDER BY target.id,target.name ASC\n");
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Compose SQL to get target data for specified TIDs.
	@param wheres	query logic propagated here
  */
  public static String TargetIDs2SQL(ArrayList<Integer> tids,
	ArrayList<String> wheres)
  {
    String sql="SELECT DISTINCT\n"
	+"\ttarget.id AS tid,\n"
	+"\ttarget.name AS tname,\n"
	+"\ttarget.descr,\n"
	+"\ttarget.species,\n"
	+"\ttarget.type,\n"
	+"\tidentifier.id_type AS tgt_id_type,\n"
	+"\tidentifier.id AS tgt_id\n"
	+"FROM\n"
	+"\ttarget\n"
	+"LEFT OUTER JOIN\n"
	+"\tidentifier ON (identifier.target_id=target.id)\n";
    String where="target.id IN (";
    for (int i=0;i<tids.size();++i)
      where+=(((i==0)?"":",")+tids.get(i));
    where+=")";
    wheres.add(where);
    sql+=("WHERE\n");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
      sql+="\t"+((i>0)?"AND ":"")+wheres.get(i)+"\n";
    sql+=("ORDER BY target.id,target.name ASC\n");
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Compose SQL to get target data for specified CIDs.
	That is, for the given compound[s], what are active targets.
	@param wheres	query logic propagated here
  */
  public static String Compounds2TargetsSQL(ArrayList<Integer> cids,
	ArrayList<String> wheres,Boolean human)
  {
    if (human) wheres.add("target.species='human'");
    String sql="SELECT DISTINCT\n"
	+"\ttarget.id AS tid,\n"
	+"\ttarget.name AS tname,\n"
	+"\ttarget.descr,\n"
	+"\ttarget.species,\n"
	+"\ttarget.type,\n"
	+"\tidentifier.id_type AS tgt_id_type,\n"
	+"\tidentifier.id AS tgt_id\n"
	+"FROM\n"
	+"\ttarget\n"
	+"LEFT OUTER JOIN\n"
	+"\tidentifier ON (identifier.target_id=target.id)\n"
        +"\t\tJOIN\n"
        +"\t\t\tcbactivity ON (target.id=cbactivity.target_id)\n"
        +"\t\tJOIN\n"
        +"\t\t\tsubstance ON (cbactivity.substance_id=substance.id)\n"
        +"\t\tJOIN\n"
        +"\t\t\ts2c ON (substance.id=s2c.substance_id)\n"
        +"\t\tJOIN\n"
        +"\t\t\tcompound ON (s2c.compound_id=compound.id)\n";
    String where="compound.id IN (";
    for (int i=0;i<cids.size();++i)
      where+=(((i==0)?"":",")+cids.get(i));
    where+=")";
    wheres.add(where);
    wheres.add("s2c.is_active");
    sql+=("WHERE\n");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
      sql+="\t"+((i>0)?"AND ":"")+wheres.get(i)+"\n";
    sql+=("ORDER BY target.id,target.name ASC\n");
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Read target data from ResultSet.
  */
  public static int ReadTargetData(
	ResultSet rset,
	HashMap<Integer,HashMap<String,String> > tgtdata,
	HashMap<Integer,HashMap<String,HashMap<String,Boolean> > > tgt_tgt_ids)
	throws SQLException
  {
    int n_row=0;
    while (rset.next())	//tid,name,descr,species,type,tgt_id_type,tgt_id
    {
      ++n_row;
      Integer tid=rset.getInt("tid");
      if (!tgtdata.containsKey(tid))
      {
        tgtdata.put(tid,new HashMap<String,String>());
        String tname=rset.getString("tname").replaceAll("\"",""); //kludge
        tgtdata.get(tid).put("tname",tname);
        tgtdata.get(tid).put("descr",rset.getString("descr"));
        tgtdata.get(tid).put("species",rset.getString("species"));
        tgtdata.get(tid).put("type",rset.getString("type"));
      }
      if (rset.getString("tgt_id_type")!=null && rset.getString("tgt_id")!=null)
      {
        if (!tgt_tgt_ids.containsKey(tid)) tgt_tgt_ids.put(tid,new HashMap<String,HashMap<String,Boolean> >());
        if (!tgt_tgt_ids.get(tid).containsKey(rset.getString("tgt_id_type")))
          tgt_tgt_ids.get(tid).put(rset.getString("tgt_id_type"),new HashMap<String,Boolean>());
        if (!tgt_tgt_ids.get(tid).get(rset.getString("tgt_id_type")).containsKey(rset.getString("tgt_id")))
        {
          String tgt_id=rset.getString("tgt_id").trim();
          if (!tgt_id.isEmpty())
            tgt_tgt_ids.get(tid).get(rset.getString("tgt_id_type")).put(tgt_id,true);
        }
      }
    }
    return n_row;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Compose SQL to get activities and compounds data.
	@param wheres	query logic propagated here
  */
  public static String ActivityCompoundsSQL(
	String sbs_id_query,String sbs_idtype_query,
	String qcpd, String matchtype_qcpd,Float minsim,
	String cname,Boolean matchtype_cname_sub,
	Integer mw_min,Integer mw_max,
	ArrayList<Integer> cids_query,
	ArrayList<Integer> scafids_query,
	ArrayList<Integer> mcesids_query,
	ArrayList<String> wheres)
  {
    String where="";
    String sql="SELECT DISTINCT\n"
    +"\tcbactivity.id AS act_id,\n"
    +"\tcbactivity.target_id AS tid,\n"
    +"\tcbactivity.substance_id AS sid,\n"
    +"\tcbactivity.type AS act_type,\n"
    +"\tcbactivity.value AS act_value_std,\n"
    +"\tcbactivity.confidence,\n"
    +"\ttarget.name AS tname,\n"
    +"\tcompound.id AS cid,\n"
    +"\tcompound.iso_smiles AS smiles,\n"
    +"\tcompound.mol_weight,\n"
    +"\tsubstance.is_drug,\n"
    +"\tsynonym.name AS csynonym,\n"
    +"\tidentifier.id_type AS sbs_id_type,\n"
    +"\tidentifier.id AS sbs_id\n"
    +"FROM\n"
    +"\tcbactivity\n"
    +"JOIN\n"
    +"\ttarget ON (target.id=cbactivity.target_id)\n"
    +"JOIN\n"
    +"\tsubstance ON (substance.id=cbactivity.substance_id)\n"
    +"JOIN\n"
    +"\ts2c ON (substance.id=s2c.substance_id)\n"
    +"JOIN\n"
    +"\tcompound ON (compound.id=s2c.compound_id)\n"
    +"LEFT OUTER JOIN\n"
    +"\tidentifier ON (identifier.substance_id=substance.id)\n"
    +"LEFT OUTER JOIN\n"
    +"\tsynonym ON (synonym.substance_id=substance.id)\n";
    wheres.add("s2c.is_active");
    if (qcpd!=null && !qcpd.isEmpty())
    {
      if (matchtype_qcpd!=null && matchtype_qcpd.equalsIgnoreCase("sim") && minsim!=null)
      {
        wheres.add("gnova.tanimoto(gnova.fp('"+qcpd+"'),compound.gfp)>"+minsim);
      }
      else if (matchtype_qcpd!=null && matchtype_qcpd.equalsIgnoreCase("exa"))
      {
        wheres.add("compound.gfp=gnova.fp('"+qcpd+"')"); //fp-screen for speed
        wheres.add("compound.smiles=gnova.cansmiles('"+qcpd+"')");
      }
      else if (matchtype_qcpd!=null && matchtype_qcpd.equalsIgnoreCase("sub"))
      {
        // If query is valid smiles, parse as such and aromatize, else handle as smarts.
        try {
          Molecule mol = MolImporter.importMol(qcpd,"smiles:");
          if (mol.isQuery() || qcpd.matches(".*[&,;~].*$") || qcpd.matches(".*#[0-9].*$"))
          {
            wheres.add("gnova.matches(compound.smiles,'"+qcpd+"')");
          }
          else
          {
            String qsmi=mol.exportToFormat("smiles:+a");
            wheres.add("gnova.bit_contains(compound.gfp,gnova.fp('"+qsmi+"'))"); //fp-screen for speed
            wheres.add("gnova.matches(compound.smiles,'"+qsmi+"')");
          }
        }
        catch (Exception e) {
          wheres.add("gnova.matches(compound.smiles,'"+qcpd+"')");
        }
      }
    }
    if (cname!=null && !cname.isEmpty())
      wheres.add(matchtype_cname_sub?"synonym.name ILIKE '%"+cname+"%'":"LOWER(synonym.name)=LOWER('"+cname+"')");
    if (mw_min!=null)
      wheres.add("compound.mol_weight>"+mw_min);
    if (mw_max!=null)
      wheres.add("compound.mol_weight<"+mw_max);

    if (cids_query!=null)
    {
      where="compound.id IN (";
      for (int i=0;i<cids_query.size();++i)
        where+=(((i==0)?"":",")+cids_query.get(i));
      where+=")";
      wheres.add(where);
    }
    if (sbs_id_query!=null && !sbs_id_query.isEmpty())
    {
      if (sbs_idtype_query!=null && !sbs_idtype_query.isEmpty() && sbs_idtype_query.equals("CARLSBAD"))
      {
        // Handled by (cids)
      }
      else if (sbs_idtype_query!=null && !sbs_idtype_query.isEmpty() && sbs_idtype_query.equals("any") && sbs_id_query.matches("^[0-9]*$"))
      {
        wheres.add("(identifier_sbs.id='"+sbs_id_query+"' OR substance.id="+sbs_id_query+" OR compound.id="+sbs_id_query+")");
      }
      else
      {
        wheres.add("identifier_sbs.id='"+sbs_id_query+"'");
        if (sbs_idtype_query!=null && !sbs_idtype_query.isEmpty() && !sbs_idtype_query.equals("any"))
          wheres.add("identifier_sbs.id_type='"+sbs_idtype_query+"'");
      }
      sql+=("JOIN\n"
        +"\tidentifier AS identifier_sbs ON (identifier_sbs.substance_id=substance.id)\n");
    }
    if (scafids_query!=null)
    {
      sql+=("\t\tJOIN\n"
        +"\t\t\tscafid2cid ON (compound.id=scafid2cid.compound_id)\n"
        +"\t\tJOIN\n"
        +"\t\t\tscaffold ON (scafid2cid.scaffold_id=scaffold.id)\n");
      where="scaffold.id IN (";
      for (int i=0;i<scafids_query.size();++i)
        where+=(((i==0)?"":",")+scafids_query.get(i));
      where+=")";
      wheres.add(where);
    }
    if (mcesids_query!=null)
    {
      where="compound.cluster_id IN (";
      for (int i=0;i<mcesids_query.size();++i)
        where+=(((i==0)?"":",")+mcesids_query.get(i));
      where+=")";
      wheres.add(where);
    }
    sql+=("WHERE");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)  //cpd-query logic propagated here
    {
      sql+=("\n\t");
      if (i>0) sql+=("AND ");
      sql+=(wheres.get(i));
    }
    sql+=("\nORDER BY cbactivity.id,compound.id,synonym.name ASC");
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Compose SQL to get activities and compounds data.
	@param wheres	query logic propagated here
  */
    public static String CompoundIDs2SQL(
	ArrayList<Integer> cids_query,
	ArrayList<String> wheres)
  {
    String sql="SELECT DISTINCT\n"
    +"\tcompound.id AS cid,\n"
    +"\tcompound.iso_smiles AS smiles,\n"
    +"\tcompound.mol_weight,\n"
    +"\tsubstance.is_drug,\n"
    +"\tsynonym.name AS csynonym,\n"
    +"\tidentifier.id_type AS sbs_id_type,\n"
    +"\tidentifier.id AS sbs_id\n"
    +"FROM\n"
    +"\tcompound\n"
    +"JOIN\n"
    +"\ts2c ON (s2c.compound_id=compound.id)\n"
    +"JOIN\n"
    +"\tsubstance ON (substance.id=s2c.substance_id)\n"
    +"LEFT OUTER JOIN\n"
    +"\tidentifier ON (identifier.substance_id=substance.id)\n"
    +"LEFT OUTER JOIN\n"
    +"\tsynonym ON (synonym.substance_id=substance.id)\n";
    String where="compound.id IN (";
    for (int i=0;i<cids_query.size();++i)
      where+=(((i==0)?"":",")+cids_query.get(i));
    where+=")";
    wheres.add(where);
    wheres.add("s2c.is_active");
    sql+=("WHERE\n");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
      sql+="\t"+((i>0)?"AND ":"")+wheres.get(i)+"\n";
    sql+=("ORDER BY compound.id\n");
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Read [activity +] compound data from ResultSet into data structures.
	Returns number of new compounds read.
  */
  public static int ReadCompoundData(
	ResultSet rset,
	HashMap<Integer,HashMap<String,String> > actdata,
	HashMap<Integer,HashMap<String,String> > cpddata,
	HashMap<Integer,HashMap<String,Boolean> > cpdsynonyms,
	HashMap<Integer,HashMap<String,HashSet<String> > > cpd_sbs_ids)
	throws SQLException
  {
    int n_row=0;
    int n_new_cpd=0;
    while (rset.next())
    {
      ++n_row;
      Integer cid=null;
      try { cid=rset.getInt("cid"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }

      if (actdata!=null)
      {
        Integer act_id=null;
        try { act_id=rset.getInt("act_id"); }
        catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
        Integer tid=null;
        try { tid=rset.getInt("tid"); }
        catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
        // act_id:sid is many-to-one.  act_id:cid is many-to-many just like sid:cid.
        // However, by requiring s2c.is_active, act_id:cid becomes many-to-one.
        if (!actdata.containsKey(act_id))
        {
          actdata.put(act_id,new HashMap<String,String>());
          actdata.get(act_id).put("act_id",""+act_id);
          actdata.get(act_id).put("tid",""+tid);
          actdata.get(act_id).put("cid",""+cid);
          actdata.get(act_id).put("act_type",rset.getString("act_type"));
          actdata.get(act_id).put("act_value_std",rset.getString("act_value_std"));
          actdata.get(act_id).put("confidence",rset.getString("confidence"));
        }
      }
      if (!cpddata.containsKey(cid))
      {
        ++n_new_cpd;
        cpddata.put(cid,new HashMap<String,String>());
        cpddata.get(cid).put("smiles",rset.getString("smiles"));
        cpddata.get(cid).put("is_drug",rset.getString("is_drug"));
      }
      if (rset.getString("csynonym")!=null)
      {
        if (rset.getString("csynonym").matches("^[0-9]*$")) continue; //skip numbers
        if (!cpdsynonyms.containsKey(cid)) cpdsynonyms.put(cid,new HashMap<String,Boolean>());
        if (!cpdsynonyms.get(cid).containsKey(rset.getString("csynonym")))
        {
          String csynonym=rset.getString("csynonym");
          csynonym=csynonym.replaceAll("\"",""); //kludge
          cpdsynonyms.get(cid).put(csynonym,true);
        }
      }
      if (rset.getString("sbs_id_type")!=null && rset.getString("sbs_id")!=null)
      {
        if (!cpd_sbs_ids.containsKey(cid)) cpd_sbs_ids.put(cid,new HashMap<String,HashSet<String> >());
        if (!cpd_sbs_ids.get(cid).containsKey(rset.getString("sbs_id_type")))
          cpd_sbs_ids.get(cid).put(rset.getString("sbs_id_type"),new HashSet<String>());
        if (!cpd_sbs_ids.get(cid).get(rset.getString("sbs_id_type")).contains(rset.getString("sbs_id")))
        {
          String sbs_id=rset.getString("sbs_id").trim();
          if (!sbs_id.isEmpty())
            cpd_sbs_ids.get(cid).get(rset.getString("sbs_id_type")).add(sbs_id);
        }
      }
    }
    return n_new_cpd;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Scaffold retreival SQL.
	ResultSet should include: scaf_id,scafsmi,cid,molsmi
	@param wheres	query logic propagated here
  */
  public static String ScaffoldSQL(String sbs_id_query,
	ArrayList<Integer> scafids_query,
	ArrayList<String> wheres)
  {
    String where="";
    String sql="SELECT DISTINCT\n"
    +"\tscaffold.id AS scaf_id,\n"
    +"\tscaffold.smiles AS scafsmi,\n"
    +"\tcompound.id AS cid,\n"
    +"\tcompound.smiles AS molsmi\n"
    +"FROM\n"
    +"\ttarget\n"
    +"JOIN\n"
    +"\tcbactivity ON (target.id=cbactivity.target_id)\n"
    +"JOIN\n"
    +"\tsubstance ON (substance.id=cbactivity.substance_id)\n"
    +"JOIN\n"
    +"\ts2c ON (substance.id=s2c.substance_id)\n"
    +"JOIN\n"
    +"\tcompound ON (compound.id=s2c.compound_id)\n"
    +"JOIN\n"
    +"\tscafid2cid ON (compound.id=scafid2cid.compound_id)\n"
    +"JOIN\n"
    +"\tscaffold ON (scaffold.id=scafid2cid.scaffold_id)\n"
    +"LEFT OUTER JOIN\n"
    +"\tsynonym ON (synonym.substance_id=substance.id)\n";
    if (sbs_id_query!=null && !sbs_id_query.isEmpty())
    {
      sql+=("JOIN\n"
        +"\tidentifier AS identifier_sbs ON (identifier_sbs.substance_id=substance.id)\n");
    }
    if (scafids_query!=null && !scafids_query.isEmpty())
    {
      sql+=("JOIN\n"
        +"\tscafid2cid ON (compound.id=scafid2cid.compound_id)\n");
      sql+=("JOIN\n"
        +"\tscaffold ON (scafid2cid.scaffold_id=scaffold.id)\n");
    }
    sql+=("WHERE");
    wheres.add("scafid2cid.is_largest");
    wheres.add("s2c.is_active");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
    {
      sql+=("\n\t");
      if (i>0) sql+=("AND ");
      sql+=(wheres.get(i));
    }
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	MCES retreival SQL.
	ResultSet should include: cluster_id,mces,cid
	@param wheres	query logic propagated here
  */
  public static String McesSQL(String sbs_id_query,
	ArrayList<Integer> mcesids_query,
	ArrayList<String> wheres)
  {
    String where="";
    String sql="SELECT DISTINCT\n"
    +"\tcompound.cluster_id,\n"
    +"\tmces.mces,\n"
    +"\tcompound.id AS cid\n"
    +"FROM\n"
    +"\ttarget\n"
    +"LEFT OUTER JOIN\n"
    +"\tcbactivity ON (target.id=cbactivity.target_id)\n"
    +"LEFT OUTER JOIN\n"
    +"\tsubstance ON (substance.id=cbactivity.substance_id)\n"
    +"JOIN\n"
    +"\ts2c ON (substance.id=s2c.substance_id)\n"
    +"JOIN\n"
    +"\tcompound ON (compound.id=s2c.compound_id)\n"
    +"JOIN\n"
    +"\tmces ON (compound.cluster_id=mces.id)\n"
    +"JOIN\n"
    +"\tscafid2cid ON (compound.id=scafid2cid.compound_id)\n"
    +"LEFT OUTER JOIN\n"
    +"\tsynonym ON (synonym.substance_id=substance.id)\n";
    if (sbs_id_query!=null && !sbs_id_query.isEmpty())
    {
      sql+=("JOIN\n"
        +"\tidentifier AS identifier_sbs ON (identifier_sbs.substance_id=substance.id)\n");
    }
    if (mcesids_query!=null && !mcesids_query.isEmpty())
    {
      sql+=("JOIN\n"
        +"\tmces ON (compound.cluster_id=mces.id)\n");
    }
    sql+=("WHERE");
    wheres.add("s2c.is_active");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
    {
      sql+=("\n\t");
      if (i>0) sql+=("AND ");
      sql+=(wheres.get(i));
    }
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Scaffold retreival SQL.
  */
  public static String ScafedgeSQL(List<Integer> cids, ArrayList<Integer> scafids)
  {
    String sql="SELECT DISTINCT\n"
    +"\tscaffold.id AS scaf_id,\n"
    +"\tscaffold.smiles AS scafsmi,\n"
    +"\tcompound.id AS cid,\n"
    +"\tcompound.smiles AS molsmi\n"
    +"FROM\n"
    +"\tcompound\n"
    +"JOIN\n"
    +"\tscafid2cid ON (compound.id=scafid2cid.compound_id)\n"
    +"JOIN\n"
    +"\tscaffold ON (scaffold.id=scafid2cid.scaffold_id)\n";
    ArrayList<String> wheres = new ArrayList<String>();
    wheres.add("scafid2cid.is_largest");
    String where="scaffold.id IN (";
    for (int i=0;i<scafids.size();++i)
      where+=(((i==0)?"":",")+scafids.get(i));
    where+=")";
    wheres.add(where);
    where="compound.id IN (";
    for (int i=0;i<cids.size();++i)
      where+=(((i==0)?"":",")+cids.get(i));
    where+=")";
    wheres.add(where);
    sql+=("WHERE\n");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
      sql+="\t"+((i>0)?"AND ":"")+wheres.get(i)+"\n";
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Provisional function for weighting scaffold-molecule associations.
	Approximates heavy atom ratio via smiles for speed and simplicity.
  */
  public static Float S2C_Weight(String scafsmi,String molsmi)
  {
    String mstr=molsmi.replaceAll("[^a-zA-Z]","");
    String sstr=scafsmi.replaceAll("[^a-zA-Z]","");
    Float s2c_weight=0.0f;
    if (sstr.length()>0 && mstr.length()>=sstr.length())
      s2c_weight=((new Integer(sstr.length())).floatValue())/((new Integer(mstr.length())).floatValue());
    return s2c_weight;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Read scaffold data from ResultSet into data structures.
	ResultSet should include: scaf_id,scafsmi,cid,molsmi
  */
  public static int ReadScaffoldData(
	ResultSet rset,
	HashMap<Integer,HashMap<String,String> > cpddata,
	HashMap<String,HashMap<String,String> > scafdata,
	HashSet<Integer> scafids_visited)
	throws SQLException
  {
    int n_row=0;
    int n_new_scaf=0;
    while (rset.next()) //scaf_id,scafsmi,cid,molsmi
    {
      int scaf_id=0;
      try { scaf_id=rset.getInt("scaf_id"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
      int cid=0;
      try { cid=rset.getInt("cid"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
      String scafsmi="";
      try { scafsmi=rset.getString("scafsmi"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
      String molsmi="";
      try { molsmi=rset.getString("molsmi"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
      Float s2c_weight=S2C_Weight(scafsmi,molsmi);
      if (!cpddata.containsKey(cid)) continue;
      if (!scafids_visited.contains(scaf_id))
      {
        ++n_new_scaf;
        scafids_visited.add(scaf_id);
      }
      String edgeid="S"+scaf_id+"_C"+cid;
      scafdata.put(edgeid,new HashMap<String,String>());
      scafdata.get(edgeid).put("scaf_id",""+scaf_id);
      scafdata.get(edgeid).put("scafsmi",scafsmi);
      scafdata.get(edgeid).put("cid",""+cid);
      scafdata.get(edgeid).put("weight",String.format("%.2f",s2c_weight));
    }
    return n_new_scaf;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Filter scafdata removing edges linking by underweight scaffold
	associations.
  */
  public static int Filter_By_S2C_Weight(
	HashMap<String,HashMap<String,String> > scafdata,
	Float minweight)
  {
    if (minweight==0.0f) return 0;
    int n_edge_removed=0;
    ArrayList<String> edgeids = new ArrayList<String>(scafdata.keySet());
    for (String edgeid: edgeids)
    {
      try {
        Float weight=Float.parseFloat(scafdata.get(edgeid).get("weight"));
        if (weight<minweight)
        {
          scafdata.remove(edgeid);
          ++n_edge_removed;
        }
      }
      catch (Exception e) { System.err.println("ERROR: (Filter_By_S2C_Weight) "+e.getMessage()); continue; }
    }
    return n_edge_removed;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	MCES retreival SQL.
  */
  public static String McesedgeSQL(List<Integer> cids, ArrayList<Integer> mcesids)
  {
    String sql="SELECT DISTINCT\n"
    +"\tcompound.cluster_id,\n"
    +"\tmces.mces,\n"
    +"\tcompound.id AS cid,\n"
    +"\tcompound.smiles AS molsmi\n"
    +"FROM\n"
    +"\tcompound\n"
    +"JOIN\n"
    +"\tmces ON (compound.cluster_id=mces.id)\n";
    ArrayList<String> wheres = new ArrayList<String>();
    String where="mces.id IN (";
    for (int i=0;i<mcesids.size();++i)
      where+=(((i==0)?"":",")+mcesids.get(i));
    where+=")";
    wheres.add(where);
    where="compound.id IN (";
    for (int i=0;i<cids.size();++i)
      where+=(((i==0)?"":",")+cids.get(i));
    where+=")";
    wheres.add(where);
    sql+=("WHERE\n");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
      sql+="\t"+((i>0)?"AND ":"")+wheres.get(i)+"\n";
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Read MCES data from ResultSet into data structures.
	ResultSet should include: cluster_id,mces,cid
  */
  public static int ReadMcesData(
	ResultSet rset,
	HashMap<Integer,HashMap<String,String> > cpddata,
	HashMap<String,HashMap<String,String> > mcesdata,
	HashSet<Integer> mcesids_visited)
	throws SQLException
  {
    int n_row=0;
    int n_new_mces=0;
    while (rset.next()) //cluster_id,mces,cid
    {
      ++n_row;
      int cluster_id=0;
      try { cluster_id=rset.getInt("cluster_id"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
      int cid=0;
      try { cid=rset.getInt("cid"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
      if (!cpddata.containsKey(cid)) continue;  //cpd-query logic propagated here
      if (!mcesids_visited.contains(cluster_id))
      {
        ++n_new_mces;
        mcesids_visited.add(cluster_id);
      }
      String edgeid="M"+cluster_id+"_C"+cid;
      mcesdata.put(edgeid,new HashMap<String,String>());
      mcesdata.get(edgeid).put("cluster_id",""+cluster_id);
      mcesdata.get(edgeid).put("mces",rset.getString("mces")); //smarts
      mcesdata.get(edgeid).put("cid",""+cid);
    }
    return n_new_mces;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Remove targets from tgtdata not linked according to actdata.
  */
  public static int Remove_Orphan_Targets(
	HashMap<Integer,HashMap<String,String> > tgtdata,
	HashMap<Integer,HashMap<String,String> > actdata)
  {
    int n_tgt_removed=0;
    HashMap<Integer,Boolean> tgt_active = new HashMap<Integer,Boolean>();
    for (Integer act_id: actdata.keySet())
    {
      try {
        Integer tid=Integer.parseInt(actdata.get(act_id).get("tid"));
        tgt_active.put(tid,true);
      }
      catch (Exception e) { System.err.println("ERROR: (Remove_Orphan_Targets) "+e.getMessage()); continue; }
    }
    ArrayList<Integer> tids = new ArrayList<Integer>(tgtdata.keySet());
    for (Integer tid: tids)
    {
      if (!tgt_active.containsKey(tid))
      {
        tgtdata.remove(tid);
        ++n_tgt_removed;
      }
    }
    return n_tgt_removed;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Remove compounds from cpddata not linked according to actdata.
	But do not remove cpds also needed by scafdata or mcesdata!

  */
  public static int Remove_Orphan_Compounds(
	HashMap<Integer,HashMap<String,String> > cpddata,
	HashMap<String,HashMap<String,String> > scafdata,
	HashMap<String,HashMap<String,String> > mcesdata,
	HashMap<Integer,HashMap<String,String> > actdata)
  {
    HashSet<Integer> scaf_cids = new HashSet<Integer>();
    for (String edgeid: scafdata.keySet())
    {
      Integer cid = Integer.parseInt(scafdata.get(edgeid).get("cid"));
      scaf_cids.add(cid);
    }
    HashSet<Integer> mces_cids = new HashSet<Integer>();
    for (String edgeid: mcesdata.keySet())
    {
      Integer cid = Integer.parseInt(mcesdata.get(edgeid).get("cid"));
      mces_cids.add(cid);
    }
    int n_cpd_removed=0;
    HashSet<Integer> cpd_active = new HashSet<Integer>();
    for (Integer act_id: actdata.keySet())
    {
      try {
        Integer cid=Integer.parseInt(actdata.get(act_id).get("cid"));
        cpd_active.add(cid);
      }
      catch (Exception e) { System.err.println("ERROR: (Remove_Orphan_Compounds) "+e.getMessage()); continue; }
    }
    ArrayList<Integer> cids = new ArrayList<Integer>(cpddata.keySet());
    for (Integer cid: cids)
    {
      if (!cpd_active.contains(cid) && !scaf_cids.contains(cid) && !mces_cids.contains(cid))
      {
        cpddata.remove(cid);
        ++n_cpd_removed;
      }
    }
    return n_cpd_removed;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Remove compounds from cpddata not linked according to actdata.
  */
  public static int Remove_Dangling_Edges(
	HashMap<String,HashMap<String,String> > scafdata,
	HashMap<Integer,HashMap<String,String> > cpddata)
  {
    int n_edge_removed=0;

    ArrayList<String> edgeids = new ArrayList<String>(scafdata.keySet());
    Integer scaf_id=null;
    Integer cid=null;
    for (String edgeid: edgeids)
    {
      try {
      scaf_id=Integer.parseInt(scafdata.get(edgeid).get("scaf_id"));
      cid=Integer.parseInt(scafdata.get(edgeid).get("cid"));
      }
      catch (Exception e)
      { System.err.println("ERROR: (Remove_Dangling_Edges) "+e.getMessage()); continue; }
      if (!cpddata.containsKey(cid))
      {
        scafdata.remove(edgeid);
        ++n_edge_removed;
      }
    }
    return n_edge_removed;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Neighbor target SQL.
  */
  public static String NeighborSQL(
	HashMap<Integer,HashMap<String,String> > cpddata,
	ArrayList<Integer> scafids_query,
	ArrayList<Integer> mcesids_query)
  {
    String sql=("SELECT DISTINCT\n"
    +"\ttarget.id AS tid,\n"
    +"\ttarget.name AS tname,\n"
    +"\ttarget.descr,\n"
    +"\ttarget.species,\n"
    +"\ttarget.type,\n"
    +"\tidentifier.id_type AS tgt_id_type,\n"
    +"\tidentifier.id AS tgt_id,\n"
    +"\tsubstance.id AS sid,\n"
    +"\tcbactivity.id AS act_id,\n"
    +"\tcbactivity.type AS act_type,\n"
    +"\tcbactivity.value AS act_value_std,\n"
    +"\tcbactivity.confidence,\n"
    +"\tcompound.id AS cid\n"
    +"FROM\n"
    +"\ttarget\n"
    +"JOIN\n"
    +"\tcbactivity ON (target.id=cbactivity.target_id)\n"
    +"JOIN\n"
    +"\tsubstance ON (substance.id=cbactivity.substance_id)\n"
    +"JOIN\n"
    +"\ts2c ON (substance.id=s2c.substance_id)\n"
    +"JOIN\n"
    +"\tcompound ON (compound.id=s2c.compound_id)\n"
    +"LEFT OUTER JOIN\n"
    +"\tidentifier ON (identifier.target_id=target.id)\n");
    ArrayList<String> wheres = new ArrayList<String>();
    String where="";
    if (scafids_query!=null)
    {
      sql+=("\t\tJOIN\n"
        +"\t\t\tscafid2cid ON (compound.id=scafid2cid.compound_id)\n"
        +"\t\tJOIN\n"
        +"\t\t\tscaffold ON (scafid2cid.scaffold_id=scaffold.id)\n");
      where="scaffold.id IN (";
      for (int i=0;i<scafids_query.size();++i)
        where+=(((i==0)?"":",")+scafids_query.get(i));
      where+=")";
      wheres.add(where);
    }
    if (mcesids_query!=null)
    {
      where="compound.cluster_id IN (";
      for (int i=0;i<mcesids_query.size();++i)
        where+=(((i==0)?"":",")+mcesids_query.get(i));
      where+=")";
      wheres.add(where);
    }
    where="compound.id IN (";
    String cids_str="";
    for (Integer cid: cpddata.keySet())
      cids_str+=(((cids_str.length()>0)?",":"")+cid);
    where+=cids_str+")";
    wheres.add(where);

    sql+=("WHERE");
    wheres.add("s2c.is_active");
    wheres=UniquifyArrayList(wheres);
    for (int i=0;i<wheres.size();++i)
    {
      sql+=("\n\t");
      if (i>0) sql+=("AND ");
      sql+=(wheres.get(i));
    }
    sql+=("\nORDER BY target.id,target.name ASC");
    return sql;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Read neighbor data from ResultSet.
  */
  public static int ReadNeighborData(
	ResultSet rset,
	ArrayList<Integer> tids,
	HashMap<Integer,HashMap<String,String> > tgtdata,
	HashMap<Integer,HashMap<String,HashMap<String,Boolean> > > tgt_tgt_ids,
	HashMap<Integer,HashMap<String,String> > actdata)
      throws SQLException
  {
    int n_row=0;
    while (rset.next())
    {
      ++n_row;
      Integer tid=null;
      try { tid=rset.getInt("tid"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
      if (tids.indexOf(tid)>=0) continue;  //already included in subnet
      if (!tgtdata.containsKey(tid))
      {
        tgtdata.put(tid,new HashMap<String,String>());
        tgtdata.get(tid).put("tname",rset.getString("tname"));
        tgtdata.get(tid).put("descr",rset.getString("descr"));
        tgtdata.get(tid).put("species",rset.getString("species"));
        tgtdata.get(tid).put("type",rset.getString("type"));
      }
      if (rset.getString("tgt_id_type")!=null && rset.getString("tgt_id")!=null)
      {
        if (!tgt_tgt_ids.containsKey(tid)) tgt_tgt_ids.put(tid,new HashMap<String,HashMap<String,Boolean> >());
        if (!tgt_tgt_ids.get(tid).containsKey(rset.getString("tgt_id_type")))
          tgt_tgt_ids.get(tid).put(rset.getString("tgt_id_type"),new HashMap<String,Boolean>());
        if (!tgt_tgt_ids.get(tid).get(rset.getString("tgt_id_type")).containsKey(rset.getString("tgt_id")))
        {
          String tgt_id=rset.getString("tgt_id").trim();
          if (!tgt_id.isEmpty())
            tgt_tgt_ids.get(tid).get(rset.getString("tgt_id_type")).put(tgt_id,true);
        }
      }
      Integer cid=null;
      try { cid=rset.getInt("cid"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
      Integer act_id=null;
      try { act_id=rset.getInt("act_id"); }
      catch (Exception e) { System.err.println("error: "+e.getMessage()); continue; }
      if (!actdata.containsKey(act_id))
      {
        actdata.put(act_id,new HashMap<String,String>());
        actdata.get(act_id).put("act_id",""+act_id);
        actdata.get(act_id).put("tid",""+tid);
        actdata.get(act_id).put("cid",""+cid);
        actdata.get(act_id).put("act_type",rset.getString("act_type"));
        actdata.get(act_id).put("act_value_std",rset.getString("act_value_std"));
        actdata.get(act_id).put("confidence",rset.getString("confidence"));
      }
    }
    return n_row;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For each TID key in passed argument hash, populate with all associated
	compound IDs.
  */
  public static int Targets2Compounds(HashMap<Integer,HashSet<Integer> > t2c_global,
	DBCon dbcon)
      throws SQLException
  {
    if (t2c_global.size()==0) return 0;
    int n=0;
    String sql=("SELECT DISTINCT\n"
	+"\ttarget.id AS tid,\n"
        +"\tcompound.id AS cid\n"
	+"FROM\n"
        +"\ttarget\n"
	+"JOIN\n"
        +"\tcbactivity ON (cbactivity.target_id=target.id)\n"
	+"JOIN\n"
        +"\tsubstance ON (substance.id=cbactivity.substance_id)\n"
	+"JOIN\n"
        +"\ts2c ON (s2c.substance_id=substance.id)\n"
	+"JOIN\n"
        +"\tcompound ON (compound.id=s2c.compound_id)\n"
	+"WHERE\n"
	+"\ts2c.is_active\n"
        +"\tAND target.id IN (");
    int i=0;
    for (int tid: t2c_global.keySet())
    {
      if (i>0) sql+=",";
      ++i;
      sql+=("\n\t\t"+tid);
    }
    sql+=("\n\t\t)\n"
	+"ORDER BY\n"
        +"\ttid");
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next())	//tid,cid
    {
      Integer cid=rset.getInt("cid");
      Integer tid=rset.getInt("tid");
      if (t2c_global.get(tid)==null) t2c_global.put(tid,new HashSet<Integer>());
      t2c_global.get(tid).add(cid);
      ++n;
    }
    return n;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For each CID key in passed argument hash, populate with all associated
	target IDs.
  */
  public static int Compounds2Targets(HashMap<Integer,HashSet<Integer> > c2t_global,
	DBCon dbcon,Boolean human)
      throws SQLException
  {
    if (c2t_global.size()==0) return 0;
    int n=0;
    String sql=("SELECT DISTINCT\n"
        +"\tcompound.id AS cid,\n"
        +"\ttarget.id AS tid\n"
	+"FROM\n"
        +"\tcompound\n"
	+"JOIN\n"
        +"\ts2c ON (s2c.compound_id=compound.id)\n"
	+"JOIN\n"
        +"\tsubstance ON (substance.id=s2c.substance_id)\n"
	+"JOIN\n"
        +"\tcbactivity ON (cbactivity.substance_id=substance.id)\n"
	+"JOIN\n"
        +"\ttarget ON (target.id=cbactivity.target_id)\n"
	+"WHERE\n"
	+"\ts2c.is_active\n"
        +"\tAND compound.id IN (");
    int i=0;
    for (int cid: c2t_global.keySet())
    {
      if (i>0) sql+=",";
      ++i;
      sql+=("\n\t\t"+cid);
    }
    sql+=("\n\t\t)");
    if (human) sql+=("\n\tAND target.species='human'");
    sql+=("\nORDER BY cid");
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next())	//cid,tid
    {
      Integer tid=rset.getInt("tid");
      Integer cid=rset.getInt("cid");
      if (c2t_global.get(cid)==null) c2t_global.put(cid,new HashSet<Integer>());
      c2t_global.get(cid).add(tid);
      ++n;
    }
    return n;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For each scaffold ID key in passed argument hash, populate with
	all associated compound IDs.
	Optionally filter on weight of association.
  */
  public static int Scaffolds2Compounds(HashMap<Integer,HashSet<Integer> > s2c_global,
	DBCon dbcon)
      throws SQLException
  {
    if (s2c_global.size()==0) return 0;
    int n=0;
    String sql=("SELECT DISTINCT\n"
	+"\tscaffold.id AS scafid,\n"
	+"\tcompound.id AS cid\n"
	+"FROM\n"
	+"\tscaffold\n"
	+"JOIN\n"
	+"\tscafid2cid ON (scafid2cid.scaffold_id=scaffold.id)\n"
	+"JOIN\n"
	+"\tcompound ON (compound.id=scafid2cid.compound_id)\n"
	+"WHERE\n"
	+"\tscafid2cid.is_largest\n"
	+"\tAND scaffold.id IN (");
    int i=0;
    for (int scafid: s2c_global.keySet())
    {
      if (i>0) sql+=",";
      ++i;
      sql+=("\n\t\t"+scafid);
    }
    sql+=("\n\t\t)\nORDER BY scafid");
    ResultSet rset=dbcon.executeSql(sql);
    while (rset.next())	//scafid,cid
    {
      Integer scafid=rset.getInt("scafid");
      Integer cid=rset.getInt("cid");
      if (s2c_global.get(scafid)==null) s2c_global.put(scafid,new HashSet<Integer>());
      s2c_global.get(scafid).add(cid);
      ++n;
    }
    return n;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For each scaffold ID key in passed argument hash, populate with all associated
	target IDs.
  */
  public static int Scaffolds2Targets(HashMap<Integer,HashSet<Integer> > s2t_global,
	DBCon dbcon)
      throws SQLException
  {
    if (s2t_global.size()==0) return 0;
    String sql=("SELECT DISTINCT\n"
	+"\tscaffold.id AS scafid,\n"
	+"\ttarget.id AS tid\n"
	+"FROM\n"
	+"\tscaffold\n"
	+"JOIN\n"
	+"\tscafid2cid ON (scafid2cid.scaffold_id=scaffold.id)\n"
	+"JOIN\n"
	+"\tcompound ON (compound.id=scafid2cid.compound_id)\n"
        +"JOIN\n"
        +"\ts2c ON (s2c.compound_id=compound.id)\n"
        +"JOIN\n"
        +"\tsubstance ON (substance.id=s2c.substance_id)\n"
        +"JOIN\n"
        +"\tcbactivity ON (cbactivity.substance_id=substance.id)\n"
        +"JOIN\n"
        +"\ttarget ON (target.id=cbactivity.target_id)\n"
	+"WHERE\n"
	+"\ts2c.is_active\n"
	+"\tAND scafid2cid.is_largest\n"
	+"\tAND scaffold.id IN (");
    int i=0;
    for (int scafid: s2t_global.keySet())
    {
      if (i>0) sql+=",";
      ++i;
      sql+=("\n\t\t"+scafid);
    }
    sql+=("\n\t\t)\nORDER BY scafid");
    ResultSet rset=dbcon.executeSql(sql);
    int n=0;
    while (rset.next())	//scafid,tid
    {
      Integer scafid=rset.getInt("scafid");
      Integer tid=rset.getInt("tid");
      if (s2t_global.get(scafid)==null) s2t_global.put(scafid,new HashSet<Integer>());
      s2t_global.get(scafid).add(tid);
      ++n;
    }
    return n;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For each mces ID key in passed argument hash, populate with all associated
	compound IDs.
  */
  public static int Mcess2Compounds(HashMap<Integer,HashSet<Integer> > m2c_global,
	DBCon dbcon)
      throws SQLException
  {
    if (m2c_global.size()==0) return 0;
    String sql=("SELECT DISTINCT\n"
	+"\tmces.id AS mcesid,\n"
	+"\tcompound.id AS cid\n"
	+"FROM\n"
	+"\tmces\n"
	+"JOIN\n"
	+"\tcompound ON (compound.cluster_id=mces.id)\n"
	+"WHERE\n"
	+"\tmces.id IN (");
    int i=0;
    for (int mcesid: m2c_global.keySet())
    {
      if (i>0) sql+=",";
      ++i;
      sql+=("\n\t\t"+mcesid);
    }
    sql+=("\n\t\t)\nORDER BY mcesid");
    ResultSet rset=dbcon.executeSql(sql);
    int n=0;
    while (rset.next())	//mcesid,cid
    {
      Integer mcesid=rset.getInt("mcesid");
      Integer cid=rset.getInt("cid");
      if (m2c_global.get(mcesid)==null) m2c_global.put(mcesid,new HashSet<Integer>());
      m2c_global.get(mcesid).add(cid);
      ++n;
    }
    return n;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	For each mces ID key in passed argument hash, populate with all associated
	target IDs.
  */
  public static int Mcess2Targets(HashMap<Integer,HashSet<Integer> > m2t_global,
	DBCon dbcon)
      throws SQLException
  {
    if (m2t_global.size()==0) return 0;
    String sql=("SELECT DISTINCT\n"
	+"\tmces.id AS mcesid,\n"
	+"\ttarget.id AS tid\n"
	+"FROM\n"
	+"\tmces\n"
	+"JOIN\n"
	+"\tcompound ON (compound.cluster_id=mces.id)\n"
        +"JOIN\n"
        +"\ts2c ON (s2c.compound_id=compound.id)\n"
        +"JOIN\n"
        +"\tsubstance ON (substance.id=s2c.substance_id)\n"
        +"JOIN\n"
        +"\tcbactivity ON (cbactivity.substance_id=substance.id)\n"
        +"JOIN\n"
        +"\ttarget ON (target.id=cbactivity.target_id)\n"
	+"WHERE\n"
	+"\tmces.id IN (");
    int i=0;
    for (int mcesid: m2t_global.keySet())
    {
      if (i>0) sql+=",";
      ++i;
      sql+=("\n\t\t"+mcesid);
    }
    sql+=("\n\t\t)\nORDER BY mcesid");
    ResultSet rset=dbcon.executeSql(sql);
    int n=0;
    while (rset.next())	//mcesid,tid
    {
      Integer mcesid=rset.getInt("mcesid");
      Integer tid=rset.getInt("tid");
      if (m2t_global.get(mcesid)==null) m2t_global.put(mcesid,new HashSet<Integer>());
      m2t_global.get(mcesid).add(tid);
      ++n;
    }
    return n;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Write target nodes to XGMML file.
  */
  public static int WriteTargets(
	HashMap<Integer,HashMap<String,String> > tgtdata,
	HashMap<Integer,HashSet<Integer> > t2c_global,
	HashMap<Integer,HashMap<String,HashMap<String,Boolean> > > tgt_tgt_ids,
	HashMap<String,Integer> counts,
	PrintWriter fout_writer)
  {
    if (counts.get("n_node_tgt")==null) counts.put("n_node_tgt",0);
    if (counts.get("n_tgt_ext_ids")==null) counts.put("n_tgt_ext_ids",0);
    ArrayList<String> lines = new ArrayList<String>();
    for (int tid: tgtdata.keySet())
    {
      lines.clear();
      lines.add("<node id=\"T"+tid+"\" label=\"T"+tid+"\">");
      lines.add("  <att type=\"string\" name=\"ID\" value=\"T"+tid+"\" />");
      lines.add("  <att type=\"string\" name=\"class\" value=\"target\" />");
      if (tgtdata.get(tid).get("tname")!=null)
        lines.add("  <att type=\"string\" name=\"name\" value=\""+HtmlEscape(tgtdata.get(tid).get("tname"))+"\" />");
      if (tgtdata.get(tid).get("descr")!=null)
        lines.add("  <att type=\"string\" name=\"descr\" value=\""+HtmlEscape(tgtdata.get(tid).get("descr"))+"\" />");
      lines.add("  <att type=\"string\" name=\"species\" value=\""+tgtdata.get(tid).get("species")+"\" />");
      lines.add("  <att type=\"string\" name=\"type\" value=\""+tgtdata.get(tid).get("type")+"\" />");
      if (t2c_global.containsKey(tid)) //degree_compound (global)
      {
        int deg_cpd=(t2c_global.get(tid)==null)?0:t2c_global.get(tid).size();
        lines.add("  <att type=\"integer\" name=\"deg_cpd\" value=\""+deg_cpd+"\" />");
      }
      lines.add("  <graphics type=\"octagon\" h=\"80.0\" w=\"80.0\" fill=\""+tgt_color+"\" outline=\""+outlinecolor+"\" cy:nodeTransparency=\"0.4\" cy:nodeLabelFont=\"SansSerif.bold-0-5\" cy:borderLineType=\"solid\" />");
      if (tgt_tgt_ids.containsKey(tid)) 
      {
        for (String tgt_id_type: tgt_tgt_ids.get(tid).keySet())
        {
          if (tgt_tgt_ids.get(tid).get(tgt_id_type).keySet().size()==0) continue;
          lines.add("  <att type=\"list\" name=\""+tgt_id_type+"\">");
          for (String tgt_id: tgt_tgt_ids.get(tid).get(tgt_id_type).keySet())
          {
            lines.add("    <att type=\"string\" value=\""+tgt_id+"\" />");
            counts.put("n_tgt_ext_ids",counts.get("n_tgt_ext_ids")+1);
          }
          lines.add("  </att>");
        }
      }
      lines.add("</node>");
      counts.put("n_node_tgt",counts.get("n_node_tgt")+1);
      WriteLines(fout_writer,lines,"  ","\n",true);
    }
    return counts.get("n_node_tgt");
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Write compound nodes, and activity edges, to XGMML file.
  */
  public static int WriteCompounds(
	HashMap<Integer,HashMap<String,String> > cpddata,
	HashMap<Integer,HashSet<Integer> > c2t_global,
	HashMap<Integer,HashMap<String,HashSet<String> > > cpd_sbs_ids,
	HashMap<Integer,HashMap<String,String> > actdata,
	HashMap<Integer,HashMap<String,Boolean> > cpdsynonyms,
	int n_max_c,
	int n_max_a,
	HashMap<String,Integer> counts,
	PrintWriter fout_writer)
  {
    if (counts.get("n_node_cpd")==null) counts.put("n_node_cpd",0);
    if (counts.get("n_edge_act")==null) counts.put("n_edge_act",0);
    if (counts.get("n_csynonyms")==null) counts.put("n_csynonyms",0);
    if (counts.get("n_cpd_ext_ids")==null) counts.put("n_cpd_ext_ids",0);

    ArrayList<String> lines = new ArrayList<String>();

    for (int cid: cpddata.keySet())
    {
      WriteCompoundNode(cid,cpddata,c2t_global,cpdsynonyms,cpd_sbs_ids,counts,fout_writer);
      counts.put("n_node_cpd",counts.get("n_node_cpd")+1);
      if (counts.get("n_node_cpd")==n_max_c) break;
    }
    WriteActivityEdges(actdata,null,null,n_max_a,counts,fout_writer);
    return counts.get("n_node_cpd");
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Write one compound node to XGMML file.
  */
  public static void WriteCompoundNode(
	Integer cid,
	HashMap<Integer,HashMap<String,String> > cpddata,
	HashMap<Integer,HashSet<Integer> > c2t_global,
	HashMap<Integer,HashMap<String,Boolean> > cpdsynonyms,
	HashMap<Integer,HashMap<String,HashSet<String> > > cpd_sbs_ids,
	HashMap<String,Integer> counts,
	PrintWriter fout_writer)
  {
    ArrayList<String> lines = new ArrayList<String>();
    lines.add("<node id=\"C"+cid+"\" label=\"C"+cid+"\">");
    lines.add("  <att type=\"string\" name=\"ID\" value=\"C"+cid+"\" />");
    lines.add("  <att type=\"string\" name=\"class\" value=\"compound\" />");
    lines.add("  <att type=\"string\" name=\"canonicalName\" value=\"cpd_"+String.format("%06d",cid)+"\" />");
    String smiles=cpddata.get(cid).get("smiles");
    lines.add("  <att type=\"string\" name=\"smiles\" value=\""+smiles.replace("\\","\\\\")+"\" />");
    if (c2t_global.containsKey(cid)) //degree_target (global)
    {
      int deg_tgt=(c2t_global.get(cid)==null)?0:c2t_global.get(cid).size();
      lines.add("  <att type=\"integer\" name=\"deg_tgt\" value=\""+deg_tgt+"\" />");
    }
    String color=cpd_color;
    if (cpddata.get(cid).get("is_drug")!=null && cpddata.get(cid).get("is_drug").equalsIgnoreCase("T"))
    {
      lines.add("  <att type=\"boolean\" name=\"is_drug\" value=\"true\" />");
      color=drug_color;
    }
    lines.add("  <graphics type=\"rectangle\" h=\"40.0\" w=\"40.0\" fill=\""+color+"\" outline=\""+outlinecolor+"\" cy:nodeTransparency=\"0.4\" cy:nodeLabelFont=\"SansSerif.bold-0-5\" cy:borderLineType=\"solid\" />");
    if (cpdsynonyms!=null && cpdsynonyms.containsKey(cid))
    {
      lines.add("  <att type=\"list\" name=\"synonym\">");
      for (String synonym: cpdsynonyms.get(cid).keySet())
      {
        synonym=synonym.replaceAll("&(\\w+);","$1"); //Kludge for "&Delta;", "&alpha;", etc.
        synonym=synonym.replaceAll("&",""); //Kludge for "SK&F-89748", etc.
        lines.add("    <att type=\"string\" value=\""+synonym+"\" />");
        counts.put("n_csynonyms",counts.get("n_csynonyms")+1);
      }
      lines.add("  </att>");
    }
    if (cpd_sbs_ids!=null && cpd_sbs_ids.containsKey(cid)) 
    {
      for (String sbs_id_type: cpd_sbs_ids.get(cid).keySet())
      {
        if (cpd_sbs_ids.get(cid).get(sbs_id_type).size()==0) continue;
        lines.add("  <att type=\"list\" name=\""+sbs_id_type+"\">");
        for (String sbs_id: cpd_sbs_ids.get(cid).get(sbs_id_type))
        {
          lines.add("    <att type=\"string\" value=\""+sbs_id+"\" />");
          counts.put("n_cpd_ext_ids",counts.get("n_cpd_ext_ids")+1);
        }
        lines.add("  </att>");
      }
    }
    lines.add("</node>");
    WriteLines(fout_writer,lines,"  ","\n",true);
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Write compounds to SDF file.
  */
  public static void WriteCompounds2SDF(CompoundList clist,File fout)
	throws Exception
  {
    MolExporter molWriter = new MolExporter(new FileOutputStream(fout),"sdf:");
    Molecule mol;
    for (int cid: clist.keySet())
    {
      Compound cpd=clist.get(cid);
      try { mol=MolImporter.importMol(cpd.getSmiles(),"smiles:"); }
      catch (IOException e) { System.err.println(e.getMessage()); continue; }
      mol.setName(""+cid);
      mol.setProperty("Carlsbad_ID",""+cid);
      for (String id_type: cpd.getIdentifierTypes())
      {
        for (String id: cpd.getIdentifiers(id_type))
          mol.setProperty(id_type,id);
      }
      HashSet<String> synonyms=cpd.getSynonyms();
      if (synonyms!=null && synonyms.size()>0)
      {
        int i=0;
        String buf="";
        for (String synonym: synonyms)
          buf+=(synonym+(++i<synonyms.size()?"\n":""));
        mol.setProperty("synonyms",buf);
      }
      molWriter.write(mol);
    }
    molWriter.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Write one disease node to XGMML file.
  */
  public static void WriteDiseaseNode(
	Disease disease,
	HashMap<String,Integer> counts,
	PrintWriter fout_writer)
  {
    if (counts.get("n_node_dis")==null) counts.put("n_node_dis",0);
    ArrayList<String> lines = new ArrayList<String>();
    lines.add("<node id=\""+disease.getID()+"\" label=\""+disease.getID()+"\">");
    lines.add("  <att type=\"string\" name=\"ID\" value=\""+disease.getID()+"\" />");
    lines.add("  <att type=\"string\" name=\"class\" value=\"disease\" />");
    lines.add("  <att type=\"string\" name=\"name\" value=\""+disease.getName()+"\" />");
    lines.add("</node>");
    counts.put("n_node_dis",counts.get("n_node_dis")+1);
    WriteLines(fout_writer,lines,"  ","\n",true);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Write one disease edges to XGMML file.
  */
  public static void WriteDiseaseEdges(
	Disease disease,
	HashMap<String,Integer> counts,
	PrintWriter fout_writer)
  {
    if (counts.get("n_edge_dis")==null) counts.put("n_edge_dis",0);
    ArrayList<String> lines = new ArrayList<String>();
    for (Integer tid: disease.getTIDs())
    {
      lines.add("<edge source=\""+disease.getID()+"\" target=\"T"+tid+"\">");
      lines.add("  <att type=\"string\" name=\"class\" value=\"disease2target\" />");
      lines.add("</edge>");
      WriteLines(fout_writer,lines,"  ","\n",true);
      counts.put("n_edge_dis",counts.get("n_edge_dis")+1);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Write activity edges to XGMML file.
  	If specified, only for CIDs and TIDs specified.
  */
  public static int WriteActivityEdges(
	HashMap<Integer,HashMap<String,String> > actdata,
	HashSet<Integer> cids,
	HashSet<Integer> tids,
	int n_max_a,
	HashMap<String,Integer> counts,
	PrintWriter fout_writer)
  {
    if (counts.get("n_edge_act")==null) counts.put("n_edge_act",0);

    ArrayList<String> lines = new ArrayList<String>();
    for (int act_id: actdata.keySet())
    {
      Integer cid=Integer.parseInt(actdata.get(act_id).get("cid"));
      if (cids!=null && !cids.contains(cid)) continue;
      Integer tid=Integer.parseInt(actdata.get(act_id).get("tid"));
      if (tids!=null && !tids.contains(tid)) continue;
      lines.clear();
      lines.add("<edge label=\"A"+act_id+"\" source=\"C"+cid+"\" target=\"T"+tid+"\">");
      lines.add("  <att type=\"string\" name=\"ID\" value=\"A"+act_id+"\" />");
      lines.add("  <att type=\"string\" name=\"class\" value=\"activity\" />");
      if (actdata.get(act_id).get("act_type")!=null)
        lines.add("  <att type=\"string\" name=\"act_type\" value=\""+HtmlEscape(actdata.get(act_id).get("act_type"))+"\" />");
      if (actdata.get(act_id).get("act_value_std")!=null)
      {
        lines.add("  <att type=\"real\" name=\"val_std\" value=\""+actdata.get(act_id).get("act_value_std")+"\" />");
      }
      if (actdata.get(act_id).get("confidence")!=null)
        lines.add("  <att type=\"string\" name=\"confidence\" value=\""+actdata.get(act_id).get("confidence")+"\" />");
      lines.add("</edge>");
      WriteLines(fout_writer,lines,"  ","\n",true);
      counts.put("n_edge_act",counts.get("n_edge_act")+1);
      if (counts.get("n_edge_act")==n_max_a) break;
    }
    return counts.get("n_edge_act");
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Write scaffold nodes, and scaffold-to-compound associative edges, to XGMML file.
  */
  public static int WriteScaffolds(
    HashMap<String,HashMap<String,String> > scafdata,
    HashSet<Integer> scafids_visited,
    HashMap<Integer,HashSet<Integer> > s2c_global,
    HashMap<Integer,HashSet<Integer> > s2t_global,
    HashMap<String,Integer> counts,
    PrintWriter fout_writer)
  {
    if (counts.get("n_node_scaf")==null) counts.put("n_node_scaf",0);
    if (counts.get("n_edge_scaf")==null) counts.put("n_edge_scaf",0);

    ArrayList<String> lines = new ArrayList<String>();
    for (String edgeid: scafdata.keySet())
    {
      Integer scaf_id = Integer.parseInt(scafdata.get(edgeid).get("scaf_id"));
      Integer cid = Integer.parseInt(scafdata.get(edgeid).get("cid"));
      Float s2c_weight = Float.parseFloat(scafdata.get(edgeid).get("weight"));
      lines.clear();
      if (!scafids_visited.contains(scaf_id))
      {
        scafids_visited.add(scaf_id);
        lines.add("<node id=\"S"+scaf_id+"\" label=\"S"+scaf_id+"\">");
        lines.add("  <att type=\"string\" name=\"ID\" value=\"S"+scaf_id+"\" />");
        lines.add("  <att type=\"string\" name=\"class\" value=\"scaffold\" />");
        lines.add("  <att type=\"boolean\" name=\"__has2DGraphics\" value=\"true\" cy:hidden=\"true\"/>");
        lines.add("  <att type=\"string\" name=\"canonicalName\" value=\"scaf_"+String.format("%06d",scaf_id)+" \" />");
        lines.add("  <att type=\"string\" name=\"smiles\" value=\""+scafdata.get(edgeid).get("scafsmi")+"\" />");
        lines.add("  <graphics type=\"hexagon\" h=\"40.0\" w=\"40.0\" fill=\""+scaf_color+"\" outline=\""+outlinecolor+"\" cy:nodeTransparency=\"0.4\" cy:nodeLabelFont=\"SansSerif.bold-0-5\" cy:borderLineType=\"solid\" />");
        if (s2c_global.containsKey(scaf_id)) //degree_compound (global)
        {
          int deg_cpd=(s2c_global.get(scaf_id)==null)?0:s2c_global.get(scaf_id).size();
          lines.add("  <att type=\"integer\" name=\"deg_cpd\" value=\""+deg_cpd+"\" />");
        }
        if (s2t_global.containsKey(scaf_id)) //degree_target (global)
        {
          int deg_tgt=(s2t_global.get(scaf_id)==null)?0:s2t_global.get(scaf_id).size();
          lines.add("  <att type=\"integer\" name=\"deg_tgt\" value=\""+deg_tgt+"\" />");
        }
        lines.add("</node>");
        counts.put("n_node_scaf",counts.get("n_node_scaf")+1);
      }
      lines.add("<edge label=\"S"+scaf_id+"_C"+cid+"\" source=\"C"+cid+"\" target=\"S"+scaf_id+"\">");
      lines.add("  <att type=\"string\" name=\"ID\" value=\"S"+scaf_id+"_C"+cid+"\" />");
      lines.add("  <att type=\"string\" name=\"class\" value=\"cpd2scaf\" />");
      if (s2c_weight!=null)
        lines.add("  <att type=\"real\" name=\"weight\" value=\""+String.format("%.2f",s2c_weight)+"\" />");
      lines.add("</edge>");
      WriteLines(fout_writer,lines,"  ","\n",true);
      counts.put("n_edge_scaf",counts.get("n_edge_scaf")+1);
    }
    return counts.get("n_edge_scaf");
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Write MCES nodes, and MCES-to-compound associative edges, to XGMML file.
  */
  public static int WriteMcess(
    HashMap<String,HashMap<String,String> > mcesdata,
    HashSet<Integer> mcesids_visited,
    HashMap<Integer,HashSet<Integer> > m2c_global,
    HashMap<Integer,HashSet<Integer> > m2t_global,
    HashMap<String,Integer> counts,
    PrintWriter fout_writer)
  {
    if (counts.get("n_node_mces")==null) counts.put("n_node_mces",0);
    if (counts.get("n_edge_mces")==null) counts.put("n_edge_mces",0);

    ArrayList<String> lines = new ArrayList<String>();
    for (String edgeid: mcesdata.keySet())
    {
      Integer cluster_id = Integer.parseInt(mcesdata.get(edgeid).get("cluster_id"));
      Integer cid = Integer.parseInt(mcesdata.get(edgeid).get("cid"));
      lines.clear();
      if (!mcesids_visited.contains(cluster_id))
      {
        mcesids_visited.add(cluster_id);
        lines.add("<node id=\"M"+cluster_id+"\" label=\"M"+cluster_id+"\">");
        lines.add("  <att type=\"string\" name=\"ID\" value=\"M"+cluster_id+"\" />");
        lines.add("  <att type=\"string\" name=\"class\" value=\"mces\" />");
        lines.add("  <att type=\"boolean\" name=\"__has2DGraphics\" value=\"true\" cy:hidden=\"true\"/>");
        lines.add("  <att type=\"string\" name=\"canonicalName\" value=\"mces_"+String.format("%06d",cluster_id)+" \" />");
        lines.add("  <att type=\"string\" name=\"smarts\" value=\""+HtmlEscape(mcesdata.get(edgeid).get("mces"))+"\" />");
        lines.add("  <graphics type=\"ellipse\" h=\"40.0\" w=\"40.0\" fill=\""+mces_color+"\" outline=\""+outlinecolor+"\" cy:nodeTransparency=\"0.4\" cy:nodeLabelFont=\"SansSerif.bold-0-5\" cy:borderLineType=\"solid\" />");
        if (m2c_global.containsKey(cluster_id)) //degree_compound (global)
        {
          int deg_cpd=(m2c_global.get(cluster_id)==null)?0:m2c_global.get(cluster_id).size();
          lines.add("  <att type=\"integer\" name=\"deg_cpd\" value=\""+deg_cpd+"\" />");
        }
        if (m2t_global.containsKey(cluster_id)) //degree_target (global)
        {
          int deg_tgt=(m2t_global.get(cluster_id)==null)?0:m2t_global.get(cluster_id).size();
          lines.add("  <att type=\"integer\" name=\"deg_tgt\" value=\""+deg_tgt+"\" />");
        }
        lines.add("</node>");
        counts.put("n_node_mces",counts.get("n_node_mces")+1);
      }
      lines.add("<edge label=\"M"+cluster_id+"_C"+cid+"\" source=\"C"+cid+"\" target=\"M"+cluster_id+"\">");
      lines.add("  <att type=\"string\" name=\"ID\" value=\"M"+cluster_id+"_C"+cid+"\" />");
      lines.add("  <att type=\"string\" name=\"class\" value=\"cpd2mces\" />");
      lines.add("</edge>");
      WriteLines(fout_writer,lines,"  ","\n",true);
      counts.put("n_edge_mces",counts.get("n_edge_mces")+1);
    }
    return counts.get("n_edge_mces");
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Write reduced-graph to XGMML.
	Annotated targets plus query entity: drug|disease.
  */
  public static int WriteReducedGraph(
	HashMap<Integer,HashMap<String,String> > tgtdata,
	Disease disease,	//disease-query mode
	Integer cid_query,	//drug-query mode
	HashMap<Integer,HashSet<Integer> > t2c_global,
	HashMap<Integer,HashSet<Integer> > s2t_global,
	HashMap<Integer,HashSet<Integer> > s2c_global,
	HashMap<Integer,HashSet<Integer> > m2t_global,
	HashMap<Integer,HashSet<Integer> > m2c_global,
	HashMap<Integer,HashMap<String,HashMap<String,Boolean> > > tgt_tgt_ids,
	HashMap<Integer,HashMap<String,String> > actdata,
	HashMap<Integer,HashMap<String,String> > cpddata,
	HashMap<Integer,HashMap<String,Boolean> > cpdsynonyms,
	HashMap<Integer,HashMap<String,HashSet<String> > > cpd_sbs_ids,
	HashMap<String,HashMap<String,String> > scafdata,
	HashMap<String,HashMap<String,String> > mcesdata,
	boolean include_ccps,
	HashMap<String,Integer> counts,
	PrintWriter fout_writer)
  {
    if (counts.get("n_node_tgt")==null) counts.put("n_node_tgt",0);
    if (counts.get("n_tgt_ext_ids")==null) counts.put("n_tgt_ext_ids",0);
    if (counts.get("n_node_tgt_rgt")==null) counts.put("n_node_tgt_rgt",0);
    if (counts.get("n_tgt_ext_ids_rgt")==null) counts.put("n_tgt_ext_ids_rgt",0);

    ArrayList<String> lines = new ArrayList<String>();
    HashSet<Integer> tids = new HashSet<Integer>(tgtdata.keySet());

    // Determine shared cpds, scafs, mcess, for edge annotations.
    // 1st initialize matrices.
    HashMap<Integer,HashMap<Integer,HashSet<Integer> > > cpd_shared = new HashMap<Integer,HashMap<Integer,HashSet<Integer> > >();
    for (int tidA: tids)
    {
      cpd_shared.put(tidA, new HashMap<Integer,HashSet<Integer> >());
      for (int tidB: tids) cpd_shared.get(tidA).put(tidB, new HashSet<Integer>());
    }
    HashMap<Integer,HashMap<Integer,HashSet<Integer> > > scaf_shared = new HashMap<Integer,HashMap<Integer,HashSet<Integer> > >();
    for (int tidA: tids)
    {
      scaf_shared.put(tidA, new HashMap<Integer,HashSet<Integer> >());
      for (int tidB: tids) scaf_shared.get(tidA).put(tidB, new HashSet<Integer>());
    }
    HashMap<Integer,HashMap<Integer,HashSet<Integer> > > mces_shared = new HashMap<Integer,HashMap<Integer,HashSet<Integer> > >();
    for (int tidA: tids)
    {
      mces_shared.put(tidA, new HashMap<Integer,HashSet<Integer> >());
      for (int tidB: tids) mces_shared.get(tidA).put(tidB, new HashSet<Integer>());
    }

    // Transform s2t_global to t2s_global
    HashMap<Integer,HashSet<Integer> > t2s_global = new HashMap<Integer,HashSet<Integer> >();
    for (int scafid: s2t_global.keySet())
    {
      for (int tid: s2t_global.get(scafid))
      {
        if (!t2s_global.containsKey(tid)) t2s_global.put(tid,new HashSet<Integer>());
        t2s_global.get(tid).add(scafid);
      }
    }

    // Transform m2t_global to t2m_global
    HashMap<Integer,HashSet<Integer> > t2m_global = new HashMap<Integer,HashSet<Integer> >();
    for (int mcesid: m2t_global.keySet())
    {
      for (int tid: m2t_global.get(mcesid))
      {
        if (!t2m_global.containsKey(tid)) t2m_global.put(tid,new HashSet<Integer>());
        t2m_global.get(tid).add(mcesid);
      }
    }

    // Populate *_shared matrices.
    // NOTE: There are some orphaned targets (e.g. 851).  
    for (int tidA: tids)
    {
      for (int tidB: tids)
      {
        if (tidA>=tidB) continue;
        if (t2c_global.containsKey(tidA) && t2c_global.containsKey(tidB)
            && t2c_global.get(tidA)!=null && t2c_global.get(tidB)!=null)
        {
          HashSet<Integer> cidsA = new HashSet<Integer>(t2c_global.get(tidA));
          HashSet<Integer> cidsB = new HashSet<Integer>(t2c_global.get(tidB));
          for (int cid: cidsA) { if (cidsB.contains(cid)) cpd_shared.get(tidA).get(tidB).add(cid); }
        }
        if (t2s_global.containsKey(tidA) &&  t2s_global.containsKey(tidB))
        {
          HashSet<Integer> scafidsA = new HashSet<Integer>(t2s_global.get(tidA));
          HashSet<Integer> scafidsB = new HashSet<Integer>(t2s_global.get(tidB));
          for (int scafid: scafidsA) { if (scafidsB.contains(scafid)) scaf_shared.get(tidA).get(tidB).add(scafid); }
        }
        if (t2m_global.containsKey(tidA) && t2m_global.containsKey(tidB))
        {
          HashSet<Integer> mcesidsA = new HashSet<Integer>(t2m_global.get(tidA));
          HashSet<Integer> mcesidsB = new HashSet<Integer>(t2m_global.get(tidB));
          for (int mcesid: mcesidsA) { if (mcesidsB.contains(mcesid)) mces_shared.get(tidA).get(tidB).add(mcesid); }
        }
      }
    }

    for (int tid: tids)
    {
      lines.clear();
      lines.add("<node id=\"T"+tid+"\" label=\"T"+tid+"\">");
      lines.add("  <att type=\"string\" name=\"ID\" value=\"T"+tid+"\" />");
      lines.add("  <att type=\"string\" name=\"class\" value=\"target\" />");
      if (tgtdata.get(tid).get("tname")!=null)
        lines.add("  <att type=\"string\" name=\"name\" value=\""+HtmlEscape(tgtdata.get(tid).get("tname"))+"\" />");
      if (tgtdata.get(tid).get("descr")!=null)
        lines.add("  <att type=\"string\" name=\"descr\" value=\""+HtmlEscape(tgtdata.get(tid).get("descr"))+"\" />");
      lines.add("  <att type=\"string\" name=\"species\" value=\""+tgtdata.get(tid).get("species")+"\" />");
      lines.add("  <att type=\"string\" name=\"type\" value=\""+tgtdata.get(tid).get("type")+"\" />");
      if (t2c_global.containsKey(tid)) //degree_compound (global)
      {
        int deg_cpd=(t2c_global.get(tid)==null)?0:t2c_global.get(tid).size();
        lines.add("  <att type=\"integer\" name=\"deg_cpd\" value=\""+deg_cpd+"\" />");
        if (cid_query!=null && t2c_global.get(tid).contains(cid_query))
        {
          lines.add("  <att type=\"boolean\" name=\"query_active\" value=\"true\" />");
        }
        else if (disease!=null && disease.getTIDs().contains(tid))
        {
          lines.add("  <att type=\"boolean\" name=\"query_active\" value=\"true\" />");
        }
      }
      if (t2s_global.containsKey(tid)) //degree_scaffold (global)
      {
        int deg_scaf=(t2s_global.get(tid)==null)?0:t2s_global.get(tid).size();
        lines.add("  <att type=\"integer\" name=\"deg_scaf\" value=\""+deg_scaf+"\" />");
      }
      if (t2m_global.containsKey(tid)) //degree_mces (global)
      {
        int deg_mces=(t2m_global.get(tid)==null)?0:t2m_global.get(tid).size();
        lines.add("  <att type=\"integer\" name=\"deg_mces\" value=\""+deg_mces+"\" />");
      }
      lines.add("  <graphics type=\"octagon\" h=\"80.0\" w=\"80.0\" fill=\""+tgt_color+"\" outline=\""+outlinecolor+"\" cy:nodeTransparency=\"0.4\" cy:nodeLabelFont=\"SansSerif.bold-0-5\" cy:borderLineType=\"solid\" />");
      if (tgt_tgt_ids.containsKey(tid)) 
      {
        for (String tgt_id_type: tgt_tgt_ids.get(tid).keySet())
        {
          if (tgt_tgt_ids.get(tid).get(tgt_id_type).keySet().size()==0) continue;
          lines.add("  <att type=\"list\" name=\""+tgt_id_type+"\">");
          for (String tgt_id: tgt_tgt_ids.get(tid).get(tgt_id_type).keySet())
          {
            lines.add("    <att type=\"string\" value=\""+tgt_id+"\" />");
            counts.put("n_tgt_ext_ids_rgt",counts.get("n_tgt_ext_ids_rgt")+1);
          }
          lines.add("  </att>");
        }
      }
      lines.add("</node>");
      counts.put("n_node_tgt_rgt",counts.get("n_node_tgt_rgt")+1);
      WriteLines(fout_writer,lines,"  ","\n",true);
    }

    HashMap<Integer,HashSet<Integer> > c2t_global = null;
    if (cid_query!=null)	//drug-query mode
    {
      // Transform t2c_global to c2t_global
      c2t_global = new HashMap<Integer,HashSet<Integer> >();
      for (int tid: t2c_global.keySet())
      {
        for (int cid: t2c_global.get(tid))
        {
          if (!c2t_global.containsKey(cid)) c2t_global.put(cid,new HashSet<Integer>());
          c2t_global.get(cid).add(tid);
        }
      }
      WriteCompoundNode(cid_query,cpddata,c2t_global,cpdsynonyms,cpd_sbs_ids,counts,fout_writer);
    }
    else if (disease!=null)	//disease-query mode
    {
      WriteDiseaseNode(disease,counts,fout_writer);
    }

    if (include_ccps)
    {
      //Write ccp nodes (scafs)
      counts.put("n_node_scaf",0);
      for (int scafid: s2t_global.keySet())
      {
        lines.add("<node id=\"S"+scafid+"\" label=\"S"+scafid+"\">");
        lines.add("  <att type=\"string\" name=\"ID\" value=\"S"+scafid+"\" />");
        lines.add("  <att type=\"string\" name=\"class\" value=\"scaffold\" />");
        lines.add("  <att type=\"boolean\" name=\"__has2DGraphics\" value=\"true\" cy:hidden=\"true\"/>");
        lines.add("  <att type=\"string\" name=\"canonicalName\" value=\"scaf_"+String.format("%06d",scafid)+" \" />");
        lines.add("  <att type=\"string\" name=\"smiles\" value=\""+ScafID2Smiles(scafid,scafdata)+"\" />");
        lines.add("  <graphics type=\"hexagon\" h=\"40.0\" w=\"40.0\" fill=\""+scaf_color+"\" outline=\""+outlinecolor+"\" cy:nodeTransparency=\"0.4\" cy:nodeLabelFont=\"SansSerif.bold-0-5\" cy:borderLineType=\"solid\" />");
        if (s2c_global.containsKey(scafid)) //degree_compound (global)
        {
          int deg_cpd=(s2c_global.get(scafid)==null)?0:s2c_global.get(scafid).size();
          lines.add("  <att type=\"integer\" name=\"deg_cpd\" value=\""+deg_cpd+"\" />");
        }

        if (s2t_global.containsKey(scafid)) //degree_target (global)
        {
          int deg_tgt=(s2t_global.get(scafid)==null)?0:s2t_global.get(scafid).size();
          lines.add("  <att type=\"integer\" name=\"deg_tgt\" value=\""+deg_tgt+"\" />");
        }
        lines.add("</node>");
        WriteLines(fout_writer,lines,"  ","\n",true);
        counts.put("n_node_scaf",counts.get("n_node_scaf")+1);
      }

      //Write ccp nodes (mcess)
      counts.put("n_node_mces",0);
      for (int mcesid: m2t_global.keySet())
      {
        lines.add("<node id=\"M"+mcesid+"\" label=\"M"+mcesid+"\">");
        lines.add("  <att type=\"string\" name=\"ID\" value=\"M"+mcesid+"\" />");
        lines.add("  <att type=\"string\" name=\"class\" value=\"mces\" />");
        lines.add("  <att type=\"boolean\" name=\"__has2DGraphics\" value=\"true\" cy:hidden=\"true\"/>");
        lines.add("  <att type=\"string\" name=\"canonicalName\" value=\"mces_"+String.format("%06d",mcesid)+" \" />");
        lines.add("  <att type=\"string\" name=\"smarts\" value=\""+MCESID2Smarts(mcesid,mcesdata)+"\" />");
        lines.add("  <graphics type=\"ellipse\" h=\"40.0\" w=\"40.0\" fill=\""+mces_color+"\" outline=\""+outlinecolor+"\" cy:nodeTransparency=\"0.4\" cy:nodeLabelFont=\"SansSerif.bold-0-5\" cy:borderLineType=\"solid\" />");
        if (m2c_global.containsKey(mcesid)) //degree_compound (global)
        {
          int deg_cpd=(m2c_global.get(mcesid)==null)?0:m2c_global.get(mcesid).size();
          lines.add("  <att type=\"integer\" name=\"deg_cpd\" value=\""+deg_cpd+"\" />");
        }
        if (m2t_global.containsKey(mcesid)) //degree_target (global)
        {
          int deg_tgt=(m2t_global.get(mcesid)==null)?0:m2t_global.get(mcesid).size();
          lines.add("  <att type=\"integer\" name=\"deg_tgt\" value=\""+deg_tgt+"\" />");
        }
        lines.add("</node>");
        counts.put("n_node_mces",counts.get("n_node_mces")+1);
      }

      //Create tgt-ccp edges if any shared cpds.
      //To do: add weight, n_cpds represented by edge?
      counts.put("n_edge_tgtccp",0);
      for (int scafid: s2t_global.keySet())
      {
        for (int tid: s2t_global.get(scafid))
        {
          if (!tids.contains(tid)) continue;
          lines.add("<edge source=\"T"+tid+"\" target=\"S"+scafid+"\">");
          lines.add("  <att type=\"string\" name=\"ID\" value=\"TS"+counts.get("n_edge_tgtccp")+"\" />");
          lines.add("  <att type=\"string\" name=\"class\" value=\"ts\"/>");
          lines.add("</edge>");
          WriteLines(fout_writer,lines,"  ","\n",true);
          counts.put("n_edge_tgtccp",counts.get("n_edge_tgtccp")+1);
        }
      }
      for (int mcesid: m2t_global.keySet())
      {
        for (int tid: m2t_global.get(mcesid))
        {
          if (!tids.contains(tid)) continue;
          lines.add("<edge source=\"T"+tid+"\" target=\"M"+mcesid+"\">");
          lines.add("  <att type=\"string\" name=\"ID\" value=\"TS"+counts.get("n_edge_tgtccp")+"\" />");
          lines.add("  <att type=\"string\" name=\"class\" value=\"ts\"/>");
          lines.add("</edge>");
          WriteLines(fout_writer,lines,"  ","\n",true);
          counts.put("n_edge_tgtccp",counts.get("n_edge_tgtccp")+1);

        }
      }
    }

    //Create tgt-tgt edges if any shared cpds, scafs or mcess.
    counts.put("n_edge_tgttgt",0);
    int n_edge_tt=0;
    for (int tidA: tids)
    {
      for (int tidB: tids)
      {
        int n_shared_cpd = 0;
        int n_shared_scaf = 0;
        int n_shared_mces = 0;
        if (tidA>=tidB) continue;
        lines.clear();
        if (cpd_shared.containsKey(tidA) && cpd_shared.get(tidA).containsKey(tidB))
          n_shared_cpd = cpd_shared.get(tidA).get(tidB).size();
        if (scaf_shared.containsKey(tidA) && scaf_shared.get(tidA).containsKey(tidB))
          n_shared_scaf = scaf_shared.get(tidA).get(tidB).size();
        if (mces_shared.containsKey(tidA) && mces_shared.get(tidA).containsKey(tidB))
          n_shared_mces = mces_shared.get(tidA).get(tidB).size();
        boolean got_edge=(n_shared_cpd>0 || n_shared_scaf>0 || n_shared_mces>0);
        if (got_edge)
        {
          counts.put("n_edge_tgttgt",counts.get("n_edge_tgttgt")+1);
          lines.add("<edge source=\"T"+tidA+"\" target=\"T"+tidB+"\">");
          lines.add("  <att type=\"string\" name=\"ID\" value=\"TT"+counts.get("n_edge_tgttgt")+"\" />");
          lines.add("  <att type=\"string\" name=\"class\" value=\"tt\"/>");
          if (n_shared_cpd>0)
            lines.add("  <att type=\"integer\" name=\"shared_cpd\" value=\""+n_shared_cpd+"\" />");
          if (n_shared_scaf>0)
            lines.add("  <att type=\"integer\" name=\"shared_scaf\" value=\""+n_shared_scaf+"\" />");
          if (n_shared_mces>0)
            lines.add("  <att type=\"integer\" name=\"shared_mces\" value=\""+n_shared_mces+"\" />");
          lines.add("</edge>");
        }
        WriteLines(fout_writer,lines,"  ","\n",true);
      }
    }

    if (cid_query!=null)	//drug-query mode
    {
      WriteActivityEdges(actdata,
	new HashSet<Integer>(Arrays.asList(cid_query)),
	tids, 0, counts, fout_writer);

      //Need drug-ccp edge.
      if (include_ccps)
      {
        for (int scafid: s2t_global.keySet())
        {
          String edgeid=("S"+scafid+"_C"+cid_query);
          if (scafdata.containsKey(edgeid))
          {
            lines.add("<edge label=\"S"+scafid+"_C"+cid_query+"\" source=\"C"+cid_query+"\" target=\"S"+scafid+"\">");
            lines.add("  <att type=\"string\" name=\"ID\" value=\"S"+scafid+"_C"+cid_query+"\" />");
            lines.add("  <att type=\"string\" name=\"class\" value=\"cpd2scaf\" />");
            Float s2c_weight=Float.parseFloat(scafdata.get(edgeid).get("weight"));
            if (s2c_weight!=null)
              lines.add("  <att type=\"real\" name=\"weight\" value=\""+String.format("%.2f",s2c_weight)+"\" />");
            lines.add("</edge>");
            WriteLines(fout_writer,lines,"  ","\n",true);
            counts.put("n_edge_scaf",counts.get("n_edge_scaf")+1);
          }
        }
      }
    }
    else if (disease!=null)	//disease-query mode
    {
      WriteDiseaseEdges(disease,counts,fout_writer);
    }
    return counts.get("n_node_tgt_rgt");
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String ScafID2Smiles(int scafid, HashMap<String,HashMap<String,String> > scafdata)
  {	//KLUDGE!
    for (String edgeid: scafdata.keySet())
    {
      if (edgeid.matches("^S"+scafid+"_.*$"))
      {
        return scafdata.get(edgeid).get("scafsmi");
      }
    }
    return null;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String MCESID2Smarts(int mcesid,HashMap<String,HashMap<String,String> > mcesdata)
  {	//KLUDGE!
    for (String edgeid: mcesdata.keySet())
    {
      if (edgeid.matches("^M"+mcesid+"_.*$"))
      {
        return mcesdata.get(edgeid).get("mces");
      }
    }
    return null;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void WriteLines(
	PrintWriter fout_writer,
	ArrayList<String> lines,
	String indent,
	String nl,
	Boolean clr)
  {
    for (String line: lines)
      fout_writer.print(indent+line+nl);
    if (clr) lines.clear();
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Replace HTML control characters with escape codes.
  */
  private static String HtmlEscape(String aText)
  {
    if (aText == null) { return ""; }
    StringBuffer escapedText = new StringBuffer();
    for (int i=0; i<aText.length(); i++) {
      char ch = aText.charAt(i);
      if (ch == '\'') escapedText.append("&#39;");
      else if (ch == '\"') escapedText.append("&#34;");
      else if (ch == '<') escapedText.append("&lt;");
      else if (ch == '>') escapedText.append("&gt;");
      else if (ch == '&') escapedText.append("&amp;");
      else if (ch == '=') escapedText.append("&#61;");
      else                escapedText.append(ch);
    }
    return escapedText.toString();
  }
  /////////////////////////////////////////////////////////////////////////////
  public static <T> ArrayList<T> UniquifyArrayList(ArrayList<T> alist)
  {
    TreeSet<T> tset = new TreeSet<T>(alist);
    alist.clear();
    alist.addAll(tset);
    return alist;
  }
}
