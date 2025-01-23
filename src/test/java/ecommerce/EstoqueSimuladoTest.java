package ecommerce;

import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.external.fake.EstoqueSimulado;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstoqueSimuladoTest {

    private final EstoqueSimulado estoqueSimulado = new EstoqueSimulado();

    @Test
    void testDarBaixa() {
        // Arrange
        List<Long> produtosIds = Arrays.asList(1L, 2L, 3L);
        List<Long> produtosQuantidades = Arrays.asList(5L, 3L, 2L);

        // Act
        EstoqueBaixaDTO resultado = estoqueSimulado.darBaixa(produtosIds, produtosQuantidades);

        // Assert
        assertNull(resultado, "O método 'darBaixa' deve retornar null para o EstoqueSimulado.");
    }

    @Test
    void testVerificarDisponibilidade() {
        // Arrange
        List<Long> produtosIds = Arrays.asList(1L, 2L, 3L);
        List<Long> produtosQuantidades = Arrays.asList(5L, 3L, 2L);

        // Act
        DisponibilidadeDTO resultado = estoqueSimulado.verificarDisponibilidade(produtosIds, produtosQuantidades);

        // Assert
        assertNull(resultado, "O método 'verificarDisponibilidade' deve retornar null para o EstoqueSimulado.");
    }
}
