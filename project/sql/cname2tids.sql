\timing
--
SELECT DISTINCT
	target.id AS tid,
	target.name,
	target.descr,
	target.species,
	target.type,
	identifier.id_type,
	identifier.id
FROM
	target
LEFT OUTER JOIN
	identifier ON (identifier.target_id=target.id)
WHERE
	target.id IN (
		SELECT DISTINCT
			target.id
		FROM
			target
		JOIN
			identifier ON (identifier.target_id=target.id)
		JOIN
			activity ON (target.id=activity.target_id)
		JOIN
			substance ON (activity.substance_id=substance.id)
		JOIN
			s2c ON (substance.id=s2c.substance_id)
		JOIN
			compound ON (s2c.compound_id=compound.id)
		JOIN
			synonym ON (synonym.substance_id=substance.id)
		WHERE
			target.species='human'
			AND s2c.is_active
			AND synonym.name='clozapine'
	)
ORDER BY target.id,target.name ASC
	;
