--
--	SUBSTR(kegg_disease.name,1,50) AS "disease",
--
SELECT DISTINCT
        kegg_disease.id,
        target.id AS tid,
        target.name,
        target.species,
        target.type
FROM
	kegg_disease
JOIN
        target_classifier ON (kegg_disease.id=target_classifier.id)
JOIN
        target ON (target_classifier.target_id=target.id)
ORDER BY
	kegg_disease.id
        ;
--
