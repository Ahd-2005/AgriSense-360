package entity;

import java.time.LocalDate;

public class Equipment {

    private Integer id;
    private String name;
    private String type;
    private String status;
    private LocalDate purchaseDate;

    public Equipment() {
    }

    public Equipment(Integer id, String name, String type, String status, LocalDate purchaseDate) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.purchaseDate = purchaseDate;
    }

    public Equipment(String name, String type, String status, LocalDate purchaseDate) {
        this.name = name;
        this.type = type;
        this.status = status;
        this.purchaseDate = purchaseDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    @Override
    public String toString() {
        String label = name != null ? name : "Equipment";
        return "#" + id + " - " + label;
    }
}
