-- Drugs:
SELECT
	count(DISTINCT compound.id) AS "drug_count"
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
WHERE
	substance.is_drug
	AND s2c.is_active
	;
--
SELECT
	count(DISTINCT compound.id) AS "drugnamed_count"
FROM
	compound
JOIN
	s2c ON (s2c.compound_id=compound.id)
JOIN
	substance ON (substance.id=s2c.substance_id)
JOIN
	synonym ON (substance.id=synonym.substance_id)
WHERE
	substance.is_drug
	AND s2c.is_active
	AND CHAR_LENGTH(synonym.name) >= 3
	AND CHAR_LENGTH(synonym.name) <= 80
	AND synonym.name ~* '[a-z][a-z][a-z]'
	;
-- Targets:
SELECT
	count(DISTINCT id) AS "target_count",
	species
FROM
	target
GROUP BY species
ORDER BY species
	;
--
SELECT
	count(DISTINCT (name||species||type)) AS "target_nst_count"
FROM
	target
WHERE
	species = 'human'
	;
-- Diseases:
SELECT
	count(DISTINCT kegg_disease.name) AS "disease_count"
FROM
        kegg_disease
        ;
--
SELECT
	count(DISTINCT (kegg_disease.name||target_classifier.target_id::character)) AS "disease_target_link_count",
	target_classifier.type
FROM
        kegg_disease 
JOIN
        target_classifier ON (kegg_disease.id=target_classifier.id)
GROUP BY target_classifier.type
ORDER BY target_classifier.type ASC
        ;
--
SELECT
	count(DISTINCT kegg_disease.name) AS "disease_count",
	count(DISTINCT target_classifier.target_id) AS "target_count",
	target_classifier.type
FROM
        kegg_disease 
JOIN
        target_classifier ON (kegg_disease.id=target_classifier.id)
GROUP BY target_classifier.type
ORDER BY target_classifier.type ASC
        ;
--
SELECT
	count(DISTINCT kegg_disease.name) AS "disease_count",
	target_classifier.type
FROM
        kegg_disease 
JOIN
        target_classifier ON (kegg_disease.id=target_classifier.id)
GROUP BY target_classifier.type
ORDER BY target_classifier.type ASC
        ;
