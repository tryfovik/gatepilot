create table if not exists gatepilot_resource (
    id bigint not null auto_increment,
    kind varchar(64) not null,
    namespace varchar(128) not null,
    name varchar(256) not null,
    uid varchar(64) not null,
    generation bigint not null,
    resource_json longtext not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_gatepilot_resource_key (kind, namespace, name),
    key idx_gatepilot_resource_updated_at (updated_at)
);
