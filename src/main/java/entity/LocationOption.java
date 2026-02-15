package entity;

public class LocationOption {
    private Integer id;
    private String name;

    public LocationOption() {}

    public LocationOption(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public LocationOption(String name) {
        this.name = name;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name != null ? name : ""; }
}
