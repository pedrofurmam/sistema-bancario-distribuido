package modelo;

public class Usuario {
    private String nome;
    private String cpf;
    private String senha;
    private double saldo;




    // CONSTRUTOR 1: Para usuários NOVOS (saldo = 0.00)
    public Usuario(String nome, String cpf, String senha) {
        this.nome = nome;
        this.cpf = cpf;
        this.senha = senha;
        this.saldo = 0.00; //  Saldo inicial ZERO
    }

    // CONSTRUTOR 2: Para buscar dados do BANCO (saldo vem do ResultSet)
    public Usuario(String nome, String cpf, String senha, double saldo) {
        this.nome = nome;
        this.cpf = cpf;
        this.senha = senha;
        this.saldo = saldo; // Saldo vem do banco
    }

    // Getters e Setters
    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }
    public String getCpf() {
        return cpf;
    }
    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
    public String getSenha() {
        return senha;
    }
    public void setSenha(String senha) {
        this.senha = senha;
    }
    public double getSaldo() {
        return saldo;
    }
    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }
}