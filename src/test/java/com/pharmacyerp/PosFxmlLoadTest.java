package com.pharmacyerp;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class PosFxmlLoadTest {

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Already started
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    private void assertFxmlLoads(String fxmlPath) throws Exception {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();
                assertNotNull(root, "Root node must not be null for " + fxmlPath);
                assertNotNull(loader.getController(), "Controller must not be null for " + fxmlPath);
                System.out.println("Successfully loaded: " + fxmlPath + " with controller: " + loader.getController().getClass().getSimpleName());
            } catch (Throwable t) {
                t.printStackTrace();
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Loading timed out for " + fxmlPath);
        if (errorRef.get() != null) {
            fail("Failed to load " + fxmlPath + ": " + errorRef.get().getMessage(), errorRef.get());
        }
    }

    @Test
    public void testLoadPosFxml() throws Exception {
        assertFxmlLoads("/fxml/Pos.fxml");
    }

    @Test
    public void testLoadDashboardFxml() throws Exception {
        assertFxmlLoads("/fxml/Dashboard.fxml");
    }

    @Test
    public void testLoadSidebarFxml() throws Exception {
        assertFxmlLoads("/fxml/Sidebar.fxml");
    }

    @Test
    public void testLoadLoginFxml() throws Exception {
        assertFxmlLoads("/fxml/Login.fxml");
    }

    @Test
    public void testLoadMedicinesFxml() throws Exception {
        assertFxmlLoads("/fxml/Medicines.fxml");
    }

    @Test
    public void testLoadInventoryFxml() throws Exception {
        assertFxmlLoads("/fxml/Inventory.fxml");
    }

    @Test
    public void testLoadPurchasesFxml() throws Exception {
        assertFxmlLoads("/fxml/Purchases.fxml");
    }

    @Test
    public void testLoadPreviousBillsFxml() throws Exception {
        assertFxmlLoads("/fxml/PreviousBills.fxml");
    }

    @Test
    public void testLoadAgentOrdersFxml() throws Exception {
        assertFxmlLoads("/fxml/AgentOrders.fxml");
    }

    @Test
    public void testLoadDayEndClosingFxml() throws Exception {
        assertFxmlLoads("/fxml/DayEndClosing.fxml");
    }

    @Test
    public void testLoadPendingOrdersFxml() throws Exception {
        assertFxmlLoads("/fxml/PendingOrders.fxml");
    }

    @Test
    public void testLoadCompletedOrdersFxml() throws Exception {
        assertFxmlLoads("/fxml/CompletedOrders.fxml");
    }

    @Test
    public void testLoadFailedOrdersFxml() throws Exception {
        assertFxmlLoads("/fxml/FailedOrders.fxml");
    }
}

