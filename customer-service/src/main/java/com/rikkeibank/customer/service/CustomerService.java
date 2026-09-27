package com.rikkeibank.customer.service;

import com.rikkeibank.common.error.BusinessException;
import com.rikkeibank.common.error.ResourceNotFoundException;
import com.rikkeibank.customer.dto.CustomerDtos.AccountTypeRequest;
import com.rikkeibank.customer.dto.CustomerDtos.AccountTypeResponse;
import com.rikkeibank.customer.dto.CustomerDtos.CustomerRequest;
import com.rikkeibank.customer.dto.CustomerDtos.CustomerResponse;
import com.rikkeibank.customer.dto.CustomerDtos.StaffRequest;
import com.rikkeibank.customer.dto.CustomerDtos.StaffResponse;
import com.rikkeibank.customer.entity.AccountType;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.repository.CustomerRepositories.AccountTypeRepository;
import com.rikkeibank.customer.repository.CustomerRepositories.CustomerRepository;
import com.rikkeibank.customer.repository.CustomerRepositories.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Nghiệp vụ quản lý danh mục: Khách hàng (có cache), Nhân viên, Loại tài khoản. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    public static final String CUSTOMER_CACHE = "customers";
    public static final String CUSTOMER_LIST_CACHE = "customer-list";

    private static final AtomicLong CUSTOMER_SEQ = new AtomicLong(1000);
    private static final AtomicLong STAFF_SEQ = new AtomicLong(500);

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final AccountTypeRepository accountTypeRepository;

    // ==================== KHÁCH HÀNG ====================

    @Cacheable(cacheNames = CUSTOMER_LIST_CACHE, key = "'all'")
    public List<CustomerResponse> getAllCustomers() {
        log.info("Querying DB for all customers");
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    /** Cache-Aside: lần đầu truy vấn DB (thấy log "Querying DB for customer"), các lần sau lấy từ Redis. */
    @Cacheable(cacheNames = CUSTOMER_CACHE, key = "#id")
    public CustomerResponse getCustomerById(Long id) {
        log.info("Querying DB for customer id={}", id);
        return toResponse(findCustomer(id));
    }

    @Transactional
    @CacheEvict(cacheNames = CUSTOMER_LIST_CACHE, allEntries = true)
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByIdentityNumber(request.identityNumber())) {
            throw new BusinessException("IDENTITY_EXISTS", "Số CCCD/CMND đã tồn tại trong hệ thống");
        }
        Customer saved = customerRepository.save(Customer.builder()
                .customerCode("CUS" + CUSTOMER_SEQ.incrementAndGet())
                .fullName(request.fullName())
                .identityNumber(request.identityNumber())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .dateOfBirth(request.dateOfBirth())
                .status("ACTIVE")
                .build());
        log.info("[CUSTOMER] Đã tạo khách hàng id={} code={}", saved.getId(), saved.getCustomerCode());
        return toResponse(saved);
    }

    @Transactional
    @Caching(put = @CachePut(cacheNames = CUSTOMER_CACHE, key = "#id"),
            evict = @CacheEvict(cacheNames = CUSTOMER_LIST_CACHE, allEntries = true))
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        customer.setFullName(request.fullName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        customer.setDateOfBirth(request.dateOfBirth());
        log.info("[CUSTOMER] Cập nhật khách hàng id={}", id);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CUSTOMER_CACHE, key = "#id"),
            @CacheEvict(cacheNames = CUSTOMER_LIST_CACHE, allEntries = true)
    })
    public void deleteCustomer(Long id) {
        Customer customer = findCustomer(id);
        customerRepository.delete(customer);
        log.warn("[CUSTOMER] Đã xoá khách hàng id={}", id);
    }

    // ==================== NHÂN VIÊN ====================

    public List<StaffResponse> getAllStaffs() {
        return staffRepository.findAll().stream().map(this::toResponse).toList();
    }

    public StaffResponse getStaffById(Long id) {
        return toResponse(staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên id=" + id)));
    }

    @Transactional
    public StaffResponse createStaff(StaffRequest request) {
        if (staffRepository.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_EXISTS", "Email nhân viên đã tồn tại");
        }
        Staff saved = staffRepository.save(Staff.builder()
                .staffCode("STF" + STAFF_SEQ.incrementAndGet())
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .branch(request.branch())
                .position(request.position())
                .status("ACTIVE")
                .build());
        log.info("[STAFF] Đã tạo nhân viên id={} code={}", saved.getId(), saved.getStaffCode());
        return toResponse(saved);
    }

    @Transactional
    public StaffResponse updateStaff(Long id, StaffRequest request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên id=" + id));
        staff.setFullName(request.fullName());
        staff.setEmail(request.email());
        staff.setPhone(request.phone());
        staff.setBranch(request.branch());
        staff.setPosition(request.position());
        return toResponse(staffRepository.save(staff));
    }

    @Transactional
    public void deleteStaff(Long id) {
        staffRepository.delete(staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên id=" + id)));
    }

    // ==================== LOẠI TÀI KHOẢN ====================

    @Cacheable(cacheNames = "accountTypes", key = "'all'")
    public List<AccountTypeResponse> getAllAccountTypes() {
        log.info("Querying DB for all account types");
        return accountTypeRepository.findAll().stream().map(this::toResponse).toList();
    }

    public AccountTypeResponse getAccountType(Long id) {
        return toResponse(findAccountType(id));
    }

    @Transactional
    @CacheEvict(cacheNames = "accountTypes", allEntries = true)
    public AccountTypeResponse createAccountType(AccountTypeRequest request) {
        AccountType saved = accountTypeRepository.save(AccountType.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .minimumBalance(request.minimumBalance())
                .active(request.active() == null || request.active())
                .build());
        log.info("[ACCOUNT-TYPE] Đã tạo loại tài khoản id={} code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "accountTypes", allEntries = true)
    public AccountTypeResponse updateAccountType(Long id, AccountTypeRequest request) {
        AccountType type = findAccountType(id);
        type.setName(request.name());
        type.setDescription(request.description());
        type.setMinimumBalance(request.minimumBalance());
        if (request.active() != null) {
            type.setActive(request.active());
        }
        return toResponse(accountTypeRepository.save(type));
    }

    @Transactional
    @CacheEvict(cacheNames = "accountTypes", allEntries = true)
    public void deleteAccountType(Long id) {
        accountTypeRepository.delete(findAccountType(id));
    }

    // ==================== HELPERS ====================

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng id=" + id));
    }

    private AccountType findAccountType(Long id) {
        return accountTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại tài khoản id=" + id));
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getCustomerCode(), c.getFullName(), c.getIdentityNumber(),
                c.getEmail(), c.getPhone(), c.getAddress(), c.getDateOfBirth(), c.getStatus());
    }

    private StaffResponse toResponse(Staff s) {
        return new StaffResponse(s.getId(), s.getStaffCode(), s.getFullName(), s.getEmail(), s.getPhone(),
                s.getBranch(), s.getPosition(), s.getStatus());
    }

    private AccountTypeResponse toResponse(AccountType a) {
        return new AccountTypeResponse(a.getId(), a.getCode(), a.getName(), a.getDescription(),
                a.getMinimumBalance(), a.getActive());
    }
}
