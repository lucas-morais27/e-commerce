package ecommerce;

import ecommerce.controller.CompraController;
import ecommerce.dto.CompraDTO;
import ecommerce.entity.Cliente;
import ecommerce.entity.TipoCliente;
import ecommerce.service.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ControllerTest {

    @Mock
    private CompraService compraService;

    @InjectMocks
    private CompraController compraController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void finalizarCompraSucesso() {
        Long clienteId = 1L;
        Long carrinhoId = 1L;
        CompraDTO compraDTO = new CompraDTO(true, 12345L, "Compra finalizada com sucesso.");

        when(compraService.finalizarCompra(carrinhoId, clienteId)).thenReturn(compraDTO);

        ResponseEntity<CompraDTO> response = compraController.finalizarCompra(carrinhoId, clienteId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(compraDTO, response.getBody());

        verify(compraService, times(1)).finalizarCompra(carrinhoId, clienteId);
    }

    @Test
    void finalizarCompraErroEstoque() {
        Long clienteId = 1L;
        Long carrinhoId = 1L;

        when(compraService.finalizarCompra(carrinhoId, clienteId))
                .thenThrow(new IllegalStateException("Itens fora de estoque."));

        ResponseEntity<CompraDTO> response = compraController.finalizarCompra(carrinhoId, clienteId);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Itens fora de estoque.", Objects.requireNonNull(response.getBody()).mensagem() );

        verify(compraService, times(1)).finalizarCompra(carrinhoId, clienteId);
    }

    @Test
    void finalizarCompraErroPagamento() {
        Long clienteId = 1L;
        Long carrinhoId = 1L;

        when(compraService.finalizarCompra(carrinhoId, clienteId))
                .thenThrow(new IllegalStateException("Pagamento não autorizado."));

        ResponseEntity<CompraDTO> response = compraController.finalizarCompra(carrinhoId, clienteId);

        assertEquals(HttpStatus.CONFLICT , response.getStatusCode());
        assertEquals("Pagamento não autorizado.", Objects.requireNonNull(response.getBody()).mensagem());

        verify(compraService, times(1)).finalizarCompra(carrinhoId, clienteId);
    }

    @Test
    void finalizarCompraParametrosInvalidos() {

        ResponseEntity<CompraDTO> response = compraController.finalizarCompra(null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Parâmetros inválidos.", Objects.requireNonNull(response.getBody()).mensagem());

        verify(compraService, never()).finalizarCompra(anyLong(), anyLong());
    }
}
