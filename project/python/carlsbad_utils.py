#!/usr/bin/env python
"""
	carlsbad_utils.py - carlsbad database utility functions

	By calling cansmi from [Open]Chord, we assure that the canonicalization
	algorithm is the same as used when loading the db, and hence the
	lookup is robust, even if [Open]Chord has been upgraded to a new version since.

	Jeremy Yang
	 8 Nov 2013
"""
import os,sys,re,getopt,cgi,time
import pgdb
import cytoscape_utils

DBHOST='habanero.health.unm.edu'
DBNAME='carlsbad'
DBUSR='jjyang'
DBPW='assword'

#target_class_type='Uniprot Family'
target_class_type='ChEMBL Class'
#############################################################################
def ExeSql(cur,sql):
  try:
    cur.execute(sql)
  except pgdb.DatabaseError, e:
    print >>sys.stderr,('Postgresql-Error: %s'%e.args)
    return False
  return True

#############################################################################
def Connect(dbhost=DBHOST,dbname=DBNAME,dbusr=DBUSR,dbpw=DBPW):
  """   Connect to database. """
  db=pgdb.connect(dsn='%s:%s:%s:%s'%(dbhost,dbname,dbusr,dbpw))
  cur=db.cursor()
  return db,cur

#############################################################################
def CpdSmi2Id(db,smi):
  """   Lookup compound smiles. """
  cur=db.cursor()
  sql=("SELECT id FROM compound WHERE smiles=gnova.cansmiles('%s')"%smi)
  ExeSql(cur,sql)
  rows=cur.fetchall()	##data rows
  cur.close()
  if not rows: return None
  return rows[0][0]

#############################################################################
def Compound2Synonyms(db,cid):
  """   Lookup compound synonyms. """
  cur=db.cursor()
  sql=("SELECT synonym.name FROM synonym JOIN substance ON (substance.id=synonym.substance_id) JOIN s2c ON (s2c.substance_id=substance.id) WHERE s2c.compound_id=%d"%cid)
  ExeSql(cur,sql)
  rows=cur.fetchall()	##data rows
  cur.close()
  if not rows: return []
  names=[]
  for row in rows:
    names.append(row[0])
  return names

#############################################################################
def CompoundInfo(db,cid):
  """	Return info for selected compound.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	compound.id AS cid,
	compound.mol_formula,
	compound.smiles,
	compound.iso_smiles,
	substance.id AS sid,
	substance.iupac_name,
	substance.is_drug
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (s2c.substance_id=substance.id)
WHERE
	compound.id = %(CID)d
ORDER BY
	sid
'''%{'CID':cid}
  ExeSql(cur,sql)
  rows=cur.fetchall()	##data rows
  cur.close()
  return rows

#############################################################################
def Compound2TIDs(db,cid):
  """	Return list of TIDs active for selected compound.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	target.id AS tid
FROM
	target
JOIN
	activity ON (target.id=activity.target_id)
JOIN
	substance ON (activity.substance_id=substance.id)
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (s2c.compound_id=compound.id)
WHERE
	compound.id = %(CID)d
	AND s2c.is_active
ORDER BY
	tid
'''%{'CID':cid}

  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  tids=[]
  for row in rows:
    try:
      tid=int(row[0])
      tids.append(tid)
    except:
      pass ##should not happen
  return tids

#############################################################################
def Compound2Targets(db,cid):
  return Compounds2Targets(db,[cid])

#############################################################################
def Compounds2Targets(db,cids):
  """   Lookup targets for which compounds have activity. """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	target.id AS tid,
	target.name,
	target.species,
	target.type
FROM
	target
JOIN
	activity ON (target.id=activity.target_id)
JOIN
	substance ON (activity.substance_id=substance.id)
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (s2c.compound_id=compound.id)
WHERE
	s2c.compound_id IN ( %(CIDS)s )
ORDER BY
	tid
'''%{'CIDS':','.join(map(lambda x:str(x),cids))}
  ExeSql(cur,sql)
  rows=cur.fetchall()	##data rows
  cur.close()
  return rows

#############################################################################
def ScafSmi2Id(db,smi):
  """   Lookup scaffold ID. """
  cur=db.cursor()
  sql=("SELECT id FROM scaffold WHERE smiles=gnova.cansmiles('%s')"%smi)
  ExeSql(cur,sql)
  row=cur.fetchone()
  cur.close()
  if not row: return None
  return row[0]

#############################################################################
def ScafSmiFile2Ids(fin,fout,db,verbose=0):
  """	For each input scaffold smiles, find ID in db if present.
	Also determine if scaf is B-M (largest) in any mol.
"""
  n_scafid=0
  n_scaf=0
  while True:
    line=fin.readline()
    if not line: break
    n_scaf+=1
    scafsmi=re.split(r'\s',line)[0]
    try:
      scafid=ScafSmi2Id(db,scafsmi)
    except pgdb.DatabaseError, e:
      print >>sys.stderr,('Postgresql-Error: %s'%e.args)
      scafid=0
      db.close()
      db=Connect()[0]
      continue
    is_bm_anymol=False
    if not scafid:
      scafid=0
    else:
      n_scafid+=1
      is_bm_anymol=ScafLargestAnyMol(scafid,db)
    fout.write("%s\t%d\t%s\n"%(line.rstrip(),scafid,is_bm_anymol))
  print >>sys.stderr, "scafs: %d  scafids: %d"%(n_scaf,n_scafid)

#############################################################################
def ScafLargestAnyMol(scafid,db):
  sql='SELECT count(*) from scafid2cid WHERE is_largest AND scaffold_id = %d'%(scafid)
  cur=db.cursor()
  ExeSql(cur,sql)
  row=cur.fetchone()
  try:
    n=int(row[0])
  except:
    n=0
  return bool(n>0)

#############################################################################
def ScafId2Smi(db,scafid):
  """   Lookup scaffold smiles. """
  cur=db.cursor()
  sql=("SELECT smiles FROM scaffold WHERE id = %d"%scafid)
  ExeSql(cur,sql)
  row=cur.fetchone()
  cur.close()
  if not row: return None
  return row[0]

#############################################################################
def SetLargestScaffoldColumn4Cpd(db,cid):
  """	For cid, set scafid2cid.is_largest. """
  cur=db.cursor()
  scafid=Compound2LargestScaffold(db,cid)
  if not scafid: return False
  sql=('UPDATE TABLE scafid2cid SET is_largest = TRUE WHERE cid = %d AND scafid = %d'%(cid,scafid))
  ExeSql(cur,sql)
  sql=('UPDATE TABLE scafid2cid SET is_largest = FALSE WHERE cid = %d AND scafid != %d'%(cid,scafid))
  ExeSql(cur,sql)
  cur.close()
  db.commit()
  return True

#############################################################################
def SetLargestScaffoldColumn4DB(db):
  """	For all cids, set scafid2cid.is_largest. """
  cur=db.cursor()
  sql=('SELECT id from compound')
  ExeSql(cur,sql)
  row=cur.fetchone()
  n=0
  while row:
    cid=row[0]
    ok=SetLargestScaffoldColumn4Cpd(db,cid)
    if ok: n+=1
    row=cur.fetchone()
  cur.close()
  print >>sys.stderr, 'SetLargestScaffoldColumn compounds: %d'%n

#############################################################################
def DescribeCounts(db):
  cur=db.cursor()
  sql=("SELECT table_name FROM information_schema.tables WHERE table_schema='public'")
  ExeSql(cur,sql)
  rows=cur.fetchall()	##data rows
  outtxt=""
  for row in rows:
    tablename=row[0]
    sql=("SELECT count(*) from %s"%tablename)
    ExeSql(cur,sql)
    rows=cur.fetchall()	##data rows
    outtxt+="count(%18s): %8d\n"%(tablename,rows[0][0])
  cur.close()
  return outtxt
  
