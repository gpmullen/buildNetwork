create schema medium;
create table links (
    Source	string,
    Target string,
    Type string,
    Id string,
    Label string,
    timeset string,
    Weight string);
create table nodes (
    Id	string,
    Label string,
    timeset string	,
    modularity_class string);

put file:///<directory>/nodes.csv @%nodes;
COPY INTO nodes from @%nodes;
put file:///<directory>/links.csv @%links;
copy into links from @%links;


create view data_shares_view as
select provider.id pid,provider.label pname,consumer.id cid, consumer.label cname
from links
inner join nodes provider
    on links.source = provider.id
inner join nodes consumer
    on links.target = consumer.id
;

CREATE or replace TABLE PROCESSED_SHARES AS
WITH SHARES AS (SELECT  PID, pname, CID, cname
FROM data_shares_view)
SELECT T.* FROM data_shares_view
,TABLE(udf_buildNetwork(pname,PID,cname, CID))T;

select * from PROCESSED_SHARES;

create or replace view links_udf as
With key_cte as (select row_number()
  over (order by providername,consumername desc) key, *
  from processed_shares)
  select DISTINCT 'provider' type, key, providername name, providerid id, providerx x,providery y from key_cte
  union select DISTINCT 'consumer' type, key, consumername name, consumerid id,consumerx x,consumery y from key_cte;

create or replace VIEW Nodes_udf as
with Provider as (select DISTINCT providerid id ,providername name, providerx x,providery y  from PROCESSED_SHARES ),
Consumer as (select DISTINCT consumerid,consumername, consumerx x, consumery y  from PROCESSED_SHARES
)
select * from Provider union select * from Consumer;
