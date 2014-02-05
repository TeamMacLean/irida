package ca.corefacility.bioinformatics.irida.pipeline.upload.galaxy;

import static com.google.common.base.Preconditions.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.corefacility.bioinformatics.irida.exceptions.galaxy.ChangeLibraryPermissionsException;
import ca.corefacility.bioinformatics.irida.exceptions.galaxy.CreateLibraryException;
import ca.corefacility.bioinformatics.irida.exceptions.galaxy.GalaxyConnectException;
import ca.corefacility.bioinformatics.irida.exceptions.galaxy.GalaxyUserNoRoleException;
import ca.corefacility.bioinformatics.irida.exceptions.galaxy.GalaxyUserNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.galaxy.LibraryUploadException;
import ca.corefacility.bioinformatics.irida.exceptions.galaxy.NoLibraryFoundException;
import ca.corefacility.bioinformatics.irida.model.upload.UploadSample;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyAccountEmail;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyFolderPath;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyObjectName;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyUploadResult;
import ca.corefacility.bioinformatics.irida.pipeline.upload.Uploader;
import ca.corefacility.bioinformatics.irida.pipeline.upload.Uploader.DataStorage;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.LibrariesClient;
import com.github.jmchilton.blend4j.galaxy.beans.FilesystemPathsLibraryUpload;
import com.github.jmchilton.blend4j.galaxy.beans.Library;
import com.github.jmchilton.blend4j.galaxy.beans.LibraryContent;
import com.github.jmchilton.blend4j.galaxy.beans.LibraryFolder;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;

public class GalaxyAPI {
	private static final Logger logger = LoggerFactory
			.getLogger(GalaxyAPI.class);

	private static final GalaxyObjectName ILLUMINA_FOLDER_NAME = new GalaxyObjectName(
			"illumina_reads");
	private static final GalaxyObjectName REFERENCES_FOLDER_NAME = new GalaxyObjectName(
			"references");
	private static final GalaxyFolderPath ILLUMINA_FOLDER_PATH = new GalaxyFolderPath(
			"/illumina_reads");
	private static final GalaxyFolderPath REFERENCES_FOLDER_PATH = new GalaxyFolderPath(
			"/references");

	private GalaxyInstance galaxyInstance;
	private GalaxyAccountEmail adminEmail;
	private GalaxySearch galaxySearchAdmin;
	private GalaxyLibraryBuilder galaxyLibrary;
	private Uploader.DataStorage dataStorage = Uploader.DataStorage.REMOTE;

	/**
	 * Builds a new GalaxyAPI instance with the given information.
	 * 
	 * @param galaxyURL
	 *            The URL to the Galaxy instance.
	 * @param adminEmail
	 *            An administrators email address for the Galaxy instance.
	 * @param adminAPIKey
	 *            A corresponding administrators API key for the Galaxy
	 *            instance.
	 * @throws ConstraintViolationException
	 *             If the adminEmail is invalid.
	 * @throws GalaxyConnectException
	 *             If an error occred when connecting to Galaxy.
	 */
	public GalaxyAPI(URL galaxyURL, @Valid GalaxyAccountEmail adminEmail,
			String adminAPIKey) throws ConstraintViolationException,
			GalaxyConnectException {
		checkNotNull(galaxyURL, "galaxyURL is null");
		checkNotNull(adminEmail, "adminEmail is null");
		checkNotNull(adminAPIKey, "apiKey is null");

		galaxyInstance = GalaxyInstanceFactory.get(galaxyURL.toString(),
				adminAPIKey);
		this.adminEmail = adminEmail;

		if (galaxyInstance == null) {
			throw new RuntimeException(
					"Could not create GalaxyInstance with URL=" + galaxyURL
							+ ", adminEmail=" + adminEmail);
		}

		galaxySearchAdmin = new GalaxySearch(galaxyInstance);
		galaxyLibrary = new GalaxyLibraryBuilder(galaxyInstance,
				galaxySearchAdmin);

		try {
			if (!galaxySearchAdmin.galaxyUserExists(adminEmail)) {
				throw new GalaxyConnectException(
						"Could not create GalaxyInstance with URL=" + galaxyURL
								+ ", adminEmail=" + adminEmail);
			}
		} catch (ClientHandlerException e) {
			throw new GalaxyConnectException(e);
		}
	}