#############################################################################
def DescribeDB(db):
  outtxt=""
  cur=db.cursor()
  sql=("SELECT table_name FROM information_schema.tables WHERE table_schema='public'")
  ExeSql(cur,sql)
  rows=cur.fetchall()	##data rows
  for row in rows:
    tablename=row[0]
    sql=("SELECT column_name,data_type FROM information_schema.columns WHERE table_name = '%s'"%tablename)
    ExeSql(cur,sql)
    rows=cur.fetchall()	##data rows
    outtxt+=("table: %s\n"%tablename)
    for row in rows:
      outtxt+=("\t%s\n"%str(row))
  cur.close()
  return outtxt

#############################################################################
def ListTargets(db):
  """	Return list of all targets.
  """
  cur=db.cursor()
  sql='''\
SELECT
	target.id AS tid,
	target.name AS tname,
	target.descr,
	target.species,
	target.type AS ttype,
	identifier.id_type,
	identifier.id
FROM
	target
LEFT OUTER JOIN
	identifier ON (identifier.target_id=target.id)
	;
'''
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  return rows

#############################################################################
def ListDiseases(db):
  cur=db.cursor()
  sql='SELECT DISTINCT id,name FROM kegg_disease'
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  return rows

#############################################################################
def ListDrugs(db):
  """	Return list of all drugs.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	compound.id AS cid,
	substance.id AS sid,
	synonym.name,
	compound.smiles
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
LEFT OUTER JOIN
	synonym ON (substance.id=synonym.substance_id)
WHERE
	substance.is_drug
ORDER BY
	compound.id
	;
'''
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  return rows

#############################################################################
def ListTargetSpecies(db):
  """	Return list of all species.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT target.species FROM target ;
'''
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  vals=[]
  for row in rows: vals.append(row[0])
  vals.sort()
  return vals

#############################################################################
def ListTargetIDTypes(db):
  """	Return list of all target ID types.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT 
	identifier.id_type
FROM
	target,identifier
WHERE
	identifier.target_id=target.id ;
'''
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  vals=[]
  for row in rows: vals.append(row[0])
  vals.sort()
  return vals

#############################################################################
def ListTargetTypes(db):
  """	Return list of all target types.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT target.type FROM target
'''
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  vals=[]
  for row in rows: vals.append(row[0])
  vals.sort()
  return vals

#############################################################################
def ListTargetClasses(db):
  """	Return list of all target classes (to be replaced when target
	classification method decided).
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT target_classifier.id
FROM target_classifier
WHERE target_classifier.type='%(TARGET_CLASS_TYPE)s'
'''%{'TARGET_CLASS_TYPE':target_class_type}
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  vals=[]
  for row in rows: vals.append(row[0])
  vals.sort()
  return vals

#############################################################################
def TargetClass2TIDs(db,tclass):
  """	Return list of all target classes (to be replaced when target
	classification method decided).
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	target.id
FROM
	target
LEFT OUTER JOIN
        target_classifier ON (target_classifier.target_id=target.id)
WHERE
	target_classifier.type='%(TARGET_CLASS_TYPE)s'
	AND target_classifier.id = '%(TCLASS)s'
'''%{'TCLASS':tclass.replace("'","''"),'TARGET_CLASS_TYPE':target_class_type}
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  vals=[]
  for row in rows: vals.append(row[0])
  vals.sort()
  return vals

#############################################################################
def TargetExternalIDLookup(db,id_type,ids):
  """	Return list of all targets.
  """
  cur=db.cursor()
  sql='''\
SELECT
	target.id AS tid,
	target.name AS tname,
	target.descr,
	target.species,
	target.type AS ttype,
	identifier.id_type,
	identifier.id
FROM
	target,
	identifier
WHERE
	identifier.target_id=target.id
	AND identifier.id_type='%(ID_TYPE)s'
	AND identifier.id IN ( %(IDS)s)
	;
'''%{'ID_TYPE':id_type,
	'IDS':(','.join(map(lambda s:"'"+s+"'",ids)))}
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  return rows

#############################################################################
def Target2Compounds(db,tid):
  """	Return list of CIDs active for selected target.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	compound.id AS cpdid
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
JOIN
	activity ON (activity.substance_id=substance.id)
JOIN
	target ON (target.id=activity.target_id)
WHERE
	target.id = %(TID)d
	AND s2c.is_active
ORDER BY
	cpdid
'''%{'TID':tid}

  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  cids=[]
  for row in rows:
    try:
      cid=int(row[0])
      cids.append(cid)
    except:
      pass ##should not happen
  return cids

#############################################################################
def Target2CompoundCount(db,tid):
  """	Return count of CIDs active for selected target.
  """
  cur=db.cursor()
  sql='''\
SELECT
	count(DISTINCT compound.id)
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
JOIN
	activity ON (activity.substance_id=substance.id)
JOIN
	target ON (target.id=activity.target_id)
WHERE
	target.id = %(TID)d
	AND s2c.is_active
'''%{'TID':tid}

  ExeSql(cur,sql)
  row=cur.fetchone()
  if row:
    count=int(row[0])
  else:
    count=0
  cur.close()
  return count

#############################################################################
def CpdId2ScafCpds(db,cid):
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	c2.id AS cid_nbr,
	c2.smiles AS smi_nbr,
	scaffold.id AS scafid,
	scaffold.smiles AS scafsmi
FROM
	compound AS c1,
	compound AS c2,
	scafid2cid AS s2c1,
	scafid2cid AS s2c2,
	scaffold
WHERE
	c1.id = %(CID)d
	AND s2c1.compound_id = c1.id
	AND s2c1.scaffold_id = scaffold.id
	AND scaffold.id = s2c2.scaffold_id
	AND s2c2.compound_id = c2.id
'''%{'CID':cid}
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  return rows

#############################################################################
def CpdId2McesCpds(db,cid):
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	c2.id AS cid_nbr,
	c2.smiles AS smi_nbr,
	mces.id AS mcesid,
	mces.mces AS mcessma
FROM
	compound AS c1,
	compound AS c2,
	mces
WHERE
	c1.id = %(CID)d
	AND c1.cluster_id = c2.cluster_id
	AND c1.cluster_id = mces.id
'''%{'CID':cid}
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  return rows

#############################################################################
def Target2Scaffolds(db,tid):
  return Targets2Scaffolds(dbhost,dbname,dbusr,dbpw,[tid])

#############################################################################
def Targets2Scaffolds(db,tids):
  """	Return list of ScafIDs from active compounds, for selected targets.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	scaffold.id AS scafid
FROM
	scaffold
JOIN
	scafid2cid ON (scafid2cid.scaffold_id=scaffold.id)
JOIN
	compound ON (compound.id=scafid2cid.compound_id)
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
JOIN
	activity ON (activity.substance_id=substance.id)
JOIN
	target ON (target.id=activity.target_id)
WHERE
	target.id IN ( %(TIDS)s )
	AND s2c.is_active
ORDER BY
	scafid
'''%{'TIDS':','.join(map(lambda x:str(x),tids))}

  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  scafids = set([])
  for row in rows:
    try:
      scafid=int(row[0])
      scafids.add(scafid)
    except:
      pass ##should not happen
  return list(scafids)

#############################################################################
def Compound2Scaffolds(db,cid):
  return Compounds2Scaffolds(dbhost,dbname,dbusr,dbpw,[cid])

#############################################################################
def Compounds2Scaffolds(db,cids):
  """	Return list of ScafIDs for selected compounds.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	scaffold.id AS scafid
FROM
	scaffold
JOIN
	scafid2cid ON (scafid2cid.scaffold_id=scaffold.id)
JOIN
	compound ON (compound.id=scafid2cid.compound_id)
WHERE
	compound.id IN ( %(CIDS)s )
ORDER BY
	scafid
'''%{'CIDS':','.join(map(lambda x:str(x),cids))}

  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  scafids = set([])
  for row in rows:
    try:
      scafid=int(row[0])
      scafids.add(scafid)
    except:
      pass ##should not happen
  return list(scafids)

