#! /usr/bin/env python
#############################################################################
### cytoscapeview.cgi - Via CytoscapeWeb plugin, display network from
### input file (XGMML).
### 
### See http://cytoscapeweb.cytoscape.org/
### API ref: http://cytoscapeweb.cytoscape.org/tutorial
### 
###  layouts: ForceDirected, Circle, Radial, Tree, Preset, CompoundSpringEmbedder
### 
### to do:
###   [x] interactive label options (PubChem vs. ChEMBL?)
###   [ ] If no file, allow upload.
###   [ ] Checkboxes for show-compounds, -scaffolds, -mceses.
### 
### species:
###   rat:    804
###   mouse:  547
###   human: 2388
### 
### ttype: 
###   enzyme:                 1884
###   receptor:                888
###   protein:                 436
###   ion channel:             274
###   membrane receptor:       103
###   cytosolic other:          44
###   transporter:              37
###   transcription factor:     24
###   secreted:                 16
###   structural:               10
###   adhesion:                  8
###   membrane other:            7
###   surface antigen:           4
###   nuclear other:             4
#############################################################################
###  Note: the mod_* JS functions are for modification of style after
###  Visualization initialization.  In contrast, NodeSize() returns an
###  JS object which can be used during initialization.
#############################################################################
### PROBLEM: Apache may not allow access to /tmp since it is not under
### DOCUMENT_ROOT.  Probably should use a scratch subdir.
### 
#############################################################################
### Jeremy J Yang
### 16 Jun 2014
#############################################################################
import os,sys,cgi,re,time

