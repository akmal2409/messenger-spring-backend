USE messenger;

CREATE TABLE IF NOT EXISTS users
(
    uid UUID,
    email TEXT,
    first_name TEXT,
    last_name TEXT,
    conversation_ids SET<TEXT>,
    PRIMARY KEY (uid)
);

CREATE TABLE IF NOT EXISTS messages_by_conversation
(
  conversation_id TIMEUUID,
  bucket int,
  message_id BIGINT,
  author_id UUID,
  body TEXT,
  PRIMARY KEY ((conversation_id, bucket), message_id)
);


CREATE TYPE IF NOT EXISTS user ( uid UUID, first_name TEXT, last_name TEXT );

CREATE TABLE IF NOT EXISTS conversations (
    conversation_id TIMEUUID PRIMARY KEY,
    participants SET<FROZEN<user>>
);
