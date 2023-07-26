CREATE TABLE IF NOT EXISTS serverless_services(id integer primary key, name varchar(255) not null);

INSERT INTO serverless_services (id,name) VALUES (1,'Cloud Run');
INSERT INTO serverless_services (id,name) VALUES (2,'Cloud Functions');
INSERT INTO serverless_services (id,name) VALUES (3,'AppEngine');