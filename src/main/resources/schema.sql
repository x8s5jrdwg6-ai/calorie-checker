CREATE TABLE IF NOT EXISTS users (
  user_id     varchar(36)  PRIMARY KEY,
  user_name   varchar(36),
  password    varchar(255) NOT NULL,
  regist_date date         NOT NULL
);

CREATE TABLE IF NOT EXISTS food_maker (
  food_maker_id  integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  maker_name     varchar(50) NOT NULL,
  regist_user_id varchar(36) NOT NULL REFERENCES users(user_id),
  UNIQUE (regist_user_id, maker_name)
);

CREATE INDEX IF NOT EXISTS idx_food_maker_user ON food_maker(regist_user_id);

CREATE TABLE IF NOT EXISTS food (
  food_id        integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  food_maker_id  integer NOT NULL REFERENCES food_maker(food_maker_id) ON DELETE CASCADE,
  food_name      varchar(50) NOT NULL,
  regist_user_id varchar(36) NOT NULL REFERENCES users(user_id),
  UNIQUE (regist_user_id, food_maker_id, food_name)
);

CREATE INDEX IF NOT EXISTS idx_food_user_maker ON food(regist_user_id, food_maker_id);

CREATE TABLE IF NOT EXISTS flavor (
  flavor_id      integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  flavor_name    varchar(20) NOT NULL,
  food_id        integer NOT NULL REFERENCES food(food_id) ON DELETE CASCADE,
  calorie        integer NOT NULL,
  protein        numeric(10,1),
  lipid          numeric(10,1),
  carbo          numeric(10,1),
  salt           numeric(10,1),
  regist_user_id varchar(36) NOT NULL REFERENCES users(user_id),
  UNIQUE (regist_user_id, food_id, flavor_name)
);

CREATE INDEX IF NOT EXISTS idx_flavor_food ON flavor(food_id);
CREATE INDEX IF NOT EXISTS idx_flavor_user ON flavor(regist_user_id);

CREATE TABLE IF NOT EXISTS intake (
  intake_id      integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  regist_user_id  varchar(36) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  flavor_id       integer NOT NULL REFERENCES flavor(flavor_id) ON DELETE CASCADE,
  eaten_date      date NOT NULL,
  eaten_time      time NOT NULL,
  qty             integer NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_intake_user_time ON intake(regist_user_id, eaten_date, eaten_time);
CREATE INDEX IF NOT EXISTS idx_intake_flavor ON intake(flavor_id);
