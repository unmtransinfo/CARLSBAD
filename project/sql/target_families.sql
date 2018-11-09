--
SELECT DISTINCT
        target_classifier.type,
        target_classifier.id
FROM
        target_classifier
WHERE
        target_classifier.type = 'Uniprot Family'
        ;
--
SELECT DISTINCT
        target_classifier.type AS CTYPE,
        target_classifier.id AS CLASS,
        target.id AS TID,
        target.name,
        target.species,
        target.type AS TTYPE
FROM
        target_classifier,
        target
WHERE
        target_classifier.target_id=target.id
        AND target_classifier.type = 'Uniprot Family'
        AND target_classifier.id LIKE '%G-protein coupled receptor%'
        ;
--
