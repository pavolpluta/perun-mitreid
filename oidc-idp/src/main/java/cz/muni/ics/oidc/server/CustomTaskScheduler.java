package cz.muni.ics.oidc.server;

import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.mitre.oauth2.service.DeviceCodeService;
import org.mitre.oauth2.service.OAuth2TokenEntityService;
import org.mitre.oauth2.service.impl.DefaultOAuth2AuthorizationCodeService;
import org.mitre.openid.connect.service.ApprovedSiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;

/**
 * A custom scheduler for tasks with usage of ShedLock.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30s")
public class CustomTaskScheduler {

	private final OAuth2TokenEntityService tokenEntityService;
	private final ApprovedSiteService approvedSiteService;
	private final DefaultOAuth2AuthorizationCodeService defaultOAuth2AuthorizationCodeService;
	private final DeviceCodeService deviceCodeService;
	private final PerunAcrRepository perunAcrRepository;
	private final DataSource dataSource;

	@Autowired
	public CustomTaskScheduler(OAuth2TokenEntityService tokenEntityService, ApprovedSiteService approvedSiteService,
							   DefaultOAuth2AuthorizationCodeService defaultOAuth2AuthorizationCodeService,
							   DeviceCodeService deviceCodeService, PerunAcrRepository perunAcrRepository,
							   @Qualifier("dataSource") DataSource dataSource) {
		this.tokenEntityService = tokenEntityService;
		this.approvedSiteService = approvedSiteService;
		this.defaultOAuth2AuthorizationCodeService = defaultOAuth2AuthorizationCodeService;
		this.deviceCodeService = deviceCodeService;
		this.perunAcrRepository = perunAcrRepository;
		this.dataSource = dataSource;
	}

	@Bean
	public LockProvider lockProvider() {
		return new JdbcTemplateLockProvider(this.dataSource);
	}

	@Scheduled(fixedDelay = 300000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredTokens")
	public void clearExpiredTokens() {
		LockAssert.assertLocked();
		this.tokenEntityService.clearExpiredTokens();
	}

	@Scheduled(fixedDelay = 300000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredSites")
	public void clearExpiredSites() {
		LockAssert.assertLocked();
		this.approvedSiteService.clearExpiredSites();
	}

	@Scheduled(fixedDelay = 300000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredAuthorizationCodes")
	public void clearExpiredAuthorizationCodes() {
		LockAssert.assertLocked();
		this.defaultOAuth2AuthorizationCodeService.clearExpiredAuthorizationCodes();
	}

	@Scheduled(fixedDelay = 300000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredDeviceCodes")
	public void clearExpiredDeviceCodes() {
		LockAssert.assertLocked();
		this.deviceCodeService.clearExpiredDeviceCodes();
	}

	@Scheduled(fixedDelay = 600000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredAcrs")
	public void clearExpiredAcrs() {
		LockAssert.assertLocked();
		this.perunAcrRepository.deleteExpired();
	}

}
