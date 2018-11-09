\timing
--
SELECT DISTINCT
        compound.id AS cid,
        compound.mol_weight,
        substance.name AS sname,
        target.id AS tid,
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
        compound.mol_weight>300
        AND compound.mol_weight<301
        ;
--
