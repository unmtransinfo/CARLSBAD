\timing
--
SELECT DISTINCT
	compound.id AS cpdid,
	compound.cluster_id AS mces,
	compound.mol_weight AS mwt,
	compound.smiles,
	synonym.name
FROM
	compound
JOIN
	s2c ON (compound.id=s2c.compound_id)
JOIN
	substance ON (substance.id=s2c.substance_id)
JOIN
	synonym ON (synonym.substance_id=substance.id)
WHERE
	synonym.name ILIKE '%clozapine%'
ORDER BY compound.id
LIMIT 500
	;
--
