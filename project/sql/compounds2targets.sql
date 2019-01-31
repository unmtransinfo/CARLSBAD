\timing
--
SELECT DISTINCT
	compound.id AS cid,
	target.id AS tid,
	target.name AS tname,
	target.species,
	target.type AS ttype
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
JOIN
	cbactivity ON (cbactivity.substance_id=substance.id)
JOIN
	target ON (target.id=cbactivity.target_id)
WHERE
	compound.id IN (393157)
	AND s2c.is_active
	;
--
