-- CREATE KEYSPACE messenger WITH replication = {'class': 'SimpleStrategy',
--     'replication_factor': 2} AND DURABLE_WRITES = true;
USE messenger;

CREATE TYPE IF NOT EXISTS user (
    uid UUID,
    first_name TEXT,
    last_name TEXT,
    profile_image_url TEXT,
    profile_thumbnail_url TEXT
);

CREATE TABLE IF NOT EXISTS users
(
    uid UUID,
    email TEXT,
    first_name TEXT,
    last_name TEXT,
    profile_image_url TEXT,
    profile_thumbnail_url TEXT,
    contacts SET<frozen<user>>,
    thread_ids SET<timeuuid>,
    PRIMARY KEY (uid)
);

CREATE TABLE IF NOT EXISTS messages_by_user_by_thread
(
  uid UUID,
  thread_id TIMEUUID,
  bucket int,
  message_id BIGINT,
  author_id UUID,
  body TEXT,
  is_read BOOLEAN,
  is_edited BOOLEAN,
  is_system_message boolean,
  PRIMARY KEY ((uid, thread_id, bucket), message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);

-- When any participant updates their details, go and update the thread
CREATE TABLE IF NOT EXISTS threads
(
  thread_id TIMEUUID,
  members SET<FROZEN<user>>,
  thread_name TEXT, -- only needed if it is a group chat
  thread_picture_thumbnail_url TEXT, -- only needed if it is a group chat
  thread_picture_url TEXT, -- only needed if it is a group chat
  is_group_thread boolean,
  PRIMARY KEY (thread_id)
);


CREATE TABLE IF NOT EXISTS threads_by_user_by_last_message
(
    uid UUID,
    thread_id TIMEUUID,
    message_id BIGINT,
    time timestamp,
    thread_name TEXT,
    thread_picture_thumbnail_url TEXT,
    message TEXT,
    author frozen<user>,
    is_read BOOLEAN,
    is_system_message boolean,
    PRIMARY KEY ((uid), thread_id)
);
