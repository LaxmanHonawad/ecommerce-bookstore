package com.bookworm.controller;

import com.bookworm.dto.Dtos.*;
import com.bookworm.model.User;
import com.bookworm.security.RequireAuth;
import com.bookworm.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Member", description = "Registration, login, profile and delivery addresses")
public class MemberController {
    private final AuthService auth;
    private final AddressService addresses;

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @RequireAuth
    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestAttribute("token") String token) { auth.logout(token); }

    @RequireAuth
    @GetMapping("/users/me")
    public UserView me(@RequestAttribute("user") User user) { return auth.me(user.getId()); }

    @RequireAuth
    @GetMapping("/users/me/addresses")
    public List<AddressView> addresses(@RequestAttribute("user") User user) { return addresses.list(user.getId()); }

    @RequireAuth
    @PostMapping("/users/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressView addAddress(@RequestAttribute("user") User user, @Valid @RequestBody AddressRequest r) {
        return addresses.create(user.getId(), r);
    }

    @RequireAuth
    @PutMapping("/users/me/addresses/{id}")
    public AddressView updateAddress(@RequestAttribute("user") User user, @PathVariable Long id, @Valid @RequestBody AddressRequest r) {
        return addresses.update(user.getId(), id, r);
    }

    @RequireAuth
    @DeleteMapping("/users/me/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@RequestAttribute("user") User user, @PathVariable Long id) { addresses.delete(user.getId(), id); }
}
