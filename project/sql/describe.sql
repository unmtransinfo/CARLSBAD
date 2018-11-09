-- describe.sql
-- Describe contents and size of CARLSBAD.
--
SELECT table_name FROM information_schema.tables WHERE table_schema='public';
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='scaffold';
SELECT count(*) AS scaffold_count FROM public.scaffold;
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='scafid2cid';
SELECT count(*) AS scafid2cid_count FROM public.scafid2cid;
SELECT count(*) AS cpd_count FROM public.compound;
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='compound';
SELECT count(*) AS target_count FROM public.target;
SELECT count(*) AS cpd_w_assdata FROM public.compound WHERE nass_tested IS NOT NULL;
SELECT count(*) AS cpd_wo_assdata FROM public.compound WHERE nass_tested IS NULL;
SELECT count(*) AS synonym_count FROM public.synonym;
SELECT column_name,data_type FROM information_schema.columns WHERE table_name='synonym';
SELECT * FROM public.db_properties;
SELECT id,name FROM public.attr_type;
SELECT name,version,to_char(load_date,'YYYY-MM-DD') AS load_date FROM public.dataset;
--
