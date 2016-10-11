package ca.corefacility.bioinformatics.irida.ria.integration.projects;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.support.PageFactory;

import com.github.springtestdbunit.annotation.DatabaseSetup;

import ca.corefacility.bioinformatics.irida.ria.integration.AbstractIridaUIITChromeDriver;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.LoginPage;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.projects.ProjectSampleMetadataImportPage;

@DatabaseSetup("/ca/corefacility/bioinformatics/irida/ria/web/projects/ProjectSampleMetadataView.xml")
public class ProjectSampleMetadataImportPageIT extends AbstractIridaUIITChromeDriver {
	private static final String GOOD_PATH = "src/test/resources/files/metadata-upload/good.xlsx";
	ProjectSampleMetadataImportPage page;

	@Before
	public void init() {
		LoginPage.loginAsManager(driver());
		page = PageFactory.initElements(driver(), ProjectSampleMetadataImportPage.class);
	}

	@Test
	public void testGoodFileAndHeaders() {
		page.uploadMetadataFile(GOOD_PATH);
		page.selectSampleNameColumn();
		assertEquals("Has incorrect amount of rows matching sample names", 5, page.getFoundCount());
	}
}
