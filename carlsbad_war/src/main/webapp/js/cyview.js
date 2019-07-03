function go_init(form) {
  form.nodescale.checked=true;
  form.nodeshape.checked=true;
  form.nodecolor.checked=true;
  form.edgescale.checked=true;
  form.edgemerge.checked=true;
  for (i=0;i<form.layout.length;++i)
    if (form.layout.options[i].value=='cose')
      form.layout.options[i].selected=true;
  mod_nodeSize(form);
  mod_nodeColor(form);
  mod_nodeShape(form);
  mod_edgeWidth(form);
  mod_edgeMerge(form);
  mod_labelStyle(form);
  mod_layout(form);
  cy.nodes().on('tap', tap_node);
  cy.edges().on('tap', tap_edge);
}
function netSummary(form) {
  var htm='<B>network summary:</B><ul>';
  htm+=('<li>nodes: '+cy.nodes().length);
  htm+=('<li>edges: '+cy.edges().length);
  htm+=('<li>targets: '+cy.nodes('[class = "target"]').length);
  htm+=('<li>compounds: '+cy.nodes('[class = "compound"]').length);
  htm+=('<li>scaffolds: '+cy.nodes('[class = "scaffold"]').length);
  htm+=('<li>mces: '+cy.nodes('[class = "mces"]').length);
  htm+=('<li>diseases: '+cy.nodes('[class = "disease"]').length);
  htm+='</ul>';
  return htm;
}
function tap_node(event){
  var node = event.target;
  //console.log('DEBUG: node: id='+node.id()+'; class='+node.data('class'));
  Object.keys(node.data()).forEach(k => {
    if (node.data()[k] != '') {
      write2div('log', `${k}: ${node.data()[k]}<br>`, false);
    }
  });
  if (node.data('class')=='compound' || node.data('class')=='scaffold') {
    var smi = (typeof node.data()['smiles'] !== 'undefined') ? node.data()['smiles'] : '*';
    smi = smi.replace('\\\\', '\\');
    write2div('depict', '<img src="http://'+servername()+MOL2IMG+'?smiles='+encodeURIComponent(smi)+'&width=180&height=140" height="140">', true);
  } else if (node.data('class')=='mces') {
    var smi = (typeof node.data()['smarts'] !== 'undefined') ? node.data()['smarts'] : '*';
    smi = smi.replace('\\\\', '\\');
    write2div('depict', '<img src="http://'+servername()+MOL2IMG+'?smiles='+encodeURIComponent(smi)+'&width=180&height=140" height="140">', true);
  }
  //write2div('log', 'DEBUG: node: id='+node.id()+'; class='+node.data('class')+'<br>', false);
}
function tap_edge(event){
  var edge = event.target;
  Object.keys(edge.data()).forEach(k => write2div('log', `${k}: ${edge.data()[k]}<br>`, false));
  write2div('log', 'edge: id='+edge.id()+'; class='+edge.data('class')+'<br>', false);
}
function mod_layout(form) {
  cy.layout({name: form.layout.value}).run();
}
function redo_layout(form) {
  mod_layout(form);
}
function mod_nodeSize(form) {
  if (form.nodescale.checked) {
    cy.style().selector('node').style('width', 25).style('height', 25).update();
    cy.style().selector('node[class = "target"]').style('width', 60).style('height', 60).update();
    cy.style().selector('node[class = "compound"]').style('width', 25).style('height', 25).update();
    cy.style().selector('node[class = "scaffold"]').style('width', 35).style('height', 35).update();
    cy.style().selector('node[class = "mces"]').style('width', 35).style('height', 35).update();
    cy.style().selector('node[class = "disease"]').style('width', 85).style('height', 35).update();
  }
  else {
    cy.style().selector('node').style('width', 25).style('height', 25).update();
  }
  return;
}
function mod_nodeColor(form) {
  if (form.nodecolor.checked) {
    cy.style().selector('node').style('background-color', '#444444').update();
    cy.style().selector('node[class = "target"]').style('background-color', '#0B94B1').update();
    cy.style().selector('node[class = "compound"]').style('background-color', '#88CC88').update();
    cy.style().selector('node[class = "scaffold"]').style('background-color', '#FFAF00').update();
    cy.style().selector('node[class = "mces"]').style('background-color', '#FFDD00').update();
    cy.style().selector('node[class = "disease"]').style('background-color', 'cyan').update();
  }
  else {
    cy.style().selector('node').style('background-color', '#444444').update();
  }
  return;
}
function mod_nodeShape(form) {
  if (form.nodeshape.checked) {
    cy.style().selector('node[class = "target"]').style('shape', 'octagon').update();
    cy.style().selector('node[class = "compound"]').style('shape', 'rectangle').update();
    cy.style().selector('node[class = "scaffold"]').style('shape', 'hexagon').update();
    cy.style().selector('node[class = "mces"]').style('shape', 'ellipse').update();
    cy.style().selector('node[class = "disease"]').style('shape', 'star').update();
  }
  else {
    cy.style().selector('node').style('shape', 'ellipse').update();
  }
  return;
}
function mod_edgeMerge(form) {
  return;
}
function mod_edgeWidth(form) { //Revise for degree-weighted width.
  if (form.edgescale.checked) {
    cy.style().selector('edge').style('width', 5).style('line-color', 'orange').style('line-style', 'dotted').update();
  }
  else {
    cy.style().selector('edge').style('width', 2).style('line-color', 'gray').update();
  }
  return;
}
function mod_compoundLabel(form) {
  var i;
  for (i=0;i<form.cpd_label.length;++i)
    if (form.cpd_label.options[i].selected)
      cpd_label=form.cpd_label.options[i].value;
  cy.style().selector('node[class = "compound"]').style('label', 'data('+cpd_label+')').update();
  return;
}
function mod_targetLabel(form) {
  var i;
  for (i=0;i<form.tgt_label.length;++i)
    if (form.tgt_label.options[i].selected)
      tgt_label=form.tgt_label.options[i].value;
  cy.style().selector('node[class = "target"]').style('label', 'data('+tgt_label+')').update();
  return;
}
function mod_labelStyle(form) {
  cy.style().selector('node').style('font-size', '12px').update();
  cy.style().selector('node').style('text-wrap', 'wrap').update();
  cy.style().selector('node').style('text-max-width', '120px').update();
  cy.style().selector('node').style('text-valign', 'center').update();
  cy.style().selector('node').style('text-halign', 'center').update();
  return;
}
function show_cheminfo(form) {
  if (form.cheminfo.checked) {
    cy.nodes('[class = "compound"]').forEach(function(n) {
      if (typeof n.data('smiles') !== 'undefined') {
        var smi = n.data('smiles').replace('\\\\', '\\');
        n.style('background-image', 'http://'+servername()+MOL2IMG+'?smiles='+encodeURIComponent(smi)).style('background-fit', 'cover');
      }
      cy.style().selector('node').style().update();
    });
  }
}
function preferred_synonym(synonyms) { // Find "name-like"
  if (synonyms.length==0) return '';
  var i_best=0;
  for (i_best=synonyms.length-1;i_best>=0;--i_best)
    if (synonyms[i_best].match(/^[A-Z][a-z]+$/)) return synonyms[i_best];
  for (i_best=synonyms.length-1;i_best>=0;--i_best)
    if (synonyms[i_best].match(/^[a-zA-Z ]+$/)) return synonyms[i_best];
  return synonyms[i_best];
}
function clear_div(id) {
  document.getElementById(id).innerHTML='';
}
function write2div(id, htm, clear) {
  if (typeof htm === 'undefined') { return; }
  if (clear) {
    document.getElementById(id).innerHTML='';
  }
  document.getElementById(id).innerHTML+=htm;
  scrollToBottom(id);
}
function scrollToBottom(id) {
   var div = document.getElementById(id);
   div.scrollTop = div.scrollHeight - div.clientHeight;
}
