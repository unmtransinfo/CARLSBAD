--
SELECT DISTINCT
        target_classifier.id
FROM
        target_classifier
WHERE
        target_classifier.type = 'ChEMBL Class'
ORDER BY
        target_classifier.id
        ;
--
