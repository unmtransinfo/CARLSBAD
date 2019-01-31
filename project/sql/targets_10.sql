--
--
SELECT DISTINCT
        target.id AS tid,
        target.species,
        target.type AS ttype,
        target.name AS tname
FROM
        target
ORDER BY target.name,target.species,target.type
        ;
--
