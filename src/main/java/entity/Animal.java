package entity;

import java.time.LocalDate;

public class Animal {

    private Integer id;
    private Integer earTag;
    private AnimalType type;
    private Gender gender;
    private Double weight;
    private String healthStatus;
    private LocalDate birthDate;
    private LocalDate entryDate;
    private Origin origin;
    private Boolean vaccinated;

    public enum AnimalType { SHEEP, COW, GOAT, CHICKEN }
    public enum Gender { MALE, FEMALE }
    public enum Origin { BORN_IN_FARM, OUTSIDE }

    public Animal() {
    }

    public Animal(Integer id, Integer earTag, AnimalType type, Gender gender, Double weight,
                  String healthStatus, LocalDate birthDate, LocalDate entryDate, Origin origin,
                  Boolean vaccinated) {
        this.id = id;
        this.earTag = earTag;
        this.type = type;
        this.gender = gender;
        this.weight = weight;
        this.healthStatus = healthStatus;
        this.birthDate = birthDate;
        this.entryDate = entryDate;
        this.origin = origin;
        this.vaccinated = vaccinated;
    }

    public Animal(Integer earTag, AnimalType type, Gender gender, Double weight,
                  String healthStatus, LocalDate birthDate, LocalDate entryDate, Origin origin,
                  Boolean vaccinated) {
        this.earTag = earTag;
        this.type = type;
        this.gender = gender;
        this.weight = weight;
        this.healthStatus = healthStatus;
        this.birthDate = birthDate;
        this.entryDate = entryDate;
        this.origin = origin;
        this.vaccinated = vaccinated;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEarTag() { return earTag; }
    public void setEarTag(Integer earTag) { this.earTag = earTag; }
    public AnimalType getType() { return type; }
    public void setType(AnimalType type) { this.type = type; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin origin) { this.origin = origin; }
    public Boolean getVaccinated() { return vaccinated; }
    public void setVaccinated(Boolean vaccinated) { this.vaccinated = vaccinated; }
}
