--
SELECT DISTINCT
        target.id AS tid,
        target.name,
        target.species,
        target.type,
        target_classifier.id,
	SUBSTR(kegg_disease.name,1,50) AS "disease"
FROM
        target
JOIN
        target_classifier ON (target_classifier.target_id=target.id)
JOIN
	kegg_disease ON (kegg_disease.id=target_classifier.id)
WHERE
        target_classifier.id = 'ds:H00523'
        AND target_classifier.type='KEGG Disease'
ORDER BY target.id,target.name ASC
        ;
--
