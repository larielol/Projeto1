package com.vitral.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.vitral.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return accountRepository.findByEmailAndAtivoTrue(email)
                .map(AccountUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Conta nao encontrada: " + email));
    }
}
