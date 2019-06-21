#!/usr/bin/env python3
#############################################################################
### networkx utilities designed for CARLSBAD bioactivity networks.
### 
### Jeremy Yang
### 25 Sep 2013
#############################################################################
import sys,os,re,getopt,types,time,math
import networkx as nx

import networkx_utils

PROG=os.path.basename(sys.argv[0])

#############################################################################
def Reduce2Scaffolds(G,verbose=0):
  for scaf in G.nodes_iter():
    if not IsScaffold(scaf): continue
    n_cpd_this=0;
    n_tgt_this=0;
    tgts_this = set([])
    for cpd in nx.all_neighbors(G,scaf):
      if not IsCompound(cpd): continue
      n_cpd_this+=1
      for tgt in nx.all_neighbors(G,cpd):
        if not IsTarget(tgt): continue
        if not G[scaf].has_key(tgt):
          tgts_this.add(tgt)
          n_tgt_this+=1
    for tgt in tgts_this:
      G.add_edge(scaf,tgt)

    G.node[scaf]['n_cpd']=n_cpd_this
    G.node[scaf]['n_tgt']=n_tgt_this

  cpds=[]; mcess=[];
  for n in G.nodes_iter():
    if IsCompound(n): cpds.append(n)
    elif IsMCES(n): mcess.append(n)
  for n in cpds+mcess:
    G.remove_node(n)

#############################################################################
def IsCompound(node): return (type(node) is types.StringType and re.match(r'C',node))
def IsTarget(node): return (type(node) is types.StringType and re.match(r'T',node))
def IsScaffold(node): return (type(node) is types.StringType and re.match(r'S',node))
def IsMCES(node): return (type(node) is types.StringType and re.match(r'M',node))

#############################################################################
def CarlsbadGraphSummary(G,verbose=0):
  txt=''
  n_nodes,n_edges = networkx_utils.Counts(G)
  txt+='nodes:     %9d\n'%n_nodes
  txt+='edges:     %9d\n'%n_edges

  n_cpd=0; n_tgt=0; n_scf=0; n_mcs=0;
  for node in G.nodes():
    if IsCompound(node): n_cpd+=1
    elif IsTarget(node): n_tgt+=1
    elif IsScaffold(node): n_scf+=1
    elif IsMCES(node): n_mcs+=1

  n_ct=0; n_mc=0; n_sc=0; n_st=0;
  for edge in G.edges():
    nodeA,nodeB = edge
    if IsCompound(nodeA)   and IsTarget(nodeB): n_ct+=1
    elif IsTarget(nodeA)   and IsCompound(nodeB): n_ct+=1
    elif IsMCES(nodeA)     and IsCompound(nodeB): n_mc+=1
    elif IsCompound(nodeA) and IsMCES(nodeB): n_mc+=1
    elif IsScaffold(nodeA) and IsCompound(nodeB): n_sc+=1
    elif IsCompound(nodeA) and IsScaffold(nodeB): n_sc+=1
    elif IsScaffold(nodeA) and IsTarget(nodeB): n_st+=1
    elif IsTarget(nodeA)   and IsScaffold(nodeB): n_st+=1

  txt+='nodes (cpd):     %9d\n'%n_cpd
  txt+='nodes (tgt):     %9d\n'%n_tgt
  txt+='nodes (scf):     %9d\n'%n_scf
  txt+='nodes (mcs):     %9d\n'%n_mcs
  txt+='nodes (total):   %9d\n'%(n_cpd+n_tgt+n_scf+n_mcs)
  txt+='edges (cpd-tgt): %9d\n'%n_ct
  txt+='edges (mcs-cpd): %9d\n'%n_mc
  txt+='edges (scf-cpd): %9d\n'%n_sc
  txt+='edges (scf-tgt): %9d\n'%n_st
  txt+='edges (total):   %9d\n'%(n_ct+n_mc+n_sc+n_st)
  return txt

