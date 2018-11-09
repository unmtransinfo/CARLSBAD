SELECT DISTINCT
	target.id AS tid,
	target.name,
	target.descr,
	target.species,
	target.type,
	target_classifier.id AS class,
	identifier.id_type,
	identifier.id
FROM
	target
LEFT OUTER JOIN
	identifier ON (identifier.target_id=target.id)
LEFT OUTER JOIN
	target_classifier ON (target_classifier.target_id=target.id)
WHERE
	target_classifier.type='Uniprot Family'
	AND target_classifier.id IN ('hedgehog')
ORDER BY target.id,target.name ASC
	;
--
