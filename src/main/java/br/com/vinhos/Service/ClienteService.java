package br.com.vinhos.Service;

import br.com.vinhos.Component.ClienteComponent;
import br.com.vinhos.Component.RequestAPI;
import br.com.vinhos.DTO.ClienteDTO;
import br.com.vinhos.DTO.HistoricoDTO;
import br.com.vinhos.Entity.Cliente;
import br.com.vinhos.Entity.Historico;
import br.com.vinhos.Entity.Item;
import br.com.vinhos.Factory.ClienteFactory;
import br.com.vinhos.Factory.HistoricoFactory;
import br.com.vinhos.Repository.ClienteRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class ClienteService {

    private ClienteRepository clienteRepository;
    private ClienteComponent clienteComponent;

    @Transactional(rollbackFor = Exception.class)
    public void popularBanco() {
        if( clienteRepository.findAll().isEmpty() ) {
            List<HistoricoDTO> historicoDTOS = RequestAPI.historico();
            List<ClienteDTO> clienteDTOS = RequestAPI.cliente();

            for (ClienteDTO clienteDTO: clienteDTOS) {
                Cliente cliente = ClienteFactory.getCliente(clienteDTO);

                List<Historico> historicosCliente = historicoDTOS.stream()
                        .filter(historicoDTO -> clienteComponent.validadorHistorico(historicoDTO, clienteDTO))
                        .map(HistoricoFactory::getHistorico)
                        .peek(historico -> historico.setItens(clienteComponent.setHistoricoNoItem(historico)) )
                        .peek(historico -> historico.setCliente(cliente))
                        .collect(toList());

                cliente.setHistoricos(historicosCliente);

                clienteRepository.save(cliente);
            }
        }
    }

    public List<Cliente> todos(){
        return clienteRepository.findAll();
    }

    public List<Cliente> ordenadoMaiorValorTotal(){
        TreeMap<Double, Cliente> mapClientes = new TreeMap<>();

        for ( Cliente cliente: clienteRepository.findAll() ){
            Double valorTotal = clienteComponent.calculoValoresTotais(cliente);

            mapClientes.put(valorTotal, cliente);
        }

        List<Cliente> clientes = new ArrayList<>(mapClientes.values());

        return clienteComponent.inverterOrdemListaClientes(clientes);
    }

    public Cliente maiorCompraUnicaDoisMilEDezesseis(){
        TreeMap<Double, Cliente> mapClientes = new TreeMap<>();

        for ( Cliente cliente: clienteRepository.findAll() ){
            Double maiorCompraUnica = clienteComponent.verificaMaiorCompraUnica(cliente);

            mapClientes.put(maiorCompraUnica, cliente);
        }

        return clienteComponent.clienteMaiorCompra(mapClientes);
    }

    public List<Cliente> clientesMaisFieis(){
        TreeMap<Double, Cliente> mapClientes = new TreeMap<>();

        for ( Cliente cliente: clienteRepository.findAll() ){
            Double numeroHistorico = clienteComponent.numeroDeHistoricos(cliente);

            mapClientes.put(numeroHistorico, cliente);
        }

        List<Cliente> clientes = new ArrayList<>(mapClientes.values());

        return clienteComponent.inverterOrdemListaClientes(clientes);
    }

    public Item recomendacaoVinho(Long id){
        Optional<Cliente> clienteOptional = clienteRepository.findById(id);

        if(clienteOptional.isPresent()){

            Cliente cliente = clienteOptional.get();
            Set<Item> itensSet = clienteComponent.populaSetDeItens(cliente);

            TreeMap<Double, Item> mapItem = clienteComponent.geraItensRecomendacao(itensSet, cliente);


            List<Item> itens = new ArrayList<>(mapItem.values());

            return clienteComponent.inverterOrdemListaItens(itens).get(0);
        }

        return new Item();
    }
}
