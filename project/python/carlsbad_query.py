#!/usr/bin/env python
"""
	carlsbad_query.py - carlsbad database utility 

	By calling cansmi from Chord, we assure that the canonicalization
	algorithm is the same as used when loading the db, and hence the
	lookup is robust, even if OpenChord has been upgraded to a new version since.

	Jeremy Yang
	 5 Jan 2015
"""
import os,sys,re,getopt,cgi
import pgdb

import cytoscape_utils
import carlsbad_utils

PROG=os.path.basename(sys.argv[0])

#target_class_type='Uniprot Family'
target_class_type='ChEMBL Class'

DBHOST='habanero.health.unm.edu'
DBNAME='carlsbad'
DBUSR='dbc'
DBPW='chem!nfo';

#############################################################################
def Test(db):
  print carlsbad_utils.DescribeCounts(db)
  print carlsbad_utils.DescribeDB(db)
  smis = [
'Cc1cccc(c1Nc2c3cncn3c4ccc(c(c4n2)OC)OC)C',
'CC1=CC=CC=C1NC2=NC(=NC(=N2)N)CSC3=NN=NN3C4=CC=CC=C4',
'C1CCNC(C1)C#CCN2CCCC2=O'
        ]
  for smi in smis:
    cid=carlsbad_utils.CpdSmi2Id(db,smi)
    print "compound: %s"%smi
    if not cid:
      print "\tnot found"
      continue
    print "\tcid:",cid
    scafid=carlsbad_utils.Compound2LargestScaffold(db,cid)
    print "\tlargest scaf id: %d"%scafid
    scafsmi=carlsbad_utils.ScafId2Smi(db,scafid)
    print "\tlargest scaf smi: %s"%scafsmi

  scafsmis = [
'c1ccc(cc1)OC2CCCC2',
'c1ccc(cc1)c2cccc(c2)CNCc3[nH]c4ccccc4n3',
'C1CC1'
        ]
  for smi in scafsmis:
    scafid=carlsbad_utils.ScafSmi2Id(db,smi)
    print "scaffold: %s"%smi
    if not scafid:
      print "\tnot found"
      continue
    print "\tscafid: %d"%scafid

#############################################################################
def Help(msg=''):
  if msg: print msg
  print '''\
%(PROG)s
required (one of):
        --counts ..................
        --describe ................
        --test ....................
        --lookup_cpd .............. requires --smi
        --lookup_scaf ............. requires --smi
        --lookup_scafs ............ requires --smifile
        --listtargets .............
        --listspecies .............
        --listidtypes .............
        --listtypes ...............
        --listclasses .............
        --profile_targets ......... target info (CSV)
        --profile2html ............
        --profile_drugs ...........
        --druglist ................
        --tids2xgmml .............. extract network for selected target IDs (requires --ids)
        --cpd_links ............... cpd-target, cpd-scaf, cpd-mces links
        --cpd_report .............. 
        --tgt_report .............. 
        --drug_links .............. cpd-target, cpd-scaf, cpd-mces links
        --disease_links ........... disease-target links
        --kegg_disease_links ...... kegg_disease-kegg_gene-CB_target links
        --kegg_disease_skip N ..... skip N diseases
        --set_largest_scaf_flags .. set scafid2cid.is_largest column for DB
        --scafsmifile2ids ......... input file should be SMILES
        --bmscaffoldstats ......... Bemis-Murko (largest) scaffold activity stats
        --cpd_isdrug_check ........ check is_drug flag for input CIDs

options:
	--i IFILE ................. input IDs (e.g. for --cpd_links, CIDs)
	--o OFILE ................. output (XGMML|CSV)
	--id ID ................... input ID (e.g. for --cpd_report, CID)
	--ids IDS ................. input IDs, comma-separated (e.g. for --tids2xgmml)
	--smi SMI ................. 
	--smifile SMIFILE ......... 
	--dbhost DBHOST ........... [%(DBHOST)s]
	--dbname DBNAME ........... [%(DBNAME)s]
	--dbusr DBUSR ............. [%(DBUSR)s]
	--dbpw DBPW ............... 
        --v ....................... verbose
        --h ....................... this help
'''%{	'PROG':PROG,
	'DBHOST':DBHOST,
	'DBNAME':DBNAME,
	'DBUSR':DBUSR
	}
  sys.exit()

