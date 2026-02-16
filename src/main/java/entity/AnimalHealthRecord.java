package entity;

import java.time.LocalDate;

public class AnimalHealthRecord {

    private Integer id;
    private Integer animalId;
    private LocalDate recordDate;
    private Double weight;
    private Appetite appetite;
    private ConditionStatus conditionStatus;
    private Double milkYield;
    private Integer eggCount;
    private Double woolLength;
    private String notes;

    public enum Appetite { LOW, NORMAL, HIGH, NONE }
    public enum ConditionStatus { HEALTHY, SICK, INJURED, CRITICAL }

    public AnimalHealthRecord() {
    }

    public AnimalHealthRecord(Integer id, Integer animalId, LocalDate recordDate, Double weight,
                              Appetite appetite, ConditionStatus conditionStatus, Double milkYield,
                              Integer eggCount, Double woolLength, String notes) {
        this.id = id;
        this.animalId = animalId;
        this.recordDate = recordDate;
        this.weight = weight;
        this.appetite = appetite;
        this.conditionStatus = conditionStatus;
        this.milkYield = milkYield;
        this.eggCount = eggCount;
        this.woolLength = woolLength;
        this.notes = notes;
    }

    public AnimalHealthRecord(Integer animalId, LocalDate recordDate, Double weight,
                              Appetite appetite, ConditionStatus conditionStatus, Double milkYield,
                              Integer eggCount, Double woolLength, String notes) {
        this.animalId = animalId;
        this.recordDate = recordDate;
        this.weight = weight;
        this.appetite = appetite;
        this.conditionStatus = conditionStatus;
        this.milkYield = milkYield;
        this.eggCount = eggCount;
        this.woolLength = woolLength;
        this.notes = notes;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getAnimalId() { return animalId; }
    public void setAnimalId(Integer animalId) { this.animalId = animalId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public Appetite getAppetite() { return appetite; }
    public void setAppetite(Appetite appetite) { this.appetite = appetite; }
    public ConditionStatus getConditionStatus() { return conditionStatus; }
    public void setConditionStatus(ConditionStatus conditionStatus) { this.conditionStatus = conditionStatus; }
    public Double getMilkYield() { return milkYield; }
    public void setMilkYield(Double milkYield) { this.milkYield = milkYield; }
    public Integer getEggCount() { return eggCount; }
    public void setEggCount(Integer eggCount) { this.eggCount = eggCount; }
    public Double getWoolLength() { return woolLength; }
    public void setWoolLength(Double woolLength) { this.woolLength = woolLength; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
