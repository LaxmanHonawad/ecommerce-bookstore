package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.Address;
import com.bookworm.repository.AddressRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {
    private final AddressRepository addresses;
    private final Mapper mapper;

    @Transactional(readOnly = true)
    public List<AddressView> list(Long userId) {
        return addresses.findByUserIdOrderByIdDesc(userId).stream().map(mapper::address).toList();
    }

    public AddressView create(Long userId, AddressRequest r) {
        Address a = new Address();
        a.setUserId(userId);
        apply(a, r);
        return mapper.address(addresses.save(a));
    }

    public AddressView update(Long userId, Long id, AddressRequest r) {
        Address a = addresses.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Address not found"));
        apply(a, r);
        return mapper.address(a);
    }

    public void delete(Long userId, Long id) {
        Address a = addresses.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Address not found"));
        addresses.delete(a);
    }

    private void apply(Address a, AddressRequest r) {
        a.setFirstName(r.firstName().trim());
        a.setLastName(r.lastName().trim());
        a.setEmail(r.email());
        a.setPhone(r.phone());
        a.setLine1(r.line1().trim());
        a.setLine2(r.line2());
        a.setCity(r.city().trim());
        a.setPin(r.pin());
        a.setState(r.state().trim());
        a.setCountry(r.country().trim());
    }
}
