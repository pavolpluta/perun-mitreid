package cz.muni.ics.oidc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunAuthenticationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthenticationUserDetailsService.class);

	private static GrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");
	private static GrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

	private List<Long> adminIds = new ArrayList<>();
	public void setAdmins(List<String> admins) {
		log.trace("setAdmins({})",admins);
		for (String id : admins) {
			long l = Long.parseLong(id);
			adminIds.add(l);
			log.debug("added admin {}",l);
		}
	}

	@Override
	public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
		log.trace("loadUserDetails({})",token);
		if (token.getPrincipal() == null) {
			throw new UsernameNotFoundException("User id is null");
		}
		Collection<GrantedAuthority> authorities = new ArrayList<>();

		authorities.add(ROLE_USER);
		//noinspection SuspiciousMethodCalls
		if(adminIds.contains(token.getPrincipal())) {
			 authorities.add(ROLE_ADMIN);
			 log.debug("user {} is admin",token.getPrincipal());
		}
		return new User(token.getPrincipal().toString(), "no password", authorities);
	}
}
