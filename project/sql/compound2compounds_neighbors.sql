--
--
--Neighbor cpd search:
SELECT DISTINCT
        c2.id AS cid_nbr,
        c2.smiles AS smi_nbr,
        scaffold.id AS scafid,
        scaffold.smiles AS scafsmi
FROM
        compound AS c1,
        compound AS c2,
        scafid2cid AS s2c1,
        scafid2cid AS s2c2,
        scaffold
WHERE
        c1.id = 1168
        AND s2c1.compound_id = c1.id
        AND s2c1.scaffold_id = scaffold.id
	AND s2c1.is_largest
        AND scaffold.id = s2c2.scaffold_id
	AND s2c2.compound_id = c2.id
	AND s2c2.is_largest
        ;
--
