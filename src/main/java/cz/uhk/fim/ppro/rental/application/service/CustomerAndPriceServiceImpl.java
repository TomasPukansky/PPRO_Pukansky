package cz.uhk.fim.ppro.rental.application.service;

import cz.uhk.fim.ppro.rental.application.exception.ResourceNotFoundException;
import cz.uhk.fim.ppro.rental.domain.model.Customer;
import cz.uhk.fim.ppro.rental.domain.repository.CustomerRepository;
import cz.uhk.fim.ppro.rental.domain.repository.PriceRuleRepository;
import cz.uhk.fim.ppro.rental.presentation.dto.CustomerDto;
import cz.uhk.fim.ppro.rental.presentation.dto.PriceRuleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerAndPriceServiceImpl implements CustomerAndPriceService {

    private final CustomerRepository customerRepository;
    private final PriceRuleRepository priceRuleRepository;

    public CustomerAndPriceServiceImpl(CustomerRepository customerRepository, PriceRuleRepository priceRuleRepository) {
        this.customerRepository = customerRepository;
        this.priceRuleRepository = priceRuleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDto> getAllCustomers(String query) {
        List<Customer> list;
        if (query != null && !query.isBlank()) {
            list = customerRepository.findByLastNameContainingIgnoreCaseOrPhoneContaining(query.trim(), query.trim());
        } else {
            list = customerRepository.findAll();
        }
        return list.stream().map(CustomerDto::new).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDto getCustomerById(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zákazník " + id + " nenalezen."));
        return new CustomerDto(c);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDto getCustomerByPhone(String phone) {
        Customer c = customerRepository.findByPhone(phone.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Zákazník s telefonem " + phone + " nenalezen."));
        return new CustomerDto(c);
    }

    @Override
    public CustomerDto createCustomer(CustomerDto dto) {
        Customer c = new Customer(
                dto.getFirstName().trim(),
                dto.getLastName().trim(),
                dto.getPhone().trim(),
                dto.getIdCardNumber().trim(),
                dto.getNote()
        );
        return new CustomerDto(customerRepository.save(c));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceRuleDto> getAllPriceRules() {
        return priceRuleRepository.findAll().stream()
                .map(PriceRuleDto::new)
                .collect(Collectors.toList());
    }
}
