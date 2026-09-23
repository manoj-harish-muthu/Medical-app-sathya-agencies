package com.pharmacyerp.model;

import java.time.LocalDateTime;

public class Substitute {
    private int id;
    private int medicineId;
    private int substituteMedicineId;
    private String medicineName;
    private String substituteName;
    private LocalDateTime createdAt;

    public Substitute() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public int getSubstituteMedicineId() { return substituteMedicineId; }
    public void setSubstituteMedicineId(int substituteMedicineId) { this.substituteMedicineId = substituteMedicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getSubstituteName() { return substituteName; }
    public void setSubstituteName(String substituteName) { this.substituteName = substituteName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
