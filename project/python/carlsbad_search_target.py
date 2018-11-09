#!/usr/bin/env python
"""
	carlsbad_search.py - carlsbad database search utility 


	Jeremy Yang
	 7 Apr 2011
"""
import os,sys,re,getopt,cgi
import pgdb
import cytoscape_utils
import carlsbad_utils

PROG=os.path.basename(sys.argv[0])

#############################################################################
def Help(msg=''):
  if msg: print msg
  print '''\
%(PROG)s
required (one of):
        --type=<TYPEQUERY>      ... target type (protein|enzyme|receptor|ion channel)
        --name=<NAMEQUERY>      ... target name query
        --desc=<DESCQUERY>      ... target description query
        --species=<SPECIES>      ... target species query (human|mouse|rat|hybrid)
        --id=<ID>      ... target ID query
        --idtype=<IDTYPE>      ... target ID type query (ChEMBL Target|UniProt|OMIM|Entrez Gene|...)
	--out_xgmml=<XMLFILE>          ... XGMML for Cytoscape 
options:
        --v             ... verbose
        --h             ... this help
'''%{'PROG':PROG}
  sys.exit()

#############################################################################
def ErrorExit(msg):
  print >>sys.stderr,msg
  sys.exit(1)

#############################################################################
if __name__=='__main__':
  type=None; name=None; desc=None; species=None; id=None; idtype=None;
  ofile_xgmml='';
  opts,pargs=getopt.getopt(sys.argv[1:],'',['out_xgmml=','type=','name=','desc=',
	'species=','id=','idtype=','h=','help','v','vv'])
  if not opts: Help()
  for (opt,val) in opts:
    if opt=='--help': Help()
    elif opt=='--type': type=val
    elif opt=='--name': name=val
    elif opt=='--desc': desc=val
    elif opt=='--species': species=val
    elif opt=='--id': id=val
    elif opt=='--idtype': idtype=val
    elif opt=='--out_xgmml': ofile_xgmml=val
    elif opt=='--v': verbose=1
    elif opt=='--vv': verbose=2
    else: Help('Illegal option: %s'%(opt))

  if ofile_xgmml:
    fout=open(ofile_xgmml,'w+')
    if not fout:
      ErrorExit('Could not open output file: %s'%ofile_xgmml)
  else:
    fout=sys.stdout

  DBHOST,DBNAME,DBUSR,DBPW = 'agave.health.unm.edu','carlsbad','dbc','chem!nfo'

  if not name and not type and not desc and not species and not id and not idtype:
    Help('ERROR: no query.')

  if not type: type='any'
  if not species: species='any'
  if not idtype: idtype='any'

  sql,rows = carlsbad_utils.TargetSearch(DBHOST,DBNAME,DBUSR,DBPW,
	None,
	ttype=type,
	nameq=name,namesub=True,
	descq=desc,descsub=True,
	species=species,
	id=id,idtype=idtype
	)
  if verbose:
    print >>sys.stderr,sql

  tid_prev=None
  tids=[]
  for i,row in enumerate(rows):
    tid,name,descr,species,type,idtype,id = row
    if tid!=tid_prev:
      tids.append(tid)

  if len(tids)>100:
    ErrorExit('ERROR: len(tids)>100 ; abort...')

  carlsbad_utils.Extract2XGMML_Targets(DBHOST,DBNAME,DBUSR,DBPW,fout,tids)
  fout.close()
