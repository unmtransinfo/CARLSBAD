--
SELECT
        compound.id,
        compound.mol_weight AS mwt,
        compound.smiles
FROM
        compound
WHERE
        gnova.matches(compound.smiles,'c1cnn[nH]1')
LIMIT 10
        ;
--
--Exact search:
SELECT
        compound.id,
        compound.mol_weight AS mwt,
        compound.smiles
FROM
        compound
WHERE
        compound.smiles=gnova.cansmiles('CCCOc1ccccc1c2[nH]c(=O)c3c(n2)[nH]nn3')
        ;
--
