package com.example.calculator.hooks;

import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.example.calculator.factory.WebDriverFactory;
import com.example.calculator.utils.AllureUtil;
import com.example.calculator.utils.ConfigReader;
import com.example.calculator.utils.LoggerUtil;
import com.example.calculator_manager.DriverManager;
import com.google.common.collect.ImmutableMap;

import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import java.net.URL;
import java.net.MalformedURLException;

public class Hooks {

	public static WebDriver driver;
	private AllureUtil allureUtil;
	private static final Logger logger = LoggerUtil.getLogger(Hooks.class);

	@Before
	public void setUp(Scenario scenario) throws MalformedURLException {

		// 1) Load environment
		String env = System.getProperty("env", System.getenv().getOrDefault("ENV", "dev"));
		ConfigReader.loadProperties(env);

		// 2) Browser & headless settings
		String browser = System.getProperty("browser",
				System.getenv().getOrDefault("BROWSER", ConfigReader.get("BROWSER")));

		boolean headless = Boolean.parseBoolean(System.getProperty("headless",
				System.getenv().getOrDefault("HEADLESS", ConfigReader.get("HEADLESS"))));

		// 3) Selenium Hub URL
		String seleniumHub = System.getProperty("selenium.hub",
				System.getenv().getOrDefault("SELENIUM_HUB", "http://localhost:4444/wd/hub"));

		// 4) Base URL of app under test
		String baseUrl = System.getProperty("baseUrl",
				System.getenv().getOrDefault("APP_URL", ConfigReader.get("APP_URL")));

		// 5) Browser capabilities
		DesiredCapabilities options = new DesiredCapabilities();
		options.setBrowserName(browser.toLowerCase());
		options.setCapability("se:headless", headless);

		// 6) RemoteWebDriver connection to selenium-node
		driver = new RemoteWebDriver(new URL(seleniumHub), options);
		driver.manage().window().setSize(new Dimension(1920, 1080));
		driver.get(baseUrl);

		DriverManager.setDriver(driver);

		// 7) Allure environment
		allureUtil = new AllureUtil(driver);
		allureUtil.writeAllureEnvironment(
				ImmutableMap.<String, String>builder()
						.put("OS", System.getProperty("os.name"))
						.put("Browser", browser)
						.put("Headless", String.valueOf(headless))
						.put("Environment", env)
						.put("BaseUrl", baseUrl)
						.put("SeleniumHub", seleniumHub)
						.build());

		logger.info("Starting scenario: {}", scenario.getName());
		logger.info("Config → env={}, browser={}, headless={}, baseUrl={}, hub={}",
				env, browser, headless, baseUrl, seleniumHub);
	}

	@After(order = 0)
	public void tearDown() {
		if (driver != null) {
			driver.quit();
		}
		logger.info("Closing the browser.");
	}

	@After(order = 1)
	public void captureFailure(Scenario scenario) {
		if (scenario.isFailed() && allureUtil != null) {
			allureUtil.captureAndAttachScreenshot();
		}
	}

	@AfterStep
	public void afterEachStep(Scenario scenario) {
		boolean everyStep = Boolean.parseBoolean(System.getProperty(
				"screenshotEveryStep",
				System.getenv().getOrDefault("SCREENSHOT_EVERY_STEP", "false")));
		if (everyStep && allureUtil != null) {
			allureUtil.captureAndAttachScreenshot();
		}
	}
}