\pset FORMAT ALIGNED ;
--
SELECT
        target.id AS tid,
        target.name AS tname,
        target.descr AS tdescr,
        target.species,
        target.type AS ttype,
        identifier.id_type,
        identifier.id
FROM
        target
LEFT OUTER JOIN
        identifier ON (identifier.target_id=target.id)
WHERE
        target.id IN (446,2121)
        ;
--
