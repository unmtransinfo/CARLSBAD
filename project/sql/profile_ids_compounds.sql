--
SELECT
	id_type,
	count(id)
FROM
	identifier
JOIN
	s2c ON (identifier.substance_id=s2c.substance_id)
GROUP BY id_type
ORDER BY id_type
	;
--
