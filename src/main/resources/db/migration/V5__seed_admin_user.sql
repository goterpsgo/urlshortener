INSERT INTO users (username, password_hash, role, created_at)
VALUES ('${admin-username}', '${admin-password-hash}', 'ADMIN', CURRENT_TIMESTAMP);
