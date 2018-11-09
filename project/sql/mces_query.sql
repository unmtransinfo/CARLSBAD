--
-- Note that "mces_id" was renamed "cluster_id".
--
SELECT DISTINCT
	compound.cluster_id,
	mces.mces,
	compound.id AS cid
FROM
	compound,
	mces
WHERE
	compound.cluster_id=mces.id
        AND gnova.matches(compound.smiles,'Cc1c(c2cc(ccc2n1C(=O)c3ccc(cc3)Cl)OC)CC(=O)O ')
	;
--
--
SELECT DISTINCT
	target.id AS tid,
	target.name,
	target.species,
	target.type,
	compound.cluster_id,
	mces.mces,
	compound.id AS cid
FROM
	target
LEFT OUTER JOIN
	identifier ON (identifier.target_id=target.id)
LEFT OUTER JOIN
	target_classifier ON (target_classifier.target_id=target.id)
LEFT OUTER JOIN
	activity ON (target.id=activity.target_id)
LEFT OUTER JOIN
	assay ON (assay.id=activity.assay_id)
LEFT OUTER JOIN
	substance ON (substance.id=activity.substance_id)
JOIN
	s2c ON (substance.id=s2c.substance_id)
JOIN
	compound ON (compound.id=s2c.compound_id)
JOIN
	mces ON (compound.cluster_id=mces.id)
WHERE
        target_classifier.type='Uniprot Family'
        AND target_classifier.id IN ('hedgehog')
	;
--
