--
SELECT DISTINCT
        target.id AS tid,
        compound.id AS cpdid,
        compound.cluster_id,
        mces.mces
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
        mces ON (compound.cluster_id=mces.id)
WHERE
        target.id IN ( 1,2 )
LIMIT 100
        ;
--
