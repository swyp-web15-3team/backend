CREATE TABLE collection_whiskies (
    collection_id BIGINT NOT NULL,
    whisky_id BIGINT NOT NULL,
    PRIMARY KEY (collection_id, whisky_id),
    CONSTRAINT fk_collection_whiskies_collection FOREIGN KEY (collection_id)
        REFERENCES collections(id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_whiskies_whisky FOREIGN KEY (whisky_id)
        REFERENCES whiskies(id) ON DELETE CASCADE
);

CREATE INDEX ix_collection_whiskies_whisky_id ON collection_whiskies(whisky_id);
