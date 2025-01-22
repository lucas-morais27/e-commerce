package ecommerce;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import ecommerce.service.CarrinhoDeComprasService;
import ecommerce.service.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.Produto;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.TipoCliente;

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

}
