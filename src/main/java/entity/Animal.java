package entity;

import java.time.LocalDate;
import java.time.Period;

public class Animal {

    private Integer id;
    private Integer earTag;
    private String type;
    private Double weight;
    private String healthStatus;
    private LocalDate birthDate;
    private LocalDate entryDate;
    private Origin origin;
    private Boolean vaccinated;
    private String location;

    public enum Origin { BORN_IN_FARM, OUTSIDE }

    public Animal() {
    }

    public Animal(Integer id, Integer earTag, String type, Double weight,
                  String healthStatus, LocalDate birthDate, LocalDate entryDate, Origin origin,
                  Boolean vaccinated, String location) {
        this.id = id;
        this.earTag = earTag;
        this.type = type;
        this.weight = weight;
        this.healthStatus = healthStatus;
        this.birthDate = birthDate;
        this.entryDate = entryDate;
        this.origin = origin;
        this.vaccinated = vaccinated;
        this.location = location;
    }

    public Animal(Integer earTag, String type, Double weight,
                  String healthStatus, LocalDate birthDate, LocalDate entryDate, Origin origin,
                  Boolean vaccinated, String location) {
        this.earTag = earTag;
        this.type = type;
        this.weight = weight;
        this.healthStatus = healthStatus;
        this.birthDate = birthDate;
        this.entryDate = entryDate;
        this.origin = origin;
        this.vaccinated = vaccinated;
        this.location = location;
    }

    public Animal(Integer earTag, String type, Double weight,
                  String healthStatus, LocalDate birthDate, LocalDate entryDate, Origin origin,
                  Boolean vaccinated) {
        this(earTag, type, weight, healthStatus, birthDate, entryDate, origin, vaccinated, null);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEarTag() { return earTag; }
    public void setEarTag(Integer earTag) { this.earTag = earTag; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
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
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }


    public Integer getAge() {
        if (birthDate == null) return null;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
