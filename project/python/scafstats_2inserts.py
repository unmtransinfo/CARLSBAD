#!/usr/bin/env python
#############################################################################
# scafstats2inserts.py
#
# This version for CARLSBAD database.
#
# input columns:
#   smiles scafid ncpds;ndups cidlist
#
#  Jeremy J Yang
#  28 Mar 2011
#############################################################################

import sys,os,re

PROG=os.path.basename(sys.argv[0])

if len(sys.argv)<4:
  print "syntax: %s <SCHEMA> <SCAFSTATS> <SCAFSQL> <S2CSQL>"%(PROG)
  sys.exit()

schema=sys.argv[1]
fin=file(sys.argv[2])
fout_scaf=file(sys.argv[3],"w")
fout_s2c=file(sys.argv[4],"w")

n_lines=0; n_inserts_scaf=0; n_inserts_s2c=0;
while True:
  line=fin.readline()
  if not line: break
  line=line.strip()
  if not line or line[0]=='#': continue
  n_lines+=1
  fields=re.split('\s',line)
  if len(fields)!=4:
    print >>sys.stderr, "Bad line: %s"%line
    continue

  smi=fields[0]
  smi=re.sub(r'\\',r"'||E'\\\\'||'",smi) #escape backslashes
  scafid=int(fields[1])
  fout_scaf.write("INSERT INTO %s.scaffold (id,smiles) VALUES (%d,gnova.cansmiles('%s'));\n"%(schema,scafid,smi))
  n_inserts_scaf+=1

  if not fields[3]:
    continue

  cids=re.split(',',fields[3])
  for cid in cids:
    if not cid: continue
    cid=int(cid)
    fout_s2c.write("INSERT INTO %s.scafid2cid (scaffold_id,compound_id) VALUES (%d,%d);\n"%(schema,scafid,cid))
    n_inserts_s2c+=1

fout_scaf.close()
fout_s2c.close()
print >>sys.stderr, "%s: input data lines: %d"%(PROG,n_lines)
print >>sys.stderr, "%s: scaffold inserts: %d"%(PROG,n_inserts_scaf)
print >>sys.stderr, "%s: s2c inserts: %d"%(PROG,n_inserts_s2c)
