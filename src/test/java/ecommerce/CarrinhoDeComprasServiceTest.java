package ecommerce;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.repository.CarrinhoDeComprasRepository;
import ecommerce.service.CarrinhoDeComprasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarrinhoDeComprasServiceTest {

    @InjectMocks
    private CarrinhoDeComprasService carrinhoService;

    @Mock
    private CarrinhoDeComprasRepository carrinhoRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void buscarPorCarrinhoIdEClienteId_CarrinhoExistente() {
        // Arrange
        Long carrinhoId = 1L;
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", null);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(carrinhoId, cliente , null , null);

        when(carrinhoRepository.findByIdAndCliente(carrinhoId, cliente)).thenReturn(Optional.of(carrinho));

        // Act
        CarrinhoDeCompras resultado = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

        // Assert
        assertNotNull(resultado);
        assertEquals(carrinhoId, resultado.getId());
        assertEquals(cliente, resultado.getCliente());
        verify(carrinhoRepository, times(1)).findByIdAndCliente(carrinhoId, cliente);
    }

    @Test
    void buscarPorCarrinhoIdEClienteId_CarrinhoNaoExistente() {
        // Arrange
        Long carrinhoId = 1L;
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", null);

        when(carrinhoRepository.findByIdAndCliente(carrinhoId, cliente)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);
        });

        assertEquals("Carrinho não encontrado.", exception.getMessage());
        verify(carrinhoRepository, times(1)).findByIdAndCliente(carrinhoId, cliente);
    }
}
