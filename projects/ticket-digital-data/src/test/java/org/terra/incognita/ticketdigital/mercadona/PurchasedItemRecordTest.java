package org.terra.incognita.ticketdigital.mercadona;

import org.junit.jupiter.api.Test;
import org.terra.incognita.ticketdigital.mercadona.data.items.PurchasedItemRecord;
import org.terra.incognita.ticketdigital.mercadona.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurchasedItemRecordTest {

    @Test
    void testFromTicketWithAllItemTypes() {
        // Arrange
        LocalDateTime purchaseDate = LocalDateTime.of(2024, 4, 18, 10, 30);
        TicketHeader header = new TicketHeader(purchaseDate, "123", "456");
        
        PurchasedItem item1 = new OneItemByUnit("Item 1", 1, new BigDecimal("1.50"),new BigDecimal("1.50"));
        PurchasedItem item2 = new NItemsByUnit("Item 2", 2, new BigDecimal("2.00"),new BigDecimal("4.00"));
        PurchasedItem item3 = new ItemByWeight("Item 3", new BigDecimal("0.500"), new BigDecimal("10.00"),new BigDecimal("5.00")); // 500g
        PurchasedItem item4 = new FreshItemByWeight("Item 4", new BigDecimal("1.250"), new BigDecimal("8.00"), new BigDecimal("0.00"),"PESCADO"); // 1250g

        TicketMercadona ticket = new TicketMercadona(
                null,
                header,
                List.of(item1, item2, item3, item4),
                null,
                new BigDecimal("18.50"),
                new BigDecimal("18.50"),
                null,
                null
        );

        // Act
        List<PurchasedItemRecord> records = PurchasedItemRecord.fromTicket(ticket);

        // Assert
        assertEquals(4, records.size());

        // Item 1: OneItemByUnit
        PurchasedItemRecord record1 = records.get(0);
        assertEquals("Item 1", record1.id());
        assertEquals(purchaseDate, record1.date());
        assertEquals(Integer.valueOf(1), record1.units());
        assertNotNull(record1.unitPrice());
        assertNull(record1.weightKg());
        assertNull(record1.pricePerKg());
        assertEquals(new BigDecimal("1.50"), record1.price());

        // Item 2: NItemsByUnit
        PurchasedItemRecord record2 = records.get(1);
        assertEquals("Item 2", record2.id());
        assertEquals(purchaseDate, record2.date());
        assertEquals(Integer.valueOf(2), record2.units());
        assertEquals(new BigDecimal("2.00"), record2.unitPrice());
        assertNull(record2.weightKg());
        assertNull(record2.pricePerKg());
        assertEquals(new BigDecimal("4.00"), record2.price());

        // Item 3: ItemByWeight
        PurchasedItemRecord record3 = records.get(2);
        assertEquals("Item 3", record3.id());
        assertEquals(purchaseDate, record3.date());
        assertEquals(Integer.valueOf(1), record3.units());
        assertNull(record3.unitPrice());
        assertEquals(new BigDecimal("0.500"), record3.weightKg());
        assertEquals(new BigDecimal("10.00"), record3.pricePerKg());
        assertEquals(new BigDecimal("5.00"), record3.price()); // 0.5 * 10.00

        // Item 4: FreshItemByWeight
        PurchasedItemRecord record4 = records.get(3);
        assertEquals("Item 4", record4.id());
        assertEquals(purchaseDate, record4.date());
        assertEquals(Integer.valueOf(1),record4.units());
        assertNull(record4.unitPrice());
        assertEquals(new BigDecimal("1.250"), record4.weightKg());
        assertEquals(new BigDecimal("8.00"), record4.pricePerKg());
        assertEquals(new BigDecimal("0.00"), record4.price());
    }
}
