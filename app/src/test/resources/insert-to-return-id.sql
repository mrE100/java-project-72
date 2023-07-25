INSERT INTO url (name, created_at) VALUES ('a', '2023-07-25 12:00:00');

INSERT INTO url_check (
    status_code, title, h1, description, url_id, created_at
)
VALUES (
           200, 'addedTestCheckWithId', 'a', 'a', (SELECT id FROM url WHERE name = 'a'), '2023-07-25 12:00:00'
       );