#############################################################################
def JavaScript(xgmml):
  js='''\
var vis; //global
var tgt_label_type=''; //global
var cpd_label_type=''; //global
function DEBUG2Log()
{
  var visual_style = vis.visualStyle();
  //console.log('DEBUG: visual_style.nodes.size="'+JSON.stringify(visual_style.nodes.size)+'"');

  var layout_options = vis.layout().options;
  console.log('DEBUG: layout_options="'+JSON.stringify(layout_options)+'"');
}
function mod_EdgeMerge(form)
{
  vis.edgesMerged(form.edgesmerge.checked);
  return;
}
function mod_NodeSize(form)
{
  visual_style = vis.visualStyle();
  visual_style.nodes.size = NodeSize(form.mode.value,form.nodescale.checked);
  vis.visualStyle(visual_style);
}
function NodeSize(mode,nodescale)
{
  if ((mode=='rgt'||mode=='rgtp') && nodescale)
  {
    size = {
          defaultValue: 100,

////////////////////////////////////////////
//CANNOT HAVE BOTH discreteMapper and continuousMapper...
//          discreteMapper: {
//            attrName: "class",
//            entries: [
//              { attrValue: "compound", value: 50 }
//            ]
//          },
////////////////////////////////////////////

          continuousMapper: { attrName: 'deg_cpd', minValue: 20, maxValue: 160 }

//	customMapper: { functionName: "customNodeSizeRGT" } //HOW?

        };
  }
  else
  {
    size = {
        defaultValue: 25,
        discreteMapper: {
          attrName: "class",
          entries: [
            { attrValue: "target", value: 60 },
            { attrValue: "compound", value: 25 },
            { attrValue: "scaffold", value: 35 },
            { attrValue: "mces", value: 35 }
          ]
        }
	};
  }
  return size;
}
function mod_NodeColor(form)
{
  visual_style = vis.visualStyle();
  visual_style.nodes.color = NodeColor(form.mode.value,form.nodecolor.checked);
  vis.visualStyle(visual_style);
}
function NodeColor(mode,nodecolor)
{
  if ((mode=='rgt'||mode=='rgtp') && nodecolor)
  {
    //console.log('DEBUG: mode="'+mode+'"; nodecolor="'+nodecolor+'"');
    color = {
          defaultValue: "#FFFFFF",
          discreteMapper: {
		attrName: 'type',
		entries: [
			{ attrValue: "enzyme", value:  "#0B94B1" },
			{ attrValue: "receptor", value: "#FF0000" },
			{ attrValue: "protein", value: "#EE00EE" },
			{ attrValue: "ion channel", value: "#4444FF" },
			{ attrValue: "membrane receptor", value: "#DDDD00" }
		]
	}
        };
  }
  else
  {
    color = {
        defaultValue: "#0B94B1",
        discreteMapper: {
        attrName: "class",
        entries: [
            { attrValue: "target", value: "#0B94B1" },
            { attrValue: "compound", value: "#FFFFFF" },
            { attrValue: "scaffold", value: "#FFAF00" },
            { attrValue: "mces", value: "#FFDD00" }
        ]
      }
	};
  }
  return color;
}
function mod_NodeShape(form)
{
  visual_style = vis.visualStyle();
  visual_style.nodes.shape = NodeShape(form.mode.value,form.nodeshape.checked);
  vis.visualStyle(visual_style);
}
function NodeShape(mode,nodeshape)
{
  if ((mode=='rgt'||mode=='rgtp') && nodeshape)
  {
//    shape = {
//          defaultValue: "OCTAGON",
//          discreteMapper: {
//		attrName: 'species',
//		entries: [
//			{ attrValue: "human", value:  "CIRCLE" },
//			{ attrValue: "mouse", value: "TRIANGLE" },
//			{ attrValue: "rat", value: "VEE" }
//		]
//	  }
//        };

    shape = {
          defaultValue: "CIRCLE",
          discreteMapper: {
          attrName: 'class',
          entries: [
            { attrValue: "target", value: "CIRCLE" },
            { attrValue: "disease", value: "RECTANGLE" },
            { attrValue: "compound", value: "RECTANGLE" }
          ]
        }
      };
  }
  else
  {
    shape = {
          defaultValue: "CIRCLE",
          discreteMapper: {
          attrName: 'class',
          entries: [
            { attrValue: "target", value: "OCTAGON" },
            { attrValue: "compound", value: "RECTANGLE" },
            { attrValue: "scaffold", value: "HEXAGON" },
            { attrValue: "mces", value: "ELLIPSE" }
          ]
        }
      };
  }
  return shape;
}
function mod_EdgeWidth(form)
{
  visual_style = vis.visualStyle();
  visual_style.edges.width = EdgeWidth(form.mode.value,form.edgescale.checked);
  vis.visualStyle(visual_style);
}
function EdgeWidth(mode,edgescale)
{
  if ((mode=='rgt'||mode=='rgtp') && edgescale)
  {
    attr='shared_cpd';
    width = {
          defaultValue: 2, 
          continuousMapper: { attrName: attr, minValue: 1, maxValue: 10 }
        };
  }
  else
  {
    attr='shared_cpd';
    width = {
        defaultValue: 2, 
        continuousMapper: { attrName: attr, minValue: 1, maxValue: 3 }
        };
  }
  return width;
}

function ReLayout()
{
  vis.layout(vis.layout());
}
function LayoutOptions(mode,method)
{
  var layout_options = { name: method };

  if (mode=='rgt'||mode=='rgtp')
  {
    //console.log('DEBUG: mode="'+mode+'"; method="'+method+'"');
    //layout_options.weightAttr = null;
    layout_options.weightAttr = "shared_cpd";
    layout_options.gravitation = -400;
    layout_options.minDistance = 1;
    layout_options.maxDistance = 500;
    //layout_options.restLength =    "auto",
    layout_options.restLength = 30;
    layout_options.tension = 0.1;
    layout_options.weightNorm = "linear";
    layout_options.mass =          3,
    layout_options.iterations =    400,
    layout_options.autoStabilize = false;
  }
  else
  {
    layout_options.weightAttr = "val_std";

    //layout_options.weightAttr =    null,
    //layout_options.drag =          0.2,
    //layout_options.gravitation =   -200,
    //layout_options.minDistance =   1,
    //layout_options.maxDistance =   400,
    //layout_options.mass =          3,
    //layout_options.tension =       0.2,
    //layout_options.weightNorm =    "linear",
    //layout_options.restLength =    "auto",
    //layout_options.iterations =    400,
    //layout_options.maxTime =       30000,
    //layout_options.autoStabilize = false
  }
  //console.log('DEBUG: layout_options="'+JSON.stringify(layout_options)+'"');
  return layout_options;
}
function mod_Layout(form)
{
  vis.layout({ name: form.layout.value, options: LayoutOptions(form.mode.value,form.layout.value) });
  return;
}
function mod_TargetLabel(form)
{
  var i;
  for (i=0;i<form.tgt_label_type.length;++i)
    if (form.tgt_label_type.options[i].selected)
      tgt_label_type=form.tgt_label_type.options[i].value;
  vis.visualStyle(vis.visualStyle());
  return;
}
function mod_CompoundLabel(form)
{
  var i;
  for (i=0;i<form.cpd_label_type.length;++i)
    if (form.cpd_label_type.options[i].selected)
      cpd_label_type=form.cpd_label_type.options[i].value;
  vis.visualStyle(vis.visualStyle());
  return;
}
function netSummary()
{
  var net = vis.networkModel(); //plugin network object
  var htm='<B>network summary:</B><P>';
  if (net.data!=null) {
    if (net.data.nodes!=null) {
      htm+='nodes: '+net.data.nodes.length;
    }
    if (net.data.edges!=null) {
      htm+='<br />edges: '+net.data.edges.length;
    }
    var n_tar=0;
    var n_cpd=0;
    var n_scaf=0;
    var n_mces=0;
    for (var i=0;i<net.data.nodes.length;++i)
    {
      var node=net.data.nodes[i];
      if (node.class!=null)
      {
        if (node.class == 'target') ++n_tar;
        else if (node.class == 'compound') ++n_cpd;
        else if (node.class == 'scaffold') ++n_scaf;
        else if (node.class == 'mces') ++n_mces;
      }
    }
    htm+=('<br />targets: '+n_tar);
    htm+=('<br />compounds: '+n_cpd);
    htm+=('<br />scaffolds: '+n_scaf);
    htm+=('<br />mces: '+n_mces);
  }
  htm+='</p>';
  return htm;
}
function clear_div(divname)
{
  document.getElementById(divname).innerHTML="";
}
function print2div(divname,msg)
{
  msg=msg.replace(/,/g,', ');
  msg=msg.replace(/;/g,'; ');
  document.getElementById(divname).innerHTML+=msg;
}
function preferred_synonym(synonyms) // Find "name-like"
{
  if (synonyms.length==0) return '';
  var i_best=0;
  for (i_best=synonyms.length-1;i_best>=0;--i_best)
    if (synonyms[i_best].match(/^[A-Z][a-z]+$/)) return synonyms[i_best];
  for (i_best=synonyms.length-1;i_best>=0;--i_best)
    if (synonyms[i_best].match(/^[a-zA-Z ]+$/)) return synonyms[i_best];
  return synonyms[i_best];
}
window.onload=function() {
  var div_id="cytoscapeweb"; // id of Cytoscape Web container div
  var init_options = {
    swfPath:"/cytoscapeweb/swf/CytoscapeWeb",
    flashInstallerPath:"/cytoscapeweb/swf/playerProductInstall",
    flashAlternateContent: "El Flash Player es necesario."
  };
  vis = new org.cytoscapeweb.Visualization(div_id,init_options);
  var visual_style = { global:{} };
  visual_style.global.backgroundColor = "%(CYWEB_BGCOLOR)s";
  visual_style.nodes = 
    {
      borderWidth: {
        discreteMapper: {
          attrName: "is_drug",
          entries: [
            { attrValue: true, value: 3 }
          ]
        },
        defaultValue: 1
      },
      borderColor: {
        discreteMapper: {
          attrName: "is_drug",
          entries: [
            { attrValue: true, value: "#FF0000" }
          ]
        },
        defaultValue: "#FFFFFF"
      },
      shape: NodeShape('%(MODE)s','%(NODESHAPE)s'),
      size: NodeSize('%(MODE)s','%(NODESCALE)s'),
      color: NodeColor('%(MODE)s','%(NODECOLOR)s'),
      tooltipText: {
        discreteMapper: {
          attrName: "class",
          entries: [
            { attrValue: "disease",
		value: "<b>${name}<br />${id}<br />" },
            { attrValue: "target",
		value: "<b>${id}<br />${name}<br />${species}, ${type}<br />deg_cpd: ${deg_cpd}</b>" },
            { attrValue: "compound",
		value: "<b>${id}<br />deg_tgt: ${deg_tgt}</b>" },
		//value: "<b>${id}<br /><img src='/tomcat/biocomp/mol2img?h=60&w=60&smiles=${smiles}'><br />deg_tgt: ${deg_tgt}</b>" },
            { attrValue: "scaffold",
		value: "<b>${id}<br />deg_tgt: ${deg_tgt}<br />deg_cpd: ${deg_cpd}</b>" },
            { attrValue: "mces", value:
		"<b>${id}<br />deg_tgt: ${deg_tgt}<br />deg_cpd: ${deg_cpd}</b>" },
          ]
        }
      },
      labelFontSize: 10,
      labelFontWeight: "bold"
    };
  visual_style.edges = 
    {
      width: EdgeWidth('%(MODE)s','%(EDGESCALE)s'),
      style: {
        discreteMapper: {
          attrName: "class",
          entries: [
            { attrValue: "activity", value:  "SOLID" },
            { attrValue: "tt", value:  "EQUAL_DASH" },
            { attrValue: "cpd2scaf", value: "DOT" },
            { attrValue: "cpd2mces", value: "DOT" }
          ]
        },
        defaultValue: "SOLID"
      },
      tooltipText: {
        discreteMapper: {
          attrName: "class",
          entries: [
            { attrValue: "activity", value: "<b>${class}: ${id}<br />val_std: ${val_std}</b>" },
            { attrValue: "tt", value: "<b>${class}: ${id}<br />shared_cpd: ${shared_cpd}</b>" }
          ]
        },
        defaultValue: "<b>${class}: ${id}</b>"
      },
      targetArrowShape: "NONE",
      color: "#0B94B1"
    };
  visual_style.edges.mergeWidth = EdgeWidth('%(MODE)s','%(EDGESCALE)s');
  visual_style.edges.mergeStyle =
      {
        discreteMapper: {
          attrName: "class",
          entries: [
            { attrValue: "activity", value:  "SOLID" },
            { attrValue: "cpd2scaf", value: "DOT" },
            { attrValue: "cpd2mces", value: "DOT" },
            { attrValue: "tt", value: "DOT" }
          ]
        },
        defaultValue: "EQUAL_DASH"
      };
  visual_style.edges.mergeColor = "#0B94B1";

  var xgmml='';
'''%{
	'MODE':MODE,
	'NODESCALE':NODESCALE,
	'NODECOLOR':NODECOLOR,
	'NODESHAPE':NODESHAPE,
	'EDGESMERGE':EDGESMERGE,
	'EDGESCALE':EDGESCALE,
	'CYWEB_BGCOLOR':CYWEB_BGCOLOR
	}
  ## now load the network via xgmml:
  for line in re.split(r'[\n\r]+',xgmml):
    line=line.replace('\'','\\\'')
    js+=('    xgmml+=\'%s\\n\';\n'%line)
  ## interaction functionality
  js+='''\
  var layout_options = LayoutOptions('%(MODE)s','%(LAYOUT)s');
  var draw_options = {
	network:xgmml,
        visualStyle: visual_style,
	layout: layout_options,
	panZoomControlVisible:true,
	edgesMerged:'%(EDGESMERGE)s'
	};
  // customMapper label function:
  vis["customLabel"] = function (data) {
    var value=data["id"];
    if (data["class"]=='target')
    {
      if (tgt_label_type && tgt_label_type!='default')
      {
        if (typeof data[tgt_label_type] != 'undefined' && data[tgt_label_type]!=null)
        {
          value=new String(data[tgt_label_type]);
          value=value.replace(/,/g,', ');
        }
      }
      else
      {
        value=data["name"]; //default
      }
    }
    else if (data["class"]=='compound')
    {
      if (cpd_label_type && cpd_label_type!='default')
      {
        if (typeof data[cpd_label_type] != 'undefined' && data[cpd_label_type]!=null)
        {
          value=new String(data[cpd_label_type]);
          value=value.replace(/,/g,', ');
        }
      }
      if (typeof data['synonym'] != 'undefined' && data['synonym']!=null)
      {
        var synonyms_str=new String(data['synonym']);
        var synonyms = synonyms_str.split(",");
        var synonym=preferred_synonym(synonyms);
        value+=(' ('+synonym+')');
      }
    }
    else if (data["class"]=='disease')
    {
      value=data["name"]+' '+data['id'];
    }
    else
      value=data["id"];
    value=value.replace(/\s/g,'\\n');
    return value;
  };

  // callback when Cytoscape Web has finished drawing
  vis.ready(
    function()
    {
      visual_style.nodes.label = { customMapper: { functionName: "customLabel" } };

      vis.visualStyle(visual_style);

      vis.nodeTooltipsEnabled(true);
      vis.edgeTooltipsEnabled(true);

      // add listeners  for when nodes and edges are clicked
      vis.addListener("click","nodes",function(event) { handle_click(event); })
      vis.addListener("click","edges",function(event) { handle_click(event); });
      function handle_click(event)
      {
        var target = event.target;
        var cls = target.data['class']; //target,compound,activity,scaffold,mces
        clear_div("note");
        clear_div("depict");
        //print2div("note","event.group = " + event.group);  //nodes, edges
        if (typeof cls != 'undefined')
          print2div("note","<B>"+cls+"</B>:<BR />" );
        var smiles='';
        for (var v in target.data)
        {
          if (target.data[v]==null) continue;
          else if (v=='smiles' || v=='smarts')
          {
            smiles=target.data[v];
          }
          else if (v=='class') continue;
          else if (v=='label') continue;
          else if (v=='ID') continue;
          else if (v.match(/^canonical/i)) continue;
          else if (v.match(/^_/)) continue;
          else if (cls=='target' && v=='descr') continue;
          else if (cls=='target' && v=='deg_tgt') continue;
          else if (cls=='compound' && v=='deg_cpd') continue;
          else if (cls!='compound' && v=='is_drug') continue;
          print2div("note","&nbsp;&nbsp;"+v+": "+target.data[v]+"<BR />" );
        }
        if (cls=='activity')          //find cpd node, smiles
        {
          var cid=target.data['source']; //e.g. "C261280"
          var net = vis.networkModel(); //plugin network object
          var nodes = net.data.nodes;
          var i;
          for (i=0;i<nodes.length;++i)
          {
            var node=nodes[i];
            if (typeof node != 'undefined' && typeof node['id'] != 'undefined' && node['id']==cid)
            {
              if (typeof node['smiles'] != 'undefined' && node['smiles'])
                smiles=node['smiles'];
            }
          }
        }
        if (smiles)
        {
          var imgurl='/tomcat/biocomp/mol2img';
          imgurl+='?h=300&w=400&smiles='+encodeURIComponent(smiles);
          var imghtm='<IMG HEIGHT="100%%" SRC="'+imgurl+'">';
          document.getElementById("depict").innerHTML=imghtm;
        }
      }
      //alert("DEBUG: "+netSummary(vis));
    }
  );

  vis.draw(draw_options);
};
'''%{
	'MODE':MODE,
	'LAYOUT':LAYOUT,
	'EDGESMERGE':EDGESMERGE
	}
  return js

