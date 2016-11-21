package br.ufsm.csi.redes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


/**
 * 
 * User: Rafael
 * Date: 13/10/14
 * Time: 10:28
 * 
 *  OK fazer interface 
 *  OK cadastrar usuário na lista de on-line (1 thread separada)
 *  OK retirar usuário da lista da on-line por timeout (2 thread separada)
 *  - validação da lista de on-line e janelas que podem ser abertas
 *  - quando clicar no usuário on-line estabeler conexão TCP p/ chat (3 thread separada)
 *          - fechar janela
 *          - envio de mensagens (mesma thread)
 *          - recebimento de mensagens (thread 3)
 *  OK quando recebe nova conexão, abre janela (thread separada)
 * 
 *  
 */

public class ChatClientSwing extends JFrame {

    private Usuario meuUsuario;
    private final String endBroadcast = "255.255.255.255";
    private JList listaChat;
    private DefaultListModel dfListModel;
    private JTabbedPane tabbedPane = new JTabbedPane();
    private Set<Usuario> chatsAbertos = new HashSet<>();
    
    private class ThreadRecebeSonda implements Runnable {
        @Override
        public void run() {
            try {
                DatagramSocket ds = new DatagramSocket(5555);

                while(true){
                    byte[] buf = new byte[1024];
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    
                    try {
                        ds.receive(dp);
                        
                        String sonda = new String(buf, 0, dp.getLength());
                        String nome = sonda.substring(sonda.indexOf("=") + 1, sonda.indexOf(","));
                        String status = sonda.substring(sonda.indexOf(",") + 1, sonda.length());
                        
                        Usuario usuario = buscarUsuario(dp.getAddress());
                        
                        if((dp.getAddress().getHostAddress()).equals(meuUsuario.getEndereco().getHostAddress())) {  // se a sonda é minha
                            // ignora e não vai adicionar na lista de usuários
                             
                        } else if(usuario == null) {  // se é um novo usuário    
                            // adiciona na lista de novos usuários
 
                            Usuario user = new Usuario(nome, StatusUsuario.valueOf(status), dp.getAddress());
                            user.setUltimaSonda(System.currentTimeMillis());
                            dfListModel.addElement(user);
                            
                            System.out.println("\n [NOVO USUÁRIO] " +dp.getAddress().getHostAddress()
                                + " | Usuário: "+ nome +"."+ status 
                                + " | Entrada: " +new SimpleDateFormat("dd MMM, HH:mm:ss").format(new Date(user.getUltimaSonda())));
                        } else {  // usuário já estava no chat
                            // atualiza o status e a última sonda recebida
                            
                            usuario.setUltimaSonda(System.currentTimeMillis());
                            usuario.setStatus(StatusUsuario.valueOf(status));
                            dfListModel.setElementAt(usuario, dfListModel.indexOf(usuario));
                                
                            System.out.println("\n [SONDA RECEBIDA] " +dp.getAddress().getHostAddress()
                                + " | Usuário: "+ nome +"."+ status +" "
                                + " | Nova Sonda: " +new SimpleDateFormat("dd MMM, HH:mm:ss").format(new Date(usuario.getUltimaSonda())));
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (SocketException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private class ThreadEnviaSonda implements Runnable {
        @Override
        public void run() {
            try {
                DatagramSocket ds = new DatagramSocket();
     
                while(true){
                    try {
                        String sonda = "OLA "
                            + "usuário=" +meuUsuario.getNome() +"," +meuUsuario.getStatus().toString();

                        byte[] buf = sonda.getBytes();
                        DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(endBroadcast), 5555);
                        ds.send(dp);
                        Thread.sleep((long) 5000); 
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private Usuario buscarUsuario(InetAddress address) {
            Enumeration e = dfListModel.elements();
            while(e.hasMoreElements()){
                Usuario usuario = (Usuario) e.nextElement();
                if(usuario.getEndereco().equals(address)) {
                    return usuario;
                }
            }
            
            return null;
    }

    private class ThreadAtualizarListaUsuarios implements Runnable {
        @Override
        public void run() {
            while(true){
                try {
                    Enumeration e = dfListModel.elements();
                    while(e.hasMoreElements()){
                        Usuario usuario = (Usuario) e.nextElement();
                        long timeOut = System.currentTimeMillis() - usuario.getUltimaSonda();

                        if(timeOut > 30000) {
                            dfListModel.removeElement(usuario);
                            
                            System.out.println("\n [USUÁRIO " +usuario.getNome()+ " SAIU] " +usuario.getEndereco()
                                + " | Atual: " +new SimpleDateFormat("dd MMM, HH:mm:ss").format(new Date(System.currentTimeMillis()))
                                + " | Última Sonda: " +new SimpleDateFormat("dd MMM, HH:mm:ss").format(new Date(usuario.getUltimaSonda())));
                        }
                    }
                    
                    Thread.sleep((long) 5000); 
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } 
    }
    
    private void iniciaTarefasBackground() {
        new Thread(new ThreadEnviaSonda()).start();
        new Thread(new ThreadRecebeSonda()).start();
        new Thread(new ThreadAtualizarListaUsuarios()).start();
        new Thread(new ThreadAguardaConexao()).start();
    }
    
    public ChatClientSwing() throws UnknownHostException {
        setLayout(new GridBagLayout());
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Status");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.DISPONIVEL.name());
        rbMenuItem.setSelected(true);
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.DISPONIVEL);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.NAO_PERTURBE.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.NAO_PERTURBE);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.VOLTO_LOGO.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.VOLTO_LOGO);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popupMenu =  new JPopupMenu();
                    final int tab = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                    JMenuItem item = new JMenuItem("Fechar");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                PainelChatPVT painel = (PainelChatPVT) tabbedPane.getComponent(tab);
                                tabbedPane.remove(tab);
                                //tabbedPane.setEnabled(false);
                                chatsAbertos.remove(painel.getUsuario());
                                
                                painel.getUsuario().getSocket().shutdownInput();
                                painel.getUsuario().getSocket().shutdownOutput();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    popupMenu.add(item);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        add(new JScrollPane(criaLista()), new GridBagConstraints(0, 0, 1, 1, 0.1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        setSize(800, 600);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - this.getWidth()) / 2;
        final int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Chat P2P - Redes de Computadores");
        String nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuário: ");
        this.meuUsuario = new Usuario(nomeUsuario, StatusUsuario.DISPONIVEL, InetAddress.getLocalHost());
        setVisible(true);
        
        iniciaTarefasBackground();
    }
    
    private class ThreadAguardaConexao implements Runnable {
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(5556);
                
                while(true) {
                    Socket socket = serverSocket.accept();
                    
                    Usuario usuario = buscarUsuario(socket.getInetAddress());
                    if(usuario != null) {
                        usuario.setSocket(socket);
                        tabbedPane.add(usuario.getNome(), new PainelChatPVT(usuario));
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
    }
    
    private class ThreadIniciarConexao implements Runnable {
        Usuario usuario;
        
        public ThreadIniciarConexao(Usuario usuario) {
            this.usuario = usuario;
        }
        
        @Override
        public void run() {
            try {
                Socket socket = new Socket(usuario.getEndereco(), 5556);
                usuario.setSocket(socket);
                
                tabbedPane.add(usuario.getNome(), new PainelChatPVT(usuario));
                /*
                while(!socket.isClosed()) { 
                    if(!tabbedPane.isEnabled()){
                        socket.close();
                    }
                } 
                */
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(new JPanel(), "Opa! Conexão com " +usuario.getNome()+ " não estabelecida.");
            }              
        }
    }
    
    private class ThreadResposta implements Runnable {
        private Socket socket;
        private PainelChatPVT chatPVT;
        
        public ThreadResposta(PainelChatPVT chatPVT, Socket socket) {
            this.socket = socket;
            this.chatPVT = chatPVT;
        }
        
        @Override
        public void run() {
            try {
                while(socket.isConnected()) {
                    byte[] bytes = new byte[1024];
                    
                    String mensagem = new String(bytes, 0, socket.getInputStream().read(bytes), "UTF-8");
                    
                    chatPVT.areaChat.append(" "
                        +chatPVT.usuario.getNome() + "  >   " +mensagem+ " \n");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }              
        }
    }
    
    private JComponent criaLista() {
        dfListModel = new DefaultListModel();
        listaChat = new JList(dfListModel);
        
        listaChat.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    Usuario user = (Usuario) list.getModel().getElementAt(index);
                    
                    if (chatsAbertos.add(user)) {
                        new Thread(new ThreadIniciarConexao(user)).start();
                    } else {
                        JOptionPane.showMessageDialog(new JPanel(), "Opa! Esta janela já esta aberta.");
                    }
                }
            }
        });
        return listaChat;
    }
    
    class PainelChatPVT extends JPanel {

        JTextArea areaChat;
        JTextField campoEntrada;
        Usuario usuario;
        Socket socket;

        PainelChatPVT(Usuario usuario) {
            setLayout(new GridBagLayout());
            areaChat = new JTextArea();
            this.usuario = usuario;
            this.socket = usuario.getSocket();
            areaChat.setEditable(false);
            campoEntrada = new JTextField();
            campoEntrada.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        socket.getOutputStream().write(e.getActionCommand().getBytes());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    
                    ((JTextField) e.getSource()).setText("");
                    areaChat.append(" " +meuUsuario.getNome() + "  >   " + e.getActionCommand() + "\n");
                }
            });
            add(new JScrollPane(areaChat), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(campoEntrada, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            
            new Thread(new ThreadResposta(this, usuario.getSocket())).start();
        }

        public Usuario getUsuario() {
            return usuario;
        }

        public void setUsuario(Usuario usuario) {
            this.usuario = usuario;
        }

    }

    public static void main(String[] args) throws UnknownHostException {
        new ChatClientSwing();

    }

    public enum StatusUsuario {
        DISPONIVEL, NAO_PERTURBE, VOLTO_LOGO
    }

}