	/**
	 * Builds a GalaxyAPI object with the given information.
	 * 
	 * @param galaxyInstance
	 *            A GalaxyInstance object pointing to the correct Galaxy
	 *            location.
	 * @param adminEmail
	 *            The administrators email address for the corresponding API key
	 *            within the GalaxyInstance.
	 * @throws ConstraintViolationException
	 *             If the adminEmail is invalid.
	 * @throws GalaxyConnectException
	 *             If an issue connecting to Galaxy occurred.
	 */
	public GalaxyAPI(GalaxyInstance galaxyInstance,
			@Valid GalaxyAccountEmail adminEmail)
			throws ConstraintViolationException, GalaxyConnectException {
		checkNotNull(galaxyInstance, "galaxyInstance is null");
		checkNotNull(adminEmail, "adminEmail is null");

		this.galaxyInstance = galaxyInstance;
		this.adminEmail = adminEmail;

		galaxySearchAdmin = new GalaxySearch(galaxyInstance);
		galaxyLibrary = new GalaxyLibraryBuilder(galaxyInstance,
				galaxySearchAdmin);

		try {
			if (!galaxySearchAdmin.galaxyUserExists(adminEmail)) {
				throw new GalaxyConnectException(
						"Could not create GalaxyInstance with URL="
								+ galaxyInstance.getGalaxyUrl()
								+ ", adminEmail=" + adminEmail);
			}
		} catch (ClientHandlerException e) {
			throw new GalaxyConnectException(e);
		}
	}

	/**
	 * Builds a GalaxyAPI object with the given information.
	 * 
	 * @param galaxyInstance
	 *            A GalaxyInstance object pointing to the correct Galaxy
	 *            location.
	 * @param adminEmail
	 *            The administrators email address for the corresponding API key
	 *            within the GalaxyInstance.
	 * @param dataStorage
	 *            If uploaded files will exist on the same or a separate
	 *            filesystem as the archive.
	 * @param galaxySearch
	 *            A GalaxySearch object.
	 * @param galaxyLibrary
	 *            A GalaxyLibrary object.
	 * @throws ConstraintViolationException
	 *             If the adminEmail is invalid.
	 * @throws GalaxyConnectException
	 *             If an issue connecting to Galaxy occurred.
	 */
	public GalaxyAPI(GalaxyInstance galaxyInstance,
			@Valid GalaxyAccountEmail adminEmail, GalaxySearch galaxySearch,
			GalaxyLibraryBuilder galaxyLibrary)
			throws ConstraintViolationException, GalaxyConnectException {
		checkNotNull(galaxyInstance, "galaxyInstance is null");
		checkNotNull(adminEmail, "adminEmail is null");
		checkNotNull(galaxySearch, "galaxySearch is null");
		checkNotNull(galaxyLibrary, "galaxyLibrary is null");

		this.galaxyInstance = galaxyInstance;
		this.adminEmail = adminEmail;

		this.galaxyLibrary = galaxyLibrary;
		this.galaxySearchAdmin = galaxySearch;

		try {
			if (!galaxySearch.galaxyUserExists(adminEmail)) {
				throw new GalaxyConnectException(
						"Could not use GalaxyInstance with URL="
								+ galaxyInstance.getGalaxyUrl()
								+ ", adminEmail=" + adminEmail);
			}
		} catch (ClientHandlerException e) {
			throw new GalaxyConnectException(e);
		}
	}

