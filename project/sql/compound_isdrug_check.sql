SELECT DISTINCT
        compound.id AS cid,
	substance.id AS sid,
	synonym.name,
        substance.is_drug,
	compound.smiles
FROM
        compound
JOIN
        s2c ON (s2c.compound_id=compound.id)
JOIN
        substance ON (substance.id=s2c.substance_id)
LEFT OUTER JOIN
        synonym ON (substance.id=synonym.substance_id)
WHERE
	compound.id IN (
	24803184,
	24802842,
	3047758,
	64928,
65981,
64928,
387316,
3883
	)
ORDER BY
	compound.id
        ;
--
