package cz.muni.ics.oidc.server;

import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.filters.PerunAuthenticationFilter;
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
 * Authentication user detail service. Specific for Perun.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunAuthenticationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthenticationUserDetailsService.class);

	private static GrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");
	private static GrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

	private PerunAdapter perunAdapter;

	public void setPerunAdapter(PerunAdapter perunAdapter) {
		this.perunAdapter = perunAdapter;
	}

	private List<Long> adminIds = new ArrayList<>();

	public void setAdmins(List<String> admins) {
		for (String id : admins) {
			long l = Long.parseLong(id);
			adminIds.add(l);
			log.debug("added user {} as admin", l);
		}
	}

	public PerunAuthenticationUserDetailsService() {
		log.info("initialized");
	}

	/**
	 * Verifies principal extracted by {@link PerunAuthenticationFilter} in Perun or throws exception.
	 *
	 * @param token token with {@link PerunPrincipal} extracted by {@link PerunAuthenticationFilter}
	 * @return UserDetails with username set to Perun user id
	 * @throws UsernameNotFoundException when user is not verified in Perun
	 */
	@Override
	public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
		PerunPrincipal perunPrincipal = (PerunPrincipal) token.getPrincipal();
		if (perunPrincipal == null) {
			throw new UsernameNotFoundException("PerunPrincipal is null");
		}
		try {
			PerunUser perunUser = perunAdapter.getPreauthenticatedUserId(perunPrincipal);
			Long userId = perunUser.getId();
			log.info("User {} {} {} logged in", userId, perunUser.getFirstName(), perunUser.getLastName());

			log.trace("setting user role for {}", userId);
			Collection<GrantedAuthority> authorities = new ArrayList<>();
			authorities.add(ROLE_USER);
			if (adminIds.contains(userId)) {
				authorities.add(ROLE_ADMIN);
				log.debug("adding admin role for user {} ({})", userId, perunPrincipal);
			}
			//returns username and list of roles, the place to check for expired accounts etc
			return new User(Long.toString(userId), "no password", authorities);
		} catch (Exception ex) {
			log.error("Cannot authenticate user for principal " + perunPrincipal, ex);
			throw new UsernameNotFoundException("user not found in Perun", ex);
		}
	}
}