#############################################################################
def Compound2LargestScaffold(db,cid):
  """	Return ScafID for largest scaffold for cid.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	scaffold.id AS scafid,
	scaffold.natoms
FROM
	scaffold
JOIN
	scafid2cid ON (scafid2cid.scaffold_id=scaffold.id)
JOIN
	compound ON (compound.id=scafid2cid.compound_id)
WHERE
	compound.id = %(CID)d
ORDER BY
	natoms DESC
'''%{'CID':cid}
  ExeSql(cur,sql)
  row=cur.fetchone()
  cur.close()
  scafid=None
  try:
    scafid=int(row[0])
  except:
    pass
  return scafid

#############################################################################
def Scaffold2Compounds(db,scafid):
  return Scaffolds2Compounds(dbhost,dbname,dbusr,dbpw,[scafid])

#############################################################################
def Scaffolds2Compounds(db,scafids):
  """	Return list of compounds for selected ScafIDs.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	compound.id AS cid
FROM
	compound
JOIN
	scafid2cid ON (compound.id=scafid2cid.compound_id)
JOIN
	scaffold ON (scafid2cid.scaffold_id=scaffold.id)
WHERE
	scaffold.id IN ( %(SCAFIDS)s )
ORDER BY
	cid
'''%{'SCAFIDS':','.join(map(lambda x:str(x),scafids))}

  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  cids = set([])
  for row in rows:
    try:
      cid=int(row[0])
      cids.add(cid)
    except:
      pass ##should not happen
  return list(cids)

#############################################################################
def Target2MCESs(db,tid):
  return Targets2MCESs(dbhost,dbname,dbusr,dbpw,[tid])

#############################################################################
def Targets2MCESs(db,tids):
  """	Return list of MCESIDs from active compounds for selected target.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	compound.cluster_id AS mcesid
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
JOIN
	activity ON (activity.substance_id=substance.id)
JOIN
	target ON (target.id=activity.target_id)
WHERE
	target.id IN ( %(TIDS)s )
	AND s2c.is_active
ORDER BY
	mcesid
'''%{'TIDS':','.join(map(lambda x:str(x),tids))}

  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  mcesids = set([])
  for row in rows:
    try:
      mcesid=int(row[0])
      mcesids.add(mcesid)
    except:
      pass ##should not happen
  return list(mcesids)

#############################################################################
def Compound2MCESs(db,cid):
  return Compounds2MCESs(dbhost,dbname,dbusr,dbpw,[cid])

#############################################################################
def Compounds2MCESs(db,cids):
  """	Return list of MCESIDs for selected compound.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	compound.cluster_id AS mcesid
FROM
	compound
WHERE
	compound.id IN ( %(CIDS)s )
'''%{'CIDS':','.join(map(lambda x:str(x),cids))}
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  mcesids = set([])
  for row in rows:
    try:
      mcesid=int(row[0])
      mcesids.add(mcesid)
    except:
      pass ##should not happen
  return list(mcesids)

#############################################################################
def MCES2Compounds(db,mcesid):
  return MCESs2Compounds(db,[mcesid])

#############################################################################
def MCESs2Compounds(db,mcesids):
  """	Return list of compounds for selected MCESID.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	compound.id AS cid
FROM
	compound
WHERE
	compound.cluster_id IN ( %(MCESIDS)s )
'''%{'MCESIDS':','.join(map(lambda x:str(x),mcesids))}
  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  if not rows: return []
  cids = set([])
  for row in rows:
    try:
      cid=int(row[0])
      cids.add(cid)
    except:
      pass ##should not happen
  return list(cids)

#############################################################################
def CompoundSearch(db,
	smiq=None,smisub=False,
	nameq=None,namesub=False,
	id=None,idtype='any'
	):
  """	Return selected compounds.
  """
  cur=db.cursor()
  sql='''\
SELECT
	compound.id,
	compound.iso_smiles
FROM
	compound
'''
  wheres=[]
  if smiq:
    if not smisub:
      wheres.append("compound.smiles=gnova.cansmiles('%s')"%smiq)
    else:
      wheres.append("gnova.bit_contains(compound.gfp,gnova.fp('%s'))"%smiq)
      wheres.append("gnova.matches(compound.smiles,'%s')"%smiq)

  sql+=('WHERE\n\t'+'\n\tAND '.join(wheres))
  sql+=('\nORDER BY compound.id ASC')

  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  return sql,rows

#############################################################################
def TargetSearch(db,
	tid,
	ttype='any',
	nameq=None,namesub=False,
	descq=None,descsub=False,
	species='any',
	id=None,idtype='any'
	):
  """	Return list of selected targets.
  """
  cur=db.cursor()
  sql='''\
SELECT DISTINCT 
	target.id,
	target.name,
	target.descr,
	target.species,
	target.type,
	identifier.id_type,
	identifier.id
FROM
	target
LEFT OUTER JOIN
	identifier ON (identifier.target_id=target.id)
'''
  wheres=[]
  if tid: wheres.append('target.id=%d'%int(tid))
  if nameq:
    if namesub:
      wheres.append("target.name ILIKE '%%%s%%'"%nameq)
    else:
      wheres.append("target.name='%s'"%nameq)
  if descq:
    if descsub:
      wheres.append("target.descr ILIKE '%%%s%%'"%descq)
    else:
      wheres.append("target.descr='%s'"%descq)
  if species!='any':
    wheres.append("target.species='%s'"%species)
  if ttype!='any':
    wheres.append("target.type='%s'"%ttype)
  if id:
    wheres.append("identifier.id='%s'"%id)
  if idtype!='any':
    wheres.append("identifier.id_type='%s'"%idtype)

  sql+=('WHERE\n\t'+'\n\tAND '.join(wheres))
  sql+=('\nORDER BY target.id,target.name ASC')

  ExeSql(cur,sql)
  rows=cur.fetchall()
  cur.close()
  return sql,rows

#############################################################################
def Extract2XGMML_Targets(db,fout,tids):
  """	Extract target, compound, scaffold, activity, for specified
	target IDs.
  """
  n_node_targ=0;
  n_node_cpd=0;
  n_edge_act=0;
  if not tids:
    ErrorExit('Target IDs must be specified.')
  if len(tids)>100:
    ErrorExit('Target IDs count exceeds 100; not allowed.')
  fout.write(cytoscape_utils.XGMML_Header(title='CARLSBAD Target-Compound Sub-Network',bgcolor='#CCDDFF'))
  cur=db.cursor()

  ### Targets:
  sql='''\
SELECT
	target.id,
	target.name,
	target.descr,
	target.species,
	target.type,
	identifier.id_type,
	identifier.id
FROM
	target
LEFT OUTER JOIN
	identifier ON (identifier.target_id=target.id)
WHERE
	target.id IN ( %(TIDS)s ) ;
