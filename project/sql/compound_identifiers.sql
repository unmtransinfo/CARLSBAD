\timing
--
SELECT DISTINCT
	compound.id AS cid,
	identifier.id_type,
	identifier.id
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
LEFT OUTER JOIN
	identifier ON (identifier.substance_id=s2c.substance_id)
WHERE
	compound.id IN (54,5442)
ORDER BY
	compound.id,identifier.id_type,identifier.id
	;
--
