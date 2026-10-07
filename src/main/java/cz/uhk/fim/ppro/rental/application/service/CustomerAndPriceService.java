package cz.uhk.fim.ppro.rental.application.service;

import cz.uhk.fim.ppro.rental.presentation.dto.CustomerDto;
import cz.uhk.fim.ppro.rental.presentation.dto.PriceRuleDto;

import java.util.List;

public interface CustomerAndPriceService {

    List<CustomerDto> getAllCustomers(String query);

    CustomerDto getCustomerById(Long id);

    CustomerDto getCustomerByPhone(String phone);

    CustomerDto createCustomer(CustomerDto dto);

    List<PriceRuleDto> getAllPriceRules();
}