'''%{'TIDS':(','.join(map(lambda x:str(x),tids))) }
  ExeSql(cur,sql)
  id_prev=None
  while True:
    row=cur.fetchone()
    if not row: break
    id,name,descr,species,ttype,idtype,id2 = row
    if id==id_prev: continue
    lines=[]
    lines.append('<node id="T%d" label="T%d">'%(id,id))
    lines.append('  <att type="string" name="ID" value="T%d" />'%(id))
    if id2:
      if re.match('UniProt$',idtype,re.I):
        lines.append('  <att type="string" name="%s" value="%s" />'%(idtype,id2))
    if name: lines.append('  <att type="string" name="name" value="%s" />'%(cgi.escape(name)))
    if descr: lines.append('  <att type="string" name="descr" value="%s" />'%(cgi.escape(descr)))
    lines.append('  <att type="string" name="species" value="%s" />'%(species))
    lines.append('  <att type="string" name="type" value="%s" />'%(ttype))
    lines.append('  <graphics type="octagon" h="80.0" w="80.0" fill="#e4e22a" outline="#666666" cy:nodeTransparency="0.4" cy:nodeLabelFont="SansSerif.bold-0-5" cy:borderLineType="solid" />')
    lines.append('</node>')
    cytoscape_utils.WriteLines(fout,lines,'  ')
    n_node_targ+=1
    id_prev=id

  ### Activities (compound-target edges):
  sql='''\
SELECT
	activity.id,
	activity.target_id,
	activity.assay_id,
	activity.substance_id,
	activity.confidence,
	activity.value,
	activity.effect,
	assay.name,
	target.name,
	compound.id,
	compound.smiles,
	synonym.name
FROM
	activity
LEFT OUTER JOIN
	target ON (target.id=activity.target_id)
LEFT OUTER JOIN
	assay ON (assay.id=activity.assay_id)
LEFT OUTER JOIN
	substance ON (substance.id=activity.substance_id)
LEFT OUTER JOIN
	synonym ON (substance.id=synonym.substance_id)
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (compound.id=s2c.compound_id)
WHERE
	activity.target_id IN ( %(TIDS)s )
	;
'''%{'TIDS':(','.join(map(lambda x:str(x),tids))) }
  ExeSql(cur,sql)
  id_prev=None
  cids={}
  while True:
    row=cur.fetchone()
    if not row: break
    id,tid,aid,sid,con,value,effect,aname,tname,cid,smi,cname = row
    if id==id_prev: continue
    id_prev=id
    lines=[]
    if not cids.has_key(cid):
      cids[cid]=True
      lines.append('<node id="C%d" label="C%d">'%(cid,cid))
      lines.append('  <att type="string" name="ID" value="C%d" />'%(cid))
      lines.append('  <att type="string" name="canonicalName" value="cpd_%06d" />'%(cid))
      lines.append('  <att type="string" name="smiles" value="%s" />'%(smi))
      if name: lines.append('  <att type="string" name="name" value="%s" />'%(cname))
      lines.append('  <graphics type="diamond" h="40.0" w="40.0" fill="#e4e22a" outline="#666666" cy:nodeTransparency="0.4" cy:nodeLabelFont="SansSerif.bold-0-5" cy:borderLineType="solid" />')
      lines.append('</node>')
      n_node_cpd+=1
    lines.append('<edge label="A%d" source="C%d" target="T%d">'%(id,cid,tid))
    lines.append('  <att type="string" name="ID" value="A%d" />'%(id))
    if aname: lines.append('  <att type="string" name="assayname" value="%s" />'%(aname))
    if effect: lines.append('  <att type="string" name="effect" value="%s" />'%(effect))
    if value: lines.append('  <att type="string" name="value" value="%s" />'%(cgi.escape(value)))
    if con: lines.append('  <att type="string" name="confidence" value="%s" />'%(con))
    lines.append('</edge>')
    cytoscape_utils.WriteLines(fout,lines,'  ')
    n_edge_act+=1

  ### Scaffolds (compound-scaffold edges):
  sql='''\
SELECT DISTINCT
	scaffold.id AS scafid,
	scaffold.smiles AS scafsmi,
	compound.id AS cpdid
FROM
	target
LEFT OUTER JOIN
	activity ON (target.id=activity.target_id)
LEFT OUTER JOIN
	assay ON (assay.id=activity.assay_id)
LEFT OUTER JOIN
	substance ON (substance.id=activity.substance_id)
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (compound.id=s2c.compound_id)
JOIN
	scafid2cid ON (compound.id=scafid2cid.compound_id)
JOIN
	scaffold ON (scaffold.id=scafid2cid.scaffold_id)
WHERE
	target.id IN ( %(TIDS)s )
	;
'''%{'TIDS':(','.join(map(lambda x:str(x),tids))) }
  ExeSql(cur,sql)
  n_node_scaf=0
  n_edge_scaf=0
  scafids={}
  while True:
    row=cur.fetchone()
    if not row: break
    scafid,smi,cid = row
    if not cids.has_key(cid): continue
    lines=[]
    if not scafids.has_key(scafid):
      scafids[scafid]=True
      lines.append('<node id="S%d" label="S%d">'%(scafid,scafid))
      lines.append('  <att type="string" name="ID" value="S%d" />'%(scafid))
      lines.append('  <att type="boolean" name="__has2DGraphics" value="true" cy:hidden="true"/>')
      lines.append('  <att type="string" name="canonicalName" value="scaf_%06d" />'%(scafid))
      lines.append('  <att type="string" name="smiles" value="%s" />'%(smi))
      lines.append('  <graphics type="ellipse" h="40.0" w="40.0" fill="#e4e22a" outline="#666666" cy:nodeTransparency="0.4" cy:nodeLabelFont="SansSerif.bold-0-5" cy:borderLineType="solid" />')
      lines.append('</node>')
      n_node_scaf+=1
    lines.append('<edge label="S%d_C%d" source="C%d" target="S%d">'%(scafid,cid,cid,scafid))
    lines.append('  <att type="string" name="ID" value="S%d_C%d" />'%(scafid,cid))
    lines.append('</edge>')
    cytoscape_utils.WriteLines(fout,lines,'  ')
    n_edge_scaf+=1
  cur.close()
  fout.write(cytoscape_utils.XGMML_Footer())
  return n_node_cpd,n_node_targ,n_node_scaf,n_edge_act,n_edge_scaf

#############################################################################
def GetTargetClassHierarchy(db):
  """ ChEMBL target classifications are encoded with double spaces signifying hierarchy levels. """
  tclasshash={}
  sql=('''\
SELECT DISTINCT
	target_classifier.id
FROM
	target_classifier
WHERE
	target_classifier.type = 'ChEMBL Class'
ORDER BY
	target_classifier.id
''')
  cur=db.cursor()
  ExeSql(cur,sql)
  id_prev=None
  while True:
    row=cur.fetchone()
    if not row: break
    tclass=row[0]
    fields=tclass.split('  ')
    p=tclasshash
    for j in range(len(fields)):
      if not p.has_key(fields[j]):
        p[fields[j]]={}
      p=p[fields[j]]
  cur.close()
  return tclasshash

#############################################################################
def ShowTargetClassHierarchy(db):

  def ShowSubClasses(c,supers):
    csubs=c.keys()
    csubs.sort()
    depth=len(supers)
    for csub in csubs:
      tclass=('  '.join(supers+[csub]))
      tids=TargetClass2TIDs(db,tclass)
      print >>sys.stderr, "%s%s [%d targets]"%(':'*(len(supers)+1),csub,len(tids))
      depth=ShowSubClasses(c[csub],supers+[csub])
    return depth

  tclasshash=GetTargetClassHierarchy(db)
  maxlevel=ShowSubClasses(tclasshash,[])
  print >>sys.stderr, 'max hierarchy level = %d'%maxlevel
  return

#############################################################################
def MaxScafID(db):
  sql='select max(id) from scaffold'
  cur=db.cursor()
  ExeSql(cur,sql)
  row=cur.fetchone()
  if not row: return 0 #ERROR
  scafid=row[0]
  return scafid

#############################################################################
def BMScaffoldCompoundCount(db,scafid,active=False,verbose=0):
  sql='''\
