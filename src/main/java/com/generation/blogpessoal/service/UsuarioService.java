package com.generation.blogpessoal.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.generation.blogpessoal.model.UsuarioLogin;
import com.generation.blogpessoal.model.Usuario;
import com.generation.blogpessoal.repository.UsuarioRepository;
import com.generation.blogpessoal.security.JwtService;
/*
 * A Classe UsuarioService é responsável por manipular as regras de negócio de usuário no sistema, definindo lógicas para cadastro e atualização de usuário
 */
@Service
public class UsuarioService {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

	public Optional<Usuario> cadastrarUsuario(Usuario usuario) {

		if (usuarioRepository.findByUsuario(usuario.getUsuario()).isPresent())
			return Optional.empty();

		usuario.setSenha(criptografarSenha(usuario.getSenha()));

		return Optional.of(usuarioRepository.save(usuario));
	}

	public Optional<Usuario> atualizarUsuario(Usuario usuario) {
		
		if(usuarioRepository.findById(usuario.getId()).isPresent()) { 

			Optional<Usuario> buscaUsuario = usuarioRepository.findByUsuario(usuario.getUsuario()); //Verifica se existe um usuário com o Id informado

			if ( (buscaUsuario.isPresent()) && ( buscaUsuario.get().getId() != usuario.getId())) //Verifica se quem está tentando atualizar tem o e-mail diferente do Id informado
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!", null); //Se o Id for diferente, ou seja, um usuário tentando atualizar cadastro com um e-mail já existente, será lançada uma exceção

			usuario.setSenha(criptografarSenha(usuario.getSenha())); //Se o Id for igual, senha será criptografada novamente e o usuário será salvo

			return Optional.ofNullable(usuarioRepository.save(usuario)); //Usuario salvo
		}

		return Optional.empty();	
	}	

	public Optional<UsuarioLogin> autenticarUsuario(Optional<UsuarioLogin> usuarioLogin) {
        
        // Gera o Objeto de autenticação
		var credenciais = new UsernamePasswordAuthenticationToken(usuarioLogin.get().getUsuario(), usuarioLogin.get().getSenha());
		
        // Autentica o Usuario com base nas credenciais
		Authentication authentication = authenticationManager.authenticate(credenciais);
        
        // Se a autenticação foi efetuada com sucesso
		if (authentication.isAuthenticated()) {

            // Busca os dados do usuário do banco de dados
			Optional<Usuario> usuario = usuarioRepository.findByUsuario(usuarioLogin.get().getUsuario());

            // Se o usuário foi encontrado
			if (usuario.isPresent()) {

               // Preenche o Objeto usuarioLogin com os dados encontrados 
			   usuarioLogin.get().setId(usuario.get().getId());
               usuarioLogin.get().setNome(usuario.get().getNome());
               usuarioLogin.get().setFoto(usuario.get().getFoto());
               usuarioLogin.get().setToken(gerarToken(usuarioLogin.get().getUsuario())); //Gera o Token
               usuarioLogin.get().setSenha("");
				
               // Retorna o Objeto preenchido
			   return usuarioLogin;
			}
        } 
            
		return Optional.empty();
    }

	private String criptografarSenha(String senha) {

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		return encoder.encode(senha);
	}

	private String gerarToken(String usuario) {
		return "Bearer " + jwtService.generateToken(usuario);
	}

}