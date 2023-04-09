-- create databases
CREATE DATABASE IF NOT EXISTS `test`;

-- create root user and grant rights

CREATE USER 'test'@'localhost' IDENTIFIED BY '1234';
CREATE USER 'test'@'%' IDENTIFIED BY '1234';

GRANT ALL PRIVILEGES ON *.* TO 'test'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO 'test'@'%';