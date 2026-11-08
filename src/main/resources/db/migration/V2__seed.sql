-- Lokal sinov uchun o'yin serveri (Admin panel → Serverlar bo'limidan o'zgartiring)
INSERT INTO game_servers (name, region, ip, port, rcon_password)
VALUES ('Toshkent #1', 'Toshkent', '127.0.0.1', 27015, 'change-me-rcon');

INSERT INTO news (category, title, body)
VALUES ('YANGILANISH', 'CS2 ARENA ishga tushdi', 'Custom 5v5 matchlar, ELO reyting va Premium endi mavjud.'),
       ('TURNIR', 'Birinchi turnirga tayyorlaning', 'Turnirlar bo''limi tez orada ochiladi.'),
       ('PREMIUM', 'Premium a''zolik', 'Premium bilan profilingizda maxsus belgi paydo bo''ladi.');
