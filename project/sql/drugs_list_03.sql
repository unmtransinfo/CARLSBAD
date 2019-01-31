\timing
--
SELECT DISTINCT
	compound.id AS cid,
	synonym.name AS drug_name,
	count(target.id) AS tgt_count
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
JOIN
	activity ON (activity.substance_id=substance.id)
JOIN    
	target ON (target.id=activity.target_id)
LEFT OUTER JOIN
	synonym ON (substance.id=synonym.substance_id)
WHERE
	substance.is_drug
GROUP BY
	compound.id,synonym.name
ORDER BY
	compound.id,synonym.name
	;
--
