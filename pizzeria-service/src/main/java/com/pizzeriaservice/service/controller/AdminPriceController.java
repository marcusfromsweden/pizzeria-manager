package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.PriceImportResponse;
import com.pizzeriaservice.service.config.CommonApiResponses;
import com.pizzeriaservice.service.service.AdminPriceService;
import com.pizzeriaservice.service.service.PizzeriaService;
import com.pizzeriaservice.service.support.ForbiddenException;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/admin/pizzerias/{pizzeriaCode}/prices")
@RequiredArgsConstructor
@Tag(name = "Admin Prices", description = "Price import and export for pizzeria administrators")
public class AdminPriceController {

  private static final Logger log = LoggerFactory.getLogger(AdminPriceController.class);

  private static final String CSV_HEADER = "type,id,name_key,price_regular,price_family\n";

  private final AdminPriceService adminPriceService;
  private final PizzeriaService pizzeriaService;

  @GetMapping("/export")
  @CommonApiResponses
  @Operation(summary = "Export prices as CSV")
  @ApiResponse(responseCode = "200", description = "CSV file with current prices")
  public Mono<ResponseEntity<String>> exportPrices(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    validateAdminAccess(user, pizzeriaCode);

    return pizzeriaService
        .resolvePizzeriaId(pizzeriaCode)
        .flatMap(
            pizzeriaId ->
                adminPriceService
                    .exportPrices(pizzeriaId)
                    .map(
                        row ->
                            String.format(
                                "%s,%s,%s,%s,%s",
                                row.type(),
                                row.id(),
                                row.nameKey(),
                                row.priceRegular(),
                                row.priceFamily()))
                    .collectList()
                    .map(
                        rows -> {
                          StringBuilder csv = new StringBuilder(CSV_HEADER);
                          for (String row : rows) {
                            csv.append(row).append("\n");
                          }
                          return csv.toString();
                        }))
        .map(
            csvContent ->
                ResponseEntity.ok()
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"prices-" + pizzeriaCode + ".csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvContent));
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @CommonApiResponses
  @Operation(summary = "Import prices from CSV")
  @ApiResponse(responseCode = "200", description = "Import result with updated/skipped counts")
  public Mono<PriceImportResponse> importPrices(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @RequestPart("file") FilePart file,
      @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun,
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    log.info(
        "Import prices request for pizzeria: {}, dryRun: {}, file: {}",
        pizzeriaCode,
        dryRun,
        file != null ? file.filename() : "null");
    validateAdminAccess(user, pizzeriaCode);

    return pizzeriaService
        .resolvePizzeriaId(pizzeriaCode)
        .flatMap(
            pizzeriaId ->
                file.content()
                    .reduce(
                        new java.io.ByteArrayOutputStream(),
                        (baos, dataBuffer) -> {
                          try {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            baos.write(bytes);
                          } catch (IOException e) {
                            throw new RuntimeException("Failed to read file content", e);
                          } finally {
                            DataBufferUtils.release(dataBuffer);
                          }
                          return baos;
                        })
                    .flatMap(
                        baos ->
                            adminPriceService.importPrices(
                                pizzeriaId,
                                new java.io.ByteArrayInputStream(baos.toByteArray()),
                                dryRun)))
        .doOnError(
            e ->
                log.error(
                    "Error importing prices for pizzeria {}: {}", pizzeriaCode, e.getMessage(), e));
  }

  private void validateAdminAccess(AuthenticatedUser user, String pizzeriaCode) {
    if (!user.isAdminFor(pizzeriaCode)) {
      throw new ForbiddenException(
          "User is not authorized to manage prices for pizzeria: " + pizzeriaCode);
    }
  }
}
