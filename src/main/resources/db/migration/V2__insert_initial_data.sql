-- Вставка статусов
INSERT INTO status (name) VALUES
('ACTIVE'),
('DELETED');

-- Вставка ролей
INSERT INTO roles (name, status_id) VALUES
('ADMIN', 1), -- 1 соответствует ID для 'ACTIVE'
('USER', 1),
('MODERATOR', 1);

-- Вставка пользователей
INSERT INTO users (username, first_name, last_name, email, password, status_id) VALUES
('kobel', 'Alex', 'Kobelskiy', 'akobelskiy@xmail.com', 'password123', 1),
('boris', 'Boris', 'Smith', 'bsmith@example.com', 'password123', 1),
('victor', 'Victor', 'Ivanov', 'vivanov@yahoo.com', 'password123', 1);

-- Вставка файлов
INSERT INTO files (name, location, status_id) VALUES
('admin_report.txt', '/files/admin_report.txt', 1),
('user_report.txt', '/files/user_report.txt', 1),
('moderator_report.txt', '/files/moderator_report.txt', 1);

-- Вставка событий
INSERT INTO events (user_id, file_id, status_id) VALUES
(1, 1, 1), -- Пользователь 'kobel', файл 'admin_report.txt', статус 'ACTIVE'
(2, 2, 1), -- Пользователь 'boris', файл 'user_report.txt', статус 'ACTIVE'
(3, 3, 1); -- Пользователь 'victor', файл 'moderator_report.txt', статус 'ACTIVE'

-- Вставка связей пользователь-роли
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- Пользователь 'kobel' с ролью 'ADMIN'
(2, 2), -- Пользователь 'boris' с ролью 'USER'
(3, 3); -- Пользователь 'victor' с ролью 'MODERATOR'