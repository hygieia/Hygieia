package com.capitalone.dashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class StandardAuthenticationProvider implements AuthenticationProvider {
	
	private AuthenticationService authenticationService;

	@Autowired
	public StandardAuthenticationProvider(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		Authentication authenticate = authenticationService.authenticate(authentication.getName(), authentication.getCredentials().toString());
		return authenticate;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}

}
