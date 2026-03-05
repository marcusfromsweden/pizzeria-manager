package com.pizzeriaservice.service.service;

import com.pizzeriaservice.api.dto.PriceChangeRow;
import com.pizzeriaservice.api.dto.PriceExportRow;
import com.pizzeriaservice.api.dto.PriceImportResponse;
import com.pizzeriaservice.service.menu.MenuItemEntity;
import com.pizzeriaservice.service.menu.MenuItemRepository;
import com.pizzeriaservice.service.menu.PizzaCustomisationEntity;
import com.pizzeriaservice.service.menu.PizzaCustomisationRepository;
import com.pizzeriaservice.service.support.DomainValidationException;
import com.pizzeriaservice.service.support.TimeProvider;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AdminPriceService {

  private static final String TYPE_MENU_ITEM = "MENU_ITEM";
  private static final String TYPE_CUSTOMISATION = "CUSTOMISATION";
  private static final String STATUS_UPDATED = "UPDATED";
  private static final String STATUS_NO_CHANGE = "NO_CHANGE";
  private static final String STATUS_NOT_FOUND = "NOT_FOUND";

  private final MenuItemRepository menuItemRepository;
  private final PizzaCustomisationRepository customisationRepository;
  private final TimeProvider timeProvider;

  /**
   * Export all prices for a pizzeria as a stream of export rows.
   *
   * @param pizzeriaId the pizzeria ID
   * @return flux of price export rows (menu items and customizations)
   */
  public Flux<PriceExportRow> exportPrices(UUID pizzeriaId) {
    Flux<PriceExportRow> menuItems =
        menuItemRepository
            .findAllByPizzeriaIdOrderBySortOrderAsc(pizzeriaId)
            .map(
                item ->
                    new PriceExportRow(
                        TYPE_MENU_ITEM,
                        item.id(),
                        item.nameKey(),
                        item.priceRegular(),
                        item.priceFamily()));

    Flux<PriceExportRow> customizations =
        customisationRepository
            .findAllByPizzeriaIdOrderBySortOrderAsc(pizzeriaId)
            .map(
                cust ->
                    new PriceExportRow(
                        TYPE_CUSTOMISATION,
                        cust.id(),
                        cust.nameKey(),
                        cust.priceRegular(),
                        cust.priceFamily()));

    return menuItems.concatWith(customizations);
  }

  /**
   * Import prices from CSV input stream.
   *
   * @param pizzeriaId the pizzeria ID
   * @param csvInputStream the CSV input stream
   * @param dryRun if true, only preview changes without applying
   * @return import response with summary and change details
   */
  public Mono<PriceImportResponse> importPrices(
      UUID pizzeriaId, InputStream csvInputStream, boolean dryRun) {
    return Mono.fromCallable(() -> parseCsv(csvInputStream))
        .flatMap(rows -> processImportRows(pizzeriaId, rows, dryRun));
  }

  private List<CsvRow> parseCsv(InputStream inputStream) {
    List<CsvRow> rows = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      int lineNum = 0;
      while ((line = reader.readLine()) != null) {
        lineNum++;
        if (lineNum == 1) {
          // Skip header row
          continue;
        }
        if (line.trim().isEmpty()) {
          continue;
        }
        rows.add(parseCsvLine(line, lineNum));
      }
    } catch (Exception e) {
      throw new DomainValidationException("Failed to parse CSV: " + e.getMessage());
    }
    return rows;
  }

  private CsvRow parseCsvLine(String line, int lineNum) {
    String[] parts = line.split(",");
    if (parts.length < 5) {
      throw new DomainValidationException(
          "Invalid CSV format at line " + lineNum + ": expected 5 columns");
    }
    try {
      String type = parts[0].trim();
      UUID id = UUID.fromString(parts[1].trim());
      String nameKey = parts[2].trim();
      BigDecimal priceRegular = parsePrice(parts[3].trim());
      BigDecimal priceFamily = parsePrice(parts[4].trim());
      return new CsvRow(type, id, nameKey, priceRegular, priceFamily);
    } catch (IllegalArgumentException e) {
      throw new DomainValidationException(
          "Invalid data at line " + lineNum + ": " + e.getMessage());
    }
  }

  private BigDecimal parsePrice(String value) {
    if (value == null || value.isEmpty() || "null".equalsIgnoreCase(value)) {
      return null;
    }
    return new BigDecimal(value);
  }

  private Mono<PriceImportResponse> processImportRows(
      UUID pizzeriaId, List<CsvRow> rows, boolean dryRun) {
    List<Mono<PriceChangeRow>> changeMono = new ArrayList<>();

    for (CsvRow row : rows) {
      if (TYPE_MENU_ITEM.equals(row.type())) {
        changeMono.add(processMenuItem(pizzeriaId, row, dryRun));
      } else if (TYPE_CUSTOMISATION.equals(row.type())) {
        changeMono.add(processCustomisation(pizzeriaId, row, dryRun));
      } else {
        changeMono.add(
            Mono.just(
                new PriceChangeRow(
                    row.type(),
                    row.id(),
                    row.nameKey(),
                    null,
                    row.priceRegular(),
                    null,
                    row.priceFamily(),
                    STATUS_NOT_FOUND)));
      }
    }

    return Flux.merge(changeMono)
        .collectList()
        .map(
            changes -> {
              int updated = 0;
              int unchanged = 0;
              int errors = 0;
              for (PriceChangeRow change : changes) {
                switch (change.status()) {
                  case STATUS_UPDATED -> updated++;
                  case STATUS_NO_CHANGE -> unchanged++;
                  default -> errors++;
                }
              }
              return new PriceImportResponse(
                  dryRun, changes.size(), updated, unchanged, errors, changes);
            });
  }

  private Mono<PriceChangeRow> processMenuItem(UUID pizzeriaId, CsvRow row, boolean dryRun) {
    return menuItemRepository
        .findByIdAndPizzeriaId(row.id(), pizzeriaId)
        .flatMap(
            item -> {
              boolean priceChanged =
                  !pricesEqual(item.priceRegular(), row.priceRegular())
                      || !pricesEqual(item.priceFamily(), row.priceFamily());

              if (!priceChanged) {
                return Mono.just(
                    new PriceChangeRow(
                        TYPE_MENU_ITEM,
                        item.id(),
                        item.nameKey(),
                        item.priceRegular(),
                        row.priceRegular(),
                        item.priceFamily(),
                        row.priceFamily(),
                        STATUS_NO_CHANGE));
              }

              if (dryRun) {
                return Mono.just(
                    new PriceChangeRow(
                        TYPE_MENU_ITEM,
                        item.id(),
                        item.nameKey(),
                        item.priceRegular(),
                        row.priceRegular(),
                        item.priceFamily(),
                        row.priceFamily(),
                        STATUS_UPDATED));
              }

              MenuItemEntity updated =
                  new MenuItemEntity(
                      item.id(),
                      item.pizzeriaId(),
                      item.sectionId(),
                      item.dishNumber(),
                      item.nameKey(),
                      item.descriptionKey(),
                      row.priceRegular(),
                      row.priceFamily(),
                      item.sortOrder(),
                      item.createdAt(),
                      timeProvider.now());

              return menuItemRepository
                  .save(updated)
                  .map(
                      saved ->
                          new PriceChangeRow(
                              TYPE_MENU_ITEM,
                              saved.id(),
                              saved.nameKey(),
                              item.priceRegular(),
                              saved.priceRegular(),
                              item.priceFamily(),
                              saved.priceFamily(),
                              STATUS_UPDATED));
            })
        .defaultIfEmpty(
            new PriceChangeRow(
                TYPE_MENU_ITEM,
                row.id(),
                row.nameKey(),
                null,
                row.priceRegular(),
                null,
                row.priceFamily(),
                STATUS_NOT_FOUND));
  }

  private Mono<PriceChangeRow> processCustomisation(UUID pizzeriaId, CsvRow row, boolean dryRun) {
    return customisationRepository
        .findById(row.id())
        .filter(cust -> cust.pizzeriaId().equals(pizzeriaId))
        .flatMap(
            cust -> {
              boolean priceChanged =
                  !pricesEqual(cust.priceRegular(), row.priceRegular())
                      || !pricesEqual(cust.priceFamily(), row.priceFamily());

              if (!priceChanged) {
                return Mono.just(
                    new PriceChangeRow(
                        TYPE_CUSTOMISATION,
                        cust.id(),
                        cust.nameKey(),
                        cust.priceRegular(),
                        row.priceRegular(),
                        cust.priceFamily(),
                        row.priceFamily(),
                        STATUS_NO_CHANGE));
              }

              if (dryRun) {
                return Mono.just(
                    new PriceChangeRow(
                        TYPE_CUSTOMISATION,
                        cust.id(),
                        cust.nameKey(),
                        cust.priceRegular(),
                        row.priceRegular(),
                        cust.priceFamily(),
                        row.priceFamily(),
                        STATUS_UPDATED));
              }

              PizzaCustomisationEntity updated =
                  new PizzaCustomisationEntity(
                      cust.id(),
                      cust.pizzeriaId(),
                      cust.nameKey(),
                      row.priceRegular(),
                      row.priceFamily(),
                      cust.sortOrder(),
                      cust.createdAt(),
                      timeProvider.now());

              return customisationRepository
                  .save(updated)
                  .map(
                      saved ->
                          new PriceChangeRow(
                              TYPE_CUSTOMISATION,
                              saved.id(),
                              saved.nameKey(),
                              cust.priceRegular(),
                              saved.priceRegular(),
                              cust.priceFamily(),
                              saved.priceFamily(),
                              STATUS_UPDATED));
            })
        .defaultIfEmpty(
            new PriceChangeRow(
                TYPE_CUSTOMISATION,
                row.id(),
                row.nameKey(),
                null,
                row.priceRegular(),
                null,
                row.priceFamily(),
                STATUS_NOT_FOUND));
  }

  private boolean pricesEqual(BigDecimal a, BigDecimal b) {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return a.compareTo(b) == 0;
  }

  private record CsvRow(
      String type, UUID id, String nameKey, BigDecimal priceRegular, BigDecimal priceFamily) {}
}
