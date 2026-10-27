IF OBJECT_ID('dbo.financas', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.financas (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        taxa FLOAT NOT NULL,
        emissor VARCHAR(255) NOT NULL,
        risco VARCHAR(255) NULL,
        vencimento VARCHAR(255) NOT NULL,
        quantidade INT NOT NULL
    );
END;

IF OBJECT_ID('dbo.futebois', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.futebois (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        ano INT NOT NULL,
        capeao VARCHAR(255) NOT NULL,
        sede VARCHAR(255) NULL,
        vice VARCHAR(255) NOT NULL,
        melhor_jogador VARCHAR(255) NOT NULL
    );
END;
