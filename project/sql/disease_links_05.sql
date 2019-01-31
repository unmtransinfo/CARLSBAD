--
--
SELECT DISTINCT
	kegg_disease.id,
	kegg_disease.name,
	target.id AS tid
FROM
	kegg_disease
JOIN
	target_classifier ON (kegg_disease.id=target_classifier.id)
JOIN
	target ON (target_classifier.target_id=target.id)
WHERE
	target_classifier.type='KEGG Disease'
ORDER BY kegg_disease.id
        ;
--
