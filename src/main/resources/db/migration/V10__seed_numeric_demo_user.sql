INSERT INTO user_account (username, password_hash, role, display_name, enabled)
SELECT '123', '{noop}123', 'ROLE_USER', '123', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM user_account WHERE username = '123'
);
