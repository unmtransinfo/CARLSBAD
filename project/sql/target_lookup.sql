--
SELECT
	id AS "UniProt",
	target_id
FROM
	identifier
WHERE
	id_type='UniProt'
	AND id IN (
		'Q6IMK6',
		'O08858',
		'Q61125',
		'P21731-3',
		'Q61143',
		'P43166',
		'Q9NYA1',
		'O54890',
		'P45983',
		'B5THE3',
		'A7E2X7' )
	;
--
--
SELECT
	id AS "NCBI_gi",
	target_id
FROM
	identifier
WHERE
	id_type='NCBI gi'
	AND id IN (
		'111118990',
		'206582107',
		'3183063',
		'543761',
		'113083',
		'6980492',
		'40217817',
		'92110055' )
	;
--
