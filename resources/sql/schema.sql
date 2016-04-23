
CREATE OR REPLACE FUNCTION column_exists(tablename varchar(255), column_name_ varchar(255)) RETURNS boolean AS $$
BEGIN
return EXISTS(SELECT table_name, column_name
              FROM  information_schema.columns
	      WHERE table_name = lower(tablename) AND column_name=lower(column_name_));
END
 $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION create_all() RETURNS void AS $$
 BEGIN

-- We can assume we're already in a database, cause, you know, this isn't MS SQL Server
-- We can also assume we have only one database

CREATE TABLE IF NOT EXISTS Font_Preference (
       ID SERIAL PRIMARY KEY,
       bold BOOLEAN NOT NULL DEFAULT FALSE,
       italic BOOLEAN NOT NULL DEFAULT FALSE,
       underline BOOLEAN NOT NULL DEFAULT FALSE,
       color CHAR(7) NOT NULL DEFAULT '#000000', 
       font_name VARCHAR(1000) NOT NULL DEFAULT '''Helvetica Neue'', Verdana, Helvetica, Arial, sans-serif');
       

CREATE TABLE IF NOT EXISTS Users (
       username VARCHAR(255) PRIMARY KEY NOT NULL,
       displayname VARCHAR(255) UNIQUE NOT NULL,
       password VARCHAR(1000) NOT NULL, --No idea how long hashes heroku's/clojure's sha-512 will produce
       img_location VARCHAR(400) NOT NULL,
       personal_message VARCHAR(2000) NOT NULL,
       font_id INT,
       FOREIGN KEY (font_id) REFERENCES Font_Preference(ID));

IF NOT column_exists('Users', 'admin') THEN
   ALTER TABLE Users
   	 ADD admin BOOLEAN NOT NULL DEFAULT FALSE;
END IF;

CREATE TABLE IF NOT EXISTS Friendship (
       username1 VARCHAR(255) NOT NULL,
       username2 VARCHAR(255) NOT NULL,
       PRIMARY KEY(username1, username2),
       FOREIGN KEY(username1) REFERENCES Users(username)
       	       ON UPDATE CASCADE
	       ON DELETE CASCADE,
       FOREIGN KEY(username2) REFERENCES Users(username)
       	       ON UPDATE CASCADE
	       ON DELETE CASCADE);
	       
IF NOT EXISTS (SELECT * FROM Font_Preference) THEN
	INSERT INTO Font_Preference (bold)
	VALUES (false);
END IF;

END
 $$ LANGUAGE plpgsql;