SELECT
	count(DISTINCT compound.id)
FROM
	compound
JOIN
	s2c ON (compound.id=s2c.compound_id)
JOIN
	substance ON (substance.id=s2c.substance_id)
JOIN
	cbactivity ON (substance.id=cbactivity.substance_id)
JOIN
	scafid2cid ON (compound.id=scafid2cid.compound_id)
JOIN
	scaffold ON (scaffold.id=scafid2cid.scaffold_id)
WHERE
	scafid2cid.is_largest
	AND scaffold.id = %(SCAFID)d
'''%{'SCAFID':scafid}
  if active: sql+=(' AND cbactivity.value >= 5.0')
  cur=db.cursor()
  ExeSql(cur,sql)
  row=cur.fetchone()
  if not row: return None #ERROR
  n=row[0]
  return n

#############################################################################
def BMScaffoldSubstanceCount(db,scafid,active,verbose=0):
  sql='''\
SELECT
	count(DISTINCT substance.id)
FROM
	substance
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (compound.id=s2c.compound_id)
JOIN
	cbactivity ON (substance.id=cbactivity.substance_id)
JOIN
	scafid2cid ON (compound.id=scafid2cid.compound_id)
JOIN
	scaffold ON (scaffold.id=scafid2cid.scaffold_id)
WHERE
	scafid2cid.is_largest
	AND scaffold.id = %(SCAFID)d
'''%{'SCAFID':scafid}
  if active: sql+=(' AND cbactivity.value >= 5.0')
  cur=db.cursor()
  ExeSql(cur,sql)
  row=cur.fetchone()
  if not row: return None #ERROR
  n=row[0]
  return n

#############################################################################
def BMScaffoldResultCount(db,scafid,active,verbose=0):
  sql='''\
SELECT
	count(DISTINCT cbactivity.id)
FROM
	cbactivity
JOIN
	substance ON (substance.id=cbactivity.substance_id)
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (compound.id=s2c.compound_id)
JOIN
	scafid2cid ON (compound.id=scafid2cid.compound_id)
JOIN
	scaffold ON (scaffold.id=scafid2cid.scaffold_id)
WHERE
	scafid2cid.is_largest
	AND scaffold.id = %(SCAFID)d
'''%{'SCAFID':scafid}
  if active: sql+=(' AND cbactivity.value >= 5.0')
  cur=db.cursor()
  ExeSql(cur,sql)
  row=cur.fetchone()
  if not row: return None #ERROR
  n=row[0]
  return n

#############################################################################
def BMScaffoldTargetCount(db,scafid,active,verbose=0):
  sql='''\
SELECT
	count(DISTINCT target.id)
FROM
	target
JOIN
	cbactivity ON (target.id=cbactivity.target_id)
JOIN
	substance ON (substance.id=cbactivity.substance_id)
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (compound.id=s2c.compound_id)
JOIN
	scafid2cid ON (compound.id=scafid2cid.compound_id)
JOIN
	scaffold ON (scaffold.id=scafid2cid.scaffold_id)
WHERE
	scafid2cid.is_largest
	AND scaffold.id = %(SCAFID)d
'''%{'SCAFID':scafid}
  if active: sql+=(' AND cbactivity.value >= 5.0')
  cur=db.cursor()
  ExeSql(cur,sql)
  row=cur.fetchone()
  if not row: return None #ERROR
  n=row[0]
  return n

#############################################################################
def BMScaffoldAssayCount(db,scafid,active,verbose=0):
  sql='''\
SELECT
	count(DISTINCT assay.id)
FROM
	assay
JOIN
	target ON (assay.target_id=target.id)
JOIN
	cbactivity ON (target.id=cbactivity.target_id)
JOIN
	substance ON (substance.id=cbactivity.substance_id)
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (compound.id=s2c.compound_id)
JOIN
	scafid2cid ON (compound.id=scafid2cid.compound_id)
JOIN
	scaffold ON (scaffold.id=scafid2cid.scaffold_id)
WHERE
	scafid2cid.is_largest
	AND scaffold.id = %(SCAFID)d
'''%{'SCAFID':scafid}
  if active: sql+=(' AND cbactivity.value >= 5.0')
  cur=db.cursor()
  ExeSql(cur,sql)
  row=cur.fetchone()
  if not row: return None #ERROR
  n=row[0]
  return n

#############################################################################
def BMScaffoldStats(db,fout,verbose=0):
  """	Similar to Badapple statistics, but for Bemis-Murko scaffolds,
	i.e. largest scaffold in molecule, and using all Carlsbad
	activity data.

	bmac = Given scaffold is largest (B-M) in how many active compounds?
	bmas = Given scaffold is largest (B-M) in how many active substances?
	bmar = Given scaffold is largest (B-M) in how many active results?
	bmat = Given scaffold is largest (B-M) on how many active targets?
	bmaa = Given scaffold is largest (B-M), any active well, in how many assays?

	bmtc = Given scaffold is largest (B-M) in how many total compounds?
	bmts = Given scaffold is largest (B-M) in how many total substances?
	bmtr = Given scaffold is largest (B-M) in how many total results?
	bmtt = Given scaffold is largest (B-M) tested on how many targets?
	bmta = Given scaffold is largest (B-M), tested in how many assays?

	a.k.a.:
	ncpd_cb_active 
	nsbs_cb_active 
	nact_cb_active 
	ntgt_cb_active 
	nass_cb_active 

	ncpd_cb_total 
	nsbs_cb_total 
	nact_cb_total 
	ntgt_cb_total 
	nass_cb_total 

	12 fields:
	scafsmi,scafid,bmac,bmas,bmar,bmat,bmaa,bmtc,bmts,bmtr,bmtt,bmta
"""
  fout.write("scafsmi,scafid,bmac,bmas,bmar,bmat,bmaa,bmtc,bmts,bmtr,bmtt,bmta\n")

  scafid_max = MaxScafID(db)
  for scafid in range(1,scafid_max+1):
    is_bm_anymol=ScafLargestAnyMol(scafid,db)
    if not is_bm_anymol: continue  ##Not B-M scaffold.

    scafsmi = ScafId2Smi(db,scafid)

    active=True
    bmac = BMScaffoldCompoundCount(db,scafid,active,verbose)
    bmas = BMScaffoldSubstanceCount(db,scafid,active,verbose)
    bmar = BMScaffoldResultCount(db,scafid,active,verbose)
    bmat = BMScaffoldTargetCount(db,scafid,active,verbose)
    bmaa = BMScaffoldAssayCount(db,scafid,active,verbose)

    active=False
    bmtc = BMScaffoldCompoundCount(db,scafid,active,verbose)
    bmts = BMScaffoldSubstanceCount(db,scafid,active,verbose)
    bmtr = BMScaffoldResultCount(db,scafid,active,verbose)
    bmtt = BMScaffoldTargetCount(db,scafid,active,verbose)
    bmta = BMScaffoldAssayCount(db,scafid,active,verbose)

    fout.write("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d\n"%(scafsmi,scafid,bmac,bmas,bmar,bmat,bmaa,bmtc,bmts,bmtr,bmtt,bmta))
    fout.flush()
    ##if verbose: print >>sys.stderr, "%s,%d,%d,%d,%d,%d,%d"%(scafsmi,scafid,bmac,bmas,bmar,bmat,bmaa)

  fout.close()

#############################################################################
def ProfileDB2Htm(db):
  cur=db.cursor()

  htm=''

  sql='''\
SELECT DISTINCT
	dataset.name,
	dataset.version,
	dataset.release_date
FROM
	dataset
ORDER BY
	dataset.name
