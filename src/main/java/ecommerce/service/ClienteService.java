package ecommerce.service;

import ecommerce.entity.TipoCliente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.entity.Cliente;
import ecommerce.repository.ClienteRepository;

@Service
public class ClienteService {
	
	private final ClienteRepository repository;
	
	@Autowired
	public ClienteService(ClienteRepository repository) {
		this.repository = repository;
	}

	public Cliente buscarPorId(Long clienteId) {
		return repository.findById(clienteId).orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
	}


	public Cliente criarCliente (Cliente cliente) {
		return repository.save(cliente);
	}

	public TipoCliente verificarTipo(Cliente cliente) {
	 return cliente.getTipo();
	}



}