#############################################################################
if __name__=='__main__':

  def ErrorExit(msg):
    print >>sys.stderr,msg
    sys.exit(1)

  counts=False; describe=False; test=False; lookup_cpd=False;
  lookup_scaf=False; lookup_scafs=False;
  smi=None; smifile=None;
  listtargets=False; profile_targets=False; profile_drugs=False; druglist=False; profile2html=False;
  listspecies=False; listidtypes=False; listtypes=False; listclasses=False;
  cpd_links=False; cpd_report=False; tgt_report=False; drug_links=False; set_largest_scaf_flags=False;
  cpd_isdrug_check=False;
  scafsmifile2ids=False;
  ifile=None; id_query=None; ofile=None; ids=None;
  kegg_disease_links=False; disease_links=False;
  kegg_disease_skip=0;
  bmscaffoldstats=False;
  tids2xgmml=None; verbose=0;
  opts,pargs=getopt.getopt(sys.argv[1:],'',[
	'dbhost=','dbname=','dbusr=','dbpw=',
	'i=', 'o=',
	'id=', 'ids=',
	'lookup_cpd', 'lookup_scaf', 'lookup_scafs',
	'smi=','smifile=',
	'tids2xgmml=',
	'listtargets', 'profile_targets', 'profile_drugs', 'druglist', 'profile2html',
	'listspecies', 'listidtypes', 'listtypes', 'listclasses',
	'cpd_links', 'cpd_report', 'cpd_isdrug_check',
	'tgt_report', 'drug_links', 'scafsmifile2ids',
	'set_largest_scaf_flags', 'kegg_disease_links', 'disease_links', 'bmscaffoldstats',
	'kegg_disease_skip=',
	'counts','test','describe','h=','help','v','vv'])
  if not opts: Help()
  for (opt,val) in opts:
    if opt=='--help': Help()
    elif opt=='--i': ifile=val
    elif opt=='--id': id_query=int(val)
    elif opt=='--ids': ids=map(lambda i:int(i),re.split(r'[,\s]',val))
    elif opt=='--o': ofile=val
    elif opt=='--dbhost': DBHOST=val
    elif opt=='--dbname': DBNAME=val
    elif opt=='--dbusr': DBUSR=val
    elif opt=='--dbpw': DBPW=val
    elif opt=='--counts': counts=True
    elif opt=='--describe': describe=True
    elif opt=='--test': test=True
    elif opt=='--lookup_cpd': lookup_cpd=True
    elif opt=='--lookup_scaf': lookup_scaf=True
    elif opt=='--lookup_scafs': lookup_scafs=True
    elif opt=='--smi': smi=val
    elif opt=='--smifile': smifile=val
    elif opt=='--tids2xgmml': tids2xgmml=val
    elif opt=='--listtargets': listtargets=True
    elif opt=='--listspecies': listspecies=True
    elif opt=='--listidtypes': listidtypes=True
    elif opt=='--listtypes': listtypes=True
    elif opt=='--listclasses': listclasses=True
    elif opt=='--cpd_links': cpd_links=True
    elif opt=='--cpd_report': cpd_report=True
    elif opt=='--cpd_isdrug_check': cpd_isdrug_check=True
    elif opt=='--tgt_report': tgt_report=True
    elif opt=='--drug_links': drug_links=True
    elif opt=='--set_largest_scaf_flags': set_largest_scaf_flags=True
    elif opt=='--scafsmifile2ids': scafsmifile2ids=True
    elif opt=='--disease_links': disease_links=True
    elif opt=='--bmscaffoldstats': bmscaffoldstats=True
    elif opt=='--kegg_disease_links': kegg_disease_links=True
    elif opt=='--kegg_disease_skip': kegg_disease_skip=int(val)
    elif opt=='--profile_targets': profile_targets=True
    elif opt=='--profile_drugs': profile_drugs=True
    elif opt=='--druglist': druglist=True
    elif opt=='--profile2html': profile2html=True
    elif opt=='--v': verbose=1
    elif opt=='--vv': verbose=2
    else: Help('Illegal option: %s'%(opt))

  if ofile:
    fout=open(ofile,'w+')
    if not fout:
      ErrorExit('Could not open output file: %s'%ofile)
  else:
    fout=sys.stdout

  if ifile:
    fin=open(ifile)
    if not fin:
      ErrorExit('Could not open input file: %s'%ifile)

  db=carlsbad_utils.Connect(DBHOST,DBNAME,DBUSR,DBPW)[0]
  if not db:
    ErrorExit('Could not connect to db.')

  if test:
    Test(db)
  elif counts:
    print carlsbad_utils.DescribeCounts(db)
  elif describe:
    print carlsbad_utils.DescribeDB(db)
  elif lookup_scaf:
    if not smi: ErrorExit('--smi required for --lookup_scaf.')

    scafid=carlsbad_utils.ScafSmi2Id(db,smi)
    if scafid:
      print "\tid:",scafid
    else:
      print "\tnot found"
  elif lookup_scafs:
    if not smifile: ErrorExit('--smifile required for --lookup_scafs.')
    n_found=0; n_scaf=0;
    fin=open(smifile)
    while True:
      line=fin.readline()
      if not line: break
      n_scaf+=1
      smi=re.sub(r'\s.*$','',line)
      scafid=carlsbad_utils.ScafSmi2Id(db,smi)
      if scafid:
        n_found+=1
        print "scaffold: [scafid: %7d]"%scafid,
      else:
        print "scaffold: [not_found_in_db]",
      print "%s"%smi
    print "scaffolds: %d ; found: %d ; not found: %d"%(n_scaf,n_found,n_scaf-n_found)
    fin.close()

  elif lookup_cpd:
    if not smi: ErrorExit('--smi required for --lookup_cpd.')
    cid=carlsbad_utils.CpdSmi2Id(db,smi)
    print "compound:",smi
    if cid:
      print "\tid:",cid
      names=carlsbad_utils.Compound2Synonyms(db,cid)
      print "\tsynonyms:",('\n\t\t'+'\n\t\t'.join(names))
      trows=carlsbad_utils.Compound2Targets(db,cid)
      print "\ttargets:"
      if trows:
        for trow in trows:
          print "\t\t",('\t'.join(map(lambda x:str(x),trow)))
      else:
        print "\t\t(none)"
    else:
      print "\tnot found"

  elif cpd_report:
    if not id_query: Help('ERROR: --id required with --cpd_report')
    cid=id_query
    print "\tid:",cid
    names=carlsbad_utils.Compound2Synonyms(db,cid)
    print "\tsynonyms:",('\n\t\t'+'\n\t\t'.join(names))
    trows=carlsbad_utils.Compound2Targets(db,cid)
    print "\ttargets:"
    if trows:
      for trow in trows:
        print "\t\t",('\t'.join(map(lambda x:str(x),trow)))
    else:
      print "\t\t(none)"
    rows=carlsbad_utils.CompoundInfo(db,cid)
    for row in rows:
      print "\t\t",('\t'.join(map(lambda x:str(x),row)))

  elif cpd_isdrug_check:
    carlsbad_utils.CompoundIsDrugCheck(db,None,fin,fout,verbose)

  elif tids2xgmml:
    if not ids: ErrorExit('--ids required for --tids2xgmml.')
    n_node_cpd,n_node_targ,n_node_scaf,n_edge_act,n_edge_scaf = carlsbad_utils.Extract2XGMML_Targets(db,fout,ids)
    print >>sys.stderr, "compound nodes: %d"%n_node_cpd
    print >>sys.stderr, "target nodes: %d"%n_node_targ
    print >>sys.stderr, "scaffold nodes: %d"%n_node_scaf
    print >>sys.stderr, "activity edges: %d"%n_edge_act
    print >>sys.stderr, "scaf2cpd edges: %d"%n_edge_scaf
    print >>sys.stderr, "total nodes: %d"%(n_node_cpd+n_node_targ+n_node_scaf)
    print >>sys.stderr, "total edges: %d"%(n_edge_act+n_edge_scaf)

  elif listtargets:
    rows=carlsbad_utils.ListTargets(db)
    for row in rows:
      for field in row:
        print field,
      print

  elif listspecies:
    vals=carlsbad_utils.ListTargetSpecies(db)
    for val in vals:
      print val

  elif listidtypes:
    vals=carlsbad_utils.ListTargetIDTypes(db)
    for val in vals:
      print val

  elif listtypes:
    vals=carlsbad_utils.ListTargetTypes(db)
    for val in vals:
      print val

  elif listclasses:
    #tclasses=carlsbad_utils.ListTargetClasses(db)
    #for tclass in tclasses:
    #  tids=carlsbad_utils.TargetClass2TIDs(db,tclass)
    #  print '%s:\n\ttargets: %2d'%(tclass,len(tids))
    carlsbad_utils.ShowTargetClassHierarchy(db)

  elif profile_targets:
    carlsbad_utils.ProfileTargets(db,fout,verbose)

  elif profile2html:
    print carlsbad_utils.ProfileDB2Htm(db)

  elif profile_drugs:
    carlsbad_utils.ProfileDrugs(db)

  elif druglist:
    carlsbad_utils.DrugList(db)

  elif cpd_links:
    carlsbad_utils.CompoundLinks(db,None,fin,fout,verbose)

  elif drug_links:
    carlsbad_utils.DrugLinks(db,fout,verbose)

  elif set_largest_scaf_flags:
    carlsbad_utils.SetLargestScaffoldColumn4DB(db,verbose)

  elif scafsmifile2ids:
    carlsbad_utils.ScafSmiFile2Ids(fin,fout,db,verbose)

  elif kegg_disease_links:
    carlsbad_utils.KeggDiseaseLinks(db,fout,kegg_disease_skip,verbose)

  elif disease_links:
    carlsbad_utils.DiseaseLinks(db,fout,verbose)

  elif bmscaffoldstats:
    carlsbad_utils.BMScaffoldStats(db,fout,verbose)

  else:
    ErrorExit('No operation specified.')

  if ofile:
    fout.close()
