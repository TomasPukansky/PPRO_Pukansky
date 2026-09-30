package cz.uhk.fim.ppro.rental.domain.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Doménová entita pro číselník kategorií vybavení (např. Lyže, Snowboardy, Boty).
 */
@Entity
@Table(name = "equipment_categories")
public class EquipmentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EquipmentItem> items = new ArrayList<>();

    public EquipmentCategory() {
    }

    public EquipmentCategory(Long id, String code, String name, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<EquipmentItem> getItems() {
        return items;
    }

    public void setItems(List<EquipmentItem> items) {
        this.items = items;
    }
}
