SELECT DISTINCT
	activity.id AS act_id,
	activity.target_id AS tid,
	activity.assay_id,
	activity.substance_id AS sid,
	activity.confidence AS con,
	activity.value,
	activity.value_unit,
	activity.effect,
	activity.type AS act_type,
	assay.name AS aname,
	target.name AS tname,
	compound.id AS cid,
	compound.smiles,
	substance.name AS sname
FROM
	activity
LEFT OUTER JOIN
	target ON (target.id=activity.target_id)
LEFT OUTER JOIN
	assay ON (assay.id=activity.assay_id)
LEFT OUTER JOIN
	substance ON (substance.id=activity.substance_id)
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (compound.id=s2c.compound_id)
WHERE
	target_id IN (327,365,1012,1210)
	AND s2c.is_active
	;
--