'''
  ExeSql(cur,sql)
  thtm='<TABLE>\n'
  thtm+='<TR><TH>name</TH><TH>version</TH><TH>release data</TH></TR>\n'
  while True:
    row=cur.fetchone()
    if not row: break
    name,ver,reldate = row
    thtm+=('<TR><TD>%s</TD><TD>%s</TD><TD>%s</TD></TR>\n'%(name,ver,reldate))
  thtm+='</TABLE>\n'
  htm+=thtm

  sql='''\
SELECT
	q1.compound_count,
	q2.target_count,
	q3.activity_count,
	q4.scaffold_count,
	q5.mces_count
FROM
	( SELECT count(*) AS compound_count FROM compound ) AS q1,
	( SELECT count(*) AS target_count FROM target ) AS q2,
	( SELECT count(*) AS activity_count FROM activity ) AS q3,
	( SELECT count(*) AS scaffold_count FROM scaffold ) AS q4,
	( SELECT count(*) AS mces_count FROM mces ) AS q5
'''
  ExeSql(cur,sql)
  thtm='<TABLE>\n'
  row=cur.fetchone()
  compound_count,target_count,activity_count,scaffold_count,mces_count = row

  thtm+=('<TR><TD>%s</TD><TD>%d</TD></TR>\n'%('total CARLSBAD targets',target_count))
  thtm+=('<TR><TD>%s</TD><TD>%d</TD></TR>\n'%('total CARLSBAD compounds',compound_count))
  thtm+=('<TR><TD>%s</TD><TD>%d</TD></TR>\n'%('total CARLSBAD activities',activity_count))
  thtm+=('<TR><TD>%s</TD><TD>%d</TD></TR>\n'%('total CARLSBAD scaffolds',scaffold_count))
  thtm+=('<TR><TD>%s</TD><TD>%d</TD></TR>\n'%('total CARLSBAD mces clusters',mces_count))
  thtm+='</TABLE>\n'
  htm+=thtm

  sql='''\
SELECT DISTINCT
	q1.id_type,
	q2.count_tid,
	q1.count_sid
FROM
	( SELECT DISTINCT
		identifier.id_type,
		count(identifier.substance_id) AS count_sid
	FROM
		identifier
	GROUP BY
		identifier.id_type
	) AS q1,
	( SELECT DISTINCT
		identifier.id_type,
		count(identifier.target_id) AS count_tid
	FROM
		identifier
	GROUP BY
		identifier.id_type
		) AS q2
WHERE
	q1.id_type=q2.id_type
'''
  ExeSql(cur,sql)
  thtm='<TABLE>\n'
  thtm+='<TR><TH>id_type</TH><TH>count_tid</TH><TH>count_sid</TH></TR>\n'
  while True:
    row=cur.fetchone()
    if not row: break
    id_type,count_tid,count_sid = row
    thtm+=('<TR><TD>%s</TD><TD>%s</TD><TD>%s</TD></TR>\n'%(id_type,count_tid,count_sid))
  thtm+='</TABLE>\n'
  htm+=thtm

  sql='''\
SELECT
	attr_type.name AS attr_name,
	attr_type.display_name AS attr_display_name,
	attr_type.description AS attr_description,
	q1.cpd_count,
	round(q2.avg_value,2) AS avg_val
FROM
	attr_type,
	( SELECT
		attr_value.attr_type_id,
		count(attr_value.compound_id) AS cpd_count
	FROM
		attr_value
	GROUP BY
		attr_value.attr_type_id
	) AS q1,
	( SELECT
		attr_value.attr_type_id,
		avg(COALESCE(attr_value.number_value,attr_value.integer_value,0)) AS avg_value
	FROM
		attr_value
	GROUP BY
		attr_value.attr_type_id
	) AS q2
WHERE
	attr_type.id=q1.attr_type_id
	AND attr_type.id=q2.attr_type_id
ORDER BY
	attr_name
'''
  ExeSql(cur,sql)
  thtm='<TABLE>\n'
  thtm+='<TR><TH>id_type</TH><TH>count_tid</TH><TH>count_sid</TH></TR>\n'
  while True:
    row=cur.fetchone()
    if not row: break
    attr_name,attr_display_name,attr_description,cpd_count,avg_val = row
    thtm+=('<TR><TD>%s</TD><TD>%s</TD><TD>%s</TD><TD>%s</TD><TD>%s</TD></TR>\n'%(attr_name,attr_display_name,attr_description,cpd_count,avg_val))
  thtm+='</TABLE>\n'
  htm+=thtm
  cur.close()
  return htm

#############################################################################
def ProfileTargets(db,fout,verbose=0):
  trows=ListTargets(db) #tid,tname,descr,species,ttype,id_type,id
  tids_visited={};
  dtable=[];
  species_counts={}; ttype_counts={};
  for trow in trows:
    tid,tname,descr,species,ttype,id_type,id = trow
    if tids_visited.has_key(tid): continue
    cids=Target2Compounds(db,tid)
    scafids=Target2Scaffolds(db,tid)
    mcesids=Target2MCESs(db,tid)
    tids_visited[tid]=True
    if descr==None: descr="None"
    descr=re.sub('"','',descr)
    drow=[tid,re.sub('"','',tname),descr,species,ttype,len(cids),len(scafids),len(mcesids)]
    if verbose:
      print >>sys.stderr, "%d (%s) n_cpd=%d; n_scaf=%d; n_mces=%d ..."%(tid,tname,len(cids),len(scafids),len(mcesids))
    dtable.append(drow)
    if species not in species_counts: species_counts[species]=0
    species_counts[species]+=1
    if ttype not in ttype_counts: ttype_counts[ttype]=0
    ttype_counts[ttype]+=1
  dtable.sort(key=(lambda a:a[5]),reverse=True) #sort by n_cpd
  fout.write("tid,tname,descr,species,ttype,active_cpd_count,active_scaf_count,active_mces_count\n")
  for drow in dtable:
    tid,tname,descr,species,ttype,n_cpd,n_scaf,n_mces = drow
    fout.write("%d,\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%d\n"%(tid,tname.rstrip(),descr.rstrip(),species.rstrip(),ttype.rstrip(),n_cpd,n_scaf,n_mces))
  for species in species_counts.keys():
    print >>sys.stderr, "species = %12s: %5d"%(species,species_counts[species])
  for ttype in ttype_counts.keys():
    print >>sys.stderr, "ttype = %12s: %d"%(ttype,ttype_counts[ttype])

#############################################################################
def ProfileDrugs(db):
  rows=ListDrugs(db) #cid,sid,synonym,smiles
  cids_data={}
  for row in rows:
    cid,sid,synonym,smiles = row
    if not cids_data.has_key(cid):
      cids_data[cid]={}
      cids_data[cid]['sids']=[]
      cids_data[cid]['synonyms']=[]
      cids_data[cid]['smiles']=smiles
    cids_data[cid]['sids'].append(sid) ##maybe duplicates
    cids_data[cid]['synonyms'].append(synonym)
 
  dtable=[]
  for cid in cids_data.keys():
    tids=Compound2TIDs(db,cid)
    scafids=Compound2Scaffolds(db,cid)
    mcesids=Compound2MCESs(db,cid)
    drow=[cid,len(set(cids_data[cid]['sids'])),len(tids),len(scafids),len(mcesids)]
    dtable.append(drow)

  print "cid,active_sbs_count,active_tgt_count,scaf_count,mces_count,smiles"
  for drow in dtable:
    cid,n_sbs,n_tgt,n_scaf,n_mces = drow
    print "%d,%d,%d,%d,%d,\"%s\""%(cid,n_sbs,n_tgt,n_scaf,n_mces,cids_data[cid]['smiles'])

#############################################################################
def DrugList(db):
  n_dup_syn=0
  rows=ListDrugs(db) #cid,sid,synonym,smiles
  synonyms={}
  print "cid,synonym,smiles"
  for row in rows:
    cid,sid,synonym,smiles = row
    if len(synonym)<5:  #uninteresting, e.g. "45", "4b"?
      continue
    elif len(synonym)>20:  #uninteresting
      continue
    elif re.match('\d*$',synonym):  #no bare ID numbers
      continue
    elif '?' in synonym:
      continue
    if synonyms.has_key(synonym):
      n_dup_syn+=1
      continue
    else:
      synonyms[synonym]=True
    print "%d,\"%s\",\"%s\""%(cid,synonym.replace('"',''),smiles)

  #print >>sys.stderr, "DEBUG: duplicate synonym count: %d"%n_dup_syn
  #print >>sys.stderr, "DEBUG: output synonym count: %d"%len(synonyms.keys())

#############################################################################
def DrugLinks(db,fout,verbose=0):
  rows=ListDrugs(db) #cid,sid,synonym,smiles
  cids_data={}
  for row in rows:
    cid,sid,synonym,smiles = row
    if not cids_data.has_key(cid):
      cids_data[cid]={}
      cids_data[cid]['sids']=[]
      cids_data[cid]['synonyms']=[]
      cids_data[cid]['smiles']=smiles
    cids_data[cid]['sids'].append(sid) ##maybe duplicates
    if re.search(r'[A-Za-z]',synonym):
      cids_data[cid]['synonyms'].append(synonym.rstrip())
  CompoundLinks(db,cids_data=cids_data,fin=None,fout=fout,verbose=verbose)

#############################################################################
def CompoundIsDrugCheck(db,cids_data=None,fin=None,fout=sys.stdout,verbose=0):
  """	Determine whether input CIDs link to any SIDs which are
	flagged is_drug.  Output in_db, is_drug and drug SID[s].
  """
  if fin:
    cids_data={}
    while True:
      line=fin.readline()
      if not line: break
      cid=int(line)
      cids_data[cid]={'synonyms':[]}

  n_drug=0; n_indb=0;
  fout.write('cid,in_db,is_drug,SIDs,\n')
  for i,cid in enumerate(cids_data.keys()):
    sids_isdrug = CID2SIDs_IsDrugCheck(db,cid)
    if not sids_isdrug: #not in db
      fout.write('%d,False,,,\n'%(cid))
      continue
    else:
      is_drug=False
      sids_str=''
      for j,sid in enumerate(sids_isdrug.keys()):
        if j>0: sids_str+=';'
        sids_str+='%d'%sid
        is_drug|=sids_isdrug[sid]

      fout.write('%d,True,%s,"%s"\n'%(cid,is_drug,sids_str))
      n_indb+=1
      if is_drug: n_drug+=1

  print >>sys.stderr, 'n_cid: %d, n_indb: %d, n_drug: %d'%(len(cids_data),n_indb,n_drug)

#############################################################################
def CID2SIDs_IsDrugCheck(db,cid):
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	compound.id AS cid,
	substance.id AS sid,
	substance.is_drug
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
WHERE
	compound.id=%(CID)d
'''%{'CID':cid}
  ExeSql(cur,sql)
  rows=cur.fetchall()	##data rows
  cur.close()
  if not rows:
    print >>sys.stderr, 'DEBUG: cid_query: %d : NOT IN DB'%(cid)
    return {}
  sids={}
  for row in rows:
    sid=row[1]
    is_drug=row[2]
    sids[sid]=is_drug
    print >>sys.stderr, 'DEBUG: cid_query: %d, cid: %d, sid: %d, is_drug: %s'%(cid,row[0],row[1],row[2])
  return sids

