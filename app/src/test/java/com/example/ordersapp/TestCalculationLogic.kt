package com.example.ordersapp


import com.example.ordersapp.adapters.ProductInOrderDisplay
import com.example.ordersapp.db.ProductsEntity
import java.util.Locale



/**
 * Рассчитывает общую стоимость списка товаров в заказе.
 * (Копия логики из OrdersRedActivity.calculateAndDisplayTotalCost)
 * @param products Список товаров с указанием количества в заказе.
 * @return Общая стоимость (Double).
 */
fun calculateTotalCostForTesting(products: List<ProductInOrderDisplay>): Double {
    // Используем sumOf для расчета суммы произведений цены на количество
    if (products.isEmpty()) {
        return 0.0
    }
    // Добавлена проверка на null cost, как в идеальной вынесенной функции
    return products.sumOf { (it.product.cost ?: 0.0) * it.quantityInOrder }
}

// Сюда можно добавить другие копии логики для тестирования, если нужно