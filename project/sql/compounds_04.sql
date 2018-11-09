\timing
--
SELECT DISTINCT
        compound.id AS cid,
        compound.mol_weight,
        target.id AS tid,
        target.name AS tname,
        target.species,
        target.type AS ttype
FROM
        compound
JOIN
        s2c ON (s2c.compound_id=compound.id)
JOIN
        substance ON (substance.id=s2c.substance_id)
JOIN
        activity ON (activity.substance_id=substance.id)
JOIN
        target ON (target.id=activity.target_id)
WHERE
        compound.id IN (100,200,300,400,500)
ORDER BY compound.id
        ;
--
