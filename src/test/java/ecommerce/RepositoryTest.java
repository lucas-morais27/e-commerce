package ecommerce;

import ecommerce.entity.*;
import ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RepositoryTest {

        @Mock
        private ProdutoRepository produtoRepository;

        @Mock
        private ClienteRepository clienteRepository;

        @Mock
        private CarrinhoDeComprasRepository carrinhoRepository;


        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
        }

        @Test
        void salvarProduto() {
                Produto produto = new Produto(1L, "Produto Teste", "", BigDecimal.valueOf(10), 1,TipoProduto.LIVRO);

                when(produtoRepository.save(produto)).thenReturn(produto);

                Produto salvo = produtoRepository.save(produto);

                assertNotNull(salvo);
                assertEquals("Produto Teste", salvo.getNome());
                verify(produtoRepository, times(1)).save(produto);
        }

        @Test
        void buscarProdutoPorId() {
                Long produtoId = 1L;
                Produto produto = new Produto(produtoId, "Produto Teste", "", BigDecimal.valueOf(10), 1,TipoProduto.LIVRO);

                when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));

                Optional<Produto> encontrado = produtoRepository.findById(produtoId);

                assertTrue(encontrado.isPresent());
                assertEquals(produtoId, encontrado.get().getId());
                verify(produtoRepository, times(1)).findById(produtoId);
        }

        @Test
        void salvarCliente() {
                Cliente cliente = new Cliente(1L, "Cliente Teste", "", TipoCliente.OURO );

                when(clienteRepository.save(cliente)).thenReturn(cliente);

                Cliente salvo = clienteRepository.save(cliente);

                assertNotNull(salvo);
                assertEquals("Cliente Teste", salvo.getNome());
                verify(clienteRepository, times(1)).save(cliente);
        }

        @Test
        void buscarClientePorId() {
                Long clienteId = 1L;
                Cliente cliente = new Cliente(clienteId, "Cliente Teste" , "", TipoCliente.OURO);

                when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

                Optional<Cliente> encontrado = clienteRepository.findById(clienteId);

                assertTrue(encontrado.isPresent());
                assertEquals(clienteId, encontrado.get().getId());
                verify(clienteRepository, times(1)).findById(clienteId);
        }

        @Test
        void salvarCarrinho() {
                LocalDate data = LocalDate.now();
                CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L , new Cliente(1L, "Cliente Teste", "", TipoCliente.OURO), null, data);

                when(carrinhoRepository.save(carrinho)).thenReturn(carrinho);

                CarrinhoDeCompras salvo = carrinhoRepository.save(carrinho);

                assertNotNull(salvo);
                assertEquals(1L, salvo.getId());
                verify(carrinhoRepository, times(1)).save(carrinho);
        }
}
