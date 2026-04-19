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
        LocalDateTime fechaCompra = LocalDateTime.of(2024, 4, 18, 10, 30);
        TicketHeader header = new TicketHeader(fechaCompra, "123", "456");
        
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
                "1234", "NC", "AUT", "AID", "ARC"
        );

        // Act
        List<PurchasedItemRecord> records = PurchasedItemRecord.fromTicket(ticket);

        // Assert
        assertEquals(4, records.size());

        // Item 1: OneItemByUnit
        PurchasedItemRecord record1 = records.get(0);
        assertEquals("Item 1", record1.id());
        assertEquals(fechaCompra, record1.fecha());
        assertEquals(Integer.valueOf(1), record1.unidades());
        assertNotNull(record1.precioPorUnidad());
        assertNull(record1.pesoKg());
        assertNull(record1.precioKg());
        assertEquals(new BigDecimal("1.50"), record1.precio());

        // Item 2: NItemsByUnit
        PurchasedItemRecord record2 = records.get(1);
        assertEquals("Item 2", record2.id());
        assertEquals(fechaCompra, record2.fecha());
        assertEquals(Integer.valueOf(2), record2.unidades());
        assertEquals(new BigDecimal("2.00"), record2.precioPorUnidad());
        assertNull(record2.pesoKg());
        assertNull(record2.precioKg());
        assertEquals(new BigDecimal("4.00"), record2.precio());

        // Item 3: ItemByWeight
        PurchasedItemRecord record3 = records.get(2);
        assertEquals("Item 3", record3.id());
        assertEquals(fechaCompra, record3.fecha());
        assertNull(record3.unidades());
        assertNull(record3.precioPorUnidad());
        assertEquals(new BigDecimal("0.500"), record3.pesoKg());
        assertEquals(new BigDecimal("10.00"), record3.precioKg());
        assertEquals(new BigDecimal("5.00"), record3.precio()); // 0.5 * 10.00

        // Item 4: FreshItemByWeight
        PurchasedItemRecord record4 = records.get(3);
        assertEquals("Item 4", record4.id());
        assertEquals(fechaCompra, record4.fecha());
        assertNull(record4.unidades());
        assertNull(record4.precioPorUnidad());
        assertEquals(new BigDecimal("1.250"), record4.pesoKg());
        assertEquals(new BigDecimal("8.00"), record4.precioKg());
        assertEquals(new BigDecimal("0.00"), record4.precio());
    }
}
