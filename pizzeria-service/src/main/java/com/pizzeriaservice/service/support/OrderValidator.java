package com.pizzeriaservice.service.support;

import com.pizzeriaservice.api.dto.CreateOrderRequest;
import com.pizzeriaservice.api.dto.OrderItemRequest;
import com.pizzeriaservice.service.domain.PizzaSize;
import com.pizzeriaservice.service.menu.MenuItemEntity;
import com.pizzeriaservice.service.menu.PizzaCustomisationEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderValidator {

  public void validateOrderItems(
      CreateOrderRequest request,
      Map<UUID, MenuItemEntity> menuItems,
      Map<UUID, PizzaCustomisationEntity> customisations) {

    List<String> violations = new ArrayList<>();

    for (OrderItemRequest item : request.items()) {
      MenuItemEntity menuItem = menuItems.get(item.menuItemId());
      if (menuItem == null) {
        violations.add("Menu item not found: " + item.menuItemId());
        continue;
      }

      PizzaSize size = PizzaSize.valueOf(item.size());
      if (size == PizzaSize.FAMILY && menuItem.priceFamily() == null) {
        violations.add("Family size is not available for menu item: " + menuItem.nameKey());
      }

      if (item.customisationIds() != null) {
        for (UUID custId : item.customisationIds()) {
          PizzaCustomisationEntity cust = customisations.get(custId);
          if (cust == null) {
            violations.add("Customisation not found: " + custId);
            continue;
          }
          if (size == PizzaSize.FAMILY && cust.priceFamily() == null) {
            violations.add(
                "Family size price is not available for customisation: " + cust.nameKey());
          }
        }
      }
    }

    if (!violations.isEmpty()) {
      throw new DomainValidationException(violations);
    }
  }
}