	/**
	 * Builds a data library in Galaxy with the name and owner.
	 * 
	 * @param libraryName
	 *            The name of the library to create.
	 * @param galaxyUserEmail
	 *            The name of the user who will own the galaxy library.
	 * @return A Library object for the library just created.
	 * @throws ConstraintViolationException
	 *             If the galaxyUserEmail or libraryName are invalid.
	 * @throws CreateLibraryException
	 *             If there was an error building a library (assuming Spring is
	 *             managing the API object).
	 * @throws ChangeLibraryPermissionsException
	 *             If an error occured while attempting to change the library
	 *             permissions.
	 * @throws GalaxyUserNotFoundException
	 *             If the passed Galaxy user does not exist.
	 * @throws GalaxyUserNoRoleException
	 *             If the passed Galaxy user has no role.
	 */
	public Library buildGalaxyLibrary(@Valid GalaxyObjectName libraryName,
			@Valid GalaxyAccountEmail galaxyUserEmail)
			throws CreateLibraryException, ConstraintViolationException,
			ChangeLibraryPermissionsException, GalaxyUserNotFoundException,
			GalaxyUserNoRoleException {
		checkNotNull(libraryName, "libraryName is null");
		checkNotNull(galaxyUserEmail, "galaxyUser is null");

		logger.debug("Attempt to create new library=" + libraryName
				+ " owned by user=" + galaxyUserEmail + " under Galaxy url="
				+ galaxyInstance.getGalaxyUrl());

		// make sure user exists and has a role before we create an empty
		// library
		if (!galaxySearchAdmin.galaxyUserExists(galaxyUserEmail)) {
			throw new GalaxyUserNotFoundException(
					"Could not find Galaxy user with email=" + galaxyUserEmail);
		}

		if (galaxySearchAdmin.findUserRoleWithEmail(galaxyUserEmail) == null) {
			throw new GalaxyUserNoRoleException(
					"Could not find role for Galaxy user with email="
							+ galaxyUserEmail);
		}

		Library library = galaxyLibrary.buildEmptyLibrary(libraryName);

		return galaxyLibrary.changeLibraryOwner(library, galaxyUserEmail,
				adminEmail);
	}

	private ClientResponse uploadFile(LibraryFolder folder, File file,
			LibrariesClient librariesClient, Library library) {
		FilesystemPathsLibraryUpload upload = new FilesystemPathsLibraryUpload();
		upload.setFolderId(folder.getId());

		upload.setContent(file.getAbsolutePath());
		upload.setName(file.getName());
		upload.setLinkData(DataStorage.LOCAL.equals(dataStorage));

		return librariesClient.uploadFilesystemPathsRequest(library.getId(),
				upload);
	}

	private String samplePath(LibraryFolder rootFolder, UploadSample sample) {
		String rootFolderName;
		if (rootFolder.getName().startsWith("/")) {
			rootFolderName = rootFolder.getName().substring(1);
		} else {
			rootFolderName = rootFolder.getName();
		}

		return String.format("/%s/%s", rootFolderName, sample.getSampleName());
	}

	private String samplePath(LibraryFolder rootFolder, UploadSample sample,
			File file) {
		String rootFolderName;
		if (rootFolder.getName().startsWith("/")) {
			rootFolderName = rootFolder.getName().substring(1);
		} else {
			rootFolderName = rootFolder.getName();
		}

		return String.format("/%s/%s/%s", rootFolderName,
				sample.getSampleName(), file.getName());
	}

	private boolean uploadSample(UploadSample sample, LibraryFolder rootFolder,
			LibrariesClient librariesClient, Library library,
			Map<String, LibraryContent> libraryMap, String errorSuffix)
			throws LibraryUploadException, CreateLibraryException {
		boolean success = false;
		LibraryFolder persistedSampleFolder;

		String expectedSamplePath = samplePath(rootFolder, sample);

		// if Galaxy already contains a folder for this sample, don't create a
		// new folder
		if (libraryMap.containsKey(expectedSamplePath)) {
			LibraryContent persistedSampleFolderAsContent = libraryMap
					.get(expectedSamplePath);

			persistedSampleFolder = new LibraryFolder();
			persistedSampleFolder.setId(persistedSampleFolderAsContent.getId());
			persistedSampleFolder.setName(persistedSampleFolderAsContent
					.getName());
		} else {
			persistedSampleFolder = galaxyLibrary.createLibraryFolder(library,
					rootFolder, sample.getSampleName());

			logger.debug("Created Galaxy sample folder name="
					+ expectedSamplePath + " id="
					+ persistedSampleFolder.getId() + " in library name="
					+ library.getName() + " id=" + library.getId()
					+ " in Galaxy url=" + galaxyInstance.getGalaxyUrl());
		}

		success = true;

		for (Path path : sample.getSampleFiles()) {
			File file = path.toFile();
			String sampleFilePath = samplePath(rootFolder, sample, file);

			if (libraryMap.containsKey(sampleFilePath)) {
				logger.debug("File from local path=" + file.getAbsolutePath()
						+ " alread exists on Galaxy path="
						+ samplePath(rootFolder, sample, file)
						+ " in library name=" + library.getName() + " id="
						+ library.getId() + " in Galaxy url="
						+ galaxyInstance.getGalaxyUrl() + " skipping upload");
			} else {
				ClientResponse uploadResponse = uploadFile(
						persistedSampleFolder, file, librariesClient, library);

				success &= ClientResponse.Status.OK.equals(uploadResponse
						.getClientResponseStatus());

				if (success) {
					logger.debug("Uploaded file to Galaxy path="
							+ samplePath(rootFolder, sample, file)
							+ " from local path=" + file.getAbsolutePath()
							+ " dataStorage=" + dataStorage
							+ " in library name=" + library.getName() + " id="
							+ library.getId() + " in Galaxy url="
							+ galaxyInstance.getGalaxyUrl());
				}
			}
		}

		return success;
	}

