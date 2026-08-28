CREATE TABLE agenda (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_agenda_title_not_blank CHECK (LENGTH(TRIM(title)) > 0)
);

CREATE TABLE voting_session (
    id UUID PRIMARY KEY,
    agenda_id UUID NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closes_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_voting_session_agenda
        FOREIGN KEY (agenda_id) REFERENCES agenda (id),
    CONSTRAINT uk_voting_session_agenda UNIQUE (agenda_id),
    CONSTRAINT ck_voting_session_period CHECK (closes_at > opened_at)
);

CREATE TABLE vote (
    id UUID PRIMARY KEY,
    voting_session_id UUID NOT NULL,
    associate_id VARCHAR(100) NOT NULL,
    vote_option VARCHAR(3) NOT NULL,
    cast_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_vote_voting_session
        FOREIGN KEY (voting_session_id) REFERENCES voting_session (id),
    CONSTRAINT uk_vote_session_associate UNIQUE (voting_session_id, associate_id),
    CONSTRAINT ck_vote_option CHECK (vote_option IN ('YES', 'NO')),
    CONSTRAINT ck_vote_associate_not_blank CHECK (LENGTH(TRIM(associate_id)) > 0)
);

CREATE INDEX idx_vote_session_option ON vote (voting_session_id, vote_option);

