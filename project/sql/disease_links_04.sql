--
--	How many diseases have links?
--
SELECT DISTINCT
        kegg_disease.id,
	SUBSTR(kegg_disease.name,1,50) AS "disease"
FROM
	kegg_disease
JOIN
        target_classifier ON (kegg_disease.id=target_classifier.id)
JOIN
        target ON (target_classifier.target_id=target.id)
        ;
--
