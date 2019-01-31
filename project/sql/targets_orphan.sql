SELECT
	target.id AS tid,
	COUNT(cbactivity.substance_id) AS cbactivity_count
FROM
	target
LEFT OUTER JOIN
	cbactivity ON (cbactivity.target_id=target.id)
GROUP BY
	target.id
ORDER BY
	cbactivity_count DESC
	;
--
SELECT
	q1.cbactivity_count,
	COUNT(q1.tid) AS tid_count
FROM
	(
	SELECT
		target.id AS tid,
		COUNT(cbactivity.substance_id) AS cbactivity_count
	FROM
		target
	LEFT OUTER JOIN
		cbactivity ON (cbactivity.target_id=target.id)
	GROUP BY
		target.id
	) AS q1
GROUP BY
	q1.cbactivity_count
ORDER BY
	q1.cbactivity_count
	;
--
