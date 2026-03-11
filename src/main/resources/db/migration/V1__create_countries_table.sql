CREATE TABLE countries (
    id              BIGSERIAL PRIMARY KEY,
    cca3            VARCHAR(3)   NOT NULL UNIQUE,
    common_name     VARCHAR(255) NOT NULL,
    official_name   VARCHAR(255),
    capital         VARCHAR(255),
    region          VARCHAR(100),
    subregion       VARCHAR(100),
    population      BIGINT       NOT NULL DEFAULT 0,
    area            DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    latitude        DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    longitude       DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    capital_lat     DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    capital_lng     DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    flag_url        VARCHAR(500),
    flag_svg        VARCHAR(500)
);
