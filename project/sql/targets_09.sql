--
--
SELECT DISTINCT
        target.id AS tid,
        target.name AS tname,
        target.species,
        target.type AS ttype,
        identifier.id_type,
        identifier.id
FROM
        target
LEFT OUTER JOIN
        identifier ON (identifier.target_id=target.id)
ORDER BY target.id,identifier.id_type,identifier.id ASC
        ;
--