	/**
	 * Uploads the given list of samples to the passed Galaxy library with the
	 * passed Galaxy user.
	 * 
	 * @param samples
	 *            The set of samples to upload.
	 * @param libraryName
	 *            The name of the library to upload to.
	 * @param galaxyUserEmail
	 *            The name of the Galaxy user who should own the files.
	 * @return A GalaxyUploadResult containing information about the location of
	 *         the uploaded files.
	 * @throws LibraryUploadException
	 *             If an error occurred.
	 * @throws CreateLibraryException
	 *             If there was an error creating the folder structure for the
	 *             library.
	 * @throws ConstraintViolationException
	 *             If the samples, libraryName or galaxyUserEmail are invalid
	 *             (assumes this object is managed by Spring).
	 * @throws ChangeLibraryPermissionsException
	 *             If an error occurred while attempting to change the library
	 *             permissions.
	 * @throws GalaxyUserNotFoundException
	 *             If the passed Galaxy user does not exist.
	 * @throws NoLibraryFoundException
	 *             If no library with the given name can be found.
	 * @throws GalaxyUserNoRoleException
	 *             If the passed Galaxy user has no associated role.
	 */
	public GalaxyUploadResult uploadSamples(@Valid List<UploadSample> samples,
			@Valid GalaxyObjectName libraryName,
			@Valid GalaxyAccountEmail galaxyUserEmail)
			throws LibraryUploadException, CreateLibraryException,
			ConstraintViolationException, ChangeLibraryPermissionsException,
			GalaxyUserNotFoundException, NoLibraryFoundException,
			GalaxyUserNoRoleException {
		checkNotNull(libraryName, "libraryName is null");
		checkNotNull(samples, "samples is null");
		checkNotNull(galaxyUserEmail, "galaxyUserEmail is null");

		GalaxyUploadResult galaxyUploadResult = null;
		GalaxyAccountEmail returnedOwner = null;

		Library uploadLibrary;
		if (galaxySearchAdmin.galaxyUserExists(galaxyUserEmail)) {
			List<Library> libraries = galaxySearchAdmin
					.findLibraryWithName(libraryName);

			if (libraries == null || libraries.size() <= 0) {
				uploadLibrary = buildGalaxyLibrary(libraryName, galaxyUserEmail);
				returnedOwner = galaxyUserEmail;
			} else {
				// get very first matching library
				uploadLibrary = libraries.get(0);
			}

			if (uploadFilesToLibrary(samples, uploadLibrary.getId())) {
				try {
					galaxyUploadResult = new GalaxyUploadResult(uploadLibrary,
							libraryName, returnedOwner,
							galaxyInstance.getGalaxyUrl());
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}

				return galaxyUploadResult;
			} else {
				throw new LibraryUploadException(
						"Could upload files to library " + libraryName + "id="
								+ uploadLibrary.getId()
								+ " in instance of galaxy with url="
								+ galaxyInstance.getGalaxyUrl());
			}
		} else {
			throw new GalaxyUserNotFoundException("Galaxy user with email "
					+ galaxyUserEmail + " does not exist");
		}
	}

