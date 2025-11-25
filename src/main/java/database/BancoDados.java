package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class BancoDados {
    private static final String DB_URL = "jdbc:sqlite:sistema_bancario.db";

    public static Connection getConnection() throws Exception {
        Connection conn = DriverManager.getConnection(DB_URL);
        // Habilitar foreign keys no SQLite
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public static void criarTabelas() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Habilitar foreign keys
            stmt.execute("PRAGMA foreign_keys = ON");

            // Usar CREATE TABLE IF NOT EXISTS para não apagar dados existentes
            String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (" +
                    "cpf TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL, " +
                    "senha TEXT NOT NULL, " +
                    "saldo REAL DEFAULT 0.00" +
                    ")";

            String sqlTransacoes = "CREATE TABLE IF NOT EXISTS transacoes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "valor_enviado REAL NOT NULL, " +
                    "cpf_enviador TEXT NOT NULL, " +
                    "cpf_recebedor TEXT NOT NULL, " +
                    "criado_em TEXT NOT NULL, " +
                    "atualizado_em TEXT NOT NULL, " +
                    "FOREIGN KEY(cpf_enviador) REFERENCES usuarios(cpf) ON DELETE CASCADE, " +
                    "FOREIGN KEY(cpf_recebedor) REFERENCES usuarios(cpf) ON DELETE CASCADE" +
                    ")";

            stmt.execute(sqlUsuarios);
            stmt.execute(sqlTransacoes);
            System.out.println("Tabelas verificadas/criadas com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro ao criar tabela: " + e.getMessage());
        }
    }
}