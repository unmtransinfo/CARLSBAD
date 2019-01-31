--
SELECT
	id_type,
	count(id)
FROM
	identifier
GROUP BY id_type
ORDER BY id_type
	;
--
