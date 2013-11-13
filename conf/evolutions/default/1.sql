# --- !Ups

CREATE TABLE THREAD_CACHE (
ID SERIAL PRIMARY KEY,
THREAD BINARY(20) NOT NULL UNIQUE,
MODIFIED NUMERIC(20) NOT NULL
);

CREATE TABLE RESPONSE_CACHE (
ID SERIAL PRIMARY KEY,
THREAD  BINARY(20) NOT NULL,
RESPONSE BINARY(20) NOT NULL UNIQUE,
MODIFIED NUMERIC(20) NOT NULL
);

CREATE TABLE SETTING_LOG (
ID SERIAL PRIMARY KEY,
MESSAGE TEXT NOT NULL,
MODIFIED NUMERIC(20) NOT NULL
);

INSERT INTO SETTING_LOG (MESSAGE, MODIFIED) VALUES ('INITIALIZED', 0);
-- INSERT INTO THREAD_CACHE (THREAD, MODIFIED) VALUES ('deadbeefdeadbeefdead', CURRENT_TIMESTAMP());
-- INSERT INTO RESPONSE_CACHE (THREAD, RESPONSE, MODIFIED) VALUES ('deadbeefdeadbeefdead', 'deadbadbeefbeefbeefe', CURRENT_TIMESTAMP());

# --- !Downs

DROP TABLE THREAD_CACHE;
DROP TABLE RESPONSE_CACHE;
DROP TABLE SETTING_LOG;
