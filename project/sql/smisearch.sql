\timing
--
SELECT DISTINCT
	target.id AS tid,
	compound.id AS cpdid,
	compound.cluster_id AS mces,
	compound.mol_weight AS mwt,
	compound.smiles,
	av1.integer_value AS ro5,
	av2.number_value AS clogp,
	av3.integer_value AS hbd,
	av4.integer_value AS hba,
	av5.number_value AS tpsa
FROM
	target
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
LEFT OUTER JOIN
	attr_value av1 ON (compound.id=av1.compound_id)
JOIN
	attr_type at1 ON (av1.attr_type_id=at1.id)
LEFT OUTER JOIN
	attr_value av2 ON (compound.id=av2.compound_id)
JOIN
	attr_type at2 ON (av2.attr_type_id=at2.id)
LEFT OUTER JOIN
	attr_value av3 ON (compound.id=av3.compound_id)
JOIN
	attr_type at3 ON (av3.attr_type_id=at3.id)
LEFT OUTER JOIN
	attr_value av4 ON (compound.id=av4.compound_id)
JOIN
	attr_type at4 ON (av4.attr_type_id=at4.id)
LEFT OUTER JOIN
	attr_value av5 ON (compound.id=av5.compound_id)
JOIN
	attr_type at5 ON (av5.attr_type_id=at5.id)
WHERE
	target.id IN ( 1,2 )
	AND gnova.bit_contains(compound.gfp,gnova.fp('NCCc1cc(O)c(O)cc1'))
	AND gnova.matches(compound.smiles,'NCCc1cc(O)c(O)cc1')
	AND compound.mol_weight>200
	AND compound.mol_weight<500
	AND at1.name='ro5'
	AND at2.name='clogp'
	AND at3.name='hydrogen_bond_donor_count'
	AND at4.name='hydrogen_bond_acceptor_count'
	AND at5.name='tpsa'
LIMIT 50
	;
--