#############################################################################
### colors:
###	'b' blue
###	'g' green
###	'r' red
###	'c' cyan
###	'm' magenta
###	'y' yellow
###	'k' black
###	'w' white
### 
### shapes:
###	'o' circle
###	's' square
###	'h' hexagon
###	'^' triangle up
###	'v' triangle down
###	'<' triangle left
###	'>' triangle right
###	'd' diamond
###	'p' pentagon
###	'8' ?
#############################################################################
def DrawCarlsbadNetwork(G,title,verbose=0):
  try:
    import matplotlib.pyplot as pyplot
  except ImportError:
    ErrorExit("Matplotlib needed for drawing. Skipping")
  try:
    xys=nx.graphviz_layout(G)
  except:
    xys=nx.spring_layout(G,iterations=20)

  tgts=[]; cpds=[]; scfs=[]; mcss=[];
  for n in G:
    if   IsTarget(n):
      tgts.append(n)
    elif IsScaffold(n):
      scfs.append(n)
    elif IsCompound(n):
      cpds.append(n)
    elif IsMCES(n):
      mcss.append(n)

  nodesize_scf=[];
  for n in scfs:
    nodesize_scf.append(20*math.sqrt(G.node[n]['n_cpd']))

  #nx.draw(G, pos=xys, nodecolor='r', edge_color='b')
  nx.draw_networkx_nodes(G,nodelist=tgts,pos=xys,node_size=300,node_color='r',node_shape='o',alpha=0.4)
  nx.draw_networkx_nodes(G,nodelist=cpds,pos=xys,node_size=30,node_color='k',node_shape='s',alpha=0.4)
  nx.draw_networkx_nodes(G,nodelist=scfs,pos=xys,node_size=nodesize_scf,node_color='b',node_shape='h',alpha=0.4)
  nx.draw_networkx_nodes(G,nodelist=mcss,pos=xys,node_size=30,node_color='m',node_shape='v',alpha=0.4)
  nx.draw_networkx_edges(G,pos=xys,alpha=0.4,node_size=0,width=1,edge_color='c')

  pyplot.title(title,{'fontsize':14,'color':'k'})
  pyplot.axis('off')
  pyplot.show()

#############################################################################
def Help(msg=''):
  if msg: print(msg, file=sys.stderr)
  print('''\
%(PROG)s
required:
        --i IFILE ............... input SIF
required (one of):
        --counts
        --summary
        --info .................. networkx info() function
        --properties ............ network properties (can be slow)
        --reduce2scafs .......... generate reduced graph: targets & scafs
options:
        --o OFILE ............... output SIF
        --draw .................. display via matplotlib.pyplot
        --v ..................... verbose
        --h ..................... this help
'''%{'PROG':PROG}, file=sys.stderr)
  sys.exit()

#############################################################################
def ErrorExit(msg):
  print(msg, file=sys.stderr)
  sys.exit(1)

#############################################################################
if __name__=='__main__':

  counts=False; summary=False;  properties=False; 
  info=False;
  reduce2scafs=False;
  draw=False;
  ifile='';
  ofile='';
  verbose=0;

  opts,pargs=getopt.getopt(sys.argv[1:],'',['i=','o=',
    'counts','info','summary','properties','reduce2scafs',
    'draw','h=','help','v','vv'])
  if not opts: Help()
  for (opt,val) in opts:
    if opt=='--help': Help()
    elif opt=='--i': ifile=val
    elif opt=='--o': ofile=val
    elif opt=='--counts': counts=True
    elif opt=='--summary': summary=True
    elif opt=='--info': info=True
    elif opt=='--properties': properties=True
    elif opt=='--reduce2scafs': reduce2scafs=True
    elif opt=='--draw': draw=True
    elif opt=='--v': verbose=1
    elif opt=='--vv': verbose=2
    else: Help('Illegal option: %s'%(opt))

  if not ifile:
    ErrorExit('--i infile required.')

  fin=open(ifile)
  if not fin:
    ErrorExit('Could not open input file: %s'%ifile)
  print('infile: "%s"'%os.path.basename(ifile), file=sys.stderr)

  if ofile:
    if ofile=='-':
      fout=sys.stdout
    else:
      fout=open(ofile,'w+')
      if not fout:
        ErrorExit('Could not open output file: %s'%ofile)

  t0=time.time()

  G = networkx_utils.SIF2Graph(fin,verbose)

  if counts:
    n_nodes,n_edges = networkx_utils.Counts(G)
    print('nodes:     %9d'%n_nodes, file=sys.stderr)
    print('edges:     %9d'%n_edges, file=sys.stderr)

  elif summary:
    print(CarlsbadGraphSummary(G,verbose), file=sys.stderr)

  elif info:
    print(nx.info(G), file=sys.stderr)

  elif properties:
    print(networkx_utils.Properties(G,verbose), file=sys.stderr)

  elif reduce2scafs:
    print(Summary(G,verbose), file=sys.stderr)
    Reduce2Scaffolds(G,verbose)
    print(Summary(G,verbose), file=sys.stderr)

  else:
    ErrorExit('No action specified.')

  if ofile:
    networkx_utils.Graph2SIF(G,fout,verbose)
    fout.close()

  if draw:
    n_nodes,n_edges = networkx_utils.Counts(G)
    if n_nodes>10000:
      ErrorExit("n_nodes>10000.  No -draw.")

    DrawCarlsbadNetwork(G,os.path.basename(ifile),verbose)

  if verbose:
    print(('elapsed time: %s'%(time.strftime('%Hh:%Mm:%Ss',time.gmtime(time.time()-t0)))), file=sys.stderr)
