import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;


// Interface para definir métodos de reserva
interface Reservavel {
    boolean reservar(Passageiro passageiro);
    boolean cancelarReserva(Passageiro passageiro);
}

// Classe abstrata representando um veículo
abstract class Veiculo {
    public abstract void decolar();
    public abstract void pousar();
}

// Classe concreta Aviao estende Veiculo
class Aviao extends Veiculo {
    @Override
    public void decolar() {
        System.out.println("Avião decolando...");
    }

    @Override
    public void pousar() {
        System.out.println("Avião pousando...");
    }
}

// Classe Passageiro, agora serializável
class Passageiro implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nome;
    private String CPF;
    private static int proximoId = 1;
    private int id;

    public Passageiro(String nome, String CPF) {
        this.nome = nome;
        this.CPF = CPF;
        this.id = proximoId++;
    }

    public String getNome() {
        return nome;
    }

    public String getCPF() {
        return CPF;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return nome + " (CPF: " + CPF + ")";
    }
}

// Passageiro com necessidades especiais
class PassageiroEspecial extends Passageiro {
    private String necessidadeEspecial;

    public PassageiroEspecial(String nome, String CPF, String necessidadeEspecial) {
        super(nome, CPF);
        this.necessidadeEspecial = necessidadeEspecial;
    }

    public String getNecessidadeEspecial() {
        return necessidadeEspecial;
    }

    @Override
    public String toString() {
        return super.toString() + " - Necessidade: " + necessidadeEspecial;
    }
}

// Classe Voo com serialização e lógica de reserva
class Voo implements Reservavel, Serializable {
    private static final long serialVersionUID = 1L;

    private Aviao aviao;
    public int numero;
    public String origem;
    public String destino;
    public LocalDate data;
    private static final int lotacaoMaxima = 150;
    private int lotacaoAtual = 0;

    private List<Passageiro> passageiros = new ArrayList<>();
    private Set<String> cpfsRegistrados = new HashSet<>();
    private Map<String, Passageiro> mapaPassageiros = new HashMap<>();

    public Voo(Aviao aviao, int numero, String origem, String destino, LocalDate data) {
        this.aviao = aviao;
        this.numero = numero;
        this.origem = origem;
        this.destino = destino;
        this.data = data;
        carregarPassageiros();
    }

    @Override
    public boolean reservar(Passageiro passageiro) {
        if (lotacaoAtual < lotacaoMaxima && !cpfsRegistrados.contains(passageiro.getCPF())) {
            passageiros.add(passageiro);
            cpfsRegistrados.add(passageiro.getCPF());
            mapaPassageiros.put(passageiro.getCPF(), passageiro);
            lotacaoAtual++;
            salvarPassageiros();
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelarReserva(Passageiro passageiro) {
        if (passageiros.remove(passageiro)) {
            cpfsRegistrados.remove(passageiro.getCPF());
            mapaPassageiros.remove(passageiro.getCPF());
            lotacaoAtual--;
            salvarPassageiros();
            return true;
        }
        return false;
    }

    public List<Passageiro> getPassageiros() {
        return passageiros;
    }

    public int getLugaresRestantes() {
        return lotacaoMaxima - lotacaoAtual;
    }

    private void salvarPassageiros() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("passageiros.dat"))) {
            out.writeObject(passageiros);
        } catch (IOException e) {
            System.err.println("Erro ao salvar passageiros: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void carregarPassageiros() {
        File arquivo = new File("passageiros.dat");
        if (arquivo.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(arquivo))) {
                passageiros = (List<Passageiro>) in.readObject();
                for (Passageiro p : passageiros) {
                    cpfsRegistrados.add(p.getCPF());
                    mapaPassageiros.put(p.getCPF(), p);
                }
                lotacaoAtual = passageiros.size();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro ao carregar passageiros: " + e.getMessage());
            }
        }
    }
}

// Interface gráfica principal
public class Viagem {
    private JFrame frame;
    private JTable tabelaPassageiros;
    private DefaultTableModel modeloTabela;
    private Voo voo;

    public Viagem() {
        Aviao aviao = new Aviao();
        voo = new Voo(aviao, 1, "São Paulo", "Rio de Janeiro", LocalDate.of(2024, 5, 25));
        criarInterface();
    }

    private void criarInterface() {
        frame = new JFrame("✈ Sistema de Reservas de Voo ✈");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Color azulClaro = new Color(173, 216, 230);
        Color cinzaClaro = new Color(245, 245, 245);
        Color azulBotao = new Color(100, 149, 237);

        JPanel painelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelSuperior.setBackground(azulClaro);
        painelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        painelSuperior.add(new JLabel("Voo nº: " + voo.numero));
        painelSuperior.add(new JLabel(" | Origem: " + voo.origem));
        painelSuperior.add(new JLabel(" | Destino: " + voo.destino));
        painelSuperior.add(new JLabel(" | Data: " + voo.data));

        modeloTabela = new DefaultTableModel();
        modeloTabela.addColumn("Nome");
        modeloTabela.addColumn("CPF");
        tabelaPassageiros = new JTable(modeloTabela);
        tabelaPassageiros.setBackground(Color.WHITE);
        tabelaPassageiros.setGridColor(Color.LIGHT_GRAY);

        for (Passageiro p : voo.getPassageiros()) {
            modeloTabela.addRow(new Object[]{p.getNome(), p.getCPF()});
        }

        JPanel painelInferior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelInferior.setBackground(cinzaClaro);
        painelInferior.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel labelNome = new JLabel("Nome:");
        JTextField campoNome = new JTextField(15);
        JLabel labelCPF = new JLabel("CPF:");
        JTextField campoCPF = new JTextField(12);

        JButton botaoCadastrar = new JButton("Fazer Reserva");
        botaoCadastrar.setBackground(azulBotao);
        botaoCadastrar.setForeground(Color.WHITE);
        botaoCadastrar.setFocusPainted(false);

        botaoCadastrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String nome = campoNome.getText().trim();
                    String cpf = campoCPF.getText().trim();

                    if (nome.isEmpty() || cpf.isEmpty()) {
                        throw new IllegalArgumentException("Por favor, preencha todos os campos!");
                    }

                    if (!cpf.matches("\\d{11}")) {
                        throw new IllegalArgumentException("CPF inválido! Digite 11 números.");
                    }

                    Passageiro passageiro = new Passageiro(nome, cpf);

                    if (voo.reservar(passageiro)) {
                        modeloTabela.addRow(new Object[]{passageiro.getNome(), passageiro.getCPF()});
                        JOptionPane.showMessageDialog(frame,
                                "Reserva confirmada para " + passageiro.getNome() + ".\n" +
                                        "Lugares restantes: " + voo.getLugaresRestantes());
                    } else {
                        JOptionPane.showMessageDialog(frame, "Desculpe, este CPF já está registrado ou voo lotado.");
                    }

                    campoNome.setText("");
                    campoCPF.setText("");
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(frame, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Erro inesperado: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        painelInferior.add(labelNome);
        painelInferior.add(campoNome);
        painelInferior.add(labelCPF);
        painelInferior.add(campoCPF);
        painelInferior.add(botaoCadastrar);

        frame.add(painelSuperior, BorderLayout.NORTH);
        frame.add(new JScrollPane(tabelaPassageiros), BorderLayout.CENTER);
        frame.add(painelInferior, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Viagem());
    }
}