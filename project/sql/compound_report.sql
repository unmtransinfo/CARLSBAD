\timing
--
SELECT DISTINCT
	compound.id AS cid,
	compound.iso_smiles AS smiles,
	compound.mol_weight,
	compound.mol_formula,
	compound.nass_tested,
	compound.nass_active,
	compound.nsam_tested,
	compound.nsam_active,
	compound.cluster_id AS mcesid,
	substance.iupac_name,
--	target.id AS tid,
--	target.name AS tname,
--	target.species,
--	target.type AS ttype,
	synonym.name AS synonym
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
--JOIN
--	activity ON (activity.substance_id=substance.id)
--JOIN
--	target ON (target.id=activity.target_id)
LEFT OUTER JOIN
	synonym ON (substance.id=synonym.substance_id)
WHERE
	compound.id = 160355
	;
--
-- compound.id=54
