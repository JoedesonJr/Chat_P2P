
package br.ufsm.csi.redes;

import java.net.InetAddress;
import java.net.Socket;

public class Usuario {
        private String nome;
        private ChatClientSwing.StatusUsuario status;
        private InetAddress endereco;
        private long ultimaSonda;
        private Socket socket;

        public Usuario(String nome, ChatClientSwing.StatusUsuario status, InetAddress endereco) {
            this.nome = nome;
            this.status = status;
            this.endereco = endereco;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public ChatClientSwing.StatusUsuario getStatus() {
            return status;
        }

        public void setStatus(ChatClientSwing.StatusUsuario status) {
            this.status = status;
        }

        public InetAddress getEndereco() {
            return endereco;
        }

        public void setEndereco(InetAddress endereco) {
            this.endereco = endereco;
        }
        
                public Socket getSocket() {
            return socket;
        }

        public void setSocket(Socket socket) {
            this.socket = socket;
        }

        public long getUltimaSonda() {
            return ultimaSonda;
        }

        public void setUltimaSonda(long ultimaSonda) {
            this.ultimaSonda = ultimaSonda;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Usuario usuario = (Usuario) o;

            return nome.equals(usuario.nome);

        }

        @Override
        public int hashCode() {
            return nome.hashCode();
        }

        public String toString() {
            return this.getNome() + " (" + getStatus().toString() + ")";
        }
}
