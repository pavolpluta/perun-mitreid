CREATE TABLE acrs (
    id BIGINT AUTO_INCREMENT,
    client_id VARCHAR(2048) NOT NULL,
    sub VARCHAR(2048) NOT NULL,
    acr_values VARCHAR(2048) NOT NULL,
    state VARCHAR(2048) NOT NULL,
    shib_authn_context_class VARCHAR(2048) NOT NULL,
    expiration DATETIME NOT NULL,
    PRIMARY KEY (id)
);