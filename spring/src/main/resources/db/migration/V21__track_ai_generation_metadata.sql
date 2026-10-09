-- Legacy results have no known prompt or generation configuration.
ALTER TABLE record_analysis
    ADD COLUMN prompt_version varchar(100),
    ADD COLUMN schema_version varchar(100),
    ADD COLUMN generation_model varchar(200),
    ADD COLUMN max_output_tokens integer,
    ADD COLUMN thinking_level varchar(20);

ALTER TABLE insights
    ADD COLUMN prompt_version varchar(100),
    ADD COLUMN schema_version varchar(100),
    ADD COLUMN generation_model varchar(200),
    ADD COLUMN max_output_tokens integer,
    ADD COLUMN thinking_level varchar(20);
