package ecommerce.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService {

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
						 IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal) {
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;

		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId) {
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel()) {
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho);

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado()) {
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso()) {
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho) {
		if (carrinho == null || carrinho.getItens().isEmpty()) {
			throw new IllegalArgumentException("Carrinho de compras vazio ou nulo.");
		}

		// Calcula o custo total dos itens no carrinho
		BigDecimal totalItens = carrinho.getItens().stream()
				.map(item -> item.getProduto().getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Aplica desconto nos itens, se aplicável
		if (totalItens.compareTo(BigDecimal.valueOf(500)) > 0 && totalItens.compareTo(BigDecimal.valueOf(1000)) <= 0) {
			totalItens = totalItens.multiply(BigDecimal.valueOf(0.9)); // 10% de desconto
		} else if (totalItens.compareTo(BigDecimal.valueOf(1000)) > 0) {
			totalItens = totalItens.multiply(BigDecimal.valueOf(0.8)); // 20% de desconto
		}

		// Calcula o peso total dos itens
		BigDecimal pesoTotal = carrinho.getItens().stream()
				.map(item -> BigDecimal.valueOf(item.getProduto().getPeso()) // Converte o peso para BigDecimal
						.multiply(BigDecimal.valueOf(item.getQuantidade()))) // Multiplica pelo número de itens
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Calcula o custo do frete com base no peso total
		BigDecimal frete;
		if (pesoTotal.compareTo(BigDecimal.valueOf(5)) <= 0) {
			frete = BigDecimal.ZERO;
		} else if (pesoTotal.compareTo(BigDecimal.valueOf(10)) <= 0) {
			frete = pesoTotal.multiply(BigDecimal.valueOf(2));
		} else if (pesoTotal.compareTo(BigDecimal.valueOf(50)) <= 0) {
			frete = pesoTotal.multiply(BigDecimal.valueOf(4));
		} else {
			frete = pesoTotal.multiply(BigDecimal.valueOf(7));
		}

		// Aplica descontos no frete com base no tipo de cliente
		TipoCliente tipoCliente = carrinho.getCliente().getTipo();
		if (tipoCliente == TipoCliente.OURO) {
			frete = BigDecimal.ZERO;
		} else if (tipoCliente == TipoCliente.PRATA) {
			frete = frete.multiply(BigDecimal.valueOf(0.5)); // 50% de desconto
		}

		// Soma o custo dos itens com o frete
		return totalItens.add(frete);
	}
}
