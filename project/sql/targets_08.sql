\timing
--
-- Count of activities?  Count of compounds?
--
SELECT DISTINCT
        target.id AS tid,
	target.name AS tname,
	target.species,
	target.type AS ttype,
	count(compound.id) AS cpd_count
FROM
        target
JOIN
        activity ON (activity.target_id=target.id)
JOIN
        substance ON (substance.id=activity.substance_id)
JOIN
        s2c ON (s2c.substance_id=substance.id)
JOIN
        compound ON (compound.id=s2c.compound_id)
GROUP BY
	target.id,target.name,target.species,target.type
        ;
--
