package com.pharmacyerp.model;

import java.time.LocalDateTime;

public class Composition {
    private int id;
    private int medicineId;
    private int saltId;
    private String medicineName;
    private String saltName;
    private String strength;
    private LocalDateTime createdAt;

    public Composition() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public int getSaltId() { return saltId; }
    public void setSaltId(int saltId) { this.saltId = saltId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getSaltName() { return saltName; }
    public void setSaltName(String saltName) { this.saltName = saltName; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
