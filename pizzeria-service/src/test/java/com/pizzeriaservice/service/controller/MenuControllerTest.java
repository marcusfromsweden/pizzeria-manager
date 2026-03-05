package com.pizzeriaservice.service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.MenuResponse;
import com.pizzeriaservice.service.service.MenuService;
import com.pizzeriaservice.service.service.PizzeriaService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MenuControllerTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String PIZZERIA_CODE = "testpizzeria";

  @Mock private MenuService menuService;
  @Mock private PizzeriaService pizzeriaService;

  private MenuController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new MenuController(menuService, pizzeriaService);
  }

  @Test
  void getMenuResolvesPizzeriaAndDelegatesToService() {
    MenuResponse menuResponse = new MenuResponse(List.of(), List.of());

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(menuService.getMenu(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(menuResponse));

    StepVerifier.create(controller.getMenu(PIZZERIA_CODE))
        .expectNext(menuResponse)
        .verifyComplete();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(menuService).getMenu(DEFAULT_PIZZERIA_ID);
  }

  @Test
  void getMenuPropagatesPizzeriaServiceError() {
    RuntimeException error = new RuntimeException("Pizzeria not found");

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE)).thenReturn(Mono.error(error));

    StepVerifier.create(controller.getMenu(PIZZERIA_CODE))
        .expectError(RuntimeException.class)
        .verify();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
  }

  @Test
  void getMenuPropagatesMenuServiceError() {
    RuntimeException error = new RuntimeException("Menu service error");

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(menuService.getMenu(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.error(error));

    StepVerifier.create(controller.getMenu(PIZZERIA_CODE))
        .expectError(RuntimeException.class)
        .verify();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(menuService).getMenu(DEFAULT_PIZZERIA_ID);
  }
}
