\timing
--
SELECT DISTINCT
        target.id AS tid,
        compound.id AS cpdid,
        compound.cluster_id,
        compound.mol_weight,
        compound.smiles
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
WHERE
        target.id IN ( 1,2 )
        AND gnova.bit_contains(compound.gfp,gnova.fp('NCCc1cc(O)c(O)cc1'))
        AND gnova.matches(compound.smiles,'NCCc1cc(O)c(O)cc1')
        AND compound.mol_weight>200
        AND compound.mol_weight<500
LIMIT 100
        ;
--
