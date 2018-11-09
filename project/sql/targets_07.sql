\timing
--
-- Count of activities?  Count of compounds?
--
SELECT DISTINCT
        target.id AS tid,
	count(activity.id) AS act_count,
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
WHERE
        target.id IN (100,200,300,400,500)
GROUP BY target.id
        ;
--
