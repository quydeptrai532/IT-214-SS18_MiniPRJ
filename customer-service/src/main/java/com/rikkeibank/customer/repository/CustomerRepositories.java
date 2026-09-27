package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.AccountType;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public class CustomerRepositories {

    private CustomerRepositories() {
    }

    @Repository
    public interface CustomerRepository extends JpaRepository<Customer, Long> {
        Optional<Customer> findByCustomerCode(String customerCode);

        Optional<Customer> findByIdentityNumber(String identityNumber);

        boolean existsByIdentityNumber(String identityNumber);
    }

    @Repository
    public interface StaffRepository extends JpaRepository<Staff, Long> {
        Optional<Staff> findByStaffCode(String staffCode);

        boolean existsByEmail(String email);
    }

    @Repository
    public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {
        Optional<AccountType> findByCode(String code);
    }
}
