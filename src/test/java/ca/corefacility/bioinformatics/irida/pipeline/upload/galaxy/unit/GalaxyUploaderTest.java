package ca.corefacility.bioinformatics.irida.pipeline.upload.galaxy.unit;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.validation.ConstraintViolationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sun.jersey.api.client.ClientHandlerException;

import static org.mockito.Mockito.*;

import ca.corefacility.bioinformatics.irida.exceptions.UploadException;
import ca.corefacility.bioinformatics.irida.exceptions.UploadConnectionException;
import ca.corefacility.bioinformatics.irida.model.upload.UploadObjectName;
import ca.corefacility.bioinformatics.irida.model.upload.UploadSample;
import ca.corefacility.bioinformatics.irida.model.upload.UploaderAccountName;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyAccountEmail;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyObjectName;
import ca.corefacility.bioinformatics.irida.pipeline.upload.galaxy.GalaxyAPI;
import ca.corefacility.bioinformatics.irida.pipeline.upload.galaxy.GalaxyUploader;

public class GalaxyUploaderTest {
	private URL galaxyURL;
	private GalaxyAccountEmail accountEmail;
	private String adminApiKey;

	@Mock
	private GalaxyAPI galaxyAPI;

	@Before
	public void setup() throws MalformedURLException {
		MockitoAnnotations.initMocks(this);

		galaxyURL = new URL("http://localhost");
		accountEmail = new GalaxyAccountEmail("admin@localhost");
		adminApiKey = "0";
	}

	@SuppressWarnings("unchecked")
	@Test(expected = UploadConnectionException.class)
	public void testUploadGalaxyConnectionFail()
			throws ConstraintViolationException, UploadException {
		GalaxyUploader galaxyUploader = new GalaxyUploader(galaxyAPI);

		when(
				galaxyAPI.uploadSamples(any(ArrayList.class),
						any(GalaxyObjectName.class),
						any(GalaxyAccountEmail.class))).thenThrow(
				new ClientHandlerException("error connecting"));

		galaxyUploader.uploadSamples(new ArrayList<UploadSample>(),
				new GalaxyObjectName("lib"), accountEmail);
	}

	@Test(expected = NullPointerException.class)
	public void testSetupGalaxyNoURL() throws ConstraintViolationException,
			UploadException {
		GalaxyUploader galaxyUploader = new GalaxyUploader();
		galaxyUploader.setupGalaxyAPI(null, accountEmail, adminApiKey);
	}

	@Test(expected = NullPointerException.class)
	public void testSetupGalaxyAccountEmail()
			throws ConstraintViolationException, UploadException {
		GalaxyUploader galaxyUploader = new GalaxyUploader();
		galaxyUploader.setupGalaxyAPI(galaxyURL, null, adminApiKey);
	}

	@Test(expected = NullPointerException.class)
	public void testSetupGalaxyNoApiKey() throws ConstraintViolationException,
			UploadException {
		GalaxyUploader galaxyUploader = new GalaxyUploader();
		galaxyUploader.setupGalaxyAPI(galaxyURL, accountEmail, null);
	}

	@Test(expected = UploadException.class)
	public void testUploadWithInvalidAccountName()
			throws ConstraintViolationException, UploadException {
		GalaxyUploader galaxyUploader = new GalaxyUploader(galaxyAPI);

		UploaderAccountName invalidNameType = new UploaderAccountName() {
			@Override
			public String getName() {
				return "test";
			}
		};
		galaxyUploader.uploadSamples(new ArrayList<UploadSample>(),
				new GalaxyObjectName("lib"), invalidNameType);
	}

	@Test(expected = UploadException.class)
	public void testUploadWithInvalidDataLibraryObject()
			throws ConstraintViolationException, UploadException {
		GalaxyUploader galaxyUploader = new GalaxyUploader(galaxyAPI);

		UploadObjectName invalidLibraryType = new UploadObjectName() {
			@Override
			public String getName() {
				return "test";
			}
		};
		galaxyUploader.uploadSamples(new ArrayList<UploadSample>(),
				invalidLibraryType, accountEmail);
	}
}