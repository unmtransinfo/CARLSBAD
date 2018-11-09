--
SELECT DISTINCT
        target.id AS tid,
        target.species,
        target.type AS ttype,
        identifier.id_type,
        identifier.id
FROM
        target
LEFT OUTER JOIN
        identifier ON (target.id=identifier.target_id)
JOIN
        activity ON (target.id=activity.target_id)
JOIN
        substance ON (activity.substance_id=substance.id)
JOIN
        s2c ON (substance.id=s2c.substance_id)
JOIN
        compound ON (s2c.compound_id=compound.id)
WHERE
        target.species='human'
        AND compound.mol_weight>300
        AND compound.mol_weight<301
        ;
--
