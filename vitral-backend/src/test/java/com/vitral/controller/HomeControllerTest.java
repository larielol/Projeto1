package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vitral.dto.HomeResponse;
import com.vitral.dto.HomeSectionResponse;
import com.vitral.service.HomeService;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock HomeService homeService;
    @InjectMocks HomeController controller;

    @Test
    void visitanteCarregaHomeSemPrincipal() {
        HomeSectionResponse vazia = new HomeSectionResponse("Secao", List.of(), 0);
        HomeResponse esperado = new HomeResponse(vazia, vazia, null, List.of());
        when(homeService.carregar(null)).thenReturn(esperado);

        var resposta = controller.carregar(null);

        assertThat(resposta.getBody()).isSameAs(esperado);
        verify(homeService).carregar(null);
    }
}
