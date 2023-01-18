CREATE TABLE sample_person (
id SERIAL,
version INTEGER,
first_name VARCHAR(255),
last_name VARCHAR(255),
phone VARCHAR(255),
role VARCHAR(10),
occupation VARCHAR(255),
important BOOLEAN,
email VARCHAR(100),
date_of_birth TIMESTAMP
);
