package entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Maintenance {

    private Integer id;
    private Integer equipmentId;
    private LocalDate maintenanceDate;
    private String maintenanceType;
    private BigDecimal cost;

    public Maintenance() {
    }

    public Maintenance(Integer id, Integer equipmentId, LocalDate maintenanceDate, String maintenanceType, BigDecimal cost) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.maintenanceDate = maintenanceDate;
        this.maintenanceType = maintenanceType;
        this.cost = cost;
    }

    public Maintenance(Integer equipmentId, LocalDate maintenanceDate, String maintenanceType, BigDecimal cost) {
        this.equipmentId = equipmentId;
        this.maintenanceDate = maintenanceDate;
        this.maintenanceType = maintenanceType;
        this.cost = cost;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Integer equipmentId) {
        this.equipmentId = equipmentId;
    }

    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(String maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }
}
