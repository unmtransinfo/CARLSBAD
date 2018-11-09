--
SELECT DISTINCT
        cbactivity.id AS act_id,
        cbactivity.target_id AS tid,
        cbactivity.substance_id AS sid,
        cbactivity.confidence AS con,
        cbactivity.value,
        cbactivity.type AS act_type,
        target.name AS tname,
        compound.id AS cid,
        compound.smiles
FROM
        cbactivity
LEFT OUTER JOIN
        target ON (target.id=cbactivity.target_id)
LEFT OUTER JOIN
        substance ON (substance.id=cbactivity.substance_id)
JOIN
        s2c ON (substance.id=s2c.substance_id)
JOIN
        compound ON (compound.id=s2c.compound_id)
WHERE
	target.id IN (983)
        ;
--
--	target_id IN (159)
