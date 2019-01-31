\timing
--
SELECT DISTINCT
	compound.id AS cid,
	id_pcc.id AS pubchem_cid,
	id_pcs.id AS pubchem_sid,
	id_chembl.id AS chembl_id
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
LEFT OUTER JOIN
	identifier AS id_pcc ON (id_pcc.substance_id=s2c.substance_id)
LEFT OUTER JOIN
	identifier AS id_pcs ON (id_pcs.substance_id=s2c.substance_id)
LEFT OUTER JOIN
	identifier AS id_chembl ON (id_chembl.substance_id=s2c.substance_id)
WHERE
	id_pcc.id_type='PubChem CID'
	AND id_pcs.id_type='PubChem SID'
	AND id_chembl.id_type='ChEMBL ID'
	AND compound.id IN (54,5442)
	;
--
-- identifier.id_type:
-- CAS Registry No.   
-- ChEBI              
-- ChEMBL ID          
-- ChEMBL Ligand      
-- DrugBank           
-- iPHACE             
-- IUPHAR Ligand ID   
-- NURSA Ligand       
-- PDSP Record Number 
-- PharmGKB Drug      
-- PubChem CID        
-- PubChem SID        
-- RCSB PDB Ligand    
-- SMDL ID            
