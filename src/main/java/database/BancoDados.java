package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class BancoDados {
    private static final String DB_URL = "jdbc:sqlite:sistema_bancario.db";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }

    public static void criarTabelas() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabela de usuários simplificada
            String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (" +
                    "cpf TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL, " +
                    "senha TEXT NOT NULL, " +
                    "saldo REAL DEFAULT 0.00" +
                    ")";

            stmt.execute(sqlUsuarios);
            System.out.println("Tabela usuarios criada com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro ao criar tabela: " + e.getMessage());
        }
    }
}