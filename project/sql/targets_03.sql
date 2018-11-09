\timing
--
SELECT DISTINCT
        target.id AS tid,
        compound.id AS cpdid,
        compound.cluster_id,
        compound.mol_weight,
        compound.smiles,
        gnova.tanimoto(gnova.fp('c1cc2c(cc1OCCO)cc3c(n2)[nH]c(=O)[nH]3'),compound.gfp)
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
        AND gnova.tanimoto(gnova.fp('c1cc2c(cc1OCCO)cc3c(n2)[nH]c(=O)[nH]3'),compound.gfp)>0.7
        AND compound.mol_weight>200
        AND compound.mol_weight<500
ORDER BY gnova.tanimoto(gnova.fp('c1cc2c(cc1OCCO)cc3c(n2)[nH]c(=O)[nH]3'),compound.gfp) DESC
LIMIT 100
        ;
--
