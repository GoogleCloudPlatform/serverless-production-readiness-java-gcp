create sequence quotes_id_seq start with 1 increment by 10;

create table quotes
(
    id bigint DEFAULT nextval('quotes_id_seq') not null,
    quote varchar(1024) not null,
    author varchar(256) not null,
    book varchar(256) not null,
    primary key (id),
    constraint quotes_code_unique unique (quote)
);