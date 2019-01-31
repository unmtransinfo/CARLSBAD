--
--	How many diseases have links?
--
SELECT
        count(DISTINCT kegg_disease.id) AS "linked_disease_count"
FROM
	kegg_disease
JOIN
        target_classifier ON (kegg_disease.id=target_classifier.id)
JOIN
        target ON (target_classifier.target_id=target.id)
        ;
--