#############################################################################
def CSS():
  return '''\
* {
  margin:0; padding:0; font-family:Helvetica, Arial, Verdana, sans-serif;
}
html,body {
  height:100%%; width:100%%; padding:0; margin:0;
}
#titlebar {
  width:98%%; height:3%%; background-color:%(CYWEB_BGCOLOR)s; margin-left:auto; margin-right:auto;
}
/* The Cytoscape Web container must have its dimensions set. */
#cytoscapeweb {
  width:98%%; height:77%%; margin-left:auto; margin-right:auto;
}
#info {
  width:98%%; height:20%%; background-color:#FFFFFF; margin-left:auto; margin-right:auto;
}
#control {
  float:left; width:30%%; height:100%%; background-color:#DDDDDD; border:solid overflow:auto; 
}
#note {
  float:left; width:43%%; background-color:#FEFEFE; border:solid overflow:auto; 
}
p {
  padding:0 0.5em; margin:0;
}
p:first-child {
  padding-top:0.5em;
}
#depict {
  float:left; width:20%%; background-color:#FFFFFF;
}
p {
  padding:0 0.5em; margin:0;
}
p:first-child {
  padding-top:0.5em;
}
'''%{ 'CYWEB_BGCOLOR':CYWEB_BGCOLOR }

#############################################################################
def PrintHeader(title='',js='',jsincludes=None,css=''):
  print 'Content-type: text/html\n\n<HTML>'
  print '<HEAD><TITLE>%s</TITLE>'%title
  print '<SCRIPT SRC="/js/Mol2Img.js"></SCRIPT>'
  if jsincludes:
    for jsinclude in jsincludes:
      print '<SCRIPT SRC="%s"></SCRIPT>'%jsinclude
  if js: print '<SCRIPT LANGUAGE="JavaScript">\n%s\n</SCRIPT>'%js
  print '<LINK REL="stylesheet" type="text/css" HREF="/css/biocomp.css" />'
  if css: print '<STYLE TYPE="text/css">%s</STYLE>'%css
  print '</HEAD><BODY BGCOLOR="#DDDDDD">'

