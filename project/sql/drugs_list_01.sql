SELECT DISTINCT
        compound.id AS cid,
	substance.id AS sid,
	synonym.name,
	compound.smiles
FROM
        compound
JOIN
        s2c ON (s2c.compound_id=compound.id)
JOIN
        substance ON (substance.id=s2c.substance_id)
LEFT OUTER JOIN
        synonym ON (substance.id=synonym.substance_id)
WHERE
        substance.is_drug
ORDER BY
	compound.id
        ;
--
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
        ;
--
