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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(CustomTaskScheduler.class);

	private OAuth2TokenEntityService tokenEntityService;
	private ApprovedSiteService approvedSiteService;
	private DefaultOAuth2AuthorizationCodeService defaultOAuth2AuthorizationCodeService;
	private DeviceCodeService deviceCodeService;
	private PerunAcrRepository perunAcrRepository;
	private DataSource dataSource;

	@Autowired
	public void setTokenEntityService(OAuth2TokenEntityService tokenEntityService) {
		this.tokenEntityService = tokenEntityService;
	}

	@Autowired
	public void setApprovedSiteService(ApprovedSiteService approvedSiteService) {
		this.approvedSiteService = approvedSiteService;
	}

	@Autowired
	public void setAuthorizationCodeServices(DefaultOAuth2AuthorizationCodeService defaultOAuth2AuthorizationCodeService) {
		this.defaultOAuth2AuthorizationCodeService = defaultOAuth2AuthorizationCodeService;
	}

	@Autowired
	public void setDeviceCodeService(DeviceCodeService deviceCodeService) {
		this.deviceCodeService = deviceCodeService;
	}

	@Autowired
	public void setPerunAcrRepository(PerunAcrRepository perunAcrRepository) {
		this.perunAcrRepository = perunAcrRepository;
	}

	@Autowired
	public void setDataSource(@Qualifier("dataSource") DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public CustomTaskScheduler() {
		log.info("CustomTasksSchedulerCreated");
	}

	@Bean
	public LockProvider lockProvider() {
		return new JdbcTemplateLockProvider(this.dataSource);
	}

	@Scheduled(fixedDelay = 300000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredTokens")
	public void clearExpiredTokens() {
		log.debug("clearExpiredTokens");
		LockAssert.assertLocked();
		this.tokenEntityService.clearExpiredTokens();
		log.debug("clearExpiredTokens - ends");
	}

	@Scheduled(fixedDelay = 300000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredSites")
	public void clearExpiredSites() {
		log.debug("clearExpiredSites");
		LockAssert.assertLocked();
		this.approvedSiteService.clearExpiredSites();
		log.debug("clearExpiredSites - ends");
	}

	@Scheduled(fixedDelay = 300000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredAuthorizationCodes")
	public void clearExpiredAuthorizationCodes() {
		log.debug("clearExpiredAuthorizationCodes");
		LockAssert.assertLocked();
		this.defaultOAuth2AuthorizationCodeService.clearExpiredAuthorizationCodes();
		log.debug("clearExpiredAuthorizationCodes - ends");
	}

	@Scheduled(fixedDelay = 300000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredDeviceCodes")
	public void clearExpiredDeviceCodes() {
		log.debug("clearExpiredDeviceCodes");
		LockAssert.assertLocked();
		this.deviceCodeService.clearExpiredDeviceCodes();
		log.debug("clearExpiredDeviceCodes - ends");
	}

	@Scheduled(fixedDelay = 600000L, initialDelay = 600000L)
	@SchedulerLock(name = "clearExpiredAcrs")
	public void clearExpiredAcrs() {
		log.debug("clearExpiredAcrs");
		LockAssert.assertLocked();
		this.perunAcrRepository.deleteExpired();
		log.debug("clearExpiredAcrs - ends");
	}
}