#############################################################################
def PrintForm():
  layouts=['ForceDirected','Circle','Radial','Tree','CompoundSpringEmbedder']
  layout_menu='<SELECT NAME="layout" onChange="mod_Layout(this.form)" >'
  for layout in layouts:
    layout_menu+=('<OPTION VALUE="%s">%s'%(layout,layout))
  layout_menu+='</SELECT>'
  layout_menu=re.sub('"%s">'%LAYOUT,'"%s" SELECTED>'%LAYOUT,layout_menu,re.I)

  tgt_idtypes=["ChEMBL ID","ChEMBL Target","EC Number","Ensembl","Entrez Gene","Gene","GeneCards","HomoloGene",
	"NCBI gi","OMIM","PDB","PharmGKB Gene","Protein Ontology (PRO)","RefSeq Nucleotide","RefSeq Protein",
	"Swissprot","UniGene","UniGene Hs.","UniProt"]
  tgt_label_type_menu='<SELECT NAME="tgt_label_type" onChange="mod_TargetLabel(this.form)" >'
  for tgt_idtype in ['default']+tgt_idtypes:
    tgt_label_type_menu+=('<OPTION VALUE="%s">%s'%(tgt_idtype,tgt_idtype))
  tgt_label_type_menu+='</SELECT>'

  cpd_idtypes=["CAS Registry No.","ChEBI","ChEMBL ID","ChEMBL Ligand","DrugBank","iPHACE","IUPHAR Ligand ID",
	"NURSA Ligand","PDSP Record Number","PharmGKB Drug","PubChem CID","PubChem SID","RCSB PDB Ligand","SMDL ID"]
  cpd_label_type_menu='<SELECT NAME="cpd_label_type" onChange="mod_CompoundLabel(this.form)" >'
  for cpd_idtype in ['default']+cpd_idtypes:
    cpd_label_type_menu+=('<OPTION VALUE="%s">%s'%(cpd_idtype,cpd_idtype))
  cpd_label_type_menu+='</SELECT>'

  print '''\
<FORM NAME="mainform">
  <TABLE WIDTH="100%%" CELLSPACING=0 CELLPADDING=1>
  <INPUT TYPE="HIDDEN" NAME="mode" VALUE="%(MODE)s">
  <TR><TD ALIGN=RIGHT>
  <B>nodes:</B>
  </TD><TD>
  <INPUT TYPE=CHECKBOX NAME="nodescale" VALUE="CHECKED" onChange="mod_NodeSize(this.form)" %(NODESCALE)s>scale 
  <INPUT TYPE=CHECKBOX NAME="nodeshape" VALUE="CHECKED" onChange="mod_NodeShape(this.form)" %(NODESHAPE)s>shape 
  <INPUT TYPE=CHECKBOX NAME="nodecolor" VALUE="CHECKED" onChange="mod_NodeColor(this.form)" %(NODECOLOR)s>color 
  </TD></TR>
  <TR><TD ALIGN=RIGHT>
  <B>edges:</B>
  </TD><TD>
  <INPUT TYPE=CHECKBOX NAME="edgescale" VALUE="CHECKED" onChange="mod_EdgeWidth(this.form)" %(EDGESCALE)s>scale 
  &nbsp;
  <INPUT TYPE=CHECKBOX NAME="edgesmerge" VALUE="CHECKED" onChange="mod_EdgeMerge(this.form)" %(EDGESMERGE)s>merge 
  </TD></TR>
  <TR><TD ALIGN=RIGHT>
  <B>layout:</B>
  </TD><TD>
'''%{	'MODE':MODE,
	'NODESCALE':NODESCALE,
	'NODECOLOR':NODECOLOR,
	'NODESHAPE':NODESHAPE,
	'EDGESMERGE':EDGESMERGE,
	'EDGESCALE':EDGESCALE
	}
  print layout_menu+'<BUTTON TYPE=BUTTON onClick="ReLayout()">ReLayout</BUTTON>'
  print '</TD></TR><TR><TD ALIGN=RIGHT><B>tgt labels:</B><BR></TD><TD>'
  print tgt_label_type_menu
  print '</TD></TR><TR><TD ALIGN=RIGHT><B>cpd labels:</B></TD><TD>'
  print cpd_label_type_menu

  print '''\
  </TD></TR>
  <TR><TD ALIGN=RIGHT></TD><TD>
  <BUTTON TYPE=BUTTON onClick="clear_div('note'); print2div('note',netSummary())">network summary</BUTTON>
  <!-- BUTTON TYPE=BUTTON onClick="DEBUG2Log()">DEBUG</BUTTON -->
  </TD></TR>
  </TABLE>
</FORM>
'''

