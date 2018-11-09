\timing
--
-- Count of activities?  Count of targets?
--
SELECT DISTINCT
        compound.id AS cid,
	count(activity.id) AS act_count,
	count(target.id) AS tgt_count
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
GROUP BY compound.id
        ;
--
