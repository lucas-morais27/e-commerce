package ecommerce;

import ecommerce.entity.Cliente;
import ecommerce.entity.TipoCliente;
import ecommerce.repository.ClienteRepository;
import ecommerce.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClienteServiceTest {

    @InjectMocks
    private ClienteService clienteService;

    @Mock
    private ClienteRepository clienteRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void criarNovoCliente() {
        // Arrange
        Cliente cliente = new Cliente(null, "Cliente Teste", "", TipoCliente.OURO);

        when(clienteRepository.save(cliente)).thenReturn(cliente);

        // Act
        Cliente salvo = clienteService.criarCliente(cliente);

        // Assert
        assertNotNull(salvo);
        assertEquals("Cliente Teste", salvo.getNome());
        verify(clienteRepository, times(1)).save(cliente);
    }

    @Test
    void verificarTipoCliente() {
        // Arrange
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.OURO);

        // Act
        TipoCliente tipoCliente = clienteService.verificarTipo(cliente);

        // Assert
        assertEquals(TipoCliente.OURO, tipoCliente);
    }

    @Test
    void buscarClientePorId_ClienteExistente() {
        // Arrange
        Long clienteId = 1L;
        Cliente cliente = new Cliente(clienteId, "Cliente Teste", "", TipoCliente.BRONZE);
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

        // Act
        Cliente encontrado = clienteService.buscarPorId(clienteId);

        // Assert
        assertNotNull(encontrado);
        assertEquals("Cliente Teste", encontrado.getNome());
        verify(clienteRepository, times(1)).findById(clienteId);
    }

    @Test
    void buscarClientePorId_ClienteNaoExistente() {
        // Arrange
        Long clienteId = 1L;
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            clienteService.buscarPorId(clienteId);
        });

        assertEquals("Cliente não encontrado", exception.getMessage());
        verify(clienteRepository, times(1)).findById(clienteId);
    }
}