#############################################################################
def CompoundLinks(db,cids_data=None,fin=None,fout=sys.stdout,verbose=0):
  """   Find links from CARLSBAD compounds (CIDs) to targets, scafs, mcess,
	neighbor cpds (related by CCPs), and neighbor targets (potential
	off-targets).
  """
  if fin:
    cids_data={}
    while True:
      line=fin.readline()
      if not line: break
      cid=int(line)
      cids_data[cid]={'synonyms':[]}

  fout.write('cid,name,n_tgts,n_nbr_cpds_scaf,n_nbr_cpds_mces,n_nbr_cpds,n_nbr_tgts,t\n')
  for i,cid in enumerate(cids_data.keys()):
    t0=time.time()
    n_tgts=0; n_nbr_cpds_scaf=0; n_nbr_cpds_mces=0; n_nbr_cpds=0; n_nbr_tgts=0;

    tgts = Compound2Targets(db,cid)
    for tgt in tgts:
      tid,tname,tspecies,ttype = tgt
      n_tgts+=1
    cpds_nbr_scaf = CpdId2ScafCpds(db,cid)
    cids_nbr_scaf = set([])
    for cpd in cpds_nbr_scaf:
      cid_nbr_scaf, smi_nbr_scaf, scafid, scafsmi = cpd
      cids_nbr_scaf.add(cid_nbr_scaf)
    n_nbr_cpds_scaf=len(cids_nbr_scaf)

    cpds_nbr_mces = CpdId2McesCpds(db,cid)
    cids_nbr_mces = set([])
    for mces in cpds_nbr_mces:
      cid_nbr_mces, smi_nbr_mces, mcesid, mcessma = cpd
      cids_nbr_mces.add(cid_nbr_mces)
    n_nbr_cpds_mces=len(cids_nbr_mces)

    tids_nbr = set([])
    cids_nbr = list(cids_nbr_scaf)+list(cids_nbr_mces)
    if cids_nbr:
      tgts_nbr = Compounds2Targets(db,cids_nbr)
      for tgt in tgts_nbr:
        tid,tname,tspecies,ttype = tgt
        tids_nbr.add(tid)
    n_nbr_tgts=len(tids_nbr)


    n_nbr_cpds=n_nbr_cpds_scaf+n_nbr_cpds_mces
    name=';'.join(cids_data[cid]['synonyms'])
    fout.write('%d,"%s",%d,%d,%d,%d,%d,%.1f\n'%(cid,name,n_tgts,n_nbr_cpds_scaf,n_nbr_cpds_mces,n_nbr_cpds,n_nbr_tgts,(time.time()-t0)))
 
#############################################################################
def KID2Targets(db,kid):
  cur=db.cursor()
  sql='''\
SELECT DISTINCT
	target.id AS tid
FROM
	target
JOIN
	target_classifier ON (target_classifier.target_id=target.id)
JOIN
	kegg_disease ON (kegg_disease.id=target_classifier.id)
WHERE
	target_classifier.id = '%(KID)s'
	AND target_classifier.type='KEGG Disease'
'''%{'KID':kid}
  ExeSql(cur,sql)
  rows=cur.fetchall()	##data rows
  cur.close()
  if not rows: return []
  tids=[]
  for row in rows:
    tids.append(row[0])
  return tids

