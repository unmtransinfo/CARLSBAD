#!/usr/bin/env python3
#############################################################################
### networkx utilities 
### 
### Jeremy Yang
### 14 Feb 2018
#############################################################################
import sys,os,re,getopt,types,time
import networkx as nx

PROG=os.path.basename(sys.argv[0])

#############################################################################
def SIF2Graph(fin,verbose=0):
  G = nx.Graph()
  while True:
    line = fin.readline()
    if not line: break
    fields = line.split()
    if len(fields)!=3:
      print('ERROR: bad SIF line n_fields!=3: "%s"'%line,file=sys.stderr) 
      continue
    nodeA,edge,nodeB = fields
    if not nodeA in G: G.add_node(nodeA)
    if not nodeB in G: G.add_node(nodeB)
    if not G[nodeA].has_key(nodeB): G.add_edge(nodeA,nodeB)
  return G

#############################################################################
def Graph2SIF(G,fout,verbose=0):
  nodes = set([])
  n_edge=0;
  for e in G.edges_iter():
    n_edge+=1
    nA, nB = e
    fout.write('%s\tpp\t%s\n'%(nA,nB))
    nodes.add(nA)
    nodes.add(nB)
  if verbose:
    print("nodes written: %d"%len(nodes),file=sys.stderr)
    print("edges written: %d"%n_edge,file=sys.stderr)

#############################################################################
def Counts(G):
  return G.number_of_nodes(),G.number_of_edges()

#############################################################################
def GraphSummary(G,verbose=0):
  txt=nx.info(G)
  n_nodes,n_edges = networkx_utils.Counts(G)
  txt+='nodes:     %9d\n'%n_nodes
  txt+='edges:     %9d\n'%n_edges
  return txt

#############################################################################
def Properties(G,verbose=0):
  txt=''
  pathlengths=[]
  for v in G.nodes():
    spl=nx.single_source_shortest_path_length(G,v)
    #print('%s %s' % (v,spl),file=sys.stderr)
    for p in spl.values():
      pathlengths.append(p)

  txt+= "average shortest path length %s\n" % (sum(pathlengths)/len(pathlengths))

  # histogram of path lengths
  dist={}
  for p in pathlengths:
    if p in dist:
      dist[p]+=1
    else:
      dist[p]=1

  txt+= "length #paths"
  verts=dist.keys()
  for d in sorted(verts):
    txt+= '%s %d\n' % (d,dist[d])

  txt+= 'radius: %d\n' % nx.radius(G)
  txt+= 'diameter: %d\n' % nx.diameter(G)
  #txt+= 'eccentricity: %s\n' % nx.eccentricity(G)
  txt+= 'center: %s\n' % nx.center(G)
  #txt+= 'periphery: %s\n' % nx.periphery(G)
  txt+= 'density: %s\n' % nx.density(G)
  return txt

#############################################################################
### colors:
###     'b' blue
###     'g' green
###     'r' red
###     'c' cyan
###     'm' magenta
###     'y' yellow
###     'k' black
###     'w' white
### 
### shapes:
###     'o' circle
###     's' square
###     'h' hexagon
###     '^' triangle up
###     'v' triangle down
###     '<' triangle left
###     '>' triangle right
###     'd' diamond
###     'p' pentagon
###     '8' ?
#############################################################################
def DrawNetwork(G,title,verbose=0):
  try:
    import matplotlib.pyplot as pyplot
  except ImportError:
    ErrorExit("Matplotlib needed for drawing. Skipping")
  try:
    xys=nx.graphviz_layout(G)
  except:
    xys=nx.spring_layout(G,iterations=20)

  #tgts=[]; cpds=[]; scfs=[]; mcss=[];
  #for n in G:
  #  if   IsTarget(n):
  #    tgts.append(n)
  #  elif IsScaffold(n):
  #    scfs.append(n)
  #  elif IsCompound(n):
  #    cpds.append(n)
  #  elif IsMCES(n):
  #    mcss.append(n)
  #nodesize_scf=[];
  #
  #for n in scfs:
  #  nodesize_scf.append(20*math.sqrt(G.node[n]['n_cpd']))

  nx.draw(G, pos=xys, nodecolor='r', edge_color='b')

  #nx.draw_networkx_nodes(G,nodelist=tgts,pos=xys,node_size=300,node_color='r',node_shape='o',alpha=0.4)
  #nx.draw_networkx_nodes(G,nodelist=cpds,pos=xys,node_size=30,node_color='k',node_shape='s',alpha=0.4)
  #nx.draw_networkx_nodes(G,nodelist=scfs,pos=xys,node_size=nodesize_scf,node_color='b',node_shape='h',alpha=0.4)
  #nx.draw_networkx_nodes(G,nodelist=mcss,pos=xys,node_size=30,node_color='m',node_shape='v',alpha=0.4)
  #nx.draw_networkx_edges(G,pos=xys,alpha=0.4,node_size=0,width=1,edge_color='c')

  pyplot.title(title,{'fontsize':14,'color':'k'})
  pyplot.axis('off')
  pyplot.show()

#############################################################################
def Help(msg=''):
  if msg: print(msg,file=sys.stderr)
  print('''\
%(PROG)s
required:
	--i IFILE ............... input SIF
required (one of):
	--counts
	--summary
	--info .................. networkx info() function
	--properties ............ network properties (can be slow)
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
  draw=False;
  ifile='';
  ofile='';
  verbose=0;

  opts,pargs=getopt.getopt(sys.argv[1:],'',['i=','o=',
    'counts','info','summary','properties',
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
    elif opt=='--draw': draw=True
    elif opt=='--v': verbose=1
    elif opt=='--vv': verbose=2
    else: Help('Illegal option: %s'%(opt))

  if not ifile:
    ErrorExit('--i infile required.')

  fin=open(ifile)
  if not fin:
    ErrorExit('Could not open input file: %s'%ifile)
  print('infile: "%s"'%os.path.basename(ifile),file=sys.stderr)

  if ofile:
    if ofile=='-':
      fout=sys.stdout
    else:
      fout=open(ofile,'w+')
      if not fout:
        ErrorExit('Could not open output file: %s'%ofile)


  t0=time.time()

  G = SIF2Graph(fin,verbose)

  if counts:
    n_nodes,n_edges = networkx_utils.Counts(G)
    print('nodes:     %9d'%n_nodes,file=sys.stderr)
    print('edges:     %9d'%n_edges,file=sys.stderr)

  elif summary:
    print(GraphSummary(G,verbose),file=sys.stderr)

  elif info:
    print(nx.info(G),file=sys.stderr)

  elif properties:
    print(Properties(G,verbose),file=sys.stderr)

  else:
    ErrorExit('No action specified.')

  if ofile:
    Graph2SIF(G,fout,verbose)
    fout.close()

  if draw:
    n_nodes,n_edges = networkx_utils.Counts(G)
    if n_nodes>10000:
      ErrorExit("n_nodes>10000.  No -draw.")
    DrawNetwork(G,os.path.basename(ifile),verbose)

  if verbose:
    print(('elapsed time: %s'%(time.strftime('%Hh:%Mm:%Ss',time.gmtime(time.time()-t0)))),file=sys.stderr)

