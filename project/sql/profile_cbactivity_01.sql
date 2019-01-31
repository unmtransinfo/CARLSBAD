--
SELECT
	'"'||type||'"' AS cbactivity_type,
	count(id),
	ROUND(AVG(value),2) AS mean_value,
	MIN(value) AS min_value,
	MAX(value) AS max_value
FROM
	cbactivity
GROUP BY type
ORDER BY type
	;
--
SELECT
	confidence,
	count(id)
FROM
	cbactivity
GROUP BY confidence
ORDER BY confidence
	;
--