#############################################################################
def NetworkView(title,mode,xgmml):
  jsincludes=('/cytoscapeweb/js/min/json2.min.js',
	'/cytoscapeweb/js/min/AC_OETags.min.js',
	'/cytoscapeweb/js/min/cytoscapeweb.min.js')
  PrintHeader(('%s:%s'%(PROG,title)),JavaScript(xgmml),jsincludes,CSS())
  print '''\
    <DIV ID="titlebar"><H2 ALIGN=CENTER>%(TITLE)s [%(MODE)s]</H2></DIV>
    <DIV ID="cytoscapeweb">Cytoscape Web: %(TITLE)s</DIV>
    <DIV ID="info">
      <DIV ID="control">
'''%{
	'TITLE':title,
	'MODE':mode
	}
  PrintForm()
  print '''\
      </DIV>
      <DIV ID="note"><P>Click (or hover-over) nodes or edges for more info...</P>
      </DIV>
      <DIV ID="depict">
      </DIV>
    </DIV>
    <DIV style="clear: both"></DIV>
'''

#############################################################################
def Initialize():
  global FORM,VERBOSE,LOG,URL,PROG,DATE
  FORM=cgi.FieldStorage(keep_blank_values=1)
  URL=os.environ['SCRIPT_NAME']
  PROG=os.path.basename(sys.argv[0])
  DATE=time.strftime('%Y%m%d%H%M',time.localtime())

  global INFILE,TITLE
  INFILE=FORM.getvalue('infile')
  TITLE=FORM.getvalue('title',INFILE)

  global MODE,LAYOUT,NODESCALE,NODECOLOR,NODESHAPE,EDGESMERGE,EDGESCALE
  MODE=FORM.getvalue('mode','full')	#rgt = reduced-graph-tgts
  LAYOUT=FORM.getvalue('layout','ForceDirected')
  NODESCALE=FORM.getvalue('nodescale','CHECKED')
  NODECOLOR=FORM.getvalue('nodecolor','CHECKED')
  NODESHAPE=FORM.getvalue('nodeshape','CHECKED')
  EDGESMERGE = 'CHECKED' if FORM.getvalue('edgesmerge','') else ''
  EDGESCALE=FORM.getvalue('edgescale','CHECKED')

  global MODES
  MODES = {
	'rgt': 'reduced graph tgts-only',
	'rgtp': 'reduced graph tgts+CCPs',
	'full': 'full graph'
	}

  global CYWEB_BGCOLOR
  CYWEB_BGCOLOR="#CCDDFF"

  logfile='./logs/'+PROG+'.log'
  logfields=['ip']
  if os.access(logfile,os.W_OK):
    LOG=open(logfile,'a')
    if os.path.getsize(logfile)==0:
      LOG.write('date\t' + '\t'.join(logfields) + '\n')
  else:
    LOG=open(logfile,'w+')
    LOG.write('date\t' + '\t'.join(logfields) + '\n')

  return True

