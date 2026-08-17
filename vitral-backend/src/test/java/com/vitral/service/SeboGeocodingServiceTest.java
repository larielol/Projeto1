package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vitral.entity.Sebo;

@ExtendWith(MockitoExtension.class)
class SeboGeocodingServiceTest {

    @Mock
    private GeocodingClient client;

    @InjectMocks
    private SeboGeocodingService service;

    @Test
    @DisplayName("Deve limpar as coordenadas quando o sebo for virtual")
    void geocodificar_seboVirtual_limpaCoordenadas() {
        Sebo sebo = Sebo.builder()
                .latitude(-7.12)
                .longitude(-34.88)
                .build();

        service.geocodificar(sebo);

        assertThat(sebo.getLatitude()).isNull();
        assertThat(sebo.getLongitude()).isNull();
    }

    @Test
    @DisplayName("Deve limpar as coordenadas quando o sebo fisico nao tiver cidade/UF")
    void geocodificar_seboFisicoSemCidadeUf_limpaCoordenadas() {
        Sebo sebo = Sebo.builder()
                .cidade(null)
                .uf(null)
                .latitude(-7.12)
                .longitude(-34.88)
                .build();

        service.geocodificar(sebo);

        assertThat(sebo.getLatitude()).isNull();
        assertThat(sebo.getLongitude()).isNull();
    }

    @Test
    @DisplayName("Deve definir latitude e longitude quando o endereco completo for encontrado")
    void geocodificar_enderecoCompletoEncontrado_definirLatLng() {
        Sebo sebo = Sebo.builder()
                .logradouro("Rua das Letras")
                .cidade("Joao Pessoa")
                .uf("PB")
                .build();
        when(client.buscarCoordenadas("Rua das Letras, Joao Pessoa - PB, Brasil"))
                .thenReturn(Optional.of(new GeocodingClient.Coordenadas(-7.115, -34.861)));

        service.geocodificar(sebo);

        assertThat(sebo.getLatitude()).isEqualTo(-7.115);
        assertThat(sebo.getLongitude()).isEqualTo(-34.861);
        verify(client).buscarCoordenadas("Rua das Letras, Joao Pessoa - PB, Brasil");
    }

    @Test
    @DisplayName("Deve tentar apenas cidade/UF quando o endereco completo nao for encontrado")
    void geocodificar_enderecoCompletoNaoEncontrado_tentaCidadeUf() {
        Sebo sebo = Sebo.builder()
                .logradouro("Rua Desconhecida, 999")
                .cidade("Brejo Santo")
                .uf("CE")
                .build();
        when(client.buscarCoordenadas("Rua Desconhecida, 999, Brejo Santo - CE, Brasil"))
                .thenReturn(Optional.empty());
        when(client.buscarCoordenadas("Brejo Santo - CE, Brasil"))
                .thenReturn(Optional.of(new GeocodingClient.Coordenadas(-7.4943711, -38.9857354)));

        service.geocodificar(sebo);

        assertThat(sebo.getLatitude()).isEqualTo(-7.4943711);
        assertThat(sebo.getLongitude()).isEqualTo(-38.9857354);
    }

    @Test
    @DisplayName("Deve limpar coordenadas quando nem o endereco completo nem a cidade forem encontrados")
    void geocodificar_nenhumaConsultaEncontrada_limpaCoordenadas() {
        Sebo sebo = Sebo.builder()
                .logradouro("Rua X")
                .cidade("Cidade Inexistente")
                .uf("XX")
                .latitude(-1.0)
                .longitude(-2.0)
                .build();
        when(client.buscarCoordenadas("Rua X, Cidade Inexistente - XX, Brasil")).thenReturn(Optional.empty());
        when(client.buscarCoordenadas("Cidade Inexistente - XX, Brasil")).thenReturn(Optional.empty());

        service.geocodificar(sebo);

        assertThat(sebo.getLatitude()).isNull();
        assertThat(sebo.getLongitude()).isNull();
    }

    @Test
    @DisplayName("Deve limpar coordenadas quando o cliente lancar excecao (best-effort)")
    void geocodificar_clienteLancaExcecao_limpaCoordenadasSemPropagar() {
        Sebo sebo = Sebo.builder()
                .logradouro("Rua Y")
                .cidade("Campina Grande")
                .uf("PB")
                .latitude(-1.0)
                .longitude(-2.0)
                .build();
        when(client.buscarCoordenadas("Rua Y, Campina Grande - PB, Brasil"))
                .thenThrow(new RuntimeException("falha de rede"));

        service.geocodificar(sebo);

        assertThat(sebo.getLatitude()).isNull();
        assertThat(sebo.getLongitude()).isNull();
    }
}
