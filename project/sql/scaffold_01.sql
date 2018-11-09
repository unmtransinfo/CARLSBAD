--
SELECT DISTINCT
        scaffold.id AS scafid,
        scaffold.smiles AS scafsmi,
        compound.id AS cpdid
FROM
        target
LEFT OUTER JOIN
        activity ON (target.id=activity.target_id)
LEFT OUTER JOIN
        assay ON (assay.id=activity.assay_id)
LEFT OUTER JOIN
        substance ON (substance.id=activity.substance_id)
JOIN
        s2c ON (substance.id=s2c.substance_id)
JOIN
        compound ON (compound.id=s2c.compound_id)
JOIN
        scafid2cid ON (compound.id=scafid2cid.compound_id)
JOIN
        scaffold ON (scaffold.id=scafid2cid.scaffold_id)
WHERE
        scafid2cid.is_largest
        AND target.id IN ( 1 )
LIMIT 100
        ;
--
--
--Scaffold search:
SELECT DISTINCT
        scaffold.id AS scafid,
        scaffold.smiles AS scafsmi,
        compound.id AS cpdid,
        compound.smiles AS cpdsmi
FROM
        scaffold,
        compound,
        scafid2cid
WHERE
        scaffold.id = 17
        AND scaffold.id = scafid2cid.scaffold_id
        AND scafid2cid.compound_id = compound.id
        AND scafid2cid.is_largest
        ;
--