#############################################################################
if __name__=='__main__':
  ok=Initialize()

  if not INFILE:
    PrintHeader('%s:%s'%(PROG,TITLE))
    print 'ERROR: Input file not specified.'
    print '</BODY></HTML>'
    sys.exit()
  if not os.access(INFILE,os.F_OK):
    PrintHeader('%s:%s'%(PROG,TITLE))
    print 'ERROR: Input file non-existent: "%s"</br>'%(INFILE)
    #import platform
    #print 'DEBUG: python_version: %s</br>'%(platform.python_version())
    print '</BODY></HTML>'
    sys.exit()
  if not os.access(INFILE,os.R_OK):
    PrintHeader('%s:%s'%(PROG,TITLE))
    print 'ERROR: Input file unreadable: "%s"'%(INFILE)
    print '</BODY></HTML>'
    sys.exit()

  try:
    f=open(INFILE)
    xgmml=f.read()
    f.close()
    NetworkView(TITLE,MODES[MODE],xgmml)
    print '</BODY></HTML>'
    LOG.write('%s\t%s\n'%(DATE,os.environ['REMOTE_ADDR']))
  except IOError,e:
    PrintHeader('%s:%s'%(PROG,TITLE))
    print 'ERROR: Cannot open input file: "%s".  Session may be expired.'%(INFILE)
    print '<PRE>%s</PRE>'%(str(e))
    print '</BODY></HTML>'
    sys.exit()

