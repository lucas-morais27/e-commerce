package ecommerce;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.*;
import ecommerce.service.CarrinhoDeComprasService;
import ecommerce.service.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ecommerce.service.ClienteService;

import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

class CompraServiceTest {

    private CompraService compraService;

    @Mock
    private CarrinhoDeComprasService carrinhoService;

    @Mock
    private ClienteService clienteService;

    @Mock
    private IEstoqueExternal estoqueExternal;

    @Mock
    private IPagamentoExternal pagamentoExternal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        compraService = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);
    }


    @Test
    void testCalcularCustoTotal_CarrinhoVazio() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Collections.emptyList());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho);
        });

        assertEquals("Carrinho de compras vazio ou nulo.", exception.getMessage());
    }

    @Test
    void testCalcularCustoTotal_ClienteOuro_SemFrete() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.OURO);
        carrinho.setCliente(cliente);

        Produto produto1 = new Produto(1L, "Produto A", "Descricao A", BigDecimal.valueOf(100), 2, null);
        Produto produto2 = new Produto(2L, "Produto B", "Descricao B", BigDecimal.valueOf(150), 3, null);
        ItemCompra item1 = new ItemCompra(null, produto1, 2L);
        ItemCompra item2 = new ItemCompra(null, produto2, 1L);
        carrinho.setItens(Arrays.asList(item1, item2));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(350), custoTotal);
    }

    @Test
    void testCalcularCustoTotal_AplicarDescontoItens() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(3L, "Produto A", "Descricao C", BigDecimal.valueOf(600), 2, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(540.0), custoTotal); // 10% de desconto aplicado
    }

    @Test
    void testCalcularCustoTotal_PesoExato5kg_SemFrete() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(4L, "Produto A", "Descricao D", BigDecimal.valueOf(1200), 5, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);

        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(960.0), custoTotal); // Sem custo de frete
    }

    @Test
    void testCalcularCustoTotal_Peso10kg_ComFrete() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(5L, "Produto A", "Descricao E", BigDecimal.valueOf(100), 10, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(120), custoTotal); // Frete de 20 (10kg * 2)
    }

    @ParameterizedTest
    @CsvSource({
            // pesoTotal, tipoCliente, valorItens, freteEsperado, descontoEsperado, totalEsperado
            "4, BRONZE, 400.00, 0.00, 0.00, 400.00",  // Sem desconto, sem frete
            "6, BRONZE, 600.00, 12.00, 60.00, 552.00", // 10% de desconto nos itens, frete incluído
            "15, BRONZE, 1200.00, 60.00, 240.00, 1020.00", // 20% de desconto nos itens, frete incluído
            "55, BRONZE, 1500.00, 385.00, 300.00, 1585.00", // 20% de desconto nos itens, frete incluído
            "10, OURO, 800.00, 0.00, 80.00, 720.00", // 10% de desconto nos itens, sem frete (cliente ouro)
            "20, PRATA, 1000.00, 40.00, 100.00, 940.00" // 10% de desconto nos itens, frete com 50% de desconto
    })
    void testCalcularCustoTotal_ComDescontosEFrete(int pesoTotal, TipoCliente tipoCliente, double valorItens, double freteEsperado, double descontoEsperado, double totalEsperado) {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(tipoCliente);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(1L, "Produto Teste", "Descrição", BigDecimal.valueOf(valorItens), pesoTotal, null);
        ItemCompra item = new ItemCompra(null, produto, 1L); // Apenas 1 unidade para simplificar
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        BigDecimal valorEsperado = BigDecimal.valueOf(totalEsperado);
        assertEquals(0, custoTotal.compareTo(valorEsperado));
    }


    @Test
    void testCalcularCustoTotal_ClientePrata_ComDescontoFrete() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.PRATA);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(6L, "Produto A", "Descricao F", BigDecimal.valueOf(100), 10, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(110.0), custoTotal); // Frete com desconto de 50% (10 em vez de 20)
    }

    @Test
    void testCalcularCustoTotal_CompraAcimaDe1000_ComDesconto20() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(7L, "Produto A", "Descricao G", BigDecimal.valueOf(1200), 5, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(960.0), custoTotal); // 20% de desconto aplicado
    }

    @Test
    void testCalcularCustoTotal_UmItem_ComFrete(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(8L, "Produto A", "Descricao H", BigDecimal.valueOf(100), 10, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(120), custoTotal); // Frete de 20 (10kg * 2)
    };

    @Test
    void testCalcularCustoTotal_ValorExato500_SemDesconto(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(9L, "Produto A", "Descricao I", BigDecimal.valueOf(500), 5, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(500), custoTotal); // Sem desconto
    };

    @Test
    void testCalcularCustoTotal_ValorExato1000_SemDesconto(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(10L, "Produto A", "Descricao J", BigDecimal.valueOf(1000), 5, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(900.0), custoTotal); // Sem desconto
    };

    @Test
    void testCalcularCustoTotal_PesoExato10kg_ComFrete(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(12L, "Produto A", "Descricao L", BigDecimal.valueOf(100), 10, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(120), custoTotal); // Frete de 20 (10kg * 2)
    };

    @Test
    void testCalcularCustoTotal_CarrinhoNulo_LancarExcecao(){
        CarrinhoDeCompras carrinho = null;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho);
        });

        assertEquals("Carrinho de compras vazio ou nulo.", exception.getMessage());
    };

    @Test
    void testCalcularCustoTotal_ClienteNulo_LancarExcecao(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setCliente(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho);
        });

        assertEquals("Carrinho de compras vazio ou nulo.", exception.getMessage());
    };

    @Test
    void testCalcularCustoTotal_DescontoItensEFrete(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(13L, "Produto A", "Descricao M", BigDecimal.valueOf(600), 10, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(560.0), custoTotal); // 10% de desconto nos itens e frete de 20 (10kg * 2)
    };

    @Test
    void testCalcularCustoTotal_ClienteOuro_Peso10kg_SemFrete(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.OURO);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(14L, "Produto A", "Descricao N", BigDecimal.valueOf(100), 10, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(100), custoTotal); // Sem frete
    };

    void testCalcularCustoTotal_Exatamente5kg(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(15L, "Produto A", "Descricao O", BigDecimal.valueOf(100), 5, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(80), custoTotal); // Sem frete
    };
    void testCalcularCustoTotal_Exatamente10kg(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(16L, "Produto A", "Descricao P", BigDecimal.valueOf(100), 10, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(100), custoTotal); // Sem frete
    };
    void testCalcularCustoTotal_Exatamente50kg(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(17L, "Produto A", "Descricao Q", BigDecimal.valueOf(100), 50, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(200), custoTotal); // Sem frete
    };
    void testCalcularCustoTotal_ValorExatamente500SemDesconto(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(18L, "Produto A", "Descricao R", BigDecimal.valueOf(500), 5, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(500), custoTotal); // Sem desconto
    };
    void testCalcularCustoTotal_ValorExatamente1000ComDesconto(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);

        Produto produto = new Produto(19L, "Produto A", "Descricao S", BigDecimal.valueOf(1000), 5, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho);

        assertEquals(BigDecimal.valueOf(900.0), custoTotal); // 10% de desconto
    };
    void testFinalizarCompra_ErroPagamento(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);
        Produto produto = new Produto(20L, "Produto A", "Descricao T", BigDecimal.valueOf(100), 5, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.finalizarCompra(carrinho.getId(), cliente.getId());
        });

        assertEquals("Erro ao processar pagamento.", exception.getMessage());
    };
    void testFinalizarCompra_ErroEstoque(){
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Cliente cliente = new Cliente();
        cliente.setTipo(TipoCliente.BRONZE);
        carrinho.setCliente(cliente);
        Produto produto = new Produto(21L, "Produto A", "Descricao U", BigDecimal.valueOf(100), 5, null);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.finalizarCompra(carrinho.getId(), cliente.getId());
        });

        assertEquals("Erro ao processar estoque.", exception.getMessage());
    };


    @Test
    void finalizarCompra_Sucesso() {
        // Arrange
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.BRONZE);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente , null , null);
        Produto produto = new Produto(1L, "Produto Teste", "", BigDecimal.valueOf(100), 2, TipoProduto.LIVRO );
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        when(clienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinho.getId(), cliente)).thenReturn(carrinho);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true , null));
        when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 12345L));
        when(estoqueExternal.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(true));

        // Act
        CompraDTO compra = compraService.finalizarCompra(carrinho.getId(), cliente.getId());

        // Assert
        assertTrue(compra.sucesso());
        assertEquals("Compra finalizada com sucesso.", compra.mensagem());
        verify(clienteService, times(1)).buscarPorId(cliente.getId());
        verify(carrinhoService, times(1)).buscarPorCarrinhoIdEClienteId(carrinho.getId(), cliente);
    }

    @Test
    void finalizarCompra_FalhaNoEstoque_DeveLancarExcecao() {
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.BRONZE);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, null, null);
        Produto produto = new Produto(1L, "Produto Teste", "Descrição", BigDecimal.valueOf(100), 5, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        when(clienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinho.getId(), cliente)).thenReturn(carrinho);

        List<Long> produtosIndisponiveis = Collections.singletonList(produto.getId());
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(false, produtosIndisponiveis));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinho.getId(), cliente.getId());
        });

        assertEquals("Itens fora de estoque.", exception.getMessage());

        verify(pagamentoExternal, never()).autorizarPagamento(anyLong(), anyDouble());
        verify(pagamentoExternal, never()).cancelarPagamento(anyLong(), anyLong());
        verify(estoqueExternal, never()).darBaixa(anyList(), anyList());
    }

    @Test
    void finalizarCompra_FalhaNoPagamento_DeveLancarExcecao() {
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.BRONZE);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, null, null);
        Produto produto = new Produto(1L, "Produto Teste", "Descrição", BigDecimal.valueOf(100), 5, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        when(clienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinho.getId(), cliente)).thenReturn(carrinho);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));
        when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble()))
                .thenReturn(new PagamentoDTO(false, null)); // Pagamento não autorizado

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinho.getId(), cliente.getId());
        });

        assertEquals("Pagamento não autorizado.", exception.getMessage());
        verify(pagamentoExternal, never()).cancelarPagamento(anyLong(), anyLong());
        verify(estoqueExternal, never()).darBaixa(anyList(), anyList());
    }

    @Test
    void finalizarCompra_FalhaNaBaixaDeEstoque_DeveLancarExcecao() {
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.BRONZE);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, null, null);
        Produto produto = new Produto(1L, "Produto Teste", "Descrição", BigDecimal.valueOf(100), 5, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(null, produto, 1L);
        carrinho.setItens(Collections.singletonList(item));

        when(clienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinho.getId(), cliente)).thenReturn(carrinho);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));
        when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 12345L));
        when(estoqueExternal.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(false)); // Falha na baixa de estoque

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinho.getId(), cliente.getId());
        });

        assertEquals("Erro ao dar baixa no estoque.", exception.getMessage());
        verify(pagamentoExternal, times(1)).cancelarPagamento(cliente.getId(), 12345L);
    }

    @Test
    void finalizarCompra_ItensIndisponiveis_DeveLancarExcecao() {
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.BRONZE);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, null, null);

        Produto produto1 = new Produto(1L, "Produto A", "Descrição A", BigDecimal.valueOf(100), 5, TipoProduto.LIVRO);
        Produto produto2 = new Produto(2L, "Produto B", "Descrição B", BigDecimal.valueOf(200), 3, TipoProduto.LIVRO);
        ItemCompra item1 = new ItemCompra(null, produto1, 1L);
        ItemCompra item2 = new ItemCompra(null, produto2, 1L);

        carrinho.setItens(Arrays.asList(item1, item2));

        when(clienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinho.getId(), cliente)).thenReturn(carrinho);

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(false, Arrays.asList(produto2.getId())));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinho.getId(), cliente.getId());
        });

        assertEquals("Itens fora de estoque.", exception.getMessage());
    }

    @Test
    void finalizarCompra_ComVariosItens_DeveCalcularCorretamente() {
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.BRONZE);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, null, null);

        Produto produto1 = new Produto(1L, "Produto A", "Descrição A", BigDecimal.valueOf(100), 2, TipoProduto.LIVRO);
        Produto produto2 = new Produto(2L, "Produto B", "Descrição B", BigDecimal.valueOf(150), 3, TipoProduto.LIVRO);
        Produto produto3 = new Produto(3L, "Produto C", "Descrição C", BigDecimal.valueOf(200), 5, TipoProduto.LIVRO);
        ItemCompra item1 = new ItemCompra(null, produto1, 2L);
        ItemCompra item2 = new ItemCompra(null, produto2, 1L);
        ItemCompra item3 = new ItemCompra(null, produto3, 3L);

        carrinho.setItens(Arrays.asList(item1, item2, item3));

        when(clienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinho.getId(), cliente)).thenReturn(carrinho);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));
        when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 12345L));
        when(estoqueExternal.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(true));

        CompraDTO compra = compraService.finalizarCompra(carrinho.getId(), cliente.getId());

        assertTrue(compra.sucesso());
        assertEquals("Compra finalizada com sucesso.", compra.mensagem());
    }

    @Test
    void finalizarCompra_ProdutoSemId_DeveLancarExcecao() {
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.BRONZE);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, null, null);

        Produto produto = new Produto(null, "Produto Sem ID", "Descrição", BigDecimal.valueOf(100), 2, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(null, produto, 1L);

        carrinho.setItens(Collections.singletonList(item));

        when(clienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinho.getId(), cliente)).thenReturn(carrinho);

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenThrow(new IllegalArgumentException("Produto sem ID no carrinho.")); // Simula o erro corretamente

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.finalizarCompra(carrinho.getId(), cliente.getId());
        });

        assertEquals("Produto sem ID no carrinho.", exception.getMessage());
    }

    @Test
    void finalizarCompra_ValidaChamadaComCustoConvertidoParaDouble() {
        Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.BRONZE);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, null, null);
        Produto produto = new Produto(1L, "Produto A", "Descrição", BigDecimal.valueOf(250.75), 2, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(null, produto, 2L);
        carrinho.setItens(Collections.singletonList(item));

        when(clienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinho.getId(), cliente)).thenReturn(carrinho);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));
        when(pagamentoExternal.autorizarPagamento(anyLong(), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 12345L));
        when(estoqueExternal.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(true));

        compraService.finalizarCompra(carrinho.getId(), cliente.getId());
        verify(pagamentoExternal).autorizarPagamento(eq(1L), eq(451.35d));
    }


}