	/**
	 * Uploads the passed set of files to a Galaxy library.
	 * 
	 * @param samples
	 *            The samples to upload to Galaxy.
	 * @param libraryID
	 *            A unique ID for the library, generated from
	 *            buildGalaxyLibrary(String)
	 * @return True if the files have been uploaded, false otherwise.
	 * @throws LibraryUploadException
	 *             If there was an error uploading files to the library.
	 * @throws ConstraintViolationException
	 *             If one of the GalaxySamples is invalid (assumes this object
	 *             is managed by Spring).
	 * @throws NoLibraryFoundException
	 *             If no library could be found with the given id.
	 * @throws CreateLibraryException
	 *             If an error occurred while attempting to build the data
	 *             library.
	 */
	public boolean uploadFilesToLibrary(@Valid List<UploadSample> samples,
			String libraryID) throws LibraryUploadException,
			ConstraintViolationException, NoLibraryFoundException,
			CreateLibraryException {
		checkNotNull(samples, "samples are null");
		checkNotNull(libraryID, "libraryID is null");

		boolean success = true;

		if (samples.size() > 0) {
			String errorSuffix = " in instance of galaxy with url="
					+ galaxyInstance.getGalaxyUrl();

			LibrariesClient librariesClient = galaxyInstance
					.getLibrariesClient();

			Library library = galaxySearchAdmin.findLibraryWithId(libraryID);
			if (library == null) {
				throw new NoLibraryFoundException(
						"Could not find library with id=" + libraryID);
			}

			Map<String, LibraryContent> libraryContentMap = galaxySearchAdmin
					.libraryContentAsMap(libraryID);
			LibraryFolder illuminaFolder;

			LibraryContent illuminaContent = galaxySearchAdmin
					.findLibraryContentWithId(libraryID, ILLUMINA_FOLDER_PATH);

			if (illuminaContent == null) {
				illuminaFolder = galaxyLibrary.createLibraryFolder(library,
						ILLUMINA_FOLDER_NAME);
			} else {
				illuminaFolder = new LibraryFolder();
				illuminaFolder.setId(illuminaContent.getId());
				illuminaFolder.setName(illuminaContent.getName());
			}

			// create references folder if it doesn't exist, but we don't need
			// to put anything into it.
			LibraryContent referenceContent = galaxySearchAdmin
					.findLibraryContentWithId(libraryID, REFERENCES_FOLDER_PATH);
			if (referenceContent == null) {
				galaxyLibrary.createLibraryFolder(library,
						REFERENCES_FOLDER_NAME);
			}

			for (UploadSample sample : samples) {
				if (sample != null) {
					success &= uploadSample(sample, illuminaFolder,
							librariesClient, library, libraryContentMap,
							errorSuffix);
				} else {
					throw new LibraryUploadException(
							"Cannot upload a null sample" + errorSuffix);
				}
			}
		}

		return success;
	}

	/**
	 * The type of data storage the remote site has. Determines whether or not
	 * files should be linked within Galaxy or a duplicate should be uploaded.
	 * 
	 * @param dataStorage
	 *            DataStorage.REMOTE if there is no shared filesystem between
	 *            the archive and the remote site, or DataStorage.LOCAL if there
	 *            is a shared filesystem.
	 */
	public void setDataStorage(DataStorage dataStorage) {
		this.dataStorage = dataStorage;
	}

	/**
	 * The type of data storage the remote site has. Determines whether or not
	 * files should be linked within Galaxy or a duplicate should be uploaded.
	 * 
	 * @return DataStorage.REMOTE if there is no shared filesystem between the
	 *         archive and the remote site, or DataStorage.LOCAL if there is a
	 *         shared filesystem.
	 */
	public DataStorage getDataStorage() {
		return dataStorage;
	}

	/**
	 * Gets the URL of the Galaxy instance we are connected to.
	 * 
	 * @return A String of the URL of the Galaxy instance we are connected to.
	 */
	public URL getGalaxyUrl() {
		try {
			return new URL(galaxyInstance.getGalaxyUrl());
		} catch (MalformedURLException e) {
			// This should never really occur, don't force all calling methods
			// to catch exception
			throw new RuntimeException("Galaxy URL is malformed", e);
		}
	}
}