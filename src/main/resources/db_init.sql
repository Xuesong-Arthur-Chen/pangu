DROP ALL OBJECTS DELETE FILES;

CREATE TABLE users
(
user_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
balance BIGINT NOT NULL CHECK (balance >= 0)
);

CREATE TABLE transactions
(
transaction_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
transaction_time TIMESTAMP NOT NULL,
from_user BIGINT NOT NULL,
to_user BIGINT NOT NULL,
amount BIGINT NOT NULL CHECK (amount > 0),
CONSTRAINT from_user_ref FOREIGN KEY (from_user) REFERENCES users(user_id),
CONSTRAINT to_user_ref FOREIGN KEY (to_user) REFERENCES users(user_id)
);

CREATE INDEX transactions_time
ON transactions (transaction_time);

CREATE INDEX transactions_from_user
ON transactions (from_user);

CREATE INDEX transactions_to_user
ON transactions (to_user);