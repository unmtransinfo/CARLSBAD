#!/usr/bin/env python
"""
	carlsbad_search_compound.py - carlsbad database search utility 


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
        --name=<NAMEQUERY>      ... compound name query
        --smiles=<SMILES>       ... compound structural query
        --substruct             ... compound query as substructure [default exact]
        --id=<ID>               ... compound ID query
        --idtype=<IDTYPE>       ... compound ID type query (???)
options:
        --v                     ... verbose
        --h                     ... this help
'''%{'PROG':PROG}
  sys.exit()

#############################################################################
def ErrorExit(msg):
  print >>sys.stderr,msg
  sys.exit(1)

#############################################################################
if __name__=='__main__':
  type=None; name_query=None; smi_query=None; id=None; idtype=None;
  substruct=False;
  verbose=0
  opts,pargs=getopt.getopt(sys.argv[1:],'',['out_xgmml=','name=','smiles=',
	'id=','idtype=','substruct','h=','help','v','vv'])
  if not opts: Help()
  for (opt,val) in opts:
    if opt=='--help': Help()
    elif opt=='--name': name_query=val
    elif opt=='--smiles': smi_query=val
    elif opt=='--id': id=val
    elif opt=='--idtype': idtype=val
    elif opt=='--substruct': substruct=True
    elif opt=='--v': verbose=1
    elif opt=='--vv': verbose=2
    else: Help('Illegal option: %s'%(opt))


  DBHOST,DBNAME,DBUSR,DBPW = 'carlsbad.health.unm.edu','cb3','dbc','chem!nfo'

  if not name_query and not smi_query and not id:
    Help('ERROR: no query.')

  if not idtype: idtype='any'

  sql,rows = carlsbad_utils.CompoundSearch(DBHOST,DBNAME,DBUSR,DBPW,
	smiq=smi_query,smisub=substruct,
	nameq=name_query,namesub=True,
	id=id,idtype=idtype
	)
  if verbose:
    print >>sys.stderr,sql

  for row in rows:
    print row
