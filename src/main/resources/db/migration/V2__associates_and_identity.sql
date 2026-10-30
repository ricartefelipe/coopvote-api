-- Identidade verificável do eleitor: o voto passa a ser vinculado ao associado autenticado,
-- nunca a um identificador enviado no corpo da requisição.
CREATE TABLE associate (
    id UUID PRIMARY KEY,
    associate_id VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    cpf VARCHAR(11),
    password_hash VARCHAR(100) NOT NULL,
    roles VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_associate_associate_id UNIQUE (associate_id),
    CONSTRAINT uk_associate_cpf UNIQUE (cpf),
    CONSTRAINT ck_associate_id_not_blank CHECK (LENGTH(TRIM(associate_id)) > 0),
    CONSTRAINT ck_associate_cpf_digits CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$')
);

-- Votos já registrados continuam válidos: o associate_id histórico é preservado como está.
-- Novos votos usam o associate_id do token; a unicidade por sessão permanece em uk_vote_session_associate.
