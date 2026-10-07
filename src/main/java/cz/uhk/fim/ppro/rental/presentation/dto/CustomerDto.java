package cz.uhk.fim.ppro.rental.presentation.dto;

import cz.uhk.fim.ppro.rental.domain.model.Customer;
import jakarta.validation.constraints.NotBlank;

public class CustomerDto {
    private Long id;

    @NotBlank(message = "Jméno je povinné")
    private String firstName;

    @NotBlank(message = "Příjmení je povinné")
    private String lastName;

    @NotBlank(message = "Telefon je povinný")
    private String phone;

    @NotBlank(message = "Číslo dokladu je povinné")
    private String idCardNumber;

    private String note;

    public CustomerDto() {}

    public CustomerDto(Customer c) {
        this.id = c.getId();
        this.firstName = c.getFirstName();
        this.lastName = c.getLastName();
        this.phone = c.getPhone();
        this.idCardNumber = c.getIdCardNumber();
        this.note = c.getNote();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIdCardNumber() { return idCardNumber; }
    public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
