package ecommerce;

import ecommerce.dto.PagamentoDTO;
import ecommerce.external.fake.PagamentoSimulado;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PagamentoSimuladoTest {

    private final PagamentoSimulado pagamentoSimulado = new PagamentoSimulado();

    @Test
    void testAutorizarPagamento() {
        // Arrange
        Long clienteId = 1L;
        Double custoTotal = 100.0;

        // Act
        PagamentoDTO resultado = pagamentoSimulado.autorizarPagamento(clienteId, custoTotal);

        // Assert
        assertNull(resultado, "O método 'autorizarPagamento' deve retornar null para o PagamentoSimulado.");
    }

    @Test
    void testCancelarPagamento() {
        // Arrange
        Long clienteId = 1L;
        Long pagamentoTransacaoId = 12345L;

        // Act
        pagamentoSimulado.cancelarPagamento(clienteId, pagamentoTransacaoId);

        // Assert
        // Como o método não retorna nada, verificamos apenas se ele não lança exceções.
        assertDoesNotThrow(() -> pagamentoSimulado.cancelarPagamento(clienteId, pagamentoTransacaoId));
    }
}
