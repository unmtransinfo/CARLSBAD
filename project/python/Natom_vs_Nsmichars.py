#!/usr/bin/env python
#############################################################################
###  Natom_vs_Nsmichars.py
###  
###  Counts alphabet chars in SMILES for comparison with the atom count.
###  Using Prestwick (from PubChem search, 1271 SMILES), the Spearman
###  correlation coefficient is .997.  This is useful since the char count
###  can be performed very fast and without a chemical toolkit, and can
###  be used for example to measure the fraction of a molecule contained
###  by a scaffold.
###
###  Jeremy Yang
###  18 Apr 2012
#############################################################################
import sys,os,re
import numpy

import openeye.oechem as oechem

if len(sys.argv)<2:
    oechem.OEThrow.Usage("%s <infile> [<outfile>]"%sys.argv[0])

ims=oechem.oemolistream()
ims.open(sys.argv[1])

if len(sys.argv)>2:
  oms=oechem.oemolostream()
  oms.open(sys.argv[2])
else:
  oms=None

natoms = []
nchars = []
for mol in ims.GetOEGraphMols():
  na = mol.NumAtoms()
  smi = oechem.OECreateSmiString(mol)
  nc = len(re.sub(r'[^a-zA-Z]','',smi))
  natoms.append(na)
  nchars.append(nc)
  if oms:
    mol.SetTitle("%d\t%d"%(na,nc))
    oechem.OEWriteMolecule(oms, mol)

ims.close()
if oms:
  oms.close()

A=numpy.array(natoms)
C=numpy.array(nchars)

print "%s: N: %d"%(sys.argv[0],len(natoms))
print "%s: correlation coefficient: %.5f"%(sys.argv[0],numpy.corrcoef([A,C])[0][1])
