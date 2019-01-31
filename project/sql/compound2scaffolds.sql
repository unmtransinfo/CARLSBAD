--
SELECT DISTINCT
        scaffold.id AS scafid,
        scaffold.smiles AS scafsmi,
        scaffold.natoms,
        compound.id AS cid
FROM
        scaffold
JOIN
        scafid2cid ON (scaffold.id=scafid2cid.scaffold_id)
JOIN
        compound ON (compound.id=scafid2cid.compound_id)
WHERE
	scafid2cid.is_largest
	AND compound.id IN ( 573243 )
ORDER BY natoms DESC
        ;
--