#############################################################################
def DiseaseLinks(db,fout,verbose=0):
  diseases=ListDiseases(db)
  n_total_tgts=0; n_no_tgts=0; n=0;
  t0=time.time()
  fout.write('"KEGG_ID","disease_name",n_tgts,n_cpds,n_scafs,n_mcess,n_nbr_cpds\n')
  for kid,name_disease in diseases:
    name_disease=name_disease.rstrip()
    n+=1
    t0_this=time.time()
    n_tgts=0; n_cpds=0; n_nbr_cpds=0; n_scafs=0; n_mcess=0;
    tids=KID2Targets(db,kid)

    cids=[]; scafids=[]; mcesids=[];
    cids_nbr = set([])  # to dedup scaf and mces nbrs
    n_tgts=len(tids)

    for tid in tids:
      cids=Target2Compounds(db,tid)

      scafids=Compounds2Scaffolds(db,cids)
      if scafids:
        cids_nbr_this = Scaffolds2Compounds(db,scafids)
        for cid_nbr in cids_nbr_this: cids_nbr.add(cid_nbr)

      mcesids=Compounds2MCESs(db,cids)
      if mcesids:
        cids_nbr_this = MCESs2Compounds(db,mcesids)
        for cid_nbr in cids_nbr_this: cids_nbr.add(cid_nbr)

    n_cpds=len(cids)
    n_scafs=len(scafids)
    n_mcess=len(mcesids)
    n_nbr_cpds=len(cids_nbr)

    if n_tgts==0: n_no_tgts+=1
    fout.write('"%s","%s",%d,%d,%d,%d,%d'%(kid,name_disease,n_tgts,n_cpds,n_scafs,n_mcess,n_nbr_cpds))
    fout.write(',%.1f\n'%(time.time()-t0_this))
    fout.flush()
    if verbose:
      print >>sys.stderr, ('%d. %s:"%s"; n_tgts=%d; n_cpds=%d; n_scafs=%d; n_mcess=%d; n_nbr_cpds=%d; t=%.1f'%(n,kid,name_disease,n_tgts,n_cpds,n_scafs,n_mcess,n_nbr_cpds,time.time()-t0_this))
      if n%100==0:
        print >>sys.stderr, ('%.1f%% done; elapsed time: %s'%(100.0*n/len(diseases),time.strftime('%Hh:%Mm:%Ss',time.gmtime(time.time()-t0))))
    n_total_tgts+=n_tgts
  if verbose:
    print >>sys.stderr, 'n_diseases: %d'%n
    print >>sys.stderr, 'n_diseases w/ targets: %d'%(n-n_no_tgts)
    print >>sys.stderr, 'n_total_tgts: %d'%n_total_tgts
    print >>sys.stderr, ('total elapsed time: %s'%(time.strftime('%Hh:%Mm:%Ss',time.gmtime(time.time()-t0))))


#############################################################################
def KeggDiseaseLinks(db,fout,nskip=0,verbose=0):
  """   Find links from KEGG diseases to CARLSBAD targets and compounds.
	TODO: [ ] Also find nbr cpds.
  Note that in KEGG, disease->genes is a smaller set than disease->pathways->genes.
  """
  import kegg_api_utils

  kegg_base_uri='http://'+kegg_api_utils.API_HOST
  diseases=kegg_api_utils.GetDiseaseList(kegg_base_uri,verbose)
  n_total_genes=0; n_total_ncbigis=0; n_total_uniprots=0; n_total_tgts=0;
  n_no_tgts=0; n=0;
  t0=time.time()
  fout.write('"KEGG_ID","disease_name",n_genes,n_ncbigis,n_uniprots,n_cb_tgts,n_cb_cpds,n_scafs,n_mcess,n_nbr_cpds')
  if verbose>1:
    fout.write(',genes,ncbigis,uniprots,cb_tgts')
  fout.write(',t\n')
  for kid_disease,name_disease in diseases:
    n+=1
    if n<=nskip:
      continue
    t0_this=time.time()
    n_genes=0; n_ncbigis=0; n_tgts=0; n_cpds=0;
    n_nbr_cpds=0; n_scafs=0; n_mcess=0;
    ncbigis=set(); uniprots=set(); tids=set(); genes=set();
    genes_this=kegg_api_utils.Link2Genes(kid_disease,kegg_base_uri,verbose)
    for kid_gene in genes_this:
      genes.add(kid_gene)
      for ncbigi in kegg_api_utils.KID2NCBIGIs(kid_gene,kegg_base_uri,verbose):
        ncbigis.add(ncbigi)
      for uniprot in kegg_api_utils.KID2Uniprots(kid_gene,kegg_base_uri,verbose):
        uniprots.add(uniprot)
    ncbigis=list(ncbigis)
    ncbigis.sort()
    uniprots=list(uniprots)
    uniprots.sort()
    genes=list(genes)
    genes.sort()
    n_genes=len(genes)
    n_ncbigis=len(ncbigis)
    n_uniprots=len(uniprots)
    if ncbigis:
      target_rows=TargetExternalIDLookup(db,'NCBI gi',ncbigis)
      for row in target_rows: tids.add(row[0])
    if uniprots:
      target_rows=TargetExternalIDLookup(db,'UniProt',uniprots)
      for row in target_rows: tids.add(row[0])
    tids=list(tids)
    cids=[]; scafids=[]; mcesids=[];
    cids_nbr = set([])  # to dedup scaf and mces nbrs
    n_tgts=len(tids)

    for tid in tids:
      cids=Target2Compounds(db,tid)

      scafids=Compounds2Scaffolds(db,cids)
      if scafids:
        cids_nbr_this = Scaffolds2Compounds(db,scafids)
        for cid_nbr in cids_nbr_this: cids_nbr.add(cid_nbr)

      mcesids=Compounds2MCESs(db,cids)
      if mcesids:
        cids_nbr_this = MCESs2Compounds(db,mcesids)
        for cid_nbr in cids_nbr_this: cids_nbr.add(cid_nbr)

    n_cpds=len(cids)
    n_scafs=len(scafids)
    n_mcess=len(mcesids)
    n_nbr_cpds=len(cids_nbr)

    if n_tgts==0: n_no_tgts+=1
    fout.write('"%s","%s",%d,%d,%d,%d,%d,%d,%d,%d'%(kid_disease,name_disease,n_genes,n_ncbigis,n_uniprots,n_tgts,n_cpds,n_scafs,n_mcess,n_nbr_cpds))
    if verbose>1:
      fout.write(',"%s"'%(str(genes).replace(' ','')))
      fout.write(',"%s"'%(str(ncbigis).replace(' ','')))
      fout.write(',"%s"'%(str(uniprots).replace(' ','')))
      fout.write(',"%s"'%(str(tids).replace(' ','')))
    fout.write(',%.1f\n'%(time.time()-t0_this))
    fout.flush()
    if verbose:
      print >>sys.stderr, ('%d. %s:"%s"; n_genes=%d; n_ncbigis=%d; n_uniprots=%d; n_tgts=%d; n_cpds=%d; n_scafs=%d; n_mcess=%d; n_nbr_cpds=%d; t=%.1f'%(n,kid_disease,name_disease,n_genes,n_ncbigis,n_uniprots,n_tgts,n_cpds,n_scafs,n_mcess,n_nbr_cpds,time.time()-t0_this))
      if n%100==0:
        print >>sys.stderr, ('%.1f%% done; elapsed time: %s'%(100.0*n/len(diseases),time.strftime('%Hh:%Mm:%Ss',time.gmtime(time.time()-t0))))
    n_total_genes+=n_genes
    n_total_ncbigis+=n_ncbigis
    n_total_uniprots+=n_uniprots
    n_total_tgts+=n_tgts
  if verbose:
    print >>sys.stderr, 'n_diseases: %d'%n
    print >>sys.stderr, 'n_diseases w/ targets: %d'%(n-n_no_tgts)
    print >>sys.stderr, 'n_total_genes: %d'%n_total_genes
    print >>sys.stderr, 'n_total_ncbigis: %d'%n_total_ncbigis
    print >>sys.stderr, 'n_total_uniprots: %d'%n_total_uniprots
    print >>sys.stderr, 'n_total_tgts: %d'%n_total_tgts
    print >>sys.stderr, ('total elapsed time: %s'%(time.strftime('%Hh:%Mm:%Ss',time.gmtime(time.time()-t0))))

#############################################################################
